package com.openrsc.server.plugins.custom.npcs;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class RangersGuildRanger implements TalkNpcTrigger {

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.RANGERS_GUILD_RANGER.id();
	}

	@Override
	public void onTalkNpc(Player player, Npc n) {
		if (getCurrentLevel(player, Skill.RANGED.id()) < 66) {
			npcsay(player, n, "Hello, only skilled rangers are allowed in here",
				"You'll need a ranged level of 66 to enter");
			player.message("You need a ranged level of 66 to enter the guild");
			return;
		}

		npcsay(player, n, "Hello, welcome to the Rangers Guild",
			"Only accomplished rangers are allowed in here",
			"Lowe can tell you about the guild inside",
			"The basement is set up for ranged practice");
	}
}
