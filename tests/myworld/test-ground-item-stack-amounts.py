#!/usr/bin/env python3

import json
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LOCS = ROOT / "server/conf/server/defs/locs"

FIRE_RUNE_ID = 31
KARAMJA_VOLCANO_FIRE_RUNES = {
    (408, 3532),
    (413, 3534),
    (405, 3537),
    (414, 3540),
    (409, 3543),
}


def load_ground_items(path: Path) -> list[dict]:
    return json.loads(path.read_text(encoding="utf-8")).get("grounditems", [])


def main() -> None:
    failures: list[str] = []

    for path in sorted(LOCS.glob("GroundItems*.json")):
        entries = load_ground_items(path)
        by_identity: dict[tuple[int, int, int], list[dict]] = defaultdict(list)
        for entry in entries:
            key = (int(entry["id"]), int(entry["pos"]["X"]), int(entry["pos"]["Y"]))
            by_identity[key].append(entry)

        for (item_id, x, y), matching_entries in sorted(by_identity.items()):
            if len(matching_entries) > 1:
                failures.append(
                    f"{path.relative_to(ROOT)} has {len(matching_entries)} duplicate "
                    f"ground item entities for item {item_id} at {x},{y}"
                )

        if path.name not in {"GroundItems.json", "GroundItems27.json"}:
            continue

        actual = {
            (int(entry["pos"]["X"]), int(entry["pos"]["Y"])): int(entry["amount"])
            for entry in entries
            if int(entry["id"]) == FIRE_RUNE_ID
            and (int(entry["pos"]["X"]), int(entry["pos"]["Y"])) in KARAMJA_VOLCANO_FIRE_RUNES
        }
        if set(actual) != KARAMJA_VOLCANO_FIRE_RUNES:
            failures.append(
                f"{path.relative_to(ROOT)} Karamja volcano fire rune locations were {sorted(actual)}"
            )
        for coord, amount in sorted(actual.items()):
            if amount != 5:
                failures.append(
                    f"{path.relative_to(ROOT)} fire rune at {coord[0]},{coord[1]} "
                    f"has amount {amount}, expected one stack of 5"
                )

    if failures:
        raise AssertionError("\n".join(failures))

    print("PASS: stackable static ground item amounts validated")


if __name__ == "__main__":
    main()
