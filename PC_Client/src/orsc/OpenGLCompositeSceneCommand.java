package orsc;

import com.openrsc.client.model.Sprite;
import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;
import orsc.graphics.three.Renderer3DModelKind;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
 * RENDERER-V2 OWNER: package-local command and diagnostic records for the
 * OpenGL composite scene path. Keep these data shapes out of
 * OpenGLFramePresenter so a real scene-command owner can replace the remaining
 * legacy bridge without another large presenter edit.
 */
final class OpenGLCompositeSceneCommand {
	enum Kind {
		WORLD_SPRITE
	}

	final Kind kind;
	final WorldSpriteCommand worldSpriteCommand;
	final int legacyDrawOrder;
	final int sequence;
	final int minExclusiveOrder;
	final int maxExclusiveOrder;
	final Set<Long> frontOccluderFaceKeys;

	OpenGLCompositeSceneCommand(
		Kind kind,
		WorldSpriteCommand worldSpriteCommand,
		int legacyDrawOrder,
		int sequence,
		int minExclusiveOrder,
		int maxExclusiveOrder,
		Set<Long> frontOccluderFaceKeys) {
		this.kind = kind;
		this.worldSpriteCommand = worldSpriteCommand;
		this.legacyDrawOrder = legacyDrawOrder;
		this.sequence = sequence;
		this.minExclusiveOrder = minExclusiveOrder;
		this.maxExclusiveOrder = maxExclusiveOrder;
		this.frontOccluderFaceKeys = frontOccluderFaceKeys == null
			? Collections.<Long>emptySet()
			: Collections.unmodifiableSet(new HashSet<Long>(frontOccluderFaceKeys));
	}

	static OpenGLCompositeSceneCommand worldSprite(WorldSpriteCommand command) {
		int sequence = command == null || command.command == null ? Integer.MAX_VALUE : command.command.getSequence();
		int legacyDrawOrder = command == null ? Integer.MAX_VALUE : command.legacyDrawOrder;
		return new OpenGLCompositeSceneCommand(
			Kind.WORLD_SPRITE,
			command,
			legacyDrawOrder,
			sequence,
			Integer.MIN_VALUE,
			Integer.MAX_VALUE,
			Collections.<Long>emptySet());
	}
}

final class WorldSpriteMatch {
	final Renderer3DFrame.SpriteAnchor anchor;
	final Renderer2DFrame.SpriteCommand command;

	WorldSpriteMatch(Renderer3DFrame.SpriteAnchor anchor, Renderer2DFrame.SpriteCommand command) {
		this.anchor = anchor;
		this.command = command;
	}
}

final class WorldSpriteCommand {
	final Renderer2DFrame.SpriteCommand command;
	final Renderer3DFrame.SpriteAnchor anchor;
	final WorldSpriteAnchorMatch anchorMatch;
	final int legacyDrawOrder;
	final boolean sourceCropped;
	final boolean skewed;

	WorldSpriteCommand(
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		WorldSpriteAnchorMatch anchorMatch) {
		this.command = command;
		this.anchor = anchor;
		this.anchorMatch = anchorMatch == null ? WorldSpriteAnchorMatch.unmatched() : anchorMatch;
		this.legacyDrawOrder = anchor == null ? Integer.MAX_VALUE : anchor.getLegacyDrawOrder();
		Sprite sprite = command == null ? null : command.getSprite();
		this.sourceCropped = command != null
			&& sprite != null
			&& (command.getSourceX() != 0
				|| command.getSourceY() != 0
				|| command.getSourceWidth() != sprite.getWidth()
				|| command.getSourceHeight() != sprite.getHeight());
		this.skewed = command != null && command.getTopX16() != command.getBottomX16();
	}

	boolean hasAnchor() {
		return anchor != null;
	}
}

final class CompositeWorldSpriteTexture {
	final int x;
	final int y;
	final int width;
	final int height;
	final DynamicTextureData textureData;

	CompositeWorldSpriteTexture(
		int x,
		int y,
		int width,
		int height,
		DynamicTextureData textureData) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.textureData = textureData;
	}
}

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

final class WorldSpriteAnchorMatch {
	final String mode;
	final int score;

	WorldSpriteAnchorMatch(String mode, int score) {
		this.mode = mode;
		this.score = score;
	}

	static WorldSpriteAnchorMatch unmatched() {
		return new WorldSpriteAnchorMatch("unmatched", Integer.MAX_VALUE);
	}
}

final class LegacyEntitySpriteDebugStats {
	final int legacySpriteId;
	int commands;
	int visiblePixels;
	int directPixels;
	int fallbacks;
	int skipped;
	int atlasFull;
	int depthEvaluations;
	int depthVisiblePixels;
	int depthMisses;
	int depthSourcePixels;
	int depthOccludedPixels;
	int depthClippedPixels;
	int depthOutOfBoundsPixels;
	int minAnchorDepth = Integer.MAX_VALUE;
	int maxAnchorDepth = Integer.MIN_VALUE;
	int minX = Integer.MAX_VALUE;
	int minY = Integer.MAX_VALUE;
	int maxX = Integer.MIN_VALUE;
	int maxY = Integer.MIN_VALUE;

	LegacyEntitySpriteDebugStats(int legacySpriteId) {
		this.legacySpriteId = legacySpriteId;
	}

	void record(
		Renderer2DFrame.SpriteCommand command,
		int visiblePixels,
		int directPixels,
		boolean fallback,
		boolean atlasFull,
		boolean skipped) {
		commands++;
		this.visiblePixels += Math.max(0, visiblePixels);
		this.directPixels += Math.max(0, directPixels);
		if (fallback) {
			fallbacks++;
		}
		if (atlasFull) {
			this.atlasFull++;
		}
		if (skipped) {
			this.skipped++;
		}
		minX = Math.min(minX, command.getX());
		minY = Math.min(minY, command.getY());
		maxX = Math.max(maxX, command.getX() + command.getWidth());
		maxY = Math.max(maxY, command.getY() + command.getHeight());
	}

	void recordDepthEvaluation(
		int sourcePixels,
		int visiblePixels,
		int occludedPixels,
		int clippedPixels,
		int outOfBoundsPixels,
		Renderer3DFrame.SpriteAnchor anchor) {
		depthEvaluations++;
		depthVisiblePixels += Math.max(0, visiblePixels);
		depthSourcePixels += Math.max(0, sourcePixels);
		depthOccludedPixels += Math.max(0, occludedPixels);
		depthClippedPixels += Math.max(0, clippedPixels);
		depthOutOfBoundsPixels += Math.max(0, outOfBoundsPixels);
		if (anchor != null) {
			minAnchorDepth = Math.min(minAnchorDepth, Math.min(anchor.getCameraZ(), anchor.getAverageDepth()));
			maxAnchorDepth = Math.max(maxAnchorDepth, Math.max(anchor.getCameraZ(), anchor.getAverageDepth()));
		}
	}

	void recordDepthFallbackMiss(
		int sourcePixels,
		int occludedPixels,
		int clippedPixels,
		int outOfBoundsPixels,
		Renderer3DFrame.SpriteAnchor anchor) {
		depthMisses++;
		if (anchor != null) {
			minAnchorDepth = Math.min(minAnchorDepth, Math.min(anchor.getCameraZ(), anchor.getAverageDepth()));
			maxAnchorDepth = Math.max(maxAnchorDepth, Math.max(anchor.getCameraZ(), anchor.getAverageDepth()));
		}
	}

	String describe() {
		StringBuilder builder = new StringBuilder();
		builder.append(legacySpriteId)
			.append(":c")
			.append(commands)
			.append("/v")
			.append(visiblePixels)
			.append("/d")
			.append(directPixels)
			.append("/s")
			.append(skipped)
			.append("/a")
			.append(atlasFull)
			.append("/f")
			.append(fallbacks)
			.append("@")
			.append(minX)
			.append(",")
			.append(minY)
			.append("-")
			.append(maxX)
			.append(",")
			.append(maxY);
		if (depthEvaluations > 0) {
			builder.append("/de")
				.append(depthEvaluations)
				.append(":vis")
				.append(depthVisiblePixels)
				.append("/src")
				.append(depthSourcePixels)
				.append("/occ")
				.append(depthOccludedPixels)
				.append("/clip")
				.append(depthClippedPixels)
				.append("/oob")
				.append(depthOutOfBoundsPixels);
		}
		if (depthMisses > 0) {
			builder.append("/dm")
				.append(depthMisses)
				.append(":full");
			if (minAnchorDepth != Integer.MAX_VALUE) {
				builder.append("/z")
					.append(minAnchorDepth)
					.append("-")
					.append(maxAnchorDepth);
			}
		}
		return builder.toString();
	}
}

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
