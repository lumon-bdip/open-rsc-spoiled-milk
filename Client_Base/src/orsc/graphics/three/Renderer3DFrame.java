package orsc.graphics.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Renderer3DFrame {
	private final int sourceModelCount;
	private final int fogDistance;
	private final int viewportWidth;
	private final int viewportHeight;
	private final int centerX;
	private final int centerY;
	private final Renderer3DTextureData[] textures;
	private final List<FaceCommand> worldFaces = new ArrayList<FaceCommand>();
	private final List<SpriteAnchor> spriteAnchors = new ArrayList<SpriteAnchor>();
	private final Map<Long, FaceCommand> worldFacesByModelFace = new HashMap<Long, FaceCommand>();
	private final int[] worldFaceCountsByKind = new int[Renderer3DModelKind.values().length];
	private Renderer3DDepthFrame depthFrame;
	private Renderer3DMeshFrame meshFrame;

	Renderer3DFrame(
		int sourceModelCount,
		int fogDistance,
		int viewportWidth,
		int viewportHeight,
		int centerX,
		int centerY,
		Renderer3DTextureData[] textures) {
		this.sourceModelCount = sourceModelCount;
		this.fogDistance = fogDistance;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.centerX = centerX;
		this.centerY = centerY;
		this.textures = textures == null ? new Renderer3DTextureData[0] : textures;
	}

	void addWorldFace(
		int modelIndex,
		int faceId,
		int texture,
		int color,
		int orientation,
		int averageDepth,
		RSModel model,
		int[] faceIndices,
		int vertexCount) {
		int[] cameraX = new int[vertexCount];
		int[] cameraY = new int[vertexCount];
		int[] cameraZ = new int[vertexCount];
		int[] screenX = new int[vertexCount];
		int[] screenY = new int[vertexCount];
		Renderer3DModelKind modelKind = model.getRenderer3DModelKind();

		for (int vertex = 0; vertex < vertexCount; vertex++) {
			int modelVertex = faceIndices[vertex];
			cameraX[vertex] = model.vertXRot[modelVertex];
			cameraY[vertex] = model.vertYRot[modelVertex];
			cameraZ[vertex] = model.vertZRot[modelVertex];
			screenX[vertex] = model.vertexParam6[modelVertex];
			screenY[vertex] = model.vertexParam2[modelVertex];
		}

		FaceCommand face = new FaceCommand(
			modelKind,
			modelIndex,
			faceId,
			texture,
			color,
			orientation,
			averageDepth,
			cameraX,
			cameraY,
			cameraZ,
			screenX,
			screenY);
		this.worldFaces.add(face);
		this.worldFacesByModelFace.put(worldFaceKey(modelIndex, faceId), face);
		this.worldFaceCountsByKind[modelKind.ordinal()]++;
	}

	void recordLegacyDrawOrder(int modelIndex, int faceId, int drawOrder) {
		FaceCommand face = this.worldFacesByModelFace.get(worldFaceKey(modelIndex, faceId));
		if (face != null) {
			face.setLegacyDrawOrder(drawOrder);
		}
	}

	void recordLegacyClippedGeometry(
		int modelIndex,
		int faceId,
		int[] cameraX,
		int[] cameraY,
		int[] cameraZ,
		int[] screenX,
		int[] screenY,
		int[] light,
		int vertexCount) {
		FaceCommand face = this.worldFacesByModelFace.get(worldFaceKey(modelIndex, faceId));
		if (face != null) {
			face.setLegacyClippedGeometry(cameraX, cameraY, cameraZ, screenX, screenY, light, vertexCount);
		}
	}

	void addSpriteAnchor(
		int faceId,
		int spriteId,
		int pickIndex,
		int legacyDrawOrder,
		int averageDepth,
		int cameraX,
		int cameraY,
		int cameraZ,
		int screenX,
		int screenY,
		int drawX,
		int drawY,
		int drawWidth,
		int drawHeight,
		int scale,
		int horizontalSkew,
		boolean pickable) {
		this.spriteAnchors.add(new SpriteAnchor(
			faceId,
			spriteId,
			pickIndex,
			legacyDrawOrder,
			averageDepth,
			cameraX,
			cameraY,
			cameraZ,
			screenX,
			screenY,
			drawX,
			drawY,
			drawWidth,
			drawHeight,
			scale,
			horizontalSkew,
			pickable));
	}

	private static long worldFaceKey(int modelIndex, int faceId) {
		return ((long) modelIndex << 32) ^ (faceId & 0xffffffffL);
	}

	public int getSourceModelCount() {
		return sourceModelCount;
	}

	public int getFogDistance() {
		return fogDistance;
	}

	public int getViewportWidth() {
		return viewportWidth;
	}

	public int getViewportHeight() {
		return viewportHeight;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public Renderer3DTextureData[] getTextures() {
		return textures;
	}

	public Renderer3DTextureData getTexture(int textureId) {
		if (textureId < 0 || textureId >= textures.length) {
			return null;
		}
		return textures[textureId];
	}

	public int getWorldFaceCount() {
		return worldFaces.size();
	}

	public List<FaceCommand> getWorldFaces() {
		return Collections.unmodifiableList(worldFaces);
	}

	public int getSpriteAnchorCount() {
		return spriteAnchors.size();
	}

	public List<SpriteAnchor> getSpriteAnchors() {
		return Collections.unmodifiableList(spriteAnchors);
	}

	public int getWorldFaceCount(Renderer3DModelKind kind) {
		if (kind == null) {
			return 0;
		}
		return worldFaceCountsByKind[kind.ordinal()];
	}

	void setDepthFrame(Renderer3DDepthFrame depthFrame) {
		this.depthFrame = depthFrame;
	}

	public Renderer3DDepthFrame getDepthFrame() {
		return depthFrame;
	}

	void setMeshFrame(Renderer3DMeshFrame meshFrame) {
		this.meshFrame = meshFrame;
	}

	public Renderer3DMeshFrame getMeshFrame() {
		return meshFrame;
	}

	public static final class SpriteAnchor {
		private final int faceId;
		private final int spriteId;
		private final int pickIndex;
		private final int legacyDrawOrder;
		private final int averageDepth;
		private final int cameraX;
		private final int cameraY;
		private final int cameraZ;
		private final int screenX;
		private final int screenY;
		private final int drawX;
		private final int drawY;
		private final int drawWidth;
		private final int drawHeight;
		private final int scale;
		private final int horizontalSkew;
		private final boolean pickable;

		private SpriteAnchor(
			int faceId,
			int spriteId,
			int pickIndex,
			int legacyDrawOrder,
			int averageDepth,
			int cameraX,
			int cameraY,
			int cameraZ,
			int screenX,
			int screenY,
			int drawX,
			int drawY,
			int drawWidth,
			int drawHeight,
			int scale,
			int horizontalSkew,
			boolean pickable) {
			this.faceId = faceId;
			this.spriteId = spriteId;
			this.pickIndex = pickIndex;
			this.legacyDrawOrder = legacyDrawOrder;
			this.averageDepth = averageDepth;
			this.cameraX = cameraX;
			this.cameraY = cameraY;
			this.cameraZ = cameraZ;
			this.screenX = screenX;
			this.screenY = screenY;
			this.drawX = drawX;
			this.drawY = drawY;
			this.drawWidth = drawWidth;
			this.drawHeight = drawHeight;
			this.scale = scale;
			this.horizontalSkew = horizontalSkew;
			this.pickable = pickable;
		}

		public int getFaceId() {
			return faceId;
		}

		public int getSpriteId() {
			return spriteId;
		}

		public int getPickIndex() {
			return pickIndex;
		}

		public int getLegacyDrawOrder() {
			return legacyDrawOrder;
		}

		public int getAverageDepth() {
			return averageDepth;
		}

		public int getCameraX() {
			return cameraX;
		}

		public int getCameraY() {
			return cameraY;
		}

		public int getCameraZ() {
			return cameraZ;
		}

		public int getScreenX() {
			return screenX;
		}

		public int getScreenY() {
			return screenY;
		}

		public int getDrawX() {
			return drawX;
		}

		public int getDrawY() {
			return drawY;
		}

		public int getDrawWidth() {
			return drawWidth;
		}

		public int getDrawHeight() {
			return drawHeight;
		}

		public int getScale() {
			return scale;
		}

		public int getHorizontalSkew() {
			return horizontalSkew;
		}

		public boolean isPickable() {
			return pickable;
		}
	}

	public static final class FaceCommand {
		private final Renderer3DModelKind modelKind;
		private final int modelIndex;
		private final int faceId;
		private final int texture;
		private final int color;
		private final int orientation;
		private final int averageDepth;
		private final int[] cameraX;
		private final int[] cameraY;
		private final int[] cameraZ;
		private final int[] screenX;
		private final int[] screenY;
		private final int[] light;
		private final float[] textureU;
		private final float[] textureV;
		private int[] clippedCameraX;
		private int[] clippedCameraY;
		private int[] clippedCameraZ;
		private int[] clippedScreenX;
		private int[] clippedScreenY;
		private int[] clippedLight;
		private float[] clippedTextureU;
		private float[] clippedTextureV;
		private int legacyDrawOrder = -1;

		private FaceCommand(
			Renderer3DModelKind modelKind,
			int modelIndex,
			int faceId,
			int texture,
			int color,
			int orientation,
			int averageDepth,
			int[] cameraX,
			int[] cameraY,
			int[] cameraZ,
			int[] screenX,
			int[] screenY) {
			this.modelKind = modelKind;
			this.modelIndex = modelIndex;
			this.faceId = faceId;
			this.texture = texture;
			this.color = color;
			this.orientation = orientation;
			this.averageDepth = averageDepth;
			this.cameraX = cameraX;
			this.cameraY = cameraY;
			this.cameraZ = cameraZ;
			this.screenX = screenX;
			this.screenY = screenY;
			this.light = new int[cameraX.length];
			this.textureU = new float[cameraX.length];
			this.textureV = new float[cameraX.length];
			populateTextureCoordinates(cameraX, cameraY, cameraZ, textureU, textureV);
		}

		public Renderer3DModelKind getModelKind() {
			return modelKind;
		}

		public int getModelIndex() {
			return modelIndex;
		}

		public int getFaceId() {
			return faceId;
		}

		public int getTexture() {
			return texture;
		}

		public int getColor() {
			return color;
		}

		public int getOrientation() {
			return orientation;
		}

		public int getAverageDepth() {
			return averageDepth;
		}

		public int getLegacyDrawOrder() {
			return legacyDrawOrder;
		}

		private void setLegacyDrawOrder(int legacyDrawOrder) {
			this.legacyDrawOrder = legacyDrawOrder;
		}

		public int getVertexCount() {
			return cameraX.length;
		}

		public int getRenderVertexCount() {
			return clippedCameraX == null ? cameraX.length : clippedCameraX.length;
		}

		public int[] getCameraX() {
			return cameraX;
		}

		public int[] getRenderCameraX() {
			return clippedCameraX == null ? cameraX : clippedCameraX;
		}

		public int[] getCameraY() {
			return cameraY;
		}

		public int[] getRenderCameraY() {
			return clippedCameraY == null ? cameraY : clippedCameraY;
		}

		public int[] getCameraZ() {
			return cameraZ;
		}

		public int[] getRenderCameraZ() {
			return clippedCameraZ == null ? cameraZ : clippedCameraZ;
		}

		public int[] getScreenX() {
			return screenX;
		}

		public int[] getRenderScreenX() {
			return clippedScreenX == null ? screenX : clippedScreenX;
		}

		public int[] getScreenY() {
			return screenY;
		}

		public int[] getRenderScreenY() {
			return clippedScreenY == null ? screenY : clippedScreenY;
		}

		public int[] getRenderLight() {
			return clippedLight == null ? light : clippedLight;
		}

		public float[] getTextureU() {
			return textureU;
		}

		public float[] getRenderTextureU() {
			return clippedTextureU == null ? textureU : clippedTextureU;
		}

		public float[] getTextureV() {
			return textureV;
		}

		public float[] getRenderTextureV() {
			return clippedTextureV == null ? textureV : clippedTextureV;
		}

		private void setLegacyClippedGeometry(
			int[] cameraX,
			int[] cameraY,
			int[] cameraZ,
			int[] screenX,
			int[] screenY,
			int[] light,
			int vertexCount) {
			if (vertexCount < 3) {
				return;
			}

			this.clippedCameraX = Arrays.copyOf(cameraX, vertexCount);
			this.clippedCameraY = Arrays.copyOf(cameraY, vertexCount);
			this.clippedCameraZ = Arrays.copyOf(cameraZ, vertexCount);
			this.clippedScreenX = Arrays.copyOf(screenX, vertexCount);
			this.clippedScreenY = Arrays.copyOf(screenY, vertexCount);
			this.clippedLight = light == null ? new int[vertexCount] : Arrays.copyOf(light, vertexCount);
			this.clippedTextureU = new float[vertexCount];
			this.clippedTextureV = new float[vertexCount];
			populateTextureCoordinates(
				this.clippedCameraX,
				this.clippedCameraY,
				this.clippedCameraZ,
				this.clippedTextureU,
				this.clippedTextureV);
		}

		private void populateTextureCoordinates(
			int[] sourceCameraX,
			int[] sourceCameraY,
			int[] sourceCameraZ,
			float[] destinationU,
			float[] destinationV) {
			if (texture < 0 || cameraX.length < 3) {
				return;
			}

			int last = cameraX.length - 1;
			double ux = cameraX[1] - cameraX[0];
			double uy = cameraY[1] - cameraY[0];
			double uz = cameraZ[1] - cameraZ[0];
			double vx = cameraX[last] - cameraX[0];
			double vy = cameraY[last] - cameraY[0];
			double vz = cameraZ[last] - cameraZ[0];
			double uu = dot(ux, uy, uz, ux, uy, uz);
			double uv = dot(ux, uy, uz, vx, vy, vz);
			double vv = dot(vx, vy, vz, vx, vy, vz);
			double determinant = uu * vv - uv * uv;
			if (Math.abs(determinant) < 0.000001) {
				return;
			}

			for (int vertex = 0; vertex < sourceCameraX.length; vertex++) {
				double px = sourceCameraX[vertex] - cameraX[0];
				double py = sourceCameraY[vertex] - cameraY[0];
				double pz = sourceCameraZ[vertex] - cameraZ[0];
				double pu = dot(px, py, pz, ux, uy, uz);
				double pv = dot(px, py, pz, vx, vy, vz);
				destinationU[vertex] = (float) ((pu * vv - pv * uv) / determinant);
				destinationV[vertex] = (float) ((pv * uu - pu * uv) / determinant);
			}
		}

		private static double dot(
			double leftX,
			double leftY,
			double leftZ,
			double rightX,
			double rightY,
			double rightZ) {
			return leftX * rightX + leftY * rightY + leftZ * rightZ;
		}
	}
}
