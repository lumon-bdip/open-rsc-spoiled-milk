package orsc;

import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;

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
