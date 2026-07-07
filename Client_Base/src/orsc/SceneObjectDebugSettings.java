package orsc;

import java.util.Locale;

final class SceneObjectDebugSettings {
	private static final String ENABLED_PROPERTY = "spoiledmilk.sceneObjectDebug";
	private static final String ENABLED_ENV = "SPOILED_MILK_SCENE_OBJECT_DEBUG";
	private static final String FILTER_PROPERTY = "spoiledmilk.sceneObjectDebugFilter";
	private static final String FILTER_ENV = "SPOILED_MILK_SCENE_OBJECT_DEBUG_FILTER";
	private static final String RADIUS_PROPERTY = "spoiledmilk.sceneObjectDebugRadius";
	private static final String RADIUS_ENV = "SPOILED_MILK_SCENE_OBJECT_DEBUG_RADIUS";

	private static final boolean ENABLED = readBoolean(ENABLED_PROPERTY, ENABLED_ENV, false);
	private static final String FILTER = readString(FILTER_PROPERTY, FILTER_ENV, "").toLowerCase(Locale.ROOT);
	private static final int RADIUS = Math.max(0, readInt(RADIUS_PROPERTY, RADIUS_ENV, 0));

	private SceneObjectDebugSettings() {
	}

	static boolean isEnabled() {
		return ENABLED;
	}

	static boolean matches(int id, String name, String modelName) {
		if (!ENABLED) {
			return false;
		}
		if (FILTER.isEmpty()) {
			return true;
		}

		String idText = String.valueOf(id);
		String nameText = normalize(name);
		String modelText = normalize(modelName);
		String[] tokens = FILTER.split(",");
		for (String token : tokens) {
			String trimmed = token.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (idText.equals(trimmed)
				|| nameText.contains(trimmed)
				|| modelText.contains(trimmed)) {
				return true;
			}
		}
		return false;
	}

	static boolean withinRadius(int playerTileX, int playerTileZ, int tileX, int tileZ) {
		if (!ENABLED || RADIUS <= 0) {
			return true;
		}
		return Math.abs(playerTileX - tileX) <= RADIUS
			&& Math.abs(playerTileZ - tileZ) <= RADIUS;
	}

	static String description() {
		return "filter='" + FILTER + "' radius=" + RADIUS;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private static boolean readBoolean(String propertyName, String envName, boolean defaultValue) {
		String value = readString(propertyName, envName, "");
		if (value.isEmpty()) {
			return defaultValue;
		}
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

	private static int readInt(String propertyName, String envName, int defaultValue) {
		String value = readString(propertyName, envName, "");
		if (value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	private static String readString(String propertyName, String envName, String defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? defaultValue : value.trim();
	}
}
