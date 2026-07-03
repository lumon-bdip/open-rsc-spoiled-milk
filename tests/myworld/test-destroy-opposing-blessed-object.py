#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PLUGIN = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "custom" / "myworld" / "skills" / "prayer" / "DestroyOpposingBlessedObject.java"
DEVOTION = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "content" / "Devotion.java"
PLAN = ROOT / "docs" / "myworld" / "in-progress-work-plans" / "prayer-devotion-equipment-plan.md"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    plugin = PLUGIN.read_text(encoding="utf-8")
    devotion = DEVOTION.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require("class DestroyOpposingBlessedObject implements UseLocTrigger" in plugin,
            "opposing blessed object destruction plugin should exist")
    require("itemGod != altarGod" in plugin,
            "only opposing blessed objects should be intercepted")
    require("altarGod != worshippedGod" in plugin,
            "player should have to worship the altar god")
    require("DEVOTION_CHANGE_PER_RESOURCE = 1" in plugin,
            "devotion reward/penalty should be resource-cost based")
    require("PRAYER_XP_MULTIPLIER = 5" in plugin,
            "destroying opposing gear should grant large Prayer XP")
    require("Devotion.addDevotionOfferings(player, worshippedGod, devotionOfferingChange)" in plugin,
            "destroying opposing gear should reward current god devotion")
    require("Devotion.removeDevotionOfferings(player, itemGod, devotionOfferingChange)" in plugin,
            "destroying opposing gear should reduce destroyed item god devotion")
    require('player.message("You gain " + devotionChangeLabel + " devotion to " + formatGodLine(worshippedGod) + ".")' in plugin,
            "destroying opposing gear should report devotion gained")
    require('player.message("You lose " + devotionChangeLabel + " devotion to " + formatGodLine(itemGod) + ".")' in plugin,
            "destroying opposing gear should report devotion lost")
    require("player.getCarriedItems().remove(item)" in plugin,
            "destroying opposing gear should consume the object")

    for marker in [
        "isZamorakKnightEquipment",
        "isSaradominKnightEquipment",
        "isGuthixKnightEquipment",
        "isZamorakBlessedWool",
        "isSaradominBlessedWool",
        "isGuthixBlessedWool",
        "isZamorakBlessedStaff",
        "isSaradominBlessedStaff",
        "isGuthixBlessedStaff",
    ]:
        require(marker in plugin, f"missing destruction support for {marker}")

    require("removeDevotionLevels" in devotion and "adjustDevotionLevels" in devotion
            and "removeDevotionOfferings" in devotion and "adjustDevotionOfferings" in devotion,
            "devotion should support clamped negative adjustments")
    require("clampOfferings((long) previousOfferings + ((long) devotionLevels * OFFERINGS_PER_DEVOTION_LEVEL))" in devotion,
            "devotion adjustments should clamp between negative and positive caps without overflowing")
    require("SYMBOL_DEVOTION_OFFERING_CHANGE = Devotion.OFFERINGS_PER_DEVOTION_LEVEL / 2" in plugin,
            "symbol destruction should use a half-devotion offering-point swing")
    require("DEVOTION_OFFERINGS_PER_RESOURCE = Devotion.OFFERINGS_PER_DEVOTION_LEVEL * DEVOTION_CHANGE_PER_RESOURCE" in plugin,
            "resource-cost destruction should still translate full devotion levels to offering points")
    require("`5x`" in plan and "`1` devotion per equivalent resource cost" in plan,
            "destroy opposing blessed object formula should be documented")

    print("PASS: opposing blessed object destruction is wired")


if __name__ == "__main__":
    main()
