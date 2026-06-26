package orsc;

final class RendererRuntimeDefaults {
	private RendererRuntimeDefaults() {
	}

	static void apply() {
		setDefault("spoiledmilk.directFramebuffer", "SPOILED_MILK_DIRECT_FRAMEBUFFER", "true");
		setDefault("spoiledmilk.openglPresenter", "SPOILED_MILK_OPENGL_PRESENTER", "true");
		setDefault("spoiledmilk.openglInput", "SPOILED_MILK_OPENGL_INPUT", "true");
		setDefault("spoiledmilk.openglPrimaryWindow", "SPOILED_MILK_OPENGL_PRIMARY_WINDOW", "true");
		setDefault("spoiledmilk.renderer3DGeometryCapture", "SPOILED_MILK_RENDERER3D_GEOMETRY_CAPTURE", "true");
		setDefault("spoiledmilk.openglWorldMesh", "SPOILED_MILK_OPENGL_WORLD_MESH", "true");
		setDefault("spoiledmilk.openglWorldMeshTexturedVisible", "SPOILED_MILK_OPENGL_WORLD_TEXTURED_VISIBLE", "true");
		setDefault("spoiledmilk.openglWorldMeshTexturedStaticVisible", "SPOILED_MILK_OPENGL_WORLD_TEXTURED_STATIC_VISIBLE", "true");
		setDefault("spoiledmilk.openglWorldStaticTextures", "SPOILED_MILK_OPENGL_WORLD_STATIC_TEXTURES", "true");
		setDefault("spoiledmilk.openglWorldTexturedAlpha", "SPOILED_MILK_OPENGL_WORLD_TEXTURED_ALPHA", "1.0");
		setDefault("spoiledmilk.openglWorldChunksTexturedVisible", "SPOILED_MILK_OPENGL_WORLD_CHUNKS_TEXTURED_VISIBLE", "true");
		setDefault("spoiledmilk.openglWorldChunksReplacementComposite", "SPOILED_MILK_OPENGL_WORLD_CHUNKS_REPLACEMENT_COMPOSITE", "true");
		setDefault("spoiledmilk.openglWorldChunksTrustedReplacement", "SPOILED_MILK_OPENGL_WORLD_CHUNKS_TRUSTED_REPLACEMENT", "true");
		setDefault("spoiledmilk.openglWorldSpritesVisible", "SPOILED_MILK_OPENGL_WORLD_SPRITES_VISIBLE", "true");
		setDefault("spoiledmilk.skipLegacyWorldRaster", "SPOILED_MILK_SKIP_LEGACY_WORLD_RASTER", "true");
		setDefault("spoiledmilk.modernClientLoop", "SPOILED_MILK_MODERN_CLIENT_LOOP", "true");
	}

	private static void setDefault(String propertyName, String envName, String value) {
		if (hasText(System.getProperty(propertyName)) || hasText(System.getenv(envName))) {
			return;
		}
		System.setProperty(propertyName, value);
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
