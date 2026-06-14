package com.openrsc.server.plugins.custom.npcs;

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

public final class RangersGuildDragonShop extends AbstractShop {

	private final Shop shop = new Shop(false, 60000, 100, 55, 3,
		new Item(ItemId.DRAGON_LONGBOW.id(), 1),
		new Item(ItemId.DRAGON_CROSSBOW.id(), 1),
		new Item(ItemId.DRAGON_ARROWS.id(), 1000),
		new Item(ItemId.POISON_DRAGON_ARROWS.id(), 1000),
		new Item(ItemId.DRAGON_BOLTS.id(), 1000),
		new Item(ItemId.POISON_DRAGON_BOLTS.id(), 1000));

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return n.getID() == NpcId.RANGERS_GUILD_DRAGON_VENDOR.id();
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
	public void onTalkNpc(final Player player, final Npc n) {
		npcsay(player, n, "Welcome to the Rangers Guild specialist shop",
			"Can I interest you in dragon ranged gear?");

		int option = multi(player, n, false,
			"Yes please",
			"No thank you");
		if (option == 0) {
			say(player, n, "Yes please");
			player.setAccessingShop(shop);
			ActionSender.showShop(player, shop);
		} else if (option == 1) {
			say(player, n, "No thank you");
		}
	}
}
