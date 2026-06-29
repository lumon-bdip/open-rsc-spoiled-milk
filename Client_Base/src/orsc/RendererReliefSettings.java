package orsc;

final class RendererReliefSettings {
	private static final String RELIEF_PROPERTY = "spoiledmilk.openglRelief";
	private static final String RELIEF_ENV = "SPOILED_MILK_OPENGL_RELIEF";

	private static volatile Mode mode = Mode.from(readRuntimeSetting(RELIEF_PROPERTY, RELIEF_ENV));

	private RendererReliefSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static float getStrength() {
		return mode.strength;
	}

	private static String readRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? "" : value.trim();
	}

	enum Mode {
		OFF("off", "@red@Off", 0.0f),
		LOW("low", "@yel@Low", 0.5f),
		MEDIUM("medium", "@gre@Medium", 1.0f),
		HIGH("high", "@cya@High", 1.5f),
		MAX("max", "@ora@Max", 2.0f);

		final String id;
		final String label;
		final float strength;

		Mode(String id, String label, float strength) {
			this.id = id;
			this.label = label;
			this.strength = strength;
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return MAX;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("none".equals(normalized) || "disabled".equals(normalized)) {
				return OFF;
			}
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown OpenGL relief '" + value + "'; using max.");
			return MAX;
		}
	}
}
