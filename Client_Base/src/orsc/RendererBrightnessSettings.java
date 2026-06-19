package orsc;

import java.util.Properties;

final class RendererBrightnessSettings {
	static final String BRIGHTNESS_PROPERTY_KEY = "opengl_brightness";
	private static final String BRIGHTNESS_PROPERTY = "spoiledmilk.openglBrightness";
	private static final String BRIGHTNESS_ENV = "SPOILED_MILK_OPENGL_BRIGHTNESS";

	private static final boolean runtimeBrightnessOverride =
		hasRuntimeSetting(BRIGHTNESS_PROPERTY, BRIGHTNESS_ENV);
	private static volatile Mode mode = Mode.from(readRuntimeSetting(BRIGHTNESS_PROPERTY, BRIGHTNESS_ENV));

	private RendererBrightnessSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static Mode cycleMode() {
		Mode next = mode.next();
		mode = next;
		return next;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeBrightnessOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(BRIGHTNESS_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(BRIGHTNESS_PROPERTY_KEY, mode.id);
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

	enum Mode {
		HIGH("high", "@gre@High", 1.0f),
		MEDIUM("medium", "@yel@Medium", 0.9f),
		LOW("low", "@ora@Low", 0.8f);

		final String id;
		final String label;
		final float multiplier;

		Mode(String id, String label, float multiplier) {
			this.id = id;
			this.label = label;
			this.multiplier = multiplier;
		}

		Mode next() {
			Mode[] modes = values();
			return modes[(ordinal() + 1) % modes.length];
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return HIGH;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown OpenGL brightness '" + value + "'; using high.");
			return HIGH;
		}
	}
}
