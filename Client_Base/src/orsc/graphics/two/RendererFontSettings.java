package orsc.graphics.two;

import java.util.Properties;

public final class RendererFontSettings {
	public static final String UI_FONT_PROPERTY_KEY = "opengl_ui_font";
	private static final String OPENGL_PRESENTER_PROPERTY = "spoiledmilk.openglPresenter";
	private static final String OPENGL_PRESENTER_ENV = "SPOILED_MILK_OPENGL_PRESENTER";
	private static final String OPENGL_PRIMARY_WINDOW_PROPERTY = "spoiledmilk.openglPrimaryWindow";
	private static final String OPENGL_PRIMARY_WINDOW_ENV = "SPOILED_MILK_OPENGL_PRIMARY_WINDOW";
	private static final String UI_FONT_PROPERTY = "spoiledmilk.openglUiFont";
	private static final String UI_FONT_ENV = "SPOILED_MILK_OPENGL_UI_FONT";

	private static final int BANK_TOOLTIP_FONT = 0;
	private static final int LEGACY_BODY_FONT = 1;
	private static final boolean OPENGL_PRIMARY =
		readBoolean(OPENGL_PRESENTER_PROPERTY, OPENGL_PRESENTER_ENV)
			&& readBoolean(OPENGL_PRIMARY_WINDOW_PROPERTY, OPENGL_PRIMARY_WINDOW_ENV);
	private static final boolean runtimeUiFontOverride = hasRuntimeSetting(UI_FONT_PROPERTY, UI_FONT_ENV);
	private static volatile Mode mode = Mode.from(readRuntimeSetting(UI_FONT_PROPERTY, UI_FONT_ENV));

	private RendererFontSettings() {
	}

	static int displayFont(int font) {
		Mode currentMode = mode;
		if (!OPENGL_PRIMARY || currentMode.fontIndex < 0) {
			return font;
		}
		if (font == LEGACY_BODY_FONT) {
			return currentMode.fontIndex;
		}
		return font;
	}

	public static void loadFromClientSettings(Properties props) {
		if (runtimeUiFontOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(UI_FONT_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	private static boolean hasRuntimeSetting(String propertyName, String envName) {
		return !readRuntimeSetting(propertyName, envName).isEmpty();
	}

	private static String readRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? "" : value.trim();
	}

	private static boolean readBoolean(String propertyName, String envName) {
		String value = readRuntimeSetting(propertyName, envName);
		if (value.isEmpty()) {
			return false;
		}

		value = value.trim();
		return "true".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "yes".equalsIgnoreCase(value)
			|| "on".equalsIgnoreCase(value);
	}

	public enum Mode {
		LEGACY("legacy", -1),
		H11P("h11p", BANK_TOOLTIP_FONT),
		H12P("h12p", 2),
		H13B("h13b", 3),
		H14B("h14b", 4);

		public final String id;
		private final int fontIndex;

		Mode(String id, int fontIndex) {
			this.id = id;
			this.fontIndex = fontIndex;
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return LEGACY;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("bank-tooltip".equals(normalized) || "tooltip".equals(normalized)) {
				return H11P;
			}
			if ("default".equals(normalized) || "h12b".equals(normalized)) {
				return LEGACY;
			}
			if ("h16b".equals(normalized) || "h20b".equals(normalized) || "h24b".equals(normalized)
				|| "readable".equals(normalized) || "big".equals(normalized)
				|| "large".equals(normalized) || "huge".equals(normalized) || "xl".equals(normalized)) {
				return H14B;
			}
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown OpenGL UI font '" + value + "'; using legacy h12b.");
			return LEGACY;
		}
	}
}
