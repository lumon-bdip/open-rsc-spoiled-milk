package orsc;

import orsc.graphics.Renderer2DFrame;
import orsc.graphics.three.Renderer3DFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/*
 * RENDERER-V2 OWNER: camera-space world-sprite quad submission. This owns the
 * VBO/EBO used for depth-owned NPC/player/item billboards; higher-level
 * command ordering and fallback decisions stay outside this class.
 */
final class OpenGLWorldSpriteRenderer implements AutoCloseable {
	private static final int QUAD_VERTEX_COUNT = 4;
	private static final int QUAD_INDEX_COUNT = 6;
	private static final int FLOATS_PER_VERTEX = 5;
	private static final int POSITION_COMPONENTS = 3;
	private static final int TEXCOORD_COMPONENTS = 2;
	private static final int TEXCOORD_OFFSET_BYTES = POSITION_COMPONENTS * 4;
	private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * 4;

	private final LwjglBindings gl;
	private int vertexBufferId;
	private int indexBufferId;
	private FloatBuffer uploadBuffer;

	OpenGLWorldSpriteRenderer(LwjglBindings gl) {
		this.gl = gl;
	}

	void initialize() throws Exception {
		vertexBufferId = gl.glGenBuffers();
		indexBufferId = gl.glGenBuffers();
		uploadBuffer = ByteBuffer
			.allocateDirect(QUAD_VERTEX_COUNT * FLOATS_PER_VERTEX * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		IntBuffer indices = ByteBuffer
			.allocateDirect(QUAD_INDEX_COUNT * 4)
			.order(ByteOrder.nativeOrder())
			.asIntBuffer();
		putQuadIndices(indices);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
		gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indices, gl.GL_STATIC_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	void drawScreenRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer3DFrame.SpriteAnchor anchor,
		int x,
		int y,
		int width,
		int height,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topDepth = estimateEntitySpriteDepth(anchor, y);
		float bottomDepth = estimateEntitySpriteDepth(anchor, y + height);
		float scale = perspectiveScale(renderer3DFrame);
		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawQuad(
			renderer3DFrame,
			x,
			x,
			y,
			y + height,
			width,
			region.getU0(),
			region.getU1(),
			region.getV0(),
			region.getV1(),
			topDepth,
			bottomDepth,
			scale);
	}

	void drawFullRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topDepth = estimateEntitySpriteDepth(anchor, command.getY());
		float bottomDepth = estimateEntitySpriteDepth(anchor, command.getY() + command.getHeight());
		float scale = perspectiveScale(renderer3DFrame);
		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawQuad(
			renderer3DFrame,
			command.getTopX16() / 65536.0f,
			command.getBottomX16() / 65536.0f,
			command.getY(),
			command.getY() + command.getHeight(),
			command.getWidth(),
			region.getU0(),
			region.getU1(),
			region.getV0(),
			region.getV1(),
			topDepth,
			bottomDepth,
			scale);
	}

	void drawCommandRegion(
		Renderer3DFrame renderer3DFrame,
		Renderer2DFrame.SpriteCommand command,
		Renderer3DFrame.SpriteAnchor anchor,
		OpenGLTextureRegion region,
		float alpha) throws Exception {
		float topScreenX = command.getTopX16() / 65536.0f;
		float bottomScreenX = command.getBottomX16() / 65536.0f;
		float topScreenY = command.getY();
		float bottomScreenY = command.getY() + command.getHeight();
		float topDepth = estimateEntitySpriteDepth(anchor, command.getY());
		float bottomDepth = estimateEntitySpriteDepth(anchor, command.getY() + command.getHeight());
		float scale = perspectiveScale(renderer3DFrame);
		float uSpan = region.getU1() - region.getU0();
		float vSpan = region.getV1() - region.getV0();
		float sourceX0 = command.getSourceStartX16() / 65536.0f;
		float sourceY0 = command.getSourceStartY16() / 65536.0f;
		float sourceX1 = (command.getSourceStartX16()
			+ (long) command.getWidth() * command.getSourceScaleX16()) / 65536.0f;
		float sourceY1 = (command.getSourceStartY16()
			+ (long) command.getHeight() * command.getSourceScaleY16()) / 65536.0f;
		float u0 = region.getU0() + uSpan * sourceX0 / region.getWidth();
		float v0 = region.getV0() + vSpan * sourceY0 / region.getHeight();
		float u1 = region.getU0() + uSpan * sourceX1 / region.getWidth();
		float v1 = region.getV0() + vSpan * sourceY1 / region.getHeight();
		float leftU = command.isMirrorX() ? u1 : u0;
		float rightU = command.isMirrorX() ? u0 : u1;

		gl.glBindTexture(gl.GL_TEXTURE_2D, region.getTextureId());
		useWorldToneColor(alpha);
		drawQuad(
			renderer3DFrame,
			topScreenX,
			bottomScreenX,
			topScreenY,
			bottomScreenY,
			command.getWidth(),
			leftU,
			rightU,
			v0,
			v1,
			topDepth,
			bottomDepth,
			scale);
	}

	static int estimateEntitySpriteDepth(Renderer3DFrame.SpriteAnchor anchor, int screenY) {
		int bottomDepth = anchor.getCameraZ();
		int topDepth = anchor.getAverageDepth() * 2 - bottomDepth;
		if (topDepth <= 0) {
			topDepth = anchor.getAverageDepth();
		}
		if (bottomDepth <= 0) {
			bottomDepth = anchor.getAverageDepth();
		}
		int drawHeight = Math.max(1, anchor.getDrawHeight());
		int relativeY = clamp(screenY - anchor.getDrawY(), 0, drawHeight);
		int depth = topDepth + (bottomDepth - topDepth) * relativeY / drawHeight;
		return Math.max(1, depth);
	}

	@Override
	public void close() throws Exception {
		if (vertexBufferId != 0) {
			gl.glDeleteBuffers(vertexBufferId);
			vertexBufferId = 0;
		}
		if (indexBufferId != 0) {
			gl.glDeleteBuffers(indexBufferId);
			indexBufferId = 0;
		}
		uploadBuffer = null;
	}

	private void drawQuad(
		Renderer3DFrame renderer3DFrame,
		float topScreenX,
		float bottomScreenX,
		float topScreenY,
		float bottomScreenY,
		float width,
		float leftU,
		float rightU,
		float v0,
		float v1,
		float topDepth,
		float bottomDepth,
		float scale) throws Exception {
		if (uploadBuffer == null || vertexBufferId == 0 || indexBufferId == 0) {
			return;
		}
		uploadBuffer.clear();
		putVertex(renderer3DFrame, topScreenX, topScreenY, topDepth, scale, leftU, v0);
		putVertex(renderer3DFrame, topScreenX + width, topScreenY, topDepth, scale, rightU, v0);
		putVertex(renderer3DFrame, bottomScreenX + width, bottomScreenY, bottomDepth, scale, rightU, v1);
		putVertex(renderer3DFrame, bottomScreenX, bottomScreenY, bottomDepth, scale, leftU, v1);
		uploadBuffer.flip();
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glBufferData(gl.GL_ARRAY_BUFFER, uploadBuffer, gl.GL_STREAM_DRAW);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(POSITION_COMPONENTS, gl.GL_FLOAT, STRIDE_BYTES, 0L);
		gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(TEXCOORD_COMPONENTS, gl.GL_FLOAT, STRIDE_BYTES, TEXCOORD_OFFSET_BYTES);
		try {
			gl.glDrawElements(gl.GL_TRIANGLES, QUAD_INDEX_COUNT, gl.GL_UNSIGNED_INT, 0L);
		} finally {
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
		}
	}

	private void putVertex(
		Renderer3DFrame frame,
		float screenX,
		float screenY,
		float cameraZ,
		float scale,
		float u,
		float v) {
		float cameraX = (screenX - frame.getCenterX()) * cameraZ / scale;
		float cameraY = (screenY - frame.getCenterY()) * cameraZ / scale;
		uploadBuffer.put(cameraX);
		uploadBuffer.put(cameraY);
		uploadBuffer.put(cameraZ);
		uploadBuffer.put(u);
		uploadBuffer.put(v);
	}

	private void useWorldToneColor(float alpha) throws Exception {
		RendererDayNightCycle.Presentation presentation = RendererDayNightCycle.currentPresentation();
		gl.glColor4f(
			presentation.redMultiplier,
			presentation.greenMultiplier,
			presentation.blueMultiplier,
			alpha);
	}

	private static float perspectiveScale(Renderer3DFrame frame) {
		return (float) (1 << Math.max(0, Math.min(24, frame.getPerspectiveShift())));
	}

	private static void putQuadIndices(IntBuffer indices) {
		indices.put(0);
		indices.put(1);
		indices.put(2);
		indices.put(0);
		indices.put(2);
		indices.put(3);
		indices.flip();
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}
}
