#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FRAME_CAPTURE = ROOT / "PC_Client/src/orsc/OpenGLFrameCapture.java"
CAPTURE_SUBSYSTEM = (
    ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java",
    ROOT / "PC_Client/src/orsc/OpenGLInputBridge.java",
    ROOT / "PC_Client/src/orsc/OpenGLCompositeSceneBuilder.java",
    ROOT / "PC_Client/src/orsc/OpenGLWorldMeshRenderer.java",
    ROOT / "PC_Client/src/orsc/OpenGLWorldSpriteDrawController.java",
)
DEPTH_FRAME = ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DDepthFrame.java"
PLAN = ROOT / "docs/myworld/in-progress-work-plans/renderer-v2-plan.md"


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"{label} missing expected snippet: {needle!r}")


def forbid(text: str, needle: str, label: str) -> None:
    if needle in text:
        raise AssertionError(f"{label} still contains forbidden snippet: {needle!r}")


def main() -> None:
    frame_capture = FRAME_CAPTURE.read_text(encoding="utf-8")
    presenter = "\n".join(path.read_text(encoding="utf-8") for path in CAPTURE_SUBSYSTEM)
    presenter += "\n" + frame_capture
    depth_frame = DEPTH_FRAME.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(
        presenter,
        'FRAME_CAPTURE_ENV = "SPOILED_MILK_OPENGL_FRAME_CAPTURE"',
        "frame capture hotkey gate",
    )
    require(
        presenter,
        "readBoolean(FRAME_CAPTURE_PROPERTY, FRAME_CAPTURE_ENV, false)",
        "frame capture hotkey should default off for release",
    )
    require(
        presenter,
        'FRAME_CAPTURE_DIR_ENV = "SPOILED_MILK_OPENGL_FRAME_CAPTURE_DIR"',
        "frame capture output directory override",
    )
    require(
        presenter,
        "binding.awtKeyCode == KeyEvent.VK_F9\n\t\t\t&& (mods & gl.GLFW_MOD_CONTROL) != 0",
        "Ctrl+F9 burst capture trigger",
    )
    require(
        presenter,
        "FRAME_CAPTURE_BURST_FRAMES = 12",
        "frame capture burst length",
    )
    require(
        presenter,
        "captureLayer(activeFrameCapture, \"04-scene-restore\");",
        "scene restore layer capture",
    )
    require(
        presenter,
        "captureLayer(activeFrameCapture, \"04b-entity-sprites\");",
        "direct entity sprite layer capture",
    )
    require(
        presenter,
        "captureLayer(activeFrameCapture, \"04c-ordered-static-overlays\");",
        "ordered static overlay layer capture",
    )
    require(
        presenter,
        "capture.writeLayer(\"07-final\", readCurrentViewportImage())",
        "final framebuffer layer capture",
    )
    require(
        frame_capture,
        "writeImage(\"00-legacy-source.png\", imageFromFrame(frame));",
        "legacy software source capture",
    )
    require(
        frame_capture,
        'new PrintWriter(new File(directory, "sprite-commands.tsv"))',
        "sprite command metadata capture",
    )
    require(
        frame_capture,
        'new PrintWriter(new File(directory, "world-sprite-commands.tsv"))',
        "typed world sprite command metadata capture",
    )
    require(
        frame_capture,
        'new PrintWriter(new File(directory, "scene-commands.tsv"))',
        "composite scene command metadata capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "static-range-candidates.tsv"))',
        "static range candidate metadata capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "front-occluder-candidates.tsv"))',
        "front occluder candidate metadata capture",
    )
    require(
        presenter,
        "frontOccluderFaceKeys",
        "front occluder exact face ownership",
    )
    require(
        presenter,
        "frontOccluderFaces",
        "front occluder scene capture count",
    )
    forbid(
        presenter,
        "Kind.FRONT_OCCLUDER_RANGE",
        "retired front occluder scene command",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "world-faces.tsv"))',
        "static world face metadata capture",
    )
    require(
        presenter,
        'new File(directory, "static-world-commands.tsv")',
        "face-owned static world command capture",
    )
    require(
        presenter,
        'new File(directory, "static-world-face-ownership.tsv")',
        "normalized static world face ownership capture",
    )
    require(
        presenter,
        'new File(directory, "static-world-material-triangles.tsv")',
        "triangle-level static world material pass capture",
    )
    require(
        presenter,
        'new File(directory, "resident-material-families.tsv")',
        "resident material-family capture",
    )
    require(presenter, '"\\tglowEmitterCount\\ttriangleCount"', "resident glow evidence")
    require(
        presenter,
        'new File(directory, "world-sprite-batch-stats.tsv")',
        "world sprite atlas batch capture",
    )
    require(
        presenter,
        "buildOpenGLCompositeStaticWorldMaterialTriangles",
        "capture-only static world material classifier",
    )
    require(
        presenter,
        "buildOpenGLCompositeStaticWorldCommands",
        "capture-only static world ownership command builder",
    )
    require(
        presenter,
        "worldMeshRenderer.uploadAndMaybeDrawOwnedBaseWorld(",
        "visible base world draw consumes the owned mesh boundary",
    )
    require(
        presenter,
        "face.getRenderCameraX()",
        "world face render camera coordinate capture",
    )
    require(
        presenter,
        "face.getRenderTextureU()",
        "world face texture coordinate capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "sprite-anchors.tsv"))',
        "sprite anchor metadata capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "sprite-submissions.tsv"))',
        "sprite submission metadata capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "character-sprites.tsv"))',
        "character sprite metadata capture",
    )
    require(
        presenter,
        "bodySourceVisiblePixels",
        "character sprite body command pixel diagnostics",
    )
    require(
        presenter,
        "restoreDepthOccludedPixels",
        "character sprite restore depth diagnostics",
    )
    require(
        presenter,
        "characterDiagnosis(character, bodyStats, restoreStats)",
        "character sprite joined diagnosis",
    )
    require(
        presenter,
        "restore-depth-out-of-bounds",
        "character sprite out-of-bounds diagnosis",
    )
    require(
        presenter,
        "renderer3DFrame.getSpriteSubmissionCount()",
        "sprite submission metadata count",
    )
    require(
        presenter,
        "renderer3DFrame.getCharacterSpriteCount()",
        "character sprite metadata count",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "entity-restore-stats.tsv"))',
        "entity restore metadata capture",
    )
    require(
        presenter,
        'new PrintWriter(new File(directory, "entity-depth-evaluations.tsv"))',
        "entity depth evaluation metadata capture",
    )
    require(
        presenter,
        "terrainOccludedPixels",
        "entity depth evaluation occluder-kind capture",
    )
    require(
        presenter,
        "dominantOccluderFaceId",
        "entity depth evaluation dominant occluder face capture",
    )
    require(
        presenter,
        "anchorMatchMode",
        "entity depth evaluation anchor match mode capture",
    )
    require(
        presenter,
        "anchorDeltaWidth",
        "entity depth evaluation anchor geometry delta capture",
    )
    require(
        presenter,
        "legacyEntitySpriteDepthEvaluations.add(new LegacyEntitySpriteDepthEvaluation(",
        "per-command entity depth evaluation capture",
    )
    require(
        presenter,
        "gl.glReadPixels(",
        "OpenGL framebuffer readback",
    )
    require(
        presenter,
        "long baseNanos = RenderTelemetry.elapsedSince(baseStart);\n\t\tcaptureLayer(frameCapture, \"01-base\");",
        "frame capture readback must not inflate base phase telemetry",
    )
    require(
        presenter,
        "long worldNanos = RenderTelemetry.elapsedSince(worldStart);\n\t\tcaptureLayer(frameCapture, \"02-opengl-world\");",
        "frame capture readback must not inflate world phase telemetry",
    )
    require(
        presenter,
        "private long phaseCaptureNanos;",
        "frame capture phase-time accumulator",
    )
    require(
        presenter,
        "long spriteOverlayNanos = Math.max(0L, RenderTelemetry.elapsedSince(spriteOverlayStart) - phaseCaptureNanos);",
        "internal sprite capture readbacks must not inflate sprite-overlay telemetry",
    )
    require(
        presenter,
        "phaseCaptureNanos += captureNanos;\n\t\t\tactiveFrameCaptureLayerNanos += captureNanos;",
        "captureLayer should report phase-local readback time",
    )
    require(
        presenter,
        "long renderNanos = uploadNanos\n\t\t\t+ baseNanos",
        "OpenGL render telemetry should use capture-free phase totals",
    )
    forbid(
        presenter,
        "RenderTelemetry.recordOpenGLFrame(uploadNanos, RenderTelemetry.elapsedSince(renderStart));",
        "OpenGL render telemetry should exclude frame capture readbacks",
    )
    require(
        depth_frame,
        "public void copyKindColorTo(int[] destination)",
        "depth kind diagnostic",
    )
    require(
        depth_frame,
        "public void copyEntityOccluderMaskTo(int[] destination)",
        "entity occluder mask diagnostic",
    )
    require(
        depth_frame,
        "private static boolean isEntitySpriteOccluder(Renderer3DModelKind modelKind)",
        "entity sprite depth occluder helper",
    )
    require(
        depth_frame,
        "public Renderer3DModelKind getEntityOccluderKindAfterSprite(",
        "entity sprite depth occluder attribution helper",
    )
    require(
        depth_frame,
        "public int getFaceIdAt(int x, int y)",
        "depth frame face id attribution helper",
    )
    require(
        depth_frame,
        "modelKind == Renderer3DModelKind.GAME_OBJECT",
        "entity sprite depth occluders include game objects",
    )
    require(
        depth_frame,
        "modelKind == Renderer3DModelKind.TERRAIN",
        "entity sprite depth occluders include terrain",
    )
    require(
        depth_frame,
        "modelKind == Renderer3DModelKind.ROOF",
        "entity sprite depth occluders include roofs",
    )
    require(
        frame_capture,
        'writer.println("roofVisibility=" + renderer3DFrame.getRoofVisibility().name());',
        "frame capture records named roof visibility state",
    )
    depth_miss_start = presenter.find(
        "if (visiblePixelCount == 0) {\n\t\t\trecordLegacyEntitySpriteDepthFallbackMiss("
    )
    if depth_miss_start < 0:
        raise AssertionError("depth-masked entity sprite zero-visible branch missing")
    depth_miss_end = presenter.find("\t\t}\n\t\toverlayPixels.flip();", depth_miss_start)
    if depth_miss_end < 0:
        raise AssertionError("depth-masked entity sprite zero-visible branch terminator missing")
    if "return null;" in presenter[depth_miss_start:depth_miss_end]:
        raise AssertionError("fully occluded entity sprites must not fall back to direct sprite drawing")
    require(
        plan,
        "Add a runtime-gated renderer-v2 frame capture dump on `Ctrl+F9`",
        "plan tracks release-gated frame capture diagnostic",
    )
    require(
        plan,
        "Start replacing software-visible scene/entity sprite recovery with",
        "plan tracks entity sprite pass rewrite",
    )

    print("PASS: renderer-v2 frame capture diagnostic is wired")


if __name__ == "__main__":
    main()
