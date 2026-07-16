package com.openrsc.server.content.minigame.monsterslayer;

import java.util.Locale;

/** Stable Monster Slayer standing. Numeric codes are part of the cache contract. */
public enum MonsterSlayerRank {
	UNSTAMPED(0),
	FLEDGLING(1),
	INITIATE(2),
	VETERAN(3),
	ELITE(4),
	CHAMPION(5),
	HERO(6),
	LEGEND(7);

	private final int code;

	MonsterSlayerRank(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public boolean isAtLeast(MonsterSlayerRank required) {
		return code >= required.code;
	}

	public static MonsterSlayerRank fromCode(int code) {
		for (MonsterSlayerRank rank : values()) {
			if (rank.code == code) {
				return rank;
			}
		}
		throw new IllegalArgumentException("Unknown Monster Slayer rank code: " + code);
	}

	public static MonsterSlayerRank fromKey(String key) {
		if (key == null) {
			throw new IllegalArgumentException("Monster Slayer rank is required");
		}
		try {
			return valueOf(key.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown Monster Slayer rank: " + key, ex);
		}
	}
}
