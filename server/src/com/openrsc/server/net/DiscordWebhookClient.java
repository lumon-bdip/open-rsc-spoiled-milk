package com.openrsc.server.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhookClient {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int TIMEOUT_MILLIS = 5000;

	private DiscordWebhookClient() {
	}

	public static boolean sendItemShowcase(
		final String webhookUrl,
		final String playerName,
		final String itemName,
		final long amount,
		final String source,
		final int itemId,
		final int combatLevel,
		final int totalLevel) {
		if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.equals("null")) {
			return false;
		}

		final String amountText = amount == 1 ? "1" : String.format("%,d", amount);
		final String json = "{"
			+ "\"allowed_mentions\":{\"parse\":[]},"
			+ "\"embeds\":[{"
			+ "\"title\":\"Item showcase\","
			+ "\"description\":\"" + jsonEscape("**" + playerName + "** showed off **" + itemName + "**") + "\","
			+ "\"color\":16766720,"
			+ "\"fields\":["
			+ "{\"name\":\"Amount\",\"value\":\"" + jsonEscape(amountText) + "\",\"inline\":true},"
			+ "{\"name\":\"Source\",\"value\":\"" + jsonEscape(source) + "\",\"inline\":true},"
			+ "{\"name\":\"Player\",\"value\":\"" + jsonEscape("Combat " + combatLevel + ", total " + totalLevel) + "\",\"inline\":true},"
			+ "{\"name\":\"Item ID\",\"value\":\"" + itemId + "\",\"inline\":true}"
			+ "],"
			+ "\"footer\":{\"text\":\"Spoiled Milk\"}"
			+ "}]"
			+ "}";

		return postJson(webhookUrl, json);
	}

	private static boolean postJson(final String webhookUrl, final String json) {
		HttpURLConnection connection = null;
		try {
			final URL url = new URL(webhookUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.addRequestProperty("User-Agent", "spoiled-milk");
			connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

			final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			connection.setFixedLengthStreamingMode(bytes.length);
			connection.connect();
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(bytes);
			}

			final int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				LOGGER.warn("Discord item showcase webhook returned HTTP {}", responseCode);
				return false;
			}
			return true;
		} catch (IOException ioe) {
			LOGGER.warn("Discord item showcase webhook post failed: {}", ioe.toString());
			return false;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static String jsonEscape(final String value) {
		final StringBuilder escaped = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			switch (c) {
				case '"':
					escaped.append("\\\"");
					break;
				case '\\':
					escaped.append("\\\\");
					break;
				case '\b':
					escaped.append("\\b");
					break;
				case '\f':
					escaped.append("\\f");
					break;
				case '\n':
					escaped.append("\\n");
					break;
				case '\r':
					escaped.append("\\r");
					break;
				case '\t':
					escaped.append("\\t");
					break;
				default:
					if (c < 0x20) {
						escaped.append(String.format("\\u%04x", (int) c));
					} else {
						escaped.append(c);
					}
					break;
			}
		}
		return escaped.toString();
	}
}
