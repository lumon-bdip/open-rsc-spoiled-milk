package com.openrsc.server.plugins.authentic.minigames.mage_arena;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.AbstractShop;

import static com.openrsc.server.plugins.Functions.*;

public class Chamber_Guardian extends AbstractShop {

	private final Shop shop = new Shop(false, 60000 * 5, 100, 60, 2,
		new Item(ItemId.STAFF_OF_ELEMENTS.id(), 5),
		new Item(ItemId.STAFF_OF_POWER.id(), 5),
		new Item(ItemId.STAFF_OF_ENLIGHTENMENT.id(), 5));

	@Override
	public void onTalkNpc(Player player, Npc n) {
		if (player.getCache().hasKey("mage_arena")
			&& (player.getCache().getInt("mage_arena") == 2 || player.getCache().getInt("mage_arena") == 3)) {
			say(player, n, "hello my friend, kolodion sent me down");
			npcsay(player, n,
				"then you have earned one of his battle mage staves",
				"claim your first staff from one of the three stones",
				"after that, i can sell you the others");
		} else if (player.getCache().hasKey("mage_arena") && player.getCache().getInt("mage_arena") == 4) {
			say(player, n, "hello again");
			npcsay(player, n, "hello adventurer, are you looking for another battle mage staff?");
			int choice = multi(player, n, "what do you have to offer?", "no thanks", "tell me about the battle mages?");
			if (choice == 0) {
				npcsay(player, n, "take a look");
				player.setAccessingShop(shop);
				ActionSender.showShop(player, shop);
			} else if (choice == 1) {
				npcsay(player, n, "well, let me know if you need one");
			} else if (choice == 2) {
				npcsay(player, n, "the battle mages no longer teach special spell drills",
					"now they test raw elemental magic",
					"use them as practice if you want a harder arena fight");
				say(player, n, "good stuff");
			}
		} else {
			npcsay(player, n, "hello adventurer, has kolodion tested you yet?");
			say(player, n, "no, not yet.");
			npcsay(player, n, "defeat him, and one of his battle mage staves can be yours");
		}
	}

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.CHAMBER_GUARDIAN.id();
	}

	@Override
	public Shop[] getShops(World world) {
		return new Shop[]{shop};
	}

	@Override
	public boolean isMembers() {
		return true;
	}

	@Override
	public Shop getShop() {
		return shop;
	}
}
