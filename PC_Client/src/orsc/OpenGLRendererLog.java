package orsc;

final class OpenGLRendererLog {
	private OpenGLRendererLog() {
	}

	static void log(String message) {
		System.out.println("[renderer-v2 opengl] " + message);
		RendererDiagnosticSession.recordEvent("renderer.opengl.log", message);
	}
}
