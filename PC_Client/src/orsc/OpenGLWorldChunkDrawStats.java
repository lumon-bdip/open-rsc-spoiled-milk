package orsc;

final class OpenGLWorldChunkDrawStats {
	static final OpenGLWorldChunkDrawStats EMPTY =
		new OpenGLWorldChunkDrawStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	final int consideredChunks;
	final int drawnChunks;
	final int culledChunks;
	final int drawnTriangles;
	final int drawnTerrainTriangles;
	final int drawnWallTriangles;
	final int drawnRoofTriangles;
	final int drawnGameObjectTriangles;
	final int drawnWallObjectTriangles;
	final int drawnOtherTriangles;
	final int fallbackTriangles;
	final int skippedTriangles;
	final int consideredBatches;
	final int drawnBatches;
	final int culledBatches;
	final int drawCalls;
	final int textureBinds;
	final int shadowProofChunks;
	final int shadowProofIndices;

	OpenGLWorldChunkDrawStats(
		int consideredChunks,
		int drawnChunks,
		int culledChunks,
		int drawnTriangles,
		int drawnTerrainTriangles,
		int drawnWallTriangles,
		int drawnRoofTriangles,
		int drawnGameObjectTriangles,
		int drawnWallObjectTriangles,
		int drawnOtherTriangles,
		int fallbackTriangles,
		int skippedTriangles,
		int consideredBatches,
		int drawnBatches,
		int culledBatches,
		int drawCalls,
		int textureBinds,
		int shadowProofChunks,
		int shadowProofIndices) {
		this.consideredChunks = consideredChunks;
		this.drawnChunks = drawnChunks;
		this.culledChunks = culledChunks;
		this.drawnTriangles = drawnTriangles;
		this.drawnTerrainTriangles = drawnTerrainTriangles;
		this.drawnWallTriangles = drawnWallTriangles;
		this.drawnRoofTriangles = drawnRoofTriangles;
		this.drawnGameObjectTriangles = drawnGameObjectTriangles;
		this.drawnWallObjectTriangles = drawnWallObjectTriangles;
		this.drawnOtherTriangles = drawnOtherTriangles;
		this.fallbackTriangles = fallbackTriangles;
		this.skippedTriangles = skippedTriangles;
		this.consideredBatches = consideredBatches;
		this.drawnBatches = drawnBatches;
		this.culledBatches = culledBatches;
		this.drawCalls = drawCalls;
		this.textureBinds = textureBinds;
		this.shadowProofChunks = shadowProofChunks;
		this.shadowProofIndices = shadowProofIndices;
	}
}
