package orsc;

import java.util.Properties;

final class RendererColorDiagnosticSettings {
	static final int MIN_LEVEL = 1;
	static final int MAX_LEVEL = 20;
	static final int CENTER_LEVEL = 10;
	private static final int LEGACY_MAX_LEVEL = 10;
	private static final int EXTENDED_LEGACY_MAX_LEVEL = 20;
	private static final String SCALE_VERSION_PROPERTY_KEY = "opengl_color_tuning_scale";
	private static final String CENTERED_SCALE_VERSION = "centered-20-v2";
	private static final String PREVIOUS_CENTERED_SCALE_VERSION = "centered-21-v1";
	static final String DIMNESS_LEVEL_PROPERTY_KEY = "opengl_dimness_level";
	static final String CONTRAST_LEVEL_PROPERTY_KEY = "opengl_contrast_level";
	private static volatile int dimnessLevel = CENTER_LEVEL;
	private static volatile int contrastLevel = CENTER_LEVEL;

	private RendererColorDiagnosticSettings() {
	}

	static int getDimnessLevel() {
		return dimnessLevel;
	}

	static int getContrastLevel() {
		return contrastLevel;
	}

	static int cycleDimnessLevel() {
		dimnessLevel = nextLevel(dimnessLevel);
		return dimnessLevel;
	}

	static int cycleContrastLevel() {
		contrastLevel = nextLevel(contrastLevel);
		return contrastLevel;
	}

	static int setDimnessLevel(int level) {
		dimnessLevel = clampLevel(level);
		return dimnessLevel;
	}

	static int setContrastLevel(int level) {
		contrastLevel = clampLevel(level);
		return contrastLevel;
	}

	static float getDimnessMultiplier() {
		return dimnessMultiplierForLevel(dimnessLevel);
	}

	static float getContrastMultiplier() {
		return contrastMultiplierForLevel(contrastLevel);
	}

	static void loadFromClientSettings(Properties props) {
		if (props == null) {
			return;
		}
		String scaleVersion = props.getProperty(SCALE_VERSION_PROPERTY_KEY);
		boolean centeredScale = CENTERED_SCALE_VERSION.equals(scaleVersion);
		if (centeredScale) {
			dimnessLevel = readSavedLevel(props, DIMNESS_LEVEL_PROPERTY_KEY, CENTER_LEVEL);
			contrastLevel = readSavedLevel(props, CONTRAST_LEVEL_PROPERTY_KEY, CENTER_LEVEL);
		} else if (PREVIOUS_CENTERED_SCALE_VERSION.equals(scaleVersion)) {
			dimnessLevel = closestLevel(previousCenteredDimnessMultiplier(
				readPreviousCenteredLevel(props, DIMNESS_LEVEL_PROPERTY_KEY)), true);
			contrastLevel = closestLevel(previousCenteredContrastMultiplier(
				readPreviousCenteredLevel(props, CONTRAST_LEVEL_PROPERTY_KEY)), false);
		} else {
			dimnessLevel = closestDimnessLevel(readLegacyLevel(props, DIMNESS_LEVEL_PROPERTY_KEY));
			contrastLevel = closestContrastLevel(readLegacyLevel(props, CONTRAST_LEVEL_PROPERTY_KEY));
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(SCALE_VERSION_PROPERTY_KEY, CENTERED_SCALE_VERSION);
			props.setProperty(DIMNESS_LEVEL_PROPERTY_KEY, String.valueOf(dimnessLevel));
			props.setProperty(CONTRAST_LEVEL_PROPERTY_KEY, String.valueOf(contrastLevel));
		}
	}

	static void resetDefaults() {
		dimnessLevel = CENTER_LEVEL;
		contrastLevel = CENTER_LEVEL;
	}

	static String debugSummary() {
		return "dim " + dimnessLevel + " (" + getDimnessMultiplier() + ")"
			+ " | contrast " + contrastLevel + " (" + getContrastMultiplier() + ")";
	}

	private static int nextLevel(int level) {
		return level >= MAX_LEVEL ? MIN_LEVEL : level + 1;
	}

	private static int clampLevel(int level) {
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
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

	private static int readLegacyLevel(Properties props, String key) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return 1;
		}
		try {
			return Math.max(1, Math.min(EXTENDED_LEGACY_MAX_LEVEL, Integer.parseInt(value.trim())));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private static int readPreviousCenteredLevel(Properties props, String key) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return 11;
		}
		try {
			return Math.max(1, Math.min(21, Integer.parseInt(value.trim())));
		} catch (NumberFormatException ignored) {
			return 11;
		}
	}

	private static float legacyDimnessMultiplier(int level) {
		int boundedLevel = Math.max(1, Math.min(EXTENDED_LEGACY_MAX_LEVEL, level));
		if (boundedLevel <= LEGACY_MAX_LEVEL) {
			return 1.0f - (boundedLevel - 1) * 0.05f;
		}
		return 0.55f * (float) Math.pow(0.9f, boundedLevel - LEGACY_MAX_LEVEL);
	}

	private static float legacyContrastMultiplier(int level) {
		int boundedLevel = Math.max(1, Math.min(EXTENDED_LEGACY_MAX_LEVEL, level));
		return 1.2f + (boundedLevel - 1) * 0.1f;
	}

	private static float previousCenteredDimnessMultiplier(int level) {
		int boundedLevel = Math.max(1, Math.min(21, level));
		if (boundedLevel <= 11) {
			return 1.0f + (11 - boundedLevel) * 0.05f;
		}
		float darkEndpoint = legacyDimnessMultiplier(EXTENDED_LEGACY_MAX_LEVEL);
		float progress = (boundedLevel - 11) / 10.0f;
		return (float) Math.pow(darkEndpoint, progress);
	}

	private static float previousCenteredContrastMultiplier(int level) {
		int boundedLevel = Math.max(1, Math.min(21, level));
		return boundedLevel <= 11
			? 1.2f - (11 - boundedLevel) * 0.09f
			: 1.2f + (boundedLevel - 11) * 0.19f;
	}

	private static int closestDimnessLevel(int legacyLevel) {
		return closestLevel(legacyDimnessMultiplier(legacyLevel), true);
	}

	private static int closestContrastLevel(int legacyLevel) {
		return closestLevel(legacyContrastMultiplier(legacyLevel), false);
	}

	private static int closestLevel(float target, boolean dimness) {
		int closest = CENTER_LEVEL;
		float closestDistance = Float.MAX_VALUE;
		for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
			float value = dimness ? dimnessMultiplierForLevel(level) : contrastMultiplierForLevel(level);
			float distance = Math.abs(value - target);
			if (distance < closestDistance) {
				closest = level;
				closestDistance = distance;
			}
		}
		return closest;
	}

	private static float dimnessMultiplierForLevel(int level) {
		int boundedLevel = clampLevel(level);
		if (boundedLevel <= CENTER_LEVEL) {
			float progress = (CENTER_LEVEL - boundedLevel) / (float) (CENTER_LEVEL - MIN_LEVEL);
			return 1.0f + progress * 0.5f;
		}
		float darkEndpoint = legacyDimnessMultiplier(EXTENDED_LEGACY_MAX_LEVEL);
		float progress = (boundedLevel - CENTER_LEVEL) / (float) (MAX_LEVEL - CENTER_LEVEL);
		return (float) Math.pow(darkEndpoint, progress);
	}

	private static float contrastMultiplierForLevel(int level) {
		int boundedLevel = clampLevel(level);
		if (boundedLevel <= CENTER_LEVEL) {
			float progress = (CENTER_LEVEL - boundedLevel) / (float) (CENTER_LEVEL - MIN_LEVEL);
			return 1.2f - progress * 0.9f;
		}
		float progress = (boundedLevel - CENTER_LEVEL) / (float) (MAX_LEVEL - CENTER_LEVEL);
		return 1.2f + progress * 1.9f;
	}
}
