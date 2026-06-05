package com.openrsc.server.plugins.authentic.npcs.varrock;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.AbstractShop;

import static com.openrsc.server.plugins.Functions.*;

public class HeadChef extends AbstractShop {

	private final Shop shop = new Shop(false, 30000, 100, 70, 2,
		new Item(ItemId.CHOCOLATE_BAR.id(), 20), new Item(ItemId.REDBERRIES.id(), 20),
		new Item(ItemId.GRAIN.id(), 20), new Item(ItemId.COOKING_APPLE.id(), 20),
		new Item(ItemId.EGG.id(), 20), new Item(ItemId.MILK.id(), 20));

	@Override
	public Shop[] getShops(World world) {
		return new Shop[]{shop};
	}

	@Override
	public boolean isMembers() {
		return false;
	}

	@Override
	public Shop getShop() {
		return shop;
	}

	@Override
	public void onTalkNpc(Player player, Npc n) {
		npcsay(player, n, "Hello welcome to the chef's guild",
			"Only accomplished chefs and cooks are allowed in here",
			"Feel free to use any of our facilities");
		boolean canBuyCape = config().WANT_CUSTOM_QUESTS
			&& getMaxLevel(player, Skill.COOKING.id()) >= 99;
		int choice = canBuyCape
			? multi(player, n, true, "I'd like to buy cooking supplies", "I'd like to buy a cooking cape", "Thanks")
			: multi(player, n, true, "I'd like to buy cooking supplies", "Thanks");
		if (choice == 0) {
			player.setAccessingShop(shop);
			ActionSender.showShop(player, shop);
		} else if (canBuyCape && choice == 1) {
			npcsay(player, n, "Also for your skill level",
				"i can offer you cape",
				"to show all your skill of cooking",
				"the cost is 99,000 coins");
			int choice2 = multi(player, n, true, "I'll buy one", "Not at the moment");
			if (choice2 == 0) {
				if (player.getCarriedItems().getInventory().countId(ItemId.COINS.id()) >= 99000) {
					if (player.getCarriedItems().remove(new Item(ItemId.COINS.id(), 99000)) > -1) {
						give(player, ItemId.COOKING_CAPE.id(), 1);
						npcsay(player, n, "if you wear this cape while cooking",
							"you'll be able to cook much faster");
					}
				} else {
					npcsay(player, n, "come back with the money anytime");
				}
			}
		}
	}

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.HEAD_CHEF.id();
	}

}
