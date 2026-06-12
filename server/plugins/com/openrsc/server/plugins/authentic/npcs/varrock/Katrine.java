package com.openrsc.server.plugins.authentic.npcs.varrock;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.plugins.AbstractShop;

public final class Katrine extends AbstractShop {

	private final Shop shop = new Shop(false, 25000, 100, 55, 2,
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

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return false;
	}

	@Override
	public void onTalkNpc(final Player player, final Npc n) {
	}

	@Override
	public boolean blockOpNpc(Player player, Npc n, String command) {
		return n.getID() == NpcId.KATRINE.id()
			&& player.getConfig().RIGHT_CLICK_TRADE
			&& (command.equalsIgnoreCase("Trade") || command.equalsIgnoreCase("Shop"));
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
}
