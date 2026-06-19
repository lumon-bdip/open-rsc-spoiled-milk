package orsc;

import java.awt.Dimension;
import java.util.Properties;

final class RenderSurfaceSettings {
	static final String SURFACE_MODE_PROPERTY_KEY = "render_surface_mode";
	private static final String SURFACE_MODE_PROPERTY = "spoiledmilk.renderSurfaceMode";
	private static final String SURFACE_MODE_ENV = "SPOILED_MILK_RENDER_SURFACE_MODE";

	private static final boolean runtimeSurfaceModeOverride =
		hasRuntimeSetting(SURFACE_MODE_PROPERTY, SURFACE_MODE_ENV);
	private static volatile Mode mode = Mode.from(readRuntimeSetting(SURFACE_MODE_PROPERTY, SURFACE_MODE_ENV));

	private RenderSurfaceSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static int getWidth() {
		return mode.width;
	}

	static int getHeight() {
		return mode.height;
	}

	static Dimension getDimensions() {
		return new Dimension(mode.width, mode.height);
	}

	static Mode cycleMode() {
		Mode next = mode.next();
		mode = next;
		return next;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeSurfaceModeOverride || props == null) {
			return;
		}
		String configuredMode = props.getProperty(SURFACE_MODE_PROPERTY_KEY);
		if (configuredMode != null && !configuredMode.trim().isEmpty()) {
			mode = Mode.from(configuredMode);
		}
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(SURFACE_MODE_PROPERTY_KEY, mode.id);
		}
	}

	private static boolean hasRuntimeSetting(String propertyName, String envName) {
		return !readRuntimeSetting(propertyName, envName).isEmpty();
	}

	private static String readRuntimeSetting(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		return value == null ? "" : value.trim();
	}

	enum Mode {
		CLASSIC("512x346", "@gre@512x346 Classic", 512, 346),
		VGA("640x480", "@yel@640x480", 640, 480),
		SVGA("800x600", "@ora@800x600", 800, 600),
		WIDE("960x540", "@yel@960x540 16:9", 960, 540),
		WIDE_PLUS("1024x576", "@ora@1024x576 16:9", 1024, 576),
		HD("1280x720", "@gre@1280x720 16:9", 1280, 720);

		final String id;
		final String label;
		final int width;
		final int height;

		Mode(String id, String label, int width, int height) {
			this.id = id;
			this.label = label;
			this.width = width;
			this.height = height;
		}

		Mode next() {
			Mode[] modes = values();
			return modes[(ordinal() + 1) % modes.length];
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return CLASSIC;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("classic".equals(normalized) || "512-346".equals(normalized)) {
				return CLASSIC;
			}
			if ("720p".equals(normalized) || "hd".equals(normalized) || "1280-720".equals(normalized)) {
				return HD;
			}

			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown render surface mode '" + value + "'; using 512x346.");
			return CLASSIC;
		}
	}
}
