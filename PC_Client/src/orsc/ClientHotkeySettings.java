package orsc;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

final class ClientHotkeySettings {
	static final String RELEASE_BUILD_PROPERTY = "spoiledmilk.releaseBuild";
	static final String RELEASE_MARKER_RESOURCE = "/spoiled-milk-release-build.marker";
	private static final boolean RELEASE_BUILD =
		Boolean.parseBoolean(System.getProperty(RELEASE_BUILD_PROPERTY, "false"))
			|| hasReleaseMarker();

	private ClientHotkeySettings() {
	}

	static boolean isReleaseBuild() {
		return RELEASE_BUILD;
	}

	static boolean shouldSuppressFunctionKey(int keyCode) {
		return RELEASE_BUILD
			&& keyCode >= KeyEvent.VK_F1
			&& keyCode <= KeyEvent.VK_F12
			&& keyCode != KeyEvent.VK_F6;
	}

	static boolean showDeveloperFunctionKeyHints() {
		return !RELEASE_BUILD;
	}

	private static boolean hasReleaseMarker() {
		try (InputStream marker = ClientHotkeySettings.class.getResourceAsStream(RELEASE_MARKER_RESOURCE)) {
			return marker != null;
		} catch (IOException ignored) {
			return false;
		}
	}
}
