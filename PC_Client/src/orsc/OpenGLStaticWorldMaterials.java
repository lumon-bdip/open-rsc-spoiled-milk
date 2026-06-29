package orsc;

import orsc.graphics.three.Renderer3DTextureData;

final class OpenGLStaticWorldMaterials {
	static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;

	private OpenGLStaticWorldMaterials() {
	}

	static StaticWorldMaterialPass classify(int textureId, Renderer3DTextureData textureData) {
		if (textureId == LEGACY_TRANSPARENT_TEXTURE) {
			return StaticWorldMaterialPass.DISCARDED;
		}
		if (textureId < 0) {
			return StaticWorldMaterialPass.OPAQUE;
		}
		if (textureData == null) {
			return StaticWorldMaterialPass.UNRESOLVED;
		}
		return textureData.hasTransparency()
			? StaticWorldMaterialPass.CUTOUT
			: StaticWorldMaterialPass.OPAQUE;
	}
}
