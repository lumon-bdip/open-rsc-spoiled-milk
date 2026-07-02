package com.openrsc.server.plugins.authentic.npcs.lostcity;

import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;

import java.util.Optional;

public class FairyQueen implements TalkNpcTrigger {

	private static final int MARKET_TAX_WAIVER_DRAGONSTONES = 5;

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.FAIRY_QUEEN.id();
	}

	@Override
	public void onTalkNpc(Player player, Npc n) {
		if (n.getID() == NpcId.FAIRY_QUEEN.id()) {
			int menu = LostCityMarketAccess.hasDiamondTaxWaiver(player)
				? multi(player, n, "How do crops and such survive down here?",
					"What's so good about this place?")
				: multi(player, n, "How do crops and such survive down here?",
					"What's so good about this place?",
					"The diamond trading tax is annoying");
			if (menu == 0) {
				say(player, n, "Surely they need a bit of sunlight?");
				npcsay(player, n, "Clearly you come from a plane dependant on sunlight",
					"Down here the plants grow in the aura of faerie");
			} else if (menu == 1) {
				npcsay(player, n, "Zanaris is a meeting point of cultures",
					"those from many worlds converge here to exchange knowledge and goods");
			} else if (menu == 2) {
				handleMarketTaxWaiver(player, n);
			}
		}
	}

	private void handleMarketTaxWaiver(Player player, Npc n) {
		say(player, n, "Is there any way I can get around it?");
		npcsay(player, n, "I suppose if you brought me something more valuable than diamonds",
			"Bring me 5 dragonstones and I'll waive the tax");
		int menu = multi(player, n, "That's a deal", "That's even more absurd!");
		if (menu == 0) {
			int dragonstones = player.getCarriedItems().getInventory()
				.countId(ItemId.DRAGONSTONE.id(), Optional.of(false));
			if (dragonstones < MARKET_TAX_WAIVER_DRAGONSTONES) {
				npcsay(player, n, "You don't have enough it would seem. Come back when you do");
				return;
			}
			if (player.getCarriedItems().remove(new Item(ItemId.DRAGONSTONE.id(), MARKET_TAX_WAIVER_DRAGONSTONES)) == -1) {
				npcsay(player, n, "You don't have enough it would seem. Come back when you do");
				return;
			}
			player.message("You give the Fairy Queen 5 dragonstones");
			LostCityMarketAccess.waiveDiamondTax(player);
			npcsay(player, n, "Very well. The guards will let you into the market from now on");
		} else if (menu == 1) {
			say(player, n, "Do you have any idea how hard those are to come by?");
			npcsay(player, n, "That's exactly the point. Take it or leave it");
		}
	}
}
