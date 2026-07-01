#!/usr/bin/env python3

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WIZARD_PLUGIN = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/yanille/WizardFrumscone.java"
NPC_DEFS = ROOT / "server/conf/server/defs/NpcDefs.json"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"

WIZARD_FRUMSCONE = 515


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> None:
	plugin = WIZARD_PLUGIN.read_text(encoding="utf-8")
	require("implements TalkNpcTrigger, OpNpcTrigger" in plugin,
		"Wizard Frumscone should handle both dialogue and right-click NPC actions")
	require('private static final String GIVE_ZOMBIE_EYES = "Give zombie eyes";' in plugin,
		"Wizard Frumscone should expose a Give zombie eyes right-click action")
	require('private static final String GIVE_SCALES = "Give scales";' in plugin,
		"Wizard Frumscone should expose a Give scales right-click action")
	require("GIVE_ZOMBIE_EYES.equalsIgnoreCase(command)" in plugin
		and "GIVE_SCALES.equalsIgnoreCase(command)" in plugin,
		"Wizard Frumscone right-click command matching should be case-insensitive")

	for line in (
		'"Do you like my magic zombies and baby blue dragons"',
		'"If you bring me blue dragon scales or zombie eyes"',
		"\"I'll trade each zombie eye for 2 stone\"",
		'"And each blue dragon scale for 3 stone"',
		'"I brought you blue dragon scales"',
		'"I brought you zombie eyes"',
	):
		require(line in plugin, f"Wizard Frumscone dialogue is missing {line}")

	require("tradeForStone(player, n, ItemId.ZOMBIE_EYE.id(), 2);" in plugin,
		"Zombie eyes should trade for two stone each")
	require("tradeForStone(player, n, ItemId.BLUE_DRAGON_SCALE.id(), 3);" in plugin,
		"Blue dragon scales should trade for three stone each")
	require("int itemCount = player.getCarriedItems().getInventory().countId(itemId);" in plugin,
		"Wizard Frumscone should trade every matching item in the inventory")
	require("player.getCarriedItems().remove(new Item(itemId, itemCount))" in plugin,
		"Wizard Frumscone should remove all traded items before awarding stone")
	require("int carriedStone = Math.min(stoneCount, player.getCarriedItems().getInventory().getFreeSlots());" in plugin,
		"Wizard Frumscone should only place stone into available inventory slots")
	require("give(player, ItemId.RUNE_STONE.id(), carriedStone);" in plugin,
		"Wizard Frumscone should award rune stone to inventory first")
	require("dropStoneOverflow(player, droppedStone);" in plugin,
		"Wizard Frumscone should drop stone overflow")
	require("\"Thank you, here's \" + stoneCount + \" stone in return\"" in plugin,
		"Wizard Frumscone should use the smoother trade completion response")
	require('"I traded those for "' not in plugin,
		"Wizard Frumscone should not use the old clunky trade response")
	require(re.search(
		r"new GroundItem\(player\.getWorld\(\), ItemId\.RUNE_STONE\.id\(\), "
		r"player\.getX\(\), player\.getY\(\), 1, player\)",
		plugin,
	) is not None, "Wizard Frumscone should drop overflow stone at the player's feet as individual items")

	npcs = json.loads(NPC_DEFS.read_text(encoding="utf-8"))["npcs"]
	frumscone = next((npc for npc in npcs if npc["id"] == WIZARD_FRUMSCONE), None)
	require(frumscone is not None, "NpcDefs should include Wizard Frumscone")
	require(frumscone["command"] == "Give zombie eyes",
		"Server NPC definition should expose Give zombie eyes")
	require(frumscone["command2"] == "Give scales",
		"Server NPC definition should expose Give scales")

	client_defs = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
	require('new NPCDef("Wizard Frumscone", "A confused looking wizard", "Give zombie eyes", "Give scales"' in client_defs,
		"Client runtime NPC definition should expose both Wizard Frumscone commands")

	print("PASS: Wizard Frumscone stone trades validated")


if __name__ == "__main__":
	main()
