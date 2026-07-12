package com.openrsc.interfaces.misc;

import com.openrsc.interfaces.InputListener;
import com.openrsc.interfaces.NCustomComponent;
import orsc.Config;
import orsc.mudclient;

/** Desktop-only world editor foundation. World mutation controls remain absent. */
public final class WorldEditorInterface extends NCustomComponent {
	public enum Mode { NAVIGATE, INSPECT, TERRAIN, SCENERY, NPC }
	private static final String[] TABS={"Navigate","Inspect","Terrain","Scenery","NPC"};
	private final mudclient mc;
	private Mode mode=Mode.NAVIGATE;
	private long sessionId;
	private int nextSequence;
	private String inspectionStatus="Nothing inspected yet";
	private String[] inspectionDetails=new String[0];
	private int[] copiedTerrainFields;
	private int lastClickedX=-1,lastClickedY=-1,brushX=-1,brushY=-1;
	private String teleportX="",teleportY="";
	private int coordinateFocus=0;
	private boolean clickTeleportPreferred=false;
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
	public void setSequence(int sequence){nextSequence=sequence;}
	public void recordWorldClick(int x,int y){lastClickedX=x;lastClickedY=y;if(mode!=Mode.NAVIGATE){brushX=x;brushY=y;}}
	public void showInfo(String text){inspectionStatus="Authoritative inspection";inspectionDetails=wrap(text,58);}
	public void showError(String text){inspectionStatus="Server rejected request";inspectionDetails=wrap(text,58);}
	public void showTerrain(int sequence,int x,int y,int plane,int sx,int sy,int lx,int ly,int elev,int texture,int overlay,int roof,int hwall,int vwall,int diag,int collision,boolean projectile,boolean copied,String definitions){
		nextSequence=sequence;inspectionStatus=copied?"Terrain fields copied locally (painting disabled)":"Authoritative terrain snapshot";
		if(copied)copiedTerrainFields=new int[]{elev,texture,overlay,roof,hwall,vwall,diag};
		java.util.List<String> lines=new java.util.ArrayList<String>();java.util.Collections.addAll(lines,
			"World: "+x+","+y+" plane "+plane,"Sector: "+sx+","+sy+" local "+lx+","+ly,
			"Elevation "+elev+"  Ground texture "+texture,"Overlay "+overlay+"  Roof "+roof,
			"Horizontal wall "+hwall+"  Vertical wall "+vwall,"Diagonal raw "+diag+"  "+diagonal(diag),
			"Collision 0x"+Integer.toHexString(collision)+"  Projectile "+projectile);
		java.util.Collections.addAll(lines,wrap(definitions,58));inspectionDetails=lines.toArray(new String[lines.size()]);
	}
	public int[] getCopiedTerrainFields(){return copiedTerrainFields==null?null:copiedTerrainFields.clone();}
	public void inspectTerrain(int worldX,int worldY,boolean copy){recordWorldClick(worldX,worldY);send(2,worldX,worldY,Math.floorDiv(worldY,944),0,0,copy?1:0);}
	public void inspectObject(int worldX,int worldY,int id,int direction,int type){recordWorldClick(worldX,worldY);send(3,worldX,worldY,Math.floorDiv(worldY,944),id,direction,type);}
	public void inspectNpc(int serverIndex){send(4,0,0,0,serverIndex,0,0);}

	private void selectMode(Mode selected){mode=selected;coordinateFocus=0;mc.setWorldEditorNavigateClickTeleport(mode==Mode.NAVIGATE&&clickTeleportPreferred);}
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
		if(!isNavigating()||coordinateFocus==0)return false;
		String value=coordinateFocus==1?teleportX:teleportY;
		if(key==8&&value.length()>0)value=value.substring(0,value.length()-1);
		else if(key==9){coordinateFocus=coordinateFocus==1?2:1;return true;}
		else if((key==10||key==13)&&!value.isEmpty()){teleportToFields();return true;}
		else if(c>='0'&&c<='9'&&value.length()<5)value+=c;
		else return true;
		if(coordinateFocus==1)teleportX=value;else teleportY=value;return true;
	}
	private boolean handleMouse(int mx,int my,int down,int click){
		if(!isVisible())return false;int rx=mx-getX(),ry=my-getY();
		if(down==1&&ry>=0&&ry<24){if(dragX<0){dragX=rx;dragY=ry;}else setLocation(Math.max(0,mx-dragX),Math.max(0,my-dragY));}else{dragX=dragY=-1;}
		if(click==1){
			if(rx>=365&&ry<24){mc.setWorldEditorNavigateClickTeleport(false);send(1,0,0,0,0,0,0);setVisible(false);return true;}
			if(ry>=30&&ry<50){selectMode(Mode.values()[Math.min(4,Math.max(0,rx/78))]);return true;}
			if(mode==Mode.NAVIGATE){
				if(ry>=150&&ry<172){clickTeleportPreferred=!clickTeleportPreferred;mc.setWorldEditorNavigateClickTeleport(clickTeleportPreferred);return true;}
				if(ry>=197&&ry<221&&rx>=45&&rx<145){coordinateFocus=1;return true;}
				if(ry>=197&&ry<221&&rx>=180&&rx<280){coordinateFocus=2;return true;}
				if(ry>=197&&ry<221&&rx>=295&&rx<375){coordinateFocus=0;teleportToFields();return true;}
			}
			coordinateFocus=0;
		}
		return rx>=0&&ry>=0&&rx<=390&&ry<=330;
	}

	@Override public void render(){
		if(!isVisible()||Config.isAndroid())return;int x=getX(),y=getY();
		graphics().drawBoxAlpha(x,y,390,330,0x24190c,235);graphics().drawBoxBorder(x,390,y,330,0);graphics().drawBoxAlpha(x,y,390,24,0x4a3620,255);
		graphics().drawString("World Editor - READ ONLY",x+8,y+17,0xffff00,2);graphics().drawString("X",x+372,y+17,0xffffff,2);
		for(int i=0;i<TABS.length;i++){graphics().drawBoxAlpha(x+i*78,y+30,77,20,mode.ordinal()==i?0x6b8e23:0x333333,220);graphics().drawString(TABS[i],x+i*78+6,y+44,0xffffff,2);}
		if(mode==Mode.NAVIGATE)renderNavigate(x,y);else if(mode==Mode.INSPECT)renderInspect(x,y);else renderFutureMode(x,y);
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
		for(String s:inspectionDetails){if(line>y+265)break;graphics().drawString(s,x+10,line,0xffffff,2);line+=17;}
		graphics().drawString("Right-click terrain, scenery, boundaries, or NPCs to inspect.",x+10,y+285,0xff981f,1);
	}
	private void renderFutureMode(int x,int y){
		graphics().drawString(TABS[mode.ordinal()]+" editing",x+10,y+70,0xffff00,2);
		graphics().drawString("Reserved for a later editor phase.",x+10,y+96,0xffffff,2);
		graphics().drawString("No editable fields or mutation controls are enabled.",x+10,y+118,0xff981f,1);
	}
	private void textField(int x,int y,int w,String text,boolean focused){graphics().drawBoxAlpha(x,y,w,24,focused?0x6580b7:0x222222,240);graphics().drawBoxBorder(x,w,y,24,0);graphics().drawString(text+(focused?"*":""),x+6,y+17,0xffffff,2);}
	private void checkbox(int x,int y,boolean checked,String text){graphics().drawBoxAlpha(x,y,18,18,checked?0x6b8e23:0x333333,255);graphics().drawBoxBorder(x,18,y,18,0);if(checked)graphics().drawString("X",x+5,y+14,0xffffff,2);graphics().drawString(text,x+26,y+14,0xffffff,2);}
	private void button(int x,int y,int w,String text){graphics().drawBoxAlpha(x,y,w,24,0x333333,220);graphics().drawBoxBorder(x,w,y,24,0);graphics().drawString(text,x+6,y+17,0xffffff,2);}
	private static String point(int x,int y){return x<0||y<0?"not set":x+","+y;}
	private static String diagonal(int v){if(v>0&&v<12000)return "def "+(v-1)+" NW-SE";if(v>12000&&v<24000)return "def "+(v-12001)+" NE-SW";return "none";}
	private static String[] wrap(String s,int width){if(s==null||s.isEmpty())return new String[0];java.util.List<String> lines=new java.util.ArrayList<String>();while(s.length()>width){int p=s.lastIndexOf(' ',width);if(p<1)p=width;lines.add(s.substring(0,p));s=s.substring(p).trim();}lines.add(s);return lines.toArray(new String[lines.size()]);}
}
