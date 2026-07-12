package com.openrsc.server.content.worldedit;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.io.WorldEditorTerrainArchive;
import com.openrsc.server.io.WorldEditorTerrainSaveFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Single-owner, strictly sequenced editor session and bounded server-lifetime terrain draft. */
public final class WorldEditorSessionManager {
	public static final int TERRAIN_DRAFT_LIMIT = 4096;
	private final SecureRandom random;
	private Session active;
	private WorldEditorTerrainArchive terrainArchive;
	private Path terrainArchivePath;
	private String terrainBaseSha256;
	private final Map<String,WorldEditorTerrainArchive.Snapshot> terrainDraft =
		new LinkedHashMap<String,WorldEditorTerrainArchive.Snapshot>();
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
	public synchronized boolean ownsActiveSession(Player player){return player!=null&&player.isAdmin()&&active!=null&&active.ownerHash==player.getUsernameHash();}
	public synchronized WorldEditorTerrainArchive.Snapshot inspectTerrain(Player player, int x, int y, int plane) throws IOException {
		WorldEditorTerrainArchive.Snapshot archived=inspectArchivedTerrain(player,x,y,plane);
		WorldEditorTerrainArchive.Snapshot drafted=terrainDraft.get(terrainKey(x,y,plane));
		return drafted==null?archived:drafted;
	}
	public synchronized WorldEditorTerrainArchive.Snapshot paintTerrain(Player player, int x, int y, int plane,
		int fieldMask, int elevation, int groundTexture, int groundOverlay,int roofTexture,
		int horizontalWall,int verticalWall,int diagonal) throws IOException {
		if(fieldMask<=0||(fieldMask&~127)!=0)throw new IllegalArgumentException("Select at least one supported terrain field.");
		if(!rawByte(elevation)||!rawByte(groundTexture)||!rawByte(groundOverlay)||!rawByte(roofTexture)
			||!rawByte(horizontalWall)||!rawByte(verticalWall))throw new IllegalArgumentException("Terrain byte values must be from 0 to 255.");
		WorldEditorTerrainArchive.Snapshot archived=inspectArchivedTerrain(player,x,y,plane);
		String key=terrainKey(x,y,plane);
		WorldEditorTerrainArchive.Snapshot current=terrainDraft.containsKey(key)?terrainDraft.get(key):archived;
		WorldEditorTerrainArchive.Snapshot painted=current.paint(fieldMask,elevation,groundTexture,groundOverlay,roofTexture,horizontalWall,verticalWall,diagonal);
		if(painted.sameRawTile(archived))terrainDraft.remove(key);
		else {
			if(!terrainDraft.containsKey(key)&&terrainDraft.size()>=TERRAIN_DRAFT_LIMIT)throw new IllegalStateException("Terrain draft limit reached.");
			terrainDraft.put(key,painted);
		}
		return painted;
	}
	public synchronized TerrainStrokeResult paintTerrainStroke(Player player,int[][] requestedTiles,int plane,
		int fieldMask,int elevation,int groundTexture,int groundOverlay,int roofTexture,
		int horizontalWall,int verticalWall,int diagonal) throws IOException {
		validateTerrainPaint(fieldMask,elevation,groundTexture,groundOverlay,roofTexture,horizontalWall,verticalWall);
		int[][] coordinates=WorldEditorTerrainStroke.validateTiles(requestedTiles);
		List<WorldEditorTerrainArchive.Snapshot> before=new ArrayList<WorldEditorTerrainArchive.Snapshot>(coordinates.length);
		List<WorldEditorTerrainArchive.Snapshot> after=new ArrayList<WorldEditorTerrainArchive.Snapshot>(coordinates.length);
		List<WorldEditorTerrainArchive.Snapshot> archived=new ArrayList<WorldEditorTerrainArchive.Snapshot>(coordinates.length);
		boolean[] draftedBefore=new boolean[coordinates.length],draftedAfter=new boolean[coordinates.length];int at=0;
		for(int[] coordinate:coordinates){
			WorldEditorTerrainArchive.Snapshot base=inspectArchivedTerrain(player,coordinate[0],coordinate[1],plane);
			String key=terrainKey(coordinate[0],coordinate[1],plane);
			WorldEditorTerrainArchive.Snapshot current=terrainDraft.containsKey(key)?terrainDraft.get(key):base;
			WorldEditorTerrainArchive.Snapshot painted=current.paint(fieldMask,elevation,groundTexture,groundOverlay,roofTexture,horizontalWall,verticalWall,diagonal);
			draftedBefore[at]=terrainDraft.containsKey(key);draftedAfter[at]=!painted.sameRawTile(base);at++;
			archived.add(base);before.add(current);after.add(painted);
		}
		int projectedDraftSize=WorldEditorTerrainStroke.projectedDraftSize(terrainDraft.size(),draftedBefore,draftedAfter);
		if(projectedDraftSize>TERRAIN_DRAFT_LIMIT)throw new IllegalStateException("Terrain draft limit reached.");
		for(int i=0;i<coordinates.length;i++){
			String key=terrainKey(coordinates[i][0],coordinates[i][1],plane);
			if(after.get(i).sameRawTile(archived.get(i)))terrainDraft.remove(key);else terrainDraft.put(key,after.get(i));
		}
		return new TerrainStrokeResult(before,after);
	}
	public synchronized int terrainDraftSize(){return terrainDraft.size();}
	public synchronized int terrainDraftSectorCount(){java.util.HashSet<String> sectors=new java.util.HashSet<String>();for(WorldEditorTerrainArchive.Snapshot tile:terrainDraft.values())sectors.add(tile.coordinates.plane+":"+tile.coordinates.sectorX+":"+tile.coordinates.sectorY);return sectors.size();}
	public synchronized WorldEditorTerrainSaveFiles.SaveResult saveTerrainDraft(Player player) throws IOException {
		if(!ownsActiveSession(player))throw new IllegalStateException("An active world editor session owned by this administrator is required.");
		if(terrainDraft.isEmpty())throw new IllegalStateException("Terrain draft is empty.");
		if(!player.getConfig().WANT_CUSTOM_LANDSCAPE)throw new IllegalStateException("Durable terrain saving requires Custom_Landscape.orsc.");
		if(terrainArchivePath==null||terrainBaseSha256==null)throw new IllegalStateException("Terrain archive base revision is unavailable.");
		List<WorldEditorTerrainSaveFiles.TileRecord> records=new ArrayList<WorldEditorTerrainSaveFiles.TileRecord>(terrainDraft.size());
		for(WorldEditorTerrainArchive.Snapshot s:terrainDraft.values())records.add(WorldEditorTerrainSaveFiles.TileRecord.of(s.coordinates.worldX,s.coordinates.worldY,s.coordinates.plane,s.elevation,s.groundTexture,s.groundOverlay,s.roofTexture,s.horizontalWall,s.verticalWall,s.diagonal));
		Path clientArchive=Paths.get("../Client_Base/Cache/video/Custom_Landscape.orsc").toAbsolutePath().normalize();
		Path backups=terrainArchivePath.getParent().resolve("world-editor-backups");closeTerrainArchive();
		try{
			WorldEditorTerrainSaveFiles.SaveResult saved=WorldEditorTerrainSaveFiles.save(terrainArchivePath,clientArchive,backups,terrainBaseSha256,records);
			terrainBaseSha256=saved.resultSha256;terrainArchive=new WorldEditorTerrainArchive(terrainArchivePath.toFile());terrainDraft.clear();return saved;
		}catch(IOException|RuntimeException failure){try{terrainArchive=new WorldEditorTerrainArchive(terrainArchivePath.toFile());}catch(IOException reopen){failure.addSuppressed(reopen);}throw failure;}
	}
	private static void validateTerrainPaint(int fieldMask,int elevation,int groundTexture,int groundOverlay,int roofTexture,int horizontalWall,int verticalWall){
		if(fieldMask<=0||(fieldMask&~127)!=0)throw new IllegalArgumentException("Select at least one supported terrain field.");
		if(!rawByte(elevation)||!rawByte(groundTexture)||!rawByte(groundOverlay)||!rawByte(roofTexture)
			||!rawByte(horizontalWall)||!rawByte(verticalWall))throw new IllegalArgumentException("Terrain byte values must be from 0 to 255.");
	}
	private WorldEditorTerrainArchive.Snapshot inspectArchivedTerrain(Player player, int x, int y, int plane) throws IOException {
		if (terrainArchive == null) {
			String name = player.getConfig().WANT_CUSTOM_LANDSCAPE ? "Custom_Landscape.orsc"
				: (player.getConfig().MEMBER_WORLD ? "Authentic_Landscape.orsc" : "F2PLandscape.orsc");
			terrainArchivePath=Paths.get("./conf/server/data/"+name).toAbsolutePath().normalize();terrainBaseSha256=WorldEditorTerrainSaveFiles.sha256(terrainArchivePath);
			terrainArchive = new WorldEditorTerrainArchive(terrainArchivePath.toFile());
		}
		return terrainArchive.inspect(x, y, plane);
	}
	private void closeTerrainArchive() throws IOException {if(terrainArchive!=null){terrainArchive.close();terrainArchive=null;}}
	private static String terrainKey(int x,int y,int plane){return plane+":"+x+":"+y;}
	private static boolean rawByte(int value){return value>=0&&value<=255;}

	private static final class Session { final long id, ownerHash; int nextSequence=1; Session(long i,long o){id=i;ownerHash=o;} }
	public static final class TerrainStrokeResult {
		public final List<WorldEditorTerrainArchive.Snapshot> before,after;
		private TerrainStrokeResult(List<WorldEditorTerrainArchive.Snapshot> b,List<WorldEditorTerrainArchive.Snapshot> a){before=b;after=a;}
	}
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
