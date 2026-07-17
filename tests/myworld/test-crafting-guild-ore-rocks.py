#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
SCENERY_REMOVALS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryRemovals.json"
BASE_SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"

GOLD_ROCK = 112
CLAY_ROCK = 114
SILVER_ROCK = 196
GEM_ROCK = 588

GOLD_COORDS = {
    (337, 604),
    (338, 604),
    (339, 604),
    (342, 604),
    (337, 605),
    (338, 605),
    (339, 605),
    (342, 605),
    (342, 606),
    (342, 607),
}

CLAY_COORDS = {
    (336, 607),
    (336, 608),
    (336, 609),
    (341, 609),
    (342, 609),
}

SILVER_COORDS = {
    (337, 599),
    (338, 599),
    (339, 599),
    (340, 599),
    (341, 600),
    (337, 601),
    (338, 601),
    (342, 601),
    (337, 602),
    (338, 602),
    (339, 602),
}

GEM_COORDS = {
    (338, 607),
    (339, 607),
    (340, 607),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    custom = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    base = json.loads(BASE_SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    removals = json.loads(SCENERY_REMOVALS.read_text(encoding="utf-8"))["scenery_removals"]
    removed_positions = {
        (loc["pos"]["X"], loc["pos"]["Y"])
        for loc in removals
    }

    # Runtime loading removes authored base objects first, then lets MyWorld
    # scenery replace anything at the same position.
    effective_by_position = {
        (loc["pos"]["X"], loc["pos"]["Y"]): loc
        for loc in base
        if (loc["pos"]["X"], loc["pos"]["Y"]) not in removed_positions
    }
    effective_by_position.update({
        (loc["pos"]["X"], loc["pos"]["Y"]): loc
        for loc in custom
    })

    expected_by_id = {
        GOLD_ROCK: GOLD_COORDS,
        CLAY_ROCK: CLAY_COORDS,
        SILVER_ROCK: SILVER_COORDS,
        GEM_ROCK: GEM_COORDS,
    }
    for rock_id, expected_positions in expected_by_id.items():
        for position in expected_positions:
            loc = effective_by_position.get(position)
            if loc is None:
                fail(f"Missing rock id {rock_id} at {position[0]},{position[1]}")
            if loc.get("id") != rock_id:
                fail(
                    f"Rock at {position[0]},{position[1]} should use id {rock_id}, "
                    f"found {loc.get('id')}"
                )
            if loc.get("direction") != 0:
                fail(f"Rock id {rock_id} at {position[0]},{position[1]} should use direction 0")

    actual_resource_positions = {
        rock_id: {
            position
            for position, loc in effective_by_position.items()
            if loc.get("id") == rock_id
            and 336 <= position[0] <= 342
            and 599 <= position[1] <= 609
        }
        for rock_id in expected_by_id
    }
    for rock_id, expected_positions in expected_by_id.items():
        if actual_resource_positions[rock_id] != expected_positions:
            fail(
                f"Unexpected Crafting Guild rock layout for id {rock_id}: "
                f"expected={sorted(expected_positions)} actual={sorted(actual_resource_positions[rock_id])}"
            )

    print("PASS: updated Crafting Guild gold, clay, silver, and gem rock placements look correct")


if __name__ == "__main__":
    main()
