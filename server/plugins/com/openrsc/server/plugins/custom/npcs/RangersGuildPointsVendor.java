package com.openrsc.server.plugins.custom.npcs;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.content.RangersGuildPoints;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpNpcTrigger;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import java.util.ArrayList;
import java.util.List;

import static com.openrsc.server.plugins.Functions.*;

public class RangersGuildPointsVendor implements TalkNpcTrigger, OpNpcTrigger {
	private static final int REWARDS_PER_PAGE = 4;

	private static final Reward[] BOW_REWARDS = new Reward[] {
		new Reward("Magic shortbow", ItemId.MAGIC_SHORTBOW, 1, 350),
		new Reward("Magic longbow", ItemId.MAGIC_LONGBOW, 1, 400),
		new Reward("Ebony shortbow", ItemId.EBONY_SHORTBOW, 1, 500),
		new Reward("Ebony longbow", ItemId.EBONY_LONGBOW, 1, 575),
		new Reward("Blood shortbow", ItemId.BLOOD_SHORTBOW, 1, 700),
		new Reward("Blood longbow", ItemId.BLOOD_LONGBOW, 1, 800),
		new Reward("Magic crossbow", ItemId.MAGIC_CROSSBOW, 1, 500),
		new Reward("Ebony crossbow", ItemId.EBONY_CROSSBOW, 1, 650),
		new Reward("Blood crossbow", ItemId.BLOOD_CROSSBOW, 1, 850),
	};

	private static final Reward[] AMMO_REWARDS = new Reward[] {
		new Reward("100 rune arrows", ItemId.RUNE_ARROWS, 100, 100),
		new Reward("100 poison rune arrows", ItemId.POISON_RUNE_ARROWS, 100, 130),
		new Reward("100 titan arrows", ItemId.TITAN_STEEL_ARROWS, 100, 120),
		new Reward("100 adamantite arrows", ItemId.ADAMANTITE_ARROWS, 100, 140),
		new Reward("100 poison adam arrows", ItemId.POISON_ADAMANTITE_ARROWS, 100, 175),
		new Reward("100 orichalcum arrows", ItemId.ORICHALCUM_ARROWS, 100, 180),
		new Reward("100 rune bolts", ItemId.RUNE_BOLTS, 100, 100),
		new Reward("100 poison rune bolts", ItemId.POISON_RUNE_BOLTS, 100, 130),
		new Reward("100 titan bolts", ItemId.TITAN_STEEL_BOLTS, 100, 120),
		new Reward("100 poison titan bolts", ItemId.POISON_TITAN_STEEL_BOLTS, 100, 150),
		new Reward("100 adamantite bolts", ItemId.ADAMANTITE_BOLTS, 100, 140),
		new Reward("100 poison adam bolts", ItemId.POISON_ADAMANTITE_BOLTS, 100, 175),
		new Reward("100 orichalcum bolts", ItemId.ORICHALCUM_BOLTS, 100, 180),
		new Reward("100 poison orich bolts", ItemId.POISON_ORICHALCUM_BOLTS, 100, 225),
	};

	private static final Reward[] THROWN_REWARDS = new Reward[] {
		new Reward("100 rune darts", ItemId.RUNE_THROWING_DART, 100, 100),
		new Reward("100 poison rune darts", ItemId.POISONED_RUNE_THROWING_DART, 100, 130),
		new Reward("100 titan darts", ItemId.TITAN_STEEL_THROWING_DART, 100, 120),
		new Reward("100 poison titan darts", ItemId.POISONED_TITAN_STEEL_THROWING_DART, 100, 150),
		new Reward("100 adamantite darts", ItemId.ADAMANTITE_THROWING_DART, 100, 140),
		new Reward("100 poison adam darts", ItemId.POISONED_ADAMANTITE_THROWING_DART, 100, 175),
		new Reward("100 orichalcum darts", ItemId.ORICHALCUM_THROWING_DART, 100, 180),
		new Reward("100 poison orich darts", ItemId.POISONED_ORICHALCUM_THROWING_DART, 100, 225),
		new Reward("100 rune knives", ItemId.RUNE_THROWING_KNIFE, 100, 100),
		new Reward("100 poison rune knives", ItemId.POISONED_RUNE_THROWING_KNIFE, 100, 130),
		new Reward("100 titan knives", ItemId.TITAN_STEEL_THROWING_KNIFE, 100, 120),
		new Reward("100 poison titan knives", ItemId.POISONED_TITAN_STEEL_THROWING_KNIFE, 100, 150),
		new Reward("100 adamantite knives", ItemId.ADAMANTITE_THROWING_KNIFE, 100, 140),
		new Reward("100 poison adam knives", ItemId.POISONED_ADAMANTITE_THROWING_KNIFE, 100, 175),
		new Reward("100 orichalcum knives", ItemId.ORICHALCUM_THROWING_KNIFE, 100, 180),
		new Reward("100 poison orich knives", ItemId.POISONED_ORICHALCUM_THROWING_KNIFE, 100, 225),
		new Reward("100 rune shuriken", ItemId.RUNE_SHURIKEN, 100, 100),
		new Reward("100 poison rune shuriken", ItemId.POISONED_RUNE_SHURIKEN, 100, 130),
		new Reward("100 titan shuriken", ItemId.TITAN_STEEL_SHURIKEN, 100, 120),
		new Reward("100 poison titan shuriken", ItemId.POISONED_TITAN_STEEL_SHURIKEN, 100, 150),
		new Reward("100 adamantite shuriken", ItemId.ADAMANTITE_SHURIKEN, 100, 140),
		new Reward("100 poison adam shuriken", ItemId.POISONED_ADAMANTITE_SHURIKEN, 100, 175),
		new Reward("100 orichalcum shuriken", ItemId.ORICHALCUM_SHURIKEN, 100, 180),
		new Reward("100 poison orich shuriken", ItemId.POISONED_ORICHALCUM_SHURIKEN, 100, 225),
	};

	@Override
	public boolean blockTalkNpc(Player player, Npc npc) {
		return isPointsVendor(npc);
	}

	@Override
	public void onTalkNpc(Player player, Npc npc) {
		npcsay(player, npc,
			"Rangers Guild points come from ranged experience in the basement",
			"The better your training, the more points you earn",
			"I can redeem them for high-end ranged supplies");
		openRedeemMenu(player, npc);
	}

	@Override
	public boolean blockOpNpc(Player player, Npc npc, String command) {
		return isPointsVendor(npc) && command.equalsIgnoreCase("Redeem");
	}

	@Override
	public void onOpNpc(Player player, Npc npc, String command) {
		openRedeemMenu(player, npc);
	}

	private boolean isPointsVendor(Npc npc) {
		return npc.getID() == NpcId.RANGERS_GUILD_POINTS_VENDOR.id();
	}

	private void openRedeemMenu(Player player, Npc npc) {
		while (true) {
			int option = multi(player, npc, false,
				"View points",
				"Bows and crossbows",
				"Arrows and bolts",
				"Thrown weapons",
				"Never mind");

			if (option == 0) {
				npcsay(player, npc,
					"You have " + pointsLabel(RangersGuildPoints.getPoints(player)),
					"Earn more by gaining ranged experience in the basement");
			} else if (option == 1) {
				showRewards(player, npc, BOW_REWARDS);
			} else if (option == 2) {
				showRewards(player, npc, AMMO_REWARDS);
			} else if (option == 3) {
				showRewards(player, npc, THROWN_REWARDS);
			} else {
				return;
			}
		}
	}

	private void showRewards(Player player, Npc npc, Reward[] rewards) {
		int page = 0;
		while (true) {
			int start = page * REWARDS_PER_PAGE;
			int end = Math.min(start + REWARDS_PER_PAGE, rewards.length);
			List<String> options = new ArrayList<String>();
			for (int i = start; i < end; i++) {
				options.add(rewards[i].menuLabel());
			}
			boolean hasMore = end < rewards.length;
			if (hasMore) {
				options.add("More rewards");
			}
			options.add("Back");

			int option = multi(player, npc, false, options.toArray(new String[0]));
			if (option < 0) {
				return;
			}

			int rewardCount = end - start;
			if (option < rewardCount) {
				redeem(player, npc, rewards[start + option]);
			} else if (hasMore && option == rewardCount) {
				page++;
			} else {
				return;
			}
		}
	}

	private void redeem(Player player, Npc npc, Reward reward) {
		if (RangersGuildPoints.getPoints(player) < reward.cost) {
			npcsay(player, npc, "You need " + pointsLabel(reward.cost) + " for that");
			return;
		}

		Item item = new Item(reward.itemId.id(), reward.amount);
		if (!player.getCarriedItems().getInventory().canHold(item)) {
			npcsay(player, npc, "You don't have enough inventory space for that");
			return;
		}

		if (!RangersGuildPoints.spendPoints(player, reward.cost)) {
			npcsay(player, npc, "You don't have enough points for that");
			return;
		}
		if (!player.getCarriedItems().getInventory().add(item)) {
			RangersGuildPoints.addPoints(player, reward.cost);
			npcsay(player, npc, "Something went wrong adding that item");
			return;
		}

		player.message("You redeem " + pointsLabel(reward.cost) + " for " + reward.label + ".");
	}

	private String pointsLabel(int points) {
		return points + " Rangers Guild point" + (points == 1 ? "" : "s");
	}

	private static final class Reward {
		private final String label;
		private final ItemId itemId;
		private final int amount;
		private final int cost;

		private Reward(String label, ItemId itemId, int amount, int cost) {
			this.label = label;
			this.itemId = itemId;
			this.amount = amount;
			this.cost = cost;
		}

		private String menuLabel() {
			return label + " - " + cost + " pts";
		}
	}
}
