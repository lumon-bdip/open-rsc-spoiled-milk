package com.openrsc.server.plugins.custom.misc;

import com.openrsc.server.constants.SceneryId;
import com.openrsc.server.external.ItemDefinition;
import com.openrsc.server.model.container.Equipment;
import com.openrsc.server.model.container.Inventory;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.shared.EntranaRestrictions;
import com.openrsc.server.plugins.triggers.OpLocTrigger;

import java.util.ArrayList;
import java.util.List;

public final class EntranaSafetyDepositBox implements OpLocTrigger {
	private static final String CACHE_KEY = "entrana_safety_deposit_box";
	private static final String SNAPSHOT_VERSION = "v1";

	@Override
	public void onOpLoc(Player player, GameObject obj, String command) {
		if (!isSafetyDepositBox(obj)) {
			return;
		}

		if (command.equalsIgnoreCase("deposit")) {
			deposit(player);
		} else if (command.equalsIgnoreCase("withdraw")) {
			withdraw(player);
		}
	}

	@Override
	public boolean blockOpLoc(Player player, GameObject obj, String command) {
		return isSafetyDepositBox(obj)
			&& (command.equalsIgnoreCase("deposit") || command.equalsIgnoreCase("withdraw"));
	}

	private void deposit(Player player) {
		if (player.getCache().hasKey(CACHE_KEY)) {
			player.message("The box already contains items.");
			return;
		}

		List<SnapshotItem> items = findBlockedItems(player);
		if (items.isEmpty()) {
			player.message("You have nothing that needs to be deposited.");
			return;
		}

		List<SnapshotItem> removed = new ArrayList<>();
		for (SnapshotItem item : items) {
			if (!removeSnapshotItem(player, item)) {
				restoreSnapshotItems(player, removed);
				player.message("The box refuses to take your items.");
				return;
			}
			removed.add(item);
		}

		player.getCache().store(CACHE_KEY, serialize(items));
		player.message("You deposit your Entrana-barred items into the box.");
	}

	private void withdraw(Player player) {
		if (!player.getCache().hasKey(CACHE_KEY)) {
			player.message("There is nothing in the box.");
			return;
		}

		List<SnapshotItem> items;
		try {
			items = parse(player.getCache().getString(CACHE_KEY));
		} catch (IllegalArgumentException e) {
			player.message("The box refuses to open.");
			return;
		}

		List<Item> inventoryRestores = new ArrayList<>();
		for (SnapshotItem item : items) {
			if (!canRestoreToEquipment(player, item)) {
				inventoryRestores.add(item.toItem());
			}
		}

		Inventory inventory = player.getCarriedItems().getInventory();
		if (inventory.getRequiredSlots(inventoryRestores) > inventory.getFreeSlots()) {
			player.message("You don't have enough room to withdraw your items.");
			return;
		}

		for (SnapshotItem item : items) {
			if (canRestoreToEquipment(player, item)) {
				Item restored = item.toItem();
				if (player.getCarriedItems().getEquipment().add(restored) != -1) {
					updateWornAppearance(player, restored);
					continue;
				}
			}
			inventory.add(item.toItem(), false);
		}

		player.getCache().remove(CACHE_KEY);
		ActionSender.sendInventory(player);
		ActionSender.sendEquipmentStats(player);
		player.getUpdateFlags().setAppearanceChanged(true);
		player.message("You withdraw your items from the box.");
	}

	private List<SnapshotItem> findBlockedItems(Player player) {
		List<SnapshotItem> items = new ArrayList<>();
		Inventory inventory = player.getCarriedItems().getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			Item item = inventory.get(slot);
			if (item != null && EntranaRestrictions.itemIsBlocked(player, item)) {
				items.add(SnapshotItem.fromInventory(slot, item));
			}
		}

		if (player.getConfig().WANT_EQUIPMENT_TAB) {
			Equipment equipment = player.getCarriedItems().getEquipment();
			for (int slot = 0; slot < Equipment.SLOT_COUNT; slot++) {
				Item item = equipment.get(slot);
				if (item != null && EntranaRestrictions.itemIsBlocked(player, item)) {
					items.add(SnapshotItem.fromEquipment(slot, item));
				}
			}
		}
		return items;
	}

	private boolean removeSnapshotItem(Player player, SnapshotItem item) {
		if (item.source == Source.INVENTORY) {
			return player.getCarriedItems().getInventory().remove(item.toExactItem(), true) != -1;
		}
		return player.getCarriedItems().getEquipment().remove(item.toExactItem(), item.amount, true) != -1;
	}

	private void restoreSnapshotItems(Player player, List<SnapshotItem> items) {
		for (SnapshotItem item : items) {
			if (canRestoreToEquipment(player, item)) {
				Item restored = item.toItem();
				if (player.getCarriedItems().getEquipment().add(restored) != -1) {
					updateWornAppearance(player, restored);
					continue;
				}
			}
			player.getCarriedItems().getInventory().add(item.toItem(), false);
		}
		ActionSender.sendInventory(player);
		ActionSender.sendEquipmentStats(player);
		player.getUpdateFlags().setAppearanceChanged(true);
	}

	private boolean canRestoreToEquipment(Player player, SnapshotItem item) {
		if (item.source != Source.EQUIPMENT || !player.getConfig().WANT_EQUIPMENT_TAB) {
			return false;
		}
		if (item.slot < 0 || item.slot >= Equipment.SLOT_COUNT) {
			return false;
		}
		if (player.getCarriedItems().getEquipment().get(item.slot) != null) {
			return false;
		}
		ItemDefinition def = item.toItem().getDef(player.getWorld());
		return def != null && def.isWieldable() && def.getWieldPosition() == item.slot;
	}

	private void updateWornAppearance(Player player, Item item) {
		ItemDefinition def = item.getDef(player.getWorld());
		if (def != null) {
			player.updateWornItems(def.getWieldPosition(), def.getAppearanceId(), def.getWearableId(), true);
		}
	}

	private String serialize(List<SnapshotItem> items) {
		StringBuilder snapshot = new StringBuilder(SNAPSHOT_VERSION);
		for (SnapshotItem item : items) {
			snapshot.append('|')
				.append(item.source.code)
				.append(',').append(item.slot)
				.append(',').append(item.catalogId)
				.append(',').append(item.amount)
				.append(',').append(item.noted);
		}
		return snapshot.toString();
	}

	private List<SnapshotItem> parse(String snapshot) {
		String[] parts = snapshot.split("\\|");
		if (parts.length < 1 || !SNAPSHOT_VERSION.equals(parts[0])) {
			throw new IllegalArgumentException("Unknown safety deposit box snapshot");
		}

		List<SnapshotItem> items = new ArrayList<>();
		for (int i = 1; i < parts.length; i++) {
			String[] fields = parts[i].split(",");
			if (fields.length != 5) {
				throw new IllegalArgumentException("Malformed safety deposit box item");
			}
			items.add(new SnapshotItem(
				Source.fromCode(fields[0]),
				Integer.parseInt(fields[1]),
				Integer.parseInt(fields[2]),
				Integer.parseInt(fields[3]),
				Boolean.parseBoolean(fields[4]),
				Item.ITEM_ID_UNASSIGNED
			));
		}
		return items;
	}

	private boolean isSafetyDepositBox(GameObject obj) {
		return obj.getID() == SceneryId.ENTRANA_SAFETY_DEPOSIT_BOX.id()
			&& obj.getX() == 266
			&& obj.getY() == 660;
	}

	private enum Source {
		INVENTORY("I"),
		EQUIPMENT("E");

		private final String code;

		Source(String code) {
			this.code = code;
		}

		private static Source fromCode(String code) {
			for (Source source : values()) {
				if (source.code.equals(code)) {
					return source;
				}
			}
			throw new IllegalArgumentException("Unknown safety deposit box source");
		}
	}

	private static final class SnapshotItem {
		private final Source source;
		private final int slot;
		private final int catalogId;
		private final int amount;
		private final boolean noted;
		private final long itemId;

		private SnapshotItem(Source source, int slot, int catalogId, int amount, boolean noted, long itemId) {
			this.source = source;
			this.slot = slot;
			this.catalogId = catalogId;
			this.amount = amount;
			this.noted = noted;
			this.itemId = itemId;
		}

		private static SnapshotItem fromInventory(int slot, Item item) {
			return new SnapshotItem(Source.INVENTORY, slot, item.getCatalogId(), item.getAmount(), item.getNoted(), item.getItemId());
		}

		private static SnapshotItem fromEquipment(int slot, Item item) {
			return new SnapshotItem(Source.EQUIPMENT, slot, item.getCatalogId(), item.getAmount(), item.getNoted(), item.getItemId());
		}

		private Item toItem() {
			return new Item(catalogId, amount, noted);
		}

		private Item toExactItem() {
			return new Item(catalogId, amount, noted, itemId);
		}
	}
}
