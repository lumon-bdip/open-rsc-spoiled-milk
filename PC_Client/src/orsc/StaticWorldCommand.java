package orsc;

import orsc.graphics.three.Renderer3DModelKind;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class StaticWorldCommand {
	final Renderer3DModelKind modelKind;
	final Set<Long> faceKeys;
	final int triangleCount;
	final int minLegacyDrawOrder;
	final int maxLegacyDrawOrder;

	StaticWorldCommand(
		Renderer3DModelKind modelKind,
		Set<Long> faceKeys,
		int triangleCount,
		int minLegacyDrawOrder,
		int maxLegacyDrawOrder) {
		this.modelKind = modelKind;
		this.faceKeys = Collections.unmodifiableSet(new HashSet<Long>(faceKeys));
		this.triangleCount = triangleCount;
		this.minLegacyDrawOrder = minLegacyDrawOrder;
		this.maxLegacyDrawOrder = maxLegacyDrawOrder;
	}
}
