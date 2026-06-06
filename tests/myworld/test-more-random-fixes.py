#!/usr/bin/env python3

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LAW_JEWELRY = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/enchanting/LawJewelry.java"
WOODCUTTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/Woodcutting.java"
NPC_LOCS = ROOT / "server/conf/server/defs/locs/NpcLocs.json"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> None:
	law_jewelry = LAW_JEWELRY.read_text(encoding="utf-8")
	for snippet in (
		"missingCharges * 5 * tier",
		"countId(ItemId.LAW_RUNE.id(), Optional.of(false)) < requiredRunes",
		'player.message("You need " + requiredRunes + " law runes to recharge this amulet.");',
		"player.getCarriedItems().remove(new Item(ItemId.LAW_RUNE.id(), requiredRunes)) != -1",
	):
		require(snippet in law_jewelry, f"Law amulet recharge should spend tiered law runes: {snippet}")

	woodcutting = WOODCUTTING.read_text(encoding="utf-8")
	require("startbatch(1);" in woodcutting, "Woodcutting should show a one-step batch progress bar")
	require("startbatch(30);" not in woodcutting, "Woodcutting should not show a misleading 30-step batch")

	npc_locs = json.loads(NPC_LOCS.read_text(encoding="utf-8"))["npclocs"]
	nurmof_locs = [loc for loc in npc_locs if loc.get("id") == 773]
	require(len(nurmof_locs) == 1, "Nurmof should have one spawn location")
	nurmof = nurmof_locs[0]
	require(nurmof["start"] == {"X": 272, "Y": 3397}, "Nurmof should start inside the Mining Guild")
	require(nurmof["min"] == {"X": 269, "Y": 3394}, "Nurmof Mining Guild patrol min should be stable")
	require(nurmof["max"] == {"X": 275, "Y": 3400}, "Nurmof Mining Guild patrol max should be stable")

	print("PASS: more random fixes validated")


if __name__ == "__main__":
	main()
