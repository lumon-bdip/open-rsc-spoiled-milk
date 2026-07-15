package orsc;

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
