#!/usr/bin/env python3

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DEVELOPMENT = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/commands/Development.java"
POPULATOR = ROOT / "server/src/com/openrsc/server/database/WorldPopulator.java"
NPC_EDIT_FILES = ROOT / "server/src/com/openrsc/server/util/WorldNpcEditFiles.java"
DOCS = ROOT / "docs/myworld/object-ids.md"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def read(path: Path) -> str:
	if not path.exists():
		fail(f"Missing file: {path}")
	return path.read_text(encoding="utf-8")


def require(path: Path, needle: str) -> None:
	text = read(path)
	if needle not in text:
		fail(f"{path.name} missing expected text: {needle}")


def main() -> None:
	require(DEVELOPMENT, "PENDING_NPC_EDITS")
	require(DEVELOPMENT, "queueWorldNpcUpsert(player, n.getLoc());")
	require(DEVELOPMENT, "queueWorldNpcRemoval(player, npc.getLoc());")
	require(DEVELOPMENT, "WorldNpcEditFiles.save(configDir, npcEdits)")
	require(DEVELOPMENT, "NPC locs: ")
	require(DEVELOPMENT, "PENDING_NPC_EDITS.clear();")

	require(NPC_EDIT_FILES, 'private static final String LOCS_RELATIVE_PATH = "defs/locs/MyWorldNpcLocs.json";')
	require(NPC_EDIT_FILES, 'private static final String REMOVALS_RELATIVE_PATH = "defs/locs/MyWorldNpcRemovals.json";')
	require(NPC_EDIT_FILES, 'private static final String LOCS_ROOT = "npclocs";')
	require(NPC_EDIT_FILES, 'private static final String REMOVALS_ROOT = "npc_removals";')
	require(NPC_EDIT_FILES, "public static SaveResult save")
	require(NPC_EDIT_FILES, "public static Set<String> readNpcRemovalKeys")

	require(POPULATOR, "applyMyWorldNpcRemovals();")
	require(POPULATOR, "WorldNpcEditFiles.readNpcRemovalKeys")
	require(POPULATOR, "WorldNpcEditFiles.npcKey(loc)")

	require(DOCS, "::cnpc <id> <radius>")
	require(DOCS, "MyWorldNpcLocs.json")
	require(DOCS, "MyWorldNpcRemovals.json")

	print("PASS: world NPC edit persistence wiring validated")


if __name__ == "__main__":
	main()
