#!/usr/bin/env python3
"""Validate retired Firemaking behavior stays inert but usable."""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FIREMAKING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/firemaking/Firemaking.java"
FIREMAKING_DEF = ROOT / "server/conf/server/defs/extras/FiremakingDef.xml"
SKILLS_MODEL = ROOT / "server/src/com/openrsc/server/model/Skills.java"
SKILLS_CONSTANTS = ROOT / "server/src/com/openrsc/server/constants/Skills.java"
SKILL_GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
INV_ACTION = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/itemactions/InvAction.java"
QUEST_REWARDS = ROOT / "server/plugins/com/openrsc/server/plugins/shared/QuestRewardRegistrar.java"
ENCHANTING_EFFECTS = ROOT / "server/src/com/openrsc/server/content/EnchantingItemEffects.java"
SERVER_ITEMS = ROOT / "server/conf/server/defs/ItemDefsCustom.json"
CLIENT_ITEMS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
CANDLE_MAKER = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/catherby/CandleMakerShop.java"
A_BONE_TO_PICK = ROOT / "server/plugins/com/openrsc/server/plugins/custom/minigames/ABoneToPick.java"

EXPECTED_LOGS = {
    14: ("LOGS", 1, 90),
    2111: ("PINE_LOGS", 8, 100),
    632: ("OAK_LOGS", 15, 110),
    633: ("WILLOW_LOGS", 22, 130),
    2112: ("PALM_LOGS", 30, 140),
    634: ("MAPLE_LOGS", 38, 150),
    635: ("YEW_LOGS", 46, 170),
    2113: ("EBONY_LOGS", 54, 180),
    636: ("MAGIC_LOGS", 62, 190),
    2114: ("BLOOD_LOGS", 70, 200),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def parse_firemaking_defs() -> dict[int, dict[str, int]]:
    root = ET.parse(FIREMAKING_DEF).getroot()
    entries = {}
    for entry in root.findall("entry"):
        item_id = int(entry.findtext("int"))
        def_node = entry.find("FiremakingDef")
        entries[item_id] = {
            "level": int(def_node.findtext("level")),
            "exp": int(def_node.findtext("exp")),
            "length": int(def_node.findtext("length")),
        }
    return entries


def main() -> None:
    firemaking_text = FIREMAKING.read_text(encoding="utf-8")
    skills_model_text = SKILLS_MODEL.read_text(encoding="utf-8")
    skills_constants_text = SKILLS_CONSTANTS.read_text(encoding="utf-8")
    skill_guide_text = SKILL_GUIDE.read_text(encoding="utf-8")
    client_text = CLIENT.read_text(encoding="utf-8")
    inv_action_text = INV_ACTION.read_text(encoding="utf-8")
    quest_rewards_text = QUEST_REWARDS.read_text(encoding="utf-8")
    enchanting_effects_text = ENCHANTING_EFFECTS.read_text(encoding="utf-8")
    server_items_text = SERVER_ITEMS.read_text(encoding="utf-8")
    client_items_text = CLIENT_ITEMS.read_text(encoding="utf-8")
    candle_maker_text = CANDLE_MAKER.read_text(encoding="utf-8")
    a_bone_to_pick_text = A_BONE_TO_PICK.read_text(encoding="utf-8")
    definitions = parse_firemaking_defs()

    if set(definitions) != set(EXPECTED_LOGS):
        missing = sorted(set(EXPECTED_LOGS) - set(definitions))
        extra = sorted(set(definitions) - set(EXPECTED_LOGS))
        fail(f"FiremakingDef log set mismatch; missing={missing}, extra={extra}")

    for item_id, (constant, level, length) in EXPECTED_LOGS.items():
        if f"ItemId.{constant}.id()" not in firemaking_text:
            fail(f"Firemaking LOGS array missing ItemId.{constant}.id()")
        if f"case {constant}:" not in firemaking_text:
            fail(f"Custom Firemaking switch missing {constant}")
        if definitions[item_id]["level"] != level:
            fail(f"{constant} requires level {definitions[item_id]['level']}, expected {level}")
        if definitions[item_id]["length"] != length:
            fail(f"{constant} burns for {definitions[item_id]['length']}s, expected {length}s")
        if definitions[item_id]["exp"] != 0:
            fail(f"{constant} should not carry Firemaking XP")

    hidden_level = 99
    highest_requirement = max(definition["level"] for definition in definitions.values())
    if highest_requirement > hidden_level:
        fail(
            f"Firemaking requirement {highest_requirement} exceeds hidden level {hidden_level}"
        )

    forbidden_pairs = (
        (firemaking_text, "player.incExp(Skill.FIREMAKING.id()", "Firemaking action should not award XP"),
        (firemaking_text, "You need at least", "Firemaking action should not display skill gates"),
        (inv_action_text, "firemaking level", "Dry sticks should not require Firemaking"),
        (inv_action_text, "Skill.FIREMAKING", "Dry sticks should not award Firemaking XP"),
        (quest_rewards_text, "Skill.FIREMAKING", "Quest rewards should not include Firemaking XP"),
        (enchanting_effects_text, "|| skillId == Skill.FIREMAKING.id()", "Hearthcraft should not boost Firemaking XP"),
        (skill_guide_text, 'getSkillGuideChosen().equals("Firemaking")', "Skill guide should not expose Firemaking"),
        (skill_guide_text, 'addSkillCapeGuide(1520, "Firemaking")', "Skill guide should not expose the old cape as a skill cape"),
        (client_text, '"30 Firemaking"', "Quest guide should not list Firemaking requirements"),
        (client_text, "Ranged, Firemaking, Woodcutting", "Quest guide should not list Firemaking rewards"),
        (server_items_text, "firemaking XP", "Server item descriptions should not advertise Firemaking XP"),
        (client_items_text, "firemaking XP", "Client item descriptions should not advertise Firemaking XP"),
        (server_items_text, '"name": "Firemaking cape"', "Server item name should not expose the retired skill"),
        (client_items_text, 'new ItemDef("Firemaking cape"', "Client item name should not expose the retired skill"),
        (candle_maker_text, "Firemaking", "Cape shop dialogue should not expose the retired skill"),
        (a_bone_to_pick_text, "firemaking", "Minigame dialogue should not expose the retired skill"),
    )
    for text, snippet, message in forbidden_pairs:
        if snippet in text:
            fail(message)

    required_pairs = (
        (skills_constants_text, "FIREMAKING = 11", "Firemaking must retain protocol skill ID 11"),
        (skills_constants_text, 'new SkillDef("Firemaking", "Firemaking", 1, 99', "Firemaking must retain its server skill slot"),
        (skills_model_text, "private static final int HIDDEN_SKILL_LEVEL = 99;", "Retired Firemaking must remain auto-maxed for players"),
        (skills_model_text, "skill == Skill.FIREMAKING.id()", "Hidden-skill handling must still target Firemaking"),
        (skills_model_text, "applyHiddenSkillDefaults();", "Loaded player data must restore the hidden Firemaking defaults"),
        (firemaking_text, "Formulae.lightLogs(player.getSkills().getLevel(Skill.FIREMAKING.id()))", "Standard logs must use the hidden Firemaking level"),
        (firemaking_text, "Formulae.lightCustomLogs(def, player.getSkills().getLevel(Skill.FIREMAKING.id()))", "Custom logs must use the hidden Firemaking level"),
        (firemaking_text, "player.getWorld().registerGameObject(fire);", "Log lighting should still create fires"),
        (inv_action_text, "player.getCarriedItems().getInventory().add(new Item(ItemId.LIT_TORCH.id()));", "Dry sticks should still light torches"),
        (client_text, 'addSkill("Retired");', "Hidden stat slot should be labeled retired in the client"),
        (server_items_text, '"name": "Combustion cape"', "Server item name should use non-skill wording"),
        (client_items_text, 'new ItemDef("Combustion cape"', "Client item name should use non-skill wording"),
        (client_items_text, '"Boosts cooking, herblaw, and fishing XP by %d%%."', "Hearthcraft client description should exclude Firemaking"),
        (server_items_text, "Boosts cooking, herblaw, and fishing XP by 50%.", "Hearthcraft server descriptions should exclude Firemaking"),
    )
    for text, snippet, message in required_pairs:
        if snippet not in text:
            fail(message)

    print("PASS: retired Firemaking keeps protocol ID 11 and all log tiers remain usable")


if __name__ == "__main__":
    main()
