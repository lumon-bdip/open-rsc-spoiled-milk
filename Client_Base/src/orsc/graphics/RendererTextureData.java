package orsc.graphics;

import com.openrsc.client.model.Sprite;

import java.nio.ByteBuffer;

public final class RendererTextureData {
	private final int width;
	private final int height;
	private final int[] rgbaPixels;
	private final boolean hasTransparency;
	private final int opaquePixelCount;
	private final int transparentPixelCount;

	private RendererTextureData(
		int width,
		int height,
		int[] rgbaPixels,
		boolean hasTransparency,
		int opaquePixelCount,
		int transparentPixelCount) {
		this.width = width;
		this.height = height;
		this.rgbaPixels = rgbaPixels;
		this.hasTransparency = hasTransparency;
		this.opaquePixelCount = opaquePixelCount;
		this.transparentPixelCount = transparentPixelCount;
	}

	public static RendererTextureData fromSprite(Sprite sprite) {
		return fromSprite(sprite, RendererSpriteTransform.IDENTITY);
	}

	public static RendererTextureData fromSprite(Sprite sprite, RendererSpriteTransform transform) {
		return fromSprite(sprite, transform, true);
	}

	public static RendererTextureData fromOpaqueSprite(Sprite sprite) {
		return fromSprite(sprite, RendererSpriteTransform.IDENTITY, false);
	}

	private static RendererTextureData fromSprite(
		Sprite sprite,
		RendererSpriteTransform transform,
		boolean transparentZero) {
		if (sprite == null) {
			throw new IllegalArgumentException("sprite must not be null");
		}
		if (transform == null) {
			transform = RendererSpriteTransform.IDENTITY;
		}

		int[] sourcePixels = sprite.getPixels();
		int width = sprite.getWidth();
		int height = sprite.getHeight();
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("sprite dimensions must be positive");
		}
		int pixelCount = width * height;
		if (sourcePixels == null || sourcePixels.length < pixelCount) {
			throw new IllegalArgumentException("sprite pixel buffer is smaller than width * height");
		}

		int[] rgbaPixels = new int[pixelCount];
		int transparentPixels = 0;
		int opaquePixels = 0;
		for (int i = 0; i < pixelCount; i++) {
			int rgba;
			if (!transparentZero) {
				rgba = (sourcePixels[i] & RendererTransparency.RGB_MASK) << 8 | 0xFF;
			} else if (RendererTransparency.isVisibleSpritePixel(sourcePixels[i])) {
				rgba = ((transform.apply(sourcePixels[i]) & RendererTransparency.RGB_MASK) << 8) | 0xFF;
			} else {
				rgba = 0;
			}
			rgbaPixels[i] = rgba;
			if (rgba == 0) {
				transparentPixels++;
			} else {
				opaquePixels++;
			}
		}

		return new RendererTextureData(
			width,
			height,
			rgbaPixels,
			transparentPixels > 0,
			opaquePixels,
			transparentPixels);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int[] getRgbaPixels() {
		return rgbaPixels;
	}

	public boolean hasTransparency() {
		return hasTransparency;
	}

	public int getOpaquePixelCount() {
		return opaquePixelCount;
	}

	public int getTransparentPixelCount() {
		return transparentPixelCount;
	}

	public ByteBuffer copyToDirectRgbaBuffer() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(rgbaPixels.length * 4);
		for (int rgba : rgbaPixels) {
			buffer.putInt(rgba);
		}
		buffer.flip();
		return buffer;
	}
}
