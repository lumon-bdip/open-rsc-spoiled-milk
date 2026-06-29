package orsc;

import java.util.Properties;

final class RendererProfileSettings {
	static final String PROFILE_PROPERTY_KEY = "opengl_renderer_profile";
	private static final String PROFILE_PROPERTY = "spoiledmilk.openglRendererProfile";
	private static final String PROFILE_ENV = "SPOILED_MILK_OPENGL_RENDERER_PROFILE";

	private static final boolean runtimeProfileOverride =
		hasRuntimeSetting(PROFILE_PROPERTY, PROFILE_ENV);
	private static volatile Mode mode = Mode.from(readRuntimeSetting(PROFILE_PROPERTY, PROFILE_ENV));

	private RendererProfileSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static Mode cycleMode() {
		Mode next = mode.next();
		mode = next;
		return next;
	}

	static Mode setMode(Mode next) {
		mode = next == null ? Mode.REMASTER : next;
		return mode;
	}

	static Mode markCustom() {
		return setMode(Mode.CUSTOM);
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeProfileOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(PROFILE_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(PROFILE_PROPERTY_KEY, mode.id);
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
		CLASSIC("classic", "@gre@Classic"),
		REMASTER("remaster", "@cya@Remaster"),
		CUSTOM("custom", "@yel@Custom");

		final String id;
		final String label;

		Mode(String id, String label) {
			this.id = id;
			this.label = label;
		}

		Mode next() {
			Mode[] modes = values();
			return modes[(ordinal() + 1) % modes.length];
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return REMASTER;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown OpenGL renderer profile '" + value + "'; using remaster.");
			return REMASTER;
		}
	}
}
