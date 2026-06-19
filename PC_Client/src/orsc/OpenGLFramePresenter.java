package orsc;

import com.openrsc.client.model.Sprite;
import orsc.graphics.Renderer2DFrame;
import orsc.graphics.Renderer2DSettings;
import orsc.graphics.RendererSpriteTransform;
import orsc.graphics.RendererTextureData;
import orsc.graphics.three.Renderer3DFrame;
import orsc.graphics.three.Renderer3DMeshFrame;
import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DTextureData;

import java.awt.Component;
import java.awt.EventQueue;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

final class OpenGLFramePresenter implements AutoCloseable {
	private static final int INITIAL_WIDTH = 512;
	private static final int INITIAL_HEIGHT = 346;
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
	private OpenGLWorldMeshRenderer worldMeshRenderer;
	private int windowWidth;
	private int windowHeight;
	private int currentSourceWidth = INITIAL_WIDTH;
	private int currentSourceHeight = INITIAL_HEIGHT;
	private int currentTargetWidth = INITIAL_WIDTH;
	private int currentTargetHeight = INITIAL_HEIGHT;
	private Viewport currentDrawViewport = new Viewport(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
	private Viewport currentFramebufferViewport = new Viewport(0, 0, INITIAL_WIDTH, INITIAL_HEIGHT);
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
			renderer3DFrame);
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
					+ ".");
				if (WORLD_MESH_TEXTURED_VISIBLE) {
					log("OpenGL world textured diagnostic alpha: " + TEXTURED_DIAGNOSTIC_ALPHA);
				}
				if (WORLD_MESH_TEXTURED_STATIC_VISIBLE) {
					log("OpenGL static world texture sampling: "
						+ (WORLD_STATIC_TEXTURES ? "enabled" : "flat-material fallback")
						+ ".");
				}
				if (WORLD_REPLACEMENT_COMPOSITE) {
					log("OpenGL world replacement composite active.");
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
		spriteTextureCache = new OpenGLSpriteTextureCache(gl);
		glyphTextureCache = new OpenGLGlyphTextureCache(gl);
		if (Renderer2DSettings.isOpenGLSpriteOverlayEnabled() || Renderer2DSettings.canReplayUiOverOpenGLWorld()) {
			visibleSpriteTextureAtlas = OpenGLDynamicTextureAtlas.create(gl, 2048, 2048);
		}
		if (WORLD_MESH_ENABLED) {
			worldMeshRenderer = new OpenGLWorldMeshRenderer(gl);
		}
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
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

		long renderStart = RenderTelemetry.now();
		long uploadStart = RenderTelemetry.now();
		uploadTexture(frame);
		long uploadNanos = RenderTelemetry.elapsedSince(uploadStart);
		boolean worldReplacementComposite = shouldUseOpenGLWorldReplacementComposite(frame);

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
		runOpenGLPass(() -> drawWorldMesh(frame));
		if (!worldReplacementComposite) {
			runOpenGLPass(() -> drawWorldSprites(frame));
		}
		runOpenGLPass(() -> drawSpriteOverlay(frame));
		gl.glfwSwapBuffers(window);
		gl.glfwPollEvents();
		processInput();

		RenderTelemetry.recordOpenGLFrame(uploadNanos, RenderTelemetry.elapsedSince(renderStart));
	}

	private boolean shouldUseOpenGLWorldReplacementComposite(Frame frame) {
		return WORLD_REPLACEMENT_COMPOSITE
			&& Renderer2DSettings.canReplayUiOverOpenGLWorld()
			&& frame.renderer3DFrame != null
			&& frame.renderer3DFrame.getMeshFrame() != null;
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

		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex2f(0.0f, 0.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex2f(1.0f, 0.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex2f(1.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex2f(0.0f, 1.0f);
		gl.glEnd();
	}

	private void drawWorldMesh(Frame frame) throws Exception {
		Renderer3DMeshFrame renderer3DMeshFrame =
			frame.renderer3DFrame == null ? null : frame.renderer3DFrame.getMeshFrame();
		if (!WORLD_MESH_ENABLED || worldMeshRenderer == null || renderer3DMeshFrame == null) {
			return;
		}

		if (WORLD_MESH_VISIBLE || WORLD_MESH_TEXTURED_VISIBLE) {
			useSourceDepthProjection(frame.sourceWidth, frame.sourceHeight);
			worldMeshRenderer.uploadAndMaybeDraw(
				renderer3DMeshFrame,
				WORLD_MESH_VISIBLE,
				WORLD_MESH_TEXTURED_VISIBLE);
			useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		} else {
			worldMeshRenderer.uploadAndMaybeDraw(renderer3DMeshFrame, false, false);
		}
	}

	private void drawWorldSprites(Frame frame) throws Exception {
		if (!WORLD_SPRITES_VISIBLE
			|| spriteTextureCache == null
			|| frame.renderer3DFrame == null
			|| frame.renderer2DFrame == null
			|| (!WORLD_MESH_VISIBLE && !WORLD_MESH_TEXTURED_VISIBLE)) {
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
		if (command.getPhase() != Renderer2DFrame.Phase.SCENE) {
			return Integer.MAX_VALUE;
		}

		if (command.getLegacySpriteId() != anchor.getSpriteId()) {
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
		float z = 0.0f;

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		gl.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(leftU, v0);
		gl.glVertex3f(topX0, y0, z);
		gl.glTexCoord2f(rightU, v0);
		gl.glVertex3f(topX1, y0, z);
		gl.glTexCoord2f(rightU, v1);
		gl.glVertex3f(bottomX1, y1, z);
		gl.glTexCoord2f(leftU, v1);
		gl.glVertex3f(bottomX0, y1, z);
		gl.glEnd();
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
			if (command.getPhase() != Renderer2DFrame.Phase.SCENE || isOpenGLCompositeDirectSpriteCommand(command)) {
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
		// Scene restoration has already been issued. Reuse its proven atlas from the
		// beginning so overlay uploads cannot fall back when a complex scene fills it.
		if (visibleSpriteTextureAtlas != null) {
			visibleSpriteTextureAtlas.beginFrame();
		}

		int spriteIndex = 0;
		int textIndex = 0;
		int primitiveIndex = 0;
		int rotatedSpriteIndex = 0;
		int circleIndex = 0;
		while (true) {
			while (spriteIndex < commands.length
				&& !isOpenGLCompositeDirectSpriteCommand(commands[spriteIndex])) {
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
			gl.glBegin(gl.GL_QUADS);
			gl.glVertex2f(x0, y0);
			gl.glVertex2f(x1, y0);
			gl.glVertex2f(x1, y1);
			gl.glVertex2f(x0, y1);
			gl.glEnd();
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
			gl.glBegin(gl.GL_QUADS);
			for (int py = command.getY() - radius; py <= command.getY() + radius; py++) {
				int dy = py - command.getY();
				int horizHalf = (int) Math.sqrt((double) (radiusSquared - dy * dy));
				float x0 = command.getX() - horizHalf;
				float x1 = command.getX() + horizHalf + 1.0f;
				float y0 = py;
				float y1 = py + 1.0f;
				gl.glVertex2f(x0, y0);
				gl.glVertex2f(x1, y0);
				gl.glVertex2f(x1, y1);
				gl.glVertex2f(x0, y1);
			}
			gl.glEnd();
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
			gl.glBegin(gl.GL_QUADS);
			gl.glTexCoord2f(region.getU0(), region.getV0());
			gl.glVertex2f(command.getX0(), command.getY0());
			gl.glTexCoord2f(region.getU1(), region.getV0());
			gl.glVertex2f(command.getX1(), command.getY1());
			gl.glTexCoord2f(region.getU1(), region.getV1());
			gl.glVertex2f(command.getX2(), command.getY2());
			gl.glTexCoord2f(region.getU0(), region.getV1());
			gl.glVertex2f(command.getX3(), command.getY3());
			gl.glEnd();
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
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(leftU, v0);
		gl.glVertex2f(topX0, y0);
		gl.glTexCoord2f(rightU, v0);
		gl.glVertex2f(topX1, y0);
		gl.glTexCoord2f(rightU, v1);
		gl.glVertex2f(bottomX1, y1);
		gl.glTexCoord2f(leftU, v1);
		gl.glVertex2f(bottomX0, y1);
		gl.glEnd();
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
			gl.glBegin(gl.GL_QUADS);
			gl.glTexCoord2f(region.getU0(), region.getV0());
			gl.glVertex2f(x0, y0);
			gl.glTexCoord2f(region.getU1(), region.getV0());
			gl.glVertex2f(x1, y0);
			gl.glTexCoord2f(region.getU1(), region.getV1());
			gl.glVertex2f(x1, y1);
			gl.glTexCoord2f(region.getU0(), region.getV1());
			gl.glVertex2f(x0, y1);
			gl.glEnd();
		}
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
		if (textureData == null) {
			return 0;
		}

		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);
		if (region == null) {
			return VISIBLE_SPRITE_ATLAS_FULL;
		}
		drawSpriteRegion(command, region, region.getU0(), region.getU1(), region.getV0(), region.getV1());
		return textureData.visiblePixelCount;
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

		OpenGLTextureRegion region = visibleSpriteTextureAtlas.upload(textureData);
		if (region == null) {
			visibleSpriteTextureAtlas.beginFrame();
			region = visibleSpriteTextureAtlas.upload(textureData);
			if (region == null) {
				drawSpriteCommand(command);
				return;
			}
		}
		drawSpriteRegion(command, region, region.getU0(), region.getU1(), region.getV0(), region.getV1(), 1.0f);
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
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(leftU, v0);
		gl.glVertex2f(topX0, y0);
		gl.glTexCoord2f(rightU, v0);
		gl.glVertex2f(topX1, y0);
		gl.glTexCoord2f(rightU, v1);
		gl.glVertex2f(bottomX1, y1);
		gl.glTexCoord2f(leftU, v1);
		gl.glVertex2f(bottomX0, y1);
		gl.glEnd();
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

	private void useSourceDepthProjection(int sourceWidth, int sourceHeight) throws Exception {
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0, sourceWidth, sourceHeight, 0.0, -32768.0, 32768.0);
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();
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

		if (action == gl.GLFW_REPEAT) {
			return;
		}

		boolean pressed = action == gl.GLFW_PRESS;
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

		if (pressed == keyDown[keyIndex]) {
			return;
		}

		KeyBinding binding = keyBindings[keyIndex];
		keyDown[keyIndex] = pressed;
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
				if (worldMeshRenderer != null) {
					worldMeshRenderer.close();
					worldMeshRenderer = null;
				}
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

	private static final class OpenGLWorldMeshRenderer implements AutoCloseable {
		private static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;
		private static final int FLAT_COLOR_BATCH_TEXTURE_ID = -1;
		private static final int UPLOAD_FLOATS_PER_VERTEX = 12;
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

		private final LwjglBindings gl;
		private final OpenGLWorldTextureCache textureCache;
		private final int vertexBufferId;
		private final int indexBufferId;
		private final int texturedIndexBufferId;
		private final List<WorldMeshTextureBatch> textureBatches = new ArrayList<>();
		private FloatBuffer vertexUploadBuffer;
		private IntBuffer indexUploadBuffer;
		private IntBuffer texturedIndexUploadBuffer;
		private int vertexUploadCapacity;
		private int indexUploadCapacity;
		private int texturedIndexUploadCapacity;
		private boolean closed;

		private OpenGLWorldMeshRenderer(LwjglBindings gl) throws Exception {
			this.gl = gl;
			this.textureCache = new OpenGLWorldTextureCache(gl);
			this.vertexBufferId = gl.glGenBuffers();
			this.indexBufferId = gl.glGenBuffers();
			this.texturedIndexBufferId = gl.glGenBuffers();
		}

		private void uploadAndMaybeDraw(
			Renderer3DMeshFrame meshFrame,
			boolean wireframeVisible,
			boolean texturedVisible) throws Exception {
			int vertexCount = meshFrame.getVertexCount();
			int indexCount = meshFrame.getIndexCount();
			if (vertexCount <= 0 || indexCount <= 0) {
				return;
			}

			WorldTextureUploadStats textureStats = textureCache.uploadReferencedTextures(meshFrame);
			ensureUploadBuffers(vertexCount, indexCount);
			copyMesh(meshFrame, vertexCount);
			copyFullIndices(meshFrame, indexCount);
			int texturedIndexCount = copyTexturedIndices(meshFrame, indexCount);

			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glBufferData(gl.GL_ARRAY_BUFFER, vertexUploadBuffer, gl.GL_STREAM_DRAW);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indexUploadBuffer, gl.GL_STREAM_DRAW);
			if (texturedIndexCount > 0) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexBufferId);
				gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexUploadBuffer, gl.GL_STREAM_DRAW);
			}
			RenderTelemetry.recordOpenGLWorldMeshFrame(vertexCount, indexCount, indexCount / 3);
			RenderTelemetry.recordOpenGLWorldTextureFrame(
				textureStats.referencedTextures,
				textureStats.cachedTextures,
				textureStats.uploadedTextures,
				textureStats.missingTextures,
				textureStats.atlases);
			if (!wireframeVisible && !texturedVisible) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
				gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
				return;
			}

			gl.glEnable(gl.GL_DEPTH_TEST);
			gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
			try {
				if (texturedVisible && texturedIndexCount > 0) {
					drawTexturedDiagnostic();
				}
				if (wireframeVisible) {
					drawWireframeDiagnostic(indexCount);
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
		}

		private void ensureUploadBuffers(int vertexCount, int indexCount) {
			int requiredVertexFloats = vertexCount * UPLOAD_FLOATS_PER_VERTEX;
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

			if (texturedIndexUploadBuffer == null || texturedIndexUploadCapacity < indexCount) {
				texturedIndexUploadCapacity = growCapacity(texturedIndexUploadCapacity, indexCount);
				texturedIndexUploadBuffer = ByteBuffer
					.allocateDirect(texturedIndexUploadCapacity * 4)
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

		private void copyMesh(Renderer3DMeshFrame meshFrame, int vertexCount) {
			float[] vertices = meshFrame.getVertices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			float brightness = RendererBrightnessSettings.getMode().multiplier;
			vertexUploadBuffer.clear();
			for (int vertex = 0; vertex < vertexCount; vertex++) {
				int sourceOffset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX;
				OpenGLTextureRegion textureRegion =
					textureRegionForVertex(meshFrame, triangleTextures, triangleModelKinds, vertex);
				vertexUploadBuffer.put(meshFrame.getCenterX() + vertices[sourceOffset + Renderer3DMeshFrame.SCREEN_X_OFFSET]);
				vertexUploadBuffer.put(meshFrame.getCenterY() + vertices[sourceOffset + Renderer3DMeshFrame.SCREEN_Y_OFFSET]);
				vertexUploadBuffer.put(-vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Z_OFFSET]);
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.RED_OFFSET], brightness));
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.GREEN_OFFSET], brightness));
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.BLUE_OFFSET], brightness));
				vertexUploadBuffer.put(atlasU(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_U_OFFSET]));
				vertexUploadBuffer.put(atlasV(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_V_OFFSET]));
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_RED_OFFSET], brightness));
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_GREEN_OFFSET], brightness));
				vertexUploadBuffer.put(brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_BLUE_OFFSET], brightness));
				vertexUploadBuffer.put(
					vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_ALPHA_OFFSET] * TEXTURED_DIAGNOSTIC_ALPHA);
			}
			vertexUploadBuffer.flip();
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

		private void copyFullIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
			int[] indices = meshFrame.getIndices();
			indexUploadBuffer.clear();
			indexUploadBuffer.put(indices, 0, indexCount);
			indexUploadBuffer.flip();
		}

		private int copyTexturedIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
			if (WORLD_MESH_TEXTURED_STATIC_VISIBLE) {
				return copyTexturedIndicesInLegacyOrder(meshFrame, indexCount);
			}

			int[] indices = meshFrame.getIndices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
			Map<Integer, WorldMeshTextureBatch> batchesByTexture = new LinkedHashMap<>();
			textureBatches.clear();

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
					continue;
				}
				OpenGLTextureRegion region = textureCache.getRegion(meshFrame, triangleTextures[triangle]);
				if (region == null) {
					continue;
				}

				WorldMeshTextureBatch batch = batchesByTexture.get(region.getTextureId());
				if (batch == null) {
					batch = new WorldMeshTextureBatch(region.getTextureId());
					batchesByTexture.put(region.getTextureId(), batch);
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
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
					continue;
				}
				OpenGLTextureRegion region = textureCache.getRegion(meshFrame, triangleTextures[triangle]);
				if (region == null) {
					continue;
				}

				int sourceIndex = triangle * 3;
				WorldMeshTextureBatch batch = batchesByTexture.get(region.getTextureId());
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex]);
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 1]);
				texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 2]);
			}

			texturedIndexUploadBuffer.position(0);
			return nextIndex;
		}

		private int copyTexturedIndicesInLegacyOrder(Renderer3DMeshFrame meshFrame, int indexCount) {
			int[] indices = meshFrame.getIndices();
			int[] triangleTextures = meshFrame.getTriangleTextures();
			int[] triangleLegacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
			Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
			int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
			List<Integer> orderedTriangles = new ArrayList<>();
			textureBatches.clear();

			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle)) {
					continue;
				}
				if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
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
			WorldMeshTextureBatch currentBatch = null;
			int nextIndex = 0;
			for (Integer orderedTriangle : orderedTriangles) {
				int triangle = orderedTriangle.intValue();
				OpenGLTextureRegion region =
					textureRegionForTriangle(meshFrame, triangleTextures[triangle], triangleModelKinds[triangle]);
				int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
				if (currentBatch == null || currentBatch.textureId != batchTextureId) {
					currentBatch = new WorldMeshTextureBatch(batchTextureId);
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

			texturedIndexUploadBuffer.flip();
			return nextIndex;
		}

		private int legacyDrawOrderOrEnd(int[] triangleLegacyDrawOrders, int triangle) {
			if (triangle < 0 || triangle >= triangleLegacyDrawOrders.length || triangleLegacyDrawOrders[triangle] < 0) {
				return Integer.MAX_VALUE;
			}
			return triangleLegacyDrawOrders[triangle];
		}

		private boolean isTexturedDiagnosticTriangle(Renderer3DModelKind[] triangleModelKinds, int triangle) {
			if (triangle < 0 || triangle >= triangleModelKinds.length) {
				return false;
			}

			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
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
			int vertex) {
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
			Renderer3DModelKind modelKind) {
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

		private void drawTexturedDiagnostic() throws Exception {
			// Legacy draw order owns this color pass; depth rejection can make
			// terrain textures incorrectly win over later scenery/wall faces.
			gl.glDisable(gl.GL_DEPTH_TEST);
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
			for (WorldMeshTextureBatch batch : textureBatches) {
				if (batch.indexCount <= 0) {
					continue;
				}
				if (batch.isTextureBacked()) {
					drawTexturedBatch(batch);
				} else {
					drawFlatColorBatch(batch);
				}
			}
		}

		private void drawTexturedBatch(WorldMeshTextureBatch batch) throws Exception {
			gl.glEnable(gl.GL_TEXTURE_2D);
			if (TEXTURED_DIAGNOSTIC_ALPHA < 0.999f) {
				gl.glDisable(gl.GL_ALPHA_TEST);
				gl.glEnable(gl.GL_BLEND);
				gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				gl.glDisable(gl.GL_BLEND);
				gl.glEnable(gl.GL_ALPHA_TEST);
				gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			}
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
			gl.glBindTexture(gl.GL_TEXTURE_2D, batch.textureId);
			gl.glColor4f(1.0f, 1.0f, 1.0f, TEXTURED_DIAGNOSTIC_ALPHA);
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				batch.indexCount,
				gl.GL_UNSIGNED_INT,
				batch.startIndex * 4L);
		}

		private void drawFlatColorBatch(WorldMeshTextureBatch batch) throws Exception {
			gl.glDisable(gl.GL_TEXTURE_2D);
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glEnableClientState(gl.GL_COLOR_ARRAY);
			gl.glColorPointer(
				MATERIAL_COLOR_COMPONENT_COUNT,
				gl.GL_FLOAT,
				STRIDE_BYTES,
				MATERIAL_COLOR_OFFSET_BYTES);
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				batch.indexCount,
				gl.GL_UNSIGNED_INT,
				batch.startIndex * 4L);
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
			textureCache.close();
			gl.glDeleteBuffers(vertexBufferId);
			gl.glDeleteBuffers(indexBufferId);
			gl.glDeleteBuffers(texturedIndexBufferId);
		}
	}

	private static final class WorldMeshTextureBatch {
		private final int textureId;
		private int startIndex;
		private int indexCount;
		private int writeIndex;

		private WorldMeshTextureBatch(int textureId) {
			this.textureId = textureId;
		}

		private boolean isTextureBacked() {
			return textureId >= 0;
		}
	}

	private static final class OpenGLWorldTextureCache implements AutoCloseable {
		private static final int DEFAULT_ATLAS_SIZE = 2048;

		private final LwjglBindings gl;
		private final Map<Renderer3DTextureData, OpenGLTextureRegion> textureRegions = new HashMap<>();
		private final List<OpenGLTextureAtlas> atlases = new ArrayList<>();

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

		private OpenGLTextureRegion getRegion(Renderer3DMeshFrame meshFrame, int textureId) {
			if (!isTextureBacked(meshFrame, textureId)) {
				return null;
			}
			Renderer3DTextureData textureData = meshFrame.getTexture(textureId);
			return textureData == null ? null : textureRegions.get(textureData);
		}

		private boolean isTextureBacked(Renderer3DMeshFrame meshFrame, int textureId) {
			return textureId >= 0 && textureId < meshFrame.getTextureCount();
		}

		private boolean uploadIfNeeded(Renderer3DTextureData textureData) throws Exception {
			if (textureRegions.containsKey(textureData)) {
				return false;
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

			textureRegions.put(textureData, region);
			return true;
		}

		@Override
		public void close() throws Exception {
			for (OpenGLTextureAtlas atlas : atlases) {
				atlas.close();
			}
			atlases.clear();
			textureRegions.clear();
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

	private static final class Frame {
		private final int sourceWidth;
		private final int sourceHeight;
		private final int targetWidth;
		private final int targetHeight;
		private final boolean linearFiltering;
		private final int byteCount;
		private final Renderer2DFrame renderer2DFrame;
		private final Renderer3DFrame renderer3DFrame;
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
			this.frameBufferPool = frameBufferPool;
			this.frameBuffer = frameBuffer;
		}

		static Frame fromImage(
			BufferedImage image,
			float scalar,
			ScaledWindow.ScalingAlgorithm scalingAlgorithm,
			FrameBufferPool frameBufferPool,
			Renderer2DFrame renderer2DFrame,
			Renderer3DFrame renderer3DFrame) {
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
		private final Method glPolygonMode;
		private final Method glGenTextures;
		private final Method glDeleteTextures;
		private final Method glBindTexture;
		private final Method glTexParameteri;
		private final Method glTexImage2D;
		private final Method glTexSubImage2D;
		private final Method glBlendFunc;
		private final Method glAlphaFunc;
		private final Method glColor4f;
		private final Method glGetString;
		private final Method glBegin;
		private final Method glEnd;
		private final Method glTexCoord2f;
		private final Method glVertex2f;
		private final Method glVertex3f;
		private final Method glMatrixMode;
		private final Method glLoadIdentity;
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
		private final int GL_SCISSOR_TEST;
		private final int GL_SRC_ALPHA;
		private final int GL_ONE_MINUS_SRC_ALPHA;
		private final int GL_ALPHA_TEST;
		private final int GL_GREATER;
		private final int GL_FRONT_AND_BACK;
		private final int GL_LINE;
		private final int GL_FILL;
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
		private final int GL_PROJECTION;
		private final int GL_MODELVIEW;
		private final int GL_VENDOR;
		private final int GL_RENDERER;
		private final int GL_VERSION;

		static LwjglBindings load() throws Exception {
			Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
			Class<?> glClass = Class.forName("org.lwjgl.opengl.GL");
			Class<?> gl11Class = Class.forName("org.lwjgl.opengl.GL11");
			Class<?> gl12Class = optionalClass("org.lwjgl.opengl.GL12");
			Class<?> gl15Class = Class.forName("org.lwjgl.opengl.GL15");
			return new LwjglBindings(glfwClass, glClass, gl11Class, gl12Class, gl15Class);
		}

		private LwjglBindings(
			Class<?> glfwClass,
			Class<?> glClass,
			Class<?> gl11Class,
			Class<?> gl12Class,
			Class<?> gl15Class)
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
			glPolygonMode = method(gl11Class, "glPolygonMode", int.class, int.class);
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
			glBlendFunc = method(gl11Class, "glBlendFunc", int.class, int.class);
			glAlphaFunc = method(gl11Class, "glAlphaFunc", int.class, float.class);
			glColor4f = method(gl11Class, "glColor4f", float.class, float.class, float.class, float.class);
			glGetString = method(gl11Class, "glGetString", int.class);
			glBegin = method(gl11Class, "glBegin", int.class);
			glEnd = method(gl11Class, "glEnd");
			glTexCoord2f = method(gl11Class, "glTexCoord2f", float.class, float.class);
			glVertex2f = method(gl11Class, "glVertex2f", float.class, float.class);
			glVertex3f = method(gl11Class, "glVertex3f", float.class, float.class, float.class);
			glMatrixMode = method(gl11Class, "glMatrixMode", int.class);
			glLoadIdentity = method(gl11Class, "glLoadIdentity");
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
			GL_SCISSOR_TEST = constant(gl11Class, "GL_SCISSOR_TEST");
			GL_SRC_ALPHA = constant(gl11Class, "GL_SRC_ALPHA");
			GL_ONE_MINUS_SRC_ALPHA = constant(gl11Class, "GL_ONE_MINUS_SRC_ALPHA");
			GL_ALPHA_TEST = constant(gl11Class, "GL_ALPHA_TEST");
			GL_GREATER = constant(gl11Class, "GL_GREATER");
			GL_FRONT_AND_BACK = constant(gl11Class, "GL_FRONT_AND_BACK");
			GL_LINE = constant(gl11Class, "GL_LINE");
			GL_FILL = constant(gl11Class, "GL_FILL");
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
			GL_PROJECTION = constant(gl11Class, "GL_PROJECTION");
			GL_MODELVIEW = constant(gl11Class, "GL_MODELVIEW");
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

		private void glPolygonMode(int face, int mode) throws Exception {
			invoke(glPolygonMode, face, mode);
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

		private void glBlendFunc(int sourceFactor, int destinationFactor) throws Exception {
			invoke(glBlendFunc, sourceFactor, destinationFactor);
		}

		private void glAlphaFunc(int function, float reference) throws Exception {
			invoke(glAlphaFunc, function, reference);
		}

		private void glColor4f(float red, float green, float blue, float alpha) throws Exception {
			invoke(glColor4f, red, green, blue, alpha);
		}

		private String glGetString(int name) throws Exception {
			return (String) invoke(glGetString, name);
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

		private void glVertex2f(float x, float y) throws Exception {
			invoke(glVertex2f, x, y);
		}

		private void glVertex3f(float x, float y, float z) throws Exception {
			invoke(glVertex3f, x, y, z);
		}

		private void glMatrixMode(int mode) throws Exception {
			invoke(glMatrixMode, mode);
		}

		private void glLoadIdentity() throws Exception {
			invoke(glLoadIdentity);
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
