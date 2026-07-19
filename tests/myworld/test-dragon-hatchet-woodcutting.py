#!/usr/bin/env python3
"""Guard Dragon Hatchet definitions and shared Woodcutting recognition."""

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ITEM_SOURCE = ROOT / "tools/generators/item-overrides/10-melee-weapons.json"
BASE_SERVER_ITEMS = ROOT / "server/conf/server/defs/ItemDefs.json"
SERVER_ITEMS = ROOT / "server/conf/server/defs/ItemDefsMyWorld.json"
CLIENT_ITEMS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/MyWorldItemOverrides.java"
ITEM_IDS = ROOT / "server/src/com/openrsc/server/constants/ItemId.java"
FORMULAE = ROOT / "server/src/com/openrsc/server/util/rsc/Formulae.java"
WOODCUTTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/Woodcutting.java"
JUNGLE = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/WoodcutJungle.java"

DRAGON_HATCHET_IDS = (594, 1480)
HATCHET_IDS = (3267, 594, 1480, 405, 2034, 204, 2023, 203, 428, 88, 12, 87, 2012, 2001)
EXPECTED_RUNTIME_TOOLS = (
    ("MyWorldItemId.EXALTED_RUNE_HATCHET", 90),
    ("ItemId.DRAGON_AXE.id()", 80),
    ("ItemId.DRAGON_WOODCUTTING_AXE.id()", 80),
    ("ItemId.RUNE_AXE.id()", 70),
    ("ItemId.ORICHALCUM_AXE.id()", 62),
    ("ItemId.ADAMANTITE_AXE.id()", 54),
    ("ItemId.TITAN_STEEL_AXE.id()", 46),
    ("ItemId.MITHRIL_AXE.id()", 38),
    ("ItemId.BLACK_AXE.id()", 30),
    ("ItemId.STEEL_AXE.id()", 30),
    ("ItemId.IRON_AXE.id()", 22),
    ("ItemId.BRONZE_AXE.id()", 15),
    ("ItemId.COPPER_AXE.id()", 8),
    ("ItemId.TIN_AXE.id()", 1),
)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def load_items(path: Path) -> dict[int, dict]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    entries = payload.get("items", payload.get("item", []))
    return {int(entry["id"]): entry for entry in entries}


def java_array(text: str, name: str) -> list[str]:
    match = re.search(
        rf"public static final int\[\] {re.escape(name)}\s*=\s*\{{(.*?)\}};",
        text,
        re.DOTALL,
    )
    if match is None:
        fail(f"Missing Java array {name}")
    return [entry.strip() for entry in match.group(1).split(",") if entry.strip()]


def method_body(text: str, signature: str, next_signature: str) -> str:
    match = re.search(
        rf"{re.escape(signature)}(.*?){re.escape(next_signature)}",
        text,
        re.DOTALL,
    )
    if match is None:
        fail(f"Could not isolate {signature}")
    return match.group(1)


def require_item_profiles() -> None:
    source_items = load_items(ITEM_SOURCE)
    base_items = load_items(BASE_SERVER_ITEMS)
    generated_items = load_items(SERVER_ITEMS)
    expected_dragon_names = {
        594: "Dragon Hatchet",
        1480: "Dragon Woodcutting Hatchet",
    }
    required_profile = {
        "isWearable": 1,
        "wearableID": 16,
        "wearSlot": 4,
        "requiredLevel": 80,
        "requiredSkillID": 8,
        "meleeOffense": 0,
        "weaponAimBonus": 0,
        "weaponPowerBonus": 0,
        "rangedOffense": 0,
        "magicOffense": 0,
    }
    for item_id in DRAGON_HATCHET_IDS:
        for label, items in (("authoritative", source_items), ("generated", generated_items)):
            item = items.get(item_id)
            if item is None:
                fail(f"{label} Dragon Hatchet {item_id} is missing")
            if item.get("name") != expected_dragon_names[item_id]:
                fail(f"{label} item {item_id} has unexpected name {item.get('name')!r}")
            for field, expected in required_profile.items():
                if item.get(field) != expected:
                    fail(f"{label} item {item_id} field {field} expected {expected}, found {item.get(field)!r}")

    if base_items[594].get("appearanceID") != 162:
        fail("Dragon Hatchet 594 no longer uses its established equipped animation 162")

    dragon_battle_axe = source_items.get(2752)
    if dragon_battle_axe is None:
        fail("Authoritative Dragon Battle Axe 2752 profile is missing")
    if dragon_battle_axe.get("meleeOffense", 0) <= 0 or dragon_battle_axe.get("weaponSpeed") != 3:
        fail("Dragon Battle Axe 2752 must remain a melee weapon")
    if dragon_battle_axe.get("requiredSkillID") == 8:
        fail("Dragon Battle Axe 2752 must not become a Woodcutting tool")

    for item_id in HATCHET_IDS:
        item = generated_items.get(item_id)
        if item is None:
            fail(f"Generated hatchet tier {item_id} is missing")
        for field, expected in (
            ("isWearable", 1),
            ("wearableID", 16),
            ("wearSlot", 4),
            ("requiredSkillID", 8),
            ("meleeOffense", 0),
            ("weaponAimBonus", 0),
            ("weaponPowerBonus", 0),
        ):
            if item.get(field) != expected:
                fail(f"Generated hatchet {item_id} field {field} expected {expected}, found {item.get(field)!r}")

    client_text = CLIENT_ITEMS.read_text(encoding="utf-8")
    for item_id, name in expected_dragon_names.items():
        expected = f'new ItemOverride({item_id}, "{name}", null, 120000, 1, 16)'
        if expected not in client_text:
            fail(f"Generated client override is missing Dragon Hatchet {item_id}")


def require_runtime_recognition() -> None:
    formulae_text = FORMULAE.read_text(encoding="utf-8")
    tools = java_array(formulae_text, "woodcuttingAxeIDs")
    levels = [int(level) for level in java_array(formulae_text, "woodcuttingAxeLvls")]
    expected_tools = [tool for tool, _ in EXPECTED_RUNTIME_TOOLS]
    expected_levels = [level for _, level in EXPECTED_RUNTIME_TOOLS]
    if tools != expected_tools:
        fail(f"Woodcutting tool order mismatch:\nexpected {expected_tools}\nfound    {tools}")
    if levels != expected_levels:
        fail(f"Woodcutting level order mismatch: expected {expected_levels}, found {levels}")
    if len(tools) != len(levels):
        fail("Woodcutting tool IDs and levels are not parallel")
    if "ItemId.DRAGON_BATTLE_AXE.id()" in tools:
        fail("Dragon Battle Axe must remain excluded from Woodcutting tools")

    item_ids_text = ITEM_IDS.read_text(encoding="utf-8")
    for snippet in ("DRAGON_AXE(594)", "DRAGON_WOODCUTTING_AXE(1480)", "DRAGON_BATTLE_AXE(2752)"):
        if snippet not in item_ids_text:
            fail(f"Item identity guard missing {snippet}")


def require_shared_equipment_policy() -> None:
    woodcutting_text = WOODCUTTING.read_text(encoding="utf-8")
    selector = method_body(woodcutting_text, "public static int getAxe(Player player)", "public static int getAxeTier")
    if "getEquipment().hasCatalogID(Formulae.woodcuttingAxeIDs[i])" not in selector:
        fail("Woodcutting selector no longer recognizes equipped tools")
    if "getInventory()" in selector or "hasCatalogID(Formulae.woodcuttingAxeIDs[i], Optional.of(false))" in selector:
        fail("Normal Woodcutting must not recognize inventory-only tools")

    tier_switch = method_body(woodcutting_text, "public static int getAxeTier(int axeId)", "public static String getWoodcuttingFocusLabel")
    for case in ("case DRAGON_AXE:", "case DRAGON_WOODCUTTING_AXE:"):
        if case not in tier_switch:
            fail(f"Woodcutting tier selector is missing {case}")
    if "case DRAGON_BATTLE_AXE:" in tier_switch:
        fail("Dragon Battle Axe must not receive a Woodcutting tier")

    jungle_text = JUNGLE.read_text(encoding="utf-8")
    jungle_bonus = method_body(jungle_text, "public int calcAxeBonus(int axeId)", "private boolean getLog")
    for case in ("case DRAGON_AXE:", "case DRAGON_WOODCUTTING_AXE:"):
        if case not in jungle_bonus:
            fail(f"Jungle Woodcutting bonus is missing {case}")
    if "case DRAGON_BATTLE_AXE:" in jungle_bonus:
        fail("Dragon Battle Axe must not receive a jungle Woodcutting bonus")


def main() -> None:
    require_item_profiles()
    require_runtime_recognition()
    require_shared_equipment_policy()
    print("Dragon Hatchet Woodcutting checks passed")


if __name__ == "__main__":
    main()
