#!/usr/bin/env python3
"""Validate the custom bank filter panel and its item-tag classifier."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[2]
BANK = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/CustomBankInterface.java"
TAGS = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/BankItemTag.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def forbid(text: str, snippet: str, label: str) -> None:
    if snippet in text:
        fail(f"{label} contains excluded feature: {snippet}")


def main() -> None:
    bank = BANK.read_text(encoding="utf-8")
    tags = TAGS.read_text(encoding="utf-8")

    expected_tags = (
        'MINING("Mining", Group.SKILLS)',
        'SMITHING("Smithing", Group.SKILLS)',
        'CRAFTING("Crafting", Group.SKILLS)',
        'ENCHANTING("Enchanting", Group.SKILLS)',
        'PRAYER("Prayer", Group.SKILLS)',
        'WOODCUTTING("Woodcutting", Group.SKILLS)',
        'FLETCHING("Fletching", Group.SKILLS)',
        'FISHING("Fishing", Group.SKILLS)',
        'FOOD("Food", Group.ITEM_TYPES)',
        'POTIONS("Potions", Group.ITEM_TYPES)',
        'POTION_INGREDIENTS("Potion ingredients", Group.ITEM_TYPES)',
        'COOKING_INGREDIENTS("Cooking ingredients", Group.ITEM_TYPES)',
        'ARMOUR("Armour", Group.ITEM_TYPES)',
        'MAGIC("Magic & Summoning", Group.ITEM_TYPES)',
        'MELEE("Melee", Group.ITEM_TYPES)',
        'RANGED("Ranged", Group.ITEM_TYPES)',
        'JEWELRY("Jewelry", Group.ITEM_TYPES)',
        'QUEST_ITEMS("Quest items", Group.ITEM_TYPES)',
    )
    for tag in expected_tags:
        require(tags, tag, "BankItemTag")

    for excluded in (
        "FIREMAKING",
        "SEEDS",
        "HARVESTING",
        "CURRENCY",
        "VALUABLES",
        "STACKABLE",
        "NOTED",
        "EQUIPPABLE",
    ):
        forbid(tags, excluded, "BankItemTag")

    for snippet in (
        'drawString("Filters"',
        'drawFilterModeButton("AND"',
        'drawFilterModeButton("OR"',
        'drawString("Search"',
        "private ArrayList<BankItem> filteredBankItems()",
        "matches = (!hasSearch || searchMatch) && (!hasTags || tagMatch);",
        "matches = (hasSearch && searchMatch) || (hasTags && tagMatch);",
        "selectedBankSlot = bankItem.bankID;",
        "if (filtersActive())",
        "filterPanelDocked",
        "filterDrawerOpen",
    ):
        require(bank, snippet, "CustomBankInterface")

    forbid(bank, 'drawString("Search for item:"', "legacy bank search")
    require(tags, 'endsWithAny(name, "-rune", " rune", " runes")', "rune item classification")
    require(tags, "if (def.untradeable", "quest item classification")
    require(tags, 'boolean ashes = equalsAny(name, "ashes");', "summoning ashes classification")
    require(tags, "boolean demonAshes = containsAny(name, \"demon ash\");", "demon ash prayer classification")
    require(tags, "|| bones || ashes || demonAshes", "summoning reagent magic classification")
    require(tags, "boolean prayerEquipment = def.isWieldable() && isPrayerEquipment", "prayer equipment classification")
    require(tags, 'startsWithAny(name, "ring ", "ring-")', "whole-word jewelry ring classification")
    require(tags, "boolean craftingGem = gem && !jewelry;", "finished gem jewelry exclusion")
    require(tags, "isCraftingToolOrMaterial(name)", "crafting input classification")
    require(tags, "boolean bow = isBowWeapon(name);", "whole-word bow classification")
    require(tags, 'endsWithAny(name, " longbow", " shortbow", " crossbow"', "bow suffix classification")
    forbid(tags, 'boolean bonesOrAshes = containsAny(name, "bone", "ashes", "demon ash");', "generic ashes classification")
    forbid(tags, "uncutGem || gem || jewelry ||", "finished jewelry crafting classification")
    forbid(tags, 'boolean bow = containsAny(name, "bow", "crossbow");', "bowl-safe ranged classification")

    print("PASS: bank filters expose the agreed tags and preserve bank-slot-safe filtering")


if __name__ == "__main__":
    main()
