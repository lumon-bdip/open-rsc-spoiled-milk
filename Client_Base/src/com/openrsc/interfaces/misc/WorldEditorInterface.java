package com.openrsc.interfaces.misc;

import com.openrsc.interfaces.InputListener;
import com.openrsc.interfaces.NCustomComponent;
import orsc.Config;
import orsc.mudclient;

/** Desktop-only, read-only editor shell. There are deliberately no mutation controls. */
public final class WorldEditorInterface extends NCustomComponent {
	public enum Mode { NAVIGATE, INSPECT, TERRAIN, SCENERY, NPC }
	private static final String[] TABS={"Navigate","Inspect","Terrain","Scenery","NPC"};
	private final mudclient mc;
	private Mode mode=Mode.INSPECT;
	private long sessionId; private int nextSequence;
	private String status="No selection"; private String[] details=new String[0];
	private int dragX=-1, dragY=-1;

	public WorldEditorInterface(mudclient client) {
		super(client); mc=client; setLocation(12,48); setSize(390,286); setVisible(false); setIsOverlay(true);
		setInputListener(new InputListener(){ @Override public boolean onMouseDown(int mx,int my,int down,int click){return handleMouse(mx,my,down,click);} });
	}
	public void open(long id,int sequence){if(Config.isAndroid())return;sessionId=id;nextSequence=sequence;status="Read-only session active";details=new String[0];setVisible(true);}
	public void closeFromServer(){setVisible(false);sessionId=0;}
	public boolean isEditorOpen(){return isVisible()&&sessionId!=0;}
	public boolean isInspecting(){return isEditorOpen()&&mode!=Mode.NAVIGATE;}
	public void setSequence(int sequence){nextSequence=sequence;}
	public void showInfo(String text){status="Authoritative inspection";details=wrap(text,58);}
	public void showError(String text){status="Server rejected request";details=wrap(text,58);}
	public void showTerrain(int sequence,int x,int y,int plane,int sx,int sy,int lx,int ly,int elev,int texture,int overlay,int roof,int hwall,int vwall,int diag,int collision,boolean projectile,boolean copied){
		nextSequence=sequence;status=copied?"Terrain fields copied locally (painting disabled)":"Authoritative terrain snapshot";
		details=new String[]{"World: "+x+","+y+" plane "+plane,"Sector: "+sx+","+sy+" local "+lx+","+ly,
			"Elevation "+elev+"  Ground texture "+texture,"Overlay "+overlay+"  Roof "+roof,
			"Horizontal wall "+hwall+"  Vertical wall "+vwall,"Diagonal raw "+diag+"  "+diagonal(diag),
			"Collision 0x"+Integer.toHexString(collision)+"  Projectile "+projectile};
	}
	public void inspectTerrain(int worldX,int worldY,boolean copy){send(2,worldX,worldY,Math.floorDiv(worldY,944),0,0,copy?1:0);}
	public void inspectObject(int worldX,int worldY,int id,int direction,int type){send(3,worldX,worldY,Math.floorDiv(worldY,944),id,direction,type);}
	public void inspectNpc(int serverIndex){send(4,0,0,0,serverIndex,0,0);}
	private void send(int type,int x,int y,int plane,int id,int direction,int subtype){
		if(!isEditorOpen())return; mc.packetHandler.getClientStream().newPacket(152); mc.packetHandler.getClientStream().bufferBits.putByte(type);
		mc.packetHandler.getClientStream().bufferBits.putLong(sessionId);mc.packetHandler.getClientStream().bufferBits.putInt(nextSequence);
		if(type==2){mc.packetHandler.getClientStream().bufferBits.putShort(x);mc.packetHandler.getClientStream().bufferBits.putShort(y);mc.packetHandler.getClientStream().bufferBits.putByte(plane);mc.packetHandler.getClientStream().bufferBits.putByte(subtype);}
		else if(type==3){mc.packetHandler.getClientStream().bufferBits.putShort(x);mc.packetHandler.getClientStream().bufferBits.putShort(y);mc.packetHandler.getClientStream().bufferBits.putByte(plane);mc.packetHandler.getClientStream().bufferBits.putShort(id);mc.packetHandler.getClientStream().bufferBits.putByte(direction);mc.packetHandler.getClientStream().bufferBits.putByte(subtype);}
		else if(type==4)mc.packetHandler.getClientStream().bufferBits.putShort(id); mc.packetHandler.getClientStream().finishPacket();
	}
	private boolean handleMouse(int mx,int my,int down,int click){
		if(!isVisible())return false;int rx=mx-getX(),ry=my-getY();
		if(down==1&&ry>=0&&ry<24){if(dragX<0){dragX=rx;dragY=ry;}else setLocation(Math.max(0,mx-dragX),Math.max(0,my-dragY));}
		else {dragX=dragY=-1;}
		if(click==1){if(rx>=365&&ry<24){send(1,0,0,0,0,0,0);setVisible(false);return true;}
			if(ry>=30&&ry<50){int tab=Math.min(4,Math.max(0,rx/78));mode=Mode.values()[tab];return true;}
			if(ry>=238&&ry<261&&rx>=10&&rx<180){inspectTerrain(mc.getEditorPlayerWorldX(),mc.getEditorPlayerWorldY(),false);return true;}
			if(ry>=238&&ry<261&&rx>=190&&rx<365){inspectTerrain(mc.getEditorPlayerWorldX(),mc.getEditorPlayerWorldY(),true);return true;}}
		return rx>=0&&ry>=0&&rx<=390&&ry<=286;
	}
	@Override public void render(){
		if(!isVisible()||Config.isAndroid())return;int x=getX(),y=getY();
		graphics().drawBoxAlpha(x,y,390,286,0x24190c,235);graphics().drawBoxBorder(x,390,y,286,0);graphics().drawBoxAlpha(x,y,390,24,0x4a3620,255);
		graphics().drawString("World Editor - READ ONLY",x+8,y+17,0xffff00,2);graphics().drawString("X",x+372,y+17,0xffffff,2);
		for(int i=0;i<TABS.length;i++){graphics().drawBoxAlpha(x+i*78,y+30,77,20,mode.ordinal()==i?0x6b8e23:0x333333,220);graphics().drawString(TABS[i],x+i*78+6,y+44,0xffffff,2);}
		graphics().drawString(status,x+10,y+70,0xffff00,2);int line=y+89;for(String s:details){graphics().drawString(s,x+10,line,0xffffff,2);line+=17;}
		graphics().drawString("Mutation controls unavailable until client/server parity is proven.",x+10,y+223,0xff981f,1);
		button(x+10,y+238,170,"Inspect player tile");button(x+190,y+238,175,"Copy terrain fields");
		graphics().drawString("Mode: "+mode+" | session sequence "+nextSequence,x+10,y+278,0xbdbdbd,1);
	}
	private void button(int x,int y,int w,String text){graphics().drawBoxAlpha(x,y,w,23,0x333333,220);graphics().drawBoxBorder(x,w,y,23,0);graphics().drawString(text,x+6,y+16,0xffffff,2);}
	private static String diagonal(int v){if(v>0&&v<12000)return "def "+(v-1)+" NW-SE";if(v>12000&&v<24000)return "def "+(v-12001)+" NE-SW";return "none";}
	private static String[] wrap(String s,int width){if(s==null)return new String[0];java.util.List<String> lines=new java.util.ArrayList<String>();while(s.length()>width){int p=s.lastIndexOf(' ',width);if(p<1)p=width;lines.add(s.substring(0,p));s=s.substring(p).trim();}lines.add(s);return lines.toArray(new String[lines.size()]);}
}
