#!/usr/bin/env python3

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
NPC_LOCS = ROOT / "server/conf/server/defs/locs/NpcLocs.json"
BOUNDARY_LOCS = ROOT / "server/conf/server/defs/locs/BoundaryLocs.json"

WIZARD_FRUMSCONE = 515
TARGET_PRACTICE_ZOMBIE = 516
TRAINING_CAGE_DOOR = 150

CAGE_MIN = {"X": 604, "Y": 3584}
CAGE_MAX = {"X": 609, "Y": 3588}
EXPECTED_ZOMBIE_STARTS = {
	(605, 3584),
	(607, 3584),
	(608, 3584),
	(606, 3585),
	(609, 3585),
	(607, 3586),
	(605, 3587),
	(609, 3587),
}


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def point_tuple(point: dict[str, int]) -> tuple[int, int]:
	return int(point["X"]), int(point["Y"])


def main() -> None:
	npcs = json.loads(NPC_LOCS.read_text(encoding="utf-8"))["npclocs"]
	boundaries = json.loads(BOUNDARY_LOCS.read_text(encoding="utf-8"))["boundaries"]

	frumscone_locs = [
		loc for loc in npcs
		if loc.get("id") == WIZARD_FRUMSCONE
		and 600 <= loc["start"]["X"] <= 612
		and 3580 <= loc["start"]["Y"] <= 3592
	]
	require(len(frumscone_locs) == 1, "Wizard Frumscone should have one Yanille zombie-cage-area spawn")
	frumscone = frumscone_locs[0]
	require(frumscone["start"] == {"X": 607, "Y": 3590}, "Wizard Frumscone should spawn at 607,3590")
	require(frumscone["min"] == {"X": 605, "Y": 3589}, "Wizard Frumscone patrol min should be outside the cage")
	require(frumscone["max"] == {"X": 609, "Y": 3592}, "Wizard Frumscone patrol max should be outside the cage")

	zombies = [
		loc for loc in npcs
		if loc.get("id") == TARGET_PRACTICE_ZOMBIE
		and 604 <= loc["start"]["X"] <= 610
		and 3583 <= loc["start"]["Y"] <= 3588
	]
	require({point_tuple(loc["start"]) for loc in zombies} == EXPECTED_ZOMBIE_STARTS,
		"Target practice zombie starts should be the eight caged positions")
	require(len(zombies) == len(EXPECTED_ZOMBIE_STARTS),
		"Target practice zombies should not have extra starts on the cage edge")
	for zombie in zombies:
		require(zombie["min"] == CAGE_MIN and zombie["max"] == CAGE_MAX,
			f"Target practice zombie at {point_tuple(zombie['start'])} should stay inside the cage")

	require(not any(
		loc.get("id") == TRAINING_CAGE_DOOR
		and loc.get("pos") == {"X": 604, "Y": 3587}
		for loc in boundaries
	), "The training zombie cage door at 604,3587 should be removed")

	print("PASS: Wizard Frumscone zombie cage layout validated")


if __name__ == "__main__":
	main()
