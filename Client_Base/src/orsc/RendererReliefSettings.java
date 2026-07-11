package orsc;

import java.util.Properties;

final class RendererReliefSettings {
	static final int MIN_LEVEL = 1;
	static final int MAX_LEVEL = 20;
	static final int DEFAULT_LEVEL = 10;
	private static final String SCALE_VERSION_PROPERTY_KEY = "opengl_relief_tuning_scale";
	private static final String CENTERED_SCALE_VERSION = "centered-default-20-v1";
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
	private static volatile int terrainLevel = closestLevelForStrength(configuredTerrainMode.strength);
	private static volatile int objectLevel = closestLevelForStrength(configuredObjectMode.strength);
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
			terrainLevel = readSavedLevel(props, TERRAIN_LEVEL_PROPERTY_KEY, configuredTerrainMode);
		}
		if (!objectRuntimeOverride) {
			objectLevel = readSavedLevel(props, OBJECT_LEVEL_PROPERTY_KEY, configuredObjectMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(SCALE_VERSION_PROPERTY_KEY, CENTERED_SCALE_VERSION);
			props.setProperty(TERRAIN_LEVEL_PROPERTY_KEY, String.valueOf(terrainLevel));
			props.setProperty(OBJECT_LEVEL_PROPERTY_KEY, String.valueOf(objectLevel));
		}
	}

	static void resetDefaults() {
		if (!terrainRuntimeOverride) {
			terrainLevel = DEFAULT_LEVEL;
		}
		if (!objectRuntimeOverride) {
			objectLevel = DEFAULT_LEVEL;
		}
	}

	static String debugSummary() {
		return "relief t/o " + terrainLevel + "/" + objectLevel
			+ " (" + getTerrainStrength() + "/" + getObjectStrength() + ")";
	}

	private static int nextLevel(int level) {
		return level >= MAX_LEVEL ? MIN_LEVEL : level + 1;
	}

	private static float strengthForLevel(int level) {
		int boundedLevel = clampLevel(level);
		if (boundedLevel <= 5) {
			return (boundedLevel - 1) * 0.25f;
		}
		if (boundedLevel <= 8) {
			return 1.0f + (boundedLevel - 5) * (0.5f / 3.0f);
		}
		if (boundedLevel <= DEFAULT_LEVEL) {
			return 1.5f + (boundedLevel - 8) * 0.25f;
		}
		return 2.0f + (boundedLevel - DEFAULT_LEVEL) * 0.75f;
	}

	private static int clampLevel(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
	}

	private static int readSavedLevel(Properties props, String key, Mode fallbackMode) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return closestLevelForStrength(fallbackMode.strength);
		}
		try {
			int savedLevel = clampLevel(Integer.parseInt(value.trim()));
			return CENTERED_SCALE_VERSION.equals(props.getProperty(SCALE_VERSION_PROPERTY_KEY))
				? savedLevel
				: closestLevelForStrength((savedLevel - 1) * 0.5f);
		} catch (NumberFormatException ignored) {
			return closestLevelForStrength(fallbackMode.strength);
		}
	}

	private static int closestLevelForStrength(float targetStrength) {
		int closestLevel = DEFAULT_LEVEL;
		float closestDistance = Float.MAX_VALUE;
		for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
			float distance = Math.abs(strengthForLevel(level) - targetStrength);
			if (distance < closestDistance) {
				closestLevel = level;
				closestDistance = distance;
			}
		}
		return closestLevel;
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
