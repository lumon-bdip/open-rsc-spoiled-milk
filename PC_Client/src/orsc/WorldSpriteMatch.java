package orsc;

import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;

final class WorldSpriteMatch {
	final Renderer3DFrame.SpriteAnchor anchor;
	final Renderer2DFrame.SpriteCommand command;

	WorldSpriteMatch(Renderer3DFrame.SpriteAnchor anchor, Renderer2DFrame.SpriteCommand command) {
		this.anchor = anchor;
		this.command = command;
	}
}
