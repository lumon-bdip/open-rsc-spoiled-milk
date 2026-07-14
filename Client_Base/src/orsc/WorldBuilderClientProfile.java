package orsc;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/** Explicit desktop-only connection profile for the isolated World Builder runtime. */
public final class WorldBuilderClientProfile {
	public static final String ENABLED_PROPERTY = "openrsc.worldBuilderMode";
	public static final String HOST_PROPERTY = "openrsc.worldBuilderHost";
	public static final String PORT_PROPERTY = "openrsc.worldBuilderPort";
	public static final String CREDENTIAL_FILE_PROPERTY = "openrsc.worldBuilderCredentialFile";
	public static final String ACCOUNT_NAME = "Builder";
	private static final Pattern CREDENTIAL_PATTERN = Pattern.compile("[A-Za-z0-9]{20}");
	private static WorldBuilderClientProfile current = disabled();

	private final boolean enabled;
	private final String host;
	private final int port;
	private final String credential;

	private WorldBuilderClientProfile(boolean enabled, String host, int port, String credential) {
		this.enabled = enabled;
		this.host = host;
		this.port = port;
		this.credential = credential;
	}

	public static synchronized WorldBuilderClientProfile initializeFromSystemProperties() {
		String enabledValue = System.getProperty(ENABLED_PROPERTY, "false").trim();
		if (!"true".equalsIgnoreCase(enabledValue) && !"false".equalsIgnoreCase(enabledValue)) {
			throw new IllegalArgumentException(ENABLED_PROPERTY + " must be true or false");
		}
		if (!Boolean.parseBoolean(enabledValue)) {
			current = disabled();
			return current;
		}

		String host = System.getProperty(HOST_PROPERTY, "127.0.0.1").trim();
		if (!isLoopbackAddress(host)) {
			throw new IllegalArgumentException("World Builder host must resolve only to loopback addresses");
		}
		int port;
		try {
			port = Integer.parseInt(System.getProperty(PORT_PROPERTY, "").trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("World Builder port is invalid");
		}
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("World Builder port is invalid");
		}

		String credentialFile = System.getProperty(CREDENTIAL_FILE_PROPERTY, "").trim();
		if (credentialFile.isEmpty()) {
			throw new IllegalArgumentException("World Builder credential file is required");
		}
		String credential = readCredential(Paths.get(credentialFile).toAbsolutePath().normalize());
		current = new WorldBuilderClientProfile(true, host, port, credential);
		return current;
	}

	public static WorldBuilderClientProfile current() {
		return current;
	}

	public static boolean isEnabled() {
		return current.enabled;
	}

	public void applyConnection() {
		if (!enabled) {
			return;
		}
		Config.SERVER_IP = host;
		Config.SERVER_PORT = port;
	}

	public String username() {
		return ACCOUNT_NAME;
	}

	public String credential() {
		return credential;
	}

	private static WorldBuilderClientProfile disabled() {
		return new WorldBuilderClientProfile(false, null, 0, null);
	}

	private static String readCredential(Path path) {
		try {
			if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
				throw new IllegalArgumentException("World Builder credential file is not a regular file");
			}
			long size = Files.size(path);
			if (size < 1 || size > 64) {
				throw new IllegalArgumentException("World Builder credential file has an invalid size");
			}
			String credential = new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim();
			if (!CREDENTIAL_PATTERN.matcher(credential).matches()) {
				throw new IllegalArgumentException("World Builder credential file is invalid");
			}
			return credential;
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalArgumentException("Unable to read World Builder credential file", exception);
		}
	}

	private static boolean isLoopbackAddress(String address) {
		if (address == null || address.isEmpty()) {
			return false;
		}
		try {
			InetAddress[] resolved = InetAddress.getAllByName(address);
			if (resolved.length == 0) {
				return false;
			}
			for (InetAddress candidate : resolved) {
				if (!candidate.isLoopbackAddress()) {
					return false;
				}
			}
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
}
