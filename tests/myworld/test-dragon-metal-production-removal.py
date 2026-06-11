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

    forbid(smithing, "LAVA_ANVIL", "Dragon bar smithing should not be handled at the lava anvil")
    forbid(smithing, "DRAGON_METAL_CHAIN.id()", "Dragon metal chain should not have a smithing recipe")
    forbid(smithing, "DRAGON_BAR.id()", "Dragon bar should not be a smithing input")
    require(
        smithing,
        "attemptDragonSquareCombine",
        "Dragon square shield repair must remain available",
    )

    forbid(smelting, "LAVA_FURNACE", "Dragon item recycling should not be handled at the lava forge")
    forbid(smelting, "handleLavaFurnace", "Dragon item recycling handler should stay removed")
    forbid(smelting, "getDragonMetalSmeltBars", "Dragon item to bar mapping should stay removed")
    forbid(smelting, "DRAGON_BAR.id()", "Dragon bar should not be produced by smelting")

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


if __name__ == "__main__":
    main()
