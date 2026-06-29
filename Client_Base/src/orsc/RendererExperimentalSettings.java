package orsc;

import java.util.Properties;

final class RendererExperimentalSettings {
	private static final String CAMERA_TILT_PROPERTY = "spoiledmilk.experimentalCameraTilt";
	private static final String CAMERA_TILT_ENV = "SPOILED_MILK_EXPERIMENTAL_CAMERA_TILT";
	private static final String EXTRA_ZOOM_PROPERTY = "spoiledmilk.experimentalExtraZoom";
	private static final String EXTRA_ZOOM_ENV = "SPOILED_MILK_EXPERIMENTAL_EXTRA_ZOOM";

	private static final boolean runtimeCameraTiltOverride =
		hasRuntimeSetting(CAMERA_TILT_PROPERTY, CAMERA_TILT_ENV);
	private static final boolean runtimeExtraZoomOverride =
		hasRuntimeSetting(EXTRA_ZOOM_PROPERTY, EXTRA_ZOOM_ENV);
	private static volatile boolean cameraTiltEnabled =
		readBoolean(CAMERA_TILT_PROPERTY, CAMERA_TILT_ENV, true);
	private static volatile boolean extraZoomEnabled =
		readBoolean(EXTRA_ZOOM_PROPERTY, EXTRA_ZOOM_ENV, true);

	private RendererExperimentalSettings() {
	}

	static boolean isCameraTiltEnabled() {
		return cameraTiltEnabled;
	}

	static boolean isExtraZoomEnabled() {
		return extraZoomEnabled;
	}

	static void loadFromClientSettings(Properties props) {
		// These are no longer player-facing options. Defaults are always on; only
		// explicit launch properties/env vars can change them for diagnostics.
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
