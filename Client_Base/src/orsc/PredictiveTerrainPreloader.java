package orsc;

import orsc.graphics.three.World;

/**
 * Owns best-effort terrain cache-warming decisions for movement targets,
 * active waypoints, and camera look-ahead. Movement, region selection, and
 * {@link World} cache policy remain with their existing owners.
 */
final class PredictiveTerrainPreloader {
	private static final int SOURCE_TARGET = 0;
	private static final int SOURCE_WAYPOINT = 1;
	private static final int SOURCE_CAMERA = 2;
	private static final int CAMERA_PRELOAD_DISTANCE_TILES = World.SECTION_SIZE;
	private static final int[] CAMERA_STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] CAMERA_STEP_Z = {1, 1, 0, -1, -1, -1, 0, 1};

	private int targetPlane = Integer.MIN_VALUE;
	private int targetSectionX = Integer.MIN_VALUE;
	private int targetSectionZ = Integer.MIN_VALUE;
	private int waypointPlane = Integer.MIN_VALUE;
	private int waypointSectionX = Integer.MIN_VALUE;
	private int waypointSectionZ = Integer.MIN_VALUE;
	private int cameraPlane = Integer.MIN_VALUE;
	private int cameraSectionX = Integer.MIN_VALUE;
	private int cameraSectionZ = Integer.MIN_VALUE;

	void preloadIncomingWorldPosition(
		World world,
		int worldX,
		int worldZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane,
		boolean initialRegionLoaded,
		boolean loadingArea) {
		if (!canPreload(world != null, initialRegionLoaded, loadingArea)) {
			return;
		}
		preload(world, prepareTargetRequest(worldX, worldZ, worldOffsetX, worldOffsetZ, plane));
	}

	void preloadLocalTarget(
		World world,
		int localTileX,
		int localTileZ,
		int midRegionBaseX,
		int midRegionBaseZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane,
		boolean initialRegionLoaded,
		boolean loadingArea) {
		if (!canPreload(world != null, initialRegionLoaded, loadingArea)) {
			return;
		}
		preload(world, prepareTargetRequest(
			midRegionBaseX + localTileX,
			midRegionBaseZ + localTileZ,
			worldOffsetX,
			worldOffsetZ,
			plane));
	}

	void preloadActiveMovementContext(
		World world,
		ORSCharacter localPlayer,
		int midRegionBaseX,
		int midRegionBaseZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane,
		int cameraRotation,
		int tileSize,
		boolean initialRegionLoaded,
		boolean loadingArea) {
		if (localPlayer == null || !canPreload(world != null, initialRegionLoaded, loadingArea)) {
			return;
		}
		preload(world, prepareWaypointRequest(
			localPlayer,
			midRegionBaseX,
			midRegionBaseZ,
			worldOffsetX,
			worldOffsetZ,
			plane,
			tileSize));
		preload(world, prepareCameraRequest(
			localPlayer,
			midRegionBaseX,
			midRegionBaseZ,
			worldOffsetX,
			worldOffsetZ,
			plane,
			cameraRotation,
			tileSize));
	}

	PreloadRequest prepareTargetRequest(
		int worldX,
		int worldZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane) {
		return prepareRequest(SOURCE_TARGET, worldX, worldZ, worldOffsetX, worldOffsetZ, plane);
	}

	PreloadRequest prepareWaypointRequest(
		ORSCharacter localPlayer,
		int midRegionBaseX,
		int midRegionBaseZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane,
		int tileSize) {
		if (localPlayer == null) {
			return null;
		}
		int nextWaypoint = localPlayer.waypointIndexNext;
		if (nextWaypoint < 0 || nextWaypoint >= localPlayer.waypointsX.length) {
			return null;
		}
		int inactiveWaypoint = (localPlayer.waypointIndexCurrent + 1) % localPlayer.waypointsX.length;
		if (nextWaypoint == inactiveWaypoint) {
			return null;
		}
		int targetTileX = localPixelToTile(localPlayer.waypointsX[nextWaypoint], tileSize);
		int targetTileZ = localPixelToTile(localPlayer.waypointsZ[nextWaypoint], tileSize);
		return prepareRequest(
			SOURCE_WAYPOINT,
			midRegionBaseX + targetTileX,
			midRegionBaseZ + targetTileZ,
			worldOffsetX,
			worldOffsetZ,
			plane);
	}

	PreloadRequest prepareCameraRequest(
		ORSCharacter localPlayer,
		int midRegionBaseX,
		int midRegionBaseZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane,
		int cameraRotation,
		int tileSize) {
		if (localPlayer == null) {
			return null;
		}
		int direction = ((cameraRotation + 16) / 32) & 7;
		int playerTileX = localPixelToTile(localPlayer.currentX, tileSize);
		int playerTileZ = localPixelToTile(localPlayer.currentZ, tileSize);
		int targetTileX = playerTileX + CAMERA_STEP_X[direction] * CAMERA_PRELOAD_DISTANCE_TILES;
		int targetTileZ = playerTileZ + CAMERA_STEP_Z[direction] * CAMERA_PRELOAD_DISTANCE_TILES;
		return prepareRequest(
			SOURCE_CAMERA,
			midRegionBaseX + targetTileX,
			midRegionBaseZ + targetTileZ,
			worldOffsetX,
			worldOffsetZ,
			plane);
	}

	static boolean canPreload(boolean worldAvailable, boolean initialRegionLoaded, boolean loadingArea) {
		return worldAvailable && initialRegionLoaded && !loadingArea;
	}

	static int localPixelToTile(int localPixel, int tileSize) {
		return Math.floorDiv(localPixel - 64, tileSize);
	}

	void reset() {
		targetPlane = Integer.MIN_VALUE;
		targetSectionX = Integer.MIN_VALUE;
		targetSectionZ = Integer.MIN_VALUE;
		waypointPlane = Integer.MIN_VALUE;
		waypointSectionX = Integer.MIN_VALUE;
		waypointSectionZ = Integer.MIN_VALUE;
		cameraPlane = Integer.MIN_VALUE;
		cameraSectionX = Integer.MIN_VALUE;
		cameraSectionZ = Integer.MIN_VALUE;
	}

	private PreloadRequest prepareRequest(
		int source,
		int worldX,
		int worldZ,
		int worldOffsetX,
		int worldOffsetZ,
		int plane) {
		int preloadWorldX = worldX + worldOffsetX;
		int preloadWorldZ = worldZ + worldOffsetZ;
		int sectionX = World.worldTileToSection(preloadWorldX);
		int sectionZ = World.worldTileToSection(preloadWorldZ);
		if (isSectionCurrent(source, plane, sectionX, sectionZ)) {
			return null;
		}
		markSection(source, plane, sectionX, sectionZ);
		return new PreloadRequest(preloadWorldX, preloadWorldZ, plane);
	}

	private boolean isSectionCurrent(int source, int plane, int sectionX, int sectionZ) {
		if (source == SOURCE_WAYPOINT) {
			return waypointPlane == plane && waypointSectionX == sectionX && waypointSectionZ == sectionZ;
		}
		if (source == SOURCE_CAMERA) {
			return cameraPlane == plane && cameraSectionX == sectionX && cameraSectionZ == sectionZ;
		}
		return targetPlane == plane && targetSectionX == sectionX && targetSectionZ == sectionZ;
	}

	private void markSection(int source, int plane, int sectionX, int sectionZ) {
		if (source == SOURCE_WAYPOINT) {
			waypointPlane = plane;
			waypointSectionX = sectionX;
			waypointSectionZ = sectionZ;
			return;
		}
		if (source == SOURCE_CAMERA) {
			cameraPlane = plane;
			cameraSectionX = sectionX;
			cameraSectionZ = sectionZ;
			return;
		}
		targetPlane = plane;
		targetSectionX = sectionX;
		targetSectionZ = sectionZ;
	}

	private static void preload(World world, PreloadRequest request) {
		if (request != null) {
			world.preloadSections(request.worldX, request.worldZ, request.plane);
		}
	}

	static final class PreloadRequest {
		private final int worldX;
		private final int worldZ;
		private final int plane;

		private PreloadRequest(int worldX, int worldZ, int plane) {
			this.worldX = worldX;
			this.worldZ = worldZ;
			this.plane = plane;
		}

		int getWorldX() {
			return worldX;
		}

		int getWorldZ() {
			return worldZ;
		}

		int getPlane() {
			return plane;
		}
	}
}
