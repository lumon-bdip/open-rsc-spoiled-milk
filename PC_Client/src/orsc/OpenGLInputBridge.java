package orsc;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/*
 * RENDERER-V2 OWNER: GLFW input callback lifecycle and AWT event forwarding
 * for the OpenGL presenter.
 */
final class OpenGLInputBridge implements AutoCloseable {
	interface Delegate {
		int mapMouseX(double cursorX);

		int mapMouseY(double cursorY);

		void requestFrameCaptureBurst();

		void log(String message);
	}

	private static final int MOUSE_BUTTON_COUNT = 3;

	private final boolean enabled;
	private final LwjglBindings gl;
	private final long window;
	private final Delegate delegate;
	private final boolean frameCaptureHotkeyEnabled;
	private final int frameCaptureBurstFrames;
	private final boolean[] mouseButtonDown = new boolean[MOUSE_BUTTON_COUNT];

	private int lastMouseX = -1;
	private int lastMouseY = -1;
	private boolean inputFocused;
	private KeyBinding[] keyBindings = new KeyBinding[0];
	private boolean[] keyDown = new boolean[0];
	private boolean[] keySuppressUntilRelease = new boolean[0];
	private Object scrollCallback;
	private Object keyCallback;
	private Object charCallback;
	private Object windowFocusCallback;

	OpenGLInputBridge(
		boolean enabled,
		LwjglBindings gl,
		long window,
		Delegate delegate,
		boolean frameCaptureHotkeyEnabled,
		int frameCaptureBurstFrames) {
		this.enabled = enabled;
		this.gl = gl;
		this.window = window;
		this.delegate = delegate;
		this.frameCaptureHotkeyEnabled = frameCaptureHotkeyEnabled;
		this.frameCaptureBurstFrames = frameCaptureBurstFrames;
	}

	void initialize() throws Exception {
		if (!enabled) {
			return;
		}

		keyBindings = OpenGLKeyBindings.create(gl);
		keyDown = new boolean[keyBindings.length];
		keySuppressUntilRelease = new boolean[keyBindings.length];
	}

	void installCallbacks() throws Exception {
		if (!enabled) {
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

	void process() throws Exception {
		if (!enabled || OpenRSC.applet == null || window == 0L) {
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

		int x = delegate.mapMouseX(cursorX[0]);
		int y = delegate.mapMouseY(cursorY[0]);

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
		if (!enabled || callbackWindow != window) {
			return;
		}

		double[] cursorX = new double[1];
		double[] cursorY = new double[1];
		try {
			gl.glfwGetCursorPos(window, cursorX, cursorY);
		} catch (Exception e) {
			return;
		}

		int x = delegate.mapMouseX(cursorX[0]);
		int y = delegate.mapMouseY(cursorY[0]);
		int wheelRotation = yOffset > 0.0 ? -1 : 1;
		postMouseWheelEvent(x, y, wheelRotation, -yOffset);
	}

	private void handleKey(long callbackWindow, int glfwKey, int scanCode, int action, int mods) {
		if (!enabled || callbackWindow != window) {
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
			&& frameCaptureHotkeyEnabled
			&& binding.awtKeyCode == KeyEvent.VK_F9
			&& (mods & gl.GLFW_MOD_CONTROL) != 0) {
			delegate.requestFrameCaptureBurst();
			delegate.log("OpenGL frame capture burst requested; next "
				+ frameCaptureBurstFrames
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
		if (!enabled || callbackWindow != window || !inputFocused || !Character.isValidCodePoint(codepoint)) {
			return;
		}

		char[] chars = Character.toChars(codepoint);
		for (char keyChar : chars) {
			postTypedCharacter(keyChar);
		}
	}

	private void handleWindowFocus(long callbackWindow, boolean focused) {
		if (!enabled || callbackWindow != window) {
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

	void releaseInputState() {
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

	void suppressKeysUntilRelease() {
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

	@Override
	public void close() {
		releaseInputState();
		if (!enabled || gl == null || window == 0L) {
			return;
		}

		try {
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
		} catch (Throwable ignored) {
		}
	}
}
