#!/usr/bin/env python3
"""Validate the staged MyWorld mining guild underground layout."""

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LOCS_DIR = ROOT / "server/conf/server/defs/locs"
ACTIVE_SCENERY_FILES = (
    LOCS_DIR / "SceneryLocs.json",
    LOCS_DIR / "SceneryLocsOther.json",
    LOCS_DIR / "MyWorldSceneryLocs.json",
)

MITHRIL_ROCK = 107
ADAMANTITE_ROCK = 109
COAL_ROCK = 110
GOLD_ROCK = 113
SILVER_ROCK = 196
RUNITE_ROCK = 210
LADDER = 5

EXPECTED_STAGE_ONE = {
    (268, 3399): MITHRIL_ROCK,
    (269, 3399): MITHRIL_ROCK,
    (270, 3399): MITHRIL_ROCK,
    (271, 3399): MITHRIL_ROCK,
    (272, 3399): MITHRIL_ROCK,
    (268, 3398): MITHRIL_ROCK,
    (269, 3398): MITHRIL_ROCK,
    (270, 3398): MITHRIL_ROCK,
    (265, 3398): MITHRIL_ROCK,
    (264, 3397): MITHRIL_ROCK,
    (263, 3396): RUNITE_ROCK,
    (263, 3395): RUNITE_ROCK,
    (263, 3394): RUNITE_ROCK,
    (263, 3393): RUNITE_ROCK,
    (264, 3389): ADAMANTITE_ROCK,
    (264, 3390): ADAMANTITE_ROCK,
    (265, 3388): ADAMANTITE_ROCK,
    (266, 3387): ADAMANTITE_ROCK,
    (266, 3386): ADAMANTITE_ROCK,
    (271, 3387): ADAMANTITE_ROCK,
    (270, 3386): ADAMANTITE_ROCK,
    (271, 3388): ADAMANTITE_ROCK,
    (272, 3389): ADAMANTITE_ROCK,
    (266, 3395): COAL_ROCK,
    (267, 3395): COAL_ROCK,
    (268, 3395): COAL_ROCK,
    (269, 3395): COAL_ROCK,
    (270, 3395): COAL_ROCK,
    (266, 3394): COAL_ROCK,
    (267, 3394): COAL_ROCK,
    (268, 3394): COAL_ROCK,
    (269, 3394): COAL_ROCK,
    (270, 3394): COAL_ROCK,
    (271, 3394): COAL_ROCK,
    (272, 3394): COAL_ROCK,
    (273, 3394): COAL_ROCK,
    (271, 3392): COAL_ROCK,
    (270, 3392): COAL_ROCK,
    (269, 3392): COAL_ROCK,
    (268, 3392): COAL_ROCK,
    (268, 3391): COAL_ROCK,
    (269, 3391): COAL_ROCK,
    (270, 3391): COAL_ROCK,
    (276, 3399): GOLD_ROCK,
    (277, 3398): GOLD_ROCK,
    (277, 3397): GOLD_ROCK,
    (277, 3395): SILVER_ROCK,
    (277, 3394): SILVER_ROCK,
    (276, 3393): SILVER_ROCK,
    (274, 3398): LADDER,
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def load_active_stage_entries() -> dict[tuple[int, int], list[int]]:
    entries: dict[tuple[int, int], list[int]] = {}
    for path in ACTIVE_SCENERY_FILES:
        sceneries = json.loads(path.read_text(encoding="utf-8"))["sceneries"]
        for scenery in sceneries:
            x = scenery["pos"]["X"]
            y = scenery["pos"]["Y"]
            if 263 <= x <= 277 and 3386 <= y <= 3400:
                entries.setdefault((x, y), []).append(scenery["id"])
    return entries


def main() -> None:
    actual = load_active_stage_entries()

    for coord, expected_id in EXPECTED_STAGE_ONE.items():
        ids = actual.get(coord)
        if ids != [expected_id]:
            fail(f"Expected {expected_id} at {coord}, found {ids}")

    extra = {coord: ids for coord, ids in actual.items() if coord not in EXPECTED_STAGE_ONE}
    if extra:
        fail(f"Unexpected mining guild stage-one scenery remains: {extra}")

    print("PASS: mining guild stage-one layout validated")


if __name__ == "__main__":
    main()
