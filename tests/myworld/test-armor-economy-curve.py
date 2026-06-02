#!/usr/bin/env python3
"""Validate MyWorld armor price curves and alchemy-safe resource costs."""

import json
import math
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ITEMS_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsMyWorld.json"

METAL_BODY_VALUES = {
    1: 120,
    2: 240,
    3: 500,
    4: 1200,
    5: 3000,
    6: 8000,
    7: 18000,
    8: 38000,
    9: 65000,
    10: 100000,
    11: 160000,
}
TIER_11_VALUES = {
    593: 120000,   # dragon sword
    594: 60000,    # dragon hatchet
    1278: 96000,   # dragon square shield
    1346: 180000,  # dragon 2h sword
    1425: 70000,   # large dragon helmet
    1426: 96000,   # dragon paladin shield
    1427: 160000,  # dragon plate body
    1428: 160000,  # dragon plate top/body alias
    1429: 96000,   # dragon plate legs
    1430: 96000,   # dragon plate skirt/legs alias
    1447: 70000,   # dragon dagger
    1448: 80000,   # poisoned dragon dagger
    1480: 60000,   # dragon woodcutting hatchet
    2752: 140000,  # dragon battleaxe
}
LEATHER_TOP_VALUES = {
    1: 90,
    2: 180,
    3: 400,
    4: 900,
    5: 2000,
    6: 5000,
    7: 12000,
    8: 25000,
    9: 45000,
    10: 75000,
}
LEATHER_MATERIAL_VALUES = {
    1: 25,
    2: 50,
    3: 110,
    4: 250,
    5: 550,
    6: 1400,
    7: 3000,
    8: 6500,
    9: 12000,
    10: 20000,
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def load_items() -> dict[int, dict]:
    payload = json.loads(ITEMS_PATH.read_text(encoding="utf-8"))
    return {entry["id"]: entry for entry in payload["items"]}


def high_alch_value(base_price: int) -> int:
    return math.floor(base_price * 0.6)


def wool_robe_rune_cost(tier: int) -> int:
    return tier * tier * 50


def require_price(items: dict[int, dict], item_id: int, expected: int, label: str) -> None:
    entry = items.get(item_id)
    require(entry is not None, f"Missing price override for {label} ({item_id})")
    actual = entry.get("basePrice")
    require(
        actual == expected,
        f"{label} ({item_id}) expected basePrice {expected}, found {actual}",
    )


def main() -> None:
    items = load_items()

    plate_bodies = {
        1964: 1,
        1970: 2,
        117: 3,
        8: 4,
        118: 5,
        119: 6,
        1976: 7,
        120: 8,
        1982: 9,
        401: 10,
    }
    for item_id, tier in plate_bodies.items():
        require_price(items, item_id, METAL_BODY_VALUES[tier], f"tier {tier} plate body")

    for item_id, expected in TIER_11_VALUES.items():
        require_price(items, item_id, expected, f"tier 11 item")

    require(
        items[1427]["basePrice"] > items[407]["basePrice"],
        "dragon plate body should sit above rune plate body value",
    )

    metal_bar_ids = {
        1955: 1,
        1956: 2,
        169: 3,
        170: 4,
        171: 5,
        173: 6,
        1957: 7,
        174: 8,
        1958: 9,
        408: 10,
    }
    for bar_id, tier in metal_bar_ids.items():
        bar_price = math.ceil(METAL_BODY_VALUES[tier] * 0.15)
        require_price(items, bar_id, bar_price, f"tier {tier} armor bar")
        require(
            bar_price * 5 > high_alch_value(METAL_BODY_VALUES[tier]),
            f"tier {tier} bought bars should cost more than plate body high alch",
        )

    leather_cuirasses = {
        1839: 1,
        1844: 1,
        1849: 2,
        1854: 2,
        1859: 2,
        1864: 2,
        1869: 3,
        1874: 3,
        1879: 3,
        1884: 4,
        1889: 4,
        1894: 5,
        1899: 5,
        1904: 5,
        1909: 6,
        1914: 7,
        1919: 7,
        1924: 7,
        1929: 7,
        1934: 8,
        1939: 8,
        1944: 9,
        1949: 9,
        1954: 10,
    }
    for item_id, tier in leather_cuirasses.items():
        require_price(items, item_id, LEATHER_TOP_VALUES[tier], f"tier {tier} leather cuirass")

    leather_material_ids = {
        148: 1,
        1790: 1,
        1792: 2,
        1794: 2,
        1796: 2,
        1806: 2,
        1798: 3,
        1802: 3,
        1808: 3,
        1816: 4,
        1818: 4,
        1804: 5,
        1810: 5,
        1812: 5,
        1820: 6,
        1800: 7,
        1814: 7,
        1822: 7,
        1824: 7,
        1826: 8,
        1828: 8,
        1830: 9,
        1832: 9,
        1834: 10,
    }
    for material_id, tier in leather_material_ids.items():
        material_price = LEATHER_MATERIAL_VALUES[tier]
        require_price(items, material_id, material_price, f"tier {tier} leather material")
        require(
            material_price * 4 > high_alch_value(LEATHER_TOP_VALUES[tier]),
            f"tier {tier} leather material should cost more than cuirass high alch",
        )

    robe_samples = {
        2085: 90,     # Beginner's Air Robe Top
        2448: 180,    # Novice Air Robe Top
        2555: 75000,  # Mythic Air Robe Top
        2565: 135,    # Novice Air Robe Skirt
        2672: 56250,  # Mythic Air Robe Skirt
        2796: 45,     # Beginner's Air Wizard Gloves
        2805: 37500,  # Mythic Air Wizard Gloves
        2936: 45,     # Beginner's Air Wizard Boots
        2945: 37500,  # Mythic Air Wizard Boots
    }
    for item_id, expected in robe_samples.items():
        require_price(items, item_id, expected, f"robe item {item_id}")

    require_price(items, 207, 15, "ball of wool")
    require(
        items[207]["basePrice"] * 4 > high_alch_value(LEATHER_TOP_VALUES[1]),
        "bought balls of wool should cost more than wool robe top high alch",
    )

    cheapest_altar_rune_price = 4
    for tier in range(2, 11):
        added_top_alch = high_alch_value(LEATHER_TOP_VALUES[tier]) - high_alch_value(LEATHER_TOP_VALUES[tier - 1])
        rune_input_value = cheapest_altar_rune_price * wool_robe_rune_cost(tier)
        require(
            rune_input_value > added_top_alch,
            f"tier {tier} robe upgrade rune cost should exceed added top high alch value",
        )

    print("PASS: armor economy curve and alchemy resource guardrails validated")


if __name__ == "__main__":
    main()
