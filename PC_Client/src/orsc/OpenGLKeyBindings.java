package orsc;

import java.awt.event.KeyEvent;

/*
 * RENDERER-V2 OWNER: GLFW key-code to AWT key-event mapping for the OpenGL
 * presenter input bridge.
 */
final class OpenGLKeyBindings {
	private OpenGLKeyBindings() {
	}

	static KeyBinding[] create(LwjglBindings gl) throws Exception {
		return new KeyBinding[] {
			key(gl, "GLFW_KEY_LEFT_SHIFT", KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_RIGHT_SHIFT", KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_LEFT_CONTROL", KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_RIGHT_CONTROL", KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_LEFT_ALT", KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_RIGHT_ALT", KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_LEFT", KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_RIGHT", KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_UP", KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_DOWN", KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_HOME", KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_PAGE_UP", KeyEvent.VK_PAGE_UP, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_PAGE_DOWN", KeyEvent.VK_PAGE_DOWN, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_ENTER", KeyEvent.VK_ENTER, '\n', '\n'),
			key(gl, "GLFW_KEY_BACKSPACE", KeyEvent.VK_BACK_SPACE, '\b', '\b'),
			key(gl, "GLFW_KEY_ESCAPE", KeyEvent.VK_ESCAPE, (char) 27, (char) 27),
			key(gl, "GLFW_KEY_TAB", KeyEvent.VK_TAB, '\t', '\t'),
			key(gl, "GLFW_KEY_SPACE", KeyEvent.VK_SPACE, ' ', ' '),
			key(gl, "GLFW_KEY_F1", KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F2", KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F3", KeyEvent.VK_F3, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F4", KeyEvent.VK_F4, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F5", KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F6", KeyEvent.VK_F6, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F7", KeyEvent.VK_F7, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F8", KeyEvent.VK_F8, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F9", KeyEvent.VK_F9, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F10", KeyEvent.VK_F10, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F11", KeyEvent.VK_F11, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_F12", KeyEvent.VK_F12, KeyEvent.CHAR_UNDEFINED, KeyEvent.CHAR_UNDEFINED),
			key(gl, "GLFW_KEY_0", KeyEvent.VK_0, '0', ')'),
			key(gl, "GLFW_KEY_1", KeyEvent.VK_1, '1', '!'),
			key(gl, "GLFW_KEY_2", KeyEvent.VK_2, '2', '@'),
			key(gl, "GLFW_KEY_3", KeyEvent.VK_3, '3', '#'),
			key(gl, "GLFW_KEY_4", KeyEvent.VK_4, '4', '$'),
			key(gl, "GLFW_KEY_5", KeyEvent.VK_5, '5', '%'),
			key(gl, "GLFW_KEY_6", KeyEvent.VK_6, '6', '^'),
			key(gl, "GLFW_KEY_7", KeyEvent.VK_7, '7', '&'),
			key(gl, "GLFW_KEY_8", KeyEvent.VK_8, '8', '*'),
			key(gl, "GLFW_KEY_9", KeyEvent.VK_9, '9', '('),
			key(gl, "GLFW_KEY_A", KeyEvent.VK_A, 'a', 'A'),
			key(gl, "GLFW_KEY_B", KeyEvent.VK_B, 'b', 'B'),
			key(gl, "GLFW_KEY_C", KeyEvent.VK_C, 'c', 'C'),
			key(gl, "GLFW_KEY_D", KeyEvent.VK_D, 'd', 'D'),
			key(gl, "GLFW_KEY_E", KeyEvent.VK_E, 'e', 'E'),
			key(gl, "GLFW_KEY_F", KeyEvent.VK_F, 'f', 'F'),
			key(gl, "GLFW_KEY_G", KeyEvent.VK_G, 'g', 'G'),
			key(gl, "GLFW_KEY_H", KeyEvent.VK_H, 'h', 'H'),
			key(gl, "GLFW_KEY_I", KeyEvent.VK_I, 'i', 'I'),
			key(gl, "GLFW_KEY_J", KeyEvent.VK_J, 'j', 'J'),
			key(gl, "GLFW_KEY_K", KeyEvent.VK_K, 'k', 'K'),
			key(gl, "GLFW_KEY_L", KeyEvent.VK_L, 'l', 'L'),
			key(gl, "GLFW_KEY_M", KeyEvent.VK_M, 'm', 'M'),
			key(gl, "GLFW_KEY_N", KeyEvent.VK_N, 'n', 'N'),
			key(gl, "GLFW_KEY_O", KeyEvent.VK_O, 'o', 'O'),
			key(gl, "GLFW_KEY_P", KeyEvent.VK_P, 'p', 'P'),
			key(gl, "GLFW_KEY_Q", KeyEvent.VK_Q, 'q', 'Q'),
			key(gl, "GLFW_KEY_R", KeyEvent.VK_R, 'r', 'R'),
			key(gl, "GLFW_KEY_S", KeyEvent.VK_S, 's', 'S'),
			key(gl, "GLFW_KEY_T", KeyEvent.VK_T, 't', 'T'),
			key(gl, "GLFW_KEY_U", KeyEvent.VK_U, 'u', 'U'),
			key(gl, "GLFW_KEY_V", KeyEvent.VK_V, 'v', 'V'),
			key(gl, "GLFW_KEY_W", KeyEvent.VK_W, 'w', 'W'),
			key(gl, "GLFW_KEY_X", KeyEvent.VK_X, 'x', 'X'),
			key(gl, "GLFW_KEY_Y", KeyEvent.VK_Y, 'y', 'Y'),
			key(gl, "GLFW_KEY_Z", KeyEvent.VK_Z, 'z', 'Z'),
			key(gl, "GLFW_KEY_MINUS", KeyEvent.VK_MINUS, '-', '_'),
			key(gl, "GLFW_KEY_EQUAL", KeyEvent.VK_EQUALS, '=', '+'),
			key(gl, "GLFW_KEY_LEFT_BRACKET", KeyEvent.VK_OPEN_BRACKET, '[', '{'),
			key(gl, "GLFW_KEY_RIGHT_BRACKET", KeyEvent.VK_CLOSE_BRACKET, ']', '}'),
			key(gl, "GLFW_KEY_BACKSLASH", KeyEvent.VK_BACK_SLASH, '\\', '|'),
			key(gl, "GLFW_KEY_SEMICOLON", KeyEvent.VK_SEMICOLON, ';', ':'),
			key(gl, "GLFW_KEY_APOSTROPHE", KeyEvent.VK_QUOTE, '\'', '"'),
			key(gl, "GLFW_KEY_COMMA", KeyEvent.VK_COMMA, ',', '<'),
			key(gl, "GLFW_KEY_PERIOD", KeyEvent.VK_PERIOD, '.', '>'),
			key(gl, "GLFW_KEY_SLASH", KeyEvent.VK_SLASH, '/', '?'),
			key(gl, "GLFW_KEY_GRAVE_ACCENT", KeyEvent.VK_BACK_QUOTE, '`', '~')
		};
	}

	private static KeyBinding key(
		LwjglBindings gl,
		String glfwConstantName,
		int awtKeyCode,
		char normalChar,
		char shiftedChar) throws Exception {
		return new KeyBinding(gl.glfwConstant(glfwConstantName), awtKeyCode, normalChar, shiftedChar);
	}
}
