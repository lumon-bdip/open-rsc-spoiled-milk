package com.openrsc.client.tools;

import com.openrsc.client.entityhandling.EntityHandler;
import com.openrsc.client.entityhandling.defs.ItemDef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ItemDefinitionAudit {
	private ItemDefinitionAudit() {
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: ItemDefinitionAudit <server-item-catalog.tsv>");
			System.exit(2);
		}

		Map<Integer, String> serverItems = loadServerItems(args[0]);
		EntityHandler.load(true);

		int failures = 0;
		for (Map.Entry<Integer, String> entry : serverItems.entrySet()) {
			int itemId = entry.getKey();
			String expectedName = entry.getValue();
			ItemDef item = EntityHandler.getItemDef(itemId);
			boolean expectedPlaceholder = itemId == 1544 || itemId == 1545;
			boolean placeholder = item == null
				|| ("Unobtanium".equals(item.getName()) && item.getSpriteID() == 70);
			boolean unknown = item != null && "Unknown item".equalsIgnoreCase(item.getName());
			boolean nameMismatch = !expectedName.isEmpty() && (item == null
				|| !normalizeName(expectedName).equals(normalizeName(item.getName())));
			if ((!expectedPlaceholder && placeholder) || unknown || item == null || item.id != itemId || nameMismatch) {
				failures++;
				System.err.println(
					"CLIENT_ITEM_AUDIT_FAIL requestedId=" + itemId
						+ " resolvedId=" + (item == null ? -1 : item.id)
						+ " expectedName=\"" + expectedName + "\""
						+ " name=\"" + (item == null ? "<missing>" : item.getName()) + "\""
						+ " sprite=" + (item == null ? -1 : item.getSpriteID())
				);
			}
		}

		if (failures > 0) {
			System.err.println("CLIENT_ITEM_AUDIT failed definitions=" + failures);
			System.exit(1);
		}

		System.out.println(
			"CLIENT_ITEM_AUDIT passed definitions=" + serverItems.size()
				+ " clientItemCount=" + EntityHandler.itemCount()
		);
	}

	private static Map<Integer, String> loadServerItems(String path) throws IOException {
		Map<Integer, String> serverItems = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] fields = line.split("\t", -1);
				if (fields.length != 2) {
					throw new IOException("Invalid server item catalog line: " + line);
				}
				serverItems.put(Integer.parseInt(fields[0]), fields[1]);
			}
		}
		return serverItems;
	}

	private static String normalizeName(String name) {
		return name.trim().replace('-', ' ').replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}
}
