package com.openrsc.server.plugins.shared;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Group;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.PlayerLoginTrigger;

import java.util.Optional;

public class PlayerLogin implements PlayerLoginTrigger {
	private static final String ANACTUALDUCK_REWARD_BACKFILL = "myworld_anactualduck_quest_reward_backfill_20260531";

	@Override
	public void onPlayerLogin(Player player) {
		applyMyWorldStaffGroups(player);
		backfillAnactualduckQuestRewards(player);
	}

	@Override
	public boolean blockPlayerLogin(Player player) {
		return false;
	}

	private void applyMyWorldStaffGroups(final Player player) {
		if ("devduck".equalsIgnoreCase(player.getUsername())) {
			if (player.getGroupID() != Group.OWNER && player.getGroupID() != Group.ADMIN && player.getGroupID() != Group.DEV) {
				player.setGroupID(Group.DEV);
			}
		} else if ("anactualduck".equalsIgnoreCase(player.getUsername())) {
			if (player.getGroupID() != Group.MOD) {
				player.setGroupID(Group.MOD);
			}
		}
	}

	private void backfillAnactualduckQuestRewards(final Player player) {
		if (!"anactualduck".equalsIgnoreCase(player.getUsername())
			|| player.getCache().hasKey(ANACTUALDUCK_REWARD_BACKFILL)) {
			return;
		}

		ifCompletedGive(player, Quests.BLACK_KNIGHTS_FORTRESS, ItemId.COINS.id(), 2500);
		ifCompletedEnsure(player, Quests.DEMON_SLAYER, ItemId.SILVERLIGHT.id(), 1);
		ifCompletedGive(player, Quests.DORICS_QUEST, ItemId.COINS.id(), 180);
		ifCompletedEnsure(player, Quests.THE_RESTLESS_GHOST, ItemId.AMULET_OF_GHOSTSPEAK.id(), 1);
		ifCompletedGive(player, Quests.GOBLIN_DIPLOMACY, ItemId.GOLD_BAR.id(), 1);
		ifCompletedGive(player, Quests.ERNEST_THE_CHICKEN, ItemId.COINS.id(), 300);
		ifCompletedGive(player, Quests.PIRATES_TREASURE, ItemId.COINS.id(), 450);
		ifCompletedGive(player, Quests.PIRATES_TREASURE, ItemId.GOLD_RING.id(), 1);
		ifCompletedGive(player, Quests.PIRATES_TREASURE, ItemId.EMERALD.id(), 1);
		ifCompletedGive(player, Quests.PRINCE_ALI_RESCUE, ItemId.COINS.id(), 700);
		ifCompletedGive(player, Quests.SHEEP_SHEARER, ItemId.COINS.id(), 180);
		ifCompletedEnsure(player, Quests.DRAGON_SLAYER, ItemId.ANTI_DRAGON_BREATH_SHIELD.id(), 1);
		ifCompletedEnsure(player, Quests.LOST_CITY, ItemId.DRAMEN_STAFF.id(), 1);
		ifCompletedEnsure(player, Quests.MERLINS_CRYSTAL, ItemId.EXCALIBUR.id(), 1);
		ifCompletedGive(player, Quests.TRIBAL_TOTEM, ItemId.SWORDFISH.id(), 5);
		ifCompletedGive(player, Quests.CLOCK_TOWER, ItemId.COINS.id(), 500);
		ifCompletedGive(player, Quests.FIGHT_ARENA, ItemId.COINS.id(), 1000);
		ifCompletedGive(player, Quests.GERTRUDES_CAT, ItemId.CHOCOLATE_CAKE.id(), 1);
		ifCompletedGive(player, Quests.GERTRUDES_CAT, ItemId.STEW.id(), 1);
		ifCompletedGive(player, Quests.THE_HAZEEL_CULT, ItemId.COINS.id(), 5);

		player.getCache().store(ANACTUALDUCK_REWARD_BACKFILL, true);
	}

	private void ifCompletedGive(final Player player, final int questId, final int itemId, final int amount) {
		if (player.getQuestStage(questId) == Quests.QUEST_STAGE_COMPLETED) {
			giveOrBank(player, itemId, amount);
		}
	}

	private void ifCompletedEnsure(final Player player, final int questId, final int itemId, final int amount) {
		if (player.getQuestStage(questId) == Quests.QUEST_STAGE_COMPLETED && !hasAnywhere(player, itemId)) {
			giveOrBank(player, itemId, amount);
		}
	}

	private boolean hasAnywhere(final Player player, final int itemId) {
		return player.getCarriedItems().hasCatalogID(itemId, Optional.empty())
			|| player.getBank().countId(itemId) > 0;
	}

	private void giveOrBank(final Player player, final int itemId, final int amount) {
		final Item item = new Item(itemId, amount);
		if (player.getCarriedItems().getInventory().canHold(item)) {
			player.getCarriedItems().getInventory().add(item);
		} else {
			player.getBank().add(item, false);
		}
	}
}
