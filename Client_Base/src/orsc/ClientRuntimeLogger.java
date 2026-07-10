package orsc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ClientRuntimeLogger {
	private static final Object LOCK = new Object();
	private static final DateTimeFormatter TIMESTAMP_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String LOG_FILE_PROPERTY = "spoiledmilk.clientLog";
	private static final String LOG_FILE_ENV = "SPOILED_MILK_CLIENT_LOG";

	private static boolean uncaughtHandlerInstalled = false;

	private ClientRuntimeLogger() {
	}

	static void installUncaughtExceptionHandler() {
		synchronized (LOCK) {
			if (uncaughtHandlerInstalled) {
				return;
			}
			uncaughtHandlerInstalled = true;
		}
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
			logThrowable("Uncaught exception in " + thread.getName(), throwable));
	}

	static void log(String message) {
		synchronized (LOCK) {
			try (PrintWriter writer = openWriter()) {
				writer.println(timestamp() + " " + message);
			} catch (IOException ignored) {
			}
		}
	}

	static void logThrowable(String context, Throwable throwable) {
		RendererDiagnosticSession.recordThrowable(context, throwable);
		synchronized (LOCK) {
			try (PrintWriter writer = openWriter()) {
				writer.println(timestamp() + " " + context);
				if (throwable != null) {
					throwable.printStackTrace(writer);
				}
			} catch (IOException ignored) {
			}
		}
	}

	private static PrintWriter openWriter() throws IOException {
		return new PrintWriter(new FileWriter(logFile(), true));
	}

	private static File logFile() {
		String configuredPath = System.getProperty(LOG_FILE_PROPERTY);
		if (configuredPath == null || configuredPath.trim().isEmpty()) {
			configuredPath = System.getenv(LOG_FILE_ENV);
		}
		if (configuredPath != null && !configuredPath.trim().isEmpty()) {
			return new File(configuredPath.trim());
		}
		return new File(System.getProperty("user.dir", "."), "spoiled-milk-client.log");
	}

	private static String timestamp() {
		return "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "]";
	}
}
