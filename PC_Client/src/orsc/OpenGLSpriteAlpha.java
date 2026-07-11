package orsc;

/*
 * Straight-alpha composition for CPU-built OpenGL bridge textures. OpenGL's
 * sprite passes use SRC_ALPHA / ONE_MINUS_SRC_ALPHA, so stored RGB channels
 * must remain unpremultiplied even when a character is partially transparent.
 */
final class OpenGLSpriteAlpha {
	private static final int FULL_ALPHA = 255;

	private OpenGLSpriteAlpha() {
	}

	static int blendStraightRgbaOver(int destinationRgba, int sourceRgb, int sourceAlpha) {
		int clampedSourceAlpha = clampAlpha(sourceAlpha);
		if (clampedSourceAlpha == 0) {
			return destinationRgba;
		}
		if (clampedSourceAlpha == FULL_ALPHA) {
			return (sourceRgb << 8) | FULL_ALPHA;
		}

		int destinationAlpha = destinationRgba & FULL_ALPHA;
		int destinationRgb = destinationRgba >>> 8;
		int inverseSourceAlpha = FULL_ALPHA - clampedSourceAlpha;
		int outputAlpha = clampedSourceAlpha
			+ divideByFullAlpha(destinationAlpha * inverseSourceAlpha);

		int red = blendStraightChannel(
			sourceRgb >>> 16 & FULL_ALPHA,
			destinationRgb >>> 16 & FULL_ALPHA,
			clampedSourceAlpha,
			destinationAlpha,
			inverseSourceAlpha,
			outputAlpha);
		int green = blendStraightChannel(
			sourceRgb >>> 8 & FULL_ALPHA,
			destinationRgb >>> 8 & FULL_ALPHA,
			clampedSourceAlpha,
			destinationAlpha,
			inverseSourceAlpha,
			outputAlpha);
		int blue = blendStraightChannel(
			sourceRgb & FULL_ALPHA,
			destinationRgb & FULL_ALPHA,
			clampedSourceAlpha,
			destinationAlpha,
			inverseSourceAlpha,
			outputAlpha);
		return (red << 24) | (green << 16) | (blue << 8) | outputAlpha;
	}

	private static int blendStraightChannel(
		int source,
		int destination,
		int sourceAlpha,
		int destinationAlpha,
		int inverseSourceAlpha,
		int outputAlpha) {
		int premultiplied = source * sourceAlpha
			+ divideByFullAlpha(destination * destinationAlpha * inverseSourceAlpha);
		return Math.min(FULL_ALPHA, (premultiplied + outputAlpha / 2) / outputAlpha);
	}

	private static int divideByFullAlpha(int value) {
		return (value + FULL_ALPHA / 2) / FULL_ALPHA;
	}

	private static int clampAlpha(int alpha) {
		return Math.max(0, Math.min(FULL_ALPHA, alpha));
	}
}
