package com.openrsc.server.model.world;

public final class WorldDayNightClock {
	public static final int CYCLE_MILLIS = 60 * 60 * 1000;

	private final long epochMillis;

	public WorldDayNightClock() {
		this(System.currentTimeMillis());
	}

	public WorldDayNightClock(final long epochMillis) {
		this.epochMillis = epochMillis;
	}

	public int getCycleMillis() {
		return CYCLE_MILLIS;
	}

	public int getCurrentCycleMillis() {
		return (int) Math.floorMod(System.currentTimeMillis() - epochMillis, (long) CYCLE_MILLIS);
	}

	public long getEpochMillis() {
		return epochMillis;
	}
}
