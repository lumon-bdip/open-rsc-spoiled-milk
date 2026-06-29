package orsc;

import java.nio.ByteBuffer;

final class DynamicTextureData {
	final int width;
	final int height;
	private final ByteBuffer pixels;
	final int visiblePixelCount;

	DynamicTextureData(int width, int height, ByteBuffer pixels, int visiblePixelCount) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
		this.visiblePixelCount = visiblePixelCount;
	}

	ByteBuffer pixels() {
		ByteBuffer duplicate = pixels.duplicate();
		duplicate.position(0);
		duplicate.limit(width * height * 4);
		return duplicate;
	}
}
