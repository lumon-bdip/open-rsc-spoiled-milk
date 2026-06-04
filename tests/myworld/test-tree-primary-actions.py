#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OBJECT_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "GameObjectDef.xml"
WOODCUTTING_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "extras" / "ObjectWoodcutting.xml"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"
WOODCUTTING_PLUGIN = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "skills" / "woodcutting" / "Woodcutting.java"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> None:
	root = ET.parse(OBJECT_DEFS).getroot()
	objects = root.findall("GameObjectDef")
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

	dead_tree = objects[407]
	require(dead_tree.findtext("name") == "dead Tree", "Vanilla dead tree should remain object id 407")
	require(dead_tree.findtext("objectModel") == "deadtree2", "Vanilla dead tree id 407 should use deadtree2 model")
	blood_tree = objects[88]
	require(blood_tree.findtext("name") == "Tree", "Blood tree should keep the generic Tree display name")
	require(blood_tree.findtext("description") == "A dark tree that pulses with a faint red glow", "Blood tree should have the red-glow examine text")
	require(blood_tree.findtext("command1") == "Chop", "Blood tree should use Chop as its primary action")
	require(blood_tree.findtext("command2") == "Examine", "Blood tree should use Examine as its secondary action")

	woodcutting_root = ET.parse(WOODCUTTING_DEFS).getroot()
	woodcutting_defs = {}
	for entry in woodcutting_root.findall("entry"):
		object_id = int(entry.findtext("int", default="-1"))
		definition = entry.find("ObjectWoodcuttingDef")
		if definition is not None:
			woodcutting_defs[object_id] = definition
	require(407 in woodcutting_defs, "Ebony logs should come from the vanilla dead tree object id 407")
	require(4480 not in woodcutting_defs, "Ebony logs should not be keyed to the GameObjectDef.xml line number")
	require(woodcutting_defs[407].findtext("logId") == "2113", "Vanilla dead trees should produce ebony logs")
	require(woodcutting_defs[407].findtext("requiredLevel") == "54", "Ebony dead trees should require level 54")
	require(88 in woodcutting_defs, "Blood logs should come from the damaging blood tree object id 88")
	require(woodcutting_defs[88].findtext("logId") == "2114", "Blood trees should produce blood logs")
	require(woodcutting_defs[88].findtext("requiredLevel") == "70", "Blood trees should require level 70")

	woodcutting_plugin = WOODCUTTING_PLUGIN.read_text(encoding="utf-8")
	require("def.getLogId() == ItemId.EBONY_LOGS.id()" in woodcutting_plugin,
		"Ebony dead trees should despawn instead of replacing with a stump")
	require("if (object.getID() == 88)" in woodcutting_plugin and "The tree lashes out at you as you begin to chop it." in woodcutting_plugin,
		"Blood tree id 88 should remain the damaging woodcutting tree")

	client = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
	for snippet in (
		'new GameObjectDef("Tree", "This tree doesn\'t look too healthy", "Chop", "Examine", 1, 1, 1, 0, "deadtree1"',
		'new GameObjectDef("Tree", "A dark tree that pulses with a faint red glow", "Chop", "Examine", 1, 1, 1, 0, "deadtree1"',
		'new GameObjectDef("dead Tree", "A rotting tree", "Chop", "Examine", 1, 1, 1, 0, "deadtree2"',
	):
		require(snippet in client, f"Client object definition should use primary Chop action: {snippet}")

	print("PASS: choppable tree primary actions validated")


if __name__ == "__main__":
	main()
