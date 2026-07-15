package com.openrsc.client.entityhandling;

import java.util.HashSet;
import java.util.Set;

/**
 * Emits each client definition fallback once so malformed IDs remain
 * diagnosable without flooding the client log.
 */
final class ClientDefinitionFallbackDiagnostics {
	private final Set<String> loggedItemFallbacks = new HashSet<>();

	void logItemFallback(
		int requestedId,
		int resolvedId,
		boolean noted,
		String reason,
		int itemCount) {
		String key = requestedId + ":" + resolvedId + ":" + noted + ":" + reason;
		if (loggedItemFallbacks.add(key)) {
			System.err.println(
				"CLIENT_ITEM_DEF_FALLBACK requestedId=" + requestedId
					+ " resolvedId=" + resolvedId
					+ " noted=" + noted
					+ " reason=" + reason
					+ " itemCount=" + itemCount
			);
		}
	}
}
