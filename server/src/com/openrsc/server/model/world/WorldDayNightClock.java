package com.openrsc.server.model.world;

public final class WorldDayNightClock {
	public static final int CYCLE_MILLIS = 60 * 60 * 1000;
	public static final int NORMAL_RATE_MULTIPLIER = 1;
	public static final int ADVANCE_RATE_MULTIPLIER = 60;
	private static final long RECENT_ACCELERATION_SYNC_MILLIS = 1000L;

	private long anchorRealMillis;
	private long anchorCycleMillis;
	private long acceleratedRemainingMillis;
	private long lastAccelerationEndedMillis;

	public WorldDayNightClock() {
		this(System.currentTimeMillis());
	}

	public WorldDayNightClock(final long epochMillis) {
		this.anchorRealMillis = epochMillis;
		this.anchorCycleMillis = 0L;
	}

	public int getCycleMillis() {
		return CYCLE_MILLIS;
	}

	public synchronized int getCurrentCycleMillis() {
		update(System.currentTimeMillis());
		return (int) anchorCycleMillis;
	}

	public synchronized void setCurrentCycleMillis(final int cycleMillis) {
		final long now = System.currentTimeMillis();
		update(now);
		anchorCycleMillis = Math.floorMod((long) cycleMillis, (long) CYCLE_MILLIS);
		anchorRealMillis = now;
		acceleratedRemainingMillis = 0L;
		lastAccelerationEndedMillis = now;
	}

	public synchronized void advanceSmoothly(final int advanceMillis) {
		if (advanceMillis <= 0) {
			return;
		}
		update(System.currentTimeMillis());
		acceleratedRemainingMillis += advanceMillis;
	}

	public synchronized int getRateMultiplier() {
		update(System.currentTimeMillis());
		return acceleratedRemainingMillis > 0L ? ADVANCE_RATE_MULTIPLIER : NORMAL_RATE_MULTIPLIER;
	}

	public synchronized boolean shouldSyncFrequently() {
		final long now = System.currentTimeMillis();
		update(now);
		return acceleratedRemainingMillis > 0L
			|| now - lastAccelerationEndedMillis < RECENT_ACCELERATION_SYNC_MILLIS;
	}

	public synchronized long getEpochMillis() {
		update(System.currentTimeMillis());
		return anchorRealMillis - anchorCycleMillis;
	}

	private void update(final long now) {
		final long realElapsedMillis = Math.max(0L, now - anchorRealMillis);
		if (realElapsedMillis <= 0L) {
			return;
		}

		long cycleAdvanceMillis = 0L;
		if (acceleratedRemainingMillis > 0L) {
			final long acceleratedAdvanceMillis = realElapsedMillis * (long) ADVANCE_RATE_MULTIPLIER;
			if (acceleratedAdvanceMillis >= acceleratedRemainingMillis) {
				final long consumedAdvanceMillis = acceleratedRemainingMillis;
				final long realMillisUsed = Math.min(
					realElapsedMillis,
					(consumedAdvanceMillis + ADVANCE_RATE_MULTIPLIER - 1L) / ADVANCE_RATE_MULTIPLIER);
				cycleAdvanceMillis += consumedAdvanceMillis;
				cycleAdvanceMillis += realElapsedMillis - realMillisUsed;
				acceleratedRemainingMillis = 0L;
				lastAccelerationEndedMillis = anchorRealMillis + realMillisUsed;
			} else {
				cycleAdvanceMillis += acceleratedAdvanceMillis;
				acceleratedRemainingMillis -= acceleratedAdvanceMillis;
			}
		} else {
			cycleAdvanceMillis = realElapsedMillis;
		}

		anchorCycleMillis = Math.floorMod(anchorCycleMillis + cycleAdvanceMillis, (long) CYCLE_MILLIS);
		anchorRealMillis = now;
	}
}
