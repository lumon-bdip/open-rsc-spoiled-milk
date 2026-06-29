package orsc;

import orsc.graphics.Renderer2DFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class OpenGLGlyphTextureCache implements AutoCloseable {
	private static final int DEFAULT_ATLAS_SIZE = 512;

	private final LwjglBindings gl;
	private final Map<GlyphTextureKey, OpenGLTextureRegion> glyphRegions = new HashMap<>();
	private final List<OpenGLDynamicTextureAtlas> atlases = new ArrayList<>();

	OpenGLGlyphTextureCache(LwjglBindings gl) {
		this.gl = gl;
	}

	OpenGLTextureRegion getOrUpload(Renderer2DFrame.TextCommand.GlyphCommand glyph) throws Exception {
		GlyphTextureKey key = new GlyphTextureKey(glyph);
		OpenGLTextureRegion region = glyphRegions.get(key);
		if (region != null) {
			return region;
		}

		DynamicTextureData textureData = createGlyphTextureData(glyph);
		for (OpenGLDynamicTextureAtlas atlas : atlases) {
			region = atlas.upload(textureData);
			if (region != null) {
				glyphRegions.put(key, region);
				return region;
			}
		}

		OpenGLDynamicTextureAtlas atlas = OpenGLDynamicTextureAtlas.create(gl, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
		atlases.add(atlas);
		region = atlas.upload(textureData);
		if (region == null) {
			throw new IllegalArgumentException(
				"glyph is too large for OpenGL atlas: "
				+ glyph.getWidth()
				+ "x"
				+ glyph.getHeight());
		}
		glyphRegions.put(key, region);
		OpenGLRendererLog.log("OpenGL glyph atlas allocated: "
			+ DEFAULT_ATLAS_SIZE
			+ "x"
			+ DEFAULT_ATLAS_SIZE
			+ " texture="
			+ atlas.getTextureId());
		return region;
	}

	private static DynamicTextureData createGlyphTextureData(Renderer2DFrame.TextCommand.GlyphCommand glyph) {
		int width = glyph.getWidth();
		int height = glyph.getHeight();
		byte[] fontData = glyph.getFontData();
		int dataAddress = glyph.getDataAddress();
		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
		int visiblePixelCount = 0;
		for (int row = 0; row < height; row++) {
			int rowAddress = dataAddress + row * width;
			for (int column = 0; column < width; column++) {
				int coverage = fontData[rowAddress + column] & 0xFF;
				int alpha = glyph.isAntiAliased() ? coverage : (coverage == 0 ? 0 : 255);
				if (alpha != 0) {
					visiblePixelCount++;
				}
				pixels.putInt((0x00FFFFFF << 8) | alpha);
			}
		}
		pixels.flip();
		return new DynamicTextureData(width, height, pixels, visiblePixelCount);
	}

	@Override
	public void close() throws Exception {
		for (OpenGLDynamicTextureAtlas atlas : atlases) {
			atlas.close();
		}
		atlases.clear();
		glyphRegions.clear();
	}

	private static final class GlyphTextureKey {
		private final byte[] fontData;
		private final int fontDataIdentityHash;
		private final int dataAddress;
		private final int width;
		private final int height;
		private final boolean antiAliased;

		private GlyphTextureKey(Renderer2DFrame.TextCommand.GlyphCommand glyph) {
			this.fontData = glyph.getFontData();
			this.fontDataIdentityHash = System.identityHashCode(fontData);
			this.dataAddress = glyph.getDataAddress();
			this.width = glyph.getWidth();
			this.height = glyph.getHeight();
			this.antiAliased = glyph.isAntiAliased();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof GlyphTextureKey)) {
				return false;
			}
			GlyphTextureKey key = (GlyphTextureKey) other;
			return fontData == key.fontData
				&& dataAddress == key.dataAddress
				&& width == key.width
				&& height == key.height
				&& antiAliased == key.antiAliased;
		}

		@Override
		public int hashCode() {
			int result = fontDataIdentityHash;
			result = 31 * result + dataAddress;
			result = 31 * result + width;
			result = 31 * result + height;
			result = 31 * result + (antiAliased ? 1 : 0);
			return result;
		}
	}
}
