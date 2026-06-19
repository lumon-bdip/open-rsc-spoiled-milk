package orsc;

import orsc.graphics.Renderer2DFrame;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

final class RenderTelemetry {
	private static final String ENABLED_PROPERTY = "spoiledmilk.rendererTelemetry";
	private static final String ENABLED_ENV = "SPOILED_MILK_RENDERER_TELEMETRY";
	private static final String REPORT_INTERVAL_PROPERTY = "spoiledmilk.rendererTelemetryInterval";
	private static final String SLOW_FRAME_MS_PROPERTY = "spoiledmilk.rendererSlowFrameMs";

	private static final boolean ENABLED = readBoolean(ENABLED_PROPERTY, ENABLED_ENV);
	private static final int REPORT_INTERVAL = Math.max(1, readInt(REPORT_INTERVAL_PROPERTY, 300));
	private static final long SLOW_FRAME_NANOS = Math.max(1L, readInt(SLOW_FRAME_MS_PROPERTY, 35)) * 1_000_000L;
	private static final long SLOW_REPORT_THROTTLE_NANOS = 1_000_000_000L;

	private static final StageStats frameStats = new StageStats();
	private static final StageStats sceneRenderStats = new StageStats();
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
	private static final CounterStats worldGeometryModelStats = new CounterStats();
	private static final CounterStats worldGeometryFaceStats = new CounterStats();
	private static final CounterStats worldSpriteAnchorStats = new CounterStats();
	private static final CounterStats worldGeometryTerrainFaceStats = new CounterStats();
	private static final CounterStats worldGeometryWallFaceStats = new CounterStats();
	private static final CounterStats worldGeometryRoofFaceStats = new CounterStats();
	private static final CounterStats worldGeometryGameObjectFaceStats = new CounterStats();
	private static final CounterStats worldGeometryWallObjectFaceStats = new CounterStats();
	private static final CounterStats worldGeometryOtherFaceStats = new CounterStats();
	private static final CounterStats worldDepthFaceStats = new CounterStats();
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
	private static final CounterStats openGLWorldTextureReferencedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureCachedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureUploadedStats = new CounterStats();
	private static final CounterStats openGLWorldTextureMissingStats = new CounterStats();
	private static final CounterStats openGLWorldTextureAtlasStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteAnchorStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteMatchedStats = new CounterStats();
	private static final CounterStats openGLWorldSpriteDrawnStats = new CounterStats();

	private static final Map<String, AllocationStats> allocationStats = new LinkedHashMap<>();

	private static long repaintRequests;
	private static long paintImmediateRequests;
	private static long nearestScalePaints;
	private static long interpolationScalePaints;
	private static long gpuPresenterPaints;
	private static long openGLFrames;
	private static long openGLDroppedFrames;
	private static long lastReportNanos;

	private RenderTelemetry() {
	}

	static boolean isEnabled() {
		return ENABLED;
	}

	static long now() {
		return ENABLED ? System.nanoTime() : 0L;
	}

	static long elapsedSince(long startNanos) {
		return ENABLED && startNanos != 0L ? System.nanoTime() - startNanos : 0L;
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
		if (!ENABLED) {
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
			if (frameStats.count % REPORT_INTERVAL == 0
				|| (slowFrame && now - lastReportNanos >= SLOW_REPORT_THROTTLE_NANOS)) {
				lastReportNanos = now;
				printReport(slowFrame, totalNanos, sourceWidth, sourceHeight, scalar, scalingType, framePath);
			}
		}
	}

	static void recordSceneRender(long nanos) {
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			sceneRenderStats.record(nanos);
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
		int depthFaces,
		int depthTriangles,
		int depthPixelWrites,
		int meshVertices,
		int meshIndices,
		int meshTriangles,
		int meshTexturedTriangles,
		int meshFlatColorTriangles,
		int meshTransparentTriangles,
		int meshSkippedTriangles) {
		if (!ENABLED) {
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
			worldDepthFaceStats.record(depthFaces);
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
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldMeshVertexStats.record(vertices);
			openGLWorldMeshIndexStats.record(indices);
			openGLWorldMeshTriangleStats.record(triangles);
		}
	}

	static void recordOpenGLWorldTextureFrame(
		int referencedTextures,
		int cachedTextures,
		int uploadedTextures,
		int missingTextures,
		int atlases) {
		if (!ENABLED) {
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
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLWorldSpriteAnchorStats.record(anchors);
			openGLWorldSpriteMatchedStats.record(matched);
			openGLWorldSpriteDrawnStats.record(drawn);
		}
	}

	static void recordSetGameImage(
		long totalNanos,
		long sourceCopyNanos,
		long paintImmediateNanos,
		boolean repaintRequested,
		boolean paintImmediateRequested) {
		if (!ENABLED) {
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
		if (!ENABLED) {
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
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			smoothScaleStats.record(nanos);
		}
	}

	static void recordGpuPresenter(long nanos) {
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			gpuPresenterStats.record(nanos);
			gpuPresenterPaints++;
		}
	}

	static void recordOpenGLSnapshot(long nanos) {
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLSnapshotStats.record(nanos);
		}
	}

	static void recordOpenGLFrame(long uploadNanos, long renderNanos) {
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLUploadStats.record(uploadNanos);
			openGLRenderStats.record(renderNanos);
			openGLFrames++;
		}
	}

	static void recordOpenGLDroppedFrame() {
		if (!ENABLED) {
			return;
		}

		synchronized (RenderTelemetry.class) {
			openGLDroppedFrames++;
		}
	}

	static void recordSpriteCaptureStats(Renderer2DFrame.CaptureStats captureStats) {
		if (!ENABLED || captureStats == null) {
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
		if (!ENABLED) {
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

	static void recordImageAllocation(String label, int width, int height, int imageType) {
		if (!ENABLED) {
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
				ENABLED,
				frameStats.count,
				formatMillis(frameStats.average()),
				formatMillis(frameStats.max),
				formatMillis(sceneRenderStats.average()),
				formatMillis(setGameImageStats.average()),
				formatMillis(sourceCopyStats.average()),
				formatMillis(openGLSnapshotStats.average()),
				formatMillis(openGLUploadStats.average()),
				formatMillis(openGLRenderStats.average()),
				openGLFrames,
				openGLDroppedFrames,
				repaintRequests,
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
				formatCount(worldMeshVertexStats.average()),
				formatCount(worldMeshIndexStats.average()),
				formatCount(worldMeshTriangleStats.average()),
				formatCount(openGLWorldMeshVertexStats.average()),
				formatCount(openGLWorldMeshIndexStats.average()),
				formatCount(openGLWorldMeshTriangleStats.average()),
				formatCount(openGLWorldSpriteAnchorStats.average()),
				formatCount(openGLWorldSpriteMatchedStats.average()),
				formatCount(openGLWorldSpriteDrawnStats.average()),
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
				+ " mesh v/i/t=" + formatCount(worldMeshVertexStats.average())
				+ "/" + formatCount(worldMeshIndexStats.average())
				+ "/" + formatCount(worldMeshTriangleStats.average())
				+ " glmesh v/i/t=" + formatCount(openGLWorldMeshVertexStats.average())
				+ "/" + formatCount(openGLWorldMeshIndexStats.average())
				+ "/" + formatCount(openGLWorldMeshTriangleStats.average()));

		System.out.println(
			"[renderer-v2 telemetry] world mesh materials avg: textured="
				+ formatCount(worldMeshTexturedTriangleStats.average())
				+ " flat=" + formatCount(worldMeshFlatColorTriangleStats.average())
				+ " transparent=" + formatCount(worldMeshTransparentTriangleStats.average())
				+ " skipped=" + formatCount(worldMeshSkippedTriangleStats.average()));

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
				+ " drawn=" + formatCount(openGLWorldSpriteDrawnStats.average()));

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

		private void record(long nanos) {
			count++;
			total += nanos;
			if (nanos > max) {
				max = nanos;
			}
		}

		private long average() {
			return count == 0L ? 0L : total / count;
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

		private void record(int value) {
			count++;
			total += value;
			if (value > max) {
				max = value;
			}
		}

		private double average() {
			return count == 0L ? 0.0 : total / (double) count;
		}
	}

	static final class Snapshot {
		final boolean enabled;
		final long frameCount;
		final String frameAverageMs;
		final String frameMaxMs;
		final String sceneAverageMs;
		final String setImageAverageMs;
		final String sourceCopyAverageMs;
		final String openGLSnapshotAverageMs;
		final String openGLUploadAverageMs;
		final String openGLRenderAverageMs;
		final long openGLFrames;
		final long openGLDroppedFrames;
		final long repaintRequests;
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
		final String worldMeshVertexAverage;
		final String worldMeshIndexAverage;
		final String worldMeshTriangleAverage;
		final String openGLWorldMeshVertexAverage;
		final String openGLWorldMeshIndexAverage;
		final String openGLWorldMeshTriangleAverage;
		final String openGLWorldSpriteAnchorAverage;
		final String openGLWorldSpriteMatchedAverage;
		final String openGLWorldSpriteDrawnAverage;
		final String allocations;

		private Snapshot(
			boolean enabled,
			long frameCount,
			String frameAverageMs,
			String frameMaxMs,
			String sceneAverageMs,
			String setImageAverageMs,
			String sourceCopyAverageMs,
			String openGLSnapshotAverageMs,
			String openGLUploadAverageMs,
			String openGLRenderAverageMs,
			long openGLFrames,
			long openGLDroppedFrames,
			long repaintRequests,
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
			String worldMeshVertexAverage,
			String worldMeshIndexAverage,
			String worldMeshTriangleAverage,
			String openGLWorldMeshVertexAverage,
			String openGLWorldMeshIndexAverage,
			String openGLWorldMeshTriangleAverage,
			String openGLWorldSpriteAnchorAverage,
			String openGLWorldSpriteMatchedAverage,
			String openGLWorldSpriteDrawnAverage,
			String allocations) {
			this.enabled = enabled;
			this.frameCount = frameCount;
			this.frameAverageMs = frameAverageMs;
			this.frameMaxMs = frameMaxMs;
			this.sceneAverageMs = sceneAverageMs;
			this.setImageAverageMs = setImageAverageMs;
			this.sourceCopyAverageMs = sourceCopyAverageMs;
			this.openGLSnapshotAverageMs = openGLSnapshotAverageMs;
			this.openGLUploadAverageMs = openGLUploadAverageMs;
			this.openGLRenderAverageMs = openGLRenderAverageMs;
			this.openGLFrames = openGLFrames;
			this.openGLDroppedFrames = openGLDroppedFrames;
			this.repaintRequests = repaintRequests;
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
			this.worldMeshVertexAverage = worldMeshVertexAverage;
			this.worldMeshIndexAverage = worldMeshIndexAverage;
			this.worldMeshTriangleAverage = worldMeshTriangleAverage;
			this.openGLWorldMeshVertexAverage = openGLWorldMeshVertexAverage;
			this.openGLWorldMeshIndexAverage = openGLWorldMeshIndexAverage;
			this.openGLWorldMeshTriangleAverage = openGLWorldMeshTriangleAverage;
			this.openGLWorldSpriteAnchorAverage = openGLWorldSpriteAnchorAverage;
			this.openGLWorldSpriteMatchedAverage = openGLWorldSpriteMatchedAverage;
			this.openGLWorldSpriteDrawnAverage = openGLWorldSpriteDrawnAverage;
			this.allocations = allocations;
		}
	}
}
