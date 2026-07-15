package orsc;

import orsc.graphics.three.Renderer3DModelKind;

final class EntitySpriteOccluderFaceStats {
	final Renderer3DModelKind kind;
	final int faceId;
	final int modelIndex;
	int pixels;
	int representativeLegacyDrawOrder = Integer.MIN_VALUE;
	int representativeDepth = Integer.MIN_VALUE;

	EntitySpriteOccluderFaceStats(Renderer3DModelKind kind, int faceId, int modelIndex) {
		this.kind = kind;
		this.faceId = faceId;
		this.modelIndex = modelIndex;
	}

	void record(int legacyDrawOrder, int depth) {
		pixels++;
		if (legacyDrawOrder >= 0) {
			representativeLegacyDrawOrder = legacyDrawOrder;
		}
		if (depth != Integer.MAX_VALUE) {
			representativeDepth = depth;
		}
	}
}
