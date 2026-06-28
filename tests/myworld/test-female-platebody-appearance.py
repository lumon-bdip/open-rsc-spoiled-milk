#!/usr/bin/env python3
"""Ensure unified platebody items use female top visuals on female bodies."""

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
EQUIPMENT = ROOT / "server/src/com/openrsc/server/model/container/Equipment.java"
ITEM_DEFS = ROOT / "server/conf/server/defs/ItemDefsCustom.json"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def method_body(source: str, signature: str) -> str:
    start = source.find(signature)
    if start == -1:
        fail(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    if brace == -1:
        fail(f"Missing method body for: {signature}")
    depth = 0
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    fail(f"Unclosed method body for: {signature}")


def require(source: str, snippet: str, message: str) -> None:
    if snippet not in source:
        fail(message)


def require_mapping(body: str, male_appearance: int, female_appearance: int, label: str) -> None:
    require(body, f"case {male_appearance}:", f"{label} body appearance should be remapped for female bodies")
    require(body, f"return {female_appearance};", f"{label} should resolve to the matching female top appearance")


def item_by_id(items: list[dict], item_id: int) -> dict:
    for item in items:
        if item.get("id") == item_id:
            return item
    fail(f"Missing item definition {item_id}")


def require_item_appearance(items: list[dict], item_id: int, appearance_id: int, label: str) -> None:
    item = item_by_id(items, item_id)
    actual = item.get("appearanceID")
    if actual != appearance_id:
        fail(f"{label} should use appearance {appearance_id}, found {actual}")


def main() -> int:
    player = PLAYER.read_text(encoding="utf-8")
    equipment = EQUIPMENT.read_text(encoding="utf-8")
    items = json.loads(ITEM_DEFS.read_text(encoding="utf-8"))["items"]

    resolver = method_body(player, "private int resolveBodyAppearance")
    require(resolver, "indexPosition != AppearanceId.SLOT_BODY", "Female plate remapping should only affect body-slot appearances")
    require(resolver, "usesFemaleBodySprite()", "Female plate remapping should only run for female body sprites")

    legacy_plate_mappings = [
        (28, 55, "bronze plate"),
        (29, 56, "iron plate"),
        (30, 57, "steel plate"),
        (31, 58, "mithril plate"),
        (32, 59, "adamantite plate"),
        (33, 61, "black plate"),
        (34, 60, "rune plate"),
        (35, 157, "white plate"),
    ]
    for male_appearance, female_appearance, label in legacy_plate_mappings:
        require_mapping(resolver, male_appearance, female_appearance, label)

    modern_plate_mappings = [
        (296, 299, "dragon plate"),
        (319, 535, "ironman plate"),
        (322, 536, "ultimate ironman plate"),
        (325, 537, "hardcore ironman plate"),
    ]
    for male_appearance, female_appearance, label in modern_plate_mappings:
        require_mapping(resolver, male_appearance, female_appearance, label)

    require(equipment, "ItemId.DRAGON_PLATE_MAIL_BODY.id()", "Dragon platebody should stay in the body normalization bucket")
    require(equipment, "ItemId.DRAGON_PLATE_MAIL_TOP.id()", "Dragon plate top should normalize into the body item")
    require(equipment, "replaceRequestedItem(request, plateBodyIds[i])", "Plate tops should be replaced by their unified body item")

    require_item_appearance(items, 1427, 296, "Dragon plate mail body")
    require_item_appearance(items, 1428, 299, "Dragon plate mail top compatibility item")
    require_item_appearance(items, 1291, 319, "Ironman platebody")
    require_item_appearance(items, 1294, 322, "Ultimate ironman platebody")
    require_item_appearance(items, 1297, 325, "Hardcore ironman platebody")
    require_item_appearance(items, 1554, 535, "Ironman plate top compatibility item")
    require_item_appearance(items, 1555, 536, "Ultimate ironman plate top compatibility item")
    require_item_appearance(items, 1556, 537, "Hardcore ironman plate top compatibility item")

    print("PASS: unified platebodies resolve to female top visuals for female bodies")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
