package com.openrsc.server.plugins.authentic.npcs.portsarim;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.SceneryId;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpLocTrigger;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;
import com.openrsc.server.plugins.shared.EntranaRestrictions;

import static com.openrsc.server.plugins.Functions.*;

public final class MonkOfEntrana implements OpLocTrigger,
	TalkNpcTrigger {
	private static final String TRAVEL_REQUIREMENT_MESSAGE = "You do not meet the requirements to travel";

	@Override
	public boolean blockTalkNpc(final Player player, final Npc npc) {
		return npc.getID() == NpcId.MONK_OF_ENTRANA_PORTSARIM.id() || npc.getID() == NpcId.MONK_OF_ENTRANA_UNRELEASED.id();
	}

	@Override
	public void onTalkNpc(final Player player, final Npc npc) {
		if (!player.getWorld().getServer().getConfig().MEMBER_WORLD) {
			return;
		}
		if (npc.getID() == NpcId.MONK_OF_ENTRANA_PORTSARIM.id()) {
			npcsay(player, npc, "Are you looking to take passage to our holy island?",
					"If so your weapons and armour must be left behind");
			if (multi(player, npc, "No I don't wish to go",
				"Yes, Okay I'm ready to go") == 1) {
				mes("The monk quickly searches you");
				delay(5);
				if (EntranaRestrictions.playerNotAllowedOnEntrana(player)) {
					npcsay(player, npc, "Sorry we cannow allow you on to our island",
						"Make sure you are not carrying weapons or armour please");
				} else {
					mes("You board the ship");
					delay(5);
					player.teleport(418, 570, false);
					delay(3);
					mes("The ship arrives at Entrana");
				}
			}
		}
		else if (npc.getID() == NpcId.MONK_OF_ENTRANA_UNRELEASED.id()) {
			// This code does not run, it is hypothetical & not based on reality.
			// There is not a spawn of this monk anywhere.
			npcsay(player, npc, "Are you looking to take passage back to port sarim?");
			if (multi(player, npc, "No I don't wish to go",
				"Yes, Okay I'm ready to go") == 1) {

				mes("You board the ship");
				delay(3);
				player.teleport(264, 660, false);
				delay(3);
				mes("The ship arrives at Port Sarim");
				delay(3);
			}
			return;
		}
	}

	@Override
	public void onOpLoc(Player player, GameObject gameObject, String command) {
		if (command.equalsIgnoreCase("board")) {
			shortcutTravelToEntrana(player);
			return;
		}

		Npc monk = ifnearvisnpc(player, NpcId.MONK_OF_ENTRANA_PORTSARIM.id(), 10);
		if (monk != null) {
			monk.initializeTalkScript(player);
		} else {
			player.message("I need to speak to the monk before boarding the ship.");
		}

	}

	private void shortcutTravelToEntrana(Player player) {
		if (!player.getWorld().getServer().getConfig().MEMBER_WORLD || EntranaRestrictions.playerNotAllowedOnEntrana(player)) {
			player.message(TRAVEL_REQUIREMENT_MESSAGE);
			return;
		}

		player.message("The monk quickly searches you");
		player.message("You board the ship");
		player.teleport(418, 570, false);
		player.message("The ship arrives at Entrana");
	}

	@Override
	public boolean blockOpLoc(Player player, GameObject gameObject, String command) {
		return gameObject.getID() == SceneryId.SHIP_BACK_ENTRANA.id() ||
			gameObject.getID() == SceneryId.SHIP_MIDDLE_ENTRANA.id() ||
			gameObject.getID() == SceneryId.SHIP_FRONT_ENTRANA.id();
	}
}
