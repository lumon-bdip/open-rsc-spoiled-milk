package orsc;

final class OpenGLTextureRegion {
	private final int textureId;
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private final float u0;
	private final float v0;
	private final float u1;
	private final float v1;
	private final boolean hasTransparency;

	OpenGLTextureRegion(
		int textureId,
		int x,
		int y,
		int width,
		int height,
		float u0,
		float v0,
		float u1,
		float v1,
		boolean hasTransparency) {
		this.textureId = textureId;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.u0 = u0;
		this.v0 = v0;
		this.u1 = u1;
		this.v1 = v1;
		this.hasTransparency = hasTransparency;
	}

	int getTextureId() {
		return textureId;
	}

	int getX() {
		return x;
	}

	int getY() {
		return y;
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	float getU0() {
		return u0;
	}

	float getV0() {
		return v0;
	}

	float getU1() {
		return u1;
	}

	float getV1() {
		return v1;
	}

	boolean hasTransparency() {
		return hasTransparency;
	}
}
