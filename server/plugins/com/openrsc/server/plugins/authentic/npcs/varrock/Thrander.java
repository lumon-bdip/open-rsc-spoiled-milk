package com.openrsc.server.plugins.authentic.npcs.varrock;

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

public class Thrander extends AbstractShop {

	private final Shop shop = new Shop(false, 35000, 120, 55, 2,
		new Item(ItemId.TIN_PLATE_MAIL_BODY.id(), 2),
		new Item(ItemId.COPPER_PLATE_MAIL_BODY.id(), 2),
		new Item(ItemId.BRONZE_PLATE_MAIL_BODY.id(), 2),
		new Item(ItemId.IRON_PLATE_MAIL_BODY.id(), 1),
		new Item(ItemId.STEEL_PLATE_MAIL_BODY.id(), 1),
		new Item(ItemId.MITHRIL_PLATE_MAIL_BODY.id(), 1),
		new Item(ItemId.TIN_GAUNTLETS.id(), 3),
		new Item(ItemId.TIN_GREAVES.id(), 3),
		new Item(ItemId.COPPER_GAUNTLETS.id(), 3),
		new Item(ItemId.COPPER_GREAVES.id(), 3),
		new Item(ItemId.BRONZE_GAUNTLETS.id(), 3),
		new Item(ItemId.BRONZE_GREAVES.id(), 3),
		new Item(ItemId.IRON_GAUNTLETS.id(), 2),
		new Item(ItemId.IRON_GREAVES.id(), 2),
		new Item(ItemId.STEEL_GAUNTLETS.id(), 2),
		new Item(ItemId.STEEL_GREAVES.id(), 2),
		new Item(ItemId.MITHRIL_GAUNTLETS.id(), 1),
		new Item(ItemId.MITHRIL_GREAVES.id(), 1));

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.THRANDER.id();
	}

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
		npcsay(player, n, "Hello I'm Thrander the smith",
			"I sell practical armour for adventurers");
		int option = multi(player, n,
			"Do you want to trade?",
			"No thank you");
		if (option == 0) {
			npcsay(player, n, "Yes, I have a practical selection of armour");
			player.setAccessingShop(shop);
			ActionSender.showShop(player, shop);
		}
	}
}
