package orsc;

import java.util.Properties;

final class RendererDebugSettings {
	static final String OVERLAY_PROPERTY_KEY = "renderer_debug_overlay";
	private static final String OVERLAY_PROPERTY = "spoiledmilk.rendererDebugOverlay";
	private static final String OVERLAY_ENV = "SPOILED_MILK_RENDERER_DEBUG_OVERLAY";

	private static final boolean runtimeOverlayOverride = hasRuntimeSetting(OVERLAY_PROPERTY, OVERLAY_ENV);
	private static volatile boolean overlayEnabled = readBoolean(OVERLAY_PROPERTY, OVERLAY_ENV, false);

	private RendererDebugSettings() {
	}

	static boolean isOverlayEnabled() {
		return overlayEnabled;
	}

	static boolean toggleOverlay() {
		overlayEnabled = !overlayEnabled;
		return overlayEnabled;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeOverlayOverride || props == null) {
			return;
		}
		String configuredOverlay = props.getProperty(OVERLAY_PROPERTY_KEY);
		if (configuredOverlay != null && !configuredOverlay.trim().isEmpty()) {
			overlayEnabled = parseBoolean(configuredOverlay, overlayEnabled);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(OVERLAY_PROPERTY_KEY, String.valueOf(overlayEnabled));
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
}
