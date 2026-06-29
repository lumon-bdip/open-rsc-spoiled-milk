package orsc;

import com.openrsc.client.model.Sprite;
import orsc.graphics.Renderer2DFrame;
import orsc.graphics.Renderer2DSettings;
import orsc.graphics.RendererSpriteTransform;
import orsc.graphics.RendererTextureData;
import orsc.graphics.three.Renderer3DFrame;
import orsc.graphics.three.Renderer3DDepthFrame;
import orsc.graphics.three.Renderer3DMeshFrame;
import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DTextureData;
import orsc.graphics.three.Renderer3DWorldChunkFrame;
import orsc.util.FastMath;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

final class OpenGLFramePresenter implements AutoCloseable {
	private static final int INITIAL_WIDTH = RenderSurfaceSettings.getWidth();
	private static final int INITIAL_HEIGHT = RenderSurfaceSettings.getHeight();
	private static final int MOUSE_BUTTON_COUNT = 3;
	private static final int VISIBLE_SPRITE_ATLAS_FULL = -1;
	private static final String WORLD_MESH_PROPERTY = "spoiledmilk.openglWorldMesh";
	private static final String WORLD_MESH_ENV = "SPOILED_MILK_OPENGL_WORLD_MESH";
	private static final boolean WORLD_MESH_ENABLED = readBoolean(WORLD_MESH_PROPERTY, WORLD_MESH_ENV);
	private static final String WORLD_MESH_VISIBLE_PROPERTY = "spoiledmilk.openglWorldMeshVisible";
	private static final String WORLD_MESH_VISIBLE_ENV = "SPOILED_MILK_OPENGL_WORLD_MESH_VISIBLE";
	private static final boolean WORLD_MESH_VISIBLE =
		WORLD_MESH_ENABLED && readBoolean(WORLD_MESH_VISIBLE_PROPERTY, WORLD_MESH_VISIBLE_ENV);
	private static final String WORLD_MESH_TEXTURED_VISIBLE_PROPERTY = "spoiledmilk.openglWorldMeshTexturedVisible";
	private static final String WORLD_MESH_TEXTURED_VISIBLE_ENV = "SPOILED_MILK_OPENGL_WORLD_TEXTURED_VISIBLE";
	private static final boolean WORLD_MESH_TEXTURED_VISIBLE =
		WORLD_MESH_ENABLED && readBoolean(WORLD_MESH_TEXTURED_VISIBLE_PROPERTY, WORLD_MESH_TEXTURED_VISIBLE_ENV);
	private static final String WORLD_MESH_TEXTURED_STATIC_VISIBLE_PROPERTY =
		"spoiledmilk.openglWorldMeshTexturedStaticVisible";
	private static final String WORLD_MESH_TEXTURED_STATIC_VISIBLE_ENV =
		"SPOILED_MILK_OPENGL_WORLD_TEXTURED_STATIC_VISIBLE";
	private static final boolean WORLD_MESH_TEXTURED_STATIC_VISIBLE =
		WORLD_MESH_TEXTURED_VISIBLE
			&& readBoolean(WORLD_MESH_TEXTURED_STATIC_VISIBLE_PROPERTY, WORLD_MESH_TEXTURED_STATIC_VISIBLE_ENV);
	private static final String WORLD_CHUNKS_VISIBLE_PROPERTY = "spoiledmilk.openglWorldChunksVisible";
	private static final String WORLD_CHUNKS_VISIBLE_ENV = "SPOILED_MILK_OPENGL_WORLD_CHUNKS_VISIBLE";
	private static final boolean WORLD_CHUNKS_VISIBLE =
		WORLD_MESH_ENABLED && readBoolean(WORLD_CHUNKS_VISIBLE_PROPERTY, WORLD_CHUNKS_VISIBLE_ENV);
	private static final String WORLD_CHUNKS_FILLED_VISIBLE_PROPERTY = "spoiledmilk.openglWorldChunksFilledVisible";
	private static final String WORLD_CHUNKS_FILLED_VISIBLE_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_FILLED_VISIBLE";
	private static final boolean WORLD_CHUNKS_FILLED_VISIBLE =
		WORLD_MESH_ENABLED
			&& readBoolean(WORLD_CHUNKS_FILLED_VISIBLE_PROPERTY, WORLD_CHUNKS_FILLED_VISIBLE_ENV);
	private static final String WORLD_CHUNKS_TEXTURED_VISIBLE_PROPERTY = "spoiledmilk.openglWorldChunksTexturedVisible";
	private static final String WORLD_CHUNKS_TEXTURED_VISIBLE_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_TEXTURED_VISIBLE";
	private static final boolean WORLD_CHUNKS_TEXTURED_VISIBLE =
		WORLD_MESH_ENABLED
			&& readBoolean(WORLD_CHUNKS_TEXTURED_VISIBLE_PROPERTY, WORLD_CHUNKS_TEXTURED_VISIBLE_ENV);
	private static final String WORLD_CHUNKS_TEXTURED_SHADER_PROPERTY =
		"spoiledmilk.openglWorldChunksTexturedShader";
	private static final String WORLD_CHUNKS_TEXTURED_SHADER_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_TEXTURED_SHADER";
	private static final String WORLD_CHUNKS_RAW_MATERIAL_SHADER_PROPERTY =
		"spoiledmilk.openglWorldChunksRawMaterialShader";
	private static final String WORLD_CHUNKS_RAW_MATERIAL_SHADER_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_RAW_MATERIAL_SHADER";
	private static final String WORLD_CHUNKS_REMASTER_LIGHTING_SHADER_PROPERTY =
		"spoiledmilk.openglWorldChunksRemasterLightingShader";
	private static final String WORLD_CHUNKS_REMASTER_LIGHTING_SHADER_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_REMASTER_LIGHTING_SHADER";
	private static final boolean WORLD_CHUNKS_RAW_MATERIAL_SHADER =
		WORLD_CHUNKS_TEXTURED_VISIBLE
			&& readBoolean(WORLD_CHUNKS_RAW_MATERIAL_SHADER_PROPERTY, WORLD_CHUNKS_RAW_MATERIAL_SHADER_ENV, false);
	private static final boolean WORLD_CHUNKS_REMASTER_LIGHTING_SHADER =
		WORLD_CHUNKS_TEXTURED_VISIBLE
			&& !WORLD_CHUNKS_RAW_MATERIAL_SHADER
			&& readBoolean(
				WORLD_CHUNKS_REMASTER_LIGHTING_SHADER_PROPERTY,
				WORLD_CHUNKS_REMASTER_LIGHTING_SHADER_ENV,
				true);
	private static final boolean WORLD_CHUNKS_TEXTURED_SHADER =
		WORLD_CHUNKS_TEXTURED_VISIBLE
			&& (WORLD_CHUNKS_RAW_MATERIAL_SHADER
				|| WORLD_CHUNKS_REMASTER_LIGHTING_SHADER
				|| readBoolean(WORLD_CHUNKS_TEXTURED_SHADER_PROPERTY, WORLD_CHUNKS_TEXTURED_SHADER_ENV, false));
	private static final String WORLD_CHUNKS_REPLACEMENT_COMPOSITE_PROPERTY =
		"spoiledmilk.openglWorldChunksReplacementComposite";
	private static final String WORLD_CHUNKS_REPLACEMENT_COMPOSITE_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_REPLACEMENT_COMPOSITE";
	private static final boolean WORLD_CHUNKS_REPLACEMENT_COMPOSITE =
		WORLD_CHUNKS_TEXTURED_VISIBLE
			&& readBoolean(WORLD_CHUNKS_REPLACEMENT_COMPOSITE_PROPERTY, WORLD_CHUNKS_REPLACEMENT_COMPOSITE_ENV);
	private static final String WORLD_CHUNKS_TRUSTED_REPLACEMENT_PROPERTY =
		"spoiledmilk.openglWorldChunksTrustedReplacement";
	private static final String WORLD_CHUNKS_TRUSTED_REPLACEMENT_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_TRUSTED_REPLACEMENT";
	private static final boolean WORLD_CHUNKS_TRUSTED_REPLACEMENT =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(WORLD_CHUNKS_TRUSTED_REPLACEMENT_PROPERTY, WORLD_CHUNKS_TRUSTED_REPLACEMENT_ENV, false);
	private static final String WORLD_CHUNKS_RESIDENT_OBJECTS_PROPERTY =
		"spoiledmilk.openglWorldChunksResidentObjects";
	private static final String WORLD_CHUNKS_RESIDENT_OBJECTS_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_RESIDENT_OBJECTS";
	private static final boolean WORLD_CHUNKS_RESIDENT_OBJECTS =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(WORLD_CHUNKS_RESIDENT_OBJECTS_PROPERTY, WORLD_CHUNKS_RESIDENT_OBJECTS_ENV, false);
	private static final String WORLD_CHUNKS_SHADOW_PROOF_PROPERTY =
		"spoiledmilk.openglWorldChunkShadowProof";
	private static final String WORLD_CHUNKS_SHADOW_PROOF_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNK_SHADOW_PROOF";
	private static final boolean WORLD_CHUNKS_SHADOW_PROOF =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(WORLD_CHUNKS_SHADOW_PROOF_PROPERTY, WORLD_CHUNKS_SHADOW_PROOF_ENV, false);
	private static final String REMASTER_SHADOW_INVENTORY_DEBUG_PROPERTY =
		"spoiledmilk.remasterShadowInventoryDebug";
	private static final String REMASTER_SHADOW_INVENTORY_DEBUG_ENV =
		"SPOILED_MILK_REMASTER_SHADOW_INVENTORY_DEBUG";
	private static final boolean REMASTER_SHADOW_INVENTORY_DEBUG =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(
				REMASTER_SHADOW_INVENTORY_DEBUG_PROPERTY,
				REMASTER_SHADOW_INVENTORY_DEBUG_ENV,
				false);
	private static final String REMASTER_TERRAIN_SHADOW_MASK_PROPERTY =
		"spoiledmilk.remasterTerrainShadowMask";
	private static final String REMASTER_TERRAIN_SHADOW_MASK_ENV =
		"SPOILED_MILK_REMASTER_TERRAIN_SHADOW_MASK";
	private static final String REMASTER_TERRAIN_SHADOW_MASK_DEBUG_PROPERTY =
		"spoiledmilk.remasterTerrainShadowMaskDebug";
	private static final String REMASTER_TERRAIN_SHADOW_MASK_DEBUG_ENV =
		"SPOILED_MILK_REMASTER_TERRAIN_SHADOW_MASK_DEBUG";
	private static final boolean REMASTER_TERRAIN_SHADOW_MASK =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(
				REMASTER_TERRAIN_SHADOW_MASK_PROPERTY,
				REMASTER_TERRAIN_SHADOW_MASK_ENV,
				readBoolean(
					REMASTER_TERRAIN_SHADOW_MASK_DEBUG_PROPERTY,
					REMASTER_TERRAIN_SHADOW_MASK_DEBUG_ENV,
					true));
	private static final String WORLD_CHUNKS_SPATIAL_CULL_PROPERTY =
		"spoiledmilk.openglWorldChunksSpatialCull";
	private static final String WORLD_CHUNKS_SPATIAL_CULL_ENV =
		"SPOILED_MILK_OPENGL_WORLD_CHUNKS_SPATIAL_CULL";
	private static final boolean WORLD_CHUNKS_SPATIAL_CULL =
		WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& readBoolean(WORLD_CHUNKS_SPATIAL_CULL_PROPERTY, WORLD_CHUNKS_SPATIAL_CULL_ENV, false);
	private static final String WORLD_STATIC_TEXTURES_PROPERTY =
		"spoiledmilk.openglWorldStaticTextures";
	private static final String WORLD_STATIC_TEXTURES_ENV =
		"SPOILED_MILK_OPENGL_WORLD_STATIC_TEXTURES";
	private static final boolean WORLD_STATIC_TEXTURES =
		WORLD_MESH_TEXTURED_STATIC_VISIBLE
			&& readBoolean(WORLD_STATIC_TEXTURES_PROPERTY, WORLD_STATIC_TEXTURES_ENV, true);
	private static final String WORLD_MESH_TEXTURED_ALPHA_PROPERTY = "spoiledmilk.openglWorldTexturedAlpha";
	private static final String WORLD_MESH_TEXTURED_ALPHA_ENV = "SPOILED_MILK_OPENGL_WORLD_TEXTURED_ALPHA";
	private static final float TEXTURED_DIAGNOSTIC_ALPHA =
		readFloat(WORLD_MESH_TEXTURED_ALPHA_PROPERTY, WORLD_MESH_TEXTURED_ALPHA_ENV, 1.0f, 0.0f, 1.0f);
	private static final String WORLD_MESH_TEXTURED_SHADER_PROPERTY =
		"spoiledmilk.openglWorldMeshTexturedShader";
	private static final String WORLD_MESH_TEXTURED_SHADER_ENV =
		"SPOILED_MILK_OPENGL_WORLD_TEXTURED_SHADER";
	private static final boolean WORLD_MESH_TEXTURED_SHADER =
		WORLD_MESH_TEXTURED_VISIBLE
			&& readBoolean(WORLD_MESH_TEXTURED_SHADER_PROPERTY, WORLD_MESH_TEXTURED_SHADER_ENV, true);
	private static final String WORLD_REPLACEMENT_COMPOSITE_PROPERTY =
		"spoiledmilk.openglWorldReplacementComposite";
	private static final String WORLD_REPLACEMENT_COMPOSITE_ENV =
		"SPOILED_MILK_OPENGL_WORLD_REPLACEMENT_COMPOSITE";
	private static final boolean WORLD_REPLACEMENT_COMPOSITE =
		WORLD_MESH_TEXTURED_STATIC_VISIBLE
			&& readBoolean(WORLD_REPLACEMENT_COMPOSITE_PROPERTY, WORLD_REPLACEMENT_COMPOSITE_ENV, true);
	private static final String WORLD_SPRITES_VISIBLE_PROPERTY = "spoiledmilk.openglWorldSpritesVisible";
	private static final String WORLD_SPRITES_VISIBLE_ENV = "SPOILED_MILK_OPENGL_WORLD_SPRITES_VISIBLE";
	private static final boolean WORLD_SPRITES_VISIBLE =
		WORLD_MESH_ENABLED && readBoolean(WORLD_SPRITES_VISIBLE_PROPERTY, WORLD_SPRITES_VISIBLE_ENV);
	private static final float WORLD_SPRITE_DIAGNOSTIC_ALPHA = 0.65f;
	private static final int VISIBLE_SPRITE_RESTORE_COLOR_TOLERANCE = 8;
	private static final String WORLD_COMPOSITE_DEBUG_PROPERTY =
		"spoiledmilk.openglWorldCompositeDebug";
	private static final String WORLD_COMPOSITE_DEBUG_ENV =
		"SPOILED_MILK_OPENGL_WORLD_COMPOSITE_DEBUG";
	private static final boolean WORLD_COMPOSITE_DEBUG =
		readBoolean(WORLD_COMPOSITE_DEBUG_PROPERTY, WORLD_COMPOSITE_DEBUG_ENV);
	private static final int WORLD_COMPOSITE_DEBUG_LOG_LIMIT = 80;
	private static final int DEBUG_OVERLAY_ATLAS_WIDTH = 1024;
	private static final int DEBUG_OVERLAY_ATLAS_HEIGHT = 512;
	private static final int DEBUG_OVERLAY_MAX_TEXTURE_WIDTH = DEBUG_OVERLAY_ATLAS_WIDTH - 2;
	private static final int DEBUG_OVERLAY_MAX_TEXTURE_HEIGHT = DEBUG_OVERLAY_ATLAS_HEIGHT - 2;
	private static final long DEBUG_OVERLAY_TEXTURE_UPDATE_NANOS = 250_000_000L;
	private static final String FRAME_CAPTURE_PROPERTY = "spoiledmilk.openglFrameCapture";
	private static final String FRAME_CAPTURE_ENV = "SPOILED_MILK_OPENGL_FRAME_CAPTURE";
	private static final boolean FRAME_CAPTURE_HOTKEY_ENABLED =
		readBoolean(FRAME_CAPTURE_PROPERTY, FRAME_CAPTURE_ENV, false);
	private static final String FRAME_CAPTURE_DIR_PROPERTY = "spoiledmilk.openglFrameCaptureDir";
	private static final String FRAME_CAPTURE_DIR_ENV = "SPOILED_MILK_OPENGL_FRAME_CAPTURE_DIR";
	private static final int FRAME_CAPTURE_BURST_FRAMES = 12;
	private static final int WORLD_SPRITE_QUAD_VERTEX_COUNT = 4;
	private static final int WORLD_SPRITE_QUAD_INDEX_COUNT = 6;
	private static final int WORLD_SPRITE_QUAD_FLOATS_PER_VERTEX = 5;
	private static final int WORLD_SPRITE_QUAD_POSITION_COMPONENTS = 3;
	private static final int WORLD_SPRITE_QUAD_TEXCOORD_COMPONENTS = 2;
	private static final int WORLD_SPRITE_QUAD_TEXCOORD_OFFSET_BYTES =
		WORLD_SPRITE_QUAD_POSITION_COMPONENTS * 4;
	private static final int WORLD_SPRITE_QUAD_STRIDE_BYTES =
		WORLD_SPRITE_QUAD_FLOATS_PER_VERTEX * 4;
	private static final int UNIT_QUAD_VERTEX_COUNT = 4;
	private static final int UNIT_QUAD_INDEX_COUNT = 6;
	private static final int UNIT_QUAD_FLOATS_PER_VERTEX = 4;
	private static final int UNIT_QUAD_POSITION_COMPONENTS = 2;
	private static final int UNIT_QUAD_TEXCOORD_COMPONENTS = 2;
	private static final int UNIT_QUAD_TEXCOORD_OFFSET_BYTES =
		UNIT_QUAD_POSITION_COMPONENTS * 4;
	private static final int UNIT_QUAD_STRIDE_BYTES =
		UNIT_QUAD_FLOATS_PER_VERTEX * 4;

	private final Object frameLock = new Object();
	private final String title;
	private final boolean inputEnabled;
	private final boolean primaryWindow;
	private final FrameBufferPool frameBufferPool = new FrameBufferPool();

	private volatile boolean started;
	private volatile boolean closed;
	private volatile boolean disabled;
	private Frame pendingFrame;
	private Thread renderThread;

	private LwjglBindings gl;
	private long window;
	private int textureId;
	private int textureWidth;
	private int textureHeight;
	private OpenGLSpriteTextureCache spriteTextureCache;
	private OpenGLGlyphTextureCache glyphTextureCache;
	private OpenGLDynamicTextureAtlas visibleSpriteTextureAtlas;
	private OpenGLDynamicTextureAtlas debugOverlayTextureAtlas;
	private OpenGLTextureRegion debugOverlayTextureRegion;
	private int debugOverlayTextureWidth;
	private int debugOverlayTextureHeight;
	private long debugOverlayNextTextureUpdateNanos;
	private OpenGLWorldTextureCache worldTextureCache;
	private OpenGLWorldMeshRenderer worldMeshRenderer;
	private OpenGLWorldChunkRenderer worldChunkRenderer;
	private OpenGLShaderProgram projectedWorldShader;
	private OpenGLShaderProgram residentChunkShader;
	private int unitQuadVertexBufferId;
	private int unitQuadIndexBufferId;
	private int screenQuadVertexBufferId;
	private FloatBuffer screenQuadUploadBuffer;
	private int worldSpriteQuadVertexBufferId;
	private int worldSpriteQuadIndexBufferId;
	private FloatBuffer worldSpriteQuadUploadBuffer;
	private int windowWidth;
	private int windowHeight;
	private int currentSourceWidth = INITIAL_WIDTH;
	private int currentSourceHeight = INITIAL_HEIGHT;
	private int currentTargetWidth = INITIAL_WIDTH;
	private int currentTargetHeight = INITIAL_HEIGHT;
	private Viewport currentDrawViewport = new Viewport(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
	private Viewport currentFramebufferViewport = new Viewport(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
	private int legacySceneSpriteRestoreCommands;
	private int legacySceneSpriteRestoreFallbacks;
	private int legacySceneSpriteRestoreFallbackPixels;
	private int legacyEntitySpriteDebugFrames;
	private int legacyEntitySpriteDebugCommands;
	private int legacyEntitySpriteDebugVisiblePixels;
	private int legacyEntitySpriteDebugDirectPixels;
	private int legacyEntitySpriteDebugFallbacks;
	private int legacyEntitySpriteDebugSkipped;
	private int legacyEntitySpriteDebugAtlasFull;
	private int legacyEntitySpriteDebugFirstId = -1;
	private int legacyEntitySpriteDebugLastId = -1;
	private final Map<Integer, LegacyEntitySpriteDebugStats> legacyEntitySpriteDebugById =
		new LinkedHashMap<Integer, LegacyEntitySpriteDebugStats>();
	private final List<LegacyEntitySpriteDepthEvaluation> legacyEntitySpriteDepthEvaluations =
		new ArrayList<LegacyEntitySpriteDepthEvaluation>();
	private OpenGLWindowSettings.Mode appliedWindowMode;
	private int windowedX = 80;
	private int windowedY = 80;
	private int windowedWidth = INITIAL_WIDTH;
	private int windowedHeight = INITIAL_HEIGHT;
	private boolean hasWindowedBounds;
	private boolean appliedInitialWindowSize;
	private int lastMouseX = -1;
	private int lastMouseY = -1;
	private boolean inputFocused;
	private final boolean[] mouseButtonDown = new boolean[MOUSE_BUTTON_COUNT];
	private KeyBinding[] keyBindings = new KeyBinding[0];
	private boolean[] keyDown = new boolean[0];
	private boolean[] keySuppressUntilRelease = new boolean[0];
	private Object scrollCallback;
	private Object keyCallback;
	private Object charCallback;
	private Object windowFocusCallback;
	private int worldCompositeDebugLogs;
	private volatile boolean frameCaptureRequested;
	private volatile int frameCaptureBurstRemaining;
	private int frameCaptureSequence;
	private OpenGLFrameCapture activeFrameCapture;
	private long phaseCaptureNanos;
	private FloatBuffer cameraToClipMatrixBuffer;
	private int worldSpriteDepthDrawCommands;
	private int worldSpriteDepthTextureBatches;

	OpenGLFramePresenter(String title, boolean inputEnabled, boolean primaryWindow) {
		this.title = title;
		this.inputEnabled = inputEnabled;
		this.primaryWindow = primaryWindow;
	}

	void present(BufferedImage image, float scalar, ScaledWindow.ScalingAlgorithm scalingAlgorithm) {
		present(image, scalar, scalingAlgorithm, Renderer2DFrame.EMPTY);
	}

	void present(
		BufferedImage image,
		float scalar,
		ScaledWindow.ScalingAlgorithm scalingAlgorithm,
		Renderer2DFrame renderer2DFrame) {
		present(image, scalar, scalingAlgorithm, renderer2DFrame, null);
	}

	void present(
		BufferedImage image,
		float scalar,
		ScaledWindow.ScalingAlgorithm scalingAlgorithm,
		Renderer2DFrame renderer2DFrame,
		Renderer3DFrame renderer3DFrame) {
		present(image, scalar, scalingAlgorithm, renderer2DFrame, renderer3DFrame, null);
	}

	void present(
		BufferedImage image,
		float scalar,
		ScaledWindow.ScalingAlgorithm scalingAlgorithm,
		Renderer2DFrame renderer2DFrame,
		Renderer3DFrame renderer3DFrame,
		String[] rendererDebugOverlayLines) {
		if (image == null || closed || disabled) {
			return;
		}
		if (renderer2DFrame == null) {
			renderer2DFrame = Renderer2DFrame.EMPTY;
		}

		ensureStarted();
		if (disabled) {
			return;
		}

		long snapshotStart = RenderTelemetry.now();
		Frame frame = Frame.fromImage(
			image,
			scalar,
			scalingAlgorithm,
			frameBufferPool,
			renderer2DFrame,
			renderer3DFrame,
			rendererDebugOverlayLines);
		RenderTelemetry.recordOpenGLSnapshot(RenderTelemetry.elapsedSince(snapshotStart));

		synchronized (frameLock) {
			if (pendingFrame != null) {
				RenderTelemetry.recordOpenGLDroppedFrame();
				pendingFrame.release();
			}
			pendingFrame = frame;
			frameLock.notifyAll();
		}
	}

	private synchronized void ensureStarted() {
		if (started || closed || disabled) {
			return;
		}

		started = true;
		renderThread = new Thread(this::renderLoop, "Spoiled Milk OpenGL Presenter");
		renderThread.setDaemon(!primaryWindow);
		renderThread.start();
	}

	private void renderLoop() {
		boolean glfwInitialized = false;

		try {
			gl = LwjglBindings.load();
			glfwInitialized = gl.glfwInit();
			if (!glfwInitialized) {
				disable("OpenGL presenter disabled: GLFW could not initialize.");
				return;
			}

			createWindow(INITIAL_WIDTH, INITIAL_HEIGHT);
			initializeInputBindings();
			installInputCallbacks();
			log("OpenGL presenter active.");
			if (inputEnabled) {
				log("OpenGL input bridge active.");
			}
			if (primaryWindow) {
				log("OpenGL primary window active; Swing client window is hidden.");
			}
			if (Renderer2DSettings.isOpenGLSpriteOverlayEnabled()) {
				log("OpenGL sprite overlay probe active: mode="
					+ Renderer2DSettings.getOpenGLSpriteOverlayModeId()
					+ ".");
			}
			if (WORLD_MESH_ENABLED) {
				log("OpenGL world mesh upload active"
					+ (WORLD_MESH_VISIBLE ? " with visible wireframe diagnostic draw" : "")
					+ (WORLD_MESH_TEXTURED_VISIBLE ? " with visible textured diagnostic draw" : "")
					+ (WORLD_CHUNKS_VISIBLE ? " with visible world chunk diagnostic draw" : "")
					+ (WORLD_CHUNKS_FILLED_VISIBLE ? " with visible filled world chunk diagnostic draw" : "")
					+ (WORLD_CHUNKS_TEXTURED_VISIBLE ? " with visible textured world chunk diagnostic draw" : "")
					+ ".");
				if (WORLD_MESH_TEXTURED_VISIBLE) {
					log("OpenGL world textured diagnostic alpha: " + TEXTURED_DIAGNOSTIC_ALPHA);
				}
				if (WORLD_MESH_TEXTURED_SHADER) {
					log("OpenGL projected world shader path active.");
				} else if (WORLD_MESH_TEXTURED_VISIBLE) {
					log("OpenGL projected world shader path disabled; fixed-function fallback active.");
				}
				if (WORLD_CHUNKS_RAW_MATERIAL_SHADER) {
					log("OpenGL resident chunk raw material shader path active; baked lighting ignored.");
				} else if (WORLD_CHUNKS_REMASTER_LIGHTING_SHADER) {
					log("OpenGL resident chunk remaster-capable shader path active.");
				} else if (WORLD_CHUNKS_TEXTURED_SHADER) {
					log("OpenGL resident chunk parity shader path active.");
				} else if (WORLD_CHUNKS_TEXTURED_VISIBLE) {
					log("OpenGL resident chunk shader path disabled; fixed-function fallback active.");
				}
				if (WORLD_MESH_TEXTURED_STATIC_VISIBLE) {
					log("OpenGL static world texture sampling: "
						+ (WORLD_STATIC_TEXTURES ? "enabled" : "flat-material fallback")
						+ ".");
					log("OpenGL world geometry projection: camera-space perspective.");
				}
				if (WORLD_REPLACEMENT_COMPOSITE) {
					log("OpenGL world replacement composite active.");
				}
				if (WORLD_CHUNKS_REPLACEMENT_COMPOSITE) {
					log("OpenGL resident chunk replacement proof active; projected ownership is guarded.");
				}
				if (WORLD_CHUNKS_TRUSTED_REPLACEMENT) {
					log("OpenGL resident chunk trusted replacement ownership active.");
				}
				if (REMASTER_SHADOW_INVENTORY_DEBUG) {
					log("OpenGL remaster shadow inventory debug overlay active.");
				}
				if (REMASTER_TERRAIN_SHADOW_MASK) {
					log("OpenGL remaster terrain shadow mask active for Directional lighting.");
				}
			}
			if (WORLD_SPRITES_VISIBLE) {
				log("OpenGL world sprite diagnostic active.");
			}
			if (WORLD_COMPOSITE_DEBUG) {
				log("OpenGL world composite debug logging active.");
			}
			log("OpenGL scale mode: " + currentScaleMode().id + (primaryWindow ? " automatic" : ""));

			boolean windowCloseRequested = false;
			while (!closed) {
				windowCloseRequested = gl.glfwWindowShouldClose(window);
				if (windowCloseRequested) {
					break;
				}

				Frame frame = takeLatestFrame();
				if (frame != null) {
					try {
						renderFrame(frame);
					} finally {
						frame.release();
					}
				} else {
					gl.glfwPollEvents();
					processInput();
				}
			}
			log("OpenGL presenter loop ended: closed=" + closed + ", windowCloseRequested=" + windowCloseRequested);
		} catch (ClassNotFoundException e) {
			disable("OpenGL presenter disabled: LWJGL jars were not found. Run scripts/download-lwjgl.sh and rebuild.");
		} catch (Throwable t) {
			disable("OpenGL presenter disabled after an unexpected error: " + t.getMessage());
			t.printStackTrace();
		} finally {
			cleanup(glfwInitialized);
		}
	}

	private void createWindow(int width, int height) throws Exception {
		gl.glfwDefaultWindowHints();
		gl.glfwWindowHint(gl.GLFW_VISIBLE, gl.GLFW_FALSE);
		gl.glfwWindowHint(gl.GLFW_RESIZABLE, gl.GLFW_TRUE);
		if (OpenGLWindowSettings.getMode() == OpenGLWindowSettings.Mode.BORDERLESS_FULLSCREEN) {
			gl.glfwWindowHint(gl.GLFW_DECORATED, gl.GLFW_FALSE);
		}

		window = gl.glfwCreateWindow(width, height, title, 0L, 0L);
		if (window == 0L) {
			throw new IllegalStateException("GLFW returned a null window handle");
		}

		windowWidth = width;
		windowHeight = height;

		gl.glfwMakeContextCurrent(window);
		gl.createCapabilities();
		logOpenGLDevice();
		gl.glfwSwapInterval(1);
		gl.glfwShowWindow(window);
		syncWindowMode(width, height);

		textureId = gl.glGenTextures();
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP_TO_EDGE);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		initializeUnitQuadBuffers();
		initializeScreenQuadBuffers();
		initializeWorldSpriteQuadBuffers();
		initializeShaderPrograms();
		spriteTextureCache = new OpenGLSpriteTextureCache(gl);
		glyphTextureCache = new OpenGLGlyphTextureCache(gl);
		if (Renderer2DSettings.isOpenGLSpriteOverlayEnabled() || Renderer2DSettings.canReplayUiOverOpenGLWorld()) {
			visibleSpriteTextureAtlas = OpenGLDynamicTextureAtlas.create(gl, 2048, 2048);
		}
		debugOverlayTextureAtlas = OpenGLDynamicTextureAtlas.create(
			gl,
			DEBUG_OVERLAY_ATLAS_WIDTH,
			DEBUG_OVERLAY_ATLAS_HEIGHT);
		if (WORLD_MESH_ENABLED) {
			worldTextureCache = new OpenGLWorldTextureCache(gl);
			worldMeshRenderer = new OpenGLWorldMeshRenderer(gl, worldTextureCache, projectedWorldShader);
			worldChunkRenderer = new OpenGLWorldChunkRenderer(gl, worldTextureCache, residentChunkShader);
		}
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	}

	private void initializeShaderPrograms() {
		try {
			projectedWorldShader = OpenGLShaderProgram.createProjectedWorld(gl);
			gl.glUseProgram(0);
			log("OpenGL projected world shader program compiled.");
		} catch (Throwable t) {
			projectedWorldShader = null;
			try {
				gl.glUseProgram(0);
			} catch (Throwable ignored) {
			}
			log("OpenGL projected world shader program unavailable: " + t.getMessage());
		}
		try {
			residentChunkShader = OpenGLShaderProgram.createResidentChunkParity(gl);
			gl.glUseProgram(0);
			log("OpenGL resident chunk shader program compiled.");
		} catch (Throwable t) {
			residentChunkShader = null;
			try {
				gl.glUseProgram(0);
			} catch (Throwable ignored) {
			}
			log("OpenGL resident chunk shader program unavailable: " + t.getMessage());
		}
	}

	private void logOpenGLDevice() {
		try {
			log("OpenGL device: "
				+ gl.glGetString(gl.GL_RENDERER)
				+ " | "
				+ gl.glGetString(gl.GL_VERSION)
				+ " | "
				+ gl.glGetString(gl.GL_VENDOR));
		} catch (Throwable t) {
			log("OpenGL device info unavailable: " + t.getMessage());
		}
	}

	private void syncWindowMode(int targetWidth, int targetHeight) throws Exception {
		OpenGLWindowSettings.Mode desiredMode = OpenGLWindowSettings.getMode();
		if (desiredMode == appliedWindowMode) {
			return;
		}

		releaseInputState();
		suppressKeysUntilRelease();

		if (desiredMode == OpenGLWindowSettings.Mode.BORDERLESS_FULLSCREEN) {
			enterBorderlessFullscreen();
		} else {
			enterWindowedMode(targetWidth, targetHeight);
		}
		appliedWindowMode = desiredMode;
	}

	private void enterBorderlessFullscreen() throws Exception {
		saveWindowedBounds();
		MonitorMode monitorMode = gl.getPrimaryMonitorMode();
		gl.glfwSetWindowAttrib(window, gl.GLFW_DECORATED, gl.GLFW_FALSE);
		gl.glfwSetWindowPos(window, monitorMode.x, monitorMode.y);
		gl.glfwSetWindowSize(window, monitorMode.width, monitorMode.height);
		windowWidth = monitorMode.width;
		windowHeight = monitorMode.height;
		log("OpenGL window mode: borderless fullscreen " + monitorMode.width + "x" + monitorMode.height + ".");
	}

	private void enterWindowedMode(int targetWidth, int targetHeight) throws Exception {
		int restoreWidth = hasWindowedBounds ? windowedWidth : targetWidth;
		int restoreHeight = hasWindowedBounds ? windowedHeight : targetHeight;
		int restoreX = windowedX;
		int restoreY = windowedY;
		MonitorMode monitorMode = gl.getPrimaryMonitorMode();
		if (!hasWindowedBounds || isEffectivelyFullscreen(restoreWidth, restoreHeight, monitorMode)) {
			restoreWidth = Math.max(1, targetWidth);
			restoreHeight = Math.max(1, targetHeight);
			restoreX = monitorMode.x + Math.max(0, (monitorMode.width - restoreWidth) / 2);
			restoreY = monitorMode.y + Math.max(0, (monitorMode.height - restoreHeight) / 2);
		}

		if (appliedWindowMode == OpenGLWindowSettings.Mode.BORDERLESS_FULLSCREEN) {
			gl.glfwHideWindow(window);
		}
		gl.glfwSetWindowAttrib(window, gl.GLFW_DECORATED, gl.GLFW_TRUE);
		gl.glfwSetWindowSize(window, Math.max(1, restoreWidth), Math.max(1, restoreHeight));
		gl.glfwSetWindowPos(window, restoreX, restoreY);
		if (appliedWindowMode == OpenGLWindowSettings.Mode.BORDERLESS_FULLSCREEN) {
			gl.glfwShowWindow(window);
		}
		windowWidth = Math.max(1, restoreWidth);
		windowHeight = Math.max(1, restoreHeight);
		windowedX = restoreX;
		windowedY = restoreY;
		windowedWidth = windowWidth;
		windowedHeight = windowHeight;
		hasWindowedBounds = true;
		log("OpenGL window mode: windowed " + windowWidth + "x" + windowHeight + ".");
	}

	private void saveWindowedBounds() throws Exception {
		if (appliedWindowMode == OpenGLWindowSettings.Mode.BORDERLESS_FULLSCREEN) {
			return;
		}

		int[] x = new int[1];
		int[] y = new int[1];
		int[] width = new int[1];
		int[] height = new int[1];
		gl.glfwGetWindowPos(window, x, y);
		gl.glfwGetWindowSize(window, width, height);
		MonitorMode monitorMode = gl.getPrimaryMonitorMode();
		if (isEffectivelyFullscreen(width[0], height[0], monitorMode)) {
			return;
		}
		windowedX = x[0];
		windowedY = y[0];
		windowedWidth = Math.max(1, width[0]);
		windowedHeight = Math.max(1, height[0]);
		hasWindowedBounds = true;
	}

	private boolean isEffectivelyFullscreen(int width, int height, MonitorMode monitorMode) {
		return width >= monitorMode.width - 32 && height >= monitorMode.height - 96;
	}

	private Frame takeLatestFrame() throws InterruptedException {
		synchronized (frameLock) {
			if (pendingFrame == null && !closed) {
				frameLock.wait(16L);
			}

			Frame frame = pendingFrame;
			pendingFrame = null;
			return frame;
		}
	}

	private OpenGLPresentationSettings.ScaleMode currentScaleMode() {
		return primaryWindow
			? OpenGLPresentationSettings.ScaleMode.ASPECT_FIT
			: OpenGLPresentationSettings.getScaleMode();
	}

	private void renderFrame(Frame frame) throws Exception {
		currentSourceWidth = frame.sourceWidth;
		currentSourceHeight = frame.sourceHeight;
		currentTargetWidth = frame.targetWidth;
		currentTargetHeight = frame.targetHeight;

		syncWindowMode(frame.targetWidth, frame.targetHeight);

		if (appliedWindowMode == OpenGLWindowSettings.Mode.WINDOWED
			&& (!primaryWindow || !appliedInitialWindowSize)
			&& (frame.targetWidth != windowWidth || frame.targetHeight != windowHeight)) {
			gl.glfwSetWindowSize(window, frame.targetWidth, frame.targetHeight);
			windowWidth = frame.targetWidth;
			windowHeight = frame.targetHeight;
		}
		appliedInitialWindowSize = true;

		int[] framebufferWidth = new int[1];
		int[] framebufferHeight = new int[1];
		gl.glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
		int framebufferViewportWidth = Math.max(1, framebufferWidth[0]);
		int framebufferViewportHeight = Math.max(1, framebufferHeight[0]);

		int[] actualWindowWidth = new int[1];
		int[] actualWindowHeight = new int[1];
		gl.glfwGetWindowSize(window, actualWindowWidth, actualWindowHeight);
		int actualWindowViewportWidth = Math.max(1, actualWindowWidth[0]);
		int actualWindowViewportHeight = Math.max(1, actualWindowHeight[0]);

		OpenGLPresentationSettings.ScaleMode currentScaleMode = currentScaleMode();
		Viewport framebufferViewport =
			computeViewport(currentScaleMode, framebufferViewportWidth, framebufferViewportHeight, frame.sourceWidth, frame.sourceHeight);
		Viewport windowViewport =
			computeViewport(currentScaleMode, actualWindowViewportWidth, actualWindowViewportHeight, frame.sourceWidth, frame.sourceHeight);
		currentDrawViewport = windowViewport;
		currentFramebufferViewport = framebufferViewport;
		currentTargetWidth = windowViewport.width;
		currentTargetHeight = windowViewport.height;

		long uploadStart = RenderTelemetry.now();
		uploadTexture(frame);
		long uploadNanos = RenderTelemetry.elapsedSince(uploadStart);
		boolean worldReplacementComposite = shouldUseOpenGLWorldReplacementComposite(frame);
		OpenGLFrameCapture frameCapture = beginFrameCaptureIfRequested(frame, worldReplacementComposite);
		activeFrameCapture = frameCapture;

		long baseStart = RenderTelemetry.now();
		phaseCaptureNanos = 0L;
		prepareBaseFramebufferPass();
		gl.glViewport(0, 0, framebufferViewportWidth, framebufferViewportHeight);
		gl.glClear(gl.GL_COLOR_BUFFER_BIT);
		gl.glViewport(
			framebufferViewport.x,
			framebufferViewport.y,
			framebufferViewport.width,
			framebufferViewport.height);
		if (!worldReplacementComposite) {
			runOpenGLPass(() -> drawTexturedQuad());
		}
		long baseNanos = RenderTelemetry.elapsedSince(baseStart);
		captureLayer(frameCapture, "01-base");

		long worldStart = RenderTelemetry.now();
		phaseCaptureNanos = 0L;
		runOpenGLPass(() -> drawWorldMesh(frame, worldReplacementComposite));
		long worldNanos = RenderTelemetry.elapsedSince(worldStart);
		captureLayer(frameCapture, "02-opengl-world");

		long worldSpriteStart = RenderTelemetry.now();
		phaseCaptureNanos = 0L;
		if (!worldReplacementComposite) {
			runOpenGLPass(() -> drawWorldSprites(frame));
		}
		long worldSpriteNanos = RenderTelemetry.elapsedSince(worldSpriteStart);
		captureLayer(frameCapture, "03-world-sprites");

		long spriteOverlayStart = RenderTelemetry.now();
		phaseCaptureNanos = 0L;
		runOpenGLPass(() -> drawSpriteOverlay(frame));
		long spriteOverlayNanos = Math.max(0L, RenderTelemetry.elapsedSince(spriteOverlayStart) - phaseCaptureNanos);
		captureLayer(frameCapture, "05-sprite-overlay");

		long debugOverlayStart = RenderTelemetry.now();
		phaseCaptureNanos = 0L;
		runOpenGLPass(() -> drawRendererDebugOverlay(frame));
		long debugOverlayNanos = RenderTelemetry.elapsedSince(debugOverlayStart);
		captureLayer(frameCapture, "06-debug-overlay");
		finishFrameCapture(frameCapture, frame, worldReplacementComposite);
		activeFrameCapture = null;

		long swapStart = RenderTelemetry.now();
		gl.glfwSwapBuffers(window);
		long swapNanos = RenderTelemetry.elapsedSince(swapStart);
		gl.glfwPollEvents();
		processInput();

		RenderTelemetry.recordOpenGLFramePhases(
			baseNanos,
			worldNanos,
			worldSpriteNanos,
			spriteOverlayNanos,
			debugOverlayNanos,
			swapNanos);
		long renderNanos = uploadNanos
			+ baseNanos
			+ worldNanos
			+ worldSpriteNanos
			+ spriteOverlayNanos
			+ debugOverlayNanos
			+ swapNanos;
		RenderTelemetry.recordOpenGLFrame(uploadNanos, renderNanos);
	}

	private OpenGLFrameCapture beginFrameCaptureIfRequested(Frame frame, boolean worldReplacementComposite) {
		if (!FRAME_CAPTURE_HOTKEY_ENABLED) {
			return null;
		}
		if (frameCaptureRequested) {
			frameCaptureRequested = false;
			if (frameCaptureBurstRemaining <= 0) {
				frameCaptureBurstRemaining = FRAME_CAPTURE_BURST_FRAMES;
			}
		}
		if (frameCaptureBurstRemaining <= 0) {
			return null;
		}
		frameCaptureBurstRemaining--;
		try {
			OpenGLFrameCapture capture =
				OpenGLFrameCapture.create(++frameCaptureSequence, frame, worldReplacementComposite, this);
			capture.writeFrameInputs(frame, this);
			log("OpenGL frame capture started: "
				+ capture.getDirectoryPath()
				+ " burstRemaining="
				+ frameCaptureBurstRemaining);
			return capture;
		} catch (Throwable t) {
			log("OpenGL frame capture could not start: " + t.getMessage());
			return null;
		}
	}

	private void captureLayer(OpenGLFrameCapture capture, String layerName) {
		if (capture == null || capture.hasFailed()) {
			return;
		}
		long captureStart = RenderTelemetry.now();
		try {
			capture.writeLayer(layerName, readCurrentViewportImage());
		} catch (Throwable t) {
			capture.markFailed(t);
			log("OpenGL frame capture failed at " + layerName + ": " + t.getMessage());
		} finally {
			phaseCaptureNanos += RenderTelemetry.elapsedSince(captureStart);
		}
	}

	private void finishFrameCapture(
		OpenGLFrameCapture capture,
		Frame frame,
		boolean worldReplacementComposite) {
		if (capture == null) {
			return;
		}
		try {
			if (!capture.hasFailed()) {
				capture.writeLayer("07-final", readCurrentViewportImage());
			}
			capture.writeEntityRestoreStats(this);
			capture.writeEntityDepthEvaluations(this);
			capture.writeWorldSpriteBatchStats(this);
			capture.writeShaderVertexParityStats(this);
			capture.writeCharacterSprites(frame, legacyEntitySpriteDebugById);
			capture.writeSummary(frame, worldReplacementComposite, capture.hasFailed());
			log("OpenGL frame capture written: " + capture.getDirectoryPath());
		} catch (Throwable t) {
			log("OpenGL frame capture could not finish: " + t.getMessage());
		}
	}

	private BufferedImage readCurrentViewportImage() throws Exception {
		Viewport viewport = currentFramebufferViewport;
		int width = Math.max(1, viewport.width);
		int height = Math.max(1, viewport.height);
		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
		gl.glReadPixels(
			viewport.x,
			viewport.y,
			width,
			height,
			gl.GL_RGBA,
			gl.GL_UNSIGNED_BYTE,
			pixels);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			int sourceRow = height - 1 - y;
			for (int x = 0; x < width; x++) {
				int offset = (sourceRow * width + x) * 4;
				int red = pixels.get(offset) & 0xFF;
				int green = pixels.get(offset + 1) & 0xFF;
				int blue = pixels.get(offset + 2) & 0xFF;
				int alpha = pixels.get(offset + 3) & 0xFF;
				image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
			}
		}
		return image;
	}

	private boolean shouldUseOpenGLWorldReplacementComposite(Frame frame) {
		return shouldUseProjectedWorldReplacementComposite(frame)
			|| shouldUseResidentChunkReplacementComposite(frame);
	}

	private boolean shouldUseProjectedWorldReplacementComposite(Frame frame) {
		if (!Renderer2DSettings.canReplayUiOverOpenGLWorld()
			|| !hasScenePhaseCommands(frame)
			|| frame.renderer3DFrame == null) {
			return false;
		}
		return WORLD_REPLACEMENT_COMPOSITE && frame.renderer3DFrame.getMeshFrame() != null;
	}

	private boolean shouldUseResidentChunkReplacementComposite(Frame frame) {
		if (!Renderer2DSettings.canReplayUiOverOpenGLWorld()
			|| !hasScenePhaseCommands(frame)
			|| frame.renderer3DFrame == null) {
			return false;
		}
		Renderer3DWorldChunkFrame worldChunkFrame = frame.renderer3DFrame.getWorldChunkFrame();
		return WORLD_CHUNKS_REPLACEMENT_COMPOSITE
			&& (!WORLD_CHUNKS_RESIDENT_OBJECTS || frame.renderer3DFrame.getDepthFrame() != null)
			&& worldChunkFrame != null
			&& worldChunkFrame.getChunkCount() > 0;
	}

	private boolean hasScenePhaseCommands(Frame frame) {
		if (frame == null || frame.renderer2DFrame == null) {
			return false;
		}
		for (Renderer2DFrame.SpriteCommand command : frame.renderer2DFrame.getSpriteCommands()) {
			if (command.getPhase() == Renderer2DFrame.Phase.SCENE) {
				return true;
			}
		}
		return false;
	}

	private void prepareBaseFramebufferPass() throws Exception {
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glDisable(gl.GL_SCISSOR_TEST);
		gl.glDisable(gl.GL_BLEND);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	}

	private void runOpenGLPass(OpenGLPassAction action) throws Exception {
		gl.glPushAttrib(gl.GL_ALL_ATTRIB_BITS);
		gl.glPushClientAttrib(gl.GL_CLIENT_ALL_ATTRIB_BITS);
		try {
			action.run();
		} finally {
			gl.glPopClientAttrib();
			gl.glPopAttrib();
		}
	}

	private void uploadTexture(Frame frame) throws Exception {
		OpenGLPresentationSettings.ScaleMode currentScaleMode = currentScaleMode();
		int filter = frame.linearFiltering && currentScaleMode != OpenGLPresentationSettings.ScaleMode.INTEGER_FIT
			? gl.GL_LINEAR
			: gl.GL_NEAREST;
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, filter);
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, filter);

		if (textureWidth != frame.sourceWidth || textureHeight != frame.sourceHeight) {
			gl.glTexImage2D(
				gl.GL_TEXTURE_2D,
				0,
				gl.GL_RGBA,
				frame.sourceWidth,
				frame.sourceHeight,
				0,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				frame.pixels());
			textureWidth = frame.sourceWidth;
			textureHeight = frame.sourceHeight;
		} else {
			gl.glTexSubImage2D(
				gl.GL_TEXTURE_2D,
				0,
				0,
				0,
				frame.sourceWidth,
				frame.sourceHeight,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				frame.pixels());
		}
	}

	private void drawTexturedQuad() throws Exception {
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glDisable(gl.GL_SCISSOR_TEST);
		gl.glDisable(gl.GL_BLEND);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		useUnitProjection();

		drawUnitTexturedQuad();
	}

	private void drawWorldMesh(Frame frame, boolean worldReplacementComposite) throws Exception {
		long chunkUploadPhaseNanos = 0L;
		long projectedMeshPhaseNanos = 0L;
		long chunkDrawPhaseNanos = 0L;
		Renderer3DMeshFrame renderer3DMeshFrame =
			frame.renderer3DFrame == null ? null : frame.renderer3DFrame.getMeshFrame();
		Renderer3DWorldChunkFrame worldChunkFrame =
			frame.renderer3DFrame == null ? Renderer3DWorldChunkFrame.EMPTY : frame.renderer3DFrame.getWorldChunkFrame();
		RenderTelemetry.recordOpenGLWorldChunkFrame(
			worldChunkFrame.getChunkCount(),
			worldChunkFrame.getTotalVertexCount(),
			worldChunkFrame.getTotalIndexCount(),
			worldChunkFrame.getTotalTriangleCount());
		RemasterShadowInventory shadowInventory = RenderTelemetry.isEnabled()
			? inspectRemasterShadowInventory(worldChunkFrame)
			: RemasterShadowInventory.EMPTY;
		RenderTelemetry.recordOpenGLRemasterShadowInventory(
			shadowInventory.receiverChunks,
			shadowInventory.receiverTriangles,
			shadowInventory.totalCasters,
			shadowInventory.wallCasters,
			shadowInventory.gameObjectCasters,
			shadowInventory.wallObjectCasters,
			shadowInventory.outdoorOnlyCasters,
			shadowInventory.clippingCandidates,
			shadowInventory.roofedReceivers,
			shadowInventory.outdoorReceivers,
			shadowInventory.unknownReceivers,
			shadowInventory.roofedCasters,
			shadowInventory.outdoorCasters,
			shadowInventory.unknownCasters,
			shadowInventory.sunlightEligibleCasters,
			shadowInventory.sunlightSuppressedRoofedCasters,
			shadowInventory.sunlightSuppressedUnknownCasters);
		if (WORLD_MESH_ENABLED && worldChunkRenderer != null) {
			long chunkUploadPhaseStart = RenderTelemetry.now();
			OpenGLWorldChunkUploadStats chunkUploadStats = worldChunkRenderer.upload(
				frame.renderer3DFrame,
				worldChunkFrame,
				WORLD_CHUNKS_TEXTURED_VISIBLE);
			chunkUploadPhaseNanos = RenderTelemetry.elapsedSince(chunkUploadPhaseStart);
			RenderTelemetry.recordOpenGLWorldChunkUpload(
				chunkUploadStats.requestedChunks,
				chunkUploadStats.uploadedChunks,
				chunkUploadStats.reusedChunks,
				chunkUploadStats.evictedChunks);
		}
		boolean requestedResidentChunkReplacementComposite =
			worldReplacementComposite && shouldUseResidentChunkReplacementComposite(frame);
		ResidentChunkReadiness residentChunkReadiness =
			requestedResidentChunkReplacementComposite && worldChunkRenderer != null
				? worldChunkRenderer.inspectDrawableResidentStaticWorld(
					frame.renderer3DFrame,
					worldChunkFrame,
					WORLD_CHUNKS_TEXTURED_VISIBLE)
				: ResidentChunkReadiness.notRequested(
					requestedResidentChunkReplacementComposite ? "no-chunk-renderer" : "not-requested");
		boolean residentChunkReplacementComposite =
			requestedResidentChunkReplacementComposite
				&& WORLD_CHUNKS_TRUSTED_REPLACEMENT
				&& residentChunkReadiness.canReplace;
		String residentReplacementReason =
			requestedResidentChunkReplacementComposite
				&& !residentChunkReplacementComposite
				&& residentChunkReadiness.canReplace
				&& !WORLD_CHUNKS_TRUSTED_REPLACEMENT
					? "not-trusted"
					: residentChunkReadiness.reason;
		RenderTelemetry.recordOpenGLResidentChunkReplacement(
			requestedResidentChunkReplacementComposite,
			residentChunkReplacementComposite,
			residentReplacementReason,
			residentChunkReadiness.drawableTerrainBatches,
			residentChunkReadiness.drawableWallBatches,
			residentChunkReadiness.drawableRoofBatches);
		boolean canDrawProjectedMesh = WORLD_MESH_ENABLED && worldMeshRenderer != null && renderer3DMeshFrame != null;
		boolean drawProjectedMeshVisible =
			!residentChunkReplacementComposite && (WORLD_MESH_VISIBLE || WORLD_MESH_TEXTURED_VISIBLE);
		boolean drawProjectedObjectMeshVisible =
			residentChunkReplacementComposite
				&& WORLD_MESH_TEXTURED_VISIBLE
				&& !WORLD_CHUNKS_RESIDENT_OBJECTS;
		if (canDrawProjectedMesh && drawProjectedMeshVisible) {
			useWorldMeshProjection(frame);
			long projectedMeshPhaseStart = RenderTelemetry.now();
			worldMeshRenderer.uploadAndMaybeDrawOwnedBaseWorld(
				renderer3DMeshFrame,
				WORLD_MESH_VISIBLE,
				WORLD_MESH_TEXTURED_VISIBLE,
				cameraToClipMatrixBuffer);
			projectedMeshPhaseNanos += RenderTelemetry.elapsedSince(projectedMeshPhaseStart);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		} else if (canDrawProjectedMesh && !residentChunkReplacementComposite) {
			long projectedMeshPhaseStart = RenderTelemetry.now();
			worldMeshRenderer.uploadAndMaybeDraw(renderer3DMeshFrame, false, false, null);
			projectedMeshPhaseNanos += RenderTelemetry.elapsedSince(projectedMeshPhaseStart);
		}
		boolean drawResidentChunkDiagnostic =
			(WORLD_CHUNKS_VISIBLE || WORLD_CHUNKS_FILLED_VISIBLE || WORLD_CHUNKS_TEXTURED_VISIBLE)
				&& (!WORLD_CHUNKS_REPLACEMENT_COMPOSITE || residentChunkReplacementComposite);
		if (drawResidentChunkDiagnostic
			&& worldChunkRenderer != null
			&& frame.renderer3DFrame != null) {
			long chunkDrawPhaseStart = RenderTelemetry.now();
			OpenGLWorldChunkDrawStats chunkDrawStats =
				worldChunkRenderer.drawDiagnostic(
					frame.renderer3DFrame,
					WORLD_CHUNKS_FILLED_VISIBLE,
					WORLD_CHUNKS_TEXTURED_VISIBLE);
			chunkDrawPhaseNanos = RenderTelemetry.elapsedSince(chunkDrawPhaseStart);
			RenderTelemetry.recordOpenGLWorldChunkDraw(
				chunkDrawStats.drawnChunks,
				chunkDrawStats.drawnTriangles,
				chunkDrawStats.drawnTerrainTriangles,
				chunkDrawStats.drawnWallTriangles,
				chunkDrawStats.drawnRoofTriangles,
				chunkDrawStats.drawnGameObjectTriangles,
				chunkDrawStats.drawnWallObjectTriangles,
				chunkDrawStats.drawnOtherTriangles,
				chunkDrawStats.fallbackTriangles,
				chunkDrawStats.skippedTriangles,
				chunkDrawStats.shadowProofChunks,
				chunkDrawStats.shadowProofIndices,
				chunkDrawStats.consideredChunks,
				chunkDrawStats.culledChunks,
				chunkDrawStats.consideredBatches,
				chunkDrawStats.drawnBatches,
				chunkDrawStats.culledBatches,
				chunkDrawStats.drawCalls,
				chunkDrawStats.textureBinds);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		if (canDrawProjectedMesh && drawProjectedObjectMeshVisible) {
			useWorldMeshProjection(frame);
			long projectedMeshPhaseStart = RenderTelemetry.now();
			worldMeshRenderer.uploadAndMaybeDrawObjects(renderer3DMeshFrame, cameraToClipMatrixBuffer);
			projectedMeshPhaseNanos += RenderTelemetry.elapsedSince(projectedMeshPhaseStart);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		if (REMASTER_SHADOW_INVENTORY_DEBUG
			&& worldChunkRenderer != null
			&& frame.renderer3DFrame != null) {
			worldChunkRenderer.drawRemasterShadowInventoryDebug(frame.renderer3DFrame);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		if (shouldDrawRemasterTerrainShadowMask()
			&& worldChunkRenderer != null
			&& frame.renderer3DFrame != null) {
			worldChunkRenderer.drawRemasterTerrainShadowMask(frame.renderer3DFrame);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		RenderTelemetry.recordOpenGLWorldPhaseBreakdown(
			chunkUploadPhaseNanos,
			projectedMeshPhaseNanos,
			chunkDrawPhaseNanos);
	}

	private static RemasterShadowInventory inspectRemasterShadowInventory(
		Renderer3DWorldChunkFrame worldChunkFrame) {
		if (worldChunkFrame == null || worldChunkFrame.getChunkCount() <= 0) {
			return RemasterShadowInventory.EMPTY;
		}

		int receiverChunks = 0;
		int receiverTriangles = 0;
		int totalCasters = 0;
		int wallCasters = 0;
		int gameObjectCasters = 0;
		int wallObjectCasters = 0;
		int outdoorOnlyCasters = 0;
		int clippingCandidates = 0;
		int roofedReceivers = 0;
		int outdoorReceivers = 0;
		int unknownReceivers = 0;
		int roofedCasters = 0;
		int outdoorCasters = 0;
		int unknownCasters = 0;
		int sunlightEligibleCasters = 0;
		int sunlightSuppressedRoofedCasters = 0;
		int sunlightSuppressedUnknownCasters = 0;
		RemasterShadowRoofCoverage roofCoverage = RemasterShadowRoofCoverage.from(worldChunkFrame);
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : worldChunkFrame.getChunks()) {
			int terrainTriangles = Math.max(0, chunk.getTerrainTriangles());
			if (terrainTriangles > 0) {
				receiverChunks++;
				receiverTriangles += terrainTriangles;
			}
			int triangleLimit = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleLimit; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				int classification = roofClassificationForTriangle(roofCoverage, chunk, triangle);
				if (classification > 0) {
					roofedReceivers++;
				} else if (classification == 0) {
					outdoorReceivers++;
				} else {
					unknownReceivers++;
				}
			}
			int casterCount = chunk.getShadowCasterCount();
			for (int index = 0; index < casterCount; index++) {
				Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
				if (!isRemasterShadowEligibleCaster(caster)) {
					continue;
				}
				totalCasters++;
				Renderer3DModelKind kind = caster.getModelKind();
				if (kind == Renderer3DModelKind.WALL) {
					wallCasters++;
				} else if (kind == Renderer3DModelKind.GAME_OBJECT) {
					gameObjectCasters++;
				} else if (kind == Renderer3DModelKind.WALL_OBJECT) {
					wallObjectCasters++;
				}
				if (caster.isOutdoorOnly()) {
					outdoorOnlyCasters++;
				}
				if (isRemasterShadowClippingCandidate(caster)) {
					clippingCandidates++;
				}
				int classification = roofClassificationForCaster(roofCoverage, chunk, caster);
				if (classification > 0) {
					roofedCasters++;
					sunlightSuppressedRoofedCasters++;
				} else if (classification == 0) {
					outdoorCasters++;
					sunlightEligibleCasters++;
				} else {
					unknownCasters++;
					sunlightSuppressedUnknownCasters++;
				}
			}
		}
		return new RemasterShadowInventory(
			receiverChunks,
			receiverTriangles,
			totalCasters,
			wallCasters,
			gameObjectCasters,
			wallObjectCasters,
			outdoorOnlyCasters,
			clippingCandidates,
			roofedReceivers,
			outdoorReceivers,
			unknownReceivers,
			roofedCasters,
			outdoorCasters,
			unknownCasters,
			sunlightEligibleCasters,
			sunlightSuppressedRoofedCasters,
			sunlightSuppressedUnknownCasters);
	}

	private static boolean isRemasterShadowEligibleCaster(Renderer3DWorldChunkFrame.ShadowCaster caster) {
		return caster != null && caster.getHeight() > 0 && caster.getOpacity() > 0;
	}

	private static boolean isRemasterShadowClippingCandidate(Renderer3DWorldChunkFrame.ShadowCaster caster) {
		if (!isRemasterShadowEligibleCaster(caster)) {
			return false;
		}
		Renderer3DModelKind kind = caster.getModelKind();
		return kind == Renderer3DModelKind.WALL || kind == Renderer3DModelKind.WALL_OBJECT;
	}

	private static int roofClassificationForTriangle(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		int triangle) {
		if (chunk == null || roofCoverage == null) {
			return -1;
		}
		int sourceIndex = triangle * 3;
		int x = 0;
		int z = 0;
		for (int corner = 0; corner < 3; corner++) {
			int vertex = chunk.getIndex(sourceIndex + corner);
			int coord = vertex * 3;
			x += chunk.getVertexCoord(coord);
			z += chunk.getVertexCoord(coord + 2);
		}
		return roofCoverage.classify(chunk.getPlane(), x / 3, z / 3);
	}

	private static int roofClassificationForCaster(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame.ChunkMesh chunk,
		Renderer3DWorldChunkFrame.ShadowCaster caster) {
		if (chunk == null || caster == null || roofCoverage == null) {
			return -1;
		}
		if (caster.getModelKind() == Renderer3DModelKind.WALL
			|| caster.getModelKind() == Renderer3DModelKind.WALL_OBJECT) {
			return roofCoverage.classifyBoundaryCaster(
				chunk.getPlane(),
				caster.getBaseX0(),
				caster.getBaseZ0(),
				caster.getBaseX1(),
				caster.getBaseZ1());
		}
		int x = (caster.getBaseX0() + caster.getBaseX1()) / 2;
		int z = (caster.getBaseZ0() + caster.getBaseZ1()) / 2;
		return roofCoverage.classify(chunk.getPlane(), x, z);
	}

	private void drawWorldSprites(Frame frame) throws Exception {
		if (!WORLD_SPRITES_VISIBLE
			|| spriteTextureCache == null
			|| frame.renderer3DFrame == null
			|| frame.renderer2DFrame == null
			|| !hasVisibleOpenGLWorldPass()) {
			return;
		}

		List<Renderer3DFrame.SpriteAnchor> anchors = frame.renderer3DFrame.getSpriteAnchors();
		Renderer2DFrame.SpriteCommand[] commands = frame.renderer2DFrame.getSpriteCommands();
		if (anchors.isEmpty() || commands.length == 0) {
			RenderTelemetry.recordOpenGLWorldSpriteFrame(anchors.size(), 0, 0);
			return;
		}

		WorldSpriteMatch[] matches = matchWorldSpriteAnchors(anchors, commands);
		int matched = 0;
		for (WorldSpriteMatch match : matches) {
			if (match != null) {
				matched++;
			}
		}
		if (matched == 0) {
			RenderTelemetry.recordOpenGLWorldSpriteFrame(anchors.size(), 0, 0);
			return;
		}

		useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(gl.GL_DEPTH_TEST);
		int drawn = 0;
		try {
			for (WorldSpriteMatch match : matches) {
				if (match == null) {
					continue;
				}
				drawWorldSpriteMatch(match);
				drawn++;
			}
		} finally {
			gl.glDisable(gl.GL_BLEND);
			gl.glEnable(gl.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		}
		RenderTelemetry.recordOpenGLWorldSpriteFrame(anchors.size(), matched, drawn);
		useSourceProjection(frame.sourceWidth, frame.sourceHeight);
	}

	private boolean hasVisibleOpenGLWorldPass() {
		return WORLD_MESH_VISIBLE
			|| WORLD_MESH_TEXTURED_VISIBLE
			|| WORLD_CHUNKS_VISIBLE
			|| WORLD_CHUNKS_FILLED_VISIBLE
			|| WORLD_CHUNKS_TEXTURED_VISIBLE;
	}

	private WorldSpriteMatch[] matchWorldSpriteAnchors(
		List<Renderer3DFrame.SpriteAnchor> anchors,
		Renderer2DFrame.SpriteCommand[] commands) {
		WorldSpriteMatch[] matches = new WorldSpriteMatch[anchors.size()];
		boolean[] usedCommands = new boolean[commands.length];
		for (int anchorIndex = 0; anchorIndex < anchors.size(); anchorIndex++) {
			Renderer3DFrame.SpriteAnchor anchor = anchors.get(anchorIndex);
			int bestCommand = -1;
			int bestScore = Integer.MAX_VALUE;
			for (int commandIndex = 0; commandIndex < commands.length; commandIndex++) {
				if (usedCommands[commandIndex]) {
					continue;
				}
				Renderer2DFrame.SpriteCommand command = commands[commandIndex];
				int score = worldSpriteMatchScore(anchor, command);
				if (score < bestScore) {
					bestScore = score;
					bestCommand = commandIndex;
				}
			}
			if (bestCommand >= 0) {
				usedCommands[bestCommand] = true;
				matches[anchorIndex] = new WorldSpriteMatch(anchor, commands[bestCommand]);
			}
		}
		return matches;
	}

	private int worldSpriteMatchScore(
		Renderer3DFrame.SpriteAnchor anchor,
		Renderer2DFrame.SpriteCommand command) {
		return worldSpriteMatchScore(anchor, command, true);
	}

	private int worldSpriteMatchScore(
		Renderer3DFrame.SpriteAnchor anchor,
		Renderer2DFrame.SpriteCommand command,
		boolean requireSpriteId) {
		if (command.getPhase() != Renderer2DFrame.Phase.SCENE) {
			return Integer.MAX_VALUE;
		}

		if (requireSpriteId && command.getLegacySpriteId() != anchor.getSpriteId()) {
			return Integer.MAX_VALUE;
		}

		int anchorLeft = anchor.getDrawX();
		int anchorTop = anchor.getDrawY();
		int anchorRight = anchorLeft + anchor.getDrawWidth();
		int anchorBottom = anchorTop + anchor.getDrawHeight();
		int commandLeft = command.getX();
		int commandTop = command.getY();
		int commandRight = commandLeft + command.getWidth();
		int commandBottom = commandTop + command.getHeight();
		int tolerance = Math.max(8, Math.max(anchor.getDrawWidth(), anchor.getDrawHeight()) / 4);
		if (commandRight < anchorLeft - tolerance
			|| commandLeft > anchorRight + tolerance
			|| commandBottom < anchorTop - tolerance
			|| commandTop > anchorBottom + tolerance) {
			return Integer.MAX_VALUE;
		}

		int anchorCenterX = anchorLeft + anchor.getDrawWidth() / 2;
		int anchorCenterY = anchorTop + anchor.getDrawHeight() / 2;
		int commandCenterX = commandLeft + command.getWidth() / 2;
		int commandCenterY = commandTop + command.getHeight() / 2;
		int centerDelta = Math.abs(anchorCenterX - commandCenterX) + Math.abs(anchorCenterY - commandCenterY);
		int sizeDelta = Math.abs(anchor.getDrawWidth() - command.getWidth())
			+ Math.abs(anchor.getDrawHeight() - command.getHeight());
		return centerDelta * 4 + sizeDelta;
	}

	private WorldSpriteAnchorMatch classifyWorldSpriteAnchorMatch(
		Frame frame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor) {
		if (frame == null || frame.renderer3DFrame == null || command == null || anchor == null) {
			return WorldSpriteAnchorMatch.unmatched();
		}
		int strictScore = worldSpriteMatchScore(anchor, command, true);
		if (strictScore != Integer.MAX_VALUE) {
			return new WorldSpriteAnchorMatch("strict-id-bounds", strictScore);
		}
		int relaxedScore = worldSpriteMatchScore(anchor, command, false);
		if (relaxedScore != Integer.MAX_VALUE) {
			return new WorldSpriteAnchorMatch(
				command.getLegacySpriteId() == anchor.getSpriteId() ? "relaxed-bounds" : "relaxed-cross-id",
				relaxedScore);
		}
		if (command.getLegacySpriteId() == anchor.getSpriteId()) {
			return new WorldSpriteAnchorMatch("id-only", Integer.MAX_VALUE);
		}
		return WorldSpriteAnchorMatch.unmatched();
	}

	private void drawWorldSpriteMatch(WorldSpriteMatch match) throws Exception {
		Renderer2DFrame.SpriteCommand command = match.command;
		Renderer3DFrame.SpriteAnchor anchor = match.anchor;
		OpenGLTextureRegion region = uploadSpriteTexture(command.getSprite(), command.getTransform());
		float commandAlpha = command.getAlpha() / (float) Renderer2DFrame.SpriteCommand.FULL_ALPHA;
		float spriteAlphaScale = WORLD_REPLACEMENT_COMPOSITE ? 1.0f : WORLD_SPRITE_DIAGNOSTIC_ALPHA;
		float alpha = spriteAlphaScale * commandAlpha;
		float topX0 = command.getTopX16() / 65536.0f;
		float bottomX0 = command.getBottomX16() / 65536.0f;
		float y0 = command.getY();
		float y1 = command.getY() + command.getHeight();
		float topX1 = topX0 + command.getWidth();
		float bottomX1 = bottomX0 + command.getWidth();
		float uSpan = region.getU1() - region.getU0();
		float vSpan = region.getV1() - region.getV0();
		float u0 = region.getU0() + uSpan * command.getSourceX() / region.getWidth();
		float v0 = region.getV0() + vSpan * command.getSourceY() / region.getHeight();
		float u1 = region.getU0() + uSpan * (command.getSourceX() + command.getSourceWidth()) / region.getWidth();
		float v1 = region.getV0() + vSpan * (command.getSourceY() + command.getSourceHeight()) / region.getHeight();
		float leftU = command.isMirrorX() ? u1 : u0;
		float rightU = command.isMirrorX() ? u0 : u1;

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawScreenQuad(
			topX0,
			y0,
			leftU,
			v0,
			topX1,
			y0,
			rightU,
			v0,
			bottomX1,
			y1,
			rightU,
			v1,
			bottomX0,
			y1,
			leftU,
			v1);
	}

	private void useWorldToneColor(float alpha) throws Exception {
		RendererDayNightCycle.Presentation presentation = RendererDayNightCycle.currentPresentation();
		gl.glColor4f(
			presentation.redMultiplier,
			presentation.greenMultiplier,
			presentation.blueMultiplier,
			alpha);
	}

	private void drawSpriteOverlay(Frame frame) throws Exception {
		Renderer2DFrame renderer2DFrame = frame.renderer2DFrame;
		boolean replayOpenGLWorldUi = shouldUseOpenGLWorldReplacementComposite(frame);
		if ((!Renderer2DSettings.isOpenGLSpriteOverlayEnabled() && !replayOpenGLWorldUi)
			|| renderer2DFrame == null) {
			return;
		}

		Renderer2DFrame.SpriteCommand[] commands = renderer2DFrame.getSpriteCommands();
		Renderer2DFrame.TextCommand[] textCommands = renderer2DFrame.getTextCommands();
		Renderer2DFrame.PrimitiveCommand[] primitiveCommands = renderer2DFrame.getPrimitiveCommands();
		Renderer2DFrame.RotatedSpriteCommand[] rotatedSpriteCommands = renderer2DFrame.getRotatedSpriteCommands();
		Renderer2DFrame.CircleCommand[] circleCommands = renderer2DFrame.getCircleCommands();
		boolean replayNativeBaseCommands = shouldReplayNativeBaseCommands(renderer2DFrame);
		RenderTelemetry.recordSpriteCaptureStats(renderer2DFrame.getCaptureStats());
		if (!Renderer2DSettings.isOpenGLSpriteOverlayEnabled() && replayOpenGLWorldUi) {
			drawOpenGLWorldCompositeOverlay(
				frame,
				renderer2DFrame,
				commands,
				textCommands,
				primitiveCommands,
				rotatedSpriteCommands,
				circleCommands);
			return;
		}
		if (Renderer2DSettings.isNativeUiOverlayMode()
			&& !Renderer2DSettings.canReplaceUiSpritesWithOpenGL()) {
			RenderTelemetry.recordSpriteOverlayFrame(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			return;
		}
		if (commands.length == 0
			&& textCommands.length == 0
			&& (!replayNativeBaseCommands || primitiveCommands.length == 0)
			&& (!replayNativeBaseCommands || rotatedSpriteCommands.length == 0)
			&& (!replayNativeBaseCommands || circleCommands.length == 0)) {
			RenderTelemetry.recordSpriteOverlayFrame(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			return;
		}

		int staticReplayed = 0;
		int visibleReplayed = 0;
		int skippedOrdered = 0;
		int skippedInvisible = 0;
		int skippedAtlasFull = 0;
		int visiblePixels = 0;
		int sceneCommands = 0;
		int worldCommands = 0;
		int uiCommands = 0;
		int unknownCommands = 0;
		int[] directReplayedByPhase = new int[Renderer2DFrame.Phase.values().length];
		int[] visibleReplayedByPhase = new int[Renderer2DFrame.Phase.values().length];
		if (visibleSpriteTextureAtlas != null) {
			visibleSpriteTextureAtlas.beginFrame();
		}
		useSourceProjection(renderer2DFrame.getWidth(), renderer2DFrame.getHeight());
		boolean visibleReplay = Renderer2DSettings.canReplayVisibleOrderSensitiveSpritesAfterFrame();
		boolean phaseAwareReplay = Renderer2DSettings.canReplayPhaseAwareSpritesAfterFrame();
		boolean replayUiOverlayCommands =
			!Renderer2DSettings.isNativeUiOverlayMode() || replayNativeBaseCommands;
		for (Renderer2DFrame.SpriteCommand command : commands) {
			switch (command.getPhase()) {
				case SCENE:
					sceneCommands++;
					break;
				case WORLD_OVERLAY:
					worldCommands++;
					break;
				case UI_OVERLAY:
					uiCommands++;
					break;
				default:
					unknownCommands++;
					break;
			}
			if (phaseAwareReplay) {
				if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY && !replayUiOverlayCommands) {
					continue;
				}
				if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY && !command.requiresOrderedReplay()) {
					continue;
				} else {
					int commandVisiblePixels = drawVisibleSpriteCommand(frame, command);
					if (commandVisiblePixels > 0) {
						visibleReplayed++;
						visiblePixels += commandVisiblePixels;
						visibleReplayedByPhase[phaseIndex(command.getPhase())]++;
					} else if (commandVisiblePixels == VISIBLE_SPRITE_ATLAS_FULL) {
						skippedAtlasFull++;
					} else {
						skippedInvisible++;
					}
				}
				continue;
			}
			if (visibleReplay) {
				int commandVisiblePixels = drawVisibleSpriteCommand(frame, command);
				if (commandVisiblePixels > 0) {
					visibleReplayed++;
					visiblePixels += commandVisiblePixels;
					visibleReplayedByPhase[phaseIndex(command.getPhase())]++;
				} else if (commandVisiblePixels == VISIBLE_SPRITE_ATLAS_FULL) {
					skippedAtlasFull++;
				} else {
					skippedInvisible++;
				}
				continue;
			}
			if (command.requiresOrderedReplay()
				&& !Renderer2DSettings.canReplayOrderSensitiveSpritesAfterFrame()) {
				skippedOrdered++;
				continue;
			}
			drawSpriteCommand(command);
			staticReplayed++;
			directReplayedByPhase[phaseIndex(command.getPhase())]++;
		}
		if (phaseAwareReplay && replayUiOverlayCommands) {
			int spriteIndex = 0;
			int textIndex = 0;
			int primitiveIndex = 0;
			int rotatedSpriteIndex = 0;
			int circleIndex = 0;
			while (true) {
				while (spriteIndex < commands.length
					&& (commands[spriteIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY
						|| commands[spriteIndex].requiresOrderedReplay())) {
					spriteIndex++;
				}
				while (textIndex < textCommands.length
					&& textCommands[textIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY) {
					textIndex++;
				}
				while (primitiveIndex < primitiveCommands.length
					&& (!replayNativeBaseCommands
						|| primitiveCommands[primitiveIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY)) {
					primitiveIndex++;
				}
				while (rotatedSpriteIndex < rotatedSpriteCommands.length
					&& (!replayNativeBaseCommands
						|| rotatedSpriteCommands[rotatedSpriteIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY)) {
					rotatedSpriteIndex++;
				}
				while (circleIndex < circleCommands.length
					&& (!replayNativeBaseCommands
						|| circleCommands[circleIndex].getPhase() != Renderer2DFrame.Phase.UI_OVERLAY)) {
					circleIndex++;
				}

				boolean hasSprite = spriteIndex < commands.length;
				boolean hasText = textIndex < textCommands.length;
				boolean hasPrimitive = replayNativeBaseCommands && primitiveIndex < primitiveCommands.length;
				boolean hasRotatedSprite =
					replayNativeBaseCommands && rotatedSpriteIndex < rotatedSpriteCommands.length;
				boolean hasCircle = replayNativeBaseCommands && circleIndex < circleCommands.length;
				if (!hasSprite && !hasText && !hasPrimitive && !hasRotatedSprite && !hasCircle) {
					break;
				}
				int nextSequence = Integer.MAX_VALUE;
				if (hasSprite) {
					nextSequence = Math.min(nextSequence, commands[spriteIndex].getSequence());
				}
				if (hasText) {
					nextSequence = Math.min(nextSequence, textCommands[textIndex].getSequence());
				}
				if (hasPrimitive) {
					nextSequence = Math.min(nextSequence, primitiveCommands[primitiveIndex].getSequence());
				}
				if (hasRotatedSprite) {
					nextSequence = Math.min(nextSequence, rotatedSpriteCommands[rotatedSpriteIndex].getSequence());
				}
				if (hasCircle) {
					nextSequence = Math.min(nextSequence, circleCommands[circleIndex].getSequence());
				}

				if (hasSprite && commands[spriteIndex].getSequence() == nextSequence) {
					Renderer2DFrame.SpriteCommand command = commands[spriteIndex++];
					drawSpriteCommand(command);
					staticReplayed++;
					directReplayedByPhase[phaseIndex(command.getPhase())]++;
				} else if (hasText && textCommands[textIndex].getSequence() == nextSequence) {
					drawTextCommand(textCommands[textIndex++]);
				} else if (hasPrimitive && primitiveCommands[primitiveIndex].getSequence() == nextSequence) {
					drawPrimitiveCommand(primitiveCommands[primitiveIndex++]);
				} else if (hasRotatedSprite && rotatedSpriteCommands[rotatedSpriteIndex].getSequence() == nextSequence) {
					drawRotatedSpriteCommand(rotatedSpriteCommands[rotatedSpriteIndex++]);
				} else {
					drawCircleCommand(circleCommands[circleIndex++]);
				}
			}
		}
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glEnable(gl.GL_TEXTURE_2D);
		RenderTelemetry.recordSpriteOverlayFrame(
			commands.length,
			staticReplayed,
			visibleReplayed,
			skippedOrdered,
			skippedInvisible,
			skippedAtlasFull,
			visiblePixels,
			sceneCommands,
			worldCommands,
			uiCommands,
			unknownCommands,
			directReplayedByPhase[Renderer2DFrame.Phase.SCENE.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.WORLD_OVERLAY.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.UI_OVERLAY.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.UNKNOWN.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.SCENE.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.WORLD_OVERLAY.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.UI_OVERLAY.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.UNKNOWN.ordinal()]);
	}

	private void drawOpenGLWorldCompositeOverlay(
		Frame frame,
		Renderer2DFrame renderer2DFrame,
		Renderer2DFrame.SpriteCommand[] commands,
		Renderer2DFrame.TextCommand[] textCommands,
		Renderer2DFrame.PrimitiveCommand[] primitiveCommands,
		Renderer2DFrame.RotatedSpriteCommand[] rotatedSpriteCommands,
		Renderer2DFrame.CircleCommand[] circleCommands) throws Exception {
		legacySceneSpriteRestoreCommands = 0;
		legacySceneSpriteRestoreFallbacks = 0;
		legacySceneSpriteRestoreFallbackPixels = 0;
		resetLegacyEntitySpriteDebugFrame();
		int sceneCommands = 0;
		int worldCommands = 0;
		int uiCommands = 0;
		int unknownCommands = 0;
		for (Renderer2DFrame.SpriteCommand command : commands) {
			switch (command.getPhase()) {
				case SCENE:
					sceneCommands++;
					break;
				case WORLD_OVERLAY:
					worldCommands++;
					break;
				case UI_OVERLAY:
					uiCommands++;
					break;
				default:
					unknownCommands++;
					break;
			}
		}

		if (sceneCommands == 0
			&& worldCommands == 0
			&& unknownCommands == 0
			&& uiCommands == 0
			&& !hasUiTextCommand(textCommands)
			&& !hasUiPrimitiveCommand(primitiveCommands)
			&& !hasUiRotatedSpriteCommand(rotatedSpriteCommands)
			&& !hasUiCircleCommand(circleCommands)) {
			RenderTelemetry.recordSpriteOverlayFrame(
				commands.length,
				0,
				0,
				0,
				0,
				0,
				0,
				sceneCommands,
				worldCommands,
				uiCommands,
				unknownCommands,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0);
			return;
		}

		useSourceProjection(renderer2DFrame.getWidth(), renderer2DFrame.getHeight());
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

		int staticReplayed = 0;
		int[] directReplayedByPhase = new int[Renderer2DFrame.Phase.values().length];
		int[] visibleReplayedByPhase = new int[Renderer2DFrame.Phase.values().length];
		if (visibleSpriteTextureAtlas != null) {
			visibleSpriteTextureAtlas.beginFrame();
		}
		int visibleReplayed = 0;
		int skippedInvisible = 0;
		int skippedAtlasFull = 0;
		int visiblePixels = 0;
		useSourceProjection(renderer2DFrame.getWidth(), renderer2DFrame.getHeight());
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		boolean[] directSpriteMask =
			buildOpenGLCompositeDirectOverlayCoverageMask(
				commands,
				textCommands,
				primitiveCommands,
				renderer2DFrame.getWidth(),
				renderer2DFrame.getHeight());
		for (Renderer2DFrame.SpriteCommand command : commands) {
			if (command.getPhase() != Renderer2DFrame.Phase.SCENE
				|| isOpenGLCompositeWorldSpriteCommand(command)
				|| isOpenGLCompositeDirectSpriteCommand(command)) {
				logCompositeSpriteCommand("skip-visible", command, 0);
				continue;
			}
			int commandVisiblePixels = drawVisibleSpriteCommand(frame, command, directSpriteMask);
			logCompositeSpriteCommand(
				"visible-scene",
				command,
				commandVisiblePixels);
			if (commandVisiblePixels > 0) {
				visibleReplayed++;
				visiblePixels += commandVisiblePixels;
				visibleReplayedByPhase[phaseIndex(command.getPhase())]++;
			} else if (commandVisiblePixels == VISIBLE_SPRITE_ATLAS_FULL) {
				skippedAtlasFull++;
			} else {
				skippedInvisible++;
			}
		}
		captureLayer(activeFrameCapture, "04-scene-restore");
		// Scene restoration has already been issued. Reuse its proven atlas from the
		// beginning so overlay uploads cannot fall back when a complex scene fills it.
		if (visibleSpriteTextureAtlas != null) {
			visibleSpriteTextureAtlas.beginFrame();
		}
		List<OpenGLCompositeSceneCommand> compositeSceneCommands =
			buildOpenGLCompositeSceneCommands(frame, commands);
		for (OpenGLCompositeSceneCommand sceneCommand : compositeSceneCommands) {
			WorldSpriteCommand worldSpriteCommand = sceneCommand.worldSpriteCommand;
			Renderer2DFrame.SpriteCommand command = worldSpriteCommand.command;
			logCompositeSpriteCommand("direct-entity", command, command.getWidth() * command.getHeight());
			directReplayedByPhase[phaseIndex(command.getPhase())]++;
		}
		staticReplayed += drawOpenGLCompositeWorldSpriteCommands(frame, compositeSceneCommands);
		captureLayer(activeFrameCapture, "04b-entity-sprites");
		captureLayer(activeFrameCapture, "04c-ordered-static-overlays");

		int spriteIndex = 0;
		int textIndex = 0;
		int primitiveIndex = 0;
		int rotatedSpriteIndex = 0;
		int circleIndex = 0;
		while (true) {
			while (spriteIndex < commands.length
				&& (!isOpenGLCompositeDirectSpriteCommand(commands[spriteIndex])
					|| isOpenGLCompositeWorldSpriteCommand(commands[spriteIndex]))) {
				spriteIndex++;
			}
			while (textIndex < textCommands.length
				&& !isOpenGLWorldOverlayPhase(textCommands[textIndex].getPhase())) {
				textIndex++;
			}
			while (primitiveIndex < primitiveCommands.length
				&& !isOpenGLWorldOverlayPhase(primitiveCommands[primitiveIndex].getPhase())) {
				primitiveIndex++;
			}
			while (rotatedSpriteIndex < rotatedSpriteCommands.length
				&& !isOpenGLWorldOverlayPhase(rotatedSpriteCommands[rotatedSpriteIndex].getPhase())) {
				rotatedSpriteIndex++;
			}
			while (circleIndex < circleCommands.length
				&& !isOpenGLWorldOverlayPhase(circleCommands[circleIndex].getPhase())) {
				circleIndex++;
			}

			boolean hasSprite = spriteIndex < commands.length;
			boolean hasText = textIndex < textCommands.length;
			boolean hasPrimitive = primitiveIndex < primitiveCommands.length;
			boolean hasRotatedSprite = rotatedSpriteIndex < rotatedSpriteCommands.length;
			boolean hasCircle = circleIndex < circleCommands.length;
			if (!hasSprite && !hasText && !hasPrimitive && !hasRotatedSprite && !hasCircle) {
				break;
			}

			int nextSequence = Integer.MAX_VALUE;
			if (hasSprite) {
				nextSequence = Math.min(nextSequence, commands[spriteIndex].getSequence());
			}
			if (hasText) {
				nextSequence = Math.min(nextSequence, textCommands[textIndex].getSequence());
			}
			if (hasPrimitive) {
				nextSequence = Math.min(nextSequence, primitiveCommands[primitiveIndex].getSequence());
			}
			if (hasRotatedSprite) {
				nextSequence = Math.min(nextSequence, rotatedSpriteCommands[rotatedSpriteIndex].getSequence());
			}
			if (hasCircle) {
				nextSequence = Math.min(nextSequence, circleCommands[circleIndex].getSequence());
			}

			if (hasSprite && commands[spriteIndex].getSequence() == nextSequence) {
				Renderer2DFrame.SpriteCommand command = commands[spriteIndex++];
				logCompositeSpriteCommand("direct-overlay", command, command.getWidth() * command.getHeight());
				drawOpenGLCompositeDirectSpriteCommand(command);
				staticReplayed++;
				directReplayedByPhase[phaseIndex(command.getPhase())]++;
			} else if (hasText && textCommands[textIndex].getSequence() == nextSequence) {
				drawTextCommand(textCommands[textIndex++]);
			} else if (hasPrimitive && primitiveCommands[primitiveIndex].getSequence() == nextSequence) {
				drawPrimitiveCommand(primitiveCommands[primitiveIndex++]);
			} else if (hasRotatedSprite && rotatedSpriteCommands[rotatedSpriteIndex].getSequence() == nextSequence) {
				drawRotatedSpriteCommand(rotatedSpriteCommands[rotatedSpriteIndex++]);
			} else {
				drawCircleCommand(circleCommands[circleIndex++]);
			}
		}

		gl.glDisable(gl.GL_BLEND);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glEnable(gl.GL_TEXTURE_2D);
		RenderTelemetry.recordLegacySceneSpriteRestore(
			legacySceneSpriteRestoreCommands,
			legacySceneSpriteRestoreFallbacks,
			legacySceneSpriteRestoreFallbackPixels);
		logLegacyEntitySpriteDebugFrame();
		RenderTelemetry.recordSpriteOverlayFrame(
			commands.length,
			staticReplayed,
			visibleReplayed,
			0,
			skippedInvisible,
			skippedAtlasFull,
			visiblePixels,
			sceneCommands,
			worldCommands,
			uiCommands,
			unknownCommands,
			directReplayedByPhase[Renderer2DFrame.Phase.SCENE.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.WORLD_OVERLAY.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.UI_OVERLAY.ordinal()],
			directReplayedByPhase[Renderer2DFrame.Phase.UNKNOWN.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.SCENE.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.WORLD_OVERLAY.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.UI_OVERLAY.ordinal()],
			visibleReplayedByPhase[Renderer2DFrame.Phase.UNKNOWN.ordinal()]);
	}

	private boolean isOpenGLWorldOverlayPhase(Renderer2DFrame.Phase phase) {
		return phase == Renderer2DFrame.Phase.WORLD_OVERLAY
			|| phase == Renderer2DFrame.Phase.UI_OVERLAY;
	}

	private boolean isOpenGLCompositeDirectSpriteCommand(Renderer2DFrame.SpriteCommand command) {
		if (command == null) {
			return false;
		}
		return isOpenGLWorldOverlayPhase(command.getPhase())
			|| (command.getPhase() == Renderer2DFrame.Phase.SCENE
				&& command.getAlpha() < Renderer2DFrame.SpriteCommand.FULL_ALPHA);
	}

	private void logCompositeSpriteCommand(
		String stage,
		Renderer2DFrame.SpriteCommand command,
		int visiblePixels) {
		if (!WORLD_COMPOSITE_DEBUG
			|| command == null
			|| worldCompositeDebugLogs >= WORLD_COMPOSITE_DEBUG_LOG_LIMIT
			|| !intersectsUpperLeftSource(command)) {
			return;
		}
		worldCompositeDebugLogs++;
		log("OpenGL composite sprite "
			+ stage
			+ " phase="
			+ command.getPhase()
			+ " seq="
			+ command.getSequence()
			+ " legacySprite="
			+ command.getLegacySpriteId()
			+ " dst="
			+ command.getX()
			+ ","
			+ command.getY()
			+ " "
			+ command.getWidth()
			+ "x"
			+ command.getHeight()
			+ " src="
			+ command.getSourceX()
			+ ","
			+ command.getSourceY()
			+ " "
			+ command.getSourceWidth()
			+ "x"
			+ command.getSourceHeight()
			+ " alpha="
			+ command.getAlpha()
			+ " visible="
			+ visiblePixels);
	}

	private boolean intersectsUpperLeftSource(Renderer2DFrame.SpriteCommand command) {
		int left = Math.min(command.getTopX16(), command.getBottomX16()) >> 16;
		int right = (Math.max(command.getTopX16(), command.getBottomX16()) >> 16) + command.getWidth();
		int top = command.getY();
		int bottom = command.getY() + command.getHeight();
		int limitX = Math.max(96, currentSourceWidth / 3);
		int limitY = Math.max(96, currentSourceHeight / 3);
		return right > 0 && bottom > 0 && left < limitX && top < limitY;
	}

	private boolean hasUiTextCommand(Renderer2DFrame.TextCommand[] commands) {
		for (Renderer2DFrame.TextCommand command : commands) {
			if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUiPrimitiveCommand(Renderer2DFrame.PrimitiveCommand[] commands) {
		for (Renderer2DFrame.PrimitiveCommand command : commands) {
			if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUiRotatedSpriteCommand(Renderer2DFrame.RotatedSpriteCommand[] commands) {
		for (Renderer2DFrame.RotatedSpriteCommand command : commands) {
			if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY) {
				return true;
			}
		}
		return false;
	}

	private boolean hasUiCircleCommand(Renderer2DFrame.CircleCommand[] commands) {
		for (Renderer2DFrame.CircleCommand command : commands) {
			if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY) {
				return true;
			}
		}
		return false;
	}

	private int phaseIndex(Renderer2DFrame.Phase phase) {
		return phase == null ? Renderer2DFrame.Phase.UNKNOWN.ordinal() : phase.ordinal();
	}

	private boolean shouldReplayNativeBaseCommands(Renderer2DFrame renderer2DFrame) {
		return Renderer2DSettings.canPresentUiBaseFrame()
			&& renderer2DFrame != null
			&& renderer2DFrame.getCaptureStats().isNativeUiBaseEligible();
	}

	private void drawPrimitiveCommand(Renderer2DFrame.PrimitiveCommand command) throws Exception {
		prepareOverlaySolidReplayState();
		float red = ((command.getColor() >> 16) & 0xFF) / 255.0f;
		float green = ((command.getColor() >> 8) & 0xFF) / 255.0f;
		float blue = (command.getColor() & 0xFF) / 255.0f;
		float alpha = command.getAlpha() / 256.0f;
		float x0 = command.getX();
		float y0 = command.getY();
		float x1 = command.getX() + command.getWidth();
		float y1 = command.getY() + command.getHeight();

		gl.glDisable(gl.GL_TEXTURE_2D);
		try {
			gl.glColor4f(red, green, blue, alpha);
			drawScreenSolidQuad(x0, y0, x1, y1);
		} finally {
			gl.glEnable(gl.GL_TEXTURE_2D);
		}
	}

	private void drawCircleCommand(Renderer2DFrame.CircleCommand command) throws Exception {
		prepareOverlaySolidReplayState();
		float red = ((command.getColor() >> 16) & 0xFF) / 255.0f;
		float green = ((command.getColor() >> 8) & 0xFF) / 255.0f;
		float blue = (command.getColor() & 0xFF) / 255.0f;
		float alpha = command.getAlpha() / 256.0f;
		int radius = command.getRadius();
		int radiusSquared = radius * radius;

		gl.glDisable(gl.GL_TEXTURE_2D);
		try {
			gl.glColor4f(red, green, blue, alpha);
			for (int py = command.getY() - radius; py <= command.getY() + radius; py++) {
				int dy = py - command.getY();
				int horizHalf = (int) Math.sqrt((double) (radiusSquared - dy * dy));
				float x0 = command.getX() - horizHalf;
				float x1 = command.getX() + horizHalf + 1.0f;
				float y0 = py;
				float y1 = py + 1.0f;
				drawScreenSolidQuad(x0, y0, x1, y1);
			}
		} finally {
			gl.glEnable(gl.GL_TEXTURE_2D);
		}
	}

	private void drawRotatedSpriteCommand(Renderer2DFrame.RotatedSpriteCommand command) throws Exception {
		prepareOverlayTexturedReplayState();
		OpenGLTextureRegion region = uploadSpriteTexture(command.getSprite(), command.isTransparentMask());

		enableSourceClip(
			command.getClipLeft(),
			command.getClipTop(),
			command.getClipRight(),
			command.getClipBottom());
		try {
			gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			drawScreenQuad(
				command.getX0(),
				command.getY0(),
				region.getU0(),
				region.getV0(),
				command.getX1(),
				command.getY1(),
				region.getU1(),
				region.getV0(),
				command.getX2(),
				command.getY2(),
				region.getU1(),
				region.getV1(),
				command.getX3(),
				command.getY3(),
				region.getU0(),
				region.getV1());
		} finally {
			disableSourceClip();
		}
	}

	private void enableSourceClip(int clipLeft, int clipTop, int clipRight, int clipBottom) throws Exception {
		Viewport viewport = currentFramebufferViewport;
		int sourceWidth = Math.max(1, currentSourceWidth);
		int sourceHeight = Math.max(1, currentSourceHeight);
		int left = Math.max(0, Math.min(sourceWidth, clipLeft));
		int top = Math.max(0, Math.min(sourceHeight, clipTop));
		int right = Math.max(left, Math.min(sourceWidth, clipRight));
		int bottom = Math.max(top, Math.min(sourceHeight, clipBottom));
		double scaleX = viewport.width / (double) sourceWidth;
		double scaleY = viewport.height / (double) sourceHeight;
		int x0 = viewport.x + (int) Math.floor(left * scaleX);
		int x1 = viewport.x + (int) Math.ceil(right * scaleX);
		int y0 = viewport.y + (int) Math.floor((sourceHeight - bottom) * scaleY);
		int y1 = viewport.y + (int) Math.ceil((sourceHeight - top) * scaleY);
		x0 = clamp(x0, viewport.x, viewport.x + viewport.width);
		x1 = clamp(x1, x0, viewport.x + viewport.width);
		y0 = clamp(y0, viewport.y, viewport.y + viewport.height);
		y1 = clamp(y1, y0, viewport.y + viewport.height);

		gl.glEnable(gl.GL_SCISSOR_TEST);
		gl.glScissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
	}

	private void disableSourceClip() throws Exception {
		gl.glDisable(gl.GL_SCISSOR_TEST);
	}

	private void drawSpriteCommand(Renderer2DFrame.SpriteCommand command) throws Exception {
		prepareOverlayTexturedReplayState();
		OpenGLTextureRegion region = uploadSpriteTexture(command.getSprite(), command.getTransform());
		float alpha = command.getAlpha() / (float) Renderer2DFrame.SpriteCommand.FULL_ALPHA;
		float topX0 = command.getTopX16() / 65536.0f;
		float bottomX0 = command.getBottomX16() / 65536.0f;
		float y0 = command.getY();
		float y1 = command.getY() + command.getHeight();
		float topX1 = topX0 + command.getWidth();
		float bottomX1 = bottomX0 + command.getWidth();
		float uSpan = region.getU1() - region.getU0();
		float vSpan = region.getV1() - region.getV0();
		float u0 = region.getU0() + uSpan * command.getSourceX() / region.getWidth();
		float v0 = region.getV0() + vSpan * command.getSourceY() / region.getHeight();
		float u1 = region.getU0() + uSpan * (command.getSourceX() + command.getSourceWidth()) / region.getWidth();
		float v1 = region.getV0() + vSpan * (command.getSourceY() + command.getSourceHeight()) / region.getHeight();
		float leftU = command.isMirrorX() ? u1 : u0;
		float rightU = command.isMirrorX() ? u0 : u1;

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		drawScreenQuad(
			topX0,
			y0,
			leftU,
			v0,
			topX1,
			y0,
			rightU,
			v0,
			bottomX1,
			y1,
			rightU,
			v1,
			bottomX0,
			y1,
			leftU,
			v1);
	}

	private void drawTextCommand(Renderer2DFrame.TextCommand command) throws Exception {
		if (glyphTextureCache == null) {
			return;
		}

		prepareOverlayTexturedReplayState();
		for (Renderer2DFrame.TextCommand.GlyphCommand glyph : command.getGlyphs()) {
			OpenGLTextureRegion region = glyphTextureCache.getOrUpload(glyph);
			float red = ((glyph.getColor() >> 16) & 0xFF) / 255.0f;
			float green = ((glyph.getColor() >> 8) & 0xFF) / 255.0f;
			float blue = (glyph.getColor() & 0xFF) / 255.0f;
			float x0 = glyph.getX();
			float y0 = glyph.getY();
			float x1 = glyph.getX() + glyph.getWidth();
			float y1 = glyph.getY() + glyph.getHeight();

			gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
			gl.glColor4f(red, green, blue, 1.0f);
			drawScreenTexturedQuad(
				x0,
				y0,
				x1,
				y1,
				region.getU0(),
				region.getV0(),
				region.getU1(),
				region.getV1());
		}
	}

	private void drawRendererDebugOverlay(Frame frame) throws Exception {
		if (frame.rendererDebugOverlayLines == null
			|| frame.rendererDebugOverlayLines.length == 0
			|| debugOverlayTextureAtlas == null) {
			return;
		}

		long now = RenderTelemetry.now();
		if (debugOverlayTextureRegion == null || now >= debugOverlayNextTextureUpdateNanos) {
			DynamicTextureData textureData = buildRendererDebugOverlayTexture(frame.rendererDebugOverlayLines);
			if (textureData == null || textureData.visiblePixelCount <= 0) {
				return;
			}
			debugOverlayTextureAtlas.beginFrame();
			OpenGLTextureRegion uploadedRegion = debugOverlayTextureAtlas.upload(textureData);
			if (uploadedRegion != null) {
				debugOverlayTextureRegion = uploadedRegion;
				debugOverlayTextureWidth = textureData.width;
				debugOverlayTextureHeight = textureData.height;
				debugOverlayNextTextureUpdateNanos = now + DEBUG_OVERLAY_TEXTURE_UPDATE_NANOS;
			}
		}

		OpenGLTextureRegion region = debugOverlayTextureRegion;
		if (region == null) {
			return;
		}

		useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		prepareOverlayTexturedReplayState();
		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
		gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		float x0 = 6.0f;
		float y0 = 6.0f;
		float x1 = x0 + debugOverlayTextureWidth;
		float y1 = y0 + debugOverlayTextureHeight;
		drawScreenTexturedQuad(
			x0,
			y0,
			x1,
			y1,
			region.getU0(),
			region.getV0(),
			region.getU1(),
			region.getV1());
	}

	private static DynamicTextureData buildRendererDebugOverlayTexture(String[] lines) {
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D scratchGraphics = scratch.createGraphics();
		Font font = new Font("Monospaced", Font.PLAIN, 12);
		int width;
		int height;
		int padding = 6;
		int lineHeight;
		try {
			scratchGraphics.setFont(font);
			FontMetrics metrics = scratchGraphics.getFontMetrics();
			int maxWidth = 0;
			for (String line : lines) {
				if (line != null && !line.isEmpty()) {
					maxWidth = Math.max(maxWidth, metrics.stringWidth(line));
				}
			}
			lineHeight = metrics.getHeight();
			width = Math.max(1, Math.min(DEBUG_OVERLAY_MAX_TEXTURE_WIDTH, maxWidth + padding * 2 + 1));
			height = Math.max(1, Math.min(DEBUG_OVERLAY_MAX_TEXTURE_HEIGHT, lines.length * lineHeight + padding * 2 + 1));
		} finally {
			scratchGraphics.dispose();
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			graphics.setFont(font);
			FontMetrics metrics = graphics.getFontMetrics();
			graphics.setClip(0, 0, width, height);
			graphics.setComposite(AlphaComposite.SrcOver.derive(0.76f));
			graphics.setColor(Color.black);
			graphics.fillRect(0, 0, width, height);
			graphics.setComposite(AlphaComposite.SrcOver);
			graphics.setColor(new Color(70, 170, 255));
			graphics.drawRect(0, 0, width - 1, height - 1);
			int y = padding + metrics.getAscent();
			for (int i = 0; i < lines.length; i++) {
				graphics.setColor(i == 0 ? new Color(190, 230, 255) : Color.white);
				graphics.drawString(lines[i] == null ? "" : lines[i], padding, y);
				y += lineHeight;
			}
		} finally {
			graphics.dispose();
		}

		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
		int visiblePixelCount = 0;
		int[] rowPixels = new int[width];
		for (int y = 0; y < height; y++) {
			image.getRGB(0, y, width, 1, rowPixels, 0, width);
			for (int x = 0; x < width; x++) {
				int pixel = rowPixels[x];
				int alpha = (pixel >>> 24) & 0xFF;
				if (alpha != 0) {
					visiblePixelCount++;
				}
				pixels.put((byte) ((pixel >> 16) & 0xFF));
				pixels.put((byte) ((pixel >> 8) & 0xFF));
				pixels.put((byte) (pixel & 0xFF));
				pixels.put((byte) alpha);
			}
		}
		pixels.flip();
		return new DynamicTextureData(width, height, pixels, visiblePixelCount);
	}

	private void prepareOverlayTexturedReplayState() throws Exception {
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glDisable(gl.GL_ALPHA_TEST);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(gl.GL_TEXTURE_2D);
	}

	private void prepareOverlaySolidReplayState() throws Exception {
		gl.glDisable(gl.GL_DEPTH_TEST);
		gl.glDisable(gl.GL_ALPHA_TEST);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(gl.GL_TEXTURE_2D);
	}

	private int drawVisibleSpriteCommand(Frame frame, Renderer2DFrame.SpriteCommand command) throws Exception {
		return drawVisibleSpriteCommand(frame, command, null);
	}

	private int drawVisibleSpriteCommand(
		Frame frame,
		Renderer2DFrame.SpriteCommand command,
		boolean[] clippedSceneRestoreMask) throws Exception {
		if (visibleSpriteTextureAtlas == null) {
			return 0;
		}

		DynamicTextureData textureData = buildVisibleSpriteTexture(frame, command, clippedSceneRestoreMask);
		boolean legacySceneSpriteCommand = isLegacySceneSpriteCommand(command);
		boolean legacyEntitySpriteCommand = isLegacyEntitySpriteCommand(command);
		int directPixelCount = 0;
		boolean usedDirectFallback = false;
		if (legacySceneSpriteCommand) {
			legacySceneSpriteRestoreCommands++;
		}
		if (textureData == null
			&& clippedSceneRestoreMask != null
			&& legacySceneSpriteCommand) {
			textureData = buildVisibleSpriteTexture(frame, command, null);
		}
		if (legacySceneSpriteCommand && !legacyEntitySpriteCommand) {
			DynamicTextureData directTextureData = null;
			if (textureData == null) {
				directTextureData = buildDirectSpriteTexture(command);
				directPixelCount = directTextureData == null ? 0 : directTextureData.visiblePixelCount;
				textureData = directTextureData;
				if (directTextureData != null) {
					legacySceneSpriteRestoreFallbacks++;
					legacySceneSpriteRestoreFallbackPixels += directTextureData.visiblePixelCount;
					usedDirectFallback = true;
				}
			} else {
				directTextureData = buildDirectSpriteTexture(command);
				directPixelCount = directTextureData == null ? 0 : directTextureData.visiblePixelCount;
				if (directTextureData != null
					&& textureData.visiblePixelCount * 10 < directTextureData.visiblePixelCount) {
					textureData = directTextureData;
					legacySceneSpriteRestoreFallbacks++;
					legacySceneSpriteRestoreFallbackPixels += directTextureData.visiblePixelCount;
					usedDirectFallback = true;
				}
			}
		}
		if (legacyEntitySpriteCommand && textureData == null) {
			Renderer3DFrame.SpriteAnchor anchor = findSpriteAnchor(frame, command);
			DynamicTextureData depthTextureData =
				buildDepthVisibleEntitySpriteTexture(frame, command, anchor, null, null);
			if (depthTextureData != null) {
				textureData = depthTextureData;
				legacySceneSpriteRestoreFallbacks++;
				legacySceneSpriteRestoreFallbackPixels += depthTextureData.visiblePixelCount;
				usedDirectFallback = true;
				directPixelCount = depthTextureData.visiblePixelCount;
			}
		}
		if (textureData == null) {
			if (legacyEntitySpriteCommand) {
				if (directPixelCount == 0) {
					DynamicTextureData directTextureData = buildDirectSpriteTexture(command);
					directPixelCount = directTextureData == null ? 0 : directTextureData.visiblePixelCount;
				}
				recordLegacyEntitySpriteDebug(command, 0, directPixelCount, false, false, true);
			}
			return 0;
		}

		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);
		if (region == null) {
			if (legacyEntitySpriteCommand) {
				recordLegacyEntitySpriteDebug(
					command,
					textureData.visiblePixelCount,
					directPixelCount,
					usedDirectFallback,
					true,
					false);
			}
			return VISIBLE_SPRITE_ATLAS_FULL;
		}
		drawSpriteRegion(command, region, region.getU0(), region.getU1(), region.getV0(), region.getV1());
		if (legacyEntitySpriteCommand) {
			recordLegacyEntitySpriteDebug(
				command,
				textureData.visiblePixelCount,
				directPixelCount,
				usedDirectFallback,
				false,
				false);
		}
		return textureData.visiblePixelCount;
	}

	private boolean isLegacySceneSpriteCommand(Renderer2DFrame.SpriteCommand command) {
		return command != null
			&& command.getPhase() == Renderer2DFrame.Phase.SCENE
			&& command.getLegacySpriteId() >= 0;
	}

	private boolean isLegacyEntitySpriteCommand(Renderer2DFrame.SpriteCommand command) {
		if (!isLegacySceneSpriteCommand(command)) {
			return false;
		}
		int legacySpriteId = command.getLegacySpriteId();
		return (legacySpriteId >= 5000 && legacySpriteId < 20000)
			|| (legacySpriteId >= 20000 && legacySpriteId < 40000);
	}

	private boolean isLegacyGroundItemSpriteCommand(Renderer2DFrame.SpriteCommand command) {
		if (!isLegacySceneSpriteCommand(command)) {
			return false;
		}
		int legacySpriteId = command.getLegacySpriteId();
		return legacySpriteId >= 40000 && legacySpriteId < 50000;
	}

	private boolean isOpenGLCompositeWorldSpriteCommand(Renderer2DFrame.SpriteCommand command) {
		return isLegacyEntitySpriteCommand(command) || isLegacyGroundItemSpriteCommand(command);
	}

	private void resetLegacyEntitySpriteDebugFrame() {
		legacyEntitySpriteDebugCommands = 0;
		legacyEntitySpriteDebugVisiblePixels = 0;
		legacyEntitySpriteDebugDirectPixels = 0;
		legacyEntitySpriteDebugFallbacks = 0;
		legacyEntitySpriteDebugSkipped = 0;
		legacyEntitySpriteDebugAtlasFull = 0;
		legacyEntitySpriteDebugFirstId = -1;
		legacyEntitySpriteDebugLastId = -1;
		legacyEntitySpriteDebugById.clear();
		legacyEntitySpriteDepthEvaluations.clear();
	}

	private void recordLegacyEntitySpriteDebug(
		Renderer2DFrame.SpriteCommand command,
		int visiblePixels,
		int directPixels,
		boolean fallback,
		boolean atlasFull,
		boolean skipped) {
		legacyEntitySpriteDebugCommands++;
		legacyEntitySpriteDebugVisiblePixels += Math.max(0, visiblePixels);
		legacyEntitySpriteDebugDirectPixels += Math.max(0, directPixels);
		if (fallback) {
			legacyEntitySpriteDebugFallbacks++;
		}
		if (atlasFull) {
			legacyEntitySpriteDebugAtlasFull++;
		}
		if (skipped) {
			legacyEntitySpriteDebugSkipped++;
		}
		int legacySpriteId = command.getLegacySpriteId();
		if (legacyEntitySpriteDebugFirstId < 0) {
			legacyEntitySpriteDebugFirstId = legacySpriteId;
		}
		legacyEntitySpriteDebugLastId = legacySpriteId;
		LegacyEntitySpriteDebugStats stats = legacyEntitySpriteDebugById.get(legacySpriteId);
		if (stats == null) {
			stats = new LegacyEntitySpriteDebugStats(legacySpriteId);
			legacyEntitySpriteDebugById.put(legacySpriteId, stats);
		}
		stats.record(command, visiblePixels, directPixels, fallback, atlasFull, skipped);
	}

	private void recordLegacyEntitySpriteDepthFallbackMiss(
		Renderer2DFrame.SpriteCommand command,
		int sourcePixels,
		int occludedPixels,
		int clippedPixels,
		int outOfBoundsPixels,
		Renderer3DFrame.SpriteAnchor anchor) {
		int legacySpriteId = command.getLegacySpriteId();
		LegacyEntitySpriteDebugStats stats = legacyEntitySpriteDebugById.get(legacySpriteId);
		if (stats == null) {
			stats = new LegacyEntitySpriteDebugStats(legacySpriteId);
			legacyEntitySpriteDebugById.put(legacySpriteId, stats);
		}
		stats.recordDepthFallbackMiss(sourcePixels, occludedPixels, clippedPixels, outOfBoundsPixels, anchor);
	}

	private void recordLegacyEntitySpriteDepthEvaluation(
		Renderer2DFrame.SpriteCommand command,
		int sourcePixels,
		int visiblePixels,
		int occludedPixels,
		int clippedPixels,
		int outOfBoundsPixels,
		int terrainOccludedPixels,
		int wallOccludedPixels,
		int roofOccludedPixels,
		int gameObjectOccludedPixels,
		int wallObjectOccludedPixels,
		int minOccluderLegacyDrawOrder,
		int maxOccluderLegacyDrawOrder,
		int minOccluderDepth,
		int maxOccluderDepth,
		EntitySpriteOccluderFaceStats dominantOccluderFace,
		WorldSpriteAnchorMatch anchorMatch,
		Renderer3DFrame.SpriteAnchor anchor) {
		int legacySpriteId = command.getLegacySpriteId();
		LegacyEntitySpriteDebugStats stats = legacyEntitySpriteDebugById.get(legacySpriteId);
		if (stats == null) {
			stats = new LegacyEntitySpriteDebugStats(legacySpriteId);
			legacyEntitySpriteDebugById.put(legacySpriteId, stats);
		}
		stats.recordDepthEvaluation(
			sourcePixels,
			visiblePixels,
			occludedPixels,
			clippedPixels,
			outOfBoundsPixels,
			anchor);
		legacyEntitySpriteDepthEvaluations.add(new LegacyEntitySpriteDepthEvaluation(
			legacyEntitySpriteDepthEvaluations.size(),
			command,
			sourcePixels,
			visiblePixels,
			occludedPixels,
			clippedPixels,
			outOfBoundsPixels,
			terrainOccludedPixels,
			wallOccludedPixels,
			roofOccludedPixels,
			gameObjectOccludedPixels,
			wallObjectOccludedPixels,
			minOccluderLegacyDrawOrder,
			maxOccluderLegacyDrawOrder,
			minOccluderDepth,
			maxOccluderDepth,
			dominantOccluderFace,
			anchorMatch,
			anchor));
	}

	private static EntitySpriteOccluderFaceStats dominantOccluderFace(
		Map<Integer, EntitySpriteOccluderFaceStats> occluderFaceStatsById) {
		EntitySpriteOccluderFaceStats dominant = null;
		for (EntitySpriteOccluderFaceStats stats : occluderFaceStatsById.values()) {
			if (dominant == null
				|| stats.pixels > dominant.pixels
				|| (stats.pixels == dominant.pixels && stats.faceId < dominant.faceId)) {
				dominant = stats;
			}
		}
		return dominant;
	}

	private void logLegacyEntitySpriteDebugFrame() {
		if (legacyEntitySpriteDebugCommands <= 0) {
			return;
		}
		legacyEntitySpriteDebugFrames++;
		if (legacyEntitySpriteDebugFrames % 30 != 0) {
			return;
		}
		log("OpenGL entity sprite restore frame commands="
			+ legacyEntitySpriteDebugCommands
			+ " visiblePixels="
			+ legacyEntitySpriteDebugVisiblePixels
			+ " directPixels="
			+ legacyEntitySpriteDebugDirectPixels
			+ " fallbacks="
			+ legacyEntitySpriteDebugFallbacks
			+ " skipped="
			+ legacyEntitySpriteDebugSkipped
			+ " atlasFull="
			+ legacyEntitySpriteDebugAtlasFull
			+ " firstId="
			+ legacyEntitySpriteDebugFirstId
			+ " lastId="
			+ legacyEntitySpriteDebugLastId);
		log("OpenGL entity sprite restore ids " + describeLegacyEntitySpriteDebugIds());
	}

	private String describeLegacyEntitySpriteDebugIds() {
		if (legacyEntitySpriteDebugById.isEmpty()) {
			return "none";
		}
		List<LegacyEntitySpriteDebugStats> stats =
			new ArrayList<LegacyEntitySpriteDebugStats>(legacyEntitySpriteDebugById.values());
		Collections.sort(stats, new Comparator<LegacyEntitySpriteDebugStats>() {
			@Override
			public int compare(LegacyEntitySpriteDebugStats a, LegacyEntitySpriteDebugStats b) {
				if (a.skipped != b.skipped) {
					return b.skipped - a.skipped;
				}
				if (a.visiblePixels != b.visiblePixels) {
					return a.visiblePixels - b.visiblePixels;
				}
				return a.legacySpriteId - b.legacySpriteId;
			}
		});
		StringBuilder builder = new StringBuilder();
		int count = Math.min(8, stats.size());
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				builder.append(" | ");
			}
			builder.append(stats.get(i).describe());
		}
		return builder.toString();
	}

	private void drawOpenGLCompositeEntitySpriteCommand(
		Frame frame,
		Renderer2DFrame.SpriteCommand command) throws Exception {
		drawOpenGLCompositeWorldSpriteCommand(frame, buildOpenGLCompositeWorldSpriteCommand(frame, command));
	}

	private int drawOpenGLCompositeWorldSpriteCommands(
		Frame frame,
		List<OpenGLCompositeSceneCommand> sceneCommands) throws Exception {
		if (frame == null || frame.renderer3DFrame == null || sceneCommands == null || sceneCommands.isEmpty()) {
			return 0;
		}
		int drawn = 0;
		for (int index = 0; index < sceneCommands.size(); index++) {
			OpenGLCompositeSceneCommand sceneCommand = sceneCommands.get(index);
			WorldSpriteCommand worldSpriteCommand = sceneCommand.worldSpriteCommand;
			if (worldSpriteCommand == null || worldSpriteCommand.command == null) {
				continue;
			}
			if (worldSpriteCommand.anchor != null && isLegacyEntitySpriteCommand(worldSpriteCommand.command)) {
				List<WorldSpriteCommand> characterLayerCommands = new ArrayList<WorldSpriteCommand>();
				characterLayerCommands.add(worldSpriteCommand);
				while (index + 1 < sceneCommands.size()) {
					WorldSpriteCommand nextWorldSpriteCommand = sceneCommands.get(index + 1).worldSpriteCommand;
					if (nextWorldSpriteCommand == null
						|| nextWorldSpriteCommand.command == null
						|| !isLegacyEntitySpriteCommand(nextWorldSpriteCommand.command)
						|| !sameWorldSpriteAnchor(worldSpriteCommand.anchor, nextWorldSpriteCommand.anchor)) {
						break;
					}
					characterLayerCommands.add(nextWorldSpriteCommand);
					index++;
				}
				if (characterLayerCommands.size() > 1
					&& drawOpenGLCompositeCharacterSpriteLayers(frame, worldSpriteCommand.anchor, characterLayerCommands)) {
					drawn += characterLayerCommands.size();
					continue;
				}
			}
			drawOpenGLCompositeWorldSpriteCommand(frame, worldSpriteCommand);
			drawn++;
		}
		worldSpriteDepthDrawCommands = drawn;
		worldSpriteDepthTextureBatches = drawn;
		return drawn;
	}
	private boolean sameWorldSpriteAnchor(
		Renderer3DFrame.SpriteAnchor left,
		Renderer3DFrame.SpriteAnchor right) {
		if (left == right) {
			return true;
		}
		return left != null
			&& right != null
			&& left.getFaceId() == right.getFaceId()
			&& left.getLegacyDrawOrder() == right.getLegacyDrawOrder();
	}

	private boolean drawOpenGLCompositeCharacterSpriteLayers(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		List<WorldSpriteCommand> layerCommands) throws Exception {
		CompositeWorldSpriteTexture compositeTexture = buildCompositeCharacterSpriteTexture(layerCommands);
		if (compositeTexture == null) {
			return false;
		}
		return drawOpenGLCompositeDepthOwnedWorldSpriteTextureData(
			frame,
			anchor,
			compositeTexture,
			1.0f);
	}

	private void drawOpenGLCompositeWorldSpriteCommand(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand) throws Exception {
		if (worldSpriteCommand == null) {
			return;
		}
		Renderer2DFrame.SpriteCommand command = worldSpriteCommand.command;
		DynamicTextureData depthDiagnosticTexture = null;
		if (worldSpriteCommand.anchor != null && activeFrameCapture != null) {
			depthDiagnosticTexture = buildDepthVisibleEntitySpriteTexture(
				frame,
				command,
				worldSpriteCommand.anchor,
				null,
				worldSpriteCommand.anchorMatch);
		}
		boolean drawn;
		if (worldSpriteCommand.anchor != null) {
			DynamicTextureData textureData = buildDirectSpriteTexture(command);
			drawn = drawOpenGLCompositeDepthOwnedWorldSpriteTextureData(
				frame,
				worldSpriteCommand,
				textureData,
				command.getAlpha() / (float) Renderer2DFrame.SpriteCommand.FULL_ALPHA);
		} else {
			DynamicTextureData textureData = depthDiagnosticTexture;
			if (textureData == null) {
				textureData = buildDirectSpriteTexture(command);
			}
			drawn = textureData != null
				&& drawOpenGLCompositeDynamicSpriteTexture(command, textureData, 1.0f);
		}
		if (!drawn) {
			drawSpriteCommand(command);
		}
	}

	private boolean drawOpenGLCompositeDepthOwnedWorldSpriteTextureData(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand,
		DynamicTextureData textureData,
		float alpha) throws Exception {
		if (visibleSpriteTextureAtlas == null || textureData == null) {
			return false;
		}
		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);
		if (region == null) {
			visibleSpriteTextureAtlas.beginFrame();
			region = visibleSpriteTextureAtlas.upload(textureData);
			if (region == null) {
				return false;
			}
		}
		return drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
			frame,
			worldSpriteCommand,
			region,
			alpha,
			true);
	}

	private boolean drawOpenGLCompositeDepthOwnedWorldSpriteTextureData(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		CompositeWorldSpriteTexture compositeTexture,
		float alpha) throws Exception {
		if (visibleSpriteTextureAtlas == null || compositeTexture == null || compositeTexture.textureData == null) {
			return false;
		}
		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(compositeTexture.textureData);
		if (region == null) {
			visibleSpriteTextureAtlas.beginFrame();
			region = visibleSpriteTextureAtlas.upload(compositeTexture.textureData);
			if (region == null) {
				return false;
			}
		}
		return drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
			frame,
			anchor,
			compositeTexture.x,
			compositeTexture.y,
			compositeTexture.width,
			compositeTexture.height,
			region,
			alpha);
	}

	private boolean drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		return drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
			frame,
			worldSpriteCommand,
			region,
			alpha,
			false);
	}

	private boolean drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand,
		OpenGLTextureRegion region,
		float alpha,
		boolean fullRegion) throws Exception {
		if (region == null
			|| frame == null
			|| frame.renderer3DFrame == null
			|| worldSpriteCommand == null
			|| worldSpriteCommand.anchor == null) {
			return false;
		}
		useWorldMeshProjection(frame);
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(gl.GL_ALPHA_TEST);
		gl.glAlphaFunc(gl.GL_GREATER, 0.0f);
		try {
			if (fullRegion) {
				drawCameraSpaceWorldSpriteFullRegion(
					frame.renderer3DFrame,
					worldSpriteCommand.command,
					worldSpriteCommand.anchor,
					region,
					alpha);
			} else {
				drawCameraSpaceWorldSpriteRegion(
					frame.renderer3DFrame,
					worldSpriteCommand.command,
					worldSpriteCommand.anchor,
					region,
					alpha);
			}
		} finally {
			gl.glDepthMask(true);
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_ALPHA_TEST);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		return true;
	}

	private boolean drawOpenGLCompositeDepthOwnedWorldSpriteTexture(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		int x,
		int y,
		int width,
		int height,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		if (region == null
			|| frame == null
			|| frame.renderer3DFrame == null
			|| anchor == null
			|| width <= 0
			|| height <= 0) {
			return false;
		}
		useWorldMeshProjection(frame);
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(gl.GL_ALPHA_TEST);
		gl.glAlphaFunc(gl.GL_GREATER, 0.0f);
		try {
			drawCameraSpaceWorldSpriteScreenRegion(
				frame.renderer3DFrame,
				anchor,
				x,
				y,
				width,
				height,
				region,
				alpha);
		} finally {
			gl.glDepthMask(true);
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_ALPHA_TEST);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		return true;
	}

	private void initializeUnitQuadBuffers() throws Exception {
		unitQuadVertexBufferId = gl.glGenBuffers();
		unitQuadIndexBufferId = gl.glGenBuffers();
		FloatBuffer vertices = ByteBuffer
			.allocateDirect(UNIT_QUAD_VERTEX_COUNT * UNIT_QUAD_FLOATS_PER_VERTEX * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		putUnitQuadVertex(vertices, 0.0f, 0.0f, 0.0f, 0.0f);
		putUnitQuadVertex(vertices, 1.0f, 0.0f, 1.0f, 0.0f);
		putUnitQuadVertex(vertices, 1.0f, 1.0f, 1.0f, 1.0f);
		putUnitQuadVertex(vertices, 0.0f, 1.0f, 0.0f, 1.0f);
		vertices.flip();
		IntBuffer indices = ByteBuffer
			.allocateDirect(UNIT_QUAD_INDEX_COUNT * 4)
			.order(ByteOrder.nativeOrder())
			.asIntBuffer();
		putQuadIndices(indices);
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, unitQuadVertexBufferId);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, vertices, gl.GL_STATIC_DRAW);
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, unitQuadIndexBufferId);
		gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indices, gl.GL_STATIC_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private static void putUnitQuadVertex(FloatBuffer vertices, float x, float y, float u, float v) {
		vertices.put(x);
		vertices.put(y);
		vertices.put(u);
		vertices.put(v);
	}

	private static void putQuadIndices(IntBuffer indices) {
		indices.put(0);
		indices.put(1);
		indices.put(2);
		indices.put(0);
		indices.put(2);
		indices.put(3);
		indices.flip();
	}

	private void deleteUnitQuadBuffers() throws Exception {
		if (unitQuadVertexBufferId != 0) {
			gl.glDeleteBuffers(unitQuadVertexBufferId);
			unitQuadVertexBufferId = 0;
		}
		if (unitQuadIndexBufferId != 0) {
			gl.glDeleteBuffers(unitQuadIndexBufferId);
			unitQuadIndexBufferId = 0;
		}
	}

	private void initializeScreenQuadBuffers() throws Exception {
		screenQuadVertexBufferId = gl.glGenBuffers();
		screenQuadUploadBuffer = ByteBuffer
			.allocateDirect(UNIT_QUAD_VERTEX_COUNT * UNIT_QUAD_FLOATS_PER_VERTEX * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
	}

	private void deleteScreenQuadBuffers() throws Exception {
		if (screenQuadVertexBufferId != 0) {
			gl.glDeleteBuffers(screenQuadVertexBufferId);
			screenQuadVertexBufferId = 0;
		}
		screenQuadUploadBuffer = null;
	}

	private void drawUnitTexturedQuad() throws Exception {
		if (unitQuadVertexBufferId == 0 || unitQuadIndexBufferId == 0) {
			return;
		}
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, unitQuadVertexBufferId);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, unitQuadIndexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			UNIT_QUAD_POSITION_COMPONENTS,
			gl.GL_FLOAT,
			UNIT_QUAD_STRIDE_BYTES,
			0L);
		gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(
			UNIT_QUAD_TEXCOORD_COMPONENTS,
			gl.GL_FLOAT,
			UNIT_QUAD_STRIDE_BYTES,
			UNIT_QUAD_TEXCOORD_OFFSET_BYTES);
		try {
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				UNIT_QUAD_INDEX_COUNT,
				gl.GL_UNSIGNED_INT,
				0L);
		} finally {
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
		}
	}

	private void drawScreenSolidQuad(float x0, float y0, float x1, float y1) throws Exception {
		drawScreenQuad(
			x0,
			y0,
			0.0f,
			0.0f,
			x1,
			y0,
			0.0f,
			0.0f,
			x1,
			y1,
			0.0f,
			0.0f,
			x0,
			y1,
			0.0f,
			0.0f);
	}

	private void drawScreenTexturedQuad(
		float x0,
		float y0,
		float x1,
		float y1,
		float u0,
		float v0,
		float u1,
		float v1) throws Exception {
		drawScreenQuad(
			x0,
			y0,
			u0,
			v0,
			x1,
			y0,
			u1,
			v0,
			x1,
			y1,
			u1,
			v1,
			x0,
			y1,
			u0,
			v1);
	}

	private void drawScreenQuad(
		float x0,
		float y0,
		float u0,
		float v0,
		float x1,
		float y1,
		float u1,
		float v1,
		float x2,
		float y2,
		float u2,
		float v2,
		float x3,
		float y3,
		float u3,
		float v3) throws Exception {
		if (screenQuadUploadBuffer == null || screenQuadVertexBufferId == 0 || unitQuadIndexBufferId == 0) {
			return;
		}
		screenQuadUploadBuffer.clear();
		putUnitQuadVertex(screenQuadUploadBuffer, x0, y0, u0, v0);
		putUnitQuadVertex(screenQuadUploadBuffer, x1, y1, u1, v1);
		putUnitQuadVertex(screenQuadUploadBuffer, x2, y2, u2, v2);
		putUnitQuadVertex(screenQuadUploadBuffer, x3, y3, u3, v3);
		screenQuadUploadBuffer.flip();
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, screenQuadVertexBufferId);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, screenQuadUploadBuffer, gl.GL_STREAM_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, unitQuadIndexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			UNIT_QUAD_POSITION_COMPONENTS,
			gl.GL_FLOAT,
			UNIT_QUAD_STRIDE_BYTES,
			0L);
		gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(
			UNIT_QUAD_TEXCOORD_COMPONENTS,
			gl.GL_FLOAT,
			UNIT_QUAD_STRIDE_BYTES,
			UNIT_QUAD_TEXCOORD_OFFSET_BYTES);
		try {
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				UNIT_QUAD_INDEX_COUNT,
				gl.GL_UNSIGNED_INT,
				0L);
		} finally {
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
		}
	}

	private void initializeWorldSpriteQuadBuffers() throws Exception {
		worldSpriteQuadVertexBufferId = gl.glGenBuffers();
		worldSpriteQuadIndexBufferId = gl.glGenBuffers();
		worldSpriteQuadUploadBuffer = ByteBuffer
			.allocateDirect(WORLD_SPRITE_QUAD_VERTEX_COUNT * WORLD_SPRITE_QUAD_FLOATS_PER_VERTEX * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		IntBuffer indices = ByteBuffer
			.allocateDirect(WORLD_SPRITE_QUAD_INDEX_COUNT * 4)
			.order(ByteOrder.nativeOrder())
			.asIntBuffer();
		putQuadIndices(indices);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, worldSpriteQuadIndexBufferId);
		gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indices, gl.GL_STATIC_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private void deleteWorldSpriteQuadBuffers() throws Exception {
		if (worldSpriteQuadVertexBufferId != 0) {
			gl.glDeleteBuffers(worldSpriteQuadVertexBufferId);
			worldSpriteQuadVertexBufferId = 0;
		}
		if (worldSpriteQuadIndexBufferId != 0) {
			gl.glDeleteBuffers(worldSpriteQuadIndexBufferId);
			worldSpriteQuadIndexBufferId = 0;
		}
		worldSpriteQuadUploadBuffer = null;
	}

	private void drawCameraSpaceWorldSpriteScreenRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer3DFrame.SpriteAnchor anchor,
		int x,
		int y,
		int width,
		int height,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topDepth = estimateEntitySpriteDepth(anchor, y);
		float bottomDepth = estimateEntitySpriteDepth(anchor, y + height);
		float scale = (float) (1 << Math.max(0, Math.min(24, renderer3DFrame.getPerspectiveShift())));
		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawCameraSpaceWorldSpriteQuad(
			renderer3DFrame,
			x,
			x,
			y,
			y + height,
			width,
			region.getU0(),
			region.getU1(),
			region.getV0(),
			region.getV1(),
			topDepth,
			bottomDepth,
			scale);
	}

	private void drawCameraSpaceWorldSpriteFullRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topDepth = estimateEntitySpriteDepth(anchor, command.getY());
		float bottomDepth = estimateEntitySpriteDepth(anchor, command.getY() + command.getHeight());
		float scale = (float) (1 << Math.max(0, Math.min(24, renderer3DFrame.getPerspectiveShift())));
		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawCameraSpaceWorldSpriteQuad(
			renderer3DFrame,
			command.getTopX16() / 65536.0f,
			command.getBottomX16() / 65536.0f,
			command.getY(),
			command.getY() + command.getHeight(),
			command.getWidth(),
			region.getU0(),
			region.getU1(),
			region.getV0(),
			region.getV1(),
			topDepth,
			bottomDepth,
			scale);
	}

	private void drawCameraSpaceWorldSpriteRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topScreenX = command.getTopX16() / 65536.0f;
		float bottomScreenX = command.getBottomX16() / 65536.0f;
		float topScreenY = command.getY();
		float bottomScreenY = command.getY() + command.getHeight();
		float topDepth = estimateEntitySpriteDepth(anchor, command.getY());
		float bottomDepth = estimateEntitySpriteDepth(anchor, command.getY() + command.getHeight());
		float scale = (float) (1 << Math.max(0, Math.min(24, renderer3DFrame.getPerspectiveShift())));
		float uSpan = region.getU1() - region.getU0();
		float vSpan = region.getV1() - region.getV0();
		float sourceX0 = command.getSourceStartX16() / 65536.0f;
		float sourceY0 = command.getSourceStartY16() / 65536.0f;
		float sourceX1 = (command.getSourceStartX16()
			+ (long) command.getWidth() * command.getSourceScaleX16()) / 65536.0f;
		float sourceY1 = (command.getSourceStartY16()
			+ (long) command.getHeight() * command.getSourceScaleY16()) / 65536.0f;
		float u0 = region.getU0() + uSpan * sourceX0 / region.getWidth();
		float v0 = region.getV0() + vSpan * sourceY0 / region.getHeight();
		float u1 = region.getU0() + uSpan * sourceX1 / region.getWidth();
		float v1 = region.getV0() + vSpan * sourceY1 / region.getHeight();
		float leftU = command.isMirrorX() ? u1 : u0;
		float rightU = command.isMirrorX() ? u0 : u1;

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawCameraSpaceWorldSpriteQuad(
			renderer3DFrame,
			topScreenX,
			bottomScreenX,
			topScreenY,
			bottomScreenY,
			command.getWidth(),
			leftU,
			rightU,
			v0,
			v1,
			topDepth,
			bottomDepth,
			scale);
	}

	private void drawCameraSpaceWorldSpriteQuad(
		Renderer3DFrame renderer3DFrame,
		float topScreenX,
		float bottomScreenX,
		float topScreenY,
		float bottomScreenY,
		float width,
		float leftU,
		float rightU,
		float v0,
		float v1,
		float topDepth,
		float bottomDepth,
		float scale) throws Exception {
		if (worldSpriteQuadUploadBuffer == null
			|| worldSpriteQuadVertexBufferId == 0
			|| worldSpriteQuadIndexBufferId == 0) {
			return;
		}
		worldSpriteQuadUploadBuffer.clear();
		putCameraSpaceSpriteQuadVertex(
			renderer3DFrame, topScreenX, topScreenY, topDepth, scale, leftU, v0);
		putCameraSpaceSpriteQuadVertex(
			renderer3DFrame, topScreenX + width, topScreenY, topDepth, scale, rightU, v0);
		putCameraSpaceSpriteQuadVertex(
			renderer3DFrame, bottomScreenX + width, bottomScreenY, bottomDepth, scale, rightU, v1);
		putCameraSpaceSpriteQuadVertex(
			renderer3DFrame, bottomScreenX, bottomScreenY, bottomDepth, scale, leftU, v1);
		worldSpriteQuadUploadBuffer.flip();
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, worldSpriteQuadVertexBufferId);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, worldSpriteQuadUploadBuffer, gl.GL_STREAM_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, worldSpriteQuadIndexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			WORLD_SPRITE_QUAD_POSITION_COMPONENTS,
			gl.GL_FLOAT,
			WORLD_SPRITE_QUAD_STRIDE_BYTES,
			0L);
		gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(
			WORLD_SPRITE_QUAD_TEXCOORD_COMPONENTS,
			gl.GL_FLOAT,
			WORLD_SPRITE_QUAD_STRIDE_BYTES,
			WORLD_SPRITE_QUAD_TEXCOORD_OFFSET_BYTES);
		try {
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				WORLD_SPRITE_QUAD_INDEX_COUNT,
				gl.GL_UNSIGNED_INT,
				0L);
		} finally {
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
		}
	}

	private void putCameraSpaceSpriteQuadVertex(
		Renderer3DFrame frame,
		float screenX,
		float screenY,
		float cameraZ,
		float scale,
		float u,
		float v) {
		float cameraX = (screenX - frame.getCenterX()) * cameraZ / scale;
		float cameraY = (screenY - frame.getCenterY()) * cameraZ / scale;
		worldSpriteQuadUploadBuffer.put(cameraX);
		worldSpriteQuadUploadBuffer.put(cameraY);
		worldSpriteQuadUploadBuffer.put(cameraZ);
		worldSpriteQuadUploadBuffer.put(u);
		worldSpriteQuadUploadBuffer.put(v);
	}

	private List<WorldSpriteCommand> buildOpenGLCompositeWorldSpriteCommands(
		Frame frame,
		Renderer2DFrame.SpriteCommand[] commands) {
		List<WorldSpriteCommand> entityCommands = new ArrayList<WorldSpriteCommand>();
		for (Renderer2DFrame.SpriteCommand command : commands) {
			if (!isOpenGLCompositeWorldSpriteCommand(command)) {
				continue;
			}
			entityCommands.add(buildOpenGLCompositeWorldSpriteCommand(frame, command));
		}
		Collections.sort(entityCommands, new Comparator<WorldSpriteCommand>() {
			@Override
			public int compare(WorldSpriteCommand left, WorldSpriteCommand right) {
				if (left.legacyDrawOrder != right.legacyDrawOrder) {
					return left.legacyDrawOrder < right.legacyDrawOrder ? -1 : 1;
				}
				int leftSequence = left.command.getSequence();
				int rightSequence = right.command.getSequence();
				if (leftSequence != rightSequence) {
					return leftSequence < rightSequence ? -1 : 1;
				}
				return 0;
			}
		});
		return entityCommands;
	}

	private List<OpenGLCompositeSceneCommand> buildOpenGLCompositeSceneCommands(
		Frame frame,
		Renderer2DFrame.SpriteCommand[] commands) {
		List<OpenGLCompositeSceneCommand> sceneCommands = new ArrayList<OpenGLCompositeSceneCommand>();
		List<WorldSpriteCommand> worldSpriteCommands = buildOpenGLCompositeWorldSpriteCommands(frame, commands);
		for (WorldSpriteCommand worldSpriteCommand : worldSpriteCommands) {
			sceneCommands.add(OpenGLCompositeSceneCommand.worldSprite(worldSpriteCommand));
		}
		Collections.sort(sceneCommands, new Comparator<OpenGLCompositeSceneCommand>() {
			@Override
			public int compare(OpenGLCompositeSceneCommand left, OpenGLCompositeSceneCommand right) {
				if (left.legacyDrawOrder != right.legacyDrawOrder) {
					return left.legacyDrawOrder < right.legacyDrawOrder ? -1 : 1;
				}
				if (left.sequence != right.sequence) {
					return left.sequence < right.sequence ? -1 : 1;
				}
				return left.kind.ordinal() - right.kind.ordinal();
			}
		});
		return sceneCommands;
	}

	private WorldSpriteCommand buildOpenGLCompositeWorldSpriteCommand(
		Frame frame,
		Renderer2DFrame.SpriteCommand command) {
		Renderer3DFrame.SpriteAnchor anchor = findSpriteAnchor(frame, command);
		WorldSpriteAnchorMatch anchorMatch = classifyWorldSpriteAnchorMatch(frame, command, anchor);
		return new WorldSpriteCommand(command, anchor, anchorMatch);
	}

	private Set<Long> buildOpenGLCompositeFrontOccluderFaceKeys(
		Frame frame,
		List<WorldSpriteCommand> worldSpriteCommands,
		int minExclusiveOrder,
		int maxExclusiveOrder) {
		if (frame == null || frame.renderer3DFrame == null || worldSpriteCommands == null) {
			return Collections.emptySet();
		}
		List<Renderer3DFrame.FaceCommand> faces = frame.renderer3DFrame.getWorldFaces();
		if (faces == null || faces.isEmpty()) {
			return Collections.emptySet();
		}
		Set<Long> faceKeys = new HashSet<Long>();
		for (Renderer3DFrame.FaceCommand face : faces) {
			int order = face.getLegacyDrawOrder();
			if (order <= minExclusiveOrder || order < 0) {
				continue;
			}
			if (maxExclusiveOrder != Integer.MAX_VALUE && order >= maxExclusiveOrder) {
				continue;
			}
			if (!isOpenGLCompositeFrontOccluderKind(face.getModelKind())) {
				continue;
			}
			int[] faceBounds = boundsForOpenGLCompositeFace(face.getRenderScreenX(), face.getRenderScreenY());
			if (countOverlappingOpenGLCompositeWorldSpriteCommands(faceBounds, worldSpriteCommands) > 0) {
				faceKeys.add(openGLCompositeModelFaceKey(face.getModelIndex(), face.getFaceId()));
			}
		}
		return faceKeys;
	}

	private List<StaticWorldCommand> buildOpenGLCompositeStaticWorldCommands(Frame frame) {
		if (frame == null || frame.renderer3DFrame == null || frame.renderer3DFrame.getMeshFrame() == null) {
			return Collections.emptyList();
		}
		Renderer3DMeshFrame meshFrame = frame.renderer3DFrame.getMeshFrame();
		Renderer3DModelKind[] modelKinds = meshFrame.getTriangleModelKinds();
		int[] modelIndices = meshFrame.getTriangleModelIndices();
		int[] faceIds = meshFrame.getTriangleFaceIds();
		int[] legacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
		int triangleCount = Math.min(
			meshFrame.getTriangleCount(),
			Math.min(
				Math.min(modelKinds.length, modelIndices.length),
				Math.min(faceIds.length, legacyDrawOrders.length)));
		Map<Renderer3DModelKind, StaticWorldCommandBuilder> builders =
			new LinkedHashMap<Renderer3DModelKind, StaticWorldCommandBuilder>();
		for (int triangle = 0; triangle < triangleCount; triangle++) {
			Renderer3DModelKind modelKind = modelKinds[triangle] == null
				? Renderer3DModelKind.UNCLASSIFIED
				: modelKinds[triangle];
			StaticWorldCommandBuilder builder = builders.get(modelKind);
			if (builder == null) {
				builder = new StaticWorldCommandBuilder(modelKind);
				builders.put(modelKind, builder);
			}
			builder.addTriangle(
				openGLCompositeModelFaceKey(modelIndices[triangle], faceIds[triangle]),
				legacyDrawOrders[triangle]);
		}
		List<StaticWorldCommand> commands = new ArrayList<StaticWorldCommand>(builders.size());
		for (StaticWorldCommandBuilder builder : builders.values()) {
			commands.add(builder.build());
		}
		return commands;
	}

	private List<StaticWorldMaterialTriangle> buildOpenGLCompositeStaticWorldMaterialTriangles(Frame frame) {
		if (frame == null || frame.renderer3DFrame == null || frame.renderer3DFrame.getMeshFrame() == null) {
			return Collections.emptyList();
		}
		Renderer3DMeshFrame meshFrame = frame.renderer3DFrame.getMeshFrame();
		int[] textures = meshFrame.getTriangleTextures();
		Renderer3DModelKind[] modelKinds = meshFrame.getTriangleModelKinds();
		int[] modelIndices = meshFrame.getTriangleModelIndices();
		int[] faceIds = meshFrame.getTriangleFaceIds();
		int triangleCount = Math.min(
			meshFrame.getTriangleCount(),
			Math.min(Math.min(textures.length, modelKinds.length), Math.min(modelIndices.length, faceIds.length)));
		List<StaticWorldMaterialTriangle> triangles =
			new ArrayList<StaticWorldMaterialTriangle>(triangleCount);
		for (int triangle = 0; triangle < triangleCount; triangle++) {
			int textureId = textures[triangle];
			Renderer3DTextureData textureData = textureId >= 0 ? meshFrame.getTexture(textureId) : null;
			triangles.add(new StaticWorldMaterialTriangle(
				triangle,
				classifyStaticWorldMaterial(textureId, textureData),
				modelKinds[triangle] == null ? Renderer3DModelKind.UNCLASSIFIED : modelKinds[triangle],
				modelIndices[triangle],
				faceIds[triangle],
				textureId,
				textureData != null && textureData.hasTransparency()));
		}
		return triangles;
	}

	private static StaticWorldMaterialPass classifyStaticWorldMaterial(
		int textureId,
		Renderer3DTextureData textureData) {
		if (textureId == OpenGLWorldMeshRenderer.LEGACY_TRANSPARENT_TEXTURE) {
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

	private static boolean isOpenGLCompositeFrontOccluderKind(Renderer3DModelKind kind) {
		return kind == Renderer3DModelKind.WALL
			|| kind == Renderer3DModelKind.GAME_OBJECT
			|| kind == Renderer3DModelKind.WALL_OBJECT;
	}

	private static int countOverlappingOpenGLCompositeWorldSpriteCommands(
		int[] faceBounds,
		List<WorldSpriteCommand> worldSpriteCommands) {
		if (faceBounds == null) {
			return 0;
		}
		int overlappingSpriteCommands = 0;
		for (WorldSpriteCommand worldSpriteCommand : worldSpriteCommands) {
			if (worldSpriteCommand == null || worldSpriteCommand.command == null) {
				continue;
			}
			Renderer2DFrame.SpriteCommand command = worldSpriteCommand.command;
			int[] spriteBounds = new int[] {
				command.getX(),
				command.getY(),
				command.getX() + command.getWidth(),
				command.getY() + command.getHeight()
			};
			if (overlapsOpenGLCompositeBounds(faceBounds, spriteBounds)) {
				overlappingSpriteCommands++;
			}
		}
		return overlappingSpriteCommands;
	}

	private static boolean overlapsOpenGLCompositeBounds(int[] first, int[] second) {
		return first != null
			&& second != null
			&& first.length >= 4
			&& second.length >= 4
			&& first[0] < second[2]
			&& first[2] > second[0]
			&& first[1] < second[3]
			&& first[3] > second[1];
	}

	private static int[] boundsForOpenGLCompositeFace(int[] xs, int[] ys) {
		if (xs == null || ys == null || xs.length == 0 || xs.length != ys.length) {
			return null;
		}
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int i = 0; i < xs.length; i++) {
			minX = Math.min(minX, xs[i]);
			minY = Math.min(minY, ys[i]);
			maxX = Math.max(maxX, xs[i]);
			maxY = Math.max(maxY, ys[i]);
		}
		return new int[] { minX, minY, maxX, maxY };
	}

	private static long openGLCompositeModelFaceKey(int modelIndex, int faceId) {
		return ((long) modelIndex << 32) ^ (faceId & 0xffffffffL);
	}

	private static int openGLCompositeModelIndex(long modelFaceKey) {
		return (int) (modelFaceKey >> 32);
	}

	private static int openGLCompositeFaceId(long modelFaceKey) {
		return (int) modelFaceKey;
	}

	private void drawOpenGLCompositeDirectSpriteCommand(Renderer2DFrame.SpriteCommand command) throws Exception {
		if (command.getPhase() == Renderer2DFrame.Phase.UI_OVERLAY || visibleSpriteTextureAtlas == null) {
			drawSpriteCommand(command);
			return;
		}

		DynamicTextureData textureData = buildDirectSpriteTexture(command);
		if (textureData == null) {
			drawSpriteCommand(command);
			return;
		}
		if (!drawOpenGLCompositeDynamicSpriteTexture(command, textureData, 1.0f)) {
			drawSpriteCommand(command);
		}
	}

	private boolean drawOpenGLCompositeDynamicSpriteTexture(
		Renderer2DFrame.SpriteCommand command,
		DynamicTextureData textureData,
		float alpha) throws Exception {
		if (visibleSpriteTextureAtlas == null || textureData == null) {
			return false;
		}
		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);
		if (region == null) {
			visibleSpriteTextureAtlas.beginFrame();
			region = visibleSpriteTextureAtlas.upload(textureData);
			if (region == null) {
				return false;
			}
		}
		drawSpriteRegion(command, region, region.getU0(), region.getU1(), region.getV0(), region.getV1(), alpha);
		return true;
	}

	private Renderer3DFrame.SpriteAnchor findSpriteAnchor(Frame frame, Renderer2DFrame.SpriteCommand command) {
		if (frame == null || frame.renderer3DFrame == null || command == null) {
			return null;
		}
		int legacySpriteId = command.getLegacySpriteId();
		List<Renderer3DFrame.SpriteAnchor> anchors = frame.renderer3DFrame.getSpriteAnchors();
		Renderer3DFrame.SpriteAnchor bestAnchor = null;
		int bestScore = Integer.MAX_VALUE;
		Renderer3DFrame.SpriteAnchor exactIdAnchor = null;
		for (Renderer3DFrame.SpriteAnchor anchor : anchors) {
			if (anchor.getSpriteId() == legacySpriteId && exactIdAnchor == null) {
				exactIdAnchor = anchor;
			}
			int score = worldSpriteMatchScore(anchor, command);
			if (score < bestScore) {
				bestScore = score;
				bestAnchor = anchor;
			}
		}
		if (bestAnchor != null) {
			return bestAnchor;
		}
		if (exactIdAnchor != null) {
			return exactIdAnchor;
		}
		for (Renderer3DFrame.SpriteAnchor anchor : anchors) {
			int score = worldSpriteMatchScore(anchor, command, false);
			if (score < bestScore) {
				bestScore = score;
				bestAnchor = anchor;
			}
		}
		if (bestAnchor != null) {
			return bestAnchor;
		}
		for (Renderer3DFrame.SpriteAnchor anchor : anchors) {
			if (anchor.getSpriteId() == legacySpriteId) {
				return anchor;
			}
		}
		return null;
	}

	private DynamicTextureData buildDepthVisibleEntitySpriteTexture(
		Frame frame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		boolean[] clippedSceneRestoreMask,
		WorldSpriteAnchorMatch providedAnchorMatch) {
		if (frame == null || command == null || anchor == null || frame.renderer3DFrame == null) {
			return null;
		}
		Renderer3DDepthFrame depthFrame = frame.renderer3DFrame.getDepthFrame();
		if (depthFrame == null) {
			return null;
		}
		Sprite sprite = command.getSprite();
		int[] sourcePixels = sprite.getPixels();
		int spriteWidth = sprite.getWidth();
		int spriteHeight = sprite.getHeight();
		int width = command.getWidth();
		int height = command.getHeight();
		int alpha = command.getAlpha();
		if (sourcePixels == null
			|| sourcePixels.length < spriteWidth * spriteHeight
			|| width <= 0
			|| height <= 0
			|| alpha <= 0) {
			return null;
		}

		ByteBuffer overlayPixels = ByteBuffer.allocateDirect(width * height * 4);
		long xDelta16 = (long) command.getBottomX16() - command.getTopX16();
		int visiblePixelCount = 0;
		int depthFallbackSourcePixels = 0;
		int depthFallbackOccludedPixels = 0;
		int depthFallbackClippedPixels = 0;
		int depthFallbackOutOfBoundsPixels = 0;
		int terrainOccludedPixels = 0;
		int wallOccludedPixels = 0;
		int roofOccludedPixels = 0;
		int gameObjectOccludedPixels = 0;
		int wallObjectOccludedPixels = 0;
		int minOccluderLegacyDrawOrder = Integer.MAX_VALUE;
		int maxOccluderLegacyDrawOrder = Integer.MIN_VALUE;
		int minOccluderDepth = Integer.MAX_VALUE;
		int maxOccluderDepth = Integer.MIN_VALUE;
		Map<Integer, EntitySpriteOccluderFaceStats> occluderFaceStatsById =
			new HashMap<Integer, EntitySpriteOccluderFaceStats>();
		WorldSpriteAnchorMatch anchorMatch = providedAnchorMatch == null
			? classifyWorldSpriteAnchorMatch(frame, command, anchor)
			: providedAnchorMatch;

		for (int row = 0; row < height; row++) {
			int screenY = command.getY() + row;
			long rowLeft16 = command.getTopX16() + xDelta16 * row / height;
			int screenX0 = (int) (rowLeft16 >> 16);
			int sourceY = (int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);
			for (int column = 0; column < width; column++) {
				int rgba = 0;
				int screenX = screenX0 + column;
				int sourceX = (int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
				if (sourceX < 0
					|| sourceX >= spriteWidth
					|| sourceY < 0
					|| sourceY >= spriteHeight) {
					depthFallbackOutOfBoundsPixels++;
					overlayPixels.putInt(rgba);
					continue;
				}
				int sourcePixel = sourcePixels[sourceY * spriteWidth + sourceX];
				if (!orsc.graphics.RendererTransparency.isVisibleSpritePixel(sourcePixel)) {
					overlayPixels.putInt(rgba);
					continue;
				}
				depthFallbackSourcePixels++;
				if (screenX < 0
					|| screenX >= frame.sourceWidth
					|| screenY < 0
					|| screenY >= frame.sourceHeight) {
					depthFallbackOutOfBoundsPixels++;
					overlayPixels.putInt(rgba);
					continue;
				}
				if (isSceneRestoreClipped(clippedSceneRestoreMask, frame.sourceWidth, screenX, screenY)) {
					depthFallbackClippedPixels++;
					overlayPixels.putInt(rgba);
					continue;
				}
				int spriteDepth = estimateEntitySpriteDepth(anchor, screenY);
				int depthTolerance = Math.max(32, spriteDepth / 96);
				Renderer3DModelKind occluderKind = depthFrame.getEntityOccluderKindAfterSprite(
					screenX,
					screenY,
					anchor.getLegacyDrawOrder(),
					spriteDepth,
					depthTolerance);
				if (occluderKind != null) {
					depthFallbackOccludedPixels++;
					if (occluderKind == Renderer3DModelKind.TERRAIN) {
						terrainOccludedPixels++;
					} else if (occluderKind == Renderer3DModelKind.WALL) {
						wallOccludedPixels++;
					} else if (occluderKind == Renderer3DModelKind.ROOF) {
						roofOccludedPixels++;
					} else if (occluderKind == Renderer3DModelKind.GAME_OBJECT) {
						gameObjectOccludedPixels++;
					} else if (occluderKind == Renderer3DModelKind.WALL_OBJECT) {
						wallObjectOccludedPixels++;
					}
					int occluderLegacyDrawOrder = depthFrame.getLegacyDrawOrderAt(screenX, screenY);
					if (occluderLegacyDrawOrder >= 0) {
						minOccluderLegacyDrawOrder =
							Math.min(minOccluderLegacyDrawOrder, occluderLegacyDrawOrder);
						maxOccluderLegacyDrawOrder =
							Math.max(maxOccluderLegacyDrawOrder, occluderLegacyDrawOrder);
					}
					int occluderDepth = depthFrame.getDepthAt(screenX, screenY);
					if (occluderDepth != Integer.MAX_VALUE) {
						minOccluderDepth = Math.min(minOccluderDepth, occluderDepth);
						maxOccluderDepth = Math.max(maxOccluderDepth, occluderDepth);
					}
					int occluderFaceId = depthFrame.getFaceIdAt(screenX, screenY);
					if (occluderFaceId >= 0) {
						EntitySpriteOccluderFaceStats occluderStats =
							occluderFaceStatsById.get(occluderFaceId);
						if (occluderStats == null) {
							occluderStats = new EntitySpriteOccluderFaceStats(
								occluderKind,
								occluderFaceId,
								depthFrame.getModelIndexAt(screenX, screenY));
							occluderFaceStatsById.put(occluderFaceId, occluderStats);
						}
						occluderStats.record(occluderLegacyDrawOrder, occluderDepth);
					}
					overlayPixels.putInt(rgba);
					continue;
				}
				int replayRgb = command.getTransform().apply(sourcePixel)
					& orsc.graphics.RendererTransparency.RGB_MASK;
				rgba = (replayRgb << 8) | alpha;
				visiblePixelCount++;
				overlayPixels.putInt(rgba);
			}
		}

		recordLegacyEntitySpriteDepthEvaluation(
			command,
			depthFallbackSourcePixels,
			visiblePixelCount,
			depthFallbackOccludedPixels,
			depthFallbackClippedPixels,
			depthFallbackOutOfBoundsPixels,
			terrainOccludedPixels,
			wallOccludedPixels,
			roofOccludedPixels,
			gameObjectOccludedPixels,
			wallObjectOccludedPixels,
			minOccluderLegacyDrawOrder,
			maxOccluderLegacyDrawOrder,
			minOccluderDepth,
			maxOccluderDepth,
			dominantOccluderFace(occluderFaceStatsById),
			anchorMatch,
			anchor);
		if (visiblePixelCount == 0) {
			recordLegacyEntitySpriteDepthFallbackMiss(
				command,
				depthFallbackSourcePixels,
				depthFallbackOccludedPixels,
				depthFallbackClippedPixels,
				depthFallbackOutOfBoundsPixels,
				anchor);
		}
		overlayPixels.flip();
		return new DynamicTextureData(width, height, overlayPixels, visiblePixelCount);
	}

	private int estimateEntitySpriteDepth(Renderer3DFrame.SpriteAnchor anchor, int screenY) {
		int bottomDepth = anchor.getCameraZ();
		int topDepth = anchor.getAverageDepth() * 2 - bottomDepth;
		if (topDepth <= 0) {
			topDepth = anchor.getAverageDepth();
		}
		if (bottomDepth <= 0) {
			bottomDepth = anchor.getAverageDepth();
		}
		int drawHeight = Math.max(1, anchor.getDrawHeight());
		int relativeY = clamp(screenY - anchor.getDrawY(), 0, drawHeight);
		int depth = topDepth + (bottomDepth - topDepth) * relativeY / drawHeight;
		return Math.max(1, depth);
	}

	private DynamicTextureData buildDirectSpriteTexture(Renderer2DFrame.SpriteCommand command) {
		Sprite sprite = command.getSprite();
		int[] sourcePixels = sprite.getPixels();
		int spriteWidth = sprite.getWidth();
		int spriteHeight = sprite.getHeight();
		int width = command.getWidth();
		int height = command.getHeight();
		int alpha = command.getAlpha();
		if (sourcePixels == null
			|| sourcePixels.length < spriteWidth * spriteHeight
			|| width <= 0
			|| height <= 0
			|| alpha <= 0) {
			return null;
		}

		ByteBuffer overlayPixels = ByteBuffer.allocateDirect(width * height * 4);
		int visiblePixelCount = 0;

		for (int row = 0; row < height; row++) {
			int sourceY = (int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);
			for (int column = 0; column < width; column++) {
				int rgba = 0;
				int sourceX = (int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
				if (sourceX >= 0
					&& sourceX < spriteWidth
					&& sourceY >= 0
					&& sourceY < spriteHeight) {
					int sourcePixel = sourcePixels[sourceY * spriteWidth + sourceX];
					if (orsc.graphics.RendererTransparency.isVisibleSpritePixel(sourcePixel)) {
						int replayRgb = command.getTransform().apply(sourcePixel)
							& orsc.graphics.RendererTransparency.RGB_MASK;
						rgba = (replayRgb << 8) | alpha;
						visiblePixelCount++;
					}
				}
				overlayPixels.putInt(rgba);
			}
		}

		if (visiblePixelCount == 0) {
			return null;
		}
		overlayPixels.flip();
		return new DynamicTextureData(width, height, overlayPixels, visiblePixelCount);
	}

	private CompositeWorldSpriteTexture buildCompositeCharacterSpriteTexture(
		List<WorldSpriteCommand> layerCommands) {
		if (layerCommands == null || layerCommands.isEmpty()) {
			return null;
		}
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (WorldSpriteCommand layerCommand : layerCommands) {
			Renderer2DFrame.SpriteCommand command =
				layerCommand == null ? null : layerCommand.command;
			if (command == null) {
				continue;
			}
			minY = Math.min(minY, command.getY());
			maxY = Math.max(maxY, command.getY() + command.getHeight());
			long xDelta16 = (long) command.getBottomX16() - command.getTopX16();
			for (int row = 0; row < command.getHeight(); row++) {
				int rowLeft = (int) ((command.getTopX16() + xDelta16 * row / command.getHeight()) >> 16);
				minX = Math.min(minX, rowLeft);
				maxX = Math.max(maxX, rowLeft + command.getWidth());
			}
		}
		if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE || maxX <= minX || maxY <= minY) {
			return null;
		}
		int width = maxX - minX;
		int height = maxY - minY;
		int[] compositePixels = new int[width * height];
		int visiblePixelCount = 0;
		for (WorldSpriteCommand layerCommand : layerCommands) {
			Renderer2DFrame.SpriteCommand command =
				layerCommand == null ? null : layerCommand.command;
			if (command == null) {
				continue;
			}
			visiblePixelCount += compositeSpriteCommandInto(command, minX, minY, width, compositePixels);
		}
		if (visiblePixelCount <= 0) {
			return null;
		}
		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
		int finalVisiblePixelCount = 0;
		for (int i = 0; i < compositePixels.length; i++) {
			int rgba = compositePixels[i];
			if ((rgba & 0xFF) != 0) {
				finalVisiblePixelCount++;
			}
			pixels.putInt(rgba);
		}
		pixels.flip();
		return new CompositeWorldSpriteTexture(
			minX,
			minY,
			width,
			height,
			new DynamicTextureData(width, height, pixels, finalVisiblePixelCount));
	}

	private int compositeSpriteCommandInto(
		Renderer2DFrame.SpriteCommand command,
		int originX,
		int originY,
		int compositeWidth,
		int[] compositePixels) {
		Sprite sprite = command.getSprite();
		int[] sourcePixels = sprite.getPixels();
		int spriteWidth = sprite.getWidth();
		int spriteHeight = sprite.getHeight();
		int width = command.getWidth();
		int height = command.getHeight();
		int alpha = command.getAlpha();
		if (sourcePixels == null
			|| sourcePixels.length < spriteWidth * spriteHeight
			|| width <= 0
			|| height <= 0
			|| alpha <= 0) {
			return 0;
		}
		long xDelta16 = (long) command.getBottomX16() - command.getTopX16();
		int visiblePixelCount = 0;
		for (int row = 0; row < height; row++) {
			int targetY = command.getY() + row - originY;
			int screenX0 = (int) ((command.getTopX16() + xDelta16 * row / height) >> 16);
			int sourceY = (int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);
			if (targetY < 0 || sourceY < 0 || sourceY >= spriteHeight) {
				continue;
			}
			for (int column = 0; column < width; column++) {
				int targetX = screenX0 + column - originX;
				int sourceX =
					(int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
				if (targetX < 0
					|| targetX >= compositeWidth
					|| sourceX < 0
					|| sourceX >= spriteWidth) {
					continue;
				}
				int sourcePixel = sourcePixels[sourceY * spriteWidth + sourceX];
				if (!orsc.graphics.RendererTransparency.isVisibleSpritePixel(sourcePixel)) {
					continue;
				}
				int rgb = command.getTransform().apply(sourcePixel) & orsc.graphics.RendererTransparency.RGB_MASK;
				int targetIndex = targetY * compositeWidth + targetX;
				if (alpha >= Renderer2DFrame.SpriteCommand.FULL_ALPHA) {
					compositePixels[targetIndex] = (rgb << 8) | 0xFF;
				} else {
					compositePixels[targetIndex] = blendRgbaOver(compositePixels[targetIndex], rgb, alpha);
				}
				visiblePixelCount++;
			}
		}
		return visiblePixelCount;
	}

	private static int blendRgbaOver(int destinationRgba, int sourceRgb, int sourceAlpha) {
		int inverseAlpha = 256 - sourceAlpha;
		int destinationAlpha = destinationRgba & 0xFF;
		int destinationRgb = destinationRgba >> 8 & orsc.graphics.RendererTransparency.RGB_MASK;
		int red = (((sourceRgb >> 16 & 0xFF) * sourceAlpha)
			+ ((destinationRgb >> 16 & 0xFF) * inverseAlpha)) >> 8;
		int green = (((sourceRgb >> 8 & 0xFF) * sourceAlpha)
			+ ((destinationRgb >> 8 & 0xFF) * inverseAlpha)) >> 8;
		int blue = (((sourceRgb & 0xFF) * sourceAlpha)
			+ ((destinationRgb & 0xFF) * inverseAlpha)) >> 8;
		int alpha = Math.min(255, sourceAlpha + (destinationAlpha * inverseAlpha >> 8));
		return (red << 24) | (green << 16) | (blue << 8) | alpha;
	}

	private DynamicTextureData buildVisibleSpriteTexture(
		Frame frame,
		Renderer2DFrame.SpriteCommand command,
		boolean[] clippedSceneRestoreMask) {
		Sprite sprite = command.getSprite();
		int[] sourcePixels = sprite.getPixels();
		int spriteWidth = sprite.getWidth();
		int spriteHeight = sprite.getHeight();
		int width = command.getWidth();
		int height = command.getHeight();
		if (sourcePixels == null
			|| sourcePixels.length < spriteWidth * spriteHeight
			|| width <= 0
			|| height <= 0) {
			return null;
		}

		ByteBuffer finalPixels = frame.pixels();
		ByteBuffer visiblePixels = ByteBuffer.allocateDirect(width * height * 4);
		long xDelta16 = (long) command.getBottomX16() - command.getTopX16();
		int visiblePixelCount = 0;

		for (int row = 0; row < height; row++) {
			int screenY = command.getY() + row;
			long rowLeft16 = command.getTopX16() + xDelta16 * row / height;
			int screenX0 = (int) (rowLeft16 >> 16);
			int sourceY = (int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);

			for (int column = 0; column < width; column++) {
				int rgba = 0;
				int screenX = screenX0 + column;
				int sourceX = (int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
				if (screenX >= 0
					&& screenX < frame.sourceWidth
					&& screenY >= 0
					&& screenY < frame.sourceHeight
					&& sourceX >= 0
					&& sourceX < spriteWidth
					&& sourceY >= 0
					&& sourceY < spriteHeight) {
					int sourcePixel = sourcePixels[sourceY * spriteWidth + sourceX];
					if (orsc.graphics.RendererTransparency.isVisibleSpritePixel(sourcePixel)
						&& !isSceneRestoreClipped(clippedSceneRestoreMask, frame.sourceWidth, screenX, screenY)) {
						int replayRgb = command.getTransform().apply(sourcePixel) & orsc.graphics.RendererTransparency.RGB_MASK;
						int finalRgb = (finalPixels.getInt((screenY * frame.sourceWidth + screenX) * 4) >>> 8)
							& orsc.graphics.RendererTransparency.RGB_MASK;
						if (matchesRestoredSpriteColor(finalRgb, replayRgb)) {
							rgba = (replayRgb << 8) | 0xFF;
							visiblePixelCount++;
						}
					}
				}
				visiblePixels.putInt(rgba);
			}
		}

		if (visiblePixelCount == 0) {
			return null;
		}
		visiblePixels.flip();
		return new DynamicTextureData(width, height, visiblePixels, visiblePixelCount);
	}

	private boolean isSceneRestoreClipped(boolean[] mask, int sourceWidth, int x, int y) {
		if (mask == null || sourceWidth <= 0 || x < 0 || y < 0) {
			return false;
		}
		int index = y * sourceWidth + x;
		return index >= 0 && index < mask.length && mask[index];
	}

	private boolean[] buildOpenGLCompositeDirectOverlayCoverageMask(
		Renderer2DFrame.SpriteCommand[] commands,
		Renderer2DFrame.TextCommand[] textCommands,
		Renderer2DFrame.PrimitiveCommand[] primitiveCommands,
		int sourceWidth,
		int sourceHeight) {
		if (sourceWidth <= 0 || sourceHeight <= 0) {
			return null;
		}

		boolean[] mask = null;
		if (commands != null) {
			for (Renderer2DFrame.SpriteCommand command : commands) {
				if (!isOpenGLCompositeDirectSpriteCommand(command)) {
					continue;
				}
				Sprite sprite = command.getSprite();
				int[] sourcePixels = sprite.getPixels();
				int spriteWidth = sprite.getWidth();
				int spriteHeight = sprite.getHeight();
				int width = command.getWidth();
				int height = command.getHeight();
				if (sourcePixels == null
					|| sourcePixels.length < spriteWidth * spriteHeight
					|| width <= 0
					|| height <= 0) {
					continue;
				}

				if (mask == null) {
					mask = new boolean[sourceWidth * sourceHeight];
				}
				long xDelta16 = (long) command.getBottomX16() - command.getTopX16();
				for (int row = 0; row < height; row++) {
					int screenY = command.getY() + row;
					if (screenY < 0 || screenY >= sourceHeight) {
						continue;
					}
					long rowLeft16 = command.getTopX16() + xDelta16 * row / height;
					int screenX0 = (int) (rowLeft16 >> 16);
					int sourceY = (int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);
					if (sourceY < 0 || sourceY >= spriteHeight) {
						continue;
					}
					for (int column = 0; column < width; column++) {
						int screenX = screenX0 + column;
						if (screenX < 0 || screenX >= sourceWidth) {
							continue;
						}
						int sourceX = (int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
						if (sourceX < 0 || sourceX >= spriteWidth) {
							continue;
						}
						int sourcePixel = sourcePixels[sourceY * spriteWidth + sourceX];
						if (orsc.graphics.RendererTransparency.isVisibleSpritePixel(sourcePixel)) {
							mask[screenY * sourceWidth + screenX] = true;
						}
					}
				}
			}
		}
		if (primitiveCommands != null) {
			for (Renderer2DFrame.PrimitiveCommand command : primitiveCommands) {
				if (!isOpenGLWorldOverlayPhase(command.getPhase())) {
					continue;
				}
				mask = markOverlayRectangle(mask, sourceWidth, sourceHeight, command.getX(), command.getY(),
					command.getWidth(), command.getHeight());
			}
		}
		if (textCommands != null) {
			for (Renderer2DFrame.TextCommand command : textCommands) {
				if (!isOpenGLWorldOverlayPhase(command.getPhase())) {
					continue;
				}
				for (Renderer2DFrame.TextCommand.GlyphCommand glyph : command.getGlyphs()) {
					mask = markOverlayRectangle(mask, sourceWidth, sourceHeight, glyph.getX(), glyph.getY(),
						glyph.getWidth(), glyph.getHeight());
				}
			}
		}
		return mask;
	}

	private boolean[] markOverlayRectangle(
		boolean[] mask,
		int sourceWidth,
		int sourceHeight,
		int x,
		int y,
		int width,
		int height) {
		if (width <= 0 || height <= 0) {
			return mask;
		}
		int left = clamp(x, 0, sourceWidth);
		int top = clamp(y, 0, sourceHeight);
		int right = clamp(x + width, left, sourceWidth);
		int bottom = clamp(y + height, top, sourceHeight);
		if (left >= right || top >= bottom) {
			return mask;
		}
		if (mask == null) {
			mask = new boolean[sourceWidth * sourceHeight];
		}
		for (int row = top; row < bottom; row++) {
			int offset = row * sourceWidth;
			for (int column = left; column < right; column++) {
				mask[offset + column] = true;
			}
		}
		return mask;
	}

	private boolean matchesRestoredSpriteColor(int finalRgb, int replayRgb) {
		if (finalRgb == replayRgb) {
			return true;
		}

		int redDelta = Math.abs(((finalRgb >> 16) & 0xFF) - ((replayRgb >> 16) & 0xFF));
		int greenDelta = Math.abs(((finalRgb >> 8) & 0xFF) - ((replayRgb >> 8) & 0xFF));
		int blueDelta = Math.abs((finalRgb & 0xFF) - (replayRgb & 0xFF));
		return redDelta <= VISIBLE_SPRITE_RESTORE_COLOR_TOLERANCE
			&& greenDelta <= VISIBLE_SPRITE_RESTORE_COLOR_TOLERANCE
			&& blueDelta <= VISIBLE_SPRITE_RESTORE_COLOR_TOLERANCE;
	}

	private void drawSpriteRegion(
		Renderer2DFrame.SpriteCommand command,
		OpenGLTextureRegion region,
		float leftU,
		float rightU,
		float v0,
		float v1) throws Exception {
		drawSpriteRegion(
			command,
			region,
			leftU,
			rightU,
			v0,
			v1,
			command.getAlpha() / (float) Renderer2DFrame.SpriteCommand.FULL_ALPHA);
	}

	private void drawSpriteRegion(
		Renderer2DFrame.SpriteCommand command,
		OpenGLTextureRegion region,
		float leftU,
		float rightU,
		float v0,
		float v1,
		float alpha) throws Exception {
		float topX0 = command.getTopX16() / 65536.0f;
		float bottomX0 = command.getBottomX16() / 65536.0f;
		float y0 = command.getY();
		float y1 = command.getY() + command.getHeight();
		float topX1 = topX0 + command.getWidth();
		float bottomX1 = bottomX0 + command.getWidth();

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		drawScreenQuad(
			topX0,
			y0,
			leftU,
			v0,
			topX1,
			y0,
			rightU,
			v0,
			bottomX1,
			y1,
			rightU,
			v1,
			bottomX0,
			y1,
			leftU,
			v1);
	}

	private void useUnitProjection() throws Exception {
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0);
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	private void useSourceProjection(int sourceWidth, int sourceHeight) throws Exception {
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0, sourceWidth, sourceHeight, 0.0, -1.0, 1.0);
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	private void useWorldMeshProjection(Frame frame) throws Exception {
		if (frame == null || frame.renderer3DFrame == null) {
			throw new IllegalArgumentException("camera-space world projection requires a renderer3D frame");
		}
		if (cameraToClipMatrixBuffer == null) {
			cameraToClipMatrixBuffer = ByteBuffer
				.allocateDirect(16 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		}
		float[] matrix = cameraToClipProjectionMatrix(frame.renderer3DFrame);
		cameraToClipMatrixBuffer.clear();
		for (int column = 0; column < 4; column++) {
			for (int row = 0; row < 4; row++) {
				cameraToClipMatrixBuffer.put(matrix[row * 4 + column]);
			}
		}
		cameraToClipMatrixBuffer.flip();
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadMatrixf(cameraToClipMatrixBuffer);
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	private static float[] cameraToClipProjectionMatrix(Renderer3DFrame frame) {
		float viewportWidth = Math.max(1.0f, frame.getViewportWidth());
		float viewportHeight = Math.max(1.0f, frame.getViewportHeight());
		float scale = (float) (1 << Math.max(0, Math.min(24, frame.getPerspectiveShift())));
		float near = Math.max(1.0f, frame.getNearPlane());
		float far = Math.max(near + 1.0f, frame.getFogDistance());
		float depthA = (far + near) / (far - near);
		float depthB = -(2.0f * far * near) / (far - near);
		return new float[] {
			2.0f * scale / viewportWidth, 0.0f, 2.0f * frame.getCenterX() / viewportWidth - 1.0f, 0.0f,
			0.0f, -2.0f * scale / viewportHeight, 1.0f - 2.0f * frame.getCenterY() / viewportHeight, 0.0f,
			0.0f, 0.0f, depthA, depthB,
			0.0f, 0.0f, 1.0f, 0.0f
		};
	}

	private void initializeInputBindings() throws Exception {
		if (!inputEnabled) {
			return;
		}

		keyBindings = new KeyBinding[] {
			key("GLFW_KEY_LEFT_SHIFT", KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_RIGHT_SHIFT", KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_LEFT_CONTROL", KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_RIGHT_CONTROL", KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_LEFT_ALT", KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_RIGHT_ALT", KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_LEFT", KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_RIGHT", KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_UP", KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_DOWN", KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_HOME", KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_PAGE_UP", KeyEvent.VK_PAGE_UP, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_PAGE_DOWN", KeyEvent.VK_PAGE_DOWN, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_ENTER", KeyEvent.VK_ENTER, '\n', '\n'),
			key("GLFW_KEY_BACKSPACE", KeyEvent.VK_BACK_SPACE, '\b', '\b'),
			key("GLFW_KEY_ESCAPE", KeyEvent.VK_ESCAPE, (char) 27, (char) 27),
			key("GLFW_KEY_TAB", KeyEvent.VK_TAB, '\t', '\t'),
			key("GLFW_KEY_SPACE", KeyEvent.VK_SPACE, ' ', ' '),
			key("GLFW_KEY_F1", KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F2", KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F3", KeyEvent.VK_F3, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F4", KeyEvent.VK_F4, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F5", KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F6", KeyEvent.VK_F6, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F7", KeyEvent.VK_F7, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F8", KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F9", KeyEvent.VK_F9, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F10", KeyEvent.VK_F10, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F11", KeyEvent.VK_F11, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_F12", KeyEvent.VK_F12, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key("GLFW_KEY_0", KeyEvent.VK_0, '0', ')'),
			key("GLFW_KEY_1", KeyEvent.VK_1, '1', '!'),
			key("GLFW_KEY_2", KeyEvent.VK_2, '2', '@'),
			key("GLFW_KEY_3", KeyEvent.VK_3, '3', '#'),
			key("GLFW_KEY_4", KeyEvent.VK_4, '4', '$'),
			key("GLFW_KEY_5", KeyEvent.VK_5, '5', '%'),
			key("GLFW_KEY_6", KeyEvent.VK_6, '6', '^'),
			key("GLFW_KEY_7", KeyEvent.VK_7, '7', '&'),
			key("GLFW_KEY_8", KeyEvent.VK_8, '8', '*'),
			key("GLFW_KEY_9", KeyEvent.VK_9, '9', '('),
			key("GLFW_KEY_A", KeyEvent.VK_A, 'a', 'A'),
			key("GLFW_KEY_B", KeyEvent.VK_B, 'b', 'B'),
			key("GLFW_KEY_C", KeyEvent.VK_C, 'c', 'C'),
			key("GLFW_KEY_D", KeyEvent.VK_D, 'd', 'D'),
			key("GLFW_KEY_E", KeyEvent.VK_E, 'e', 'E'),
			key("GLFW_KEY_F", KeyEvent.VK_F, 'f', 'F'),
			key("GLFW_KEY_G", KeyEvent.VK_G, 'g', 'G'),
			key("GLFW_KEY_H", KeyEvent.VK_H, 'h', 'H'),
			key("GLFW_KEY_I", KeyEvent.VK_I, 'i', 'I'),
			key("GLFW_KEY_J", KeyEvent.VK_J, 'j', 'J'),
			key("GLFW_KEY_K", KeyEvent.VK_K, 'k', 'K'),
			key("GLFW_KEY_L", KeyEvent.VK_L, 'l', 'L'),
			key("GLFW_KEY_M", KeyEvent.VK_M, 'm', 'M'),
			key("GLFW_KEY_N", KeyEvent.VK_N, 'n', 'N'),
			key("GLFW_KEY_O", KeyEvent.VK_O, 'o', 'O'),
			key("GLFW_KEY_P", KeyEvent.VK_P, 'p', 'P'),
			key("GLFW_KEY_Q", KeyEvent.VK_Q, 'q', 'Q'),
			key("GLFW_KEY_R", KeyEvent.VK_R, 'r', 'R'),
			key("GLFW_KEY_S", KeyEvent.VK_S, 's', 'S'),
			key("GLFW_KEY_T", KeyEvent.VK_T, 't', 'T'),
			key("GLFW_KEY_U", KeyEvent.VK_U, 'u', 'U'),
			key("GLFW_KEY_V", KeyEvent.VK_V, 'v', 'V'),
			key("GLFW_KEY_W", KeyEvent.VK_W, 'w', 'W'),
			key("GLFW_KEY_X", KeyEvent.VK_X, 'x', 'X'),
			key("GLFW_KEY_Y", KeyEvent.VK_Y, 'y', 'Y'),
			key("GLFW_KEY_Z", KeyEvent.VK_Z, 'z', 'Z'),
			key("GLFW_KEY_MINUS", KeyEvent.VK_MINUS, '-', '_'),
			key("GLFW_KEY_EQUAL", KeyEvent.VK_EQUALS, '=', '+'),
			key("GLFW_KEY_LEFT_BRACKET", KeyEvent.VK_OPEN_BRACKET, '[', '{'),
			key("GLFW_KEY_RIGHT_BRACKET", KeyEvent.VK_CLOSE_BRACKET, ']', '}'),
			key("GLFW_KEY_BACKSLASH", KeyEvent.VK_BACK_SLASH, '\\', '|'),
			key("GLFW_KEY_SEMICOLON", KeyEvent.VK_SEMICOLON, ';', ':'),
			key("GLFW_KEY_APOSTROPHE", KeyEvent.VK_QUOTE, '\'', '"'),
			key("GLFW_KEY_COMMA", KeyEvent.VK_COMMA, ',', '<'),
			key("GLFW_KEY_PERIOD", KeyEvent.VK_PERIOD, '.', '>'),
			key("GLFW_KEY_SLASH", KeyEvent.VK_SLASH, '/', '?'),
			key("GLFW_KEY_GRAVE_ACCENT", KeyEvent.VK_BACK_QUOTE, '`', '~')
		};
		keyDown = new boolean[keyBindings.length];
		keySuppressUntilRelease = new boolean[keyBindings.length];
	}

	private void installInputCallbacks() throws Exception {
		if (!inputEnabled) {
			return;
		}

		scrollCallback = gl.createScrollCallback(this::handleScroll);
		gl.glfwSetScrollCallback(window, scrollCallback);
		keyCallback = gl.createKeyCallback(this::handleKey);
		gl.glfwSetKeyCallback(window, keyCallback);
		charCallback = gl.createCharCallback(this::handleChar);
		gl.glfwSetCharCallback(window, charCallback);
		windowFocusCallback = gl.createWindowFocusCallback(this::handleWindowFocus);
		gl.glfwSetWindowFocusCallback(window, windowFocusCallback);
		inputFocused = gl.glfwGetWindowAttrib(window, gl.GLFW_FOCUSED) == gl.GLFW_TRUE;
	}

	private KeyBinding key(String glfwConstantName, int awtKeyCode, char normalChar, char shiftedChar)
		throws Exception {
		return new KeyBinding(gl.glfwConstant(glfwConstantName), awtKeyCode, normalChar, shiftedChar);
	}

	private void processInput() throws Exception {
		if (!inputEnabled || OpenRSC.applet == null || window == 0L) {
			return;
		}

		if (inputFocused) {
			processMouseInput();
		}
	}

	private void processMouseInput() throws Exception {
		double[] cursorX = new double[1];
		double[] cursorY = new double[1];
		gl.glfwGetCursorPos(window, cursorX, cursorY);

		int x = mapMouseX(cursorX[0]);
		int y = mapMouseY(cursorY[0]);

		int[] glfwButtons = {
			gl.GLFW_MOUSE_BUTTON_LEFT,
			gl.GLFW_MOUSE_BUTTON_RIGHT,
			gl.GLFW_MOUSE_BUTTON_MIDDLE
		};
		int[] awtButtons = {
			MouseEvent.BUTTON1,
			MouseEvent.BUTTON3,
			MouseEvent.BUTTON2
		};

		for (int i = 0; i < glfwButtons.length; i++) {
			boolean pressed = gl.glfwGetMouseButton(window, glfwButtons[i]) == gl.GLFW_PRESS;
			if (pressed != mouseButtonDown[i]) {
				mouseButtonDown[i] = pressed;
				postMouseEvent(
					pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
					x,
					y,
					awtButtons[i]);
			}
		}

		if (x != lastMouseX || y != lastMouseY) {
			postMouseEvent(
				isAnyMouseButtonDown() ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
				x,
				y,
				activeAwtMouseButton());
			lastMouseX = x;
			lastMouseY = y;
		}
	}

	private void handleScroll(long callbackWindow, double xOffset, double yOffset) {
		if (!inputEnabled || callbackWindow != window) {
			return;
		}

		double[] cursorX = new double[1];
		double[] cursorY = new double[1];
		try {
			gl.glfwGetCursorPos(window, cursorX, cursorY);
		} catch (Exception e) {
			return;
		}

		int x = mapMouseX(cursorX[0]);
		int y = mapMouseY(cursorY[0]);
		int wheelRotation = yOffset > 0.0 ? -1 : 1;
		postMouseWheelEvent(x, y, wheelRotation, -yOffset);
	}

	private void handleKey(long callbackWindow, int glfwKey, int scanCode, int action, int mods) {
		if (!inputEnabled || callbackWindow != window) {
			return;
		}

		int keyIndex = keyBindingIndex(glfwKey);
		if (keyIndex < 0) {
			return;
		}

		KeyBinding binding = keyBindings[keyIndex];
		boolean repeated = action == gl.GLFW_REPEAT;
		if (repeated && !binding.postsRepeatPressEvents()) {
			return;
		}

		boolean pressed = action == gl.GLFW_PRESS || repeated;
		boolean released = action == gl.GLFW_RELEASE;
		if (!pressed && !released) {
			return;
		}

		if (keySuppressUntilRelease[keyIndex]) {
			if (released) {
				keySuppressUntilRelease[keyIndex] = false;
				keyDown[keyIndex] = false;
			}
			return;
		}

		if (!repeated && pressed == keyDown[keyIndex]) {
			return;
		}

		if (!repeated) {
			keyDown[keyIndex] = pressed;
		}
		if (pressed
			&& FRAME_CAPTURE_HOTKEY_ENABLED
			&& binding.awtKeyCode == KeyEvent.VK_F9
			&& (mods & gl.GLFW_MOD_CONTROL) != 0) {
			frameCaptureRequested = true;
			frameCaptureBurstRemaining = FRAME_CAPTURE_BURST_FRAMES;
			log("OpenGL frame capture burst requested; next "
				+ FRAME_CAPTURE_BURST_FRAMES
				+ " rendered frames will be dumped.");
			keySuppressUntilRelease[keyIndex] = true;
			return;
		}
		if (!binding.postsPhysicalEvents()) {
			return;
		}

		postKeyEvent(
			pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
			binding,
			pressed ? binding.keyChar((mods & gl.GLFW_MOD_SHIFT) != 0) : binding.normalChar);
	}

	private void handleChar(long callbackWindow, int codepoint) {
		if (!inputEnabled || callbackWindow != window || !inputFocused || !Character.isValidCodePoint(codepoint)) {
			return;
		}

		char[] chars = Character.toChars(codepoint);
		for (char keyChar : chars) {
			postTypedCharacter(keyChar);
		}
	}

	private void handleWindowFocus(long callbackWindow, boolean focused) {
		if (!inputEnabled || callbackWindow != window) {
			return;
		}

		if (!focused) {
			releaseInputState();
		}
		inputFocused = focused;
	}

	private int keyBindingIndex(int glfwKey) {
		for (int i = 0; i < keyBindings.length; i++) {
			if (keyBindings[i].glfwKey == glfwKey) {
				return i;
			}
		}
		return -1;
	}

	private int mapMouseX(double cursorX) {
		Viewport viewport = currentDrawViewport;
		int sourceWidth = Math.max(1, currentSourceWidth);
		int x = (int) Math.round((cursorX - viewport.x) * sourceWidth / Math.max(1, viewport.width));
		return clamp(x, 0, sourceWidth - 1);
	}

	private int mapMouseY(double cursorY) {
		Viewport viewport = currentDrawViewport;
		int sourceHeight = Math.max(1, currentSourceHeight);
		int y = (int) Math.round((cursorY - viewport.y) * sourceHeight / Math.max(1, viewport.height));
		return clamp(y, 0, sourceHeight - 1);
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void releaseInputState() {
		if (!inputFocused) {
			return;
		}

		int x = lastMouseX >= 0 ? lastMouseX : 0;
		int y = lastMouseY >= 0 ? lastMouseY : 0;
		for (int i = 0; i < mouseButtonDown.length; i++) {
			if (mouseButtonDown[i]) {
				mouseButtonDown[i] = false;
				postMouseEvent(MouseEvent.MOUSE_RELEASED, x, y, mouseButtonIndexToAwtButton(i));
			}
		}

		for (int i = 0; i < keyDown.length; i++) {
			if (keyDown[i]) {
				keyDown[i] = false;
				postKeyEvent(KeyEvent.KEY_RELEASED, keyBindings[i], keyBindings[i].normalChar);
			}
		}
	}

	private void suppressKeysUntilRelease() {
		for (int i = 0; i < keySuppressUntilRelease.length; i++) {
			keySuppressUntilRelease[i] = true;
			keyDown[i] = false;
		}
	}

	private int mouseButtonIndexToAwtButton(int index) {
		if (index == 1) {
			return MouseEvent.BUTTON3;
		}
		if (index == 2) {
			return MouseEvent.BUTTON2;
		}
		return MouseEvent.BUTTON1;
	}

	private boolean isAnyMouseButtonDown() {
		for (boolean down : mouseButtonDown) {
			if (down) {
				return true;
			}
		}
		return false;
	}

	private int activeAwtMouseButton() {
		for (int i = 0; i < mouseButtonDown.length; i++) {
			if (mouseButtonDown[i]) {
				return mouseButtonIndexToAwtButton(i);
			}
		}
		return MouseEvent.NOBUTTON;
	}

	private boolean isKeyDown(int awtKeyCode) {
		for (int i = 0; i < keyBindings.length; i++) {
			if (keyBindings[i].awtKeyCode == awtKeyCode && keyDown[i]) {
				return true;
			}
		}
		return false;
	}

	private boolean isGlfwKeyDown(int glfwKey) {
		if (gl == null || window == 0L) {
			return false;
		}
		try {
			return gl.glfwGetKey(window, glfwKey) == gl.GLFW_PRESS;
		} catch (Exception ignored) {
			return false;
		}
	}

	private boolean isShiftDown() {
		if (gl == null) {
			return isKeyDown(KeyEvent.VK_SHIFT);
		}
		return isKeyDown(KeyEvent.VK_SHIFT)
			|| isGlfwKeyDown(gl.GLFW_KEY_LEFT_SHIFT)
			|| isGlfwKeyDown(gl.GLFW_KEY_RIGHT_SHIFT);
	}

	private boolean isControlDown() {
		if (gl == null) {
			return isKeyDown(KeyEvent.VK_CONTROL);
		}
		return isKeyDown(KeyEvent.VK_CONTROL)
			|| isGlfwKeyDown(gl.GLFW_KEY_LEFT_CONTROL)
			|| isGlfwKeyDown(gl.GLFW_KEY_RIGHT_CONTROL);
	}

	private boolean isAltDown() {
		if (gl == null) {
			return isKeyDown(KeyEvent.VK_ALT);
		}
		return isKeyDown(KeyEvent.VK_ALT)
			|| isGlfwKeyDown(gl.GLFW_KEY_LEFT_ALT)
			|| isGlfwKeyDown(gl.GLFW_KEY_RIGHT_ALT);
	}

	private int currentModifiers() {
		int modifiers = 0;
		if (isShiftDown()) {
			modifiers |= InputEvent.SHIFT_MASK;
		}
		if (isControlDown()) {
			modifiers |= InputEvent.CTRL_MASK;
		}
		if (isAltDown()) {
			modifiers |= InputEvent.ALT_MASK;
		}
		if (mouseButtonDown[0]) {
			modifiers |= InputEvent.BUTTON1_MASK;
		}
		if (mouseButtonDown[1]) {
			modifiers |= InputEvent.BUTTON3_MASK;
		}
		if (mouseButtonDown[2]) {
			modifiers |= InputEvent.BUTTON2_MASK;
		}
		return modifiers;
	}

	private void postMouseEvent(int id, int x, int y, int button) {
		OpenRSC applet = OpenRSC.applet;
		if (applet == null || applet.getMouseHandler() == null) {
			return;
		}

		int modifiers = currentModifiers();

		Component source = applet;
		MouseEvent event = new MouseEvent(
			source,
			id,
			System.currentTimeMillis(),
			modifiers,
			x,
			y,
			0,
			false,
			button);

		EventQueue.invokeLater(() -> {
			if (id == MouseEvent.MOUSE_PRESSED) {
				applet.getMouseHandler().mousePressed(event);
			} else if (id == MouseEvent.MOUSE_RELEASED) {
				applet.getMouseHandler().mouseReleased(event);
			} else if (id == MouseEvent.MOUSE_DRAGGED) {
				applet.getMouseHandler().mouseDragged(event);
			} else if (id == MouseEvent.MOUSE_MOVED) {
				applet.getMouseHandler().mouseMoved(event);
			}
		});
	}

	private void postMouseWheelEvent(int x, int y, int wheelRotation, double preciseWheelRotation) {
		OpenRSC applet = OpenRSC.applet;
		if (applet == null || applet.getMouseHandler() == null) {
			return;
		}

		int modifiers = currentModifiers();

		MouseWheelEvent event = new MouseWheelEvent(
			applet,
			MouseEvent.MOUSE_WHEEL,
			System.currentTimeMillis(),
			modifiers,
			x,
			y,
			0,
			0,
			0,
			false,
			MouseWheelEvent.WHEEL_UNIT_SCROLL,
			1,
			wheelRotation,
			preciseWheelRotation);

		EventQueue.invokeLater(() -> applet.getMouseHandler().mouseWheelMoved(event));
	}

	private void postKeyEvent(int id, KeyBinding binding, char keyChar) {
		OpenRSC applet = OpenRSC.applet;
		if (applet == null || applet.getKeyHandler() == null) {
			return;
		}

		int modifiers = currentModifiers();

		KeyEvent event = new KeyEvent(
			applet,
			id,
			System.currentTimeMillis(),
			modifiers,
			binding.awtKeyCode,
			keyChar,
			KeyEvent.KEY_LOCATION_STANDARD);

		EventQueue.invokeLater(() -> {
			if (id == KeyEvent.KEY_PRESSED) {
				applet.getKeyHandler().keyPressed(event);
			} else if (id == KeyEvent.KEY_RELEASED) {
				applet.getKeyHandler().keyReleased(event);
			}
		});
	}

	private void postTypedCharacter(char keyChar) {
		OpenRSC applet = OpenRSC.applet;
		if (applet == null || applet.getKeyHandler() == null) {
			return;
		}

		KeyEvent event = new KeyEvent(
			applet,
			KeyEvent.KEY_PRESSED,
			System.currentTimeMillis(),
			currentModifiers(),
			KeyEvent.VK_UNDEFINED,
			keyChar,
			KeyEvent.KEY_LOCATION_UNKNOWN);

		EventQueue.invokeLater(() -> applet.getKeyHandler().keyPressed(event));
	}

	private void cleanup(boolean glfwInitialized) {
		boolean userClosedWindow = !closed && primaryWindow;
		releaseInputState();
		try {
			if (gl != null && window != 0L) {
				if (spriteTextureCache != null) {
					spriteTextureCache.close();
					spriteTextureCache = null;
				}
				if (glyphTextureCache != null) {
					glyphTextureCache.close();
					glyphTextureCache = null;
				}
				if (visibleSpriteTextureAtlas != null) {
					visibleSpriteTextureAtlas.close();
					visibleSpriteTextureAtlas = null;
				}
				if (debugOverlayTextureAtlas != null) {
					debugOverlayTextureAtlas.close();
					debugOverlayTextureAtlas = null;
				}
				if (worldMeshRenderer != null) {
					worldMeshRenderer.close();
					worldMeshRenderer = null;
				}
				if (worldChunkRenderer != null) {
					worldChunkRenderer.close();
					worldChunkRenderer = null;
				}
				if (worldTextureCache != null) {
					worldTextureCache.close();
					worldTextureCache = null;
				}
				if (projectedWorldShader != null) {
					projectedWorldShader.close();
					projectedWorldShader = null;
				}
				if (residentChunkShader != null) {
					residentChunkShader.close();
					residentChunkShader = null;
				}
				deleteWorldSpriteQuadBuffers();
				deleteScreenQuadBuffers();
				deleteUnitQuadBuffers();
				if (textureId != 0) {
					gl.glDeleteTextures(textureId);
					textureId = 0;
					textureWidth = 0;
					textureHeight = 0;
				}
				if (scrollCallback != null) {
					gl.glfwSetScrollCallback(window, null);
					gl.freeCallback(scrollCallback);
					scrollCallback = null;
				}
				if (keyCallback != null) {
					gl.glfwSetKeyCallback(window, null);
					gl.freeCallback(keyCallback);
					keyCallback = null;
				}
				if (charCallback != null) {
					gl.glfwSetCharCallback(window, null);
					gl.freeCallback(charCallback);
					charCallback = null;
				}
				if (windowFocusCallback != null) {
					gl.glfwSetWindowFocusCallback(window, null);
					gl.freeCallback(windowFocusCallback);
					windowFocusCallback = null;
				}
				gl.glfwDestroyWindow(window);
			}
		} catch (Throwable ignored) {
		} finally {
			window = 0L;
		}

		try {
			if (gl != null && glfwInitialized) {
				gl.glfwTerminate();
			}
		} catch (Throwable ignored) {
		}

		if (userClosedWindow) {
			closeClientFromOpenGLWindow();
		}
	}

	private void disable(String message) {
		disabled = true;
		log(message);
		synchronized (frameLock) {
			if (pendingFrame != null) {
				pendingFrame.release();
				pendingFrame = null;
			}
			frameLock.notifyAll();
		}
	}

	@Override
	public void close() {
		log("OpenGL presenter close requested.");
		closed = true;
		synchronized (frameLock) {
			if (pendingFrame != null) {
				pendingFrame.release();
				pendingFrame = null;
			}
			frameLock.notifyAll();
		}
	}

	private static void log(String message) {
		System.out.println("[renderer-v2 opengl] " + message);
	}

	private static ByteBuffer buildPaddedRgbaPixels(
		ByteBuffer sourcePixels,
		int contentWidth,
		int contentHeight,
		int padding) {
		if (padding <= 0) {
			ByteBuffer duplicate = sourcePixels.duplicate();
			duplicate.position(0);
			duplicate.limit(contentWidth * contentHeight * 4);
			return duplicate;
		}

		int paddedWidth = contentWidth + padding * 2;
		int paddedHeight = contentHeight + padding * 2;
		ByteBuffer source = sourcePixels.duplicate();
		source.position(0);
		source.limit(contentWidth * contentHeight * 4);
		ByteBuffer padded = ByteBuffer.allocateDirect(paddedWidth * paddedHeight * 4);
		for (int y = -padding; y < contentHeight + padding; y++) {
			int sourceY = clampStatic(y, 0, contentHeight - 1);
			for (int x = -padding; x < contentWidth + padding; x++) {
				int sourceX = clampStatic(x, 0, contentWidth - 1);
				padded.putInt(source.getInt((sourceY * contentWidth + sourceX) * 4));
			}
		}
		padded.flip();
		return padded;
	}

	private static int clampStatic(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean readBoolean(String propertyName, String envName) {
		return readBoolean(propertyName, envName, false);
	}

	private static boolean readBoolean(String propertyName, String envName, boolean defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null) {
			return defaultValue;
		}

		value = value.trim();
		return "true".equalsIgnoreCase(value)
			|| "1".equals(value)
			|| "yes".equalsIgnoreCase(value)
			|| "on".equalsIgnoreCase(value);
	}

	private static boolean shouldDrawRemasterTerrainShadowMask() {
		return REMASTER_TERRAIN_SHADOW_MASK
			&& RendererLightingSettings.getMode() == RendererLightingSettings.Mode.DIRECTIONAL;
	}

	private static float readFloat(
		String propertyName,
		String envName,
		float defaultValue,
		float minValue,
		float maxValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.trim().isEmpty()) {
			value = System.getenv(envName);
		}
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}

		try {
			float parsed = Float.parseFloat(value.trim());
			if (Float.isNaN(parsed)) {
				return defaultValue;
			}
			return Math.max(minValue, Math.min(maxValue, parsed));
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	@SuppressWarnings("unused")
	private OpenGLTextureRegion uploadSpriteTexture(Sprite sprite) throws Exception {
		return uploadSpriteTexture(sprite, RendererSpriteTransform.IDENTITY);
	}

	private OpenGLTextureRegion uploadSpriteTexture(Sprite sprite, RendererSpriteTransform transform) throws Exception {
		if (spriteTextureCache == null) {
			throw new IllegalStateException("OpenGL sprite texture cache is not initialized");
		}
		return spriteTextureCache.getOrUpload(sprite, transform);
	}

	private OpenGLTextureRegion uploadSpriteTexture(Sprite sprite, boolean transparentMask) throws Exception {
		if (spriteTextureCache == null) {
			throw new IllegalStateException("OpenGL sprite texture cache is not initialized");
		}
		return spriteTextureCache.getOrUpload(sprite, RendererSpriteTransform.IDENTITY, transparentMask);
	}

	private static void closeClientFromOpenGLWindow() {
		EventQueue.invokeLater(() -> {
			if (OpenRSC.jframe != null) {
				OpenRSC.jframe.dispatchEvent(new WindowEvent(OpenRSC.jframe, WindowEvent.WINDOW_CLOSING));
			} else if (OpenRSC.applet != null) {
				OpenRSC.applet.close();
			}
		});
	}

	private static Viewport computeViewport(
		OpenGLPresentationSettings.ScaleMode scaleMode,
		int surfaceWidth,
		int surfaceHeight,
		int sourceWidth,
		int sourceHeight) {
		switch (scaleMode) {
			case INTEGER_FIT:
				int scale = Math.min(surfaceWidth / sourceWidth, surfaceHeight / sourceHeight);
				if (scale >= 1) {
					return centeredViewport(surfaceWidth, surfaceHeight, sourceWidth * scale, sourceHeight * scale);
				}
				return computeViewport(
					OpenGLPresentationSettings.ScaleMode.ASPECT_FIT,
					surfaceWidth,
					surfaceHeight,
					sourceWidth,
					sourceHeight);
			case STRETCH:
				return new Viewport(0, 0, Math.max(1, surfaceWidth), Math.max(1, surfaceHeight));
			case ASPECT_FIT:
			default:
				return computeAspectViewport(surfaceWidth, surfaceHeight, sourceWidth / (double) sourceHeight);
		}
	}

	private static Viewport computeAspectViewport(int surfaceWidth, int surfaceHeight, double aspectRatio) {
		int width = Math.max(1, surfaceWidth);
		int height = Math.max(1, (int) Math.round(width / aspectRatio));
		if (height > surfaceHeight) {
			height = Math.max(1, surfaceHeight);
			width = Math.max(1, (int) Math.round(height * aspectRatio));
		}
		return centeredViewport(surfaceWidth, surfaceHeight, width, height);
	}

	private static Viewport centeredViewport(int surfaceWidth, int surfaceHeight, int width, int height) {
		int clampedWidth = Math.max(1, Math.min(surfaceWidth, width));
		int clampedHeight = Math.max(1, Math.min(surfaceHeight, height));
		int x = Math.max(0, (surfaceWidth - clampedWidth) / 2);
		int y = Math.max(0, (surfaceHeight - clampedHeight) / 2);
		return new Viewport(x, y, clampedWidth, clampedHeight);
	}

	private static final class Viewport {
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		private Viewport(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}

	private static final class MonitorMode {
		private final int x;
		private final int y;
		private final int width;
		private final int height;

		private MonitorMode(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = Math.max(1, width);
			this.height = Math.max(1, height);
		}
	}

	private static final class WorldSpriteMatch {
		private final Renderer3DFrame.SpriteAnchor anchor;
		private final Renderer2DFrame.SpriteCommand command;

		private WorldSpriteMatch(Renderer3DFrame.SpriteAnchor anchor, Renderer2DFrame.SpriteCommand command) {
			this.anchor = anchor;
			this.command = command;
		}
	}

	private static final class WorldSpriteCommand {
		private final Renderer2DFrame.SpriteCommand command;
		private final Renderer3DFrame.SpriteAnchor anchor;
		private final WorldSpriteAnchorMatch anchorMatch;
		private final int legacyDrawOrder;
		private final boolean sourceCropped;
		private final boolean skewed;

		private WorldSpriteCommand(
			Renderer2DFrame.SpriteCommand command,
			Renderer3DFrame.SpriteAnchor anchor,
			WorldSpriteAnchorMatch anchorMatch) {
			this.command = command;
			this.anchor = anchor;
			this.anchorMatch = anchorMatch == null ? WorldSpriteAnchorMatch.unmatched() : anchorMatch;
			this.legacyDrawOrder = anchor == null ? Integer.MAX_VALUE : anchor.getLegacyDrawOrder();
			Sprite sprite = command == null ? null : command.getSprite();
			this.sourceCropped = command != null
				&& sprite != null
				&& (command.getSourceX() != 0
					|| command.getSourceY() != 0
					|| command.getSourceWidth() != sprite.getWidth()
					|| command.getSourceHeight() != sprite.getHeight());
			this.skewed = command != null && command.getTopX16() != command.getBottomX16();
		}

		private boolean hasAnchor() {
			return anchor != null;
		}
	}

	private static final class CompositeWorldSpriteTexture {
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final DynamicTextureData textureData;

		private CompositeWorldSpriteTexture(
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

	private static final class StaticWorldCommand {
		private final Renderer3DModelKind modelKind;
		private final Set<Long> faceKeys;
		private final int triangleCount;
		private final int minLegacyDrawOrder;
		private final int maxLegacyDrawOrder;

		private StaticWorldCommand(
			Renderer3DModelKind modelKind,
			Set<Long> faceKeys,
			int triangleCount,
			int minLegacyDrawOrder,
			int maxLegacyDrawOrder) {
			this.modelKind = modelKind;
			this.faceKeys = Collections.unmodifiableSet(new HashSet<Long>(faceKeys));
			this.triangleCount = triangleCount;
			this.minLegacyDrawOrder = minLegacyDrawOrder;
			this.maxLegacyDrawOrder = maxLegacyDrawOrder;
		}
	}

	private static final class StaticWorldCommandBuilder {
		private final Renderer3DModelKind modelKind;
		private final Set<Long> faceKeys = new HashSet<Long>();
		private int triangleCount;
		private int minLegacyDrawOrder = Integer.MAX_VALUE;
		private int maxLegacyDrawOrder = Integer.MIN_VALUE;

		private StaticWorldCommandBuilder(Renderer3DModelKind modelKind) {
			this.modelKind = modelKind;
		}

		private void addTriangle(long faceKey, int legacyDrawOrder) {
			faceKeys.add(faceKey);
			triangleCount++;
			minLegacyDrawOrder = Math.min(minLegacyDrawOrder, legacyDrawOrder);
			maxLegacyDrawOrder = Math.max(maxLegacyDrawOrder, legacyDrawOrder);
		}

		private StaticWorldCommand build() {
			return new StaticWorldCommand(
				modelKind,
				faceKeys,
				triangleCount,
				minLegacyDrawOrder,
				maxLegacyDrawOrder);
		}
	}

	private enum StaticWorldMaterialPass {
		OPAQUE,
		CUTOUT,
		TRANSLUCENT,
		DISCARDED,
		UNRESOLVED
	}

	private static final class StaticWorldMaterialTriangle {
		private final int triangleIndex;
		private final StaticWorldMaterialPass materialPass;
		private final Renderer3DModelKind modelKind;
		private final int modelIndex;
		private final int faceId;
		private final int textureId;
		private final boolean textureHasTransparency;

		private StaticWorldMaterialTriangle(
			int triangleIndex,
			StaticWorldMaterialPass materialPass,
			Renderer3DModelKind modelKind,
			int modelIndex,
			int faceId,
			int textureId,
			boolean textureHasTransparency) {
			this.triangleIndex = triangleIndex;
			this.materialPass = materialPass;
			this.modelKind = modelKind;
			this.modelIndex = modelIndex;
			this.faceId = faceId;
			this.textureId = textureId;
			this.textureHasTransparency = textureHasTransparency;
		}
	}

	private static final class OpenGLCompositeSceneCommand {
		private enum Kind {
			WORLD_SPRITE
		}

		private final Kind kind;
		private final WorldSpriteCommand worldSpriteCommand;
		private final int legacyDrawOrder;
		private final int sequence;
		private final int minExclusiveOrder;
		private final int maxExclusiveOrder;
		private final Set<Long> frontOccluderFaceKeys;

		private OpenGLCompositeSceneCommand(
			Kind kind,
			WorldSpriteCommand worldSpriteCommand,
			int legacyDrawOrder,
			int sequence,
			int minExclusiveOrder,
			int maxExclusiveOrder,
			Set<Long> frontOccluderFaceKeys) {
			this.kind = kind;
			this.worldSpriteCommand = worldSpriteCommand;
			this.legacyDrawOrder = legacyDrawOrder;
			this.sequence = sequence;
			this.minExclusiveOrder = minExclusiveOrder;
			this.maxExclusiveOrder = maxExclusiveOrder;
			this.frontOccluderFaceKeys = frontOccluderFaceKeys == null
				? Collections.<Long>emptySet()
				: Collections.unmodifiableSet(new HashSet<Long>(frontOccluderFaceKeys));
		}

		private static OpenGLCompositeSceneCommand worldSprite(WorldSpriteCommand command) {
			int sequence = command == null || command.command == null ? Integer.MAX_VALUE : command.command.getSequence();
			int legacyDrawOrder = command == null ? Integer.MAX_VALUE : command.legacyDrawOrder;
			return new OpenGLCompositeSceneCommand(
				Kind.WORLD_SPRITE,
				command,
				legacyDrawOrder,
				sequence,
				Integer.MIN_VALUE,
				Integer.MAX_VALUE,
				Collections.<Long>emptySet());
		}

	}

	private static final class OpenGLWorldChunkRenderer implements AutoCloseable {
		private static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;
		private static final int NO_FALLBACK_COLOR = Integer.MIN_VALUE;
		private static final int MAX_RESIDENT_CHUNKS = 256;
		private static final int POSITION_COMPONENT_COUNT = 3;
		private static final int COLOR_COMPONENT_COUNT = 4;
		private static final int TEXTURE_COORD_COMPONENT_COUNT = 2;
		private static final int TEXTURE_LIGHT_COMPONENT_COUNT = 4;
		private static final int LEGACY_LIGHT_COMPONENT_COUNT = 1;
		private static final int BASE_LEGACY_LIGHT_COMPONENT_COUNT = 1;
		private static final int RAW_MATERIAL_COLOR_COMPONENT_COUNT = 3;
		private static final int NORMAL_COMPONENT_COUNT = 3;
		private static final int MODEL_KIND_COMPONENT_COUNT = 1;
		private static final int FLOATS_PER_VERTEX =
			POSITION_COMPONENT_COUNT
				+ COLOR_COMPONENT_COUNT
				+ TEXTURE_COORD_COMPONENT_COUNT
				+ TEXTURE_LIGHT_COMPONENT_COUNT
				+ LEGACY_LIGHT_COMPONENT_COUNT
				+ BASE_LEGACY_LIGHT_COMPONENT_COUNT
				+ RAW_MATERIAL_COLOR_COMPONENT_COUNT
				+ NORMAL_COMPONENT_COUNT
				+ MODEL_KIND_COMPONENT_COUNT;
		private static final int COLOR_OFFSET_BYTES = POSITION_COMPONENT_COUNT * 4;
		private static final int TEXTURE_COORD_OFFSET_BYTES =
			(POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * 4;
		private static final int TEXTURE_LIGHT_OFFSET_BYTES =
			(POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * 4;
		private static final int LEGACY_LIGHT_OFFSET_BYTES =
			TEXTURE_LIGHT_OFFSET_BYTES + TEXTURE_LIGHT_COMPONENT_COUNT * 4;
		private static final int BASE_LEGACY_LIGHT_OFFSET_BYTES =
			LEGACY_LIGHT_OFFSET_BYTES + LEGACY_LIGHT_COMPONENT_COUNT * 4;
		private static final int RAW_MATERIAL_COLOR_OFFSET_BYTES =
			BASE_LEGACY_LIGHT_OFFSET_BYTES + BASE_LEGACY_LIGHT_COMPONENT_COUNT * 4;
		private static final int NORMAL_OFFSET_BYTES =
			RAW_MATERIAL_COLOR_OFFSET_BYTES + RAW_MATERIAL_COLOR_COMPONENT_COUNT * 4;
		private static final int MODEL_KIND_OFFSET_BYTES =
			NORMAL_OFFSET_BYTES + NORMAL_COMPONENT_COUNT * 4;
		private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * 4;
		private static final int SPATIAL_BATCH_TILE_SIZE = 6;
		private static final int SPATIAL_BATCH_WORLD_SIZE = SPATIAL_BATCH_TILE_SIZE * 128;
		private static final int SPATIAL_BATCH_AXIS = 24;
		private static final int SPATIAL_BATCH_DISABLED = -1;
		private static final float FRUSTUM_BATCH_CULL_SCREEN_PADDING = 128.0f;
		private static final float FOG_BATCH_CULL_PADDING = 0.0f;
		private static final float WALL_DEPTH_PRIORITY_FACTOR = -1.0f;
		private static final float WALL_DEPTH_PRIORITY_UNITS = -1.0f;
		private static final float SHADOW_PROOF_DIRECTION_X = 0.78f;
		private static final float SHADOW_PROOF_DIRECTION_Z = 0.46f;
		private static final float SHADOW_PROOF_MIN_LENGTH = 384.0f;
		private static final float SHADOW_PROOF_MAX_LENGTH = 1536.0f;
		private static final float SHADOW_PROOF_MIN_WIDTH = 32.0f;
		private static final float SHADOW_PROOF_DIAGONAL_MIN_SPAN = 48.0f;
		private static final float SHADOW_PROOF_DIRECTIONAL_ALPHA = 0.55f;
		private static final float SHADOW_PROOF_MAX_ALPHA = 0.72f;
		private static final int REMASTER_SHADOW_MASK_GRID_SIZE = 512;
		private static final float REMASTER_SHADOW_MASK_BASE_ALPHA = 0.42f;
		private static final float REMASTER_SHADOW_MASK_MAX_ALPHA = 0.58f;
		private static final float REMASTER_SHADOW_MASK_MIN_LENGTH = 96.0f;
		private static final float REMASTER_SHADOW_MASK_MAX_LENGTH = 1792.0f;
		private static final float REMASTER_SHADOW_MASK_MIN_WIDTH = 24.0f;
		private static final float REMASTER_SHADOW_MASK_MIN_DRAW_ALPHA = 0.018f;
		private static final int REMASTER_SHADOW_MASK_TEXTURE_SIZE = 1024;
		private static final float REMASTER_SHADOW_MASK_TEXTURE_PADDING = 384.0f;
		private static final int REMASTER_SHADOW_MASK_BLUR_RADIUS = 7;
		private static final float REMASTER_SHADOW_MASK_AZIMUTH_BUCKET_DEGREES = 6.0f;
		private static final float REMASTER_SHADOW_MASK_ELEVATION_BUCKET_DEGREES = 3.0f;
		private static final float REMASTER_SHADOW_MASK_CENTER_RETAIN = 0.82f;
		private static final float REMASTER_SHADOW_MASK_BLUR_BOOST = 1.18f;
		private static final float REMASTER_SHADOW_MASK_CLIP_START_OFFSET = 24.0f;
		private static final boolean REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP = false;
		private static final Renderer3DModelKind[] WORLD_CHUNK_KIND_DRAW_ORDER = new Renderer3DModelKind[] {
			Renderer3DModelKind.TERRAIN,
			Renderer3DModelKind.WALL,
			Renderer3DModelKind.ROOF,
			Renderer3DModelKind.WALL_OBJECT,
			Renderer3DModelKind.GAME_OBJECT,
			Renderer3DModelKind.UNCLASSIFIED
		};

		private final LwjglBindings gl;
		private final OpenGLWorldTextureCache textureCache;
		private final OpenGLShaderProgram residentChunkShader;
		private final LinkedHashMap<WorldChunkBufferKey, WorldChunkBuffer> residentChunks =
			new LinkedHashMap<WorldChunkBufferKey, WorldChunkBuffer>(MAX_RESIDENT_CHUNKS, 0.75f, true);
		private FloatBuffer vertexUploadBuffer;
		private IntBuffer indexUploadBuffer;
		private IntBuffer materialIndexUploadBuffer;
		private FloatBuffer worldToClipMatrixBuffer;
		private FloatBuffer worldViewMatrixBuffer;
		private FloatBuffer fogColorBuffer;
		private int vertexUploadCapacity;
		private int indexUploadCapacity;
		private int materialIndexUploadCapacity;
		private int remasterShadowMaskTextureId;
		private int remasterShadowMaskTextureWidth;
		private int remasterShadowMaskTextureHeight;
		private long remasterShadowMaskUploadedSignature;
		private RemasterTerrainShadowMask remasterShadowMaskCache;
		private long remasterShadowMaskCacheSignature;
		private boolean remasterShadowMaskLastCacheHit;
		private boolean remasterShadowMaskLastRebuild;
		private boolean remasterShadowMaskLastUpload;
		private boolean remasterShadowMaskLastUploadSkip;
		private boolean closed;

		private OpenGLWorldChunkRenderer(
			LwjglBindings gl,
			OpenGLWorldTextureCache textureCache,
			OpenGLShaderProgram residentChunkShader) {
			this.gl = gl;
			this.textureCache = textureCache;
			this.residentChunkShader = residentChunkShader;
		}

		private OpenGLWorldChunkUploadStats upload(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame chunkFrame,
			boolean atlasTextureCoordinates) throws Exception {
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
				return OpenGLWorldChunkUploadStats.EMPTY;
			}
			if (atlasTextureCoordinates && frame != null && textureCache != null) {
				textureCache.uploadReferencedTextures(frame, chunkFrame);
			}

			int requestedChunks = 0;
			int uploadedChunks = 0;
			int reusedChunks = 0;
			List<ShadowProofCaster> shadowProofCasters = shouldDrawProjectedShadowProof()
				? buildProjectedShadowProofCasters(chunkFrame)
				: Collections.<ShadowProofCaster>emptyList();
			long shadowProofSignature =
				shouldDrawProjectedShadowProof() ? shadowProofCasterSignature(shadowProofCasters) : 0L;
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int vertexCount = chunk.getVertexCount();
				int indexCount = chunk.getIndexCount();
				if (vertexCount <= 0 || indexCount <= 0) {
					continue;
				}

				requestedChunks++;
				WorldChunkBufferKey key = WorldChunkBufferKey.from(chunk);
				WorldChunkBuffer buffer = residentChunks.get(key);
				int brightnessBits = currentBrightnessBits();
				int fogModeBits = currentFogModeBits();
				int lightingModeBits = currentLightingModeBits();
				int geometryModeBits = currentGeometryModeBits();
				if (buffer != null && buffer.matches(
					chunk.getSignature(),
					vertexCount,
					indexCount,
					chunk.getTriangleCount(),
					atlasTextureCoordinates,
					brightnessBits,
					fogModeBits,
					lightingModeBits,
					geometryModeBits,
					WORLD_CHUNKS_REPLACEMENT_COMPOSITE,
					shadowProofSignature)) {
					reusedChunks++;
					continue;
				}

				if (buffer == null) {
					buffer = new WorldChunkBuffer(gl.glGenBuffers(), gl.glGenBuffers(), gl.glGenBuffers());
					residentChunks.put(key, buffer);
				}
				uploadChunk(
					chunk,
					buffer,
					frame,
					vertexCount,
					indexCount,
					atlasTextureCoordinates,
					brightnessBits,
					fogModeBits,
					lightingModeBits,
					geometryModeBits,
					WORLD_CHUNKS_REPLACEMENT_COMPOSITE,
					shadowProofCasters,
					shadowProofSignature);
				uploadedChunks++;
			}

			int evictedChunks = evictOverflow();
			return new OpenGLWorldChunkUploadStats(requestedChunks, uploadedChunks, reusedChunks, evictedChunks);
		}

		private OpenGLWorldChunkDrawStats drawDiagnostic(
			Renderer3DFrame frame,
			boolean filled,
			boolean textured) throws Exception {
			Renderer3DWorldChunkFrame chunkFrame = frame.getWorldChunkFrame();
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0 || residentChunks.isEmpty()) {
				return OpenGLWorldChunkDrawStats.EMPTY;
			}
			if ((filled || textured) && textureCache != null) {
				textureCache.uploadReferencedTextures(frame, chunkFrame);
			}

			boolean shaderActive = useResidentChunkShader(textured);
			FloatBuffer residentWorldToClipMatrix = null;
			FloatBuffer residentWorldViewMatrix = null;
			loadWorldProjectionAndView(frame);
			float[] batchCullViewMatrix = worldViewMatrix(frame);
			if (shaderActive) {
				residentWorldToClipMatrix = putWorldToClipMatrix(
					multiply(projectionMatrix(frame), batchCullViewMatrix));
				residentWorldViewMatrix = putWorldViewMatrix(batchCullViewMatrix);
			}
			if (shaderActive) {
				gl.glDisable(gl.GL_FOG);
			} else {
				configureFog(frame);
			}
			if (textured) {
				gl.glEnable(gl.GL_TEXTURE_2D);
				gl.glDisable(gl.GL_BLEND);
				gl.glEnable(gl.GL_ALPHA_TEST);
				gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			} else {
				gl.glDisable(gl.GL_TEXTURE_2D);
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glEnable(gl.GL_BLEND);
				gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			}
			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
			boolean wireGeometry = RendererGeometrySettings.getMode() == RendererGeometrySettings.Mode.WIRE;
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, wireGeometry || (!filled && !textured) ? gl.GL_LINE : gl.GL_FILL);
			if (shaderActive) {
				gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
				gl.glDisableClientState(gl.GL_COLOR_ARRAY);
				gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			} else {
				gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
				gl.glEnableClientState(gl.GL_COLOR_ARRAY);
				if (textured) {
					gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				}
			}
			int drawnChunks = 0;
			int drawnTriangles = 0;
			int drawnTerrainTriangles = 0;
			int drawnWallTriangles = 0;
			int drawnRoofTriangles = 0;
			int drawnGameObjectTriangles = 0;
			int drawnWallObjectTriangles = 0;
			int drawnOtherTriangles = 0;
			int fallbackTriangles = 0;
			int skippedTriangles = 0;
			WorldChunkDrawAccumulator accumulator = new WorldChunkDrawAccumulator();
			try {
				if (textured) {
					drawChunkDiagnosticLayers(
						frame,
						chunkFrame,
						true,
						false,
						accumulator,
						batchCullViewMatrix,
						shaderActive,
						residentWorldToClipMatrix,
						residentWorldViewMatrix);
					gl.glEnable(gl.GL_BLEND);
					gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
					gl.glEnable(gl.GL_ALPHA_TEST);
					gl.glAlphaFunc(gl.GL_GREATER, 0.0f);
					drawChunkDiagnosticLayers(
						frame,
						chunkFrame,
						true,
						true,
						accumulator,
						batchCullViewMatrix,
						shaderActive,
						residentWorldToClipMatrix,
						residentWorldViewMatrix);
				} else {
					drawChunkDiagnosticLayers(
						frame,
						chunkFrame,
						false,
						false,
						accumulator,
						batchCullViewMatrix,
						false,
						null,
						null);
				}
				drawnChunks = accumulator.drawnChunkCount();
				drawnTriangles = accumulator.drawnTriangles;
				drawnTerrainTriangles = accumulator.drawnTerrainTriangles;
				drawnWallTriangles = accumulator.drawnWallTriangles;
				drawnRoofTriangles = accumulator.drawnRoofTriangles;
				drawnGameObjectTriangles = accumulator.drawnGameObjectTriangles;
				drawnWallObjectTriangles = accumulator.drawnWallObjectTriangles;
				drawnOtherTriangles = accumulator.drawnOtherTriangles;
				fallbackTriangles = accumulator.fallbackTriangles;
				skippedTriangles = accumulator.skippedTriangles;
			} finally {
				if (shaderActive && residentChunkShader != null) {
					residentChunkShader.unbindWorldTextureAttributes();
					gl.glUseProgram(0);
				}
				gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				gl.glDisableClientState(gl.GL_COLOR_ARRAY);
				gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
				gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
				gl.glDepthMask(true);
				gl.glDisable(gl.GL_DEPTH_TEST);
				gl.glDisable(gl.GL_FOG);
				gl.glDisable(gl.GL_BLEND);
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glDisable(gl.GL_CULL_FACE);
				gl.glEnable(gl.GL_TEXTURE_2D);
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}
			return new OpenGLWorldChunkDrawStats(
				accumulator.consideredChunkCount(),
				drawnChunks,
				accumulator.culledChunkCount(),
				drawnTriangles,
				drawnTerrainTriangles,
				drawnWallTriangles,
				drawnRoofTriangles,
				drawnGameObjectTriangles,
				drawnWallObjectTriangles,
				drawnOtherTriangles,
				fallbackTriangles,
				skippedTriangles,
				accumulator.consideredBatches,
				accumulator.drawnBatches,
				accumulator.culledBatches,
				accumulator.drawCalls,
				accumulator.textureBinds,
				accumulator.shadowProofChunks,
				accumulator.shadowProofIndices);
		}

		private void drawRemasterShadowInventoryDebug(Renderer3DFrame frame) throws Exception {
			Renderer3DWorldChunkFrame chunkFrame = frame == null ? null : frame.getWorldChunkFrame();
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
				return;
			}

			loadWorldProjectionAndView(frame);
			gl.glUseProgram(0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glDisable(gl.GL_FOG);
			gl.glDisable(gl.GL_CULL_FACE);
			gl.glEnable(gl.GL_BLEND);
			gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDepthMask(false);
			try {
				RemasterShadowRoofCoverage roofCoverage = RemasterShadowRoofCoverage.from(chunkFrame);
				drawRemasterShadowReceiverDebug(chunkFrame, roofCoverage);
				drawRemasterShadowCasterDebug(chunkFrame, roofCoverage);
			} finally {
				gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
				gl.glLineWidth(1.0f);
				gl.glDepthMask(true);
				gl.glDisable(gl.GL_BLEND);
				gl.glDisable(gl.GL_DEPTH_TEST);
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glDisable(gl.GL_FOG);
				gl.glDisable(gl.GL_CULL_FACE);
				gl.glEnable(gl.GL_TEXTURE_2D);
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

		private void drawRemasterShadowReceiverDebug(
			Renderer3DWorldChunkFrame chunkFrame,
			RemasterShadowRoofCoverage roofCoverage) throws Exception {
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glBegin(gl.GL_TRIANGLES);
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
				for (int triangle = 0; triangle < triangleCount; triangle++) {
					if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
						continue;
					}
					applyRemasterShadowReceiverDebugColor(roofClassificationForTriangle(roofCoverage, chunk, triangle));
					int indexBase = triangle * 3;
					for (int corner = 0; corner < 3; corner++) {
						drawChunkVertex(chunk, chunk.getIndex(indexBase + corner));
					}
				}
			}
			gl.glEnd();
		}

		private void drawRemasterShadowCasterDebug(
			Renderer3DWorldChunkFrame chunkFrame,
			RemasterShadowRoofCoverage roofCoverage) throws Exception {
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glLineWidth(2.0f);
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int casterCount = chunk.getShadowCasterCount();
				for (int index = 0; index < casterCount; index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (!isRemasterShadowEligibleCaster(caster)) {
						continue;
					}
					applyRemasterShadowCasterDebugColor(caster.getModelKind());
					drawRemasterShadowCasterOutline(caster, 0.0f);
				}
			}

			gl.glLineWidth(4.0f);
			gl.glColor4f(1.0f, 0.05f, 0.0f, 0.95f);
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int casterCount = chunk.getShadowCasterCount();
				for (int index = 0; index < casterCount; index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (isRemasterShadowClippingCandidate(caster)) {
						drawRemasterShadowCasterTopEdge(caster, 16.0f);
					}
				}
			}

			gl.glLineWidth(3.0f);
			gl.glColor4f(0.25f, 0.45f, 1.0f, 0.95f);
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int casterCount = chunk.getShadowCasterCount();
				for (int index = 0; index < casterCount; index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (isRemasterShadowEligibleCaster(caster)
						&& roofClassificationForCaster(roofCoverage, chunk, caster) > 0) {
						drawRemasterShadowCasterTopEdge(caster, 32.0f);
					}
				}
			}

			gl.glLineWidth(2.0f);
			gl.glColor4f(0.1f, 1.0f, 0.15f, 0.85f);
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int casterCount = chunk.getShadowCasterCount();
				for (int index = 0; index < casterCount; index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (isRemasterShadowEligibleCaster(caster)
						&& roofClassificationForCaster(roofCoverage, chunk, caster) == 0) {
						drawRemasterShadowCasterTopEdge(caster, 48.0f);
					}
				}
			}
		}

		private void applyRemasterShadowReceiverDebugColor(int classification) throws Exception {
			if (classification > 0) {
				gl.glColor4f(1.0f, 0.62f, 0.0f, 0.32f);
			} else if (classification == 0) {
				gl.glColor4f(0.0f, 0.82f, 1.0f, 0.20f);
			} else {
				gl.glColor4f(1.0f, 1.0f, 1.0f, 0.18f);
			}
		}

		private void applyRemasterShadowCasterDebugColor(Renderer3DModelKind kind) throws Exception {
			if (kind == Renderer3DModelKind.WALL) {
				gl.glColor4f(1.0f, 0.88f, 0.05f, 0.95f);
			} else if (kind == Renderer3DModelKind.GAME_OBJECT) {
				gl.glColor4f(1.0f, 0.2f, 0.95f, 0.95f);
			} else if (kind == Renderer3DModelKind.WALL_OBJECT) {
				gl.glColor4f(0.0f, 1.0f, 0.85f, 0.95f);
			} else {
				gl.glColor4f(1.0f, 1.0f, 1.0f, 0.8f);
			}
		}

		private void drawRemasterShadowCasterOutline(
			Renderer3DWorldChunkFrame.ShadowCaster caster,
			float yOffset) throws Exception {
			float baseY = caster.getBaseY() + yOffset;
			float topY = caster.getBaseY() + caster.getHeight() + yOffset;
			gl.glBegin(gl.GL_LINES);
			gl.glVertex3f(caster.getBaseX0(), baseY, caster.getBaseZ0());
			gl.glVertex3f(caster.getBaseX1(), baseY, caster.getBaseZ1());
			gl.glVertex3f(caster.getBaseX0(), topY, caster.getBaseZ0());
			gl.glVertex3f(caster.getBaseX1(), topY, caster.getBaseZ1());
			gl.glVertex3f(caster.getBaseX0(), baseY, caster.getBaseZ0());
			gl.glVertex3f(caster.getBaseX0(), topY, caster.getBaseZ0());
			gl.glVertex3f(caster.getBaseX1(), baseY, caster.getBaseZ1());
			gl.glVertex3f(caster.getBaseX1(), topY, caster.getBaseZ1());
			gl.glEnd();
		}

		private void drawRemasterShadowCasterTopEdge(
			Renderer3DWorldChunkFrame.ShadowCaster caster,
			float yOffset) throws Exception {
			float y = caster.getBaseY() + caster.getHeight() + yOffset;
			gl.glBegin(gl.GL_LINES);
			gl.glVertex3f(caster.getBaseX0(), y, caster.getBaseZ0());
			gl.glVertex3f(caster.getBaseX1(), y, caster.getBaseZ1());
			gl.glEnd();
		}

		private void drawChunkVertex(Renderer3DWorldChunkFrame.ChunkMesh chunk, int vertex) throws Exception {
			int coord = vertex * POSITION_COMPONENT_COUNT;
			gl.glVertex3f(
				chunk.getVertexCoord(coord),
				chunk.getVertexCoord(coord + 1),
				chunk.getVertexCoord(coord + 2));
		}

		private void drawRemasterTerrainShadowMask(Renderer3DFrame frame) throws Exception {
			Renderer3DWorldChunkFrame chunkFrame = frame == null ? null : frame.getWorldChunkFrame();
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
				return;
			}
			RemasterShadowRoofCoverage roofCoverage = RemasterShadowRoofCoverage.from(chunkFrame);
			List<RemasterTerrainShadowCaster> casters =
				buildRemasterTerrainShadowCasters(chunkFrame, roofCoverage);
			if (casters.isEmpty()) {
				return;
			}
			int stripCasterCount = countRemasterShadowMaskCasters(casters, RemasterTerrainShadowCaster.STYLE_STRIP);
			int softSceneryCasterCount =
				countRemasterShadowMaskCasters(casters, RemasterTerrainShadowCaster.STYLE_SOFT_SCENERY);
			long buildStart = RenderTelemetry.now();
			RemasterTerrainShadowMask shadowMask =
				buildRemasterTerrainShadowMaskTexture(roofCoverage, chunkFrame, casters);
			long buildNanos = RenderTelemetry.elapsedSince(buildStart);
			if (shadowMask == null) {
				return;
			}
			long uploadStart = RenderTelemetry.now();
			uploadRemasterTerrainShadowMask(shadowMask);
			long uploadNanos = RenderTelemetry.elapsedSince(uploadStart);
			RenderTelemetry.recordOpenGLRemasterShadowMask(
				shadowMask.width,
				shadowMask.height,
				shadowMask.visiblePixels,
				remasterShadowMaskLastCacheHit,
				remasterShadowMaskLastRebuild,
				remasterShadowMaskLastUpload,
				remasterShadowMaskLastUploadSkip,
				stripCasterCount,
				softSceneryCasterCount,
				buildNanos,
				uploadNanos);

			loadWorldProjectionAndView(frame);
			gl.glUseProgram(0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnable(gl.GL_TEXTURE_2D);
			gl.glBindTexture(gl.GL_TEXTURE_2D, remasterShadowMaskTextureId);
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glDisable(gl.GL_FOG);
			gl.glDisable(gl.GL_CULL_FACE);
			gl.glEnable(gl.GL_BLEND);
			gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(-4.0f, -4.0f);
			gl.glDepthMask(false);
			try {
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				gl.glBegin(gl.GL_TRIANGLES);
				for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
					drawRemasterTerrainShadowMaskChunk(roofCoverage, shadowMask, chunk);
				}
				gl.glEnd();
			} finally {
				gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
				gl.glDepthMask(true);
				gl.glDisable(gl.GL_DEPTH_TEST);
				gl.glDisable(gl.GL_BLEND);
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glDisable(gl.GL_FOG);
				gl.glDisable(gl.GL_CULL_FACE);
				gl.glBindTexture(gl.GL_TEXTURE_2D, 0);
				gl.glEnable(gl.GL_TEXTURE_2D);
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

		private void drawRemasterTerrainShadowMaskChunk(
			RemasterShadowRoofCoverage roofCoverage,
			RemasterTerrainShadowMask shadowMask,
			Renderer3DWorldChunkFrame.ChunkMesh chunk) throws Exception {
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				if (roofClassificationForTriangle(roofCoverage, chunk, triangle) != 0) {
					continue;
				}
				int indexBase = triangle * 3;
				int vertex0 = chunk.getIndex(indexBase);
				int vertex1 = chunk.getIndex(indexBase + 1);
				int vertex2 = chunk.getIndex(indexBase + 2);
				drawRemasterTerrainShadowMaskVertex(shadowMask, chunk, vertex0);
				drawRemasterTerrainShadowMaskVertex(shadowMask, chunk, vertex1);
				drawRemasterTerrainShadowMaskVertex(shadowMask, chunk, vertex2);
			}
		}

		private void drawRemasterTerrainShadowMaskVertex(
			RemasterTerrainShadowMask shadowMask,
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int vertex) throws Exception {
			int coord = vertex * POSITION_COMPONENT_COUNT;
			float x = chunk.getVertexCoord(coord);
			float z = chunk.getVertexCoord(coord + 2);
			gl.glTexCoord2f(shadowMask.u(x), shadowMask.v(z));
			gl.glVertex3f(x, chunk.getVertexCoord(coord + 1), z);
		}

		private RemasterTerrainShadowMask buildRemasterTerrainShadowMaskTexture(
			RemasterShadowRoofCoverage roofCoverage,
			Renderer3DWorldChunkFrame chunkFrame,
			List<RemasterTerrainShadowCaster> casters) {
			RemasterShadowMaskBounds bounds = RemasterShadowMaskBounds.from(roofCoverage, chunkFrame);
			if (bounds == null) {
				return null;
			}
			bounds = bounds.withPadding(REMASTER_SHADOW_MASK_TEXTURE_PADDING);
			long signature = remasterTerrainShadowMaskSignature(casters, bounds);
			if (remasterShadowMaskCache != null && remasterShadowMaskCacheSignature == signature) {
				remasterShadowMaskLastCacheHit = true;
				remasterShadowMaskLastRebuild = false;
				return remasterShadowMaskCache;
			}
			remasterShadowMaskLastCacheHit = false;
			remasterShadowMaskLastRebuild = true;
			int width = REMASTER_SHADOW_MASK_TEXTURE_SIZE;
			int height = REMASTER_SHADOW_MASK_TEXTURE_SIZE;
			float[] sourceAlpha = new float[width * height];
			float[] horizontalAlpha = new float[width * height];
			float[] blurredAlpha = new float[width * height];
			Map<Long, List<RemasterTerrainShadowCaster>> casterGrid =
				buildRemasterTerrainShadowCasterGrid(casters);
			for (int y = 0; y < height; y++) {
				float z = bounds.zAt(y, height);
				int row = y * width;
				for (int x = 0; x < width; x++) {
					sourceAlpha[row + x] =
						remasterTerrainShadowMaskAlpha(roofCoverage, casterGrid, bounds.xAt(x, width), z);
				}
			}
			blurHorizontal(sourceAlpha, horizontalAlpha, width, height, REMASTER_SHADOW_MASK_BLUR_RADIUS);
			blurVertical(horizontalAlpha, blurredAlpha, width, height, REMASTER_SHADOW_MASK_BLUR_RADIUS);
			ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
			int visiblePixels = 0;
			for (int index = 0; index < sourceAlpha.length; index++) {
				float alpha = Math.max(
					sourceAlpha[index] * REMASTER_SHADOW_MASK_CENTER_RETAIN,
					blurredAlpha[index] * REMASTER_SHADOW_MASK_BLUR_BOOST);
				alpha = clampStatic(alpha, 0.0f, REMASTER_SHADOW_MASK_MAX_ALPHA);
				if (alpha > REMASTER_SHADOW_MASK_MIN_DRAW_ALPHA) {
					visiblePixels++;
				}
				pixels.put((byte) 0);
				pixels.put((byte) 0);
				pixels.put((byte) 0);
				pixels.put((byte) Math.round(alpha * 255.0f));
			}
			if (visiblePixels <= 0) {
				return null;
			}
			pixels.flip();
			remasterShadowMaskCache = new RemasterTerrainShadowMask(
				signature,
				width,
				height,
				visiblePixels,
				bounds.minX,
				bounds.minZ,
				bounds.invSpanX(),
				bounds.invSpanZ(),
				pixels);
			remasterShadowMaskCacheSignature = signature;
			return remasterShadowMaskCache;
		}

		private void uploadRemasterTerrainShadowMask(RemasterTerrainShadowMask shadowMask) throws Exception {
			if (remasterShadowMaskTextureId == 0) {
				remasterShadowMaskTextureId = gl.glGenTextures();
				gl.glBindTexture(gl.GL_TEXTURE_2D, remasterShadowMaskTextureId);
				gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_LINEAR);
				gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_LINEAR);
				gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP_TO_EDGE);
				gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP_TO_EDGE);
			} else {
				gl.glBindTexture(gl.GL_TEXTURE_2D, remasterShadowMaskTextureId);
			}
			if (remasterShadowMaskUploadedSignature == shadowMask.signature
				&& remasterShadowMaskTextureWidth == shadowMask.width
				&& remasterShadowMaskTextureHeight == shadowMask.height) {
				remasterShadowMaskLastUpload = false;
				remasterShadowMaskLastUploadSkip = true;
				return;
			}
			remasterShadowMaskLastUpload = true;
			remasterShadowMaskLastUploadSkip = false;
			if (remasterShadowMaskTextureWidth != shadowMask.width
				|| remasterShadowMaskTextureHeight != shadowMask.height) {
				gl.glTexImage2D(
					gl.GL_TEXTURE_2D,
					0,
					gl.GL_RGBA,
					shadowMask.width,
					shadowMask.height,
					0,
					gl.GL_RGBA,
					gl.GL_UNSIGNED_BYTE,
					shadowMask.pixels());
				remasterShadowMaskTextureWidth = shadowMask.width;
				remasterShadowMaskTextureHeight = shadowMask.height;
			} else {
				gl.glTexSubImage2D(
					gl.GL_TEXTURE_2D,
					0,
					0,
					0,
					shadowMask.width,
					shadowMask.height,
					gl.GL_RGBA,
					gl.GL_UNSIGNED_BYTE,
					shadowMask.pixels());
			}
			remasterShadowMaskUploadedSignature = shadowMask.signature;
		}

		private long remasterTerrainShadowMaskSignature(
			List<RemasterTerrainShadowCaster> casters,
			RemasterShadowMaskBounds bounds) {
			long hash = 0xcbf29ce484222325L;
			hash = shadowProofMix(hash, REMASTER_SHADOW_MASK_TEXTURE_SIZE);
			hash = shadowProofMix(hash, REMASTER_SHADOW_MASK_BLUR_RADIUS);
			hash = shadowProofMix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_TEXTURE_PADDING));
			hash = shadowProofMix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_CENTER_RETAIN));
			hash = shadowProofMix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_BLUR_BOOST));
			hash = shadowProofMix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_CLIP_START_OFFSET));
			hash = shadowProofMix(hash, REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP ? 1 : 0);
			hash = shadowProofMix(hash, Float.floatToIntBits(bounds.minX));
			hash = shadowProofMix(hash, Float.floatToIntBits(bounds.maxX));
			hash = shadowProofMix(hash, Float.floatToIntBits(bounds.minZ));
			hash = shadowProofMix(hash, Float.floatToIntBits(bounds.maxZ));
			hash = shadowProofMix(hash, casters == null ? 0 : casters.size());
			if (casters != null) {
				for (RemasterTerrainShadowCaster caster : casters) {
					hash = caster.mixSignature(hash);
				}
			}
			return hash;
		}

		private float remasterTerrainShadowMaskAlpha(
			RemasterShadowRoofCoverage roofCoverage,
			Map<Long, List<RemasterTerrainShadowCaster>> casterGrid,
			float x,
			float z) {
			List<RemasterTerrainShadowCaster> casters =
				casterGrid.get(remasterShadowMaskCellKey(remasterShadowMaskCell(x), remasterShadowMaskCell(z)));
			if (casters == null || casters.isEmpty()) {
				return 0.0f;
			}
			float alpha = 0.0f;
			for (RemasterTerrainShadowCaster caster : casters) {
				float casterAlpha = caster.alphaAt(x, z);
				if (casterAlpha <= 0.0f || caster.isBlockedBy(roofCoverage, x, z)) {
					continue;
				}
				alpha = Math.max(alpha, casterAlpha);
			}
			return clampStatic(alpha, 0.0f, REMASTER_SHADOW_MASK_MAX_ALPHA);
		}

		private static void blurHorizontal(float[] source, float[] target, int width, int height, int radius) {
			if (radius <= 0) {
				System.arraycopy(source, 0, target, 0, source.length);
				return;
			}
			for (int y = 0; y < height; y++) {
				int row = y * width;
				for (int x = 0; x < width; x++) {
					float total = 0.0f;
					int count = 0;
					for (int offset = -radius; offset <= radius; offset++) {
						int sampleX = x + offset;
						if (sampleX < 0 || sampleX >= width) {
							continue;
						}
						total += source[row + sampleX];
						count++;
					}
					target[row + x] = count <= 0 ? 0.0f : total / count;
				}
			}
		}

		private static void blurVertical(float[] source, float[] target, int width, int height, int radius) {
			if (radius <= 0) {
				System.arraycopy(source, 0, target, 0, source.length);
				return;
			}
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					float total = 0.0f;
					int count = 0;
					for (int offset = -radius; offset <= radius; offset++) {
						int sampleY = y + offset;
						if (sampleY < 0 || sampleY >= height) {
							continue;
						}
						total += source[sampleY * width + x];
						count++;
					}
					target[y * width + x] = count <= 0 ? 0.0f : total / count;
				}
			}
		}

		private List<RemasterTerrainShadowCaster> buildRemasterTerrainShadowCasters(
			Renderer3DWorldChunkFrame chunkFrame,
			RemasterShadowRoofCoverage roofCoverage) {
			List<RemasterTerrainShadowCaster> casters = new ArrayList<RemasterTerrainShadowCaster>();
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int casterCount = chunk.getShadowCasterCount();
				for (int index = 0; index < casterCount; index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (!isRemasterShadowEligibleCaster(caster)) {
						continue;
					}
					if (roofClassificationForCaster(roofCoverage, chunk, caster) != 0) {
						continue;
					}
					RemasterTerrainShadowCaster projected = RemasterTerrainShadowCaster.from(caster, chunk.getPlane());
					if (projected != null) {
						casters.add(projected);
					}
				}
			}
			return casters;
		}

		private int countRemasterShadowMaskCasters(List<RemasterTerrainShadowCaster> casters, int style) {
			int count = 0;
			for (RemasterTerrainShadowCaster caster : casters) {
				if (caster.style == style) {
					count++;
				}
			}
			return count;
		}

		private Map<Long, List<RemasterTerrainShadowCaster>> buildRemasterTerrainShadowCasterGrid(
			List<RemasterTerrainShadowCaster> casters) {
			Map<Long, List<RemasterTerrainShadowCaster>> grid =
				new HashMap<Long, List<RemasterTerrainShadowCaster>>();
			for (RemasterTerrainShadowCaster caster : casters) {
				int minCellX = remasterShadowMaskCell(caster.minX);
				int maxCellX = remasterShadowMaskCell(caster.maxX);
				int minCellZ = remasterShadowMaskCell(caster.minZ);
				int maxCellZ = remasterShadowMaskCell(caster.maxZ);
				for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
					for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
						long key = remasterShadowMaskCellKey(cellX, cellZ);
						List<RemasterTerrainShadowCaster> cellCasters = grid.get(key);
						if (cellCasters == null) {
							cellCasters = new ArrayList<RemasterTerrainShadowCaster>();
							grid.put(key, cellCasters);
						}
						cellCasters.add(caster);
					}
				}
			}
			return grid;
		}

		private static int remasterShadowMaskCell(float value) {
			return (int) Math.floor(value / REMASTER_SHADOW_MASK_GRID_SIZE);
		}

		private static long remasterShadowMaskCellKey(int cellX, int cellZ) {
			return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
		}

		private ResidentChunkReadiness inspectDrawableResidentStaticWorld(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame chunkFrame,
			boolean textured) {
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0 || residentChunks.isEmpty()) {
				return ResidentChunkReadiness.notRequested("no-resident-chunks");
			}
			long shadowProofSignature = currentShadowProofSignature(chunkFrame);
			boolean matchedBuffer = false;
			int drawableTerrainBatches = 0;
			int drawableWallBatches = 0;
			int drawableRoofBatches = 0;
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				WorldChunkBufferKey key = WorldChunkBufferKey.from(chunk);
				WorldChunkBuffer buffer = residentChunks.get(key);
				if (buffer == null || !buffer.matches(
					chunk.getSignature(),
					chunk.getVertexCount(),
					chunk.getIndexCount(),
					chunk.getTriangleCount(),
					textured,
					currentBrightnessBits(),
					currentFogModeBits(),
					currentLightingModeBits(),
					currentGeometryModeBits(),
					WORLD_CHUNKS_REPLACEMENT_COMPOSITE,
					shadowProofSignature)) {
					continue;
				}
				matchedBuffer = true;
				for (WorldChunkMaterialBatch batch : buffer.materialBatches) {
					if (batch.indexCount <= 0 || !shouldDrawChunkModelKind(batch.kind)) {
						continue;
					}
					if (!isDrawableResidentStaticBatch(frame, batch, textured)) {
						continue;
					}
					if (batch.kind == Renderer3DModelKind.TERRAIN) {
						drawableTerrainBatches++;
					} else if (batch.kind == Renderer3DModelKind.WALL) {
						drawableWallBatches++;
					} else if (batch.kind == Renderer3DModelKind.ROOF) {
						drawableRoofBatches++;
					}
				}
			}
			if (!matchedBuffer) {
				return ResidentChunkReadiness.notRequested("no-matching-buffers");
			}
			if (drawableTerrainBatches <= 0) {
				return new ResidentChunkReadiness(
					false,
					"no-drawable-terrain",
					drawableTerrainBatches,
					drawableWallBatches,
					drawableRoofBatches);
			}
			return new ResidentChunkReadiness(
				true,
				"active",
				drawableTerrainBatches,
				drawableWallBatches,
				drawableRoofBatches);
		}

		private boolean isDrawableResidentStaticBatch(
			Renderer3DFrame frame,
			WorldChunkMaterialBatch batch,
			boolean textured) {
			return !textured
				|| textureRegionForBatch(frame, batch) != null
				|| fallbackColorForBatch(frame, batch) != NO_FALLBACK_COLOR;
		}

		private void drawChunkDiagnosticLayers(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame chunkFrame,
			boolean textured,
			boolean transparentPass,
			WorldChunkDrawAccumulator accumulator,
			float[] batchCullViewMatrix,
			boolean shaderActive,
			FloatBuffer residentWorldToClipMatrix,
			FloatBuffer residentWorldViewMatrix) throws Exception {
			for (Renderer3DModelKind kind : WORLD_CHUNK_KIND_DRAW_ORDER) {
				drawChunkDiagnosticPass(
					frame,
					chunkFrame,
					textured,
					transparentPass,
					kind,
					accumulator,
					batchCullViewMatrix,
					shaderActive,
					residentWorldToClipMatrix,
					residentWorldViewMatrix);
			}
		}

		private boolean shouldDrawProjectedShadowProof() {
			RendererLightingSettings.Mode lightingMode = RendererLightingSettings.getMode();
			return WORLD_CHUNKS_SHADOW_PROOF && lightingMode == RendererLightingSettings.Mode.DIRECTIONAL;
		}

		private long currentShadowProofSignature(Renderer3DWorldChunkFrame chunkFrame) {
			if (!shouldDrawProjectedShadowProof()) {
				return 0L;
			}
			return shadowProofCasterSignature(buildProjectedShadowProofCasters(chunkFrame));
		}

		private long shadowProofCasterSignature(List<ShadowProofCaster> casters) {
			long hash = 0xcbf29ce484222325L;
			if (casters == null || casters.isEmpty()) {
				return hash;
			}
			for (ShadowProofCaster caster : casters) {
				hash = shadowProofMix(hash, (int) caster.key);
				hash = shadowProofMix(hash, (int) (caster.key >>> 32));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.baseX0));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.baseZ0));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.baseX1));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.baseZ1));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.centerX));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.centerZ));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.fallbackY));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.height));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.length));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.halfWidth));
				hash = shadowProofMix(hash, Float.floatToIntBits(caster.opacity));
			}
			return hash;
		}

		private static long shadowProofMix(long hash, int value) {
			hash ^= value & 0xffffffffL;
			return hash * 0x100000001b3L;
		}

		private static float remasterShadowMaskLightAzimuthDegrees() {
			return quantize(
				RendererRemasterLightSettings.getAzimuthDegrees(),
				REMASTER_SHADOW_MASK_AZIMUTH_BUCKET_DEGREES);
		}

		private static float remasterShadowMaskLightElevationDegrees() {
			return clampStatic(
				quantize(
					RendererRemasterLightSettings.getElevationDegrees(),
					REMASTER_SHADOW_MASK_ELEVATION_BUCKET_DEGREES),
				5.0f,
				85.0f);
		}

		private static float quantize(float value, float bucketSize) {
			if (bucketSize <= 0.0f) {
				return value;
			}
			return Math.round(value / bucketSize) * bucketSize;
		}

		private List<ShadowProofCaster> buildProjectedShadowProofCasters(Renderer3DWorldChunkFrame chunkFrame) {
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
				return Collections.emptyList();
			}
			Map<Long, ShadowProofCaster> castersByKey = new LinkedHashMap<Long, ShadowProofCaster>();
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				addProjectedShadowProofCasters(castersByKey, chunk);
			}
			return new ArrayList<ShadowProofCaster>(castersByKey.values());
		}

		private List<ShadowProofCaster> buildProjectedShadowProofCasters(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
			Map<Long, ShadowProofCaster> castersByKey = new LinkedHashMap<Long, ShadowProofCaster>();
			addProjectedShadowProofCasters(castersByKey, chunk);
			return new ArrayList<ShadowProofCaster>(castersByKey.values());
		}

		private void addProjectedShadowProofCasters(
			Map<Long, ShadowProofCaster> castersByKey,
			Renderer3DWorldChunkFrame.ChunkMesh chunk) {
			if (castersByKey == null || chunk == null) {
				return;
			}
			if (chunk.getShadowCasterCount() > 0) {
				for (int index = 0; index < chunk.getShadowCasterCount(); index++) {
					Renderer3DWorldChunkFrame.ShadowCaster source = chunk.getShadowCaster(index);
					if (!shouldCastProjectedShadowProof(source.getModelKind())) {
						continue;
					}
					ShadowProofCaster caster = ShadowProofCaster.from(source);
					if (caster == null) {
						continue;
					}
					addProjectedShadowProofCaster(castersByKey, caster);
				}
				return;
			}

			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!shouldCastProjectedShadowProof(chunk.getTriangleModelKind(triangle))) {
					continue;
				}
				ShadowProofCaster caster = ShadowProofCaster.from(chunk, triangle);
				if (caster == null) {
					continue;
				}
				addProjectedShadowProofCaster(castersByKey, caster);
			}
		}

		private void addProjectedShadowProofCaster(
			Map<Long, ShadowProofCaster> castersByKey,
			ShadowProofCaster caster) {
			ShadowProofCaster current = castersByKey.get(caster.key);
			if (current == null || caster.height > current.height) {
				castersByKey.put(caster.key, caster);
			}
		}

		private boolean shouldCastProjectedShadowProof(Renderer3DModelKind kind) {
			if (kind == null) {
				return false;
			}
			return kind == Renderer3DModelKind.WALL
				|| kind == Renderer3DModelKind.GAME_OBJECT
				|| kind == Renderer3DModelKind.WALL_OBJECT;
		}

		private float projectedShadowProofAlpha(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int triangle,
			List<ShadowProofCaster> shadowProofCasters) {
			int sourceIndex = triangle * 3;
			float x = 0.0f;
			float z = 0.0f;
			for (int index = 0; index < 3; index++) {
				int vertex = chunk.getIndex(sourceIndex + index);
				int coord = vertex * POSITION_COMPONENT_COUNT;
				x += chunk.getVertexCoord(coord);
				z += chunk.getVertexCoord(coord + 2);
			}
			x /= 3.0f;
			z /= 3.0f;
			float baseAlpha = SHADOW_PROOF_DIRECTIONAL_ALPHA;
			float alpha = 0.0f;
			for (ShadowProofCaster caster : shadowProofCasters) {
				alpha += caster.alphaAt(x, z, baseAlpha);
				if (alpha >= SHADOW_PROOF_MAX_ALPHA) {
					return SHADOW_PROOF_MAX_ALPHA;
				}
			}
			return alpha;
		}

		private static final class ShadowProofCaster {
			private final long key;
			private final float baseX0;
			private final float baseZ0;
			private final float baseX1;
			private final float baseZ1;
			private final float centerX;
			private final float centerZ;
			private final float fallbackY;
			private final float height;
			private final float length;
			private final float halfWidth;
			private final float edgeX;
			private final float edgeZ;
			private final float edgeLength;
			private final float directionX;
			private final float directionZ;
			private final float opacity;

			private ShadowProofCaster(
				long key,
				float baseX0,
				float baseZ0,
				float baseX1,
				float baseZ1,
				float fallbackY,
				float height,
				float length,
				float halfWidth,
				float opacity) {
				float edgeDx = baseX1 - baseX0;
				float edgeDz = baseZ1 - baseZ0;
				float edgeLength = (float) Math.sqrt(edgeDx * edgeDx + edgeDz * edgeDz);
				float directionLength = (float) Math.sqrt(
					SHADOW_PROOF_DIRECTION_X * SHADOW_PROOF_DIRECTION_X
						+ SHADOW_PROOF_DIRECTION_Z * SHADOW_PROOF_DIRECTION_Z);
				this.key = key;
				this.baseX0 = baseX0;
				this.baseZ0 = baseZ0;
				this.baseX1 = baseX1;
				this.baseZ1 = baseZ1;
				this.centerX = (baseX0 + baseX1) * 0.5f;
				this.centerZ = (baseZ0 + baseZ1) * 0.5f;
				this.fallbackY = fallbackY;
				this.height = height;
				this.length = length;
				this.halfWidth = halfWidth;
				this.edgeLength = edgeLength;
				this.edgeX = edgeDx / Math.max(0.0001f, edgeLength);
				this.edgeZ = edgeDz / Math.max(0.0001f, edgeLength);
				this.directionX = SHADOW_PROOF_DIRECTION_X / Math.max(0.0001f, directionLength);
				this.directionZ = SHADOW_PROOF_DIRECTION_Z / Math.max(0.0001f, directionLength);
				this.opacity = clampStatic(opacity, 0.0f, 1.0f);
			}

			private float alphaAt(float x, float z, float baseAlpha) {
				float determinant = edgeX * directionZ - edgeZ * directionX;
				if (edgeLength > 0.0001f && Math.abs(determinant) > 0.08f) {
					float px = x - baseX0;
					float pz = z - baseZ0;
					float edgeAlong = (px * directionZ - pz * directionX) / determinant;
					float shadowAlong = (edgeX * pz - edgeZ * px) / determinant;
					if (shadowAlong < 0.0f || shadowAlong > length) {
						return 0.0f;
					}
					if (edgeAlong < -halfWidth || edgeAlong > edgeLength + halfWidth) {
						return 0.0f;
					}
					float sideFade = Math.min(
						(edgeAlong + halfWidth) / Math.max(16.0f, halfWidth),
						(edgeLength + halfWidth - edgeAlong) / Math.max(16.0f, halfWidth));
					float endFade = (length - shadowAlong) / Math.max(64.0f, length * 0.22f);
					return baseAlpha * opacity * clampStatic(Math.min(sideFade, endFade), 0.0f, 1.0f);
				}
				return alphaAtCenterFallback(x, z, baseAlpha);
			}

			private float alphaAtCenterFallback(float x, float z, float baseAlpha) {
				float dx = x - centerX;
				float dz = z - centerZ;
				float along = dx * directionX + dz * directionZ;
				if (along < 0.0f || along > length) {
					return 0.0f;
				}
				float across = Math.abs(dx * -directionZ + dz * directionX);
				if (across > halfWidth) {
					return 0.0f;
				}
				float sideFade = (halfWidth - across) / Math.max(16.0f, halfWidth * 0.25f);
				float endFade = (length - along) / Math.max(64.0f, length * 0.22f);
				return baseAlpha * opacity * clampStatic(Math.min(sideFade, endFade), 0.0f, 1.0f);
			}

			private static ShadowProofCaster from(Renderer3DWorldChunkFrame.ChunkMesh chunk, int triangle) {
				int sourceIndex = triangle * 3;
				int minX = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				int minY = Integer.MAX_VALUE;
				int maxY = Integer.MIN_VALUE;
				int minZ = Integer.MAX_VALUE;
				int maxZ = Integer.MIN_VALUE;
				for (int index = 0; index < 3; index++) {
					int vertex = chunk.getIndex(sourceIndex + index);
					int coord = vertex * POSITION_COMPONENT_COUNT;
					int x = chunk.getVertexCoord(coord);
					int y = chunk.getVertexCoord(coord + 1);
					int z = chunk.getVertexCoord(coord + 2);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					minZ = Math.min(minZ, z);
					maxZ = Math.max(maxZ, z);
				}
				int spanX = maxX - minX;
				int spanZ = maxZ - minZ;
				if (spanX > SHADOW_PROOF_DIAGONAL_MIN_SPAN && spanZ > SHADOW_PROOF_DIAGONAL_MIN_SPAN) {
					return null;
				}
				float height = Math.max(0.0f, maxY - minY);
				float length = clampStatic(
					SHADOW_PROOF_MIN_LENGTH + height * 1.25f,
					SHADOW_PROOF_MIN_LENGTH,
					SHADOW_PROOF_MAX_LENGTH);
				float width = Math.max(spanX, spanZ);
				float centerX = (minX + maxX) * 0.5f;
				float centerZ = (minZ + maxZ) * 0.5f;
				return new ShadowProofCaster(
					casterKey(spanX >= spanZ, centerX, centerZ, width),
					spanX >= spanZ ? minX : centerX,
					spanX >= spanZ ? centerZ : minZ,
					spanX >= spanZ ? maxX : centerX,
					spanX >= spanZ ? centerZ : maxZ,
					minY,
					height,
					length,
					Math.max(SHADOW_PROOF_MIN_WIDTH, width * 0.55f),
					0.75f);
			}

			private static ShadowProofCaster from(Renderer3DWorldChunkFrame.ShadowCaster source) {
				int spanX = Math.abs(source.getBaseX1() - source.getBaseX0());
				int spanZ = Math.abs(source.getBaseZ1() - source.getBaseZ0());
				if (spanX > SHADOW_PROOF_DIAGONAL_MIN_SPAN && spanZ > SHADOW_PROOF_DIAGONAL_MIN_SPAN) {
					return null;
				}
				float height = Math.max(0.0f, source.getHeight());
				float length = clampStatic(
					SHADOW_PROOF_MIN_LENGTH + height * 3.0f,
					SHADOW_PROOF_MIN_LENGTH,
					SHADOW_PROOF_MAX_LENGTH);
				float width = Math.max(source.getWidth(), Math.max(spanX, spanZ));
				float centerX = (source.getBaseX0() + source.getBaseX1()) * 0.5f;
				float centerZ = (source.getBaseZ0() + source.getBaseZ1()) * 0.5f;
				float baseX0 = source.getBaseX0();
				float baseZ0 = source.getBaseZ0();
				float baseX1 = source.getBaseX1();
				float baseZ1 = source.getBaseZ1();
				if (source.getModelKind() == Renderer3DModelKind.GAME_OBJECT) {
					float directionLength = (float) Math.sqrt(
						SHADOW_PROOF_DIRECTION_X * SHADOW_PROOF_DIRECTION_X
							+ SHADOW_PROOF_DIRECTION_Z * SHADOW_PROOF_DIRECTION_Z);
					float normalX = -SHADOW_PROOF_DIRECTION_Z / Math.max(0.0001f, directionLength);
					float normalZ = SHADOW_PROOF_DIRECTION_X / Math.max(0.0001f, directionLength);
					float halfWidth = Math.max(SHADOW_PROOF_MIN_WIDTH, width * 0.5f);
					baseX0 = centerX - normalX * halfWidth;
					baseZ0 = centerZ - normalZ * halfWidth;
					baseX1 = centerX + normalX * halfWidth;
					baseZ1 = centerZ + normalZ * halfWidth;
				}
				return new ShadowProofCaster(
					casterKey(spanX >= spanZ, centerX, centerZ, width),
					baseX0,
					baseZ0,
					baseX1,
					baseZ1,
					source.getBaseY(),
					height,
					length,
					Math.max(SHADOW_PROOF_MIN_WIDTH, width * 0.55f),
					source.getOpacity() / 255.0f);
			}

			private static long casterKey(boolean xMajor, float centerX, float centerZ, float width) {
				float along = xMajor ? centerX : centerZ;
				float across = xMajor ? centerZ : centerX;
				long result = xMajor ? 1L : 2L;
				result = result * 31L + Math.round(along / 128.0f);
				result = result * 31L + Math.round(across / 64.0f);
				result = result * 31L + Math.round(width / 128.0f);
				return result;
			}
		}

		private static final class RemasterTerrainShadowMask {
			private final long signature;
			private final int width;
			private final int height;
			private final int visiblePixels;
			private final float minX;
			private final float minZ;
			private final float invSpanX;
			private final float invSpanZ;
			private final ByteBuffer pixels;

			private RemasterTerrainShadowMask(
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

			private float u(float x) {
				return clampStatic((x - minX) * invSpanX, 0.0f, 1.0f);
			}

			private float v(float z) {
				return clampStatic((z - minZ) * invSpanZ, 0.0f, 1.0f);
			}

			private ByteBuffer pixels() {
				ByteBuffer duplicate = pixels.duplicate();
				duplicate.position(0);
				return duplicate;
			}
		}

		private static final class RemasterShadowMaskBounds {
			private final float minX;
			private final float maxX;
			private final float minZ;
			private final float maxZ;

			private RemasterShadowMaskBounds(float minX, float maxX, float minZ, float maxZ) {
				this.minX = minX;
				this.maxX = maxX;
				this.minZ = minZ;
				this.maxZ = maxZ;
			}

			private static RemasterShadowMaskBounds from(
				RemasterShadowRoofCoverage roofCoverage,
				Renderer3DWorldChunkFrame chunkFrame) {
				float minX = Float.POSITIVE_INFINITY;
				float maxX = Float.NEGATIVE_INFINITY;
				float minZ = Float.POSITIVE_INFINITY;
				float maxZ = Float.NEGATIVE_INFINITY;
				for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
					int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
					for (int triangle = 0; triangle < triangleCount; triangle++) {
						if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
							continue;
						}
						if (roofClassificationForTriangle(roofCoverage, chunk, triangle) != 0) {
							continue;
						}
						int sourceIndex = triangle * 3;
						for (int corner = 0; corner < 3; corner++) {
							int vertex = chunk.getIndex(sourceIndex + corner);
							int coord = vertex * POSITION_COMPONENT_COUNT;
							float x = chunk.getVertexCoord(coord);
							float z = chunk.getVertexCoord(coord + 2);
							minX = Math.min(minX, x);
							maxX = Math.max(maxX, x);
							minZ = Math.min(minZ, z);
							maxZ = Math.max(maxZ, z);
						}
					}
				}
				if (!Float.isFinite(minX)
					|| !Float.isFinite(maxX)
					|| !Float.isFinite(minZ)
					|| !Float.isFinite(maxZ)
					|| maxX <= minX
					|| maxZ <= minZ) {
					return null;
				}
				return new RemasterShadowMaskBounds(minX, maxX, minZ, maxZ);
			}

			private RemasterShadowMaskBounds withPadding(float padding) {
				return new RemasterShadowMaskBounds(
					minX - padding,
					maxX + padding,
					minZ - padding,
					maxZ + padding);
			}

			private float xAt(int pixelX, int width) {
				return minX + ((pixelX + 0.5f) / Math.max(1.0f, (float) width)) * spanX();
			}

			private float zAt(int pixelY, int height) {
				return minZ + ((pixelY + 0.5f) / Math.max(1.0f, (float) height)) * spanZ();
			}

			private float invSpanX() {
				return 1.0f / Math.max(1.0f, spanX());
			}

			private float invSpanZ() {
				return 1.0f / Math.max(1.0f, spanZ());
			}

			private float spanX() {
				return maxX - minX;
			}

			private float spanZ() {
				return maxZ - minZ;
			}
		}

		private static final class RemasterTerrainShadowCaster {
			private static final int STYLE_STRIP = 0;
			private static final int STYLE_SOFT_SCENERY = 1;

			private final int style;
			private final int plane;
			private final float baseX0;
			private final float baseZ0;
			private final float baseX1;
			private final float baseZ1;
			private final float centerX;
			private final float centerZ;
			private final float length;
			private final float halfWidth;
			private final float edgeX;
			private final float edgeZ;
			private final float edgeLength;
			private final float directionX;
			private final float directionZ;
			private final float opacity;
			private final float minX;
			private final float maxX;
			private final float minZ;
			private final float maxZ;

			private RemasterTerrainShadowCaster(
				int style,
				int plane,
				float baseX0,
				float baseZ0,
				float baseX1,
				float baseZ1,
				float length,
				float halfWidth,
				float directionX,
				float directionZ,
				float opacity) {
				float edgeDx = baseX1 - baseX0;
				float edgeDz = baseZ1 - baseZ0;
				float edgeLength = (float) Math.sqrt(edgeDx * edgeDx + edgeDz * edgeDz);
				this.style = style;
				this.plane = plane;
				this.baseX0 = baseX0;
				this.baseZ0 = baseZ0;
				this.baseX1 = baseX1;
				this.baseZ1 = baseZ1;
				this.centerX = (baseX0 + baseX1) * 0.5f;
				this.centerZ = (baseZ0 + baseZ1) * 0.5f;
				this.length = length;
				this.halfWidth = halfWidth;
				this.edgeLength = edgeLength;
				this.edgeX = edgeDx / Math.max(0.0001f, edgeLength);
				this.edgeZ = edgeDz / Math.max(0.0001f, edgeLength);
				this.directionX = directionX;
				this.directionZ = directionZ;
				this.opacity = clampStatic(opacity, 0.0f, 1.0f);
				float projectedX0 = baseX0 + directionX * length;
				float projectedZ0 = baseZ0 + directionZ * length;
				float projectedX1 = baseX1 + directionX * length;
				float projectedZ1 = baseZ1 + directionZ * length;
				this.minX = Math.min(Math.min(baseX0, baseX1), Math.min(projectedX0, projectedX1)) - halfWidth;
				this.maxX = Math.max(Math.max(baseX0, baseX1), Math.max(projectedX0, projectedX1)) + halfWidth;
				this.minZ = Math.min(Math.min(baseZ0, baseZ1), Math.min(projectedZ0, projectedZ1)) - halfWidth;
				this.maxZ = Math.max(Math.max(baseZ0, baseZ1), Math.max(projectedZ0, projectedZ1)) + halfWidth;
			}

			private static RemasterTerrainShadowCaster from(
				Renderer3DWorldChunkFrame.ShadowCaster source,
				int plane) {
				if (source == null || source.getHeight() <= 0) {
					return null;
				}
				double azimuth = Math.toRadians(remasterShadowMaskLightAzimuthDegrees());
				double elevation = Math.toRadians(remasterShadowMaskLightElevationDegrees());
				float lightX = (float) (Math.cos(elevation) * Math.cos(azimuth));
				float lightY = Math.max(0.12f, Math.abs((float) Math.sin(elevation)));
				float lightZ = (float) (Math.cos(elevation) * Math.sin(azimuth));
				float horizontalLength = (float) Math.sqrt(lightX * lightX + lightZ * lightZ);
				if (horizontalLength <= 0.0001f) {
					return null;
				}
				float shadowDirectionX = -lightX / horizontalLength;
				float shadowDirectionZ = -lightZ / horizontalLength;
				float height = Math.max(0.0f, source.getHeight());
				float length = clampStatic(
					REMASTER_SHADOW_MASK_MIN_LENGTH + height * (horizontalLength / lightY) * 2.0f,
					REMASTER_SHADOW_MASK_MIN_LENGTH,
					REMASTER_SHADOW_MASK_MAX_LENGTH);
				float width = Math.max(
					source.getWidth(),
					Math.max(
						Math.abs(source.getBaseX1() - source.getBaseX0()),
						Math.abs(source.getBaseZ1() - source.getBaseZ0())));
				Renderer3DModelKind kind = source.getModelKind();
				int style = kind == Renderer3DModelKind.GAME_OBJECT ? STYLE_SOFT_SCENERY : STYLE_STRIP;
				float halfWidth = remasterShadowHalfWidth(kind, width);
				float baseX0 = source.getBaseX0();
				float baseZ0 = source.getBaseZ0();
				float baseX1 = source.getBaseX1();
				float baseZ1 = source.getBaseZ1();
				if (style == STYLE_SOFT_SCENERY) {
					float centerX = (source.getBaseX0() + source.getBaseX1()) * 0.5f;
					float centerZ = (source.getBaseZ0() + source.getBaseZ1()) * 0.5f;
					float normalX = -shadowDirectionZ;
					float normalZ = shadowDirectionX;
					halfWidth = Math.max(36.0f, Math.min(132.0f, width * 0.30f));
					baseX0 = centerX - normalX * halfWidth;
					baseZ0 = centerZ - normalZ * halfWidth;
					baseX1 = centerX + normalX * halfWidth;
					baseZ1 = centerZ + normalZ * halfWidth;
				}
				return new RemasterTerrainShadowCaster(
					style,
					plane,
					baseX0,
					baseZ0,
					baseX1,
					baseZ1,
					length,
					halfWidth,
					shadowDirectionX,
					shadowDirectionZ,
					source.getOpacity() / 255.0f);
			}

			private static float remasterShadowHalfWidth(Renderer3DModelKind kind, float width) {
				if (kind == Renderer3DModelKind.GAME_OBJECT) {
					return Math.max(36.0f, Math.min(132.0f, width * 0.30f));
				}
				if (kind == Renderer3DModelKind.WALL_OBJECT) {
					return Math.max(32.0f, width * 0.28f);
				}
				return Math.max(REMASTER_SHADOW_MASK_MIN_WIDTH, Math.min(96.0f, width * 0.18f));
			}

			private float alphaAt(float x, float z) {
				if (style == STYLE_SOFT_SCENERY) {
					return alphaAtSoftScenery(x, z);
				}
				float determinant = edgeX * directionZ - edgeZ * directionX;
				if (edgeLength > 0.0001f && Math.abs(determinant) > 0.08f) {
					float px = x - baseX0;
					float pz = z - baseZ0;
					float edgeAlong = (px * directionZ - pz * directionX) / determinant;
					float shadowAlong = (edgeX * pz - edgeZ * px) / determinant;
					if (shadowAlong < 0.0f || shadowAlong > length) {
						return 0.0f;
					}
					if (edgeAlong < -halfWidth || edgeAlong > edgeLength + halfWidth) {
						return 0.0f;
					}
					float sideFade = Math.min(
						(edgeAlong + halfWidth) / Math.max(16.0f, halfWidth),
						(edgeLength + halfWidth - edgeAlong) / Math.max(16.0f, halfWidth));
					float endFade = (length - shadowAlong) / Math.max(64.0f, length * 0.22f);
					return REMASTER_SHADOW_MASK_BASE_ALPHA
						* opacity
						* clampStatic(Math.min(sideFade, endFade), 0.0f, 1.0f);
				}
				return alphaAtCenterFallback(x, z);
			}

			private float alphaAtSoftScenery(float x, float z) {
				float dx = x - centerX;
				float dz = z - centerZ;
				float along = dx * directionX + dz * directionZ;
				if (along < -halfWidth * 0.45f || along > length) {
					return 0.0f;
				}
				float across = Math.abs(dx * -directionZ + dz * directionX);
				float farFade = smoothStep(0.0f, Math.max(96.0f, length * 0.28f), length - along);
				float startFade = smoothStep(-halfWidth * 0.35f, Math.max(16.0f, halfWidth * 0.45f), along);
				float trunkWidth = Math.max(9.0f, Math.min(26.0f, halfWidth * 0.24f));
				float trunk = 1.0f - smoothStep(trunkWidth, trunkWidth * 3.2f, across);
				float trunkFade = smoothStep(0.0f, Math.max(36.0f, halfWidth * 0.75f), along)
					* smoothStep(0.0f, Math.max(112.0f, length * 0.36f), length - along);
				float canopyCenter = Math.min(length * 0.36f, Math.max(72.0f, halfWidth * 1.45f));
				float canopyRadiusAlong = Math.max(112.0f, halfWidth * 2.35f);
				float canopyRadiusAcross = Math.max(48.0f, halfWidth * 1.25f);
				float canopyAlong = (along - canopyCenter) / canopyRadiusAlong;
				float canopyAcross = across / canopyRadiusAcross;
				float canopyDistance = canopyAlong * canopyAlong + canopyAcross * canopyAcross;
				float canopy = 1.0f - smoothStep(0.16f, 1.0f, canopyDistance);
				float shapedAlpha = Math.max(trunk * trunkFade * 0.85f, canopy * startFade * farFade * 0.55f);
				return REMASTER_SHADOW_MASK_BASE_ALPHA
					* opacity
					* clampStatic(shapedAlpha, 0.0f, 1.0f);
			}

			private float alphaAtCenterFallback(float x, float z) {
				float dx = x - centerX;
				float dz = z - centerZ;
				float along = dx * directionX + dz * directionZ;
				if (along < 0.0f || along > length) {
					return 0.0f;
				}
				float across = Math.abs(dx * -directionZ + dz * directionX);
				if (across > halfWidth) {
					return 0.0f;
				}
				float sideFade = (halfWidth - across) / Math.max(16.0f, halfWidth * 0.25f);
				float endFade = (length - along) / Math.max(64.0f, length * 0.22f);
				return REMASTER_SHADOW_MASK_BASE_ALPHA
					* opacity
					* clampStatic(Math.min(sideFade, endFade), 0.0f, 1.0f);
			}

			private boolean isBlockedBy(RemasterShadowRoofCoverage roofCoverage, float x, float z) {
				if (roofCoverage == null) {
					return false;
				}
				float along = shadowAlongAt(x, z);
				if (along <= REMASTER_SHADOW_MASK_CLIP_START_OFFSET) {
					return false;
				}
				float sourceX = x - directionX * along;
				float sourceZ = z - directionZ * along;
				float startX = sourceX + directionX * REMASTER_SHADOW_MASK_CLIP_START_OFFSET;
				float startZ = sourceZ + directionZ * REMASTER_SHADOW_MASK_CLIP_START_OFFSET;
				return roofCoverage.crossesShadowBlocker(plane, startX, startZ, x, z);
			}

			private float shadowAlongAt(float x, float z) {
				if (style == STYLE_SOFT_SCENERY) {
					float dx = x - centerX;
					float dz = z - centerZ;
					return dx * directionX + dz * directionZ;
				}
				float determinant = edgeX * directionZ - edgeZ * directionX;
				if (edgeLength > 0.0001f && Math.abs(determinant) > 0.08f) {
					float px = x - baseX0;
					float pz = z - baseZ0;
					return (edgeX * pz - edgeZ * px) / determinant;
				}
				float dx = x - centerX;
				float dz = z - centerZ;
				return dx * directionX + dz * directionZ;
			}

			private long mixSignature(long hash) {
				hash = shadowProofMix(hash, style);
				hash = shadowProofMix(hash, plane);
				hash = shadowProofMix(hash, Float.floatToIntBits(baseX0));
				hash = shadowProofMix(hash, Float.floatToIntBits(baseZ0));
				hash = shadowProofMix(hash, Float.floatToIntBits(baseX1));
				hash = shadowProofMix(hash, Float.floatToIntBits(baseZ1));
				hash = shadowProofMix(hash, Float.floatToIntBits(centerX));
				hash = shadowProofMix(hash, Float.floatToIntBits(centerZ));
				hash = shadowProofMix(hash, Float.floatToIntBits(length));
				hash = shadowProofMix(hash, Float.floatToIntBits(halfWidth));
				hash = shadowProofMix(hash, Float.floatToIntBits(directionX));
				hash = shadowProofMix(hash, Float.floatToIntBits(directionZ));
				hash = shadowProofMix(hash, Float.floatToIntBits(opacity));
				hash = shadowProofMix(hash, Float.floatToIntBits(minX));
				hash = shadowProofMix(hash, Float.floatToIntBits(maxX));
				hash = shadowProofMix(hash, Float.floatToIntBits(minZ));
				return shadowProofMix(hash, Float.floatToIntBits(maxZ));
			}

			private static float smoothStep(float edge0, float edge1, float value) {
				if (edge1 <= edge0) {
					return value >= edge1 ? 1.0f : 0.0f;
				}
				float t = clampStatic((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
				return t * t * (3.0f - 2.0f * t);
			}
		}

		private static float clampStatic(float value, float min, float max) {
			return Math.max(min, Math.min(max, value));
		}

		private float clamp(float value, float min, float max) {
			return Math.max(min, Math.min(max, value));
		}

		private void drawChunkDiagnosticPass(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame chunkFrame,
			boolean textured,
			boolean transparentPass,
			Renderer3DModelKind modelKind,
			WorldChunkDrawAccumulator accumulator,
			float[] batchCullViewMatrix,
			boolean shaderActive,
			FloatBuffer residentWorldToClipMatrix,
			FloatBuffer residentWorldViewMatrix) throws Exception {
			if (!shouldDrawChunkModelKind(modelKind)) {
				return;
			}
			boolean wallDepthPriority = modelKind == Renderer3DModelKind.WALL;
			if (wallDepthPriority) {
				enableWallDepthPriority();
			}
			boolean residentObjectCull = shouldCullResidentObjectBatches(modelKind);
			if (residentObjectCull) {
				gl.glEnable(gl.GL_CULL_FACE);
				gl.glCullFace(gl.GL_BACK);
			}
			long shadowProofSignature = currentShadowProofSignature(chunkFrame);
			try {
				for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
					WorldChunkBufferKey key = WorldChunkBufferKey.from(chunk);
					WorldChunkBuffer buffer = residentChunks.get(key);
					if (buffer == null || !buffer.matches(
						chunk.getSignature(),
						chunk.getVertexCount(),
						chunk.getIndexCount(),
						chunk.getTriangleCount(),
						textured,
						currentBrightnessBits(),
						currentFogModeBits(),
						currentLightingModeBits(),
						currentGeometryModeBits(),
						WORLD_CHUNKS_REPLACEMENT_COMPOSITE,
						shadowProofSignature)) {
						continue;
					}
					accumulator.recordConsideredChunk(key);
					gl.glBindBuffer(gl.GL_ARRAY_BUFFER, buffer.vertexBufferId);
					gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, buffer.materialIndexBufferId);
					if (!shaderActive) {
						gl.glVertexPointer(POSITION_COMPONENT_COUNT, gl.GL_FLOAT, STRIDE_BYTES, 0L);
						bindChunkVertexAttributes(textured);
					}
					int chunkDrawnTriangles = 0;
					for (WorldChunkMaterialBatch batch : buffer.materialBatches) {
						if (batch.indexCount <= 0) {
							continue;
						}
						if (batch.kind != modelKind) {
							continue;
						}
						if (textured && isTransparentChunkBatch(frame, batch) != transparentPass) {
							continue;
						}
						accumulator.recordConsideredBatch();
						if (isFrustumCulledChunkBatch(frame, batch, batchCullViewMatrix)
							|| isFogCulledChunkBatch(frame, batch, batchCullViewMatrix)) {
							accumulator.recordCulledBatch();
							accumulator.recordSkippedBatch(batch.indexCount / 3);
							continue;
						}
						WorldChunkBatchBindResult bindResult = WorldChunkBatchBindResult.TEXTURED;
						if (textured) {
							bindResult = bindTexturedOrFlatChunkBatch(frame, batch, accumulator, shaderActive);
							if (bindResult == WorldChunkBatchBindResult.SKIPPED) {
								accumulator.recordSkippedBatch(batch.indexCount / 3);
								continue;
							}
						}
						if (shaderActive) {
							bindResidentChunkShaderBatch(
								frame,
								bindResult == WorldChunkBatchBindResult.TEXTURED,
								residentWorldToClipMatrix,
								residentWorldViewMatrix);
						}
						gl.glDrawElements(
							gl.GL_TRIANGLES,
							batch.indexCount,
							gl.GL_UNSIGNED_INT,
							batch.startIndex * 4L);
						accumulator.recordDrawnBatch();
						accumulator.recordDrawCall();
						int batchTriangles = batch.indexCount / 3;
						chunkDrawnTriangles += batchTriangles;
						accumulator.recordBatch(batch.kind, batchTriangles);
						if (bindResult == WorldChunkBatchBindResult.FLAT_FALLBACK) {
							accumulator.recordFallbackBatch(batchTriangles);
						}
					}
					if (chunkDrawnTriangles > 0) {
						accumulator.recordChunk(key);
					}
				}
			} finally {
				if (residentObjectCull) {
					gl.glDisable(gl.GL_CULL_FACE);
				}
				if (wallDepthPriority) {
					disableWallDepthPriority();
				}
			}
		}

		private boolean shouldCullResidentObjectBatches(Renderer3DModelKind modelKind) {
			return WORLD_CHUNKS_RESIDENT_OBJECTS
				&& (modelKind == Renderer3DModelKind.GAME_OBJECT
					|| modelKind == Renderer3DModelKind.WALL_OBJECT);
		}

		private void enableWallDepthPriority() throws Exception {
			gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(WALL_DEPTH_PRIORITY_FACTOR, WALL_DEPTH_PRIORITY_UNITS);
		}

		private void disableWallDepthPriority() throws Exception {
			gl.glPolygonOffset(0.0f, 0.0f);
			gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
		}

		private boolean shouldDrawChunkModelKind(Renderer3DModelKind modelKind) {
			if (WORLD_CHUNKS_REPLACEMENT_COMPOSITE
				&& (modelKind == Renderer3DModelKind.GAME_OBJECT
					|| modelKind == Renderer3DModelKind.WALL_OBJECT)
				&& !WORLD_CHUNKS_RESIDENT_OBJECTS) {
				return false;
			}
			return modelKind != Renderer3DModelKind.ROOF || !Config.C_HIDE_ROOFS;
		}

		private void bindChunkVertexAttributes(boolean textured) throws Exception {
			if (textured) {
				gl.glEnableClientState(gl.GL_COLOR_ARRAY);
				gl.glColorPointer(
					TEXTURE_LIGHT_COMPONENT_COUNT,
					gl.GL_FLOAT,
					STRIDE_BYTES,
					TEXTURE_LIGHT_OFFSET_BYTES);
				gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				gl.glTexCoordPointer(
					TEXTURE_COORD_COMPONENT_COUNT,
					gl.GL_FLOAT,
					STRIDE_BYTES,
					TEXTURE_COORD_OFFSET_BYTES);
			} else {
				gl.glEnableClientState(gl.GL_COLOR_ARRAY);
				gl.glColorPointer(COLOR_COMPONENT_COUNT, gl.GL_FLOAT, STRIDE_BYTES, COLOR_OFFSET_BYTES);
			}
		}

		private boolean useResidentChunkShader(boolean textured) {
			return textured && WORLD_CHUNKS_TEXTURED_SHADER && residentChunkShader != null;
		}

		private void bindResidentChunkShaderBatch(
			Renderer3DFrame frame,
			boolean textureEnabled,
			FloatBuffer residentWorldToClipMatrix,
			FloatBuffer residentWorldViewMatrix) throws Exception {
			boolean remasterLightingEnabled =
				WORLD_CHUNKS_REMASTER_LIGHTING_SHADER
					&& RendererLightingSettings.getMode() == RendererLightingSettings.Mode.DIRECTIONAL;
			boolean rawMaterialMode = WORLD_CHUNKS_RAW_MATERIAL_SHADER;
			residentChunkShader.useResidentChunk(
				residentWorldToClipMatrix,
				residentWorldViewMatrix,
				textureEnabled,
				rawMaterialMode,
				remasterLightingEnabled,
				frame);
			residentChunkShader.bindWorldParityAttributes(
				POSITION_COMPONENT_COUNT,
				TEXTURE_COORD_COMPONENT_COUNT,
				textureEnabled ? TEXTURE_LIGHT_COMPONENT_COUNT : COLOR_COMPONENT_COUNT,
				RAW_MATERIAL_COLOR_COMPONENT_COUNT,
				NORMAL_COMPONENT_COUNT,
				MODEL_KIND_COMPONENT_COUNT,
				STRIDE_BYTES,
				0L,
				TEXTURE_COORD_OFFSET_BYTES,
				textureEnabled ? TEXTURE_LIGHT_OFFSET_BYTES : COLOR_OFFSET_BYTES,
				RAW_MATERIAL_COLOR_OFFSET_BYTES,
				NORMAL_OFFSET_BYTES,
				MODEL_KIND_OFFSET_BYTES);
		}

		private boolean isTransparentChunkBatch(Renderer3DFrame frame, WorldChunkMaterialBatch batch) {
			OpenGLTextureRegion region = textureRegionForBatch(frame, batch);
			return region != null && region.hasTransparency();
		}

		private boolean isFrustumCulledChunkBatch(
			Renderer3DFrame frame,
			WorldChunkMaterialBatch batch,
			float[] viewMatrix) {
			if (frame == null || batch == null || viewMatrix == null || !batch.hasBounds()) {
				return false;
			}
			float near = Math.max(1.0f, frame.getNearPlane());
			float scale = (float) (1 << Math.max(0, Math.min(24, frame.getPerspectiveShift())));
			float viewportWidth = Math.max(1.0f, frame.getViewportWidth());
			float viewportHeight = Math.max(1.0f, frame.getViewportHeight());
			float centerX = frame.getCenterX();
			float centerY = frame.getCenterY();
			float minScreenX = Float.MAX_VALUE;
			float maxScreenX = -Float.MAX_VALUE;
			float minScreenY = Float.MAX_VALUE;
			float maxScreenY = -Float.MAX_VALUE;
			for (int xIndex = 0; xIndex < 2; xIndex++) {
				float x = xIndex == 0 ? batch.minX : batch.maxX;
				for (int yIndex = 0; yIndex < 2; yIndex++) {
					float y = yIndex == 0 ? batch.minY : batch.maxY;
					for (int zIndex = 0; zIndex < 2; zIndex++) {
						float z = zIndex == 0 ? batch.minZ : batch.maxZ;
						float cameraX = viewMatrix[0] * x + viewMatrix[1] * y + viewMatrix[2] * z + viewMatrix[3];
						float cameraY = viewMatrix[4] * x + viewMatrix[5] * y + viewMatrix[6] * z + viewMatrix[7];
						float cameraZ = viewMatrix[8] * x + viewMatrix[9] * y + viewMatrix[10] * z + viewMatrix[11];
						if (cameraZ <= near) {
							return false;
						}
						float screenX = centerX + scale * cameraX / cameraZ;
						float screenY = centerY - scale * cameraY / cameraZ;
						minScreenX = Math.min(minScreenX, screenX);
						maxScreenX = Math.max(maxScreenX, screenX);
						minScreenY = Math.min(minScreenY, screenY);
						maxScreenY = Math.max(maxScreenY, screenY);
					}
				}
			}
			if (minScreenX == Float.MAX_VALUE || minScreenY == Float.MAX_VALUE) {
				return false;
			}
			return maxScreenX < -FRUSTUM_BATCH_CULL_SCREEN_PADDING
				|| minScreenX > viewportWidth + FRUSTUM_BATCH_CULL_SCREEN_PADDING
				|| maxScreenY < -FRUSTUM_BATCH_CULL_SCREEN_PADDING
				|| minScreenY > viewportHeight + FRUSTUM_BATCH_CULL_SCREEN_PADDING;
		}

		private boolean isFogCulledChunkBatch(
			Renderer3DFrame frame,
			WorldChunkMaterialBatch batch,
			float[] viewMatrix) {
			if (frame == null || batch == null || viewMatrix == null || !batch.hasSpatialBounds()) {
				return false;
			}
			float fogEnd = frame.getFogDistance() + FOG_BATCH_CULL_PADDING;
			float nearestCameraZ = Float.MAX_VALUE;
			for (int xIndex = 0; xIndex < 2; xIndex++) {
				float x = xIndex == 0 ? batch.minX : batch.maxX;
				for (int yIndex = 0; yIndex < 2; yIndex++) {
					float y = yIndex == 0 ? batch.minY : batch.maxY;
					for (int zIndex = 0; zIndex < 2; zIndex++) {
						float z = zIndex == 0 ? batch.minZ : batch.maxZ;
						float cameraZ = viewMatrix[8] * x + viewMatrix[9] * y + viewMatrix[10] * z + viewMatrix[11];
						if (cameraZ < nearestCameraZ) {
							nearestCameraZ = cameraZ;
						}
					}
				}
			}
			return nearestCameraZ > fogEnd;
		}

		private WorldChunkBatchBindResult bindTexturedOrFlatChunkBatch(
			Renderer3DFrame frame,
			WorldChunkMaterialBatch batch,
			WorldChunkDrawAccumulator accumulator,
			boolean shaderActive) throws Exception {
			OpenGLTextureRegion region = textureRegionForBatch(frame, batch);
			if (region == null) {
				return bindFlatChunkBatch(frame, batch, shaderActive)
					? WorldChunkBatchBindResult.FLAT_FALLBACK
					: WorldChunkBatchBindResult.SKIPPED;
			}
			gl.glEnable(gl.GL_TEXTURE_2D);
			gl.glEnable(gl.GL_ALPHA_TEST);
			gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			gl.glDisable(gl.GL_BLEND);
			if (!shaderActive) {
				gl.glEnableClientState(gl.GL_COLOR_ARRAY);
				gl.glColorPointer(
					TEXTURE_LIGHT_COMPONENT_COUNT,
					gl.GL_FLOAT,
					STRIDE_BYTES,
					TEXTURE_LIGHT_OFFSET_BYTES);
				gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				gl.glTexCoordPointer(
					TEXTURE_COORD_COMPONENT_COUNT,
					gl.GL_FLOAT,
					STRIDE_BYTES,
					TEXTURE_COORD_OFFSET_BYTES);
			}
			bindChunkTexture(region.getTextureId(), accumulator);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			return WorldChunkBatchBindResult.TEXTURED;
		}

		private void bindChunkTexture(int textureId, WorldChunkDrawAccumulator accumulator) throws Exception {
			if (accumulator.boundTextureId == textureId) {
				return;
			}
			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			accumulator.boundTextureId = textureId;
			accumulator.recordTextureBind();
		}

		private OpenGLTextureRegion textureRegionForBatch(Renderer3DFrame frame, WorldChunkMaterialBatch batch) {
			if (textureCache == null || batch == null) {
				return null;
			}
			int textureId = effectiveChunkTextureId(frame, batch.textureId, batch.fallbackColor);
			return textureId == LEGACY_TRANSPARENT_TEXTURE ? null : textureCache.getRegion(frame, textureId);
		}

		private boolean bindFlatChunkBatch(
			Renderer3DFrame frame,
			WorldChunkMaterialBatch batch,
			boolean shaderActive) throws Exception {
			int fallbackColor = fallbackColorForBatch(frame, batch);
			if (fallbackColor == NO_FALLBACK_COLOR) {
				return false;
			}
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glDisable(gl.GL_BLEND);
			if (!shaderActive) {
				gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				gl.glEnableClientState(gl.GL_COLOR_ARRAY);
				gl.glColorPointer(COLOR_COMPONENT_COUNT, gl.GL_FLOAT, STRIDE_BYTES, COLOR_OFFSET_BYTES);
			}
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			return true;
		}

		private int fallbackColorForBatch(Renderer3DFrame frame, WorldChunkMaterialBatch batch) {
			int textureId = effectiveChunkTextureId(frame, batch.textureId, batch.fallbackColor);
			int textureAverageColor = averageTextureColor(frame, textureId);
			if (textureAverageColor != NO_FALLBACK_COLOR) {
				return textureAverageColor;
			}
			int fallbackTextureAverageColor = averageTextureColor(frame, batch.fallbackColor);
			if (fallbackTextureAverageColor != NO_FALLBACK_COLOR) {
				return fallbackTextureAverageColor;
			}
			if (batch.textureId == LEGACY_TRANSPARENT_TEXTURE && batch.fallbackColor != LEGACY_TRANSPARENT_TEXTURE) {
				if (isFrameTextureReference(frame, batch.fallbackColor)) {
					return NO_FALLBACK_COLOR;
				}
				return batch.fallbackColor & 0xFFFFFF;
			}
			return NO_FALLBACK_COLOR;
		}

		private boolean isFrameTextureReference(Renderer3DFrame frame, int textureId) {
			return frame != null && textureId >= 0 && textureId < frame.getTextures().length;
		}

		private int averageTextureColor(Renderer3DFrame frame, int textureId) {
			if (frame == null || textureId < 0) {
				return NO_FALLBACK_COLOR;
			}
			Renderer3DTextureData texture = frame.getTexture(textureId);
			if (texture == null || texture.getOpaquePixelCount() <= 0) {
				return NO_FALLBACK_COLOR;
			}
			return texture.getAverageOpaqueRgb();
		}

		private void putFlatFallbackColor(int color) throws Exception {
			float brightness = RendererDayNightCycle.currentBrightnessMultiplier();
			gl.glColor4f(
				brightnessColor(((color >> 16) & 0xFF) / 255.0f, brightness),
				brightnessColor(((color >> 8) & 0xFF) / 255.0f, brightness),
				brightnessColor((color & 0xFF) / 255.0f, brightness),
				1.0f);
		}

		private float brightnessColor(float color, float brightness) {
			float adjusted = color * brightness;
			if (adjusted < 0.0f) {
				return 0.0f;
			}
			if (adjusted > 1.0f) {
				return 1.0f;
			}
			return adjusted;
		}

		private int currentBrightnessBits() {
			return Float.floatToIntBits(RendererDayNightCycle.currentBrightnessMultiplier());
		}

		private int currentFogModeBits() {
			return RendererFogSettings.getMode().ordinal();
		}

		private int currentLightingModeBits() {
			return RendererLightingSettings.getMode().ordinal();
		}

		private int currentGeometryModeBits() {
			return RendererGeometrySettings.getMode().ordinal();
		}

		private boolean shouldUseSpatialMaterialBatches() {
			return WORLD_CHUNKS_SPATIAL_CULL;
		}

		private void loadWorldProjectionAndView(Renderer3DFrame frame) throws Exception {
			if (worldToClipMatrixBuffer == null) {
				worldToClipMatrixBuffer = ByteBuffer
					.allocateDirect(16 * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}
			loadMatrix(gl.GL_PROJECTION, projectionMatrix(frame));
			loadMatrix(gl.GL_MODELVIEW, worldViewMatrix(frame));
		}

		private void loadMatrix(int matrixMode, float[] matrix) throws Exception {
			worldToClipMatrixBuffer.clear();
			for (int column = 0; column < 4; column++) {
				for (int row = 0; row < 4; row++) {
					worldToClipMatrixBuffer.put(matrix[row * 4 + column]);
				}
			}
			worldToClipMatrixBuffer.flip();
			gl.glMatrixMode(matrixMode);
			gl.glLoadMatrixf(worldToClipMatrixBuffer);
		}

		private FloatBuffer putWorldToClipMatrix(float[] matrix) {
			if (worldToClipMatrixBuffer == null) {
				worldToClipMatrixBuffer = ByteBuffer
					.allocateDirect(16 * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}
			return putColumnMajorMatrix(worldToClipMatrixBuffer, matrix);
		}

		private FloatBuffer putWorldViewMatrix(float[] matrix) {
			if (worldViewMatrixBuffer == null) {
				worldViewMatrixBuffer = ByteBuffer
					.allocateDirect(16 * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}
			return putColumnMajorMatrix(worldViewMatrixBuffer, matrix);
		}

		private FloatBuffer putColumnMajorMatrix(FloatBuffer buffer, float[] matrix) {
			buffer.clear();
			for (int column = 0; column < 4; column++) {
				for (int row = 0; row < 4; row++) {
					buffer.put(matrix[row * 4 + column]);
				}
			}
			buffer.flip();
			return buffer;
		}

		private float[] worldViewMatrix(Renderer3DFrame frame) {
			float[] view = translationMatrix(
				-frame.getCameraOffsetX(),
				-frame.getCameraOffsetY(),
				-frame.getCameraOffsetZ());
			if (frame.getCameraRotationZ() != 0) {
				view = multiply(rotationZMatrix(frame.getCameraRotationZ()), view);
			}
			if (frame.getCameraRotationY() != 0) {
				view = multiply(rotationYMatrix(frame.getCameraRotationY()), view);
			}
			if (frame.getCameraRotationX() != 0) {
				view = multiply(rotationXMatrix(frame.getCameraRotationX()), view);
			}
			return view;
		}

		private void configureFog(Renderer3DFrame frame) throws Exception {
			if (RendererFogSettings.getMode() == RendererFogSettings.Mode.OFF) {
				gl.glDisable(gl.GL_FOG);
				return;
			}
			if (fogColorBuffer == null) {
				fogColorBuffer = ByteBuffer.allocateDirect(4 * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
				fogColorBuffer.put(0.0f).put(0.0f).put(0.0f).put(1.0f).flip();
			}
			gl.glFogi(gl.GL_FOG_MODE, gl.GL_LINEAR);
			gl.glFogf(gl.GL_FOG_START, frame.getFogStartDistance());
			gl.glFogf(gl.GL_FOG_END, frame.getFogDistance());
			gl.glFogfv(gl.GL_FOG_COLOR, fogColorBuffer);
			gl.glEnable(gl.GL_FOG);
		}

		private float[] projectionMatrix(Renderer3DFrame frame) {
			float viewportWidth = Math.max(1.0f, frame.getViewportWidth());
			float viewportHeight = Math.max(1.0f, frame.getViewportHeight());
			float scale = (float) (1 << Math.max(0, Math.min(24, frame.getPerspectiveShift())));
			float near = Math.max(1.0f, frame.getNearPlane());
			float far = Math.max(near + 1.0f, frame.getFogDistance());
			float depthA = (far + near) / (far - near);
			float depthB = -(2.0f * far * near) / (far - near);
			return new float[] {
				2.0f * scale / viewportWidth, 0.0f, 2.0f * frame.getCenterX() / viewportWidth - 1.0f, 0.0f,
				0.0f, -2.0f * scale / viewportHeight, 1.0f - 2.0f * frame.getCenterY() / viewportHeight, 0.0f,
				0.0f, 0.0f, depthA, depthB,
				0.0f, 0.0f, 1.0f, 0.0f
			};
		}

		private float[] translationMatrix(float x, float y, float z) {
			return new float[] {
				1.0f, 0.0f, 0.0f, x,
				0.0f, 1.0f, 0.0f, y,
				0.0f, 0.0f, 1.0f, z,
				0.0f, 0.0f, 0.0f, 1.0f
			};
		}

		private float[] rotationZMatrix(int angle) {
			float sin = sin1024(angle);
			float cos = cos1024(angle);
			return new float[] {
				cos, sin, 0.0f, 0.0f,
				-sin, cos, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 1.0f
			};
		}

		private float[] rotationYMatrix(int angle) {
			float sin = sin1024(angle);
			float cos = cos1024(angle);
			return new float[] {
				cos, 0.0f, sin, 0.0f,
				0.0f, 1.0f, 0.0f, 0.0f,
				-sin, 0.0f, cos, 0.0f,
				0.0f, 0.0f, 0.0f, 1.0f
			};
		}

		private float[] rotationXMatrix(int angle) {
			float sin = sin1024(angle);
			float cos = cos1024(angle);
			return new float[] {
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, cos, -sin, 0.0f,
				0.0f, sin, cos, 0.0f,
				0.0f, 0.0f, 0.0f, 1.0f
			};
		}

		private float[] multiply(float[] left, float[] right) {
			float[] result = new float[16];
			for (int row = 0; row < 4; row++) {
				for (int column = 0; column < 4; column++) {
					float value = 0.0f;
					for (int index = 0; index < 4; index++) {
						value += left[row * 4 + index] * right[index * 4 + column];
					}
					result[row * 4 + column] = value;
				}
			}
			return result;
		}

		private float sin1024(int angle) {
			return FastMath.trigTable1024[angle & 1023] / 32768.0f;
		}

		private float cos1024(int angle) {
			return FastMath.trigTable1024[(angle & 1023) + 1024] / 32768.0f;
		}

		private void uploadChunk(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			WorldChunkBuffer buffer,
			Renderer3DFrame frame,
			int vertexCount,
			int indexCount,
			boolean atlasTextureCoordinates,
			int brightnessBits,
			int fogModeBits,
			int lightingModeBits,
			int geometryModeBits,
			boolean replacementCompositeDrawOnly,
			List<ShadowProofCaster> shadowProofCasters,
			long shadowProofSignature) throws Exception {
			ensureUploadBuffers(vertexCount, indexCount);
			copyChunkVertices(chunk, frame, vertexCount, atlasTextureCoordinates, shadowProofCasters);
			copyChunkIndices(chunk, indexCount);
			WorldChunkMaterialBatch[] materialBatches =
				copyMaterialIndices(chunk, indexCount, replacementCompositeDrawOnly, shouldUseSpatialMaterialBatches());

			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, buffer.vertexBufferId);
			gl.glBufferData(gl.GL_ARRAY_BUFFER, vertexUploadBuffer, gl.GL_STATIC_DRAW);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, buffer.indexBufferId);
			gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indexUploadBuffer, gl.GL_STATIC_DRAW);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, buffer.materialIndexBufferId);
			gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, materialIndexUploadBuffer, gl.GL_STATIC_DRAW);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);

			buffer.signature = chunk.getSignature();
			buffer.vertexCount = vertexCount;
			buffer.indexCount = indexCount;
			buffer.triangleCount = chunk.getTriangleCount();
			buffer.atlasTextureCoordinates = atlasTextureCoordinates;
			buffer.brightnessBits = brightnessBits;
			buffer.fogModeBits = fogModeBits;
			buffer.lightingModeBits = lightingModeBits;
			buffer.geometryModeBits = geometryModeBits;
			buffer.replacementCompositeDrawOnly = replacementCompositeDrawOnly;
			buffer.materialBatches = materialBatches;
			buffer.shadowProofSignature = shadowProofSignature;
		}

		private WorldChunkMaterialBatch[] copyMaterialIndices(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int indexCount,
			boolean replacementCompositeDrawOnly,
			boolean spatialBatches) {
			int triangleCount = Math.min(chunk.getTriangleCount(), indexCount / 3);
			Map<WorldChunkMaterialKey, WorldChunkMaterialBatchBuilder> batchesByMaterial =
				new LinkedHashMap<WorldChunkMaterialKey, WorldChunkMaterialBatchBuilder>();
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!shouldIncludeChunkTriangleInResidentDrawBuffer(chunk, triangle, replacementCompositeDrawOnly)) {
					continue;
				}
				WorldChunkMaterialKey key = WorldChunkMaterialKey.from(chunk, triangle, spatialBatches);
				WorldChunkMaterialBatchBuilder batch = batchesByMaterial.get(key);
				if (batch == null) {
					batch = new WorldChunkMaterialBatchBuilder(key);
					batchesByMaterial.put(key, batch);
				}
				batch.indexCount += 3;
				batch.includeTriangleBounds(chunk, triangle);
			}

			int nextIndex = 0;
			List<WorldChunkMaterialBatchBuilder> batchBuilders =
				new ArrayList<WorldChunkMaterialBatchBuilder>(batchesByMaterial.values());
			for (WorldChunkMaterialBatchBuilder batch : batchBuilders) {
				batch.startIndex = nextIndex;
				batch.writeIndex = nextIndex;
				nextIndex += batch.indexCount;
			}

			materialIndexUploadBuffer.clear();
			materialIndexUploadBuffer.limit(nextIndex);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!shouldIncludeChunkTriangleInResidentDrawBuffer(chunk, triangle, replacementCompositeDrawOnly)) {
					continue;
				}
				WorldChunkMaterialBatchBuilder batch =
					batchesByMaterial.get(WorldChunkMaterialKey.from(chunk, triangle, spatialBatches));
				int sourceIndex = triangle * 3;
				materialIndexUploadBuffer.put(batch.writeIndex++, chunk.getIndex(sourceIndex));
				materialIndexUploadBuffer.put(batch.writeIndex++, chunk.getIndex(sourceIndex + 1));
				materialIndexUploadBuffer.put(batch.writeIndex++, chunk.getIndex(sourceIndex + 2));
			}
			materialIndexUploadBuffer.position(0);

			WorldChunkMaterialBatch[] batches = new WorldChunkMaterialBatch[batchBuilders.size()];
			for (int i = 0; i < batchBuilders.size(); i++) {
				batches[i] = batchBuilders.get(i).toBatch();
			}
			return batches;
		}

		private boolean shouldIncludeChunkTriangleInResidentDrawBuffer(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int triangle,
			boolean replacementCompositeDrawOnly) {
			if (!replacementCompositeDrawOnly) {
				return true;
			}
			Renderer3DModelKind kind = chunk.getTriangleModelKind(triangle);
			return WORLD_CHUNKS_RESIDENT_OBJECTS
				|| (kind != Renderer3DModelKind.GAME_OBJECT && kind != Renderer3DModelKind.WALL_OBJECT);
		}

		private void ensureUploadBuffers(int vertexCount, int indexCount) {
			int requiredVertexFloats = vertexCount * FLOATS_PER_VERTEX;
			if (vertexUploadBuffer == null || vertexUploadCapacity < requiredVertexFloats) {
				vertexUploadCapacity = growCapacity(vertexUploadCapacity, requiredVertexFloats);
				vertexUploadBuffer = ByteBuffer
					.allocateDirect(vertexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}

			if (indexUploadBuffer == null || indexUploadCapacity < indexCount) {
				indexUploadCapacity = growCapacity(indexUploadCapacity, indexCount);
				indexUploadBuffer = ByteBuffer
					.allocateDirect(indexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asIntBuffer();
			}

			if (materialIndexUploadBuffer == null || materialIndexUploadCapacity < indexCount) {
				materialIndexUploadCapacity = growCapacity(materialIndexUploadCapacity, indexCount);
				materialIndexUploadBuffer = ByteBuffer
					.allocateDirect(materialIndexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asIntBuffer();
			}
		}

		private int growCapacity(int currentCapacity, int requiredCapacity) {
			int capacity = Math.max(1024, currentCapacity);
			while (capacity < requiredCapacity) {
				capacity *= 2;
			}
			return capacity;
		}

		private void copyChunkVertices(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			Renderer3DFrame frame,
			int vertexCount,
			boolean atlasTextureCoordinates,
			List<ShadowProofCaster> shadowProofCasters) {
			vertexUploadBuffer.clear();
			int[] smoothDiffuseLights = buildChunkSmoothDiffuseLights(chunk, vertexCount);
			float[] bakedTerrainShadowMask = buildBakedTerrainShadowMask(chunk, shadowProofCasters);
			for (int vertex = 0; vertex < vertexCount; vertex++) {
				int coord = vertex * POSITION_COMPONENT_COUNT;
				int triangle = vertex / 3;
				OpenGLTextureRegion textureRegion =
					atlasTextureCoordinates ? textureRegionForVertex(frame, chunk, vertex) : null;
				int effectiveTextureId = effectiveChunkTextureId(
					frame,
					chunk.getTriangleTexture(triangle),
					chunk.getTriangleFallbackColor(triangle));
				int rawMaterialColor = rawMaterialColorForTriangle(frame, chunk, triangle);
				vertexUploadBuffer.put((float) chunk.getVertexCoord(coord));
				vertexUploadBuffer.put((float) chunk.getVertexCoord(coord + 1));
				vertexUploadBuffer.put((float) chunk.getVertexCoord(coord + 2));
				int legacyLight = chunkLegacyLight(chunk, vertex, smoothDiffuseLights);
				int baseLegacyLight = legacyLight;
				legacyLight = applyBakedTerrainShadow(legacyLight, bakedTerrainShadowMask, triangle);
				putChunkMaterialColor(
					materialColorForTriangle(frame, chunk, triangle),
					effectiveTextureId,
					legacyLight);
				vertexUploadBuffer.put(chunkTextureU(textureRegion, chunk.getVertexTextureU(vertex)));
				vertexUploadBuffer.put(chunkTextureV(textureRegion, chunk.getVertexTextureV(vertex)));
				putChunkTextureLight(textureLightFactor(legacyLight));
				putChunkShaderInputs(chunk, vertex, triangle, legacyLight, baseLegacyLight, rawMaterialColor);
			}
			vertexUploadBuffer.flip();
		}

		private void putChunkShaderInputs(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int vertex,
			int triangle,
			int legacyLight,
			int baseLegacyLight,
			int rawMaterialColor) {
			vertexUploadBuffer.put((float) clampLegacyLight(legacyLight));
			vertexUploadBuffer.put((float) clampLegacyLight(baseLegacyLight));
			vertexUploadBuffer.put(((rawMaterialColor >> 16) & 0xFF) / 255.0f);
			vertexUploadBuffer.put(((rawMaterialColor >> 8) & 0xFF) / 255.0f);
			vertexUploadBuffer.put((rawMaterialColor & 0xFF) / 255.0f);
			vertexUploadBuffer.put(chunk.getVertexNormalX(vertex) / 256.0f);
			vertexUploadBuffer.put(chunk.getVertexNormalY(vertex) / 256.0f);
			vertexUploadBuffer.put(chunk.getVertexNormalZ(vertex) / 256.0f);
			Renderer3DModelKind modelKind = triangle >= 0 && triangle < chunk.getTriangleCount()
				? chunk.getTriangleModelKind(triangle)
				: Renderer3DModelKind.UNCLASSIFIED;
			vertexUploadBuffer.put((float) modelKind.ordinal());
		}

		private float[] buildBakedTerrainShadowMask(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			List<ShadowProofCaster> shadowProofCasters) {
			if (!shouldDrawProjectedShadowProof()
				|| chunk == null
				|| shadowProofCasters == null
				|| shadowProofCasters.isEmpty()
				|| chunk.getTerrainTriangles() <= 0) {
				return null;
			}
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			float[] shadowMask = new float[triangleCount];
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				shadowMask[triangle] = projectedShadowProofAlpha(chunk, triangle, shadowProofCasters);
			}
			return shadowMask;
		}

		private int applyBakedTerrainShadow(int legacyLight, float[] bakedTerrainShadowMask, int triangle) {
			if (bakedTerrainShadowMask == null || triangle < 0 || triangle >= bakedTerrainShadowMask.length) {
				return legacyLight;
			}
			float shadow = bakedTerrainShadowMask[triangle];
			if (shadow <= 0.0f) {
				return legacyLight;
			}
			return clampLegacyLight(legacyLight + Math.round(shadow * 230.0f));
		}

		private int[] buildChunkSmoothDiffuseLights(Renderer3DWorldChunkFrame.ChunkMesh chunk, int vertexCount) {
			int[] diffuseLights = new int[vertexCount];
			if (RendererGeometrySettings.getMode() == RendererGeometrySettings.Mode.FACETED
				|| RendererGeometrySettings.getMode() == RendererGeometrySettings.Mode.WIRE) {
				int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
				for (int triangle = 0; triangle < triangleCount; triangle++) {
					int faceDiffuse = chunkFaceDiffuseLight(chunk, triangle);
					int sourceIndex = triangle * 3;
					for (int i = 0; i < 3; i++) {
						int vertex = chunk.getIndex(sourceIndex + i);
						if (isChunkVertexIndexValid(chunk, vertex, vertexCount)) {
							diffuseLights[vertex] = faceDiffuse;
						}
					}
				}
				return diffuseLights;
			}
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			Map<ChunkVertexKey, SmoothDiffuseAccumulator> terrainDiffuseByVertex =
				new HashMap<ChunkVertexKey, SmoothDiffuseAccumulator>();
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				int diffuse = chunkFaceDiffuseLight(chunk, triangle);
				int sourceIndex = triangle * 3;
				for (int i = 0; i < 3; i++) {
					int vertex = chunk.getIndex(sourceIndex + i);
					if (!isChunkVertexIndexValid(chunk, vertex, vertexCount)) {
						continue;
					}
					ChunkVertexKey key = ChunkVertexKey.from(chunk, vertex);
					SmoothDiffuseAccumulator accumulator = terrainDiffuseByVertex.get(key);
					if (accumulator == null) {
						accumulator = new SmoothDiffuseAccumulator();
						terrainDiffuseByVertex.put(key, accumulator);
					}
					accumulator.add(diffuse);
				}
			}

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				boolean smoothTerrain = chunk.getTriangleModelKind(triangle) == Renderer3DModelKind.TERRAIN;
				int faceDiffuse = smoothTerrain ? 0 : chunkFaceDiffuseLight(chunk, triangle);
				int sourceIndex = triangle * 3;
				for (int i = 0; i < 3; i++) {
					int vertex = chunk.getIndex(sourceIndex + i);
					if (!isChunkVertexIndexValid(chunk, vertex, vertexCount)) {
						continue;
					}
					if (smoothTerrain) {
						SmoothDiffuseAccumulator accumulator =
							terrainDiffuseByVertex.get(ChunkVertexKey.from(chunk, vertex));
						diffuseLights[vertex] = accumulator == null ? 0 : accumulator.average();
					} else {
						diffuseLights[vertex] = faceDiffuse;
					}
				}
			}
			return diffuseLights;
		}

		private boolean isChunkVertexIndexValid(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int vertex,
			int expectedVertexCount) {
			return vertex >= 0 && vertex < expectedVertexCount && vertex < chunk.getVertexCount();
		}

		private int materialColorForTriangle(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int triangle) {
			if (triangle < 0 || triangle >= chunk.getTriangleCount()) {
				return modelKindDiagnosticColor(Renderer3DModelKind.UNCLASSIFIED);
			}
			int fallbackColor = chunk.getTriangleFallbackColor(triangle);
			int textureId = effectiveChunkTextureId(frame, chunk.getTriangleTexture(triangle), fallbackColor);
			int textureAverageColor = averageTextureColor(frame, textureId);
			if (textureAverageColor != NO_FALLBACK_COLOR) {
				return textureAverageColor;
			}
			int fallbackTextureAverageColor = averageTextureColor(frame, fallbackColor);
			if (fallbackTextureAverageColor != NO_FALLBACK_COLOR) {
				return fallbackTextureAverageColor;
			}
			if (chunk.getTriangleTexture(triangle) == LEGACY_TRANSPARENT_TEXTURE
				&& fallbackColor != LEGACY_TRANSPARENT_TEXTURE
				&& !isFrameTextureReference(frame, fallbackColor)) {
				return fallbackColor & 0xFFFFFF;
			}
			return modelKindDiagnosticColor(chunk.getTriangleModelKind(triangle));
		}

		private int rawMaterialColorForTriangle(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int triangle) {
			if (triangle < 0 || triangle >= chunk.getTriangleCount()) {
				return modelKindDiagnosticColor(Renderer3DModelKind.UNCLASSIFIED);
			}
			int fallbackColor = chunk.getTriangleFallbackColor(triangle);
			int textureId = chunk.getTriangleTexture(triangle);
			if (textureId == LEGACY_TRANSPARENT_TEXTURE
				&& fallbackColor != LEGACY_TRANSPARENT_TEXTURE
				&& !isFrameTextureReference(frame, fallbackColor)) {
				return fallbackColor & 0xFFFFFF;
			}
			int fallbackTextureAverageColor = averageTextureColor(frame, fallbackColor);
			if (fallbackTextureAverageColor != NO_FALLBACK_COLOR) {
				return fallbackTextureAverageColor;
			}
			return materialColorForTriangle(frame, chunk, triangle);
		}

		private OpenGLTextureRegion textureRegionForVertex(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int vertex) {
			if (textureCache == null || frame == null) {
				return null;
			}
			int triangle = vertex / 3;
			if (triangle < 0 || triangle >= chunk.getTriangleCount()) {
				return null;
			}
			int textureId = effectiveChunkTextureId(
				frame,
				chunk.getTriangleTexture(triangle),
				chunk.getTriangleFallbackColor(triangle));
			return textureId == LEGACY_TRANSPARENT_TEXTURE ? null : textureCache.getRegion(frame, textureId);
		}

		private int effectiveChunkTextureId(Renderer3DFrame frame, int textureId, int fallbackColor) {
			if (textureId != LEGACY_TRANSPARENT_TEXTURE) {
				return textureId;
			}
			return isFrameTextureReference(frame, fallbackColor)
				? fallbackColor
				: textureId;
		}

		private float chunkTextureU(OpenGLTextureRegion region, float sourceU) {
			if (region == null) {
				return 0.0f;
			}
			return region.getU0() + (region.getU1() - region.getU0()) * clampUnit(sourceU);
		}

		private float chunkTextureV(OpenGLTextureRegion region, float sourceV) {
			if (region == null) {
				return 0.0f;
			}
			return region.getV0() + (region.getV1() - region.getV0()) * clampUnit(sourceV);
		}

		private float clampUnit(float value) {
			if (Float.isNaN(value) || value <= 0.0f) {
				return 0.0f;
			}
			if (value >= 1.0f) {
				return 1.0f;
			}
			return value;
		}

		private int modelKindDiagnosticColor(Renderer3DModelKind kind) {
			if (kind == Renderer3DModelKind.TERRAIN) {
				return 0x26F28C;
			} else if (kind == Renderer3DModelKind.WALL) {
				return 0xFF594C;
			} else if (kind == Renderer3DModelKind.ROOF) {
				return 0xFFCC33;
			}
			return 0xBF72FF;
		}

		private void putChunkMaterialColor(int color, int textureId, int legacyLight) {
			int shadedColor = shadedChunkMaterialColor(color, textureId, legacyLight);
			float brightness = RendererDayNightCycle.currentBrightnessMultiplier();
			vertexUploadBuffer.put(brightnessColor(((shadedColor >> 16) & 0xFF) / 255.0f, brightness));
			vertexUploadBuffer.put(brightnessColor(((shadedColor >> 8) & 0xFF) / 255.0f, brightness));
			vertexUploadBuffer.put(brightnessColor((shadedColor & 0xFF) / 255.0f, brightness));
			vertexUploadBuffer.put(1.0f);
		}

		private int shadedChunkMaterialColor(int color, int textureId, int legacyLight) {
			int normalizedLight = clampLegacyLight(legacyLight);
			if (textureId < 0 || textureId == LEGACY_TRANSPARENT_TEXTURE) {
				return legacyFlatResourceColor(color, normalizedLight);
			}
			return legacyTextureShadeColor(color, legacyTextureShadeBand(normalizedLight));
		}

		private int chunkLegacyLight(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int vertex,
			int[] diffuseLights) {
			int triangle = vertex / 3;
			Renderer3DModelKind modelKind = triangle >= 0 && triangle < chunk.getTriangleCount()
				? chunk.getTriangleModelKind(triangle)
				: Renderer3DModelKind.UNCLASSIFIED;
			int vertexLightOther = clampLegacyLight(chunk.getVertexLight(vertex));
			int diffuseLight = diffuseLights == null || vertex < 0 || vertex >= diffuseLights.length
				? 0
				: diffuseLights[vertex];
			if (modelKind == Renderer3DModelKind.GAME_OBJECT
				|| modelKind == Renderer3DModelKind.WALL_OBJECT) {
				return applyLightingModeLegacyLight(vertexLightOther);
			}
			return applyLightingModeLegacyLight(96 + vertexLightOther + diffuseLight);
		}

		private int applyLightingModeLegacyLight(int legacyLight) {
			return clampLegacyLight(legacyLight);
		}

		private int chunkFaceDiffuseLight(Renderer3DWorldChunkFrame.ChunkMesh chunk, int triangle) {
			int sourceIndex = triangle * 3;
			if (triangle < 0 || sourceIndex + 2 >= chunk.getIndexCount()) {
				return 0;
			}
			int first = chunk.getIndex(sourceIndex);
			if (!isChunkVertexIndexValid(chunk, first, chunk.getVertexCount())) {
				return 0;
			}
			int scaledNormalX = chunk.getVertexNormalX(first);
			int scaledNormalY = chunk.getVertexNormalY(first);
			int scaledNormalZ = chunk.getVertexNormalZ(first);
			if (scaledNormalX == 0 && scaledNormalY == 0 && scaledNormalZ == 0) {
				return 0;
			}
			return (int) ((scaledNormalY * -10.0d + scaledNormalX * -50.0d + scaledNormalZ * -50.0d) / 106.0d);
		}

		private int legacyFlatResourceColor(int color, int legacyLight) {
			int shade = 255 - legacyLight;
			int shadeSquared = shade * shade;
			int red = (((color >> 16) & 0xFF) * shadeSquared) / 65536;
			int green = (((color >> 8) & 0xFF) * shadeSquared) / 65536;
			int blue = ((color & 0xFF) * shadeSquared) / 65536;
			return red << 16 | green << 8 | blue;
		}

		private int legacyTextureShadeColor(int color, int shadeBand) {
			switch (shadeBand) {
				case 1:
					return (color - (color >>> 3)) & 0xFFFFFF;
				case 2:
					return (color - (color >>> 2)) & 0xFFFFFF;
				case 3:
					return (color - (color >>> 3) - (color >>> 2)) & 0xFFFFFF;
				default:
					return color;
			}
		}

		private void putChunkTextureLight(float light) {
			vertexUploadBuffer.put(light);
			vertexUploadBuffer.put(light);
			vertexUploadBuffer.put(light);
			vertexUploadBuffer.put(1.0f);
		}

		private float legacyTextureLightFactor(int legacyLight) {
			switch (legacyTextureShadeBand(legacyLight)) {
				case 1:
					return 216.0f / 248.0f;
				case 2:
					return 184.0f / 248.0f;
				case 3:
					return 152.0f / 248.0f;
				default:
					return 1.0f;
			}
		}

		private float textureLightFactor(int legacyLight) {
			return legacyTextureLightFactor(legacyLight);
		}

		private int legacyTextureShadeBand(int legacyLight) {
			return clampLegacyLight(legacyLight) >> 6;
		}

		private int clampLegacyLight(int legacyLight) {
			if (legacyLight < 0) {
				return 0;
			}
			if (legacyLight > 255) {
				return 255;
			}
			return legacyLight;
		}

		private static final class ChunkVertexKey {
			private final int x;
			private final int y;
			private final int z;

			private ChunkVertexKey(int x, int y, int z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}

			private static ChunkVertexKey from(Renderer3DWorldChunkFrame.ChunkMesh chunk, int vertex) {
				int coord = vertex * POSITION_COMPONENT_COUNT;
				return new ChunkVertexKey(
					chunk.getVertexCoord(coord),
					chunk.getVertexCoord(coord + 1),
					chunk.getVertexCoord(coord + 2));
			}

			@Override
			public boolean equals(Object other) {
				if (this == other) {
					return true;
				}
				if (!(other instanceof ChunkVertexKey)) {
					return false;
				}
				ChunkVertexKey key = (ChunkVertexKey) other;
				return x == key.x && y == key.y && z == key.z;
			}

			@Override
			public int hashCode() {
				int result = x;
				result = 31 * result + y;
				result = 31 * result + z;
				return result;
			}
		}

		private static final class SmoothDiffuseAccumulator {
			private int total;
			private int count;

			private void add(int diffuse) {
				total += diffuse;
				count++;
			}

			private int average() {
				return count <= 0 ? 0 : total / count;
			}
		}

		private void copyChunkIndices(Renderer3DWorldChunkFrame.ChunkMesh chunk, int indexCount) {
			indexUploadBuffer.clear();
			for (int index = 0; index < indexCount; index++) {
				indexUploadBuffer.put(chunk.getIndex(index));
			}
			indexUploadBuffer.flip();
		}

		private int evictOverflow() throws Exception {
			int evictedChunks = 0;
			Iterator<Map.Entry<WorldChunkBufferKey, WorldChunkBuffer>> iterator = residentChunks.entrySet().iterator();
			while (residentChunks.size() > MAX_RESIDENT_CHUNKS && iterator.hasNext()) {
				Map.Entry<WorldChunkBufferKey, WorldChunkBuffer> eldest = iterator.next();
				eldest.getValue().close(gl);
				iterator.remove();
				evictedChunks++;
			}
			return evictedChunks;
		}

		@Override
		public void close() throws Exception {
			if (closed) {
				return;
			}
			closed = true;
			for (WorldChunkBuffer buffer : residentChunks.values()) {
				buffer.close(gl);
			}
			residentChunks.clear();
			if (remasterShadowMaskTextureId != 0) {
				gl.glDeleteTextures(remasterShadowMaskTextureId);
				remasterShadowMaskTextureId = 0;
				remasterShadowMaskTextureWidth = 0;
				remasterShadowMaskTextureHeight = 0;
				remasterShadowMaskUploadedSignature = 0L;
			}
			remasterShadowMaskCache = null;
			remasterShadowMaskCacheSignature = 0L;
			remasterShadowMaskLastCacheHit = false;
			remasterShadowMaskLastRebuild = false;
			remasterShadowMaskLastUpload = false;
			remasterShadowMaskLastUploadSkip = false;
		}
	}

	private static final class RemasterShadowInventory {
		private static final RemasterShadowInventory EMPTY =
			new RemasterShadowInventory(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		private final int receiverChunks;
		private final int receiverTriangles;
		private final int totalCasters;
		private final int wallCasters;
		private final int gameObjectCasters;
		private final int wallObjectCasters;
		private final int outdoorOnlyCasters;
		private final int clippingCandidates;
		private final int roofedReceivers;
		private final int outdoorReceivers;
		private final int unknownReceivers;
		private final int roofedCasters;
		private final int outdoorCasters;
		private final int unknownCasters;
		private final int sunlightEligibleCasters;
		private final int sunlightSuppressedRoofedCasters;
		private final int sunlightSuppressedUnknownCasters;

		private RemasterShadowInventory(
			int receiverChunks,
			int receiverTriangles,
			int totalCasters,
			int wallCasters,
			int gameObjectCasters,
			int wallObjectCasters,
			int outdoorOnlyCasters,
			int clippingCandidates,
			int roofedReceivers,
			int outdoorReceivers,
			int unknownReceivers,
			int roofedCasters,
			int outdoorCasters,
			int unknownCasters,
			int sunlightEligibleCasters,
			int sunlightSuppressedRoofedCasters,
			int sunlightSuppressedUnknownCasters) {
			this.receiverChunks = receiverChunks;
			this.receiverTriangles = receiverTriangles;
			this.totalCasters = totalCasters;
			this.wallCasters = wallCasters;
			this.gameObjectCasters = gameObjectCasters;
			this.wallObjectCasters = wallObjectCasters;
			this.outdoorOnlyCasters = outdoorOnlyCasters;
			this.clippingCandidates = clippingCandidates;
			this.roofedReceivers = roofedReceivers;
			this.outdoorReceivers = outdoorReceivers;
			this.unknownReceivers = unknownReceivers;
			this.roofedCasters = roofedCasters;
			this.outdoorCasters = outdoorCasters;
			this.unknownCasters = unknownCasters;
			this.sunlightEligibleCasters = sunlightEligibleCasters;
			this.sunlightSuppressedRoofedCasters = sunlightSuppressedRoofedCasters;
			this.sunlightSuppressedUnknownCasters = sunlightSuppressedUnknownCasters;
		}
	}

	private static final class RemasterShadowRoofCoverage {
		private static final RemasterShadowRoofCoverage EMPTY =
			new RemasterShadowRoofCoverage(
				Collections.<Renderer3DWorldChunkFrame.ChunkMesh>emptyList(),
				Collections.<Integer, RemasterShadowIndoorFlood>emptyMap());
		private static final int TILE_SIZE = 128;
		private static final int BOUNDARY_SAMPLE_OFFSET = TILE_SIZE / 2;

		private final List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks;
		private final Map<Integer, RemasterShadowIndoorFlood> indoorFloodByPlane;

		private RemasterShadowRoofCoverage(
			List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks,
			Map<Integer, RemasterShadowIndoorFlood> indoorFloodByPlane) {
			this.roofChunks = roofChunks == null
				? Collections.<Renderer3DWorldChunkFrame.ChunkMesh>emptyList()
				: roofChunks;
			this.indoorFloodByPlane = indoorFloodByPlane == null
				? Collections.<Integer, RemasterShadowIndoorFlood>emptyMap()
				: indoorFloodByPlane;
		}

		private static RemasterShadowRoofCoverage from(Renderer3DWorldChunkFrame chunkFrame) {
			if (chunkFrame == null || chunkFrame.getChunkCount() <= 0) {
				return EMPTY;
			}
			List<Renderer3DWorldChunkFrame.ChunkMesh> roofChunks =
				new ArrayList<Renderer3DWorldChunkFrame.ChunkMesh>();
			Map<Integer, RemasterShadowIndoorFlood.Builder> floodBuilders =
				new HashMap<Integer, RemasterShadowIndoorFlood.Builder>();
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				if (chunk.hasRoofCoverageData()) {
					roofChunks.add(chunk);
				}
				RemasterShadowIndoorFlood.Builder builder = floodBuilderForPlane(floodBuilders, chunk.getPlane());
				builder.addTerrain(chunk);
			}
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				RemasterShadowIndoorFlood.Builder builder = floodBuilders.get(Integer.valueOf(chunk.getPlane()));
				if (builder == null) {
					continue;
				}
				builder.addWallBlockers(chunk);
			}
			Map<Integer, RemasterShadowIndoorFlood> floodByPlane =
				new HashMap<Integer, RemasterShadowIndoorFlood>();
			for (Map.Entry<Integer, RemasterShadowIndoorFlood.Builder> entry : floodBuilders.entrySet()) {
				RemasterShadowIndoorFlood flood = entry.getValue().build();
				if (flood != null) {
					floodByPlane.put(entry.getKey(), flood);
				}
			}
			return roofChunks.isEmpty() && floodByPlane.isEmpty()
				? EMPTY
				: new RemasterShadowRoofCoverage(roofChunks, floodByPlane);
		}

		private int classify(int plane, int worldX, int worldZ) {
			int roofClassification = -1;
			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : roofChunks) {
				if (chunk.getPlane() != plane) {
					continue;
				}
				int classification = chunk.roofClassificationForWorldPoint(worldX, worldZ);
				if (classification > 0) {
					return 1;
				}
				if (classification == 0) {
					roofClassification = 0;
				}
			}
			RemasterShadowIndoorFlood flood = indoorFloodByPlane.get(Integer.valueOf(plane));
			if (flood != null) {
				int classification = flood.classify(worldX, worldZ);
				if (classification >= 0) {
					return classification;
				}
			}
			return roofClassification;
		}

		private int classifyBoundaryCaster(int plane, int x0, int z0, int x1, int z1) {
			int midX = (x0 + x1) / 2;
			int midZ = (z0 + z1) / 2;
			int dx = x1 - x0;
			int dz = z1 - z0;
			double length = Math.sqrt((double) dx * dx + (double) dz * dz);
			if (length < 1.0d) {
				return classify(plane, midX, midZ);
			}

			int offsetX = (int) Math.round((-dz / length) * BOUNDARY_SAMPLE_OFFSET);
			int offsetZ = (int) Math.round((dx / length) * BOUNDARY_SAMPLE_OFFSET);
			int sideA = classify(plane, midX + offsetX, midZ + offsetZ);
			int sideB = classify(plane, midX - offsetX, midZ - offsetZ);
			if (sideA == 0 || sideB == 0) {
				return 0;
			}
			if (sideA > 0 && sideB > 0) {
				return 1;
			}
			if (sideA > 0 || sideB > 0) {
				return 1;
			}
			return classify(plane, midX, midZ);
		}

		private boolean crossesShadowBlocker(int plane, float startX, float startZ, float endX, float endZ) {
			RemasterShadowIndoorFlood flood = indoorFloodByPlane.get(Integer.valueOf(plane));
			return flood != null && flood.crossesBlocker(startX, startZ, endX, endZ);
		}

		private static RemasterShadowIndoorFlood.Builder floodBuilderForPlane(
			Map<Integer, RemasterShadowIndoorFlood.Builder> floodBuilders,
			int plane) {
			Integer key = Integer.valueOf(plane);
			RemasterShadowIndoorFlood.Builder builder = floodBuilders.get(key);
			if (builder == null) {
				builder = new RemasterShadowIndoorFlood.Builder(plane);
				floodBuilders.put(key, builder);
			}
			return builder;
		}

		private static int tileForWorld(int world) {
			return Math.floorDiv(world, TILE_SIZE);
		}

		private static int boundaryForWorld(int world) {
			return Math.round(world / (float) TILE_SIZE);
		}
	}

	private static final class RemasterShadowIndoorFlood {
		private static final int BOUNDS_PADDING_TILES = 2;

		private final int minTileX;
		private final int minTileZ;
		private final int width;
		private final int height;
		private final boolean[] outdoorTiles;
		private final boolean[] blockEast;
		private final boolean[] blockSouth;
		private final Map<Long, List<WallEdge>> wallEdgesByTile;

		private RemasterShadowIndoorFlood(
			int minTileX,
			int minTileZ,
			int width,
			int height,
			boolean[] outdoorTiles,
			boolean[] blockEast,
			boolean[] blockSouth,
			Map<Long, List<WallEdge>> wallEdgesByTile) {
			this.minTileX = minTileX;
			this.minTileZ = minTileZ;
			this.width = width;
			this.height = height;
			this.outdoorTiles = outdoorTiles;
			this.blockEast = blockEast == null ? new boolean[0] : blockEast;
			this.blockSouth = blockSouth == null ? new boolean[0] : blockSouth;
			this.wallEdgesByTile = wallEdgesByTile == null
				? Collections.<Long, List<WallEdge>>emptyMap()
				: wallEdgesByTile;
		}

		private int classify(int worldX, int worldZ) {
			int tileX = RemasterShadowRoofCoverage.tileForWorld(worldX);
			int tileZ = RemasterShadowRoofCoverage.tileForWorld(worldZ);
			int localX = tileX - minTileX;
			int localZ = tileZ - minTileZ;
			if (localX < 0 || localZ < 0 || localX >= width || localZ >= height) {
				return -1;
			}
			return outdoorTiles[index(localX, localZ, width)] ? 0 : 1;
		}

		private boolean crossesBlocker(float worldX0, float worldZ0, float worldX1, float worldZ1) {
			float dx = worldX1 - worldX0;
			float dz = worldZ1 - worldZ0;
			float distance = (float) Math.sqrt(dx * dx + dz * dz);
			if (distance < 1.0f) {
				return false;
			}
			int steps = Math.max(
				1,
				(int) Math.ceil(distance / Math.max(16.0f, RemasterShadowRoofCoverage.TILE_SIZE * 0.25f)));
			int lastX = localTileX(worldX0);
			int lastZ = localTileZ(worldZ0);
			float lastWorldX = worldX0;
			float lastWorldZ = worldZ0;
			for (int step = 1; step <= steps; step++) {
				float t = step / (float) steps;
				float nextWorldX = worldX0 + dx * t;
				float nextWorldZ = worldZ0 + dz * t;
				int nextX = localTileX(nextWorldX);
				int nextZ = localTileZ(nextWorldZ);
				if (OpenGLWorldChunkRenderer.REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP
					&& crossesDirectWallEdge(lastWorldX, lastWorldZ, nextWorldX, nextWorldZ, lastX, lastZ, nextX, nextZ)) {
					return true;
				}
				if (crossesBlockedTransition(lastX, lastZ, nextX, nextZ)) {
					return true;
				}
				lastX = nextX;
				lastZ = nextZ;
				lastWorldX = nextWorldX;
				lastWorldZ = nextWorldZ;
			}
			return false;
		}

		private int localTileX(float worldX) {
			return RemasterShadowRoofCoverage.tileForWorld((int) Math.floor(worldX)) - minTileX;
		}

		private int localTileZ(float worldZ) {
			return RemasterShadowRoofCoverage.tileForWorld((int) Math.floor(worldZ)) - minTileZ;
		}

		private boolean crossesBlockedTransition(int x, int z, int nextX, int nextZ) {
			if (x == nextX && z == nextZ) {
				return false;
			}
			if (nextX > x) {
				for (int boundaryX = x; boundaryX < nextX; boundaryX++) {
					if (isVerticalBlocked(boundaryX, z) || isVerticalBlocked(boundaryX, nextZ)) {
						return true;
					}
				}
			} else if (nextX < x) {
				for (int boundaryX = nextX; boundaryX < x; boundaryX++) {
					if (isVerticalBlocked(boundaryX, z) || isVerticalBlocked(boundaryX, nextZ)) {
						return true;
					}
				}
			}
			if (nextZ > z) {
				for (int boundaryZ = z; boundaryZ < nextZ; boundaryZ++) {
					if (isHorizontalBlocked(x, boundaryZ) || isHorizontalBlocked(nextX, boundaryZ)) {
						return true;
					}
				}
			} else if (nextZ < z) {
				for (int boundaryZ = nextZ; boundaryZ < z; boundaryZ++) {
					if (isHorizontalBlocked(x, boundaryZ) || isHorizontalBlocked(nextX, boundaryZ)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean crossesDirectWallEdge(
			float worldX0,
			float worldZ0,
			float worldX1,
			float worldZ1,
			int localX0,
			int localZ0,
			int localX1,
			int localZ1) {
			int minX = Math.min(localX0, localX1) - 1;
			int maxX = Math.max(localX0, localX1) + 1;
			int minZ = Math.min(localZ0, localZ1) - 1;
			int maxZ = Math.max(localZ0, localZ1) + 1;
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					List<WallEdge> edges = wallEdgesByTile.get(tileKey(x, z));
					if (edges == null) {
						continue;
					}
					for (WallEdge edge : edges) {
						if (edge.intersectsSegment(worldX0, worldZ0, worldX1, worldZ1)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private boolean isVerticalBlocked(int leftTileX, int tileZ) {
			if (leftTileX < 0 || leftTileX >= width - 1 || tileZ < 0 || tileZ >= height) {
				return false;
			}
			int index = tileZ * (width - 1) + leftTileX;
			return index >= 0 && index < blockEast.length && blockEast[index];
		}

		private boolean isHorizontalBlocked(int tileX, int topTileZ) {
			if (tileX < 0 || tileX >= width || topTileZ < 0 || topTileZ >= height - 1) {
				return false;
			}
			int index = topTileZ * width + tileX;
			return index >= 0 && index < blockSouth.length && blockSouth[index];
		}

		private static int index(int x, int z, int width) {
			return z * width + x;
		}

		private static final class Builder {
			private final int plane;
			private final List<WallEdge> wallEdges = new ArrayList<WallEdge>();
			private int minTileX = Integer.MAX_VALUE;
			private int maxTileX = Integer.MIN_VALUE;
			private int minTileZ = Integer.MAX_VALUE;
			private int maxTileZ = Integer.MIN_VALUE;

			private Builder(int plane) {
				this.plane = plane;
			}

			private void addTerrain(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
				if (chunk == null || chunk.getPlane() != plane) {
					return;
				}
				int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
				for (int triangle = 0; triangle < triangleCount; triangle++) {
					if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
						continue;
					}
					int sourceIndex = triangle * 3;
					for (int corner = 0; corner < 3; corner++) {
						int vertex = chunk.getIndex(sourceIndex + corner);
						int coord = vertex * 3;
						addTile(
							RemasterShadowRoofCoverage.tileForWorld(chunk.getVertexCoord(coord)),
							RemasterShadowRoofCoverage.tileForWorld(chunk.getVertexCoord(coord + 2)));
					}
				}
			}

			private void addWallBlockers(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
				if (chunk == null || chunk.getPlane() != plane) {
					return;
				}
				for (int index = 0; index < chunk.getShadowCasterCount(); index++) {
					Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
					if (caster == null || caster.getModelKind() != Renderer3DModelKind.WALL) {
						continue;
					}
					wallEdges.add(new WallEdge(
						caster.getBaseX0(),
						caster.getBaseZ0(),
						caster.getBaseX1(),
						caster.getBaseZ1()));
				}
			}

			private RemasterShadowIndoorFlood build() {
				if (minTileX == Integer.MAX_VALUE || minTileZ == Integer.MAX_VALUE) {
					return null;
				}
				int paddedMinTileX = minTileX - BOUNDS_PADDING_TILES;
				int paddedMinTileZ = minTileZ - BOUNDS_PADDING_TILES;
				int paddedMaxTileX = maxTileX + BOUNDS_PADDING_TILES;
				int paddedMaxTileZ = maxTileZ + BOUNDS_PADDING_TILES;
				int width = Math.max(1, paddedMaxTileX - paddedMinTileX + 1);
				int height = Math.max(1, paddedMaxTileZ - paddedMinTileZ + 1);
				boolean[] blockEast = new boolean[Math.max(0, width - 1) * height];
				boolean[] blockSouth = new boolean[width * Math.max(0, height - 1)];
				for (WallEdge edge : wallEdges) {
					edge.addBlockers(paddedMinTileX, paddedMinTileZ, width, height, blockEast, blockSouth);
				}
				closeSingleTileWallGaps(width, height, blockEast, blockSouth);
				boolean[] outdoor = floodOutdoor(width, height, blockEast, blockSouth);
				Map<Long, List<WallEdge>> wallEdgesByTile =
					OpenGLWorldChunkRenderer.REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP
					? buildWallEdgeGrid(paddedMinTileX, paddedMinTileZ, width, height, wallEdges)
					: Collections.<Long, List<WallEdge>>emptyMap();
				return new RemasterShadowIndoorFlood(
					paddedMinTileX,
					paddedMinTileZ,
					width,
					height,
					outdoor,
					blockEast,
					blockSouth,
					wallEdgesByTile);
			}

			private void addTile(int tileX, int tileZ) {
				minTileX = Math.min(minTileX, tileX);
				maxTileX = Math.max(maxTileX, tileX);
				minTileZ = Math.min(minTileZ, tileZ);
				maxTileZ = Math.max(maxTileZ, tileZ);
			}

			private static void closeSingleTileWallGaps(
				int width,
				int height,
				boolean[] blockEast,
				boolean[] blockSouth) {
				boolean[] originalEast = blockEast.clone();
				for (int x = 0; x < width - 1; x++) {
					for (int z = 1; z < height - 1; z++) {
						int index = z * (width - 1) + x;
						if (!originalEast[index]
							&& originalEast[(z - 1) * (width - 1) + x]
							&& originalEast[(z + 1) * (width - 1) + x]) {
							blockEast[index] = true;
						}
					}
				}
				boolean[] originalSouth = blockSouth.clone();
				for (int z = 0; z < height - 1; z++) {
					for (int x = 1; x < width - 1; x++) {
						int index = z * width + x;
						if (!originalSouth[index]
							&& originalSouth[z * width + x - 1]
							&& originalSouth[z * width + x + 1]) {
							blockSouth[index] = true;
						}
					}
				}
			}

			private static boolean[] floodOutdoor(
				int width,
				int height,
				boolean[] blockEast,
				boolean[] blockSouth) {
				boolean[] outdoor = new boolean[width * height];
				ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
				for (int x = 0; x < width; x++) {
					addFloodSeed(x, 0, width, height, outdoor, queue);
					addFloodSeed(x, height - 1, width, height, outdoor, queue);
				}
				for (int z = 1; z < height - 1; z++) {
					addFloodSeed(0, z, width, height, outdoor, queue);
					addFloodSeed(width - 1, z, width, height, outdoor, queue);
				}
				while (!queue.isEmpty()) {
					int packed = queue.removeFirst().intValue();
					int x = packed & 0xffff;
					int z = packed >>> 16;
					addFloodNeighbor(x, z, x + 1, z, width, height, blockEast, blockSouth, outdoor, queue);
					addFloodNeighbor(x, z, x - 1, z, width, height, blockEast, blockSouth, outdoor, queue);
					addFloodNeighbor(x, z, x, z + 1, width, height, blockEast, blockSouth, outdoor, queue);
					addFloodNeighbor(x, z, x, z - 1, width, height, blockEast, blockSouth, outdoor, queue);
				}
				return outdoor;
			}

			private static Map<Long, List<WallEdge>> buildWallEdgeGrid(
				int minTileX,
				int minTileZ,
				int width,
				int height,
				List<WallEdge> wallEdges) {
				if (wallEdges == null || wallEdges.isEmpty()) {
					return Collections.emptyMap();
				}
				Map<Long, List<WallEdge>> grid = new HashMap<Long, List<WallEdge>>();
				for (WallEdge edge : wallEdges) {
					edge.addToGrid(minTileX, minTileZ, width, height, grid);
				}
				return grid;
			}

			private static void addFloodSeed(
				int x,
				int z,
				int width,
				int height,
				boolean[] outdoor,
				ArrayDeque<Integer> queue) {
				if (x < 0 || z < 0 || x >= width || z >= height) {
					return;
				}
				int index = index(x, z, width);
				if (outdoor[index]) {
					return;
				}
				outdoor[index] = true;
				queue.add(Integer.valueOf((z << 16) | x));
			}

			private static void addFloodNeighbor(
				int x,
				int z,
				int nextX,
				int nextZ,
				int width,
				int height,
				boolean[] blockEast,
				boolean[] blockSouth,
				boolean[] outdoor,
				ArrayDeque<Integer> queue) {
				if (nextX < 0 || nextZ < 0 || nextX >= width || nextZ >= height) {
					return;
				}
				if (isBlocked(x, z, nextX, nextZ, width, blockEast, blockSouth)) {
					return;
				}
				int index = index(nextX, nextZ, width);
				if (outdoor[index]) {
					return;
				}
				outdoor[index] = true;
				queue.add(Integer.valueOf((nextZ << 16) | nextX));
			}

			private static boolean isBlocked(
				int x,
				int z,
				int nextX,
				int nextZ,
				int width,
				boolean[] blockEast,
				boolean[] blockSouth) {
				if (nextX == x + 1) {
					return blockEast[z * (width - 1) + x];
				}
				if (nextX == x - 1) {
					return blockEast[z * (width - 1) + nextX];
				}
				if (nextZ == z + 1) {
					return blockSouth[z * width + x];
				}
				if (nextZ == z - 1) {
					return blockSouth[nextZ * width + x];
				}
				return false;
			}
		}

		private static final class WallEdge {
			private static final float INTERSECTION_EPSILON = 0.001f;

			private final int x0;
			private final int z0;
			private final int x1;
			private final int z1;

			private WallEdge(int x0, int z0, int x1, int z1) {
				this.x0 = x0;
				this.z0 = z0;
				this.x1 = x1;
				this.z1 = z1;
			}

			private void addBlockers(
				int minTileX,
				int minTileZ,
				int width,
				int height,
				boolean[] blockEast,
				boolean[] blockSouth) {
				int dx = Math.abs(x1 - x0);
				int dz = Math.abs(z1 - z0);
				if (dx <= 8 && dz > 0) {
					int boundaryX = RemasterShadowRoofCoverage.boundaryForWorld((x0 + x1) / 2);
					int startTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.min(z0, z1));
					int endTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.max(z0, z1) - 1);
					for (int tileZ = startTileZ; tileZ <= endTileZ; tileZ++) {
						addVerticalBlock(boundaryX, tileZ, minTileX, minTileZ, width, height, blockEast);
					}
				} else if (dz <= 8 && dx > 0) {
					int boundaryZ = RemasterShadowRoofCoverage.boundaryForWorld((z0 + z1) / 2);
					int startTileX = RemasterShadowRoofCoverage.tileForWorld(Math.min(x0, x1));
					int endTileX = RemasterShadowRoofCoverage.tileForWorld(Math.max(x0, x1) - 1);
					for (int tileX = startTileX; tileX <= endTileX; tileX++) {
						addHorizontalBlock(tileX, boundaryZ, minTileX, minTileZ, width, height, blockSouth);
					}
				}
			}

			private void addToGrid(
				int minTileX,
				int minTileZ,
				int width,
				int height,
				Map<Long, List<WallEdge>> grid) {
				int startTileX = RemasterShadowRoofCoverage.tileForWorld(Math.min(x0, x1)) - minTileX - 1;
				int endTileX = RemasterShadowRoofCoverage.tileForWorld(Math.max(x0, x1)) - minTileX + 1;
				int startTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.min(z0, z1)) - minTileZ - 1;
				int endTileZ = RemasterShadowRoofCoverage.tileForWorld(Math.max(z0, z1)) - minTileZ + 1;
				for (int z = startTileZ; z <= endTileZ; z++) {
					if (z < 0 || z >= height) {
						continue;
					}
					for (int x = startTileX; x <= endTileX; x++) {
						if (x < 0 || x >= width) {
							continue;
						}
						long key = tileKey(x, z);
						List<WallEdge> edges = grid.get(Long.valueOf(key));
						if (edges == null) {
							edges = new ArrayList<WallEdge>();
							grid.put(Long.valueOf(key), edges);
						}
						edges.add(this);
					}
				}
			}

			private boolean intersectsSegment(float segmentX0, float segmentZ0, float segmentX1, float segmentZ1) {
				float dx = segmentX1 - segmentX0;
				float dz = segmentZ1 - segmentZ0;
				float edgeDx = x1 - x0;
				float edgeDz = z1 - z0;
				float denominator = cross(dx, dz, edgeDx, edgeDz);
				if (Math.abs(denominator) <= INTERSECTION_EPSILON) {
					return false;
				}
				float offsetX = x0 - segmentX0;
				float offsetZ = z0 - segmentZ0;
				float segmentT = cross(offsetX, offsetZ, edgeDx, edgeDz) / denominator;
				float edgeT = cross(offsetX, offsetZ, dx, dz) / denominator;
				return segmentT > INTERSECTION_EPSILON
					&& segmentT < 1.0f - INTERSECTION_EPSILON
					&& edgeT > INTERSECTION_EPSILON
					&& edgeT < 1.0f - INTERSECTION_EPSILON;
			}

			private static float cross(float ax, float az, float bx, float bz) {
				return ax * bz - az * bx;
			}

			private static void addVerticalBlock(
				int boundaryX,
				int tileZ,
				int minTileX,
				int minTileZ,
				int width,
				int height,
				boolean[] blockEast) {
				int localBoundaryX = boundaryX - minTileX;
				int localZ = tileZ - minTileZ;
				if (localBoundaryX <= 0 || localBoundaryX >= width || localZ < 0 || localZ >= height) {
					return;
				}
				blockEast[localZ * (width - 1) + localBoundaryX - 1] = true;
			}

			private static void addHorizontalBlock(
				int tileX,
				int boundaryZ,
				int minTileX,
				int minTileZ,
				int width,
				int height,
				boolean[] blockSouth) {
				int localX = tileX - minTileX;
				int localBoundaryZ = boundaryZ - minTileZ;
				if (localX < 0 || localX >= width || localBoundaryZ <= 0 || localBoundaryZ >= height) {
					return;
				}
				blockSouth[(localBoundaryZ - 1) * width + localX] = true;
			}
		}

		private static long tileKey(int localX, int localZ) {
			return ((long) localX << 32) ^ (localZ & 0xffffffffL);
		}
	}

	private static final class OpenGLWorldChunkUploadStats {
		private static final OpenGLWorldChunkUploadStats EMPTY =
			new OpenGLWorldChunkUploadStats(0, 0, 0, 0);

		private final int requestedChunks;
		private final int uploadedChunks;
		private final int reusedChunks;
		private final int evictedChunks;

		private OpenGLWorldChunkUploadStats(
			int requestedChunks,
			int uploadedChunks,
			int reusedChunks,
			int evictedChunks) {
			this.requestedChunks = requestedChunks;
			this.uploadedChunks = uploadedChunks;
			this.reusedChunks = reusedChunks;
			this.evictedChunks = evictedChunks;
		}
	}

	private static final class ResidentChunkReadiness {
		private final boolean canReplace;
		private final String reason;
		private final int drawableTerrainBatches;
		private final int drawableWallBatches;
		private final int drawableRoofBatches;

		private ResidentChunkReadiness(
			boolean canReplace,
			String reason,
			int drawableTerrainBatches,
			int drawableWallBatches,
			int drawableRoofBatches) {
			this.canReplace = canReplace;
			this.reason = reason == null || reason.trim().isEmpty() ? "unknown" : reason;
			this.drawableTerrainBatches = Math.max(0, drawableTerrainBatches);
			this.drawableWallBatches = Math.max(0, drawableWallBatches);
			this.drawableRoofBatches = Math.max(0, drawableRoofBatches);
		}

		private static ResidentChunkReadiness notRequested(String reason) {
			return new ResidentChunkReadiness(false, reason, 0, 0, 0);
		}
	}

	private static final class OpenGLWorldChunkDrawStats {
		private static final OpenGLWorldChunkDrawStats EMPTY =
			new OpenGLWorldChunkDrawStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

		private final int consideredChunks;
		private final int drawnChunks;
		private final int culledChunks;
		private final int drawnTriangles;
		private final int drawnTerrainTriangles;
		private final int drawnWallTriangles;
		private final int drawnRoofTriangles;
		private final int drawnGameObjectTriangles;
		private final int drawnWallObjectTriangles;
		private final int drawnOtherTriangles;
		private final int fallbackTriangles;
		private final int skippedTriangles;
		private final int consideredBatches;
		private final int drawnBatches;
		private final int culledBatches;
		private final int drawCalls;
		private final int textureBinds;
		private final int shadowProofChunks;
		private final int shadowProofIndices;

		private OpenGLWorldChunkDrawStats(
			int consideredChunks,
			int drawnChunks,
			int culledChunks,
			int drawnTriangles,
			int drawnTerrainTriangles,
			int drawnWallTriangles,
			int drawnRoofTriangles,
			int drawnGameObjectTriangles,
			int drawnWallObjectTriangles,
			int drawnOtherTriangles,
			int fallbackTriangles,
			int skippedTriangles,
			int consideredBatches,
			int drawnBatches,
			int culledBatches,
			int drawCalls,
			int textureBinds,
			int shadowProofChunks,
			int shadowProofIndices) {
			this.consideredChunks = consideredChunks;
			this.drawnChunks = drawnChunks;
			this.culledChunks = culledChunks;
			this.drawnTriangles = drawnTriangles;
			this.drawnTerrainTriangles = drawnTerrainTriangles;
			this.drawnWallTriangles = drawnWallTriangles;
			this.drawnRoofTriangles = drawnRoofTriangles;
			this.drawnGameObjectTriangles = drawnGameObjectTriangles;
			this.drawnWallObjectTriangles = drawnWallObjectTriangles;
			this.drawnOtherTriangles = drawnOtherTriangles;
			this.fallbackTriangles = fallbackTriangles;
			this.skippedTriangles = skippedTriangles;
			this.consideredBatches = consideredBatches;
			this.drawnBatches = drawnBatches;
			this.culledBatches = culledBatches;
			this.drawCalls = drawCalls;
			this.textureBinds = textureBinds;
			this.shadowProofChunks = shadowProofChunks;
			this.shadowProofIndices = shadowProofIndices;
		}
	}

	private enum WorldChunkBatchBindResult {
		TEXTURED,
		FLAT_FALLBACK,
		SKIPPED
	}

	private static final class WorldChunkDrawAccumulator {
		private final Set<WorldChunkBufferKey> consideredChunkKeys = new HashSet<WorldChunkBufferKey>();
		private final Set<WorldChunkBufferKey> drawnChunkKeys = new HashSet<WorldChunkBufferKey>();
		private int drawnTriangles;
		private int drawnTerrainTriangles;
		private int drawnWallTriangles;
		private int drawnRoofTriangles;
		private int drawnGameObjectTriangles;
		private int drawnWallObjectTriangles;
		private int drawnOtherTriangles;
		private int fallbackTriangles;
		private int skippedTriangles;
		private int consideredBatches;
		private int drawnBatches;
		private int culledBatches;
		private int drawCalls;
		private int textureBinds;
		private int boundTextureId = -1;
		private int shadowProofChunks;
		private int shadowProofIndices;

		private void recordConsideredChunk(WorldChunkBufferKey key) {
			consideredChunkKeys.add(key);
		}

		private void recordChunk(WorldChunkBufferKey key) {
			drawnChunkKeys.add(key);
		}

		private int consideredChunkCount() {
			return consideredChunkKeys.size();
		}

		private int drawnChunkCount() {
			return drawnChunkKeys.size();
		}

		private int culledChunkCount() {
			return Math.max(0, consideredChunkCount() - drawnChunkCount());
		}

		private void recordConsideredBatch() {
			consideredBatches++;
		}

		private void recordDrawnBatch() {
			drawnBatches++;
		}

		private void recordCulledBatch() {
			culledBatches++;
		}

		private void recordDrawCall() {
			drawCalls++;
		}

		private void recordTextureBind() {
			textureBinds++;
		}

		private void recordBatch(Renderer3DModelKind kind, int triangles) {
			drawnTriangles += triangles;
			if (kind == Renderer3DModelKind.TERRAIN) {
				drawnTerrainTriangles += triangles;
			} else if (kind == Renderer3DModelKind.WALL) {
				drawnWallTriangles += triangles;
			} else if (kind == Renderer3DModelKind.ROOF) {
				drawnRoofTriangles += triangles;
			} else if (kind == Renderer3DModelKind.GAME_OBJECT) {
				drawnGameObjectTriangles += triangles;
			} else if (kind == Renderer3DModelKind.WALL_OBJECT) {
				drawnWallObjectTriangles += triangles;
			} else {
				drawnOtherTriangles += triangles;
			}
		}

		private void recordFallbackBatch(int triangles) {
			fallbackTriangles += triangles;
		}

		private void recordSkippedBatch(int triangles) {
			skippedTriangles += triangles;
		}

		private void recordShadowProof(int chunks, int indices) {
			shadowProofChunks += chunks;
			shadowProofIndices += indices;
		}
	}

	private static final class WorldChunkMaterialBatch {
		private final Renderer3DModelKind kind;
		private final int textureId;
		private final int fallbackColor;
		private final int startIndex;
		private final int indexCount;
		private final int spatialX;
		private final int spatialZ;
		private final int minX;
		private final int maxX;
		private final int minY;
		private final int maxY;
		private final int minZ;
		private final int maxZ;

		private WorldChunkMaterialBatch(
			Renderer3DModelKind kind,
			int textureId,
			int fallbackColor,
			int startIndex,
			int indexCount,
			int spatialX,
			int spatialZ,
			int minX,
			int maxX,
			int minY,
			int maxY,
			int minZ,
			int maxZ) {
			this.kind = kind == null ? Renderer3DModelKind.UNCLASSIFIED : kind;
			this.textureId = textureId;
			this.fallbackColor = fallbackColor;
			this.startIndex = Math.max(0, startIndex);
			this.indexCount = Math.max(0, indexCount);
			this.spatialX = spatialX;
			this.spatialZ = spatialZ;
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
			this.minZ = minZ;
			this.maxZ = maxZ;
		}

		private boolean hasSpatialBounds() {
			return spatialX != OpenGLWorldChunkRenderer.SPATIAL_BATCH_DISABLED
				&& spatialZ != OpenGLWorldChunkRenderer.SPATIAL_BATCH_DISABLED;
		}

		private boolean hasBounds() {
			return indexCount > 0 && minX <= maxX && minY <= maxY && minZ <= maxZ;
		}
	}

	private static final class WorldChunkMaterialBatchBuilder {
		private final WorldChunkMaterialKey key;
		private int startIndex;
		private int writeIndex;
		private int indexCount;
		private int minX = Integer.MAX_VALUE;
		private int maxX = Integer.MIN_VALUE;
		private int minY = Integer.MAX_VALUE;
		private int maxY = Integer.MIN_VALUE;
		private int minZ = Integer.MAX_VALUE;
		private int maxZ = Integer.MIN_VALUE;

		private WorldChunkMaterialBatchBuilder(WorldChunkMaterialKey key) {
			this.key = key;
		}

		private WorldChunkMaterialBatch toBatch() {
			return new WorldChunkMaterialBatch(
				key.kind,
				key.textureId,
				key.fallbackColor,
				startIndex,
				indexCount,
				key.spatialX,
				key.spatialZ,
				minX == Integer.MAX_VALUE ? 0 : minX,
				maxX == Integer.MIN_VALUE ? 0 : maxX,
				minY == Integer.MAX_VALUE ? 0 : minY,
				maxY == Integer.MIN_VALUE ? 0 : maxY,
				minZ == Integer.MAX_VALUE ? 0 : minZ,
				maxZ == Integer.MIN_VALUE ? 0 : maxZ);
		}

		private void includeTriangleBounds(Renderer3DWorldChunkFrame.ChunkMesh chunk, int triangle) {
			int sourceIndex = triangle * 3;
			for (int index = 0; index < 3; index++) {
				int vertex = chunk.getIndex(sourceIndex + index);
				int coord = vertex * OpenGLWorldChunkRenderer.POSITION_COMPONENT_COUNT;
				int x = chunk.getVertexCoord(coord);
				int y = chunk.getVertexCoord(coord + 1);
				int z = chunk.getVertexCoord(coord + 2);
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
				minZ = Math.min(minZ, z);
				maxZ = Math.max(maxZ, z);
			}
		}
	}

	private static final class WorldChunkMaterialKey {
		private final Renderer3DModelKind kind;
		private final int textureId;
		private final int fallbackColor;
		private final int spatialX;
		private final int spatialZ;

		private WorldChunkMaterialKey(
			Renderer3DModelKind kind,
			int textureId,
			int fallbackColor,
			int spatialX,
			int spatialZ) {
			this.kind = kind == null ? Renderer3DModelKind.UNCLASSIFIED : kind;
			this.textureId = textureId;
			this.fallbackColor = fallbackColor;
			this.spatialX = spatialX;
			this.spatialZ = spatialZ;
		}

		private static WorldChunkMaterialKey from(
			Renderer3DWorldChunkFrame.ChunkMesh chunk,
			int triangle,
			boolean spatialBatches) {
			int spatialX = OpenGLWorldChunkRenderer.SPATIAL_BATCH_DISABLED;
			int spatialZ = OpenGLWorldChunkRenderer.SPATIAL_BATCH_DISABLED;
			if (spatialBatches) {
				int sourceIndex = triangle * 3;
				int xTotal = 0;
				int zTotal = 0;
				for (int index = 0; index < 3; index++) {
					int vertex = chunk.getIndex(sourceIndex + index);
					int coord = vertex * OpenGLWorldChunkRenderer.POSITION_COMPONENT_COUNT;
					xTotal += chunk.getVertexCoord(coord);
					zTotal += chunk.getVertexCoord(coord + 2);
				}
				spatialX = spatialCell(xTotal / 3);
				spatialZ = spatialCell(zTotal / 3);
			}
			return new WorldChunkMaterialKey(
				chunk.getTriangleModelKind(triangle),
				chunk.getTriangleTexture(triangle),
				chunk.getTriangleFallbackColor(triangle),
				spatialX,
				spatialZ);
		}

		private static int spatialCell(int coordinate) {
			int cell = coordinate / OpenGLWorldChunkRenderer.SPATIAL_BATCH_WORLD_SIZE;
			if (cell < 0) {
				return 0;
			}
			if (cell >= OpenGLWorldChunkRenderer.SPATIAL_BATCH_AXIS) {
				return OpenGLWorldChunkRenderer.SPATIAL_BATCH_AXIS - 1;
			}
			return cell;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof WorldChunkMaterialKey)) {
				return false;
			}
			WorldChunkMaterialKey key = (WorldChunkMaterialKey) other;
			return kind == key.kind
				&& textureId == key.textureId
				&& fallbackColor == key.fallbackColor
				&& spatialX == key.spatialX
				&& spatialZ == key.spatialZ;
		}

		@Override
		public int hashCode() {
			int result = kind.hashCode();
			result = 31 * result + textureId;
			result = 31 * result + fallbackColor;
			result = 31 * result + spatialX;
			result = 31 * result + spatialZ;
			return result;
		}
	}

	private static final class WorldChunkBufferKey {
		private final int plane;
		private final int centerSectionX;
		private final int centerSectionY;
		private final int originWorldX;
		private final int originWorldZ;
		private final boolean objectOnly;

		private WorldChunkBufferKey(
			int plane,
			int centerSectionX,
			int centerSectionY,
			int originWorldX,
			int originWorldZ,
			boolean objectOnly) {
			this.plane = plane;
			this.centerSectionX = centerSectionX;
			this.centerSectionY = centerSectionY;
			this.originWorldX = originWorldX;
			this.originWorldZ = originWorldZ;
			this.objectOnly = objectOnly;
		}

		private static WorldChunkBufferKey from(Renderer3DWorldChunkFrame.ChunkMesh chunk) {
			return new WorldChunkBufferKey(
				chunk.getPlane(),
				chunk.getCenterSectionX(),
				chunk.getCenterSectionY(),
				chunk.getOriginWorldX(),
				chunk.getOriginWorldZ(),
				chunk.isObjectChunk());
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof WorldChunkBufferKey)) {
				return false;
			}
			WorldChunkBufferKey key = (WorldChunkBufferKey) other;
			return plane == key.plane
				&& centerSectionX == key.centerSectionX
				&& centerSectionY == key.centerSectionY
				&& originWorldX == key.originWorldX
				&& originWorldZ == key.originWorldZ
				&& objectOnly == key.objectOnly;
		}

		@Override
		public int hashCode() {
			int result = plane;
			result = 31 * result + centerSectionX;
			result = 31 * result + centerSectionY;
			result = 31 * result + originWorldX;
			result = 31 * result + originWorldZ;
			result = 31 * result + (objectOnly ? 1 : 0);
			return result;
		}
	}

	private static final class WorldChunkBuffer {
		private final int vertexBufferId;
		private final int indexBufferId;
		private final int materialIndexBufferId;
		private long signature;
		private int vertexCount;
		private int indexCount;
		private int triangleCount;
		private boolean atlasTextureCoordinates;
		private int brightnessBits;
		private int fogModeBits;
		private int lightingModeBits;
		private int geometryModeBits;
		private boolean replacementCompositeDrawOnly;
		private WorldChunkMaterialBatch[] materialBatches = new WorldChunkMaterialBatch[0];
		private long shadowProofSignature;

		private WorldChunkBuffer(int vertexBufferId, int indexBufferId, int materialIndexBufferId) {
			this.vertexBufferId = vertexBufferId;
			this.indexBufferId = indexBufferId;
			this.materialIndexBufferId = materialIndexBufferId;
		}

		private boolean matches(
			long signature,
			int vertexCount,
			int indexCount,
			int triangleCount,
			boolean atlasTextureCoordinates,
			int brightnessBits,
			int fogModeBits,
			int lightingModeBits,
			int geometryModeBits,
			boolean replacementCompositeDrawOnly,
			long shadowProofSignature) {
			return this.signature == signature
				&& this.vertexCount == vertexCount
				&& this.indexCount == indexCount
				&& this.triangleCount == triangleCount
				&& this.atlasTextureCoordinates == atlasTextureCoordinates
				&& this.brightnessBits == brightnessBits
				&& this.fogModeBits == fogModeBits
				&& this.lightingModeBits == lightingModeBits
				&& this.geometryModeBits == geometryModeBits
				&& this.replacementCompositeDrawOnly == replacementCompositeDrawOnly
				&& this.shadowProofSignature == shadowProofSignature;
		}

		private void close(LwjglBindings gl) throws Exception {
			gl.glDeleteBuffers(vertexBufferId);
			gl.glDeleteBuffers(indexBufferId);
			gl.glDeleteBuffers(materialIndexBufferId);
		}
	}

	private static final class OpenGLWorldMeshRenderer implements AutoCloseable {
		private static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;
		private static final int FLAT_COLOR_BATCH_TEXTURE_ID = -1;
		private static final int UPLOAD_FLOATS_PER_VERTEX = 12;
		private static final int SHADER_UPLOAD_FLOATS_PER_VERTEX = 14;
		private static final int POSITION_COMPONENT_COUNT = 3;
		private static final int MATERIAL_COLOR_COMPONENT_COUNT = 3;
		private static final int TEXTURE_COORD_COMPONENT_COUNT = 2;
		private static final int TEXTURE_LIGHT_COMPONENT_COUNT = 4;
		private static final int MATERIAL_COLOR_OFFSET_BYTES = POSITION_COMPONENT_COUNT * 4;
		private static final int TEXTURE_COORD_OFFSET_BYTES =
			(POSITION_COMPONENT_COUNT + MATERIAL_COLOR_COMPONENT_COUNT) * 4;
		private static final int TEXTURE_LIGHT_OFFSET_BYTES =
			(POSITION_COMPONENT_COUNT + MATERIAL_COLOR_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * 4;
		private static final int STRIDE_BYTES = UPLOAD_FLOATS_PER_VERTEX * 4;
		private static final int SHADER_POSITION_OFFSET_BYTES = 0;
		private static final int SHADER_TEXTURE_COORD_OFFSET_BYTES = POSITION_COMPONENT_COUNT * 4;
		private static final int SHADER_MATERIAL_COLOR_OFFSET_BYTES =
			(POSITION_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * 4;
		private static final int SHADER_LEGACY_LIGHT_OFFSET_BYTES =
			SHADER_MATERIAL_COLOR_OFFSET_BYTES + TEXTURE_LIGHT_COMPONENT_COUNT * 4;
		private static final int SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES =
			SHADER_LEGACY_LIGHT_OFFSET_BYTES + 4;
		private static final int SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES =
			SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES + 4;
		private static final int SHADER_STRIDE_BYTES = SHADER_UPLOAD_FLOATS_PER_VERTEX * 4;
		private static final float WALL_DEPTH_PRIORITY_FACTOR = -1.0f;
		private static final float WALL_DEPTH_PRIORITY_UNITS = -1.0f;

		private final LwjglBindings gl;
		private final OpenGLWorldTextureCache textureCache;
		private final OpenGLShaderProgram texturedShaderProgram;
		private final int vertexBufferId;
		private final int shaderVertexBufferId;
		private final int indexBufferId;
		private final int texturedIndexBufferId;
		private final int occluderIndexBufferId;
		private final List<WorldMeshTextureBatch> textureBatches = new ArrayList<>();
		private FloatBuffer vertexUploadBuffer;
		private FloatBuffer shaderVertexUploadBuffer;
		private IntBuffer indexUploadBuffer;
		private IntBuffer texturedIndexUploadBuffer;
		private IntBuffer occluderIndexUploadBuffer;
		private int vertexUploadCapacity;
		private int shaderVertexUploadCapacity;
		private int indexUploadCapacity;
		private int texturedIndexUploadCapacity;
		private int occluderIndexUploadCapacity;
		private WorldMeshUploadState lastUploadState;
		private ShaderVertexParityStats lastShaderVertexParityStats = ShaderVertexParityStats.EMPTY;
		private int lastDrawTriangles;
		private int lastDrawOccluderTriangles;
		private int lastDrawBatches;
		private int lastDrawCalls;
		private boolean closed;

		private OpenGLWorldMeshRenderer(
			LwjglBindings gl,
			OpenGLWorldTextureCache textureCache,
			OpenGLShaderProgram texturedShaderProgram) throws Exception {
			this.gl = gl;
			this.textureCache = textureCache;
			this.texturedShaderProgram = texturedShaderProgram;
			this.vertexBufferId = gl.glGenBuffers();
			this.shaderVertexBufferId = gl.glGenBuffers();
			this.indexBufferId = gl.glGenBuffers();
			this.texturedIndexBufferId = gl.glGenBuffers();
			this.occluderIndexBufferId = gl.glGenBuffers();
		}

		private void uploadAndMaybeDrawObjects(
			Renderer3DMeshFrame meshFrame,
			FloatBuffer projectionMatrix) throws Exception {
			uploadAndMaybeDraw(meshFrame, false, true, WorldMeshTriangleFilter.STATIC_OBJECTS, true, projectionMatrix);
		}

		private int getLastDrawTriangles() {
			return lastDrawTriangles;
		}

		private int getLastDrawOccluderTriangles() {
			return lastDrawOccluderTriangles;
		}

		private int getLastDrawBatches() {
			return lastDrawBatches;
		}

		private int getLastDrawCalls() {
			return lastDrawCalls;
		}

		private void uploadAndMaybeDraw(
			Renderer3DMeshFrame meshFrame,
			boolean wireframeVisible,
			boolean texturedVisible,
			FloatBuffer projectionMatrix) throws Exception {
			uploadAndMaybeDraw(
				meshFrame,
				wireframeVisible,
				texturedVisible,
				WorldMeshTriangleFilter.ALL_STATIC,
				projectionMatrix);
		}

		private void uploadAndMaybeDrawOwnedBaseWorld(
			Renderer3DMeshFrame meshFrame,
			boolean wireframeVisible,
			boolean texturedVisible,
			FloatBuffer projectionMatrix) throws Exception {
			// Renderer3DMeshFrame carries model kind, model index, and face id per
			// triangle. The capture analyzer verifies complete, unique ownership.
			uploadAndMaybeDraw(
				meshFrame,
				wireframeVisible,
				texturedVisible,
				WorldMeshTriangleFilter.ALL_STATIC,
				projectionMatrix);
		}

		private void uploadAndMaybeDraw(
			Renderer3DMeshFrame meshFrame,
			boolean wireframeVisible,
			boolean texturedVisible,
			WorldMeshTriangleFilter triangleFilter,
			FloatBuffer projectionMatrix) throws Exception {
			uploadAndMaybeDraw(meshFrame, wireframeVisible, texturedVisible, triangleFilter, false, projectionMatrix);
		}

		private void uploadAndMaybeDraw(
			Renderer3DMeshFrame meshFrame,
			boolean wireframeVisible,
			boolean texturedVisible,
			WorldMeshTriangleFilter triangleFilter,
			boolean depthAwareObjectBridge,
			FloatBuffer projectionMatrix) throws Exception {
			int vertexCount = meshFrame.getVertexCount();
			int indexCount = meshFrame.getIndexCount();
			if (vertexCount <= 0 || indexCount <= 0) {
				return;
			}

			WorldTextureUploadStats textureStats = textureCache.uploadReferencedTextures(meshFrame);
			WorldMeshUploadSignature uploadSignature =
				WorldMeshUploadSignature.from(meshFrame, vertexCount, indexCount, triangleFilter);
			int texturedIndexCount;
			int occluderIndexCount;
			boolean needsFullIndexBuffer = wireframeVisible || !depthAwareObjectBridge;
			boolean reusedUpload = lastUploadState != null && lastUploadState.matches(uploadSignature);
			if (!reusedUpload) {
				ensureUploadBuffers(vertexCount, indexCount);
				copyMesh(meshFrame, vertexCount);
				if (needsFullIndexBuffer) {
					copyFullIndices(meshFrame, indexCount);
				}
				texturedIndexCount = copyTexturedIndices(meshFrame, indexCount, triangleFilter);
				occluderIndexCount =
					depthAwareObjectBridge ? copyProjectedOccluderIndices(meshFrame, indexCount) : 0;

				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
				gl.glBufferData(gl.GL_ARRAY_BUFFER, vertexUploadBuffer, gl.GL_STREAM_DRAW);
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
				gl.glBufferData(gl.GL_ARRAY_BUFFER, shaderVertexUploadBuffer, gl.GL_STREAM_DRAW);
				if (needsFullIndexBuffer) {
					gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
					gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indexUploadBuffer, gl.GL_STREAM_DRAW);
				}
				if (texturedIndexCount > 0) {
					gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexBufferId);
					gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexUploadBuffer, gl.GL_STREAM_DRAW);
				}
				if (occluderIndexCount > 0) {
					gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexBufferId);
					gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexUploadBuffer, gl.GL_STREAM_DRAW);
				}
				lastUploadState = new WorldMeshUploadState(uploadSignature, texturedIndexCount, occluderIndexCount);
			} else {
				texturedIndexCount = lastUploadState.texturedIndexCount;
				occluderIndexCount = lastUploadState.occluderIndexCount;
			}
			RenderTelemetry.recordOpenGLWorldMeshUpload(reusedUpload);
			RenderTelemetry.recordOpenGLWorldMeshFrame(vertexCount, indexCount, indexCount / 3);
			RenderTelemetry.recordOpenGLWorldTextureFrame(
				textureStats.referencedTextures,
				textureStats.cachedTextures,
				textureStats.uploadedTextures,
				textureStats.missingTextures,
				textureStats.atlases);
			int drawnTriangles = 0;
			int drawnOccluderTriangles = 0;
			int drawnBatches = 0;
			int drawCalls = 0;
			if (!wireframeVisible && !texturedVisible) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
				RenderTelemetry.recordOpenGLWorldMeshDraw(drawnTriangles, drawnOccluderTriangles, drawnBatches, drawCalls);
				return;
			}

			gl.glEnable(gl.GL_DEPTH_TEST);
			if (!depthAwareObjectBridge) {
				gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
			}
			try {
				if (texturedVisible && texturedIndexCount > 0) {
					if (depthAwareObjectBridge) {
						drawProjectedObjectBridge(occluderIndexCount, projectionMatrix);
						drawnOccluderTriangles = occluderIndexCount / 3;
						if (occluderIndexCount > 0) {
							drawCalls++;
						}
					} else {
						drawTexturedDiagnostic(projectionMatrix);
					}
					drawnTriangles = texturedIndexCount / 3;
					drawnBatches = countDrawableTextureBatches();
					drawCalls += drawnBatches;
				}
				if (wireframeVisible) {
					drawWireframeDiagnostic(indexCount);
					drawCalls++;
				}
			} finally {
				gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
				gl.glDisableClientState(gl.GL_COLOR_ARRAY);
				gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
				gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
				gl.glDisable(gl.GL_DEPTH_TEST);
				gl.glEnable(gl.GL_TEXTURE_2D);
				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}
			lastDrawTriangles = drawnTriangles;
			lastDrawOccluderTriangles = drawnOccluderTriangles;
			lastDrawBatches = drawnBatches;
			lastDrawCalls = drawCalls;
			RenderTelemetry.recordOpenGLWorldMeshDraw(drawnTriangles, drawnOccluderTriangles, drawnBatches, drawCalls);
		}

		private void ensureUploadBuffers(int vertexCount, int indexCount) {
			int requiredVertexFloats = vertexCount * UPLOAD_FLOATS_PER_VERTEX;
			int requiredShaderVertexFloats = vertexCount * SHADER_UPLOAD_FLOATS_PER_VERTEX;
			if (vertexUploadBuffer == null || vertexUploadCapacity < requiredVertexFloats) {
				vertexUploadCapacity = growCapacity(vertexUploadCapacity, requiredVertexFloats);
				vertexUploadBuffer = ByteBuffer
					.allocateDirect(vertexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}
			if (shaderVertexUploadBuffer == null || shaderVertexUploadCapacity < requiredShaderVertexFloats) {
				shaderVertexUploadCapacity = growCapacity(shaderVertexUploadCapacity, requiredShaderVertexFloats);
				shaderVertexUploadBuffer = ByteBuffer
					.allocateDirect(shaderVertexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asFloatBuffer();
			}

			if (indexUploadBuffer == null || indexUploadCapacity < indexCount) {
				indexUploadCapacity = growCapacity(indexUploadCapacity, indexCount);
				indexUploadBuffer = ByteBuffer
					.allocateDirect(indexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asIntBuffer();
			}

			if (texturedIndexUploadBuffer == null || texturedIndexUploadCapacity < indexCount) {
				texturedIndexUploadCapacity = growCapacity(texturedIndexUploadCapacity, indexCount);
				texturedIndexUploadBuffer = ByteBuffer
					.allocateDirect(texturedIndexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asIntBuffer();
			}

			if (occluderIndexUploadBuffer == null || occluderIndexUploadCapacity < indexCount) {
				occluderIndexUploadCapacity = growCapacity(occluderIndexUploadCapacity, indexCount);
				occluderIndexUploadBuffer = ByteBuffer
					.allocateDirect(occluderIndexUploadCapacity * 4)
					.order(ByteOrder.nativeOrder())
					.asIntBuffer();
			}
		}

		private int growCapacity(int currentCapacity, int requiredCapacity) {
			int capacity = Math.max(1024, currentCapacity);
			while (capacity < requiredCapacity) {
				capacity *= 2;
			}
			return capacity;
		}

		private void copyMesh(Renderer3DMeshFrame meshFrame, int vertexCount) throws Exception {
			float[] vertices = meshFrame.getVertices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			int[] triangleFallbackColors = meshFrame.getTriangleFallbackColors();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			float brightness = RendererDayNightCycle.currentBrightnessMultiplier();
			boolean flatGeometryLighting = usesTriangleFlatWorldMeshLighting();
			int cachedFlatLightTriangle = -1;
			int cachedFlatLegacyLight = 0;
			int cachedFlatBaseLegacyLight = 0;
			vertexUploadBuffer.clear();
			shaderVertexUploadBuffer.clear();
			for (int vertex = 0; vertex < vertexCount; vertex++) {
				int sourceOffset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX;
				int triangle = vertex / 3;
				int textureId = triangle >= 0 && triangle < triangleTextures.length ? triangleTextures[triangle] : 0;
				int rawMaterialColor =
					triangle >= 0 && triangle < triangleFallbackColors.length ? triangleFallbackColors[triangle] : 0;
				if (flatGeometryLighting && triangle != cachedFlatLightTriangle) {
					cachedFlatLightTriangle = triangle;
					cachedFlatLegacyLight = triangleAverageLegacyLight(
						vertices,
						vertexCount,
						triangle,
						Renderer3DMeshFrame.LEGACY_LIGHT_OFFSET);
					cachedFlatBaseLegacyLight = triangleAverageLegacyLight(
						vertices,
						vertexCount,
						triangle,
						Renderer3DMeshFrame.BASE_LEGACY_LIGHT_OFFSET);
				}
				int sourceLegacyLight = flatGeometryLighting
					? cachedFlatLegacyLight
					: Math.round(vertices[sourceOffset + Renderer3DMeshFrame.LEGACY_LIGHT_OFFSET]);
				int sourceBaseLegacyLight = flatGeometryLighting
					? cachedFlatBaseLegacyLight
					: Math.round(vertices[sourceOffset + Renderer3DMeshFrame.BASE_LEGACY_LIGHT_OFFSET]);
				int legacyLight = applyLightingModeLegacyLight(sourceLegacyLight);
				int baseLegacyLight = applyLightingModeLegacyLight(sourceBaseLegacyLight);
				boolean flatMaterial = isFlatColorMaterial(textureId);
				OpenGLTextureRegion textureRegion =
					textureRegionForVertex(meshFrame, triangleTextures, triangleModelKinds, vertex);
				vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_X_OFFSET]);
				vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Y_OFFSET]);
				vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Z_OFFSET]);
				int flatGeometryMaterialColor = flatGeometryLighting && textureRegion == null
					? shadedWorldMeshMaterialColor(rawMaterialColor, textureId, legacyLight)
					: -1;
				float materialRed = flatGeometryMaterialColor >= 0
					? brightnessColor(((flatGeometryMaterialColor >> 16) & 0xFF) / 255.0f, brightness)
					: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.RED_OFFSET], brightness);
				float materialGreen = flatGeometryMaterialColor >= 0
					? brightnessColor(((flatGeometryMaterialColor >> 8) & 0xFF) / 255.0f, brightness)
					: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.GREEN_OFFSET], brightness);
				float materialBlue = flatGeometryMaterialColor >= 0
					? brightnessColor((flatGeometryMaterialColor & 0xFF) / 255.0f, brightness)
					: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.BLUE_OFFSET], brightness);
				float textureLight = brightnessColor(textureLightFactor(legacyLight), brightness);
				float alpha =
					vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_ALPHA_OFFSET] * TEXTURED_DIAGNOSTIC_ALPHA;
				float atlasU = atlasU(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_U_OFFSET]);
				float atlasV = atlasV(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_V_OFFSET]);
				float textureRed = textureLight;
				float textureGreen = textureLight;
				float textureBlue = textureLight;
				if (flatMaterial && textureRegion != null) {
					textureRed = materialRed;
					textureGreen = materialGreen;
					textureBlue = materialBlue;
				}
				vertexUploadBuffer.put(materialRed);
				vertexUploadBuffer.put(materialGreen);
				vertexUploadBuffer.put(materialBlue);
				vertexUploadBuffer.put(atlasU);
				vertexUploadBuffer.put(atlasV);
				vertexUploadBuffer.put(textureRed);
				vertexUploadBuffer.put(textureGreen);
				vertexUploadBuffer.put(textureBlue);
				vertexUploadBuffer.put(alpha);

				shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_X_OFFSET]);
				shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Y_OFFSET]);
				shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Z_OFFSET]);
				shaderVertexUploadBuffer.put(atlasU);
				shaderVertexUploadBuffer.put(atlasV);
				shaderVertexUploadBuffer.put(materialRed);
				shaderVertexUploadBuffer.put(materialGreen);
				shaderVertexUploadBuffer.put(materialBlue);
				shaderVertexUploadBuffer.put(alpha);
				shaderVertexUploadBuffer.put(legacyLight);
				shaderVertexUploadBuffer.put(baseLegacyLight);
				shaderVertexUploadBuffer.put(((rawMaterialColor >> 16) & 0xFF) / 255.0f);
				shaderVertexUploadBuffer.put(((rawMaterialColor >> 8) & 0xFF) / 255.0f);
				shaderVertexUploadBuffer.put((rawMaterialColor & 0xFF) / 255.0f);
			}
			vertexUploadBuffer.flip();
			shaderVertexUploadBuffer.flip();
			lastShaderVertexParityStats = ShaderVertexParityStats.compare(
				vertexUploadBuffer,
				shaderVertexUploadBuffer,
				vertexCount);
		}

		private boolean usesTriangleFlatWorldMeshLighting() {
			RendererGeometrySettings.Mode mode = RendererGeometrySettings.getMode();
			return mode == RendererGeometrySettings.Mode.FACETED
				|| mode == RendererGeometrySettings.Mode.WIRE;
		}

		private int triangleAverageLegacyLight(float[] vertices, int vertexCount, int triangle, int lightOffset) {
			int firstVertex = triangle * 3;
			int total = 0;
			int count = 0;
			for (int i = 0; i < 3; i++) {
				int vertex = firstVertex + i;
				int offset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + lightOffset;
				if (vertex < 0 || vertex >= vertexCount || offset < 0 || offset >= vertices.length) {
					continue;
				}
				total += Math.round(vertices[offset]);
				count++;
			}
			return count <= 0 ? 0 : Math.round((float) total / count);
		}

		private int shadedWorldMeshMaterialColor(int color, int textureId, int legacyLight) {
			int normalizedLight = clampLegacyLight(legacyLight);
			if (textureId < 0 || textureId == LEGACY_TRANSPARENT_TEXTURE) {
				return legacyFlatResourceColor(color, normalizedLight);
			}
			return legacyTextureShadeColor(color, legacyTextureShadeBand(normalizedLight));
		}

		private int legacyFlatResourceColor(int color, int legacyLight) {
			int shade = 255 - legacyLight;
			int shadeSquared = shade * shade;
			int red = (((color >> 16) & 0xFF) * shadeSquared) / 65536;
			int green = (((color >> 8) & 0xFF) * shadeSquared) / 65536;
			int blue = ((color & 0xFF) * shadeSquared) / 65536;
			return red << 16 | green << 8 | blue;
		}

		private int legacyTextureShadeColor(int color, int shadeBand) {
			switch (shadeBand) {
				case 1:
					return (color - (color >>> 3)) & 0xFFFFFF;
				case 2:
					return (color - (color >>> 2)) & 0xFFFFFF;
				case 3:
					return (color - (color >>> 3) - (color >>> 2)) & 0xFFFFFF;
				default:
					return color;
			}
		}

		private ShaderVertexParityStats getLastShaderVertexParityStats() {
			return lastShaderVertexParityStats;
		}

		private float brightnessColor(float color, float brightness) {
			float adjusted = color * brightness;
			if (adjusted < 0.0f) {
				return 0.0f;
			}
			if (adjusted > 1.0f) {
				return 1.0f;
			}
			return adjusted;
		}

		private int applyLightingModeLegacyLight(int legacyLight) {
			return clampLegacyLight(legacyLight);
		}

		private float textureLightFactor(int legacyLight) {
			return legacyTextureLightFactor(legacyLight);
		}

		private float legacyTextureLightFactor(int legacyLight) {
			switch (legacyTextureShadeBand(legacyLight)) {
				case 1:
					return 216.0f / 248.0f;
				case 2:
					return 184.0f / 248.0f;
				case 3:
					return 152.0f / 248.0f;
				default:
					return 1.0f;
			}
		}

		private int legacyTextureShadeBand(int legacyLight) {
			return clampLegacyLight(legacyLight) >> 6;
		}

		private int clampLegacyLight(int legacyLight) {
			if (legacyLight < 0) {
				return 0;
			}
			if (legacyLight > 255) {
				return 255;
			}
			return legacyLight;
		}

		private float clamp(float value, float min, float max) {
			return Math.max(min, Math.min(max, value));
		}

		private void copyFullIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
			int[] indices = meshFrame.getIndices();
			indexUploadBuffer.clear();
			indexUploadBuffer.put(indices, 0, indexCount);
			indexUploadBuffer.flip();
		}

		private int copyTexturedIndices(
			Renderer3DMeshFrame meshFrame,
			int indexCount,
			WorldMeshTriangleFilter triangleFilter) throws Exception {
			if (WORLD_MESH_TEXTURED_STATIC_VISIBLE && triangleFilter != WorldMeshTriangleFilter.STATIC_OBJECTS) {
				return copyTexturedIndicesInLegacyOrder(meshFrame, indexCount, triangleFilter);
			}

			int[] indices = meshFrame.getIndices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
			Map<WorldMeshTextureBatchKey, WorldMeshTextureBatch> batchesByTexture = new LinkedHashMap<>();
			textureBatches.clear();

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
					continue;
				}
				Renderer3DModelKind modelKind = triangleModelKinds[triangle];
				if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], modelKind)) {
					continue;
				}
				Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
					? meshFrame.getTexture(triangleTextures[triangle])
					: null;
				StaticWorldMaterialPass materialPass =
					classifyStaticWorldMaterial(triangleTextures[triangle], textureData);
				if (materialPass == StaticWorldMaterialPass.DISCARDED
					|| materialPass == StaticWorldMaterialPass.UNRESOLVED
					|| materialPass == StaticWorldMaterialPass.TRANSLUCENT) {
					continue;
				}
				OpenGLTextureRegion region =
					textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
				int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
				WorldMeshTextureBatchKey batchKey =
					new WorldMeshTextureBatchKey(batchTextureId, materialPass, modelKind);
				WorldMeshTextureBatch batch = batchesByTexture.get(batchKey);
				if (batch == null) {
					batch = new WorldMeshTextureBatch(
						batchTextureId,
						materialPass,
						modelKind);
					batchesByTexture.put(batchKey, batch);
					textureBatches.add(batch);
				}
				batch.indexCount += 3;
			}

			int nextIndex = 0;
			for (WorldMeshTextureBatch batch : textureBatches) {
				batch.startIndex = nextIndex;
				batch.writeIndex = nextIndex;
				nextIndex += batch.indexCount;
			}

			texturedIndexUploadBuffer.clear();
			texturedIndexUploadBuffer.limit(nextIndex);
			if (nextIndex == 0) {
				return 0;
			}

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
					continue;
				}
				Renderer3DModelKind modelKind = triangleModelKinds[triangle];
				if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], modelKind)) {
					continue;
				}
				Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
					? meshFrame.getTexture(triangleTextures[triangle])
					: null;
				StaticWorldMaterialPass materialPass =
					classifyStaticWorldMaterial(triangleTextures[triangle], textureData);
				if (materialPass == StaticWorldMaterialPass.DISCARDED
					|| materialPass == StaticWorldMaterialPass.UNRESOLVED
					|| materialPass == StaticWorldMaterialPass.TRANSLUCENT) {
					continue;
				}
				OpenGLTextureRegion region =
					textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
				int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();

				int sourceIndex = triangle * 3;
				WorldMeshTextureBatchKey batchKey = new WorldMeshTextureBatchKey(
					batchTextureId,
					materialPass,
					modelKind);
				WorldMeshTextureBatch batch = batchesByTexture.get(batchKey);
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex]);
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 1]);
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 2]);
			}

			texturedIndexUploadBuffer.position(0);
			return nextIndex;
		}

		private int copyTexturedIndicesInLegacyOrder(
			Renderer3DMeshFrame meshFrame,
			int indexCount,
			WorldMeshTriangleFilter triangleFilter) throws Exception {
			return copyTexturedIndicesInLegacyOrderRange(
				meshFrame,
				indexCount,
				triangleFilter,
				Integer.MIN_VALUE,
				Integer.MAX_VALUE);
		}

		private int copyTexturedIndicesInLegacyOrderRange(
			Renderer3DMeshFrame meshFrame,
			int indexCount,
			WorldMeshTriangleFilter triangleFilter,
			int minExclusiveLegacyDrawOrder,
			int maxExclusiveLegacyDrawOrder) throws Exception {
			return copyTexturedIndicesInLegacyOrderRange(
				meshFrame,
				indexCount,
				triangleFilter,
				minExclusiveLegacyDrawOrder,
				maxExclusiveLegacyDrawOrder,
				null);
		}

		private int copyTexturedIndicesInLegacyOrderRange(
			Renderer3DMeshFrame meshFrame,
			int indexCount,
			WorldMeshTriangleFilter triangleFilter,
			int minExclusiveLegacyDrawOrder,
			int maxExclusiveLegacyDrawOrder,
			Set<Long> modelFaceKeys) throws Exception {
			int[] indices = meshFrame.getIndices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			int[] triangleLegacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			int[] triangleModelIndices = meshFrame.getTriangleModelIndices();
			int[] triangleFaceIds = meshFrame.getTriangleFaceIds();
			int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
			List<Integer> orderedTriangles = new ArrayList<>();
			textureBatches.clear();
			boolean fullRange =
				minExclusiveLegacyDrawOrder == Integer.MIN_VALUE
					&& maxExclusiveLegacyDrawOrder == Integer.MAX_VALUE;

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
					continue;
				}
				int legacyDrawOrder = triangle < triangleLegacyDrawOrders.length
					? triangleLegacyDrawOrders[triangle]
					: -1;
				if (!fullRange
					&& (legacyDrawOrder < 0
					|| legacyDrawOrder <= minExclusiveLegacyDrawOrder
					|| legacyDrawOrder >= maxExclusiveLegacyDrawOrder)) {
					continue;
				}
				if (modelFaceKeys != null
					&& !modelFaceKeys.isEmpty()
					&& !modelFaceKeys.contains(triangleModelFaceKey(triangleModelIndices, triangleFaceIds, triangle))) {
					continue;
				}
				if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], triangleModelKinds[triangle])) {
					continue;
				}
				orderedTriangles.add(Integer.valueOf(triangle));
			}

			Collections.sort(orderedTriangles, new Comparator<Integer>() {
				@Override
				public int compare(Integer left, Integer right) {
					int leftTriangle = left.intValue();
					int rightTriangle = right.intValue();
					int leftOrder = legacyDrawOrderOrEnd(triangleLegacyDrawOrders, leftTriangle);
					int rightOrder = legacyDrawOrderOrEnd(triangleLegacyDrawOrders, rightTriangle);
					if (leftOrder != rightOrder) {
						return leftOrder < rightOrder ? -1 : 1;
					}
					return leftTriangle < rightTriangle ? -1 : (leftTriangle == rightTriangle ? 0 : 1);
				}
			});

			texturedIndexUploadBuffer.clear();
			int nextIndex = 0;
			if (triangleFilter == WorldMeshTriangleFilter.ALL_STATIC) {
				boolean canBatchOpaqueStatic =
					fullRange && (modelFaceKeys == null || modelFaceKeys.isEmpty());
				if (canBatchOpaqueStatic) {
					nextIndex = appendOpaqueStaticMaterialBatches(
						meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles, nextIndex);
					nextIndex = appendMaterialPassTriangles(
						meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
						StaticWorldMaterialPass.OPAQUE, nextIndex, true);
				} else {
					nextIndex = appendMaterialPassTriangles(
						meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
						StaticWorldMaterialPass.OPAQUE, nextIndex);
				}
				nextIndex = appendMaterialPassTriangles(
					meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
					StaticWorldMaterialPass.CUTOUT, nextIndex);
			} else {
				nextIndex = appendMaterialPassTriangles(
					meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles, null, nextIndex);
			}

			texturedIndexUploadBuffer.flip();
			return nextIndex;
		}

		private int appendOpaqueStaticMaterialBatches(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int[] indices,
			List<Integer> orderedTriangles,
			int nextIndex) throws Exception {
			Map<WorldMeshTextureBatchKey, WorldMeshTextureBatch> batchesByMaterial =
				new LinkedHashMap<WorldMeshTextureBatchKey, WorldMeshTextureBatch>();
			Map<WorldMeshTextureBatchKey, List<Integer>> trianglesByMaterial =
				new LinkedHashMap<WorldMeshTextureBatchKey, List<Integer>>();
			for (Integer orderedTriangle : orderedTriangles) {
				int triangle = orderedTriangle.intValue();
				Renderer3DModelKind modelKind = triangleModelKinds[triangle];
				if (!isProjectedMeshOpaqueBatchKind(modelKind)) {
					continue;
				}
				Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
					? meshFrame.getTexture(triangleTextures[triangle])
					: null;
				StaticWorldMaterialPass materialPass =
					classifyStaticWorldMaterial(triangleTextures[triangle], textureData);
				if (materialPass != StaticWorldMaterialPass.OPAQUE) {
					continue;
				}
				OpenGLTextureRegion region =
					textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
				if (!canAppendWorldMeshTriangle(triangleTextures[triangle], modelKind, region)) {
					continue;
				}
				int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
				WorldMeshTextureBatchKey batchKey =
					new WorldMeshTextureBatchKey(batchTextureId, materialPass, modelKind);
				WorldMeshTextureBatch batch = batchesByMaterial.get(batchKey);
				if (batch == null) {
					batch = new WorldMeshTextureBatch(
						batchTextureId,
						materialPass,
						modelKind);
					batchesByMaterial.put(batchKey, batch);
					trianglesByMaterial.put(batchKey, new ArrayList<Integer>());
				}
				trianglesByMaterial.get(batchKey).add(orderedTriangle);
			}
			for (Map.Entry<WorldMeshTextureBatchKey, List<Integer>> entry : trianglesByMaterial.entrySet()) {
				WorldMeshTextureBatch batch = batchesByMaterial.get(entry.getKey());
				batch.startIndex = nextIndex;
				textureBatches.add(batch);
				for (Integer terrainTriangle : entry.getValue()) {
					int sourceIndex = terrainTriangle.intValue() * 3;
					texturedIndexUploadBuffer.put(indices[sourceIndex]);
					texturedIndexUploadBuffer.put(indices[sourceIndex + 1]);
					texturedIndexUploadBuffer.put(indices[sourceIndex + 2]);
					nextIndex += 3;
					batch.indexCount += 3;
				}
			}
			return nextIndex;
		}

		private boolean isProjectedMeshOpaqueBatchKind(Renderer3DModelKind modelKind) {
			return modelKind == Renderer3DModelKind.TERRAIN
				|| modelKind == Renderer3DModelKind.WALL
				|| modelKind == Renderer3DModelKind.ROOF
				|| modelKind == Renderer3DModelKind.GAME_OBJECT
				|| modelKind == Renderer3DModelKind.WALL_OBJECT;
		}

		private int appendMaterialPassTriangles(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int[] indices,
			List<Integer> orderedTriangles,
			StaticWorldMaterialPass requiredPass,
			int nextIndex) throws Exception {
			return appendMaterialPassTriangles(
				meshFrame,
				triangleTextures,
				triangleModelKinds,
				indices,
				orderedTriangles,
				requiredPass,
				nextIndex,
				null);
		}

		private int appendMaterialPassTriangles(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int[] indices,
			List<Integer> orderedTriangles,
			StaticWorldMaterialPass requiredPass,
			int nextIndex,
			Renderer3DModelKind excludedKind) throws Exception {
			return appendMaterialPassTriangles(
				meshFrame,
				triangleTextures,
				triangleModelKinds,
				indices,
				orderedTriangles,
				requiredPass,
				nextIndex,
				excludedKind,
				false);
		}

		private int appendMaterialPassTriangles(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int[] indices,
			List<Integer> orderedTriangles,
			StaticWorldMaterialPass requiredPass,
			int nextIndex,
			boolean excludeOpaqueBatchKinds) throws Exception {
			return appendMaterialPassTriangles(
				meshFrame,
				triangleTextures,
				triangleModelKinds,
				indices,
				orderedTriangles,
				requiredPass,
				nextIndex,
				null,
				excludeOpaqueBatchKinds);
		}

		private int appendMaterialPassTriangles(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int[] indices,
			List<Integer> orderedTriangles,
			StaticWorldMaterialPass requiredPass,
			int nextIndex,
			Renderer3DModelKind excludedKind,
			boolean excludeOpaqueBatchKinds) throws Exception {
			WorldMeshTextureBatch currentBatch = null;
			for (Integer orderedTriangle : orderedTriangles) {
				int triangle = orderedTriangle.intValue();
				Renderer3DModelKind modelKind = triangleModelKinds[triangle];
				if (excludedKind != null && modelKind == excludedKind) {
					continue;
				}
				if (excludeOpaqueBatchKinds && isProjectedMeshOpaqueBatchKind(modelKind)) {
					continue;
				}
				Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
					? meshFrame.getTexture(triangleTextures[triangle])
					: null;
				StaticWorldMaterialPass materialPass =
					classifyStaticWorldMaterial(triangleTextures[triangle], textureData);
				if (requiredPass != null && materialPass != requiredPass) {
					continue;
				}
				OpenGLTextureRegion region =
					textureRegionForTriangle(meshFrame, triangleTextures[triangle], triangleModelKinds[triangle]);
				int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
				if (currentBatch == null
					|| currentBatch.textureId != batchTextureId
					|| currentBatch.materialPass != materialPass
					|| currentBatch.modelKind != triangleModelKinds[triangle]) {
					currentBatch = new WorldMeshTextureBatch(
						batchTextureId,
						materialPass,
						triangleModelKinds[triangle]);
					currentBatch.startIndex = nextIndex;
					textureBatches.add(currentBatch);
				}
				if (region == null
					&& !isFlatColorMaterial(triangleTextures[triangle])
					&& canSampleTextureForKind(triangleModelKinds[triangle])) {
					continue;
				}

				int sourceIndex = triangle * 3;
				texturedIndexUploadBuffer.put(indices[sourceIndex]);
				texturedIndexUploadBuffer.put(indices[sourceIndex + 1]);
				texturedIndexUploadBuffer.put(indices[sourceIndex + 2]);
				nextIndex += 3;
					currentBatch.indexCount += 3;
			}
			return nextIndex;
		}

		private boolean canAppendWorldMeshTriangle(
			int textureId,
			Renderer3DModelKind modelKind,
			OpenGLTextureRegion region) {
			return region != null || isFlatColorMaterial(textureId) || !canSampleTextureForKind(modelKind);
		}

		private long triangleModelFaceKey(int[] triangleModelIndices, int[] triangleFaceIds, int triangle) {
			int modelIndex = triangle >= 0 && triangle < triangleModelIndices.length
				? triangleModelIndices[triangle]
				: -1;
			int faceId = triangle >= 0 && triangle < triangleFaceIds.length
				? triangleFaceIds[triangle]
				: -1;
			return openGLCompositeModelFaceKey(modelIndex, faceId);
		}

		private int copyProjectedOccluderIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
			int[] indices = meshFrame.getIndices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			int triangleCount = Math.min(
				meshFrame.getTriangleCount(),
				Math.min(indexCount / 3, Math.min(triangleTextures.length, triangleModelKinds.length)));
			occluderIndexUploadBuffer.clear();
			int nextIndex = 0;
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isProjectedObjectBridgeOccluderTriangle(
					meshFrame,
					indices,
					triangleTextures,
					triangleModelKinds,
					triangle)) {
					continue;
				}
				int sourceIndex = triangle * 3;
				occluderIndexUploadBuffer.put(indices[sourceIndex]);
				occluderIndexUploadBuffer.put(indices[sourceIndex + 1]);
				occluderIndexUploadBuffer.put(indices[sourceIndex + 2]);
				nextIndex += 3;
			}
			occluderIndexUploadBuffer.flip();
			return nextIndex;
		}

		private boolean isProjectedObjectBridgeOccluderTriangle(
			Renderer3DMeshFrame meshFrame,
			int[] indices,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int triangle) {
			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (modelKind != Renderer3DModelKind.TERRAIN
				&& modelKind != Renderer3DModelKind.WALL
				&& modelKind != Renderer3DModelKind.ROOF) {
				return false;
			}
			if (modelKind == Renderer3DModelKind.ROOF && Config.C_HIDE_ROOFS) {
				return false;
			}
			if (isTransparentMaterial(triangleTextures[triangle])) {
				return false;
			}
			return isReasonableProjectedTriangle(meshFrame, indices, triangle);
		}

		private int legacyDrawOrderOrEnd(int[] triangleLegacyDrawOrders, int triangle) {
			if (triangle < 0 || triangle >= triangleLegacyDrawOrders.length || triangleLegacyDrawOrders[triangle] < 0) {
				return Integer.MAX_VALUE;
			}
			return triangleLegacyDrawOrders[triangle];
		}

		private boolean isTexturedDiagnosticTriangle(
			Renderer3DModelKind[] triangleModelKinds,
			int triangle,
			WorldMeshTriangleFilter triangleFilter) {
			if (triangle < 0 || triangle >= triangleModelKinds.length) {
				return false;
			}

			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (!triangleFilter.accepts(modelKind)) {
				return false;
			}
			if (modelKind == Renderer3DModelKind.TERRAIN) {
				return true;
			}
			return WORLD_MESH_TEXTURED_STATIC_VISIBLE
				&& (modelKind == Renderer3DModelKind.WALL
					|| modelKind == Renderer3DModelKind.ROOF
					|| modelKind == Renderer3DModelKind.GAME_OBJECT
					|| modelKind == Renderer3DModelKind.WALL_OBJECT);
		}

		private boolean isDrawableWorldMaterial(
			Renderer3DMeshFrame meshFrame,
			int textureId,
			Renderer3DModelKind modelKind) {
			if (isFlatColorMaterial(textureId)) {
				return true;
			}
			if (isTransparentMaterial(textureId)) {
				return false;
			}
			if (!canSampleTextureForKind(modelKind)) {
				return true;
			}
			return textureCache.getRegion(meshFrame, textureId) != null;
		}

		private boolean isFlatColorMaterial(int textureId) {
			return textureId < 0;
		}

		private boolean isTransparentMaterial(int textureId) {
			return textureId == LEGACY_TRANSPARENT_TEXTURE;
		}

		private boolean canSampleTextureForKind(Renderer3DModelKind modelKind) {
			if (modelKind == Renderer3DModelKind.TERRAIN) {
				return true;
			}
			return WORLD_STATIC_TEXTURES
				&& (modelKind == Renderer3DModelKind.WALL
					|| modelKind == Renderer3DModelKind.ROOF
					|| modelKind == Renderer3DModelKind.GAME_OBJECT
					|| modelKind == Renderer3DModelKind.WALL_OBJECT);
		}

		private boolean isReasonableProjectedTriangle(
			Renderer3DMeshFrame meshFrame,
			int[] indices,
			int triangle) {
			int sourceIndex = triangle * 3;
			if (sourceIndex < 0 || sourceIndex + 2 >= indices.length) {
				return false;
			}
			float[] vertices = meshFrame.getVertices();
			int first = indices[sourceIndex];
			int second = indices[sourceIndex + 1];
			int third = indices[sourceIndex + 2];
			if (!isReasonableProjectedVertex(meshFrame, vertices, first)
				|| !isReasonableProjectedVertex(meshFrame, vertices, second)
				|| !isReasonableProjectedVertex(meshFrame, vertices, third)) {
				return false;
			}

			float firstX = projectedX(meshFrame, vertices, first);
			float secondX = projectedX(meshFrame, vertices, second);
			float thirdX = projectedX(meshFrame, vertices, third);
			float firstY = projectedY(meshFrame, vertices, first);
			float secondY = projectedY(meshFrame, vertices, second);
			float thirdY = projectedY(meshFrame, vertices, third);
			float minX = Math.min(firstX, Math.min(secondX, thirdX));
			float maxX = Math.max(firstX, Math.max(secondX, thirdX));
			float minY = Math.min(firstY, Math.min(secondY, thirdY));
			float maxY = Math.max(firstY, Math.max(secondY, thirdY));
			float viewportWidth = Math.max(1, meshFrame.getViewportWidth());
			float viewportHeight = Math.max(1, meshFrame.getViewportHeight());
			return maxX - minX <= viewportWidth * 1.75f
				&& maxY - minY <= viewportHeight * 1.75f;
		}

		private boolean isReasonableProjectedVertex(
			Renderer3DMeshFrame meshFrame,
			float[] vertices,
			int vertex) {
			int offset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX;
			if (vertex < 0 || offset + Renderer3DMeshFrame.SCREEN_Y_OFFSET >= vertices.length) {
				return false;
			}
			float x = projectedX(meshFrame, vertices, vertex);
			float y = projectedY(meshFrame, vertices, vertex);
			float viewportWidth = Math.max(1, meshFrame.getViewportWidth());
			float viewportHeight = Math.max(1, meshFrame.getViewportHeight());
			return x >= -viewportWidth
				&& x <= viewportWidth * 2.0f
				&& y >= -viewportHeight
				&& y <= viewportHeight * 2.0f;
		}

		private float projectedX(Renderer3DMeshFrame meshFrame, float[] vertices, int vertex) {
			return meshFrame.getCenterX()
				+ vertices[vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + Renderer3DMeshFrame.SCREEN_X_OFFSET];
		}

		private float projectedY(Renderer3DMeshFrame meshFrame, float[] vertices, int vertex) {
			return meshFrame.getCenterY()
				+ vertices[vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + Renderer3DMeshFrame.SCREEN_Y_OFFSET];
		}

		private OpenGLTextureRegion textureRegionForVertex(
			Renderer3DMeshFrame meshFrame,
			int[] triangleTextures,
			Renderer3DModelKind[] triangleModelKinds,
			int vertex) throws Exception {
			int triangle = vertex / 3;
			if (triangle < 0 || triangle >= triangleTextures.length) {
				return null;
			}
			Renderer3DModelKind modelKind =
				triangle < triangleModelKinds.length ? triangleModelKinds[triangle] : Renderer3DModelKind.UNCLASSIFIED;
			return textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
		}

		private OpenGLTextureRegion textureRegionForTriangle(
			Renderer3DMeshFrame meshFrame,
			int textureId,
			Renderer3DModelKind modelKind) throws Exception {
			if (isFlatColorMaterial(textureId)) {
				return null;
			}
			if (!canSampleTextureForKind(modelKind)) {
				return null;
			}
			return textureCache.getRegion(meshFrame, textureId);
		}

		private float atlasU(OpenGLTextureRegion region, float sourceU) {
			if (region == null) {
				return 0.0f;
			}
			return region.getU0() + (region.getU1() - region.getU0()) * clampUnit(sourceU);
		}

		private float atlasV(OpenGLTextureRegion region, float sourceV) {
			if (region == null) {
				return 0.0f;
			}
			return region.getV0() + (region.getV1() - region.getV0()) * clampUnit(sourceV);
		}

		private float clampUnit(float value) {
			if (Float.isNaN(value) || value <= 0.0f) {
				return 0.0f;
			}
			if (value >= 1.0f) {
				return 1.0f;
			}
			return value;
		}

		private void drawTexturedDiagnostic(FloatBuffer projectionMatrix) throws Exception {
			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glDepthMask(true);
			drawTexturedDiagnosticBatches(projectionMatrix);
		}

		private void drawProjectedObjectBridge(int occluderIndexCount, FloatBuffer projectionMatrix) throws Exception {
			if (occluderIndexCount > 0) {
				drawProjectedObjectBridgeDepthPrepass(occluderIndexCount);
			}
			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glDepthMask(true);
			try {
				drawTexturedDiagnosticBatches(projectionMatrix);
			} finally {
				gl.glDepthMask(true);
			}
		}

		private void drawProjectedObjectBridgeDepthPrepass(int occluderIndexCount) throws Exception {
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisable(gl.GL_BLEND);
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexBufferId);
			gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
			gl.glVertexPointer(
				POSITION_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				0L);
			gl.glDepthMask(true);
			gl.glColorMask(false, false, false, false);
			try {
				gl.glDrawElements(gl.GL_TRIANGLES, occluderIndexCount, gl.GL_UNSIGNED_INT, 0L);
			} finally {
				gl.glColorMask(true, true, true, true);
			}
		}

		private void drawTexturedDiagnosticBatches(FloatBuffer projectionMatrix) throws Exception {
			if (TEXTURED_DIAGNOSTIC_ALPHA < 0.999f) {
				gl.glEnable(gl.GL_BLEND);
				gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				gl.glDisable(gl.GL_BLEND);
				gl.glEnable(gl.GL_ALPHA_TEST);
				gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			}
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexBufferId);
			gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
			gl.glVertexPointer(
				POSITION_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				0L);
			boolean wireGeometry = RendererGeometrySettings.getMode() == RendererGeometrySettings.Mode.WIRE;
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, wireGeometry ? gl.GL_LINE : gl.GL_FILL);
			WorldMeshBatchDrawState drawState = new WorldMeshBatchDrawState(projectionMatrix);
			try {
				for (WorldMeshTextureBatch batch : textureBatches) {
					if (batch.indexCount <= 0) {
						continue;
					}
					if (batch.isTextureBacked()) {
						configureMaterialPass(batch.materialPass);
						drawTexturedBatch(batch, drawState);
					} else {
						configureMaterialPass(batch.materialPass);
						drawFlatColorBatch(batch, drawState);
					}
				}
			} finally {
				drawState.close();
				if (wireGeometry) {
					gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
				}
			}
		}

		private void configureMaterialPass(StaticWorldMaterialPass materialPass) throws Exception {
			if (TEXTURED_DIAGNOSTIC_ALPHA < 0.999f) {
				return;
			}
			gl.glDisable(gl.GL_BLEND);
			if (materialPass == StaticWorldMaterialPass.CUTOUT) {
				gl.glEnable(gl.GL_ALPHA_TEST);
				gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			} else {
				gl.glDisable(gl.GL_ALPHA_TEST);
			}
		}

		private int countDrawableTextureBatches() {
			int count = 0;
			for (WorldMeshTextureBatch batch : textureBatches) {
				if (batch.indexCount > 0) {
					count++;
				}
			}
			return count;
		}

		private void drawTexturedBatch(WorldMeshTextureBatch batch, WorldMeshBatchDrawState drawState) throws Exception {
			gl.glEnable(gl.GL_TEXTURE_2D);
			if (TEXTURED_DIAGNOSTIC_ALPHA < 0.999f) {
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glEnable(gl.GL_BLEND);
				gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				gl.glDisable(gl.GL_BLEND);
				if (batch.materialPass == StaticWorldMaterialPass.CUTOUT) {
					gl.glEnable(gl.GL_ALPHA_TEST);
					gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
				} else {
					gl.glDisable(gl.GL_ALPHA_TEST);
				}
			}
			drawState.useTextured();
			drawState.bindTexture(batch.textureId);
			gl.glColor4f(1.0f, 1.0f, 1.0f, TEXTURED_DIAGNOSTIC_ALPHA);
			drawWorldMeshBatchElements(batch);
		}

		private void drawFlatColorBatch(WorldMeshTextureBatch batch, WorldMeshBatchDrawState drawState) throws Exception {
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisable(gl.GL_ALPHA_TEST);
			drawState.useFlatColor();
			drawWorldMeshBatchElements(batch);
		}

		private void bindTexturedBatchState(FloatBuffer projectionMatrix) throws Exception {
			boolean shaderActive = WORLD_MESH_TEXTURED_SHADER && texturedShaderProgram != null;
			if (shaderActive) {
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
				texturedShaderProgram.useWorld(projectionMatrix, true);
				texturedShaderProgram.bindWorldTextureAttributes(
					POSITION_COMPONENT_COUNT,
					TEXTURE_COORD_COMPONENT_COUNT,
					TEXTURE_LIGHT_COMPONENT_COUNT,
					1,
					1,
					3,
					SHADER_STRIDE_BYTES,
					SHADER_POSITION_OFFSET_BYTES,
					SHADER_TEXTURE_COORD_OFFSET_BYTES,
					SHADER_MATERIAL_COLOR_OFFSET_BYTES,
					SHADER_LEGACY_LIGHT_OFFSET_BYTES,
					SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES,
					SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES);
				return;
			}
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glEnableClientState(gl.GL_COLOR_ARRAY);
			gl.glColorPointer(
				TEXTURE_LIGHT_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				TEXTURE_LIGHT_OFFSET_BYTES);
			gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(
				TEXTURE_COORD_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				TEXTURE_COORD_OFFSET_BYTES);
		}

		private void bindFlatColorBatchState(FloatBuffer projectionMatrix) throws Exception {
			boolean shaderActive = WORLD_MESH_TEXTURED_SHADER && texturedShaderProgram != null;
			if (shaderActive) {
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
				texturedShaderProgram.useWorld(projectionMatrix, false);
				texturedShaderProgram.bindWorldFlatColorAttributes(
					POSITION_COMPONENT_COUNT,
					TEXTURE_COORD_COMPONENT_COUNT,
					TEXTURE_LIGHT_COMPONENT_COUNT,
					SHADER_STRIDE_BYTES,
					SHADER_POSITION_OFFSET_BYTES,
					SHADER_TEXTURE_COORD_OFFSET_BYTES,
					SHADER_MATERIAL_COLOR_OFFSET_BYTES);
				return;
			}
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(gl.GL_COLOR_ARRAY);
			gl.glColorPointer(
				MATERIAL_COLOR_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				MATERIAL_COLOR_OFFSET_BYTES);
		}

		private void unbindShaderBatchState() throws Exception {
			if (WORLD_MESH_TEXTURED_SHADER && texturedShaderProgram != null) {
				texturedShaderProgram.unbindWorldTextureAttributes();
				gl.glUseProgram(0);
			}
		}

		private final class WorldMeshBatchDrawState implements AutoCloseable {
			private static final int MODE_NONE = 0;
			private static final int MODE_TEXTURED = 1;
			private static final int MODE_FLAT_COLOR = 2;

			private final FloatBuffer projectionMatrix;
			private final boolean shaderActive;
			private int mode = MODE_NONE;
			private int boundTextureId = Integer.MIN_VALUE;

			private WorldMeshBatchDrawState(FloatBuffer projectionMatrix) {
				this.projectionMatrix = projectionMatrix;
				this.shaderActive = WORLD_MESH_TEXTURED_SHADER && texturedShaderProgram != null;
			}

			private void useTextured() throws Exception {
				if (mode == MODE_TEXTURED) {
					return;
				}
				unbindActiveShaderState();
				bindTexturedBatchState(projectionMatrix);
				mode = MODE_TEXTURED;
			}

			private void useFlatColor() throws Exception {
				if (mode == MODE_FLAT_COLOR) {
					return;
				}
				unbindActiveShaderState();
				bindFlatColorBatchState(projectionMatrix);
				mode = MODE_FLAT_COLOR;
			}

			private void bindTexture(int textureId) throws Exception {
				if (boundTextureId == textureId) {
					return;
				}
				gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
				boundTextureId = textureId;
			}

			private void unbindActiveShaderState() throws Exception {
				if (shaderActive && mode != MODE_NONE) {
					unbindShaderBatchState();
				}
			}

			@Override
			public void close() throws Exception {
				unbindActiveShaderState();
				mode = MODE_NONE;
			}
		}

		private void drawWorldMeshBatchElements(WorldMeshTextureBatch batch) throws Exception {
			boolean wallDepthPriority = batch.modelKind == Renderer3DModelKind.WALL;
			if (wallDepthPriority) {
				enableWallDepthPriority();
			}
			try {
				gl.glDrawElements(
					gl.GL_TRIANGLES,
					batch.indexCount,
					gl.GL_UNSIGNED_INT,
					batch.startIndex * 4L);
			} finally {
				if (wallDepthPriority) {
					disableWallDepthPriority();
				}
			}
		}

		private void enableWallDepthPriority() throws Exception {
			gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(WALL_DEPTH_PRIORITY_FACTOR, WALL_DEPTH_PRIORITY_UNITS);
		}

		private void disableWallDepthPriority() throws Exception {
			gl.glPolygonOffset(0.0f, 0.0f);
			gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
		}

		private void drawWireframeDiagnostic(int indexCount) throws Exception {
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glEnable(gl.GL_BLEND);
			gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
			gl.glColor4f(0.1f, 0.85f, 1.0f, 0.35f);
			gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
			gl.glVertexPointer(
				POSITION_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				0L);
			gl.glDrawElements(gl.GL_TRIANGLES, indexCount, gl.GL_UNSIGNED_INT, 0L);
		}

		@Override
		public void close() throws Exception {
			if (closed) {
				return;
			}
			closed = true;
			gl.glDeleteBuffers(vertexBufferId);
			gl.glDeleteBuffers(shaderVertexBufferId);
			gl.glDeleteBuffers(indexBufferId);
			gl.glDeleteBuffers(texturedIndexBufferId);
			gl.glDeleteBuffers(occluderIndexBufferId);
		}
	}

	private static final class ShaderVertexParityStats {
		private static final ShaderVertexParityStats EMPTY = new ShaderVertexParityStats(0, 0, 0, 0, 0.0f);
		private static final int[] PARITY_COLOR_OFFSETS = new int[] {3, 4, 5, 11};
		private static final int[] SHADER_COLOR_OFFSETS = new int[] {5, 6, 7, 8};

		private final int vertexCount;
		private final int positionMismatches;
		private final int textureCoordMismatches;
		private final int colorMismatches;
		private final float maxAbsoluteDelta;

		private ShaderVertexParityStats(
			int vertexCount,
			int positionMismatches,
			int textureCoordMismatches,
			int colorMismatches,
			float maxAbsoluteDelta) {
			this.vertexCount = vertexCount;
			this.positionMismatches = positionMismatches;
			this.textureCoordMismatches = textureCoordMismatches;
			this.colorMismatches = colorMismatches;
			this.maxAbsoluteDelta = maxAbsoluteDelta;
		}

		private static ShaderVertexParityStats compare(
			FloatBuffer parityVertices,
			FloatBuffer shaderVertices,
			int vertexCount) {
			int positionMismatches = 0;
			int textureCoordMismatches = 0;
			int colorMismatches = 0;
			float maxAbsoluteDelta = 0.0f;
			for (int vertex = 0; vertex < vertexCount; vertex++) {
				int parity = vertex * OpenGLWorldMeshRenderer.UPLOAD_FLOATS_PER_VERTEX;
				int shader = vertex * OpenGLWorldMeshRenderer.SHADER_UPLOAD_FLOATS_PER_VERTEX;
				for (int component = 0; component < 3; component++) {
					float delta = Math.abs(parityVertices.get(parity + component) - shaderVertices.get(shader + component));
					maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
					if (delta != 0.0f) {
						positionMismatches++;
					}
				}
				for (int component = 0; component < 2; component++) {
					float delta = Math.abs(parityVertices.get(parity + 6 + component) - shaderVertices.get(shader + 3 + component));
					maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
					if (delta != 0.0f) {
						textureCoordMismatches++;
					}
				}
				for (int component = 0; component < PARITY_COLOR_OFFSETS.length; component++) {
					float delta = Math.abs(
						parityVertices.get(parity + PARITY_COLOR_OFFSETS[component])
							- shaderVertices.get(shader + SHADER_COLOR_OFFSETS[component]));
					maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
					if (delta != 0.0f) {
						colorMismatches++;
					}
				}
			}
			return new ShaderVertexParityStats(
				vertexCount,
				positionMismatches,
				textureCoordMismatches,
				colorMismatches,
				maxAbsoluteDelta);
		}
	}

	private static final class WorldMeshUploadState {
		private final WorldMeshUploadSignature signature;
		private final int texturedIndexCount;
		private final int occluderIndexCount;

		private WorldMeshUploadState(
			WorldMeshUploadSignature signature,
			int texturedIndexCount,
			int occluderIndexCount) {
			this.signature = signature;
			this.texturedIndexCount = Math.max(0, texturedIndexCount);
			this.occluderIndexCount = Math.max(0, occluderIndexCount);
		}

		private boolean matches(WorldMeshUploadSignature other) {
			return signature != null && signature.equals(other);
		}
	}

	private enum WorldMeshTriangleFilter {
		ALL_STATIC,
		STATIC_OBJECTS;

		private boolean accepts(Renderer3DModelKind kind) {
			if (this == ALL_STATIC) {
				return true;
			}
			return kind == Renderer3DModelKind.GAME_OBJECT
				|| kind == Renderer3DModelKind.WALL_OBJECT;
		}
	}

	private static final class WorldMeshUploadSignature {
		private static final long FNV_OFFSET_BASIS = -3750763034362895579L;
		private static final long FNV_PRIME = 1099511628211L;

		private final int vertexCount;
		private final int indexCount;
		private final int triangleCount;
		private final int centerX;
		private final int centerY;
		private final int viewportWidth;
		private final int viewportHeight;
		private final int brightnessBits;
		private final int fogStrengthBits;
		private final int lightingModeBits;
		private final int geometryModeBits;
		private final boolean hideRoofs;
		private final WorldMeshTriangleFilter triangleFilter;
		private final long hash;

		private WorldMeshUploadSignature(
			int vertexCount,
			int indexCount,
			int triangleCount,
			int centerX,
			int centerY,
			int viewportWidth,
			int viewportHeight,
			int brightnessBits,
			int fogStrengthBits,
			int lightingModeBits,
			int geometryModeBits,
			boolean hideRoofs,
			WorldMeshTriangleFilter triangleFilter,
			long hash) {
			this.vertexCount = vertexCount;
			this.indexCount = indexCount;
			this.triangleCount = triangleCount;
			this.centerX = centerX;
			this.centerY = centerY;
			this.viewportWidth = viewportWidth;
			this.viewportHeight = viewportHeight;
			this.brightnessBits = brightnessBits;
			this.fogStrengthBits = fogStrengthBits;
			this.lightingModeBits = lightingModeBits;
			this.geometryModeBits = geometryModeBits;
			this.hideRoofs = hideRoofs;
			this.triangleFilter = triangleFilter == null ? WorldMeshTriangleFilter.ALL_STATIC : triangleFilter;
			this.hash = hash;
		}

		private static WorldMeshUploadSignature from(
			Renderer3DMeshFrame meshFrame,
			int vertexCount,
			int indexCount) {
			return from(meshFrame, vertexCount, indexCount, WorldMeshTriangleFilter.ALL_STATIC);
		}

		private static WorldMeshUploadSignature from(
			Renderer3DMeshFrame meshFrame,
			int vertexCount,
			int indexCount,
			WorldMeshTriangleFilter triangleFilter) {
			int triangleCount = meshFrame.getTriangleCount();
			int brightnessBits = Float.floatToIntBits(RendererDayNightCycle.currentBrightnessMultiplier());
			int fogStrengthBits = RendererFogSettings.getMode().ordinal();
			int lightingModeBits = RendererLightingSettings.getMode().ordinal();
			int geometryModeBits = RendererGeometrySettings.getMode().ordinal();
			boolean hideRoofs = Config.C_HIDE_ROOFS;
			long hash = FNV_OFFSET_BASIS;
			hash = mix(hash, vertexCount);
			hash = mix(hash, indexCount);
			hash = mix(hash, triangleCount);
			hash = mix(hash, meshFrame.getCenterX());
			hash = mix(hash, meshFrame.getCenterY());
			hash = mix(hash, meshFrame.getViewportWidth());
			hash = mix(hash, meshFrame.getViewportHeight());
			hash = mix(hash, brightnessBits);
			hash = mix(hash, fogStrengthBits);
			hash = mix(hash, lightingModeBits);
			hash = mix(hash, geometryModeBits);
			hash = mix(hash, hideRoofs ? 1 : 0);
			hash = mix(hash, triangleFilter == null ? 0 : triangleFilter.ordinal());

			float[] vertices = meshFrame.getVertices();
			int vertexFloatCount = Math.min(vertices.length, vertexCount * Renderer3DMeshFrame.FLOATS_PER_VERTEX);
			for (int i = 0; i < vertexFloatCount; i++) {
				hash = mix(hash, Float.floatToIntBits(vertices[i]));
			}

			int[] indices = meshFrame.getIndices();
			for (int i = 0; i < indexCount && i < indices.length; i++) {
				hash = mix(hash, indices[i]);
			}

			int[] triangleTextures = meshFrame.getTriangleTextures();
			int[] triangleLegacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			for (int i = 0; i < triangleCount; i++) {
				hash = mix(hash, i < triangleTextures.length ? triangleTextures[i] : 0);
				hash = mix(hash, i < triangleLegacyDrawOrders.length ? triangleLegacyDrawOrders[i] : -1);
				Renderer3DModelKind kind =
					i < triangleModelKinds.length ? triangleModelKinds[i] : Renderer3DModelKind.UNCLASSIFIED;
				hash = mix(hash, kind.ordinal());
			}

			return new WorldMeshUploadSignature(
				vertexCount,
				indexCount,
				triangleCount,
				meshFrame.getCenterX(),
				meshFrame.getCenterY(),
				meshFrame.getViewportWidth(),
				meshFrame.getViewportHeight(),
				brightnessBits,
				fogStrengthBits,
				lightingModeBits,
				geometryModeBits,
				hideRoofs,
				triangleFilter,
				hash);
		}

		private static long mix(long hash, int value) {
			hash ^= value & 0xffffffffL;
			return hash * FNV_PRIME;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof WorldMeshUploadSignature)) {
				return false;
			}
			WorldMeshUploadSignature signature = (WorldMeshUploadSignature) other;
			return vertexCount == signature.vertexCount
				&& indexCount == signature.indexCount
				&& triangleCount == signature.triangleCount
				&& centerX == signature.centerX
				&& centerY == signature.centerY
				&& viewportWidth == signature.viewportWidth
				&& viewportHeight == signature.viewportHeight
				&& brightnessBits == signature.brightnessBits
				&& fogStrengthBits == signature.fogStrengthBits
				&& lightingModeBits == signature.lightingModeBits
				&& geometryModeBits == signature.geometryModeBits
				&& hideRoofs == signature.hideRoofs
				&& triangleFilter == signature.triangleFilter
				&& hash == signature.hash;
		}

		@Override
		public int hashCode() {
			int result = vertexCount;
			result = 31 * result + indexCount;
			result = 31 * result + triangleCount;
			result = 31 * result + centerX;
			result = 31 * result + centerY;
			result = 31 * result + viewportWidth;
			result = 31 * result + viewportHeight;
			result = 31 * result + brightnessBits;
			result = 31 * result + fogStrengthBits;
			result = 31 * result + lightingModeBits;
			result = 31 * result + geometryModeBits;
			result = 31 * result + (hideRoofs ? 1 : 0);
			result = 31 * result + triangleFilter.ordinal();
			result = 31 * result + (int) (hash ^ (hash >>> 32));
			return result;
		}
	}

	private static final class WorldMeshTextureBatch {
		private final int textureId;
		private final StaticWorldMaterialPass materialPass;
		private final Renderer3DModelKind modelKind;
		private int startIndex;
		private int indexCount;
		private int writeIndex;

		private WorldMeshTextureBatch(int textureId) {
			this(textureId, StaticWorldMaterialPass.OPAQUE, Renderer3DModelKind.UNCLASSIFIED);
		}

		private WorldMeshTextureBatch(
			int textureId,
			StaticWorldMaterialPass materialPass,
			Renderer3DModelKind modelKind) {
			this.textureId = textureId;
			this.materialPass = materialPass;
			this.modelKind = modelKind == null ? Renderer3DModelKind.UNCLASSIFIED : modelKind;
		}

		private boolean isTextureBacked() {
			return textureId >= 0;
		}
	}

	private static final class WorldMeshTextureBatchKey {
		private final int textureId;
		private final StaticWorldMaterialPass materialPass;
		private final Renderer3DModelKind modelKind;

		private WorldMeshTextureBatchKey(
			int textureId,
			StaticWorldMaterialPass materialPass,
			Renderer3DModelKind modelKind) {
			this.textureId = textureId;
			this.materialPass = materialPass == null ? StaticWorldMaterialPass.OPAQUE : materialPass;
			this.modelKind = modelKind == null ? Renderer3DModelKind.UNCLASSIFIED : modelKind;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof WorldMeshTextureBatchKey)) {
				return false;
			}
			WorldMeshTextureBatchKey key = (WorldMeshTextureBatchKey) other;
			return textureId == key.textureId
				&& materialPass == key.materialPass
				&& modelKind == key.modelKind;
		}

		@Override
		public int hashCode() {
			int result = textureId;
			result = 31 * result + materialPass.ordinal();
			result = 31 * result + modelKind.ordinal();
			return result;
		}
	}

	private static final class OpenGLWorldTextureCache implements AutoCloseable {
		private static final int DEFAULT_ATLAS_SIZE = 2048;
		private static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;

		private final LwjglBindings gl;
		private final Map<Integer, OpenGLTextureRegion> textureRegionsById = new HashMap<>();
		private final Map<Integer, Long> textureSignaturesById = new HashMap<>();
		private final List<OpenGLTextureAtlas> atlases = new ArrayList<>();
		private long lastChunkTextureSignature = Long.MIN_VALUE;
		private WorldTextureUploadStats lastChunkTextureStats;

		private OpenGLWorldTextureCache(LwjglBindings gl) {
			this.gl = gl;
		}

		private WorldTextureUploadStats uploadReferencedTextures(Renderer3DMeshFrame meshFrame) throws Exception {
			int referencedTextures = 0;
			int cachedTextures = 0;
			int uploadedTextures = 0;
			int missingTextures = 0;
			int triangleCount = Math.min(meshFrame.getTriangleCount(), meshFrame.getTriangleTextures().length);
			int[] triangleTextures = meshFrame.getTriangleTextures();
			Set<Integer> seenTextureIds = new HashSet<>();

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				int textureId = triangleTextures[triangle];
				if (!isTextureBacked(meshFrame, textureId) || !seenTextureIds.add(textureId)) {
					continue;
				}

				referencedTextures++;
				Renderer3DTextureData textureData = meshFrame.getTexture(textureId);
				if (textureData == null) {
					missingTextures++;
				} else if (uploadIfNeeded(textureData)) {
					uploadedTextures++;
				} else {
					cachedTextures++;
				}
			}

			return new WorldTextureUploadStats(
				referencedTextures,
				cachedTextures,
				uploadedTextures,
				missingTextures,
				atlases.size());
		}

		private WorldTextureUploadStats uploadReferencedTextures(
			Renderer3DFrame frame,
			Renderer3DWorldChunkFrame chunkFrame) throws Exception {
			long chunkTextureSignature = chunkTextureUploadSignature(frame, chunkFrame);
			if (lastChunkTextureStats != null
				&& lastChunkTextureStats.missingTextures == 0
				&& lastChunkTextureSignature == chunkTextureSignature) {
				return new WorldTextureUploadStats(
					lastChunkTextureStats.referencedTextures,
					lastChunkTextureStats.referencedTextures,
					0,
					0,
					atlases.size());
			}

			int referencedTextures = 0;
			int cachedTextures = 0;
			int uploadedTextures = 0;
			int missingTextures = 0;
			Set<Integer> seenTextureIds = new HashSet<>();

			for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
				int triangleCount = chunk.getTriangleCount();
				for (int triangle = 0; triangle < triangleCount; triangle++) {
					int textureId = chunk.getTriangleTexture(triangle);
					int fallbackTextureId = chunk.getTriangleFallbackColor(triangle);
					if (isTextureBacked(frame, textureId) && seenTextureIds.add(textureId)) {
						referencedTextures++;
						Renderer3DTextureData textureData = frame.getTexture(textureId);
						if (textureData == null) {
							missingTextures++;
						} else if (uploadIfNeeded(textureData)) {
							uploadedTextures++;
						} else {
							cachedTextures++;
						}
					}
					if (textureId == LEGACY_TRANSPARENT_TEXTURE
						&& isTextureBacked(frame, fallbackTextureId)
						&& seenTextureIds.add(fallbackTextureId)) {
						referencedTextures++;
						Renderer3DTextureData textureData = frame.getTexture(fallbackTextureId);
						if (textureData == null) {
							missingTextures++;
						} else if (uploadIfNeeded(textureData)) {
							uploadedTextures++;
						} else {
							cachedTextures++;
						}
					}
				}
			}

			WorldTextureUploadStats stats = new WorldTextureUploadStats(
				referencedTextures,
				cachedTextures,
				uploadedTextures,
				missingTextures,
				atlases.size());
			lastChunkTextureSignature = chunkTextureSignature;
			lastChunkTextureStats = stats;
			return stats;
		}

		private long chunkTextureUploadSignature(Renderer3DFrame frame, Renderer3DWorldChunkFrame chunkFrame) {
			long signature = 1469598103934665603L;
			signature = mixSignature(signature, frame == null ? 0 : frame.getTextures().length);
			signature = mixSignature(signature, chunkFrame == null ? 0 : chunkFrame.getChunkCount());
			if (chunkFrame != null) {
				Set<Integer> seenTextureIds = new HashSet<>();
				for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
					signature = mixSignature(signature, chunk.getSignature());
					signature = mixSignature(signature, chunk.getTriangleCount());
					int triangleCount = chunk.getTriangleCount();
					for (int triangle = 0; triangle < triangleCount; triangle++) {
						signature = mixTextureSignature(frame, signature, seenTextureIds, chunk.getTriangleTexture(triangle));
						if (chunk.getTriangleTexture(triangle) == LEGACY_TRANSPARENT_TEXTURE) {
							signature = mixTextureSignature(
								frame,
								signature,
								seenTextureIds,
								chunk.getTriangleFallbackColor(triangle));
						}
					}
				}
			}
			return signature;
		}

		private long mixTextureSignature(
			Renderer3DFrame frame,
			long signature,
			Set<Integer> seenTextureIds,
			int textureId) {
			if (!isTextureBacked(frame, textureId) || !seenTextureIds.add(textureId)) {
				return signature;
			}
			Renderer3DTextureData textureData = frame.getTexture(textureId);
			signature = mixSignature(signature, textureId);
			return mixSignature(signature, textureData == null ? 0L : textureData.getSignature());
		}

		private long mixSignature(long signature, long value) {
			signature ^= value;
			return signature * 1099511628211L;
		}

		private OpenGLTextureRegion getRegion(Renderer3DMeshFrame meshFrame, int textureId) {
			if (!isTextureBacked(meshFrame, textureId)) {
				return null;
			}
			Renderer3DTextureData textureData = meshFrame.getTexture(textureId);
			return textureData == null ? null : textureRegionsById.get(textureData.getTextureId());
		}

		private OpenGLTextureRegion getRegion(Renderer3DFrame frame, int textureId) {
			if (!isTextureBacked(frame, textureId)) {
				return null;
			}
			Renderer3DTextureData textureData = frame.getTexture(textureId);
			return textureData == null ? null : textureRegionsById.get(textureData.getTextureId());
		}

		private boolean isTextureBacked(Renderer3DMeshFrame meshFrame, int textureId) {
			return textureId >= 0 && textureId < meshFrame.getTextureCount();
		}

		private boolean isTextureBacked(Renderer3DFrame frame, int textureId) {
			return frame != null && textureId >= 0 && textureId < frame.getTextures().length;
		}

		private boolean uploadIfNeeded(Renderer3DTextureData textureData) throws Exception {
			OpenGLTextureRegion existingRegion = textureRegionsById.get(textureData.getTextureId());
			Long existingSignature = textureSignaturesById.get(textureData.getTextureId());
			if (existingRegion != null
				&& existingRegion.getWidth() == textureData.getWidth()
				&& existingRegion.getHeight() == textureData.getHeight()
				&& existingSignature != null
				&& existingSignature.longValue() == textureData.getSignature()) {
				return false;
			}
			if (existingRegion != null
				&& existingRegion.getWidth() == textureData.getWidth()
				&& existingRegion.getHeight() == textureData.getHeight()) {
				OpenGLTextureRegion updatedRegion = updateTextureRegion(textureData, existingRegion);
				textureRegionsById.put(textureData.getTextureId(), updatedRegion);
				textureSignaturesById.put(textureData.getTextureId(), textureData.getSignature());
				return true;
			}

			OpenGLTextureRegion region = null;
			for (OpenGLTextureAtlas atlas : atlases) {
				region = atlas.upload(textureData);
				if (region != null) {
					break;
				}
			}

			if (region == null) {
				OpenGLTextureAtlas atlas = OpenGLTextureAtlas.create(gl, DEFAULT_ATLAS_SIZE, DEFAULT_ATLAS_SIZE);
				atlases.add(atlas);
				region = atlas.upload(textureData);
				if (region == null) {
					throw new IllegalArgumentException(
						"world texture is too large for OpenGL atlas: "
						+ textureData.getWidth()
						+ "x"
						+ textureData.getHeight());
				}
				log("OpenGL world texture atlas allocated: "
					+ DEFAULT_ATLAS_SIZE
					+ "x"
					+ DEFAULT_ATLAS_SIZE
					+ " texture="
					+ atlas.textureId);
			}

			textureRegionsById.put(textureData.getTextureId(), region);
			textureSignaturesById.put(textureData.getTextureId(), textureData.getSignature());
			return true;
		}

		private OpenGLTextureRegion updateTextureRegion(
			Renderer3DTextureData textureData,
			OpenGLTextureRegion region) throws Exception {
			gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
			gl.glTexSubImage2D(
				gl.GL_TEXTURE_2D,
				0,
				region.getX() - OpenGLTextureAtlas.PADDING,
				region.getY() - OpenGLTextureAtlas.PADDING,
				textureData.getWidth() + OpenGLTextureAtlas.PADDING * 2,
				textureData.getHeight() + OpenGLTextureAtlas.PADDING * 2,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				buildPaddedRgbaPixels(
					textureData.copyToDirectRgbaBuffer(),
					textureData.getWidth(),
					textureData.getHeight(),
					OpenGLTextureAtlas.PADDING));
			return new OpenGLTextureRegion(
				region.getTextureId(),
				region.getX(),
				region.getY(),
				region.getWidth(),
				region.getHeight(),
				region.getU0(),
				region.getV0(),
				region.getU1(),
				region.getV1(),
				textureData.hasTransparency());
		}

		@Override
		public void close() throws Exception {
			for (OpenGLTextureAtlas atlas : atlases) {
				atlas.close();
			}
			atlases.clear();
			textureRegionsById.clear();
			textureSignaturesById.clear();
		}
	}

	private static final class WorldTextureUploadStats {
		private final int referencedTextures;
		private final int cachedTextures;
		private final int uploadedTextures;
		private final int missingTextures;
		private final int atlases;

		private WorldTextureUploadStats(
			int referencedTextures,
			int cachedTextures,
			int uploadedTextures,
			int missingTextures,
			int atlases) {
			this.referencedTextures = referencedTextures;
			this.cachedTextures = cachedTextures;
			this.uploadedTextures = uploadedTextures;
			this.missingTextures = missingTextures;
			this.atlases = atlases;
		}
	}

	private static final class KeyBinding {
		private final int glfwKey;
		private final int awtKeyCode;
		private final char normalChar;
		private final char shiftedChar;

		private KeyBinding(int glfwKey, int awtKeyCode, char normalChar, char shiftedChar) {
			this.glfwKey = glfwKey;
			this.awtKeyCode = awtKeyCode;
			this.normalChar = normalChar;
			this.shiftedChar = shiftedChar;
		}

		private char keyChar(boolean shiftDown) {
			return shiftDown ? shiftedChar : normalChar;
		}

		private boolean postsPhysicalEvents() {
			return normalChar == KeyEvent.CHAR_UNDEFINED
				|| normalChar == '\b'
				|| normalChar == '\n'
				|| normalChar == '\t'
				|| normalChar == 27;
		}

		private boolean postsRepeatPressEvents() {
			return normalChar == '\b';
		}
	}

	private interface ScrollHandler {
		void handle(long window, double xOffset, double yOffset);
	}

	private interface GlfwKeyHandler {
		void handle(long window, int key, int scanCode, int action, int mods);
	}

	private interface CharHandler {
		void handle(long window, int codepoint);
	}

	private interface WindowFocusHandler {
		void handle(long window, boolean focused);
	}

	private interface OpenGLPassAction {
		void run() throws Exception;
	}

	private static final class OpenGLFrameCapture {
		private final File directory;
		private final int sequence;
		private final List<String> layers = new ArrayList<String>();
		private boolean failed;
		private String failureMessage;

		private OpenGLFrameCapture(File directory, int sequence) {
			this.directory = directory;
			this.sequence = sequence;
		}

		private static OpenGLFrameCapture create(
			int sequence,
			Frame frame,
			boolean worldReplacementComposite,
			OpenGLFramePresenter presenter) throws Exception {
			File baseDirectory = frameCaptureBaseDirectory();
			String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
			File directory = new File(
				baseDirectory,
				"capture-" + timestamp + "-" + String.format("%03d", sequence));
			if (!directory.mkdirs() && !directory.isDirectory()) {
				throw new IllegalStateException("could not create " + directory.getAbsolutePath());
			}
			OpenGLFrameCapture capture = new OpenGLFrameCapture(directory, sequence);
			capture.writeMetadata(frame, worldReplacementComposite, presenter);
			return capture;
		}

		private String getDirectoryPath() {
			return directory.getAbsolutePath();
		}

		private boolean hasFailed() {
			return failed;
		}

		private void markFailed(Throwable throwable) {
			failed = true;
			failureMessage = throwable == null ? "unknown" : throwable.getMessage();
		}

		private void writeFrameInputs(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			writeImage("00-legacy-source.png", imageFromFrame(frame));
			writeDepthDiagnostics(frame);
			writeWorldFaces(frame);
			writeSpriteCommands(frame);
			writeWorldSpriteCommands(frame, presenter);
			writeCompositeSceneCommands(frame, presenter);
			writeStaticWorldCommands(frame, presenter);
			writeStaticWorldMaterialTriangles(frame, presenter);
			writeStaticRangeCandidates(frame, presenter);
			writeFrontOccluderCandidates(frame, presenter);
			writeSpriteSubmissions(frame);
			writeCharacterSprites(frame, null);
			writeSpriteAnchors(frame);
		}

		private void writeLayer(String name, BufferedImage image) throws Exception {
			if (image == null) {
				return;
			}
			String fileName = name + ".png";
			writeImage(fileName, image);
			layers.add(fileName);
		}

		private void writeSummary(
			Frame frame,
			boolean worldReplacementComposite,
			boolean failed) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "summary.txt"));
			try {
				writer.println("sequence=" + sequence);
				writer.println("failed=" + failed);
				if (failureMessage != null) {
					writer.println("failure=" + failureMessage);
				}
				writer.println("source=" + frame.sourceWidth + "x" + frame.sourceHeight);
				writer.println("target=" + frame.targetWidth + "x" + frame.targetHeight);
				writer.println("worldReplacementComposite=" + worldReplacementComposite);
				writeRendererMode(writer, worldReplacementComposite);
				writer.println("layers=");
				for (String layer : layers) {
					writer.println(layer);
				}
			} finally {
				writer.close();
			}
		}

		private void writeEntityRestoreStats(OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "entity-restore-stats.tsv"));
			try {
				writer.println(
					"legacySpriteId\tcommands\tvisiblePixels\tdirectPixels\tfallbacks\tskipped\tatlasFull"
						+ "\tdepthEvaluations\tdepthVisiblePixels\tdepthMisses\tdepthSourcePixels"
						+ "\tdepthOccludedPixels\tdepthClippedPixels\tdepthOutOfBoundsPixels"
						+ "\tminAnchorDepth\tmaxAnchorDepth\tbounds\tdescription");
				List<LegacyEntitySpriteDebugStats> stats =
					new ArrayList<LegacyEntitySpriteDebugStats>(presenter.legacyEntitySpriteDebugById.values());
				Collections.sort(stats, new Comparator<LegacyEntitySpriteDebugStats>() {
					@Override
					public int compare(LegacyEntitySpriteDebugStats a, LegacyEntitySpriteDebugStats b) {
						return a.legacySpriteId - b.legacySpriteId;
					}
				});
				for (LegacyEntitySpriteDebugStats stat : stats) {
					writer.println(stat.legacySpriteId
						+ "\t" + stat.commands
						+ "\t" + stat.visiblePixels
						+ "\t" + stat.directPixels
						+ "\t" + stat.fallbacks
						+ "\t" + stat.skipped
						+ "\t" + stat.atlasFull
						+ "\t" + stat.depthEvaluations
						+ "\t" + stat.depthVisiblePixels
						+ "\t" + stat.depthMisses
						+ "\t" + stat.depthSourcePixels
						+ "\t" + stat.depthOccludedPixels
						+ "\t" + stat.depthClippedPixels
						+ "\t" + stat.depthOutOfBoundsPixels
						+ "\t" + emptyIfUnset(stat.minAnchorDepth, Integer.MAX_VALUE)
						+ "\t" + emptyIfUnset(stat.maxAnchorDepth, Integer.MIN_VALUE)
						+ "\t" + bounds(stat.minX, stat.minY, stat.maxX, stat.maxY)
						+ "\t" + stat.describe());
				}
			} finally {
				writer.close();
			}
		}

		private void writeEntityDepthEvaluations(OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "entity-depth-evaluations.tsv"));
			try {
				writer.println(
					"index\tsequence\tphase\tlegacySpriteId\tx\ty\twidth\theight\ttopX16\tbottomX16"
						+ "\tanchorFaceId\tanchorLegacyDrawOrder\tanchorAverageDepth\tanchorCameraZ"
						+ "\tanchorMatchMode\tanchorMatchScore"
						+ "\tanchorDrawX\tanchorDrawY\tanchorDrawWidth\tanchorDrawHeight"
						+ "\tanchorDeltaX\tanchorDeltaY\tanchorDeltaWidth\tanchorDeltaHeight"
						+ "\tsourcePixels\tvisiblePixels\toccludedPixels\tclippedPixels"
						+ "\toutOfBoundsPixels\tterrainOccludedPixels\twallOccludedPixels"
						+ "\troofOccludedPixels\tgameObjectOccludedPixels\twallObjectOccludedPixels"
						+ "\tminOccluderLegacyDrawOrder\tmaxOccluderLegacyDrawOrder"
						+ "\tminOccluderDepth\tmaxOccluderDepth"
						+ "\tdominantOccluderKind\tdominantOccluderFaceId\tdominantOccluderModelIndex"
						+ "\tdominantOccluderPixels\tdominantOccluderLegacyDrawOrder"
						+ "\tdominantOccluderDepth"
						+ "\tfullyOccluded\tvisiblePct\toccludedPct\tbounds");
				for (LegacyEntitySpriteDepthEvaluation evaluation : presenter.legacyEntitySpriteDepthEvaluations) {
					writer.println(evaluation.index
						+ "\t" + evaluation.sequence
						+ "\t" + evaluation.phase
						+ "\t" + evaluation.legacySpriteId
						+ "\t" + evaluation.x
						+ "\t" + evaluation.y
						+ "\t" + evaluation.width
						+ "\t" + evaluation.height
						+ "\t" + evaluation.topX16
						+ "\t" + evaluation.bottomX16
						+ "\t" + emptyIfUnset(evaluation.anchorFaceId, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorLegacyDrawOrder, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorAverageDepth, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorCameraZ, Integer.MIN_VALUE)
						+ "\t" + safeTsv(evaluation.anchorMatchMode)
						+ "\t" + emptyIfUnset(evaluation.anchorMatchScore, Integer.MAX_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDrawX, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDrawY, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDrawWidth, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDrawHeight, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDeltaX, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDeltaY, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDeltaWidth, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.anchorDeltaHeight, Integer.MIN_VALUE)
						+ "\t" + evaluation.sourcePixels
						+ "\t" + evaluation.visiblePixels
						+ "\t" + evaluation.occludedPixels
						+ "\t" + evaluation.clippedPixels
						+ "\t" + evaluation.outOfBoundsPixels
						+ "\t" + evaluation.terrainOccludedPixels
						+ "\t" + evaluation.wallOccludedPixels
						+ "\t" + evaluation.roofOccludedPixels
						+ "\t" + evaluation.gameObjectOccludedPixels
						+ "\t" + evaluation.wallObjectOccludedPixels
						+ "\t" + emptyIfUnset(evaluation.minOccluderLegacyDrawOrder, Integer.MAX_VALUE)
						+ "\t" + emptyIfUnset(evaluation.maxOccluderLegacyDrawOrder, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.minOccluderDepth, Integer.MAX_VALUE)
						+ "\t" + emptyIfUnset(evaluation.maxOccluderDepth, Integer.MIN_VALUE)
						+ "\t" + safeTsv(evaluation.dominantOccluderKind)
						+ "\t" + emptyIfUnset(evaluation.dominantOccluderFaceId, -1)
						+ "\t" + emptyIfUnset(evaluation.dominantOccluderModelIndex, -1)
						+ "\t" + evaluation.dominantOccluderPixels
						+ "\t" + emptyIfUnset(evaluation.dominantOccluderLegacyDrawOrder, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(evaluation.dominantOccluderDepth, Integer.MIN_VALUE)
						+ "\t" + evaluation.fullyOccluded
						+ "\t" + percent(evaluation.visiblePixels, evaluation.sourcePixels)
						+ "\t" + percent(evaluation.occludedPixels, evaluation.sourcePixels)
						+ "\t" + bounds(evaluation.x, evaluation.y, evaluation.x + evaluation.width, evaluation.y + evaluation.height));
				}
			} finally {
				writer.close();
			}
		}

		private void writeWorldSpriteBatchStats(OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "world-sprite-batch-stats.tsv"));
			try {
				writer.println("commands\ttextureBatches");
				writer.println(presenter.worldSpriteDepthDrawCommands
					+ "\t" + presenter.worldSpriteDepthTextureBatches);
			} finally {
				writer.close();
			}
		}

		private void writeShaderVertexParityStats(OpenGLFramePresenter presenter) throws Exception {
			ShaderVertexParityStats stats = presenter == null || presenter.worldMeshRenderer == null
				? ShaderVertexParityStats.EMPTY
				: presenter.worldMeshRenderer.getLastShaderVertexParityStats();
			PrintWriter writer = new PrintWriter(new File(directory, "shader-vertex-parity.txt"));
			try {
				writer.println("vertexCount=" + stats.vertexCount);
				writer.println("positionMismatches=" + stats.positionMismatches);
				writer.println("textureCoordMismatches=" + stats.textureCoordMismatches);
				writer.println("colorMismatches=" + stats.colorMismatches);
				writer.println("maxAbsoluteDelta=" + stats.maxAbsoluteDelta);
			} finally {
				writer.close();
			}
		}

		private void writeMetadata(
			Frame frame,
			boolean worldReplacementComposite,
			OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "metadata.txt"));
			try {
				writer.println("sequence=" + sequence);
				writer.println("created=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
				writer.println("source=" + frame.sourceWidth + "x" + frame.sourceHeight);
				writer.println("target=" + frame.targetWidth + "x" + frame.targetHeight);
				writer.println("linearFiltering=" + frame.linearFiltering);
				writer.println("worldReplacementComposite=" + worldReplacementComposite);
				writeRendererMode(writer, worldReplacementComposite);
				if (frame.renderer2DFrame != null) {
					Renderer2DFrame renderer2DFrame = frame.renderer2DFrame;
					writer.println("renderer2D=" + renderer2DFrame.getWidth() + "x" + renderer2DFrame.getHeight());
					writer.println("spriteCommands=" + renderer2DFrame.getSpriteCommands().length);
					writer.println("textCommands=" + renderer2DFrame.getTextCommands().length);
					writer.println("primitiveCommands=" + renderer2DFrame.getPrimitiveCommands().length);
					writer.println("rotatedSpriteCommands=" + renderer2DFrame.getRotatedSpriteCommands().length);
					writer.println("circleCommands=" + renderer2DFrame.getCircleCommands().length);
				}
				if (frame.renderer3DFrame != null) {
					Renderer3DFrame renderer3DFrame = frame.renderer3DFrame;
					writer.println("renderer3DViewport="
						+ renderer3DFrame.getViewportWidth()
						+ "x"
						+ renderer3DFrame.getViewportHeight());
					writer.println("cameraOffset="
						+ renderer3DFrame.getCameraOffsetX()
						+ ","
						+ renderer3DFrame.getCameraOffsetY()
						+ ","
						+ renderer3DFrame.getCameraOffsetZ());
					writer.println("cameraRotation="
						+ renderer3DFrame.getCameraRotationX()
						+ ","
						+ renderer3DFrame.getCameraRotationY()
						+ ","
						+ renderer3DFrame.getCameraRotationZ());
					writer.println("worldFaces=" + renderer3DFrame.getWorldFaceCount());
					for (Renderer3DModelKind kind : Renderer3DModelKind.values()) {
						writer.println("worldFaces." + kind.name() + "=" + renderer3DFrame.getWorldFaceCount(kind));
					}
					writer.println("spriteSubmissions=" + renderer3DFrame.getSpriteSubmissionCount());
					writer.println("characterSprites=" + renderer3DFrame.getCharacterSpriteCount());
					writer.println("spriteAnchors=" + renderer3DFrame.getSpriteAnchorCount());
					Renderer3DDepthFrame depthFrame = renderer3DFrame.getDepthFrame();
					if (depthFrame != null) {
						writer.println("depthFrame="
							+ depthFrame.getWidth()
							+ "x"
							+ depthFrame.getHeight()
							+ " acceptedFaces="
							+ depthFrame.getAcceptedFaceCount()
							+ " triangles="
							+ depthFrame.getTriangleCount()
							+ " pixels="
							+ depthFrame.getPixelWriteCount());
					}
					if (presenter != null && presenter.worldMeshRenderer != null) {
						writer.println("worldMeshDraw=triangles="
							+ presenter.worldMeshRenderer.getLastDrawTriangles()
							+ " occluderTriangles="
							+ presenter.worldMeshRenderer.getLastDrawOccluderTriangles()
							+ " batches="
							+ presenter.worldMeshRenderer.getLastDrawBatches()
							+ " calls="
							+ presenter.worldMeshRenderer.getLastDrawCalls());
					}
				}
			} finally {
				writer.close();
			}
		}

		private static void writeRendererMode(PrintWriter writer, boolean worldReplacementComposite) {
			writer.println("rendererMode.worldMeshEnabled=" + WORLD_MESH_ENABLED);
			writer.println("rendererMode.worldMeshVisible=" + WORLD_MESH_VISIBLE);
			writer.println("rendererMode.worldMeshTexturedVisible=" + WORLD_MESH_TEXTURED_VISIBLE);
			writer.println("rendererMode.worldMeshTexturedStaticVisible=" + WORLD_MESH_TEXTURED_STATIC_VISIBLE);
			writer.println("rendererMode.worldMeshTexturedShader=" + WORLD_MESH_TEXTURED_SHADER);
			writer.println("rendererMode.worldMeshShaderNativeVbo=" + WORLD_MESH_TEXTURED_SHADER);
			writer.println("rendererMode.worldMeshTexturedAlpha=" + TEXTURED_DIAGNOSTIC_ALPHA);
			writer.println("rendererMode.worldStaticTextures=" + WORLD_STATIC_TEXTURES);
			writer.println("rendererMode.worldChunksVisible=" + WORLD_CHUNKS_VISIBLE);
			writer.println("rendererMode.worldChunksFilledVisible=" + WORLD_CHUNKS_FILLED_VISIBLE);
			writer.println("rendererMode.worldChunksTexturedVisible=" + WORLD_CHUNKS_TEXTURED_VISIBLE);
			writer.println("rendererMode.worldReplacementComposite=" + worldReplacementComposite);
			writer.println("rendererMode.worldReplacementCompositeConfigured=" + WORLD_REPLACEMENT_COMPOSITE);
			writer.println("rendererMode.worldChunksReplacementCompositeConfigured=" + WORLD_CHUNKS_REPLACEMENT_COMPOSITE);
			writer.println("rendererMode.worldChunksTrustedReplacement=" + WORLD_CHUNKS_TRUSTED_REPLACEMENT);
			writer.println("rendererMode.worldChunksResidentObjects=" + WORLD_CHUNKS_RESIDENT_OBJECTS);
			writer.println("rendererMode.worldSpritesVisible=" + WORLD_SPRITES_VISIBLE);
		}

		private void writeDepthDiagnostics(Frame frame) throws Exception {
			if (frame.renderer3DFrame == null || frame.renderer3DFrame.getDepthFrame() == null) {
				return;
			}
			Renderer3DDepthFrame depthFrame = frame.renderer3DFrame.getDepthFrame();
			int width = depthFrame.getWidth();
			int height = depthFrame.getHeight();
			int[] pixels = new int[width * height];
			depthFrame.copyColorTo(pixels);
			writeImage("00-depth-color.png", opaqueRgbImage(width, height, pixels));
			depthFrame.copyKindColorTo(pixels);
			writeImage("00-depth-kind.png", argbImage(width, height, pixels));
			depthFrame.copyEntityOccluderMaskTo(pixels);
			writeImage("00-entity-occluder-mask.png", argbImage(width, height, pixels));
		}

		private void writeWorldFaces(Frame frame) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "world-faces.tsv"));
			try {
				writer.println(
					"index\tmodelKind\tmodelIndex\tfaceId\ttexture\tcolor\torientation"
						+ "\tlegacyDrawOrder\taverageDepth\tvertexCount\trenderVertexCount"
						+ "\tcameraX\tcameraY\tcameraZ\tscreenX\tscreenY\tlight\ttextureU\ttextureV");
				if (frame.renderer3DFrame == null) {
					return;
				}
				List<Renderer3DFrame.FaceCommand> faces = frame.renderer3DFrame.getWorldFaces();
				for (int i = 0; i < faces.size(); i++) {
					Renderer3DFrame.FaceCommand face = faces.get(i);
					writer.println(i
						+ "\t" + face.getModelKind()
						+ "\t" + face.getModelIndex()
						+ "\t" + face.getFaceId()
						+ "\t" + face.getTexture()
						+ "\t" + face.getColor()
						+ "\t" + face.getOrientation()
						+ "\t" + face.getLegacyDrawOrder()
						+ "\t" + face.getAverageDepth()
						+ "\t" + face.getVertexCount()
						+ "\t" + face.getRenderVertexCount()
						+ "\t" + joinInts(face.getRenderCameraX())
						+ "\t" + joinInts(face.getRenderCameraY())
						+ "\t" + joinInts(face.getRenderCameraZ())
						+ "\t" + joinInts(face.getRenderScreenX())
						+ "\t" + joinInts(face.getRenderScreenY())
						+ "\t" + joinInts(face.getRenderLight())
						+ "\t" + joinFloats(face.getRenderTextureU())
						+ "\t" + joinFloats(face.getRenderTextureV()));
				}
			} finally {
				writer.close();
			}
		}

		private void writeSpriteCommands(Frame frame) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "sprite-commands.tsv"));
			try {
				writer.println(
					"index\tsequence\tphase\tlegacySpriteId\tlegacyEntity\trequiresOrderedReplay"
						+ "\tx\ty\twidth\theight\ttopX16\tbottomX16\talpha"
						+ "\tsourceX\tsourceY\tsourceWidth\tsourceHeight"
						+ "\tspriteWidth\tspriteHeight\tmirrorX");
				if (frame.renderer2DFrame == null) {
					return;
				}
				Renderer2DFrame.SpriteCommand[] commands = frame.renderer2DFrame.getSpriteCommands();
				for (int i = 0; i < commands.length; i++) {
					Renderer2DFrame.SpriteCommand command = commands[i];
					Sprite sprite = command.getSprite();
					writer.println(i
						+ "\t" + command.getSequence()
						+ "\t" + command.getPhase()
						+ "\t" + command.getLegacySpriteId()
						+ "\t" + isLegacyEntitySpriteId(command.getLegacySpriteId())
						+ "\t" + command.requiresOrderedReplay()
						+ "\t" + command.getX()
						+ "\t" + command.getY()
						+ "\t" + command.getWidth()
						+ "\t" + command.getHeight()
						+ "\t" + command.getTopX16()
						+ "\t" + command.getBottomX16()
						+ "\t" + command.getAlpha()
						+ "\t" + command.getSourceX()
						+ "\t" + command.getSourceY()
						+ "\t" + command.getSourceWidth()
						+ "\t" + command.getSourceHeight()
						+ "\t" + (sprite == null ? 0 : sprite.getWidth())
						+ "\t" + (sprite == null ? 0 : sprite.getHeight())
						+ "\t" + command.isMirrorX());
				}
			} finally {
				writer.close();
			}
		}

		private void writeWorldSpriteCommands(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "world-sprite-commands.tsv"));
			try {
				writer.println(
					"index\tsequence\tphase\tlegacySpriteId\tworldSpriteKind\tanchorMatchMode\tanchorMatchScore"
						+ "\tanchorFaceId\tanchorLegacyDrawOrder\tanchorAverageDepth\tanchorCameraZ\tdepthOwned"
						+ "\tx\ty\twidth\theight\ttopX16\tbottomX16\talpha"
						+ "\tsourceX\tsourceY\tsourceWidth\tsourceHeight\tspriteWidth\tspriteHeight"
						+ "\tsourceCropped\tmirrorX\tskewed");
				if (frame.renderer2DFrame == null || presenter == null) {
					return;
				}
				List<WorldSpriteCommand> commands = presenter.buildOpenGLCompositeWorldSpriteCommands(
					frame,
					frame.renderer2DFrame.getSpriteCommands());
				for (int i = 0; i < commands.size(); i++) {
					WorldSpriteCommand worldCommand = commands.get(i);
					Renderer2DFrame.SpriteCommand command = worldCommand.command;
					Sprite sprite = command.getSprite();
					Renderer3DFrame.SpriteAnchor anchor = worldCommand.anchor;
					writer.println(i
						+ "\t" + command.getSequence()
						+ "\t" + command.getPhase()
						+ "\t" + command.getLegacySpriteId()
						+ "\t" + worldSpriteKind(command)
						+ "\t" + worldCommand.anchorMatch.mode
						+ "\t" + worldCommand.anchorMatch.score
						+ "\t" + (anchor == null ? "" : String.valueOf(anchor.getFaceId()))
						+ "\t" + (anchor == null ? "" : String.valueOf(anchor.getLegacyDrawOrder()))
						+ "\t" + (anchor == null ? "" : String.valueOf(anchor.getAverageDepth()))
						+ "\t" + (anchor == null ? "" : String.valueOf(anchor.getCameraZ()))
						+ "\t" + (anchor != null)
						+ "\t" + command.getX()
						+ "\t" + command.getY()
						+ "\t" + command.getWidth()
						+ "\t" + command.getHeight()
						+ "\t" + command.getTopX16()
						+ "\t" + command.getBottomX16()
						+ "\t" + command.getAlpha()
						+ "\t" + command.getSourceX()
						+ "\t" + command.getSourceY()
						+ "\t" + command.getSourceWidth()
						+ "\t" + command.getSourceHeight()
						+ "\t" + (sprite == null ? 0 : sprite.getWidth())
						+ "\t" + (sprite == null ? 0 : sprite.getHeight())
						+ "\t" + worldCommand.sourceCropped
						+ "\t" + command.isMirrorX()
						+ "\t" + worldCommand.skewed);
				}
			} finally {
				writer.close();
			}
		}

		private void writeCompositeSceneCommands(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "scene-commands.tsv"));
			try {
				writer.println(
					"index\tkind\tlegacyDrawOrder\tsequence\tminExclusiveOrder\tmaxExclusiveOrder"
						+ "\tfrontOccluderFaces\tlegacySpriteId\tworldSpriteKind\tanchorMatchMode\tanchorFaceId"
						+ "\tsourceCropped\tmirrorX\tskewed");
				if (frame.renderer2DFrame == null || presenter == null) {
					return;
				}
				List<OpenGLCompositeSceneCommand> commands = presenter.buildOpenGLCompositeSceneCommands(
					frame,
					frame.renderer2DFrame.getSpriteCommands());
				for (int i = 0; i < commands.size(); i++) {
					OpenGLCompositeSceneCommand sceneCommand = commands.get(i);
					WorldSpriteCommand worldCommand = sceneCommand.worldSpriteCommand;
					Renderer2DFrame.SpriteCommand spriteCommand =
						worldCommand == null ? null : worldCommand.command;
					Renderer3DFrame.SpriteAnchor anchor = worldCommand == null ? null : worldCommand.anchor;
					writer.println(i
						+ "\t" + sceneCommand.kind
						+ "\t" + sceneCommand.legacyDrawOrder
						+ "\t" + sceneCommand.sequence
						+ "\t" + emptyIfUnset(sceneCommand.minExclusiveOrder, Integer.MIN_VALUE)
						+ "\t" + emptyIfUnset(sceneCommand.maxExclusiveOrder, Integer.MAX_VALUE)
						+ "\t" + sceneCommand.frontOccluderFaceKeys.size()
						+ "\t" + (spriteCommand == null ? "" : String.valueOf(spriteCommand.getLegacySpriteId()))
						+ "\t" + (spriteCommand == null ? "" : worldSpriteKind(spriteCommand))
						+ "\t" + (worldCommand == null ? "" : worldCommand.anchorMatch.mode)
						+ "\t" + (anchor == null ? "" : String.valueOf(anchor.getFaceId()))
						+ "\t" + (worldCommand != null && worldCommand.sourceCropped)
						+ "\t" + (spriteCommand != null && spriteCommand.isMirrorX())
						+ "\t" + (worldCommand != null && worldCommand.skewed));
				}
			} finally {
				writer.close();
			}
		}

		private void writeStaticWorldCommands(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			List<StaticWorldCommand> commands = presenter == null
				? Collections.<StaticWorldCommand>emptyList()
				: presenter.buildOpenGLCompositeStaticWorldCommands(frame);
			PrintWriter commandWriter = new PrintWriter(new File(directory, "static-world-commands.tsv"));
			PrintWriter ownershipWriter =
				new PrintWriter(new File(directory, "static-world-face-ownership.tsv"));
			try {
				commandWriter.println(
					"index\tkind\tmodelKind\tfaceCount\ttriangleCount\tminLegacyDrawOrder\tmaxLegacyDrawOrder");
				ownershipWriter.println("commandIndex\tkind\tmodelKind\tmodelIndex\tfaceId");
				for (int commandIndex = 0; commandIndex < commands.size(); commandIndex++) {
					StaticWorldCommand command = commands.get(commandIndex);
					commandWriter.println(commandIndex
						+ "\tBASE_WORLD"
						+ "\t" + command.modelKind
						+ "\t" + command.faceKeys.size()
						+ "\t" + command.triangleCount
						+ "\t" + emptyIfUnset(command.minLegacyDrawOrder, Integer.MAX_VALUE)
						+ "\t" + emptyIfUnset(command.maxLegacyDrawOrder, Integer.MIN_VALUE));
					List<Long> faceKeys = new ArrayList<Long>(command.faceKeys);
					Collections.sort(faceKeys);
					for (Long faceKey : faceKeys) {
						ownershipWriter.println(commandIndex
							+ "\tBASE_WORLD"
							+ "\t" + command.modelKind
							+ "\t" + openGLCompositeModelIndex(faceKey.longValue())
							+ "\t" + openGLCompositeFaceId(faceKey.longValue()));
					}
				}
			} finally {
				commandWriter.close();
				ownershipWriter.close();
			}
		}

		private void writeStaticWorldMaterialTriangles(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			List<StaticWorldMaterialTriangle> triangles = presenter == null
				? Collections.<StaticWorldMaterialTriangle>emptyList()
				: presenter.buildOpenGLCompositeStaticWorldMaterialTriangles(frame);
			PrintWriter writer = new PrintWriter(new File(directory, "static-world-material-triangles.tsv"));
			try {
				writer.println(
					"triangleIndex\tmaterialPass\tmodelKind\tmodelIndex\tfaceId\ttextureId\ttextureHasTransparency");
				for (StaticWorldMaterialTriangle triangle : triangles) {
					writer.println(triangle.triangleIndex
						+ "\t" + triangle.materialPass
						+ "\t" + triangle.modelKind
						+ "\t" + triangle.modelIndex
						+ "\t" + triangle.faceId
						+ "\t" + triangle.textureId
						+ "\t" + triangle.textureHasTransparency);
				}
			} finally {
				writer.close();
			}
		}

		private void writeStaticRangeCandidates(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "static-range-candidates.tsv"));
			try {
				writer.println(
					"index\tminExclusiveOrder\tmaxExclusiveOrder\tworldSpriteCommandsAtOrder\tfinalRange"
						+ "\tstaticFaces\tterrainFaces\twallFaces\troofFaces\tgameObjectFaces\twallObjectFaces"
						+ "\toverlapFaces\toverlapTerrainFaces\toverlapWallFaces\toverlapRoofFaces"
						+ "\toverlapGameObjectFaces\toverlapWallObjectFaces\toverlapWorldSpriteCommands");
				if (frame.renderer2DFrame == null || frame.renderer3DFrame == null || presenter == null) {
					return;
				}
				List<WorldSpriteCommand> worldSpriteCommands = presenter.buildOpenGLCompositeWorldSpriteCommands(
					frame,
					frame.renderer2DFrame.getSpriteCommands());
				List<Renderer3DFrame.FaceCommand> faces = frame.renderer3DFrame.getWorldFaces();
				int index = 0;
				for (int commandIndex = 0; commandIndex < worldSpriteCommands.size();) {
					int currentOrder = worldSpriteCommands.get(commandIndex).legacyDrawOrder;
					int nextCommandIndex = commandIndex + 1;
					while (nextCommandIndex < worldSpriteCommands.size()
						&& worldSpriteCommands.get(nextCommandIndex).legacyDrawOrder == currentOrder) {
						nextCommandIndex++;
					}
					int maxExclusiveOrder = nextCommandIndex < worldSpriteCommands.size()
						? worldSpriteCommands.get(nextCommandIndex).legacyDrawOrder
						: Integer.MAX_VALUE;
					StaticRangeCandidateStats stats = staticRangeCandidateStats(
						faces,
						worldSpriteCommands,
						currentOrder,
						maxExclusiveOrder);
					writer.println(index
						+ "\t" + currentOrder
						+ "\t" + emptyIfUnset(maxExclusiveOrder, Integer.MAX_VALUE)
						+ "\t" + (nextCommandIndex - commandIndex)
						+ "\t" + (maxExclusiveOrder == Integer.MAX_VALUE)
						+ "\t" + stats.staticFaces
						+ "\t" + stats.terrainFaces
						+ "\t" + stats.wallFaces
						+ "\t" + stats.roofFaces
						+ "\t" + stats.gameObjectFaces
						+ "\t" + stats.wallObjectFaces
						+ "\t" + stats.overlapFaces
						+ "\t" + stats.overlapTerrainFaces
						+ "\t" + stats.overlapWallFaces
						+ "\t" + stats.overlapRoofFaces
						+ "\t" + stats.overlapGameObjectFaces
						+ "\t" + stats.overlapWallObjectFaces
						+ "\t" + stats.overlapWorldSpriteCommands);
					index++;
					commandIndex = nextCommandIndex;
				}
			} finally {
				writer.close();
			}
		}

		private void writeFrontOccluderCandidates(Frame frame, OpenGLFramePresenter presenter) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "front-occluder-candidates.tsv"));
			try {
				writer.println(
					"index\trangeIndex\tminExclusiveOrder\tmaxExclusiveOrder\tmodelKind\tmodelIndex\tfaceId"
						+ "\tlegacyDrawOrder\taverageDepth\toverlapWorldSpriteCommands\tbounds");
				if (frame.renderer2DFrame == null || frame.renderer3DFrame == null || presenter == null) {
					return;
				}
				List<WorldSpriteCommand> worldSpriteCommands = presenter.buildOpenGLCompositeWorldSpriteCommands(
					frame,
					frame.renderer2DFrame.getSpriteCommands());
				List<Renderer3DFrame.FaceCommand> faces = frame.renderer3DFrame.getWorldFaces();
				int index = 0;
				int rangeIndex = 0;
				for (int commandIndex = 0; commandIndex < worldSpriteCommands.size();) {
					int currentOrder = worldSpriteCommands.get(commandIndex).legacyDrawOrder;
					int nextCommandIndex = commandIndex + 1;
					while (nextCommandIndex < worldSpriteCommands.size()
						&& worldSpriteCommands.get(nextCommandIndex).legacyDrawOrder == currentOrder) {
						nextCommandIndex++;
					}
					int maxExclusiveOrder = nextCommandIndex < worldSpriteCommands.size()
						? worldSpriteCommands.get(nextCommandIndex).legacyDrawOrder
						: Integer.MAX_VALUE;
					for (Renderer3DFrame.FaceCommand face : faces) {
						int order = face.getLegacyDrawOrder();
						if (order <= currentOrder || order < 0) {
							continue;
						}
						if (maxExclusiveOrder != Integer.MAX_VALUE && order >= maxExclusiveOrder) {
							continue;
						}
						if (!isFrontOccluderCandidateKind(face.getModelKind())) {
							continue;
						}
						int[] faceBounds = boundsFor(face.getRenderScreenX(), face.getRenderScreenY());
						int overlappingSpriteCommands =
							countOverlappingWorldSpriteCommands(faceBounds, worldSpriteCommands);
						if (overlappingSpriteCommands <= 0) {
							continue;
						}
						writer.println(index
							+ "\t" + rangeIndex
							+ "\t" + currentOrder
							+ "\t" + emptyIfUnset(maxExclusiveOrder, Integer.MAX_VALUE)
							+ "\t" + face.getModelKind()
							+ "\t" + face.getModelIndex()
							+ "\t" + face.getFaceId()
							+ "\t" + order
							+ "\t" + face.getAverageDepth()
							+ "\t" + overlappingSpriteCommands
							+ "\t" + bounds(faceBounds));
						index++;
					}
					rangeIndex++;
					commandIndex = nextCommandIndex;
				}
			} finally {
				writer.close();
			}
		}

		private static StaticRangeCandidateStats staticRangeCandidateStats(
			List<Renderer3DFrame.FaceCommand> faces,
			List<WorldSpriteCommand> worldSpriteCommands,
			int minExclusiveOrder,
			int maxExclusiveOrder) {
			StaticRangeCandidateStats stats = new StaticRangeCandidateStats();
			for (Renderer3DFrame.FaceCommand face : faces) {
				int order = face.getLegacyDrawOrder();
				if (order <= minExclusiveOrder || order < 0) {
					continue;
				}
				if (maxExclusiveOrder != Integer.MAX_VALUE && order >= maxExclusiveOrder) {
					continue;
				}
				stats.recordFace(face.getModelKind(), false);
				int[] faceBounds = boundsFor(face.getRenderScreenX(), face.getRenderScreenY());
				int overlappingSpriteCommands =
					countOverlappingWorldSpriteCommands(faceBounds, worldSpriteCommands);
				if (overlappingSpriteCommands > 0) {
					stats.recordFace(face.getModelKind(), true);
					stats.overlapWorldSpriteCommands += overlappingSpriteCommands;
				}
			}
			return stats;
		}

		private static int countOverlappingWorldSpriteCommands(
			int[] faceBounds,
			List<WorldSpriteCommand> worldSpriteCommands) {
			if (faceBounds == null) {
				return 0;
			}
			int overlappingSpriteCommands = 0;
			for (WorldSpriteCommand worldSpriteCommand : worldSpriteCommands) {
				if (worldSpriteCommand == null || worldSpriteCommand.command == null) {
					continue;
				}
				Renderer2DFrame.SpriteCommand command = worldSpriteCommand.command;
				int[] spriteBounds = new int[] {
					command.getX(),
					command.getY(),
					command.getX() + command.getWidth(),
					command.getY() + command.getHeight()
				};
				if (overlaps(faceBounds, spriteBounds)) {
					overlappingSpriteCommands++;
				}
			}
			return overlappingSpriteCommands;
		}

		private static boolean isFrontOccluderCandidateKind(Renderer3DModelKind kind) {
			return kind == Renderer3DModelKind.WALL
				|| kind == Renderer3DModelKind.GAME_OBJECT
				|| kind == Renderer3DModelKind.WALL_OBJECT;
		}

		private static int[] boundsFor(int[] xs, int[] ys) {
			if (xs == null || ys == null || xs.length == 0 || xs.length != ys.length) {
				return null;
			}
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			for (int i = 0; i < xs.length; i++) {
				minX = Math.min(minX, xs[i]);
				minY = Math.min(minY, ys[i]);
				maxX = Math.max(maxX, xs[i]);
				maxY = Math.max(maxY, ys[i]);
			}
			return new int[] { minX, minY, maxX, maxY };
		}

		private static boolean overlaps(int[] left, int[] right) {
			return left[0] < right[2]
				&& left[2] > right[0]
				&& left[1] < right[3]
				&& left[3] > right[1];
		}

		private void writeSpriteSubmissions(Frame frame) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "sprite-submissions.tsv"));
			try {
				writer.println(
					"index\tfaceId\tspriteId\tlegacyEntity\tpickIndex\tprojected\tcullReason"
						+ "\tworldX\tworldY\tworldZ\tsourceWidth\tsourceHeight"
						+ "\tcameraX\tcameraY\tcameraZ\tscreenX\tscreenY"
						+ "\tdrawX\tdrawY\tdrawWidth\tdrawHeight\tscale\thorizontalSkew");
				if (frame.renderer3DFrame == null) {
					return;
				}
				List<Renderer3DFrame.SpriteSubmission> submissions =
					frame.renderer3DFrame.getSpriteSubmissions();
				for (int i = 0; i < submissions.size(); i++) {
					Renderer3DFrame.SpriteSubmission submission = submissions.get(i);
					writer.println(i
						+ "\t" + submission.getFaceId()
						+ "\t" + submission.getSpriteId()
						+ "\t" + isLegacyEntitySpriteId(submission.getSpriteId())
						+ "\t" + submission.getPickIndex()
						+ "\t" + submission.isProjected()
						+ "\t" + submission.getCullReason()
						+ "\t" + submission.getWorldX()
						+ "\t" + submission.getWorldY()
						+ "\t" + submission.getWorldZ()
						+ "\t" + submission.getSourceWidth()
						+ "\t" + submission.getSourceHeight()
						+ "\t" + submission.getCameraX()
						+ "\t" + submission.getCameraY()
						+ "\t" + submission.getCameraZ()
						+ "\t" + submission.getScreenX()
						+ "\t" + submission.getScreenY()
						+ "\t" + submission.getDrawX()
						+ "\t" + submission.getDrawY()
						+ "\t" + submission.getDrawWidth()
						+ "\t" + submission.getDrawHeight()
						+ "\t" + submission.getScale()
						+ "\t" + submission.getHorizontalSkew());
				}
			} finally {
				writer.close();
			}
		}

		private void writeCharacterSprites(
			Frame frame,
			Map<Integer, LegacyEntitySpriteDebugStats> restoreStatsById) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "character-sprites.tsv"));
			try {
				writer.println(
					"index\tkind\tfaceId\tspriteId\tarrayIndex\tserverIndex\tentityId\tdisplayName"
						+ "\tprojected\tcullReason\tdirection\tcombatDirection\tcombatTimeout"
						+ "\thealthCurrent\thealthMax\tdamageTaken\tactiveHitSplats"
						+ "\tattackingNpcServerIndex\tattackingPlayerServerIndex"
						+ "\tcombatEffectType\tcombatEffectTime"
						+ "\tworldX\tworldY\tworldZ\tvisualOffsetX\tvisualOffsetZ"
						+ "\tsourceWidth\tsourceHeight\tdrawX\tdrawY\tdrawWidth\tdrawHeight"
						+ "\tbodyCommands\tbodySceneCommands\tbodyWorldCommands\tbodyUiCommands"
						+ "\tbodyUnknownCommands\tbodyOrderedCommands\tbodyAlphaZeroCommands"
						+ "\tbodyDestPixels\tbodySourceVisiblePixels\tbodyFirstSequence\tbodyLastSequence"
						+ "\tbodyBounds"
						+ "\trestoreCommands\trestoreVisiblePixels\trestoreDirectPixels"
						+ "\trestoreFallbacks\trestoreSkipped\trestoreAtlasFull"
						+ "\trestoreDepthEvaluations\trestoreDepthVisiblePixels"
						+ "\trestoreDepthMisses\trestoreDepthSourcePixels\trestoreDepthOccludedPixels"
						+ "\trestoreDepthClippedPixels\trestoreDepthOutOfBoundsPixels\trestoreBounds"
						+ "\tdiagnosis");
				if (frame.renderer3DFrame == null) {
					return;
				}
				Map<Integer, SpriteCommandStats> bodyStatsById = buildSpriteCommandStats(frame);
				List<Renderer3DFrame.CharacterSprite> characters =
					frame.renderer3DFrame.getCharacterSprites();
				for (int i = 0; i < characters.size(); i++) {
					Renderer3DFrame.CharacterSprite character = characters.get(i);
					SpriteCommandStats bodyStats = bodyStatsById.get(character.getSpriteId());
					LegacyEntitySpriteDebugStats restoreStats =
						restoreStatsById == null ? null : restoreStatsById.get(character.getSpriteId());
					writer.println(i
						+ "\t" + safeTsv(character.getKind())
						+ "\t" + character.getFaceId()
						+ "\t" + character.getSpriteId()
						+ "\t" + character.getArrayIndex()
						+ "\t" + character.getServerIndex()
						+ "\t" + character.getEntityId()
						+ "\t" + safeTsv(character.getDisplayName())
						+ "\t" + character.isProjected()
						+ "\t" + safeTsv(character.getCullReason())
						+ "\t" + safeTsv(character.getDirection())
						+ "\t" + character.isCombatDirection()
						+ "\t" + character.getCombatTimeout()
						+ "\t" + character.getHealthCurrent()
						+ "\t" + character.getHealthMax()
						+ "\t" + character.getDamageTaken()
						+ "\t" + character.hasActiveHitSplats()
						+ "\t" + character.getAttackingNpcServerIndex()
						+ "\t" + character.getAttackingPlayerServerIndex()
						+ "\t" + character.getCombatEffectType()
						+ "\t" + character.getCombatEffectTime()
						+ "\t" + character.getWorldX()
						+ "\t" + character.getWorldY()
						+ "\t" + character.getWorldZ()
						+ "\t" + character.getVisualOffsetX()
						+ "\t" + character.getVisualOffsetZ()
						+ "\t" + character.getSourceWidth()
						+ "\t" + character.getSourceHeight()
						+ "\t" + character.getDrawX()
						+ "\t" + character.getDrawY()
						+ "\t" + character.getDrawWidth()
						+ "\t" + character.getDrawHeight()
						+ "\t" + intStat(bodyStats, "commands")
						+ "\t" + intStat(bodyStats, "sceneCommands")
						+ "\t" + intStat(bodyStats, "worldCommands")
						+ "\t" + intStat(bodyStats, "uiCommands")
						+ "\t" + intStat(bodyStats, "unknownCommands")
						+ "\t" + intStat(bodyStats, "orderedCommands")
						+ "\t" + intStat(bodyStats, "alphaZeroCommands")
						+ "\t" + intStat(bodyStats, "destPixels")
						+ "\t" + intStat(bodyStats, "sourceVisiblePixels")
						+ "\t" + firstSequence(bodyStats)
						+ "\t" + lastSequence(bodyStats)
						+ "\t" + safeTsv(spriteCommandBounds(bodyStats))
						+ "\t" + restoreIntStat(restoreStats, "commands")
						+ "\t" + restoreIntStat(restoreStats, "visiblePixels")
						+ "\t" + restoreIntStat(restoreStats, "directPixels")
						+ "\t" + restoreIntStat(restoreStats, "fallbacks")
						+ "\t" + restoreIntStat(restoreStats, "skipped")
						+ "\t" + restoreIntStat(restoreStats, "atlasFull")
						+ "\t" + restoreIntStat(restoreStats, "depthEvaluations")
						+ "\t" + restoreIntStat(restoreStats, "depthVisiblePixels")
						+ "\t" + restoreIntStat(restoreStats, "depthMisses")
						+ "\t" + restoreIntStat(restoreStats, "depthSourcePixels")
						+ "\t" + restoreIntStat(restoreStats, "depthOccludedPixels")
						+ "\t" + restoreIntStat(restoreStats, "depthClippedPixels")
						+ "\t" + restoreIntStat(restoreStats, "depthOutOfBoundsPixels")
						+ "\t" + safeTsv(restoreBounds(restoreStats))
						+ "\t" + safeTsv(characterDiagnosis(character, bodyStats, restoreStats)));
				}
			} finally {
				writer.close();
			}
		}

		private static Map<Integer, SpriteCommandStats> buildSpriteCommandStats(Frame frame) {
			Map<Integer, SpriteCommandStats> statsById = new HashMap<Integer, SpriteCommandStats>();
			if (frame.renderer2DFrame == null) {
				return statsById;
			}
			Renderer2DFrame.SpriteCommand[] commands = frame.renderer2DFrame.getSpriteCommands();
			for (Renderer2DFrame.SpriteCommand command : commands) {
				int legacySpriteId = command.getLegacySpriteId();
				if (!isLegacyEntitySpriteId(legacySpriteId)) {
					continue;
				}
				SpriteCommandStats stats = statsById.get(legacySpriteId);
				if (stats == null) {
					stats = new SpriteCommandStats(legacySpriteId);
					statsById.put(legacySpriteId, stats);
				}
				stats.record(command);
			}
			return statsById;
		}

		private static String characterDiagnosis(
			Renderer3DFrame.CharacterSprite character,
			SpriteCommandStats bodyStats,
			LegacyEntitySpriteDebugStats restoreStats) {
			if (!character.isProjected()) {
				return "not-projected:" + character.getCullReason();
			}
			if (bodyStats == null || bodyStats.commands == 0) {
				return "projected-no-body-command";
			}
			if (bodyStats.sourceVisiblePixels == 0) {
				return "body-command-has-no-visible-source-pixels";
			}
			if (restoreStats == null) {
				return "body-command-before-restore-stats";
			}
			if (restoreStats.depthEvaluations > 0 && restoreStats.depthVisiblePixels > 0) {
				return "restore-depth-visible";
			}
			if (restoreStats.depthMisses > 0 && restoreStats.depthOccludedPixels >= restoreStats.depthSourcePixels) {
				return "restore-depth-fully-occluded";
			}
			if (restoreStats.depthEvaluations > 0
				&& restoreStats.depthSourcePixels > 0
				&& restoreStats.depthVisiblePixels == 0
				&& restoreStats.depthOutOfBoundsPixels > 0
				&& restoreStats.depthOutOfBoundsPixels
					+ restoreStats.depthClippedPixels
					+ restoreStats.depthOccludedPixels >= restoreStats.depthSourcePixels) {
				return "restore-depth-out-of-bounds";
			}
			if (restoreStats.depthEvaluations > 0
				&& restoreStats.depthSourcePixels > 0
				&& restoreStats.depthVisiblePixels == 0
				&& restoreStats.depthClippedPixels > 0
				&& restoreStats.depthOutOfBoundsPixels
					+ restoreStats.depthClippedPixels
					+ restoreStats.depthOccludedPixels >= restoreStats.depthSourcePixels) {
				return "restore-depth-clipped";
			}
			if (restoreStats.commands == 0) {
				return "body-command-not-restored";
			}
			if (restoreStats.visiblePixels > 0) {
				return "restore-visible";
			}
			if (restoreStats.atlasFull > 0) {
				return "restore-atlas-full";
			}
			if (restoreStats.skipped > 0) {
				return "restore-zero-visible-pixels";
			}
			if (restoreStats.depthMisses > 0) {
				return "restore-depth-miss";
			}
			return "restore-no-visible-output";
		}

		private static int intStat(SpriteCommandStats stats, String field) {
			if (stats == null) {
				return 0;
			}
			if ("commands".equals(field)) {
				return stats.commands;
			}
			if ("sceneCommands".equals(field)) {
				return stats.sceneCommands;
			}
			if ("worldCommands".equals(field)) {
				return stats.worldCommands;
			}
			if ("uiCommands".equals(field)) {
				return stats.uiCommands;
			}
			if ("unknownCommands".equals(field)) {
				return stats.unknownCommands;
			}
			if ("orderedCommands".equals(field)) {
				return stats.orderedCommands;
			}
			if ("alphaZeroCommands".equals(field)) {
				return stats.alphaZeroCommands;
			}
			if ("destPixels".equals(field)) {
				return stats.destPixels;
			}
			if ("sourceVisiblePixels".equals(field)) {
				return stats.sourceVisiblePixels;
			}
			return 0;
		}

		private static int restoreIntStat(LegacyEntitySpriteDebugStats stats, String field) {
			if (stats == null) {
				return 0;
			}
			if ("commands".equals(field)) {
				return stats.commands;
			}
			if ("visiblePixels".equals(field)) {
				return stats.visiblePixels;
			}
			if ("directPixels".equals(field)) {
				return stats.directPixels;
			}
			if ("fallbacks".equals(field)) {
				return stats.fallbacks;
			}
			if ("skipped".equals(field)) {
				return stats.skipped;
			}
			if ("atlasFull".equals(field)) {
				return stats.atlasFull;
			}
			if ("depthEvaluations".equals(field)) {
				return stats.depthEvaluations;
			}
			if ("depthVisiblePixels".equals(field)) {
				return stats.depthVisiblePixels;
			}
			if ("depthMisses".equals(field)) {
				return stats.depthMisses;
			}
			if ("depthSourcePixels".equals(field)) {
				return stats.depthSourcePixels;
			}
			if ("depthOccludedPixels".equals(field)) {
				return stats.depthOccludedPixels;
			}
			if ("depthClippedPixels".equals(field)) {
				return stats.depthClippedPixels;
			}
			if ("depthOutOfBoundsPixels".equals(field)) {
				return stats.depthOutOfBoundsPixels;
			}
			return 0;
		}

		private static String firstSequence(SpriteCommandStats stats) {
			if (stats == null || stats.firstSequence == Integer.MAX_VALUE) {
				return "";
			}
			return String.valueOf(stats.firstSequence);
		}

		private static String lastSequence(SpriteCommandStats stats) {
			if (stats == null || stats.lastSequence == Integer.MIN_VALUE) {
				return "";
			}
			return String.valueOf(stats.lastSequence);
		}

		private static String spriteCommandBounds(SpriteCommandStats stats) {
			if (stats == null) {
				return "";
			}
			return bounds(stats.minX, stats.minY, stats.maxX, stats.maxY);
		}

		private static String restoreBounds(LegacyEntitySpriteDebugStats stats) {
			if (stats == null) {
				return "";
			}
			return bounds(stats.minX, stats.minY, stats.maxX, stats.maxY);
		}

		private static String joinInts(int[] values) {
			if (values == null || values.length == 0) {
				return "";
			}
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					builder.append(',');
				}
				builder.append(values[i]);
			}
			return builder.toString();
		}

		private static String joinFloats(float[] values) {
			if (values == null || values.length == 0) {
				return "";
			}
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i > 0) {
					builder.append(',');
				}
				builder.append(Float.toString(values[i]));
			}
			return builder.toString();
		}

		private static final class SpriteCommandStats {
			private final int legacySpriteId;
			private int commands;
			private int sceneCommands;
			private int worldCommands;
			private int uiCommands;
			private int unknownCommands;
			private int orderedCommands;
			private int alphaZeroCommands;
			private int destPixels;
			private int sourceVisiblePixels;
			private int firstSequence = Integer.MAX_VALUE;
			private int lastSequence = Integer.MIN_VALUE;
			private int minX = Integer.MAX_VALUE;
			private int minY = Integer.MAX_VALUE;
			private int maxX = Integer.MIN_VALUE;
			private int maxY = Integer.MIN_VALUE;

			private SpriteCommandStats(int legacySpriteId) {
				this.legacySpriteId = legacySpriteId;
			}

			private void record(Renderer2DFrame.SpriteCommand command) {
				commands++;
				switch (command.getPhase()) {
					case SCENE:
						sceneCommands++;
						break;
					case WORLD_OVERLAY:
						worldCommands++;
						break;
					case UI_OVERLAY:
						uiCommands++;
						break;
					default:
						unknownCommands++;
						break;
				}
				if (command.requiresOrderedReplay()) {
					orderedCommands++;
				}
				if (command.getAlpha() <= 0) {
					alphaZeroCommands++;
				}
				destPixels += command.getWidth() * command.getHeight();
				sourceVisiblePixels += countVisibleOutputPixels(command);
				firstSequence = Math.min(firstSequence, command.getSequence());
				lastSequence = Math.max(lastSequence, command.getSequence());
				minX = Math.min(minX, command.getX());
				minY = Math.min(minY, command.getY());
				maxX = Math.max(maxX, command.getX() + command.getWidth());
				maxY = Math.max(maxY, command.getY() + command.getHeight());
			}
		}

		private static int countVisibleOutputPixels(Renderer2DFrame.SpriteCommand command) {
			if (command.getAlpha() <= 0) {
				return 0;
			}
			Sprite sprite = command.getSprite();
			int[] sourcePixels = sprite.getPixels();
			int spriteWidth = sprite.getWidth();
			int spriteHeight = sprite.getHeight();
			if (sourcePixels == null || sourcePixels.length < spriteWidth * spriteHeight) {
				return 0;
			}
			int visiblePixels = 0;
			for (int row = 0; row < command.getHeight(); row++) {
				int sourceY =
					(int) ((command.getSourceStartY16() + (long) row * command.getSourceScaleY16()) >> 16);
				if (sourceY < 0 || sourceY >= spriteHeight) {
					continue;
				}
				for (int column = 0; column < command.getWidth(); column++) {
					int sourceX =
						(int) ((command.getSourceStartX16() + (long) column * command.getSourceScaleX16()) >> 16);
					if (sourceX < 0 || sourceX >= spriteWidth) {
						continue;
					}
					if (orsc.graphics.RendererTransparency.isVisibleSpritePixel(
						sourcePixels[sourceY * spriteWidth + sourceX])) {
						visiblePixels++;
					}
				}
			}
			return visiblePixels;
		}

		private void writeSpriteAnchors(Frame frame) throws Exception {
			PrintWriter writer = new PrintWriter(new File(directory, "sprite-anchors.tsv"));
			try {
				writer.println(
					"index\tfaceId\tspriteId\tlegacyEntity\tpickIndex\tlegacyDrawOrder"
						+ "\taverageDepth\tcameraX\tcameraY\tcameraZ\tscreenX\tscreenY"
						+ "\tdrawX\tdrawY\tdrawWidth\tdrawHeight\tscale\thorizontalSkew\tpickable");
				if (frame.renderer3DFrame == null) {
					return;
				}
				List<Renderer3DFrame.SpriteAnchor> anchors = frame.renderer3DFrame.getSpriteAnchors();
				for (int i = 0; i < anchors.size(); i++) {
					Renderer3DFrame.SpriteAnchor anchor = anchors.get(i);
					writer.println(i
						+ "\t" + anchor.getFaceId()
						+ "\t" + anchor.getSpriteId()
						+ "\t" + isLegacyEntitySpriteId(anchor.getSpriteId())
						+ "\t" + anchor.getPickIndex()
						+ "\t" + anchor.getLegacyDrawOrder()
						+ "\t" + anchor.getAverageDepth()
						+ "\t" + anchor.getCameraX()
						+ "\t" + anchor.getCameraY()
						+ "\t" + anchor.getCameraZ()
						+ "\t" + anchor.getScreenX()
						+ "\t" + anchor.getScreenY()
						+ "\t" + anchor.getDrawX()
						+ "\t" + anchor.getDrawY()
						+ "\t" + anchor.getDrawWidth()
						+ "\t" + anchor.getDrawHeight()
						+ "\t" + anchor.getScale()
						+ "\t" + anchor.getHorizontalSkew()
						+ "\t" + anchor.isPickable());
				}
			} finally {
				writer.close();
			}
		}

		private static File frameCaptureBaseDirectory() {
			String path = System.getProperty(FRAME_CAPTURE_DIR_PROPERTY);
			if (path == null || path.trim().isEmpty()) {
				path = System.getenv(FRAME_CAPTURE_DIR_ENV);
			}
			if (path == null || path.trim().isEmpty()) {
				path = "renderer-v2-captures";
			}
			File directory = new File(path.trim());
			if (!directory.isAbsolute()) {
				directory = new File(System.getProperty("user.dir"), path.trim());
			}
			return directory;
		}

		private static BufferedImage imageFromFrame(Frame frame) {
			BufferedImage image =
				new BufferedImage(frame.sourceWidth, frame.sourceHeight, BufferedImage.TYPE_INT_ARGB);
			ByteBuffer pixels = frame.pixels();
			for (int y = 0; y < frame.sourceHeight; y++) {
				for (int x = 0; x < frame.sourceWidth; x++) {
					int offset = (y * frame.sourceWidth + x) * 4;
					int red = pixels.get(offset) & 0xFF;
					int green = pixels.get(offset + 1) & 0xFF;
					int blue = pixels.get(offset + 2) & 0xFF;
					int alpha = pixels.get(offset + 3) & 0xFF;
					image.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
				}
			}
			return image;
		}

		private static BufferedImage opaqueRgbImage(int width, int height, int[] rgbPixels) {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setRGB(x, y, 0xFF000000 | (rgbPixels[y * width + x] & 0xFFFFFF));
				}
			}
			return image;
		}

		private static BufferedImage argbImage(int width, int height, int[] argbPixels) {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					image.setRGB(x, y, argbPixels[y * width + x]);
				}
			}
			return image;
		}

		private void writeImage(String fileName, BufferedImage image) throws Exception {
			if (!ImageIO.write(image, "png", new File(directory, fileName))) {
				throw new IllegalStateException("ImageIO did not find a PNG writer");
			}
		}

		private static final class StaticRangeCandidateStats {
			private int staticFaces;
			private int terrainFaces;
			private int wallFaces;
			private int roofFaces;
			private int gameObjectFaces;
			private int wallObjectFaces;
			private int overlapFaces;
			private int overlapTerrainFaces;
			private int overlapWallFaces;
			private int overlapRoofFaces;
			private int overlapGameObjectFaces;
			private int overlapWallObjectFaces;
			private int overlapWorldSpriteCommands;

			private void recordFace(Renderer3DModelKind kind, boolean overlap) {
				staticFaces++;
				if (overlap) {
					overlapFaces++;
				}
				if (kind == Renderer3DModelKind.TERRAIN) {
					terrainFaces++;
					if (overlap) {
						overlapTerrainFaces++;
					}
				} else if (kind == Renderer3DModelKind.WALL) {
					wallFaces++;
					if (overlap) {
						overlapWallFaces++;
					}
				} else if (kind == Renderer3DModelKind.ROOF) {
					roofFaces++;
					if (overlap) {
						overlapRoofFaces++;
					}
				} else if (kind == Renderer3DModelKind.GAME_OBJECT) {
					gameObjectFaces++;
					if (overlap) {
						overlapGameObjectFaces++;
					}
				} else if (kind == Renderer3DModelKind.WALL_OBJECT) {
					wallObjectFaces++;
					if (overlap) {
						overlapWallObjectFaces++;
					}
				}
			}
		}

		private static boolean isLegacyEntitySpriteId(int legacySpriteId) {
			return (legacySpriteId >= 5000 && legacySpriteId < 20000)
				|| (legacySpriteId >= 20000 && legacySpriteId < 40000);
		}

		private static String worldSpriteKind(Renderer2DFrame.SpriteCommand command) {
			if (command == null) {
				return "unknown";
			}
			int legacySpriteId = command.getLegacySpriteId();
			if (isLegacyEntitySpriteId(legacySpriteId)) {
				return "entity";
			}
			if (legacySpriteId >= 40000 && legacySpriteId < 50000) {
				return "ground-item";
			}
			return "unknown";
		}

		private static String emptyIfUnset(int value, int unsetValue) {
			return value == unsetValue ? "" : String.valueOf(value);
		}

		private static String bounds(int minX, int minY, int maxX, int maxY) {
			if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE) {
				return "";
			}
			return minX + "," + minY + "-" + maxX + "," + maxY;
		}

		private static String bounds(int[] bounds) {
			if (bounds == null || bounds.length < 4) {
				return "";
			}
			return bounds(bounds[0], bounds[1], bounds[2], bounds[3]);
		}

		private static String percent(int numerator, int denominator) {
			if (denominator <= 0) {
				return "";
			}
			return String.format(Locale.US, "%.1f", numerator * 100.0 / denominator);
		}

		private static String safeTsv(String value) {
			if (value == null) {
				return "";
			}
			return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
		}
	}

	private static final class Frame {
		private final int sourceWidth;
		private final int sourceHeight;
		private final int targetWidth;
		private final int targetHeight;
		private final boolean linearFiltering;
		private final int byteCount;
		private final Renderer2DFrame renderer2DFrame;
		private final Renderer3DFrame renderer3DFrame;
		private final String[] rendererDebugOverlayLines;
		private final FrameBufferPool frameBufferPool;
		private final FrameBuffer frameBuffer;
		private boolean released;

		private Frame(
			int sourceWidth,
			int sourceHeight,
			int targetWidth,
			int targetHeight,
			boolean linearFiltering,
			int byteCount,
			Renderer2DFrame renderer2DFrame,
			Renderer3DFrame renderer3DFrame,
			String[] rendererDebugOverlayLines,
			FrameBufferPool frameBufferPool,
			FrameBuffer frameBuffer) {
			this.sourceWidth = sourceWidth;
			this.sourceHeight = sourceHeight;
			this.targetWidth = targetWidth;
			this.targetHeight = targetHeight;
			this.linearFiltering = linearFiltering;
			this.byteCount = byteCount;
			this.renderer2DFrame = renderer2DFrame;
			this.renderer3DFrame = renderer3DFrame;
			this.rendererDebugOverlayLines =
				rendererDebugOverlayLines == null ? null : rendererDebugOverlayLines.clone();
			this.frameBufferPool = frameBufferPool;
			this.frameBuffer = frameBuffer;
		}

		static Frame fromImage(
			BufferedImage image,
			float scalar,
			ScaledWindow.ScalingAlgorithm scalingAlgorithm,
			FrameBufferPool frameBufferPool,
			Renderer2DFrame renderer2DFrame,
			Renderer3DFrame renderer3DFrame,
			String[] rendererDebugOverlayLines) {
			int sourceWidth = image.getWidth();
			int sourceHeight = image.getHeight();
			int targetWidth = Math.max(1, Math.round(sourceWidth * scalar));
			int targetHeight = Math.max(1, Math.round(sourceHeight * scalar));
			boolean linearFiltering = scalingAlgorithm != ScaledWindow.ScalingAlgorithm.INTEGER_SCALING;
			int byteCount = sourceWidth * sourceHeight * 4;
			FrameBuffer frameBuffer = frameBufferPool.acquire(byteCount);

			try {
				frameBuffer.buffer.clear();
				frameBuffer.buffer.limit(byteCount);
				copyPixelsToRgba(image, sourceWidth, sourceHeight, frameBuffer.buffer);
				frameBuffer.buffer.flip();
				return new Frame(
					sourceWidth,
					sourceHeight,
					targetWidth,
					targetHeight,
					linearFiltering,
					byteCount,
					renderer2DFrame,
					renderer3DFrame,
					rendererDebugOverlayLines,
					frameBufferPool,
					frameBuffer);
			} catch (RuntimeException e) {
				frameBufferPool.release(frameBuffer);
				throw e;
			}
		}

		private ByteBuffer pixels() {
			ByteBuffer pixels = frameBuffer.buffer.duplicate();
			pixels.position(0);
			pixels.limit(byteCount);
			return pixels;
		}

		private void release() {
			if (released) {
				return;
			}
			released = true;
			frameBufferPool.release(frameBuffer);
		}

		private static void copyPixelsToRgba(BufferedImage image, int width, int height, ByteBuffer target) {
			if (copyPackedIntRgbToRgba(image, width, height, target)) {
				return;
			}

			int[] rowPixels = new int[width];
			for (int y = 0; y < height; y++) {
				image.getRGB(0, y, width, 1, rowPixels, 0, width);
				for (int x = 0; x < width; x++) {
					putRgbAsRgba(target, rowPixels[x]);
				}
			}
		}

		private static boolean copyPackedIntRgbToRgba(
			BufferedImage image,
			int width,
			int height,
			ByteBuffer target) {
			Raster raster = image.getRaster();
			DataBuffer dataBuffer = raster.getDataBuffer();
			SampleModel sampleModel = raster.getSampleModel();
			if (!(dataBuffer instanceof DataBufferInt)
				|| ((DataBufferInt) dataBuffer).getNumBanks() != 1
				|| !(sampleModel instanceof SinglePixelPackedSampleModel)) {
				return false;
			}

			SinglePixelPackedSampleModel packedSampleModel = (SinglePixelPackedSampleModel) sampleModel;
			if (!hasRgbMasks(packedSampleModel.getBitMasks())) {
				return false;
			}

			int stride = packedSampleModel.getScanlineStride();
			if (stride < width) {
				return false;
			}

			DataBufferInt intBuffer = (DataBufferInt) dataBuffer;
			int[] source = intBuffer.getData();
			int sourceOffset = intBuffer.getOffset()
				+ packedSampleModel.getOffset(
					raster.getMinX() - raster.getSampleModelTranslateX(),
					raster.getMinY() - raster.getSampleModelTranslateY());
			int lastSourceIndex = sourceOffset + (height - 1) * stride + width - 1;
			if (sourceOffset < 0 || lastSourceIndex >= source.length) {
				return false;
			}

			for (int y = 0; y < height; y++) {
				int rowOffset = sourceOffset + y * stride;
				for (int x = 0; x < width; x++) {
					putRgbAsRgba(target, source[rowOffset + x]);
				}
			}
			return true;
		}

		private static boolean hasRgbMasks(int[] masks) {
			return masks.length >= 3
				&& masks[0] == 0x00FF0000
				&& masks[1] == 0x0000FF00
				&& masks[2] == 0x000000FF;
		}

		private static void putRgbAsRgba(ByteBuffer target, int pixel) {
			target.putInt(((pixel & 0x00FFFFFF) << 8) | 0xFF);
		}
	}

	private static final class FrameBuffer {
		private final ByteBuffer buffer;

		private FrameBuffer(int byteCount) {
			buffer = ByteBuffer.allocateDirect(byteCount);
		}

		private int capacity() {
			return buffer.capacity();
		}
	}

	private static final class FrameBufferPool {
		private static final int MAX_RETAINED_BUFFERS = 3;

		private final ArrayDeque<FrameBuffer> available = new ArrayDeque<>();

		private synchronized FrameBuffer acquire(int requiredBytes) {
			FrameBuffer selected = null;
			for (FrameBuffer frameBuffer : available) {
				if (frameBuffer.capacity() >= requiredBytes) {
					selected = frameBuffer;
					break;
				}
			}
			if (selected != null) {
				available.remove(selected);
				return selected;
			}
			return new FrameBuffer(requiredBytes);
		}

		private synchronized void release(FrameBuffer frameBuffer) {
			if (frameBuffer == null) {
				return;
			}
			frameBuffer.buffer.clear();
			if (available.size() < MAX_RETAINED_BUFFERS) {
				available.addFirst(frameBuffer);
			}
		}
	}

	static final class OpenGLTextureRegion {
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

		private OpenGLTextureRegion(
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

	private static final class OpenGLSpriteTextureCache implements AutoCloseable {
		private static final int DEFAULT_ATLAS_SIZE = 2048;

		private final LwjglBindings gl;
		private final Map<SpriteTextureKey, OpenGLTextureRegion> spriteRegions = new HashMap<>();
		private final List<OpenGLTextureAtlas> atlases = new ArrayList<>();

		private OpenGLSpriteTextureCache(LwjglBindings gl) {
			this.gl = gl;
		}

		private OpenGLTextureRegion getOrUpload(Sprite sprite) throws Exception {
			return getOrUpload(sprite, RendererSpriteTransform.IDENTITY);
		}

		private OpenGLTextureRegion getOrUpload(Sprite sprite, RendererSpriteTransform transform) throws Exception {
			return getOrUpload(sprite, transform, true);
		}

		private OpenGLTextureRegion getOrUpload(
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
			log("OpenGL sprite atlas allocated: "
				+ DEFAULT_ATLAS_SIZE
				+ "x"
				+ DEFAULT_ATLAS_SIZE
				+ " texture="
				+ atlas.textureId);
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

	private static final class OpenGLGlyphTextureCache implements AutoCloseable {
		private static final int DEFAULT_ATLAS_SIZE = 512;

		private final LwjglBindings gl;
		private final Map<GlyphTextureKey, OpenGLTextureRegion> glyphRegions = new HashMap<>();
		private final List<OpenGLDynamicTextureAtlas> atlases = new ArrayList<>();

		private OpenGLGlyphTextureCache(LwjglBindings gl) {
			this.gl = gl;
		}

		private OpenGLTextureRegion getOrUpload(Renderer2DFrame.TextCommand.GlyphCommand glyph) throws Exception {
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
			log("OpenGL glyph atlas allocated: "
				+ DEFAULT_ATLAS_SIZE
				+ "x"
				+ DEFAULT_ATLAS_SIZE
				+ " texture="
				+ atlas.textureId);
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

	private static final class DynamicTextureData {
		private final int width;
		private final int height;
		private final ByteBuffer pixels;
		private final int visiblePixelCount;

		private DynamicTextureData(int width, int height, ByteBuffer pixels, int visiblePixelCount) {
			this.width = width;
			this.height = height;
			this.pixels = pixels;
			this.visiblePixelCount = visiblePixelCount;
		}

		private ByteBuffer pixels() {
			ByteBuffer duplicate = pixels.duplicate();
			duplicate.position(0);
			duplicate.limit(width * height * 4);
			return duplicate;
		}
	}

	private static final class LegacyEntitySpriteDebugStats {
		private final int legacySpriteId;
		private int commands;
		private int visiblePixels;
		private int directPixels;
		private int fallbacks;
		private int skipped;
		private int atlasFull;
		private int depthEvaluations;
		private int depthVisiblePixels;
		private int depthMisses;
		private int depthSourcePixels;
		private int depthOccludedPixels;
		private int depthClippedPixels;
		private int depthOutOfBoundsPixels;
		private int minAnchorDepth = Integer.MAX_VALUE;
		private int maxAnchorDepth = Integer.MIN_VALUE;
		private int minX = Integer.MAX_VALUE;
		private int minY = Integer.MAX_VALUE;
		private int maxX = Integer.MIN_VALUE;
		private int maxY = Integer.MIN_VALUE;

		private LegacyEntitySpriteDebugStats(int legacySpriteId) {
			this.legacySpriteId = legacySpriteId;
		}

		private void record(
			Renderer2DFrame.SpriteCommand command,
			int visiblePixels,
			int directPixels,
			boolean fallback,
			boolean atlasFull,
			boolean skipped) {
			commands++;
			this.visiblePixels += Math.max(0, visiblePixels);
			this.directPixels += Math.max(0, directPixels);
			if (fallback) {
				fallbacks++;
			}
			if (atlasFull) {
				this.atlasFull++;
			}
			if (skipped) {
				this.skipped++;
			}
			minX = Math.min(minX, command.getX());
			minY = Math.min(minY, command.getY());
			maxX = Math.max(maxX, command.getX() + command.getWidth());
			maxY = Math.max(maxY, command.getY() + command.getHeight());
		}

		private void recordDepthEvaluation(
			int sourcePixels,
			int visiblePixels,
			int occludedPixels,
			int clippedPixels,
			int outOfBoundsPixels,
			Renderer3DFrame.SpriteAnchor anchor) {
			depthEvaluations++;
			depthVisiblePixels += Math.max(0, visiblePixels);
			depthSourcePixels += Math.max(0, sourcePixels);
			depthOccludedPixels += Math.max(0, occludedPixels);
			depthClippedPixels += Math.max(0, clippedPixels);
			depthOutOfBoundsPixels += Math.max(0, outOfBoundsPixels);
			if (anchor != null) {
				minAnchorDepth = Math.min(minAnchorDepth, Math.min(anchor.getCameraZ(), anchor.getAverageDepth()));
				maxAnchorDepth = Math.max(maxAnchorDepth, Math.max(anchor.getCameraZ(), anchor.getAverageDepth()));
			}
		}

		private void recordDepthFallbackMiss(
			int sourcePixels,
			int occludedPixels,
			int clippedPixels,
			int outOfBoundsPixels,
			Renderer3DFrame.SpriteAnchor anchor) {
			depthMisses++;
			if (anchor != null) {
				minAnchorDepth = Math.min(minAnchorDepth, Math.min(anchor.getCameraZ(), anchor.getAverageDepth()));
				maxAnchorDepth = Math.max(maxAnchorDepth, Math.max(anchor.getCameraZ(), anchor.getAverageDepth()));
			}
		}

		private String describe() {
			StringBuilder builder = new StringBuilder();
			builder.append(legacySpriteId)
				.append(":c")
				.append(commands)
				.append("/v")
				.append(visiblePixels)
				.append("/d")
				.append(directPixels)
				.append("/s")
				.append(skipped)
				.append("/a")
				.append(atlasFull)
				.append("/f")
				.append(fallbacks)
				.append("@")
				.append(minX)
				.append(",")
				.append(minY)
				.append("-")
				.append(maxX)
				.append(",")
				.append(maxY);
			if (depthEvaluations > 0) {
				builder.append("/de")
					.append(depthEvaluations)
					.append(":vis")
					.append(depthVisiblePixels)
					.append("/src")
					.append(depthSourcePixels)
					.append("/occ")
					.append(depthOccludedPixels)
					.append("/clip")
					.append(depthClippedPixels)
					.append("/oob")
					.append(depthOutOfBoundsPixels);
			}
			if (depthMisses > 0) {
				builder.append("/dm")
					.append(depthMisses)
					.append(":full");
				if (minAnchorDepth != Integer.MAX_VALUE) {
					builder.append("/z")
						.append(minAnchorDepth)
						.append("-")
						.append(maxAnchorDepth);
				}
			}
			return builder.toString();
		}
	}

	private static final class LegacyEntitySpriteDepthEvaluation {
		private final int index;
		private final int sequence;
		private final String phase;
		private final int legacySpriteId;
		private final int x;
		private final int y;
		private final int width;
		private final int height;
		private final int topX16;
		private final int bottomX16;
		private final int anchorFaceId;
		private final int anchorLegacyDrawOrder;
		private final int anchorAverageDepth;
		private final int anchorCameraZ;
		private final String anchorMatchMode;
		private final int anchorMatchScore;
		private final int anchorDrawX;
		private final int anchorDrawY;
		private final int anchorDrawWidth;
		private final int anchorDrawHeight;
		private final int anchorDeltaX;
		private final int anchorDeltaY;
		private final int anchorDeltaWidth;
		private final int anchorDeltaHeight;
		private final int sourcePixels;
		private final int visiblePixels;
		private final int occludedPixels;
		private final int clippedPixels;
		private final int outOfBoundsPixels;
		private final int terrainOccludedPixels;
		private final int wallOccludedPixels;
		private final int roofOccludedPixels;
		private final int gameObjectOccludedPixels;
		private final int wallObjectOccludedPixels;
		private final int minOccluderLegacyDrawOrder;
		private final int maxOccluderLegacyDrawOrder;
		private final int minOccluderDepth;
		private final int maxOccluderDepth;
		private final String dominantOccluderKind;
		private final int dominantOccluderFaceId;
		private final int dominantOccluderModelIndex;
		private final int dominantOccluderPixels;
		private final int dominantOccluderLegacyDrawOrder;
		private final int dominantOccluderDepth;
		private final boolean fullyOccluded;

		private LegacyEntitySpriteDepthEvaluation(
			int index,
			Renderer2DFrame.SpriteCommand command,
			int sourcePixels,
			int visiblePixels,
			int occludedPixels,
			int clippedPixels,
			int outOfBoundsPixels,
			int terrainOccludedPixels,
			int wallOccludedPixels,
			int roofOccludedPixels,
			int gameObjectOccludedPixels,
			int wallObjectOccludedPixels,
			int minOccluderLegacyDrawOrder,
			int maxOccluderLegacyDrawOrder,
			int minOccluderDepth,
			int maxOccluderDepth,
			EntitySpriteOccluderFaceStats dominantOccluderFace,
			WorldSpriteAnchorMatch anchorMatch,
			Renderer3DFrame.SpriteAnchor anchor) {
			this.index = index;
			this.sequence = command.getSequence();
			this.phase = String.valueOf(command.getPhase());
			this.legacySpriteId = command.getLegacySpriteId();
			this.x = command.getX();
			this.y = command.getY();
			this.width = command.getWidth();
			this.height = command.getHeight();
			this.topX16 = command.getTopX16();
			this.bottomX16 = command.getBottomX16();
			this.anchorFaceId = anchor == null ? Integer.MIN_VALUE : anchor.getFaceId();
			this.anchorLegacyDrawOrder = anchor == null ? Integer.MIN_VALUE : anchor.getLegacyDrawOrder();
			this.anchorAverageDepth = anchor == null ? Integer.MIN_VALUE : anchor.getAverageDepth();
			this.anchorCameraZ = anchor == null ? Integer.MIN_VALUE : anchor.getCameraZ();
			this.anchorMatchMode = anchorMatch == null ? "unmatched" : anchorMatch.mode;
			this.anchorMatchScore = anchorMatch == null ? Integer.MAX_VALUE : anchorMatch.score;
			this.anchorDrawX = anchor == null ? Integer.MIN_VALUE : anchor.getDrawX();
			this.anchorDrawY = anchor == null ? Integer.MIN_VALUE : anchor.getDrawY();
			this.anchorDrawWidth = anchor == null ? Integer.MIN_VALUE : anchor.getDrawWidth();
			this.anchorDrawHeight = anchor == null ? Integer.MIN_VALUE : anchor.getDrawHeight();
			this.anchorDeltaX = anchor == null ? Integer.MIN_VALUE : this.x - anchor.getDrawX();
			this.anchorDeltaY = anchor == null ? Integer.MIN_VALUE : this.y - anchor.getDrawY();
			this.anchorDeltaWidth = anchor == null ? Integer.MIN_VALUE : this.width - anchor.getDrawWidth();
			this.anchorDeltaHeight = anchor == null ? Integer.MIN_VALUE : this.height - anchor.getDrawHeight();
			this.sourcePixels = Math.max(0, sourcePixels);
			this.visiblePixels = Math.max(0, visiblePixels);
			this.occludedPixels = Math.max(0, occludedPixels);
			this.clippedPixels = Math.max(0, clippedPixels);
			this.outOfBoundsPixels = Math.max(0, outOfBoundsPixels);
			this.terrainOccludedPixels = Math.max(0, terrainOccludedPixels);
			this.wallOccludedPixels = Math.max(0, wallOccludedPixels);
			this.roofOccludedPixels = Math.max(0, roofOccludedPixels);
			this.gameObjectOccludedPixels = Math.max(0, gameObjectOccludedPixels);
			this.wallObjectOccludedPixels = Math.max(0, wallObjectOccludedPixels);
			this.minOccluderLegacyDrawOrder = minOccluderLegacyDrawOrder;
			this.maxOccluderLegacyDrawOrder = maxOccluderLegacyDrawOrder;
			this.minOccluderDepth = minOccluderDepth;
			this.maxOccluderDepth = maxOccluderDepth;
			this.dominantOccluderKind = dominantOccluderFace == null
				? ""
				: String.valueOf(dominantOccluderFace.kind);
			this.dominantOccluderFaceId = dominantOccluderFace == null ? -1 : dominantOccluderFace.faceId;
			this.dominantOccluderModelIndex =
				dominantOccluderFace == null ? -1 : dominantOccluderFace.modelIndex;
			this.dominantOccluderPixels = dominantOccluderFace == null ? 0 : dominantOccluderFace.pixels;
			this.dominantOccluderLegacyDrawOrder =
				dominantOccluderFace == null ? Integer.MIN_VALUE : dominantOccluderFace.representativeLegacyDrawOrder;
			this.dominantOccluderDepth =
				dominantOccluderFace == null ? Integer.MIN_VALUE : dominantOccluderFace.representativeDepth;
			this.fullyOccluded = this.sourcePixels > 0 && this.visiblePixels == 0;
		}
	}

	private static final class EntitySpriteOccluderFaceStats {
		private final Renderer3DModelKind kind;
		private final int faceId;
		private final int modelIndex;
		private int pixels;
		private int representativeLegacyDrawOrder = Integer.MIN_VALUE;
		private int representativeDepth = Integer.MIN_VALUE;

		private EntitySpriteOccluderFaceStats(Renderer3DModelKind kind, int faceId, int modelIndex) {
			this.kind = kind;
			this.faceId = faceId;
			this.modelIndex = modelIndex;
		}

		private void record(int legacyDrawOrder, int depth) {
			pixels++;
			if (legacyDrawOrder >= 0) {
				representativeLegacyDrawOrder = legacyDrawOrder;
			}
			if (depth != Integer.MAX_VALUE) {
				representativeDepth = depth;
			}
		}
	}

	private static final class WorldSpriteAnchorMatch {
		private final String mode;
		private final int score;

		private WorldSpriteAnchorMatch(String mode, int score) {
			this.mode = mode;
			this.score = score;
		}

		private static WorldSpriteAnchorMatch unmatched() {
			return new WorldSpriteAnchorMatch("unmatched", Integer.MAX_VALUE);
		}
	}

	private static final class OpenGLDynamicTextureAtlas implements AutoCloseable {
		private static final int PADDING = 1;

		private final LwjglBindings gl;
		private final int textureId;
		private final int width;
		private final int height;
		private int cursorX;
		private int cursorY;
		private int rowHeight;
		private boolean closed;

		private OpenGLDynamicTextureAtlas(LwjglBindings gl, int textureId, int width, int height) {
			this.gl = gl;
			this.textureId = textureId;
			this.width = width;
			this.height = height;
		}

		private static OpenGLDynamicTextureAtlas create(LwjglBindings gl, int width, int height) throws Exception {
			int textureId = gl.glGenTextures();
			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP_TO_EDGE);
			ByteBuffer emptyPixels = ByteBuffer.allocateDirect(width * height * 4);
			gl.glTexImage2D(
				gl.GL_TEXTURE_2D,
				0,
				gl.GL_RGBA,
				width,
				height,
				0,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				emptyPixels);
			return new OpenGLDynamicTextureAtlas(gl, textureId, width, height);
		}

		private void beginFrame() {
			cursorX = 0;
			cursorY = 0;
			rowHeight = 0;
		}

		private OpenGLTextureRegion upload(DynamicTextureData textureData) throws Exception {
			Placement placement = allocate(textureData.width, textureData.height);
			if (placement == null) {
				return null;
			}

			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			gl.glTexSubImage2D(
				gl.GL_TEXTURE_2D,
				0,
				placement.x - PADDING,
				placement.y - PADDING,
				textureData.width + PADDING * 2,
				textureData.height + PADDING * 2,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				buildPaddedRgbaPixels(textureData.pixels(), textureData.width, textureData.height, PADDING));
			return new OpenGLTextureRegion(
				textureId,
				placement.x,
				placement.y,
				textureData.width,
				textureData.height,
				texelCenterU(placement.x),
				texelCenterV(placement.y),
				texelCenterU(placement.x + textureData.width - 1),
				texelCenterV(placement.y + textureData.height - 1),
				true);
		}

		private float texelCenterU(int x) {
			return (x + 0.5f) / width;
		}

		private float texelCenterV(int y) {
			return (y + 0.5f) / height;
		}

		private Placement allocate(int contentWidth, int contentHeight) {
			int allocationWidth = contentWidth + PADDING * 2;
			int allocationHeight = contentHeight + PADDING * 2;
			if (allocationWidth > width || allocationHeight > height) {
				return null;
			}

			if (cursorX + allocationWidth > width) {
				cursorX = 0;
				cursorY += rowHeight;
				rowHeight = 0;
			}
			if (cursorY + allocationHeight > height) {
				return null;
			}

			Placement placement = new Placement(cursorX + PADDING, cursorY + PADDING);
			cursorX += allocationWidth;
			rowHeight = Math.max(rowHeight, allocationHeight);
			return placement;
		}

		@Override
		public void close() throws Exception {
			if (closed) {
				return;
			}
			closed = true;
			gl.glDeleteTextures(textureId);
		}

		private static final class Placement {
			private final int x;
			private final int y;

			private Placement(int x, int y) {
				this.x = x;
				this.y = y;
			}
		}
	}

	private static final class OpenGLTextureAtlas implements AutoCloseable {
		private static final int PADDING = 1;

		private final LwjglBindings gl;
		private final int textureId;
		private final int width;
		private final int height;
		private int cursorX;
		private int cursorY;
		private int rowHeight;
		private boolean closed;

		private OpenGLTextureAtlas(LwjglBindings gl, int textureId, int width, int height) {
			this.gl = gl;
			this.textureId = textureId;
			this.width = width;
			this.height = height;
		}

		private static OpenGLTextureAtlas create(LwjglBindings gl, int width, int height) throws Exception {
			int textureId = gl.glGenTextures();
			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_S, gl.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_WRAP_T, gl.GL_CLAMP_TO_EDGE);
			ByteBuffer emptyPixels = ByteBuffer.allocateDirect(width * height * 4);
			gl.glTexImage2D(
				gl.GL_TEXTURE_2D,
				0,
				gl.GL_RGBA,
				width,
				height,
				0,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				emptyPixels);
			return new OpenGLTextureAtlas(gl, textureId, width, height);
		}

		private OpenGLTextureRegion upload(RendererTextureData textureData) throws Exception {
			return upload(
				textureData.getWidth(),
				textureData.getHeight(),
				textureData.copyToDirectRgbaBuffer(),
				textureData.hasTransparency());
		}

		private OpenGLTextureRegion upload(Renderer3DTextureData textureData) throws Exception {
			return upload(
				textureData.getWidth(),
				textureData.getHeight(),
				textureData.copyToDirectRgbaBuffer(),
				textureData.hasTransparency());
		}

		private OpenGLTextureRegion upload(
			int contentWidth,
			int contentHeight,
			ByteBuffer pixels,
			boolean hasTransparency) throws Exception {
			Placement placement = allocate(contentWidth, contentHeight);
			if (placement == null) {
				return null;
			}

			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			gl.glTexSubImage2D(
				gl.GL_TEXTURE_2D,
				0,
				placement.x - PADDING,
				placement.y - PADDING,
				contentWidth + PADDING * 2,
				contentHeight + PADDING * 2,
				gl.GL_RGBA,
				gl.GL_UNSIGNED_BYTE,
				buildPaddedRgbaPixels(pixels, contentWidth, contentHeight, PADDING));
			return new OpenGLTextureRegion(
				textureId,
				placement.x,
				placement.y,
				contentWidth,
				contentHeight,
				texelCenterU(placement.x),
				texelCenterV(placement.y),
				texelCenterU(placement.x + contentWidth - 1),
				texelCenterV(placement.y + contentHeight - 1),
				hasTransparency);
		}

		private float texelCenterU(int x) {
			return (x + 0.5f) / width;
		}

		private float texelCenterV(int y) {
			return (y + 0.5f) / height;
		}

		private Placement allocate(int contentWidth, int contentHeight) {
			int allocationWidth = contentWidth + PADDING * 2;
			int allocationHeight = contentHeight + PADDING * 2;
			if (allocationWidth > width || allocationHeight > height) {
				return null;
			}

			if (cursorX + allocationWidth > width) {
				cursorX = 0;
				cursorY += rowHeight;
				rowHeight = 0;
			}
			if (cursorY + allocationHeight > height) {
				return null;
			}

			Placement placement = new Placement(cursorX + PADDING, cursorY + PADDING);
			cursorX += allocationWidth;
			rowHeight = Math.max(rowHeight, allocationHeight);
			return placement;
		}

		@Override
		public void close() throws Exception {
			if (closed) {
				return;
			}
			closed = true;
			gl.glDeleteTextures(textureId);
		}

		private static final class Placement {
			private final int x;
			private final int y;

			private Placement(int x, int y) {
				this.x = x;
				this.y = y;
			}
		}
	}

	private static final class OpenGLShaderProgram implements AutoCloseable {
		private static final int POSITION_ATTRIBUTE_LOCATION = 0;
		private static final int TEXTURE_COORD_ATTRIBUTE_LOCATION = 1;
		private static final int MATERIAL_COLOR_ATTRIBUTE_LOCATION = 2;
		private static final int LEGACY_LIGHT_ATTRIBUTE_LOCATION = 3;
		private static final int RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION = 4;
		private static final int BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION = 5;
		private static final int NORMAL_ATTRIBUTE_LOCATION = 6;
		private static final int MODEL_KIND_ATTRIBUTE_LOCATION = 7;
		private static final String FIXED_PIPELINE_VERTEX_SHADER =
			"#version 120\n"
				+ "uniform mat4 uProjectionMatrix;\n"
				+ "attribute vec3 aPosition;\n"
				+ "attribute vec2 aTexCoord;\n"
				+ "attribute vec4 aMaterialColor;\n"
				+ "attribute float aLegacyLight;\n"
				+ "attribute float aBaseLegacyLight;\n"
				+ "attribute vec3 aRawMaterialColor;\n"
				+ "varying vec2 vTexCoord;\n"
				+ "varying vec4 vMaterialColor;\n"
				+ "varying float vLegacyLight;\n"
				+ "varying float vBaseLegacyLight;\n"
				+ "varying vec3 vRawMaterialColor;\n"
				+ "void main() {\n"
				+ "\tgl_Position = uProjectionMatrix * vec4(aPosition, 1.0);\n"
				+ "\tvTexCoord = aTexCoord;\n"
				+ "\tvMaterialColor = aMaterialColor;\n"
				+ "\tvLegacyLight = aLegacyLight;\n"
				+ "\tvBaseLegacyLight = aBaseLegacyLight;\n"
				+ "\tvRawMaterialColor = aRawMaterialColor;\n"
				+ "}\n";
		private static final String FIXED_PIPELINE_FRAGMENT_SHADER =
			"#version 120\n"
			+ "uniform sampler2D uTexture;\n"
			+ "uniform int uTextureEnabled;\n"
			+ "uniform int uLightingMode;\n"
			+ "uniform float uBrightness;\n"
			+ "uniform float uFogStrength;\n"
			+ "uniform float uToneRed;\n"
			+ "uniform float uToneGreen;\n"
			+ "uniform float uToneBlue;\n"
			+ "uniform float uToneBlend;\n"
			+ "varying vec2 vTexCoord;\n"
			+ "varying vec4 vMaterialColor;\n"
			+ "varying float vLegacyLight;\n"
			+ "varying float vBaseLegacyLight;\n"
			+ "varying vec3 vRawMaterialColor;\n"
			+ "float effectiveLegacyLight(float baseLight, float combinedLight) {\n"
			+ "\tfloat fogDelta = max(0.0, combinedLight - baseLight);\n"
			+ "\treturn clamp(baseLight + fogDelta * clamp(uFogStrength, 0.0, 1.0), 0.0, 255.0);\n"
			+ "}\n"
			+ "float textureLightFactor(float light) {\n"
			+ "\tfloat clamped = clamp(light, 0.0, 255.0);\n"
			+ "\tfloat band = floor(clamped / 64.0);\n"
			+ "\tif (band < 1.0) {\n"
			+ "\t\treturn 1.0;\n"
			+ "\t}\n"
			+ "\tif (band < 2.0) {\n"
			+ "\t\treturn 216.0 / 248.0;\n"
			+ "\t}\n"
			+ "\tif (band < 3.0) {\n"
			+ "\t\treturn 184.0 / 248.0;\n"
			+ "\t}\n"
			+ "\treturn 152.0 / 248.0;\n"
			+ "}\n"
			+ "vec3 legacyFlatMaterialColor(vec3 color, float light) {\n"
			+ "\tfloat shade = 255.0 - clamp(light, 0.0, 255.0);\n"
			+ "\tvec3 channel = floor(clamp(color, 0.0, 1.0) * 255.0 + 0.0001);\n"
			+ "\tvec3 shaded = floor(channel * shade * shade / 65536.0) / 255.0;\n"
			+ "\treturn clamp(shaded * uBrightness, 0.0, 1.0);\n"
			+ "}\n"
			+ "float legacyTextureLightFactor(float light) {\n"
			+ "\tfloat band = floor(clamp(light, 0.0, 255.0) / 64.0);\n"
			+ "\tif (band < 1.0) {\n"
			+ "\t\treturn 1.0;\n"
			+ "\t}\n"
			+ "\tif (band < 2.0) {\n"
			+ "\t\treturn 216.0 / 248.0;\n"
			+ "\t}\n"
			+ "\tif (band < 3.0) {\n"
			+ "\t\treturn 184.0 / 248.0;\n"
			+ "\t}\n"
			+ "\treturn 152.0 / 248.0;\n"
			+ "}\n"
			+ "vec3 applyTone(vec3 color) {\n"
			+ "\tvec3 toned = clamp(color * vec3(uToneRed, uToneGreen, uToneBlue), 0.0, 1.0);\n"
			+ "\treturn mix(color, toned, clamp(uToneBlend, 0.0, 1.0));\n"
			+ "}\n"
			+ "void main() {\n"
			+ "\tfloat effectiveLight = effectiveLegacyLight(vBaseLegacyLight, vLegacyLight);\n"
			+ "\tvec4 color = uTextureEnabled != 0\n"
			+ "\t\t? vec4(vec3(textureLightFactor(effectiveLight) * uBrightness), vMaterialColor.a) * texture2D(uTexture, vTexCoord)\n"
			+ "\t\t: vec4(legacyFlatMaterialColor(vRawMaterialColor, effectiveLight), vMaterialColor.a);\n"
			+ "\tcolor.rgb = applyTone(color.rgb);\n"
			+ "\tgl_FragColor = color;\n"
			+ "}\n";
		private static final String RESIDENT_CHUNK_PARITY_VERTEX_SHADER =
			"#version 120\n"
				+ "uniform mat4 uProjectionMatrix;\n"
				+ "uniform mat4 uWorldViewMatrix;\n"
				+ "attribute vec3 aPosition;\n"
				+ "attribute vec2 aTexCoord;\n"
				+ "attribute vec4 aMaterialColor;\n"
				+ "attribute vec3 aRawMaterialColor;\n"
				+ "attribute vec3 aNormal;\n"
				+ "attribute float aModelKind;\n"
				+ "varying vec2 vTexCoord;\n"
				+ "varying vec4 vMaterialColor;\n"
				+ "varying vec3 vRawMaterialColor;\n"
				+ "varying vec3 vNormal;\n"
				+ "varying float vModelKind;\n"
				+ "varying float vCameraDepth;\n"
				+ "void main() {\n"
				+ "\tvec4 worldPosition = vec4(aPosition, 1.0);\n"
				+ "\tgl_Position = uProjectionMatrix * worldPosition;\n"
				+ "\tvTexCoord = aTexCoord;\n"
				+ "\tvMaterialColor = aMaterialColor;\n"
				+ "\tvRawMaterialColor = aRawMaterialColor;\n"
				+ "\tvNormal = aNormal;\n"
				+ "\tvModelKind = aModelKind;\n"
				+ "\tvCameraDepth = (uWorldViewMatrix * worldPosition).z;\n"
				+ "}\n";
		private static final String RESIDENT_CHUNK_PARITY_FRAGMENT_SHADER =
			"#version 120\n"
				+ "uniform sampler2D uTexture;\n"
				+ "uniform int uTextureEnabled;\n"
				+ "uniform int uRawMaterialMode;\n"
				+ "uniform int uRemasterLightingEnabled;\n"
				+ "uniform float uLightDirectionX;\n"
				+ "uniform float uLightDirectionY;\n"
				+ "uniform float uLightDirectionZ;\n"
				+ "uniform float uLightAmbient;\n"
				+ "uniform float uLightIntensity;\n"
				+ "uniform int uFogEnabled;\n"
				+ "uniform float uFogStart;\n"
				+ "uniform float uFogEnd;\n"
				+ "uniform float uToneRed;\n"
				+ "uniform float uToneGreen;\n"
				+ "uniform float uToneBlue;\n"
				+ "uniform float uToneBlend;\n"
				+ "varying vec2 vTexCoord;\n"
				+ "varying vec4 vMaterialColor;\n"
				+ "varying vec3 vRawMaterialColor;\n"
				+ "varying vec3 vNormal;\n"
				+ "varying float vModelKind;\n"
				+ "varying float vCameraDepth;\n"
				+ "vec3 remasterNormal() {\n"
				+ "\tfloat normalLengthSquared = dot(vNormal, vNormal);\n"
				+ "\tif (normalLengthSquared <= 0.0001) {\n"
				+ "\t\treturn vModelKind > 1.5 && vModelKind < 2.5 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);\n"
				+ "\t}\n"
				+ "\treturn normalize(vNormal);\n"
				+ "}\n"
				+ "float wrappedDiffuse(vec3 normal, vec3 lightDirection, float wrap) {\n"
				+ "\treturn clamp((dot(normal, lightDirection) + wrap) / (1.0 + wrap), 0.0, 1.0);\n"
				+ "}\n"
				+ "float remasterDiffuse(vec3 lightDirection) {\n"
				+ "\tvec3 normal = remasterNormal();\n"
				+ "\tif (vModelKind > 0.5 && vModelKind < 1.5) {\n"
				+ "\t\tvec3 terrainNormal = normalize(mix(vec3(0.0, 1.0, 0.0), normal, 0.85));\n"
				+ "\t\tfloat terrainDiffuse = wrappedDiffuse(terrainNormal, lightDirection, 0.08);\n"
				+ "\t\treturn smoothstep(0.08, 0.92, terrainDiffuse);\n"
				+ "\t}\n"
				+ "\tfloat wrapped = wrappedDiffuse(normal, lightDirection, 0.65);\n"
				+ "\tfloat twoSided = abs(dot(normal, lightDirection)) * 0.45;\n"
				+ "\tfloat skyFill = max(lightDirection.y, 0.0) * 0.20;\n"
				+ "\treturn clamp(max(wrapped, twoSided) + skyFill, 0.0, 1.0);\n"
				+ "}\n"
				+ "vec3 applyTone(vec3 color) {\n"
				+ "\tvec3 toned = clamp(color * vec3(uToneRed, uToneGreen, uToneBlue), 0.0, 1.0);\n"
				+ "\treturn mix(color, toned, clamp(uToneBlend, 0.0, 1.0));\n"
				+ "}\n"
				+ "void main() {\n"
				+ "\tvec4 color;\n"
				+ "\tif (uRawMaterialMode != 0) {\n"
				+ "\t\tcolor = uTextureEnabled != 0\n"
				+ "\t\t\t? texture2D(uTexture, vTexCoord)\n"
				+ "\t\t\t: vec4(vRawMaterialColor, vMaterialColor.a);\n"
				+ "\t} else {\n"
				+ "\t\tcolor = uTextureEnabled != 0\n"
				+ "\t\t\t? texture2D(uTexture, vTexCoord) * vMaterialColor\n"
				+ "\t\t\t: vMaterialColor;\n"
				+ "\t}\n"
				+ "\tif (uRemasterLightingEnabled != 0) {\n"
				+ "\t\tvec3 lightDirection = normalize(vec3(uLightDirectionX, uLightDirectionY, uLightDirectionZ));\n"
				+ "\t\tfloat diffuse = remasterDiffuse(lightDirection);\n"
				+ "\t\tfloat light = clamp(uLightAmbient + diffuse * uLightIntensity, 0.0, 1.0);\n"
				+ "\t\tcolor.rgb *= light;\n"
				+ "\t}\n"
				+ "\tcolor.rgb = applyTone(color.rgb);\n"
				+ "\tif (uFogEnabled != 0) {\n"
				+ "\t\tfloat fogRange = max(1.0, uFogEnd - uFogStart);\n"
				+ "\t\tfloat fogFactor = clamp((uFogEnd - vCameraDepth) / fogRange, 0.0, 1.0);\n"
				+ "\t\tcolor.rgb = mix(vec3(0.0, 0.0, 0.0), color.rgb, fogFactor);\n"
				+ "\t}\n"
				+ "\tgl_FragColor = color;\n"
				+ "}\n";

		private final LwjglBindings gl;
		private final int programId;
		private final int projectionMatrixUniformLocation;
		private final int worldViewMatrixUniformLocation;
		private final int textureUniformLocation;
		private final int textureEnabledUniformLocation;
		private final int rawMaterialModeUniformLocation;
		private final int remasterLightingEnabledUniformLocation;
		private final int lightDirectionXUniformLocation;
		private final int lightDirectionYUniformLocation;
		private final int lightDirectionZUniformLocation;
		private final int lightAmbientUniformLocation;
		private final int lightIntensityUniformLocation;
		private final int lightingModeUniformLocation;
		private final int brightnessUniformLocation;
		private final int fogStrengthUniformLocation;
		private final int toneRedUniformLocation;
		private final int toneGreenUniformLocation;
		private final int toneBlueUniformLocation;
		private final int toneBlendUniformLocation;
		private final int fogEnabledUniformLocation;
		private final int fogStartUniformLocation;
		private final int fogEndUniformLocation;
		private boolean closed;

		private OpenGLShaderProgram(
			LwjglBindings gl,
			int programId,
			int projectionMatrixUniformLocation,
			int worldViewMatrixUniformLocation,
			int textureUniformLocation,
			int textureEnabledUniformLocation,
			int rawMaterialModeUniformLocation,
			int remasterLightingEnabledUniformLocation,
			int lightDirectionXUniformLocation,
			int lightDirectionYUniformLocation,
			int lightDirectionZUniformLocation,
			int lightAmbientUniformLocation,
			int lightIntensityUniformLocation,
			int lightingModeUniformLocation,
			int brightnessUniformLocation,
			int fogStrengthUniformLocation,
			int toneRedUniformLocation,
			int toneGreenUniformLocation,
			int toneBlueUniformLocation,
			int toneBlendUniformLocation,
			int fogEnabledUniformLocation,
			int fogStartUniformLocation,
			int fogEndUniformLocation) {
			this.gl = gl;
			this.programId = programId;
			this.projectionMatrixUniformLocation = projectionMatrixUniformLocation;
			this.worldViewMatrixUniformLocation = worldViewMatrixUniformLocation;
			this.textureUniformLocation = textureUniformLocation;
			this.textureEnabledUniformLocation = textureEnabledUniformLocation;
			this.rawMaterialModeUniformLocation = rawMaterialModeUniformLocation;
			this.remasterLightingEnabledUniformLocation = remasterLightingEnabledUniformLocation;
			this.lightDirectionXUniformLocation = lightDirectionXUniformLocation;
			this.lightDirectionYUniformLocation = lightDirectionYUniformLocation;
			this.lightDirectionZUniformLocation = lightDirectionZUniformLocation;
			this.lightAmbientUniformLocation = lightAmbientUniformLocation;
			this.lightIntensityUniformLocation = lightIntensityUniformLocation;
			this.lightingModeUniformLocation = lightingModeUniformLocation;
			this.brightnessUniformLocation = brightnessUniformLocation;
			this.fogStrengthUniformLocation = fogStrengthUniformLocation;
			this.toneRedUniformLocation = toneRedUniformLocation;
			this.toneGreenUniformLocation = toneGreenUniformLocation;
			this.toneBlueUniformLocation = toneBlueUniformLocation;
			this.toneBlendUniformLocation = toneBlendUniformLocation;
			this.fogEnabledUniformLocation = fogEnabledUniformLocation;
			this.fogStartUniformLocation = fogStartUniformLocation;
			this.fogEndUniformLocation = fogEndUniformLocation;
		}

		private static OpenGLShaderProgram createProjectedWorld(LwjglBindings gl) throws Exception {
			int vertexShader = compileShader(gl, gl.GL_VERTEX_SHADER, FIXED_PIPELINE_VERTEX_SHADER);
			int fragmentShader = 0;
			int program = 0;
			try {
				fragmentShader = compileShader(gl, gl.GL_FRAGMENT_SHADER, FIXED_PIPELINE_FRAGMENT_SHADER);
				program = gl.glCreateProgram();
				gl.glAttachShader(program, vertexShader);
				gl.glAttachShader(program, fragmentShader);
				gl.glBindAttribLocation(program, POSITION_ATTRIBUTE_LOCATION, "aPosition");
				gl.glBindAttribLocation(program, TEXTURE_COORD_ATTRIBUTE_LOCATION, "aTexCoord");
				gl.glBindAttribLocation(program, MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aMaterialColor");
				gl.glBindAttribLocation(program, LEGACY_LIGHT_ATTRIBUTE_LOCATION, "aLegacyLight");
				gl.glBindAttribLocation(program, RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aRawMaterialColor");
				gl.glBindAttribLocation(program, BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION, "aBaseLegacyLight");
				gl.glLinkProgram(program);
				if (gl.glGetProgrami(program, gl.GL_LINK_STATUS) == 0) {
					String log = gl.glGetProgramInfoLog(program);
					throw new IllegalStateException("shader link failed: " + log);
				}
				OpenGLShaderProgram shaderProgram =
					new OpenGLShaderProgram(
						gl,
						program,
						gl.glGetUniformLocation(program, "uProjectionMatrix"),
						-1,
						gl.glGetUniformLocation(program, "uTexture"),
						gl.glGetUniformLocation(program, "uTextureEnabled"),
						-1,
						-1,
						-1,
						-1,
						-1,
						-1,
						-1,
						gl.glGetUniformLocation(program, "uLightingMode"),
						gl.glGetUniformLocation(program, "uBrightness"),
						gl.glGetUniformLocation(program, "uFogStrength"),
						gl.glGetUniformLocation(program, "uToneRed"),
						gl.glGetUniformLocation(program, "uToneGreen"),
						gl.glGetUniformLocation(program, "uToneBlue"),
						gl.glGetUniformLocation(program, "uToneBlend"),
						-1,
						-1,
						-1);
				program = 0;
				return shaderProgram;
			} finally {
				if (program != 0) {
					gl.glDeleteProgram(program);
				}
				if (fragmentShader != 0) {
					gl.glDeleteShader(fragmentShader);
				}
				if (vertexShader != 0) {
					gl.glDeleteShader(vertexShader);
				}
			}
		}

		private static OpenGLShaderProgram createResidentChunkParity(LwjglBindings gl) throws Exception {
			int vertexShader = compileShader(gl, gl.GL_VERTEX_SHADER, RESIDENT_CHUNK_PARITY_VERTEX_SHADER);
			int fragmentShader = 0;
			int program = 0;
			try {
				fragmentShader = compileShader(gl, gl.GL_FRAGMENT_SHADER, RESIDENT_CHUNK_PARITY_FRAGMENT_SHADER);
				program = gl.glCreateProgram();
				gl.glAttachShader(program, vertexShader);
				gl.glAttachShader(program, fragmentShader);
				gl.glBindAttribLocation(program, POSITION_ATTRIBUTE_LOCATION, "aPosition");
				gl.glBindAttribLocation(program, TEXTURE_COORD_ATTRIBUTE_LOCATION, "aTexCoord");
				gl.glBindAttribLocation(program, MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aMaterialColor");
				gl.glBindAttribLocation(program, RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION, "aRawMaterialColor");
				gl.glBindAttribLocation(program, NORMAL_ATTRIBUTE_LOCATION, "aNormal");
				gl.glBindAttribLocation(program, MODEL_KIND_ATTRIBUTE_LOCATION, "aModelKind");
				gl.glLinkProgram(program);
				if (gl.glGetProgrami(program, gl.GL_LINK_STATUS) == 0) {
					String log = gl.glGetProgramInfoLog(program);
					throw new IllegalStateException("shader link failed: " + log);
				}
				OpenGLShaderProgram shaderProgram =
					new OpenGLShaderProgram(
						gl,
						program,
						gl.glGetUniformLocation(program, "uProjectionMatrix"),
						gl.glGetUniformLocation(program, "uWorldViewMatrix"),
						gl.glGetUniformLocation(program, "uTexture"),
						gl.glGetUniformLocation(program, "uTextureEnabled"),
						gl.glGetUniformLocation(program, "uRawMaterialMode"),
						gl.glGetUniformLocation(program, "uRemasterLightingEnabled"),
						gl.glGetUniformLocation(program, "uLightDirectionX"),
						gl.glGetUniformLocation(program, "uLightDirectionY"),
						gl.glGetUniformLocation(program, "uLightDirectionZ"),
						gl.glGetUniformLocation(program, "uLightAmbient"),
						gl.glGetUniformLocation(program, "uLightIntensity"),
						-1,
						-1,
						-1,
						gl.glGetUniformLocation(program, "uToneRed"),
						gl.glGetUniformLocation(program, "uToneGreen"),
						gl.glGetUniformLocation(program, "uToneBlue"),
						gl.glGetUniformLocation(program, "uToneBlend"),
						gl.glGetUniformLocation(program, "uFogEnabled"),
						gl.glGetUniformLocation(program, "uFogStart"),
						gl.glGetUniformLocation(program, "uFogEnd"));
				program = 0;
				return shaderProgram;
			} finally {
				if (program != 0) {
					gl.glDeleteProgram(program);
				}
				if (fragmentShader != 0) {
					gl.glDeleteShader(fragmentShader);
				}
				if (vertexShader != 0) {
					gl.glDeleteShader(vertexShader);
				}
			}
		}

		private static int compileShader(LwjglBindings gl, int type, String source) throws Exception {
			int shader = gl.glCreateShader(type);
			try {
				gl.glShaderSource(shader, source);
				gl.glCompileShader(shader);
				if (gl.glGetShaderi(shader, gl.GL_COMPILE_STATUS) == 0) {
					String log = gl.glGetShaderInfoLog(shader);
					throw new IllegalStateException("shader compile failed: " + log);
				}
				int compiledShader = shader;
				shader = 0;
				return compiledShader;
			} finally {
				if (shader != 0) {
					gl.glDeleteShader(shader);
				}
			}
		}

		private void useWorld(FloatBuffer projectionMatrix, boolean textureEnabled) throws Exception {
			if (projectionMatrix == null) {
				throw new IllegalArgumentException("world shader requires an explicit projection matrix");
			}
			gl.glUseProgram(programId);
			if (projectionMatrixUniformLocation >= 0) {
				gl.glUniformMatrix4fv(projectionMatrixUniformLocation, false, projectionMatrix);
			}
			if (textureUniformLocation >= 0) {
				gl.glUniform1i(textureUniformLocation, 0);
			}
			if (textureEnabledUniformLocation >= 0) {
				gl.glUniform1i(textureEnabledUniformLocation, textureEnabled ? 1 : 0);
			}
			if (lightingModeUniformLocation >= 0) {
				gl.glUniform1i(lightingModeUniformLocation, RendererLightingSettings.getMode().ordinal());
			}
			if (brightnessUniformLocation >= 0) {
				gl.glUniform1f(brightnessUniformLocation, RendererDayNightCycle.currentBrightnessMultiplier());
			}
			if (fogStrengthUniformLocation >= 0) {
				gl.glUniform1f(fogStrengthUniformLocation, RendererFogSettings.getMode().multiplier);
			}
			RendererDayNightCycle.Presentation presentation = RendererDayNightCycle.currentPresentation();
			if (toneRedUniformLocation >= 0) {
				gl.glUniform1f(toneRedUniformLocation, presentation.redMultiplier);
			}
			if (toneGreenUniformLocation >= 0) {
				gl.glUniform1f(toneGreenUniformLocation, presentation.greenMultiplier);
			}
			if (toneBlueUniformLocation >= 0) {
				gl.glUniform1f(toneBlueUniformLocation, presentation.blueMultiplier);
			}
			if (toneBlendUniformLocation >= 0) {
				gl.glUniform1f(toneBlendUniformLocation, presentation.toneBlend);
			}
		}

		private void useResidentChunk(
			FloatBuffer worldToClipMatrix,
			FloatBuffer worldViewMatrix,
			boolean textureEnabled,
			boolean rawMaterialMode,
			boolean remasterLightingEnabled,
			Renderer3DFrame frame) throws Exception {
			if (worldViewMatrix == null) {
				throw new IllegalArgumentException("resident chunk shader requires an explicit world-view matrix");
			}
			useWorld(worldToClipMatrix, textureEnabled);
			if (worldViewMatrixUniformLocation >= 0) {
				gl.glUniformMatrix4fv(worldViewMatrixUniformLocation, false, worldViewMatrix);
			}
			if (rawMaterialModeUniformLocation >= 0) {
				gl.glUniform1i(rawMaterialModeUniformLocation, rawMaterialMode ? 1 : 0);
			}
			if (remasterLightingEnabledUniformLocation >= 0) {
				gl.glUniform1i(remasterLightingEnabledUniformLocation, remasterLightingEnabled ? 1 : 0);
			}
			if (lightDirectionXUniformLocation >= 0) {
				gl.glUniform1f(lightDirectionXUniformLocation, RendererRemasterLightSettings.getLightDirectionX());
			}
			if (lightDirectionYUniformLocation >= 0) {
				gl.glUniform1f(lightDirectionYUniformLocation, RendererRemasterLightSettings.getLightDirectionY());
			}
			if (lightDirectionZUniformLocation >= 0) {
				gl.glUniform1f(lightDirectionZUniformLocation, RendererRemasterLightSettings.getLightDirectionZ());
			}
			if (lightAmbientUniformLocation >= 0) {
				gl.glUniform1f(lightAmbientUniformLocation, RendererRemasterLightSettings.getAmbient());
			}
			if (lightIntensityUniformLocation >= 0) {
				gl.glUniform1f(lightIntensityUniformLocation, RendererRemasterLightSettings.getIntensity());
			}
			boolean fogEnabled =
				!rawMaterialMode
					&& !remasterLightingEnabled
					&& frame != null
					&& RendererFogSettings.getMode() != RendererFogSettings.Mode.OFF;
			if (fogEnabledUniformLocation >= 0) {
				gl.glUniform1i(fogEnabledUniformLocation, fogEnabled ? 1 : 0);
			}
			if (fogStartUniformLocation >= 0) {
				gl.glUniform1f(fogStartUniformLocation, fogEnabled ? frame.getFogStartDistance() : 0.0f);
			}
			if (fogEndUniformLocation >= 0) {
				gl.glUniform1f(fogEndUniformLocation, fogEnabled ? frame.getFogDistance() : 1.0f);
			}
		}

		private void bindWorldParityAttributes(
			int positionComponents,
			int textureCoordComponents,
			int materialColorComponents,
			int rawMaterialColorComponents,
			int normalComponents,
			int modelKindComponents,
			int strideBytes,
			long positionOffsetBytes,
			long textureCoordOffsetBytes,
			long materialColorOffsetBytes,
			long rawMaterialColorOffsetBytes,
			long normalOffsetBytes,
			long modelKindOffsetBytes) throws Exception {
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(NORMAL_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(MODEL_KIND_ATTRIBUTE_LOCATION);
			gl.glVertexAttribPointer(
				POSITION_ATTRIBUTE_LOCATION,
				positionComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				positionOffsetBytes);
			gl.glVertexAttribPointer(
				TEXTURE_COORD_ATTRIBUTE_LOCATION,
				textureCoordComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				textureCoordOffsetBytes);
			gl.glVertexAttribPointer(
				MATERIAL_COLOR_ATTRIBUTE_LOCATION,
				materialColorComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				materialColorOffsetBytes);
			gl.glVertexAttribPointer(
				RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION,
				rawMaterialColorComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				rawMaterialColorOffsetBytes);
			gl.glVertexAttribPointer(
				NORMAL_ATTRIBUTE_LOCATION,
				normalComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				normalOffsetBytes);
			gl.glVertexAttribPointer(
				MODEL_KIND_ATTRIBUTE_LOCATION,
				modelKindComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				modelKindOffsetBytes);
		}

		private void bindWorldTextureAttributes(
			int positionComponents,
			int textureCoordComponents,
			int materialColorComponents,
			int legacyLightComponents,
			int baseLegacyLightComponents,
			int rawMaterialColorComponents,
			int strideBytes,
			long positionOffsetBytes,
			long textureCoordOffsetBytes,
			long materialColorOffsetBytes,
			long legacyLightOffsetBytes,
			long baseLegacyLightOffsetBytes,
			long rawMaterialColorOffsetBytes) throws Exception {
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(LEGACY_LIGHT_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION);
			gl.glEnableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glVertexAttribPointer(
				POSITION_ATTRIBUTE_LOCATION,
				positionComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				positionOffsetBytes);
			gl.glVertexAttribPointer(
				TEXTURE_COORD_ATTRIBUTE_LOCATION,
				textureCoordComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				textureCoordOffsetBytes);
			gl.glVertexAttribPointer(
				MATERIAL_COLOR_ATTRIBUTE_LOCATION,
				materialColorComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				materialColorOffsetBytes);
			gl.glVertexAttribPointer(
				LEGACY_LIGHT_ATTRIBUTE_LOCATION,
				legacyLightComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				legacyLightOffsetBytes);
			gl.glVertexAttribPointer(
				BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION,
				baseLegacyLightComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				baseLegacyLightOffsetBytes);
			gl.glVertexAttribPointer(
				RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION,
				rawMaterialColorComponents,
				gl.GL_FLOAT,
				false,
				strideBytes,
				rawMaterialColorOffsetBytes);
		}

		private void bindWorldFlatColorAttributes(
			int positionComponents,
			int textureCoordComponents,
			int materialColorComponents,
			int strideBytes,
			long positionOffsetBytes,
			long textureCoordOffsetBytes,
			long materialColorOffsetBytes) throws Exception {
			bindWorldTextureAttributes(
				positionComponents,
				textureCoordComponents,
				materialColorComponents,
				1,
				1,
				3,
				strideBytes,
				positionOffsetBytes,
				textureCoordOffsetBytes,
				materialColorOffsetBytes,
				OpenGLWorldMeshRenderer.SHADER_LEGACY_LIGHT_OFFSET_BYTES,
				OpenGLWorldMeshRenderer.SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES,
				OpenGLWorldMeshRenderer.SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES);
		}

		private void unbindWorldTextureAttributes() throws Exception {
			gl.glDisableVertexAttribArray(MODEL_KIND_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(NORMAL_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(RAW_MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(BASE_LEGACY_LIGHT_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(LEGACY_LIGHT_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(MATERIAL_COLOR_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(TEXTURE_COORD_ATTRIBUTE_LOCATION);
			gl.glDisableVertexAttribArray(POSITION_ATTRIBUTE_LOCATION);
		}

		@Override
		public void close() throws Exception {
			if (closed) {
				return;
			}
			closed = true;
			if (programId != 0) {
				gl.glDeleteProgram(programId);
			}
		}
	}

	private static final class LwjglBindings {
		private final Class<?> glfwClass;
		private final Method glfwInit;
		private final Method glfwTerminate;
		private final Method glfwDefaultWindowHints;
		private final Method glfwWindowHint;
		private final Method glfwCreateWindow;
		private final Method glfwMakeContextCurrent;
		private final Method glfwSwapInterval;
		private final Method glfwShowWindow;
		private final Method glfwHideWindow;
		private final Method glfwWindowShouldClose;
		private final Method glfwPollEvents;
		private final Method glfwSwapBuffers;
		private final Method glfwDestroyWindow;
		private final Method glfwSetWindowSize;
		private final Method glfwGetWindowPos;
		private final Method glfwSetWindowPos;
		private final Method glfwSetWindowAttrib;
		private final Method glfwGetPrimaryMonitor;
		private final Method glfwGetMonitorPos;
		private final Method glfwGetVideoMode;
		private final Class<?> glfwScrollCallbackInterface;
		private final Method glfwSetScrollCallback;
		private final Object glfwScrollCallbackCif;
		private final Class<?> glfwKeyCallbackInterface;
		private final Method glfwSetKeyCallback;
		private final Object glfwKeyCallbackCif;
		private final Class<?> glfwCharCallbackInterface;
		private final Method glfwSetCharCallback;
		private final Object glfwCharCallbackCif;
		private final Class<?> glfwWindowFocusCallbackInterface;
		private final Method glfwSetWindowFocusCallback;
		private final Object glfwWindowFocusCallbackCif;
		private final Method callbackCreate;
		private final Method callbackFree;
		private final Method memGetAddress;
		private final Method memGetInt;
		private final Method memGetDouble;
		private final Method glfwGetWindowSize;
		private final Method glfwGetFramebufferSize;
		private final Method glfwGetWindowAttrib;
		private final Method glfwGetCursorPos;
		private final Method glfwGetMouseButton;
		private final Method glfwGetKey;
		private final Method createCapabilities;
		private final Method glClearColor;
		private final Method glClear;
		private final Method glViewport;
		private final Method glEnable;
		private final Method glDisable;
		private final Method glScissor;
		private final Method glPushAttrib;
		private final Method glPopAttrib;
		private final Method glPushClientAttrib;
		private final Method glPopClientAttrib;
		private final Method glPolygonOffset;
		private final Method glPolygonMode;
		private final Method glCullFace;
		private final Method glGenTextures;
		private final Method glDeleteTextures;
		private final Method glBindTexture;
		private final Method glTexParameteri;
		private final Method glTexImage2D;
		private final Method glTexSubImage2D;
		private final Method glReadPixels;
		private final Method glBlendFunc;
		private final Method glAlphaFunc;
		private final Method glDepthMask;
		private final Method glColorMask;
		private final Method glColor4f;
		private final Method glFogi;
		private final Method glFogf;
		private final Method glFogfv;
		private final Method glGetString;
		private final Method glMatrixMode;
		private final Method glLoadIdentity;
		private final Method glLoadMatrixf;
		private final Method glOrtho;
		private final Method glGenBuffers;
		private final Method glDeleteBuffers;
		private final Method glBindBuffer;
		private final Method glBufferDataFloat;
		private final Method glBufferDataInt;
		private final Method glEnableClientState;
		private final Method glDisableClientState;
		private final Method glVertexPointer;
		private final Method glColorPointer;
		private final Method glTexCoordPointer;
		private final Method glDrawElements;
		private final Method glBegin;
		private final Method glEnd;
		private final Method glTexCoord2f;
		private final Method glVertex3f;
		private final Method glLineWidth;
		private final Method glCreateShader;
		private final Method glShaderSource;
		private final Method glCompileShader;
		private final Method glGetShaderi;
		private final Method glGetShaderInfoLog;
		private final Method glDeleteShader;
		private final Method glCreateProgram;
		private final Method glAttachShader;
		private final Method glBindAttribLocation;
		private final Method glLinkProgram;
		private final Method glGetProgrami;
		private final Method glGetProgramInfoLog;
		private final Method glUseProgram;
		private final Method glGetUniformLocation;
		private final Method glUniform1i;
		private final Method glUniform1f;
		private final Method glUniformMatrix4fv;
		private final Method glEnableVertexAttribArray;
		private final Method glDisableVertexAttribArray;
		private final Method glVertexAttribPointer;
		private final Method glDeleteProgram;

		private final int GLFW_FALSE;
		private final int GLFW_TRUE;
		private final int GLFW_RESIZABLE;
		private final int GLFW_VISIBLE;
		private final int GLFW_FOCUSED;
		private final int GLFW_DECORATED;
		private final int GLFW_PRESS;
		private final int GLFW_RELEASE;
		private final int GLFW_REPEAT;
		private final int GLFW_MOD_SHIFT;
		private final int GLFW_MOD_CONTROL;
		private final int GLFW_KEY_LEFT_SHIFT;
		private final int GLFW_KEY_RIGHT_SHIFT;
		private final int GLFW_KEY_LEFT_CONTROL;
		private final int GLFW_KEY_RIGHT_CONTROL;
		private final int GLFW_KEY_LEFT_ALT;
		private final int GLFW_KEY_RIGHT_ALT;
		private final int GLFW_MOUSE_BUTTON_LEFT;
		private final int GLFW_MOUSE_BUTTON_RIGHT;
		private final int GLFW_MOUSE_BUTTON_MIDDLE;
		private final int POINTER_SIZE;
		private final int GL_COLOR_BUFFER_BIT;
		private final int GL_DEPTH_BUFFER_BIT;
		private final int GL_ALL_ATTRIB_BITS;
		private final int GL_CLIENT_ALL_ATTRIB_BITS;
		private final int GL_TEXTURE_2D;
		private final int GL_TEXTURE_MIN_FILTER;
		private final int GL_TEXTURE_MAG_FILTER;
		private final int GL_TEXTURE_WRAP_S;
		private final int GL_TEXTURE_WRAP_T;
		private final int GL_NEAREST;
		private final int GL_LINEAR;
		private final int GL_CLAMP;
		private final int GL_CLAMP_TO_EDGE;
		private final int GL_RGBA;
		private final int GL_UNSIGNED_BYTE;
		private final int GL_BLEND;
		private final int GL_DEPTH_TEST;
		private final int GL_FOG;
		private final int GL_FOG_MODE;
		private final int GL_FOG_START;
		private final int GL_FOG_END;
		private final int GL_FOG_COLOR;
		private final int GL_SCISSOR_TEST;
		private final int GL_POLYGON_OFFSET_FILL;
		private final int GL_CULL_FACE;
		private final int GL_BACK;
		private final int GL_SRC_ALPHA;
		private final int GL_ONE_MINUS_SRC_ALPHA;
		private final int GL_ALPHA_TEST;
		private final int GL_GREATER;
		private final int GL_FRONT_AND_BACK;
		private final int GL_LINE;
		private final int GL_FILL;
		private final int GL_LINES;
		private final int GL_QUADS;
		private final int GL_TRIANGLES;
		private final int GL_VERTEX_ARRAY;
		private final int GL_COLOR_ARRAY;
		private final int GL_TEXTURE_COORD_ARRAY;
		private final int GL_FLOAT;
		private final int GL_UNSIGNED_INT;
		private final int GL_ARRAY_BUFFER;
		private final int GL_ELEMENT_ARRAY_BUFFER;
		private final int GL_STREAM_DRAW;
		private final int GL_STATIC_DRAW;
		private final int GL_PROJECTION;
		private final int GL_MODELVIEW;
		private final int GL_VERTEX_SHADER;
		private final int GL_FRAGMENT_SHADER;
		private final int GL_COMPILE_STATUS;
		private final int GL_LINK_STATUS;
		private final int GL_VENDOR;
		private final int GL_RENDERER;
		private final int GL_VERSION;

		static LwjglBindings load() throws Exception {
			Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
			Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
			Class<?> gl11Class = Class.forName("org.lwjgl.opengl.GL11");
			Class<?> gl12Class = optionalClass("org.lwjgl.opengl.GL12");
			Class<?> gl15Class = Class.forName("org.lwjgl.opengl.GL15");
			Class<?> gl20Class = Class.forName("org.lwjgl.opengl.GL20");
			return new LwjglBindings(glfwClass, glClass, gl11Class, gl12Class, gl15Class, gl20Class);
		}

		private LwjglBindings(
			Class<?> glfwClass,
			Class<?> glClass,
			Class<?> gl11Class,
			Class<?> gl12Class,
			Class<?> gl15Class,
			Class<?> gl20Class)
			throws Exception {
			Class<?> ffiCifClass = Class.forName("org.lwjgl.system.libffi.FFICIF");
			Class<?> callbackClass = Class.forName("org.lwjgl.system.Callback");
			Class<?> memoryUtilClass = Class.forName("org.lwjgl.system.MemoryUtil");
			Class<?> pointerClass = Class.forName("org.lwjgl.system.Pointer");

			this.glfwClass = glfwClass;
			glfwInit = method(glfwClass, "glfwInit");
			glfwTerminate = method(glfwClass, "glfwTerminate");
			glfwDefaultWindowHints = method(glfwClass, "glfwDefaultWindowHints");
			glfwWindowHint = method(glfwClass, "glfwWindowHint", int.class, int.class);
			glfwCreateWindow = method(
				glfwClass,
				"glfwCreateWindow",
				int.class,
				int.class,
				CharSequence.class,
				long.class,
				long.class);
			glfwMakeContextCurrent = method(glfwClass, "glfwMakeContextCurrent", long.class);
			glfwSwapInterval = method(glfwClass, "glfwSwapInterval", int.class);
			glfwShowWindow = method(glfwClass, "glfwShowWindow", long.class);
			glfwHideWindow = method(glfwClass, "glfwHideWindow", long.class);
			glfwWindowShouldClose = method(glfwClass, "glfwWindowShouldClose", long.class);
			glfwPollEvents = method(glfwClass, "glfwPollEvents");
			glfwSwapBuffers = method(glfwClass, "glfwSwapBuffers", long.class);
			glfwDestroyWindow = method(glfwClass, "glfwDestroyWindow", long.class);
			glfwSetWindowSize = method(glfwClass, "glfwSetWindowSize", long.class, int.class, int.class);
			glfwGetWindowPos = method(glfwClass, "glfwGetWindowPos", long.class, int[].class, int[].class);
			glfwSetWindowPos = method(glfwClass, "glfwSetWindowPos", long.class, int.class, int.class);
			glfwSetWindowAttrib = method(glfwClass, "glfwSetWindowAttrib", long.class, int.class, int.class);
			glfwGetPrimaryMonitor = method(glfwClass, "glfwGetPrimaryMonitor");
			glfwGetMonitorPos = method(glfwClass, "glfwGetMonitorPos", long.class, int[].class, int[].class);
			glfwGetVideoMode = method(glfwClass, "glfwGetVideoMode", long.class);
			glfwScrollCallbackInterface = Class.forName("org.lwjgl.glfw.GLFWScrollCallbackI");
			glfwSetScrollCallback =
				method(glfwClass, "glfwSetScrollCallback", long.class, glfwScrollCallbackInterface);
			glfwScrollCallbackCif = fieldValue(glfwScrollCallbackInterface, "CIF");
			glfwKeyCallbackInterface = Class.forName("org.lwjgl.glfw.GLFWKeyCallbackI");
			glfwSetKeyCallback = method(glfwClass, "glfwSetKeyCallback", long.class, glfwKeyCallbackInterface);
			glfwKeyCallbackCif = fieldValue(glfwKeyCallbackInterface, "CIF");
			glfwCharCallbackInterface = Class.forName("org.lwjgl.glfw.GLFWCharCallbackI");
			glfwSetCharCallback = method(glfwClass, "glfwSetCharCallback", long.class, glfwCharCallbackInterface);
			glfwCharCallbackCif = fieldValue(glfwCharCallbackInterface, "CIF");
			glfwWindowFocusCallbackInterface = Class.forName("org.lwjgl.glfw.GLFWWindowFocusCallbackI");
			glfwSetWindowFocusCallback =
				method(glfwClass, "glfwSetWindowFocusCallback", long.class, glfwWindowFocusCallbackInterface);
			glfwWindowFocusCallbackCif = fieldValue(glfwWindowFocusCallbackInterface, "CIF");
			callbackCreate = declaredMethod(callbackClass, "create", ffiCifClass, Object.class);
			callbackFree = method(callbackClass, "free", long.class);
			memGetAddress = method(memoryUtilClass, "memGetAddress", long.class);
			memGetInt = method(memoryUtilClass, "memGetInt", long.class);
			memGetDouble = method(memoryUtilClass, "memGetDouble", long.class);
			glfwGetWindowSize = method(glfwClass, "glfwGetWindowSize", long.class, int[].class, int[].class);
			glfwGetFramebufferSize =
				method(glfwClass, "glfwGetFramebufferSize", long.class, int[].class, int[].class);
			glfwGetWindowAttrib = method(glfwClass, "glfwGetWindowAttrib", long.class, int.class);
			glfwGetCursorPos = method(glfwClass, "glfwGetCursorPos", long.class, double[].class, double[].class);
			glfwGetMouseButton = method(glfwClass, "glfwGetMouseButton", long.class, int.class);
			glfwGetKey = method(glfwClass, "glfwGetKey", long.class, int.class);
			createCapabilities = method(glClass, "createCapabilities");

			glClearColor = method(gl11Class, "glClearColor", float.class, float.class, float.class, float.class);
			glClear = method(gl11Class, "glClear", int.class);
			glViewport = method(gl11Class, "glViewport", int.class, int.class, int.class, int.class);
			glEnable = method(gl11Class, "glEnable", int.class);
			glDisable = method(gl11Class, "glDisable", int.class);
			glScissor = method(gl11Class, "glScissor", int.class, int.class, int.class, int.class);
			glPushAttrib = method(gl11Class, "glPushAttrib", int.class);
			glPopAttrib = method(gl11Class, "glPopAttrib");
			glPushClientAttrib = method(gl11Class, "glPushClientAttrib", int.class);
			glPopClientAttrib = method(gl11Class, "glPopClientAttrib");
			glPolygonOffset = method(gl11Class, "glPolygonOffset", float.class, float.class);
			glPolygonMode = method(gl11Class, "glPolygonMode", int.class, int.class);
			glCullFace = method(gl11Class, "glCullFace", int.class);
			glGenTextures = method(gl11Class, "glGenTextures");
			glDeleteTextures = method(gl11Class, "glDeleteTextures", int.class);
			glBindTexture = method(gl11Class, "glBindTexture", int.class, int.class);
			glTexParameteri = method(gl11Class, "glTexParameteri", int.class, int.class, int.class);
			glTexImage2D = method(
				gl11Class,
				"glTexImage2D",
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				ByteBuffer.class);
			glTexSubImage2D = method(
				gl11Class,
				"glTexSubImage2D",
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				ByteBuffer.class);
			glReadPixels = method(
				gl11Class,
				"glReadPixels",
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				int.class,
				ByteBuffer.class);
			glBlendFunc = method(gl11Class, "glBlendFunc", int.class, int.class);
			glAlphaFunc = method(gl11Class, "glAlphaFunc", int.class, float.class);
			glDepthMask = method(gl11Class, "glDepthMask", boolean.class);
			glColorMask = method(gl11Class, "glColorMask", boolean.class, boolean.class, boolean.class, boolean.class);
			glColor4f = method(gl11Class, "glColor4f", float.class, float.class, float.class, float.class);
			glFogi = method(gl11Class, "glFogi", int.class, int.class);
			glFogf = method(gl11Class, "glFogf", int.class, float.class);
			glFogfv = method(gl11Class, "glFogfv", int.class, FloatBuffer.class);
			glGetString = method(gl11Class, "glGetString", int.class);
			glMatrixMode = method(gl11Class, "glMatrixMode", int.class);
			glLoadIdentity = method(gl11Class, "glLoadIdentity");
			glLoadMatrixf = method(gl11Class, "glLoadMatrixf", FloatBuffer.class);
			glOrtho = method(
				gl11Class,
				"glOrtho",
				double.class,
				double.class,
				double.class,
				double.class,
				double.class,
				double.class);
			glGenBuffers = method(gl15Class, "glGenBuffers");
			glDeleteBuffers = method(gl15Class, "glDeleteBuffers", int.class);
			glBindBuffer = method(gl15Class, "glBindBuffer", int.class, int.class);
			glBufferDataFloat = method(gl15Class, "glBufferData", int.class, FloatBuffer.class, int.class);
			glBufferDataInt = method(gl15Class, "glBufferData", int.class, IntBuffer.class, int.class);
			glEnableClientState = method(gl11Class, "glEnableClientState", int.class);
			glDisableClientState = method(gl11Class, "glDisableClientState", int.class);
			glVertexPointer = method(gl11Class, "glVertexPointer", int.class, int.class, int.class, long.class);
			glColorPointer = method(gl11Class, "glColorPointer", int.class, int.class, int.class, long.class);
			glTexCoordPointer = method(gl11Class, "glTexCoordPointer", int.class, int.class, int.class, long.class);
			glDrawElements = method(gl11Class, "glDrawElements", int.class, int.class, int.class, long.class);
			glBegin = method(gl11Class, "glBegin", int.class);
			glEnd = method(gl11Class, "glEnd");
			glTexCoord2f = method(gl11Class, "glTexCoord2f", float.class, float.class);
			glVertex3f = method(gl11Class, "glVertex3f", float.class, float.class, float.class);
			glLineWidth = method(gl11Class, "glLineWidth", float.class);
			glCreateShader = method(gl20Class, "glCreateShader", int.class);
			glShaderSource = method(gl20Class, "glShaderSource", int.class, CharSequence.class);
			glCompileShader = method(gl20Class, "glCompileShader", int.class);
			glGetShaderi = method(gl20Class, "glGetShaderi", int.class, int.class);
			glGetShaderInfoLog = method(gl20Class, "glGetShaderInfoLog", int.class);
			glDeleteShader = method(gl20Class, "glDeleteShader", int.class);
			glCreateProgram = method(gl20Class, "glCreateProgram");
			glAttachShader = method(gl20Class, "glAttachShader", int.class, int.class);
			glBindAttribLocation = method(gl20Class, "glBindAttribLocation", int.class, int.class, CharSequence.class);
			glLinkProgram = method(gl20Class, "glLinkProgram", int.class);
			glGetProgrami = method(gl20Class, "glGetProgrami", int.class, int.class);
			glGetProgramInfoLog = method(gl20Class, "glGetProgramInfoLog", int.class);
			glUseProgram = method(gl20Class, "glUseProgram", int.class);
			glGetUniformLocation = method(gl20Class, "glGetUniformLocation", int.class, CharSequence.class);
			glUniform1i = method(gl20Class, "glUniform1i", int.class, int.class);
			glUniform1f = method(gl20Class, "glUniform1f", int.class, float.class);
			glUniformMatrix4fv = method(gl20Class, "glUniformMatrix4fv", int.class, boolean.class, FloatBuffer.class);
			glEnableVertexAttribArray = method(gl20Class, "glEnableVertexAttribArray", int.class);
			glDisableVertexAttribArray = method(gl20Class, "glDisableVertexAttribArray", int.class);
			glVertexAttribPointer =
				method(gl20Class, "glVertexAttribPointer", int.class, int.class, int.class, boolean.class, int.class, long.class);
			glDeleteProgram = method(gl20Class, "glDeleteProgram", int.class);

			GLFW_FALSE = constant(glfwClass, "GLFW_FALSE");
			GLFW_TRUE = constant(glfwClass, "GLFW_TRUE");
			GLFW_RESIZABLE = constant(glfwClass, "GLFW_RESIZABLE");
			GLFW_VISIBLE = constant(glfwClass, "GLFW_VISIBLE");
			GLFW_FOCUSED = constant(glfwClass, "GLFW_FOCUSED");
			GLFW_DECORATED = constant(glfwClass, "GLFW_DECORATED");
			GLFW_PRESS = constant(glfwClass, "GLFW_PRESS");
			GLFW_RELEASE = constant(glfwClass, "GLFW_RELEASE");
			GLFW_REPEAT = constant(glfwClass, "GLFW_REPEAT");
			GLFW_MOD_SHIFT = constant(glfwClass, "GLFW_MOD_SHIFT");
			GLFW_MOD_CONTROL = constant(glfwClass, "GLFW_MOD_CONTROL");
			GLFW_KEY_LEFT_SHIFT = constant(glfwClass, "GLFW_KEY_LEFT_SHIFT");
			GLFW_KEY_RIGHT_SHIFT = constant(glfwClass, "GLFW_KEY_RIGHT_SHIFT");
			GLFW_KEY_LEFT_CONTROL = constant(glfwClass, "GLFW_KEY_LEFT_CONTROL");
			GLFW_KEY_RIGHT_CONTROL = constant(glfwClass, "GLFW_KEY_RIGHT_CONTROL");
			GLFW_KEY_LEFT_ALT = constant(glfwClass, "GLFW_KEY_LEFT_ALT");
			GLFW_KEY_RIGHT_ALT = constant(glfwClass, "GLFW_KEY_RIGHT_ALT");
			GLFW_MOUSE_BUTTON_LEFT = constant(glfwClass, "GLFW_MOUSE_BUTTON_LEFT");
			GLFW_MOUSE_BUTTON_RIGHT = constant(glfwClass, "GLFW_MOUSE_BUTTON_RIGHT");
			GLFW_MOUSE_BUTTON_MIDDLE = constant(glfwClass, "GLFW_MOUSE_BUTTON_MIDDLE");
			POINTER_SIZE = constant(pointerClass, "POINTER_SIZE");
			GL_COLOR_BUFFER_BIT = constant(gl11Class, "GL_COLOR_BUFFER_BIT");
			GL_DEPTH_BUFFER_BIT = constant(gl11Class, "GL_DEPTH_BUFFER_BIT");
			GL_ALL_ATTRIB_BITS = constant(gl11Class, "GL_ALL_ATTRIB_BITS");
			GL_CLIENT_ALL_ATTRIB_BITS = constant(gl11Class, "GL_CLIENT_ALL_ATTRIB_BITS");
			GL_TEXTURE_2D = constant(gl11Class, "GL_TEXTURE_2D");
			GL_TEXTURE_MIN_FILTER = constant(gl11Class, "GL_TEXTURE_MIN_FILTER");
			GL_TEXTURE_MAG_FILTER = constant(gl11Class, "GL_TEXTURE_MAG_FILTER");
			GL_TEXTURE_WRAP_S = constant(gl11Class, "GL_TEXTURE_WRAP_S");
			GL_TEXTURE_WRAP_T = constant(gl11Class, "GL_TEXTURE_WRAP_T");
			GL_NEAREST = constant(gl11Class, "GL_NEAREST");
			GL_LINEAR = constant(gl11Class, "GL_LINEAR");
			GL_CLAMP = constant(gl11Class, "GL_CLAMP");
			GL_CLAMP_TO_EDGE = optionalConstant(gl12Class, "GL_CLAMP_TO_EDGE", GL_CLAMP);
			GL_RGBA = constant(gl11Class, "GL_RGBA");
			GL_UNSIGNED_BYTE = constant(gl11Class, "GL_UNSIGNED_BYTE");
			GL_BLEND = constant(gl11Class, "GL_BLEND");
			GL_DEPTH_TEST = constant(gl11Class, "GL_DEPTH_TEST");
			GL_FOG = constant(gl11Class, "GL_FOG");
			GL_FOG_MODE = constant(gl11Class, "GL_FOG_MODE");
			GL_FOG_START = constant(gl11Class, "GL_FOG_START");
			GL_FOG_END = constant(gl11Class, "GL_FOG_END");
			GL_FOG_COLOR = constant(gl11Class, "GL_FOG_COLOR");
			GL_SCISSOR_TEST = constant(gl11Class, "GL_SCISSOR_TEST");
			GL_POLYGON_OFFSET_FILL = constant(gl11Class, "GL_POLYGON_OFFSET_FILL");
			GL_CULL_FACE = constant(gl11Class, "GL_CULL_FACE");
			GL_BACK = constant(gl11Class, "GL_BACK");
			GL_SRC_ALPHA = constant(gl11Class, "GL_SRC_ALPHA");
			GL_ONE_MINUS_SRC_ALPHA = constant(gl11Class, "GL_ONE_MINUS_SRC_ALPHA");
			GL_ALPHA_TEST = constant(gl11Class, "GL_ALPHA_TEST");
			GL_GREATER = constant(gl11Class, "GL_GREATER");
			GL_FRONT_AND_BACK = constant(gl11Class, "GL_FRONT_AND_BACK");
			GL_LINE = constant(gl11Class, "GL_LINE");
			GL_FILL = constant(gl11Class, "GL_FILL");
			GL_LINES = constant(gl11Class, "GL_LINES");
			GL_QUADS = constant(gl11Class, "GL_QUADS");
			GL_TRIANGLES = constant(gl11Class, "GL_TRIANGLES");
			GL_VERTEX_ARRAY = constant(gl11Class, "GL_VERTEX_ARRAY");
			GL_COLOR_ARRAY = constant(gl11Class, "GL_COLOR_ARRAY");
			GL_TEXTURE_COORD_ARRAY = constant(gl11Class, "GL_TEXTURE_COORD_ARRAY");
			GL_FLOAT = constant(gl11Class, "GL_FLOAT");
			GL_UNSIGNED_INT = constant(gl11Class, "GL_UNSIGNED_INT");
			GL_ARRAY_BUFFER = constant(gl15Class, "GL_ARRAY_BUFFER");
			GL_ELEMENT_ARRAY_BUFFER = constant(gl15Class, "GL_ELEMENT_ARRAY_BUFFER");
			GL_STREAM_DRAW = constant(gl15Class, "GL_STREAM_DRAW");
			GL_STATIC_DRAW = constant(gl15Class, "GL_STATIC_DRAW");
			GL_PROJECTION = constant(gl11Class, "GL_PROJECTION");
			GL_MODELVIEW = constant(gl11Class, "GL_MODELVIEW");
			GL_VERTEX_SHADER = constant(gl20Class, "GL_VERTEX_SHADER");
			GL_FRAGMENT_SHADER = constant(gl20Class, "GL_FRAGMENT_SHADER");
			GL_COMPILE_STATUS = constant(gl20Class, "GL_COMPILE_STATUS");
			GL_LINK_STATUS = constant(gl20Class, "GL_LINK_STATUS");
			GL_VENDOR = constant(gl11Class, "GL_VENDOR");
			GL_RENDERER = constant(gl11Class, "GL_RENDERER");
			GL_VERSION = constant(gl11Class, "GL_VERSION");
		}

		private boolean glfwInit() throws Exception {
			return ((Boolean) invoke(glfwInit)).booleanValue();
		}

		private void glfwTerminate() throws Exception {
			invoke(glfwTerminate);
		}

		private void glfwDefaultWindowHints() throws Exception {
			invoke(glfwDefaultWindowHints);
		}

		private void glfwWindowHint(int hint, int value) throws Exception {
			invoke(glfwWindowHint, hint, value);
		}

		private long glfwCreateWindow(int width, int height, String title, long monitor, long share) throws Exception {
			return ((Long) invoke(glfwCreateWindow, width, height, title, monitor, share)).longValue();
		}

		private void glfwMakeContextCurrent(long window) throws Exception {
			invoke(glfwMakeContextCurrent, window);
		}

		private void glfwSwapInterval(int interval) throws Exception {
			invoke(glfwSwapInterval, interval);
		}

		private void glfwShowWindow(long window) throws Exception {
			invoke(glfwShowWindow, window);
		}

		private void glfwHideWindow(long window) throws Exception {
			invoke(glfwHideWindow, window);
		}

		private boolean glfwWindowShouldClose(long window) throws Exception {
			return ((Boolean) invoke(glfwWindowShouldClose, window)).booleanValue();
		}

		private void glfwPollEvents() throws Exception {
			invoke(glfwPollEvents);
		}

		private void glfwSwapBuffers(long window) throws Exception {
			invoke(glfwSwapBuffers, window);
		}

		private void glfwDestroyWindow(long window) throws Exception {
			invoke(glfwDestroyWindow, window);
		}

		private void glfwSetWindowSize(long window, int width, int height) throws Exception {
			invoke(glfwSetWindowSize, window, width, height);
		}

		private void glfwGetWindowPos(long window, int[] x, int[] y) throws Exception {
			invoke(glfwGetWindowPos, window, x, y);
		}

		private void glfwSetWindowPos(long window, int x, int y) throws Exception {
			invoke(glfwSetWindowPos, window, x, y);
		}

		private void glfwSetWindowAttrib(long window, int attribute, int value) throws Exception {
			invoke(glfwSetWindowAttrib, window, attribute, value);
		}

		private MonitorMode getPrimaryMonitorMode() throws Exception {
			long monitor = ((Long) invoke(glfwGetPrimaryMonitor)).longValue();
			if (monitor == 0L) {
				return new MonitorMode(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
			}

			int[] x = new int[1];
			int[] y = new int[1];
			glfwGetMonitorPos(monitor, x, y);
			Object videoMode = invoke(glfwGetVideoMode, monitor);
			if (videoMode == null) {
				return new MonitorMode(x[0], y[0], INITIAL_WIDTH, INITIAL_HEIGHT);
			}

			return new MonitorMode(
				x[0],
				y[0],
				videoModeInt(videoMode, "width"),
				videoModeInt(videoMode, "height"));
		}

		private void glfwGetMonitorPos(long monitor, int[] x, int[] y) throws Exception {
			invoke(glfwGetMonitorPos, monitor, x, y);
		}

		private int videoModeInt(Object videoMode, String methodName) throws Exception {
			Method method = videoMode.getClass().getMethod(methodName);
			method.setAccessible(true);
			return ((Integer) method.invoke(videoMode)).intValue();
		}

		private void glfwSetScrollCallback(long window, Object callback) throws Exception {
			invoke(glfwSetScrollCallback, window, callback);
		}

		private Object createScrollCallback(ScrollHandler handler) throws Exception {
			ScrollCallbackInvocationHandler invocationHandler =
				new ScrollCallbackInvocationHandler(this, handler);
			return createCallback(glfwScrollCallbackInterface, glfwScrollCallbackCif, invocationHandler);
		}

		private void glfwSetKeyCallback(long window, Object callback) throws Exception {
			invoke(glfwSetKeyCallback, window, callback);
		}

		private Object createKeyCallback(GlfwKeyHandler handler) throws Exception {
			KeyCallbackInvocationHandler invocationHandler =
				new KeyCallbackInvocationHandler(this, handler);
			return createCallback(glfwKeyCallbackInterface, glfwKeyCallbackCif, invocationHandler);
		}

		private void glfwSetCharCallback(long window, Object callback) throws Exception {
			invoke(glfwSetCharCallback, window, callback);
		}

		private Object createCharCallback(CharHandler handler) throws Exception {
			CharCallbackInvocationHandler invocationHandler =
				new CharCallbackInvocationHandler(this, handler);
			return createCallback(glfwCharCallbackInterface, glfwCharCallbackCif, invocationHandler);
		}

		private void glfwSetWindowFocusCallback(long window, Object callback) throws Exception {
			invoke(glfwSetWindowFocusCallback, window, callback);
		}

		private Object createWindowFocusCallback(WindowFocusHandler handler) throws Exception {
			WindowFocusCallbackInvocationHandler invocationHandler =
				new WindowFocusCallbackInvocationHandler(this, handler);
			return createCallback(
				glfwWindowFocusCallbackInterface,
				glfwWindowFocusCallbackCif,
				invocationHandler);
		}

		private Object createCallback(
			Class<?> callbackInterface,
			Object callbackCif,
			CallbackInvocationHandler invocationHandler) throws Exception {
			Object callback = Proxy.newProxyInstance(
				callbackInterface.getClassLoader(),
				new Class[] {callbackInterface},
				invocationHandler);
			long callbackAddress = ((Long) invoke(callbackCreate, callbackCif, callback)).longValue();
			invocationHandler.setCallbackAddress(callbackAddress);
			return callback;
		}

		private void freeCallback(Object callback) throws Exception {
			if (callback == null) {
				return;
			}
			long callbackAddress = ((Long) callback.getClass().getMethod("address").invoke(callback)).longValue();
			if (callbackAddress != 0L) {
				invoke(callbackFree, callbackAddress);
			}
		}

		private long memGetAddress(long address) throws Exception {
			return ((Long) invoke(memGetAddress, address)).longValue();
		}

		private int memGetInt(long address) throws Exception {
			return ((Integer) invoke(memGetInt, address)).intValue();
		}

		private double memGetDouble(long address) throws Exception {
			return ((Double) invoke(memGetDouble, address)).doubleValue();
		}

		private void glfwGetWindowSize(long window, int[] width, int[] height) throws Exception {
			invoke(glfwGetWindowSize, window, width, height);
		}

		private void glfwGetFramebufferSize(long window, int[] width, int[] height) throws Exception {
			invoke(glfwGetFramebufferSize, window, width, height);
		}

		private int glfwGetWindowAttrib(long window, int attribute) throws Exception {
			return ((Integer) invoke(glfwGetWindowAttrib, window, attribute)).intValue();
		}

		private void glfwGetCursorPos(long window, double[] x, double[] y) throws Exception {
			invoke(glfwGetCursorPos, window, x, y);
		}

		private int glfwGetMouseButton(long window, int button) throws Exception {
			return ((Integer) invoke(glfwGetMouseButton, window, button)).intValue();
		}

		private int glfwGetKey(long window, int key) throws Exception {
			return ((Integer) invoke(glfwGetKey, window, key)).intValue();
		}

		private int glfwConstant(String name) throws Exception {
			return constant(glfwClass, name);
		}

		private void createCapabilities() throws Exception {
			invoke(createCapabilities);
		}

		private void glClearColor(float red, float green, float blue, float alpha) throws Exception {
			invoke(glClearColor, red, green, blue, alpha);
		}

		private void glClear(int mask) throws Exception {
			invoke(glClear, mask);
		}

		private void glViewport(int x, int y, int width, int height) throws Exception {
			invoke(glViewport, x, y, width, height);
		}

		private void glEnable(int capability) throws Exception {
			invoke(glEnable, capability);
		}

		private void glDisable(int capability) throws Exception {
			invoke(glDisable, capability);
		}

		private void glScissor(int x, int y, int width, int height) throws Exception {
			invoke(glScissor, x, y, width, height);
		}

		private void glPushAttrib(int mask) throws Exception {
			invoke(glPushAttrib, mask);
		}

		private void glPopAttrib() throws Exception {
			invoke(glPopAttrib);
		}

		private void glPushClientAttrib(int mask) throws Exception {
			invoke(glPushClientAttrib, mask);
		}

		private void glPopClientAttrib() throws Exception {
			invoke(glPopClientAttrib);
		}

		private void glPolygonOffset(float factor, float units) throws Exception {
			invoke(glPolygonOffset, factor, units);
		}

		private void glPolygonMode(int face, int mode) throws Exception {
			invoke(glPolygonMode, face, mode);
		}

		private void glCullFace(int mode) throws Exception {
			invoke(glCullFace, mode);
		}

		private int glGenTextures() throws Exception {
			return ((Integer) invoke(glGenTextures)).intValue();
		}

		private void glDeleteTextures(int texture) throws Exception {
			if (texture != 0) {
				invoke(glDeleteTextures, texture);
			}
		}

		private void glBindTexture(int target, int texture) throws Exception {
			invoke(glBindTexture, target, texture);
		}

		private void glTexParameteri(int target, int name, int value) throws Exception {
			invoke(glTexParameteri, target, name, value);
		}

		private void glTexImage2D(
			int target,
			int level,
			int internalFormat,
			int width,
			int height,
			int border,
			int format,
			int type,
			ByteBuffer pixels) throws Exception {
			invoke(glTexImage2D, target, level, internalFormat, width, height, border, format, type, pixels);
		}

		private void glTexSubImage2D(
			int target,
			int level,
			int xOffset,
			int yOffset,
			int width,
			int height,
			int format,
			int type,
			ByteBuffer pixels) throws Exception {
			invoke(glTexSubImage2D, target, level, xOffset, yOffset, width, height, format, type, pixels);
		}

		private void glReadPixels(
			int x,
			int y,
			int width,
			int height,
			int format,
			int type,
			ByteBuffer pixels) throws Exception {
			invoke(glReadPixels, x, y, width, height, format, type, pixels);
		}

		private void glBlendFunc(int sourceFactor, int destinationFactor) throws Exception {
			invoke(glBlendFunc, sourceFactor, destinationFactor);
		}

		private void glAlphaFunc(int function, float reference) throws Exception {
			invoke(glAlphaFunc, function, reference);
		}

		private void glDepthMask(boolean flag) throws Exception {
			invoke(glDepthMask, flag);
		}

		private void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) throws Exception {
			invoke(glColorMask, red, green, blue, alpha);
		}

		private void glColor4f(float red, float green, float blue, float alpha) throws Exception {
			invoke(glColor4f, red, green, blue, alpha);
		}

		private void glFogi(int name, int value) throws Exception {
			invoke(glFogi, name, value);
		}

		private void glFogf(int name, float value) throws Exception {
			invoke(glFogf, name, value);
		}

		private void glFogfv(int name, FloatBuffer values) throws Exception {
			invoke(glFogfv, name, values);
		}

		private String glGetString(int name) throws Exception {
			return (String) invoke(glGetString, name);
		}

		private void glMatrixMode(int mode) throws Exception {
			invoke(glMatrixMode, mode);
		}

		private void glLoadIdentity() throws Exception {
			invoke(glLoadIdentity);
		}

		private void glLoadMatrixf(FloatBuffer matrix) throws Exception {
			invoke(glLoadMatrixf, matrix);
		}

		private void glOrtho(double left, double right, double bottom, double top, double near, double far)
			throws Exception {
			invoke(glOrtho, left, right, bottom, top, near, far);
		}

		private int glGenBuffers() throws Exception {
			return ((Integer) invoke(glGenBuffers)).intValue();
		}

		private void glDeleteBuffers(int buffer) throws Exception {
			if (buffer != 0) {
				invoke(glDeleteBuffers, buffer);
			}
		}

		private void glBindBuffer(int target, int buffer) throws Exception {
			invoke(glBindBuffer, target, buffer);
		}

		private void glBufferData(int target, FloatBuffer data, int usage) throws Exception {
			invoke(glBufferDataFloat, target, data, usage);
		}

		private void glBufferData(int target, IntBuffer data, int usage) throws Exception {
			invoke(glBufferDataInt, target, data, usage);
		}

		private void glEnableClientState(int array) throws Exception {
			invoke(glEnableClientState, array);
		}

		private void glDisableClientState(int array) throws Exception {
			invoke(glDisableClientState, array);
		}

		private void glVertexPointer(int size, int type, int stride, long pointer) throws Exception {
			invoke(glVertexPointer, size, type, stride, pointer);
		}

		private void glColorPointer(int size, int type, int stride, long pointer) throws Exception {
			invoke(glColorPointer, size, type, stride, pointer);
		}

		private void glTexCoordPointer(int size, int type, int stride, long pointer) throws Exception {
			invoke(glTexCoordPointer, size, type, stride, pointer);
		}

		private void glDrawElements(int mode, int count, int type, long indices) throws Exception {
			invoke(glDrawElements, mode, count, type, indices);
		}

		private void glBegin(int mode) throws Exception {
			invoke(glBegin, mode);
		}

		private void glEnd() throws Exception {
			invoke(glEnd);
		}

		private void glTexCoord2f(float s, float t) throws Exception {
			invoke(glTexCoord2f, s, t);
		}

		private void glVertex3f(float x, float y, float z) throws Exception {
			invoke(glVertex3f, x, y, z);
		}

		private void glLineWidth(float width) throws Exception {
			invoke(glLineWidth, width);
		}

		private int glCreateShader(int type) throws Exception {
			return ((Integer) invoke(glCreateShader, type)).intValue();
		}

		private void glShaderSource(int shader, CharSequence source) throws Exception {
			invoke(glShaderSource, shader, source);
		}

		private void glCompileShader(int shader) throws Exception {
			invoke(glCompileShader, shader);
		}

		private int glGetShaderi(int shader, int pname) throws Exception {
			return ((Integer) invoke(glGetShaderi, shader, pname)).intValue();
		}

		private String glGetShaderInfoLog(int shader) throws Exception {
			return (String) invoke(glGetShaderInfoLog, shader);
		}

		private void glDeleteShader(int shader) throws Exception {
			if (shader != 0) {
				invoke(glDeleteShader, shader);
			}
		}

		private int glCreateProgram() throws Exception {
			return ((Integer) invoke(glCreateProgram)).intValue();
		}

		private void glAttachShader(int program, int shader) throws Exception {
			invoke(glAttachShader, program, shader);
		}

		private void glBindAttribLocation(int program, int index, CharSequence name) throws Exception {
			invoke(glBindAttribLocation, program, index, name);
		}

		private void glLinkProgram(int program) throws Exception {
			invoke(glLinkProgram, program);
		}

		private int glGetProgrami(int program, int pname) throws Exception {
			return ((Integer) invoke(glGetProgrami, program, pname)).intValue();
		}

		private String glGetProgramInfoLog(int program) throws Exception {
			return (String) invoke(glGetProgramInfoLog, program);
		}

		private void glUseProgram(int program) throws Exception {
			invoke(glUseProgram, program);
		}

		private int glGetUniformLocation(int program, CharSequence name) throws Exception {
			return ((Integer) invoke(glGetUniformLocation, program, name)).intValue();
		}

		private void glUniform1i(int location, int value) throws Exception {
			invoke(glUniform1i, location, value);
		}

		private void glUniform1f(int location, float value) throws Exception {
			invoke(glUniform1f, location, value);
		}

		private void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) throws Exception {
			invoke(glUniformMatrix4fv, location, transpose, value);
		}

		private void glEnableVertexAttribArray(int index) throws Exception {
			invoke(glEnableVertexAttribArray, index);
		}

		private void glDisableVertexAttribArray(int index) throws Exception {
			invoke(glDisableVertexAttribArray, index);
		}

		private void glVertexAttribPointer(
			int index,
			int size,
			int type,
			boolean normalized,
			int stride,
			long pointer) throws Exception {
			invoke(glVertexAttribPointer, index, size, type, normalized, stride, pointer);
		}

		private void glDeleteProgram(int program) throws Exception {
			if (program != 0) {
				invoke(glDeleteProgram, program);
			}
		}

		private abstract static class CallbackInvocationHandler implements java.lang.reflect.InvocationHandler {
			private final LwjglBindings gl;
			private final Object callbackCif;
			private long callbackAddress;

			private CallbackInvocationHandler(LwjglBindings gl, Object callbackCif) {
				this.gl = gl;
				this.callbackCif = callbackCif;
			}

			private void setCallbackAddress(long callbackAddress) {
				this.callbackAddress = callbackAddress;
			}

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String methodName = method.getName();
				if ("address".equals(methodName)) {
					return Long.valueOf(callbackAddress);
				}
				if ("getCallInterface".equals(methodName)) {
					return callbackCif;
				}
				if ("callback".equals(methodName) && args != null && args.length == 2) {
					dispatchNativeCallback(((Long) args[1]).longValue());
					return null;
				}
				if ("invoke".equals(methodName)) {
					return dispatchDirectCallback(args);
				}
				if ("hashCode".equals(methodName)) {
					return Integer.valueOf(System.identityHashCode(proxy));
				}
				if ("equals".equals(methodName)) {
					return Boolean.valueOf(proxy == args[0]);
				}
				if ("toString".equals(methodName)) {
					return description();
				}
				return null;
			}

			protected abstract Object dispatchDirectCallback(Object[] args) throws Exception;

			protected abstract void dispatchNativeCallback(long argsAddress) throws Exception;

			protected abstract String description();
		}

		private static final class ScrollCallbackInvocationHandler extends CallbackInvocationHandler {
			private final LwjglBindings gl;
			private final ScrollHandler handler;

			private ScrollCallbackInvocationHandler(LwjglBindings gl, ScrollHandler handler) {
				super(gl, gl.glfwScrollCallbackCif);
				this.gl = gl;
				this.handler = handler;
			}

			@Override
			protected Object dispatchDirectCallback(Object[] args) {
				if (args != null && args.length == 3) {
					handler.handle(
						((Long) args[0]).longValue(),
						((Double) args[1]).doubleValue(),
						((Double) args[2]).doubleValue());
				}
				return null;
			}

			@Override
			protected void dispatchNativeCallback(long argsAddress) throws Exception {
				long window = gl.memGetAddress(gl.memGetAddress(argsAddress));
				double xOffset = gl.memGetDouble(gl.memGetAddress(argsAddress + gl.POINTER_SIZE));
				double yOffset = gl.memGetDouble(gl.memGetAddress(argsAddress + 2L * gl.POINTER_SIZE));
				handler.handle(window, xOffset, yOffset);
			}

			@Override
			protected String description() {
				return "Spoiled Milk GLFW scroll callback";
			}
		}

		private static final class KeyCallbackInvocationHandler extends CallbackInvocationHandler {
			private final LwjglBindings gl;
			private final GlfwKeyHandler handler;

			private KeyCallbackInvocationHandler(LwjglBindings gl, GlfwKeyHandler handler) {
				super(gl, gl.glfwKeyCallbackCif);
				this.gl = gl;
				this.handler = handler;
			}

			@Override
			protected Object dispatchDirectCallback(Object[] args) {
				if (args != null && args.length == 5) {
					handler.handle(
						((Long) args[0]).longValue(),
						((Integer) args[1]).intValue(),
						((Integer) args[2]).intValue(),
						((Integer) args[3]).intValue(),
						((Integer) args[4]).intValue());
				}
				return null;
			}

			@Override
			protected void dispatchNativeCallback(long argsAddress) throws Exception {
				handler.handle(
					gl.memGetAddress(gl.memGetAddress(argsAddress)),
					gl.memGetInt(gl.memGetAddress(argsAddress + gl.POINTER_SIZE)),
					gl.memGetInt(gl.memGetAddress(argsAddress + 2L * gl.POINTER_SIZE)),
					gl.memGetInt(gl.memGetAddress(argsAddress + 3L * gl.POINTER_SIZE)),
					gl.memGetInt(gl.memGetAddress(argsAddress + 4L * gl.POINTER_SIZE)));
			}

			@Override
			protected String description() {
				return "Spoiled Milk GLFW key callback";
			}
		}

		private static final class CharCallbackInvocationHandler extends CallbackInvocationHandler {
			private final LwjglBindings gl;
			private final CharHandler handler;

			private CharCallbackInvocationHandler(LwjglBindings gl, CharHandler handler) {
				super(gl, gl.glfwCharCallbackCif);
				this.gl = gl;
				this.handler = handler;
			}

			@Override
			protected Object dispatchDirectCallback(Object[] args) {
				if (args != null && args.length == 2) {
					handler.handle(((Long) args[0]).longValue(), ((Integer) args[1]).intValue());
				}
				return null;
			}

			@Override
			protected void dispatchNativeCallback(long argsAddress) throws Exception {
				handler.handle(
					gl.memGetAddress(gl.memGetAddress(argsAddress)),
					gl.memGetInt(gl.memGetAddress(argsAddress + gl.POINTER_SIZE)));
			}

			@Override
			protected String description() {
				return "Spoiled Milk GLFW char callback";
			}
		}

		private static final class WindowFocusCallbackInvocationHandler extends CallbackInvocationHandler {
			private final LwjglBindings gl;
			private final WindowFocusHandler handler;

			private WindowFocusCallbackInvocationHandler(LwjglBindings gl, WindowFocusHandler handler) {
				super(gl, gl.glfwWindowFocusCallbackCif);
				this.gl = gl;
				this.handler = handler;
			}

			@Override
			protected Object dispatchDirectCallback(Object[] args) {
				if (args != null && args.length == 2) {
					handler.handle(((Long) args[0]).longValue(), ((Boolean) args[1]).booleanValue());
				}
				return null;
			}

			@Override
			protected void dispatchNativeCallback(long argsAddress) throws Exception {
				handler.handle(
					gl.memGetAddress(gl.memGetAddress(argsAddress)),
					gl.memGetInt(gl.memGetAddress(argsAddress + gl.POINTER_SIZE)) != 0);
			}

			@Override
			protected String description() {
				return "Spoiled Milk GLFW window focus callback";
			}
		}

		private static Method method(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
			Method method = type.getMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		}

		private static Method declaredMethod(Class<?> type, String name, Class<?>... parameterTypes)
			throws Exception {
			Method method = type.getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		}

		private static int constant(Class<?> type, String name) throws Exception {
			Field field = type.getField(name);
			field.setAccessible(true);
			return ((Integer) field.get(null)).intValue();
		}

		private static int optionalConstant(Class<?> type, String name, int fallback) throws Exception {
			return type == null ? fallback : constant(type, name);
		}

		private static Class<?> optionalClass(String name) {
			try {
				return Class.forName(name);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		private static Object fieldValue(Class<?> type, String name) throws Exception {
			Field field = type.getField(name);
			field.setAccessible(true);
			return field.get(null);
		}

		private static Object invoke(Method method, Object... arguments) throws Exception {
			try {
				return method.invoke(null, arguments);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof Exception) {
					throw (Exception) cause;
				}
				if (cause instanceof Error) {
					throw (Error) cause;
				}
				throw new RuntimeException(cause);
			}
		}
	}
}
