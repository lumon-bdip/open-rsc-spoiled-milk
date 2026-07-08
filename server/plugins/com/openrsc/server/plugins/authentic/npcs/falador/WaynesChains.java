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

import java.util.ArrayList;
import java.util.List;

import static com.openrsc.server.plugins.Functions.*;

public final class WaynesChains extends AbstractShop {

	private Shop shop = null;
	private static final int SPECIAL_ARMOUR_FEE = 500000;
	private static final int SCALE_MAIL_CHAIN_COST = 500;
	private static final int SCALE_MAIL_BODY_SCALE_COST = 150;
	private static final int SCALE_MAIL_LEGS_SCALE_COST = 100;
	private static final int PLATEBODY_BAR_COST = 4;

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

			List<String> options = new ArrayList<>();
			options.add("Yes please");
			options.add("No thanks");
			if (player.getCache().hasKey("miniquest_dwarf_youth_rescue")
			&& player.getCache().getInt("miniquest_dwarf_youth_rescue") == 2)
				options.add("I need your help with a special armour");
			String[] optionsArray = new String[options.size()];
			optionsArray = options.toArray(optionsArray);
			int option = multi(player, n, false, //do not send over
				optionsArray);
			if (option == 0) {
				say(player, n, "Yes Please");
				player.setAccessingShop(getShop(player.getWorld()));
				ActionSender.showShop(player, getShop(player.getWorld()));
			} else if (option == 1) {
				say(player, n, "No thanks");
			} else if (option == 2) {
				handleSpecialArmour(player, n);
			}
		}
	}

	private void handleSpecialArmour(final Player player, final Npc n) {
		say(player, n, "I need your help with a special armor");
		npcsay(player, n, "special you say? how can I help?");
		int option = multi(player, n, false,
			"Dragon scale mail body",
			"Dragon scale mail legs",
			"Dragon plate mail body",
			"Never mind");
		if (option == 0) {
			handleDragonScaleMail(player, n, ItemId.DRAGON_SCALE_MAIL.id(),
				"Gramat told me you can make a dragon scale mail body",
				"a dragon scale mail body", SCALE_MAIL_BODY_SCALE_COST);
		} else if (option == 1) {
			handleDragonScaleMail(player, n, ItemId.DRAGON_SCALE_MAIL_LEGS.id(),
				"Can you make me dragon scale mail legs",
				"dragon scale mail legs", SCALE_MAIL_LEGS_SCALE_COST);
		} else if (option == 2) {
			handleDragonPlateArmour(player, n, ItemId.DRAGON_PLATE_MAIL_BODY.id(),
				"Can you make me a dragon plate mail body",
				"a dragon plate mail body", PLATEBODY_BAR_COST);
		} else if (option == 3) {
			say(player, n, "Never mind");
		}
	}

	private void handleDragonScaleMail(final Player player, final Npc n, int itemId, String requestLine,
									   String itemName, int scaleCost) {
		say(player, n, requestLine);
		npcsay(player, n, "ah yes. I am able, but it's very difficult",
			"first, you need to bring me the materials",
			SCALE_MAIL_CHAIN_COST + " dragon metal chains",
			"and " + scaleCost + " chipped dragon scales");

		boolean hasChains = player.getCarriedItems().getInventory().countId(ItemId.DRAGON_METAL_CHAIN.id()) >= SCALE_MAIL_CHAIN_COST;
		boolean hasScales = player.getCarriedItems().getInventory().countId(ItemId.CHIPPED_DRAGON_SCALE.id()) >= scaleCost;
		if (hasScales) {
			say(player, n, "i have the scales here");
		} else {
			say(player, n, "i don't seem to have enough scales");
		}
		if (hasChains) {
			say(player, n, "i have the chains here");
		} else {
			say(player, n, "i don't seem to have enough chains");
		}
		if (!hasChains || !hasScales) {
			npcsay(player, n, "if you're able to gather them",
				"come see me again");
			return;
		}

		if (!confirmSpecialArmourFee(player, n)) {
			return;
		}
		player.getCarriedItems().remove(new Item(ItemId.CHIPPED_DRAGON_SCALE.id(), scaleCost));
		player.getCarriedItems().remove(new Item(ItemId.DRAGON_METAL_CHAIN.id(), SCALE_MAIL_CHAIN_COST));
		player.getCarriedItems().remove(new Item(ItemId.COINS.id(), SPECIAL_ARMOUR_FEE));
		completeSpecialArmour(player, n, itemId, itemName);
	}

	private void handleDragonPlateArmour(final Player player, final Npc n, int itemId, String requestLine,
										 String itemName, int barCost) {
		say(player, n, requestLine);
		npcsay(player, n, "i can, but platework of that strength is costly",
			"bring me " + barCost + " dragon bars",
			"and " + SPECIAL_ARMOUR_FEE + " gold pieces");

		if (player.getCarriedItems().getInventory().countId(ItemId.DRAGON_BAR.id()) < barCost) {
			say(player, n, "i don't seem to have enough bars");
			npcsay(player, n, "come back when you have the materials");
			return;
		}
		say(player, n, "i have the bars here");
		if (!confirmSpecialArmourFee(player, n)) {
			return;
		}
		player.getCarriedItems().remove(new Item(ItemId.DRAGON_BAR.id(), barCost));
		player.getCarriedItems().remove(new Item(ItemId.COINS.id(), SPECIAL_ARMOUR_FEE));
		completeSpecialArmour(player, n, itemId, itemName);
	}

	private boolean confirmSpecialArmourFee(final Player player, final Npc n) {
		npcsay(player, n, "great, you have the materials",
			"for my time I also require compensation",
			"how does " + SPECIAL_ARMOUR_FEE + " gold pieces sound");
		int option = multi(player, n, false, "sounds fair", "no way");
		if (option != 0) {
			say(player, n, "no way");
			npcsay(player, n, "suit yourself");
			return false;
		}
		say(player, n, "sounds fair");
		if (player.getCarriedItems().getInventory().countId(ItemId.COINS.id()) < SPECIAL_ARMOUR_FEE) {
			say(player, n, "but I'm short at the moment");
			npcsay(player, n, "get the money and return to me");
			return false;
		}
		return true;
	}

	private void completeSpecialArmour(final Player player, final Npc n, int itemId, String itemName) {
		player.message("you hand over the materials and money");
		delay(4);
		player.message("Wayne flashes a smile");
		delay(4);
		npcsay(player, n, "i happen to have one made already",
			"so there's no need for you to wait");
		player.message("Wayne hands you " + itemName);
		give(player, itemId, 1);
		say(player, n, "thanks");
		npcsay(player, n, "my pleasure " + player.getUsername(),
			"if you need my help again",
			"i'm always open");
		player.message("Congratulations! You have received " + itemName);
	}
}
