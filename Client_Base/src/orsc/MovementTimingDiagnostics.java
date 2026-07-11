package orsc;

final class MovementTimingDiagnostics {
	private static final String SUMMARY_INTERVAL_PROPERTY =
		"spoiledmilk.movementDiagnosticSummaryMillis";
	private static final String ARRIVAL_OUTLIER_PROPERTY =
		"spoiledmilk.movementDiagnosticArrivalOutlierMillis";
	private static final long DEFAULT_SUMMARY_INTERVAL_NANOS = 5_000_000_000L;
	private static final long DEFAULT_ARRIVAL_OUTLIER_NANOS = 650_000_000L;
	private static final int RECENT_PACKET_LIMIT = 64;
	private static final int MARKER_PACKET_LIMIT = 16;
	private static final int OUTLIER_EVENT_LIMIT_PER_WINDOW = 8;
	private static final long[] HISTOGRAM_BOUNDS_NANOS = new long[] {
		1_000_000L,
		2_000_000L,
		5_000_000L,
		10_000_000L,
		16_000_000L,
		25_000_000L,
		50_000_000L,
		100_000_000L,
		250_000_000L,
		430_000_000L,
		500_000_000L,
		650_000_000L,
		1_000_000_000L,
		Long.MAX_VALUE
	};

	private static final boolean ENABLED = RendererDiagnosticSession.isEnabled();
	private static final long SUMMARY_INTERVAL_NANOS = Math.max(
		1_000_000_000L,
		readLong(SUMMARY_INTERVAL_PROPERTY, DEFAULT_SUMMARY_INTERVAL_NANOS / 1_000_000L)
			* 1_000_000L);
	private static final long ARRIVAL_OUTLIER_NANOS = Math.max(
		1_000_000L,
		readLong(ARRIVAL_OUTLIER_PROPERTY, DEFAULT_ARRIVAL_OUTLIER_NANOS / 1_000_000L)
			* 1_000_000L);
	private static final TimingAccumulator ACCUMULATOR = new TimingAccumulator(
		SUMMARY_INTERVAL_NANOS,
		ARRIVAL_OUTLIER_NANOS,
		RECENT_PACKET_LIMIT,
		OUTLIER_EVENT_LIMIT_PER_WINDOW);

	private MovementTimingDiagnostics() {
	}

	static boolean isEnabled() {
		return ENABLED;
	}

	static void recordMovementPacket(int playerRecords, int npcRecords) {
		if (!ENABLED) {
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			boolean outlier = ACCUMULATOR.recordPacket(
				now,
				TimingAccumulator.LANE_MOVEMENT,
				-1,
				-1,
				Math.max(0, playerRecords) + Math.max(0, npcRecords));
			if (outlier) {
				writePacketOutlier("movement", now);
			}
			maybeWriteSummary(now);
		}
	}

	static void recordMovementSnapshot(
		int serverTick,
		int sequence,
		int playerRecords,
		int npcRecords) {
		if (!ENABLED) {
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			boolean outlier = ACCUMULATOR.recordPacket(
				now,
				TimingAccumulator.LANE_SNAPSHOT,
				serverTick,
				sequence,
				Math.max(0, playerRecords) + Math.max(0, npcRecords));
			if (outlier) {
				writePacketOutlier("snapshot", now);
			}
			maybeWriteSummary(now);
		}
	}

	static void observeLocalPlayer(ORSCharacter localPlayer) {
		if (!ENABLED || localPlayer == null) {
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			ACCUMULATOR.observeLocal(now, waypointDepth(localPlayer));
			maybeWriteSummary(now);
		}
	}

	static void recordWaypointAppend(ORSCharacter character, boolean localPlayer, boolean wasIdle) {
		if (!ENABLED || character == null) {
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			ACCUMULATOR.recordWaypointAppend(
				now,
				localPlayer,
				wasIdle,
				waypointDepth(character));
			maybeWriteSummary(now);
		}
	}

	static void recordEndpointSnap(boolean localPlayer, int waypointDepth) {
		if (!ENABLED) {
			return;
		}
		synchronized (ACCUMULATOR) {
			ACCUMULATOR.recordEndpointSnap(localPlayer, waypointDepth);
		}
	}

	static void recordCorrectionSnap(boolean localPlayer, String reason, int distancePixels) {
		if (!ENABLED) {
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			boolean writeEvent = ACCUMULATOR.recordCorrectionSnap(
				localPlayer,
				Math.max(0, distancePixels));
			if (!writeEvent) {
				return;
			}
			RendererDiagnosticSession.Record event =
				RendererDiagnosticSession.newEventRecord("movement.correction-snap");
			if (event != null) {
				event.bool("localPlayer", localPlayer);
				event.string("reason", safeReason(reason));
				event.number("distancePixels", Math.max(0, distancePixels));
				event.number("waypointDepth", ACCUMULATOR.currentWaypointDepth);
				RenderTelemetry.appendMovementCorrelation(event, now);
				RendererDiagnosticSession.writeEventRecord(event);
			}
		}
	}

	static void markStutterObserved(ORSCharacter localPlayer) {
		if (!ENABLED) {
			System.out.println("[movement diagnostics] marker ignored; launch with renderer diagnostics enabled.");
			return;
		}
		long now = System.nanoTime();
		synchronized (ACCUMULATOR) {
			if (localPlayer != null) {
				ACCUMULATOR.observeLocal(now, waypointDepth(localPlayer));
			}
			ACCUMULATOR.markers++;
			RendererDiagnosticSession.Record event =
				RendererDiagnosticSession.newEventRecord("movement.stutter-observed");
			if (event != null) {
				appendAccumulator(event, now, "movement");
				event.strings("movement.recentPackets", ACCUMULATOR.recentPacketLines(MARKER_PACKET_LIMIT));
				RenderTelemetry.appendMovementCorrelation(event, now);
				RendererDiagnosticSession.writeEventRecord(event);
			}
			System.out.println("[movement diagnostics] stutter marker recorded (Ctrl+F8).");
		}
	}

	private static void writePacketOutlier(String lane, long now) {
		RendererDiagnosticSession.Record event =
			RendererDiagnosticSession.newEventRecord("movement.packet-arrival-outlier");
		if (event == null) {
			return;
		}
		event.string("lane", lane);
		event.number("arrivalIntervalNanos", ACCUMULATOR.latestArrivalIntervalNanos);
		event.number("serverTick", ACCUMULATOR.latestServerTick);
		event.number("sequence", ACCUMULATOR.latestSequence);
		event.number("sequenceDelta", ACCUMULATOR.latestSequenceDelta);
		event.number("serverTickDelta", ACCUMULATOR.latestServerTickDelta);
		event.number("records", ACCUMULATOR.latestRecords);
		RenderTelemetry.appendMovementCorrelation(event, now);
		RendererDiagnosticSession.writeEventRecord(event);
	}

	private static void maybeWriteSummary(long now) {
		if (!ACCUMULATOR.summaryDue(now)) {
			return;
		}
		RendererDiagnosticSession.Record event =
			RendererDiagnosticSession.newEventRecord("movement.timing-summary");
		if (event != null) {
			appendAccumulator(event, now, "movement");
			RenderTelemetry.appendMovementCorrelation(event, now);
			RendererDiagnosticSession.writeEventRecord(event);
		}
		ACCUMULATOR.startNextSummaryWindow(now);
	}

	private static void appendAccumulator(
		RendererDiagnosticSession.Record record,
		long now,
		String prefix) {
		record.number(prefix + ".summaryWindowNanos", ACCUMULATOR.windowDuration(now));
		record.number(prefix + ".movementPackets", ACCUMULATOR.movementPackets);
		record.number(prefix + ".snapshotPackets", ACCUMULATOR.snapshotPackets);
		record.number(prefix + ".packetRecords", ACCUMULATOR.packetRecords);
		record.number(prefix + ".latestServerTick", ACCUMULATOR.latestServerTick);
		record.number(prefix + ".latestSequence", ACCUMULATOR.latestSequence);
		record.number(prefix + ".latestSequenceDelta", ACCUMULATOR.latestSequenceDelta);
		record.number(prefix + ".latestServerTickDelta", ACCUMULATOR.latestServerTickDelta);
		record.number(prefix + ".latestArrivalIntervalNanos", ACCUMULATOR.latestArrivalIntervalNanos);
		record.number(prefix + ".waypointAppends", ACCUMULATOR.waypointAppends);
		record.number(prefix + ".idleWaypointAppends", ACCUMULATOR.idleWaypointAppends);
		record.number(prefix + ".currentWaypointDepth", ACCUMULATOR.currentWaypointDepth);
		record.number(prefix + ".maxWaypointDepth", ACCUMULATOR.maxWaypointDepth);
		record.bool(prefix + ".currentlyIdle", ACCUMULATOR.localIdle);
		record.number(prefix + ".currentIdleNanos", ACCUMULATOR.currentIdleDuration(now));
		record.number(prefix + ".idleTransitions", ACCUMULATOR.idleTransitions);
		record.number(prefix + ".latestCompletedIdleNanos", ACCUMULATOR.latestCompletedIdleNanos);
		record.number(prefix + ".endpointSnaps", ACCUMULATOR.endpointSnaps);
		record.number(prefix + ".localEndpointSnaps", ACCUMULATOR.localEndpointSnaps);
		record.number(prefix + ".correctionSnaps", ACCUMULATOR.correctionSnaps);
		record.number(prefix + ".localCorrectionSnaps", ACCUMULATOR.localCorrectionSnaps);
		record.number(prefix + ".maxCorrectionDistancePixels", ACCUMULATOR.maxCorrectionDistancePixels);
		record.number(prefix + ".correctionEvents", ACCUMULATOR.correctionEvents);
		record.number(prefix + ".correctionEventsSuppressed", ACCUMULATOR.correctionEventsSuppressed);
		record.number(prefix + ".markers", ACCUMULATOR.markers);
		record.number(prefix + ".arrivalOutliers", ACCUMULATOR.arrivalOutliers);
		record.number(prefix + ".arrivalOutlierEvents", ACCUMULATOR.outlierEvents);
		record.number(prefix + ".arrivalOutlierEventsSuppressed", ACCUMULATOR.outlierEventsSuppressed);
		ACCUMULATOR.movementArrival.append(record, prefix + ".movementArrival");
		ACCUMULATOR.snapshotArrival.append(record, prefix + ".snapshotArrival");
		ACCUMULATOR.idleDuration.append(record, prefix + ".idleDuration");
		ACCUMULATOR.waypointDepth.append(record, prefix + ".waypointDepth");
	}

	static int waypointDepth(ORSCharacter character) {
		if (character == null || character.waypointsX == null || character.waypointsX.length == 0) {
			return 0;
		}
		int capacity = character.waypointsX.length;
		int inactive = (character.waypointIndexCurrent + 1) % capacity;
		int next = character.waypointIndexNext;
		if (next == inactive) {
			return 0;
		}
		return inactive <= next ? inactive + capacity - next : inactive - next;
	}

	private static String safeReason(String reason) {
		if ("distance".equals(reason) || "region-reset".equals(reason)) {
			return reason;
		}
		return "other";
	}

	private static long readLong(String property, long defaultValue) {
		String value = System.getProperty(property);
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	static final class TimingAccumulator {
		static final byte LANE_MOVEMENT = 1;
		static final byte LANE_SNAPSHOT = 2;

		private final long summaryIntervalNanos;
		private final long arrivalOutlierNanos;
		private final int outlierEventLimit;
		private final byte[] recentLane;
		private final long[] recentInterval;
		private final int[] recentServerTick;
		private final int[] recentSequence;
		private final int[] recentRecords;
		private final FixedHistogram movementArrival = new FixedHistogram(HISTOGRAM_BOUNDS_NANOS);
		private final FixedHistogram snapshotArrival = new FixedHistogram(HISTOGRAM_BOUNDS_NANOS);
		private final FixedHistogram idleDuration = new FixedHistogram(HISTOGRAM_BOUNDS_NANOS);
		private final FixedHistogram waypointDepth = new FixedHistogram(
			new long[] { 0L, 1L, 2L, 3L, 5L, 8L, Long.MAX_VALUE });

		private long windowStartedNanos;
		private long lastMovementArrivalNanos;
		private long lastSnapshotArrivalNanos;
		private int previousServerTick = -1;
		private int previousSequence = -1;
		private int recentIndex;
		private int recentCount;
		private long movementPackets;
		private long snapshotPackets;
		private long packetRecords;
		private int latestServerTick = -1;
		private int latestSequence = -1;
		private int latestSequenceDelta;
		private int latestServerTickDelta;
		private long latestArrivalIntervalNanos;
		private int latestRecords;
		private long waypointAppends;
		private long idleWaypointAppends;
		private int currentWaypointDepth;
		private int maxWaypointDepth;
		private boolean localIdle;
		private long localIdleStartedNanos;
		private long idleTransitions;
		private long latestCompletedIdleNanos;
		private long endpointSnaps;
		private long localEndpointSnaps;
		private long correctionSnaps;
		private long localCorrectionSnaps;
		private int maxCorrectionDistancePixels;
		private int correctionEvents;
		private long correctionEventsSuppressed;
		private long markers;
		private long arrivalOutliers;
		private int outlierEvents;
		private long outlierEventsSuppressed;

		TimingAccumulator(
			long summaryIntervalNanos,
			long arrivalOutlierNanos,
			int recentPacketLimit,
			int outlierEventLimit) {
			this.summaryIntervalNanos = Math.max(1L, summaryIntervalNanos);
			this.arrivalOutlierNanos = Math.max(1L, arrivalOutlierNanos);
			this.outlierEventLimit = Math.max(0, outlierEventLimit);
			int boundedRecentLimit = Math.max(1, recentPacketLimit);
			this.recentLane = new byte[boundedRecentLimit];
			this.recentInterval = new long[boundedRecentLimit];
			this.recentServerTick = new int[boundedRecentLimit];
			this.recentSequence = new int[boundedRecentLimit];
			this.recentRecords = new int[boundedRecentLimit];
		}

		boolean recordPacket(long now, byte lane, int serverTick, int sequence, int records) {
			startWindow(now);
			long previous = lane == LANE_SNAPSHOT ? lastSnapshotArrivalNanos : lastMovementArrivalNanos;
			long interval = previous == 0L ? 0L : Math.max(0L, now - previous);
			if (lane == LANE_SNAPSHOT) {
				lastSnapshotArrivalNanos = now;
				snapshotPackets++;
				if (interval > 0L) {
					snapshotArrival.record(interval);
				}
				latestSequenceDelta = previousSequence < 0 ? 0 : sequence - previousSequence;
				latestServerTickDelta = previousServerTick < 0 ? 0 : serverTick - previousServerTick;
				previousSequence = sequence;
				previousServerTick = serverTick;
				latestServerTick = serverTick;
				latestSequence = sequence;
			} else {
				lastMovementArrivalNanos = now;
				movementPackets++;
				if (interval > 0L) {
					movementArrival.record(interval);
				}
			}
			packetRecords += Math.max(0, records);
			latestRecords = Math.max(0, records);
			latestArrivalIntervalNanos = interval;
			rememberPacket(lane, interval, serverTick, sequence, records);
			if (interval < arrivalOutlierNanos) {
				return false;
			}
			arrivalOutliers++;
			if (outlierEvents >= outlierEventLimit) {
				outlierEventsSuppressed++;
				return false;
			}
			outlierEvents++;
			return true;
		}

		void observeLocal(long now, int depth) {
			startWindow(now);
			int safeDepth = Math.max(0, depth);
			currentWaypointDepth = safeDepth;
			maxWaypointDepth = Math.max(maxWaypointDepth, safeDepth);
			waypointDepth.record(safeDepth);
			if (safeDepth == 0) {
				if (!localIdle) {
					localIdle = true;
					localIdleStartedNanos = now;
				}
				return;
			}
			finishIdle(now);
		}

		void recordWaypointAppend(long now, boolean local, boolean wasIdle, int depth) {
			startWindow(now);
			waypointAppends++;
			if (wasIdle) {
				idleWaypointAppends++;
			}
			if (local) {
				observeLocal(now, depth);
			}
		}

		void recordEndpointSnap(boolean local, int depth) {
			endpointSnaps++;
			if (local) {
				localEndpointSnaps++;
				currentWaypointDepth = Math.max(0, depth);
			}
		}

		boolean recordCorrectionSnap(boolean local, int distancePixels) {
			correctionSnaps++;
			if (local) {
				localCorrectionSnaps++;
			}
			maxCorrectionDistancePixels = Math.max(maxCorrectionDistancePixels, distancePixels);
			if (correctionEvents >= outlierEventLimit) {
				correctionEventsSuppressed++;
				return false;
			}
			correctionEvents++;
			return true;
		}

		boolean summaryDue(long now) {
			return windowStartedNanos != 0L && now - windowStartedNanos >= summaryIntervalNanos;
		}

		long windowDuration(long now) {
			return windowStartedNanos == 0L ? 0L : Math.max(0L, now - windowStartedNanos);
		}

		long currentIdleDuration(long now) {
			return !localIdle || localIdleStartedNanos == 0L
				? 0L
				: Math.max(0L, now - localIdleStartedNanos);
		}

		void startNextSummaryWindow(long now) {
			windowStartedNanos = Math.max(1L, now);
			movementPackets = 0L;
			snapshotPackets = 0L;
			packetRecords = 0L;
			waypointAppends = 0L;
			idleWaypointAppends = 0L;
			maxWaypointDepth = currentWaypointDepth;
			idleTransitions = 0L;
			endpointSnaps = 0L;
			localEndpointSnaps = 0L;
			correctionSnaps = 0L;
			localCorrectionSnaps = 0L;
			maxCorrectionDistancePixels = 0;
			correctionEvents = 0;
			correctionEventsSuppressed = 0L;
			markers = 0L;
			arrivalOutliers = 0L;
			outlierEvents = 0;
			outlierEventsSuppressed = 0L;
			movementArrival.reset();
			snapshotArrival.reset();
			idleDuration.reset();
			waypointDepth.reset();
		}

		String[] recentPacketLines(int requestedLimit) {
			int count = Math.min(recentCount, Math.max(0, requestedLimit));
			String[] lines = new String[count];
			for (int i = 0; i < count; i++) {
				int index = (recentIndex - count + i + recentLane.length) % recentLane.length;
				lines[i] = (recentLane[index] == LANE_SNAPSHOT ? "snapshot" : "movement")
					+ ":dt=" + recentInterval[index]
					+ ",tick=" + recentServerTick[index]
					+ ",seq=" + recentSequence[index]
					+ ",records=" + recentRecords[index];
			}
			return lines;
		}

		long movementPacketCount() {
			return movementPackets;
		}

		long snapshotPacketCount() {
			return snapshotPackets;
		}

		long packetRecordCount() {
			return packetRecords;
		}

		long idleTransitionCount() {
			return idleTransitions;
		}

		long latestCompletedIdleDuration() {
			return latestCompletedIdleNanos;
		}

		int currentDepth() {
			return currentWaypointDepth;
		}

		long arrivalOutlierCount() {
			return arrivalOutliers;
		}

		long suppressedOutlierEventCount() {
			return outlierEventsSuppressed;
		}

		private void finishIdle(long now) {
			if (!localIdle) {
				return;
			}
			latestCompletedIdleNanos = localIdleStartedNanos == 0L
				? 0L
				: Math.max(0L, now - localIdleStartedNanos);
			if (latestCompletedIdleNanos > 0L) {
				idleDuration.record(latestCompletedIdleNanos);
				idleTransitions++;
			}
			localIdle = false;
			localIdleStartedNanos = 0L;
		}

		private void rememberPacket(byte lane, long interval, int tick, int sequence, int records) {
			recentLane[recentIndex] = lane;
			recentInterval[recentIndex] = interval;
			recentServerTick[recentIndex] = tick;
			recentSequence[recentIndex] = sequence;
			recentRecords[recentIndex] = Math.max(0, records);
			recentIndex = (recentIndex + 1) % recentLane.length;
			if (recentCount < recentLane.length) {
				recentCount++;
			}
		}

		private void startWindow(long now) {
			if (windowStartedNanos == 0L) {
				windowStartedNanos = Math.max(1L, now);
			}
		}
	}

	static final class FixedHistogram {
		private final long[] upperBounds;
		private final long[] buckets;
		private long count;
		private long total;
		private long max;

		FixedHistogram(long[] bounds) {
			if (bounds == null || bounds.length == 0) {
				throw new IllegalArgumentException("histogram bounds required");
			}
			upperBounds = bounds.clone();
			buckets = new long[bounds.length];
			long previous = -1L;
			for (long bound : upperBounds) {
				if (bound <= previous) {
					throw new IllegalArgumentException("histogram bounds must increase");
				}
				previous = bound;
			}
		}

		void record(long value) {
			long safe = Math.max(0L, value);
			count++;
			total += safe;
			max = Math.max(max, safe);
			for (int i = 0; i < upperBounds.length; i++) {
				if (safe <= upperBounds[i]) {
					buckets[i]++;
					return;
				}
			}
			buckets[buckets.length - 1]++;
		}

		long percentileUpperBound(double percentile) {
			if (count == 0L) {
				return 0L;
			}
			long rank = Math.max(1L, (long)Math.ceil(count * Math.max(0.0D, Math.min(1.0D, percentile))));
			long seen = 0L;
			for (int i = 0; i < buckets.length; i++) {
				seen += buckets[i];
				if (seen >= rank) {
					return upperBounds[i];
				}
			}
			return upperBounds[upperBounds.length - 1];
		}

		long count() {
			return count;
		}

		void append(RendererDiagnosticSession.Record record, String prefix) {
			record.number(prefix + ".count", count);
			record.number(prefix + ".average", count == 0L ? 0L : total / count);
			record.number(prefix + ".p50UpperBound", percentileUpperBound(0.50D));
			record.number(prefix + ".p95UpperBound", percentileUpperBound(0.95D));
			record.number(prefix + ".p99UpperBound", percentileUpperBound(0.99D));
			record.number(prefix + ".max", max);
		}

		void reset() {
			for (int i = 0; i < buckets.length; i++) {
				buckets[i] = 0L;
			}
			count = 0L;
			total = 0L;
			max = 0L;
		}
	}
}
