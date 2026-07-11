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
LIGHTING_SETTINGS = ROOT / "Client_Base/src/orsc/RendererLightingSettings.java"
TERRAIN_SETTINGS = ROOT / "Client_Base/src/orsc/RendererTerrainVariationSettings.java"
PLAN = ROOT / "docs/myworld/in-progress-work-plans/renderer-v2-plan.md"


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
    presenter = OPENGL_PRESENTER.read_text(encoding="utf-8")
    openrsc = OPENRSC.read_text(encoding="utf-8")
    surface = RENDER_SURFACE_SETTINGS.read_text(encoding="utf-8")
    profile = PROFILE_SETTINGS.read_text(encoding="utf-8")
    lighting = LIGHTING_SETTINGS.read_text(encoding="utf-8")
    terrain = TERRAIN_SETTINGS.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(
        scaled_window,
        "public static boolean isOpenGLPrimaryWindowEnabled()",
        "OpenGL-primary visibility helper",
    )
    require(
        scaled_window,
        "public void resizeWindowToScalar() {\n\t\tif (OPENGL_PRIMARY_WINDOW_ENABLED) {\n\t\t\treturn;",
        "OpenGL-primary scalar resize guard",
    )
    require(
        scaled_window,
        "public void componentResized(ComponentEvent e) {\n\t\tif (!OPENGL_PRIMARY_WINDOW_ENABLED) {",
        "OpenGL-primary Swing resize guard",
    )
    require(
        mudclient,
        "boolean isScalarOptionOffered = !isAndroid() && !isOpenGLPrimaryWindow;",
        "legacy scaler visibility guard",
    )
    require(
        mudclient,
        "ScaledWindow.isOpenGLPrimaryWindowEnabled() ? OPENGL_PRIMARY_TARGET_FPS : getFPS()",
        "OpenGL-primary client frame target",
    )

    require(mudclient, 'index = addSettingsSection(index, "Graphics");', "Graphics section")
    require(mudclient, 'index = addSettingsSection(index, "Interface");', "Interface section")
    forbid(mudclient, 'index = addSettingsSection(index, "Video");', "Video section")
    require(
        mudclient,
        "if (settingIndex == SETTINGS_SECTION_ROW) {\n\t\t\treturn;",
        "non-action section rows",
    )

    expected_rows = (
        ("Preset", "RendererProfileSettings.getMode().label", 59),
        ("Aspect Ratio", "RenderSurfaceSettings.getAspectLabel()", 56),
        ("Lighting", "RendererLightingSettings.getMode().label", 61),
        ("Geometry", "RendererGeometrySettings.getMode().label", 62),
        ("Terrain Variation", "RendererTerrainVariationSettings.getMode().label", 64),
        ("Fog", "RendererFogSettings.getMode().label", 60),
    )
    for label, value, action in expected_rows:
        require(
            mudclient,
            f'index = addSettingsRow(index, "@whi@{label} - " + {value}, {action});',
            f"{label} settings row",
        )
    require(mudclient, 'index = addSettingsRow(index, "@whi@Borderless - " + borderlessLabel, 63);', "Borderless settings row")

    for action, handler in (
        (56, "cycleRenderSurfaceMode();"),
        (59, "cycleOpenGLRendererProfileMode();"),
        (60, "cycleOpenGLFogMode();"),
        (61, "cycleOpenGLLightingMode();"),
        (62, "cycleOpenGLGeometryMode();"),
        (63, "cycleOpenGLWindowMode();"),
        (64, "cycleOpenGLTerrainVariationMode();"),
    ):
        require(mudclient, f"settingIndex == {action} && this.mouseButtonClick == 1", f"action {action} click guard")
        require(mudclient, handler, f"action {action} handler")

    for retired in ("@whi@Resolution - ", "@whi@Renderer - ", "@whi@Font - ", "@whi@Tone - "):
        forbid(mudclient, retired, f"player-facing {retired.strip()} row")
    forbid(mudclient, '"@whi@Brightness - "', "superseded player-facing Brightness row")
    forbid(mudclient, "cycleOpenGLBrightnessMode()", "superseded Brightness click handler")
    for label in ("Terrain shading", "Object shading", "Dimness", "Contrast"):
        require(mudclient, f'index = addSettingsRow(index, "@whi@{label}", SETTINGS_SECTION_ROW);',
                f"two-line {label} slider label")
    require(mudclient, 'new StringBuilder("@whi@- [")', "slider minus and track presentation")
    require(mudclient, 'bar.append("] + @yel@[")', "slider plus and value presentation")
    require(mudclient, "handleRendererTuningSliderInput(settingIndex, var6);", "slider click/drag handling")
    forbid(mudclient, '"@whi@Fog - @red@Off"', "legacy Interface fog row")
    forbid(mudclient, "if (C_HIDE_FOG)", "legacy fog render ownership")

    require(profile, 'CLASSIC("classic", "@gre@Classic")', "Classic preset")
    require(profile, 'REMASTER("remaster", "@cya@Remaster")', "Remaster preset")
    require(profile, 'CUSTOM("custom", "@yel@Custom")', "Custom preset")
    require(profile, "return REMASTER;", "Remaster default")
    require(mudclient, "if (mode == RendererProfileSettings.Mode.CLASSIC) {", "Classic bundle")
    require(mudclient, "RenderSurfaceSettings.setMode(RenderSurfaceSettings.Mode.SVGA);", "Classic 4:3 surface")
    require(mudclient, "RendererTerrainVariationSettings.setMode(RendererTerrainVariationSettings.Mode.OFF);", "Classic terrain bundle")
    require(mudclient, "} else if (mode == RendererProfileSettings.Mode.REMASTER) {", "Remaster bundle")
    require(mudclient, "RenderSurfaceSettings.setMode(RenderSurfaceSettings.Mode.WIDE);", "Remaster 16:9 surface")
    require(mudclient, "RendererLightingSettings.setMode(RendererLightingSettings.Mode.DIRECTIONAL);", "Remaster directional lighting")
    require(mudclient, "RendererTerrainVariationSettings.setMode(RendererTerrainVariationSettings.Mode.ON);", "Remaster terrain bundle")
    require(mudclient, "RendererProfileSettings.markCustom();", "manual setting Custom marker")

    require(lighting, 'CLASSIC("classic", "@gre@Classic")', "Classic lighting")
    require(lighting, 'DIRECTIONAL("directional", "@yel@Directional")', "Directional lighting")
    require(lighting, "props.setProperty(LIGHTING_PROPERTY_KEY, mode.id);", "saved lighting selection")
    require(terrain, 'ON("on", "@gre@On")', "terrain variation on mode")
    require(terrain, 'OFF("off", "@red@Off")', "terrain variation off mode")

    require(surface, 'SVGA("800x600", "@gre@4:3", 800, 600, true', "player-visible 4:3 surface")
    require(surface, 'WIDE("960x540", "@yel@16:9", 960, 540, true', "player-visible 16:9 surface")
    require(surface, 'HD("1280x720", "@yel@16:9", 1280, 720, false', "hidden 720p migration surface")
    require(surface, 'FULL_HD("1920x1080", "@yel@16:9", 1920, 1080, false', "hidden 1080p migration surface")
    require(surface, '"720p".equals(normalized)', "720p aspect migration")
    require(surface, '"1080p".equals(normalized)', "1080p aspect migration")

    for settings_class in (
        "RendererProfileSettings",
        "RendererReliefSettings",
        "RendererColorDiagnosticSettings",
        "RendererFogSettings",
        "RendererLightingSettings",
        "RendererGeometrySettings",
        "RendererTerrainVariationSettings",
    ):
        require(openrsc, f"{settings_class}.loadFromClientSettings(props);", f"{settings_class} startup load")

    require(presenter, "private static final int INITIAL_WIDTH = RenderSurfaceSettings.getWidth();", "configured OpenGL width")
    require(presenter, "private static final int INITIAL_HEIGHT = RenderSurfaceSettings.getHeight();", "configured OpenGL height")
    require(presenter, "? OpenGLPresentationSettings.ScaleMode.ASPECT_FIT", "automatic aspect-fit presentation")
    require(applet, '"Ctrl+F6 expanded"', "release debug overlay shortcut hint")
    for retired_hotkey in (
        "mudclient.cycleOpenGLWindowMode();",
        "mudclient.cycleRenderSurfaceMode();",
        "mudclient.cycleOpenGLUiFontMode();",
        "mudclient.cycleOpenGLScaleMode();",
        "mudclient.cycleScalingType();",
        "mudclient.scaleDown();",
        "mudclient.scaleUp();",
    ):
        forbid(applet, retired_hotkey, f"retired quick hotkey {retired_hotkey}")

    require(
        plan,
        "`Preset`, `Aspect Ratio`, `Borderless`, `Lighting`, `Geometry`,\n`Terrain Variation`, and `Fog`, followed by two-line `Terrain shading`,\n`Object shading`, `Dimness`, and `Contrast` sliders",
        "current Graphics row contract",
    )
    require(plan, "`Preset` provides `Classic`, `Remaster`, and `Custom`.", "preset contract")
    require(plan, "Active player choices are `4:3` (`800x600`) and `16:9` (`960x540`)", "aspect contract")

    print("PASS: renderer-v2 OpenGL-primary options match the current graphics contract")


if __name__ == "__main__":
    main()
