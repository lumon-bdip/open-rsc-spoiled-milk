#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEVOTION = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "content" / "Devotion.java"
EQUIPMENT = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "model" / "container" / "Equipment.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    devotion = DEVOTION.read_text()
    equipment = EQUIPMENT.read_text()

    require(devotion, "COMBAT_GROWTH_START_LEVEL = 250", "combat growth start")
    require(devotion, "PRAYER_BONUS_GROWTH_MAX = 10", "prayer bonus growth cap")
    require(devotion, "getDevotionGrowthBonus", "shared devotion growth helper")
    require(devotion, "getPrayerBonusGrowth", "shared prayer growth helper")
    require(devotion, "ActionSender.sendEquipmentStats(player);",
            "devotion changes should refresh dynamic equipment stats")

    require(equipment, "getScaledWeaponAimBonus(item)", "scaled weapon aim in equipment total")
    require(equipment, "getScaledWeaponPowerBonus(item)", "scaled weapon power in equipment total")
    require(equipment, "getScaledArmourBonus(item)", "scaled armour in equipment total")
    require(equipment, "getGodEquipmentTargetMeleeOffense(item.getCatalogId())",
            "explicit melee offense should scale with devotion")
    require(equipment, "getGodEquipmentTargetMeleeDefense(item.getCatalogId())",
            "explicit melee defense should scale with devotion")
    require(equipment, "getGodEquipmentTargetRangedDefense(item.getCatalogId())",
            "explicit ranged defense should scale with devotion")
    require(equipment, "getGodEquipmentTargetMagicDefense(item.getCatalogId())",
            "explicit magic defense should scale with devotion")
    require(equipment, "getGodEquipmentPrayerBonus(item.getCatalogId())", "god equipment prayer override")
    if not ("isZamorakBlessedStaff(itemId)" in equipment
            and "isSaradominBlessedStaff(itemId)" in equipment
            and "isGuthixBlessedStaff(itemId)" in equipment):
        fail("missing blessed staff god-line assignment")
    require(equipment, "return PrayerCatalog.GodLine.ZAMORAK;", "legacy blessed staves should be Zamorak")
    require(equipment, "Devotion.getDevotionGrowthBonus(player, godLine, targetValue - baseValue)",
            "combat stats should grow from current value to target value")
    require(equipment, "Devotion.getPrayerBonusGrowth(player, godLine)",
            "prayer bonus should gain devotion growth")
    require(equipment, "isBlessedWoolArmor(item.getCatalogId())",
            "blessed wool armor should participate in magic-defense scaling")
    require(equipment, "getBlessedWoolMagicDefense(item)",
            "blessed wool armor should have derived magic defense")

    for item in (
        "case 3131: // BLACK_GAUNTLETS",
        "case 3132: // BLACK_GREAVES",
        "case 3133: // WHITE_GAUNTLETS",
        "case 3134: // WHITE_GREAVES",
        "case 3135: // GREY_GAUNTLETS",
        "case 3136: // GREY_GREAVES",
        "case 3229: // BLACK_SPEAR",
        "case 3230: // WHITE_SPEAR",
        "case 3231: // GREY_SPEAR",
        "case 3232: // BLACK_SCYTHE",
        "case 3233: // WHITE_SCYTHE",
        "case 3234: // GREY_SCYTHE",
        "case 3138: // ZAMORAK_WOOL_ROBE_TOP",
        "case 3143: // SARADOMIN_WOOL_ROBE_TOP",
        "case 3148: // GUTHIX_WOOL_ROBE_TOP",
    ):
        require(equipment, item, f"{item} worship matching/scaling coverage")

    require(equipment, "case 196: // BLACK_PLATE_MAIL_BODY", "black plate body resource cost")
    require(equipment, "case 2163: // WHITE_PLATE_MAIL_BODY", "white plate body resource cost")
    require(equipment, "case 3125: // GREY_PLATE_MAIL_BODY", "grey plate body resource cost")
    require(equipment, "return 4;", "plate body resource cost should produce baseline +4 prayer")
    require(equipment, "return 63;", "plate body should scale to adamantite plate body armour")
    require(equipment, "return 30;", "plate body melee defense and battle axe aim should reach tier 8 equivalents")
    require(equipment, "return 24;", "mace aim, spear aim, and paladin shield armour targets should reach tier 8 equivalents")
    require(equipment, "return 18;", "mace power and paladin shield melee defense should reach tier 8 equivalents")
    require(equipment, "return 99;", "blessed scythes should scale to adamantite scythe power")
    require(equipment, "+ resourceCost", "baseline prayer should include resource equivalency")

    print("PASS: devotion equipment scaling hooks are wired")


if __name__ == "__main__":
    main()
