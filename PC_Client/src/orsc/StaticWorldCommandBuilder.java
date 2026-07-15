package orsc;

import orsc.graphics.three.Renderer3DModelKind;

import java.util.HashSet;
import java.util.Set;

final class StaticWorldCommandBuilder {
	private final Renderer3DModelKind modelKind;
	private final Set<Long> faceKeys = new HashSet<Long>();
	private int triangleCount;
	private int minLegacyDrawOrder = Integer.MAX_VALUE;
	private int maxLegacyDrawOrder = Integer.MIN_VALUE;

	StaticWorldCommandBuilder(Renderer3DModelKind modelKind) {
		this.modelKind = modelKind;
	}

	void addTriangle(long faceKey, int legacyDrawOrder) {
		faceKeys.add(faceKey);
		triangleCount++;
		minLegacyDrawOrder = Math.min(minLegacyDrawOrder, legacyDrawOrder);
		maxLegacyDrawOrder = Math.max(maxLegacyDrawOrder, legacyDrawOrder);
	}

	StaticWorldCommand build() {
		return new StaticWorldCommand(
			modelKind,
			faceKeys,
			triangleCount,
			minLegacyDrawOrder,
			maxLegacyDrawOrder);
	}
}
