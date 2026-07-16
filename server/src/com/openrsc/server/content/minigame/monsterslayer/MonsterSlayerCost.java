package com.openrsc.server.content.minigame.monsterslayer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable, typed price. Challenge components are never summed or exchanged. */
public final class MonsterSlayerCost {
	private final EnumMap<MonsterSlayerChallenge, Long> amounts;

	private MonsterSlayerCost(Map<MonsterSlayerChallenge, Long> source) {
		this.amounts = new EnumMap<MonsterSlayerChallenge, Long>(MonsterSlayerChallenge.class);
		boolean positive = false;
		for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
			Long value = source.get(challenge);
			long amount = value == null ? 0L : value;
			if (amount < 0L) {
				throw new IllegalArgumentException("Negative " + challenge + " cost");
			}
			if (amount > 0L) {
				amounts.put(challenge, amount);
				positive = true;
			}
		}
		if (!positive) {
			throw new IllegalArgumentException("Monster Slayer cost must have a positive component");
		}
	}

	public static MonsterSlayerCost of(Map<MonsterSlayerChallenge, Long> amounts) {
		if (amounts == null) {
			throw new IllegalArgumentException("Monster Slayer cost is required");
		}
		return new MonsterSlayerCost(amounts);
	}

	public static MonsterSlayerCost single(MonsterSlayerChallenge challenge, long amount) {
		EnumMap<MonsterSlayerChallenge, Long> values =
			new EnumMap<MonsterSlayerChallenge, Long>(MonsterSlayerChallenge.class);
		values.put(challenge, amount);
		return of(values);
	}

	public long get(MonsterSlayerChallenge challenge) {
		Long amount = amounts.get(challenge);
		return amount == null ? 0L : amount;
	}

	public Map<MonsterSlayerChallenge, Long> asMap() {
		return Collections.unmodifiableMap(amounts);
	}

	public MonsterSlayerCost multiply(long quantity) {
		if (quantity <= 0L) {
			throw new IllegalArgumentException("Reward quantity must be positive");
		}
		EnumMap<MonsterSlayerChallenge, Long> multiplied =
			new EnumMap<MonsterSlayerChallenge, Long>(MonsterSlayerChallenge.class);
		try {
			for (Map.Entry<MonsterSlayerChallenge, Long> entry : amounts.entrySet()) {
				multiplied.put(entry.getKey(), Math.multiplyExact(entry.getValue(), quantity));
			}
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException("Monster Slayer cost overflow", ex);
		}
		return new MonsterSlayerCost(multiplied);
	}

	public void validateForShop(MonsterSlayerChallenge shopTier, boolean requireNativeCost) {
		if (shopTier == null) {
			throw new IllegalArgumentException("Shop challenge is required");
		}
		for (MonsterSlayerChallenge challenge : amounts.keySet()) {
			if (!challenge.isAtOrBelow(shopTier)) {
				throw new IllegalArgumentException(
					challenge + " cost is above " + shopTier + " shop tier");
			}
		}
		if (requireNativeCost && get(shopTier) <= 0L) {
			throw new IllegalArgumentException("Reward requires a positive native " + shopTier + " cost");
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MonsterSlayerCost
			&& amounts.equals(((MonsterSlayerCost) other).amounts);
	}

	@Override
	public int hashCode() {
		return amounts.hashCode();
	}

	@Override
	public String toString() {
		return amounts.toString();
	}
}
