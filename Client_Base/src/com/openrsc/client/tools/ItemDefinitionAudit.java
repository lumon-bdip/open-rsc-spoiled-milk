package com.openrsc.client.tools;

import com.openrsc.client.entityhandling.EntityHandler;
import com.openrsc.client.entityhandling.defs.ItemDef;

public final class ItemDefinitionAudit {
	private ItemDefinitionAudit() {
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: ItemDefinitionAudit <max-server-item-id>");
			System.exit(2);
		}

		int maxServerItemId = Integer.parseInt(args[0]);
		EntityHandler.load(true);

		int failures = 0;
		for (int itemId = 0; itemId <= maxServerItemId; itemId++) {
			ItemDef item = EntityHandler.getItemDef(itemId);
			boolean expectedPlaceholder = itemId == 1544 || itemId == 1545;
			boolean placeholder = item == null
				|| ("Unobtanium".equals(item.getName()) && item.getSpriteID() == 70);
			if ((!expectedPlaceholder && placeholder) || item == null || item.id != itemId) {
				failures++;
				System.err.println(
					"CLIENT_ITEM_AUDIT_FAIL requestedId=" + itemId
						+ " resolvedId=" + (item == null ? -1 : item.id)
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
			"CLIENT_ITEM_AUDIT passed definitions=" + (maxServerItemId + 1)
				+ " clientItemCount=" + EntityHandler.itemCount()
		);
	}
}
