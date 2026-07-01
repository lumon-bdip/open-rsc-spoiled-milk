package com.openrsc.server.plugins.authentic.npcs.lostcity;

import com.openrsc.server.model.entity.player.Player;

public final class LostCityMarketAccess {

	public static final String DIAMOND_TAX_WAIVED_CACHE_KEY = "lost_city_market_diamond_tax_waived";

	private LostCityMarketAccess() {
	}

	public static boolean hasDiamondTaxWaiver(Player player) {
		return player.getCache().hasKey(DIAMOND_TAX_WAIVED_CACHE_KEY);
	}

	public static void waiveDiamondTax(Player player) {
		player.getCache().set(DIAMOND_TAX_WAIVED_CACHE_KEY, 1);
	}
}
