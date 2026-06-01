#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"

GOLD_ROCK = 112
SILVER_ROCK = 195

GOLD_COORDS = {
    (337, 614),
    (339, 614),
    (340, 614),
    (338, 611),
    (337, 610),
    (342, 608),
    (342, 607),
    (342, 606),
    (342, 605),
}

SILVER_COORDS = {
    (342, 604),
    (337, 601),
    (337, 602),
    (338, 604),
    (339, 604),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    sceneries = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    by_id_and_pos = {
        (loc.get("id"), loc.get("pos", {}).get("X"), loc.get("pos", {}).get("Y")): loc
        for loc in sceneries
    }

    for x, y in GOLD_COORDS:
        loc = by_id_and_pos.get((GOLD_ROCK, x, y))
        if loc is None:
            fail(f"Missing gold rock at {x},{y}")
        if loc.get("direction") != 0:
            fail(f"Gold rock at {x},{y} should use direction 0")

    for x, y in SILVER_COORDS:
        loc = by_id_and_pos.get((SILVER_ROCK, x, y))
        if loc is None:
            fail(f"Missing silver rock at {x},{y}")
        if loc.get("direction") != 0:
            fail(f"Silver rock at {x},{y} should use direction 0")

    print("PASS: crafting guild gold and silver rock placements look correct")


if __name__ == "__main__":
    main()
