package orsc;

final class ResidentChunkReadiness {
	final boolean canReplace;
	final String reason;
	final int drawableTerrainBatches;
	final int drawableWallBatches;
	final int drawableRoofBatches;

	ResidentChunkReadiness(
		boolean canReplace,
		String reason,
		int drawableTerrainBatches,
		int drawableWallBatches,
		int drawableRoofBatches) {
		this.canReplace = canReplace;
		this.reason = reason == null || reason.trim().isEmpty() ? "unknown" : reason;
		this.drawableTerrainBatches = Math.max(0, drawableTerrainBatches);
		this.drawableWallBatches = Math.max(0, drawableWallBatches);
		this.drawableRoofBatches = Math.max(0, drawableRoofBatches);
	}

	static ResidentChunkReadiness notRequested(String reason) {
		return new ResidentChunkReadiness(false, reason, 0, 0, 0);
	}
}
