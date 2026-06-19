package orsc.graphics.three;

final class Renderer3DSettings {
	private static final String GEOMETRY_CAPTURE_PROPERTY = "spoiledmilk.renderer3DGeometryCapture";
	private static final String GEOMETRY_CAPTURE_ENV = "SPOILED_MILK_RENDERER3D_GEOMETRY_CAPTURE";
	private static final String VISIBLE_WORLD_PROPERTY = "spoiledmilk.renderer3DVisibleWorld";
	private static final String VISIBLE_WORLD_ENV = "SPOILED_MILK_RENDERER3D_VISIBLE_WORLD";
	private static final boolean VISIBLE_WORLD_ENABLED =
		readBoolean(VISIBLE_WORLD_PROPERTY, VISIBLE_WORLD_ENV);
	private static final boolean GEOMETRY_CAPTURE_ENABLED =
		VISIBLE_WORLD_ENABLED || readBoolean(GEOMETRY_CAPTURE_PROPERTY, GEOMETRY_CAPTURE_ENV);

	private Renderer3DSettings() {
	}

	static boolean isGeometryCaptureEnabled() {
		return GEOMETRY_CAPTURE_ENABLED;
	}

	static boolean isVisibleWorldEnabled() {
		return VISIBLE_WORLD_ENABLED;
	}

	private static boolean readBoolean(String propertyName, String environmentName) {
		String property = System.getProperty(propertyName);
		if (property != null) {
			return Boolean.parseBoolean(property);
		}
		String environment = System.getenv(environmentName);
		return environment != null && Boolean.parseBoolean(environment);
	}
}
