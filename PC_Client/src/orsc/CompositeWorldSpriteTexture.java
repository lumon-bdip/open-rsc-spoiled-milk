package orsc;

final class CompositeWorldSpriteTexture {
	final int x;
	final int y;
	final int width;
	final int height;
	final DynamicTextureData textureData;

	CompositeWorldSpriteTexture(
		int x,
		int y,
		int width,
		int height,
		DynamicTextureData textureData) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.textureData = textureData;
	}
}
