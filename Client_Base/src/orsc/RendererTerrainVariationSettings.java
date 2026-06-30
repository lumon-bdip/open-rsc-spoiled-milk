package orsc;

import java.util.Properties;

final class RendererTerrainVariationSettings {
	static final String TERRAIN_VARIATION_PROPERTY_KEY = "opengl_terrain_variation";
	private static final String ENABLED_PROPERTY = "spoiledmilk.terrainVariationEnabled";
	private static final String ENABLED_ENV = "SPOILED_MILK_TERRAIN_VARIATION_ENABLED";
	private static final String TARGET_RGB_PROPERTY = "spoiledmilk.terrainVariationTargetRgb";
	private static final String TARGET_RGB_ENV = "SPOILED_MILK_TERRAIN_VARIATION_TARGET_RGB";
	private static final String STRENGTH_PROPERTY = "spoiledmilk.terrainVariationStrength";
	private static final String STRENGTH_ENV = "SPOILED_MILK_TERRAIN_VARIATION_STRENGTH";
	private static final String TOLERANCE_PROPERTY = "spoiledmilk.terrainVariationTolerance";
	private static final String TOLERANCE_ENV = "SPOILED_MILK_TERRAIN_VARIATION_TOLERANCE";
	private static final int DEFAULT_GRASS_RGB = 0x109000;
	private static final boolean runtimeEnabledOverride =
		hasRuntimeSetting(ENABLED_PROPERTY, ENABLED_ENV);
	private static final int TARGET_RGB = readTargetRgb();
	private static final float STRENGTH = readFloat(STRENGTH_PROPERTY, STRENGTH_ENV, 0.14f, 0.0f, 0.50f);
	private static final float TOLERANCE = readFloat(TOLERANCE_PROPERTY, TOLERANCE_ENV, 0.020f, 0.001f, 0.40f);
	private static volatile Mode mode = readBoolean(ENABLED_PROPERTY, ENABLED_ENV, true) ? Mode.ON : Mode.OFF;

	private RendererTerrainVariationSettings() {
	}

	static boolean isEnabled() {
		return mode == Mode.ON;
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
		mode = next == null ? Mode.ON : next;
		return mode;
	}

	static float getTargetRed() {
		return ((TARGET_RGB >> 16) & 0xff) / 255.0f;
	}

	static float getTargetGreen() {
		return ((TARGET_RGB >> 8) & 0xff) / 255.0f;
	}

	static float getTargetBlue() {
		return (TARGET_RGB & 0xff) / 255.0f;
	}

	static float getStrength() {
		return STRENGTH;
	}

	static float getTolerance() {
		return TOLERANCE;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeEnabledOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(TERRAIN_VARIATION_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(TERRAIN_VARIATION_PROPERTY_KEY, mode.id);
		}
	}

	private static int readTargetRgb() {
		String value = readRuntimeSetting(TARGET_RGB_PROPERTY, TARGET_RGB_ENV);
		if (value.isEmpty()) {
			return DEFAULT_GRASS_RGB;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.startsWith("#")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("0x")) {
			normalized = normalized.substring(2);
		}
		try {
			return Integer.parseInt(normalized, 16) & 0xffffff;
		} catch (NumberFormatException ex) {
			System.out.println("[renderer-v2] Unknown terrain variation target RGB '" + value + "'; using #109000.");
			return DEFAULT_GRASS_RGB;
		}
	}

	private static boolean readBoolean(String propertyName, String envName, boolean defaultValue) {
		String value = readRuntimeSetting(propertyName, envName);
		if (value.isEmpty()) {
			return defaultValue;
		}
		String normalized = value.toLowerCase();
		if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
			return true;
		}
		if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
			return false;
		}
		System.out.println("[renderer-v2] Unknown terrain variation toggle '" + value + "'; using " + defaultValue + ".");
		return defaultValue;
	}

	private static float readFloat(String propertyName, String envName, float defaultValue, float minValue, float maxValue) {
		String value = readRuntimeSetting(propertyName, envName);
		if (value.isEmpty()) {
			return defaultValue;
		}
		try {
			float parsed = Float.parseFloat(value);
			if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
				return defaultValue;
			}
			return Math.max(minValue, Math.min(maxValue, parsed));
		} catch (NumberFormatException ex) {
			System.out.println("[renderer-v2] Unknown terrain variation value '" + value + "'; using " + defaultValue + ".");
			return defaultValue;
		}
	}

	private static String readRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? "" : value.trim();
	}

	private static boolean hasRuntimeSetting(String propertyName, String envName) {
		return !readRuntimeSetting(propertyName, envName).isEmpty();
	}

	enum Mode {
		ON("on", "@gre@On"),
		OFF("off", "@red@Off");

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
				return ON;
			}
			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("true".equals(normalized) || "enabled".equals(normalized)) {
				return ON;
			}
			if ("false".equals(normalized) || "disabled".equals(normalized)) {
				return OFF;
			}
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}
			System.out.println("[renderer-v2] Unknown terrain variation setting '" + value + "'; using on.");
			return ON;
		}
	}
}
