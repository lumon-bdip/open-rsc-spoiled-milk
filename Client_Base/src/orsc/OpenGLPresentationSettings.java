package orsc;

import java.util.Properties;

final class OpenGLPresentationSettings {
	static final String SCALE_MODE_PROPERTY_KEY = "opengl_scale_mode";
	private static final String SCALE_MODE_PROPERTY = "spoiledmilk.openglScaleMode";
	private static final String SCALE_MODE_ENV = "SPOILED_MILK_OPENGL_SCALE_MODE";

	private static final boolean runtimeScaleModeOverride = hasRuntimeSetting(SCALE_MODE_PROPERTY, SCALE_MODE_ENV);
	private static volatile ScaleMode scaleMode = ScaleMode.from(readRuntimeSetting(SCALE_MODE_PROPERTY, SCALE_MODE_ENV));

	private OpenGLPresentationSettings() {
	}

	static ScaleMode getScaleMode() {
		return scaleMode;
	}

	static void setScaleMode(ScaleMode mode) {
		if (mode != null) {
			scaleMode = mode;
		}
	}

	static ScaleMode cycleScaleMode() {
		ScaleMode next = scaleMode.next();
		scaleMode = next;
		return next;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeScaleModeOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(SCALE_MODE_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			scaleMode = ScaleMode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(SCALE_MODE_PROPERTY_KEY, scaleMode.id);
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

	enum ScaleMode {
		ASPECT_FIT("aspect-fit", "@gre@Aspect Fit"),
		INTEGER_FIT("integer-fit", "@yel@Integer Fit"),
		STRETCH("stretch", "@red@Stretch");

		final String id;
		final String label;

		ScaleMode(String id, String label) {
			this.id = id;
			this.label = label;
		}

		ScaleMode next() {
			ScaleMode[] modes = values();
			return modes[(ordinal() + 1) % modes.length];
		}

		static ScaleMode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return ASPECT_FIT;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("4:3-fit".equals(normalized) || "4:3".equals(normalized) || "four-three-fit".equals(normalized)) {
				return ASPECT_FIT;
			}
			if ("16:9-fit".equals(normalized) || "16:9".equals(normalized) || "sixteen-nine-fit".equals(normalized)) {
				return ASPECT_FIT;
			}
			for (ScaleMode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2 opengl] Unknown OpenGL scale mode '" + value + "'; using aspect-fit.");
			return ASPECT_FIT;
		}
	}
}
