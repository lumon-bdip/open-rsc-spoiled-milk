package orsc;

import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DWorldChunkFrame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RemasterShadowClassifier {
	static final boolean DIRECT_WALL_SEGMENT_CLIP = false;

	static RemasterShadowInventory inspectInventory(
		Renderer3DWorldChunkFrame worldChunkFrame) {
		if (worldChunkFrame == null || worldChunkFrame.getChunkCount() <= 0) {
			return RemasterShadowInventory.EMPTY;
		}

		int receiverChunks = 0;
		int receiverTriangles = 0;
		int totalCasters = 0;
		int wallCasters = 0;
		int gameObjectCasters = 0;
		int wallObjectCasters = 0;
		int outdoorOnlyCasters = 0;
		int clippingCandidates = 0;
		int roofedReceivers = 0;
		int outdoorReceivers = 0;
		int unknownReceivers = 0;
		int roofedCasters = 0;
		int outdoorCasters = 0;
		int unknownCasters = 0;
		int sunlightEligibleCasters = 0;
		int sunlightSuppressedRoofedCasters = 0;
		int sunlightSuppressedUnknownCasters = 0;
		RemasterShadowRoofCoverage roofCoverage = RemasterShadowRoofCoverage.from(worldChunkFrame);
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : worldChunkFrame.getChunks()) {
			int terrainTriangles = Math.max(0, chunk.getTerrainTriangles());
			if (terrainTriangles > 0) {
				receiverChunks++;
				receiverTriangles += terrainTriangles;
			}
			int triangleLimit = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleLimit; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				int classification = roofClassificationForTriangle(roofCoverage, chunk, triangle);
				if (classification > 0) {
					roofedReceivers++;
				} else if (classification == 0) {
					outdoorReceivers++;
				} else {
					unknownReceivers++;
				}
			}
			int casterCount = chunk.getShadowCasterCount();
			for (int index = 0; index < casterCount; index++) {
				Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
				if (!isEligibleCaster(caster)) {
					continue;
				}
				totalCasters++;
				Renderer3DModelKind kind = caster.getModelKind();
				if (kind == Renderer3DModelKind.WALL) {
					wallCasters++;
				} else if (kind == Renderer3DModelKind.GAME_OBJECT) {
					gameObjectCasters++;
				} else if (kind == Renderer3DModelKind.WALL_OBJECT) {
					wallObjectCasters++;
				}
				if (caster.isOutdoorOnly()) {
					outdoorOnlyCasters++;
				}
				if (isClippingCandidate(caster)) {
					clippingCandidates++;
				}
				int classification = roofClassificationForCaster(roofCoverage, chunk, caster);
				if (classification > 0) {
					roofedCasters++;
					sunlightSuppressedRoofedCasters++;
				} else if (classification == 0) {
					outdoorCasters++;
					sunlightEligibleCasters++;
				} else {
					unknownCasters++;
					sunlightSuppressedUnknownCasters++;
				}
			}
		}
		return new RemasterShadowInventory(
			receiverChunks,
			receiverTriangles,
			totalCasters,
			wallCasters,
			gameObjectCasters,
			wallObjectCasters,
			outdoorOnlyCasters,
			clippingCandidates,
			roofedReceivers,
			outdoorReceivers,
			unknownReceivers,
			roofedCasters,
			outdoorCasters,
			unknownCasters,
			sunlightEligibleCasters,
			sunlightSuppressedRoofedCasters,
			sunlightSuppressedUnknownCasters);
	}

	static boolean isEligibleCaster(Renderer3DWorldChunkFrame.ShadowCaster caster) {
		return caster != null && caster.getHeight() > 0 && caster.getOpacity() > 0;
	}

	static boolean isClippingCandidate(Renderer3DWorldChunkFrame.ShadowCaster caster) {
		if (!isEligibleCaster(caster)) {
			return false;
		}
		Renderer3DModelKind kind = caster.getModelKind();
		return kind == Renderer3DModelKind.WALL || kind == Renderer3DModelKind.WALL_OBJECT;
	}

	static int roofClassificationForTriangle(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		int triangle) {
		if (chunk == null || roofCoverage == null) {
			return -1;
		}
		int sourceIndex = triangle * 3;
		int x = 0;
		int z = 0;
		for (int corner = 0; corner < 3; corner++) {
			int vertex = chunk.getIndex(sourceIndex + corner);
			int coord = vertex * 3;
			x += chunk.getVertexCoord(coord);
			z += chunk.getVertexCoord(coord + 2);
		}
		return roofCoverage.classify(chunk.getPlane(), x / 3, z / 3);
	}

	static int roofClassificationForCaster(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		Renderer3DWorldChunkFrame.ShadowCaster caster) {
		if (chunk == null || caster == null || roofCoverage == null) {
			return -1;
		}
		if (caster.getModelKind() == Renderer3DModelKind.WALL
			|| caster.getModelKind() == Renderer3DModelKind.WALL_OBJECT) {
			return roofCoverage.classifyBoundaryCaster(
				chunk.getPlane(),
				caster.getBaseX0(),
				caster.getBaseZ0(),
				caster.getBaseX1(),
				caster.getBaseZ1());
		}
		int x = (caster.getBaseX0() + caster.getBaseX1()) / 2;
		int z = (caster.getBaseZ0() + caster.getBaseZ1()) / 2;
		return roofCoverage.classify(chunk.getPlane(), x, z);
	}
}

final class RemasterShadowInventory {
	static final RemasterShadowInventory EMPTY =
		new RemasterShadowInventory(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	final int receiverChunks;
	final int receiverTriangles;
	final int totalCasters;
	final int wallCasters;
	final int gameObjectCasters;
	final int wallObjectCasters;
	final int outdoorOnlyCasters;
	final int clippingCandidates;
	final int roofedReceivers;
	final int outdoorReceivers;
	final int unknownReceivers;
	final int roofedCasters;
	final int outdoorCasters;
	final int unknownCasters;
	final int sunlightEligibleCasters;
	final int sunlightSuppressedRoofedCasters;
	final int sunlightSuppressedUnknownCasters;

	RemasterShadowInventory(
		int receiverChunks,
		int receiverTriangles,
		int totalCasters,
		int wallCasters,
		int gameObjectCasters,
		int wallObjectCasters,
		int outdoorOnlyCasters,
		int clippingCandidates,
		int roofedReceivers,
		int outdoorReceivers,
		int unknownReceivers,
		int roofedCasters,
		int outdoorCasters,
		int unknownCasters,
		int sunlightEligibleCasters,
		int sunlightSuppressedRoofedCasters,
		int sunlightSuppressedUnknownCasters) {
		this.receiverChunks = receiverChunks;
		this.receiverTriangles = receiverTriangles;
		this.totalCasters = totalCasters;
		this.wallCasters = wallCasters;
		this.gameObjectCasters = gameObjectCasters;
		this.wallObjectCasters = wallObjectCasters;
		this.outdoorOnlyCasters = outdoorOnlyCasters;
		this.clippingCandidates = clippingCandidates;
		this.roofedReceivers = roofedReceivers;
		this.outdoorReceivers = outdoorReceivers;
		this.unknownReceivers = unknownReceivers;
		this.roofedCasters = roofedCasters;
		this.outdoorCasters = outdoorCasters;
		this.unknownCasters = unknownCasters;
		this.sunlightEligibleCasters = sunlightEligibleCasters;
		this.sunlightSuppressedRoofedCasters = sunlightSuppressedRoofedCasters;
		this.sunlightSuppressedUnknownCasters = sunlightSuppressedUnknownCasters;
	}
}

final class RemasterShadowRoofCoverage {
	private static final RemasterShadowRoofCoverage EMPTY =
		new RemasterShadowRoofCoverage(
			Collections.<Renderer3DWorldChunkFrame.ChunkMesh>emptyList(),
			Collections.<Integer, RemasterShadowIndoorFlood>emptyMap());
	static final int TILE_SIZE = 128;
	private static final int BOUNDARY_SAMPLE_OFFSET = TILE_SIZE / 2;

	private final List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks;
	private final Map<Integer, RemasterShadowIndoorFlood> indoorFloodByPlane;

	private RemasterShadowRoofCoverage(
		List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks,
		Map<Integer, RemasterShadowIndoorFlood> indoorFloodByPlane) {
		this.roofChunks = roofChunks == null
			? Collections.<Renderer3DWorldChunkFrame.ChunkMesh>emptyList()
			: roofChunks;
		this.indoorFloodByPlane = indoorFloodByPlane == null
			? Collections.<Integer, RemasterShadowIndoorFlood>emptyMap()
			: indoorFloodByPlane;
	}

	static RemasterShadowRoofCoverage from(Renderer3DWorldChunkFrame chunkFrame) {
		if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
			return EMPTY;
		}
		List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks =
			new ArrayList<Renderer3DWorldChunkFrame.ChunkMesh>();
		Map<Integer, RemasterShadowIndoorFlood.Builder> floodBuilders =
			new HashMap<Integer, RemasterShadowIndoorFlood.Builder>();
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
			if (chunk.hasRoofCoverageData()) {
				roofChunks.add(chunk);
			}
			RemasterShadowIndoorFlood.Builder builder = floodBuilderForPlane(floodBuilders, chunk.getPlane());
			builder.addTerrain(chunk);
		}
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
			RemasterShadowIndoorFlood.Builder builder = floodBuilders.get(Integer.valueOf(chunk.getPlane()));
			if (builder == null) {
				continue;
			}
			builder.addWallBlockers(chunk);
		}
		Map<Integer, RemasterShadowIndoorFlood> floodByPlane =
			new HashMap<Integer, RemasterShadowIndoorFlood>();
		for (Map.Entry<Integer, RemasterShadowIndoorFlood.Builder> entry : floodBuilders.entrySet()) {
			RemasterShadowIndoorFlood flood = entry.getValue().build();
			if (flood != null) {
				floodByPlane.put(entry.getKey(), flood);
			}
		}
		return roofChunks.isEmpty() && floodByPlane.isEmpty()
			? EMPTY
			: new RemasterShadowRoofCoverage(roofChunks, floodByPlane);
	}

	int classify(int plane, int worldX, int worldZ) {
		int roofClassification = -1;
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : roofChunks) {
			if (chunk.getPlane() != plane) {
				continue;
			}
			int classification = chunk.roofClassificationForWorldPoint(worldX, worldZ);
			if (classification > 0) {
				return 1;
			}
			if (classification == 0) {
				roofClassification = 0;
			}
		}
		RemasterShadowIndoorFlood flood = indoorFloodByPlane.get(Integer.valueOf(plane));
		if (flood != null) {
			int classification = flood.classify(worldX, worldZ);
			if (classification >= 0) {
				return classification;
			}
		}
		return roofClassification;
	}

	int classifyBoundaryCaster(int plane, int x0, int z0, int x1, int z1) {
		int midX = (x0 + x1) / 2;
		int midZ = (z0 + z1) / 2;
		int dx = x1 - x0;
		int dz = z1 - z0;
		double length = Math.sqrt((double) dx * dx + (double) dz * dz);
		if (length < 1.0d) {
			return classify(plane, midX, midZ);
		}

		int offsetX = (int) Math.round((-dz / length) * BOUNDARY_SAMPLE_OFFSET);
		int offsetZ = (int) Math.round((dx / length) * BOUNDARY_SAMPLE_OFFSET);
		int sideA = classify(plane, midX + offsetX, midZ + offsetZ);
		int sideB = classify(plane, midX - offsetX, midZ - offsetZ);
		if (sideA == 0 || sideB == 0) {
			return 0;
		}
		if (sideA > 0 && sideB > 0) {
			return 1;
		}
		if (sideA > 0 || sideB > 0) {
			return 1;
		}
		return classify(plane, midX, midZ);
	}

	boolean crossesShadowBlocker(int plane, float startX, float startZ, float endX, float endZ) {
		RemasterShadowIndoorFlood flood = indoorFloodByPlane.get(Integer.valueOf(plane));
		return flood != null && flood.crossesBlocker(startX, startZ, endX, endZ);
	}

	private static RemasterShadowIndoorFlood.Builder floodBuilderForPlane(
		Map<Integer, RemasterShadowIndoorFlood.Builder> floodBuilders,
		int plane) {
		Integer key = Integer.valueOf(plane);
		RemasterShadowIndoorFlood.Builder builder = floodBuilders.get(key);
		if (builder == null) {
			builder = new RemasterShadowIndoorFlood.Builder(plane);
			floodBuilders.put(key, builder);
		}
		return builder;
	}

	static int tileForWorld(int world) {
		return Math.floorDiv(world, TILE_SIZE);
	}

	static int boundaryForWorld(int world) {
		return Math.round(world / (float) TILE_SIZE);
	}
}

final class RemasterShadowIndoorFlood {
	private static final int BOUNDS_PADDING_TILES = 2;

	private final int minTileX;
	private final int minTileZ;
	private final int width;
	private final int height;
	private final boolean[] outdoorTiles;
	private final boolean[] blockEast;
	private final boolean[] blockSouth;
	private final Map<Long, List<WallEdge>> wallEdgesByTile;

	private RemasterShadowIndoorFlood(
		int minTileX,
		int minTileZ,
		int width,
		int height,
		boolean[] outdoorTiles,
		boolean[] blockEast,
		boolean[] blockSouth,
		Map<Long, List<WallEdge>> wallEdgesByTile) {
		this.minTileX = minTileX;
		this.minTileZ = minTileZ;
		this.width = width;
		this.height = height;
		this.outdoorTiles = outdoorTiles;
		this.blockEast = blockEast == null ? new boolean[0] : blockEast;
		this.blockSouth = blockSouth == null ? new boolean[0] : blockSouth;
		this.wallEdgesByTile = wallEdgesByTile == null
			? Collections.<Long, List<WallEdge>>emptyMap()
			: wallEdgesByTile;
	}

	int classify(int worldX, int worldZ) {
		int tileX = RemasterShadowRoofCoverage.tileForWorld(worldX);
		int tileZ = RemasterShadowRoofCoverage.tileForWorld(worldZ);
		int localX = tileX - minTileX;
		int localZ = tileZ - minTileZ;
		if (localX < 0 || localZ < 0 || localX >= width || localZ >= height) {
			return -1;
		}
		return outdoorTiles[index(localX, localZ, width)] ? 0 : 1;
	}

	boolean crossesBlocker(float worldX0, float worldZ0, float worldX1, float worldZ1) {
		float dx = worldX1 - worldX0;
		float dz = worldZ1 - worldZ0;
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		if (distance < 1.0f) {
			return false;
		}
		int steps = Math.max(
			1,
			(int) Math.ceil(distance / Math.max(16.0f, RemasterShadowRoofCoverage.TILE_SIZE * 0.25f)));
		int lastX = localTileX(worldX0);
		int lastZ = localTileZ(worldZ0);
		float lastWorldX = worldX0;
		float lastWorldZ = worldZ0;
		for (int step = 1; step <= steps; step++) {
			float t = step / (float) steps;
			float nextWorldX = worldX0 + dx * t;
			float nextWorldZ = worldZ0 + dz * t;
			int nextX = localTileX(nextWorldX);
			int nextZ = localTileZ(nextWorldZ);
			if (RemasterShadowClassifier.DIRECT_WALL_SEGMENT_CLIP
				&& crossesDirectWallEdge(lastWorldX, lastWorldZ, nextWorldX, nextWorldZ, lastX, lastZ, nextX, nextZ)) {
				return true;
			}
			if (crossesBlockedTransition(lastX, lastZ, nextX, nextZ)) {
				return true;
			}
			lastX = nextX;
			lastZ = nextZ;
			lastWorldX = nextWorldX;
			lastWorldZ = nextWorldZ;
		}
		return false;
	}

	private int localTileX(float worldX) {
		return RemasterShadowRoofCoverage.tileForWorld((int) Math.floor(worldX)) - minTileX;
	}

	private int localTileZ(float worldZ) {
		return RemasterShadowRoofCoverage.tileForWorld((int) Math.floor(worldZ)) - minTileZ;
	}

	private boolean crossesBlockedTransition(int x, int z, int nextX, int nextZ) {
		if (x == nextX && z == nextZ) {
			return false;
		}
		if (nextX > x) {
			for (int boundaryX = x; boundaryX < nextX; boundaryX++) {
				if (isVerticalBlocked(boundaryX, z) || isVerticalBlocked(boundaryX, nextZ)) {
					return true;
				}
			}
		} else if (nextX < x) {
			for (int boundaryX = nextX; boundaryX < x; boundaryX++) {
				if (isVerticalBlocked(boundaryX, z) || isVerticalBlocked(boundaryX, nextZ)) {
					return true;
				}
			}
		}
		if (nextZ > z) {
			for (int boundaryZ = z; boundaryZ < nextZ; boundaryZ++) {
				if (isHorizontalBlocked(x, boundaryZ) || isHorizontalBlocked(nextX, boundaryZ)) {
					return true;
				}
			}
		} else if (nextZ < z) {
			for (int boundaryZ = nextZ; boundaryZ < z; boundaryZ++) {
				if (isHorizontalBlocked(x, boundaryZ) || isHorizontalBlocked(nextX, boundaryZ)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean crossesDirectWallEdge(
		float worldX0,
		float worldZ0,
		float worldX1,
		float worldZ1,
		int localX0,
		int localZ0,
		int localX1,
		int localZ1) {
		int minX = Math.min(localX0, localX1) - 1;
		int maxX = Math.max(localX0, localX1) + 1;
		int minZ = Math.min(localZ0, localZ1) - 1;
		int maxZ = Math.max(localZ0, localZ1) + 1;
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				List<WallEdge> edges = wallEdgesByTile.get(tileKey(x, z));
				if (edges == null) {
					continue;
				}
				for (WallEdge edge : edges) {
					if (edge.intersectsSegment(worldX0, worldZ0, worldX1, worldZ1)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isVerticalBlocked(int leftTileX, int tileZ) {
		if (leftTileX < 0 || leftTileX >= width - 1 || tileZ < 0 || tileZ >= height) {
			return false;
		}
		int index = tileZ * (width - 1) + leftTileX;
		return index >= 0 && index < blockEast.length && blockEast[index];
	}

	private boolean isHorizontalBlocked(int tileX, int topTileZ) {
		if (tileX < 0 || tileX >= width || topTileZ < 0 || topTileZ >= height - 1) {
			return false;
		}
		int index = topTileZ * width + tileX;
		return index >= 0 && index < blockSouth.length && blockSouth[index];
	}

	private static int index(int x, int z, int width) {
		return z * width + x;
	}

		static final class Builder {
		private final int plane;
		private final List<WallEdge> wallEdges = new ArrayList<WallEdge>();
		private int minTileX = Integer.MAX_VALUE;
		private int maxTileX = Integer.MIN_VALUE;
		private int minTileZ = Integer.MAX_VALUE;
		private int maxTileZ = Integer.MIN_VALUE;

			Builder(int plane) {
				this.plane = plane;
			}

			void addTerrain(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
			if (chunk == null || chunk.getPlane() != plane) {
				return;
			}
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				int sourceIndex = triangle * 3;
				for (int corner = 0; corner < 3; corner++) {
					int vertex = chunk.getIndex(sourceIndex + corner);
					int coord = vertex * 3;
					addTile(
						RemasterShadowRoofCoverage.tileForWorld(chunk.getVertexCoord(coord)),
						RemasterShadowRoofCoverage.tileForWorld(chunk.getVertexCoord(coord + 2)));
				}
			}
		}

			void addWallBlockers(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
			if (chunk == null || chunk.getPlane() != plane) {
				return;
			}
			for (int index = 0; index < chunk.getShadowCasterCount(); index++) {
				Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
				if (caster == null || caster.getModelKind() != Renderer3DModelKind.WALL) {
					continue;
				}
				wallEdges.add(new WallEdge(
					caster.getBaseX0(),
					caster.getBaseZ0(),
					caster.getBaseX1(),
					caster.getBaseZ1()));
			}
		}

			RemasterShadowIndoorFlood build() {
			if (minTileX == Integer.MAX_VALUE || minTileZ == Integer.MAX_VALUE) {
				return null;
			}
			int paddedMinTileX = minTileX - BOUNDS_PADDING_TILES;
			int paddedMinTileZ = minTileZ - BOUNDS_PADDING_TILES;
			int paddedMaxTileX = maxTileX + BOUNDS_PADDING_TILES;
			int paddedMaxTileZ = maxTileZ + BOUNDS_PADDING_TILES;
			int width = Math.max(1, paddedMaxTileX - paddedMinTileX + 1);
			int height = Math.max(1, paddedMaxTileZ - paddedMinTileZ + 1);
			boolean[] blockEast = new boolean[Math.max(0, width - 1) * height];
			boolean[] blockSouth = new boolean[width * Math.max(0, height - 1)];
			for (WallEdge edge : wallEdges) {
				edge.addBlockers(paddedMinTileX, paddedMinTileZ, width, height, blockEast, blockSouth);
			}
			closeSingleTileWallGaps(width, height, blockEast, blockSouth);
			boolean[] outdoor = floodOutdoor(width, height, blockEast, blockSouth);
			Map<Long, List<WallEdge>> wallEdgesByTile =
				RemasterShadowClassifier.DIRECT_WALL_SEGMENT_CLIP
				? buildWallEdgeGrid(paddedMinTileX, paddedMinTileZ, width, height, wallEdges)
				: Collections.<Long, List<WallEdge>>emptyMap();
			return new RemasterShadowIndoorFlood(
				paddedMinTileX,
				paddedMinTileZ,
				width,
				height,
				outdoor,
				blockEast,
				blockSouth,
				wallEdgesByTile);
		}

		private void addTile(int tileX, int tileZ) {
			minTileX = Math.min(minTileX, tileX);
			maxTileX = Math.max(maxTileX, tileX);
			minTileZ = Math.min(minTileZ, tileZ);
			maxTileZ = Math.max(maxTileZ, tileZ);
		}

		private static void closeSingleTileWallGaps(
			int width,
			int height,
			boolean[] blockEast,
			boolean[] blockSouth) {
			boolean[] originalEast = blockEast.clone();
			for (int x = 0; x < width - 1; x++) {
				for (int z = 1; z < height - 1; z++) {
					int index = z * (width - 1) + x;
					if (!originalEast[index]
						&& originalEast[(z - 1) * (width - 1) + x]
						&& originalEast[(z + 1) * (width - 1) + x]) {
						blockEast[index] = true;
					}
				}
			}
			boolean[] originalSouth = blockSouth.clone();
			for (int z = 0; z < height - 1; z++) {
				for (int x = 1; x < width - 1; x++) {
					int index = z * width + x;
					if (!originalSouth[index]
						&& originalSouth[z * width + x - 1]
						&& originalSouth[z * width + x + 1]) {
						blockSouth[index] = true;
					}
				}
			}
		}

		private static boolean[] floodOutdoor(
			int width,
			int height,
			boolean[] blockEast,
			boolean[] blockSouth) {
			boolean[] outdoor = new boolean[width * height];
			ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
			for (int x = 0; x < width; x++) {
				addFloodSeed(x, 0, width, height, outdoor, queue);
				addFloodSeed(x, height - 1, width, height, outdoor, queue);
			}
			for (int z = 1; z < height - 1; z++) {
				addFloodSeed(0, z, width, height, outdoor, queue);
				addFloodSeed(width - 1, z, width, height, outdoor, queue);
			}
			while (!queue.isEmpty()) {
				int packed = queue.removeFirst().intValue();
				int x = packed & 0xffff;
				int z = packed >>> 16;
				addFloodNeighbor(x, z, x + 1, z, width, height, blockEast, blockSouth, outdoor, queue);
				addFloodNeighbor(x, z, x - 1, z, width, height, blockEast, blockSouth, outdoor, queue);
				addFloodNeighbor(x, z, x, z + 1, width, height, blockEast, blockSouth, outdoor, queue);
				addFloodNeighbor(x, z, x, z - 1, width, height, blockEast, blockSouth, outdoor, queue);
			}
			return outdoor;
		}

		private static Map<Long, List<WallEdge>> buildWallEdgeGrid(
			int minTileX,
			int minTileZ,
			int width,
			int height,
			List<WallEdge> wallEdges) {
			if (wallEdges == null || wallEdges.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<Long, List<WallEdge>> grid = new HashMap<Long, List<WallEdge>>();
			for (WallEdge edge : wallEdges) {
				edge.addToGrid(minTileX, minTileZ, width, height, grid);
			}
			return grid;
		}

		private static void addFloodSeed(
			int x,
			int z,
			int width,
			int height,
			boolean[] outdoor,
			ArrayDeque<Integer> queue) {
			if (x < 0 || z < 0 || x >= width || z >= height) {
				return;
			}
			int index = index(x, z, width);
			if (outdoor[index]) {
				return;
			}
			outdoor[index] = true;
			queue.add(Integer.valueOf((z << 16) | x));
		}

		private static void addFloodNeighbor(
			int x,
			int z,
			int nextX,
			int nextZ,
			int width,
			int height,
			boolean[] blockEast,
			boolean[] blockSouth,
			boolean[] outdoor,
			ArrayDeque<Integer> queue) {
			if (nextX < 0 || nextZ < 0 || nextX >= width || nextZ >= height) {
				return;
			}
			if (isBlocked(x, z, nextX, nextZ, width, blockEast, blockSouth)) {
				return;
			}
			int index = index(nextX, nextZ, width);
			if (outdoor[index]) {
				return;
			}
			outdoor[index] = true;
			queue.add(Integer.valueOf((nextZ << 16) | nextX));
		}

		private static boolean isBlocked(
			int x,
			int z,
			int nextX,
			int nextZ,
			int width,
			boolean[] blockEast,
			boolean[] blockSouth) {
			if (nextX == x + 1) {
				return blockEast[z * (width - 1) + x];
			}
			if (nextX == x - 1) {
				return blockEast[z * (width - 1) + nextX];
			}
			if (nextZ == z + 1) {
				return blockSouth[z * width + x];
			}
			if (nextZ == z - 1) {
				return blockSouth[nextZ * width + x];
			}
			return false;
		}
	}

	private static final class WallEdge {
		private static final float INTERSECTION_EPSILON = 0.001f;

		private final int x0;
		private final int z0;
		private final int x1;
		private final int z1;

		private WallEdge(int x0, int z0, int x1, int z1) {
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
		}

		private void addBlockers(
			int minTileX,
			int minTileZ,
			int width,
			int height,
			boolean[] blockEast,
			boolean[] blockSouth) {
			int dx = Math.abs(x1 - x0);
			int dz = Math.abs(z1 - z0);
			if (dx <= 8 && dz > 0) {
				int boundaryX = RemasterShadowRoofCoverage.boundaryForWorld((x0 + x1) / 2);
				int startTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.min(z0, z1));
				int endTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.max(z0, z1) - 1);
				for (int tileZ = startTileZ; tileZ <= endTileZ; tileZ++) {
					addVerticalBlock(boundaryX, tileZ, minTileX, minTileZ, width, height, blockEast);
				}
			} else if (dz <= 8 && dx > 0) {
				int boundaryZ = RemasterShadowRoofCoverage.boundaryForWorld((z0 + z1) / 2);
				int startTileX = RemasterShadowRoofCoverage.tileForWorld(Math.min(x0, x1));
				int endTileX = RemasterShadowRoofCoverage.tileForWorld(Math.max(x0, x1) - 1);
				for (int tileX = startTileX; tileX <= endTileX; tileX++) {
					addHorizontalBlock(tileX, boundaryZ, minTileX, minTileZ, width, height, blockSouth);
				}
			}
		}

		private void addToGrid(
			int minTileX,
			int minTileZ,
			int width,
			int height,
			Map<Long, List<WallEdge>> grid) {
			int startTileX = RemasterShadowRoofCoverage.tileForWorld(Math.min(x0, x1)) - minTileX - 1;
			int endTileX = RemasterShadowRoofCoverage.tileForWorld(Math.max(x0, x1)) - minTileX + 1;
			int startTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.min(z0, z1)) - minTileZ - 1;
			int endTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.max(z0, z1)) - minTileZ + 1;
			for (int z = startTileZ; z <= endTileZ; z++) {
				if (z < 0 || z >= height) {
					continue;
				}
				for (int x = startTileX; x <= endTileX; x++) {
					if (x < 0 || x >= width) {
						continue;
					}
					long key = tileKey(x, z);
					List<WallEdge> edges = grid.get(Long.valueOf(key));
					if (edges == null) {
						edges = new ArrayList<WallEdge>();
						grid.put(Long.valueOf(key), edges);
					}
					edges.add(this);
				}
			}
		}

		private boolean intersectsSegment(float segmentX0, float segmentZ0, float segmentX1, float segmentZ1) {
			float dx = segmentX1 - segmentX0;
			float dz = segmentZ1 - segmentZ0;
			float edgeDx = x1 - x0;
			float edgeDz = z1 - z0;
			float denominator = cross(dx, dz, edgeDx, edgeDz);
			if (Math.abs(denominator) <= INTERSECTION_EPSILON) {
				return false;
			}
			float offsetX = x0 - segmentX0;
			float offsetZ = z0 - segmentZ0;
			float segmentT = cross(offsetX, offsetZ, edgeDx, edgeDz) / denominator;
			float edgeT = cross(offsetX, offsetZ, dx, dz) / denominator;
			return segmentT > INTERSECTION_EPSILON
				&& segmentT < 1.0f - INTERSECTION_EPSILON
				&& edgeT > INTERSECTION_EPSILON
				&& edgeT < 1.0f - INTERSECTION_EPSILON;
		}

		private static float cross(float ax, float az, float bx, float bz) {
			return ax * bz - az * bx;
		}

		private static void addVerticalBlock(
			int boundaryX,
			int tileZ,
			int minTileX,
			int minTileZ,
			int width,
			int height,
			boolean[] blockEast) {
			int localBoundaryX = boundaryX - minTileX;
			int localZ = tileZ - minTileZ;
			if (localBoundaryX <= 0 || localBoundaryX >= width || localZ < 0 || localZ >= height) {
				return;
			}
			blockEast[localZ * (width - 1) + localBoundaryX - 1] = true;
		}

		private static void addHorizontalBlock(
			int tileX,
			int boundaryZ,
			int minTileX,
			int minTileZ,
			int width,
			int height,
			boolean[] blockSouth) {
			int localX = tileX - minTileX;
			int localBoundaryZ = boundaryZ - minTileZ;
			if (localX < 0 || localX >= width || localBoundaryZ <= 0 || localBoundaryZ >= height) {
				return;
			}
			blockSouth[(localBoundaryZ - 1) * width + localX] = true;
		}
	}

	private static long tileKey(int localX, int localZ) {
		return ((long) localX << 32) ^ (localZ & 0xffffffffL);
	}
}
