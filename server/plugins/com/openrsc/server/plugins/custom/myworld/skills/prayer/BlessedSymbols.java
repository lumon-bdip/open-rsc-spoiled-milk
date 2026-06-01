package com.openrsc.server.plugins.custom.myworld.skills.prayer;

import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Skill;
import com.openrsc.server.content.Devotion;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.PrayerCatalog;
import com.openrsc.server.plugins.triggers.UseLocTrigger;

import static com.openrsc.server.plugins.Functions.give;

public final class BlessedSymbols implements UseLocTrigger {
	private static final int SYMBOL_DEVOTION_REQUIREMENT = 25;
	private static final int SYMBOL_CRAFTING_XP = 200;

	@Override
	public boolean blockUseLoc(final Player player, final GameObject obj, final Item item) {
		if (item == null || item.getNoted()) {
			return false;
		}
		return PrayerCatalog.getGodLineForAltar(obj.getID(), obj.getX(), obj.getY()) != null
			&& isUnblessedSymbol(item.getCatalogId());
	}

	@Override
	public void onUseLoc(final Player player, final GameObject obj, final Item item) {
		if (item == null || item.getNoted()) {
			return;
		}

		final PrayerCatalog.GodLine godLine = PrayerCatalog.getGodLineForAltar(obj.getID(), obj.getX(), obj.getY());
		if (godLine == null || !isUnblessedSymbol(item.getCatalogId())) {
			return;
		}

		final int productId = getBlessedSymbolProduct(godLine, item.getCatalogId());
		if (productId == -1) {
			player.message("This symbol is not aligned with " + formatGodLine(godLine) + ".");
			return;
		}

		final int currentDevotion = Devotion.getDevotionLevel(player, godLine);
		if (currentDevotion < SYMBOL_DEVOTION_REQUIREMENT) {
			player.message("You need " + SYMBOL_DEVOTION_REQUIREMENT + " devotion to " + formatGodLine(godLine) + " to bless this symbol.");
			player.message("Your current devotion to " + formatGodLine(godLine) + " is " + currentDevotion + ".");
			return;
		}

		if (player.getCarriedItems().remove(item) == -1) {
			return;
		}

		give(player, productId, 1);
		final int prayerXp = Devotion.getBlessingPrayerXp(player, godLine, SYMBOL_CRAFTING_XP);
		if (prayerXp > 0) {
			player.incExp(Skill.PRAYER.id(), prayerXp, true);
		}
		player.message("The altar blesses the symbol.");
	}

	private boolean isUnblessedSymbol(final int itemId) {
		return itemId == ItemId.UNBLESSED_HOLY_SYMBOL.id()
			|| itemId == ItemId.UNBLESSED_UNHOLY_SYMBOL_OF_ZAMORAK.id()
			|| itemId == ItemId.UNBLESSED_GUTHIX_SYMBOL.id();
	}

	private int getBlessedSymbolProduct(final PrayerCatalog.GodLine godLine, final int itemId) {
		if (godLine == PrayerCatalog.GodLine.SARADOMIN && itemId == ItemId.UNBLESSED_HOLY_SYMBOL.id()) {
			return ItemId.HOLY_SYMBOL_OF_SARADOMIN.id();
		}
		if (godLine == PrayerCatalog.GodLine.ZAMORAK && itemId == ItemId.UNBLESSED_UNHOLY_SYMBOL_OF_ZAMORAK.id()) {
			return ItemId.UNHOLY_SYMBOL_OF_ZAMORAK.id();
		}
		if (godLine == PrayerCatalog.GodLine.GUTHIX && itemId == ItemId.UNBLESSED_GUTHIX_SYMBOL.id()) {
			return ItemId.GUTHIX_SYMBOL.id();
		}
		return -1;
	}

	private String formatGodLine(final PrayerCatalog.GodLine godLine) {
		final String lower = godLine.name().toLowerCase();
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
