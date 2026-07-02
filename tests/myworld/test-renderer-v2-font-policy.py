#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "Client_Base/src/orsc/graphics/two/Fonts.java"
FONT_SETTINGS = ROOT / "Client_Base/src/orsc/graphics/two/RendererFontSettings.java"
GRAPHICS = ROOT / "Client_Base/src/orsc/graphics/two/GraphicsController.java"
CUSTOM_BANK = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/CustomBankInterface.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
OPENRSC = ROOT / "PC_Client/src/orsc/OpenRSC.java"
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
    fonts = FONTS.read_text(encoding="utf-8")
    font_settings = FONT_SETTINGS.read_text(encoding="utf-8")
    graphics = GRAPHICS.read_text(encoding="utf-8")
    custom_bank = CUSTOM_BANK.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    openrsc = OPENRSC.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(mudclient, 'Fonts.addFont(DataOperations.loadData("h11p.jf"', "bank-tooltip font load order")
    require(mudclient, 'Fonts.addFont(DataOperations.loadData("h12b.jf"', "legacy body font load order")
    require(mudclient, 'Fonts.addFont(DataOperations.loadData("h12p.jf"', "regular h12p font load order")
    require(mudclient, 'Fonts.addFont(DataOperations.loadData("h13b.jf"', "h13b font load order")
    require(mudclient, 'Fonts.addFont(DataOperations.loadData("h14b.jf"', "h14b font load order")
    require(custom_bank, "private static final int HOVER_TOOLTIP_FONT = 0;", "bank tooltip font marker")
    require(font_settings, "public static final String UI_FONT_PROPERTY_KEY = \"opengl_ui_font\";", "UI font config key")
    require(font_settings, "private static final int BANK_TOOLTIP_FONT = 0;", "bank tooltip font policy constant")
    require(font_settings, "private static final int LEGACY_BODY_FONT = 1;", "legacy body font policy constant")
    require(font_settings, '"SPOILED_MILK_OPENGL_UI_FONT"', "OpenGL UI font runtime override")
    require(font_settings, 'LEGACY("legacy", -1)', "legacy font mode")
    require(font_settings, 'H11P("h11p", BANK_TOOLTIP_FONT)', "h11p font mode")
    require(font_settings, 'H12P("h12p", 2)', "h12p font mode")
    require(font_settings, 'H13B("h13b", 3)', "h13b font mode")
    require(font_settings, 'H14B("h14b", 4)', "h14b font mode")
    forbid(font_settings, 'H16B("h16b"', "oversized UI font mode")
    forbid(font_settings, 'H20B("h20b"', "oversized UI font mode")
    forbid(font_settings, 'H24B("h24b"', "oversized UI font mode")
    require(
        font_settings,
        '"h16b".equals(normalized) || "h20b".equals(normalized) || "h24b".equals(normalized)',
        "retired oversized UI font fallback",
    )
    forbid(font_settings, "public static Mode cycleMode()", "player-facing OpenGL UI font cycle")
    forbid(font_settings, "public static Mode getMode()", "player-facing OpenGL UI font getter")
    require(
        font_settings,
        "if (font == LEGACY_BODY_FONT) {\n\t\t\treturn currentMode.fontIndex;",
        "OpenGL-primary body text remaps to selected font",
    )
    require(openrsc, "RendererFontSettings.loadFromClientSettings(props);", "UI font setting loaded")
    forbid(mudclient, "RendererFontSettings.saveToClientSettings(props);", "UI font setting saved")
    forbid(mudclient, "void cycleOpenGLUiFontMode()", "mudclient UI font cycle method")
    forbid(mudclient, '"@whi@Font - " + RendererFontSettings.getMode().label', "General options UI font row")
    forbid(mudclient, "if (isOpenGLPrimaryWindow && settingIndex == 57 && this.mouseButtonClick == 1)", "General options UI font click handler")
    forbid(applet, "mudclient.cycleOpenGLUiFontMode();", "F9 OpenGL-primary UI font hotkey")
    require(applet, 'new Font("Monospaced", Font.PLAIN, 13)', "debug overlay font size")
    require(graphics, "font = RendererFontSettings.displayFont(font);", "font policy applied")
    require(
        graphics,
        "public final int fontHeight(int font) {\n\t\ttry {\n\t\t\tfont = RendererFontSettings.displayFont(font);",
        "font policy applied to font height",
    )
    require(
        graphics,
        "public final int stringWidth(int font, String str) {\n\t\ttry {\n\t\t\tfont = RendererFontSettings.displayFont(font);",
        "font policy applied to string width",
    )
    require(
        plan,
        "OpenGL-primary body-font remapping remains a hidden compatibility path",
        "renderer plan documents OpenGL font option",
    )
    require(
        plan,
        "debug overlay uses a separate 13pt monospaced font",
        "renderer plan documents debug overlay font size",
    )

    print("PASS: renderer-v2 OpenGL-primary font policy is hidden compatibility-only")


if __name__ == "__main__":
    main()
