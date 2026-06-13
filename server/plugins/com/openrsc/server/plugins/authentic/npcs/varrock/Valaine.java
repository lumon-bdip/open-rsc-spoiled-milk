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

public final class Valaine extends AbstractShop {

	private final Shop valsShop = new Shop(false, 60000, 130, 40, 3,
		new Item(ItemId.BLUE_CAPE.id(), 2),
		new Item(ItemId.ORICHALCUM_LARGE_HELMET.id(), 1),
		new Item(ItemId.ORICHALCUM_GAUNTLETS.id(), 1),
		new Item(ItemId.ORICHALCUM_GREAVES.id(), 1),
		new Item(ItemId.ORICHALCUM_SQUARE_SHIELD.id(), 1),
		new Item(ItemId.ORICHALCUM_KITE_SHIELD.id(), 1),
		new Item(ItemId.ORICHALCUM_PLATE_MAIL_LEGS.id(), 1),
		new Item(ItemId.ORICHALCUM_PLATE_MAIL_BODY.id(), 1),
		new Item(ItemId.ORICHALCUM_DAGGER.id(), 1),
		new Item(ItemId.ORICHALCUM_SHORT_SWORD.id(), 1),
		new Item(ItemId.ORICHALCUM_LONG_SWORD.id(), 1),
		new Item(ItemId.ORICHALCUM_SCIMITAR.id(), 1),
		new Item(ItemId.ORICHALCUM_2_HANDED_SWORD.id(), 1),
		new Item(ItemId.ORICHALCUM_AXE.id(), 1),
		new Item(ItemId.ORICHALCUM_BATTLE_AXE.id(), 1),
		new Item(ItemId.ORICHALCUM_MACE.id(), 1),
		new Item(ItemId.ORICHALCUM_SPEAR.id(), 1),
		new Item(ItemId.ORICHALCUM_SCYTHE.id(), 1));

	@Override
	public boolean blockTalkNpc(final Player player, final Npc n) {
		return n.getID() == NpcId.VALAINE.id();
	}

	@Override
	public Shop[] getShops(World world) {
		return new Shop[]{valsShop};
	}

	@Override
	public boolean isMembers() {
		return false;
	}

	@Override
	public Shop getShop() {
		return valsShop;
	}

	@Override
	public void onTalkNpc(final Player player, final Npc n) {
		npcsay(player, n, "Hello there.",
			"Want to have a look at what we're selling today?");
		int opt = multi(player, n, false, //do not send over
			 "Yes please", "No thank you");
		if (opt == 0) {
			say(player, n, "Yes please.");
			player.setAccessingShop(valsShop);
			ActionSender.showShop(player, valsShop);
		}
	}
}
