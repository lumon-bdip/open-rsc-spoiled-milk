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
    594: 120000,   # dragon hatchet
    1278: 96000,   # dragon square shield
    1346: 360000,  # dragon 2h sword
    1425: 140000,  # dragon helmet
    1426: 192000,  # dragon paladin shield
    1427: 320000,  # dragon plate body
    1428: 320000,  # dragon plate top/body alias
    1429: 192000,  # dragon plate legs
    1430: 192000,  # dragon scale mail legs
    1447: 140000,  # dragon dagger
    1448: 300000,  # poisoned dragon dagger
    1480: 120000,  # dragon woodcutting hatchet
    2752: 280000,  # dragon battleaxe
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
JEWELRY_MATERIAL_VALUES = {
    160: 350,    # uncut sapphire
    159: 900,    # uncut emerald
    158: 2200,   # uncut ruby
    157: 5500,   # uncut diamond
    542: 18000,  # uncut dragonstone
    164: 600,    # sapphire
    163: 1400,   # emerald
    162: 3500,   # ruby
    161: 8000,   # diamond
    523: 24000,  # dragonstone
    152: 300,    # gold ore
    172: 600,    # gold bar
}
RING_AND_AMULET_VALUES = {
    1: 900,
    2: 1800,
    3: 3000,
    4: 6000,
    5: 12000,
    6: 35000,
}
NECKLACE_VALUES = {
    1: 1100,
    2: 2000,
    3: 3300,
    4: 6400,
    5: 13000,
    6: 38000,
}
GOD_SYMBOL_VALUES = {
    44: 200,    # unstrung Saradomin symbol
    45: 200,    # unblessed Saradomin symbol
    385: 300,   # blessed Saradomin symbol
    1027: 200,  # unstrung Zamorak symbol
    1028: 200,  # unblessed Zamorak symbol
    1029: 300,  # blessed Zamorak symbol
    3173: 200,  # unstrung Guthix symbol
    3174: 200,  # unblessed Guthix symbol
    3175: 300,  # blessed Guthix symbol
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


def weapon_value(tier: int, bars: int) -> int:
    return int(round(METAL_BODY_VALUES[tier] * (0.20 * bars)))


def ranged_piece_value(tier: int) -> int:
    bar_price = math.ceil(METAL_BODY_VALUES[tier] * 0.15)
    return max(1, math.floor((bar_price / 5) / 0.75))


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

    one_bar_weapons = {
        1995: 1, 62: 3, 28: 4, 63: 5, 2017: 7, 65: 8, 2028: 9, 396: 10,
        1997: 1, 66: 3, 1: 4, 67: 5, 2019: 7, 69: 8, 2030: 9, 397: 10,
        2001: 1, 87: 3, 12: 4, 88: 5, 2023: 7, 204: 8, 2034: 9, 405: 10,
    }
    for item_id, tier in one_bar_weapons.items():
        require_price(items, item_id, weapon_value(tier, 1), f"tier {tier} one-bar weapon")

    two_bar_weapons = {
        1998: 1, 70: 3, 71: 4, 72: 5, 2020: 7, 74: 8, 2031: 9, 75: 10,
        1999: 1, 82: 3, 83: 4, 84: 5, 2021: 7, 86: 8, 2032: 9, 398: 10,
    }
    for item_id, tier in two_bar_weapons.items():
        require_price(items, item_id, weapon_value(tier, 2), f"tier {tier} two-bar weapon")

    three_bar_weapons = {
        2000: 1, 76: 3, 77: 4, 78: 5, 2022: 7, 80: 8, 2033: 9, 81: 10,
        2002: 1, 205: 3, 89: 4, 90: 5, 2024: 7, 92: 8, 2035: 9, 93: 10,
    }
    for item_id, tier in three_bar_weapons.items():
        require_price(items, item_id, weapon_value(tier, 3), f"tier {tier} three-bar weapon")

    ranged_casting_outputs = {
        2004: 1, 669: 3, 670: 4, 671: 5, 2026: 7, 673: 8, 2037: 9, 674: 10,
        190: 1, 2180: 3, 2182: 4, 2184: 5, 2188: 7, 2190: 8, 2192: 9, 2194: 10,
        2005: 1, 1062: 3, 1063: 4, 1064: 5, 2027: 7, 1066: 8, 2038: 9, 1067: 10,
        1996: 1, 1076: 3, 1075: 4, 1077: 5, 2018: 7, 1079: 8, 2029: 9, 1080: 10,
    }
    for item_id, tier in ranged_casting_outputs.items():
        piece_price = ranged_piece_value(tier)
        require_price(items, item_id, piece_price, f"tier {tier} ranged casting output")
        require(
            piece_price * 5 > high_alch_value(piece_price) * 5,
            f"tier {tier} ranged output should keep a positive shop value",
        )
        require(
            math.ceil(METAL_BODY_VALUES[tier] * 0.15) > high_alch_value(piece_price) * 5,
            f"tier {tier} bought bar should cost more than ranged output high alch",
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

    for item_id, expected in JEWELRY_MATERIAL_VALUES.items():
        require_price(items, item_id, expected, f"jewelry material {item_id}")

    rings = {283: 1, 284: 2, 285: 3, 286: 4, 287: 5, 543: 6}
    unstrung_amulets = {296: 1, 297: 2, 298: 3, 299: 4, 300: 5, 524: 6}
    strung_amulets = {301: 1, 302: 2, 303: 3, 304: 4, 305: 5, 522: 6}
    necklaces = {288: 1, 289: 2, 290: 3, 291: 4, 292: 5, 544: 6}
    for item_id, tier in {**rings, **unstrung_amulets, **strung_amulets}.items():
        require_price(items, item_id, RING_AND_AMULET_VALUES[tier], f"tier {tier} ring/amulet jewelry")
    for item_id, tier in necklaces.items():
        require_price(items, item_id, NECKLACE_VALUES[tier], f"tier {tier} necklace jewelry")

    for item_id, expected in GOD_SYMBOL_VALUES.items():
        require_price(items, item_id, expected, f"god symbol {item_id}")

    cut_gem_ids = {2: 164, 3: 163, 4: 162, 5: 161, 6: 523}
    for tier in range(2, 7):
        material_cost = items[172]["basePrice"] + items[cut_gem_ids[tier]]["basePrice"]
        require(
            material_cost >= high_alch_value(RING_AND_AMULET_VALUES[tier]),
            f"tier {tier} jewelry materials should cover ring/amulet high alch",
        )
        require(
            material_cost >= high_alch_value(NECKLACE_VALUES[tier]),
            f"tier {tier} jewelry materials should cover necklace high alch",
        )

    print("PASS: equipment economy curve and alchemy resource guardrails validated")


if __name__ == "__main__":
    main()
