package com.openrsc.server.content.worldedit;

import com.openrsc.server.model.entity.player.Group;
import com.openrsc.server.model.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Applies Builder-only session state after an ordinary authenticated login. */
public final class WorldBuilderPlayerSession {
	private static final Logger LOGGER = LogManager.getLogger(WorldBuilderPlayerSession.class);

	private WorldBuilderPlayerSession() {
	}

	public static void activate(Player player) {
		if (!player.getConfig().WORLD_BUILDER_MODE) {
			return;
		}
		if (!WorldBuilderMode.isBuilderAccount(player.getUsername()) || player.getGroupID() != Group.ADMIN) {
			LOGGER.error("Refusing World Builder session for an unauthorized player identity");
			player.message(player.getConfig().MESSAGE_PREFIX + "World Builder authorization failed.");
			return;
		}
		player.setCacheInvulnerable(true);
		WorldEditorAccessService.open(player);
	}
}
