package com.openrsc.server.plugins.authentic.npcs.edgeville;

import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class OziachsRunePlateShop implements TalkNpcTrigger {

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return n.getID() == NpcId.OZIACH.id() && player.getQuestStage(Quests.DRAGON_SLAYER) == -1;
	}

	@Override
	public void onTalkNpc(final Player player, final Npc n) {
		say(player, n, "I have slain the dragon.");
		npcsay(player, n, "Well done.",
			"Go speak with Scavvo at the Champion's Guild to get your Rune Platebody");
	}
}
