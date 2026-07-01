#!/usr/bin/env python3

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
NPC_DROPS = ROOT / "server/src/com/openrsc/server/constants/NpcDrops.java"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> None:
	drops = NPC_DROPS.read_text(encoding="utf-8")
	require("this.bigBoneNpcs.add(NpcId.BABY_BLUE_DRAGON.id());" in drops,
		"Baby Blue Dragons should keep guaranteed Big bones")
	require('addGuaranteedDrop(NpcId.BABY_BLUE_DRAGON.id(), ItemId.BABY_DRAGON_HIDE.id(), "Baby Dragon hide");' in drops,
		"Baby Blue Dragons should keep guaranteed Baby Dragon hide")
	require('addGuaranteedDrop(NpcId.BABY_BLUE_DRAGON.id(), ItemId.BLUE_DRAGON_SCALE.id(), "Baby Blue Dragon scale");' not in drops,
		"Baby Blue Dragon scales should not be guaranteed")
	require("DropTable babyBlueDragonDrops = this.npcDrops.get(NpcId.BABY_BLUE_DRAGON.id());" in drops,
		"Baby Blue Dragon should reuse its material drop table for uncommon scales")
	require("babyBlueDragonDrops.addItemDrop(ItemId.BLUE_DRAGON_SCALE.id(), 1, 4);" in drops,
		"Baby Blue Dragons should drop one Blue dragon scale at uncommon weight")
	require("babyBlueDragonDrops.addEmptyDrop(128 - babyBlueDragonDrops.getTotalWeight());" in drops,
		"Baby Blue Dragon uncommon scale table should keep 128-weight odds")

	print("PASS: Baby Blue Dragon drops validated")


if __name__ == "__main__":
	main()
