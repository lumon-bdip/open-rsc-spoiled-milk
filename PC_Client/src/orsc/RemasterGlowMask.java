package orsc;

import java.nio.ByteBuffer;

final class RemasterGlowMask {
	final long signature;
	final int width;
	final int height;
	final int visiblePixels;
	final float minX;
	final float minZ;
	final float invSpanX;
	final float invSpanZ;
	private final ByteBuffer pixels;

	RemasterGlowMask(
		long signature,
		int width,
		int height,
		int visiblePixels,
		float minX,
		float minZ,
		float invSpanX,
		float invSpanZ,
		ByteBuffer pixels) {
		this.signature = signature;
		this.width = width;
		this.height = height;
		this.visiblePixels = visiblePixels;
		this.minX = minX;
		this.minZ = minZ;
		this.invSpanX = invSpanX;
		this.invSpanZ = invSpanZ;
		this.pixels = pixels;
	}

	ByteBuffer pixels() {
		ByteBuffer duplicate = pixels.duplicate();
		duplicate.rewind();
		return duplicate;
	}
}
