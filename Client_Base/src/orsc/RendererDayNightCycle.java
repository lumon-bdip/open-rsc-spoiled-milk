package orsc;

final class RendererDayNightCycle {
	private static final int DEFAULT_CYCLE_MILLIS = 60 * 60 * 1000;
	private static final int MIN_CYCLE_MILLIS = 60 * 1000;
	private static volatile int syncedCycleMillis = DEFAULT_CYCLE_MILLIS;
	private static volatile int syncedCyclePositionMillis = -1;
	private static volatile int syncedRateMultiplier = 1;
	private static volatile long syncedAtClientMillis = 0L;

	private RendererDayNightCycle() {
	}

	static void syncServerTime(int cycleMillis, int currentCycleMillis) {
		syncServerTime(cycleMillis, currentCycleMillis, 1);
	}

	static void syncServerTime(int cycleMillis, int currentCycleMillis, int rateMultiplier) {
		if (cycleMillis < MIN_CYCLE_MILLIS) {
			return;
		}
		syncedCycleMillis = cycleMillis;
		syncedCyclePositionMillis = Math.floorMod(currentCycleMillis, cycleMillis);
		syncedRateMultiplier = clampRateMultiplier(rateMultiplier);
		syncedAtClientMillis = System.currentTimeMillis();
	}

	static Presentation currentPresentation() {
		RendererToneSettings.Mode toneMode = RendererToneSettings.getMode();
		if (toneMode != RendererToneSettings.Mode.CYCLE) {
			return Presentation.fromMode(
				toneMode,
				RendererBrightnessSettings.getMode().multiplier,
				toneMode.id);
		}

		CyclePosition cycle = currentCyclePosition();
		long cycleMillis = cycle.positionMillis;
		long dawnEndMillis = cycle.cycleMillis * 5L / 60L;
		long dayEndMillis = cycle.cycleMillis * 30L / 60L;
		long duskEndMillis = cycle.cycleMillis * 35L / 60L;
		long nightCoolInEndMillis = cycle.cycleMillis * 375L / 600L;
		long nightDeepEndMillis = cycle.cycleMillis * 575L / 600L;
		float savedBrightness = RendererBrightnessSettings.getMode().multiplier;
		if (cycleMillis < dawnEndMillis) {
			float t = fraction(cycleMillis, 0L, dawnEndMillis);
			return new Presentation(
				morningTone(t),
				transitionBrightness(),
				"dawn");
		}
		if (cycleMillis < dayEndMillis) {
			return Presentation.fromMode(RendererToneSettings.Mode.DAY, savedBrightness, "day");
		}
		if (cycleMillis < duskEndMillis) {
			float t = fraction(cycleMillis, dayEndMillis, duskEndMillis);
			return new Presentation(
				eveningTone(t),
				transitionBrightness(),
				"dusk");
		}
		if (cycleMillis < nightCoolInEndMillis) {
			return Presentation.fromMode(RendererToneSettings.Mode.COOL_NIGHT, savedBrightness, "night-cool");
		}
		if (cycleMillis < nightDeepEndMillis) {
			return Presentation.fromMode(RendererToneSettings.Mode.DEEP_BLUE, savedBrightness, "night-deep");
		}
		return Presentation.fromMode(RendererToneSettings.Mode.COOL_NIGHT, savedBrightness, "night-cool");
	}

	static float currentBrightnessMultiplier() {
		return currentPresentation().brightnessMultiplier;
	}

	static float currentCycleFraction() {
		CyclePosition cycle = currentCyclePosition();
		return cycle.positionMillis / (float) Math.max(1, cycle.cycleMillis);
	}

	static String debugSummary() {
		Presentation presentation = currentPresentation();
		if (RendererToneSettings.getMode() != RendererToneSettings.Mode.CYCLE) {
			return presentation.debugLabel;
		}
		CyclePosition cycle = currentCyclePosition();
		long minute = cycle.positionMillis / 60000L;
		long second = (cycle.positionMillis / 1000L) % 60L;
		return (cycle.serverSynced ? "server-cycle:" : "local-cycle:")
			+ presentation.debugLabel + " "
			+ twoDigits(minute) + ":" + twoDigits(second)
			+ (cycle.rateMultiplier > 1 ? " x" + cycle.rateMultiplier : "");
	}

	private static CyclePosition currentCyclePosition() {
		int cycleMillis = syncedCycleMillis;
		int cyclePositionMillis = syncedCyclePositionMillis;
		long syncedAtMillis = syncedAtClientMillis;
		int rateMultiplier = syncedRateMultiplier;
		long now = System.currentTimeMillis();
		if (cyclePositionMillis >= 0 && cycleMillis >= MIN_CYCLE_MILLIS) {
			long elapsedMillis = Math.max(0L, now - syncedAtMillis) * (long) clampRateMultiplier(rateMultiplier);
			return new CyclePosition(
				cycleMillis,
				Math.floorMod((long) cyclePositionMillis + elapsedMillis, (long) cycleMillis),
				true,
				rateMultiplier);
		}
		return new CyclePosition(
			DEFAULT_CYCLE_MILLIS,
			Math.floorMod(now, (long) DEFAULT_CYCLE_MILLIS),
			false,
			1);
	}

	private static int clampRateMultiplier(int rateMultiplier) {
		if (rateMultiplier < 1) {
			return 1;
		}
		return Math.min(rateMultiplier, 3600);
	}

	private static String twoDigits(long value) {
		return value < 10L ? "0" + value : Long.toString(value);
	}

	private static float transitionBrightness() {
		switch (RendererBrightnessSettings.getMode()) {
			case HIGH:
				return RendererBrightnessSettings.Mode.MEDIUM.multiplier;
			case MEDIUM:
				return RendererBrightnessSettings.Mode.LOW.multiplier;
			case LOW:
			default:
				return 0.7f;
		}
	}

	private static RendererToneSettings.ToneValues morningTone(float t) {
		if (t < 0.2f) {
			return mix(
				RendererToneSettings.Mode.COOL_NIGHT,
				RendererToneSettings.Mode.SUNRISE_AMBER,
				t / 0.2f);
		}
		if (t > 0.8f) {
			return mix(
				RendererToneSettings.Mode.SUNRISE_AMBER,
				RendererToneSettings.Mode.DAY,
				(t - 0.8f) / 0.2f);
		}
		return RendererToneSettings.Mode.SUNRISE_AMBER.toneValues();
	}

	private static RendererToneSettings.ToneValues eveningTone(float t) {
		if (t < 0.2f) {
			return mix(
				RendererToneSettings.Mode.DAY,
				RendererToneSettings.Mode.ROSE_DUSK,
				t / 0.2f);
		}
		if (t > 0.8f) {
			return mix(
				RendererToneSettings.Mode.ROSE_DUSK,
				RendererToneSettings.Mode.COOL_NIGHT,
				(t - 0.8f) / 0.2f);
		}
		return RendererToneSettings.Mode.ROSE_DUSK.toneValues();
	}

	private static RendererToneSettings.ToneValues mix(
		RendererToneSettings.Mode from,
		RendererToneSettings.Mode to,
		float t) {
		return RendererToneSettings.ToneValues.mix(from.toneValues(), to.toneValues(), clamp01(t));
	}

	private static float fraction(long value, long start, long end) {
		return clamp01((value - start) / (float) Math.max(1L, end - start));
	}

	private static float clamp01(float value) {
		if (value <= 0.0f) {
			return 0.0f;
		}
		if (value >= 1.0f) {
			return 1.0f;
		}
		return value;
	}

	static final class Presentation {
		final float redMultiplier;
		final float greenMultiplier;
		final float blueMultiplier;
		final float toneBlend;
		final float brightnessMultiplier;
		final String debugLabel;

		Presentation(
			RendererToneSettings.ToneValues toneValues,
			float brightnessMultiplier,
			String debugLabel) {
			this.redMultiplier = toneValues.redMultiplier;
			this.greenMultiplier = toneValues.greenMultiplier;
			this.blueMultiplier = toneValues.blueMultiplier;
			this.toneBlend = toneValues.blend;
			this.brightnessMultiplier = brightnessMultiplier;
			this.debugLabel = debugLabel;
		}

		static Presentation fromMode(
			RendererToneSettings.Mode mode,
			float brightnessMultiplier,
			String debugLabel) {
			return new Presentation(mode.toneValues(), brightnessMultiplier, debugLabel);
		}
	}

	private static final class CyclePosition {
		final int cycleMillis;
		final long positionMillis;
		final boolean serverSynced;
		final int rateMultiplier;

		CyclePosition(int cycleMillis, long positionMillis, boolean serverSynced, int rateMultiplier) {
			this.cycleMillis = cycleMillis;
			this.positionMillis = positionMillis;
			this.serverSynced = serverSynced;
			this.rateMultiplier = rateMultiplier;
		}
	}
}
