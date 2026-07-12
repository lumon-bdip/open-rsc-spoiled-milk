package com.openrsc.server.model.world.region;

import com.openrsc.server.util.rsc.CollisionFlag;

public class TileValue {
	public byte traversalMask = CollisionFlag.FULL_BLOCK;
	public short diagWallVal = 0;
	public byte horizontalWallVal = 0;
	public byte overlay = 0;
	public byte verticalWallVal = 0;
	public byte elevation = 0;
	public boolean projectileAllowed = false;
	public boolean originalProjectileAllowed = false;
	private boolean terrainBlocked = false;
	private int blockingSceneryCount = 0;

	public void setTerrainBlocked(boolean blocked) {
		terrainBlocked=blocked;
		refreshFullBlock();
	}
	public boolean isTerrainBlocked(){return terrainBlocked;}
	public void addBlockingScenery(){blockingSceneryCount++;refreshFullBlock();}
	public void removeBlockingScenery(){if(blockingSceneryCount>0)blockingSceneryCount--;refreshFullBlock();}
	public int getBlockingSceneryCount(){return blockingSceneryCount;}
	private void refreshFullBlock(){
		if(terrainBlocked||blockingSceneryCount>0)traversalMask|=CollisionFlag.FULL_BLOCK_C;
		else traversalMask&=~CollisionFlag.FULL_BLOCK_C;
	}

	@Override
	public String toString() {
		return "TileValue{" +
			"traversalMask=" + traversalMask +
			", diagWallVal=" + diagWallVal +
			", horizontalWallVal=" + horizontalWallVal +
			", overlay=" + overlay +
			", verticalWallVal=" + verticalWallVal +
			", elevation=" + elevation +
			", projectileAllowed=" + projectileAllowed +
			", originalProjectileAllowed=" + originalProjectileAllowed +
			", terrainBlocked=" + terrainBlocked +
			", blockingSceneryCount=" + blockingSceneryCount +
			'}';
	}

	public boolean equals(final TileValue other) {
		return 	this.traversalMask == other.traversalMask &&
				this.diagWallVal == other.diagWallVal &&
				this.horizontalWallVal == other.horizontalWallVal &&
				this.overlay == other.overlay &&
				this.verticalWallVal == other.verticalWallVal &&
				this.elevation == other.elevation &&
				this.projectileAllowed == other.projectileAllowed &&
				this.originalProjectileAllowed == other.originalProjectileAllowed &&
				this.terrainBlocked == other.terrainBlocked &&
				this.blockingSceneryCount == other.blockingSceneryCount;
	}
}
