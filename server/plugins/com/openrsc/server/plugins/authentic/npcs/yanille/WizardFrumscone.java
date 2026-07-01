package com.openrsc.server.plugins.authentic.npcs.yanille;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.NpcId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.OpNpcTrigger;
import com.openrsc.server.plugins.triggers.TalkNpcTrigger;

import static com.openrsc.server.plugins.Functions.*;

public class WizardFrumscone implements TalkNpcTrigger, OpNpcTrigger {

	private static final String GIVE_ZOMBIE_EYES = "Give zombie eyes";
	private static final String GIVE_SCALES = "Give scales";
	private static final String MAGIC_CAPE_OPTION = "Does your cape have any magical properties?";
	private static final String BLUE_SCALE_OPTION = "I brought you blue dragon scales";
	private static final String ZOMBIE_EYE_OPTION = "I brought you zombie eyes";
	private static final String LEAVE_OPTION = "I was going to kill them with or without your permission";

	@Override
	public boolean blockTalkNpc(Player player, Npc n) {
		return n.getID() == NpcId.WIZARD_FRUMSCONE.id();
	}

	@Override
	public boolean blockOpNpc(Player player, Npc n, String command) {
		return n.getID() == NpcId.WIZARD_FRUMSCONE.id()
			&& (GIVE_ZOMBIE_EYES.equalsIgnoreCase(command) || GIVE_SCALES.equalsIgnoreCase(command));
	}

	@Override
	public void onOpNpc(Player player, Npc n, String command) {
		if (GIVE_ZOMBIE_EYES.equalsIgnoreCase(command)) {
			tradeForStone(player, n, ItemId.ZOMBIE_EYE.id(), 2);
		} else if (GIVE_SCALES.equalsIgnoreCase(command)) {
			tradeForStone(player, n, ItemId.BLUE_DRAGON_SCALE.id(), 3);
		}
	}

	@Override
	public void onTalkNpc(Player player, Npc n) {
		if (n.getID() == NpcId.WIZARD_FRUMSCONE.id()) {
			npcsay(player, n, "Do you like my magic zombies and baby blue dragons",
				"Feel free to kill them",
				"Theres plenty more where these came from",
				"If you bring me blue dragon scales or zombie eyes",
				"I'll trade each zombie eye for 2 stone",
				"And each blue dragon scale for 3 stone");

			boolean canBuyMagicCape = config().WANT_CUSTOM_SPRITES
				&& getMaxLevel(player, Skill.MAGIC.id()) >= 99;
			int option = canBuyMagicCape
				? multi(player, n, MAGIC_CAPE_OPTION, BLUE_SCALE_OPTION, ZOMBIE_EYE_OPTION, LEAVE_OPTION)
				: multi(player, n, BLUE_SCALE_OPTION, ZOMBIE_EYE_OPTION, LEAVE_OPTION);

			if (option == -1 || LEAVE_OPTION.equals(getSelectedOption(canBuyMagicCape, option))) {
				return;
			}

			String selectedOption = getSelectedOption(canBuyMagicCape, option);
			if (MAGIC_CAPE_OPTION.equals(selectedOption)) {
				handleMagicCape(player, n);
			} else if (BLUE_SCALE_OPTION.equals(selectedOption)) {
				tradeForStone(player, n, ItemId.BLUE_DRAGON_SCALE.id(), 3);
			} else if (ZOMBIE_EYE_OPTION.equals(selectedOption)) {
				tradeForStone(player, n, ItemId.ZOMBIE_EYE.id(), 2);
			}
		}
	}

	private String getSelectedOption(boolean canBuyMagicCape, int option) {
		if (canBuyMagicCape) {
			switch (option) {
				case 0:
					return MAGIC_CAPE_OPTION;
				case 1:
					return BLUE_SCALE_OPTION;
				case 2:
					return ZOMBIE_EYE_OPTION;
				case 3:
					return LEAVE_OPTION;
				default:
					return "";
			}
		}

		switch (option) {
			case 0:
				return BLUE_SCALE_OPTION;
			case 1:
				return ZOMBIE_EYE_OPTION;
			case 2:
				return LEAVE_OPTION;
			default:
				return "";
		}
	}

	private void handleMagicCape(Player player, Npc n) {
		npcsay(player, n, "Yes it does",
			"Only masters of magic can harness its power",
			"It seems that you are ready for such power",
			"It will only cost you 99,000 coins.");
		if (multi(player, n, "I am ready", "I am not ready") == 0) {
			if (player.getCarriedItems().getInventory().countId(ItemId.COINS.id()) >= 99000) {
				mes("Wizard Frumscone takes your coins");
				delay(3);
				if (player.getCarriedItems().remove(new Item(ItemId.COINS.id(), 99000)) > -1) {
					mes("And hands you a Magic cape");
					delay(3);
					give(player, ItemId.MAGIC_CAPE.id(), 1);
					npcsay(player, n, "You have now been bestowed with great power",
						"This cape will allow you to cast some spells without using runes");
				}
			} else {
				npcsay(player, n, "You do not have enough coins to unlock your full power");
			}
		}
	}

	private void tradeForStone(Player player, Npc n, int itemId, int stonePerItem) {
		int itemCount = player.getCarriedItems().getInventory().countId(itemId);
		if (itemCount <= 0) {
			npcsay(player, n, "You don't have any of those for me");
			return;
		}

		if (player.getCarriedItems().remove(new Item(itemId, itemCount)) == -1) {
			npcsay(player, n, "Something went wrong while taking those");
			return;
		}

		int stoneCount = itemCount * stonePerItem;
		int carriedStone = Math.min(stoneCount, player.getCarriedItems().getInventory().getFreeSlots());
		int droppedStone = stoneCount - carriedStone;
		if (carriedStone > 0) {
			give(player, ItemId.RUNE_STONE.id(), carriedStone);
		}
		dropStoneOverflow(player, droppedStone);

		if (droppedStone > 0) {
			npcsay(player, n, "Thank you, here's " + stoneCount + " stone in return",
				"Some of it fell to the ground because you had no room");
		} else {
			npcsay(player, n, "Thank you, here's " + stoneCount + " stone in return");
		}
	}

	private void dropStoneOverflow(Player player, int amount) {
		for (int i = 0; i < amount; i++) {
			player.getWorld().registerItem(new GroundItem(player.getWorld(), ItemId.RUNE_STONE.id(), player.getX(), player.getY(), 1, player));
		}
	}
}
