package orsc;

import java.util.Properties;

final class RendererReliefSettings {
	static final String TERRAIN_LEVEL_PROPERTY_KEY = "opengl_terrain_relief_level";
	static final String OBJECT_LEVEL_PROPERTY_KEY = "opengl_object_relief_level";
	private static final String RELIEF_PROPERTY = "spoiledmilk.openglRelief";
	private static final String RELIEF_ENV = "SPOILED_MILK_OPENGL_RELIEF";
	private static final String TERRAIN_RELIEF_PROPERTY = "spoiledmilk.openglTerrainRelief";
	private static final String TERRAIN_RELIEF_ENV = "SPOILED_MILK_OPENGL_TERRAIN_RELIEF";
	private static final String OBJECT_RELIEF_PROPERTY = "spoiledmilk.openglObjectRelief";
	private static final String OBJECT_RELIEF_ENV = "SPOILED_MILK_OPENGL_OBJECT_RELIEF";

	private static final Mode configuredTerrainMode = Mode.from(readScopedRuntimeSetting(
		TERRAIN_RELIEF_PROPERTY,
		TERRAIN_RELIEF_ENV));
	private static final Mode configuredObjectMode = Mode.from(readScopedRuntimeSetting(
		OBJECT_RELIEF_PROPERTY,
		OBJECT_RELIEF_ENV));
	private static volatile int terrainLevel = configuredTerrainMode.diagnosticLevel;
	private static volatile int objectLevel = configuredObjectMode.diagnosticLevel;
	private static final boolean terrainRuntimeOverride = hasScopedRuntimeSetting(
		TERRAIN_RELIEF_PROPERTY,
		TERRAIN_RELIEF_ENV);
	private static final boolean objectRuntimeOverride = hasScopedRuntimeSetting(
		OBJECT_RELIEF_PROPERTY,
		OBJECT_RELIEF_ENV);

	private RendererReliefSettings() {
	}

	static Mode getTerrainMode() {
		return configuredTerrainMode;
	}

	static Mode getObjectMode() {
		return configuredObjectMode;
	}

	static int getTerrainLevel() {
		return terrainLevel;
	}

	static int getObjectLevel() {
		return objectLevel;
	}

	static int cycleTerrainLevel() {
		terrainLevel = nextLevel(terrainLevel);
		return terrainLevel;
	}

	static int cycleObjectLevel() {
		objectLevel = nextLevel(objectLevel);
		return objectLevel;
	}

	static int setTerrainLevel(int level) {
		terrainLevel = clampLevel(level);
		return terrainLevel;
	}

	static int setObjectLevel(int level) {
		objectLevel = clampLevel(level);
		return objectLevel;
	}

	static float getTerrainStrength() {
		return strengthForLevel(terrainLevel);
	}

	static float getObjectStrength() {
		return strengthForLevel(objectLevel);
	}

	static void loadFromClientSettings(Properties props) {
		if (props == null) {
			return;
		}
		if (!terrainRuntimeOverride) {
			terrainLevel = readSavedLevel(props, TERRAIN_LEVEL_PROPERTY_KEY, configuredTerrainMode.diagnosticLevel);
		}
		if (!objectRuntimeOverride) {
			objectLevel = readSavedLevel(props, OBJECT_LEVEL_PROPERTY_KEY, configuredObjectMode.diagnosticLevel);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(TERRAIN_LEVEL_PROPERTY_KEY, String.valueOf(terrainLevel));
			props.setProperty(OBJECT_LEVEL_PROPERTY_KEY, String.valueOf(objectLevel));
		}
	}

	static void resetDefaults() {
		if (!terrainRuntimeOverride) {
			terrainLevel = Mode.MAX.diagnosticLevel;
		}
		if (!objectRuntimeOverride) {
			objectLevel = Mode.MAX.diagnosticLevel;
		}
	}

	static String debugSummary() {
		return "relief t/o " + terrainLevel + "/" + objectLevel
			+ " (" + getTerrainStrength() + "/" + getObjectStrength() + ")";
	}

	private static int nextLevel(int level) {
		return level >= 10 ? 1 : level + 1;
	}

	private static float strengthForLevel(int level) {
		return (clampLevel(level) - 1) * 0.5f;
	}

	private static int clampLevel(int level) {
		return Math.max(1, Math.min(10, level));
	}

	private static int readSavedLevel(Properties props, String key, int fallback) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}
		try {
			return clampLevel(Integer.parseInt(value.trim()));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static boolean hasScopedRuntimeSetting(String propertyName, String envName) {
		return !readRuntimeSetting(propertyName, envName).isEmpty()
			|| !readRuntimeSetting(RELIEF_PROPERTY, RELIEF_ENV).isEmpty();
	}

	private static String readScopedRuntimeSetting(String propertyName, String envName) {
		String value = readRuntimeSetting(propertyName, envName);
		return value.isEmpty() ? readRuntimeSetting(RELIEF_PROPERTY, RELIEF_ENV) : value;
	}

	private static String readRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? "" : value.trim();
	}

	enum Mode {
		OFF("off", "@red@Off", 0.0f, 1),
		LOW("low", "@yel@Low", 0.5f, 2),
		MEDIUM("medium", "@gre@Medium", 1.0f, 3),
		HIGH("high", "@cya@High", 1.5f, 4),
		MAX("max", "@ora@Max", 2.0f, 5);

		final String id;
		final String label;
		final float strength;
		final int diagnosticLevel;

		Mode(String id, String label, float strength, int diagnosticLevel) {
			this.id = id;
			this.label = label;
			this.strength = strength;
			this.diagnosticLevel = diagnosticLevel;
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
