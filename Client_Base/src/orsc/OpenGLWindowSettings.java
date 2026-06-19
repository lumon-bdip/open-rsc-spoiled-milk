package orsc;

import java.util.Properties;

final class OpenGLWindowSettings {
	static final String WINDOW_MODE_PROPERTY_KEY = "opengl_window_mode";
	private static final String WINDOW_MODE_PROPERTY = "spoiledmilk.openglWindowMode";
	private static final String WINDOW_MODE_ENV = "SPOILED_MILK_OPENGL_WINDOW_MODE";

	private static final boolean runtimeWindowModeOverride = hasRuntimeSetting(WINDOW_MODE_PROPERTY, WINDOW_MODE_ENV);
	private static volatile Mode mode = Mode.from(readRuntimeSetting(WINDOW_MODE_PROPERTY, WINDOW_MODE_ENV));

	private OpenGLWindowSettings() {
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
		if (runtimeWindowModeOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(WINDOW_MODE_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(WINDOW_MODE_PROPERTY_KEY, mode.id);
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
		WINDOWED("windowed", "@gre@Windowed", "Windowed"),
		BORDERLESS_FULLSCREEN("borderless-fullscreen", "@yel@Borderless", "Borderless");

		final String id;
		final String label;
		final String displayName;

		Mode(String id, String label, String displayName) {
			this.id = id;
			this.label = label;
			this.displayName = displayName;
		}

		Mode next() {
			Mode[] modes = values();
			return modes[(ordinal() + 1) % modes.length];
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return WINDOWED;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("fullscreen".equals(normalized) || "borderless".equals(normalized)) {
				return BORDERLESS_FULLSCREEN;
			}
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2 opengl] Unknown OpenGL window mode '" + value + "'; using windowed.");
			return WINDOWED;
		}
	}
}
