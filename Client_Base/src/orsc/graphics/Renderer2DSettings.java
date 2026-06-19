package orsc.graphics;

public final class Renderer2DSettings {
	public enum SpriteOverlayMode {
		SAFE_AFTER_FRAME("safe"),
		VISIBLE_AFTER_FRAME("visible"),
		PHASE_AWARE_AFTER_FRAME("phased"),
		NATIVE_UI_AFTER_FRAME("native-ui"),
		EXPERIMENTAL_ORDER_SENSITIVE_AFTER_FRAME("geometry");

		private final String id;

		SpriteOverlayMode(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	private static final boolean OPENGL_SPRITE_OVERLAY_ENABLED =
		readBoolean("spoiled_milk.opengl_sprite_overlay", "SPOILED_MILK_OPENGL_SPRITE_OVERLAY");
	private static final SpriteOverlayMode OPENGL_SPRITE_OVERLAY_MODE =
		readSpriteOverlayMode("spoiled_milk.opengl_sprite_overlay_mode", "SPOILED_MILK_OPENGL_SPRITE_OVERLAY_MODE");
	private static final boolean OPENGL_UI_BASE_FRAME_ENABLED =
		readBoolean("spoiled_milk.opengl_ui_base_frame", "SPOILED_MILK_OPENGL_UI_BASE_FRAME");
	private static final boolean OPENGL_NATIVE_UI_REPLACE_ENABLED =
		readBoolean("spoiled_milk.opengl_native_ui_replace", "SPOILED_MILK_OPENGL_NATIVE_UI_REPLACE");
	private static final boolean OPENGL_WORLD_SPRITES_VISIBLE_ENABLED =
		readBoolean("spoiledmilk.openglWorldSpritesVisible", "SPOILED_MILK_OPENGL_WORLD_SPRITES_VISIBLE");
	private static final boolean OPENGL_WORLD_UI_REPLAY_ENABLED =
		readBoolean("spoiledmilk.openglWorldUiReplay", "SPOILED_MILK_OPENGL_WORLD_UI_REPLAY")
			|| readBoolean("spoiledmilk.openglWorldMeshVisible", "SPOILED_MILK_OPENGL_WORLD_MESH_VISIBLE")
			|| readBoolean("spoiledmilk.openglWorldMeshTexturedVisible", "SPOILED_MILK_OPENGL_WORLD_TEXTURED_VISIBLE");

	private Renderer2DSettings() {
	}

	public static boolean isOpenGLSpriteOverlayEnabled() {
		return OPENGL_SPRITE_OVERLAY_ENABLED;
	}

	public static boolean isOpenGLSpriteCaptureEnabled() {
		return OPENGL_SPRITE_OVERLAY_ENABLED
			|| OPENGL_WORLD_SPRITES_VISIBLE_ENABLED
			|| OPENGL_WORLD_UI_REPLAY_ENABLED;
	}

	public static boolean canReplayUiOverOpenGLWorld() {
		return OPENGL_WORLD_UI_REPLAY_ENABLED;
	}

	public static boolean canReplayOrderSensitiveSpritesAfterFrame() {
		return OPENGL_SPRITE_OVERLAY_MODE == SpriteOverlayMode.EXPERIMENTAL_ORDER_SENSITIVE_AFTER_FRAME;
	}

	public static boolean canReplayVisibleOrderSensitiveSpritesAfterFrame() {
		return OPENGL_SPRITE_OVERLAY_MODE == SpriteOverlayMode.VISIBLE_AFTER_FRAME;
	}

	public static boolean canReplayPhaseAwareSpritesAfterFrame() {
		return OPENGL_SPRITE_OVERLAY_MODE == SpriteOverlayMode.PHASE_AWARE_AFTER_FRAME
			|| canReplaceUiSpritesWithOpenGL();
	}

	public static boolean canReplaceUiSpritesWithOpenGL() {
		return OPENGL_SPRITE_OVERLAY_ENABLED
			&& OPENGL_SPRITE_OVERLAY_MODE == SpriteOverlayMode.NATIVE_UI_AFTER_FRAME
			&& OPENGL_NATIVE_UI_REPLACE_ENABLED;
	}

	public static boolean isNativeUiOverlayMode() {
		return OPENGL_SPRITE_OVERLAY_MODE == SpriteOverlayMode.NATIVE_UI_AFTER_FRAME;
	}

	public static boolean canPresentUiBaseFrame() {
		return canReplaceUiSpritesWithOpenGL() && OPENGL_UI_BASE_FRAME_ENABLED;
	}

	public static String getOpenGLSpriteOverlayModeId() {
		return OPENGL_SPRITE_OVERLAY_MODE.getId();
	}

	private static boolean readBoolean(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null) {
			return false;
		}

		value = value.trim();
		return "true".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "yes".equalsIgnoreCase(value)
			|| "on".equalsIgnoreCase(value);
	}

	private static SpriteOverlayMode readSpriteOverlayMode(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null || value.trim().isEmpty()) {
			return SpriteOverlayMode.SAFE_AFTER_FRAME;
		}

		value = value.trim();
		for (SpriteOverlayMode mode : SpriteOverlayMode.values()) {
			if (mode.getId().equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return SpriteOverlayMode.SAFE_AFTER_FRAME;
	}
}
