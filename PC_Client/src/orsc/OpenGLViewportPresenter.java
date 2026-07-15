package orsc;

/*
 * RENDERER-V2 OWNER: computes the OpenGL draw/framebuffer viewports and owns
 * the coordinate mapping derived from the current presentation layout.
 */
final class OpenGLViewportPresenter {
	private static final double INTEGER_SCALE_EPSILON = 0.01d;
	private static final float MAX_FRACTIONAL_SCALE_SMOOTHING_ALPHA = 0.85f;

	private final boolean primaryWindow;
	private int sourceWidth;
	private int sourceHeight;
	private Viewport drawViewport;
	private Viewport framebufferViewport;
	private float textSmoothingAlpha;

	OpenGLViewportPresenter(boolean primaryWindow, int initialWidth, int initialHeight) {
		this.primaryWindow = primaryWindow;
		sourceWidth = Math.max(1, initialWidth);
		sourceHeight = Math.max(1, initialHeight);
		drawViewport = new Viewport(0, 0, sourceWidth, sourceHeight);
		framebufferViewport = new Viewport(0, 0, sourceWidth, sourceHeight);
	}

	OpenGLPresentationSettings.ScaleMode currentScaleMode() {
		return primaryWindow
			? OpenGLPresentationSettings.ScaleMode.ASPECT_FIT
			: OpenGLPresentationSettings.getScaleMode();
	}

	void update(
		int framebufferWidth,
		int framebufferHeight,
		int windowWidth,
		int windowHeight,
		int nextSourceWidth,
		int nextSourceHeight) {
		sourceWidth = Math.max(1, nextSourceWidth);
		sourceHeight = Math.max(1, nextSourceHeight);
		OpenGLPresentationSettings.ScaleMode scaleMode = currentScaleMode();
		framebufferViewport = computeViewport(
			scaleMode,
			Math.max(1, framebufferWidth),
			Math.max(1, framebufferHeight),
			sourceWidth,
			sourceHeight);
		drawViewport = computeViewport(
			scaleMode,
			Math.max(1, windowWidth),
			Math.max(1, windowHeight),
			sourceWidth,
			sourceHeight);
		textSmoothingAlpha = computeTextSmoothingAlpha(
			framebufferViewport,
			sourceWidth,
			sourceHeight);
	}

	Viewport drawViewport() {
		return drawViewport;
	}

	Viewport framebufferViewport() {
		return framebufferViewport;
	}

	int sourceWidth() {
		return sourceWidth;
	}

	int sourceHeight() {
		return sourceHeight;
	}

	float textSmoothingAlpha() {
		return textSmoothingAlpha;
	}

	int mapMouseX(double cursorX) {
		int x = (int) Math.round(
			(cursorX - drawViewport.x) * sourceWidth / Math.max(1, drawViewport.width));
		return clamp(x, 0, sourceWidth - 1);
	}

	int mapMouseY(double cursorY) {
		int y = (int) Math.round(
			(cursorY - drawViewport.y) * sourceHeight / Math.max(1, drawViewport.height));
		return clamp(y, 0, sourceHeight - 1);
	}

	static Viewport computeViewport(
		OpenGLPresentationSettings.ScaleMode scaleMode,
		int surfaceWidth,
		int surfaceHeight,
		int sourceWidth,
		int sourceHeight) {
		switch (scaleMode) {
			case INTEGER_FIT:
				int scale = Math.min(surfaceWidth / sourceWidth, surfaceHeight / sourceHeight);
				if (scale >= 1) {
					return centeredViewport(
						surfaceWidth,
						surfaceHeight,
						sourceWidth * scale,
						sourceHeight * scale);
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
				return computeAspectViewport(
					surfaceWidth,
					surfaceHeight,
					sourceWidth / (double) sourceHeight);
		}
	}

	private static float computeTextSmoothingAlpha(
		Viewport viewport,
		int sourceWidth,
		int sourceHeight) {
		double scaleX = viewport.width / (double) Math.max(1, sourceWidth);
		double scaleY = viewport.height / (double) Math.max(1, sourceHeight);
		double scaleError = Math.max(integerScaleError(scaleX), integerScaleError(scaleY));
		if (scaleError <= INTEGER_SCALE_EPSILON) {
			return 0.0f;
		}
		double normalizedError = Math.min(1.0d, scaleError / 0.5d);
		return (float) (normalizedError * MAX_FRACTIONAL_SCALE_SMOOTHING_ALPHA);
	}

	private static double integerScaleError(double scale) {
		if (scale <= 0.0d) {
			return 1.0d;
		}
		double nearestIntegerScale = Math.max(1.0d, Math.rint(scale));
		return Math.min(1.0d, Math.abs(scale - nearestIntegerScale));
	}

	private static Viewport computeAspectViewport(
		int surfaceWidth,
		int surfaceHeight,
		double aspectRatio) {
		int width = Math.max(1, surfaceWidth);
		int height = Math.max(1, (int) Math.round(width / aspectRatio));
		if (height > surfaceHeight) {
			height = Math.max(1, surfaceHeight);
			width = Math.max(1, (int) Math.round(height * aspectRatio));
		}
		return centeredViewport(surfaceWidth, surfaceHeight, width, height);
	}

	private static Viewport centeredViewport(
		int surfaceWidth,
		int surfaceHeight,
		int width,
		int height) {
		int clampedWidth = Math.max(1, Math.min(surfaceWidth, width));
		int clampedHeight = Math.max(1, Math.min(surfaceHeight, height));
		int x = Math.max(0, (surfaceWidth - clampedWidth) / 2);
		int y = Math.max(0, (surfaceHeight - clampedHeight) / 2);
		return new Viewport(x, y, clampedWidth, clampedHeight);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	static final class Viewport {
		final int x;
		final int y;
		final int width;
		final int height;

		Viewport(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
}
