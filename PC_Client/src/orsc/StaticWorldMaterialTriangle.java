package orsc;

import orsc.graphics.three.Renderer3DModelKind;

final class StaticWorldMaterialTriangle {
	final int triangleIndex;
	final StaticWorldMaterialPass materialPass;
	final Renderer3DModelKind modelKind;
	final int modelIndex;
	final int faceId;
	final int textureId;
	final boolean textureHasTransparency;

	StaticWorldMaterialTriangle(
		int triangleIndex,
		StaticWorldMaterialPass materialPass,
		Renderer3DModelKind modelKind,
		int modelIndex,
		int faceId,
		int textureId,
		boolean textureHasTransparency) {
		this.triangleIndex = triangleIndex;
		this.materialPass = materialPass;
		this.modelKind = modelKind;
		this.modelIndex = modelIndex;
		this.faceId = faceId;
		this.textureId = textureId;
		this.textureHasTransparency = textureHasTransparency;
	}
}
