#!/usr/bin/env python3

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
NPC_DROPS = ROOT / "server/src/com/openrsc/server/constants/NpcDrops.java"


WHITE_EQUIPMENT = {
    "WHITE_DAGGER",
    "WHITE_SHORT_SWORD",
    "WHITE_LONG_SWORD",
    "WHITE_2_HANDED_SWORD",
    "WHITE_SCIMITAR",
    "WHITE_BATTLE_AXE",
    "WHITE_MACE",
    "LARGE_WHITE_HELMET",
    "WHITE_KITE_SHIELD",
    "WHITE_PLATE_MAIL_BODY",
    "WHITE_PLATE_MAIL_LEGS",
    "WHITE_GAUNTLETS",
    "WHITE_GREAVES",
}

BLACK_EQUIPMENT = {
    "BLACK_DAGGER",
    "BLACK_SHORT_SWORD",
    "BLACK_LONG_SWORD",
    "BLACK_MACE",
    "LARGE_BLACK_HELMET",
    "BLACK_KITE_SHIELD",
    "BLACK_PLATE_MAIL_BODY",
    "BLACK_PLATE_MAIL_LEGS",
}


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def table_section(text: str, name: str) -> str:
    start = text.find(f'new DropTable("{name}")')
    if start < 0:
        fail(f"Missing drop table: {name}")
    end = text.find("addEmptyDrop", start)
    if end < 0:
        fail(f"Missing drop table budget terminator: {name}")
    return text[start:end]


def item_weights(section: str) -> dict[str, int]:
    weights: dict[str, int] = {}
    for item, weight in re.findall(
        r"addItemDrop\(ItemId\.([A-Z0-9_]+)\.id\(\),\s*\d+,\s*(\d+)\);",
        section,
    ):
        weights[item] = weights.get(item, 0) + int(weight)
    return weights


def total_weight(section: str) -> int:
    return sum(
        int(weight)
        for weight in re.findall(r"add(?:Item|Table)Drop\([^;]*,\s*(\d+)\);", section)
    )


def equipment_weight(weights: dict[str, int], equipment: set[str]) -> int:
    return sum(weight for item, weight in weights.items() if item in equipment)


def main() -> None:
    text = NPC_DROPS.read_text(encoding="utf-8")
    white_knight = table_section(text, "White Knight (102)")
    paladin = table_section(text, "Paladin (323)")
    dark_warrior = table_section(text, "Dark Warrior (199)")

    white_weights = item_weights(white_knight)
    paladin_weights = item_weights(paladin)
    dark_weights = item_weights(dark_warrior)

    white_knight_white_weight = equipment_weight(white_weights, WHITE_EQUIPMENT)
    paladin_white_weight = equipment_weight(paladin_weights, WHITE_EQUIPMENT)
    if paladin_white_weight <= white_knight_white_weight * 2:
        fail(
            f"Paladin white equipment weight {paladin_white_weight} should be more than "
            f"double White Knight weight {white_knight_white_weight}"
        )

    missing_white = sorted(WHITE_EQUIPMENT - paladin_weights.keys())
    if missing_white:
        fail(f"Paladin table is missing white equipment: {missing_white}")

    for retired in ("MITHRIL_SHORT_SWORD", "MITHRIL_LONG_SWORD", "LARGE_MITHRIL_HELMET"):
        if retired in paladin_weights:
            fail(f"Paladins should no longer drop old mithril equipment: {retired}")

    if dark_weights.get("LARGE_BLACK_HELMET") != 1:
        fail("Dark Warrior large black helmet drop should be reduced to weight 1")
    dark_black_weight = equipment_weight(dark_weights, BLACK_EQUIPMENT)
    if dark_black_weight != 8:
        fail(f"Dark Warrior black equipment weight should be 8, found {dark_black_weight}")

    for name, section in (
        ("White Knight (102)", white_knight),
        ("Paladin (323)", paladin),
        ("Dark Warrior (199)", dark_warrior),
    ):
        weight = total_weight(section)
        if weight > 128:
            fail(f"{name} exceeds 128 drop-weight budget: {weight}")

    print("PASS: paladin and dark warrior equipment drop balance validated")


if __name__ == "__main__":
    main()
