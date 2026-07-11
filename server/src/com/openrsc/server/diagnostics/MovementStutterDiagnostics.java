package com.openrsc.server.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allocation-light, privacy-safe timing aggregation for movement stutter
 * investigations. The game thread is the only writer in normal operation.
 */
public final class MovementStutterDiagnostics {
	private static final long[] HISTOGRAM_UPPER_BOUNDS_NANOS = new long[] {
		1_000_000L,
		2_000_000L,
		5_000_000L,
		10_000_000L,
		16_000_000L,
		25_000_000L,
		50_000_000L,
		100_000_000L,
		250_000_000L,
		500_000_000L,
		1_000_000_000L,
		Long.MAX_VALUE
	};
	private static final int OUTLIER_LIMIT = 16;
	private static final byte OUTLIER_POLL = 1;
	private static final byte OUTLIER_TICK = 2;

	private final boolean enabled;
	private final long expectedPollIntervalNanos;
	private final long summaryIntervalNanos;
	private final long pollOutlierLatenessNanos;
	private final long pollOutlierDurationNanos;
	private final long tickOutlierDurationNanos;

	private final BoundedHistogram pollInterval = new BoundedHistogram(HISTOGRAM_UPPER_BOUNDS_NANOS);
	private final BoundedHistogram pollDuration = new BoundedHistogram(HISTOGRAM_UPPER_BOUNDS_NANOS);
	private final BoundedHistogram pollLateness = new BoundedHistogram(HISTOGRAM_UPPER_BOUNDS_NANOS);
	private final BoundedHistogram tickDuration = new BoundedHistogram(HISTOGRAM_UPPER_BOUNDS_NANOS);

	private final byte[] outlierKinds = new byte[OUTLIER_LIMIT];
	private final long[] outlierIds = new long[OUTLIER_LIMIT];
	private final long[] outlierIntervals = new long[OUTLIER_LIMIT];
	private final long[] outlierDurations = new long[OUTLIER_LIMIT];
	private final long[] outlierLateness = new long[OUTLIER_LIMIT];
	private final int[] outlierMovedPlayers = new int[OUTLIER_LIMIT];
	private final int[] outlierMovedNpcs = new int[OUTLIER_LIMIT];
	private final int[] outlierQueuedPackets = new int[OUTLIER_LIMIT];
	private final int[] outlierMaxQueue = new int[OUTLIER_LIMIT];
	private final int[] outlierBackpressuredPlayers = new int[OUTLIER_LIMIT];
	private final long[] outlierWorldUpdate = new long[OUTLIER_LIMIT];
	private final long[] outlierEvents = new long[OUTLIER_LIMIT];
	private final long[] outlierPlayers = new long[OUTLIER_LIMIT];
	private final long[] outlierNpcs = new long[OUTLIER_LIMIT];
	private final long[] outlierUpdateClients = new long[OUTLIER_LIMIT];
	private final long[] outlierOutgoing = new long[OUTLIER_LIMIT];
	private final long[] outlierCleanup = new long[OUTLIER_LIMIT];

	private long windowStartedNanos;
	private long lastPollStartedNanos;
	private long pollCount;
	private long pollsWithMovement;
	private long movedPlayersTotal;
	private long movedNpcsTotal;
	private long queuedPacketsTotal;
	private int queuedPacketsMax;
	private int playerQueueMax;
	private long backpressuredPlayerSamples;
	private long tickCount;
	private long outlierCount;
	private int outlierStored;

	public MovementStutterDiagnostics(
		boolean enabled,
		long expectedPollIntervalNanos,
		long summaryIntervalNanos,
		long pollOutlierLatenessNanos,
		long pollOutlierDurationNanos,
		long tickOutlierDurationNanos) {
		this.enabled = enabled;
		this.expectedPollIntervalNanos = Math.max(1L, expectedPollIntervalNanos);
		this.summaryIntervalNanos = Math.max(1L, summaryIntervalNanos);
		this.pollOutlierLatenessNanos = Math.max(1L, pollOutlierLatenessNanos);
		this.pollOutlierDurationNanos = Math.max(1L, pollOutlierDurationNanos);
		this.tickOutlierDurationNanos = Math.max(1L, tickOutlierDurationNanos);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void recordMovementPoll(
		long startedNanos,
		long finishedNanos,
		int movedPlayers,
		int movedNpcs,
		int queuedPackets,
		int maxPlayerQueue,
		int backpressuredPlayers) {
		if (!enabled) {
			return;
		}
		startWindowIfNeeded(startedNanos);
		long duration = Math.max(0L, finishedNanos - startedNanos);
		long interval = lastPollStartedNanos == 0L
			? expectedPollIntervalNanos
			: Math.max(0L, startedNanos - lastPollStartedNanos);
		long lateness = Math.max(0L, interval - expectedPollIntervalNanos);
		lastPollStartedNanos = startedNanos;

		pollCount++;
		pollInterval.record(interval);
		pollDuration.record(duration);
		pollLateness.record(lateness);
		if (movedPlayers > 0 || movedNpcs > 0) {
			pollsWithMovement++;
		}
		movedPlayersTotal += Math.max(0, movedPlayers);
		movedNpcsTotal += Math.max(0, movedNpcs);
		queuedPacketsTotal += Math.max(0, queuedPackets);
		queuedPacketsMax = Math.max(queuedPacketsMax, Math.max(0, queuedPackets));
		playerQueueMax = Math.max(playerQueueMax, Math.max(0, maxPlayerQueue));
		backpressuredPlayerSamples += Math.max(0, backpressuredPlayers);

		if (lateness >= pollOutlierLatenessNanos || duration >= pollOutlierDurationNanos) {
			storePollOutlier(
				startedNanos,
				interval,
				duration,
				lateness,
				movedPlayers,
				movedNpcs,
				queuedPackets,
				maxPlayerQueue,
				backpressuredPlayers);
		}
	}

	public void recordWorldTick(long nowNanos, long tickId, long durationNanos, TickStages stages) {
		if (!enabled) {
			return;
		}
		startWindowIfNeeded(nowNanos);
		tickCount++;
		tickDuration.record(Math.max(0L, durationNanos));
		if (durationNanos >= tickOutlierDurationNanos) {
			storeTickOutlier(nowNanos, tickId, durationNanos, stages == null ? TickStages.EMPTY : stages);
		}
	}

	public List<String> drainIfDue(long nowNanos) {
		if (!enabled || windowStartedNanos == 0L || nowNanos - windowStartedNanos < summaryIntervalNanos) {
			return Collections.emptyList();
		}

		List<String> lines = new ArrayList<String>(1 + outlierStored);
		lines.add(buildSummary(nowNanos));
		for (int i = 0; i < outlierStored; i++) {
			lines.add(buildOutlier(i));
		}
		resetWindow(nowNanos);
		return lines;
	}

	private String buildSummary(long nowNanos) {
		return "summary"
			+ " windowMs=" + nanosToMillis(nowNanos - windowStartedNanos)
			+ " polls=" + pollCount
			+ " pollsWithMovement=" + pollsWithMovement
			+ " movedPlayers=" + movedPlayersTotal
			+ " movedNpcs=" + movedNpcsTotal
			+ " queuedPacketsTotal=" + queuedPacketsTotal
			+ " queuedPacketsMax=" + queuedPacketsMax
			+ " playerQueueMax=" + playerQueueMax
			+ " backpressuredPlayerSamples=" + backpressuredPlayerSamples
			+ " ticks=" + tickCount
			+ " pollInterval=" + pollInterval.summaryMillis()
			+ " pollDuration=" + pollDuration.summaryMillis()
			+ " pollLateness=" + pollLateness.summaryMillis()
			+ " tickDuration=" + tickDuration.summaryMillis()
			+ " outliers=" + outlierCount
			+ " outliersRetained=" + outlierStored
			+ " outliersDropped=" + Math.max(0L, outlierCount - outlierStored);
	}

	private String buildOutlier(int index) {
		if (outlierKinds[index] == OUTLIER_TICK) {
			return "outlier kind=tick"
				+ " tick=" + outlierIds[index]
				+ " durationMs=" + nanosToMillis(outlierDurations[index])
				+ " worldUpdateMs=" + nanosToMillis(outlierWorldUpdate[index])
				+ " eventsMs=" + nanosToMillis(outlierEvents[index])
				+ " playersMs=" + nanosToMillis(outlierPlayers[index])
				+ " npcsMs=" + nanosToMillis(outlierNpcs[index])
				+ " updateClientsMs=" + nanosToMillis(outlierUpdateClients[index])
				+ " outgoingMs=" + nanosToMillis(outlierOutgoing[index])
				+ " cleanupMs=" + nanosToMillis(outlierCleanup[index]);
		}
		return "outlier kind=movement-poll"
			+ " sample=" + outlierIds[index]
			+ " intervalMs=" + nanosToMillis(outlierIntervals[index])
			+ " durationMs=" + nanosToMillis(outlierDurations[index])
			+ " latenessMs=" + nanosToMillis(outlierLateness[index])
			+ " movedPlayers=" + outlierMovedPlayers[index]
			+ " movedNpcs=" + outlierMovedNpcs[index]
			+ " queuedPackets=" + outlierQueuedPackets[index]
			+ " maxPlayerQueue=" + outlierMaxQueue[index]
			+ " backpressuredPlayers=" + outlierBackpressuredPlayers[index];
	}

	private void storePollOutlier(
		long sampleId,
		long interval,
		long duration,
		long lateness,
		int movedPlayers,
		int movedNpcs,
		int queuedPackets,
		int maxPlayerQueue,
		int backpressuredPlayers) {
		outlierCount++;
		if (outlierStored >= OUTLIER_LIMIT) {
			return;
		}
		int index = outlierStored++;
		outlierKinds[index] = OUTLIER_POLL;
		outlierIds[index] = sampleId;
		outlierIntervals[index] = interval;
		outlierDurations[index] = duration;
		outlierLateness[index] = lateness;
		outlierMovedPlayers[index] = Math.max(0, movedPlayers);
		outlierMovedNpcs[index] = Math.max(0, movedNpcs);
		outlierQueuedPackets[index] = Math.max(0, queuedPackets);
		outlierMaxQueue[index] = Math.max(0, maxPlayerQueue);
		outlierBackpressuredPlayers[index] = Math.max(0, backpressuredPlayers);
	}

	private void storeTickOutlier(long nowNanos, long tickId, long durationNanos, TickStages stages) {
		outlierCount++;
		if (outlierStored >= OUTLIER_LIMIT) {
			return;
		}
		int index = outlierStored++;
		outlierKinds[index] = OUTLIER_TICK;
		outlierIds[index] = tickId;
		outlierDurations[index] = Math.max(0L, durationNanos);
		outlierIntervals[index] = nowNanos;
		outlierWorldUpdate[index] = stages.worldUpdateNanos;
		outlierEvents[index] = stages.eventsNanos;
		outlierPlayers[index] = stages.playersNanos;
		outlierNpcs[index] = stages.npcsNanos;
		outlierUpdateClients[index] = stages.updateClientsNanos;
		outlierOutgoing[index] = stages.outgoingNanos;
		outlierCleanup[index] = stages.cleanupNanos;
	}

	private void startWindowIfNeeded(long nowNanos) {
		if (windowStartedNanos == 0L) {
			windowStartedNanos = Math.max(1L, nowNanos);
		}
	}

	private void resetWindow(long nowNanos) {
		windowStartedNanos = Math.max(1L, nowNanos);
		pollCount = 0L;
		pollsWithMovement = 0L;
		movedPlayersTotal = 0L;
		movedNpcsTotal = 0L;
		queuedPacketsTotal = 0L;
		queuedPacketsMax = 0;
		playerQueueMax = 0;
		backpressuredPlayerSamples = 0L;
		tickCount = 0L;
		outlierCount = 0L;
		outlierStored = 0;
		pollInterval.reset();
		pollDuration.reset();
		pollLateness.reset();
		tickDuration.reset();
	}

	private static long nanosToMillis(long nanos) {
		return Math.max(0L, nanos) / 1_000_000L;
	}

	public static final class TickStages {
		private static final TickStages EMPTY = new TickStages(0L, 0L, 0L, 0L, 0L, 0L, 0L);

		public final long worldUpdateNanos;
		public final long eventsNanos;
		public final long playersNanos;
		public final long npcsNanos;
		public final long updateClientsNanos;
		public final long outgoingNanos;
		public final long cleanupNanos;

		public TickStages(
			long worldUpdateNanos,
			long eventsNanos,
			long playersNanos,
			long npcsNanos,
			long updateClientsNanos,
			long outgoingNanos,
			long cleanupNanos) {
			this.worldUpdateNanos = Math.max(0L, worldUpdateNanos);
			this.eventsNanos = Math.max(0L, eventsNanos);
			this.playersNanos = Math.max(0L, playersNanos);
			this.npcsNanos = Math.max(0L, npcsNanos);
			this.updateClientsNanos = Math.max(0L, updateClientsNanos);
			this.outgoingNanos = Math.max(0L, outgoingNanos);
			this.cleanupNanos = Math.max(0L, cleanupNanos);
		}
	}

	public static final class BoundedHistogram {
		private final long[] upperBounds;
		private final long[] buckets;
		private long count;
		private long total;
		private long max;

		public BoundedHistogram(long[] configuredUpperBounds) {
			if (configuredUpperBounds == null || configuredUpperBounds.length == 0) {
				throw new IllegalArgumentException("histogram bounds required");
			}
			this.upperBounds = configuredUpperBounds.clone();
			this.buckets = new long[configuredUpperBounds.length];
			long previous = 0L;
			for (long bound : this.upperBounds) {
				if (bound <= previous) {
					throw new IllegalArgumentException("histogram bounds must increase");
				}
				previous = bound;
			}
		}

		public void record(long value) {
			long safeValue = Math.max(0L, value);
			count++;
			total += safeValue;
			max = Math.max(max, safeValue);
			for (int i = 0; i < upperBounds.length; i++) {
				if (safeValue <= upperBounds[i]) {
					buckets[i]++;
					return;
				}
			}
			buckets[buckets.length - 1]++;
		}

		public long count() {
			return count;
		}

		public long percentileUpperBound(double percentile) {
			if (count == 0L) {
				return 0L;
			}
			double boundedPercentile = Math.max(0.0D, Math.min(1.0D, percentile));
			long rank = Math.max(1L, (long)Math.ceil(count * boundedPercentile));
			long seen = 0L;
			for (int i = 0; i < buckets.length; i++) {
				seen += buckets[i];
				if (seen >= rank) {
					return upperBounds[i];
				}
			}
			return upperBounds[upperBounds.length - 1];
		}

		public String summaryMillis() {
			return "count:" + count
				+ ",avg:" + nanosToMillis(count == 0L ? 0L : total / count)
				+ ",p50:" + nanosToMillis(percentileUpperBound(0.50D))
				+ ",p95:" + nanosToMillis(percentileUpperBound(0.95D))
				+ ",p99:" + nanosToMillis(percentileUpperBound(0.99D))
				+ ",max:" + nanosToMillis(max);
		}

		public void reset() {
			for (int i = 0; i < buckets.length; i++) {
				buckets[i] = 0L;
			}
			count = 0L;
			total = 0L;
			max = 0L;
		}
	}
}
