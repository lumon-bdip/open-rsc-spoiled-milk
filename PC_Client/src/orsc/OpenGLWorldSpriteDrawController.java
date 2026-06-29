package orsc;

import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;

import java.util.ArrayList;
import java.util.List;

/*
 * RENDERER-V2 OWNER: high-level world-sprite draw orchestration. This class
 * groups character layers, builds command-sized sprite textures, uploads them
 * to the dynamic atlas, and submits anchored sprites through
 * OpenGLWorldSpriteRenderer. The presenter supplies only projection and bridge
 * callbacks that are still shared with other overlay paths.
 */
final class OpenGLWorldSpriteDrawController {
	interface Delegate {
		boolean hasActiveFrameCapture();

		DynamicTextureData buildDepthVisibleEntitySpriteTexture(
			Frame frame,
			Renderer2DFrame.SpriteCommand command,
			Renderer3DFrame.SpriteAnchor anchor,
			boolean[] clippedSceneRestoreMask,
			WorldSpriteAnchorMatch providedAnchorMatch);

		boolean drawDynamicSpriteTexture(
			Renderer2DFrame.SpriteCommand command,
			DynamicTextureData textureData,
			float alpha) throws Exception;

		void drawFallbackSprite(Renderer2DFrame.SpriteCommand command) throws Exception;

		void useWorldMeshProjection(Frame frame) throws Exception;

		void useSourceProjection(int sourceWidth, int sourceHeight) throws Exception;
	}

	private final LwjglBindings gl;
	private final OpenGLDynamicTextureAtlas textureAtlas;
	private final OpenGLWorldSpriteRenderer spriteRenderer;
	private final Delegate delegate;

	OpenGLWorldSpriteDrawController(
		LwjglBindings gl,
		OpenGLDynamicTextureAtlas textureAtlas,
		OpenGLWorldSpriteRenderer spriteRenderer,
		Delegate delegate) {
		this.gl = gl;
		this.textureAtlas = textureAtlas;
		this.spriteRenderer = spriteRenderer;
		this.delegate = delegate;
	}

	void drawEntitySpriteCommand(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand) throws Exception {
		drawWorldSpriteCommand(frame, worldSpriteCommand);
	}

	int drawSceneCommands(
		Frame frame,
		List<OpenGLCompositeSceneCommand> sceneCommands) throws Exception {
		if (frame == null || frame.renderer3DFrame == null || sceneCommands == null || sceneCommands.isEmpty()) {
			return 0;
		}
		int drawn = 0;
		for (int index = 0; index < sceneCommands.size(); index++) {
			OpenGLCompositeSceneCommand sceneCommand = sceneCommands.get(index);
			WorldSpriteCommand worldSpriteCommand = sceneCommand.worldSpriteCommand;
			if (worldSpriteCommand == null || worldSpriteCommand.command == null) {
				continue;
			}
			if (worldSpriteCommand.anchor != null
				&& OpenGLCompositeSceneBuilder.isLegacyEntitySpriteCommand(worldSpriteCommand.command)) {
				List<WorldSpriteCommand> characterLayerCommands = new ArrayList<WorldSpriteCommand>();
				characterLayerCommands.add(worldSpriteCommand);
				while (index + 1 < sceneCommands.size()) {
					WorldSpriteCommand nextWorldSpriteCommand = sceneCommands.get(index + 1).worldSpriteCommand;
					if (nextWorldSpriteCommand == null
						|| nextWorldSpriteCommand.command == null
						|| !OpenGLCompositeSceneBuilder.isLegacyEntitySpriteCommand(nextWorldSpriteCommand.command)
						|| !sameWorldSpriteAnchor(worldSpriteCommand.anchor, nextWorldSpriteCommand.anchor)) {
						break;
					}
					characterLayerCommands.add(nextWorldSpriteCommand);
					index++;
				}
				if (characterLayerCommands.size() > 1
					&& drawCharacterSpriteLayers(frame, worldSpriteCommand.anchor, characterLayerCommands)) {
					drawn += characterLayerCommands.size();
					continue;
				}
			}
			drawWorldSpriteCommand(frame, worldSpriteCommand);
			drawn++;
		}
		return drawn;
	}

	private boolean drawCharacterSpriteLayers(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		List<WorldSpriteCommand> layerCommands) throws Exception {
		CompositeWorldSpriteTexture compositeTexture =
			OpenGLSpriteTextureBuilder.buildCompositeCharacterSpriteTexture(layerCommands);
		if (compositeTexture == null) {
			return false;
		}
		return drawDepthOwnedWorldSpriteTextureData(
			frame,
			anchor,
			compositeTexture,
			1.0f);
	}

	private void drawWorldSpriteCommand(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand) throws Exception {
		if (worldSpriteCommand == null) {
			return;
		}
		Renderer2DFrame.SpriteCommand command = worldSpriteCommand.command;
		DynamicTextureData depthDiagnosticTexture = null;
		if (worldSpriteCommand.anchor != null && delegate.hasActiveFrameCapture()) {
			depthDiagnosticTexture = delegate.buildDepthVisibleEntitySpriteTexture(
				frame,
				command,
				worldSpriteCommand.anchor,
				null,
				worldSpriteCommand.anchorMatch);
		}
		boolean drawn;
		if (worldSpriteCommand.anchor != null) {
			DynamicTextureData textureData = OpenGLSpriteTextureBuilder.buildDirectSpriteTexture(command);
			drawn = drawDepthOwnedWorldSpriteTextureData(
				frame,
				worldSpriteCommand,
				textureData,
				command.getAlpha() / (float) Renderer2DFrame.SpriteCommand.FULL_ALPHA);
		} else {
			DynamicTextureData textureData = depthDiagnosticTexture;
			if (textureData == null) {
				textureData = OpenGLSpriteTextureBuilder.buildDirectSpriteTexture(command);
			}
			drawn = textureData != null
				&& delegate.drawDynamicSpriteTexture(command, textureData, 1.0f);
		}
		if (!drawn) {
			delegate.drawFallbackSprite(command);
		}
	}

	private boolean drawDepthOwnedWorldSpriteTextureData(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand,
		DynamicTextureData textureData,
		float alpha) throws Exception {
		if (textureAtlas == null || textureData == null) {
			return false;
		}
		OpenGLTextureRegion region = textureAtlas.upload(textureData);
		if (region == null) {
			textureAtlas.beginFrame();
			region = textureAtlas.upload(textureData);
			if (region == null) {
				return false;
			}
		}
		return drawDepthOwnedWorldSpriteTexture(
			frame,
			worldSpriteCommand,
			region,
			alpha,
			true);
	}

	private boolean drawDepthOwnedWorldSpriteTextureData(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		CompositeWorldSpriteTexture compositeTexture,
		float alpha) throws Exception {
		if (textureAtlas == null || compositeTexture == null || compositeTexture.textureData == null) {
			return false;
		}
		OpenGLTextureRegion region = textureAtlas.upload(compositeTexture.textureData);
		if (region == null) {
			textureAtlas.beginFrame();
			region = textureAtlas.upload(compositeTexture.textureData);
			if (region == null) {
				return false;
			}
		}
		return drawDepthOwnedWorldSpriteTexture(
			frame,
			anchor,
			compositeTexture.x,
			compositeTexture.y,
			compositeTexture.width,
			compositeTexture.height,
			region,
			alpha);
	}

	private boolean drawDepthOwnedWorldSpriteTexture(
		Frame frame,
		WorldSpriteCommand worldSpriteCommand,
		OpenGLTextureRegion region,
		float alpha,
		boolean fullRegion) throws Exception {
		if (region == null
			|| frame == null
			|| frame.renderer3DFrame == null
			|| worldSpriteCommand == null
			|| worldSpriteCommand.anchor == null
			|| spriteRenderer == null) {
			return false;
		}
		delegate.useWorldMeshProjection(frame);
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(gl.GL_ALPHA_TEST);
		gl.glAlphaFunc(gl.GL_GREATER, 0.0f);
		try {
			if (fullRegion) {
				spriteRenderer.drawFullRegion(
					frame.renderer3DFrame,
					worldSpriteCommand.command,
					worldSpriteCommand.anchor,
					region,
					alpha);
			} else {
				spriteRenderer.drawCommandRegion(
					frame.renderer3DFrame,
					worldSpriteCommand.command,
					worldSpriteCommand.anchor,
					region,
					alpha);
			}
		} finally {
			gl.glDepthMask(true);
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_ALPHA_TEST);
			delegate.useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		return true;
	}

	private boolean drawDepthOwnedWorldSpriteTexture(
		Frame frame,
		Renderer3DFrame.SpriteAnchor anchor,
		int x,
		int y,
		int width,
		int height,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		if (region == null
			|| frame == null
			|| frame.renderer3DFrame == null
			|| anchor == null
			|| width <= 0
			|| height <= 0
			|| spriteRenderer == null) {
			return false;
		}
		delegate.useWorldMeshProjection(frame);
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		gl.glEnable(gl.GL_TEXTURE_2D);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(gl.GL_ALPHA_TEST);
		gl.glAlphaFunc(gl.GL_GREATER, 0.0f);
		try {
			spriteRenderer.drawScreenRegion(
				frame.renderer3DFrame,
				anchor,
				x,
				y,
				width,
				height,
				region,
				alpha);
		} finally {
			gl.glDepthMask(true);
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glDisable(gl.GL_ALPHA_TEST);
			delegate.useSourceProjection(frame.sourceWidth, frame.sourceHeight);
		}
		return true;
	}

	private static boolean sameWorldSpriteAnchor(
		Renderer3DFrame.SpriteAnchor left,
		Renderer3DFrame.SpriteAnchor right) {
		if (left == right) {
			return true;
		}
		return left != null
			&& right != null
			&& left.getFaceId() == right.getFaceId()
			&& left.getLegacyDrawOrder() == right.getLegacyDrawOrder();
	}
}
