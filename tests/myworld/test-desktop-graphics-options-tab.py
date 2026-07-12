#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def forbid(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"unexpected {description}: {needle}")


def method_body(source: str, signature: str) -> str:
    start = source.find(signature)
    if start < 0:
        fail(f"missing method: {signature}")
    brace = source.find("{", start)
    if brace < 0:
        fail(f"missing method body: {signature}")
    depth = 0
    for index in range(brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[brace + 1:index]
    fail(f"unterminated method: {signature}")


def main() -> None:
    source = MUDCLIENT.read_text(encoding="utf-8")
    desktop_box = method_body(source, "private void drawCustomSettingsBox(")
    android_box = method_body(source, "private void drawAndroidSettingsBox(")
    options = method_body(source, "private void drawUiTabOptions(")
    general = method_body(source, "private void drawGeneralSettingsOptions(")
    graphics = method_body(source, "private void drawGraphicsSettingsOptions(")
    authentic = method_body(source, "private void drawAuthenticSettingsOptions(")
    clicks = method_body(source, "private void handleGeneralSettingsClicks(")

    for label in ('"Social"', '"General"', '"Graphics"'):
        require(desktop_box, label, f"desktop {label} tab label")
    forbid(desktop_box, '"Android"', "Android label on desktop tab bar")
    require(desktop_box, "int firstDivider = var5 / 3;", "desktop first tab divider")
    require(desktop_box, "int secondDivider = 2 * (var5 / 3) + 1;", "desktop second tab divider")
    for selected in range(3):
        require(desktop_box, f"this.settingTab == {selected} ? chosenColor : unchosenColor",
                f"desktop selected color for tab {selected}")

    for label in ('"Social"', '"General"', '"Android"'):
        require(android_box, label, f"unchanged Android {label} tab label")
    forbid(android_box, '"Graphics"', "Graphics label on Android tab bar")

    # Both desktop and Android use the established inclusive boundaries:
    # 0..65 Social, 66..131 General, and 132..195 platform-specific third tab.
    if options.count("var3 < 66") != 2:
        fail("Social click boundary must exist once for Android and once for desktop")
    if options.count("var3 >= 66 && var3 <= 131") != 2:
        fail("General click boundary must exist once for Android and once for desktop")
    if options.count("var3 > 131") != 2:
        fail("third-tab click boundary must exist once for Android and once for desktop")
    require(options, "this.settingTab = 2; // Android Settings Tab", "Android third-tab routing")
    require(options, "this.settingTab = 2; // Graphics Settings Tab", "desktop third-tab routing")
    require(options, "if (isAndroid()) {\n\t\t\t\t\t\tthis.drawAndroidSettingsOptions",
            "Android third-tab renderer")
    require(options, "this.drawGraphicsSettingsOptions(var3, var5, var6, var7);",
            "desktop Graphics renderer")

    graphics_rows = (
        ("Sprites:", "SETTINGS_REMASTERED_SPRITES"),
        ("Scaling - ", "49"),
        ("Scaling type - ", "46"),
        ("Preset - ", "59"),
        ("Aspect Ratio - ", "56"),
        ("Borderless - ", "63"),
        ("Lighting - ", "61"),
        ("Geometry - ", "62"),
        ("Terrain Variation - ", "64"),
        ("Fog - ", "60"),
        ("Terrain shading", "SETTINGS_TERRAIN_RELIEF_SLIDER"),
        ("Object shading", "SETTINGS_OBJECT_RELIEF_SLIDER"),
        ("Brightness / dimness", "SETTINGS_DIMNESS_SLIDER"),
        ("Contrast", "SETTINGS_CONTRAST_SLIDER"),
        ("Gamma", "SETTINGS_GAMMA_SLIDER"),
        ("Saturation", "SETTINGS_SATURATION_SLIDER"),
        ("Hide Roofs", "26"),
        ("Hide Underground Flicker", "42"),
    )
    for label, action in graphics_rows:
        require(graphics, label, f"Graphics placement for {label}")
        require(graphics, action, f"preserved action for {label}")

    for visual_owner in (
        "RendererProfileSettings", "RenderSurfaceSettings", "OpenGLWindowSettings",
        "RendererLightingSettings", "RendererGeometrySettings", "RendererTerrainVariationSettings",
        "RendererFogSettings", "SETTINGS_TERRAIN_RELIEF_SLIDER", "SETTINGS_OBJECT_RELIEF_SLIDER",
        "SETTINGS_DIMNESS_SLIDER", "SETTINGS_CONTRAST_SLIDER", "SETTINGS_GAMMA_SLIDER",
        "SETTINGS_SATURATION_SLIDER", '"@whi@Scaling - "', '"@whi@Scaling type - "',
    ):
        forbid(general, visual_owner, "desktop visual setting in General")

    # Android keeps the visual rows it already exposed through General.
    require(general, "if (isAndroid() && !ScaledWindow.isOpenGLPrimaryWindowEnabled())",
            "Android sprite compatibility guard")
    require(general, "if (isAndroid() && S_SHOW_ROOF_TOGGLE)", "Android roof compatibility guard")
    require(general, "if (isAndroid() && S_SHOW_UNDERGROUND_FLICKER_TOGGLE)",
            "Android underground-flicker compatibility guard")

    for label in (
        "Camera angle mode", "Sound effects", "Mouse buttons", "Minimap position", "Coordinates",
        "Spellbook layout", "Batch Progress Bar", "Tool Focus Menu", "Hits XP Focus Menu",
        "Summon Health Bars", "Experience Drops",
    ):
        require(general, label, f"General placement for {label}")

    for action, handler in (
        (56, "cycleRenderSurfaceMode();"),
        (59, "cycleOpenGLRendererProfileMode();"),
        (60, "cycleOpenGLFogMode();"),
        (61, "cycleOpenGLLightingMode();"),
        (62, "cycleOpenGLGeometryMode();"),
        (63, "cycleOpenGLWindowMode();"),
        (64, "cycleOpenGLTerrainVariationMode();"),
        (26, "reloadCurrentRegionForRoofVisibility();"),
    ):
        require(clicks, f"settingIndex == {action}", f"action {action} dispatch")
        require(clicks, handler, f"action {action} behavior")
    require(clicks, "bufferBits.putByte(42);", "underground flicker packet key")
    require(clicks, "this.settingTab == 2", "software scaling limited to Graphics")
    require(options, "if (this.authenticSettings)\n\t\t\t\tthis.drawAuthenticSettingsOptions",
            "authentic settings route")
    require(authentic, 'drawString("Game options - click to toggle"', "unchanged authentic options UI")

    print("PASS: desktop Graphics options tab placement and boundaries are guarded")


if __name__ == "__main__":
    main()
