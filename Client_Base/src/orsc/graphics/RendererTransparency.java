package orsc.graphics;

/**
 * Shared transparency rules for the legacy software renderer and renderer-v2.
 *
 * The old client uses color keys rather than explicit alpha for most sprites
 * and textures. Keeping those rules named here prevents the OpenGL path from
 * rediscovering magic zero/black behavior in each renderer stage.
 */
public final class RendererTransparency {
	public static final int RGB_MASK = 0x00FFFFFF;
	public static final int TRANSPARENT_SAMPLE = 0;
	public static final int OPAQUE_BLACK_REPLACEMENT = 1;
	public static final int LEGACY_TEXTURE_COLOR_MASK = 16316671;
	public static final int LEGACY_TEXTURE_TRANSPARENT_KEY = 16253183;

	private RendererTransparency() {
	}

	public static boolean isVisibleSpritePixel(int pixel) {
		return pixel != TRANSPARENT_SAMPLE;
	}

	public static boolean isVisibleTextureSample(int sample) {
		return sample != TRANSPARENT_SAMPLE;
	}

	public static boolean isLegacyTextureTransparentKey(int color) {
		return color == LEGACY_TEXTURE_TRANSPARENT_KEY;
	}

	public static int normalizeLegacyTexturePaletteColor(int color) {
		int normalized = color & LEGACY_TEXTURE_COLOR_MASK;
		if (isLegacyTextureTransparentKey(normalized)) {
			return TRANSPARENT_SAMPLE;
		}
		if (normalized == TRANSPARENT_SAMPLE) {
			return OPAQUE_BLACK_REPLACEMENT;
		}
		return normalized;
	}

	public static int normalizeLegacyTextureResourceColor(int color) {
		int normalized = color & LEGACY_TEXTURE_COLOR_MASK;
		if (normalized == TRANSPARENT_SAMPLE || isLegacyTextureTransparentKey(normalized)) {
			return TRANSPARENT_SAMPLE;
		}
		return normalized;
	}

	public static int spritePixelToRgba8888(int pixel) {
		if (!isVisibleSpritePixel(pixel)) {
			return 0;
		}
		return ((pixel & RGB_MASK) << 8) | 0xFF;
	}
}
