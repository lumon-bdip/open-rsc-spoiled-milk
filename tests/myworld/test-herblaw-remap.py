#!/usr/bin/env python3
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
HERB_SECOND = ROOT / "server" / "conf" / "server" / "defs" / "extras" / "ItemHerbSecond.xml"
ITEM_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefs.json"
ITEM_DEFS_CUSTOM = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsCustom.json"
HERBLAW = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "skills" / "herblaw" / "Herblaw.java"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def load_item_names(path: Path) -> dict[int, tuple[str, str, str]]:
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    if isinstance(data, dict):
        if "items" in data:
            items = data["items"]
        elif "item" in data:
            items = data["item"]
        else:
            fail(f"Unexpected item-def container shape in {path}")
    else:
        items = data
    return {item["id"]: (item["name"], item["description"], item["command"]) for item in items}


def main() -> None:
    xml_root = ET.parse(HERB_SECOND).getroot()
    recipes = {
        (int(node.findtext("unfinishedID")), int(node.findtext("secondID"))): int(node.findtext("potionID"))
        for node in xml_root.findall("ItemHerbSecond")
    }

    expected_recipes = {
        (454, 220): 474,
        (456, 220): 477,
        (458, 220): 480,
        (460, 220): 483,
        (462, 220): 486,
        (935, 220): 3198,
        (454, 1410): 489,
        (456, 1410): 492,
        (458, 1410): 495,
        (460, 1410): 498,
        (462, 1410): 566,
        (935, 1410): 3201,
        (454, 270): 569,
        (456, 270): 963,
        (458, 270): 1411,
        (460, 270): 1414,
        (462, 270): 1468,
        (935, 270): 3204,
        (457, 473): 1471,
        (455, 219): 1474,
        (457, 472): 572,
        (459, 469): 1477,
        (459, 471): 3192,
    }
    for pair, potion_id in expected_recipes.items():
        if recipes.get(pair) != potion_id:
            fail(f"Expected herb recipe {pair} -> {potion_id}")

    item_defs = load_item_names(ITEM_DEFS)
    custom_defs = load_item_names(ITEM_DEFS_CUSTOM)

    expected_names = {
        221: "Strong Skiller's Brew",
        474: "Potion of Brawn v1",
        477: "Potion of Brawn v2",
        480: "Potion of Brawn v3",
        483: "Potion of Brawn v4",
        486: "Potion of Brawn v5",
        3198: "Potion of Brawn v6",
        489: "Potion of Deftness v1",
        492: "Potion of Deftness v2",
        495: "Potion of Deftness v3",
        498: "Potion of Deftness v4",
        566: "Potion of Deftness v5",
        3201: "Potion of Deftness v6",
        569: "Potion of Insight v1",
        963: "Potion of Insight v2",
        1411: "Potion of Insight v3",
        1414: "Potion of Insight v4",
        1468: "Potion of Insight v5",
        3204: "Potion of Insight v6",
        1471: "Stat restore",
        1474: "Antidote",
        1477: "Skiller's Brew",
        3192: "Warrior's Brew",
        3195: "Strong Warrior's Brew",
    }
    for item_id, expected_name in expected_names.items():
        defs = custom_defs if item_id >= 1400 else item_defs
        actual = defs.get(item_id)
        if actual is None or actual[0] != expected_name:
            fail(f"Expected item {item_id} to be named '{expected_name}'")

    herblaw_text = HERBLAW.read_text(encoding="utf-8")
    for snippet in (
        "secondaryId == ItemId.SNAPE_GRASS.id()",
        "secondaryId == ItemId.WHITE_BERRIES.id()",
        "return secondaryId == ItemId.FISH_OIL.id() ? 10 : 1;",
        "requiredCount = 5",
        "resultId = ItemId.FULL_STRENGTH_POTION.id()",
        "resultId = ItemId.FULL_STRONG_WARRIORS_BREW.id()",
    ):
        if snippet not in herblaw_text:
            fail(f"Herblaw strong brew recipe missing: {snippet}")

    print("PASS: herblaw recipe and item remap validated")


if __name__ == "__main__":
    main()
