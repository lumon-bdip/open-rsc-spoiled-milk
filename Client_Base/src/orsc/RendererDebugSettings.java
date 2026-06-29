package orsc;

import java.util.Properties;

final class RendererDebugSettings {
	static final String OVERLAY_PROPERTY_KEY = "renderer_debug_overlay";
	static final String OVERLAY_MODE_PROPERTY_KEY = "renderer_debug_overlay_mode";
	private static final String OVERLAY_PROPERTY = "spoiledmilk.rendererDebugOverlay";
	private static final String OVERLAY_ENV = "SPOILED_MILK_RENDERER_DEBUG_OVERLAY";
	private static final String OVERLAY_MODE_PROPERTY = "spoiledmilk.rendererDebugOverlayMode";
	private static final String OVERLAY_MODE_ENV = "SPOILED_MILK_RENDERER_DEBUG_OVERLAY_MODE";

	private static final boolean runtimeOverlayOverride = hasRuntimeSetting(OVERLAY_PROPERTY, OVERLAY_ENV);
	private static final boolean runtimeOverlayModeOverride = hasRuntimeSetting(OVERLAY_MODE_PROPERTY, OVERLAY_MODE_ENV);
	private static volatile boolean overlayEnabled = readBoolean(OVERLAY_PROPERTY, OVERLAY_ENV, false);
	private static volatile Mode overlayMode = readMode(OVERLAY_MODE_PROPERTY, OVERLAY_MODE_ENV, Mode.SIMPLE);

	enum Mode {
		SIMPLE("simple"),
		EXPANDED("expanded");

		final String id;

		Mode(String id) {
			this.id = id;
		}
	}

	private RendererDebugSettings() {
	}

	static boolean isOverlayEnabled() {
		return overlayEnabled;
	}

	static Mode getMode() {
		return overlayMode;
	}

	static boolean toggleOverlay() {
		overlayEnabled = !overlayEnabled;
		RenderTelemetry.resetOpenGLFramePacing();
		return overlayEnabled;
	}

	static Mode toggleMode() {
		overlayMode = overlayMode == Mode.SIMPLE ? Mode.EXPANDED : Mode.SIMPLE;
		return overlayMode;
	}

	static void loadFromClientSettings(Properties props) {
		if (props == null) {
			return;
		}
		if (!runtimeOverlayOverride) {
			String configuredOverlay = props.getProperty(OVERLAY_PROPERTY_KEY);
			if (configuredOverlay != null && !configuredOverlay.trim().isEmpty()) {
				overlayEnabled = parseBoolean(configuredOverlay, overlayEnabled);
			}
		}
		if (!runtimeOverlayModeOverride) {
			String configuredMode = props.getProperty(OVERLAY_MODE_PROPERTY_KEY);
			if (configuredMode != null && !configuredMode.trim().isEmpty()) {
				overlayMode = parseMode(configuredMode, overlayMode);
			}
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(OVERLAY_PROPERTY_KEY, String.valueOf(overlayEnabled));
			props.setProperty(OVERLAY_MODE_PROPERTY_KEY, overlayMode.id);
		}
	}

	private static boolean hasRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value != null && !value.trim().isEmpty();
	}

	private static boolean readBoolean(String propertyName, String envName, boolean defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return parseBoolean(value, defaultValue);
	}

	private static Mode readMode(String propertyName, String envName, Mode defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return parseMode(value, defaultValue);
	}

	private static boolean parseBoolean(String value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		value = value.trim();
		if ("true".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "yes".equalsIgnoreCase(value)
			|| "on".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)
			|| "0".equals(value)
			|| "no".equalsIgnoreCase(value)
			|| "off".equalsIgnoreCase(value)) {
			return false;
		}
		return defaultValue;
	}

	private static Mode parseMode(String value, Mode defaultValue) {
		if (value == null) {
			return defaultValue;
		}

		value = value.trim();
		for (Mode mode : Mode.values()) {
			if (mode.id.equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return defaultValue;
	}
}
