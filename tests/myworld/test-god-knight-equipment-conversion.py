#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SERVER = ROOT / "server"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def main() -> None:
    equipment = (SERVER / "plugins/com/openrsc/server/plugins/custom/myworld/skills/prayer/GodKnightEquipment.java").read_text(encoding="utf-8")
    constants = (SERVER / "src/com/openrsc/server/constants/ItemId.java").read_text(encoding="utf-8")
    custom_defs = json.loads((SERVER / "conf/server/defs/ItemDefsCustom.json").read_text(encoding="utf-8"))["items"]
    myworld_defs = json.loads((SERVER / "conf/server/defs/ItemDefsMyWorld.json").read_text(encoding="utf-8"))["items"]

    expected_items = {
        3131: ("Black gauntlets", 8),
        3132: ("Black greaves", 9),
        3133: ("White gauntlets", 8),
        3134: ("White greaves", 9),
        3135: ("Grey gauntlets", 8),
        3136: ("Grey greaves", 9),
    }
    custom_by_id = {item["id"]: item for item in custom_defs}
    myworld_by_id = {item["id"]: item for item in myworld_defs}

    for item_id, (name, slot) in expected_items.items():
        require(f"({item_id})" in constants, f"Missing ItemId constant for {name}")
        item = custom_by_id.get(item_id)
        require(item is not None, f"Missing custom item definition for {name}")
        require(item["name"] == name, f"Incorrect item name for {item_id}: {item['name']}")
        require(item["isWearable"] == 1 and item["wearSlot"] == slot, f"{name} should be wearable in slot {slot}")
        override = myworld_by_id.get(item_id)
        require(override is not None, f"Missing MyWorld combat override for {name}")
        require(override["meleeDefense"] == 8 and override["rangedDefense"] == 2, f"{name} should mirror steel hand/foot defense")
        require(override["prayerBonus"] == 1, f"{name} should have a small prayer bonus")

    for source, black, white, grey in (
        ("case 698: // STEEL_GAUNTLETS", "ItemId.BLACK_GAUNTLETS.id()", "ItemId.WHITE_GAUNTLETS.id()", "ItemId.GREY_GAUNTLETS.id()"),
        ("case 1988: // STEEL_GREAVES", "ItemId.BLACK_GREAVES.id()", "ItemId.WHITE_GREAVES.id()", "ItemId.GREY_GREAVES.id()"),
    ):
        require(source in equipment, f"Missing source mapping: {source}")
        require(black in equipment, f"Missing Zamorak product mapping for {source}")
        require(white in equipment, f"Missing Saradomin product mapping for {source}")
        require(grey in equipment, f"Missing Guthix product mapping for {source}")

    for source, requirement in (
        ("case 698: // STEEL_GAUNTLETS", "return 100;"),
        ("case 1988: // STEEL_GREAVES", "return 200;"),
        ("case 105: // MEDIUM_STEEL_HELMET", "return 300;"),
        ("case 121: // STEEL_PLATE_MAIL_LEGS", "return 400;"),
        ("case 118: // STEEL_PLATE_MAIL_BODY", "return 500;"),
    ):
        require(source in equipment and requirement in equipment, f"Missing devotion requirement for {source}")

    require("Devotion.getDevotionLevel(player, godLine) < devotionRequirement" in equipment,
            "steel armour conversion should be gated by devotion")
    require("player.incExp(Skill.PRAYER.id(), prayerXp, true)" in equipment,
            "steel armour conversion should grant Prayer XP")
    require("Devotion.addDevotionLevels(player, godLine, 1)" in equipment,
            "steel armour conversion should add one devotion reward")

    require("public static final int maxCustom = 3137;" in constants, "ItemId.maxCustom should include god gauntlets and greaves")
    print("PASS: god knight armour conversion validated")


if __name__ == "__main__":
    main()
