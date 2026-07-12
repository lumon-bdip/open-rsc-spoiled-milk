package com.openrsc.server.net.rsc.struct.outgoing;
import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;
public final class WorldEditorStruct extends AbstractStruct<OpcodeOut> {
	public int type, protocolVersion=1, sequence, x, y, plane, sectorX, sectorY, localX, localY;
	public int elevation, groundTexture, groundOverlay, roofTexture, horizontalWall, verticalWall, diagonal;
	public int traversalMask, fieldMask; public boolean projectileAllowed, copy;
	public long sessionId; public String message="";
}
