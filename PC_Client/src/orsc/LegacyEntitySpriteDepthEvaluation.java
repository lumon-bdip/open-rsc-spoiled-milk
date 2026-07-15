package orsc;

import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;

final class LegacyEntitySpriteDepthEvaluation {
	final int index;
	final int sequence;
	final String phase;
	final int legacySpriteId;
	final int x;
	final int y;
	final int width;
	final int height;
	final int topX16;
	final int bottomX16;
	final int anchorFaceId;
	final int anchorLegacyDrawOrder;
	final int anchorAverageDepth;
	final int anchorCameraZ;
	final String anchorMatchMode;
	final int anchorMatchScore;
	final int anchorDrawX;
	final int anchorDrawY;
	final int anchorDrawWidth;
	final int anchorDrawHeight;
	final int anchorDeltaX;
	final int anchorDeltaY;
	final int anchorDeltaWidth;
	final int anchorDeltaHeight;
	final int sourcePixels;
	final int visiblePixels;
	final int occludedPixels;
	final int clippedPixels;
	final int outOfBoundsPixels;
	final int terrainOccludedPixels;
	final int wallOccludedPixels;
	final int roofOccludedPixels;
	final int gameObjectOccludedPixels;
	final int wallObjectOccludedPixels;
	final int minOccluderLegacyDrawOrder;
	final int maxOccluderLegacyDrawOrder;
	final int minOccluderDepth;
	final int maxOccluderDepth;
	final String dominantOccluderKind;
	final int dominantOccluderFaceId;
	final int dominantOccluderModelIndex;
	final int dominantOccluderPixels;
	final int dominantOccluderLegacyDrawOrder;
	final int dominantOccluderDepth;
	final boolean fullyOccluded;

	LegacyEntitySpriteDepthEvaluation(
		int index,
		Renderer2DFrame.SpriteCommand command,
		int sourcePixels,
		int visiblePixels,
		int occludedPixels,
		int clippedPixels,
		int outOfBoundsPixels,
		int terrainOccludedPixels,
		int wallOccludedPixels,
		int roofOccludedPixels,
		int gameObjectOccludedPixels,
		int wallObjectOccludedPixels,
		int minOccluderLegacyDrawOrder,
		int maxOccluderLegacyDrawOrder,
		int minOccluderDepth,
		int maxOccluderDepth,
		EntitySpriteOccluderFaceStats dominantOccluderFace,
		WorldSpriteAnchorMatch anchorMatch,
		Renderer3DFrame.SpriteAnchor anchor) {
		this.index = index;
		this.sequence = command.getSequence();
		this.phase = String.valueOf(command.getPhase());
		this.legacySpriteId = command.getLegacySpriteId();
		this.x = command.getX();
		this.y = command.getY();
		this.width = command.getWidth();
		this.height = command.getHeight();
		this.topX16 = command.getTopX16();
		this.bottomX16 = command.getBottomX16();
		this.anchorFaceId = anchor == null ? Integer.MIN_VALUE : anchor.getFaceId();
		this.anchorLegacyDrawOrder = anchor == null ? Integer.MIN_VALUE : anchor.getLegacyDrawOrder();
		this.anchorAverageDepth = anchor == null ? Integer.MIN_VALUE : anchor.getAverageDepth();
		this.anchorCameraZ = anchor == null ? Integer.MIN_VALUE : anchor.getCameraZ();
		this.anchorMatchMode = anchorMatch == null ? "unmatched" : anchorMatch.mode;
		this.anchorMatchScore = anchorMatch == null ? Integer.MAX_VALUE : anchorMatch.score;
		this.anchorDrawX = anchor == null ? Integer.MIN_VALUE : anchor.getDrawX();
		this.anchorDrawY = anchor == null ? Integer.MIN_VALUE : anchor.getDrawY();
		this.anchorDrawWidth = anchor == null ? Integer.MIN_VALUE : anchor.getDrawWidth();
		this.anchorDrawHeight = anchor == null ? Integer.MIN_VALUE : anchor.getDrawHeight();
		this.anchorDeltaX = anchor == null ? Integer.MIN_VALUE : this.x - anchor.getDrawX();
		this.anchorDeltaY = anchor == null ? Integer.MIN_VALUE : this.y - anchor.getDrawY();
		this.anchorDeltaWidth = anchor == null ? Integer.MIN_VALUE : this.width - anchor.getDrawWidth();
		this.anchorDeltaHeight = anchor == null ? Integer.MIN_VALUE : this.height - anchor.getDrawHeight();
		this.sourcePixels = Math.max(0, sourcePixels);
		this.visiblePixels = Math.max(0, visiblePixels);
		this.occludedPixels = Math.max(0, occludedPixels);
		this.clippedPixels = Math.max(0, clippedPixels);
		this.outOfBoundsPixels = Math.max(0, outOfBoundsPixels);
		this.terrainOccludedPixels = Math.max(0, terrainOccludedPixels);
		this.wallOccludedPixels = Math.max(0, wallOccludedPixels);
		this.roofOccludedPixels = Math.max(0, roofOccludedPixels);
		this.gameObjectOccludedPixels = Math.max(0, gameObjectOccludedPixels);
		this.wallObjectOccludedPixels = Math.max(0, wallObjectOccludedPixels);
		this.minOccluderLegacyDrawOrder = minOccluderLegacyDrawOrder;
		this.maxOccluderLegacyDrawOrder = maxOccluderLegacyDrawOrder;
		this.minOccluderDepth = minOccluderDepth;
		this.maxOccluderDepth = maxOccluderDepth;
		this.dominantOccluderKind = dominantOccluderFace == null
			? ""
			: String.valueOf(dominantOccluderFace.kind);
		this.dominantOccluderFaceId = dominantOccluderFace == null ? -1 : dominantOccluderFace.faceId;
		this.dominantOccluderModelIndex =
			dominantOccluderFace == null ? -1 : dominantOccluderFace.modelIndex;
		this.dominantOccluderPixels = dominantOccluderFace == null ? 0 : dominantOccluderFace.pixels;
		this.dominantOccluderLegacyDrawOrder =
			dominantOccluderFace == null ? Integer.MIN_VALUE : dominantOccluderFace.representativeLegacyDrawOrder;
		this.dominantOccluderDepth =
			dominantOccluderFace == null ? Integer.MIN_VALUE : dominantOccluderFace.representativeDepth;
		this.fullyOccluded = this.sourcePixels > 0 && this.visiblePixels == 0;
	}
}
