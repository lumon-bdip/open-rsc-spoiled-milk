package orsc.graphics.three;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds and caches square tile boundaries for the active terrain plane.
 * Loaded height-grid data keeps void tiles visible; geometry extraction remains
 * as a compatibility fallback. Triangle diagonals and non-terrain geometry are
 * deliberately excluded from that fallback.
 */
public final class WorldEditorTerrainGrid {
	private static final int TILE_SIZE = 128;
	private static final int COORDS_PER_SEGMENT = 6;
	private static final long FNV_OFFSET = 0xcbf29ce484222325L;
	private static final long FNV_PRIME = 0x100000001b3L;

	private long cachedSignature = Long.MIN_VALUE;
	private int[] cachedSegments = new int[0];

	public int[] segments(Renderer3DWorldChunkFrame frame, int activePlane) {
		long signature = signature(frame, activePlane);
		if (signature == cachedSignature) {
			return cachedSegments;
		}
		cachedSegments = extract(frame, activePlane);
		cachedSignature = signature;
		return cachedSegments;
	}

	private static long signature(Renderer3DWorldChunkFrame frame, int activePlane) {
		long hash = mix(FNV_OFFSET, activePlane);
		if (frame == null) {
			return hash;
		}
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : frame.getChunks()) {
			if (!isActiveTerrainChunk(chunk, activePlane)) {
				continue;
			}
			hash = mix(hash, chunk.getPlane());
			hash = mix(hash, chunk.getTerrainTriangles());
			hash = mix(hash, chunk.getWorldEditorTerrainGridSignature());
			hash ^= chunk.getSignature();
			hash *= FNV_PRIME;
		}
		return hash;
	}

	private static int[] extract(Renderer3DWorldChunkFrame frame, int activePlane) {
		if (frame == null || frame.getChunkCount() == 0) {
			return new int[0];
		}
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : frame.getChunks()) {
			if (isActiveTerrainChunk(chunk, activePlane) && chunk.hasWorldEditorTerrainGrid()) {
				return completeGrid(chunk);
			}
		}
		Set<Edge> edges = new LinkedHashSet<Edge>();
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : frame.getChunks()) {
			if (!isActiveTerrainChunk(chunk, activePlane)) {
				continue;
			}
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				int index = triangle * 3;
				addEdge(edges, chunk, chunk.getIndex(index), chunk.getIndex(index + 1));
				addEdge(edges, chunk, chunk.getIndex(index + 1), chunk.getIndex(index + 2));
				addEdge(edges, chunk, chunk.getIndex(index + 2), chunk.getIndex(index));
			}
		}
		int[] result = new int[edges.size() * COORDS_PER_SEGMENT];
		int offset = 0;
		for (Edge edge : edges) {
			offset = edge.write(result, offset);
		}
		return result;
	}

	private static int[] completeGrid(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
		int axis = chunk.getWorldEditorTerrainGridAxis();
		int[] result = new int[2 * axis * (axis - 1) * COORDS_PER_SEGMENT];
		int offset = 0;
		for (int x = 0; x < axis; x++) {
			for (int z = 0; z < axis - 1; z++) {
				offset = writeGridSegment(result, offset, chunk, axis, x, z, x, z + 1);
			}
		}
		for (int x = 0; x < axis - 1; x++) {
			for (int z = 0; z < axis; z++) {
				offset = writeGridSegment(result, offset, chunk, axis, x, z, x + 1, z);
			}
		}
		return result;
	}

	private static int writeGridSegment(
		int[] destination,
		int offset,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		int axis,
		int x1,
		int z1,
		int x2,
		int z2) {
		destination[offset++] = x1 * TILE_SIZE;
		destination[offset++] = chunk.getWorldEditorTerrainGridHeight(x1 * axis + z1);
		destination[offset++] = z1 * TILE_SIZE;
		destination[offset++] = x2 * TILE_SIZE;
		destination[offset++] = chunk.getWorldEditorTerrainGridHeight(x2 * axis + z2);
		destination[offset++] = z2 * TILE_SIZE;
		return offset;
	}

	private static boolean isActiveTerrainChunk(Renderer3DWorldChunkFrame.ChunkMesh chunk, int activePlane) {
		return chunk != null
			&& chunk.getChunkRole() == Renderer3DWorldChunkFrame.CHUNK_ROLE_WORLD
			&& chunk.getPlane() == activePlane
			&& (chunk.getTerrainTriangles() > 0 || chunk.hasWorldEditorTerrainGrid());
	}

	private static void addEdge(
		Set<Edge> edges,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		int firstVertex,
		int secondVertex) {
		if (firstVertex < 0 || secondVertex < 0
			|| firstVertex >= chunk.getVertexCount() || secondVertex >= chunk.getVertexCount()) {
			return;
		}
		int first = firstVertex * 3;
		int second = secondVertex * 3;
		int x1 = chunk.getVertexCoord(first);
		int y1 = chunk.getVertexCoord(first + 1);
		int z1 = chunk.getVertexCoord(first + 2);
		int x2 = chunk.getVertexCoord(second);
		int y2 = chunk.getVertexCoord(second + 1);
		int z2 = chunk.getVertexCoord(second + 2);
		boolean tileBoundary = (x1 == x2 && Math.abs(z1 - z2) == TILE_SIZE)
			|| (z1 == z2 && Math.abs(x1 - x2) == TILE_SIZE);
		if (tileBoundary) {
			edges.add(new Edge(x1, y1, z1, x2, y2, z2));
		}
	}

	private static long mix(long hash, int value) {
		hash ^= value & 0xffffffffL;
		return hash * FNV_PRIME;
	}

	private static final class Edge {
		private final int x1;
		private final int y1;
		private final int z1;
		private final int x2;
		private final int y2;
		private final int z2;

		private Edge(int x1, int y1, int z1, int x2, int y2, int z2) {
			if (compare(x1, y1, z1, x2, y2, z2) <= 0) {
				this.x1 = x1;
				this.y1 = y1;
				this.z1 = z1;
				this.x2 = x2;
				this.y2 = y2;
				this.z2 = z2;
			} else {
				this.x1 = x2;
				this.y1 = y2;
				this.z1 = z2;
				this.x2 = x1;
				this.y2 = y1;
				this.z2 = z1;
			}
		}

		private int write(int[] destination, int offset) {
			destination[offset++] = x1;
			destination[offset++] = y1;
			destination[offset++] = z1;
			destination[offset++] = x2;
			destination[offset++] = y2;
			destination[offset++] = z2;
			return offset;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Edge)) {
				return false;
			}
			Edge edge = (Edge) other;
			return x1 == edge.x1 && y1 == edge.y1 && z1 == edge.z1
				&& x2 == edge.x2 && y2 == edge.y2 && z2 == edge.z2;
		}

		@Override
		public int hashCode() {
			int result = x1;
			result = 31 * result + y1;
			result = 31 * result + z1;
			result = 31 * result + x2;
			result = 31 * result + y2;
			return 31 * result + z2;
		}

		private static int compare(int x1, int y1, int z1, int x2, int y2, int z2) {
			if (x1 != x2) {
				return x1 < x2 ? -1 : 1;
			}
			if (z1 != z2) {
				return z1 < z2 ? -1 : 1;
			}
			return Integer.compare(y1, y2);
		}
	}
}
