package orsc;

/** Runtime-only switch for reversible below-terrain visual experiments. */
final class OpenGLBelowTerrainSettings {
	private static final String MODE_PROPERTY = "spoiledmilk.openglBelowTerrain";
	private static final String MODE_ENV = "SPOILED_MILK_OPENGL_BELOW_TERRAIN";

	private static volatile Mode mode = Mode.from(readRuntimeSetting());

	private OpenGLBelowTerrainSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static Mode setMode(Mode next) {
		mode = next == null ? Mode.DEPTH_FLOOR : next;
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
		OFF("off"),
		DEPTH_FLOOR("depth-floor");

		final String id;

		Mode(String id) {
			this.id = id;
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return DEPTH_FLOOR;
			}
			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("none".equals(normalized) || "false".equals(normalized) || "disabled".equals(normalized)) {
				return OFF;
			}
			if ("floor".equals(normalized) || "depth".equals(normalized) || "true".equals(normalized)) {
				return DEPTH_FLOOR;
			}
			for (Mode candidate : values()) {
				if (candidate.id.equals(normalized)) {
					return candidate;
				}
			}
			System.out.println("[renderer-v2] Unknown below-terrain mode '" + value + "'; using depth-floor.");
			return DEPTH_FLOOR;
		}
	}
}
