package orsc;

import java.util.Properties;

final class RendererToneSettings {
	static final String TONE_PROPERTY_KEY = "opengl_tone_preview";
	private static final String TONE_PROPERTY = "spoiledmilk.openglTonePreview";
	private static final String TONE_ENV = "SPOILED_MILK_OPENGL_TONE_PREVIEW";

	private static final boolean runtimeToneOverride =
		hasRuntimeSetting(TONE_PROPERTY, TONE_ENV);
	private static volatile Mode mode = runtimeToneOverride
		? Mode.from(readRuntimeSetting(TONE_PROPERTY, TONE_ENV))
		: Mode.CYCLE;

	private RendererToneSettings() {
	}

	static Mode getMode() {
		return mode;
	}

	static Mode setMode(Mode next) {
		mode = next == null ? Mode.DAY : next;
		return mode;
	}

	static void loadFromClientSettings(Properties props) {
		if (runtimeToneOverride) {
			return;
		}
		mode = RendererProfileSettings.getMode() == RendererProfileSettings.Mode.CLASSIC
			? Mode.DAY
			: Mode.CYCLE;
	}

	static void saveToClientSettings(Properties props) {
		if (props != null) {
			props.setProperty(TONE_PROPERTY_KEY, mode.id);
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
		DAY("day", "@gre@Day", 1.0f, 1.0f, 1.0f, 0.0f),
		CYCLE("cycle", "@cya@Cycle", 1.0f, 1.0f, 1.0f, 0.0f),
		SUNRISE_AMBER("sunrise-amber", "@ora@Sunrise Amber", 1.26f, 0.96f, 0.64f, 0.56f),
		DAWN_GOLD("dawn-gold", "@yel@Dawn Gold", 1.20f, 0.96f, 0.62f, 0.46f),
		ROSE_DUSK("rose-dusk", "@red@Rose Dusk", 1.26f, 0.70f, 0.84f, 0.52f),
		COOL_NIGHT("cool-night", "@cya@Cool Night", 0.72f, 0.84f, 1.18f, 0.44f),
		DEEP_BLUE("deep-blue", "@blu@Deep Blue", 0.62f, 0.76f, 1.24f, 0.50f);

		final String id;
		final String label;
		final float redMultiplier;
		final float greenMultiplier;
		final float blueMultiplier;
		final float blend;

		Mode(
			String id,
			String label,
			float redMultiplier,
			float greenMultiplier,
			float blueMultiplier,
			float blend) {
			this.id = id;
			this.label = label;
			this.redMultiplier = redMultiplier;
			this.greenMultiplier = greenMultiplier;
			this.blueMultiplier = blueMultiplier;
			this.blend = blend;
		}

		float effectiveRedMultiplier() {
			return effectiveMultiplier(redMultiplier);
		}

		float effectiveGreenMultiplier() {
			return effectiveMultiplier(greenMultiplier);
		}

		float effectiveBlueMultiplier() {
			return effectiveMultiplier(blueMultiplier);
		}

		ToneValues toneValues() {
			return new ToneValues(
				effectiveRedMultiplier(),
				effectiveGreenMultiplier(),
				effectiveBlueMultiplier(),
				1.0f);
		}

		private float effectiveMultiplier(float multiplier) {
			return 1.0f + (multiplier - 1.0f) * blend;
		}

		static Mode from(String value) {
			if (value == null || value.trim().isEmpty()) {
				return DAY;
			}

			String normalized = value.trim().toLowerCase().replace('_', '-');
			if ("off".equals(normalized) || "neutral".equals(normalized) || "none".equals(normalized)) {
				return DAY;
			}
			for (Mode mode : values()) {
				if (mode.id.equals(normalized)) {
					return mode;
				}
			}

			System.out.println("[renderer-v2] Unknown OpenGL tone preview '" + value + "'; using day.");
			return DAY;
		}
	}

	static final class ToneValues {
		final float redMultiplier;
		final float greenMultiplier;
		final float blueMultiplier;
		final float blend;

		ToneValues(float redMultiplier, float greenMultiplier, float blueMultiplier, float blend) {
			this.redMultiplier = redMultiplier;
			this.greenMultiplier = greenMultiplier;
			this.blueMultiplier = blueMultiplier;
			this.blend = blend;
		}

		static ToneValues mix(ToneValues from, ToneValues to, float t) {
			return new ToneValues(
				lerp(from.redMultiplier, to.redMultiplier, t),
				lerp(from.greenMultiplier, to.greenMultiplier, t),
				lerp(from.blueMultiplier, to.blueMultiplier, t),
				1.0f);
		}

		private static float lerp(float from, float to, float t) {
			return from + (to - from) * t;
		}
	}
}
