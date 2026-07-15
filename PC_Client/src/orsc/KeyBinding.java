package orsc;

import java.awt.event.KeyEvent;

final class KeyBinding {
	final int glfwKey;
	final int awtKeyCode;
	final char normalChar;
	final char shiftedChar;

	KeyBinding(int glfwKey, int awtKeyCode, char normalChar, char shiftedChar) {
		this.glfwKey = glfwKey;
		this.awtKeyCode = awtKeyCode;
		this.normalChar = normalChar;
		this.shiftedChar = shiftedChar;
	}

	char keyChar(boolean shiftDown) {
		return shiftDown ? shiftedChar : normalChar;
	}

	boolean postsPhysicalEvents() {
		return normalChar == KeyEvent.CHAR_UNDEFINED
			|| normalChar == '\b'
			|| normalChar == '\n'
			|| normalChar == '\t'
			|| normalChar == 27;
	}

	boolean postsRepeatPressEvents() {
		return normalChar == '\b';
	}

	boolean isLetter() {
		return normalChar >= 'a' && normalChar <= 'z';
	}
}
