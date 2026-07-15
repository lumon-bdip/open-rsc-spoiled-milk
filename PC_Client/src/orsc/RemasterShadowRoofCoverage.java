package orsc;

import orsc.graphics.three.Renderer3DWorldChunkFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
