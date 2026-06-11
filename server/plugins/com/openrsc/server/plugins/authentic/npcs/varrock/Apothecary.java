package com.openrsc.server.plugins.authentic.npcs.varrock;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.model.Shop;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.AbstractShop;
import com.openrsc.server.plugins.custom.minigames.ABoneToPick;
import com.openrsc.server.util.rsc.DataConversions;

import java.util.ArrayList;
import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;

public final class Apothecary extends AbstractShop {

	private final Shop shop = new Shop(false, 10000, 100, 70, 2,
		new Item(ItemId.VIAL.id(), 50),
		new Item(ItemId.PESTLE_AND_MORTAR.id(), 3),
		new Item(ItemId.GUAM_LEAF.id(), 10),
		new Item(ItemId.MARRENTILL.id(), 10),
		new Item(ItemId.TARROMIN.id(), 10),
		new Item(ItemId.HARRALANDER.id(), 10),
		new Item(ItemId.EYE_OF_NEWT.id(), 50),
		new Item(ItemId.UNICORN_HORN.id(), 10),
		new Item(ItemId.LIMPWURT_ROOT.id(), 10),
		new Item(ItemId.RED_SPIDERS_EGGS.id(), 10),
		new Item(ItemId.CHOCOLATE_BAR.id(), 20));

	@Override
	public void onTalkNpc(Player player, final Npc npc) {
		if (player.getQuestStage(Quests.ROMEO_N_JULIET) == 4) {
			say(player, npc, "Apothecary. Father Lawrence sent me",
				"I need some Cadava potion to help Romeo and Juliet");
			npcsay(player, npc, "Cadava potion. Its pretty nasty. And hard to make",
				"Wing of Rat, Tail of frog. Ear of snake and horn of dog",
				"I have all that, but I need some cadavaberries",
				"You will have to find them while I get the rest ready",
				"Bring them here when you have them. But be careful. They are nasty");
			player.updateQuestStage(Quests.ROMEO_N_JULIET, 5);
			return;
		} else if (player.getQuestStage(Quests.ROMEO_N_JULIET) == 5) {
			if (!player.getCarriedItems().hasCatalogID(ItemId.CADAVABERRIES.id())) {
				npcsay(player, npc, "Keep searching for the berries",
					"They are needed for the potion");
			} else {
				npcsay(player, npc, "Well done. You have the berries");
				mes("You hand over the berries");
				delay(3);
				player.getCarriedItems().remove(new Item(ItemId.CADAVABERRIES.id()));
				player.message("Which the apothecary shakes up in vial of strange liquid");
				npcsay(player, npc, "Here is what you need");
				player.message("The apothecary gives you a Cadava potion");
				player.getCarriedItems().getInventory().add(new Item(ItemId.CADAVA.id()));
				player.updateQuestStage(Quests.ROMEO_N_JULIET, 6);
			}
			return;
		}
		npcsay(player, npc, "I am the apothecary", "I have potions to brew. Do you need anything specific?");

		/*if (!getServer().getConfig().WANT_EXPERIENCE_ELIXIRS)
			option = showMenu(p, n, "Can you make a strength potion?",
				"Do you know a potion to make hair fall out?",
				"Have you got any good potions to give way?");
		else
			option = showMenu(p, n, "Can you make a strength potion?",
				"Do you know a potion to make hair fall out?",
				"Have you got any good potions to give way?",
				"Do you have any experience elixir?");*/

		// Disabled experience elixir due to not being functional at this time

		ArrayList<String> options = new ArrayList<String>();
		options.add("What are you selling?");
		options.add("Do you know a potion to make hair fall out?");
		options.add("Have you got any good potions to give way?");

		if (config().A_BONE_TO_PICK
			&& ABoneToPick.getStage(player) == ABoneToPick.TALKED_TO_ODDENSTEIN
			&& !ifheld(player, ItemId.CHIPPED_PESTLE_AND_MORTAR.id())) {
			options.add("Can I have a pestle and mortar?");
		}

		int option = multi(player, npc, options.toArray(new String[options.size()]));

		if (option == 0) {
			player.setAccessingShop(shop);
			ActionSender.showShop(player, shop);
		} else if (option == 1) {
			npcsay(player, npc, "I do indeed. I gave it to my mother. That's why I now live alone");
		} else if (option == 2) {
			if (player.getCarriedItems().hasCatalogID(ItemId.POTION.id(), Optional.of(false))) {
				npcsay(player, npc, "Only that spot cream. Hope you enjoy it",
					"Yes, ok. Try this potion");
				give(player, ItemId.POTION.id(), 1);
			} else {
				int chance = DataConversions.random(0, 2);
				if (chance < 2) {
					npcsay(player, npc, "Yes, ok. Try this potion");
					give(player, ItemId.POTION.id(), 1);
				} else {
					npcsay(player, npc, "Sorry, charity is not my strong point");
				}
			}
		} else if (option == 3 && config().A_BONE_TO_PICK && ABoneToPick.getStage(player) == ABoneToPick.TALKED_TO_ODDENSTEIN
			&& !ifheld(player, ItemId.CHIPPED_PESTLE_AND_MORTAR.id())) {
			ABoneToPick.apothecaryDialogue(player, npc);
		}
		/* } else if (option == 3 && config().WANT_EXPERIENCE_ELIXIRS) {
			npcsay(player, n, "Yes, it's my most mysterious and special elixir",
				"It has a strange taste and sure does give you a rush",
				"I would know..",
				"I sell it for 5,000gp");
			int menu = multi(player, n, "Yes please", "No thankyou");
			if (menu == 0) {
				long lastElixir = 0;
				if (player.getCache().hasKey("buy_elixir")) {
					lastElixir = player.getCache().getLong("buy_elixir");
				}
				if (System.currentTimeMillis() - lastElixir < 24 * 60 * 60 * 1000) {
					npcsay(player, n, "Wait.. it's you, I recently made an elixir for you",
						"I don't want to poison my customers",
						"You'll need to wait before I make you a new one");
					int time = (int) (86400 - ((System.currentTimeMillis() - lastElixir) / 1000));
					player.message("You need to wait: " + DataConversions.getDateFromMsec(time * 1000));
					return;
				}
				if (ifheld(player, ItemId.COINS.id(), 5000)) {
					say(player, n, "I have the 5,000 gold with me");
					player.message("you give Apothecary 5,000 gold");
					player.getCarriedItems().remove(new Item(ItemId.COINS.id(), 5000));
					mes("Apothecary: starts brewing and fixes to a elixir");
					delay(3);
					player.message("Apothecary gives you a mysterious experience elixir.");
					//TODO: Determine if elixir will be added and indexed ID if so
					//addItem(p, ItemId.EXPERIENCE_ELIXIR.id(), 1);
					player.getCache().store("buy_elixir", System.currentTimeMillis());
				} else {
					say(player, n, "Oops, I don't have enough coins");
					npcsay(player, n, "Ok. I need my money, the ingredients are hard to find");
				}
			}
		} */
	}

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.APOTHECARY.id();
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
