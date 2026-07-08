#!/usr/bin/env python3
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
SMITHING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/smithing/Smithing.java"
SMELTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/smithing/Smelting.java"
SKILL_GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
INV_ACTION = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/itemactions/InvAction.java"
DWARF_RESCUE = ROOT / "server/plugins/com/openrsc/server/plugins/custom/minigames/DwarfRescue.java"
WAYNE = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/falador/WaynesChains.java"
COMBAT_ODYSSEY = ROOT / "server/plugins/com/openrsc/server/plugins/custom/minigames/CombatOdyssey.java"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def forbid(text: str, needle: str, message: str) -> None:
    if needle in text:
        fail(message)


def require(text: str, needle: str, message: str) -> None:
    if needle not in text:
        fail(message)


def main() -> None:
    smithing = SMITHING.read_text(encoding="utf-8")
    smelting = SMELTING.read_text(encoding="utf-8")
    guide = SKILL_GUIDE.read_text(encoding="utf-8")
    inv_action = INV_ACTION.read_text(encoding="utf-8")
    dwarf_rescue = DWARF_RESCUE.read_text(encoding="utf-8")
    wayne = WAYNE.read_text(encoding="utf-8")
    combat_odyssey = COMBAT_ODYSSEY.read_text(encoding="utf-8")

    forbid(smithing, "LAVA_ANVIL", "Dragon bar smithing should not be handled at the lava anvil")
    forbid(smithing, "DRAGON_METAL_CHAIN.id()", "Dragon metal chain should not have a smithing recipe")
    forbid(smithing, "DRAGON_BAR.id()", "Dragon bar should not be a smithing input")
    require(
        smithing,
        "attemptDragonSquareCombine",
        "Dragon square shield repair must remain available",
    )
    require(
        smithing,
        "createDragonShieldProductionSession",
        "Dragon shield halves should open a production choice UI",
    )
    require(
        smithing,
        "Smithing::beginDragonShieldProductionFromInterface",
        "Dragon shield production UI should use a dedicated starter",
    )
    require(
        smithing,
        "ItemId.DRAGON_SQUARE_SHIELD.id()",
        "Dragon shield production UI should offer square shield",
    )
    require(
        smithing,
        "ItemId.DRAGON_KITE_SHIELD.id()",
        "Dragon shield production UI should offer paladin shield",
    )

    require(smelting, "LAVA_FORGE = SceneryId.LAVA_FORGE.id()", "Raw dragon metal should use the lava forge")
    require(smelting, "DRAGON_SMELTING_LEVEL = 80", "Raw dragon metal should require 80 smithing")
    require(smelting, "RAW_DRAGON_METAL.id()", "Smelting should consume raw dragon metal")
    require(smelting, "DRAGON_BAR.id()", "Raw dragon metal should smelt into dragon bars")
    require(smelting, "DRAGON_METAL_CHAIN.id()", "Raw dragon metal should work directly into chains")
    forbid(smelting, "getDragonMetalSmeltBars", "Dragon item to bar mapping should stay removed")
    for recycled_item in (
        "DRAGON_SWORD.id()",
        "DRAGON_AXE.id()",
        "DRAGON_2_HANDED_SWORD.id()",
        "DRAGON_DAGGER.id()",
        "DRAGON_BATTLE_AXE.id()",
    ):
        forbid(smelting, recycled_item, "Dragon equipment should not be recycled into bars")

    forbid(
        guide,
        "Dragon bar - 1 piece of dragon equipment",
        "Smithing guide should not advertise dragon item smelting",
    )
    forbid(
        guide,
        "Dragon metal chain - 1 dragon bar",
        "Smithing guide should not advertise dragon chain smithing",
    )
    require(
        guide,
        "Dragon square shield - smith the 2 halves together",
        "Smithing guide should still mention dragon square shield repair",
    )
    require(
        guide,
        'new SkillMenuItem(1365, "80", "Dragon bar - 1 raw dragon metal at the lava forge")',
        "Smithing guide should explain raw dragon metal bars",
    )
    require(
        guide,
        'new SkillMenuItem(1367, "80", "Dragon metal chains - 1 raw dragon metal at the lava forge")',
        "Smithing guide should explain raw dragon metal chains",
    )

    for note_text in (inv_action, dwarf_rescue):
        forbid(
            note_text,
            "Dragon metal chains can be smithed",
            "Dwarf smithy notes should not describe removed chain smithing",
        )
        forbid(
            note_text,
            "obtain dragon bars",
            "Dwarf smithy notes should not point players at removed dragon bars",
        )
        require(note_text, "Dragon scale mail legs", "Dwarf smithy notes should list dragon scale mail legs")
        require(note_text, "100 Chipped Dragon Scales", "Scale legs should cost 50 fewer chipped scales than body")
        require(note_text, "Dragon plate mail legs", "Dwarf smithy notes should list dragon plate mail legs")
        require(
            note_text,
            "Complete Combat Odyssey through the Legends Guild",
            "Dwarf smithy notes should point dragon plate legs at Combat Odyssey",
        )

    require(wayne, "ItemId.DRAGON_SCALE_MAIL_LEGS.id()", "Wayne should craft dragon scale mail legs")
    require(wayne, "SCALE_MAIL_LEGS_SCALE_COST = 100", "Wayne scale legs should cost 100 chipped scales")
    forbid(wayne, "ItemId.DRAGON_PLATE_MAIL_LEGS.id()", "Wayne should not craft dragon plate mail legs")
    forbid(wayne, "PLATELEGS_BAR_COST", "Wayne should not retain a plate legs bar cost")

    require(
        combat_odyssey,
        "ItemId.DRAGON_PLATE_MAIL_LEGS.id()",
        "Combat Odyssey should remain the dragon plate legs route",
    )
    forbid(
        combat_odyssey,
        "Dragon Plated Skirt",
        "Combat Odyssey should not offer the retired dragon skirt reward",
    )


if __name__ == "__main__":
    main()
