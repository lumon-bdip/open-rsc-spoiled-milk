package com.openrsc.server.plugins.custom.skills.crafting;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.SceneryId;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.plugins.triggers.UseLocTrigger;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;

import java.util.Optional;

import static com.openrsc.server.plugins.Functions.thinkbubble;

public final class OreCrusher implements UseLocTrigger {

	private static final OreCrushDef[] ORE_CRUSH_DEFS = {
		new OreCrushDef(ItemId.TIN_ORE.id(), 20, 90, 9, 1, 0),
		new OreCrushDef(ItemId.COPPER_ORE.id(), 30, 85, 13, 2, 0),
		new OreCrushDef(ItemId.IRON_ORE.id(), 40, 70, 23, 6, 1),
		new OreCrushDef(ItemId.MITHRIL_ORE.id(), 50, 50, 35, 13, 2),
		new OreCrushDef(ItemId.ADAMANTITE_ORE.id(), 60, 30, 42, 23, 5),
		new OreCrushDef(ItemId.RUNITE_ORE.id(), 80, 15, 35, 40, 10)
	};

	@Override
	public void onUseLoc(Player player, GameObject obj, Item item) {
		if (!isOreCrusher(obj)) {
			return;
		}

		OreCrushDef def = getOreCrushDef(item.getCatalogId());
		if (def == null) {
			return;
		}

		processOre(player, def);
	}

	@Override
	public boolean blockUseLoc(Player player, GameObject obj, Item item) {
		return isOreCrusher(obj) && getOreCrushDef(item.getCatalogId()) != null;
	}

	private void processOre(Player player, OreCrushDef def) {
		int oreCount = player.getCarriedItems().getInventory().countId(def.oreId, Optional.of(false));
		if (oreCount < 1) {
			return;
		}

		Item source = new Item(def.oreId);
		thinkbubble(source);
		player.playerServerMessage(MessageType.QUEST, "You feed the ore into the crusher.");

		int sapphires = 0;
		int emeralds = 0;
		int rubies = 0;
		int diamonds = 0;
		int destroyed = 0;

		for (int loop = 0; loop < oreCount; loop++) {
			if (player.getCarriedItems().remove(source) == -1) {
				break;
			}

			if (DataConversions.random(1, 100) > def.successChance) {
				destroyed++;
				continue;
			}

			int gemId = def.rollGem();
			player.getCarriedItems().getInventory().add(new Item(gemId));
			if (gemId == ItemId.UNCUT_SAPPHIRE.id()) {
				sapphires++;
			} else if (gemId == ItemId.UNCUT_EMERALD.id()) {
				emeralds++;
			} else if (gemId == ItemId.UNCUT_RUBY.id()) {
				rubies++;
			} else if (gemId == ItemId.UNCUT_DIAMOND.id()) {
				diamonds++;
			}
		}

		int produced = sapphires + emeralds + rubies + diamonds;
		if (produced < 1) {
			player.playerServerMessage(MessageType.QUEST, "The crusher grinds the ore into dust.");
			return;
		}

		player.playerServerMessage(MessageType.QUEST, "The crusher produces " + formatGemSummary(sapphires, emeralds, rubies, diamonds) + ".");
		if (destroyed > 0) {
			player.playerServerMessage(MessageType.QUEST, destroyed + " ore " + (destroyed == 1 ? "is" : "are") + " ground into dust.");
		}
	}

	private String formatGemSummary(int sapphires, int emeralds, int rubies, int diamonds) {
		StringBuilder summary = new StringBuilder();
		appendGemSummary(summary, sapphires, "sapphire");
		appendGemSummary(summary, emeralds, "emerald");
		appendGemSummary(summary, rubies, "ruby");
		appendGemSummary(summary, diamonds, "diamond");
		return summary.toString();
	}

	private void appendGemSummary(StringBuilder summary, int count, String gemName) {
		if (count < 1) {
			return;
		}
		if (summary.length() > 0) {
			summary.append(", ");
		}
		summary.append(count).append(' ').append(gemName);
		if (count != 1) {
			summary.append('s');
		}
	}

	private OreCrushDef getOreCrushDef(int itemId) {
		for (OreCrushDef def : ORE_CRUSH_DEFS) {
			if (def.oreId == itemId) {
				return def;
			}
		}
		return null;
	}

	private boolean isOreCrusher(GameObject obj) {
		return obj.getID() == SceneryId.ORE_CRUSHER.id();
	}

	private static final class OreCrushDef {
		private final int oreId;
		private final int successChance;
		private final int sapphireWeight;
		private final int emeraldWeight;
		private final int rubyWeight;
		private final int diamondWeight;

		private OreCrushDef(int oreId, int successChance, int sapphireWeight, int emeraldWeight, int rubyWeight, int diamondWeight) {
			this.oreId = oreId;
			this.successChance = successChance;
			this.sapphireWeight = sapphireWeight;
			this.emeraldWeight = emeraldWeight;
			this.rubyWeight = rubyWeight;
			this.diamondWeight = diamondWeight;
		}

		private int rollGem() {
			int roll = DataConversions.random(1, sapphireWeight + emeraldWeight + rubyWeight + diamondWeight);
			if (roll <= sapphireWeight) {
				return ItemId.UNCUT_SAPPHIRE.id();
			}
			roll -= sapphireWeight;
			if (roll <= emeraldWeight) {
				return ItemId.UNCUT_EMERALD.id();
			}
			roll -= emeraldWeight;
			if (roll <= rubyWeight) {
				return ItemId.UNCUT_RUBY.id();
			}
			return ItemId.UNCUT_DIAMOND.id();
		}
	}
}
