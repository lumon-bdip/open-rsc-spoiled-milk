package orsc;

final class RemasterShadowInventory {
	static final RemasterShadowInventory EMPTY =
		new RemasterShadowInventory(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	final int receiverChunks;
	final int receiverTriangles;
	final int totalCasters;
	final int wallCasters;
	final int gameObjectCasters;
	final int wallObjectCasters;
	final int outdoorOnlyCasters;
	final int clippingCandidates;
	final int roofedReceivers;
	final int outdoorReceivers;
	final int unknownReceivers;
	final int roofedCasters;
	final int outdoorCasters;
	final int unknownCasters;
	final int sunlightEligibleCasters;
	final int sunlightSuppressedRoofedCasters;
	final int sunlightSuppressedUnknownCasters;

	RemasterShadowInventory(
		int receiverChunks,
		int receiverTriangles,
		int totalCasters,
		int wallCasters,
		int gameObjectCasters,
		int wallObjectCasters,
		int outdoorOnlyCasters,
		int clippingCandidates,
		int roofedReceivers,
		int outdoorReceivers,
		int unknownReceivers,
		int roofedCasters,
		int outdoorCasters,
		int unknownCasters,
		int sunlightEligibleCasters,
		int sunlightSuppressedRoofedCasters,
		int sunlightSuppressedUnknownCasters) {
		this.receiverChunks = receiverChunks;
		this.receiverTriangles = receiverTriangles;
		this.totalCasters = totalCasters;
		this.wallCasters = wallCasters;
		this.gameObjectCasters = gameObjectCasters;
		this.wallObjectCasters = wallObjectCasters;
		this.outdoorOnlyCasters = outdoorOnlyCasters;
		this.clippingCandidates = clippingCandidates;
		this.roofedReceivers = roofedReceivers;
		this.outdoorReceivers = outdoorReceivers;
		this.unknownReceivers = unknownReceivers;
		this.roofedCasters = roofedCasters;
		this.outdoorCasters = outdoorCasters;
		this.unknownCasters = unknownCasters;
		this.sunlightEligibleCasters = sunlightEligibleCasters;
		this.sunlightSuppressedRoofedCasters = sunlightSuppressedRoofedCasters;
		this.sunlightSuppressedUnknownCasters = sunlightSuppressedUnknownCasters;
	}
}
