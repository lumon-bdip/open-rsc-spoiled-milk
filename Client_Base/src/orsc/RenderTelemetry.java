package orsc;

import orsc.graphics.Renderer2DFrame;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RenderTelemetry {
	private static final String ENABLED_PROPERTY = "spoiledmilk.rendererTelemetry";
	private static final String ENABLED_ENV = "SPOILED_MILK_RENDERER_TELEMETRY";
	private static final String REPORT_INTERVAL_PROPERTY = "spoiledmilk.rendererTelemetryInterval";
	private static final String SLOW_FRAME_MS_PROPERTY = "spoiledmilk.rendererSlowFrameMs";

	private static final int RECENT_SAMPLE_LIMIT = 120;
	private static final boolean RUNTIME_ENABLED = readBoolean(ENABLED_PROPERTY, ENABLED_ENV);
	private static final int REPORT_INTERVAL = Math.max(1, readInt(REPORT_INTERVAL_PROPERTY, 300));
	private static final long SLOW_FRAME_NANOS = Math.max(1L, readInt(SLOW_FRAME_MS_PROPERTY, 35)) * 1_000_000L;
	private static final long SLOW_REPORT_THROTTLE_NANOS = 1_000_000_000L;

	private static final StageStats frameStats = new StageStats();
	private static final StageStats clientLoopStats = new StageStats();
	private static final StageStats clientLoopSleepStats = new StageStats();
	private static final StageStats clientLoopUpdateStats = new StageStats();
	private static final StageStats clientLoopRepositionStats = new StageStats();
	private static final StageStats clientLoopDrawStats = new StageStats();
	private static final StageStats sceneRenderStats = new StageStats();
	private static final StageStats sceneModelRotateStats = new StageStats();
	private static final StageStats sceneWorldCullStats = new StageStats();
	private static final StageStats sceneDepthExportStats = new StageStats();
	private static final StageStats sceneLegacyDrawStats = new StageStats();
	private static final StageStats sceneMeshExportStats = new StageStats();
	private static final StageStats commitStats = new StageStats();
	private static final StageStats scalarResizeStats = new StageStats();
	private static final StageStats backingCopyStats = new StageStats();
	private static final StageStats presentStats = new StageStats();
	private static final StageStats setGameImageStats = new StageStats();
	private static final StageStats sourceCopyStats = new StageStats();
	private static final StageStats paintImmediateStats = new StageStats();
	private static final StageStats viewportPaintStats = new StageStats();
	private static final StageStats viewportScaleStats = new StageStats();
	private static final StageStats smoothScaleStats = new StageStats();
	private static final StageStats gpuPresenterStats = new StageStats();
	private static final StageStats openGLSnapshotStats = new StageStats();
	private static final StageStats openGLUploadStats = new StageStats();
	private static final StageStats openGLRenderStats = new StageStats();
	private static final StageStats openGLBaseStats = new StageStats();
	private static final StageStats openGLWorldStats = new StageStats();
	private static final StageStats openGLWorldSpriteStats = new StageStats();
	private static final StageStats openGLWorldChunkUploadPhaseStats = new StageStats();
	private static final StageStats openGLWorldProjectedMeshPhaseStats = new StageStats();
	private static final StageStats openGLWorldChunkDrawPhaseStats = new StageStats();
	private static final StageStats openGLRemasterShadowMaskBuildStats = new StageStats();
	private static final StageStats openGLRemasterShadowMaskUploadStats = new StageStats();
	private static final StageStats openGLSpriteOverlayStats = new StageStats();
	private static final StageStats openGLDebugOverlayStats = new StageStats();
	private static final StageStats openGLSwapStats = new StageStats();
	private static final CounterStats spriteOverlayCapturedStats = new CounterStats();
	private static final CounterStats spriteOverlayStaticReplayStats = new CounterStats();
	private static final CounterStats spriteOverlayVisibleReplayStats = new CounterStats();
	private static final CounterStats spriteOverlaySkippedOrderedStats = new CounterStats();
	private static final CounterStats spriteOverlaySkippedInvisibleStats = new CounterStats();
	private static final CounterStats spriteOverlaySkippedAtlasFullStats = new CounterStats();
	private static final CounterStats spriteOverlayVisiblePixelStats = new CounterStats();
	private static final CounterStats spriteOverlaySceneCommandStats = new CounterStats();
	private static final CounterStats spriteOverlayWorldCommandStats = new CounterStats();
	private static final CounterStats spriteOverlayUiCommandStats = new CounterStats();
	private static final CounterStats spriteOverlayUnknownCommandStats = new CounterStats();
	private static final CounterStats spriteOverlayDirectSceneStats = new CounterStats();
	private static final CounterStats spriteOverlayDirectWorldStats = new CounterStats();
	private static final CounterStats spriteOverlayDirectUiStats = new CounterStats();
	private static final CounterStats spriteOverlayDirectUnknownStats = new CounterStats();
	private static final CounterStats spriteOverlayVisibleSceneStats = new CounterStats();
	private static final CounterStats spriteOverlayVisibleWorldStats = new CounterStats();
	private static final CounterStats spriteOverlayVisibleUiStats = new CounterStats();
	private static final CounterStats spriteOverlayVisibleUnknownStats = new CounterStats();
	private static final CounterStats legacySceneSpriteRestoreCommandStats = new CounterStats();
	private static final CounterStats legacySceneSpriteRestoreFallbackStats = new CounterStats();
	private static final CounterStats legacySceneSpriteRestoreFallbackPixelStats = new CounterStats();
	private static final CounterStats spriteCaptureAttemptStats = new CounterStats();
	private static final CounterStats spriteCaptureAcceptedStats = new CounterStats();
	private static final CounterStats spriteCaptureReplacedUiStats = new CounterStats();
	private static final CounterStats spriteCaptureUiBaseStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedAlphaStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedBoundsStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedSourceStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedTransformStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedInterlaceStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedOverflowStats = new CounterStats();
	private static final CounterStats spriteCaptureSkippedInvalidStats = new CounterStats();
	private static final CounterStats textCaptureDrawStats = new CounterStats();
	private static final CounterStats textCaptureGlyphStats = new CounterStats();
	private static final CounterStats textCaptureReplacedUiStats = new CounterStats();
	private static final CounterStats textCaptureSceneStats = new CounterStats();
	private static final CounterStats textCaptureWorldStats = new CounterStats();
	private static final CounterStats textCaptureUiStats = new CounterStats();
	private static final CounterStats textCaptureUnknownStats = new CounterStats();
	private static final CounterStats primitiveCaptureDrawStats = new CounterStats();
	private static final CounterStats primitiveCaptureReplacedUiStats = new CounterStats();
	private static final CounterStats rotatedSpriteDrawStats = new CounterStats();
	private static final CounterStats rotatedSpriteCapturedUiStats = new CounterStats();
	private static final CounterStats circleDrawStats = new CounterStats();
	private static final CounterStats circleCapturedUiStats = new CounterStats();
	private static final CounterStats nativeUiBlockSpriteStats = new CounterStats();
	private static final CounterStats nativeUiBlockTextStats = new CounterStats();
	private static final CounterStats nativeUiBlockPrimitiveStats = new CounterStats();
	private static final CounterStats nativeUiBlockMinimapStats = new CounterStats();
	private static final CounterStats nativeUiBlockGradientStats = new CounterStats();
	private static final CounterStats nativeUiBlockClearStats = new CounterStats();
	private static final CounterStats nativeUiBlockCircleStats = new CounterStats();
	private static final CounterStats nativeUiBlockPixelStats = new CounterStats();
	private static final CounterStats nativeUiBaseEligibleStats = new CounterStats();
	private static final CounterStats clientLoopUpdateCountStats = new CounterStats();
	private static final CounterStats clientLoopSkippedDrawStats = new CounterStats();
	private static final CounterStats clientLoopSleepRequestStats = new CounterStats();
	private static final CounterStats clientLoopStepSizeStats = new CounterStats();
	private static final CounterStats worldGeometryModelStats = new CounterStats();
	private static final CounterStats worldGeometryFaceStats = new CounterStats();
	private static final CounterStats worldSpriteAnchorStats = new CounterStats();
	private static final CounterStats worldGeometryTerrainFaceStats = new CounterStats();
	private static final CounterStats worldGeometryWallFaceStats = new CounterStats();
	private static final CounterStats worldGeometryRoofFaceStats = new CounterStats();
	private static final CounterStats worldGeometryGameObjectFaceStats = new CounterStats();
	private static final CounterStats worldGeometryWallObjectFaceStats = new CounterStats();
	private static final CounterStats worldGeometryOtherFaceStats = new CounterStats();
	private static final CounterStats worldDepthConsideredFaceStats = new CounterStats();
	private static final CounterStats worldDepthFaceStats = new CounterStats();
	private static final CounterStats worldDepthRejectedFaceStats = new CounterStats();
	private static final CounterStats worldDepthTriangleStats = new CounterStats();
	private static final CounterStats worldDepthPixelWriteStats = new CounterStats();
	private static final CounterStats worldMeshVertexStats = new CounterStats();
	private static final CounterStats worldMeshIndexStats = new CounterStats();
	private static final CounterStats worldMeshTriangleStats = new CounterStats();
	private static final CounterStats worldMeshTexturedTriangleStats = new CounterStats();
	private static final CounterStats worldMeshFlatColorTriangleStats = new CounterStats();
	private static final CounterStats worldMeshTransparentTriangleStats = new CounterStats();
	private static final CounterStats worldMeshSkippedTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldMeshVertexStats = new CounterStats();
	private static final CounterStats openGLWorldMeshIndexStats = new CounterStats();
	private static final CounterStats openGLWorldMeshTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldMeshUploadStats = new CounterStats();
	private static final CounterStats openGLWorldMeshReuseStats = new CounterStats();
	private static final CounterStats openGLWorldMeshDrawTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldMeshDrawOccluderTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldMeshDrawBatchStats = new CounterStats();
	private static final CounterStats openGLWorldMeshDrawCallStats = new CounterStats();
	private static final CounterStats openGLWorldChunkStats = new CounterStats();
	private static final CounterStats openGLWorldChunkVertexStats = new CounterStats();
	private static final CounterStats openGLWorldChunkIndexStats = new CounterStats();
	private static final CounterStats openGLWorldChunkTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldChunkRequestedStats = new CounterStats();
	private static final CounterStats openGLWorldChunkUploadStats = new CounterStats();
	private static final CounterStats openGLWorldChunkReuseStats = new CounterStats();
	private static final CounterStats openGLWorldChunkEvictStats = new CounterStats();
	private static final CounterStats openGLWorldChunkConsideredStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawStats = new CounterStats();
	private static final CounterStats openGLWorldChunkCulledStats = new CounterStats();
	private static final CounterStats openGLWorldChunkBatchConsideredStats = new CounterStats();
	private static final CounterStats openGLWorldChunkBatchDrawStats = new CounterStats();
	private static final CounterStats openGLWorldChunkBatchCulledStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawTriangleStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawTerrainStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawWallStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawRoofStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawGameObjectStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawWallObjectStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawOtherStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawFallbackStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawSkippedStats = new CounterStats();
	private static final CounterStats openGLWorldChunkDrawCallStats = new CounterStats();
	private static final CounterStats openGLWorldChunkTextureBindStats = new CounterStats();
	private static final CounterStats openGLWorldChunkShadowChunkStats = new CounterStats();
	private static final CounterStats openGLWorldChunkShadowIndexStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowReceiverChunkStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowReceiverTriangleStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowWallCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowGameObjectCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowWallObjectCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowOutdoorOnlyCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowClippingCandidateStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowRoofedReceiverStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowOutdoorReceiverStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowUnknownReceiverStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowRoofedCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowOutdoorCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowUnknownCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowSunlightEligibleCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowSunlightSuppressedRoofedCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowSunlightSuppressedUnknownCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskWidthStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskHeightStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskVisiblePixelStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskCacheHitStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskRebuildStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskUploadCountStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskUploadSkipStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskStripCasterStats = new CounterStats();
	private static final CounterStats openGLRemasterShadowMaskSoftSceneryCasterStats = new CounterStats();
	private static final CounterStats openGLResidentChunkReplacementRequestedStats = new CounterStats();
	private static final CounterStats openGLResidentChunkReplacementActiveStats = new CounterStats();
	private static final CounterStats openGLResidentChunkReplacementFallbackStats = new CounterStats();
	private static final CounterStats openGLResidentChunkDrawableTerrainBatchStats = new CounterStats();
	private static final CounterStats openGLResidentChunkDrawableWallBatchStats = new CounterStats();
	private static final CounterStats openGLResidentChunkDrawableRoofBatchStats = new CounterStats();
	private static final CounterStats openGLWorldTextureReferencedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureCachedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureUploadedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureMissingStats = new CounterStats();
	private static final CounterStats openGLWorldTextureAtlasStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteAnchorStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteMatchedStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteDrawnStats = new CounterStats();
	private static final CounterStats openGLWorldEntityConsideredStats = new CounterStats();
	private static final CounterStats openGLWorldEntityDrawnStats = new CounterStats();
	private static final CounterStats openGLWorldEntityCulledStats = new CounterStats();

	private static final Map<String, AllocationStats> allocationStats = new LinkedHashMap<>();

	private static long repaintRequests;
	private static long paintImmediateRequests;
	private static long nearestScalePaints;
	private static long interpolationScalePaints;
	private static long gpuPresenterPaints;
	private static long openGLFrames;
	private static long openGLDroppedFrames;
	private static long openGLFramesWindow;
	private static long openGLDroppedFramesWindow;
	private static long lastReportNanos;
	private static String openGLResidentChunkReplacementReason = "not-requested";

	private RenderTelemetry() {
	}

	public static boolean isEnabled() {
		return isCollectionEnabled();
	}

	public static long now() {
		return isCollectionEnabled() ? System.nanoTime() : 0L;
	}

	public static long elapsedSince(long startNanos) {
		return isCollectionEnabled() && startNanos != 0L ? System.nanoTime() - startNanos : 0L;
	}

	private static boolean isCollectionEnabled() {
		return RUNTIME_ENABLED || RendererDebugSettings.isOverlayEnabled();
	}

	static void recordFrame(
		long totalNanos,
		long commitNanos,
		long scalarResizeNanos,
		long backingCopyNanos,
		long presentNanos,
		int sourceWidth,
		int sourceHeight,
		float scalar,
		Object scalingType,
		String framePath) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			frameStats.record(totalNanos);
			commitStats.record(commitNanos);
			scalarResizeStats.record(scalarResizeNanos);
			backingCopyStats.record(backingCopyNanos);
			presentStats.record(presentNanos);

			boolean slowFrame = totalNanos >= SLOW_FRAME_NANOS;
			long now = System.nanoTime();
			if (RUNTIME_ENABLED
				&& (frameStats.count % REPORT_INTERVAL == 0
					|| (slowFrame && now - lastReportNanos >= SLOW_REPORT_THROTTLE_NANOS))) {
				lastReportNanos = now;
				printReport(slowFrame, totalNanos, sourceWidth, sourceHeight, scalar, scalingType, framePath);
			}
		}
	}

	static void recordSceneRender(long nanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			sceneRenderStats.record(nanos);
		}
	}

	static void recordClientLoop(
		long loopNanos,
		long sleepNanos,
		long updateNanos,
		long repositionNanos,
		long drawNanos,
		int updateCount,
		int sleepRequestMillis,
		int stepSize,
		boolean skippedDraw) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			clientLoopStats.record(loopNanos);
			clientLoopSleepStats.record(sleepNanos);
			clientLoopUpdateStats.record(updateNanos);
			clientLoopRepositionStats.record(repositionNanos);
			clientLoopDrawStats.record(drawNanos);
			clientLoopUpdateCountStats.record(updateCount);
			clientLoopSkippedDrawStats.record(skippedDraw ? 1 : 0);
			clientLoopSleepRequestStats.record(sleepRequestMillis);
			clientLoopStepSizeStats.record(stepSize);
		}
	}

	public static void recordScene3DPhases(
		long modelRotateNanos,
		long worldCullNanos,
		long depthExportNanos,
		long legacyDrawNanos,
		long meshExportNanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			sceneModelRotateStats.record(modelRotateNanos);
			sceneWorldCullStats.record(worldCullNanos);
			sceneDepthExportStats.record(depthExportNanos);
			sceneLegacyDrawStats.record(legacyDrawNanos);
			sceneMeshExportStats.record(meshExportNanos);
		}
	}

	static void recordWorldGeometryFrame(
		int sourceModels,
		int worldFaces,
		int spriteAnchors,
		int terrainFaces,
		int wallFaces,
		int roofFaces,
		int gameObjectFaces,
		int wallObjectFaces,
		int otherFaces,
		int depthConsideredFaces,
		int depthFaces,
		int depthRejectedFaces,
		int depthTriangles,
		int depthPixelWrites,
		int meshVertices,
		int meshIndices,
		int meshTriangles,
		int meshTexturedTriangles,
		int meshFlatColorTriangles,
		int meshTransparentTriangles,
		int meshSkippedTriangles) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			worldGeometryModelStats.record(sourceModels);
			worldGeometryFaceStats.record(worldFaces);
			worldSpriteAnchorStats.record(spriteAnchors);
			worldGeometryTerrainFaceStats.record(terrainFaces);
			worldGeometryWallFaceStats.record(wallFaces);
			worldGeometryRoofFaceStats.record(roofFaces);
			worldGeometryGameObjectFaceStats.record(gameObjectFaces);
			worldGeometryWallObjectFaceStats.record(wallObjectFaces);
			worldGeometryOtherFaceStats.record(otherFaces);
			worldDepthConsideredFaceStats.record(depthConsideredFaces);
			worldDepthFaceStats.record(depthFaces);
			worldDepthRejectedFaceStats.record(depthRejectedFaces);
			worldDepthTriangleStats.record(depthTriangles);
			worldDepthPixelWriteStats.record(depthPixelWrites);
			worldMeshVertexStats.record(meshVertices);
			worldMeshIndexStats.record(meshIndices);
			worldMeshTriangleStats.record(meshTriangles);
			worldMeshTexturedTriangleStats.record(meshTexturedTriangles);
			worldMeshFlatColorTriangleStats.record(meshFlatColorTriangles);
			worldMeshTransparentTriangleStats.record(meshTransparentTriangles);
			worldMeshSkippedTriangleStats.record(meshSkippedTriangles);
		}
	}

	static void recordOpenGLWorldMeshFrame(int vertices, int indices, int triangles) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldMeshVertexStats.record(vertices);
			openGLWorldMeshIndexStats.record(indices);
			openGLWorldMeshTriangleStats.record(triangles);
		}
	}

	static void recordOpenGLWorldMeshUpload(boolean reused) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldMeshUploadStats.record(reused ? 0 : 1);
			openGLWorldMeshReuseStats.record(reused ? 1 : 0);
		}
	}

	static void recordOpenGLWorldMeshDraw(int triangles, int occluderTriangles, int batches, int drawCalls) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldMeshDrawTriangleStats.record(triangles);
			openGLWorldMeshDrawOccluderTriangleStats.record(occluderTriangles);
			openGLWorldMeshDrawBatchStats.record(batches);
			openGLWorldMeshDrawCallStats.record(drawCalls);
		}
	}

	static void recordOpenGLWorldChunkFrame(int chunks, int vertices, int indices, int triangles) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldChunkStats.record(chunks);
			openGLWorldChunkVertexStats.record(vertices);
			openGLWorldChunkIndexStats.record(indices);
			openGLWorldChunkTriangleStats.record(triangles);
		}
	}

	static void recordOpenGLWorldChunkUpload(int requested, int uploaded, int reused, int evicted) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldChunkRequestedStats.record(requested);
			openGLWorldChunkUploadStats.record(uploaded);
			openGLWorldChunkReuseStats.record(reused);
			openGLWorldChunkEvictStats.record(evicted);
		}
	}

	static void recordOpenGLWorldChunkDraw(
		int drawnChunks,
		int drawnTriangles,
		int drawnTerrainTriangles,
		int drawnWallTriangles,
		int drawnRoofTriangles,
		int drawnGameObjectTriangles,
		int drawnWallObjectTriangles,
		int drawnOtherTriangles,
		int fallbackTriangles,
		int skippedTriangles,
		int shadowChunks,
		int shadowIndices,
		int consideredChunks,
		int culledChunks,
		int consideredBatches,
		int drawnBatches,
		int culledBatches,
		int drawCalls,
		int textureBinds) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldChunkConsideredStats.record(consideredChunks);
			openGLWorldChunkDrawStats.record(drawnChunks);
			openGLWorldChunkCulledStats.record(culledChunks);
			openGLWorldChunkBatchConsideredStats.record(consideredBatches);
			openGLWorldChunkBatchDrawStats.record(drawnBatches);
			openGLWorldChunkBatchCulledStats.record(culledBatches);
			openGLWorldChunkDrawTriangleStats.record(drawnTriangles);
			openGLWorldChunkDrawTerrainStats.record(drawnTerrainTriangles);
			openGLWorldChunkDrawWallStats.record(drawnWallTriangles);
			openGLWorldChunkDrawRoofStats.record(drawnRoofTriangles);
			openGLWorldChunkDrawGameObjectStats.record(drawnGameObjectTriangles);
			openGLWorldChunkDrawWallObjectStats.record(drawnWallObjectTriangles);
			openGLWorldChunkDrawOtherStats.record(drawnOtherTriangles);
			openGLWorldChunkDrawFallbackStats.record(fallbackTriangles);
			openGLWorldChunkDrawSkippedStats.record(skippedTriangles);
			openGLWorldChunkDrawCallStats.record(drawCalls);
			openGLWorldChunkTextureBindStats.record(textureBinds);
			openGLWorldChunkShadowChunkStats.record(shadowChunks);
			openGLWorldChunkShadowIndexStats.record(shadowIndices);
		}
	}

	static void recordOpenGLRemasterShadowInventory(
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
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLRemasterShadowReceiverChunkStats.record(receiverChunks);
			openGLRemasterShadowReceiverTriangleStats.record(receiverTriangles);
			openGLRemasterShadowCasterStats.record(totalCasters);
			openGLRemasterShadowWallCasterStats.record(wallCasters);
			openGLRemasterShadowGameObjectCasterStats.record(gameObjectCasters);
			openGLRemasterShadowWallObjectCasterStats.record(wallObjectCasters);
			openGLRemasterShadowOutdoorOnlyCasterStats.record(outdoorOnlyCasters);
			openGLRemasterShadowClippingCandidateStats.record(clippingCandidates);
			openGLRemasterShadowRoofedReceiverStats.record(roofedReceivers);
			openGLRemasterShadowOutdoorReceiverStats.record(outdoorReceivers);
			openGLRemasterShadowUnknownReceiverStats.record(unknownReceivers);
			openGLRemasterShadowRoofedCasterStats.record(roofedCasters);
			openGLRemasterShadowOutdoorCasterStats.record(outdoorCasters);
			openGLRemasterShadowUnknownCasterStats.record(unknownCasters);
			openGLRemasterShadowSunlightEligibleCasterStats.record(sunlightEligibleCasters);
			openGLRemasterShadowSunlightSuppressedRoofedCasterStats.record(sunlightSuppressedRoofedCasters);
			openGLRemasterShadowSunlightSuppressedUnknownCasterStats.record(sunlightSuppressedUnknownCasters);
		}
	}

	static void recordOpenGLRemasterShadowMask(
		int width,
		int height,
		int visiblePixels,
		boolean cacheHit,
		boolean rebuilt,
		boolean uploaded,
		boolean uploadSkipped,
		int stripCasters,
		int softSceneryCasters,
		long buildNanos,
		long uploadNanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLRemasterShadowMaskWidthStats.record(width);
			openGLRemasterShadowMaskHeightStats.record(height);
			openGLRemasterShadowMaskVisiblePixelStats.record(visiblePixels);
			openGLRemasterShadowMaskCacheHitStats.record(cacheHit ? 1 : 0);
			openGLRemasterShadowMaskRebuildStats.record(rebuilt ? 1 : 0);
			openGLRemasterShadowMaskUploadCountStats.record(uploaded ? 1 : 0);
			openGLRemasterShadowMaskUploadSkipStats.record(uploadSkipped ? 1 : 0);
			openGLRemasterShadowMaskStripCasterStats.record(stripCasters);
			openGLRemasterShadowMaskSoftSceneryCasterStats.record(softSceneryCasters);
			openGLRemasterShadowMaskBuildStats.record(buildNanos);
			openGLRemasterShadowMaskUploadStats.record(uploadNanos);
		}
	}

	static void recordOpenGLResidentChunkReplacement(
		boolean requested,
		boolean active,
		String reason,
		int drawableTerrainBatches,
		int drawableWallBatches,
		int drawableRoofBatches) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLResidentChunkReplacementRequestedStats.record(requested ? 1 : 0);
			openGLResidentChunkReplacementActiveStats.record(active ? 1 : 0);
			openGLResidentChunkReplacementFallbackStats.record(requested && !active ? 1 : 0);
			openGLResidentChunkDrawableTerrainBatchStats.record(drawableTerrainBatches);
			openGLResidentChunkDrawableWallBatchStats.record(drawableWallBatches);
			openGLResidentChunkDrawableRoofBatchStats.record(drawableRoofBatches);
			openGLResidentChunkReplacementReason =
				reason == null || reason.trim().isEmpty() ? "unknown" : reason;
		}
	}

	static void recordOpenGLWorldTextureFrame(
		int referencedTextures,
		int cachedTextures,
		int uploadedTextures,
		int missingTextures,
		int atlases) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldTextureReferencedStats.record(referencedTextures);
			openGLWorldTextureCachedStats.record(cachedTextures);
			openGLWorldTextureUploadedStats.record(uploadedTextures);
			openGLWorldTextureMissingStats.record(missingTextures);
			openGLWorldTextureAtlasStats.record(atlases);
		}
	}

	static void recordOpenGLWorldSpriteFrame(int anchors, int matched, int drawn) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldSpriteAnchorStats.record(anchors);
			openGLWorldSpriteMatchedStats.record(matched);
			openGLWorldSpriteDrawnStats.record(drawn);
			openGLWorldEntityConsideredStats.record(anchors);
			openGLWorldEntityDrawnStats.record(drawn);
			openGLWorldEntityCulledStats.record(Math.max(0, anchors - drawn));
		}
	}

	static void recordSetGameImage(
		long totalNanos,
		long sourceCopyNanos,
		long paintImmediateNanos,
		boolean repaintRequested,
		boolean paintImmediateRequested) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			setGameImageStats.record(totalNanos);
			sourceCopyStats.record(sourceCopyNanos);
			paintImmediateStats.record(paintImmediateNanos);
			if (repaintRequested) {
				repaintRequests++;
			}
			if (paintImmediateRequested) {
				paintImmediateRequests++;
			}
		}
	}

	static void recordViewportPaint(
		long totalNanos,
		long scaleNanos,
		boolean nearestScale,
		boolean interpolationScale) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			viewportPaintStats.record(totalNanos);
			viewportScaleStats.record(scaleNanos);
			if (nearestScale) {
				nearestScalePaints++;
			}
			if (interpolationScale) {
				interpolationScalePaints++;
			}
		}
	}

	static void recordSmoothScale(long nanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			smoothScaleStats.record(nanos);
		}
	}

	static void recordGpuPresenter(long nanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			gpuPresenterStats.record(nanos);
			gpuPresenterPaints++;
		}
	}

	static void recordOpenGLSnapshot(long nanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLSnapshotStats.record(nanos);
		}
	}

	static void recordOpenGLFrame(long uploadNanos, long renderNanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLUploadStats.record(uploadNanos);
			openGLRenderStats.record(renderNanos);
			openGLFrames++;
			openGLFramesWindow++;
		}
	}

	static void recordOpenGLFramePhases(
		long baseNanos,
		long worldNanos,
		long worldSpriteNanos,
		long spriteOverlayNanos,
		long debugOverlayNanos,
		long swapNanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLBaseStats.record(baseNanos);
			openGLWorldStats.record(worldNanos);
			openGLWorldSpriteStats.record(worldSpriteNanos);
			openGLSpriteOverlayStats.record(spriteOverlayNanos);
			openGLDebugOverlayStats.record(debugOverlayNanos);
			openGLSwapStats.record(swapNanos);
		}
	}

	static void recordOpenGLWorldPhaseBreakdown(
		long chunkUploadNanos,
		long projectedMeshNanos,
		long chunkDrawNanos) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldChunkUploadPhaseStats.record(chunkUploadNanos);
			openGLWorldProjectedMeshPhaseStats.record(projectedMeshNanos);
			openGLWorldChunkDrawPhaseStats.record(chunkDrawNanos);
		}
	}

	static void recordOpenGLDroppedFrame() {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLDroppedFrames++;
			openGLDroppedFramesWindow++;
		}
	}

	static void recordSpriteCaptureStats(Renderer2DFrame.CaptureStats captureStats) {
		if (!isCollectionEnabled() || captureStats == null) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			spriteCaptureAttemptStats.record(captureStats.getAttempts());
			spriteCaptureAcceptedStats.record(captureStats.getCaptured());
			spriteCaptureReplacedUiStats.record(captureStats.getReplacedUi());
			spriteCaptureUiBaseStats.record(captureStats.getUiBaseCaptured());
			spriteCaptureSkippedAlphaStats.record(captureStats.getSkippedAlpha());
			spriteCaptureSkippedBoundsStats.record(captureStats.getSkippedBounds());
			spriteCaptureSkippedSourceStats.record(captureStats.getSkippedSource());
			spriteCaptureSkippedTransformStats.record(captureStats.getSkippedTransform());
			spriteCaptureSkippedInterlaceStats.record(captureStats.getSkippedInterlace());
			spriteCaptureSkippedOverflowStats.record(captureStats.getSkippedOverflow());
			spriteCaptureSkippedInvalidStats.record(captureStats.getSkippedInvalid());
			textCaptureDrawStats.record(captureStats.getTextDraws());
			textCaptureGlyphStats.record(captureStats.getTextGlyphs());
			textCaptureReplacedUiStats.record(captureStats.getTextReplacedUi());
			textCaptureSceneStats.record(captureStats.getTextScene());
			textCaptureWorldStats.record(captureStats.getTextWorld());
			textCaptureUiStats.record(captureStats.getTextUi());
			textCaptureUnknownStats.record(captureStats.getTextUnknown());
			primitiveCaptureDrawStats.record(captureStats.getPrimitiveDraws());
			primitiveCaptureReplacedUiStats.record(captureStats.getPrimitiveReplacedUi());
			rotatedSpriteDrawStats.record(captureStats.getRotatedSpriteDraws());
			rotatedSpriteCapturedUiStats.record(captureStats.getRotatedSpriteCapturedUi());
			circleDrawStats.record(captureStats.getCircleDraws());
			circleCapturedUiStats.record(captureStats.getCircleCapturedUi());
			nativeUiBlockSpriteStats.record(captureStats.getNativeUiBlockSprite());
			nativeUiBlockTextStats.record(captureStats.getNativeUiBlockText());
			nativeUiBlockPrimitiveStats.record(captureStats.getNativeUiBlockPrimitive());
			nativeUiBlockMinimapStats.record(captureStats.getNativeUiBlockMinimap());
			nativeUiBlockGradientStats.record(captureStats.getNativeUiBlockGradient());
			nativeUiBlockClearStats.record(captureStats.getNativeUiBlockClear());
			nativeUiBlockCircleStats.record(captureStats.getNativeUiBlockCircle());
			nativeUiBlockPixelStats.record(captureStats.getNativeUiBlockPixel());
			nativeUiBaseEligibleStats.record(captureStats.isNativeUiBaseEligible() ? 1 : 0);
		}
	}

	static void recordSpriteOverlayFrame(
		int captured,
		int staticReplayed,
		int visibleReplayed,
		int skippedOrdered,
		int skippedInvisible,
		int skippedAtlasFull,
		int visiblePixels,
		int sceneCommands,
		int worldCommands,
		int uiCommands,
		int unknownCommands,
		int directSceneCommands,
		int directWorldCommands,
		int directUiCommands,
		int directUnknownCommands,
		int visibleSceneCommands,
		int visibleWorldCommands,
		int visibleUiCommands,
		int visibleUnknownCommands) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			spriteOverlayCapturedStats.record(captured);
			spriteOverlayStaticReplayStats.record(staticReplayed);
			spriteOverlayVisibleReplayStats.record(visibleReplayed);
			spriteOverlaySkippedOrderedStats.record(skippedOrdered);
			spriteOverlaySkippedInvisibleStats.record(skippedInvisible);
			spriteOverlaySkippedAtlasFullStats.record(skippedAtlasFull);
			spriteOverlayVisiblePixelStats.record(visiblePixels);
			spriteOverlaySceneCommandStats.record(sceneCommands);
			spriteOverlayWorldCommandStats.record(worldCommands);
			spriteOverlayUiCommandStats.record(uiCommands);
			spriteOverlayUnknownCommandStats.record(unknownCommands);
			spriteOverlayDirectSceneStats.record(directSceneCommands);
			spriteOverlayDirectWorldStats.record(directWorldCommands);
			spriteOverlayDirectUiStats.record(directUiCommands);
			spriteOverlayDirectUnknownStats.record(directUnknownCommands);
			spriteOverlayVisibleSceneStats.record(visibleSceneCommands);
			spriteOverlayVisibleWorldStats.record(visibleWorldCommands);
			spriteOverlayVisibleUiStats.record(visibleUiCommands);
			spriteOverlayVisibleUnknownStats.record(visibleUnknownCommands);
		}
	}

	static void recordLegacySceneSpriteRestore(int commands, int fallbacks, int fallbackPixels) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			legacySceneSpriteRestoreCommandStats.record(commands);
			legacySceneSpriteRestoreFallbackStats.record(fallbacks);
			legacySceneSpriteRestoreFallbackPixelStats.record(fallbackPixels);
		}
	}

	static void recordImageAllocation(String label, int width, int height, int imageType) {
		if (!isCollectionEnabled()) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			AllocationStats stats = allocationStats.get(label);
			if (stats == null) {
				stats = new AllocationStats();
				allocationStats.put(label, stats);
			}
			stats.record(width, height, bytesPerPixel(imageType));
		}
	}

	static Snapshot snapshot() {
		synchronized (RenderTelemetry.class) {
			return new Snapshot(
				isCollectionEnabled(),
				frameStats.count,
				formatMillis(frameStats.average()),
				formatMillis(frameStats.max),
				formatMillis(sceneRenderStats.average()),
				formatMillis(sceneModelRotateStats.average()),
				formatMillis(sceneWorldCullStats.average()),
				formatMillis(sceneDepthExportStats.average()),
				formatMillis(sceneLegacyDrawStats.average()),
				formatMillis(sceneMeshExportStats.average()),
				formatMillis(setGameImageStats.average()),
				formatMillis(sourceCopyStats.average()),
				formatMillis(openGLSnapshotStats.average()),
				formatMillis(openGLUploadStats.average()),
				formatMillis(openGLRenderStats.average()),
				formatMillis(openGLBaseStats.average()),
				formatMillis(openGLWorldStats.average()),
				formatMillis(openGLWorldSpriteStats.average()),
				formatMillis(openGLSpriteOverlayStats.average()),
				formatMillis(openGLDebugOverlayStats.average()),
				formatMillis(openGLSwapStats.average()),
				formatMillis(openGLWorldChunkUploadPhaseStats.average()),
				formatMillis(openGLWorldProjectedMeshPhaseStats.average()),
				formatMillis(openGLWorldChunkDrawPhaseStats.average()),
				openGLFrames,
				openGLDroppedFrames,
				repaintRequests,
				recentFrameSummary(),
				recentOpenGLTimingSummary(),
				recentOpenGLPhaseSummary(),
				recentScenePhaseSummary(),
				recentClientLoopSummary(),
				formatCount(spriteOverlayCapturedStats.average()),
				formatCount(spriteOverlayStaticReplayStats.average()),
				formatCount(spriteOverlayVisibleReplayStats.average()),
				formatCount(spriteOverlaySkippedOrderedStats.average()),
				formatCount(spriteOverlaySkippedInvisibleStats.average()),
				formatCount(spriteOverlaySkippedAtlasFullStats.average()),
				formatCount(spriteOverlayVisiblePixelStats.average()),
				formatCount(spriteOverlaySceneCommandStats.average()),
				formatCount(spriteOverlayWorldCommandStats.average()),
				formatCount(spriteOverlayUiCommandStats.average()),
				formatCount(spriteOverlayUnknownCommandStats.average()),
				formatCount(spriteOverlayDirectSceneStats.average()),
				formatCount(spriteOverlayDirectWorldStats.average()),
				formatCount(spriteOverlayDirectUiStats.average()),
				formatCount(spriteOverlayDirectUnknownStats.average()),
				formatCount(spriteOverlayVisibleSceneStats.average()),
				formatCount(spriteOverlayVisibleWorldStats.average()),
				formatCount(spriteOverlayVisibleUiStats.average()),
				formatCount(spriteOverlayVisibleUnknownStats.average()),
				formatCount(legacySceneSpriteRestoreCommandStats.average()),
				formatCount(legacySceneSpriteRestoreFallbackStats.average()),
				formatCount(legacySceneSpriteRestoreFallbackPixelStats.average()),
				formatCount(spriteCaptureAttemptStats.average()),
				formatCount(spriteCaptureAcceptedStats.average()),
				formatCount(spriteCaptureReplacedUiStats.average()),
				formatCount(spriteCaptureUiBaseStats.average()),
				formatCount(spriteCaptureSkippedAlphaStats.average()),
				formatCount(spriteCaptureSkippedBoundsStats.average()),
				formatCount(spriteCaptureSkippedSourceStats.average()),
				formatCount(spriteCaptureSkippedTransformStats.average()),
				formatCount(spriteCaptureSkippedInterlaceStats.average()),
				formatCount(spriteCaptureSkippedOverflowStats.average()),
				formatCount(spriteCaptureSkippedInvalidStats.average()),
				formatCount(textCaptureDrawStats.average()),
				formatCount(textCaptureGlyphStats.average()),
				formatCount(textCaptureReplacedUiStats.average()),
				formatCount(textCaptureSceneStats.average()),
				formatCount(textCaptureWorldStats.average()),
				formatCount(textCaptureUiStats.average()),
				formatCount(textCaptureUnknownStats.average()),
				formatCount(primitiveCaptureDrawStats.average()),
				formatCount(primitiveCaptureReplacedUiStats.average()),
				formatCount(rotatedSpriteDrawStats.average()),
				formatCount(rotatedSpriteCapturedUiStats.average()),
				formatCount(circleDrawStats.average()),
				formatCount(circleCapturedUiStats.average()),
				formatCount(nativeUiBlockSpriteStats.average()),
				formatCount(nativeUiBlockTextStats.average()),
				formatCount(nativeUiBlockPrimitiveStats.average()),
				formatCount(nativeUiBlockMinimapStats.average()),
				formatCount(nativeUiBlockGradientStats.average()),
				formatCount(nativeUiBlockClearStats.average()),
				formatCount(nativeUiBlockCircleStats.average()),
				formatCount(nativeUiBlockPixelStats.average()),
				formatCount(nativeUiBaseEligibleStats.average()),
				formatCount(worldGeometryModelStats.average()),
				formatCount(worldGeometryFaceStats.average()),
				formatCount(worldSpriteAnchorStats.average()),
				formatCount(worldGeometryTerrainFaceStats.average()),
				formatCount(worldGeometryWallFaceStats.average()),
				formatCount(worldGeometryRoofFaceStats.average()),
				formatCount(worldGeometryGameObjectFaceStats.average()),
				formatCount(worldGeometryWallObjectFaceStats.average()),
				formatCount(worldGeometryOtherFaceStats.average()),
				formatCount(worldDepthFaceStats.average()),
				formatCount(worldDepthTriangleStats.average()),
				formatCount(worldDepthPixelWriteStats.average()),
				formatVisibility(
					worldDepthConsideredFaceStats,
					worldDepthFaceStats,
					worldDepthRejectedFaceStats,
					false),
				formatCount(worldMeshVertexStats.average()),
				formatCount(worldMeshIndexStats.average()),
				formatCount(worldMeshTriangleStats.average()),
				formatCount(openGLWorldMeshVertexStats.average()),
				formatCount(openGLWorldMeshIndexStats.average()),
				formatCount(openGLWorldMeshTriangleStats.average()),
				formatCount(openGLWorldMeshDrawTriangleStats.average()),
				formatCount(openGLWorldMeshDrawOccluderTriangleStats.average()),
				formatCount(openGLWorldMeshDrawBatchStats.average()),
				formatCount(openGLWorldMeshDrawCallStats.average()),
				formatCount(openGLWorldChunkStats.average()),
				formatCount(openGLWorldChunkVertexStats.average()),
				formatCount(openGLWorldChunkIndexStats.average()),
				formatCount(openGLWorldChunkTriangleStats.average()),
				formatCount(openGLWorldChunkRequestedStats.average()),
				formatCount(openGLWorldChunkUploadStats.average()),
				formatCount(openGLWorldChunkReuseStats.average()),
				formatCount(openGLWorldChunkEvictStats.average()),
				formatVisibility(
					openGLWorldChunkConsideredStats,
					openGLWorldChunkDrawStats,
					openGLWorldChunkCulledStats,
					false)
					+ " | batch c/d/cull " + formatVisibility(
						openGLWorldChunkBatchConsideredStats,
						openGLWorldChunkBatchDrawStats,
						openGLWorldChunkBatchCulledStats,
						false),
				formatCount(openGLWorldChunkDrawCallStats.average())
					+ "/" + formatCount(openGLWorldChunkTextureBindStats.average()),
				formatCount(openGLWorldChunkDrawStats.average()),
				formatCount(openGLWorldChunkDrawTriangleStats.average()),
				formatCount(openGLWorldChunkDrawTerrainStats.average()),
				formatCount(openGLWorldChunkDrawWallStats.average()),
				formatCount(openGLWorldChunkDrawRoofStats.average()),
				formatCount(openGLWorldChunkDrawGameObjectStats.average()),
				formatCount(openGLWorldChunkDrawWallObjectStats.average()),
				formatCount(openGLWorldChunkDrawOtherStats.average()),
				formatCount(openGLWorldChunkDrawFallbackStats.average()),
				formatCount(openGLWorldChunkDrawSkippedStats.average()),
				formatCount(openGLRemasterShadowReceiverChunkStats.average()),
				formatCount(openGLRemasterShadowReceiverTriangleStats.average()),
				formatCount(openGLRemasterShadowCasterStats.average()),
				formatCount(openGLRemasterShadowWallCasterStats.average()),
				formatCount(openGLRemasterShadowGameObjectCasterStats.average()),
				formatCount(openGLRemasterShadowWallObjectCasterStats.average()),
				formatCount(openGLRemasterShadowOutdoorOnlyCasterStats.average()),
				formatCount(openGLRemasterShadowClippingCandidateStats.average()),
				formatCount(openGLRemasterShadowRoofedReceiverStats.average()),
				formatCount(openGLRemasterShadowOutdoorReceiverStats.average()),
				formatCount(openGLRemasterShadowUnknownReceiverStats.average()),
				formatCount(openGLRemasterShadowRoofedCasterStats.average()),
				formatCount(openGLRemasterShadowOutdoorCasterStats.average()),
				formatCount(openGLRemasterShadowUnknownCasterStats.average()),
				formatCount(openGLRemasterShadowSunlightEligibleCasterStats.average()),
				formatCount(openGLRemasterShadowSunlightSuppressedRoofedCasterStats.average()),
				formatCount(openGLRemasterShadowSunlightSuppressedUnknownCasterStats.average()),
				formatCount(openGLRemasterShadowMaskWidthStats.average())
					+ "x" + formatCount(openGLRemasterShadowMaskHeightStats.average())
					+ "/" + formatCount(openGLRemasterShadowMaskVisiblePixelStats.average()),
				formatMillis(openGLRemasterShadowMaskBuildStats.average())
					+ "/" + formatMillis(openGLRemasterShadowMaskUploadStats.average()),
				formatCount(openGLRemasterShadowMaskCacheHitStats.average())
					+ "/" + formatCount(openGLRemasterShadowMaskRebuildStats.average())
					+ "/" + formatCount(openGLRemasterShadowMaskUploadCountStats.average())
					+ "/" + formatCount(openGLRemasterShadowMaskUploadSkipStats.average()),
				formatCount(openGLRemasterShadowMaskStripCasterStats.average())
					+ "/" + formatCount(openGLRemasterShadowMaskSoftSceneryCasterStats.average()),
				formatCount(openGLResidentChunkReplacementRequestedStats.average()),
				formatCount(openGLResidentChunkReplacementActiveStats.average()),
				formatCount(openGLResidentChunkReplacementFallbackStats.average()),
				openGLResidentChunkReplacementReason,
				formatCount(openGLResidentChunkDrawableTerrainBatchStats.average()),
				formatCount(openGLResidentChunkDrawableWallBatchStats.average()),
				formatCount(openGLResidentChunkDrawableRoofBatchStats.average()),
				formatCount(openGLWorldSpriteAnchorStats.average()),
				formatCount(openGLWorldSpriteMatchedStats.average()),
				formatCount(openGLWorldSpriteDrawnStats.average()),
				formatVisibility(
					openGLWorldEntityConsideredStats,
					openGLWorldEntityDrawnStats,
					openGLWorldEntityCulledStats,
					false),
				allocationSummary());
		}
	}

	private static void printReport(
		boolean slowFrame,
		long lastFrameNanos,
		int sourceWidth,
		int sourceHeight,
		float scalar,
		Object scalingType,
		String framePath) {
		StringBuilder summary = new StringBuilder();
		summary.append("[renderer-v2 telemetry] frames=").append(frameStats.count);
		if (slowFrame) {
			summary.append(" slow-frame");
		}
		summary.append(" source=").append(sourceWidth).append('x').append(sourceHeight);
		summary.append(" scalar=").append(scalar);
		summary.append(" mode=").append(scalingType);
		summary.append(" path=").append(framePath);
		summary.append(" last=").append(formatMillis(lastFrameNanos)).append("ms");
		summary.append(" window=").append(formatMillis(frameStats.windowAverage())).append("ms");
		summary.append(" windowMax=").append(formatMillis(frameStats.windowMax)).append("ms");
		summary.append(" avg=").append(formatMillis(frameStats.average())).append("ms");
		summary.append(" max=").append(formatMillis(frameStats.max)).append("ms");
		System.out.println(summary);

		System.out.println(
			"[renderer-v2 telemetry] draw avg ms: scene=" + formatMillis(sceneRenderStats.average())
				+ " commit=" + formatMillis(commitStats.average())
				+ " resize=" + formatMillis(scalarResizeStats.average())
				+ " copy=" + formatMillis(backingCopyStats.average())
				+ " present=" + formatMillis(presentStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] draw window ms: scene=" + formatMillis(sceneRenderStats.windowAverage())
				+ " commit=" + formatMillis(commitStats.windowAverage())
				+ " resize=" + formatMillis(scalarResizeStats.windowAverage())
				+ " copy=" + formatMillis(backingCopyStats.windowAverage())
				+ " present=" + formatMillis(presentStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] scene phases avg ms: rotate=" + formatMillis(sceneModelRotateStats.average())
				+ " cull=" + formatMillis(sceneWorldCullStats.average())
				+ " legacy=" + formatMillis(sceneLegacyDrawStats.average())
				+ " depth=" + formatMillis(sceneDepthExportStats.average())
				+ " mesh=" + formatMillis(sceneMeshExportStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] scene phases window ms: rotate=" + formatMillis(sceneModelRotateStats.windowAverage())
				+ " cull=" + formatMillis(sceneWorldCullStats.windowAverage())
				+ " legacy=" + formatMillis(sceneLegacyDrawStats.windowAverage())
				+ " depth=" + formatMillis(sceneDepthExportStats.windowAverage())
				+ " mesh=" + formatMillis(sceneMeshExportStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] world geometry avg: models=" + formatCount(worldGeometryModelStats.average())
				+ " faces=" + formatCount(worldGeometryFaceStats.average())
				+ " anchors=" + formatCount(worldSpriteAnchorStats.average())
				+ " terrain/wall/roof/obj/wobj/other="
				+ formatCount(worldGeometryTerrainFaceStats.average())
				+ "/" + formatCount(worldGeometryWallFaceStats.average())
				+ "/" + formatCount(worldGeometryRoofFaceStats.average())
				+ "/" + formatCount(worldGeometryGameObjectFaceStats.average())
				+ "/" + formatCount(worldGeometryWallObjectFaceStats.average())
				+ "/" + formatCount(worldGeometryOtherFaceStats.average())
				+ " depth f/t/p=" + formatCount(worldDepthFaceStats.average())
				+ "/" + formatCount(worldDepthTriangleStats.average())
				+ "/" + formatCount(worldDepthPixelWriteStats.average())
				+ " depth c/a/r=" + formatVisibility(
					worldDepthConsideredFaceStats,
					worldDepthFaceStats,
					worldDepthRejectedFaceStats,
					false)
				+ " mesh v/i/t=" + formatCount(worldMeshVertexStats.average())
				+ "/" + formatCount(worldMeshIndexStats.average())
				+ "/" + formatCount(worldMeshTriangleStats.average())
				+ " glmesh v/i/t=" + formatCount(openGLWorldMeshVertexStats.average())
				+ "/" + formatCount(openGLWorldMeshIndexStats.average())
				+ "/" + formatCount(openGLWorldMeshTriangleStats.average())
				+ " glchunks c/v/i/t=" + formatCount(openGLWorldChunkStats.average())
				+ "/" + formatCount(openGLWorldChunkVertexStats.average())
				+ "/" + formatCount(openGLWorldChunkIndexStats.average())
				+ "/" + formatCount(openGLWorldChunkTriangleStats.average())
				+ " chunk req/up/reuse/evict=" + formatCount(openGLWorldChunkRequestedStats.average())
				+ "/" + formatCount(openGLWorldChunkUploadStats.average())
				+ "/" + formatCount(openGLWorldChunkReuseStats.average())
				+ "/" + formatCount(openGLWorldChunkEvictStats.average())
				+ " chunk vis c/d/cull=" + formatVisibility(
					openGLWorldChunkConsideredStats,
					openGLWorldChunkDrawStats,
					openGLWorldChunkCulledStats,
					false)
				+ " batch c/d/cull=" + formatVisibility(
					openGLWorldChunkBatchConsideredStats,
					openGLWorldChunkBatchDrawStats,
					openGLWorldChunkBatchCulledStats,
					false)
				+ " chunk draw c/t=" + formatCount(openGLWorldChunkDrawStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawTriangleStats.average())
				+ " chunk draw t/w/r/go/wo/o=" + formatCount(openGLWorldChunkDrawTerrainStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawWallStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawRoofStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawGameObjectStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawWallObjectStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawOtherStats.average())
				+ " chunk draw fallback/skip=" + formatCount(openGLWorldChunkDrawFallbackStats.average())
				+ "/" + formatCount(openGLWorldChunkDrawSkippedStats.average())
				+ " chunk submit calls/binds=" + formatCount(openGLWorldChunkDrawCallStats.average())
				+ "/" + formatCount(openGLWorldChunkTextureBindStats.average())
				+ " chunk shadow c/i=" + formatCount(openGLWorldChunkShadowChunkStats.average())
				+ "/" + formatCount(openGLWorldChunkShadowIndexStats.average())
				+ " remaster shadow recv c/t=" + formatCount(openGLRemasterShadowReceiverChunkStats.average())
				+ "/" + formatCount(openGLRemasterShadowReceiverTriangleStats.average())
				+ " casters all/w/go/wo/out/clip="
				+ formatCount(openGLRemasterShadowCasterStats.average())
				+ "/" + formatCount(openGLRemasterShadowWallCasterStats.average())
				+ "/" + formatCount(openGLRemasterShadowGameObjectCasterStats.average())
				+ "/" + formatCount(openGLRemasterShadowWallObjectCasterStats.average())
				+ "/" + formatCount(openGLRemasterShadowOutdoorOnlyCasterStats.average())
				+ "/" + formatCount(openGLRemasterShadowClippingCandidateStats.average())
				+ " resident req/active/fallback="
				+ formatCount(openGLResidentChunkReplacementRequestedStats.average())
				+ "/" + formatCount(openGLResidentChunkReplacementActiveStats.average())
				+ "/" + formatCount(openGLResidentChunkReplacementFallbackStats.average())
				+ " resident batches t/w/r="
				+ formatCount(openGLResidentChunkDrawableTerrainBatchStats.average())
				+ "/" + formatCount(openGLResidentChunkDrawableWallBatchStats.average())
				+ "/" + formatCount(openGLResidentChunkDrawableRoofBatchStats.average())
				+ " reason=" + openGLResidentChunkReplacementReason
				+ " uploads/reused=" + formatCount(openGLWorldMeshUploadStats.average())
				+ "/" + formatCount(openGLWorldMeshReuseStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] world geometry window: models=" + formatCount(worldGeometryModelStats.windowAverage())
				+ " faces=" + formatCount(worldGeometryFaceStats.windowAverage())
				+ " anchors=" + formatCount(worldSpriteAnchorStats.windowAverage())
				+ " terrain/wall/roof/obj/wobj/other="
				+ formatCount(worldGeometryTerrainFaceStats.windowAverage())
				+ "/" + formatCount(worldGeometryWallFaceStats.windowAverage())
				+ "/" + formatCount(worldGeometryRoofFaceStats.windowAverage())
				+ "/" + formatCount(worldGeometryGameObjectFaceStats.windowAverage())
				+ "/" + formatCount(worldGeometryWallObjectFaceStats.windowAverage())
				+ "/" + formatCount(worldGeometryOtherFaceStats.windowAverage())
				+ " depth f/t/p=" + formatCount(worldDepthFaceStats.windowAverage())
				+ "/" + formatCount(worldDepthTriangleStats.windowAverage())
				+ "/" + formatCount(worldDepthPixelWriteStats.windowAverage())
				+ " depth c/a/r=" + formatVisibility(
					worldDepthConsideredFaceStats,
					worldDepthFaceStats,
					worldDepthRejectedFaceStats,
					true)
				+ " mesh v/i/t=" + formatCount(worldMeshVertexStats.windowAverage())
				+ "/" + formatCount(worldMeshIndexStats.windowAverage())
				+ "/" + formatCount(worldMeshTriangleStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] opengl world window: glmesh v/i/t="
				+ formatCount(openGLWorldMeshVertexStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshIndexStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshTriangleStats.windowAverage())
				+ " glchunks c/v/i/t=" + formatCount(openGLWorldChunkStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkVertexStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkIndexStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkTriangleStats.windowAverage())
				+ " chunk req/up/reuse/evict=" + formatCount(openGLWorldChunkRequestedStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkUploadStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkReuseStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkEvictStats.windowAverage())
				+ " chunk vis c/d/cull=" + formatVisibility(
					openGLWorldChunkConsideredStats,
					openGLWorldChunkDrawStats,
					openGLWorldChunkCulledStats,
					true)
				+ " batch c/d/cull=" + formatVisibility(
					openGLWorldChunkBatchConsideredStats,
					openGLWorldChunkBatchDrawStats,
					openGLWorldChunkBatchCulledStats,
					true)
				+ " chunk draw c/t=" + formatCount(openGLWorldChunkDrawStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawTriangleStats.windowAverage())
				+ " chunk draw t/w/r/go/wo/o=" + formatCount(openGLWorldChunkDrawTerrainStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawWallStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawRoofStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawGameObjectStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawWallObjectStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawOtherStats.windowAverage())
				+ " fallback/skip=" + formatCount(openGLWorldChunkDrawFallbackStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkDrawSkippedStats.windowAverage())
				+ " chunk submit calls/binds=" + formatCount(openGLWorldChunkDrawCallStats.windowAverage())
				+ "/" + formatCount(openGLWorldChunkTextureBindStats.windowAverage())
				+ " remaster shadow recv c/t=" + formatCount(openGLRemasterShadowReceiverChunkStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowReceiverTriangleStats.windowAverage())
				+ " casters all/w/go/wo/out/clip="
				+ formatCount(openGLRemasterShadowCasterStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowWallCasterStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowGameObjectCasterStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowWallObjectCasterStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowOutdoorOnlyCasterStats.windowAverage())
				+ "/" + formatCount(openGLRemasterShadowClippingCandidateStats.windowAverage())
				+ " resident req/active/fallback="
				+ formatCount(openGLResidentChunkReplacementRequestedStats.windowAverage())
				+ "/" + formatCount(openGLResidentChunkReplacementActiveStats.windowAverage())
				+ "/" + formatCount(openGLResidentChunkReplacementFallbackStats.windowAverage())
				+ " batches t/w/r="
				+ formatCount(openGLResidentChunkDrawableTerrainBatchStats.windowAverage())
				+ "/" + formatCount(openGLResidentChunkDrawableWallBatchStats.windowAverage())
				+ "/" + formatCount(openGLResidentChunkDrawableRoofBatchStats.windowAverage())
				+ " reason=" + openGLResidentChunkReplacementReason
				+ " mesh draw tri/occ/b/calls=" + formatCount(openGLWorldMeshDrawTriangleStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshDrawOccluderTriangleStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshDrawBatchStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshDrawCallStats.windowAverage())
				+ " uploads/reused=" + formatCount(openGLWorldMeshUploadStats.windowAverage())
				+ "/" + formatCount(openGLWorldMeshReuseStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] world mesh materials avg: textured="
				+ formatCount(worldMeshTexturedTriangleStats.average())
				+ " flat=" + formatCount(worldMeshFlatColorTriangleStats.average())
				+ " transparent=" + formatCount(worldMeshTransparentTriangleStats.average())
				+ " skipped=" + formatCount(worldMeshSkippedTriangleStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] world mesh materials window: textured="
				+ formatCount(worldMeshTexturedTriangleStats.windowAverage())
				+ " flat=" + formatCount(worldMeshFlatColorTriangleStats.windowAverage())
				+ " transparent=" + formatCount(worldMeshTransparentTriangleStats.windowAverage())
				+ " skipped=" + formatCount(worldMeshSkippedTriangleStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] opengl world textures avg: referenced="
				+ formatCount(openGLWorldTextureReferencedStats.average())
				+ " cached=" + formatCount(openGLWorldTextureCachedStats.average())
				+ " uploaded=" + formatCount(openGLWorldTextureUploadedStats.average())
				+ " missing=" + formatCount(openGLWorldTextureMissingStats.average())
				+ " atlases=" + formatCount(openGLWorldTextureAtlasStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] opengl world sprites avg: anchors="
				+ formatCount(openGLWorldSpriteAnchorStats.average())
				+ " matched=" + formatCount(openGLWorldSpriteMatchedStats.average())
				+ " drawn=" + formatCount(openGLWorldSpriteDrawnStats.average())
				+ " entity vis c/d/cull=" + formatVisibility(
					openGLWorldEntityConsideredStats,
					openGLWorldEntityDrawnStats,
					openGLWorldEntityCulledStats,
					false));

		System.out.println(
			"[renderer-v2 telemetry] presentation avg ms: setImage=" + formatMillis(setGameImageStats.average())
				+ " sourceCopy=" + formatMillis(sourceCopyStats.average())
				+ " paintImmediate=" + formatMillis(paintImmediateStats.average())
				+ " viewportPaint=" + formatMillis(viewportPaintStats.average())
				+ " viewportScale=" + formatMillis(viewportScaleStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] scale ops avg ms: smooth=" + formatMillis(smoothScaleStats.average())
				+ " gpuPresenter=" + formatMillis(gpuPresenterStats.average())
				+ " nearestPaints=" + nearestScalePaints
				+ " interpolationPaints=" + interpolationScalePaints
				+ " gpuPaints=" + gpuPresenterPaints);

		System.out.println(
			"[renderer-v2 telemetry] opengl avg ms: snapshot=" + formatMillis(openGLSnapshotStats.average())
				+ " upload=" + formatMillis(openGLUploadStats.average())
				+ " render=" + formatMillis(openGLRenderStats.average())
				+ " frames=" + openGLFrames
				+ " dropped=" + openGLDroppedFrames);

		System.out.println(
			"[renderer-v2 telemetry] opengl window ms: snapshot=" + formatMillis(openGLSnapshotStats.windowAverage())
				+ " upload=" + formatMillis(openGLUploadStats.windowAverage())
				+ " render=" + formatMillis(openGLRenderStats.windowAverage())
				+ " frames=" + openGLFramesWindow
				+ " dropped=" + openGLDroppedFramesWindow
				+ " phases b/w/ws/o/db/s=" + formatMillis(openGLBaseStats.windowAverage())
				+ "/" + formatMillis(openGLWorldStats.windowAverage())
				+ "/" + formatMillis(openGLWorldSpriteStats.windowAverage())
				+ "/" + formatMillis(openGLSpriteOverlayStats.windowAverage())
				+ "/" + formatMillis(openGLDebugOverlayStats.windowAverage())
				+ "/" + formatMillis(openGLSwapStats.windowAverage())
				+ " world split chunk/proj/chdraw=" + formatMillis(openGLWorldChunkUploadPhaseStats.windowAverage())
				+ "/" + formatMillis(openGLWorldProjectedMeshPhaseStats.windowAverage())
				+ "/" + formatMillis(openGLWorldChunkDrawPhaseStats.windowAverage()));

		System.out.println(
			"[renderer-v2 telemetry] sprite overlay avg: captured=" + formatCount(spriteOverlayCapturedStats.average())
				+ " static=" + formatCount(spriteOverlayStaticReplayStats.average())
				+ " visible=" + formatCount(spriteOverlayVisibleReplayStats.average())
				+ " skipOrdered=" + formatCount(spriteOverlaySkippedOrderedStats.average())
				+ " skipInvisible=" + formatCount(spriteOverlaySkippedInvisibleStats.average())
				+ " skipAtlasFull=" + formatCount(spriteOverlaySkippedAtlasFullStats.average())
				+ " visiblePixels=" + formatCount(spriteOverlayVisiblePixelStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] sprite capture avg: attempts=" + formatCount(spriteCaptureAttemptStats.average())
				+ " accepted=" + formatCount(spriteCaptureAcceptedStats.average())
				+ " replacedUi=" + formatCount(spriteCaptureReplacedUiStats.average())
				+ " uiBase=" + formatCount(spriteCaptureUiBaseStats.average())
				+ " skipAlpha=" + formatCount(spriteCaptureSkippedAlphaStats.average())
				+ " skipBounds=" + formatCount(spriteCaptureSkippedBoundsStats.average())
				+ " skipSource=" + formatCount(spriteCaptureSkippedSourceStats.average())
				+ " skipTransform=" + formatCount(spriteCaptureSkippedTransformStats.average())
				+ " skipInterlace=" + formatCount(spriteCaptureSkippedInterlaceStats.average())
				+ " skipOverflow=" + formatCount(spriteCaptureSkippedOverflowStats.average())
				+ " skipInvalid=" + formatCount(spriteCaptureSkippedInvalidStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] text capture avg: draws=" + formatCount(textCaptureDrawStats.average())
				+ " glyphs=" + formatCount(textCaptureGlyphStats.average())
				+ " replacedUi=" + formatCount(textCaptureReplacedUiStats.average())
				+ " scene=" + formatCount(textCaptureSceneStats.average())
				+ " world=" + formatCount(textCaptureWorldStats.average())
				+ " ui=" + formatCount(textCaptureUiStats.average())
				+ " unknown=" + formatCount(textCaptureUnknownStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] primitive capture avg: draws=" + formatCount(primitiveCaptureDrawStats.average())
				+ " replacedUi=" + formatCount(primitiveCaptureReplacedUiStats.average())
				+ " baseReady=" + formatCount(nativeUiBaseEligibleStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] native UI base avg: rotated="
				+ formatCount(rotatedSpriteDrawStats.average())
				+ "/" + formatCount(rotatedSpriteCapturedUiStats.average())
				+ " circle=" + formatCount(circleDrawStats.average())
				+ "/" + formatCount(circleCapturedUiStats.average())
				+ " blockers s/t/p/m=" + formatCount(nativeUiBlockSpriteStats.average())
				+ "/" + formatCount(nativeUiBlockTextStats.average())
				+ "/" + formatCount(nativeUiBlockPrimitiveStats.average())
				+ "/" + formatCount(nativeUiBlockMinimapStats.average())
				+ " g/circ/pix/clear=" + formatCount(nativeUiBlockGradientStats.average())
				+ "/" + formatCount(nativeUiBlockCircleStats.average())
				+ "/" + formatCount(nativeUiBlockPixelStats.average())
				+ "/" + formatCount(nativeUiBlockClearStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] sprite phases avg: scene=" + formatCount(spriteOverlaySceneCommandStats.average())
				+ " world=" + formatCount(spriteOverlayWorldCommandStats.average())
				+ " ui=" + formatCount(spriteOverlayUiCommandStats.average())
				+ " unknown=" + formatCount(spriteOverlayUnknownCommandStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] sprite direct phases avg: scene=" + formatCount(spriteOverlayDirectSceneStats.average())
				+ " world=" + formatCount(spriteOverlayDirectWorldStats.average())
				+ " ui=" + formatCount(spriteOverlayDirectUiStats.average())
				+ " unknown=" + formatCount(spriteOverlayDirectUnknownStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] sprite visible phases avg: scene=" + formatCount(spriteOverlayVisibleSceneStats.average())
				+ " world=" + formatCount(spriteOverlayVisibleWorldStats.average())
				+ " ui=" + formatCount(spriteOverlayVisibleUiStats.average())
				+ " unknown=" + formatCount(spriteOverlayVisibleUnknownStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] presents: repaint=" + repaintRequests
				+ " paintImmediate=" + paintImmediateRequests
				+ " allocations=" + allocationSummary());
		resetReportWindow();
	}

	private static void resetReportWindow() {
		StageStats[] stageStats = {
			frameStats,
			clientLoopStats,
			clientLoopSleepStats,
			clientLoopUpdateStats,
			clientLoopRepositionStats,
			clientLoopDrawStats,
			sceneRenderStats,
			sceneModelRotateStats,
			sceneWorldCullStats,
			sceneDepthExportStats,
			sceneLegacyDrawStats,
			sceneMeshExportStats,
			commitStats,
			scalarResizeStats,
			backingCopyStats,
			presentStats,
			setGameImageStats,
			sourceCopyStats,
			paintImmediateStats,
			viewportPaintStats,
			viewportScaleStats,
			smoothScaleStats,
			gpuPresenterStats,
			openGLSnapshotStats,
			openGLUploadStats,
			openGLRenderStats,
			openGLBaseStats,
			openGLWorldStats,
			openGLWorldSpriteStats,
			openGLWorldChunkUploadPhaseStats,
			openGLWorldProjectedMeshPhaseStats,
			openGLWorldChunkDrawPhaseStats,
			openGLRemasterShadowMaskBuildStats,
			openGLRemasterShadowMaskUploadStats,
			openGLSpriteOverlayStats,
			openGLDebugOverlayStats,
			openGLSwapStats
		};
		for (StageStats stats : stageStats) {
			stats.resetWindow();
		}

		CounterStats[] counterStats = {
			spriteOverlayCapturedStats,
			spriteOverlayStaticReplayStats,
			spriteOverlayVisibleReplayStats,
			spriteOverlaySkippedOrderedStats,
			spriteOverlaySkippedInvisibleStats,
			spriteOverlaySkippedAtlasFullStats,
			spriteOverlayVisiblePixelStats,
			spriteOverlaySceneCommandStats,
			spriteOverlayWorldCommandStats,
			spriteOverlayUiCommandStats,
			spriteOverlayUnknownCommandStats,
			spriteOverlayDirectSceneStats,
			spriteOverlayDirectWorldStats,
			spriteOverlayDirectUiStats,
			spriteOverlayDirectUnknownStats,
			spriteOverlayVisibleSceneStats,
			spriteOverlayVisibleWorldStats,
			spriteOverlayVisibleUiStats,
			spriteOverlayVisibleUnknownStats,
			legacySceneSpriteRestoreCommandStats,
			legacySceneSpriteRestoreFallbackStats,
			legacySceneSpriteRestoreFallbackPixelStats,
			spriteCaptureAttemptStats,
			spriteCaptureAcceptedStats,
			spriteCaptureReplacedUiStats,
			spriteCaptureUiBaseStats,
			spriteCaptureSkippedAlphaStats,
			spriteCaptureSkippedBoundsStats,
			spriteCaptureSkippedSourceStats,
			spriteCaptureSkippedTransformStats,
			spriteCaptureSkippedInterlaceStats,
			spriteCaptureSkippedOverflowStats,
			spriteCaptureSkippedInvalidStats,
			textCaptureDrawStats,
			textCaptureGlyphStats,
			textCaptureReplacedUiStats,
			textCaptureSceneStats,
			textCaptureWorldStats,
			textCaptureUiStats,
			textCaptureUnknownStats,
			primitiveCaptureDrawStats,
			primitiveCaptureReplacedUiStats,
			rotatedSpriteDrawStats,
			rotatedSpriteCapturedUiStats,
			circleDrawStats,
			circleCapturedUiStats,
			nativeUiBlockSpriteStats,
			nativeUiBlockTextStats,
			nativeUiBlockPrimitiveStats,
			nativeUiBlockMinimapStats,
			nativeUiBlockGradientStats,
			nativeUiBlockClearStats,
			nativeUiBlockCircleStats,
			nativeUiBlockPixelStats,
			nativeUiBaseEligibleStats,
			clientLoopUpdateCountStats,
			clientLoopSkippedDrawStats,
			clientLoopSleepRequestStats,
			clientLoopStepSizeStats,
			worldGeometryModelStats,
			worldGeometryFaceStats,
			worldSpriteAnchorStats,
			worldGeometryTerrainFaceStats,
			worldGeometryWallFaceStats,
			worldGeometryRoofFaceStats,
			worldGeometryGameObjectFaceStats,
			worldGeometryWallObjectFaceStats,
			worldGeometryOtherFaceStats,
			worldDepthConsideredFaceStats,
			worldDepthFaceStats,
			worldDepthRejectedFaceStats,
			worldDepthTriangleStats,
			worldDepthPixelWriteStats,
			worldMeshVertexStats,
			worldMeshIndexStats,
			worldMeshTriangleStats,
			worldMeshTexturedTriangleStats,
			worldMeshFlatColorTriangleStats,
			worldMeshTransparentTriangleStats,
			worldMeshSkippedTriangleStats,
			openGLWorldMeshVertexStats,
			openGLWorldMeshIndexStats,
			openGLWorldMeshTriangleStats,
			openGLWorldMeshUploadStats,
			openGLWorldMeshReuseStats,
			openGLWorldMeshDrawTriangleStats,
			openGLWorldMeshDrawOccluderTriangleStats,
			openGLWorldMeshDrawBatchStats,
			openGLWorldMeshDrawCallStats,
			openGLWorldChunkStats,
			openGLWorldChunkVertexStats,
			openGLWorldChunkIndexStats,
			openGLWorldChunkTriangleStats,
			openGLWorldChunkRequestedStats,
			openGLWorldChunkUploadStats,
			openGLWorldChunkReuseStats,
			openGLWorldChunkEvictStats,
			openGLWorldChunkConsideredStats,
			openGLWorldChunkDrawStats,
			openGLWorldChunkCulledStats,
			openGLWorldChunkBatchConsideredStats,
			openGLWorldChunkBatchDrawStats,
			openGLWorldChunkBatchCulledStats,
			openGLWorldChunkDrawTriangleStats,
			openGLWorldChunkDrawTerrainStats,
			openGLWorldChunkDrawWallStats,
			openGLWorldChunkDrawRoofStats,
			openGLWorldChunkDrawGameObjectStats,
			openGLWorldChunkDrawWallObjectStats,
			openGLWorldChunkDrawOtherStats,
			openGLWorldChunkDrawFallbackStats,
			openGLWorldChunkDrawSkippedStats,
			openGLWorldChunkDrawCallStats,
			openGLWorldChunkTextureBindStats,
			openGLWorldChunkShadowChunkStats,
			openGLWorldChunkShadowIndexStats,
			openGLRemasterShadowReceiverChunkStats,
			openGLRemasterShadowReceiverTriangleStats,
			openGLRemasterShadowCasterStats,
			openGLRemasterShadowWallCasterStats,
			openGLRemasterShadowGameObjectCasterStats,
			openGLRemasterShadowWallObjectCasterStats,
			openGLRemasterShadowOutdoorOnlyCasterStats,
			openGLRemasterShadowClippingCandidateStats,
			openGLRemasterShadowRoofedReceiverStats,
			openGLRemasterShadowOutdoorReceiverStats,
			openGLRemasterShadowUnknownReceiverStats,
			openGLRemasterShadowRoofedCasterStats,
			openGLRemasterShadowOutdoorCasterStats,
			openGLRemasterShadowUnknownCasterStats,
			openGLRemasterShadowSunlightEligibleCasterStats,
			openGLRemasterShadowSunlightSuppressedRoofedCasterStats,
			openGLRemasterShadowSunlightSuppressedUnknownCasterStats,
			openGLRemasterShadowMaskWidthStats,
			openGLRemasterShadowMaskHeightStats,
			openGLRemasterShadowMaskVisiblePixelStats,
			openGLRemasterShadowMaskCacheHitStats,
			openGLRemasterShadowMaskRebuildStats,
			openGLRemasterShadowMaskUploadCountStats,
			openGLRemasterShadowMaskUploadSkipStats,
			openGLRemasterShadowMaskStripCasterStats,
			openGLRemasterShadowMaskSoftSceneryCasterStats,
			openGLResidentChunkReplacementRequestedStats,
			openGLResidentChunkReplacementActiveStats,
			openGLResidentChunkReplacementFallbackStats,
			openGLResidentChunkDrawableTerrainBatchStats,
			openGLResidentChunkDrawableWallBatchStats,
			openGLResidentChunkDrawableRoofBatchStats,
			openGLWorldTextureReferencedStats,
			openGLWorldTextureCachedStats,
			openGLWorldTextureUploadedStats,
			openGLWorldTextureMissingStats,
			openGLWorldTextureAtlasStats,
			openGLWorldSpriteAnchorStats,
			openGLWorldSpriteMatchedStats,
			openGLWorldSpriteDrawnStats,
			openGLWorldEntityConsideredStats,
			openGLWorldEntityDrawnStats,
			openGLWorldEntityCulledStats
		};
		for (CounterStats stats : counterStats) {
			stats.resetWindow();
		}
		openGLFramesWindow = 0L;
		openGLDroppedFramesWindow = 0L;
	}

	private static String recentFrameSummary() {
		long frameNanos = frameStats.recentAverage();
		return formatMillis(frameNanos)
			+ "/" + formatMillis(sceneRenderStats.recentAverage())
			+ "/" + formatMillis(openGLRenderStats.recentAverage())
			+ "ms | drawfps " + formatFramesPerSecond(frameNanos);
	}

	private static String recentOpenGLTimingSummary() {
		return formatMillis(openGLSnapshotStats.recentAverage())
			+ "/" + formatMillis(openGLUploadStats.recentAverage())
			+ "/" + formatMillis(openGLRenderStats.recentAverage())
			+ "ms";
	}

	private static String recentOpenGLPhaseSummary() {
		return formatMillis(openGLBaseStats.recentAverage())
			+ "/" + formatMillis(openGLWorldStats.recentAverage())
			+ "/" + formatMillis(openGLWorldSpriteStats.recentAverage())
			+ "/" + formatMillis(openGLSpriteOverlayStats.recentAverage())
			+ "/" + formatMillis(openGLDebugOverlayStats.recentAverage())
			+ "/" + formatMillis(openGLSwapStats.recentAverage())
			+ "ms";
	}

	private static String recentScenePhaseSummary() {
		return formatMillis(sceneModelRotateStats.recentAverage())
			+ "/" + formatMillis(sceneWorldCullStats.recentAverage())
			+ "/" + formatMillis(sceneDepthExportStats.recentAverage())
			+ "/" + formatMillis(sceneLegacyDrawStats.recentAverage())
			+ "/" + formatMillis(sceneMeshExportStats.recentAverage())
			+ "ms";
	}

	private static String recentClientLoopSummary() {
		return formatMillis(clientLoopStats.recentAverage())
			+ "/" + formatMillis(clientLoopSleepStats.recentAverage())
			+ "/" + formatMillis(clientLoopUpdateStats.recentAverage())
			+ "/" + formatMillis(clientLoopRepositionStats.recentAverage())
			+ "/" + formatMillis(clientLoopDrawStats.recentAverage())
			+ "ms | upd/skip/sleep/step "
			+ formatCount(clientLoopUpdateCountStats.recentAverage())
			+ "/" + formatCount(clientLoopSkippedDrawStats.recentAverage())
			+ "/" + formatCount(clientLoopSleepRequestStats.recentAverage())
			+ "/" + formatCount(clientLoopStepSizeStats.recentAverage());
	}

	private static String allocationSummary() {
		if (allocationStats.isEmpty()) {
			return "none";
		}

		StringBuilder summary = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, AllocationStats> entry : allocationStats.entrySet()) {
			if (!first) {
				summary.append(", ");
			}
			first = false;
			AllocationStats stats = entry.getValue();
			summary.append(entry.getKey())
				.append('=')
				.append(stats.count)
				.append('/')
				.append(formatMegabytes(stats.estimatedBytes))
				.append("MB");
		}
		return summary.toString();
	}

	private static String formatMillis(long nanos) {
		return String.format("%.3f", nanos / 1_000_000.0);
	}

	private static String formatMegabytes(long bytes) {
		return String.format("%.2f", bytes / (1024.0 * 1024.0));
	}

	private static String formatCount(double count) {
		if (count >= 100.0) {
			return String.valueOf(Math.round(count));
		}
		return String.format("%.1f", count);
	}

	private static String formatFramesPerSecond(long averageFrameNanos) {
		if (averageFrameNanos <= 0L) {
			return "0.0";
		}
		return String.format("%.1f", 1_000_000_000.0 / averageFrameNanos);
	}

	private static String formatVisibility(
		CounterStats consideredStats,
		CounterStats drawnStats,
		CounterStats culledStats,
		boolean window) {
		double considered = window ? consideredStats.windowAverage() : consideredStats.average();
		double drawn = window ? drawnStats.windowAverage() : drawnStats.average();
		double culled = window ? culledStats.windowAverage() : culledStats.average();
		return formatCount(considered) + "/" + formatCount(drawn) + "/" + formatCount(culled);
	}

	private static boolean readBoolean(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null) {
			return false;
		}

		value = value.trim();
		return "true".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "yes".equalsIgnoreCase(value)
			|| "on".equalsIgnoreCase(value);
	}

	private static int readInt(String propertyName, int defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static int bytesPerPixel(int imageType) {
		if (imageType == BufferedImage.TYPE_3BYTE_BGR) {
			return 3;
		}
		return 4;
	}

	private static final class StageStats {
		private long count;
		private long total;
		private long max;
		private long windowCount;
		private long windowTotal;
		private long windowMax;
		private final long[] recentSamples = new long[RECENT_SAMPLE_LIMIT];
		private int recentIndex;
		private int recentCount;
		private long recentTotal;

		private void record(long nanos) {
			count++;
			total += nanos;
			if (nanos > max) {
				max = nanos;
			}
			windowCount++;
			windowTotal += nanos;
			if (nanos > windowMax) {
				windowMax = nanos;
			}
			recordRecent(nanos);
		}

		private long average() {
			return count == 0L ? 0L : total / count;
		}

		private long windowAverage() {
			return windowCount == 0L ? 0L : windowTotal / windowCount;
		}

		private long recentAverage() {
			return recentCount == 0 ? 0L : recentTotal / recentCount;
		}

		private void resetWindow() {
			windowCount = 0L;
			windowTotal = 0L;
			windowMax = 0L;
		}

		private void recordRecent(long nanos) {
			if (recentCount < recentSamples.length) {
				recentSamples[recentIndex] = nanos;
				recentTotal += nanos;
				recentCount++;
			} else {
				recentTotal -= recentSamples[recentIndex];
				recentSamples[recentIndex] = nanos;
				recentTotal += nanos;
			}
			recentIndex = (recentIndex + 1) % recentSamples.length;
		}
	}

	private static final class AllocationStats {
		private long count;
		private long estimatedBytes;

		private void record(int width, int height, int bytesPerPixel) {
			count++;
			estimatedBytes += (long) width * (long) height * (long) bytesPerPixel;
		}
	}

	private static final class CounterStats {
		private long count;
		private long total;
		private long max;
		private long windowCount;
		private long windowTotal;
		private long windowMax;
		private final int[] recentSamples = new int[RECENT_SAMPLE_LIMIT];
		private int recentIndex;
		private int recentCount;
		private long recentTotal;

		private void record(int value) {
			count++;
			total += value;
			if (value > max) {
				max = value;
			}
			windowCount++;
			windowTotal += value;
			if (value > windowMax) {
				windowMax = value;
			}
			recordRecent(value);
		}

		private double average() {
			return count == 0L ? 0.0 : total / (double) count;
		}

		private double windowAverage() {
			return windowCount == 0L ? 0.0 : windowTotal / (double) windowCount;
		}

		private double recentAverage() {
			return recentCount == 0 ? 0.0 : recentTotal / (double) recentCount;
		}

		private void resetWindow() {
			windowCount = 0L;
			windowTotal = 0L;
			windowMax = 0L;
		}

		private void recordRecent(int value) {
			if (recentCount < recentSamples.length) {
				recentSamples[recentIndex] = value;
				recentTotal += value;
				recentCount++;
			} else {
				recentTotal -= recentSamples[recentIndex];
				recentSamples[recentIndex] = value;
				recentTotal += value;
			}
			recentIndex = (recentIndex + 1) % recentSamples.length;
		}
	}

	static final class Snapshot {
		final boolean enabled;
		final long frameCount;
		final String frameAverageMs;
		final String frameMaxMs;
		final String sceneAverageMs;
		final String sceneModelRotateAverageMs;
		final String sceneWorldCullAverageMs;
		final String sceneDepthExportAverageMs;
		final String sceneLegacyDrawAverageMs;
		final String sceneMeshExportAverageMs;
		final String setImageAverageMs;
		final String sourceCopyAverageMs;
		final String openGLSnapshotAverageMs;
		final String openGLUploadAverageMs;
		final String openGLRenderAverageMs;
		final String openGLBaseAverageMs;
		final String openGLWorldAverageMs;
		final String openGLWorldSpriteAverageMs;
		final String openGLSpriteOverlayAverageMs;
		final String openGLDebugOverlayAverageMs;
		final String openGLSwapAverageMs;
		final String openGLWorldChunkUploadPhaseAverageMs;
		final String openGLWorldProjectedMeshPhaseAverageMs;
		final String openGLWorldChunkDrawPhaseAverageMs;
		final long openGLFrames;
		final long openGLDroppedFrames;
		final long repaintRequests;
		final String recentFrameSummary;
		final String recentOpenGLTimingSummary;
		final String recentOpenGLPhaseSummary;
		final String recentScenePhaseSummary;
		final String recentClientLoopSummary;
		final String spriteOverlayCapturedAverage;
		final String spriteOverlayStaticReplayAverage;
		final String spriteOverlayVisibleReplayAverage;
		final String spriteOverlaySkippedOrderedAverage;
		final String spriteOverlaySkippedInvisibleAverage;
		final String spriteOverlaySkippedAtlasFullAverage;
		final String spriteOverlayVisiblePixelsAverage;
		final String spriteOverlaySceneCommandAverage;
		final String spriteOverlayWorldCommandAverage;
		final String spriteOverlayUiCommandAverage;
		final String spriteOverlayUnknownCommandAverage;
		final String spriteOverlayDirectSceneAverage;
		final String spriteOverlayDirectWorldAverage;
		final String spriteOverlayDirectUiAverage;
		final String spriteOverlayDirectUnknownAverage;
		final String spriteOverlayVisibleSceneAverage;
		final String spriteOverlayVisibleWorldAverage;
		final String spriteOverlayVisibleUiAverage;
		final String spriteOverlayVisibleUnknownAverage;
		final String legacySceneSpriteRestoreCommandAverage;
		final String legacySceneSpriteRestoreFallbackAverage;
		final String legacySceneSpriteRestoreFallbackPixelAverage;
		final String spriteCaptureAttemptAverage;
		final String spriteCaptureAcceptedAverage;
		final String spriteCaptureReplacedUiAverage;
		final String spriteCaptureUiBaseAverage;
		final String spriteCaptureSkippedAlphaAverage;
		final String spriteCaptureSkippedBoundsAverage;
		final String spriteCaptureSkippedSourceAverage;
		final String spriteCaptureSkippedTransformAverage;
		final String spriteCaptureSkippedInterlaceAverage;
		final String spriteCaptureSkippedOverflowAverage;
		final String spriteCaptureSkippedInvalidAverage;
		final String textCaptureDrawAverage;
		final String textCaptureGlyphAverage;
		final String textCaptureReplacedUiAverage;
		final String textCaptureSceneAverage;
		final String textCaptureWorldAverage;
		final String textCaptureUiAverage;
		final String textCaptureUnknownAverage;
		final String primitiveCaptureDrawAverage;
		final String primitiveCaptureReplacedUiAverage;
		final String rotatedSpriteDrawAverage;
		final String rotatedSpriteCapturedUiAverage;
		final String circleDrawAverage;
		final String circleCapturedUiAverage;
		final String nativeUiBlockSpriteAverage;
		final String nativeUiBlockTextAverage;
		final String nativeUiBlockPrimitiveAverage;
		final String nativeUiBlockMinimapAverage;
		final String nativeUiBlockGradientAverage;
		final String nativeUiBlockClearAverage;
		final String nativeUiBlockCircleAverage;
		final String nativeUiBlockPixelAverage;
		final String nativeUiBaseEligibleAverage;
		final String worldGeometryModelAverage;
		final String worldGeometryFaceAverage;
		final String worldSpriteAnchorAverage;
		final String worldGeometryTerrainFaceAverage;
		final String worldGeometryWallFaceAverage;
		final String worldGeometryRoofFaceAverage;
		final String worldGeometryGameObjectFaceAverage;
		final String worldGeometryWallObjectFaceAverage;
		final String worldGeometryOtherFaceAverage;
		final String worldDepthFaceAverage;
		final String worldDepthTriangleAverage;
		final String worldDepthPixelWriteAverage;
		final String worldDepthCullAverage;
		final String worldMeshVertexAverage;
		final String worldMeshIndexAverage;
		final String worldMeshTriangleAverage;
		final String openGLWorldMeshVertexAverage;
		final String openGLWorldMeshIndexAverage;
		final String openGLWorldMeshTriangleAverage;
		final String openGLWorldMeshDrawTriangleAverage;
		final String openGLWorldMeshDrawOccluderTriangleAverage;
		final String openGLWorldMeshDrawBatchAverage;
		final String openGLWorldMeshDrawCallAverage;
		final String openGLWorldChunkAverage;
		final String openGLWorldChunkVertexAverage;
		final String openGLWorldChunkIndexAverage;
		final String openGLWorldChunkTriangleAverage;
		final String openGLWorldChunkRequestedAverage;
		final String openGLWorldChunkUploadAverage;
		final String openGLWorldChunkReuseAverage;
		final String openGLWorldChunkEvictAverage;
		final String openGLWorldChunkVisibilityAverage;
		final String openGLWorldChunkSubmitAverage;
		final String openGLWorldChunkDrawAverage;
		final String openGLWorldChunkDrawTriangleAverage;
		final String openGLWorldChunkDrawTerrainAverage;
		final String openGLWorldChunkDrawWallAverage;
		final String openGLWorldChunkDrawRoofAverage;
		final String openGLWorldChunkDrawGameObjectAverage;
		final String openGLWorldChunkDrawWallObjectAverage;
		final String openGLWorldChunkDrawOtherAverage;
		final String openGLWorldChunkDrawFallbackAverage;
		final String openGLWorldChunkDrawSkippedAverage;
		final String openGLRemasterShadowReceiverChunkAverage;
		final String openGLRemasterShadowReceiverTriangleAverage;
		final String openGLRemasterShadowCasterAverage;
		final String openGLRemasterShadowWallCasterAverage;
		final String openGLRemasterShadowGameObjectCasterAverage;
		final String openGLRemasterShadowWallObjectCasterAverage;
		final String openGLRemasterShadowOutdoorOnlyCasterAverage;
		final String openGLRemasterShadowClippingCandidateAverage;
		final String openGLRemasterShadowRoofedReceiverAverage;
		final String openGLRemasterShadowOutdoorReceiverAverage;
		final String openGLRemasterShadowUnknownReceiverAverage;
		final String openGLRemasterShadowRoofedCasterAverage;
		final String openGLRemasterShadowOutdoorCasterAverage;
		final String openGLRemasterShadowUnknownCasterAverage;
		final String openGLRemasterShadowSunlightEligibleCasterAverage;
		final String openGLRemasterShadowSunlightSuppressedRoofedCasterAverage;
		final String openGLRemasterShadowSunlightSuppressedUnknownCasterAverage;
		final String openGLRemasterShadowMaskSizeAverage;
		final String openGLRemasterShadowMaskTimingAverageMs;
		final String openGLRemasterShadowMaskCacheAverage;
		final String openGLRemasterShadowMaskCasterAverage;
		final String openGLResidentChunkReplacementRequestedAverage;
		final String openGLResidentChunkReplacementActiveAverage;
		final String openGLResidentChunkReplacementFallbackAverage;
		final String openGLResidentChunkReplacementReason;
		final String openGLResidentChunkDrawableTerrainBatchAverage;
		final String openGLResidentChunkDrawableWallBatchAverage;
		final String openGLResidentChunkDrawableRoofBatchAverage;
		final String openGLWorldSpriteAnchorAverage;
		final String openGLWorldSpriteMatchedAverage;
		final String openGLWorldSpriteDrawnAverage;
		final String openGLWorldEntityVisibilityAverage;
		final String allocations;

		private Snapshot(
			boolean enabled,
			long frameCount,
			String frameAverageMs,
			String frameMaxMs,
			String sceneAverageMs,
			String sceneModelRotateAverageMs,
			String sceneWorldCullAverageMs,
			String sceneDepthExportAverageMs,
			String sceneLegacyDrawAverageMs,
			String sceneMeshExportAverageMs,
			String setImageAverageMs,
			String sourceCopyAverageMs,
			String openGLSnapshotAverageMs,
			String openGLUploadAverageMs,
			String openGLRenderAverageMs,
			String openGLBaseAverageMs,
			String openGLWorldAverageMs,
			String openGLWorldSpriteAverageMs,
			String openGLSpriteOverlayAverageMs,
			String openGLDebugOverlayAverageMs,
			String openGLSwapAverageMs,
			String openGLWorldChunkUploadPhaseAverageMs,
			String openGLWorldProjectedMeshPhaseAverageMs,
			String openGLWorldChunkDrawPhaseAverageMs,
			long openGLFrames,
			long openGLDroppedFrames,
			long repaintRequests,
			String recentFrameSummary,
			String recentOpenGLTimingSummary,
			String recentOpenGLPhaseSummary,
			String recentScenePhaseSummary,
			String recentClientLoopSummary,
			String spriteOverlayCapturedAverage,
			String spriteOverlayStaticReplayAverage,
			String spriteOverlayVisibleReplayAverage,
			String spriteOverlaySkippedOrderedAverage,
			String spriteOverlaySkippedInvisibleAverage,
			String spriteOverlaySkippedAtlasFullAverage,
			String spriteOverlayVisiblePixelsAverage,
			String spriteOverlaySceneCommandAverage,
			String spriteOverlayWorldCommandAverage,
			String spriteOverlayUiCommandAverage,
			String spriteOverlayUnknownCommandAverage,
			String spriteOverlayDirectSceneAverage,
			String spriteOverlayDirectWorldAverage,
			String spriteOverlayDirectUiAverage,
			String spriteOverlayDirectUnknownAverage,
			String spriteOverlayVisibleSceneAverage,
			String spriteOverlayVisibleWorldAverage,
			String spriteOverlayVisibleUiAverage,
			String spriteOverlayVisibleUnknownAverage,
			String legacySceneSpriteRestoreCommandAverage,
			String legacySceneSpriteRestoreFallbackAverage,
			String legacySceneSpriteRestoreFallbackPixelAverage,
			String spriteCaptureAttemptAverage,
			String spriteCaptureAcceptedAverage,
			String spriteCaptureReplacedUiAverage,
			String spriteCaptureUiBaseAverage,
			String spriteCaptureSkippedAlphaAverage,
			String spriteCaptureSkippedBoundsAverage,
			String spriteCaptureSkippedSourceAverage,
			String spriteCaptureSkippedTransformAverage,
			String spriteCaptureSkippedInterlaceAverage,
			String spriteCaptureSkippedOverflowAverage,
			String spriteCaptureSkippedInvalidAverage,
			String textCaptureDrawAverage,
			String textCaptureGlyphAverage,
			String textCaptureReplacedUiAverage,
			String textCaptureSceneAverage,
			String textCaptureWorldAverage,
			String textCaptureUiAverage,
			String textCaptureUnknownAverage,
			String primitiveCaptureDrawAverage,
			String primitiveCaptureReplacedUiAverage,
			String rotatedSpriteDrawAverage,
			String rotatedSpriteCapturedUiAverage,
			String circleDrawAverage,
			String circleCapturedUiAverage,
			String nativeUiBlockSpriteAverage,
			String nativeUiBlockTextAverage,
			String nativeUiBlockPrimitiveAverage,
			String nativeUiBlockMinimapAverage,
			String nativeUiBlockGradientAverage,
			String nativeUiBlockClearAverage,
			String nativeUiBlockCircleAverage,
			String nativeUiBlockPixelAverage,
			String nativeUiBaseEligibleAverage,
			String worldGeometryModelAverage,
			String worldGeometryFaceAverage,
			String worldSpriteAnchorAverage,
			String worldGeometryTerrainFaceAverage,
			String worldGeometryWallFaceAverage,
			String worldGeometryRoofFaceAverage,
			String worldGeometryGameObjectFaceAverage,
			String worldGeometryWallObjectFaceAverage,
			String worldGeometryOtherFaceAverage,
			String worldDepthFaceAverage,
			String worldDepthTriangleAverage,
			String worldDepthPixelWriteAverage,
			String worldDepthCullAverage,
			String worldMeshVertexAverage,
			String worldMeshIndexAverage,
			String worldMeshTriangleAverage,
			String openGLWorldMeshVertexAverage,
			String openGLWorldMeshIndexAverage,
			String openGLWorldMeshTriangleAverage,
			String openGLWorldMeshDrawTriangleAverage,
			String openGLWorldMeshDrawOccluderTriangleAverage,
			String openGLWorldMeshDrawBatchAverage,
			String openGLWorldMeshDrawCallAverage,
			String openGLWorldChunkAverage,
			String openGLWorldChunkVertexAverage,
			String openGLWorldChunkIndexAverage,
			String openGLWorldChunkTriangleAverage,
			String openGLWorldChunkRequestedAverage,
			String openGLWorldChunkUploadAverage,
			String openGLWorldChunkReuseAverage,
			String openGLWorldChunkEvictAverage,
			String openGLWorldChunkVisibilityAverage,
			String openGLWorldChunkSubmitAverage,
			String openGLWorldChunkDrawAverage,
			String openGLWorldChunkDrawTriangleAverage,
			String openGLWorldChunkDrawTerrainAverage,
			String openGLWorldChunkDrawWallAverage,
			String openGLWorldChunkDrawRoofAverage,
			String openGLWorldChunkDrawGameObjectAverage,
			String openGLWorldChunkDrawWallObjectAverage,
			String openGLWorldChunkDrawOtherAverage,
			String openGLWorldChunkDrawFallbackAverage,
			String openGLWorldChunkDrawSkippedAverage,
			String openGLRemasterShadowReceiverChunkAverage,
			String openGLRemasterShadowReceiverTriangleAverage,
			String openGLRemasterShadowCasterAverage,
			String openGLRemasterShadowWallCasterAverage,
			String openGLRemasterShadowGameObjectCasterAverage,
			String openGLRemasterShadowWallObjectCasterAverage,
			String openGLRemasterShadowOutdoorOnlyCasterAverage,
			String openGLRemasterShadowClippingCandidateAverage,
			String openGLRemasterShadowRoofedReceiverAverage,
			String openGLRemasterShadowOutdoorReceiverAverage,
			String openGLRemasterShadowUnknownReceiverAverage,
			String openGLRemasterShadowRoofedCasterAverage,
			String openGLRemasterShadowOutdoorCasterAverage,
			String openGLRemasterShadowUnknownCasterAverage,
			String openGLRemasterShadowSunlightEligibleCasterAverage,
			String openGLRemasterShadowSunlightSuppressedRoofedCasterAverage,
			String openGLRemasterShadowSunlightSuppressedUnknownCasterAverage,
			String openGLRemasterShadowMaskSizeAverage,
			String openGLRemasterShadowMaskTimingAverageMs,
			String openGLRemasterShadowMaskCacheAverage,
			String openGLRemasterShadowMaskCasterAverage,
			String openGLResidentChunkReplacementRequestedAverage,
			String openGLResidentChunkReplacementActiveAverage,
			String openGLResidentChunkReplacementFallbackAverage,
			String openGLResidentChunkReplacementReason,
			String openGLResidentChunkDrawableTerrainBatchAverage,
			String openGLResidentChunkDrawableWallBatchAverage,
			String openGLResidentChunkDrawableRoofBatchAverage,
			String openGLWorldSpriteAnchorAverage,
			String openGLWorldSpriteMatchedAverage,
			String openGLWorldSpriteDrawnAverage,
			String openGLWorldEntityVisibilityAverage,
			String allocations) {
			this.enabled = enabled;
			this.frameCount = frameCount;
			this.frameAverageMs = frameAverageMs;
			this.frameMaxMs = frameMaxMs;
			this.sceneAverageMs = sceneAverageMs;
			this.sceneModelRotateAverageMs = sceneModelRotateAverageMs;
			this.sceneWorldCullAverageMs = sceneWorldCullAverageMs;
			this.sceneDepthExportAverageMs = sceneDepthExportAverageMs;
			this.sceneLegacyDrawAverageMs = sceneLegacyDrawAverageMs;
			this.sceneMeshExportAverageMs = sceneMeshExportAverageMs;
			this.setImageAverageMs = setImageAverageMs;
			this.sourceCopyAverageMs = sourceCopyAverageMs;
			this.openGLSnapshotAverageMs = openGLSnapshotAverageMs;
			this.openGLUploadAverageMs = openGLUploadAverageMs;
			this.openGLRenderAverageMs = openGLRenderAverageMs;
			this.openGLBaseAverageMs = openGLBaseAverageMs;
			this.openGLWorldAverageMs = openGLWorldAverageMs;
			this.openGLWorldSpriteAverageMs = openGLWorldSpriteAverageMs;
			this.openGLSpriteOverlayAverageMs = openGLSpriteOverlayAverageMs;
			this.openGLDebugOverlayAverageMs = openGLDebugOverlayAverageMs;
			this.openGLSwapAverageMs = openGLSwapAverageMs;
			this.openGLWorldChunkUploadPhaseAverageMs = openGLWorldChunkUploadPhaseAverageMs;
			this.openGLWorldProjectedMeshPhaseAverageMs = openGLWorldProjectedMeshPhaseAverageMs;
			this.openGLWorldChunkDrawPhaseAverageMs = openGLWorldChunkDrawPhaseAverageMs;
			this.openGLFrames = openGLFrames;
			this.openGLDroppedFrames = openGLDroppedFrames;
			this.repaintRequests = repaintRequests;
			this.recentFrameSummary = recentFrameSummary;
			this.recentOpenGLTimingSummary = recentOpenGLTimingSummary;
			this.recentOpenGLPhaseSummary = recentOpenGLPhaseSummary;
			this.recentScenePhaseSummary = recentScenePhaseSummary;
			this.recentClientLoopSummary = recentClientLoopSummary;
			this.spriteOverlayCapturedAverage = spriteOverlayCapturedAverage;
			this.spriteOverlayStaticReplayAverage = spriteOverlayStaticReplayAverage;
			this.spriteOverlayVisibleReplayAverage = spriteOverlayVisibleReplayAverage;
			this.spriteOverlaySkippedOrderedAverage = spriteOverlaySkippedOrderedAverage;
			this.spriteOverlaySkippedInvisibleAverage = spriteOverlaySkippedInvisibleAverage;
			this.spriteOverlaySkippedAtlasFullAverage = spriteOverlaySkippedAtlasFullAverage;
			this.spriteOverlayVisiblePixelsAverage = spriteOverlayVisiblePixelsAverage;
			this.spriteOverlaySceneCommandAverage = spriteOverlaySceneCommandAverage;
			this.spriteOverlayWorldCommandAverage = spriteOverlayWorldCommandAverage;
			this.spriteOverlayUiCommandAverage = spriteOverlayUiCommandAverage;
			this.spriteOverlayUnknownCommandAverage = spriteOverlayUnknownCommandAverage;
			this.spriteOverlayDirectSceneAverage = spriteOverlayDirectSceneAverage;
			this.spriteOverlayDirectWorldAverage = spriteOverlayDirectWorldAverage;
			this.spriteOverlayDirectUiAverage = spriteOverlayDirectUiAverage;
			this.spriteOverlayDirectUnknownAverage = spriteOverlayDirectUnknownAverage;
			this.spriteOverlayVisibleSceneAverage = spriteOverlayVisibleSceneAverage;
			this.spriteOverlayVisibleWorldAverage = spriteOverlayVisibleWorldAverage;
			this.spriteOverlayVisibleUiAverage = spriteOverlayVisibleUiAverage;
			this.spriteOverlayVisibleUnknownAverage = spriteOverlayVisibleUnknownAverage;
			this.legacySceneSpriteRestoreCommandAverage = legacySceneSpriteRestoreCommandAverage;
			this.legacySceneSpriteRestoreFallbackAverage = legacySceneSpriteRestoreFallbackAverage;
			this.legacySceneSpriteRestoreFallbackPixelAverage = legacySceneSpriteRestoreFallbackPixelAverage;
			this.spriteCaptureAttemptAverage = spriteCaptureAttemptAverage;
			this.spriteCaptureAcceptedAverage = spriteCaptureAcceptedAverage;
			this.spriteCaptureReplacedUiAverage = spriteCaptureReplacedUiAverage;
			this.spriteCaptureUiBaseAverage = spriteCaptureUiBaseAverage;
			this.spriteCaptureSkippedAlphaAverage = spriteCaptureSkippedAlphaAverage;
			this.spriteCaptureSkippedBoundsAverage = spriteCaptureSkippedBoundsAverage;
			this.spriteCaptureSkippedSourceAverage = spriteCaptureSkippedSourceAverage;
			this.spriteCaptureSkippedTransformAverage = spriteCaptureSkippedTransformAverage;
			this.spriteCaptureSkippedInterlaceAverage = spriteCaptureSkippedInterlaceAverage;
			this.spriteCaptureSkippedOverflowAverage = spriteCaptureSkippedOverflowAverage;
			this.spriteCaptureSkippedInvalidAverage = spriteCaptureSkippedInvalidAverage;
			this.textCaptureDrawAverage = textCaptureDrawAverage;
			this.textCaptureGlyphAverage = textCaptureGlyphAverage;
			this.textCaptureReplacedUiAverage = textCaptureReplacedUiAverage;
			this.textCaptureSceneAverage = textCaptureSceneAverage;
			this.textCaptureWorldAverage = textCaptureWorldAverage;
			this.textCaptureUiAverage = textCaptureUiAverage;
			this.textCaptureUnknownAverage = textCaptureUnknownAverage;
			this.primitiveCaptureDrawAverage = primitiveCaptureDrawAverage;
			this.primitiveCaptureReplacedUiAverage = primitiveCaptureReplacedUiAverage;
			this.rotatedSpriteDrawAverage = rotatedSpriteDrawAverage;
			this.rotatedSpriteCapturedUiAverage = rotatedSpriteCapturedUiAverage;
			this.circleDrawAverage = circleDrawAverage;
			this.circleCapturedUiAverage = circleCapturedUiAverage;
			this.nativeUiBlockSpriteAverage = nativeUiBlockSpriteAverage;
			this.nativeUiBlockTextAverage = nativeUiBlockTextAverage;
			this.nativeUiBlockPrimitiveAverage = nativeUiBlockPrimitiveAverage;
			this.nativeUiBlockMinimapAverage = nativeUiBlockMinimapAverage;
			this.nativeUiBlockGradientAverage = nativeUiBlockGradientAverage;
			this.nativeUiBlockClearAverage = nativeUiBlockClearAverage;
			this.nativeUiBlockCircleAverage = nativeUiBlockCircleAverage;
			this.nativeUiBlockPixelAverage = nativeUiBlockPixelAverage;
			this.nativeUiBaseEligibleAverage = nativeUiBaseEligibleAverage;
			this.worldGeometryModelAverage = worldGeometryModelAverage;
			this.worldGeometryFaceAverage = worldGeometryFaceAverage;
			this.worldSpriteAnchorAverage = worldSpriteAnchorAverage;
			this.worldGeometryTerrainFaceAverage = worldGeometryTerrainFaceAverage;
			this.worldGeometryWallFaceAverage = worldGeometryWallFaceAverage;
			this.worldGeometryRoofFaceAverage = worldGeometryRoofFaceAverage;
			this.worldGeometryGameObjectFaceAverage = worldGeometryGameObjectFaceAverage;
			this.worldGeometryWallObjectFaceAverage = worldGeometryWallObjectFaceAverage;
			this.worldGeometryOtherFaceAverage = worldGeometryOtherFaceAverage;
			this.worldDepthFaceAverage = worldDepthFaceAverage;
			this.worldDepthTriangleAverage = worldDepthTriangleAverage;
			this.worldDepthPixelWriteAverage = worldDepthPixelWriteAverage;
			this.worldDepthCullAverage = worldDepthCullAverage;
			this.worldMeshVertexAverage = worldMeshVertexAverage;
			this.worldMeshIndexAverage = worldMeshIndexAverage;
			this.worldMeshTriangleAverage = worldMeshTriangleAverage;
			this.openGLWorldMeshVertexAverage = openGLWorldMeshVertexAverage;
			this.openGLWorldMeshIndexAverage = openGLWorldMeshIndexAverage;
			this.openGLWorldMeshTriangleAverage = openGLWorldMeshTriangleAverage;
			this.openGLWorldMeshDrawTriangleAverage = openGLWorldMeshDrawTriangleAverage;
			this.openGLWorldMeshDrawOccluderTriangleAverage = openGLWorldMeshDrawOccluderTriangleAverage;
			this.openGLWorldMeshDrawBatchAverage = openGLWorldMeshDrawBatchAverage;
			this.openGLWorldMeshDrawCallAverage = openGLWorldMeshDrawCallAverage;
			this.openGLWorldChunkAverage = openGLWorldChunkAverage;
			this.openGLWorldChunkVertexAverage = openGLWorldChunkVertexAverage;
			this.openGLWorldChunkIndexAverage = openGLWorldChunkIndexAverage;
			this.openGLWorldChunkTriangleAverage = openGLWorldChunkTriangleAverage;
			this.openGLWorldChunkRequestedAverage = openGLWorldChunkRequestedAverage;
			this.openGLWorldChunkUploadAverage = openGLWorldChunkUploadAverage;
			this.openGLWorldChunkReuseAverage = openGLWorldChunkReuseAverage;
			this.openGLWorldChunkEvictAverage = openGLWorldChunkEvictAverage;
			this.openGLWorldChunkVisibilityAverage = openGLWorldChunkVisibilityAverage;
			this.openGLWorldChunkSubmitAverage = openGLWorldChunkSubmitAverage;
			this.openGLWorldChunkDrawAverage = openGLWorldChunkDrawAverage;
			this.openGLWorldChunkDrawTriangleAverage = openGLWorldChunkDrawTriangleAverage;
			this.openGLWorldChunkDrawTerrainAverage = openGLWorldChunkDrawTerrainAverage;
			this.openGLWorldChunkDrawWallAverage = openGLWorldChunkDrawWallAverage;
			this.openGLWorldChunkDrawRoofAverage = openGLWorldChunkDrawRoofAverage;
			this.openGLWorldChunkDrawGameObjectAverage = openGLWorldChunkDrawGameObjectAverage;
			this.openGLWorldChunkDrawWallObjectAverage = openGLWorldChunkDrawWallObjectAverage;
			this.openGLWorldChunkDrawOtherAverage = openGLWorldChunkDrawOtherAverage;
			this.openGLWorldChunkDrawFallbackAverage = openGLWorldChunkDrawFallbackAverage;
			this.openGLWorldChunkDrawSkippedAverage = openGLWorldChunkDrawSkippedAverage;
			this.openGLRemasterShadowReceiverChunkAverage = openGLRemasterShadowReceiverChunkAverage;
			this.openGLRemasterShadowReceiverTriangleAverage = openGLRemasterShadowReceiverTriangleAverage;
			this.openGLRemasterShadowCasterAverage = openGLRemasterShadowCasterAverage;
			this.openGLRemasterShadowWallCasterAverage = openGLRemasterShadowWallCasterAverage;
			this.openGLRemasterShadowGameObjectCasterAverage = openGLRemasterShadowGameObjectCasterAverage;
			this.openGLRemasterShadowWallObjectCasterAverage = openGLRemasterShadowWallObjectCasterAverage;
			this.openGLRemasterShadowOutdoorOnlyCasterAverage = openGLRemasterShadowOutdoorOnlyCasterAverage;
			this.openGLRemasterShadowClippingCandidateAverage = openGLRemasterShadowClippingCandidateAverage;
			this.openGLRemasterShadowRoofedReceiverAverage = openGLRemasterShadowRoofedReceiverAverage;
			this.openGLRemasterShadowOutdoorReceiverAverage = openGLRemasterShadowOutdoorReceiverAverage;
			this.openGLRemasterShadowUnknownReceiverAverage = openGLRemasterShadowUnknownReceiverAverage;
			this.openGLRemasterShadowRoofedCasterAverage = openGLRemasterShadowRoofedCasterAverage;
			this.openGLRemasterShadowOutdoorCasterAverage = openGLRemasterShadowOutdoorCasterAverage;
			this.openGLRemasterShadowUnknownCasterAverage = openGLRemasterShadowUnknownCasterAverage;
			this.openGLRemasterShadowSunlightEligibleCasterAverage = openGLRemasterShadowSunlightEligibleCasterAverage;
			this.openGLRemasterShadowSunlightSuppressedRoofedCasterAverage =
				openGLRemasterShadowSunlightSuppressedRoofedCasterAverage;
			this.openGLRemasterShadowSunlightSuppressedUnknownCasterAverage =
				openGLRemasterShadowSunlightSuppressedUnknownCasterAverage;
			this.openGLRemasterShadowMaskSizeAverage = openGLRemasterShadowMaskSizeAverage;
			this.openGLRemasterShadowMaskTimingAverageMs = openGLRemasterShadowMaskTimingAverageMs;
			this.openGLRemasterShadowMaskCacheAverage = openGLRemasterShadowMaskCacheAverage;
			this.openGLRemasterShadowMaskCasterAverage = openGLRemasterShadowMaskCasterAverage;
			this.openGLResidentChunkReplacementRequestedAverage = openGLResidentChunkReplacementRequestedAverage;
			this.openGLResidentChunkReplacementActiveAverage = openGLResidentChunkReplacementActiveAverage;
			this.openGLResidentChunkReplacementFallbackAverage = openGLResidentChunkReplacementFallbackAverage;
			this.openGLResidentChunkReplacementReason = openGLResidentChunkReplacementReason;
			this.openGLResidentChunkDrawableTerrainBatchAverage = openGLResidentChunkDrawableTerrainBatchAverage;
			this.openGLResidentChunkDrawableWallBatchAverage = openGLResidentChunkDrawableWallBatchAverage;
			this.openGLResidentChunkDrawableRoofBatchAverage = openGLResidentChunkDrawableRoofBatchAverage;
			this.openGLWorldSpriteAnchorAverage = openGLWorldSpriteAnchorAverage;
			this.openGLWorldSpriteMatchedAverage = openGLWorldSpriteMatchedAverage;
			this.openGLWorldSpriteDrawnAverage = openGLWorldSpriteDrawnAverage;
			this.openGLWorldEntityVisibilityAverage = openGLWorldEntityVisibilityAverage;
			this.allocations = allocations;
		}
	}
}
