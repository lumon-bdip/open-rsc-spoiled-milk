package orsc;

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
