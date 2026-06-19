package orsc.graphics;

public final class RendererSpriteTransform {
	private static final int MODE_IDENTITY = 0;
	private static final int MODE_LEGACY_ONE_MASK = 1;
	private static final int MODE_LEGACY_TWO_MASKS = 2;
	private static final int DEFAULT_MASK = 0xFFFFFF;
	private static final int DEFAULT_COLOUR_TRANSFORM = 0xFFFFFFFF;

	public static final RendererSpriteTransform IDENTITY =
		new RendererSpriteTransform(MODE_IDENTITY, DEFAULT_MASK, DEFAULT_MASK, DEFAULT_MASK, DEFAULT_COLOUR_TRANSFORM);

	private final int mode;
	private final int mask1;
	private final int mask2;
	private final int blueMask;
	private final int colourTransform;

	private RendererSpriteTransform(int mode, int mask1, int mask2, int blueMask, int colourTransform) {
		this.mode = mode;
		this.mask1 = mask1 & RendererTransparency.RGB_MASK;
		this.mask2 = mask2 & RendererTransparency.RGB_MASK;
		this.blueMask = blueMask & RendererTransparency.RGB_MASK;
		this.colourTransform = colourTransform;
	}

	public static RendererSpriteTransform legacyMasks(
		int mask1,
		int mask2,
		int blueMask,
		int colourTransform) {
		int normalizedMask1 = normalizeMask(mask1);
		int normalizedMask2 = normalizeMask(mask2);
		int normalizedBlueMask = normalizeMask(blueMask);
		int mode = normalizedMask2 == DEFAULT_MASK ? MODE_LEGACY_ONE_MASK : MODE_LEGACY_TWO_MASKS;
		return new RendererSpriteTransform(
			mode,
			normalizedMask1,
			normalizedMask2,
			normalizedBlueMask,
			colourTransform);
	}

	public boolean canReplayOverSoftwareFrame() {
		return mode == MODE_IDENTITY || getOpacity() == 0xFF;
	}

	public int getOpacity() {
		return colourTransform >> 24 & 0xFF;
	}

	public int apply(int pixel) {
		if (!RendererTransparency.isVisibleSpritePixel(pixel)) {
			return RendererTransparency.TRANSPARENT_SAMPLE;
		}
		if (mode == MODE_IDENTITY) {
			return pixel & RendererTransparency.RGB_MASK;
		}

		int red = pixel >> 16 & 0xFF;
		int green = pixel >> 8 & 0xFF;
		int blue = pixel & 0xFF;

		if (red == green && green == blue) {
			red = red * (mask1 >> 16 & 0xFF) >> 8;
			green = green * (mask1 >> 8 & 0xFF) >> 8;
			blue = blue * (mask1 & 0xFF) >> 8;
		} else if (mode == MODE_LEGACY_TWO_MASKS && red == 255 && green == blue) {
			red = red * (mask2 >> 16 & 0xFF) >> 8;
			green = green * (mask2 >> 8 & 0xFF) >> 8;
			blue = blue * (mask2 & 0xFF) >> 8;
		} else if (blueMask != DEFAULT_MASK && red == green && blue != green) {
			int shifter = red * blue;
			red = ((blueMask >> 16 & 0xFF) * shifter) >> 16;
			green = ((blueMask >> 8 & 0xFF) * shifter) >> 16;
			blue = ((blueMask & 0xFF) * shifter) >> 16;
		}

		int transformRed = colourTransform >> 16 & 0xFF;
		int transformGreen = colourTransform >> 8 & 0xFF;
		int transformBlue = colourTransform & 0xFF;

		red = (red * transformRed) >> 8;
		green = (green * transformGreen) >> 8;
		blue = (blue * transformBlue) >> 8;
		return red << 16 | green << 8 | blue;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RendererSpriteTransform)) {
			return false;
		}
		RendererSpriteTransform transform = (RendererSpriteTransform) other;
		return mode == transform.mode
			&& mask1 == transform.mask1
			&& mask2 == transform.mask2
			&& blueMask == transform.blueMask
			&& colourTransform == transform.colourTransform;
	}

	@Override
	public int hashCode() {
		int result = mode;
		result = 31 * result + mask1;
		result = 31 * result + mask2;
		result = 31 * result + blueMask;
		result = 31 * result + colourTransform;
		return result;
	}

	private static int normalizeMask(int mask) {
		if (mask == 0) {
			return DEFAULT_MASK;
		}
		return mask & RendererTransparency.RGB_MASK;
	}
}
