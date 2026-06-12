#!/usr/bin/env python3
import json
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_DEFS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
SERVER_DEFS = ROOT / "server/conf/server/defs/GameObjectDef.xml"
SCENERY_IDS = ROOT / "server/src/com/openrsc/server/constants/SceneryId.java"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
MONK = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/portsarim/MonkOfEntrana.java"
RESTRICTIONS = ROOT / "server/plugins/com/openrsc/server/plugins/shared/EntranaRestrictions.java"
BOX = ROOT / "server/plugins/com/openrsc/server/plugins/custom/misc/EntranaSafetyDepositBox.java"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    scenery_ids = SCENERY_IDS.read_text(encoding="utf-8")
    require("COSMIC_SPARKLES(1325)" in scenery_ids, "SceneryId should expose existing Cosmic sparkles id 1325")
    require("ENTRANA_SAFETY_DEPOSIT_BOX(1326)" in scenery_ids, "SceneryId should expose safety deposit box id 1326")

    definitions = ET.parse(SERVER_DEFS).getroot()
    box_def = definitions[1326]
    require(box_def.findtext("name") == "Safety Deposit Box", "Server object 1326 should be the safety deposit box")
    require(box_def.findtext("command1") == "Deposit", "Safety box primary action should be Deposit")
    require(box_def.findtext("command2") == "Withdraw", "Safety box secondary action should be Withdraw")
    require(box_def.findtext("objectModel") == "ChestClosed", "Safety box should use the closed chest model")

    client_defs = CLIENT_DEFS.read_text(encoding="utf-8")
    require(
        'new GameObjectDef("Safety Deposit Box", "A secure box for items barred from Entrana", "Deposit", "Withdraw", 1, 1, 1, 0, "ChestClosed", ++i)); //1326'
        in client_defs,
        "Client should define the safety box with Deposit and Withdraw actions",
    )

    locs = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    require(
        {"id": 1326, "pos": {"X": 266, "Y": 660}, "direction": 0} in locs,
        "Safety box should be placed beside the Entrana ship at 266,660",
    )

    restrictions = RESTRICTIONS.read_text(encoding="utf-8")
    require("public static boolean itemIsBlocked(Player player, Item item)" in restrictions, "Restrictions should expose reusable item check")
    require("public static boolean playerNotAllowedOnEntrana(Player player)" in restrictions, "Restrictions should expose reusable player check")
    require("Equipment.EquipmentSlot.SLOT_NECK" in restrictions, "Restrictions should preserve cape and neck exception")
    require("Skill.HARVESTING.id()" in restrictions, "Restrictions should keep custom gathering tools barred")
    for item in (
        "SCYTHE",
        "TIN_SCYTHE",
        "COPPER_SCYTHE",
        "BRONZE_SCYTHE",
        "IRON_SCYTHE",
        "STEEL_SCYTHE",
        "MITHRIL_SCYTHE",
        "TITAN_STEEL_SCYTHE",
        "ADAMANTITE_SCYTHE",
        "ORICHALCUM_SCYTHE",
        "RUNE_SCYTHE",
        "TIN_SHURIKEN",
        "COPPER_SHURIKEN",
        "BRONZE_SHURIKEN",
        "IRON_SHURIKEN",
        "STEEL_SHURIKEN",
        "MITHRIL_SHURIKEN",
        "TITAN_STEEL_SHURIKEN",
        "ADAMANTITE_SHURIKEN",
        "ORICHALCUM_SHURIKEN",
        "RUNE_SHURIKEN",
        "POISONED_TIN_SHURIKEN",
        "POISONED_COPPER_SHURIKEN",
        "POISONED_BRONZE_SHURIKEN",
        "POISONED_IRON_SHURIKEN",
        "POISONED_STEEL_SHURIKEN",
        "POISONED_MITHRIL_SHURIKEN",
        "POISONED_TITAN_STEEL_SHURIKEN",
        "POISONED_ADAMANTITE_SHURIKEN",
        "POISONED_ORICHALCUM_SHURIKEN",
        "POISONED_RUNE_SHURIKEN",
    ):
        require(f"ItemId.{item}.id()" in restrictions, f"Entrana restrictions should block {item}")

    monk = MONK.read_text(encoding="utf-8")
    require("EntranaRestrictions.playerNotAllowedOnEntrana(player)" in monk, "Monk should use shared Entrana restrictions")
    require("private boolean itemIsBlocked" not in monk, "Monk should not keep a duplicate item restriction method")
    require("final private int[] blockedItems" not in monk, "Monk should not keep duplicate blocked item arrays")

    box = BOX.read_text(encoding="utf-8")
    for needle, description in (
        ("EntranaRestrictions.itemIsBlocked(player, item)", "shared restriction checks"),
        ('CACHE_KEY = "entrana_safety_deposit_box"', "per-player cache snapshot"),
        ('player.message("The box already contains items.")', "second deposit refusal"),
        ('player.message("There is nothing in the box.")', "empty withdraw refusal"),
        ("inventory.getRequiredSlots(inventoryRestores) > inventory.getFreeSlots()", "withdraw room precheck"),
        ("Equipment.SLOT_COUNT", "equipment slot scan"),
        ("ActionSender.sendEquipmentStats(player)", "equipment refresh after restore"),
        ("obj.getID() == SceneryId.ENTRANA_SAFETY_DEPOSIT_BOX.id()", "object id guard"),
        ("obj.getX() == 266", "x coordinate guard"),
        ("obj.getY() == 660", "y coordinate guard"),
    ):
        require(needle in box, f"Safety box should include {description}")

    print("PASS: Entrana safety deposit box wiring and snapshot safeguards validated")


if __name__ == "__main__":
    main()
