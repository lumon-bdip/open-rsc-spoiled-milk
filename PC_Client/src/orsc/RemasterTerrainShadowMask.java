package orsc;

import java.nio.ByteBuffer;

final class RemasterTerrainShadowMask {
	final long signature;
	final int width;
	final int height;
	final int visiblePixels;
	final float minX;
	final float minZ;
	final float invSpanX;
	final float invSpanZ;
	private final ByteBuffer pixels;

	RemasterTerrainShadowMask(
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

	float u(float x) {
		return RemasterShadowMaskBuilder.clamp((x - minX) * invSpanX, 0.0f, 1.0f);
	}

	float v(float z) {
		return RemasterShadowMaskBuilder.clamp((z - minZ) * invSpanZ, 0.0f, 1.0f);
	}

	ByteBuffer pixels() {
		ByteBuffer duplicate = pixels.duplicate();
		duplicate.position(0);
		return duplicate;
	}
}
