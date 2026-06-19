package orsc.graphics.three;

import orsc.graphics.RendererTransparency;

import java.util.EnumSet;
import java.util.List;

public final class Renderer3DMeshFrame {
	public static final int FLOATS_PER_VERTEX = 14;
	public static final int CAMERA_X_OFFSET = 0;
	public static final int CAMERA_Y_OFFSET = 1;
	public static final int CAMERA_Z_OFFSET = 2;
	public static final int SCREEN_X_OFFSET = 3;
	public static final int SCREEN_Y_OFFSET = 4;
	public static final int RED_OFFSET = 5;
	public static final int GREEN_OFFSET = 6;
	public static final int BLUE_OFFSET = 7;
	public static final int TEXTURE_U_OFFSET = 8;
	public static final int TEXTURE_V_OFFSET = 9;
	public static final int TEXTURE_RED_OFFSET = 10;
	public static final int TEXTURE_GREEN_OFFSET = 11;
	public static final int TEXTURE_BLUE_OFFSET = 12;
	public static final int TEXTURE_ALPHA_OFFSET = 13;
	private static final int INDICES_PER_TRIANGLE = 3;
	private final float[] vertices;
	private final int[] indices;
	private final int[] triangleTextures;
	private final int[] triangleFallbackColors;
	private final Renderer3DModelKind[] triangleModelKinds;
	private final int[] triangleModelIndices;
	private final int[] triangleFaceIds;
	private final int[] triangleAverageDepths;
	private final int[] triangleLegacyDrawOrders;
	private final Renderer3DTextureData[] textures;
	private final int viewportWidth;
	private final int viewportHeight;
	private final int centerX;
	private final int centerY;
	private int vertexCount;
	private int indexCount;
	private int triangleCount;
	private int skippedTriangleCount;
	private int texturedTriangleCount;
	private int flatColorTriangleCount;
	private int transparentTriangleCount;

	private Renderer3DMeshFrame(int maxTriangles, Renderer3DFrame frame) {
		this.vertices = new float[maxTriangles * 3 * FLOATS_PER_VERTEX];
		this.indices = new int[maxTriangles * INDICES_PER_TRIANGLE];
		this.triangleTextures = new int[maxTriangles];
		this.triangleFallbackColors = new int[maxTriangles];
		this.triangleModelKinds = new Renderer3DModelKind[maxTriangles];
		this.triangleModelIndices = new int[maxTriangles];
		this.triangleFaceIds = new int[maxTriangles];
		this.triangleAverageDepths = new int[maxTriangles];
		this.triangleLegacyDrawOrders = new int[maxTriangles];
		this.textures = frame.getTextures();
		this.viewportWidth = frame.getViewportWidth();
		this.viewportHeight = frame.getViewportHeight();
		this.centerX = frame.getCenterX();
		this.centerY = frame.getCenterY();
	}

	static Renderer3DMeshFrame build(Renderer3DFrame frame, EnumSet<Renderer3DModelKind> includedKinds) {
		int triangleCapacity = 0;
		List<Renderer3DFrame.FaceCommand> faces = frame.getWorldFaces();
		for (Renderer3DFrame.FaceCommand face : faces) {
			if (includedKinds.contains(face.getModelKind())) {
				triangleCapacity += Math.max(0, face.getRenderVertexCount() - 2);
			}
		}

		Renderer3DMeshFrame mesh = new Renderer3DMeshFrame(
			triangleCapacity,
			frame);
		for (Renderer3DFrame.FaceCommand face : faces) {
			if (!includedKinds.contains(face.getModelKind()) || face.getRenderVertexCount() < 3) {
				continue;
			}
			for (int vertex = 1; vertex < face.getRenderVertexCount() - 1; vertex++) {
				mesh.addTriangle(face, 0, vertex, vertex + 1);
			}
		}
		return mesh;
	}

	public float[] getVertices() {
		return vertices;
	}

	public int[] getIndices() {
		return indices;
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public int getIndexCount() {
		return indexCount;
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	public int getSkippedTriangleCount() {
		return skippedTriangleCount;
	}

	public int getTexturedTriangleCount() {
		return texturedTriangleCount;
	}

	public int getFlatColorTriangleCount() {
		return flatColorTriangleCount;
	}

	public int getTransparentTriangleCount() {
		return transparentTriangleCount;
	}

	public int[] getTriangleTextures() {
		return triangleTextures;
	}

	public int[] getTriangleFallbackColors() {
		return triangleFallbackColors;
	}

	public Renderer3DModelKind[] getTriangleModelKinds() {
		return triangleModelKinds;
	}

	public int[] getTriangleModelIndices() {
		return triangleModelIndices;
	}

	public int[] getTriangleFaceIds() {
		return triangleFaceIds;
	}

	public int[] getTriangleAverageDepths() {
		return triangleAverageDepths;
	}

	public int[] getTriangleLegacyDrawOrders() {
		return triangleLegacyDrawOrders;
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

	public int getTextureCount() {
		return textures.length;
	}

	public int getAvailableTextureCount() {
		int count = 0;
		for (Renderer3DTextureData texture : textures) {
			if (texture != null) {
				count++;
			}
		}
		return count;
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

	private void addTriangle(Renderer3DFrame.FaceCommand face, int a, int b, int c) {
		if (!isDrawableTriangle(face, a, b, c)) {
			skippedTriangleCount++;
			return;
		}

		int firstVertex = vertexCount;
		addVertex(face, a);
		addVertex(face, b);
		addVertex(face, c);
		indices[indexCount++] = firstVertex;
		indices[indexCount++] = firstVertex + 1;
		indices[indexCount++] = firstVertex + 2;
		addTriangleMaterial(face);
		triangleCount++;
	}

	private void addTriangleMaterial(Renderer3DFrame.FaceCommand face) {
		int triangle = triangleCount;
		int texture = face.getTexture();
		triangleTextures[triangle] = texture;
		triangleFallbackColors[triangle] = face.getColor();
		triangleModelKinds[triangle] = face.getModelKind();
		triangleModelIndices[triangle] = face.getModelIndex();
		triangleFaceIds[triangle] = face.getFaceId();
		triangleAverageDepths[triangle] = face.getAverageDepth();
		triangleLegacyDrawOrders[triangle] = face.getLegacyDrawOrder();
		if (texture == Scene.TRANSPARENT) {
			transparentTriangleCount++;
		} else if (texture >= 0) {
			texturedTriangleCount++;
		} else {
			flatColorTriangleCount++;
		}
	}

	private boolean isDrawableTriangle(Renderer3DFrame.FaceCommand face, int a, int b, int c) {
		int[] cameraZ = face.getRenderCameraZ();
		if (cameraZ[a] <= 0 || cameraZ[b] <= 0 || cameraZ[c] <= 0) {
			return false;
		}

		int[] screenX = face.getRenderScreenX();
		int[] screenY = face.getRenderScreenY();
		int ax = centerX + screenX[a];
		int ay = centerY + screenY[a];
		int bx = centerX + screenX[b];
		int by = centerY + screenY[b];
		int cx = centerX + screenX[c];
		int cy = centerY + screenY[c];
		int minX = Math.min(ax, Math.min(bx, cx));
		int maxX = Math.max(ax, Math.max(bx, cx));
		int minY = Math.min(ay, Math.min(by, cy));
		int maxY = Math.max(ay, Math.max(by, cy));
		if (maxX < 0 || minX >= viewportWidth || maxY < 0 || minY >= viewportHeight) {
			return false;
		}

		int marginX = Math.max(64, viewportWidth);
		int marginY = Math.max(64, viewportHeight);
		return minX >= -marginX
			&& maxX <= viewportWidth + marginX
			&& minY >= -marginY
			&& maxY <= viewportHeight + marginY;
	}

	private void addVertex(Renderer3DFrame.FaceCommand face, int vertex) {
		int color = fallbackColorForFace(face);
		int legacyLight = legacyLightForFace(face, vertex);
		float textureLight = textureLightForFace(face, vertex);
		color = shadedFallbackColorForFace(face, color, legacyLight);
		int offset = vertexCount * FLOATS_PER_VERTEX;
		vertices[offset + CAMERA_X_OFFSET] = face.getRenderCameraX()[vertex];
		vertices[offset + CAMERA_Y_OFFSET] = face.getRenderCameraY()[vertex];
		vertices[offset + CAMERA_Z_OFFSET] = face.getRenderCameraZ()[vertex];
		vertices[offset + SCREEN_X_OFFSET] = face.getRenderScreenX()[vertex];
		vertices[offset + SCREEN_Y_OFFSET] = face.getRenderScreenY()[vertex];
		vertices[offset + RED_OFFSET] = ((color >> 16) & 0xFF) / 255.0f;
		vertices[offset + GREEN_OFFSET] = ((color >> 8) & 0xFF) / 255.0f;
		vertices[offset + BLUE_OFFSET] = (color & 0xFF) / 255.0f;
		vertices[offset + TEXTURE_U_OFFSET] = face.getRenderTextureU()[vertex];
		vertices[offset + TEXTURE_V_OFFSET] = face.getRenderTextureV()[vertex];
		vertices[offset + TEXTURE_RED_OFFSET] = textureLight;
		vertices[offset + TEXTURE_GREEN_OFFSET] = textureLight;
		vertices[offset + TEXTURE_BLUE_OFFSET] = textureLight;
		vertices[offset + TEXTURE_ALPHA_OFFSET] = 1.0f;
		vertexCount++;
	}

	private float textureLightForFace(Renderer3DFrame.FaceCommand face, int vertex) {
		return legacyTextureLightFactor(legacyLightForFace(face, vertex));
	}

	private int legacyLightForFace(Renderer3DFrame.FaceCommand face, int vertex) {
		int[] lights = face.getRenderLight();
		int light = vertex >= 0 && vertex < lights.length ? lights[vertex] : 0;
		if (light < 0) {
			return 0;
		}
		if (light > 255) {
			return 255;
		}
		return light;
	}

	private int shadedFallbackColorForFace(Renderer3DFrame.FaceCommand face, int color, int legacyLight) {
		if (face.getTexture() < 0) {
			return legacyFlatResourceColor(color, legacyLight);
		}
		return legacyTextureShadeColor(color, legacyTextureShadeBand(legacyLight));
	}

	private float legacyTextureLightFactor(int legacyLight) {
		switch (legacyTextureShadeBand(legacyLight)) {
			case 1:
				return 216.0f / 248.0f;
			case 2:
				return 184.0f / 248.0f;
			case 3:
				return 152.0f / 248.0f;
			default:
				return 1.0f;
		}
	}

	private int legacyTextureShadeBand(int legacyLight) {
		return Math.max(0, Math.min(3, legacyLight >> 6));
	}

	private int legacyTextureShadeColor(int color, int shadeBand) {
		switch (shadeBand) {
			case 1:
				return (color - (color >>> 3)) & RendererTransparency.LEGACY_TEXTURE_COLOR_MASK;
			case 2:
				return (color - (color >>> 2)) & RendererTransparency.LEGACY_TEXTURE_COLOR_MASK;
			case 3:
				return (color - (color >>> 3) - (color >>> 2)) & RendererTransparency.LEGACY_TEXTURE_COLOR_MASK;
			default:
				return color;
		}
	}

	private int legacyFlatResourceColor(int color, int legacyLight) {
		int shade = 255 - legacyLight;
		int shadeSquared = shade * shade;
		int red = (((color >> 16) & 0xFF) * shadeSquared) / 65536;
		int green = (((color >> 8) & 0xFF) * shadeSquared) / 65536;
		int blue = ((color & 0xFF) * shadeSquared) / 65536;
		return red << 16 | green << 8 | blue;
	}

	private int fallbackColorForFace(Renderer3DFrame.FaceCommand face) {
		int texture = face.getTexture();
		if (texture >= 0 && texture < textures.length) {
			Renderer3DTextureData textureData = textures[texture];
			if (textureData != null && textureData.getOpaquePixelCount() > 0) {
				return textureData.getAverageOpaqueRgb();
			}
		}
		return face.getColor();
	}
}
