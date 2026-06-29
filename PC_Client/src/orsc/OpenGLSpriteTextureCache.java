package orsc;

import com.openrsc.client.model.Sprite;
import orsc.graphics.RendererSpriteTransform;
import orsc.graphics.RendererTextureData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class OpenGLSpriteTextureCache implements AutoCloseable {
	private static final int DEFAULT_ATLAS_SIZE = 2048;

	private final LwjglBindings gl;
	private final Map<SpriteTextureKey, OpenGLTextureRegion> spriteRegions = new HashMap<>();
	private final List<OpenGLTextureAtlas> atlases = new ArrayList<>();

	OpenGLSpriteTextureCache(LwjglBindings gl) {
		this.gl = gl;
	}

	OpenGLTextureRegion getOrUpload(Sprite sprite) throws Exception {
		return getOrUpload(sprite, RendererSpriteTransform.IDENTITY);
	}

	OpenGLTextureRegion getOrUpload(Sprite sprite, RendererSpriteTransform transform) throws Exception {
		return getOrUpload(sprite, transform, true);
	}

	OpenGLTextureRegion getOrUpload(
		Sprite sprite,
		RendererSpriteTransform transform,
		boolean transparentMask) throws Exception {
		SpriteTextureKey key = new SpriteTextureKey(sprite, transform, transparentMask);
		OpenGLTextureRegion region = spriteRegions.get(key);
		if (region != null) {
			return region;
		}

		RendererTextureData textureData = transparentMask
			? RendererTextureData.fromSprite(sprite, transform)
			: RendererTextureData.fromOpaqueSprite(sprite);
		for (OpenGLTextureAtlas atlas : atlases) {
			region = atlas.upload(textureData);
			if (region != null) {
				spriteRegions.put(key, region);
				return region;
			}
		}

		OpenGLTextureAtlas atlas = OpenGLTextureAtlas.create(gl, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
		atlases.add(atlas);
		region = atlas.upload(textureData);
		if (region == null) {
			throw new IllegalArgumentException(
				"sprite is too large for OpenGL atlas: "
				+ textureData.getWidth()
				+ "x"
				+ textureData.getHeight());
		}
		spriteRegions.put(key, region);
		OpenGLRendererLog.log("OpenGL sprite atlas allocated: "
			+ DEFAULT_ATLAS_SIZE
			+ "x"
			+ DEFAULT_ATLAS_SIZE
			+ " texture="
			+ atlas.getTextureId());
		return region;
	}

	@Override
	public void close() throws Exception {
		for (OpenGLTextureAtlas atlas : atlases) {
			atlas.close();
		}
		atlases.clear();
		spriteRegions.clear();
	}

	private static final class SpriteTextureKey {
		private final Sprite sprite;
		private final RendererSpriteTransform transform;
		private final boolean transparentMask;
		private final int spriteIdentityHash;

		private SpriteTextureKey(Sprite sprite, RendererSpriteTransform transform, boolean transparentMask) {
			this.sprite = sprite;
			this.transform = transform == null ? RendererSpriteTransform.IDENTITY : transform;
			this.transparentMask = transparentMask;
			this.spriteIdentityHash = System.identityHashCode(sprite);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SpriteTextureKey)) {
				return false;
			}
			SpriteTextureKey key = (SpriteTextureKey) other;
			return sprite == key.sprite
				&& transparentMask == key.transparentMask
				&& transform.equals(key.transform);
		}

		@Override
		public int hashCode() {
			return 31 * (31 * spriteIdentityHash + transform.hashCode()) + (transparentMask ? 1 : 0);
		}
	}
}
