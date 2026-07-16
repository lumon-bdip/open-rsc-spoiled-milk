package com.openrsc.server.content.minigame.monsterslayer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable owner of the six independent challenge balances. */
public final class MonsterSlayerBalances {
	public static final long MAX_BALANCE = 2_000_000_000L;

	private final EnumMap<MonsterSlayerChallenge, Long> amounts;

	private MonsterSlayerBalances(Map<MonsterSlayerChallenge, Long> source) {
		this.amounts = new EnumMap<MonsterSlayerChallenge, Long>(MonsterSlayerChallenge.class);
		if (source.containsKey(null)) {
			throw new IllegalArgumentException("Monster Slayer balances have a null challenge");
		}
		for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
			Long value = source.get(challenge);
			long amount = value == null ? 0L : value;
			validateAmount(challenge, amount);
			amounts.put(challenge, amount);
		}
	}

	public static MonsterSlayerBalances zero() {
		return new MonsterSlayerBalances(Collections.<MonsterSlayerChallenge, Long>emptyMap());
	}

	public static MonsterSlayerBalances of(Map<MonsterSlayerChallenge, Long> amounts) {
		if (amounts == null) {
			throw new IllegalArgumentException("Monster Slayer balances are required");
		}
		return new MonsterSlayerBalances(amounts);
	}

	public long get(MonsterSlayerChallenge challenge) {
		if (challenge == null) {
			throw new IllegalArgumentException("Monster Slayer challenge is required");
		}
		return amounts.get(challenge);
	}

	public Map<MonsterSlayerChallenge, Long> asMap() {
		return Collections.unmodifiableMap(amounts);
	}

	public MonsterSlayerBalances credit(MonsterSlayerChallenge challenge, long amount) {
		if (challenge == null) {
			throw new IllegalArgumentException("Monster Slayer challenge is required");
		}
		if (amount < 0L) {
			throw new IllegalArgumentException("Credit must be nonnegative");
		}
		EnumMap<MonsterSlayerChallenge, Long> credited = copyAmounts();
		try {
			credited.put(challenge, Math.addExact(get(challenge), amount));
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException("Monster Slayer balance overflow", ex);
		}
		return new MonsterSlayerBalances(credited);
	}

	public MonsterSlayerBalances credit(MonsterSlayerBalances award) {
		if (award == null) {
			throw new IllegalArgumentException("Monster Slayer award is required");
		}
		EnumMap<MonsterSlayerChallenge, Long> credited = copyAmounts();
		try {
			for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
				credited.put(challenge, Math.addExact(get(challenge), award.get(challenge)));
			}
		} catch (ArithmeticException ex) {
			throw new IllegalArgumentException("Monster Slayer balance overflow", ex);
		}
		return new MonsterSlayerBalances(credited);
	}

	public boolean canAfford(MonsterSlayerCost cost) {
		if (cost == null) {
			return false;
		}
		for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
			if (get(challenge) < cost.get(challenge)) {
				return false;
			}
		}
		return true;
	}

	/** Computes all six post-spend values before returning a successful proposal. */
	public SpendResult trySpend(MonsterSlayerCost unitCost, long quantity) {
		if (unitCost == null) {
			throw new IllegalArgumentException("Monster Slayer cost is required");
		}
		MonsterSlayerCost cost = unitCost.multiply(quantity);
		if (!canAfford(cost)) {
			return SpendResult.insufficient(this);
		}
		EnumMap<MonsterSlayerChallenge, Long> remaining = copyAmounts();
		for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
			remaining.put(challenge, get(challenge) - cost.get(challenge));
		}
		return SpendResult.success(new MonsterSlayerBalances(remaining), new RefundReceipt(cost));
	}

	private EnumMap<MonsterSlayerChallenge, Long> copyAmounts() {
		return new EnumMap<MonsterSlayerChallenge, Long>(amounts);
	}

	private static void validateAmount(MonsterSlayerChallenge challenge, long amount) {
		if (amount < 0L || amount > MAX_BALANCE) {
			throw new IllegalArgumentException(
				challenge + " balance outside 0.." + MAX_BALANCE + ": " + amount);
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MonsterSlayerBalances
			&& amounts.equals(((MonsterSlayerBalances) other).amounts);
	}

	@Override
	public int hashCode() {
		return amounts.hashCode();
	}

	@Override
	public String toString() {
		return amounts.toString();
	}

	public static final class SpendResult {
		private final boolean successful;
		private final MonsterSlayerBalances balances;
		private final RefundReceipt receipt;

		private SpendResult(boolean successful, MonsterSlayerBalances balances, RefundReceipt receipt) {
			this.successful = successful;
			this.balances = balances;
			this.receipt = receipt;
		}

		private static SpendResult success(MonsterSlayerBalances balances, RefundReceipt receipt) {
			return new SpendResult(true, balances, receipt);
		}

		private static SpendResult insufficient(MonsterSlayerBalances unchanged) {
			return new SpendResult(false, unchanged, null);
		}

		public boolean isSuccessful() {
			return successful;
		}

		public MonsterSlayerBalances getBalances() {
			return balances;
		}

		public RefundReceipt getReceipt() {
			if (!successful) {
				throw new IllegalStateException("An unsuccessful spend has no refund receipt");
			}
			return receipt;
		}
	}

	/** Exact spend token; only the successful spend path can construct one. */
	public static final class RefundReceipt {
		private final MonsterSlayerCost cost;
		private boolean refunded;

		private RefundReceipt(MonsterSlayerCost cost) {
			this.cost = cost;
		}

		public synchronized MonsterSlayerBalances refund(MonsterSlayerBalances current) {
			if (current == null) {
				throw new IllegalArgumentException("Current Monster Slayer balances are required");
			}
			if (refunded) {
				throw new IllegalStateException("Monster Slayer spend was already refunded");
			}
			EnumMap<MonsterSlayerChallenge, Long> restored =
				new EnumMap<MonsterSlayerChallenge, Long>(MonsterSlayerChallenge.class);
			try {
				for (MonsterSlayerChallenge challenge : MonsterSlayerChallenge.values()) {
					restored.put(challenge, Math.addExact(current.get(challenge), cost.get(challenge)));
				}
				MonsterSlayerBalances result = new MonsterSlayerBalances(restored);
				refunded = true;
				return result;
			} catch (ArithmeticException ex) {
				throw new IllegalArgumentException("Monster Slayer refund overflow", ex);
			}
		}

		public synchronized boolean isRefunded() {
			return refunded;
		}
	}
}
