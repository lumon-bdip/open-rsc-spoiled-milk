package com.openrsc.server.plugins.authentic.npcs.varrock;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class Scavvo implements TalkNpcTrigger {

	private static final int RUNE_PLATE_PRICE = 800000;

	@Override
	public void onTalkNpc(final Player player, final Npc n) {
		if (player.getQuestStage(Quests.DRAGON_SLAYER) != -1) {
			npcsay(player, n, "Ello matey", "Come back when you've killed the dragon");
			return;
		}

		npcsay(player, n, "Ello matey", "You ready for your Rune Plate?");
		say(player, n, "You bet I am!");
		npcsay(player, n, "Attaboy", "Just 800k and we'll be square");

		final int option = multi(player, n, false,
			"Come again?",
			"Wait, but I killed the dragon",
			"Sounds like a deal to me!");
		if (option == 0) {
			say(player, n, "Come again?",
				"I'm not sure I heard you correctly, 800k? Surely not.");
			npcsay(player, n, "Of course 800k, that's just simple supply and demand",
				"where else are you going to get one.");
			final int confirm = multi(player, n, false,
				"I guess you're right",
				"I'm sure I'll figure it out");
			if (confirm == 0) {
				say(player, n, "I guess you're right");
				buyRunePlate(player, n);
			} else if (confirm == 1) {
				say(player, n, "I'm sure I'll figure it out");
			}
		} else if (option == 1) {
			say(player, n, "Wait, but I killed the dragon",
				"Why should I be paying for the armor, I thought I earned that armor");
			npcsay(player, n, "You EARNED the right to buy it. For 800k.",
				"You want it or not?");
			final int confirm = multi(player, n, false,
				"I suppose, but I'm not happy about it...",
				"No way! You're out of your mind");
			if (confirm == 0) {
				say(player, n, "I suppose, but I'm not happy about it...");
				buyRunePlate(player, n);
			} else if (confirm == 1) {
				say(player, n, "No way! You're out of your mind");
			}
		} else if (option == 2) {
			say(player, n, "Sounds like a deal to me!");
			buyRunePlate(player, n);
		}
	}

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return n.getID() == NpcId.SCAVVO.id();
	}

	private void buyRunePlate(final Player player, final Npc n) {
		final int coins = player.getCarriedItems().getInventory().countId(ItemId.COINS.id());
		if (coins < RUNE_PLATE_PRICE) {
			npcsay(player, n, "I said 800k, matey", "Come back when your purse is heavier");
			return;
		}

		final Item runePlate = new Item(ItemId.RUNE_PLATE_MAIL_BODY.id(), 1);
		final boolean coinSlotWillEmpty = coins == RUNE_PLATE_PRICE;
		if (!player.getCarriedItems().getInventory().canHold(runePlate)
			&& !(coinSlotWillEmpty && player.getCarriedItems().getInventory().canHold(runePlate, 1))) {
			npcsay(player, n, "You need room to carry the plate first");
			return;
		}

		if (player.getCarriedItems().remove(new Item(ItemId.COINS.id(), RUNE_PLATE_PRICE)) == -1) {
			npcsay(player, n, "I said 800k, matey", "Come back when your purse is heavier");
			return;
		}
		give(player, ItemId.RUNE_PLATE_MAIL_BODY.id(), 1);
		player.message("You hand Scavvo 800,000 coins.");
		npcsay(player, n, "Pleasure doing business with you");
	}
}
