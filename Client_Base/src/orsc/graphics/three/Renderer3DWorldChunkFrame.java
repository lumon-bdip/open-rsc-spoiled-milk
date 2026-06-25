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
		private final int[] vertexNormalX;
		private final int[] vertexNormalY;
		private final int[] vertexNormalZ;
		private final int[] indices;
		private final int[] triangleTextures;
		private final int[] triangleFallbackColors;
		private final Renderer3DModelKind[] triangleModelKinds;
		private final ShadowCaster[] shadowCasters;
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
				null,
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
				null,
				terrainTriangles,
				wallTriangles,
				roofTriangles,
				objectChunk,
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
			ShadowCaster[] shadowCasters,
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
			int[][] vertexNormals = buildVertexNormals(this.vertexCoords, this.indices, this.triangleTextures.length);
			this.vertexNormalX = vertexNormals[0];
			this.vertexNormalY = vertexNormals[1];
			this.vertexNormalZ = vertexNormals[2];
			this.shadowCasters = normalizeShadowCasters(shadowCasters);
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

		private static ShadowCaster[] normalizeShadowCasters(ShadowCaster[] shadowCasters) {
			return shadowCasters == null ? new ShadowCaster[0] : shadowCasters.clone();
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

		private static int[][] buildVertexNormals(
			int[] vertexCoords,
			int[] indices,
			int triangleCount) {
			int vertexCount = vertexCoords.length / 3;
			int[] normalX = new int[vertexCount];
			int[] normalY = new int[vertexCount];
			int[] normalZ = new int[vertexCount];
			for (int vertex = 0; vertex < vertexCount; vertex++) {
				normalY[vertex] = 256;
			}

			int limit = Math.min(triangleCount, indices.length / 3);
			for (int triangle = 0; triangle < limit; triangle++) {
				int sourceIndex = triangle * 3;
				int first = indices[sourceIndex];
				int second = indices[sourceIndex + 1];
				int third = indices[sourceIndex + 2];
				if (!isNormalVertexIndexValid(first, vertexCount)
					|| !isNormalVertexIndexValid(second, vertexCount)
					|| !isNormalVertexIndexValid(third, vertexCount)) {
					continue;
				}
				int firstCoord = first * 3;
				int secondCoord = second * 3;
				int thirdCoord = third * 3;
				double x21 = vertexCoords[secondCoord] - vertexCoords[firstCoord];
				double y21 = vertexCoords[secondCoord + 1] - vertexCoords[firstCoord + 1];
				double z21 = vertexCoords[secondCoord + 2] - vertexCoords[firstCoord + 2];
				double x31 = vertexCoords[thirdCoord] - vertexCoords[firstCoord];
				double y31 = vertexCoords[thirdCoord + 1] - vertexCoords[firstCoord + 1];
				double z31 = vertexCoords[thirdCoord + 2] - vertexCoords[firstCoord + 2];
				double faceNormalX = z31 * y21 - z21 * y31;
				double faceNormalY = z21 * x31 - x21 * z31;
				double faceNormalZ = x21 * y31 - x31 * y21;
				double magnitude = Math.sqrt(
					faceNormalX * faceNormalX
						+ faceNormalY * faceNormalY
						+ faceNormalZ * faceNormalZ);
				if (magnitude <= 0.000001d) {
					continue;
				}
				int scaledNormalX = (int) (faceNormalX * 256.0d / magnitude);
				int scaledNormalY = (int) (faceNormalY * 256.0d / magnitude);
				int scaledNormalZ = (int) (faceNormalZ * 256.0d / magnitude);
				normalX[first] = scaledNormalX;
				normalY[first] = scaledNormalY;
				normalZ[first] = scaledNormalZ;
				normalX[second] = scaledNormalX;
				normalY[second] = scaledNormalY;
				normalZ[second] = scaledNormalZ;
				normalX[third] = scaledNormalX;
				normalY[third] = scaledNormalY;
				normalZ[third] = scaledNormalZ;
			}
			return new int[][] {normalX, normalY, normalZ};
		}

		private static boolean isNormalVertexIndexValid(int vertex, int vertexCount) {
			return vertex >= 0 && vertex < vertexCount;
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

		public int getShadowCasterCount() {
			return shadowCasters.length;
		}

		public ShadowCaster getShadowCaster(int index) {
			return shadowCasters[index];
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

		public int getVertexNormalX(int vertexIndex) {
			return vertexNormalX[vertexIndex];
		}

		public int getVertexNormalY(int vertexIndex) {
			return vertexNormalY[vertexIndex];
		}

		public int getVertexNormalZ(int vertexIndex) {
			return vertexNormalZ[vertexIndex];
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

		public ShadowCaster[] copyShadowCasters() {
			return shadowCasters.clone();
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

		public int[] copyVertexNormalX() {
			return vertexNormalX.clone();
		}

		public int[] copyVertexNormalY() {
			return vertexNormalY.clone();
		}

		public int[] copyVertexNormalZ() {
			return vertexNormalZ.clone();
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

	public static final class ShadowCaster {
		private final Renderer3DModelKind modelKind;
		private final int baseX0;
		private final int baseY;
		private final int baseZ0;
		private final int baseX1;
		private final int baseZ1;
		private final int height;
		private final int width;
		private final int opacity;
		private final boolean outdoorOnly;

		public ShadowCaster(
			Renderer3DModelKind modelKind,
			int baseX0,
			int baseY,
			int baseZ0,
			int baseX1,
			int baseZ1,
			int height,
			int width,
			int opacity,
			boolean outdoorOnly) {
			this.modelKind = modelKind == null ? Renderer3DModelKind.UNCLASSIFIED : modelKind;
			this.baseX0 = baseX0;
			this.baseY = baseY;
			this.baseZ0 = baseZ0;
			this.baseX1 = baseX1;
			this.baseZ1 = baseZ1;
			this.height = Math.max(0, height);
			this.width = Math.max(0, width);
			this.opacity = Math.max(0, Math.min(255, opacity));
			this.outdoorOnly = outdoorOnly;
		}

		public Renderer3DModelKind getModelKind() {
			return modelKind;
		}

		public int getBaseX0() {
			return baseX0;
		}

		public int getBaseY() {
			return baseY;
		}

		public int getBaseZ0() {
			return baseZ0;
		}

		public int getBaseX1() {
			return baseX1;
		}

		public int getBaseZ1() {
			return baseZ1;
		}

		public int getHeight() {
			return height;
		}

		public int getWidth() {
			return width;
		}

		public int getOpacity() {
			return opacity;
		}

		public boolean isOutdoorOnly() {
			return outdoorOnly;
		}
	}
}
