package com.openrsc.interfaces.misc;

import com.openrsc.interfaces.InputListener;
import com.openrsc.interfaces.NCustomComponent;
import com.openrsc.client.entityhandling.EntityHandler;
import orsc.Config;
import orsc.mudclient;

/** Desktop-only world editor shell and the first command-backed entity tools. */
public final class WorldEditorInterface extends NCustomComponent {
	public enum Mode { NAVIGATE, INSPECT, TERRAIN, SCENERY, NPC }
	public enum SceneryTool { PLACE, ROTATE, REMOVE }
	public enum NpcTool { PLACE, REMOVE }
	private static final String[] TABS={"Navigate","Inspect","Terrain","Scenery","NPC"};
	private final mudclient mc;
	private Mode mode=Mode.NAVIGATE;
	private long sessionId;
	private int nextSequence;
	private String inspectionStatus="Nothing inspected yet";
	private String[] inspectionDetails=new String[0];
	private String inspectionKind="";
	private String copiedInspectionKind="";
	private String[] copiedInspectionDetails=new String[0];
	private boolean copyNextInspection=false;
	private int[] copiedTerrainFields;
	private int[] inspectedTerrainFields;
	private int lastClickedX=-1,lastClickedY=-1,brushX=-1,brushY=-1;
	private String teleportX="",teleportY="";
	private int coordinateFocus=0;
	private boolean replaceFocusedText=false;
	private boolean clickTeleportPreferred=false;
	private SceneryTool sceneryTool=SceneryTool.PLACE;
	private NpcTool npcTool=NpcTool.PLACE;
	private int sceneryId=0,npcId=0,npcRadius=0;
	private String sceneryIdText="0",npcIdText="0",npcRadiusText="0";
	private boolean paintElevation=false,paintFloorColor=true,paintFloorTexture=false;
	private int terrainElevation=0,terrainFloorColor=0,terrainFloorTexture=0;
	private String terrainElevationText="0",terrainFloorColorText="0",terrainFloorTextureText="0";
	private int terrainBrushSize=1,terrainStrokeIndex=0,terrainStrokeMask=0;
	private int terrainStrokeElevation=0,terrainStrokeColor=0,terrainStrokeTexture=0;
	private int[][] terrainStrokeTiles=null;
	private int dragX=-1,dragY=-1;

	public WorldEditorInterface(mudclient client) {
		super(client);mc=client;setLocation(12,8);setSize(390,330);setVisible(false);setIsOverlay(true);
		setInputListener(new InputListener(){
			@Override public boolean onMouseDown(int mx,int my,int down,int click){return handleMouse(mx,my,down,click);}
			@Override public boolean onCharTyped(char c,int key){return handleKey(c,key);}
		});
	}

	public void open(long id,int sequence){
		if(Config.isAndroid())return;
		sessionId=id;nextSequence=sequence;mode=Mode.NAVIGATE;
		int x=mc.getEditorPlayerWorldX(),y=mc.getEditorPlayerWorldY();brushX=x;brushY=y;teleportX=String.valueOf(x);teleportY=String.valueOf(y);
		clickTeleportPreferred=false;mc.setWorldEditorNavigateClickTeleport(false);setVisible(true);
	}
	public void closeFromServer(){mc.setWorldEditorNavigateClickTeleport(false);setVisible(false);sessionId=0;coordinateFocus=0;}
	public boolean isEditorOpen(){return isVisible()&&sessionId!=0;}
	public boolean isInspecting(){return isEditorOpen()&&mode==Mode.INSPECT;}
	public boolean isNavigating(){return isEditorOpen()&&mode==Mode.NAVIGATE;}
	public boolean isTerrainPainting(){return isEditorOpen()&&mode==Mode.TERRAIN;}
	public boolean isSceneryPlacing(){return isEditorOpen()&&mode==Mode.SCENERY&&sceneryTool==SceneryTool.PLACE;}
	public boolean isSceneryRotating(){return isEditorOpen()&&mode==Mode.SCENERY&&sceneryTool==SceneryTool.ROTATE;}
	public boolean isSceneryRemoving(){return isEditorOpen()&&mode==Mode.SCENERY&&sceneryTool==SceneryTool.REMOVE;}
	public boolean isNpcPlacing(){return isEditorOpen()&&mode==Mode.NPC&&npcTool==NpcTool.PLACE;}
	public boolean isNpcRemoving(){return isEditorOpen()&&mode==Mode.NPC&&npcTool==NpcTool.REMOVE;}
	public int getSceneryId(){return sceneryId;}
	public int getNpcId(){return npcId;}
	public int getNpcRadius(){return npcRadius;}
	public void selectScenery(int id){setSceneryId(id);}
	public void selectNpc(int id,int radius){setNpcId(id);setNpcRadius(radius);}
	public void setSequence(int sequence){nextSequence=sequence;}
	public void recordWorldClick(int x,int y){lastClickedX=x;lastClickedY=y;if(mode!=Mode.NAVIGATE){brushX=x;brushY=y;}}
	public void showInfo(int responseType,String text){
		inspectionKind=responseType==5?"NPC":(text!=null&&text.contains("type=boundary")?"Boundary":"Scenery");
		inspectionStatus="Authoritative "+inspectionKind.toLowerCase()+" inspection";inspectionDetails=wrap(text,58);
		if(responseType==4){int id=valueAfter(text,"id=");if(id>=0&&copyNextInspection)setSceneryId(id);}
		if(responseType==5){int id=valueAfter(text,"id="),radius=valueAfter(text,"radius=");if(copyNextInspection){if(id>=0)setNpcId(id);if(radius>=0)setNpcRadius(radius);}}
		if(copyNextInspection)copyInspected();copyNextInspection=false;
	}
	public void showError(String text){
		copyNextInspection=false;if(terrainStrokeTiles!=null&&terrainStrokeIndex>0)mc.reloadWorldEditorTerrain();terrainStrokeTiles=null;
		inspectionStatus="Server rejected request";inspectionDetails=wrap(text,58);
	}
	public void showTerrain(int sequence,int x,int y,int plane,int sx,int sy,int lx,int ly,int elev,int texture,int overlay,int roof,int hwall,int vwall,int diag,int collision,boolean projectile,boolean copied,String definitions){
		nextSequence=sequence;inspectionKind="Terrain";inspectionStatus="Authoritative terrain inspection";
		inspectedTerrainFields=new int[]{elev,texture,overlay,roof,hwall,vwall,diag};
		String[] names=definitions==null?new String[0]:definitions.split("\\t",-1);
		String northName=names.length>0?names[0]:"unknown",eastName=names.length>1?names[1]:"unknown",diagonalName=names.length>2?names[2]:"unknown";
		int northId=vwall>0?vwall-1:-1,eastId=hwall>0?hwall-1:-1,diagonalId=diagonalDefinitionId(diag);
		java.util.List<String> lines=new java.util.ArrayList<String>();java.util.Collections.addAll(lines,
			"Coordinates: "+x+", "+y,"Plane: "+plane+" ("+planeName(plane)+")","Elevation: "+elev,
			"Floor Color: "+texture,"Floor Texture: "+overlay,
			"Walls: North "+wall(northId,northName)+" | East "+wall(eastId,eastName),
			"Diagonal "+wall(diagonalId,diagonalName)+" ("+diagonalRotation(diag)+")",
			"Collision: 0x"+Integer.toHexString(collision)+" | Projectiles: "+(projectile?"allowed":"blocked"),
			"Archive: sector "+sx+","+sy+" | local "+lx+","+ly);
		inspectionDetails=lines.toArray(new String[lines.size()]);
		if(copied||copyNextInspection)copyInspected();copyNextInspection=false;
	}
	public void acceptTerrainPaint(int sequence,int x,int y,int plane,int sx,int sy,int lx,int ly,int elev,int texture,int overlay,int roof,int hwall,int vwall,int diag,int collision,boolean projectile,int fieldMask,String definitions){
		showTerrain(sequence,x,y,plane,sx,sy,lx,ly,elev,texture,overlay,roof,hwall,vwall,diag,collision,projectile,false,definitions);
		boolean last=terrainStrokeTiles==null||terrainStrokeIndex>=terrainStrokeTiles.length-1;
		mc.applyWorldEditorTerrainPatch(x,y,plane,elev,texture,overlay,(fieldMask&4)!=0,last);
		if(last){int count=terrainStrokeTiles==null?1:terrainStrokeTiles.length;terrainStrokeTiles=null;inspectionStatus="Paint accepted: "+count+" tile"+(count==1?"":"s")+" (unsaved draft)";}
		else{terrainStrokeIndex++;inspectionStatus="Paint accepted "+terrainStrokeIndex+"/"+terrainStrokeTiles.length;sendTerrainStrokeTile();}
	}
	public int[] getCopiedTerrainFields(){return copiedTerrainFields==null?null:copiedTerrainFields.clone();}
	public void inspectTerrain(int worldX,int worldY,boolean copy){recordWorldClick(worldX,worldY);send(2,worldX,worldY,Math.floorDiv(worldY,944),0,0,copy?1:0);}
	public void paintTerrain(int worldX,int worldY){
		recordWorldClick(worldX,worldY);int mask=(paintElevation?1:0)|(paintFloorColor?2:0)|(paintFloorTexture?4:0);
		if(mask==0){showError("Select at least one terrain field to paint.");return;}if(!isTerrainPainting()||terrainStrokeTiles!=null)return;
		terrainStrokeTiles=terrainBrushSize==1?new int[][]{{worldX,worldY}}:centeredThreeByThree(worldX,worldY);
		terrainStrokeIndex=0;terrainStrokeMask=mask;terrainStrokeElevation=terrainElevation;terrainStrokeColor=terrainFloorColor;terrainStrokeTexture=terrainFloorTexture;
		sendTerrainStrokeTile();
	}
	private void sendTerrainStrokeTile(){
		int worldX=terrainStrokeTiles[terrainStrokeIndex][0],worldY=terrainStrokeTiles[terrainStrokeIndex][1];
		mc.packetHandler.getClientStream().newPacket(152);mc.packetHandler.getClientStream().bufferBits.putByte(5);
		mc.packetHandler.getClientStream().bufferBits.putLong(sessionId);mc.packetHandler.getClientStream().bufferBits.putInt(nextSequence);
		mc.packetHandler.getClientStream().bufferBits.putShort(worldX);mc.packetHandler.getClientStream().bufferBits.putShort(worldY);
		mc.packetHandler.getClientStream().bufferBits.putByte(Math.floorDiv(worldY,944));mc.packetHandler.getClientStream().bufferBits.putByte(terrainStrokeMask);
		mc.packetHandler.getClientStream().bufferBits.putByte(terrainStrokeElevation);mc.packetHandler.getClientStream().bufferBits.putByte(terrainStrokeColor);
		mc.packetHandler.getClientStream().bufferBits.putByte(terrainStrokeTexture);mc.packetHandler.getClientStream().finishPacket();
	}
	private static int[][] centeredThreeByThree(int x,int y){int[][] tiles=new int[9][2];tiles[0][0]=x;tiles[0][1]=y;int at=1;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0){tiles[at][0]=x+dx;tiles[at++][1]=y+dy;}return tiles;}
	public void inspectObject(int worldX,int worldY,int id,int direction,int type){inspectObject(worldX,worldY,id,direction,type,false);}
	public void inspectObject(int worldX,int worldY,int id,int direction,int type,boolean copy){recordWorldClick(worldX,worldY);copyNextInspection=copy;send(3,worldX,worldY,Math.floorDiv(worldY,944),id,direction,type);}
	public void inspectNpc(int serverIndex){inspectNpc(serverIndex,false);}
	public void inspectNpc(int serverIndex,boolean copy){copyNextInspection=copy;send(4,0,0,0,serverIndex,0,0);}
	public void copyInspected(){
		if(inspectionKind.isEmpty())return;copiedInspectionKind=inspectionKind;copiedInspectionDetails=inspectionDetails.clone();
		if("Terrain".equals(inspectionKind)&&inspectedTerrainFields!=null){copiedTerrainFields=inspectedTerrainFields.clone();seedTerrain(inspectedTerrainFields[0],inspectedTerrainFields[1],inspectedTerrainFields[2]);}
		if("Scenery".equals(inspectionKind)){int id=valueAfter(join(inspectionDetails),"id=");if(id>=0)setSceneryId(id);}
		if("NPC".equals(inspectionKind)){String text=join(inspectionDetails);int id=valueAfter(text,"id="),radius=valueAfter(text,"radius=");if(id>=0)setNpcId(id);if(radius>=0)setNpcRadius(radius);}
		inspectionStatus="Copied "+inspectionKind.toLowerCase()+" inspection into its editor selection";
	}

	private void selectMode(Mode selected){mode=selected;coordinateFocus=0;replaceFocusedText=false;mc.setWorldEditorNavigateClickTeleport(mode==Mode.NAVIGATE&&clickTeleportPreferred);}
	private void setSceneryId(int id){sceneryId=Math.max(0,Math.min(id,EntityHandler.objectCount()-1));sceneryIdText=String.valueOf(sceneryId);}
	private void setNpcId(int id){npcId=Math.max(0,Math.min(id,EntityHandler.npcs.size()-1));npcIdText=String.valueOf(npcId);}
	private void setNpcRadius(int radius){npcRadius=Math.max(0,Math.min(radius,64));npcRadiusText=String.valueOf(npcRadius);}
	private void setTerrainElevation(int value){terrainElevation=rawByte(value);terrainElevationText=String.valueOf(terrainElevation);}
	private void setTerrainFloorColor(int value){terrainFloorColor=rawByte(value);terrainFloorColorText=String.valueOf(terrainFloorColor);}
	private void setTerrainFloorTexture(int value){terrainFloorTexture=rawByte(value);terrainFloorTextureText=String.valueOf(terrainFloorTexture);}
	private void seedTerrain(int elevation,int color,int texture){setTerrainElevation(elevation);setTerrainFloorColor(color);setTerrainFloorTexture(texture);}
	private static int rawByte(int value){return Math.max(0,Math.min(value,255));}
	private void teleportToFields(){
		try{int x=Integer.parseInt(teleportX),y=Integer.parseInt(teleportY);if(x<0||x>32767||y<0||y>32767)throw new NumberFormatException();
			recordWorldClick(x,y);mc.worldEditorTeleport(x,y);
		}catch(NumberFormatException e){inspectionStatus="Coordinates must be whole numbers from 0 to 32767";inspectionDetails=new String[0];}
	}
	private void send(int type,int x,int y,int plane,int id,int direction,int subtype){
		if(!isEditorOpen())return;mc.packetHandler.getClientStream().newPacket(152);mc.packetHandler.getClientStream().bufferBits.putByte(type);
		mc.packetHandler.getClientStream().bufferBits.putLong(sessionId);mc.packetHandler.getClientStream().bufferBits.putInt(nextSequence);
		if(type==2){mc.packetHandler.getClientStream().bufferBits.putShort(x);mc.packetHandler.getClientStream().bufferBits.putShort(y);mc.packetHandler.getClientStream().bufferBits.putByte(plane);mc.packetHandler.getClientStream().bufferBits.putByte(subtype);}
		else if(type==3){mc.packetHandler.getClientStream().bufferBits.putShort(x);mc.packetHandler.getClientStream().bufferBits.putShort(y);mc.packetHandler.getClientStream().bufferBits.putByte(plane);mc.packetHandler.getClientStream().bufferBits.putShort(id);mc.packetHandler.getClientStream().bufferBits.putByte(direction);mc.packetHandler.getClientStream().bufferBits.putByte(subtype);}
		else if(type==4)mc.packetHandler.getClientStream().bufferBits.putShort(id);mc.packetHandler.getClientStream().finishPacket();
	}

	private boolean handleKey(char c,int key){
		if(coordinateFocus==0)return false;
		String value=focusedText();
		if(key==8){if(replaceFocusedText)value="";else if(value.length()>0)value=value.substring(0,value.length()-1);replaceFocusedText=false;}
		else if(key==9){coordinateFocus=coordinateFocus==1?2:coordinateFocus==2?1:coordinateFocus;replaceFocusedText=true;return true;}
		else if((key==10||key==13)&&!value.isEmpty()){applyFocusedValue(value);return true;}
		else if(c>='0'&&c<='9'&&(replaceFocusedText||value.length()<5)){value=replaceFocusedText?String.valueOf(c):value+c;replaceFocusedText=false;}
		else return true;
		setFocusedText(value);return true;
	}
	private void applyFocusedValue(String value){
		try{int parsed=Integer.parseInt(value);if(coordinateFocus==1||coordinateFocus==2){teleportToFields();return;}if(coordinateFocus==3)setSceneryId(parsed);else if(coordinateFocus==4)setNpcId(parsed);else if(coordinateFocus==5)setNpcRadius(parsed);else if(coordinateFocus==6)setTerrainElevation(parsed);else if(coordinateFocus==7)setTerrainFloorColor(parsed);else setTerrainFloorTexture(parsed);}
		catch(NumberFormatException ignored){}coordinateFocus=0;
	}
	private String focusedText(){switch(coordinateFocus){case 1:return teleportX;case 2:return teleportY;case 3:return sceneryIdText;case 4:return npcIdText;case 5:return npcRadiusText;case 6:return terrainElevationText;case 7:return terrainFloorColorText;default:return terrainFloorTextureText;}}
	private void setFocusedText(String value){switch(coordinateFocus){case 1:teleportX=value;break;case 2:teleportY=value;break;case 3:sceneryIdText=value;break;case 4:npcIdText=value;break;case 5:npcRadiusText=value;break;case 6:terrainElevationText=value;break;case 7:terrainFloorColorText=value;break;default:terrainFloorTextureText=value;}}
	private void focusNumber(int focus){coordinateFocus=focus;replaceFocusedText=true;}
	private boolean handleMouse(int mx,int my,int down,int click){
		if(!isVisible())return false;int rx=mx-getX(),ry=my-getY();
		if(down==1&&ry>=0&&ry<24){if(dragX<0){dragX=rx;dragY=ry;}else setLocation(Math.max(0,mx-dragX),Math.max(0,my-dragY));}else{dragX=dragY=-1;}
		if(click==1){
			if(rx>=365&&ry<24){mc.setWorldEditorNavigateClickTeleport(false);send(1,0,0,0,0,0,0);setVisible(false);return true;}
			if(ry>=30&&ry<50){selectMode(Mode.values()[Math.min(4,Math.max(0,rx/78))]);return true;}
			if(mode==Mode.NAVIGATE){
				if(ry>=150&&ry<172){clickTeleportPreferred=!clickTeleportPreferred;mc.setWorldEditorNavigateClickTeleport(clickTeleportPreferred);return true;}
				if(ry>=197&&ry<221&&rx>=45&&rx<145){focusNumber(1);return true;}
				if(ry>=197&&ry<221&&rx>=180&&rx<280){focusNumber(2);return true;}
				if(ry>=197&&ry<221&&rx>=295&&rx<375){coordinateFocus=0;teleportToFields();return true;}
			}
			if(mode==Mode.INSPECT&&ry>=276&&ry<300&&rx>=10&&rx<175&&!inspectionKind.isEmpty()){copyInspected();return true;}
			if(mode==Mode.TERRAIN){
				if(ry>=82&&ry<106){if(rx>=10&&rx<30)paintElevation=!paintElevation;else if(rx>=150&&rx<178)setTerrainElevation(terrainElevation-1);else if(rx>=185&&rx<265)focusNumber(6);else if(rx>=272&&rx<300)setTerrainElevation(terrainElevation+1);return true;}
				if(ry>=122&&ry<146){if(rx>=10&&rx<30)paintFloorColor=!paintFloorColor;else if(rx>=150&&rx<178)setTerrainFloorColor(terrainFloorColor-1);else if(rx>=185&&rx<265)focusNumber(7);else if(rx>=272&&rx<300)setTerrainFloorColor(terrainFloorColor+1);return true;}
				if(ry>=162&&ry<186){if(rx>=10&&rx<30)paintFloorTexture=!paintFloorTexture;else if(rx>=150&&rx<178)setTerrainFloorTexture(terrainFloorTexture-1);else if(rx>=185&&rx<265)focusNumber(8);else if(rx>=272&&rx<300)setTerrainFloorTexture(terrainFloorTexture+1);return true;}
				if(ry>=194&&ry<218){if(rx>=80&&rx<140)terrainBrushSize=1;else if(rx>=147&&rx<207)terrainBrushSize=3;return true;}
			}
			if(mode==Mode.SCENERY){
				if(ry>=86&&ry<110&&rx>=10&&rx<38){setSceneryId(sceneryId-1);return true;}
				if(ry>=86&&ry<110&&rx>=45&&rx<125){focusNumber(3);return true;}
				if(ry>=86&&ry<110&&rx>=132&&rx<160){setSceneryId(sceneryId+1);return true;}
				if(ry>=145&&ry<169){if(rx>=10&&rx<105)sceneryTool=SceneryTool.PLACE;else if(rx>=112&&rx<207)sceneryTool=SceneryTool.ROTATE;else if(rx>=214&&rx<309)sceneryTool=SceneryTool.REMOVE;return true;}
				if(ry>=276&&ry<300&&rx>=10&&rx<175){mc.sendCommandString("saveworldedits");return true;}
			}
			if(mode==Mode.NPC){
				if(ry>=86&&ry<110&&rx>=10&&rx<38){setNpcId(npcId-1);return true;}
				if(ry>=86&&ry<110&&rx>=45&&rx<125){focusNumber(4);return true;}
				if(ry>=86&&ry<110&&rx>=132&&rx<160){setNpcId(npcId+1);return true;}
				if(ry>=145&&ry<169&&rx>=10&&rx<38){setNpcRadius(npcRadius-1);return true;}
				if(ry>=145&&ry<169&&rx>=45&&rx<125){focusNumber(5);return true;}
				if(ry>=145&&ry<169&&rx>=132&&rx<160){setNpcRadius(npcRadius+1);return true;}
				if(ry>=190&&ry<214){if(rx>=10&&rx<105)npcTool=NpcTool.PLACE;else if(rx>=112&&rx<207)npcTool=NpcTool.REMOVE;return true;}
				if(ry>=276&&ry<300&&rx>=10&&rx<175){mc.sendCommandString("saveworldedits");return true;}
			}
			coordinateFocus=0;
		}
		return rx>=0&&ry>=0&&rx<=390&&ry<=330;
	}

	@Override public void render(){
		if(!isVisible()||Config.isAndroid())return;int x=getX(),y=getY();
		graphics().drawBoxAlpha(x,y,390,330,0x24190c,235);graphics().drawBoxBorder(x,390,y,330,0);graphics().drawBoxAlpha(x,y,390,24,0x4a3620,255);
		graphics().drawString("World Editor",x+8,y+17,0xffff00,2);graphics().drawString("X",x+372,y+17,0xffffff,2);
		for(int i=0;i<TABS.length;i++){graphics().drawBoxAlpha(x+i*78,y+30,77,20,mode.ordinal()==i?0x6b8e23:0x333333,220);graphics().drawString(TABS[i],x+i*78+6,y+44,0xffffff,2);}
		if(mode==Mode.NAVIGATE)renderNavigate(x,y);else if(mode==Mode.INSPECT)renderInspect(x,y);else if(mode==Mode.TERRAIN)renderTerrain(x,y);else if(mode==Mode.SCENERY)renderScenery(x,y);else renderNpc(x,y);
		graphics().drawString("Mode: "+mode+" | session sequence "+nextSequence,x+10,y+321,0xbdbdbd,1);
	}
	private void renderNavigate(int x,int y){
		int px=mc.getEditorPlayerWorldX(),py=mc.getEditorPlayerWorldY();
		graphics().drawString("Navigation",x+10,y+70,0xffff00,2);
		graphics().drawString("Player: "+px+","+py+"  plane "+Math.floorDiv(py,944),x+10,y+91,0xffffff,2);
		graphics().drawString("Last clicked tile: "+point(lastClickedX,lastClickedY),x+10,y+111,0xffffff,2);
		graphics().drawString("Brush: inactive at "+point(brushX,brushY),x+10,y+131,0xbdbdbd,2);
		checkbox(x+10,y+150,clickTeleportPreferred,"Click teleport (Navigate only)");
		graphics().drawString("Teleport to coordinates",x+10,y+188,0xffff00,2);
		graphics().drawString("X",x+28,y+214,0xffffff,2);textField(x+45,y+197,100,teleportX,coordinateFocus==1);
		graphics().drawString("Y",x+163,y+214,0xffffff,2);textField(x+180,y+197,100,teleportY,coordinateFocus==2);button(x+295,y+197,80,"Teleport");
		graphics().drawString("Navigate uses movement options; brush/edit actions are off.",x+10,y+244,0xff981f,1);
	}
	private void renderInspect(int x,int y){
		graphics().drawString(inspectionStatus,x+10,y+70,0xffff00,2);int line=y+89;
		for(String s:inspectionDetails){if(line>y+242)break;graphics().drawString(s,x+10,line,0xffffff,2);line+=17;}
		graphics().drawString("Right-click targets to inspect or copy authoritative data.",x+10,y+263,0xff981f,1);
		button(x+10,y+276,165,inspectionKind.isEmpty()?"Copy inspected (empty)":"Copy inspected");
	}
	private void renderTerrain(int x,int y){
		graphics().drawString("Terrain paint",x+10,y+70,0xffff00,2);
		terrainField(x,y+82,"Elevation",paintElevation,terrainElevationText,coordinateFocus==6);
		terrainField(x,y+122,"Floor Color",paintFloorColor,terrainFloorColorText,coordinateFocus==7);
		terrainField(x,y+162,"Floor Texture",paintFloorTexture,terrainFloorTextureText,coordinateFocus==8);
		graphics().drawString("Brush",x+10,y+211,0xffffff,2);toolButton(x+80,y+194,60,"1x1",terrainBrushSize==1);toolButton(x+147,y+194,60,"3x3",terrainBrushSize==3);
		graphics().drawString(floorTextureDescription(),x+10,y+232,0xbdbdbd,1);
		graphics().drawString("Click terrain to center the "+terrainBrushSize+"x"+terrainBrushSize+" brush at "+point(brushX,brushY)+".",x+10,y+252,0xffffff,2);
		graphics().drawString("Copy inspected fills values; checked fields are painted.",x+10,y+272,0xff981f,1);
		graphics().drawString("Unsaved server draft; saving and undo remain disabled.",x+10,y+290,0xff981f,1);
		graphics().drawString(inspectionStatus,x+10,y+307,0xbdbdbd,1);
	}
	private void terrainField(int x,int y,String label,boolean enabled,String value,boolean focused){checkbox(x+10,y,enabled,label);button(x+150,y,28,"-");textField(x+185,y,80,value,focused);button(x+272,y,28,"+");}
	private String floorTextureDescription(){
		if(terrainFloorTexture==0)return "Floor Texture 0: none (base Floor Color is visible).";
		if(terrainFloorTexture==250)return "Floor Texture 250: bridge transition sentinel.";
		try{return "Floor Texture "+terrainFloorTexture+": "+(EntityHandler.getTileDef(terrainFloorTexture-1).getObjectType()!=0?"blocking":"walkable")+" definition.";}catch(Exception e){return "Floor Texture "+terrainFloorTexture+": undefined and will be rejected.";}
	}
	private void renderScenery(int x,int y){
		graphics().drawString("Scenery editing",x+10,y+70,0xffff00,2);
		button(x+10,y+86,28,"-");textField(x+45,y+86,80,sceneryIdText,coordinateFocus==3);button(x+132,y+86,28,"+");
		graphics().drawString(sceneryName(),x+175,y+103,0xffffff,2);
		toolButton(x+10,y+145,95,"Place",sceneryTool==SceneryTool.PLACE);toolButton(x+112,y+145,95,"Rotate",sceneryTool==SceneryTool.ROTATE);toolButton(x+214,y+145,95,"Remove",sceneryTool==SceneryTool.REMOVE);
		graphics().drawString(sceneryTool==SceneryTool.PLACE?"Click terrain to place one object.":"Click an existing scenery object to "+sceneryTool.name().toLowerCase()+" it.",x+10,y+190,0xffffff,2);
		graphics().drawString("Copying scenery selects its ID. Boundaries remain inspection-only.",x+10,y+218,0xff981f,1);
		button(x+10,y+276,165,"Save queued edits");
	}
	private void renderNpc(int x,int y){
		graphics().drawString("NPC editing",x+10,y+70,0xffff00,2);
		button(x+10,y+86,28,"-");textField(x+45,y+86,80,npcIdText,coordinateFocus==4);button(x+132,y+86,28,"+");
		graphics().drawString(npcName(),x+175,y+103,0xffffff,2);
		graphics().drawString("Roam radius",x+10,y+137,0xffffff,2);button(x+10,y+145,28,"-");textField(x+45,y+145,80,npcRadiusText,coordinateFocus==5);button(x+132,y+145,28,"+");
		toolButton(x+10,y+190,95,"Place",npcTool==NpcTool.PLACE);toolButton(x+112,y+190,95,"Remove",npcTool==NpcTool.REMOVE);
		graphics().drawString(npcTool==NpcTool.PLACE?"Click terrain to place one NPC.":"Click an existing NPC to remove it.",x+10,y+235,0xffffff,2);
		button(x+10,y+276,165,"Save queued edits");
	}
	private void toolButton(int x,int y,int w,String text,boolean active){graphics().drawBoxAlpha(x,y,w,24,active?0x6b8e23:0x333333,220);graphics().drawBoxBorder(x,w,y,24,0);graphics().drawString(text,x+6,y+17,0xffffff,2);}
	private String sceneryName(){try{return EntityHandler.getObjectDef(sceneryId).getName();}catch(Exception e){return "Unknown scenery";}}
	private String npcName(){try{return EntityHandler.getNpcDef(npcId).getName();}catch(Exception e){return "Unknown NPC";}}
	private void textField(int x,int y,int w,String text,boolean focused){graphics().drawBoxAlpha(x,y,w,24,focused?0x6580b7:0x222222,240);graphics().drawBoxBorder(x,w,y,24,0);graphics().drawString(text+(focused?"*":""),x+6,y+17,0xffffff,2);}
	private void checkbox(int x,int y,boolean checked,String text){graphics().drawBoxAlpha(x,y,18,18,checked?0x6b8e23:0x333333,255);graphics().drawBoxBorder(x,18,y,18,0);if(checked)graphics().drawString("X",x+5,y+14,0xffffff,2);graphics().drawString(text,x+26,y+14,0xffffff,2);}
	private void button(int x,int y,int w,String text){graphics().drawBoxAlpha(x,y,w,24,0x333333,220);graphics().drawBoxBorder(x,w,y,24,0);graphics().drawString(text,x+6,y+17,0xffffff,2);}
	private static String point(int x,int y){return x<0||y<0?"not set":x+","+y;}
	private static int diagonalDefinitionId(int v){if(v>0&&v<12000)return v-1;if(v>12000&&v<24000)return v-12001;return -1;}
	private static String diagonalRotation(int v){return v>12000&&v<24000?"rotated":v>0&&v<12000?"not rotated":"none";}
	private static String wall(int id,String name){return id<0?"none":"#"+id+" ("+name+")";}
	private static String planeName(int plane){switch(plane){case 0:return "Surface";case 1:return "First floor";case 2:return "Second floor";case 3:return "Underground";default:return "Unknown";}}
	private static int valueAfter(String text,String marker){if(text==null)return -1;int at=text.indexOf(marker);if(at<0)return -1;at+=marker.length();int end=at;while(end<text.length()&&Character.isDigit(text.charAt(end)))end++;try{return Integer.parseInt(text.substring(at,end));}catch(Exception e){return -1;}}
	private static String join(String[] lines){StringBuilder b=new StringBuilder();for(String line:lines)b.append(line).append(' ');return b.toString();}
	private static String[] wrap(String s,int width){if(s==null||s.isEmpty())return new String[0];java.util.List<String> lines=new java.util.ArrayList<String>();while(s.length()>width){int p=s.lastIndexOf(' ',width);if(p<1)p=width;lines.add(s.substring(0,p));s=s.substring(p).trim();}lines.add(s);return lines.toArray(new String[lines.size()]);}
}
