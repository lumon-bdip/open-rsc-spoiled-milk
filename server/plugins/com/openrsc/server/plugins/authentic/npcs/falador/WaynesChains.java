package com.openrsc.server.plugins.authentic.npcs.falador;

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

public final class WaynesChains extends AbstractShop {

	private Shop shop = null;

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return n.getID() == NpcId.WAYNE.id();
	}

	@Override
	public Shop[] getShops(World world) {
		return new Shop[]{getShop(world)};
	}

	@Override
	public boolean isMembers() {
		return false;
	}

	@Override
	public Shop getShop() {
		return shop;
	}

	public Shop getShop(World world) {
		if(shop == null) {
			shop = new Shop(false, 25000, 100, 65, 1,
				new Item(ItemId.TIN_THROWING_DART.id(), 1000),
				new Item(ItemId.COPPER_THROWING_DART.id(), 1000),
				new Item(ItemId.BRONZE_THROWING_DART.id(), 1000),
				new Item(ItemId.IRON_THROWING_DART.id(), 1000),
				new Item(ItemId.STEEL_THROWING_DART.id(), 1000),
				new Item(ItemId.MITHRIL_THROWING_DART.id(), 1000),
				new Item(ItemId.TIN_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.COPPER_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.BRONZE_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.IRON_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.STEEL_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.MITHRIL_THROWING_KNIFE.id(), 1000),
				new Item(ItemId.TIN_SHURIKEN.id(), 1000),
				new Item(ItemId.COPPER_SHURIKEN.id(), 1000),
				new Item(ItemId.BRONZE_SHURIKEN.id(), 1000),
				new Item(ItemId.IRON_SHURIKEN.id(), 1000),
				new Item(ItemId.STEEL_SHURIKEN.id(), 1000),
				new Item(ItemId.MITHRIL_SHURIKEN.id(), 1000));
		}

		return shop;
	}

	@Override
	public void onTalkNpc(final Player player, final Npc n) {
		if (n.getID() == NpcId.WAYNE.id()) {
			npcsay(player, n, "I've decided to pivot my business to Throwing weapons!",
				"Care to try some darts or shuriken?");

			int option = multi(player, n, false, //do not send over
				"Yes please",
				"No thanks");
			if (option == 0) {
				say(player, n, "Yes Please");
				player.setAccessingShop(getShop(player.getWorld()));
				ActionSender.showShop(player, getShop(player.getWorld()));
			} else if (option == 1) {
				say(player, n, "No thanks");
			}
		}
	}
}
