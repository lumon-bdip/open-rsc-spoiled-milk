#!/usr/bin/env python3

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LAW_JEWELRY = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/enchanting/LawJewelry.java"
MAGIC_GUILD_PORTALS = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/misc/MagicGuildPortals.java"
SPELL_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/SpellHandler.java"
SPELL_DEFS = ROOT / "server/conf/server/defs/SpellDef.xml"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
WOODCUTTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/Woodcutting.java"
NPC_LOCS = ROOT / "server/conf/server/defs/locs/NpcLocs.json"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"


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

	scenery_locs = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
	require(any(loc.get("id") == 235 and loc.get("pos") == {"X": 588, "Y": 666} for loc in scenery_locs),
		"Guthix altar should be moved to 588,666")
	require(any(loc.get("id") == 200 and loc.get("pos") == {"X": 309, "Y": 571} for loc in scenery_locs),
		"Saradomin altar should remain at 309,571")
	require(not any(loc.get("id") == 200 and loc.get("pos") == {"X": 321, "Y": 571} for loc in scenery_locs),
		"Duplicate Saradomin altar should be removed from 321,571")

	client_defs = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
	require('new ItemDef("Robe of Guthix", "A robe top blessed by Guthix"' in client_defs,
		"Client should name the former druid robe top as Guthix")
	require('new ItemDef("Robe of Guthix", "A robe bottom blessed by Guthix"' in client_defs,
		"Client should name the former druid robe bottom as Guthix")
	require('spells.add(new SpellDef("Iban blast", "Dark magic that splashes nearby foes"' in client_defs,
		"Client Iban blast description should mention splash damage")
	require('spells.add(new SpellDef("Lesser Heal", "Restores health over 9 seconds"' in client_defs,
		"Client lesser heal description should mention heal over time")
	require('spells.add(new SpellDef("Greater Heal", "Restores more health over 9 seconds"' in client_defs,
		"Client greater heal description should mention heal over time")

	spell_defs = SPELL_DEFS.read_text(encoding="utf-8")
	require("<description>Dark magic that splashes nearby foes</description>" in spell_defs,
		"Server Iban blast description should mention splash damage")
	require("<description>Restores health over 9 seconds</description>" in spell_defs,
		"Server lesser heal description should mention heal over time")
	require("<description>Restores more health over 9 seconds</description>" in spell_defs,
		"Server greater heal description should mention heal over time")

	spell_handler = SPELL_HANDLER.read_text(encoding="utf-8")
	for snippet in (
		"applyIbanBlastAreaEffects(getPlayer(), affectedMob);",
		"private void applyIbanBlastAreaEffects(final Player caster, final Mob primaryTarget)",
		"final int secondaryMax = 8;",
		"private static final String HEAL_SPELL_ACTIVE_KEY = \"heal_spell_active\";",
		"private static final int HEAL_SPELL_PULSES = 3;",
		"private static final int HEAL_SPELL_INTERVAL_MS = 3000;",
		"private static final int LESSER_HEAL_POWER_PER_PULSE = 60;",
		"private static final class HealOverTimeEvent extends GameTickEvent",
		"player.getCarriedItems().getEquipment().getDisplayedMagicOffense()",
		"return 1 + (Math.max(0, magicPower) / LESSER_HEAL_POWER_PER_PULSE);",
		"player.message(\"A healing spell is already restoring your health\");",
	):
		require(snippet in spell_handler, f"SpellHandler missing expected random-fix spell snippet: {snippet}")

	def lesser_heal_per_pulse(magic_power: int) -> int:
		return 1 + max(0, magic_power) // 60

	require(lesser_heal_per_pulse(24) == 1, "Lesser Heal should remain at 1 healing per pulse at 24 magic power")
	require(lesser_heal_per_pulse(59) == 1, "Lesser Heal should not reach 2 healing before 60 magic power")
	require(lesser_heal_per_pulse(60) == 2, "Lesser Heal should reach 2 healing at 60 magic power")
	require(lesser_heal_per_pulse(120) == 3, "Lesser Heal should reach 3 healing at 120 magic power")

	magic_portals = MAGIC_GUILD_PORTALS.read_text(encoding="utf-8")
	for snippet in (
		"""private static final String[] MAGIC_PORTAL_DESTINATIONS = {"Wizards' Tower", "Thormac's tower", "Dark Wizards' Tower"};""",
		'player.playerServerMessage(MessageType.QUEST, "This portal leads to " + getDestinationName(obj.getID()) + ".");',
	):
		require(snippet in magic_portals, f"Magic guild portals should expose destination examine text: {snippet}")

	print("PASS: more random fixes validated")


if __name__ == "__main__":
	main()
