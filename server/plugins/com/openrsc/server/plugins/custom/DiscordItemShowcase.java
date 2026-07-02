package com.openrsc.server.plugins.custom;

import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.container.Equipment;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.DiscordWebhookClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DiscordItemShowcase {
	private static final ConcurrentMap<String, Long> LAST_SHOWCASE_BY_USERNAME = new ConcurrentHashMap<String, Long>();
	private static final int MAX_MATCHES_TO_SHOW = 5;

	private DiscordItemShowcase() {
	}

	public static void showItem(final Player player, final String[] args) {
		if (!player.getConfig().WANT_DISCORD_ITEM_SHOWCASE) {
			player.message("The Discord item showcase is not configured yet.");
			return;
		}

		final String query = String.join(" ", args).trim();
		if (query.isEmpty()) {
			player.message("Usage: ::showitem item name");
			return;
		}

		final MatchResult matchResult = resolveItem(player, query);
		if (matchResult.status == MatchStatus.NOT_FOUND) {
			player.message("No item matched: " + query);
			return;
		}
		if (matchResult.status == MatchStatus.AMBIGUOUS) {
			player.message("Multiple items matched. Try a more specific name: " + matchResult.message);
			return;
		}

		final ItemDefinition itemDef = matchResult.itemDef;
		final OwnedItem ownedItem = countOwnedItem(player, itemDef.getId());
		if (ownedItem.totalAmount <= 0) {
			player.message("You do not have " + itemDef.getName() + " to show off.");
			return;
		}

		final long now = System.currentTimeMillis();
		final String usernameKey = player.getUsername().toLowerCase();
		final long cooldownMillis = Math.max(0, player.getConfig().DISCORD_ITEM_SHOWCASE_COOLDOWN_SECONDS) * 1000L;
		final Long lastShowcase = LAST_SHOWCASE_BY_USERNAME.get(usernameKey);
		if (lastShowcase != null && now - lastShowcase < cooldownMillis) {
			final long secondsLeft = ((cooldownMillis - (now - lastShowcase)) + 999L) / 1000L;
			player.message("Please wait " + secondsLeft + " seconds before showing another item.");
			return;
		}

		final boolean posted = DiscordWebhookClient.sendItemShowcase(
			player.getConfig().DISCORD_ITEM_SHOWCASE_WEBHOOK_URL,
			player.getUsername(),
			itemDef.getName(),
			ownedItem.totalAmount,
			ownedItem.sourceLabel,
			itemDef.getId(),
			player.getCombatLevel(),
			player.getTotalLevel());

		if (!posted) {
			player.message("The Discord item showcase could not post right now.");
			return;
		}

		LAST_SHOWCASE_BY_USERNAME.put(usernameKey, now);
		player.message("Showed off " + formatAmount(ownedItem.totalAmount) + " " + itemDef.getName() + " in Discord.");
	}

	private static MatchResult resolveItem(final Player player, final String query) {
		if (query.matches("[0-9]+")) {
			try {
				final int itemId = Integer.parseInt(query);
				final ItemDefinition itemDef = player.getWorld().getServer().getEntityHandler().getItemDef(itemId);
				return itemDef == null ? MatchResult.notFound() : MatchResult.match(itemDef);
			} catch (NumberFormatException nfe) {
				return MatchResult.notFound();
			}
		}

		final String normalizedQuery = normalize(query);
		final List<ItemDefinition> exactMatches = new ArrayList<ItemDefinition>();
		final List<ItemDefinition> partialMatches = new ArrayList<ItemDefinition>();
		final int itemCount = player.getWorld().getServer().getEntityHandler().getItemCount();
		for (int itemId = 0; itemId < itemCount; itemId++) {
			final ItemDefinition itemDef = player.getWorld().getServer().getEntityHandler().getItemDef(itemId);
			if (itemDef == null || itemDef.getName() == null) {
				continue;
			}

			final String normalizedName = normalize(itemDef.getName());
			if (normalizedName.equals(normalizedQuery)) {
				exactMatches.add(itemDef);
			} else if (normalizedName.contains(normalizedQuery)) {
				partialMatches.add(itemDef);
			}
		}

		if (!exactMatches.isEmpty()) {
			return selectBestMatch(player, exactMatches);
		}
		if (!partialMatches.isEmpty()) {
			return selectBestMatch(player, partialMatches);
		}
		return MatchResult.notFound();
	}

	private static MatchResult selectBestMatch(final Player player, final List<ItemDefinition> matches) {
		final List<ItemDefinition> ownedMatches = new ArrayList<ItemDefinition>();
		for (ItemDefinition itemDef : matches) {
			if (countOwnedItem(player, itemDef.getId()).totalAmount > 0) {
				ownedMatches.add(itemDef);
			}
		}

		final List<ItemDefinition> candidates = ownedMatches.isEmpty() ? matches : ownedMatches;
		if (candidates.size() == 1) {
			return MatchResult.match(candidates.get(0));
		}

		return MatchResult.ambiguous(formatMatches(candidates));
	}

	private static String formatMatches(final List<ItemDefinition> matches) {
		final StringBuilder builder = new StringBuilder();
		final int limit = Math.min(matches.size(), MAX_MATCHES_TO_SHOW);
		for (int index = 0; index < limit; index++) {
			if (index > 0) {
				builder.append(", ");
			}
			final ItemDefinition itemDef = matches.get(index);
			builder.append(itemDef.getName()).append(" (").append(itemDef.getId()).append(")");
		}
		if (matches.size() > limit) {
			builder.append(", ...");
		}
		return builder.toString();
	}

	private static OwnedItem countOwnedItem(final Player player, final int itemId) {
		long inventoryAmount = player.getCarriedItems().getInventory().countId(itemId, Optional.<Boolean>empty());
		long equippedAmount = countEquippedItem(player, itemId);
		long bankAmount = player.getBank().countId(itemId);
		long totalAmount = inventoryAmount + equippedAmount + bankAmount;

		final List<String> sources = new ArrayList<String>();
		if (equippedAmount > 0) {
			sources.add("equipped");
		}
		if (inventoryAmount > 0) {
			sources.add("inventory");
		}
		if (bankAmount > 0) {
			sources.add("bank");
		}

		return new OwnedItem(totalAmount, sources.isEmpty() ? "unknown" : String.join(", ", sources));
	}

	private static long countEquippedItem(final Player player, final int itemId) {
		long count = 0;
		for (int slot = 0; slot < Equipment.SLOT_COUNT; slot++) {
			final Item item = player.getCarriedItems().getEquipment().get(slot);
			if (item != null && item.getCatalogId() == itemId) {
				count += Math.max(1, item.getAmount());
			}
		}
		return count;
	}

	private static String normalize(final String value) {
		return value.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
	}

	private static String formatAmount(final long amount) {
		return String.format("%,d", amount);
	}

	private enum MatchStatus {
		MATCH,
		NOT_FOUND,
		AMBIGUOUS
	}

	private static final class MatchResult {
		private final MatchStatus status;
		private final ItemDefinition itemDef;
		private final String message;

		private MatchResult(final MatchStatus status, final ItemDefinition itemDef, final String message) {
			this.status = status;
			this.itemDef = itemDef;
			this.message = message;
		}

		private static MatchResult match(final ItemDefinition itemDef) {
			return new MatchResult(MatchStatus.MATCH, itemDef, "");
		}

		private static MatchResult notFound() {
			return new MatchResult(MatchStatus.NOT_FOUND, null, "");
		}

		private static MatchResult ambiguous(final String message) {
			return new MatchResult(MatchStatus.AMBIGUOUS, null, message);
		}
	}

	private static final class OwnedItem {
		private final long totalAmount;
		private final String sourceLabel;

		private OwnedItem(final long totalAmount, final String sourceLabel) {
			this.totalAmount = totalAmount;
			this.sourceLabel = sourceLabel;
		}
	}
}
