#!/usr/bin/env python3
import json
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MYWORLD_SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"

CAVERN_ROWS = {
    3276: (356, 376),
    3277: (352, 377),
    3278: (350, 378),
    3279: (349, 379),
    3280: (348, 380),
    3281: (347, 380),
    3282: (346, 381),
    3283: (345, 381),
    3284: (344, 382),
    3285: (343, 382),
    3286: (342, 381),
    3287: (341, 381),
    3288: (341, 380),
    3289: (342, 380),
    3290: (342, 379),
    3291: (343, 379),
    3292: (344, 378),
    3293: (345, 378),
    3294: (347, 377),
    3295: (350, 375),
}

RESOURCE_IDS = {
    88: "evil tree",
    98: "stone",
    106: "mithril",
    108: "adamantite",
    110: "coal",
    210: "runite",
    407: "dead tree",
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    cavern_tiles = {
        (x, y)
        for y, (minimum_x, maximum_x) in CAVERN_ROWS.items()
        for x in range(minimum_x, maximum_x + 1)
    }
    interior_tiles = {
        (x, y)
        for x, y in cavern_tiles
        if all(
            (x + dx, y + dy) in cavern_tiles
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
    }

    locs = json.loads(MYWORLD_SCENERY_LOCS.read_text())["sceneries"]
    resources = [
        entry
        for entry in locs
        if entry["id"] in RESOURCE_IDS
        and (entry["pos"]["X"], entry["pos"]["Y"]) in cavern_tiles
    ]
    counts = Counter(entry["id"] for entry in resources)

    expected_counts = {
        88: 3,
        98: 3,
        106: 7,
        108: 5,
        110: 10,
        210: 3,
        407: 5,
    }
    if counts != expected_counts:
        readable = {RESOURCE_IDS[key]: value for key, value in sorted(counts.items())}
        fail(f"Heroes' Guild cavern resource counts are wrong: {readable}")

    positions = [
        (entry["pos"]["X"], entry["pos"]["Y"])
        for entry in resources
    ]
    if len(positions) != len(set(positions)):
        fail("Heroes' Guild cavern has overlapping resource placements")

    for entry in resources:
        point = (entry["pos"]["X"], entry["pos"]["Y"])
        if entry["id"] in {88, 407} and point not in interior_tiles:
            fail(f"{RESOURCE_IDS[entry['id']]} at {point[0]},{point[1]} is too close to a wall")

    print("PASS: Heroes' Guild cavern resources validated")


if __name__ == "__main__":
    main()
