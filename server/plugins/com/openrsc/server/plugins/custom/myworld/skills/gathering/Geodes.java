package com.openrsc.server.plugins.custom.myworld.skills.gathering;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.triggers.OpInvTrigger;
import com.openrsc.server.plugins.triggers.UseInvTrigger;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.*;

public class Geodes implements OpInvTrigger, UseInvTrigger {

	private static final int[] STANDARD_GEMS = {
		ItemId.UNCUT_SAPPHIRE.id(),
		ItemId.UNCUT_EMERALD.id(),
		ItemId.UNCUT_RUBY.id(),
		ItemId.UNCUT_DIAMOND.id()
	};
	private static final int[] STANDARD_GEM_WEIGHTS = {35, 18, 12, 6};
	private static final int[] RUNE_REWARDS = {
		ItemId.AIR_RUNE.id(),
		ItemId.WATER_RUNE.id(),
		ItemId.EARTH_RUNE.id(),
		ItemId.FIRE_RUNE.id(),
		ItemId.MIND_RUNE.id(),
		ItemId.BODY_RUNE.id(),
		ItemId.COSMIC_RUNE.id(),
		ItemId.CHAOS_RUNE.id(),
		ItemId.NATURE_RUNE.id(),
		ItemId.LAW_RUNE.id(),
		ItemId.DEATH_RUNE.id(),
		ItemId.SOUL_RUNE.id(),
		ItemId.BLOOD_RUNE.id(),
		ItemId.LIFE_RUNE.id()
	};
	private static final int[] RUNE_WEIGHTS = {80, 80, 80, 80, 60, 55, 35, 28, 22, 18, 10, 6, 4, 8};

	@Override
	public boolean blockOpInv(Player player, Integer invIndex, Item item, String command) {
		return player.getConfig().WANT_MYWORLD
			&& isGeode(item.getCatalogId())
			&& "open".equalsIgnoreCase(command);
	}

	@Override
	public void onOpInv(Player player, Integer invIndex, Item item, String command) {
		if (!blockOpInv(player, invIndex, item, command)) {
			return;
		}
		player.playerServerMessage(MessageType.QUEST, "Use a chisel to crack this open.");
	}

	@Override
	public boolean blockUseInv(Player player, Integer invIndex, Item item1, Item item2) {
		return player.getConfig().WANT_MYWORLD
			&& isChisel(item1.getCatalogId(), item2.getCatalogId())
			&& (isGeode(item1.getCatalogId()) || isGeode(item2.getCatalogId()));
	}

	@Override
	public void onUseInv(Player player, Integer invIndex, Item item1, Item item2) {
		if (!blockUseInv(player, invIndex, item1, item2)) {
			return;
		}
		Item geode = isGeode(item1.getCatalogId()) ? item1 : item2;
		GeodeSize size = GeodeSize.forItem(geode.getCatalogId());
		if (size == null) {
			return;
		}
		startbatchunlimited();
		batchOpenGeode(player, size);
	}

	private void batchOpenGeode(Player player, GeodeSize size) {
		if (!hasChisel(player)) {
			stopbatch();
			return;
		}
		ActionSender.sendActionProgressBar(player, ItemId.CHISEL.id(), 3);
		delay(3);
		if (ifinterrupted()) {
			return;
		}
		if (player.getCarriedItems().remove(new Item(size.itemId, 1)) == -1) {
			stopbatch();
			return;
		}
		player.playerServerMessage(MessageType.QUEST, "You crack the geode open...");
		GeodeReward reward = rollReward(size);
		grantReward(player, reward);
		player.playerServerMessage(MessageType.QUEST, "There was " + reward.description + " inside!");

		updatebatch();
		if (!ifinterrupted()
			&& !isbatchcomplete()
			&& player.getCarriedItems().getInventory().countId(size.itemId, Optional.of(false)) > 0
			&& hasChisel(player)) {
			batchOpenGeode(player, size);
		} else {
			stopbatch();
		}
	}

	private GeodeReward rollReward(GeodeSize size) {
		int roll = DataConversions.random(1, size.totalWeight);
		for (RewardKind kind : RewardKind.values()) {
			int weight = kind.weights[size.ordinal()];
			if (weight <= 0) {
				continue;
			}
			roll -= weight;
			if (roll <= 0) {
				return buildReward(size, kind);
			}
		}
		return buildReward(size, RewardKind.STONE);
	}

	private GeodeReward buildReward(GeodeSize size, RewardKind kind) {
		switch (kind) {
			case MINING_XP:
				return GeodeReward.xp(Skill.MINING.id(), DataConversions.random(size.minXp, size.maxXp),
					"some Mining experience");
			case CRAFTING_XP:
				return GeodeReward.xp(Skill.CRAFTING.id(), DataConversions.random(size.minXp, size.maxXp),
					"some Crafting experience");
			case COINS:
				return GeodeReward.item(ItemId.COINS.id(), DataConversions.random(size.minCoins, size.maxCoins),
					"some coins");
			case GEM:
				int gemId = weightedRandom(STANDARD_GEMS, STANDARD_GEM_WEIGHTS);
				int gemQuantity = getGemQuantity(size, gemId);
				return GeodeReward.item(gemId, gemQuantity,
					gemQuantity == 1 ? article(itemName(gemId)) : gemQuantity + " " + itemName(gemId) + "s");
			case RUNES:
				int runeId = weightedRandom(RUNE_REWARDS, RUNE_WEIGHTS);
				return GeodeReward.item(runeId, getRuneQuantity(size, runeId),
					"some runes");
			case KEY_HALVES:
				return GeodeReward.keyHalves(DataConversions.random(size.minKeyHalves, size.maxKeyHalves));
			case STONE:
			default:
				return GeodeReward.notedItem(ItemId.RUNE_STONE.id(),
					DataConversions.random(size.minStone, size.maxStone), "noted stone");
		}
	}

	private void grantReward(Player player, GeodeReward reward) {
		if (reward.skillId >= 0) {
			player.incExp(reward.skillId, reward.amount, true);
			return;
		}
		if (reward.keyHalves) {
			for (int i = 0; i < reward.amount; i++) {
				int keyHalf = DataConversions.random(0, 1) == 0 ? ItemId.TOOTH_KEY_HALF.id() : ItemId.LOOP_KEY_HALF.id();
				addOrDrop(player, new Item(keyHalf, 1));
			}
			return;
		}
		addOrDrop(player, new Item(reward.itemId, reward.amount, reward.noted));
	}

	private void addOrDrop(Player player, Item item) {
		int freeSlots = player.getCarriedItems().getInventory().getFreeSlots();
		boolean stacks = item.getDef(player.getWorld()).isStackable() || item.getNoted();
		if (stacks || item.getAmount() <= freeSlots) {
			player.getCarriedItems().getInventory().add(item);
			return;
		}
		int inventoryAmount = Math.max(0, freeSlots);
		for (int i = 0; i < inventoryAmount; i++) {
			player.getCarriedItems().getInventory().add(new Item(item.getCatalogId(), 1, item.getNoted()));
		}
		for (int i = inventoryAmount; i < item.getAmount(); i++) {
			player.getWorld().registerItem(new GroundItem(player.getWorld(), item.getCatalogId(),
				player.getX(), player.getY(), 1, player, item.getNoted()));
		}
	}

	private static boolean isChisel(int item1, int item2) {
		return item1 == ItemId.CHISEL.id()
			|| item2 == ItemId.CHISEL.id()
			|| item1 == ItemId.SUPERCHISEL.id()
			|| item2 == ItemId.SUPERCHISEL.id();
	}

	private static boolean isGeode(int itemId) {
		return GeodeSize.forItem(itemId) != null;
	}

	private static boolean hasChisel(Player player) {
		return player.getCarriedItems().getInventory().countId(ItemId.CHISEL.id(), Optional.of(false)) > 0
			|| player.getCarriedItems().getInventory().countId(ItemId.SUPERCHISEL.id(), Optional.of(false)) > 0;
	}

	private static int getRuneQuantity(GeodeSize size, int runeId) {
		int altarLevel = getRuneAltarLevel(runeId);
		int hugeAverage = 400 - ((Math.max(1, altarLevel) - 1) * 360 / 69);
		int average = Math.max(1, (int) Math.round(hugeAverage * size.runeQuantityMultiplier));
		int minimum = Math.max(1, average * 3 / 4);
		int maximum = Math.max(minimum, average * 5 / 4);
		return DataConversions.random(minimum, maximum);
	}

	private static int getRuneAltarLevel(int runeId) {
		switch (ItemId.getById(runeId)) {
			case MIND_RUNE:
				return 8;
			case BODY_RUNE:
				return 15;
			case CHAOS_RUNE:
				return 22;
			case COSMIC_RUNE:
				return 30;
			case NATURE_RUNE:
				return 38;
			case LAW_RUNE:
				return 46;
			case DEATH_RUNE:
				return 54;
			case SOUL_RUNE:
				return 62;
			case BLOOD_RUNE:
				return 70;
			default:
				return 1;
		}
	}

	private static int getGemQuantity(GeodeSize size, int gemId) {
		int baseQuantity;
		switch (ItemId.getById(gemId)) {
			case UNCUT_SAPPHIRE:
				baseQuantity = 10;
				break;
			case UNCUT_EMERALD:
				baseQuantity = 7;
				break;
			case UNCUT_RUBY:
				baseQuantity = 5;
				break;
			case UNCUT_DIAMOND:
			default:
				baseQuantity = 3;
				break;
		}
		int average = Math.max(1, (int) Math.round(baseQuantity * size.gemQuantityMultiplier));
		int minimum = Math.max(1, average - Math.max(1, average / 4));
		int maximum = average + Math.max(1, average / 4);
		return DataConversions.random(minimum, maximum);
	}

	private static int weightedRandom(int[] ids, int[] weights) {
		int total = 0;
		for (int weight : weights) {
			total += weight;
		}
		int roll = DataConversions.random(1, total);
		for (int i = 0; i < ids.length; i++) {
			roll -= weights[i];
			if (roll <= 0) {
				return ids[i];
			}
		}
		return ids[0];
	}

	private static String itemName(int itemId) {
		if (itemId == ItemId.UNCUT_SAPPHIRE.id()) return "uncut sapphire";
		if (itemId == ItemId.UNCUT_EMERALD.id()) return "uncut emerald";
		if (itemId == ItemId.UNCUT_RUBY.id()) return "uncut ruby";
		if (itemId == ItemId.UNCUT_DIAMOND.id()) return "uncut diamond";
		return "gem";
	}

	private static String article(String text) {
		return text.matches("(?i)^[aeiou].*") ? "an " + text : "a " + text;
	}

	private enum RewardKind {
		MINING_XP(new int[] {18, 18, 18, 18}),
		CRAFTING_XP(new int[] {12, 14, 16, 18}),
		COINS(new int[] {24, 22, 20, 18}),
		GEM(new int[] {18, 18, 18, 18}),
		RUNES(new int[] {14, 16, 18, 18}),
		STONE(new int[] {14, 10, 8, 6}),
		KEY_HALVES(new int[] {0, 1, 1, 1});

		private final int[] weights;

		RewardKind(int[] weights) {
			this.weights = weights;
		}
	}

	private enum GeodeSize {
		SMALL(ItemId.SMALL_GEODE.id(), 1500, 4000, 144, 360, 25, 50, 0.10D, 0.25D, 0, 0),
		STANDARD(ItemId.STANDARD_GEODE.id(), 4000, 9000, 960, 2160, 50, 100, 0.25D, 0.50D, 1, 1),
		LARGE(ItemId.LARGE_GEODE.id(), 9000, 18000, 4560, 7800, 100, 200, 0.50D, 0.75D, 1, 2),
		HUGE(ItemId.HUGE_GEODE.id(), 18000, 35000, 9000, 15000, 200, 400, 1.00D, 1.00D, 1, 3);

		private final int itemId;
		private final int minXp;
		private final int maxXp;
		private final int minCoins;
		private final int maxCoins;
		private final int minStone;
		private final int maxStone;
		private final double runeQuantityMultiplier;
		private final double gemQuantityMultiplier;
		private final int minKeyHalves;
		private final int maxKeyHalves;
		private final int totalWeight;

		GeodeSize(int itemId, int minXp, int maxXp, int minCoins, int maxCoins, int minStone, int maxStone,
				  double runeQuantityMultiplier, double gemQuantityMultiplier,
				  int minKeyHalves, int maxKeyHalves) {
			this.itemId = itemId;
			this.minXp = minXp;
			this.maxXp = maxXp;
			this.minCoins = minCoins;
			this.maxCoins = maxCoins;
			this.minStone = minStone;
			this.maxStone = maxStone;
			this.runeQuantityMultiplier = runeQuantityMultiplier;
			this.gemQuantityMultiplier = gemQuantityMultiplier;
			this.minKeyHalves = minKeyHalves;
			this.maxKeyHalves = maxKeyHalves;
			int weight = 0;
			for (RewardKind kind : RewardKind.values()) {
				weight += kind.weights[ordinal()];
			}
			this.totalWeight = weight;
		}

		private static GeodeSize forItem(int itemId) {
			for (GeodeSize size : values()) {
				if (size.itemId == itemId) {
					return size;
				}
			}
			return null;
		}
	}

	private static final class GeodeReward {
		private final int itemId;
		private final int amount;
		private final boolean noted;
		private final int skillId;
		private final boolean keyHalves;
		private final String description;

		private GeodeReward(int itemId, int amount, boolean noted, int skillId, boolean keyHalves, String description) {
			this.itemId = itemId;
			this.amount = amount;
			this.noted = noted;
			this.skillId = skillId;
			this.keyHalves = keyHalves;
			this.description = description;
		}

		private static GeodeReward item(int itemId, int amount, String description) {
			return new GeodeReward(itemId, amount, false, -1, false, description);
		}

		private static GeodeReward notedItem(int itemId, int amount, String description) {
			return new GeodeReward(itemId, amount, true, -1, false, description);
		}

		private static GeodeReward xp(int skillId, int amount, String description) {
			return new GeodeReward(ItemId.NOTHING.id(), amount, false, skillId, false, description);
		}

		private static GeodeReward keyHalves(int amount) {
			return new GeodeReward(ItemId.NOTHING.id(), amount, false, -1, true,
				amount == 1 ? "a key half" : amount + " key halves");
		}
	}
}
