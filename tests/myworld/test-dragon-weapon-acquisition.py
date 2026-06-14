#!/usr/bin/env python3
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
JAKUT_PATH = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/lostcity/Jakut.java"
FIONELLA_PATH = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/members/legendsquest/npcs/shop/Fionella.java"
RANGERS_GUILD_DRAGON_SHOP_PATH = ROOT / "server/plugins/com/openrsc/server/plugins/custom/npcs/RangersGuildDragonShop.java"
INV_ITEM_POISONING_PATH = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/itemactions/InvItemPoisoning.java"
ITEM_DEFS_PATH = ROOT / "server/conf/server/defs/ItemDefsCustom.json"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, needle: str, message: str) -> None:
    if needle not in text:
        fail(message)


def forbid(text: str, needle: str, message: str) -> None:
    if needle in text:
        fail(message)


def main() -> None:
    jakut = JAKUT_PATH.read_text(encoding="utf-8")
    fionella = FIONELLA_PATH.read_text(encoding="utf-8")
    rangers_guild_dragon_shop = RANGERS_GUILD_DRAGON_SHOP_PATH.read_text(encoding="utf-8")
    poisoning = INV_ITEM_POISONING_PATH.read_text(encoding="utf-8")
    item_defs = ITEM_DEFS_PATH.read_text(encoding="utf-8")

    require(jakut, "new Item(ItemId.DRAGON_SWORD.id(), 2)", "Jakut should keep selling dragon swords")
    require(jakut, "new Item(ItemId.DRAGON_DAGGER.id(), 2)", "Jakut should sell regular dragon daggers")
    forbid(jakut, "POISONED_DRAGON_DAGGER", "Poisoned dragon daggers should not be stocked in Lost City")

    require(fionella, "new Item(ItemId.DRAGON_2_HANDED_SWORD.id(), 1)", "Fionella should sell the dragon 2-hander in Legends Guild")
    forbid(fionella, "DRAGON_CROSSBOW", "Dragon ranged weapons should be sold in the Rangers Guild, not Legends Guild")
    forbid(fionella, "DRAGON_LONGBOW", "Dragon ranged weapons should be sold in the Rangers Guild, not Legends Guild")
    forbid(fionella, "DRAGON_ARROWS", "Dragon ranged ammo should be sold in the Rangers Guild, not Legends Guild")
    forbid(fionella, "DRAGON_BOLTS", "Dragon ranged ammo should be sold in the Rangers Guild, not Legends Guild")

    require(rangers_guild_dragon_shop, "new Item(ItemId.DRAGON_LONGBOW.id(), 1)", "Aeron should sell dragon longbows")
    require(rangers_guild_dragon_shop, "new Item(ItemId.DRAGON_CROSSBOW.id(), 1)", "Aeron should sell dragon crossbows")
    require(rangers_guild_dragon_shop, "new Item(ItemId.DRAGON_ARROWS.id(), 1000)", "Aeron should sell dragon arrows")
    require(rangers_guild_dragon_shop, "new Item(ItemId.POISON_DRAGON_ARROWS.id(), 1000)", "Aeron should sell poison dragon arrows")
    require(rangers_guild_dragon_shop, "new Item(ItemId.DRAGON_BOLTS.id(), 1000)", "Aeron should sell dragon bolts")
    require(rangers_guild_dragon_shop, "new Item(ItemId.POISON_DRAGON_BOLTS.id(), 1000)", "Aeron should sell poison dragon bolts")

    require(poisoning, 'String poisonedVersion = "Poisoned " + name;', "Weapon poison should use normal poisoned-name lookup")
    require(item_defs, '"name": "Dragon dagger"', "Regular dragon dagger item definition should exist")
    require(item_defs, '"name": "Poisoned dragon dagger"', "Poisoned dragon dagger item definition should exist for normal weapon poison")

    print("PASS: dragon weapon acquisition paths validated")


if __name__ == "__main__":
    main()
