#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCS_DIR = ROOT / "server/conf/server/defs/locs"
BASE_SCENERY = LOCS_DIR / "SceneryLocs.json"
MYWORLD_SCENERY = LOCS_DIR / "MyWorldSceneryLocs.json"

ROCK_IDS = set(range(98, 116)) | {
    167,
    496,
    1042,
    1043,
    1044,
    1045,
    1046,
    1048,
    1050,
    1052,
    1056,
    1058,
}


def load_sceneries(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return data[next(iter(data))]


def rock_tiles(path: Path) -> dict[tuple[int, int], int]:
    tiles: dict[tuple[int, int], int] = {}
    for entry in load_sceneries(path):
        if entry.get("id") not in ROCK_IDS:
            continue
        pos = entry["pos"]
        tiles[(pos["X"], pos["Y"])] = entry["id"]
    return tiles


def main() -> None:
    base_rocks = rock_tiles(BASE_SCENERY)
    myworld_rocks = rock_tiles(MYWORLD_SCENERY)
    overlaps = []
    for entry in load_sceneries(MYWORLD_SCENERY):
        pos = entry["pos"]
        tile = (pos["X"], pos["Y"])
        if entry.get("id") in ROCK_IDS and tile in base_rocks:
            overlaps.append(f"{tile}: base {base_rocks[tile]}, myworld {entry['id']}")

    if overlaps:
        raise SystemExit(
            "MyWorld rock scenery overlaps base map rock scenery, which can cause client flicker:\n"
            + "\n".join(overlaps)
        )

    if myworld_rocks.get((420, 3523)) != 98:
        raise SystemExit("Expected a generic Stone rock at 420,3523")

    print("PASS: MyWorld rock scenery does not overlap base map rock scenery")


if __name__ == "__main__":
    main()
