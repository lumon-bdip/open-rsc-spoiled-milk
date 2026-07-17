package orsc;

/** Runtime switch used to compare the legacy screen sky with world-anchored prototypes. */
final class OpenGLSkySettings {
	private static final String MODE_PROPERTY = "spoiledmilk.openglSky";
	private static final String MODE_ENV = "SPOILED_MILK_OPENGL_SKY";

	private static volatile Mode mode = Mode.from(readRuntimeSetting());

	private OpenGLSkySettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static Mode setMode(Mode next) {
		mode = next == null ? Mode.SCREEN : next;
		return mode;
	}

	private static String readRuntimeSetting() {
		String value = System.getProperty(MODE_PROPERTY);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(MODE_ENV);
		}
		return value == null ? "" : value.trim();
	}

	enum Mode {
		SCREEN("screen"),
		WORLD_DOME("world-dome");

		final String id;

		Mode(String id) {
			this.id = id;
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return SCREEN;
			}
			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("legacy".equals(normalized) || "flat".equals(normalized)) {
				return SCREEN;
			}
			if ("dome".equals(normalized) || "sphere".equals(normalized) || "world".equals(normalized)) {
				return WORLD_DOME;
			}
			for (Mode candidate : values()) {
				if (candidate.id.equals(normalized)) {
					return candidate;
				}
			}
			System.out.println("[renderer-v2] Unknown OpenGL sky '" + value + "'; using screen.");
			return SCREEN;
		}
	}
}
