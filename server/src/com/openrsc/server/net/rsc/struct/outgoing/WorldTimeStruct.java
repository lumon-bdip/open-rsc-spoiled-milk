package com.openrsc.server.net.rsc.struct.outgoing;

import com.openrsc.server.net.rsc.enums.OpcodeOut;
import com.openrsc.server.net.rsc.struct.AbstractStruct;

public class WorldTimeStruct extends AbstractStruct<OpcodeOut> {
	public int cycleMillis;
	public int currentCycleMillis;
	public int rateMultiplier;
}
