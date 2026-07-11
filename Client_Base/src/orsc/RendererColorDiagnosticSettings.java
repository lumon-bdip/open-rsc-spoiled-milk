package orsc;

import java.util.Properties;

final class RendererColorDiagnosticSettings {
	static final String DIMNESS_LEVEL_PROPERTY_KEY = "opengl_dimness_level";
	static final String CONTRAST_LEVEL_PROPERTY_KEY = "opengl_contrast_level";
	private static volatile int dimnessLevel = 1;
	private static volatile int contrastLevel = 1;

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
		return 1.0f - (dimnessLevel - 1) * 0.05f;
	}

	static float getContrastMultiplier() {
		return 1.2f + (contrastLevel - 1) * 0.1f;
	}

	static void loadFromClientSettings(Properties props) {
		if (props == null) {
			return;
		}
		dimnessLevel = readSavedLevel(props, DIMNESS_LEVEL_PROPERTY_KEY, 1);
		contrastLevel = readSavedLevel(props, CONTRAST_LEVEL_PROPERTY_KEY, 1);
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(DIMNESS_LEVEL_PROPERTY_KEY, String.valueOf(dimnessLevel));
			props.setProperty(CONTRAST_LEVEL_PROPERTY_KEY, String.valueOf(contrastLevel));
		}
	}

	static void resetDefaults() {
		dimnessLevel = 1;
		contrastLevel = 1;
	}

	static String debugSummary() {
		return "dim " + dimnessLevel + " (" + getDimnessMultiplier() + ")"
			+ " | contrast " + contrastLevel + " (" + getContrastMultiplier() + ")";
	}

	private static int nextLevel(int level) {
		return level >= 10 ? 1 : level + 1;
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
}
