package orsc;

/** Client-local presentation state for the world editor's reversible Build view. */
public final class WorldEditorBuildSettings {
	private static volatile boolean enabled;

	private WorldEditorBuildSettings() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}
}
