#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
SCALED_WINDOW = ROOT / "PC_Client/src/orsc/ScaledWindow.java"
OPENGL_PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
RENDER_SURFACE_SETTINGS = ROOT / "Client_Base/src/orsc/RenderSurfaceSettings.java"
BRIGHTNESS_SETTINGS = ROOT / "Client_Base/src/orsc/RendererBrightnessSettings.java"
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
    render_surface_settings = RENDER_SURFACE_SETTINGS.read_text(encoding="utf-8")
    brightness_settings = BRIGHTNESS_SETTINGS.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(
        scaled_window,
        "public static boolean isOpenGLPrimaryWindowEnabled()",
        "OpenGL-primary visibility helper",
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
        mudclient,
        "boolean isScalarOptionOffered = !isAndroid() && !isOpenGLPrimaryWindow;",
        "legacy scaler hidden in OpenGL-primary options",
    )
    require(
        mudclient,
        '"@whi@Resolution - " + RenderSurfaceSettings.getMode().label',
        "player-facing render resolution row",
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
    require(
        mudclient,
        "if (isOpenGLPrimaryWindow && settingIndex == 58 && this.mouseButtonClick == 1)",
        "OpenGL-primary brightness click handler",
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
        opengl_presenter,
        "float brightness = RendererBrightnessSettings.getMode().multiplier;",
        "OpenGL world mesh brightness multiplier",
    )
    require(
        opengl_presenter,
        "gl.glDisable(gl.GL_ALPHA_TEST);\n\t\tgl.glDisable(gl.GL_DEPTH_TEST);",
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
        '"F6 overlay F7 window F8 resolution F9 font"',
        "OpenGL-primary debug overlay shortcut hint",
    )
    require(
        applet,
        "if (ScaledWindow.isOpenGLPrimaryWindowEnabled()) {",
        "OpenGL-primary shortcut branch",
    )
    require(
        applet,
        "if (keyCode == KeyEvent.VK_F9) mudclient.cycleOpenGLUiFontMode();",
        "OpenGL-primary F9 cycles UI font",
    )
    require(
        applet,
        "} else {\n\t\t\t\t\tif (keyCode == KeyEvent.VK_F9) mudclient.cycleOpenGLScaleMode();",
        "legacy scaler hotkeys gated outside OpenGL-primary",
    )

    require(
        plan,
        "three player-facing renderer rows: `Resolution`, `Font`, and\n`Brightness`",
        "renderer plan documents simplified OpenGL-primary options",
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

    print("PASS: renderer-v2 OpenGL-primary options expose resolution, font, and brightness")


if __name__ == "__main__":
    main()
