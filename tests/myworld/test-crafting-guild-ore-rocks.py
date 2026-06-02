#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
BASE_SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"

GOLD_ROCK = 112
SILVER_ROCK = 195
DEPLETED_SILVER_ROCK = 196
GEM_ROCK = 588

GOLD_COORDS = {
    (337, 614),
    (339, 614),
    (340, 614),
    (338, 611),
    (337, 610),
}

SILVER_COORDS = {
    (337, 601),
    (337, 602),
    (338, 604),
    (339, 604),
}

GEM_COORDS = {
    (342, 608),
    (342, 607),
    (342, 606),
    (342, 605),
    (342, 603),
    (342, 602),
    (341, 601),
}

REMOVED_SILVER_COORDS = {
    (341, 602),
}

REMOVED_GEM_COORDS = {
    (342, 601),
    (342, 600),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    sceneries = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    all_sceneries = sceneries + json.loads(BASE_SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    by_id_and_pos = {
        (loc.get("id"), loc.get("pos", {}).get("X"), loc.get("pos", {}).get("Y")): loc
        for loc in sceneries
    }
    all_by_id_and_pos = {
        (loc.get("id"), loc.get("pos", {}).get("X"), loc.get("pos", {}).get("Y")): loc
        for loc in all_sceneries
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

    for x, y in GEM_COORDS:
        loc = by_id_and_pos.get((GEM_ROCK, x, y))
        if loc is None:
            fail(f"Missing gem rock at {x},{y}")
        if loc.get("direction") != 0:
            fail(f"Gem rock at {x},{y} should use direction 0")

    for x, y in REMOVED_SILVER_COORDS:
        for rock_id in (SILVER_ROCK, DEPLETED_SILVER_ROCK):
            if all_by_id_and_pos.get((rock_id, x, y)) is not None:
                fail(f"Unexpected silver rock id {rock_id} at {x},{y}")

    for x, y in REMOVED_GEM_COORDS:
        if all_by_id_and_pos.get((GEM_ROCK, x, y)) is not None:
            fail(f"Unexpected gem rock at {x},{y}")

    print("PASS: crafting guild gold, silver, and gem rock placements look correct")


if __name__ == "__main__":
    main()
