package orsc.graphics.three;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Renderer3DWorldChunkFrame {
	public static final Renderer3DWorldChunkFrame EMPTY =
		new Renderer3DWorldChunkFrame(Collections.<ChunkMesh>emptyList(), 0, 0, 0);

	private final List<ChunkMesh> chunks;
	private final int totalVertexCount;
	private final int totalIndexCount;
	private final int totalTriangleCount;

	private Renderer3DWorldChunkFrame(
		List<ChunkMesh> chunks,
		int totalVertexCount,
		int totalIndexCount,
		int totalTriangleCount) {
		this.chunks = chunks;
		this.totalVertexCount = totalVertexCount;
		this.totalIndexCount = totalIndexCount;
		this.totalTriangleCount = totalTriangleCount;
	}

	public static Renderer3DWorldChunkFrame fromChunks(List<ChunkMesh> chunks) {
		if (chunks == null || chunks.isEmpty()) {
			return EMPTY;
		}

		List<ChunkMesh> chunkCopy = new ArrayList<ChunkMesh>(chunks);
		int totalVertexCount = 0;
		int totalIndexCount = 0;
		int totalTriangleCount = 0;
		for (ChunkMesh chunk : chunkCopy) {
			totalVertexCount += chunk.getVertexCount();
			totalIndexCount += chunk.getIndexCount();
			totalTriangleCount += chunk.getTriangleCount();
		}
		return new Renderer3DWorldChunkFrame(
			Collections.unmodifiableList(chunkCopy),
			totalVertexCount,
			totalIndexCount,
			totalTriangleCount);
	}

	public List<ChunkMesh> getChunks() {
		return chunks;
	}

	public int getChunkCount() {
		return chunks.size();
	}

	public int getTotalVertexCount() {
		return totalVertexCount;
	}

	public int getTotalIndexCount() {
		return totalIndexCount;
	}

	public int getTotalTriangleCount() {
		return totalTriangleCount;
	}

	public static final class ChunkMesh {
		private final int plane;
		private final int centerSectionX;
		private final int centerSectionY;
		private final int originWorldX;
		private final int originWorldZ;
		private final int[] vertexCoords;
		private final float[] vertexTextureU;
		private final float[] vertexTextureV;
		private final int[] vertexLights;
		private final int[] indices;
		private final int[] triangleTextures;
		private final int[] triangleFallbackColors;
		private final Renderer3DModelKind[] triangleModelKinds;
		private final int terrainTriangles;
		private final int wallTriangles;
		private final int roofTriangles;
		private final boolean objectChunk;
		private final long signature;

		public ChunkMesh(
			int plane,
			int centerSectionX,
			int centerSectionY,
			int originWorldX,
			int originWorldZ,
			int[] vertexCoords,
			float[] vertexTextureU,
			float[] vertexTextureV,
			int[] vertexLights,
			int[] indices,
			int[] triangleTextures,
			int[] triangleFallbackColors,
			Renderer3DModelKind[] triangleModelKinds,
			int terrainTriangles,
			int wallTriangles,
			int roofTriangles,
			long signature) {
			this(
				plane,
				centerSectionX,
				centerSectionY,
				originWorldX,
				originWorldZ,
				vertexCoords,
				vertexTextureU,
				vertexTextureV,
				vertexLights,
				indices,
				triangleTextures,
				triangleFallbackColors,
				triangleModelKinds,
				terrainTriangles,
				wallTriangles,
				roofTriangles,
				false,
				signature);
		}

		public ChunkMesh(
			int plane,
			int centerSectionX,
			int centerSectionY,
			int originWorldX,
			int originWorldZ,
			int[] vertexCoords,
			float[] vertexTextureU,
			float[] vertexTextureV,
			int[] vertexLights,
			int[] indices,
			int[] triangleTextures,
			int[] triangleFallbackColors,
			Renderer3DModelKind[] triangleModelKinds,
			int terrainTriangles,
			int wallTriangles,
			int roofTriangles,
			boolean objectChunk,
			long signature) {
			this.plane = plane;
			this.centerSectionX = centerSectionX;
			this.centerSectionY = centerSectionY;
			this.originWorldX = originWorldX;
			this.originWorldZ = originWorldZ;
			this.vertexCoords = vertexCoords == null ? new int[0] : vertexCoords.clone();
			int vertexCount = this.vertexCoords.length / 3;
			this.vertexTextureU = normalizeFloatArray(vertexTextureU, vertexCount, 0.0f);
			this.vertexTextureV = normalizeFloatArray(vertexTextureV, vertexCount, 0.0f);
			this.vertexLights = normalizeIntArray(vertexLights, vertexCount, 0);
			this.indices = indices == null ? new int[0] : indices.clone();
			this.triangleTextures = triangleTextures == null ? new int[0] : triangleTextures.clone();
			this.triangleFallbackColors =
				triangleFallbackColors == null ? new int[0] : triangleFallbackColors.clone();
			this.triangleModelKinds = normalizeKinds(triangleModelKinds, this.triangleTextures.length);
			this.terrainTriangles = terrainTriangles;
			this.wallTriangles = wallTriangles;
			this.roofTriangles = roofTriangles;
			this.objectChunk = objectChunk;
			this.signature = signature;
		}

		private static float[] normalizeFloatArray(float[] values, int count, float defaultValue) {
			float[] normalized = new float[count];
			for (int i = 0; i < normalized.length; i++) {
				normalized[i] = values == null || i >= values.length ? defaultValue : values[i];
			}
			return normalized;
		}

		private static int[] normalizeIntArray(int[] values, int count, int defaultValue) {
			int[] normalized = new int[count];
			for (int i = 0; i < normalized.length; i++) {
				normalized[i] = values == null || i >= values.length ? defaultValue : values[i];
			}
			return normalized;
		}

		private static Renderer3DModelKind[] normalizeKinds(Renderer3DModelKind[] kinds, int triangleCount) {
			Renderer3DModelKind[] normalized = new Renderer3DModelKind[triangleCount];
			for (int i = 0; i < normalized.length; i++) {
				Renderer3DModelKind kind = kinds == null || i >= kinds.length ? null : kinds[i];
				normalized[i] = kind == null ? Renderer3DModelKind.UNCLASSIFIED : kind;
			}
			return normalized;
		}

		public int getPlane() {
			return plane;
		}

		public int getCenterSectionX() {
			return centerSectionX;
		}

		public int getCenterSectionY() {
			return centerSectionY;
		}

		public int getOriginWorldX() {
			return originWorldX;
		}

		public int getOriginWorldZ() {
			return originWorldZ;
		}

		public int getVertexCount() {
			return vertexCoords.length / 3;
		}

		public int getIndexCount() {
			return indices.length;
		}

		public int getTriangleCount() {
			return triangleTextures.length;
		}

		public int getTerrainTriangles() {
			return terrainTriangles;
		}

		public int getWallTriangles() {
			return wallTriangles;
		}

		public int getRoofTriangles() {
			return roofTriangles;
		}

		public boolean isObjectChunk() {
			return objectChunk;
		}

		public long getSignature() {
			return signature;
		}

		public int getVertexCoord(int coordIndex) {
			return vertexCoords[coordIndex];
		}

		public float getVertexTextureU(int vertexIndex) {
			return vertexTextureU[vertexIndex];
		}

		public float getVertexTextureV(int vertexIndex) {
			return vertexTextureV[vertexIndex];
		}

		public int getVertexLight(int vertexIndex) {
			return vertexLights[vertexIndex];
		}

		public int getIndex(int indexOffset) {
			return indices[indexOffset];
		}

		public int getTriangleTexture(int triangleIndex) {
			return triangleTextures[triangleIndex];
		}

		public int getTriangleFallbackColor(int triangleIndex) {
			return triangleFallbackColors[triangleIndex];
		}

		public Renderer3DModelKind getTriangleModelKind(int triangleIndex) {
			return triangleModelKinds[triangleIndex];
		}

		public int[] copyVertexCoords() {
			return vertexCoords.clone();
		}

		public float[] copyVertexTextureU() {
			return vertexTextureU.clone();
		}

		public float[] copyVertexTextureV() {
			return vertexTextureV.clone();
		}

		public int[] copyVertexLights() {
			return vertexLights.clone();
		}

		public int[] copyIndices() {
			return indices.clone();
		}

		public int[] copyTriangleTextures() {
			return triangleTextures.clone();
		}

		public int[] copyTriangleFallbackColors() {
			return triangleFallbackColors.clone();
		}

		public Renderer3DModelKind[] copyTriangleModelKinds() {
			return triangleModelKinds.clone();
		}
	}
}
