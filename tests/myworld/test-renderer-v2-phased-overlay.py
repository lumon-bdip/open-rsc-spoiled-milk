#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GRAPHICS = ROOT / "Client_Base/src/orsc/graphics/two/GraphicsController.java"
RENDERER_FRAME = ROOT / "Client_Base/src/orsc/graphics/Renderer2DFrame.java"
SETTINGS = ROOT / "Client_Base/src/orsc/graphics/Renderer2DSettings.java"
TELEMETRY = ROOT / "Client_Base/src/orsc/RenderTelemetry.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
SCALED_WINDOW = ROOT / "PC_Client/src/orsc/ScaledWindow.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
PLAN = ROOT / "docs/myworld/renderer-v2-plan.md"


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"{label} missing expected snippet: {needle!r}")


def main() -> None:
    graphics = GRAPHICS.read_text(encoding="utf-8")
    frame = RENDERER_FRAME.read_text(encoding="utf-8")
    settings = SETTINGS.read_text(encoding="utf-8")
    telemetry = TELEMETRY.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    presenter = PRESENTER.read_text(encoding="utf-8")
    scaled_window = SCALED_WINDOW.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(
        settings,
        'PHASE_AWARE_AFTER_FRAME("phased")',
        "phase-aware overlay mode",
    )
    require(
        settings,
        'NATIVE_UI_AFTER_FRAME("native-ui")',
        "native UI overlay mode",
    )
    require(
        settings,
        "public static boolean canReplayPhaseAwareSpritesAfterFrame()",
        "phase-aware overlay accessor",
    )
    require(
        settings,
        "public static boolean canReplaceUiSpritesWithOpenGL()",
        "native UI replacement accessor",
    )
    require(
        settings,
        "SPOILED_MILK_OPENGL_NATIVE_UI_REPLACE",
        "native UI destructive replacement flag",
    )
    require(
        settings,
        "&& OPENGL_NATIVE_UI_REPLACE_ENABLED",
        "native UI replacement requires explicit destructive opt-in",
    )
    require(
        settings,
        "public static boolean canPresentUiBaseFrame()",
        "pre-UI base frame accessor",
    )
    require(
        settings,
        "OPENGL_WORLD_UI_REPLAY_ENABLED",
        "OpenGL world UI replay flag",
    )
    require(
        settings,
        "public static boolean canReplayUiOverOpenGLWorld()",
        "OpenGL world UI replay accessor",
    )
    require(
        presenter,
        "boolean phaseAwareReplay = Renderer2DSettings.canReplayPhaseAwareSpritesAfterFrame();",
        "phase-aware replay branch",
    )
    require(
        presenter,
        "private void drawOpenGLWorldCompositeOverlay(",
        "OpenGL world composite replay branch",
    )
    require(
        presenter,
        "boolean replayOpenGLWorldUi = shouldUseOpenGLWorldReplacementComposite(frame);",
        "OpenGL world UI replay requires active replacement world frame",
    )
    require(
        presenter,
        "if (!worldReplacementComposite) {\n\t\t\trunOpenGLPass(() -> drawWorldSprites(frame));\n\t\t}",
        "OpenGL replacement composite owns scene sprite restore without duplicate diagnostic world sprites",
    )
    require(
        presenter,
        "commands[spriteIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY",
        "OpenGL world UI replay filters sprite phase",
    )
    require(
        presenter,
        "&& !isOpenGLCompositeDirectSpriteCommand(commands[spriteIndex])",
        "OpenGL world composite direct-replays overlay sprite commands",
    )
    require(
        presenter,
        "private boolean isOpenGLCompositeDirectSpriteCommand(Renderer2DFrame.SpriteCommand command)",
        "OpenGL world composite direct-replays translucent scene sprites",
    )
    require(
        presenter,
        "command.getPhase() == Renderer2DFrame.Phase.SCENE\n\t\t\t\t&& command.getAlpha() < Renderer2DFrame.SpriteCommand.FULL_ALPHA",
        "OpenGL world composite treats translucent scene sprites as overlays",
    )
    require(
        presenter,
        "buildOpenGLCompositeDirectOverlayCoverageMask(\n\t\t\t\tcommands,\n\t\t\t\ttextCommands,\n\t\t\t\tprimitiveCommands,",
        "OpenGL world composite masks scene restore under direct sprite overlays",
    )
    require(
        presenter,
        "int commandVisiblePixels = drawVisibleSpriteCommand(frame, command, directSpriteMask);",
        "OpenGL world composite clips scene restore using direct sprite mask",
    )
    require(
        presenter,
        "&& !isSceneRestoreClipped(clippedSceneRestoreMask, frame.sourceWidth, screenX, screenY))",
        "OpenGL scene sprite restore leaves direct overlay pixels uncontested",
    )
    require(
        presenter,
        "private boolean[] markOverlayRectangle(",
        "OpenGL world composite masks primitive and text overlay rectangles",
    )
    require(
        mudclient,
        "private void drawHitSplatBox(int x, int y, int amount, int color) {\n\t\tthis.getSurface().setRenderer2DPhase(Renderer2DFrame.Phase.WORLD_OVERLAY);",
        "OpenGL world overlay phase for hit splat primitive/text replay",
    )
    require(
        mudclient,
        "if (S_SHOW_FLOATING_NAMETAGS) {\n\t\t\t\t\t\tthis.getSurface().setRenderer2DPhase(Renderer2DFrame.Phase.WORLD_OVERLAY);",
        "OpenGL world overlay phase for floating nametag text replay",
    )
    require(
        mudclient,
        "this.getSurface().setRenderer2DPhase(Renderer2DFrame.Phase.WORLD_OVERLAY);\n\t\ttry {\n\t\t\tif (isDragonBreathCombatEffect(character.combatEffectType))",
        "OpenGL world overlay phase for attached combat effect replay",
    )
    require(
        presenter,
        "drawVisibleSpriteCommand(frame, command)",
        "OpenGL world restores software-visible scene sprites",
    )
    require(
        presenter,
        "VISIBLE_SPRITE_RESTORE_COLOR_TOLERANCE",
        "OpenGL world scene sprite restore tolerates dark pixel drift",
    )
    require(
        presenter,
        "private boolean matchesRestoredSpriteColor(int finalRgb, int replayRgb)",
        "OpenGL world scene sprite restore color matcher",
    )
    require(
        presenter,
        "private boolean isOpenGLWorldOverlayPhase(Renderer2DFrame.Phase phase)",
        "OpenGL world overlay phase helper",
    )
    require(
        presenter,
        "private void prepareOverlayTexturedReplayState() throws Exception",
        "OpenGL overlay textured replay state reset",
    )
    require(
        presenter,
        "private void prepareOverlaySolidReplayState() throws Exception",
        "OpenGL overlay solid replay state reset",
    )
    require(
        presenter,
        "gl.glDisable(gl.GL_ALPHA_TEST);\n\t\tgl.glEnable(gl.GL_BLEND);\n\t\tgl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);",
        "OpenGL overlay replay clears inherited alpha-test state",
    )
    require(
        presenter,
        "prepareOverlayTexturedReplayState();\n\t\tOpenGLTextureRegion region = uploadSpriteTexture(command.getSprite(), command.getTransform());",
        "OpenGL direct sprite replay owns transparent overlay state",
    )
    require(
        presenter,
        "prepareOverlayTexturedReplayState();\n\t\tfor (Renderer2DFrame.TextCommand.GlyphCommand glyph : command.getGlyphs())",
        "OpenGL text replay owns transparent overlay state",
    )
    require(
        presenter,
        "command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY && !command.requiresOrderedReplay()",
        "direct UI-phase replay guard",
    )
    require(
        presenter,
        "int commandVisiblePixels = drawVisibleSpriteCommand(frame, command);",
        "scene/world visibility fallback",
    )
    require(
        presenter,
        "logCompositeSpriteCommand(\"direct-overlay\", command, command.getWidth() * command.getHeight());\n\t\t\t\tdrawOpenGLCompositeDirectSpriteCommand(command);",
        "OpenGL replacement composite replays direct world sprite overlays with baked alpha textures",
    )
    require(
        presenter,
        "private DynamicTextureData buildDirectSpriteTexture(Renderer2DFrame.SpriteCommand command)",
        "OpenGL replacement composite direct sprite texture builder",
    )
    require(
        presenter,
        "Scene restoration has already been issued. Reuse its proven atlas",
        "overlay atlas capacity is recycled after scene restoration",
    )
    require(
        presenter,
        "OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);",
        "direct overlays use the proven dynamic sprite atlas",
    )
    require(
        presenter,
        "rgba = (replayRgb << 8) | alpha;",
        "OpenGL replacement composite bakes command alpha into direct sprite pixels",
    )
    require(
        presenter,
        "int[] directReplayedByPhase = new int[Renderer2DFrame.Phase.values().length];",
        "direct replay phase counters",
    )
    require(
        presenter,
        "RenderTelemetry.recordSpriteCaptureStats(renderer2DFrame.getCaptureStats());",
        "sprite capture telemetry hook",
    )
    require(
        presenter,
        "Renderer2DSettings.isNativeUiOverlayMode()\n\t\t\t&& !Renderer2DSettings.canReplaceUiSpritesWithOpenGL()",
        "native UI capture-only safety gate",
    )
    require(
        graphics,
        "renderer2DCaptureSkippedAlpha++;",
        "alpha capture rejection counter",
    )
    require(
        graphics,
        "private boolean canCaptureAlphaRenderer2DSprite(int alpha)",
        "native UI alpha capture gate",
    )
    require(
        graphics,
        "Renderer2DSettings.canReplayUiOverOpenGLWorld()",
        "OpenGL world UI replay capture gate",
    )
    require(
        graphics,
        "private boolean canCaptureRenderer2DNativeUiCommand()",
        "native UI command capture-or-replay helper",
    )
    require(
        graphics,
        "private boolean canCaptureRenderer2DOpenGLWorldOverlayReplay()",
        "OpenGL world overlay command capture helper",
    )
    require(
        graphics,
        "private void addRenderer2DTextGlyphCommand(",
        "OpenGL text capture keeps valid glyphs when later glyphs are clipped",
    )
    require(
        graphics,
        "boolean renderer2DNativeTextReplayable = canCaptureRenderer2DNativeUiCommand();",
        "OpenGL world UI text replay capture",
    )
    require(
        graphics,
        "&& shouldReplaceRenderer2DText(recordRenderer2DTextCommand(renderer2DNativeGlyphs)))",
        "OpenGL text replacement accepts partial clipped glyph commands",
    )
    require(
        graphics,
        "createRenderer2DTextGlyphCommand(antiAliased, fontData, x, color, indexAddr, y);",
        "OpenGL text replay captures glyphs individually",
    )
    require(
        graphics,
        "boolean replacedByRenderer2D = shouldReplaceRenderer2DUiSprite(recordRenderer2DSprite(\n"
        "\t\t\t\t\tsprite,\n"
        "\t\t\t\t\tvar3,\n"
        "\t\t\t\t\tvar5,\n"
        "\t\t\t\t\tvar9,\n"
        "\t\t\t\t\tvar8,",
        "legacy alpha sprite capture",
    )
    require(
        graphics,
        "shouldReplaceRenderer2DUiSprite(recordRenderer2DScaledSprite(",
        "native UI replacement guard",
    )
    require(
        graphics,
        "private boolean shouldReplaceRenderer2DUiSprite(boolean captured, boolean requiresOrderedReplay)",
        "native UI ordered-replay replacement guard",
    )
    require(
        graphics,
        "boolean requiresOrderedReplay = mirrorX || destColumnSkewPerRow != 0;",
        "masked native UI ordered-replay detection",
    )
    require(
        graphics,
        "boolean replacedByRenderer2D = shouldReplaceRenderer2DUiSprite(\n"
        "\t\t\t\t\trecordRenderer2DMaskedScaledSprite(",
        "masked native UI replacement path",
    )
    require(
        graphics,
        "public final void captureRenderer2DUiBaseFrame()",
        "pre-UI base frame surface capture",
    )
    require(
        mudclient,
        "this.getSurface().captureRenderer2DUiBaseFrame();",
        "pre-UI base frame phase boundary",
    )
    require(
        applet,
        "BufferedImage renderer2DUiBaseImage = getRenderer2DUiBaseImage();",
        "pre-UI base frame applet wrapper",
    )
    require(
        scaled_window,
        "shouldUseRenderer2DUiBaseImage(renderer2DUiBaseImage, renderer2DFrame)",
        "pre-UI base frame OpenGL route",
    )
    require(
        scaled_window,
        "renderer2DFrame.getCaptureStats().isNativeUiBaseEligible()",
        "pre-UI base frame safety gate",
    )
    require(
        telemetry,
        "replacedUi=",
        "native UI replacement telemetry",
    )
    require(
        telemetry,
        "uiBase=",
        "pre-UI base frame telemetry",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] sprite capture avg:",
        "capture rejection telemetry report",
    )
    require(
        graphics,
        "recordRenderer2DTextDraw(renderer2DGlyphs);",
        "renderer-v2 text telemetry capture",
    )
    require(
        graphics,
        "shouldReplaceRenderer2DText(recordRenderer2DTextCommand(renderer2DNativeGlyphs))",
        "native UI text replacement guard",
    )
    require(
        graphics,
        "new Renderer2DFrame.TextCommand.GlyphCommand(",
        "native UI text glyph command capture",
    )
    require(
        presenter,
        "Renderer2DFrame.TextCommand[] textCommands = renderer2DFrame.getTextCommands();",
        "OpenGL text command frame handoff",
    )
    require(
        presenter,
        "Renderer2DFrame.PrimitiveCommand[] primitiveCommands = renderer2DFrame.getPrimitiveCommands();",
        "OpenGL primitive command frame handoff",
    )
    require(
        presenter,
        "private void drawTextCommand(Renderer2DFrame.TextCommand command)",
        "OpenGL text command renderer",
    )
    require(
        presenter,
        "private void drawPrimitiveCommand(Renderer2DFrame.PrimitiveCommand command)",
        "OpenGL primitive command renderer",
    )
    require(
        presenter,
        "private boolean shouldReplayNativeBaseCommands(Renderer2DFrame renderer2DFrame)",
        "OpenGL native base command replay safety gate",
    )
    require(
        presenter,
        "Renderer2DSettings.canPresentUiBaseFrame()",
        "OpenGL native base replay requires pre-UI base mode",
    )
    require(
        presenter,
        "Renderer2DFrame.RotatedSpriteCommand[] rotatedSpriteCommands = renderer2DFrame.getRotatedSpriteCommands();",
        "OpenGL rotated sprite command frame handoff",
    )
    require(
        presenter,
        "Renderer2DFrame.CircleCommand[] circleCommands = renderer2DFrame.getCircleCommands();",
        "OpenGL circle command frame handoff",
    )
    require(
        presenter,
        "private void drawRotatedSpriteCommand(Renderer2DFrame.RotatedSpriteCommand command)",
        "OpenGL rotated sprite command renderer",
    )
    require(
        presenter,
        "private void enableSourceClip(int clipLeft, int clipTop, int clipRight, int clipBottom)",
        "OpenGL source clip helper",
    )
    require(
        presenter,
        "gl.glScissor(",
        "OpenGL scissor clipping for native minimap replay",
    )
    require(
        presenter,
        "GL_SCISSOR_TEST",
        "OpenGL scissor state binding",
    )
    require(
        presenter,
        "private void drawCircleCommand(Renderer2DFrame.CircleCommand command)",
        "OpenGL circle command renderer",
    )
    require(
        presenter,
        "RendererTextureData.fromOpaqueSprite(sprite)",
        "OpenGL opaque minimap texture upload",
    )
    require(
        presenter,
        "private static final class OpenGLGlyphTextureCache",
        "OpenGL glyph atlas cache",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] text capture avg:",
        "text capture telemetry report",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] primitive capture avg:",
        "primitive capture telemetry report",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] native UI base avg:",
        "native UI base blocker telemetry report",
    )
    require(
        telemetry,
        "circle=\" + formatCount(circleDrawStats.average())",
        "native UI circle telemetry report",
    )
    require(
        telemetry,
        "replacedUi=\" + formatCount(textCaptureReplacedUiStats.average())",
        "native text replacement telemetry report",
    )
    require(
        applet,
        "text draws/glyphs ",
        "debug overlay text capture line",
    )
    require(
        applet,
        "replUI \" + telemetry.textCaptureReplacedUiAverage",
        "debug overlay native text replacement line",
    )
    require(
        applet,
        "baseReady \" + telemetry.nativeUiBaseEligibleAverage",
        "debug overlay native base readiness line",
    )
    require(
        applet,
        "block s/t/p/m \" + telemetry.nativeUiBlockSpriteAverage",
        "debug overlay native base blocker line",
    )
    require(
        applet,
        "base rot/circ \" + telemetry.rotatedSpriteDrawAverage",
        "debug overlay native base circle line",
    )
    require(
        applet,
        "cap try/ok ",
        "debug overlay capture rejection line",
    )
    require(
        presenter,
        "VISIBLE_SPRITE_ATLAS_FULL",
        "visible replay atlas-full sentinel",
    )
    require(
        telemetry,
        "skipAtlasFull=",
        "atlas-full telemetry report",
    )
    require(
        applet,
        "skip o/i/a ",
        "debug overlay atlas-full skip line",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] sprite direct phases avg:",
        "direct phase telemetry report",
    )
    require(
        telemetry,
        "[renderer-v2 telemetry] sprite visible phases avg:",
        "visible phase telemetry report",
    )
    require(
        applet,
        "replay dir s/w/u ",
        "debug overlay replay phase line",
    )
    require(
        graphics,
        "boolean replacedByRenderer2D = shouldReplaceRenderer2DUiSprite(recordRenderer2DScaledSprite(\n"
        "\t\t\t\t\tsprite,\n"
        "\t\t\t\t\tvar14,\n"
        "\t\t\t\t\twidth,\n"
        "\t\t\t\t\theight,\n"
        "\t\t\t\t\tvar10,\n"
        "\t\t\t\t\tvar11,\n"
        "\t\t\t\t\tscaleX,\n"
        "\t\t\t\t\tscaleY,\n"
        "\t\t\t\t\tvar7));",
        "legacy spriteClipping scaled capture",
    )
    require(
        graphics,
        "private boolean recordRenderer2DPrimitive(int x, int y, int width, int height, int color, int alpha)",
        "native UI primitive capture helper",
    )
    require(
        graphics,
        "shouldReplaceRenderer2DPrimitive(recordRenderer2DPrimitive(",
        "native UI primitive replacement guard",
    )
    require(
        graphics,
        "recordRenderer2DPrimitive(x, y, 1, 1, val, 256)",
        "native UI setPixel primitive capture",
    )
    require(
        graphics,
        "markRenderer2DNativeUiSoftwareDirty(Renderer2DNativeUiBlocker.MINIMAP);",
        "native UI base minimap blocker guard",
    )
    require(
        graphics,
        "new Renderer2DFrame.PrimitiveCommand(",
        "native UI primitive command capture",
    )
    require(
        graphics,
        "private boolean recordRenderer2DRotatedSprite(",
        "native UI rotated sprite capture helper",
    )
    require(
        graphics,
        "new Renderer2DFrame.RotatedSpriteCommand(",
        "native UI rotated sprite command capture",
    )
    require(
        graphics,
        "this.clipLeft,\n\t\t\t\tthis.clipTop,\n\t\t\t\tthis.clipRight,\n\t\t\t\tthis.clipBottom,",
        "native UI rotated sprite clip capture",
    )
    require(
        frame,
        "public int getClipLeft()",
        "rotated sprite clip left accessor",
    )
    require(
        graphics,
        "private boolean recordRenderer2DCircle(int x, int y, int radius, int color, int alpha)",
        "native UI circle capture helper",
    )
    require(
        graphics,
        "new Renderer2DFrame.CircleCommand(",
        "native UI circle command capture",
    )
    require(
        graphics,
        "markRenderer2DNativeUiSoftwareDirty(Renderer2DNativeUiBlocker.CIRCLE);",
        "native UI base circle blocker fallback",
    )
    require(
        graphics,
        "Renderer2DNativeUiBlocker",
        "native UI blocker reason enum",
    )
    require(
        plan,
        "safe|visible|phased|native-ui|geometry",
        "documented native UI overlay mode",
    )
    require(
        plan,
        "SPOILED_MILK_OPENGL_UI_BASE_FRAME=true",
        "documented pre-UI base frame diagnostic mode",
    )
    require(
        plan,
        "SPOILED_MILK_OPENGL_NATIVE_UI_REPLACE=true",
        "documented native UI destructive replacement flag",
    )

    print("PASS: renderer-v2 phased sprite overlay mode is wired")


if __name__ == "__main__":
    main()
