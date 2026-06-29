package orsc;

final class RendererRemasterLightSettings {
	private static final String AZIMUTH_PROPERTY = "spoiledmilk.remasterLightAzimuth";
	private static final String AZIMUTH_ENV = "SPOILED_MILK_REMASTER_LIGHT_AZIMUTH";
	private static final String ELEVATION_PROPERTY = "spoiledmilk.remasterLightElevation";
	private static final String ELEVATION_ENV = "SPOILED_MILK_REMASTER_LIGHT_ELEVATION";
	private static final String AMBIENT_PROPERTY = "spoiledmilk.remasterLightAmbient";
	private static final String AMBIENT_ENV = "SPOILED_MILK_REMASTER_LIGHT_AMBIENT";
	private static final String INTENSITY_PROPERTY = "spoiledmilk.remasterLightIntensity";
	private static final String INTENSITY_ENV = "SPOILED_MILK_REMASTER_LIGHT_INTENSITY";

	private static volatile float azimuthDegrees = normalizeDegrees(
		readFloat(AZIMUTH_PROPERTY, AZIMUTH_ENV, 135.0f));
	private static volatile float elevationDegrees = clamp(
		readFloat(ELEVATION_PROPERTY, ELEVATION_ENV, 45.0f),
		5.0f,
		85.0f);
	private static volatile float ambient = clamp(
		readFloat(AMBIENT_PROPERTY, AMBIENT_ENV, 0.46f),
		0.0f,
		1.0f);
	private static volatile float intensity = clamp(
		readFloat(INTENSITY_PROPERTY, INTENSITY_ENV, 0.78f),
		0.0f,
		2.0f);

	private RendererRemasterLightSettings() {
	}

	static float getAzimuthDegrees() {
		return azimuthDegrees;
	}

	static float getElevationDegrees() {
		return elevationDegrees;
	}

	static float getAmbient() {
		return ambient;
	}

	static float getIntensity() {
		return intensity;
	}

	static float adjustAzimuth(float deltaDegrees) {
		azimuthDegrees = normalizeDegrees(azimuthDegrees + deltaDegrees);
		return azimuthDegrees;
	}

	static float adjustElevation(float deltaDegrees) {
		elevationDegrees = clamp(elevationDegrees + deltaDegrees, 5.0f, 85.0f);
		return elevationDegrees;
	}

	static float getLightDirectionX() {
		double azimuth = Math.toRadians(getAzimuthDegrees());
		double elevation = Math.toRadians(getElevationDegrees());
		return (float) (Math.cos(elevation) * Math.cos(azimuth));
	}

	static float getLightDirectionY() {
		return (float) Math.sin(Math.toRadians(getElevationDegrees()));
	}

	static float getLightDirectionZ() {
		double azimuth = Math.toRadians(getAzimuthDegrees());
		double elevation = Math.toRadians(getElevationDegrees());
		return (float) (Math.cos(elevation) * Math.sin(azimuth));
	}

	static String debugSummary() {
		return "az " + Math.round(getAzimuthDegrees())
			+ " elev " + Math.round(getElevationDegrees())
			+ " amb " + roundedPercent(getAmbient())
			+ " int " + roundedPercent(getIntensity());
	}

	private static int roundedPercent(float value) {
		return Math.round(value * 100.0f);
	}

	private static float normalizeDegrees(float degrees) {
		float normalized = degrees % 360.0f;
		return normalized < 0.0f ? normalized + 360.0f : normalized;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float readFloat(String propertyName, String envName, float defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(value.trim());
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}
}
