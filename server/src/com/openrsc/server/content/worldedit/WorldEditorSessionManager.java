package com.openrsc.server.content.worldedit;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.io.WorldEditorTerrainArchive;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

/** Single-owner, strictly sequenced, read-only editor session state. */
public final class WorldEditorSessionManager {
	private final SecureRandom random;
	private Session active;
	private WorldEditorTerrainArchive terrainArchive;
	public WorldEditorSessionManager() { this(new SecureRandom()); }
	WorldEditorSessionManager(SecureRandom random) { this.random = random; }

	public synchronized OpenResult open(Player player, boolean enabled) {
		if (!enabled) return OpenResult.denied("The in-game world editor is disabled on this server.");
		if (player == null || !player.isAdmin()) return OpenResult.denied("Administrator authorization is required.");
		if (active != null && active.ownerHash != player.getUsernameHash()) return OpenResult.denied("Another administrator owns the active editor session.");
		if (active == null) {
			long id;
			do { id = random.nextLong(); } while (id == 0L);
			active = new Session(id, player.getUsernameHash());
		}
		return OpenResult.opened(active.id, active.nextSequence);
	}

	public synchronized Validation validate(Player player, long id, int sequence) {
		if (player == null || !player.isAdmin() || active == null || active.ownerHash != player.getUsernameHash() || active.id != id)
			return Validation.denied("Editor session is not active or is not owned by this administrator.");
		if (sequence != active.nextSequence) return Validation.denied("Editor request sequence mismatch.");
		active.nextSequence++;
		return Validation.accepted(active.nextSequence);
	}

	public synchronized boolean close(Player player, long id, int sequence) {
		if (!validate(player, id, sequence).accepted) return false;
		active = null;
		return true;
	}
	public synchronized void closeFor(Player player) {
		if (player != null && active != null && active.ownerHash == player.getUsernameHash()) active = null;
	}
	public synchronized boolean hasActiveSession() { return active != null; }
	public synchronized WorldEditorTerrainArchive.Snapshot inspectTerrain(Player player, int x, int y, int plane) throws IOException {
		if (terrainArchive == null) {
			String name = player.getConfig().WANT_CUSTOM_LANDSCAPE ? "Custom_Landscape.orsc"
				: (player.getConfig().MEMBER_WORLD ? "Authentic_Landscape.orsc" : "F2PLandscape.orsc");
			terrainArchive = new WorldEditorTerrainArchive(new File("./conf/server/data/" + name));
		}
		return terrainArchive.inspect(x, y, plane);
	}

	private static final class Session { final long id, ownerHash; int nextSequence=1; Session(long i,long o){id=i;ownerHash=o;} }
	public static final class OpenResult {
		public final boolean opened; public final long sessionId; public final int nextSequence; public final String message;
		private OpenResult(boolean o,long i,int s,String m){opened=o;sessionId=i;nextSequence=s;message=m;}
		static OpenResult opened(long i,int s){return new OpenResult(true,i,s,"");}
		static OpenResult denied(String m){return new OpenResult(false,0,0,m);}
	}
	public static final class Validation {
		public final boolean accepted; public final int nextSequence; public final String message;
		private Validation(boolean a,int n,String m){accepted=a;nextSequence=n;message=m;}
		static Validation accepted(int n){return new Validation(true,n,"");}
		static Validation denied(String m){return new Validation(false,0,m);}
	}
}
