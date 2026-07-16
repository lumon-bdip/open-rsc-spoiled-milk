package com.openrsc.server.model.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Tracks the one active ground item owned by each authored world spawn.
 *
 * Authored ground-item location loading already defines a tile as the identity
 * of a spawn: a later location definition on the same tile replaces the
 * earlier definition. Player and NPC drops never enter this registry.
 */
public final class AuthoredGroundItemRegistry<T> {
	public static final long NO_GENERATION = -1L;

	private final Map<Long, T> activeItems = new HashMap<>();
	private long generation;

	public synchronized T register(final int x, final int y, final Supplier<T> factory) {
		return registerForGeneration(x, y, generation, factory);
	}

	public synchronized T registerForGeneration(final int x, final int y, final long expectedGeneration,
		final Supplier<T> factory) {
		if (expectedGeneration != generation) {
			return null;
		}
		final long key = tileKey(x, y);
		final T existing = activeItems.get(key);
		if (existing != null) {
			return existing;
		}
		final T item = Objects.requireNonNull(factory.get(), "authored ground item");
		activeItems.put(key, item);
		return item;
	}

	/**
	 * Releases the spawn only when {@code item} is its current active instance.
	 * Reference identity is the ownership token here, so the PMD object-equality
	 * warning is intentionally suppressed rather than replacing {@code !=}.
	 * The returned generation is the token a delayed respawn must present.
	 */
	@SuppressWarnings("PMD.CompareObjectsWithEquals")
	public synchronized long remove(final int x, final int y, final T item) {
		final long key = tileKey(x, y);
		if (activeItems.get(key) != item) {
			return NO_GENERATION;
		}
		activeItems.remove(key);
		return generation;
	}

	public synchronized int size() {
		return activeItems.size();
	}

	/**
	 * Starts a new world lifecycle and invalidates every outstanding timer.
	 */
	public synchronized void reset() {
		activeItems.clear();
		generation++;
	}

	private long tileKey(final int x, final int y) {
		return ((long)x << 32) ^ (y & 0xffffffffL);
	}
}
