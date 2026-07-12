package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.content.worldedit.WorldEditorSessionManager.Validation;
import com.openrsc.server.io.WorldEditorTerrainArchive;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.region.TileValue;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.net.rsc.PayloadProcessor;
import com.openrsc.server.net.rsc.enums.OpcodeIn;
import com.openrsc.server.net.rsc.struct.incoming.WorldEditorRequestStruct;
import com.openrsc.server.net.rsc.struct.outgoing.WorldEditorStruct;

public final class WorldEditorHandler implements PayloadProcessor<WorldEditorRequestStruct, OpcodeIn> {
	@Override public void process(WorldEditorRequestStruct request, Player player) {
		if (!player.getConfig().ALLOW_IN_GAME_WORLD_EDITOR || !player.isAdmin()) { error(player, 0, "Editor authorization was revoked."); return; }
		Validation validation = player.getWorld().getServer().getWorldEditorSessions().validate(player, request.sessionId, request.sequence);
		if (!validation.accepted) { error(player, 0, validation.message); return; }
		if (request.type == 1) {
			player.getWorld().getServer().getWorldEditorSessions().closeFor(player);
			WorldEditorStruct out = new WorldEditorStruct(); out.type=2; out.sequence=validation.nextSequence;
			ActionSender.sendWorldEditor(player, out); return;
		}
		try {
			if (request.type == 2) inspectTerrain(request, player, validation.nextSequence);
			else if (request.type == 3) inspectObject(request, player, validation.nextSequence);
			else if (request.type == 4) inspectNpc(request, player, validation.nextSequence);
			else if (request.type == 5) paintTerrain(request, player, validation.nextSequence);
			else error(player, validation.nextSequence, "Unsupported editor operation.");
		} catch (Exception e) { error(player, validation.nextSequence, "Editor request failed: " + e.getMessage()); }
	}

	private void inspectTerrain(WorldEditorRequestStruct r, Player p, int next) throws Exception {
		WorldEditorTerrainArchive.Snapshot s=p.getWorld().getServer().getWorldEditorSessions().inspectTerrain(p,r.x,r.y,r.plane);
		sendTerrain(p,s,next,3,r.objectType==1);
	}
	private void paintTerrain(WorldEditorRequestStruct r,Player p,int next) throws Exception {
		if(!p.getWorld().withinWorld(r.x,r.y))throw new IllegalArgumentException("Terrain tile is outside the runtime world.");
		validateOverlay(p,r.fieldMask,r.groundOverlay);
		WorldEditorTerrainArchive.Snapshot s=p.getWorld().getServer().getWorldEditorSessions().paintTerrain(p,r.x,r.y,r.plane,r.fieldMask,r.elevation,r.groundTexture,r.groundOverlay);
		TileValue runtime=p.getWorld().getTile(r.x,r.y);
		if(runtime==null)throw new IllegalArgumentException("Terrain tile is outside the runtime world.");
		if((r.fieldMask&1)!=0)runtime.elevation=(byte)s.elevation;
		if((r.fieldMask&4)!=0){runtime.overlay=(byte)s.groundOverlay;runtime.setTerrainBlocked(overlayBlocks(p,s.groundOverlay));}
		sendTerrain(p,s,next,7,false);
	}
	private void sendTerrain(Player p,WorldEditorTerrainArchive.Snapshot s,int next,int type,boolean copied) {
		TileValue runtime=p.getWorld().getTile(s.coordinates.worldX,s.coordinates.worldY);
		WorldEditorStruct o=new WorldEditorStruct(); o.type=type; o.sequence=next; o.x=s.coordinates.worldX;o.y=s.coordinates.worldY;o.plane=s.coordinates.plane;
		o.sectorX=s.coordinates.sectorX;o.sectorY=s.coordinates.sectorY;o.localX=s.coordinates.localX;o.localY=s.coordinates.localY;
		o.elevation=s.elevation;o.groundTexture=s.groundTexture;o.groundOverlay=s.groundOverlay;o.roofTexture=s.roofTexture;
		o.horizontalWall=s.horizontalWall;o.verticalWall=s.verticalWall;o.diagonal=s.diagonal;
		o.traversalMask=runtime==null?0:runtime.traversalMask&0xff;o.projectileAllowed=runtime!=null&&runtime.projectileAllowed;o.copy=copied;
		// Names are tab-delimited protocol data; the client owns the concise semantic layout.
		o.message=wallDefinitionName(p,s.verticalWall-1)+"\t"+wallDefinitionName(p,s.horizontalWall-1)
			+"\t"+wallDefinitionName(p,s.diagonalDefinitionId());
		ActionSender.sendWorldEditor(p,o);
	}
	private void validateOverlay(Player p,int mask,int overlay){if((mask&4)==0)return;overlayBlocks(p,overlay);}
	private boolean overlayBlocks(Player p,int overlay){
		int effective=overlay==250?2:overlay;if(effective==0)return false;
		try{return p.getWorld().getServer().getEntityHandler().getTileDef(effective-1).getObjectType()!=0;}
		catch(Exception e){throw new IllegalArgumentException("Floor texture "+overlay+" is not defined.");}
	}
	private String wallDefinitionName(Player p,int id){if(id<0)return "none";try{return p.getWorld().getServer().getEntityHandler().getDoorDef(id).getName();}catch(Exception e){return "unknown";}}
	private void inspectObject(WorldEditorRequestStruct r, Player p, int next) {
		GameObject object=p.getViewArea().getGameObject(Point.location(r.x,r.y));
		String text;
		if(object==null||object.getID()!=r.entityId) text="Object is no longer present at " + r.x + "," + r.y;
		else text=(object.isScenery()?object.getGameObjectDef().getName():object.getDoorDef().getName())
			+" | id="+object.getID()+" direction="+object.getDirection()+" type="+(object.isScenery()?"scenery":"boundary")
			+" source=runtime-authoritative @ "+object.getX()+","+object.getY();
		info(p,4,next,text);
	}
	private void inspectNpc(WorldEditorRequestStruct r, Player p, int next) {
		Npc npc=p.getWorld().getNpc(r.entityId);
		int radius=npc==null||npc.getLoc()==null?0:Math.max(Math.max(npc.getLoc().startX-npc.getLoc().minX,npc.getLoc().maxX-npc.getLoc().startX),Math.max(npc.getLoc().startY-npc.getLoc().minY,npc.getLoc().maxY-npc.getLoc().startY));
		String text=npc==null?"NPC is no longer present":npc.getDef().getName()+" | id="+npc.getID()+" radius="+radius+" serverIndex="+npc.getIndex()
			+" source=runtime-authoritative @ "+npc.getX()+","+npc.getY();
		info(p,5,next,text);
	}
	private static void info(Player p,int type,int sequence,String text){WorldEditorStruct o=new WorldEditorStruct();o.type=type;o.sequence=sequence;o.message=text;ActionSender.sendWorldEditor(p,o);}
	private static void error(Player p,int sequence,String text){info(p,6,sequence,text==null?"Unknown editor error":text);}
}
