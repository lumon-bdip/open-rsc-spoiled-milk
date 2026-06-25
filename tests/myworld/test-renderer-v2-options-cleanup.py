#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
SCALED_WINDOW = ROOT / "PC_Client/src/orsc/ScaledWindow.java"
OPENGL_PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
OPENRSC = ROOT / "PC_Client/src/orsc/OpenRSC.java"
RENDER_SURFACE_SETTINGS = ROOT / "Client_Base/src/orsc/RenderSurfaceSettings.java"
PROFILE_SETTINGS = ROOT / "Client_Base/src/orsc/RendererProfileSettings.java"
BRIGHTNESS_SETTINGS = ROOT / "Client_Base/src/orsc/RendererBrightnessSettings.java"
FOG_SETTINGS = ROOT / "Client_Base/src/orsc/RendererFogSettings.java"
LIGHTING_SETTINGS = ROOT / "Client_Base/src/orsc/RendererLightingSettings.java"
GEOMETRY_SETTINGS = ROOT / "Client_Base/src/orsc/RendererGeometrySettings.java"
PLAN = ROOT / "docs/myworld/renderer-v2-plan.md"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def forbid(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"retired {description} still present: {needle}")


def main() -> None:
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    scaled_window = SCALED_WINDOW.read_text(encoding="utf-8")
    opengl_presenter = OPENGL_PRESENTER.read_text(encoding="utf-8")
    openrsc = OPENRSC.read_text(encoding="utf-8")
    render_surface_settings = RENDER_SURFACE_SETTINGS.read_text(encoding="utf-8")
    profile_settings = PROFILE_SETTINGS.read_text(encoding="utf-8")
    brightness_settings = BRIGHTNESS_SETTINGS.read_text(encoding="utf-8")
    fog_settings = FOG_SETTINGS.read_text(encoding="utf-8")
    lighting_settings = LIGHTING_SETTINGS.read_text(encoding="utf-8")
    geometry_settings = GEOMETRY_SETTINGS.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(
        scaled_window,
        "public static boolean isOpenGLPrimaryWindowEnabled()",
        "OpenGL-primary visibility helper",
    )
    require(
        scaled_window,
        "if (initialRender) {\n"
        "\t\t\tif (!OPENGL_PRIMARY_WINDOW_ENABLED) {\n"
        "\t\t\t\t// Set the window size for the scalar",
        "OpenGL-primary skips hidden Swing initial resize",
    )
    require(
        scaled_window,
        "openGLFramePresenter.present(\n"
        "\t\t\t\topenGLImage,\n"
        "\t\t\t\t1.0f,\n"
        "\t\t\t\tScalingAlgorithm.INTEGER_SCALING,",
        "OpenGL-primary ignores legacy source scale/filter",
    )
    require(
        scaled_window,
        "public void resizeWindowToScalar() {\n"
        "\t\tif (OPENGL_PRIMARY_WINDOW_ENABLED) {\n"
        "\t\t\treturn;\n"
        "\t\t}",
        "OpenGL-primary scalar resize no-op",
    )
    require(
        scaled_window,
        "private void resizeApplet() {\n"
        "\t\tif (OPENGL_PRIMARY_WINDOW_ENABLED) {\n"
        "\t\t\treturn;\n"
        "\t\t}",
        "OpenGL-primary applet resize no-op",
    )
    require(
        scaled_window,
        "public void validateAppletSize() {\n"
        "\t\tif (OPENGL_PRIMARY_WINDOW_ENABLED) {\n"
        "\t\t\treturn;\n"
        "\t\t}",
        "OpenGL-primary login validation resize no-op",
    )
    require(
        scaled_window,
        "public void componentResized(ComponentEvent e) {\n"
        "\t\tif (!OPENGL_PRIMARY_WINDOW_ENABLED) {\n"
        "\t\t\tresizeApplet();\n"
        "\t\t}",
        "OpenGL-primary Swing resize event ignored",
    )
    require(
        mudclient,
        "boolean isScalarOptionOffered = !isAndroid() && !isOpenGLPrimaryWindow;",
        "legacy scaler hidden in OpenGL-primary options",
    )
    require(
        mudclient,
        "ScaledWindow.isOpenGLPrimaryWindowEnabled() ? OPENGL_PRIMARY_TARGET_FPS : getFPS()",
        "OpenGL-primary 60 FPS client target",
    )
    require(
        mudclient,
        "private static final int SETTINGS_SECTION_ROW = -1000;",
        "reserved non-action settings section row",
    )
    require(
        mudclient,
        'index = addSettingsSection(index, "Video");',
        "settings video section",
    )
    require(
        mudclient,
        'index = addSettingsSection(index, "Graphics");',
        "settings graphics section",
    )
    require(
        mudclient,
        'index = addSettingsSection(index, "Interface");',
        "settings interface section",
    )
    require(
        mudclient,
        "if (settingIndex == SETTINGS_SECTION_ROW) {\n\t\t\treturn;\n\t\t}",
        "settings section rows ignore clicks",
    )
    require(
        mudclient,
        "int scalarOptionIdx = getLegacyScalingSettingsRowIndex();",
        "settings scaling row accounts for section headers",
    )
    require(
        mudclient,
        '"@whi@Resolution - " + RenderSurfaceSettings.getMode().label',
        "player-facing render resolution row",
    )
    require(
        mudclient,
        '"@whi@Renderer - " + RendererProfileSettings.getMode().label',
        "player-facing OpenGL renderer profile row",
    )
    require(
        mudclient,
        '"@whi@Font - " + RendererFontSettings.getMode().label',
        "player-facing OpenGL UI font row",
    )
    require(
        mudclient,
        '"@whi@Brightness - " + RendererBrightnessSettings.getMode().label',
        "player-facing OpenGL brightness row",
    )
    forbid(
        mudclient,
        '"@whi@Lighting - " + RendererLightingSettings.getMode().label',
        "release-facing OpenGL lighting row",
    )
    require(
        mudclient,
        '"@whi@Geometry - " + RendererGeometrySettings.getMode().label',
        "player-facing OpenGL geometry row",
    )
    require(
        mudclient,
        '"@whi@Fog - " + RendererFogSettings.getMode().label',
        "player-facing OpenGL fog row",
    )
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 58 && this.mouseButtonClick == 1)",
        "OpenGL-primary brightness click handler",
    )
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 59 && this.mouseButtonClick == 1)",
        "OpenGL-primary renderer profile click handler",
    )
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 60 && this.mouseButtonClick == 1)",
        "OpenGL-primary fog click handler",
    )
    forbid(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 61 && this.mouseButtonClick == 1)",
        "release-facing OpenGL lighting click handler",
    )
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 62 && this.mouseButtonClick == 1)",
        "OpenGL-primary geometry click handler",
    )
    forbid(
        mudclient,
        '"@whi@Fog - @red@Off"',
        "retired legacy fog toggle row",
    )
    forbid(
        mudclient,
        "if (C_HIDE_FOG)",
        "legacy fog state must not control camera fog generation",
    )
    require(
        profile_settings,
        'static final String PROFILE_PROPERTY_KEY = "opengl_renderer_profile";',
        "renderer profile config key",
    )
    require(
        profile_settings,
        'CLASSIC("classic", "@gre@Classic")',
        "classic renderer profile mode",
    )
    require(
        profile_settings,
        'CUSTOM("custom", "@yel@Custom")',
        "custom renderer profile mode",
    )
    require(
        profile_settings,
        "static Mode markCustom()",
        "renderer profile custom override marker",
    )
    require(
        profile_settings,
        '"SPOILED_MILK_OPENGL_RENDERER_PROFILE"',
        "renderer profile runtime override",
    )
    require(
        openrsc,
        "RendererProfileSettings.loadFromClientSettings(props);",
        "renderer profile setting loaded",
    )
    require(
        openrsc,
        "RendererFogSettings.loadFromClientSettings(props);",
        "renderer fog setting loaded",
    )
    require(
        openrsc,
        "RendererLightingSettings.loadFromClientSettings(props);",
        "renderer lighting setting loaded",
    )
    require(
        openrsc,
        "RendererGeometrySettings.loadFromClientSettings(props);",
        "renderer geometry setting loaded",
    )
    require(
        mudclient,
        "RendererProfileSettings.saveToClientSettings(props);",
        "renderer profile setting saved",
    )
    require(
        mudclient,
        "RendererProfileSettings.setMode(RendererProfileSettings.Mode.CLASSIC);\n"
        "\t\tRendererBrightnessSettings.setMode(RendererBrightnessSettings.Mode.HIGH);\n"
        "\t\tRendererFogSettings.setMode(RendererFogSettings.Mode.ON);\n"
        "\t\tRendererLightingSettings.setMode(RendererLightingSettings.Mode.CLASSIC);\n"
        "\t\tRendererGeometrySettings.setMode(RendererGeometrySettings.Mode.SMOOTH);",
        "renderer profile row restores classic bundle",
    )
    require(
        mudclient,
        "RendererProfileSettings.markCustom();\n\t\tsaveRendererBrightnessSettings();\n\t\tsaveRendererProfileSettings();",
        "brightness override marks renderer preset custom",
    )
    require(
        mudclient,
        "RendererProfileSettings.markCustom();\n\t\tsaveRendererFogSettings();\n\t\tsaveRendererProfileSettings();",
        "fog override marks renderer preset custom",
    )
    require(
        mudclient,
        "RendererProfileSettings.markCustom();\n\t\tsaveRendererGeometrySettings();\n\t\tsaveRendererProfileSettings();",
        "geometry override marks renderer preset custom",
    )
    require(
        brightness_settings,
        'HIGH("high", "@gre@High", 1.0f)',
        "brightness high mode preserves current lighting",
    )
    require(
        brightness_settings,
        'MEDIUM("medium", "@yel@Medium", 0.9f)',
        "brightness medium mode",
    )
    require(
        brightness_settings,
        'LOW("low", "@ora@Low", 0.8f)',
        "brightness low mode",
    )
    require(
        fog_settings,
        'static final String FOG_PROPERTY_KEY = "opengl_fog_distance";',
        "renderer fog config key",
    )
    require(
        fog_settings,
        'ON("on", "@gre@On", 28, 40, 1.0f)',
        "enabled fog mode",
    )
    require(
        fog_settings,
        'OFF("off", "@red@Off", 0, 0, 0.0f)',
        "disabled fog distance mode",
    )
    require(
        fog_settings,
        '|| "close".equals(normalized)\n\t\t\t\t|| "far".equals(normalized)',
        "legacy close/far fog settings should migrate to on",
    )
    require(
        lighting_settings,
        'static final String LIGHTING_PROPERTY_KEY = "opengl_lighting";',
        "renderer lighting config key",
    )
    require(
        lighting_settings,
        'CLASSIC("classic", "@gre@Classic")',
        "classic lighting mode",
    )
    require(
        lighting_settings,
        "mode = Mode.CLASSIC;",
        "normal client settings force release lighting back to Classic",
    )
    require(
        lighting_settings,
        "props.setProperty(LIGHTING_PROPERTY_KEY, Mode.CLASSIC.id);",
        "release lighting persistence writes Classic",
    )
    require(
        lighting_settings,
        'DIRECTIONAL("directional", "@yel@Directional")',
        "directional lighting mode",
    )
    require(
        lighting_settings,
        'TOON("toon", "@cya@Toon")',
        "toon lighting mode",
    )
    require(
        geometry_settings,
        'static final String GEOMETRY_PROPERTY_KEY = "opengl_geometry";',
        "renderer geometry config key",
    )
    require(
        geometry_settings,
        'SMOOTH("smooth", "@gre@Smooth")',
        "smooth geometry mode",
    )
    require(
        geometry_settings,
        'FACETED("faceted", "@yel@Faceted")',
        "faceted geometry mode",
    )
    require(
        geometry_settings,
        'WIRE("wire", "@cya@Wire")',
        "wire geometry mode",
    )
    require(
        mudclient,
        "drawDistance = cameraDepthOffset + World.LOCAL_TILE_COUNT * this.tileSize;",
        "fog-off full loaded-world draw distance",
    )
    require(
        mudclient,
        'System.out.println("[renderer-v2] OpenGL fog: " + mode.id);',
        "OpenGL fog log should describe binary fog setting",
    )
    require(
        opengl_presenter,
        "float brightness = RendererBrightnessSettings.getMode().multiplier;",
        "OpenGL world mesh brightness multiplier",
    )
    require(
        opengl_presenter,
        "prepareOverlayTexturedReplayState();\n\t\tfor (Renderer2DFrame.TextCommand.GlyphCommand glyph : command.getGlyphs())",
        "OpenGL glyph replay owns transparent text state",
    )
    require(
        render_surface_settings,
        'HD("1280x720", "@gre@1280x720 16:9", 1280, 720)',
        "1280x720 render-surface mode",
    )
    require(
        render_surface_settings,
        '"720p".equals(normalized)',
        "1280x720 render-surface alias",
    )
    require(
        mudclient,
        "if (!isOpenGLPrimaryWindow && settingIndex == 46 && this.mouseButtonClick == 1)",
        "legacy scaling type click guard",
    )
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 57 && this.mouseButtonClick == 1)",
        "OpenGL-primary font click handler",
    )
    forbid(mudclient, '"@whi@OpenGL fit - "', "OpenGL fit options row")
    forbid(mudclient, '"@whi@OpenGL window - "', "OpenGL window options row")
    forbid(mudclient, '"@whi@Renderer overlay - "', "renderer overlay options row")

    require(
        opengl_presenter,
        "private static final int INITIAL_WIDTH = RenderSurfaceSettings.getWidth();",
        "OpenGL initial width follows configured render surface",
    )
    require(
        opengl_presenter,
        "private static final int INITIAL_HEIGHT = RenderSurfaceSettings.getHeight();",
        "OpenGL initial height follows configured render surface",
    )
    require(
        opengl_presenter,
        "private OpenGLPresentationSettings.ScaleMode currentScaleMode()",
        "OpenGL scale mode helper",
    )
    require(
        opengl_presenter,
        "? OpenGLPresentationSettings.ScaleMode.ASPECT_FIT",
        "OpenGL-primary automatic aspect-fit mode",
    )
    require(
        applet,
        '"F6 overlay"',
        "release debug overlay shortcut hint",
    )
    require(
        applet,
        "public void componentResized(ComponentEvent e) {\n"
        "\t\tif (ScaledWindow.isOpenGLPrimaryWindowEnabled()) {\n"
        "\t\t\treturn;\n"
        "\t\t}",
        "OpenGL-primary ignores hidden applet resize events",
    )
    require(
        applet,
        "if (!ScaledWindow.isOpenGLPrimaryWindowEnabled() && scaledWindow.isViewportLoaded()) {\n"
        "\t\t\tscaledWindow.resizeWindowToScalar();",
        "OpenGL-primary scalar resize skips hidden Swing window",
    )
    require(
        applet,
        '"renderer " + RendererProfileSettings.getMode().id',
        "OpenGL-primary debug overlay includes renderer profile",
    )
    forbid(applet, "mudclient.cycleOpenGLWindowMode();", "F7 OpenGL window mode hotkey")
    forbid(applet, "mudclient.cycleRenderSurfaceMode();", "F8 render resolution hotkey")
    forbid(applet, "mudclient.cycleOpenGLUiFontMode();", "F9 OpenGL font hotkey")
    forbid(applet, "mudclient.cycleOpenGLScaleMode();", "F9 OpenGL fit hotkey")
    forbid(applet, "mudclient.cycleScalingType();", "F10 scaling type hotkey")
    forbid(applet, "mudclient.scaleDown();", "F11 scale down hotkey")
    forbid(applet, "mudclient.scaleUp();", "F12 scale up hotkey")

    require(
        plan,
        "six player-facing renderer rows: `Resolution`, `Renderer`, `Geometry`,\n`Brightness`, `Fog`, and `Font`",
        "renderer plan documents release OpenGL-primary options",
    )
    require(
        plan,
        "`Renderer` currently exposes the official `Classic` visual\nprofile",
        "renderer plan documents Classic renderer profile",
    )
    require(
        plan,
        "Release/default lighting is locked to Classic.",
        "renderer plan documents release lighting lock",
    )
    require(
        plan,
        "1024x576|1280x720",
        "renderer plan documents 1280x720 resolution option",
    )
    require(
        plan,
        "integer/bilinear/bicubic",
        "renderer plan keeps old scaler terminology out of player options",
    )
    forbid(
        plan,
        "SPOILED_MILK_OPENGL_SCALE_MODE=aspect-fit\n",
        "OpenGL scale mode in current alpha baseline",
    )

    print("PASS: renderer-v2 OpenGL-primary options expose release-safe graphics controls")


if __name__ == "__main__":
    main()
