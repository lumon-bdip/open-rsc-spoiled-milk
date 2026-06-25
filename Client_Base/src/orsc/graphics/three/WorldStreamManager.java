package orsc.graphics.three;

import java.util.LinkedHashMap;
import java.util.Map;

final class WorldStreamManager {
	private static final String ENABLED_PROPERTY = "spoiledmilk.worldStreamTelemetry";
	private static final String ENABLED_ENV = "SPOILED_MILK_WORLD_STREAM_TELEMETRY";
	private static final String REPORT_INTERVAL_PROPERTY = "spoiledmilk.worldStreamTelemetryInterval";
	private static final String SLOW_ACTIVE_WINDOW_MS_PROPERTY = "spoiledmilk.worldStreamSlowWindowMs";
	private static final boolean ENABLED = readBoolean(ENABLED_PROPERTY, ENABLED_ENV);
	private static final int REPORT_INTERVAL = Math.max(1, readInt(REPORT_INTERVAL_PROPERTY, 25));
	private static final long SLOW_ACTIVE_WINDOW_NANOS =
		Math.max(1L, readInt(SLOW_ACTIVE_WINDOW_MS_PROPERTY, 20)) * 1_000_000L;
	private static final int CHUNK_RECORD_LIMIT = 1024;

	private final Map<ChunkKey, ChunkRecord> chunks =
		new LinkedHashMap<ChunkKey, ChunkRecord>(CHUNK_RECORD_LIMIT, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<ChunkKey, ChunkRecord> eldest) {
				return size() > CHUNK_RECORD_LIMIT;
			}
		};

	private long preloadWindowRequests;
	private long chunkRequests;
	private long decodeStarts;
	private long cacheHits;
	private long cpuWindowBuilds;
	private long cpuWindowCacheHits;
	private long terrainModelInputBuilds;
	private long terrainModelInputCacheHits;
	private long wallModelInputBuilds;
	private long wallModelInputCacheHits;
	private long roofModelInputBuilds;
	private long roofModelInputCacheHits;
	private long worldModelProductBuilds;
	private long worldModelProductCacheHits;
	private long gpuMeshProductBuilds;
	private long gpuMeshProductTriangles;
	private long decodeFinishes;
	private long activeWindowLoads;
	private long slowActiveWindowLoads;
	private long activeWindowTotalNanos;
	private long activeWindowMaxNanos;
	private long decodeTotalNanos;
	private long decodeMaxNanos;
	private long cpuBuildTotalNanos;
	private long cpuBuildMaxNanos;

	static long now() {
		return ENABLED ? System.nanoTime() : 0L;
	}

	static long elapsedSince(long startNanos) {
		return ENABLED && startNanos != 0L ? System.nanoTime() - startNanos : 0L;
	}

	synchronized void markWindowRequested(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int lowOffset,
		int highOffset) {
		preloadWindowRequests++;
		for (int x = centerSectionX + lowOffset; x <= centerSectionX + highOffset; x++) {
			for (int y = centerSectionY + lowOffset; y <= centerSectionY + highOffset; y++) {
				markRequestedLocked(plane, x, y);
			}
		}
	}

	synchronized void markRequested(int plane, int sectionX, int sectionY) {
		markRequestedLocked(plane, sectionX, sectionY);
	}

	synchronized void markDecoding(int plane, int sectionX, int sectionY) {
		ChunkRecord record = recordForLocked(plane, sectionX, sectionY);
		record.state = ChunkState.DECODING;
		record.decodeStartNanos = now();
		decodeStarts++;
	}

	synchronized void markCacheHit(int plane, int sectionX, int sectionY) {
		ChunkRecord record = recordForLocked(plane, sectionX, sectionY);
		if (record.state.ordinal() < ChunkState.DECODED.ordinal()) {
			record.state = ChunkState.DECODED;
		}
		cacheHits++;
	}

	synchronized void markDecoded(int plane, int sectionX, int sectionY, long decodeNanos) {
		ChunkRecord record = recordForLocked(plane, sectionX, sectionY);
		record.state = ChunkState.DECODED;
		record.lastDecodeNanos = decodeNanos;
		decodeFinishes++;
		decodeTotalNanos += decodeNanos;
		if (decodeNanos > decodeMaxNanos) {
			decodeMaxNanos = decodeNanos;
		}
	}

	synchronized void markCpuBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset,
		long buildNanos) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		cpuWindowBuilds++;
		cpuBuildTotalNanos += buildNanos;
		if (buildNanos > cpuBuildMaxNanos) {
			cpuBuildMaxNanos = buildNanos;
		}
	}

	synchronized void markCpuCacheHit(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		cpuWindowCacheHits++;
	}

	synchronized void markTerrainInputBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		terrainModelInputBuilds++;
	}

	synchronized void markTerrainInputCacheHit(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		terrainModelInputCacheHits++;
	}

	synchronized void markWallInputBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		wallModelInputBuilds++;
	}

	synchronized void markWallInputCacheHit(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		wallModelInputCacheHits++;
	}

	synchronized void markRoofInputBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		roofModelInputBuilds++;
	}

	synchronized void markRoofInputCacheHit(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.CPU_BUILT);
		roofModelInputCacheHits++;
	}

	synchronized void markWorldProductBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.PRESENTABLE);
		worldModelProductBuilds++;
	}

	synchronized void markWorldProductCacheHit(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.PRESENTABLE);
		worldModelProductCacheHits++;
	}

	synchronized void markGpuMeshProductBuilt(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset,
		int triangles) {
		markWindowStateLocked(plane, centerSectionX, centerSectionY, grid, originOffset, ChunkState.PRESENTABLE);
		gpuMeshProductBuilds++;
		gpuMeshProductTriangles += Math.max(0, triangles);
	}

	synchronized void markActiveWindow(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset) {
		int activeMinX = centerSectionX - originOffset;
		int activeMinY = centerSectionY - originOffset;
		int activeMaxX = activeMinX + grid;
		int activeMaxY = activeMinY + grid;
		for (Map.Entry<ChunkKey, ChunkRecord> entry : chunks.entrySet()) {
			ChunkKey key = entry.getKey();
			ChunkRecord record = entry.getValue();
			if (record.state == ChunkState.ACTIVE
				&& key.plane == plane
				&& (key.sectionX < activeMinX || key.sectionX >= activeMaxX
					|| key.sectionY < activeMinY || key.sectionY >= activeMaxY)) {
				record.state = ChunkState.STALE;
			}
		}
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				ChunkRecord record = recordForLocked(
					plane,
					centerSectionX - originOffset + x,
					centerSectionY - originOffset + y);
				record.state = ChunkState.ACTIVE;
			}
		}
	}

	synchronized void recordActiveWindowLoad(
		int plane,
		int centerSectionX,
		int centerSectionY,
		long loadNanos) {
		activeWindowLoads++;
		activeWindowTotalNanos += loadNanos;
		if (loadNanos > activeWindowMaxNanos) {
			activeWindowMaxNanos = loadNanos;
		}
		boolean slowLoad = loadNanos >= SLOW_ACTIVE_WINDOW_NANOS;
		if (slowLoad) {
			slowActiveWindowLoads++;
		}
		if (ENABLED && (slowLoad || activeWindowLoads % REPORT_INTERVAL == 0)) {
			printReport(plane, centerSectionX, centerSectionY, loadNanos, slowLoad);
		}
	}

	private void markRequestedLocked(int plane, int sectionX, int sectionY) {
		ChunkRecord record = recordForLocked(plane, sectionX, sectionY);
		if (record.state == ChunkState.UNKNOWN) {
			record.state = ChunkState.REQUESTED;
		}
		chunkRequests++;
	}

	private void markWindowStateLocked(
		int plane,
		int centerSectionX,
		int centerSectionY,
		int grid,
		int originOffset,
		ChunkState state) {
		for (int y = 0; y < grid; y++) {
			for (int x = 0; x < grid; x++) {
				ChunkRecord record = recordForLocked(
					plane,
					centerSectionX - originOffset + x,
					centerSectionY - originOffset + y);
				if (record.state.ordinal() < state.ordinal()) {
					record.state = state;
				}
			}
		}
	}

	private ChunkRecord recordForLocked(int plane, int sectionX, int sectionY) {
		ChunkKey key = new ChunkKey(plane, sectionX, sectionY);
		ChunkRecord record = chunks.get(key);
		if (record == null) {
			record = new ChunkRecord();
			chunks.put(key, record);
		}
		return record;
	}

	private void printReport(
		int plane,
		int centerSectionX,
		int centerSectionY,
		long lastLoadNanos,
		boolean slowLoad) {
		StringBuilder summary = new StringBuilder();
		summary.append("[world-stream telemetry] activeWindows=").append(activeWindowLoads);
		if (slowLoad) {
			summary.append(" slow-window");
		}
		summary.append(" center=h").append(plane)
			.append('x').append(centerSectionX)
			.append('y').append(centerSectionY);
		summary.append(" last=").append(formatMillis(lastLoadNanos)).append("ms");
		summary.append(" avg=").append(formatMillis(activeWindowTotalNanos / Math.max(1L, activeWindowLoads))).append("ms");
		summary.append(" max=").append(formatMillis(activeWindowMaxNanos)).append("ms");
		summary.append(" slow=").append(slowActiveWindowLoads);
		System.out.println(summary);

		System.out.println(
			"[world-stream telemetry] chunks: tracked=" + chunks.size()
				+ " preloadWindows=" + preloadWindowRequests
				+ " requested=" + chunkRequests
				+ " decoding=" + decodeStarts
				+ " decoded=" + decodeFinishes
				+ " cacheHits=" + cacheHits
				+ " cpuWindows=" + cpuWindowBuilds
				+ " cpuCacheHits=" + cpuWindowCacheHits
				+ " terrainInputs=" + terrainModelInputBuilds
				+ " terrainInputHits=" + terrainModelInputCacheHits
				+ " wallInputs=" + wallModelInputBuilds
				+ " wallInputHits=" + wallModelInputCacheHits
				+ " roofInputs=" + roofModelInputBuilds
				+ " roofInputHits=" + roofModelInputCacheHits
				+ " worldProducts=" + worldModelProductBuilds
				+ " worldProductHits=" + worldModelProductCacheHits
				+ " gpuMeshProducts=" + gpuMeshProductBuilds
				+ " gpuMeshTriangles=" + gpuMeshProductTriangles
				+ " decodeAvg=" + formatMillis(decodeTotalNanos / Math.max(1L, decodeFinishes)) + "ms"
				+ " decodeMax=" + formatMillis(decodeMaxNanos) + "ms"
				+ " cpuAvg=" + formatMillis(cpuBuildTotalNanos / Math.max(1L, cpuWindowBuilds)) + "ms"
				+ " cpuMax=" + formatMillis(cpuBuildMaxNanos) + "ms");
	}

	private static boolean readBoolean(String property, String env) {
		String propertyValue = System.getProperty(property);
		if (propertyValue != null) {
			return "true".equalsIgnoreCase(propertyValue)
				|| "1".equals(propertyValue)
				|| "yes".equalsIgnoreCase(propertyValue);
		}
		String envValue = System.getenv(env);
		return envValue != null
			&& ("true".equalsIgnoreCase(envValue) || "1".equals(envValue) || "yes".equalsIgnoreCase(envValue));
	}

	private static int readInt(String property, int defaultValue) {
		String value = System.getProperty(property);
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	private static String formatMillis(long nanos) {
		return String.format("%.3f", nanos / 1_000_000.0D);
	}

	private enum ChunkState {
		UNKNOWN,
		REQUESTED,
		DECODING,
		DECODED,
		CPU_BUILT,
		GPU_UPLOADED,
		PRESENTABLE,
		ACTIVE,
		STALE
	}

	private static final class ChunkRecord {
		private ChunkState state = ChunkState.UNKNOWN;
		private long decodeStartNanos;
		private long lastDecodeNanos;
	}

	private static final class ChunkKey {
		private final int plane;
		private final int sectionX;
		private final int sectionY;

		private ChunkKey(int plane, int sectionX, int sectionY) {
			this.plane = plane;
			this.sectionX = sectionX;
			this.sectionY = sectionY;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ChunkKey)) {
				return false;
			}
			ChunkKey key = (ChunkKey) other;
			return plane == key.plane && sectionX == key.sectionX && sectionY == key.sectionY;
		}

		@Override
		public int hashCode() {
			int result = plane;
			result = 31 * result + sectionX;
			result = 31 * result + sectionY;
			return result;
		}
	}
}
