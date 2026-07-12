package com.openrsc.server.net.rsc.struct.incoming;
import com.openrsc.server.net.rsc.enums.OpcodeIn;
import com.openrsc.server.net.rsc.struct.AbstractStruct;
public final class WorldEditorRequestStruct extends AbstractStruct<OpcodeIn> {
	public int type, sequence, x, y, plane, entityId, direction, objectType;
	public int fieldMask, elevation, groundTexture, groundOverlay;
	public int roofTexture, horizontalWall, verticalWall, diagonal;
	public long sessionId;
}
