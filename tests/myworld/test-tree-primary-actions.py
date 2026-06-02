#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OBJECT_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "GameObjectDef.xml"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> None:
	root = ET.parse(OBJECT_DEFS).getroot()
	for index, obj in enumerate(root.findall("GameObjectDef")):
		commands = [
			(obj.findtext("command1") or ""),
			(obj.findtext("command2") or ""),
		]
		if any(command.lower() == "chop" for command in commands):
			name = obj.findtext("name") or f"object {index}"
			model = obj.findtext("objectModel") or "unknown model"
			require(
				commands[0].lower() == "chop",
				f"{name} ({model}) has Chop outside the primary action slot: {commands}",
			)

	client = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
	for snippet in (
		'new GameObjectDef("Tree", "This tree doesn\'t look too healthy", "Chop", "Examine", 1, 1, 1, 0, "deadtree1"',
		'new GameObjectDef("dead Tree", "A rotting tree", "Chop", "Examine", 1, 1, 1, 0, "deadtree2"',
	):
		require(snippet in client, f"Client object definition should use primary Chop action: {snippet}")

	print("PASS: choppable tree primary actions validated")


if __name__ == "__main__":
	main()
