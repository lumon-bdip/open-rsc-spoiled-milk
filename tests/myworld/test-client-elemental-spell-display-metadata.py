#!/usr/bin/env python3

import json
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPELL_XML = ROOT / "server/conf/server/defs/SpellDef.xml"
CONSTANTS = ROOT / "server/src/com/openrsc/server/constants/Constants.java"
SPELL_DAMAGES = ROOT / "server/src/com/openrsc/server/constants/SpellDamages.java"
SPELL_CLASSIFICATION = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/SpellClassification.java"
METADATA = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/defs/ElementalSpellDisplayMetadata.java"
ENTITY_DEF = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/defs/EntityDef.java"
SPELL_DEF = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/defs/SpellDef.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
SKILL_GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"


CLASSIFICATION_METHODS = {
    "AIR": "isAirSpell",
    "WATER": "isWaterSpell",
    "EARTH": "isEarthSpell",
    "FIRE": "isFireSpell",
    "THUNDER": "isThunderSpell",
    "ICE": "isIceSpell",
    "ACID": "isAcidSpell",
    "WOOD": "isWoodSpell",
}
EFFECTS = {
    "AIR": "Unsteady",
    "WATER": "Dampen",
    "EARTH": "Slow",
    "FIRE": "Scorch",
    "THUNDER": "Startle",
    "ICE": "Frostbite",
    "ACID": "Corrode",
    "WOOD": "Splinter",
}
QUALIFIERS = {1: "Weaker", 2: "Weak", 3: "Strong", 4: "Stronger"}
DAMAGE_WORDS = {1: "minor", 2: "moderate", 3: "major", 4: "heavy"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def parse_spells() -> list[dict]:
    spells = []
    for spell in ET.parse(SPELL_XML).getroot().findall("SpellDef"):
        runes = {}
        required = spell.find("requiredRunes")
        if required is not None:
            for entry in required.findall("entry"):
                values = [int(node.text or "0") for node in entry.findall("int")]
                if len(values) == 2:
                    runes[values[0]] = values[1]
        spells.append({
            "name": spell.findtext("name", ""),
            "description": spell.findtext("description", ""),
            "level": int(spell.findtext("reqLevel", "0")),
            "type": int(spell.findtext("type", "0")),
            "rune_count": int(spell.findtext("runeCount", "0")),
            "runes": runes,
        })
    return spells


def method_body(source: str, method_name: str) -> str:
    match = re.search(
        rf"(?:private )?static [^\n]+ {method_name}\([^)]*\) \{{(.*?)\n\t\}}",
        source,
        flags=re.S,
    )
    require(match is not None, f"Could not locate server method {method_name}")
    return match.group(1)


def server_metadata() -> dict[int, dict]:
    constants = CONSTANTS.read_text(encoding="utf-8")
    classification_source = SPELL_CLASSIFICATION.read_text(encoding="utf-8")
    damages = SPELL_DAMAGES.read_text(encoding="utf-8")

    indexes = {
        identity: int(index)
        for identity, index in re.findall(r"put\(Spells\.([A-Z0-9_]+),\s*(\d+)\);", constants)
    }
    damage_by_identity = {}
    for identity, damage in re.findall(
        r"put\(Spells\.([A-Z0-9_]+),.*?Pair\.of\(EntityType\.PLAYER,\s*([0-9.]+)\)",
        damages,
        flags=re.S,
    ):
        damage_by_identity.setdefault(identity, float(damage))

    cap_by_identity = {}
    cap_body = method_body(classification_source, "getSpellDamageCapPercent")
    for cases, cap in re.findall(
        r"((?:\s*case [A-Z0-9_]+:)+)\s*return ([0-9.]+)D;", cap_body
    ):
        for identity in re.findall(r"case ([A-Z0-9_]+):", cases):
            cap_by_identity[identity] = float(cap)

    result = {}
    for classification, server_method in CLASSIFICATION_METHODS.items():
        body = method_body(classification_source, server_method)
        for identity in re.findall(r"Spells\.([A-Z0-9_]+)", body):
            if identity not in indexes:
                continue
            index = indexes[identity]
            require(index not in result, f"Spell index {index} has two elemental classifications")
            require(identity in damage_by_identity, f"Missing server base damage for {identity}")
            require(identity in cap_by_identity, f"Missing server damage cap for {identity}")
            cap = cap_by_identity[identity]
            tier = round(cap * 5) - 1
            result[index] = {
                "identity": identity,
                "classification": classification,
                "base": damage_by_identity[identity],
                "cap": cap,
                "tier": tier,
            }
    require(len(result) == 28, f"Expected 28 server elemental identities, found {len(result)}")
    return result


def java_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=True)


def run_metadata_fixture(spells: list[dict]) -> dict[int, dict]:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")

    statements = []
    for index, spell in enumerate(spells):
        statements.append("runes.clear();")
        for rune_id, amount in spell["runes"].items():
            statements.append(f"runes.put({rune_id}, {amount});")
        statements.append(
            "spell = new SpellDef("
            f"{java_string(spell['name'])}, {java_string(spell['description'])}, "
            f"{spell['level']}, {spell['type']}, {spell['rune_count']}, new HashMap<Integer, Integer>(runes));"
        )
        statements.append(
            f"print({index}, ElementalSpellDisplayMetadata.resolve({index}, spell));"
        )

    fixture = """
package com.openrsc.client.entityhandling.defs;

import java.util.HashMap;

public final class ElementalMetadataFixture {
    private static void print(int index, ElementalSpellDisplayMetadata metadata) {
        if (metadata == null) return;
        System.out.println(index + "\\t" + metadata.getSpellName() + "\\t"
            + metadata.getTier() + "\\t" + metadata.getClassification() + "\\t"
            + metadata.isDualElement() + "\\t" + metadata.getBaseDamage() + "\\t"
            + metadata.getDamageCapPercent() + "\\t" + metadata.getDamageWord() + "\\t"
            + metadata.getEffectName() + "\\t" + metadata.getGuideTooltip());
    }

    public static void main(String[] args) {
        HashMap<Integer, Integer> runes = new HashMap<Integer, Integer>();
        SpellDef spell;
        %s
    }
}
""" % "\n        ".join(statements)

    with tempfile.TemporaryDirectory(prefix="elemental-metadata-") as directory:
        temp = Path(directory)
        fixture_path = temp / "ElementalMetadataFixture.java"
        fixture_path.write_text(fixture, encoding="utf-8")
        compile_result = subprocess.run(
            [javac, "-d", str(temp), str(ENTITY_DEF), str(SPELL_DEF), str(METADATA), str(fixture_path)],
            capture_output=True,
            text=True,
        )
        require(compile_result.returncode == 0, f"Metadata fixture failed to compile:\n{compile_result.stderr}")
        run_result = subprocess.run(
            [java, "-cp", str(temp), "com.openrsc.client.entityhandling.defs.ElementalMetadataFixture"],
            capture_output=True,
            text=True,
        )
        require(run_result.returncode == 0, f"Metadata fixture failed:\n{run_result.stderr}")

    actual = {}
    for line in run_result.stdout.splitlines():
        fields = line.split("\t")
        require(len(fields) == 10, f"Unexpected metadata fixture row: {line}")
        index = int(fields[0])
        actual[index] = {
            "name": fields[1],
            "tier": int(fields[2]),
            "classification": fields[3],
            "dual": fields[4] == "true",
            "base": float(fields[5]),
            "cap": float(fields[6]),
            "word": fields[7],
            "effect": fields[8],
            "guide": fields[9],
        }
    return actual


def ensure_display_consumers_share_metadata() -> None:
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    skill_guide = SKILL_GUIDE.read_text(encoding="utf-8")
    for source, label in ((mudclient, "mudclient"), (skill_guide, "SkillGuideInterface")):
        require(
            "ElementalSpellDisplayMetadata.resolve(spellIndex, spell" in source,
            f"{label} must resolve shared spell-index metadata",
        )
        require("metadata.getSpellName()" in source, f"{label} must use the shared display name")

    for helper in (
        "isTierOneElementalSpell", "isTierTwoElementalSpell", "isTierThreeElementalSpell",
        "isTierOneDualElementalSpell", "isTierTwoDualElementalSpell",
        "isTierThreeDualElementalSpell", "getElementalDamageWord",
        "getDualElementalEffectName",
    ):
        require(helper not in mudclient and helper not in skill_guide, f"Duplicated helper remains: {helper}")

    require("metadata.getBaseDamage()" in mudclient, "Combat tooltip must use shared base damage")
    require("metadata.getDamageCapPercent()" in mudclient, "Combat tooltip must use shared damage cap")
    require("metadata.getClassification()" in mudclient, "Ring tooltip must use shared classification")
    require("metadata.getGuideTooltip()" in skill_guide, "Skill guide must use shared damage word/effect")


def main() -> int:
    spells = parse_spells()
    expected = server_metadata()
    actual = run_metadata_fixture(spells)
    require(set(actual) == set(expected), f"Client/server elemental index sets differ: {set(actual) ^ set(expected)}")

    for index, server in sorted(expected.items()):
        row = actual[index]
        classification = server["classification"]
        tier = server["tier"]
        dual = classification in {"THUNDER", "ICE", "ACID", "WOOD"}
        effect = EFFECTS[classification]
        if not dual:
            effect = f"{QUALIFIERS[tier]} {effect}"
        guide = (
            f"Deals {DAMAGE_WORDS[tier]} damage. Can {effect}."
            if dual else f"Deals {DAMAGE_WORDS[tier]} damage. Applies {effect}."
        )
        require(row["name"] == spells[index]["name"], f"Index {index} name drifted")
        require(row["tier"] == tier, f"Index {index} tier differs from server cap")
        require(row["classification"] == classification, f"Index {index} classification drifted")
        require(row["dual"] == dual, f"Index {index} dual-element classification drifted")
        require(abs(row["base"] - server["base"]) < 0.000001, f"Index {index} base damage drifted")
        require(abs(row["cap"] - server["cap"]) < 0.000001, f"Index {index} cap drifted")
        require(row["word"] == DAMAGE_WORDS[tier], f"Index {index} damage word drifted")
        require(row["effect"] == effect, f"Index {index} effect classification drifted")
        require(row["guide"] == guide, f"Index {index} guide tooltip drifted")

    tier_two_names = {spells[index]["name"] for index in range(19, 23)}
    require(
        tier_two_names == {"Thunder Bird", "Ice Slash", "Acid Splash", "Wood Drill"},
        f"Tier-two dual-element display names drifted: {tier_two_names}",
    )
    ensure_display_consumers_share_metadata()
    print("PASS: all 28 elemental spell identities match server names, classes, bases, caps, and client displays")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, FileNotFoundError) as error:
        print(f"FAIL: {error}")
        raise SystemExit(1)
