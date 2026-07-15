package orsc;

import orsc.graphics.three.Renderer3DMeshFrame;
import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DTextureData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * LEGACY BRIDGE: projected/captured world-mesh rendering retained for
 * diagnostics and compatibility while resident chunks take over static-world
 * ownership. Prefer extending OpenGLWorldChunkRenderer for new world features.
 */
final class OpenGLWorldMeshRenderer implements AutoCloseable {
	private static final int LEGACY_TRANSPARENT_TEXTURE = 12345678;
	private static final int FLAT_COLOR_BATCH_TEXTURE_ID = -1;
	static final int UPLOAD_FLOATS_PER_VERTEX = 12;
	static final int SHADER_UPLOAD_FLOATS_PER_VERTEX = 14;
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int MATERIAL_COLOR_COMPONENT_COUNT = 3;
	private static final int TEXTURE_COORD_COMPONENT_COUNT = 2;
	private static final int TEXTURE_LIGHT_COMPONENT_COUNT = 4;
	private static final int MATERIAL_COLOR_OFFSET_BYTES = POSITION_COMPONENT_COUNT * 4;
	private static final int TEXTURE_COORD_OFFSET_BYTES =
		(POSITION_COMPONENT_COUNT + MATERIAL_COLOR_COMPONENT_COUNT) * 4;
	private static final int TEXTURE_LIGHT_OFFSET_BYTES =
		(POSITION_COMPONENT_COUNT + MATERIAL_COLOR_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * 4;
	private static final int STRIDE_BYTES = UPLOAD_FLOATS_PER_VERTEX * 4;
	private static final int SHADER_POSITION_OFFSET_BYTES = 0;
	private static final int SHADER_TEXTURE_COORD_OFFSET_BYTES = POSITION_COMPONENT_COUNT * 4;
	private static final int SHADER_MATERIAL_COLOR_OFFSET_BYTES =
		(POSITION_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * 4;
	private static final int SHADER_LEGACY_LIGHT_OFFSET_BYTES =
		SHADER_MATERIAL_COLOR_OFFSET_BYTES + TEXTURE_LIGHT_COMPONENT_COUNT * 4;
	private static final int SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES =
		SHADER_LEGACY_LIGHT_OFFSET_BYTES + 4;
	private static final int SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES =
		SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES + 4;
	private static final int SHADER_STRIDE_BYTES = SHADER_UPLOAD_FLOATS_PER_VERTEX * 4;
	private static final float WALL_DEPTH_PRIORITY_FACTOR = -1.0f;
	private static final float WALL_DEPTH_PRIORITY_UNITS = -1.0f;

	private final LwjglBindings gl;
	private final OpenGLWorldTextureCache textureCache;
	private final OpenGLShaderProgram texturedShaderProgram;
	private final boolean texturedStaticVisible;
	private final boolean staticTexturesEnabled;
	private final float texturedDiagnosticAlpha;
	private final boolean texturedShaderEnabled;
	private final int vertexBufferId;
	private final int shaderVertexBufferId;
	private final int indexBufferId;
	private final int texturedIndexBufferId;
	private final int occluderIndexBufferId;
	private final List<WorldMeshTextureBatch> textureBatches = new ArrayList<>();
	private FloatBuffer vertexUploadBuffer;
	private FloatBuffer shaderVertexUploadBuffer;
	private IntBuffer indexUploadBuffer;
	private IntBuffer texturedIndexUploadBuffer;
	private IntBuffer occluderIndexUploadBuffer;
	private int vertexUploadCapacity;
	private int shaderVertexUploadCapacity;
	private int indexUploadCapacity;
	private int texturedIndexUploadCapacity;
	private int occluderIndexUploadCapacity;
	private WorldMeshUploadState lastUploadState;
	private ShaderVertexParityStats lastShaderVertexParityStats = ShaderVertexParityStats.EMPTY;
	private int lastDrawTriangles;
	private int lastDrawOccluderTriangles;
	private int lastDrawBatches;
	private int lastDrawCalls;
	private boolean closed;

	OpenGLWorldMeshRenderer(
		LwjglBindings gl,
		OpenGLWorldTextureCache textureCache,
		OpenGLShaderProgram texturedShaderProgram,
		boolean texturedStaticVisible,
		boolean staticTexturesEnabled,
		float texturedDiagnosticAlpha,
		boolean texturedShaderEnabled) throws Exception {
		this.gl = gl;
		this.textureCache = textureCache;
		this.texturedShaderProgram = texturedShaderProgram;
		this.texturedStaticVisible = texturedStaticVisible;
		this.staticTexturesEnabled = staticTexturesEnabled;
		this.texturedDiagnosticAlpha = texturedDiagnosticAlpha;
		this.texturedShaderEnabled = texturedShaderEnabled;
		this.vertexBufferId = gl.glGenBuffers();
		this.shaderVertexBufferId = gl.glGenBuffers();
		this.indexBufferId = gl.glGenBuffers();
		this.texturedIndexBufferId = gl.glGenBuffers();
		this.occluderIndexBufferId = gl.glGenBuffers();
	}

	void uploadAndMaybeDrawObjects(
		Renderer3DMeshFrame meshFrame,
		FloatBuffer projectionMatrix) throws Exception {
		uploadAndMaybeDraw(meshFrame, false, true, WorldMeshTriangleFilter.STATIC_OBJECTS, true, projectionMatrix);
	}

	int getLastDrawTriangles() {
		return lastDrawTriangles;
	}

	int getLastDrawOccluderTriangles() {
		return lastDrawOccluderTriangles;
	}

	int getLastDrawBatches() {
		return lastDrawBatches;
	}

	int getLastDrawCalls() {
		return lastDrawCalls;
	}

	void uploadAndMaybeDraw(
		Renderer3DMeshFrame meshFrame,
		boolean wireframeVisible,
		boolean texturedVisible,
		FloatBuffer projectionMatrix) throws Exception {
		uploadAndMaybeDraw(
			meshFrame,
			wireframeVisible,
			texturedVisible,
			WorldMeshTriangleFilter.ALL_STATIC,
			projectionMatrix);
	}

	void uploadAndMaybeDrawOwnedBaseWorld(
		Renderer3DMeshFrame meshFrame,
		boolean wireframeVisible,
		boolean texturedVisible,
		FloatBuffer projectionMatrix) throws Exception {
		// Renderer3DMeshFrame carries model kind, model index, and face id per
		// triangle. The capture analyzer verifies complete, unique ownership.
		uploadAndMaybeDraw(
			meshFrame,
			wireframeVisible,
			texturedVisible,
			WorldMeshTriangleFilter.ALL_STATIC,
			projectionMatrix);
	}

	void uploadAndMaybeDraw(
		Renderer3DMeshFrame meshFrame,
		boolean wireframeVisible,
		boolean texturedVisible,
		WorldMeshTriangleFilter triangleFilter,
		FloatBuffer projectionMatrix) throws Exception {
		uploadAndMaybeDraw(meshFrame, wireframeVisible, texturedVisible, triangleFilter, false, projectionMatrix);
	}

	void uploadAndMaybeDraw(
		Renderer3DMeshFrame meshFrame,
		boolean wireframeVisible,
		boolean texturedVisible,
		WorldMeshTriangleFilter triangleFilter,
		boolean depthAwareObjectBridge,
		FloatBuffer projectionMatrix) throws Exception {
		// LEGACY BRIDGE: this uploads captured projected mesh data. It should keep
		// parity narrow and should not become the owner for new remaster features.
		int vertexCount = meshFrame.getVertexCount();
		int indexCount = meshFrame.getIndexCount();
		if (vertexCount <= 0 || indexCount <= 0) {
			return;
		}

		WorldTextureUploadStats textureStats = textureCache.uploadReferencedTextures(meshFrame);
		WorldMeshUploadSignature uploadSignature =
			WorldMeshUploadSignature.from(meshFrame, vertexCount, indexCount, triangleFilter);
		int texturedIndexCount;
		int occluderIndexCount;
		boolean needsFullIndexBuffer = wireframeVisible || !depthAwareObjectBridge;
		boolean reusedUpload = lastUploadState != null && lastUploadState.matches(uploadSignature);
		if (!reusedUpload) {
			ensureUploadBuffers(vertexCount, indexCount);
			copyMesh(meshFrame, vertexCount);
			if (needsFullIndexBuffer) {
				copyFullIndices(meshFrame, indexCount);
			}
			texturedIndexCount = copyTexturedIndices(meshFrame, indexCount, triangleFilter);
			occluderIndexCount =
				depthAwareObjectBridge ? copyProjectedOccluderIndices(meshFrame, indexCount) : 0;

			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
			gl.glBufferData(gl.GL_ARRAY_BUFFER, vertexUploadBuffer, gl.GL_STREAM_DRAW);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
			gl.glBufferData(gl.GL_ARRAY_BUFFER, shaderVertexUploadBuffer, gl.GL_STREAM_DRAW);
			if (needsFullIndexBuffer) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
				gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, indexUploadBuffer, gl.GL_STREAM_DRAW);
			}
			if (texturedIndexCount > 0) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexBufferId);
				gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexUploadBuffer, gl.GL_STREAM_DRAW);
			}
			if (occluderIndexCount > 0) {
				gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexBufferId);
				gl.glBufferData(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexUploadBuffer, gl.GL_STREAM_DRAW);
			}
			lastUploadState = new WorldMeshUploadState(uploadSignature, texturedIndexCount, occluderIndexCount);
		} else {
			texturedIndexCount = lastUploadState.texturedIndexCount;
			occluderIndexCount = lastUploadState.occluderIndexCount;
		}
		RenderTelemetry.recordOpenGLWorldMeshUpload(reusedUpload);
		RenderTelemetry.recordOpenGLWorldMeshFrame(vertexCount, indexCount, indexCount / 3);
		RenderTelemetry.recordOpenGLWorldTextureFrame(
			textureStats.referencedTextures,
			textureStats.cachedTextures,
			textureStats.uploadedTextures,
			textureStats.missingTextures,
			textureStats.atlases);
		int drawnTriangles = 0;
		int drawnOccluderTriangles = 0;
		int drawnBatches = 0;
		int drawCalls = 0;
		if (!wireframeVisible && !texturedVisible) {
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
			RenderTelemetry.recordOpenGLWorldMeshDraw(drawnTriangles, drawnOccluderTriangles, drawnBatches, drawCalls);
			return;
		}

		gl.glEnable(gl.GL_DEPTH_TEST);
		if (!depthAwareObjectBridge) {
			gl.glClear(gl.GL_DEPTH_BUFFER_BIT);
		}
		try {
			if (texturedVisible && texturedIndexCount > 0) {
				if (depthAwareObjectBridge) {
					drawProjectedObjectBridge(occluderIndexCount, projectionMatrix);
					drawnOccluderTriangles = occluderIndexCount / 3;
					if (occluderIndexCount > 0) {
						drawCalls++;
					}
				} else {
					drawTexturedDiagnostic(projectionMatrix);
				}
				drawnTriangles = texturedIndexCount / 3;
				drawnBatches = countDrawableTextureBatches();
				drawCalls += drawnBatches;
			}
			if (wireframeVisible) {
				drawWireframeDiagnostic(indexCount);
				drawCalls++;
			}
		} finally {
			gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(gl.GL_COLOR_ARRAY);
			gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
			gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, 0);
			gl.glDisable(gl.GL_DEPTH_TEST);
			gl.glEnable(gl.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		}
		lastDrawTriangles = drawnTriangles;
		lastDrawOccluderTriangles = drawnOccluderTriangles;
		lastDrawBatches = drawnBatches;
		lastDrawCalls = drawCalls;
		RenderTelemetry.recordOpenGLWorldMeshDraw(drawnTriangles, drawnOccluderTriangles, drawnBatches, drawCalls);
	}

	private void ensureUploadBuffers(int vertexCount, int indexCount) {
		int requiredVertexFloats = vertexCount * UPLOAD_FLOATS_PER_VERTEX;
		int requiredShaderVertexFloats = vertexCount * SHADER_UPLOAD_FLOATS_PER_VERTEX;
		if (vertexUploadBuffer == null || vertexUploadCapacity < requiredVertexFloats) {
			vertexUploadCapacity = growCapacity(vertexUploadCapacity, requiredVertexFloats);
			vertexUploadBuffer = ByteBuffer
				.allocateDirect(vertexUploadCapacity * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		}
		if (shaderVertexUploadBuffer == null || shaderVertexUploadCapacity < requiredShaderVertexFloats) {
			shaderVertexUploadCapacity = growCapacity(shaderVertexUploadCapacity, requiredShaderVertexFloats);
			shaderVertexUploadBuffer = ByteBuffer
				.allocateDirect(shaderVertexUploadCapacity * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		}

		if (indexUploadBuffer == null || indexUploadCapacity < indexCount) {
			indexUploadCapacity = growCapacity(indexUploadCapacity, indexCount);
			indexUploadBuffer = ByteBuffer
				.allocateDirect(indexUploadCapacity * 4)
				.order(ByteOrder.nativeOrder())
				.asIntBuffer();
		}

		if (texturedIndexUploadBuffer == null || texturedIndexUploadCapacity < indexCount) {
			texturedIndexUploadCapacity = growCapacity(texturedIndexUploadCapacity, indexCount);
			texturedIndexUploadBuffer = ByteBuffer
				.allocateDirect(texturedIndexUploadCapacity * 4)
				.order(ByteOrder.nativeOrder())
				.asIntBuffer();
		}

		if (occluderIndexUploadBuffer == null || occluderIndexUploadCapacity < indexCount) {
			occluderIndexUploadCapacity = growCapacity(occluderIndexUploadCapacity, indexCount);
			occluderIndexUploadBuffer = ByteBuffer
				.allocateDirect(occluderIndexUploadCapacity * 4)
				.order(ByteOrder.nativeOrder())
				.asIntBuffer();
		}
	}

	private int growCapacity(int currentCapacity, int requiredCapacity) {
		int capacity = Math.max(1024, currentCapacity);
		while (capacity < requiredCapacity) {
			capacity *= 2;
		}
		return capacity;
	}

	private void copyMesh(Renderer3DMeshFrame meshFrame, int vertexCount) throws Exception {
		float[] vertices = meshFrame.getVertices();
		int[] triangleTextures = meshFrame.getTriangleTextures();
		int[] triangleFallbackColors = meshFrame.getTriangleFallbackColors();
		Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
		float brightness = RendererDayNightCycle.currentBrightnessMultiplier();
		boolean flatGeometryLighting = usesTriangleFlatWorldMeshLighting();
		int cachedFlatLightTriangle = -1;
		int cachedFlatLegacyLight = 0;
		int cachedFlatBaseLegacyLight = 0;
		vertexUploadBuffer.clear();
		shaderVertexUploadBuffer.clear();
		for (int vertex = 0; vertex < vertexCount; vertex++) {
			int sourceOffset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX;
			int triangle = vertex / 3;
			int textureId = triangle >= 0 && triangle < triangleTextures.length ? triangleTextures[triangle] : 0;
			int rawMaterialColor =
				triangle >= 0 && triangle < triangleFallbackColors.length ? triangleFallbackColors[triangle] : 0;
			if (flatGeometryLighting && triangle != cachedFlatLightTriangle) {
				cachedFlatLightTriangle = triangle;
				cachedFlatLegacyLight = triangleAverageLegacyLight(
					vertices,
					vertexCount,
					triangle,
					Renderer3DMeshFrame.LEGACY_LIGHT_OFFSET);
				cachedFlatBaseLegacyLight = triangleAverageLegacyLight(
					vertices,
					vertexCount,
					triangle,
					Renderer3DMeshFrame.BASE_LEGACY_LIGHT_OFFSET);
			}
			int sourceLegacyLight = flatGeometryLighting
				? cachedFlatLegacyLight
				: Math.round(vertices[sourceOffset + Renderer3DMeshFrame.LEGACY_LIGHT_OFFSET]);
			int sourceBaseLegacyLight = flatGeometryLighting
				? cachedFlatBaseLegacyLight
				: Math.round(vertices[sourceOffset + Renderer3DMeshFrame.BASE_LEGACY_LIGHT_OFFSET]);
			int legacyLight = applyLightingModeLegacyLight(sourceLegacyLight);
			int baseLegacyLight = applyLightingModeLegacyLight(sourceBaseLegacyLight);
			boolean flatMaterial = isFlatColorMaterial(textureId);
			OpenGLTextureRegion textureRegion =
				textureRegionForVertex(meshFrame, triangleTextures, triangleModelKinds, vertex);
			vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_X_OFFSET]);
			vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Y_OFFSET]);
			vertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Z_OFFSET]);
			int flatGeometryMaterialColor = flatGeometryLighting && textureRegion == null
				? shadedWorldMeshMaterialColor(rawMaterialColor, textureId, legacyLight)
				: -1;
			float materialRed = flatGeometryMaterialColor >= 0
				? brightnessColor(((flatGeometryMaterialColor >> 16) & 0xFF) / 255.0f, brightness)
				: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.RED_OFFSET], brightness);
			float materialGreen = flatGeometryMaterialColor >= 0
				? brightnessColor(((flatGeometryMaterialColor >> 8) & 0xFF) / 255.0f, brightness)
				: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.GREEN_OFFSET], brightness);
			float materialBlue = flatGeometryMaterialColor >= 0
				? brightnessColor((flatGeometryMaterialColor & 0xFF) / 255.0f, brightness)
				: brightnessColor(vertices[sourceOffset + Renderer3DMeshFrame.BLUE_OFFSET], brightness);
			float textureLight = brightnessColor(textureLightFactor(legacyLight), brightness);
			float alpha =
				vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_ALPHA_OFFSET] * texturedDiagnosticAlpha;
			float atlasU = atlasU(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_U_OFFSET]);
			float atlasV = atlasV(textureRegion, vertices[sourceOffset + Renderer3DMeshFrame.TEXTURE_V_OFFSET]);
			float textureRed = textureLight;
			float textureGreen = textureLight;
			float textureBlue = textureLight;
			if (flatMaterial && textureRegion != null) {
				textureRed = materialRed;
				textureGreen = materialGreen;
				textureBlue = materialBlue;
			}
			vertexUploadBuffer.put(materialRed);
			vertexUploadBuffer.put(materialGreen);
			vertexUploadBuffer.put(materialBlue);
			vertexUploadBuffer.put(atlasU);
			vertexUploadBuffer.put(atlasV);
			vertexUploadBuffer.put(textureRed);
			vertexUploadBuffer.put(textureGreen);
			vertexUploadBuffer.put(textureBlue);
			vertexUploadBuffer.put(alpha);

			shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_X_OFFSET]);
			shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Y_OFFSET]);
			shaderVertexUploadBuffer.put(vertices[sourceOffset + Renderer3DMeshFrame.CAMERA_Z_OFFSET]);
			shaderVertexUploadBuffer.put(atlasU);
			shaderVertexUploadBuffer.put(atlasV);
			shaderVertexUploadBuffer.put(materialRed);
			shaderVertexUploadBuffer.put(materialGreen);
			shaderVertexUploadBuffer.put(materialBlue);
			shaderVertexUploadBuffer.put(alpha);
			shaderVertexUploadBuffer.put(legacyLight);
			shaderVertexUploadBuffer.put(baseLegacyLight);
			shaderVertexUploadBuffer.put(((rawMaterialColor >> 16) & 0xFF) / 255.0f);
			shaderVertexUploadBuffer.put(((rawMaterialColor >> 8) & 0xFF) / 255.0f);
			shaderVertexUploadBuffer.put((rawMaterialColor & 0xFF) / 255.0f);
		}
		vertexUploadBuffer.flip();
		shaderVertexUploadBuffer.flip();
		lastShaderVertexParityStats = ShaderVertexParityStats.compare(
			vertexUploadBuffer,
			shaderVertexUploadBuffer,
			vertexCount);
	}

	private boolean usesTriangleFlatWorldMeshLighting() {
		RendererGeometrySettings.Mode mode = RendererGeometrySettings.getMode();
		return mode == RendererGeometrySettings.Mode.FACETED
			|| mode == RendererGeometrySettings.Mode.WIRE;
	}

	private int triangleAverageLegacyLight(float[] vertices, int vertexCount, int triangle, int lightOffset) {
		int firstVertex = triangle * 3;
		int total = 0;
		int count = 0;
		for (int i = 0; i < 3; i++) {
			int vertex = firstVertex + i;
			int offset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + lightOffset;
			if (vertex < 0 || vertex >= vertexCount || offset < 0 || offset >= vertices.length) {
				continue;
			}
			total += Math.round(vertices[offset]);
			count++;
		}
		return count <= 0 ? 0 : Math.round((float) total / count);
	}

	private int shadedWorldMeshMaterialColor(int color, int textureId, int legacyLight) {
		int normalizedLight = clampLegacyLight(legacyLight);
		if (textureId < 0 || textureId == LEGACY_TRANSPARENT_TEXTURE) {
			return legacyFlatResourceColor(color, normalizedLight);
		}
		return legacyTextureShadeColor(color, legacyTextureShadeBand(normalizedLight));
	}

	private int legacyFlatResourceColor(int color, int legacyLight) {
		int shade = 255 - legacyLight;
		int shadeSquared = shade * shade;
		int red = (((color >> 16) & 0xFF) * shadeSquared) / 65536;
		int green = (((color >> 8) & 0xFF) * shadeSquared) / 65536;
		int blue = ((color & 0xFF) * shadeSquared) / 65536;
		return red << 16 | green << 8 | blue;
	}

	private int legacyTextureShadeColor(int color, int shadeBand) {
		switch (shadeBand) {
			case 1:
				return (color - (color >>> 3)) & 0xFFFFFF;
			case 2:
				return (color - (color >>> 2)) & 0xFFFFFF;
			case 3:
				return (color - (color >>> 3) - (color >>> 2)) & 0xFFFFFF;
			default:
				return color;
		}
	}

	ShaderVertexParityStats getLastShaderVertexParityStats() {
		return lastShaderVertexParityStats;
	}

	private float brightnessColor(float color, float brightness) {
		float adjusted = color * brightness;
		if (adjusted < 0.0f) {
			return 0.0f;
		}
		if (adjusted > 1.0f) {
			return 1.0f;
		}
		return adjusted;
	}

	private int applyLightingModeLegacyLight(int legacyLight) {
		return clampLegacyLight(legacyLight);
	}

	private float textureLightFactor(int legacyLight) {
		return legacyTextureLightFactor(legacyLight);
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
		return clampLegacyLight(legacyLight) >> 6;
	}

	private int clampLegacyLight(int legacyLight) {
		if (legacyLight < 0) {
			return 0;
		}
		if (legacyLight > 255) {
			return 255;
		}
		return legacyLight;
	}

	private float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private void copyFullIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
		int[] indices = meshFrame.getIndices();
		indexUploadBuffer.clear();
		indexUploadBuffer.put(indices, 0, indexCount);
		indexUploadBuffer.flip();
	}

	private int copyTexturedIndices(
		Renderer3DMeshFrame meshFrame,
		int indexCount,
		WorldMeshTriangleFilter triangleFilter) throws Exception {
		if (texturedStaticVisible && triangleFilter != WorldMeshTriangleFilter.STATIC_OBJECTS) {
			return copyTexturedIndicesInLegacyOrder(meshFrame, indexCount, triangleFilter);
		}

		int[] indices = meshFrame.getIndices();
		int[] triangleTextures = meshFrame.getTriangleTextures();
		Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
		int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
		Map<WorldMeshTextureBatchKey, WorldMeshTextureBatch> batchesByTexture = new LinkedHashMap<>();
		textureBatches.clear();

		for (int triangle = 0; triangle < triangleCount; triangle++) {
			if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
				continue;
			}
			if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
				continue;
			}
			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], modelKind)) {
				continue;
			}
			Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
				? meshFrame.getTexture(triangleTextures[triangle])
				: null;
			StaticWorldMaterialPass materialPass =
				OpenGLStaticWorldMaterials.classify(triangleTextures[triangle], textureData);
			if (materialPass == StaticWorldMaterialPass.DISCARDED
				|| materialPass == StaticWorldMaterialPass.UNRESOLVED
				|| materialPass == StaticWorldMaterialPass.TRANSLUCENT) {
				continue;
			}
			OpenGLTextureRegion region =
				textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
			int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
			WorldMeshTextureBatchKey batchKey =
				new WorldMeshTextureBatchKey(batchTextureId, materialPass, modelKind);
			WorldMeshTextureBatch batch = batchesByTexture.get(batchKey);
			if (batch == null) {
				batch = new WorldMeshTextureBatch(
					batchTextureId,
					materialPass,
					modelKind);
				batchesByTexture.put(batchKey, batch);
				textureBatches.add(batch);
			}
			batch.indexCount += 3;
		}

		int nextIndex = 0;
		for (WorldMeshTextureBatch batch : textureBatches) {
			batch.startIndex = nextIndex;
			batch.writeIndex = nextIndex;
			nextIndex += batch.indexCount;
		}

		texturedIndexUploadBuffer.clear();
		texturedIndexUploadBuffer.limit(nextIndex);
		if (nextIndex == 0) {
			return 0;
		}

		for (int triangle = 0; triangle < triangleCount; triangle++) {
			if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
				continue;
			}
			if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
				continue;
			}
			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], modelKind)) {
				continue;
			}
			Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
				? meshFrame.getTexture(triangleTextures[triangle])
				: null;
			StaticWorldMaterialPass materialPass =
				OpenGLStaticWorldMaterials.classify(triangleTextures[triangle], textureData);
			if (materialPass == StaticWorldMaterialPass.DISCARDED
				|| materialPass == StaticWorldMaterialPass.UNRESOLVED
				|| materialPass == StaticWorldMaterialPass.TRANSLUCENT) {
				continue;
			}
			OpenGLTextureRegion region =
				textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
			int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();

			int sourceIndex = triangle * 3;
			WorldMeshTextureBatchKey batchKey = new WorldMeshTextureBatchKey(
				batchTextureId,
				materialPass,
				modelKind);
			WorldMeshTextureBatch batch = batchesByTexture.get(batchKey);
			texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex]);
			texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 1]);
			texturedIndexUploadBuffer.put(batch.writeIndex++, indices[sourceIndex + 2]);
		}

		texturedIndexUploadBuffer.position(0);
		return nextIndex;
	}

	private int copyTexturedIndicesInLegacyOrder(
		Renderer3DMeshFrame meshFrame,
		int indexCount,
		WorldMeshTriangleFilter triangleFilter) throws Exception {
		return copyTexturedIndicesInLegacyOrderRange(
			meshFrame,
			indexCount,
			triangleFilter,
			Integer.MIN_VALUE,
			Integer.MAX_VALUE);
	}

	private int copyTexturedIndicesInLegacyOrderRange(
		Renderer3DMeshFrame meshFrame,
		int indexCount,
		WorldMeshTriangleFilter triangleFilter,
		int minExclusiveLegacyDrawOrder,
		int maxExclusiveLegacyDrawOrder) throws Exception {
		return copyTexturedIndicesInLegacyOrderRange(
			meshFrame,
			indexCount,
			triangleFilter,
			minExclusiveLegacyDrawOrder,
			maxExclusiveLegacyDrawOrder,
			null);
	}

	private int copyTexturedIndicesInLegacyOrderRange(
		Renderer3DMeshFrame meshFrame,
		int indexCount,
		WorldMeshTriangleFilter triangleFilter,
		int minExclusiveLegacyDrawOrder,
		int maxExclusiveLegacyDrawOrder,
		Set<Long> modelFaceKeys) throws Exception {
		int[] indices = meshFrame.getIndices();
		int[] triangleTextures = meshFrame.getTriangleTextures();
		int[] triangleLegacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
		Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
		int[] triangleModelIndices = meshFrame.getTriangleModelIndices();
		int[] triangleFaceIds = meshFrame.getTriangleFaceIds();
		int triangleCount = Math.min(meshFrame.getTriangleCount(), Math.min(triangleTextures.length, indexCount / 3));
		List<Integer> orderedTriangles = new ArrayList<>();
		textureBatches.clear();
		boolean fullRange =
			minExclusiveLegacyDrawOrder == Integer.MIN_VALUE
				&& maxExclusiveLegacyDrawOrder == Integer.MAX_VALUE;

		for (int triangle = 0; triangle < triangleCount; triangle++) {
			if (!isTexturedDiagnosticTriangle(triangleModelKinds, triangle, triangleFilter)) {
				continue;
			}
			if (!isReasonableProjectedTriangle(meshFrame, indices, triangle)) {
				continue;
			}
			int legacyDrawOrder = triangle < triangleLegacyDrawOrders.length
				? triangleLegacyDrawOrders[triangle]
				: -1;
			if (!fullRange
				&& (legacyDrawOrder < 0
				|| legacyDrawOrder <= minExclusiveLegacyDrawOrder
				|| legacyDrawOrder >= maxExclusiveLegacyDrawOrder)) {
				continue;
			}
			if (modelFaceKeys != null
				&& !modelFaceKeys.isEmpty()
				&& !modelFaceKeys.contains(triangleModelFaceKey(triangleModelIndices, triangleFaceIds, triangle))) {
				continue;
			}
			if (!isDrawableWorldMaterial(meshFrame, triangleTextures[triangle], triangleModelKinds[triangle])) {
				continue;
			}
			orderedTriangles.add(Integer.valueOf(triangle));
		}

		Collections.sort(orderedTriangles, new Comparator<Integer>() {
			@Override
			public int compare(Integer left, Integer right) {
				int leftTriangle = left.intValue();
				int rightTriangle = right.intValue();
				int leftOrder = legacyDrawOrderOrEnd(triangleLegacyDrawOrders, leftTriangle);
				int rightOrder = legacyDrawOrderOrEnd(triangleLegacyDrawOrders, rightTriangle);
				if (leftOrder != rightOrder) {
					return leftOrder < rightOrder ? -1 : 1;
				}
				return leftTriangle < rightTriangle ? -1 : (leftTriangle == rightTriangle ? 0 : 1);
			}
		});

		texturedIndexUploadBuffer.clear();
		int nextIndex = 0;
		if (triangleFilter == WorldMeshTriangleFilter.ALL_STATIC) {
			boolean canBatchOpaqueStatic =
				fullRange && (modelFaceKeys == null || modelFaceKeys.isEmpty());
			if (canBatchOpaqueStatic) {
				nextIndex = appendOpaqueStaticMaterialBatches(
					meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles, nextIndex);
				nextIndex = appendMaterialPassTriangles(
					meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
					StaticWorldMaterialPass.OPAQUE, nextIndex, true);
			} else {
				nextIndex = appendMaterialPassTriangles(
					meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
					StaticWorldMaterialPass.OPAQUE, nextIndex);
			}
			nextIndex = appendMaterialPassTriangles(
				meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles,
				StaticWorldMaterialPass.CUTOUT, nextIndex);
		} else {
			nextIndex = appendMaterialPassTriangles(
				meshFrame, triangleTextures, triangleModelKinds, indices, orderedTriangles, null, nextIndex);
		}

		texturedIndexUploadBuffer.flip();
		return nextIndex;
	}

	private int appendOpaqueStaticMaterialBatches(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int[] indices,
		List<Integer> orderedTriangles,
		int nextIndex) throws Exception {
		Map<WorldMeshTextureBatchKey, WorldMeshTextureBatch> batchesByMaterial =
			new LinkedHashMap<WorldMeshTextureBatchKey, WorldMeshTextureBatch>();
		Map<WorldMeshTextureBatchKey, List<Integer>> trianglesByMaterial =
			new LinkedHashMap<WorldMeshTextureBatchKey, List<Integer>>();
		for (Integer orderedTriangle : orderedTriangles) {
			int triangle = orderedTriangle.intValue();
			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (!isProjectedMeshOpaqueBatchKind(modelKind)) {
				continue;
			}
			Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
				? meshFrame.getTexture(triangleTextures[triangle])
				: null;
			StaticWorldMaterialPass materialPass =
				OpenGLStaticWorldMaterials.classify(triangleTextures[triangle], textureData);
			if (materialPass != StaticWorldMaterialPass.OPAQUE) {
				continue;
			}
			OpenGLTextureRegion region =
				textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
			if (!canAppendWorldMeshTriangle(triangleTextures[triangle], modelKind, region)) {
				continue;
			}
			int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
			WorldMeshTextureBatchKey batchKey =
				new WorldMeshTextureBatchKey(batchTextureId, materialPass, modelKind);
			WorldMeshTextureBatch batch = batchesByMaterial.get(batchKey);
			if (batch == null) {
				batch = new WorldMeshTextureBatch(
					batchTextureId,
					materialPass,
					modelKind);
				batchesByMaterial.put(batchKey, batch);
				trianglesByMaterial.put(batchKey, new ArrayList<Integer>());
			}
			trianglesByMaterial.get(batchKey).add(orderedTriangle);
		}
		for (Map.Entry<WorldMeshTextureBatchKey, List<Integer>> entry : trianglesByMaterial.entrySet()) {
			WorldMeshTextureBatch batch = batchesByMaterial.get(entry.getKey());
			batch.startIndex = nextIndex;
			textureBatches.add(batch);
			for (Integer terrainTriangle : entry.getValue()) {
				int sourceIndex = terrainTriangle.intValue() * 3;
				texturedIndexUploadBuffer.put(indices[sourceIndex]);
				texturedIndexUploadBuffer.put(indices[sourceIndex + 1]);
				texturedIndexUploadBuffer.put(indices[sourceIndex + 2]);
				nextIndex += 3;
				batch.indexCount += 3;
			}
		}
		return nextIndex;
	}

	private boolean isProjectedMeshOpaqueBatchKind(Renderer3DModelKind modelKind) {
		return modelKind == Renderer3DModelKind.TERRAIN
			|| modelKind == Renderer3DModelKind.WALL
			|| modelKind == Renderer3DModelKind.ROOF
			|| modelKind == Renderer3DModelKind.GAME_OBJECT
			|| modelKind == Renderer3DModelKind.WALL_OBJECT;
	}

	private int appendMaterialPassTriangles(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int[] indices,
		List<Integer> orderedTriangles,
		StaticWorldMaterialPass requiredPass,
		int nextIndex) throws Exception {
		return appendMaterialPassTriangles(
			meshFrame,
			triangleTextures,
			triangleModelKinds,
			indices,
			orderedTriangles,
			requiredPass,
			nextIndex,
			null);
	}

	private int appendMaterialPassTriangles(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int[] indices,
		List<Integer> orderedTriangles,
		StaticWorldMaterialPass requiredPass,
		int nextIndex,
		Renderer3DModelKind excludedKind) throws Exception {
		return appendMaterialPassTriangles(
			meshFrame,
			triangleTextures,
			triangleModelKinds,
			indices,
			orderedTriangles,
			requiredPass,
			nextIndex,
			excludedKind,
			false);
	}

	private int appendMaterialPassTriangles(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int[] indices,
		List<Integer> orderedTriangles,
		StaticWorldMaterialPass requiredPass,
		int nextIndex,
		boolean excludeOpaqueBatchKinds) throws Exception {
		return appendMaterialPassTriangles(
			meshFrame,
			triangleTextures,
			triangleModelKinds,
			indices,
			orderedTriangles,
			requiredPass,
			nextIndex,
			null,
			excludeOpaqueBatchKinds);
	}

	private int appendMaterialPassTriangles(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int[] indices,
		List<Integer> orderedTriangles,
		StaticWorldMaterialPass requiredPass,
		int nextIndex,
		Renderer3DModelKind excludedKind,
		boolean excludeOpaqueBatchKinds) throws Exception {
		WorldMeshTextureBatch currentBatch = null;
		for (Integer orderedTriangle : orderedTriangles) {
			int triangle = orderedTriangle.intValue();
			Renderer3DModelKind modelKind = triangleModelKinds[triangle];
			if (excludedKind != null && modelKind == excludedKind) {
				continue;
			}
			if (excludeOpaqueBatchKinds && isProjectedMeshOpaqueBatchKind(modelKind)) {
				continue;
			}
			Renderer3DTextureData textureData = triangleTextures[triangle] >= 0
				? meshFrame.getTexture(triangleTextures[triangle])
				: null;
			StaticWorldMaterialPass materialPass =
				OpenGLStaticWorldMaterials.classify(triangleTextures[triangle], textureData);
			if (requiredPass != null && materialPass != requiredPass) {
				continue;
			}
			OpenGLTextureRegion region =
				textureRegionForTriangle(meshFrame, triangleTextures[triangle], triangleModelKinds[triangle]);
			int batchTextureId = region == null ? FLAT_COLOR_BATCH_TEXTURE_ID : region.getTextureId();
			if (currentBatch == null
				|| currentBatch.textureId != batchTextureId
				|| currentBatch.materialPass != materialPass
				|| currentBatch.modelKind != triangleModelKinds[triangle]) {
				currentBatch = new WorldMeshTextureBatch(
					batchTextureId,
					materialPass,
					triangleModelKinds[triangle]);
				currentBatch.startIndex = nextIndex;
				textureBatches.add(currentBatch);
			}
			if (region == null
				&& !isFlatColorMaterial(triangleTextures[triangle])
				&& canSampleTextureForKind(triangleModelKinds[triangle])) {
				continue;
			}

			int sourceIndex = triangle * 3;
			texturedIndexUploadBuffer.put(indices[sourceIndex]);
			texturedIndexUploadBuffer.put(indices[sourceIndex + 1]);
			texturedIndexUploadBuffer.put(indices[sourceIndex + 2]);
			nextIndex += 3;
				currentBatch.indexCount += 3;
		}
		return nextIndex;
	}

	private boolean canAppendWorldMeshTriangle(
		int textureId,
		Renderer3DModelKind modelKind,
		OpenGLTextureRegion region) {
		return region != null || isFlatColorMaterial(textureId) || !canSampleTextureForKind(modelKind);
	}

	private long triangleModelFaceKey(int[] triangleModelIndices, int[] triangleFaceIds, int triangle) {
		int modelIndex = triangle >= 0 && triangle < triangleModelIndices.length
			? triangleModelIndices[triangle]
			: -1;
		int faceId = triangle >= 0 && triangle < triangleFaceIds.length
			? triangleFaceIds[triangle]
			: -1;
		return modelFaceKey(modelIndex, faceId);
	}

	private static long modelFaceKey(int modelIndex, int faceId) {
		return ((long) modelIndex << 32) ^ (faceId & 0xffffffffL);
	}

	private int copyProjectedOccluderIndices(Renderer3DMeshFrame meshFrame, int indexCount) {
		int[] indices = meshFrame.getIndices();
		int[] triangleTextures = meshFrame.getTriangleTextures();
		Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
		int triangleCount = Math.min(
			meshFrame.getTriangleCount(),
			Math.min(indexCount / 3, Math.min(triangleTextures.length, triangleModelKinds.length)));
		occluderIndexUploadBuffer.clear();
		int nextIndex = 0;
		for (int triangle = 0; triangle < triangleCount; triangle++) {
			if (!isProjectedObjectBridgeOccluderTriangle(
				meshFrame,
				indices,
				triangleTextures,
				triangleModelKinds,
				triangle)) {
				continue;
			}
			int sourceIndex = triangle * 3;
			occluderIndexUploadBuffer.put(indices[sourceIndex]);
			occluderIndexUploadBuffer.put(indices[sourceIndex + 1]);
			occluderIndexUploadBuffer.put(indices[sourceIndex + 2]);
			nextIndex += 3;
		}
		occluderIndexUploadBuffer.flip();
		return nextIndex;
	}

	private boolean isProjectedObjectBridgeOccluderTriangle(
		Renderer3DMeshFrame meshFrame,
		int[] indices,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int triangle) {
		Renderer3DModelKind modelKind = triangleModelKinds[triangle];
		if (modelKind != Renderer3DModelKind.TERRAIN
			&& modelKind != Renderer3DModelKind.WALL
			&& modelKind != Renderer3DModelKind.ROOF) {
			return false;
		}
		if (modelKind == Renderer3DModelKind.ROOF && Config.C_HIDE_ROOFS) {
			return false;
		}
		if (isTransparentMaterial(triangleTextures[triangle])) {
			return false;
		}
		return isReasonableProjectedTriangle(meshFrame, indices, triangle);
	}

	private int legacyDrawOrderOrEnd(int[] triangleLegacyDrawOrders, int triangle) {
		if (triangle < 0 || triangle >= triangleLegacyDrawOrders.length || triangleLegacyDrawOrders[triangle] < 0) {
			return Integer.MAX_VALUE;
		}
		return triangleLegacyDrawOrders[triangle];
	}

	private boolean isTexturedDiagnosticTriangle(
		Renderer3DModelKind[] triangleModelKinds,
		int triangle,
		WorldMeshTriangleFilter triangleFilter) {
		if (triangle < 0 || triangle >= triangleModelKinds.length) {
			return false;
		}

		Renderer3DModelKind modelKind = triangleModelKinds[triangle];
		if (!triangleFilter.accepts(modelKind)) {
			return false;
		}
		if (modelKind == Renderer3DModelKind.TERRAIN) {
			return true;
		}
		return texturedStaticVisible
			&& (modelKind == Renderer3DModelKind.WALL
				|| modelKind == Renderer3DModelKind.ROOF
				|| modelKind == Renderer3DModelKind.GAME_OBJECT
				|| modelKind == Renderer3DModelKind.WALL_OBJECT);
	}

	private boolean isDrawableWorldMaterial(
		Renderer3DMeshFrame meshFrame,
		int textureId,
		Renderer3DModelKind modelKind) {
		if (isFlatColorMaterial(textureId)) {
			return true;
		}
		if (isTransparentMaterial(textureId)) {
			return false;
		}
		if (!canSampleTextureForKind(modelKind)) {
			return true;
		}
		return textureCache.getRegion(meshFrame, textureId) != null;
	}

	private boolean isFlatColorMaterial(int textureId) {
		return textureId < 0;
	}

	private boolean isTransparentMaterial(int textureId) {
		return textureId == LEGACY_TRANSPARENT_TEXTURE;
	}

	private boolean canSampleTextureForKind(Renderer3DModelKind modelKind) {
		if (modelKind == Renderer3DModelKind.TERRAIN) {
			return true;
		}
		return staticTexturesEnabled
			&& (modelKind == Renderer3DModelKind.WALL
				|| modelKind == Renderer3DModelKind.ROOF
				|| modelKind == Renderer3DModelKind.GAME_OBJECT
				|| modelKind == Renderer3DModelKind.WALL_OBJECT);
	}

	private boolean isReasonableProjectedTriangle(
		Renderer3DMeshFrame meshFrame,
		int[] indices,
		int triangle) {
		int sourceIndex = triangle * 3;
		if (sourceIndex < 0 || sourceIndex + 2 >= indices.length) {
			return false;
		}
		float[] vertices = meshFrame.getVertices();
		int first = indices[sourceIndex];
		int second = indices[sourceIndex + 1];
		int third = indices[sourceIndex + 2];
		if (!isReasonableProjectedVertex(meshFrame, vertices, first)
			|| !isReasonableProjectedVertex(meshFrame, vertices, second)
			|| !isReasonableProjectedVertex(meshFrame, vertices, third)) {
			return false;
		}

		float firstX = projectedX(meshFrame, vertices, first);
		float secondX = projectedX(meshFrame, vertices, second);
		float thirdX = projectedX(meshFrame, vertices, third);
		float firstY = projectedY(meshFrame, vertices, first);
		float secondY = projectedY(meshFrame, vertices, second);
		float thirdY = projectedY(meshFrame, vertices, third);
		float minX = Math.min(firstX, Math.min(secondX, thirdX));
		float maxX = Math.max(firstX, Math.max(secondX, thirdX));
		float minY = Math.min(firstY, Math.min(secondY, thirdY));
		float maxY = Math.max(firstY, Math.max(secondY, thirdY));
		float viewportWidth = Math.max(1, meshFrame.getViewportWidth());
		float viewportHeight = Math.max(1, meshFrame.getViewportHeight());
		return maxX - minX <= viewportWidth * 1.75f
			&& maxY - minY <= viewportHeight * 1.75f;
	}

	private boolean isReasonableProjectedVertex(
		Renderer3DMeshFrame meshFrame,
		float[] vertices,
		int vertex) {
		int offset = vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX;
		if (vertex < 0 || offset + Renderer3DMeshFrame.SCREEN_Y_OFFSET >= vertices.length) {
			return false;
		}
		float x = projectedX(meshFrame, vertices, vertex);
		float y = projectedY(meshFrame, vertices, vertex);
		float viewportWidth = Math.max(1, meshFrame.getViewportWidth());
		float viewportHeight = Math.max(1, meshFrame.getViewportHeight());
		return x >= -viewportWidth
			&& x <= viewportWidth * 2.0f
			&& y >= -viewportHeight
			&& y <= viewportHeight * 2.0f;
	}

	private float projectedX(Renderer3DMeshFrame meshFrame, float[] vertices, int vertex) {
		return meshFrame.getCenterX()
			+ vertices[vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + Renderer3DMeshFrame.SCREEN_X_OFFSET];
	}

	private float projectedY(Renderer3DMeshFrame meshFrame, float[] vertices, int vertex) {
		return meshFrame.getCenterY()
			+ vertices[vertex * Renderer3DMeshFrame.FLOATS_PER_VERTEX + Renderer3DMeshFrame.SCREEN_Y_OFFSET];
	}

	private OpenGLTextureRegion textureRegionForVertex(
		Renderer3DMeshFrame meshFrame,
		int[] triangleTextures,
		Renderer3DModelKind[] triangleModelKinds,
		int vertex) throws Exception {
		int triangle = vertex / 3;
		if (triangle < 0 || triangle >= triangleTextures.length) {
			return null;
		}
		Renderer3DModelKind modelKind =
			triangle < triangleModelKinds.length ? triangleModelKinds[triangle] : Renderer3DModelKind.UNCLASSIFIED;
		return textureRegionForTriangle(meshFrame, triangleTextures[triangle], modelKind);
	}

	private OpenGLTextureRegion textureRegionForTriangle(
		Renderer3DMeshFrame meshFrame,
		int textureId,
		Renderer3DModelKind modelKind) throws Exception {
		if (isFlatColorMaterial(textureId)) {
			return null;
		}
		if (!canSampleTextureForKind(modelKind)) {
			return null;
		}
		return textureCache.getRegion(meshFrame, textureId);
	}

	private float atlasU(OpenGLTextureRegion region, float sourceU) {
		if (region == null) {
			return 0.0f;
		}
		return region.getU0() + (region.getU1() - region.getU0()) * clampUnit(sourceU);
	}

	private float atlasV(OpenGLTextureRegion region, float sourceV) {
		if (region == null) {
			return 0.0f;
		}
		return region.getV0() + (region.getV1() - region.getV0()) * clampUnit(sourceV);
	}

	private float clampUnit(float value) {
		if (Float.isNaN(value) || value <= 0.0f) {
			return 0.0f;
		}
		if (value >= 1.0f) {
			return 1.0f;
		}
		return value;
	}

	private void drawTexturedDiagnostic(FloatBuffer projectionMatrix) throws Exception {
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(true);
		drawTexturedDiagnosticBatches(projectionMatrix);
	}

	private void drawProjectedObjectBridge(int occluderIndexCount, FloatBuffer projectionMatrix) throws Exception {
		if (occluderIndexCount > 0) {
			drawProjectedObjectBridgeDepthPrepass(occluderIndexCount);
		}
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthMask(true);
		try {
			drawTexturedDiagnosticBatches(projectionMatrix);
		} finally {
			gl.glDepthMask(true);
		}
	}

	private void drawProjectedObjectBridgeDepthPrepass(int occluderIndexCount) throws Exception {
		gl.glDisable(gl.GL_TEXTURE_2D);
		gl.glDisable(gl.GL_BLEND);
		gl.glDisable(gl.GL_ALPHA_TEST);
		gl.glDisableClientState(gl.GL_COLOR_ARRAY);
		gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, occluderIndexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			POSITION_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			0L);
		gl.glDepthMask(true);
		gl.glColorMask(false, false, false, false);
		try {
			gl.glDrawElements(gl.GL_TRIANGLES, occluderIndexCount, gl.GL_UNSIGNED_INT, 0L);
		} finally {
			gl.glColorMask(true, true, true, true);
		}
	}

	private void drawTexturedDiagnosticBatches(FloatBuffer projectionMatrix) throws Exception {
		if (texturedDiagnosticAlpha < 0.999f) {
			gl.glEnable(gl.GL_BLEND);
			gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			gl.glDisable(gl.GL_BLEND);
			gl.glEnable(gl.GL_ALPHA_TEST);
			gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
		}
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, texturedIndexBufferId);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			POSITION_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			0L);
		boolean wireGeometry = RendererGeometrySettings.getMode() == RendererGeometrySettings.Mode.WIRE;
		gl.glPolygonMode(gl.GL_FRONT_AND_BACK, wireGeometry ? gl.GL_LINE : gl.GL_FILL);
		WorldMeshBatchDrawState drawState = new WorldMeshBatchDrawState(projectionMatrix);
		try {
			for (WorldMeshTextureBatch batch : textureBatches) {
				if (batch.indexCount <= 0) {
					continue;
				}
				if (batch.isTextureBacked()) {
					configureMaterialPass(batch.materialPass);
					drawTexturedBatch(batch, drawState);
				} else {
					configureMaterialPass(batch.materialPass);
					drawFlatColorBatch(batch, drawState);
				}
			}
		} finally {
			drawState.close();
			if (wireGeometry) {
				gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
			}
		}
	}

	private void configureMaterialPass(StaticWorldMaterialPass materialPass) throws Exception {
		if (texturedDiagnosticAlpha < 0.999f) {
			return;
		}
		gl.glDisable(gl.GL_BLEND);
		if (materialPass == StaticWorldMaterialPass.CUTOUT) {
			gl.glEnable(gl.GL_ALPHA_TEST);
			gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
		} else {
			gl.glDisable(gl.GL_ALPHA_TEST);
		}
	}

	private int countDrawableTextureBatches() {
		int count = 0;
		for (WorldMeshTextureBatch batch : textureBatches) {
			if (batch.indexCount > 0) {
				count++;
			}
		}
		return count;
	}

	private void drawTexturedBatch(WorldMeshTextureBatch batch, WorldMeshBatchDrawState drawState) throws Exception {
		gl.glEnable(gl.GL_TEXTURE_2D);
		if (texturedDiagnosticAlpha < 0.999f) {
			gl.glDisable(gl.GL_ALPHA_TEST);
			gl.glEnable(gl.GL_BLEND);
			gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			gl.glDisable(gl.GL_BLEND);
			if (batch.materialPass == StaticWorldMaterialPass.CUTOUT) {
				gl.glEnable(gl.GL_ALPHA_TEST);
				gl.glAlphaFunc(gl.GL_GREATER, 0.5f);
			} else {
				gl.glDisable(gl.GL_ALPHA_TEST);
			}
		}
		drawState.useTextured();
		drawState.bindTexture(batch.textureId);
		gl.glColor4f(1.0f, 1.0f, 1.0f, texturedDiagnosticAlpha);
		drawWorldMeshBatchElements(batch);
	}

	private void drawFlatColorBatch(WorldMeshTextureBatch batch, WorldMeshBatchDrawState drawState) throws Exception {
		gl.glDisable(gl.GL_TEXTURE_2D);
		gl.glDisable(gl.GL_ALPHA_TEST);
		drawState.useFlatColor();
		drawWorldMeshBatchElements(batch);
	}

	private void bindTexturedBatchState(FloatBuffer projectionMatrix) throws Exception {
		boolean shaderActive = texturedShaderEnabled && texturedShaderProgram != null;
		if (shaderActive) {
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
			texturedShaderProgram.useWorld(projectionMatrix, true);
			texturedShaderProgram.bindWorldTextureAttributes(
				POSITION_COMPONENT_COUNT,
				TEXTURE_COORD_COMPONENT_COUNT,
				TEXTURE_LIGHT_COMPONENT_COUNT,
				1,
				1,
				3,
				SHADER_STRIDE_BYTES,
				SHADER_POSITION_OFFSET_BYTES,
				SHADER_TEXTURE_COORD_OFFSET_BYTES,
				SHADER_MATERIAL_COLOR_OFFSET_BYTES,
				SHADER_LEGACY_LIGHT_OFFSET_BYTES,
				SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES,
				SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES);
			return;
		}
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glEnableClientState(gl.GL_COLOR_ARRAY);
		gl.glColorPointer(
			TEXTURE_LIGHT_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			TEXTURE_LIGHT_OFFSET_BYTES);
		gl.glEnableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(
			TEXTURE_COORD_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			TEXTURE_COORD_OFFSET_BYTES);
	}

	private void bindFlatColorBatchState(FloatBuffer projectionMatrix) throws Exception {
		boolean shaderActive = texturedShaderEnabled && texturedShaderProgram != null;
		if (shaderActive) {
			gl.glBindBuffer(gl.GL_ARRAY_BUFFER, shaderVertexBufferId);
			texturedShaderProgram.useWorld(projectionMatrix, false);
			texturedShaderProgram.bindWorldFlatColorAttributes(
				POSITION_COMPONENT_COUNT,
				TEXTURE_COORD_COMPONENT_COUNT,
				TEXTURE_LIGHT_COMPONENT_COUNT,
				SHADER_STRIDE_BYTES,
				SHADER_POSITION_OFFSET_BYTES,
				SHADER_TEXTURE_COORD_OFFSET_BYTES,
				SHADER_MATERIAL_COLOR_OFFSET_BYTES,
				SHADER_LEGACY_LIGHT_OFFSET_BYTES,
				SHADER_BASE_LEGACY_LIGHT_OFFSET_BYTES,
				SHADER_RAW_MATERIAL_COLOR_OFFSET_BYTES);
			return;
		}
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glEnableClientState(gl.GL_COLOR_ARRAY);
		gl.glColorPointer(
			MATERIAL_COLOR_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			MATERIAL_COLOR_OFFSET_BYTES);
	}

	private void unbindShaderBatchState() throws Exception {
		if (texturedShaderEnabled && texturedShaderProgram != null) {
			texturedShaderProgram.unbindWorldTextureAttributes();
			gl.glUseProgram(0);
		}
	}

	private final class WorldMeshBatchDrawState implements AutoCloseable {
		private static final int MODE_NONE = 0;
		private static final int MODE_TEXTURED = 1;
		private static final int MODE_FLAT_COLOR = 2;

		private final FloatBuffer projectionMatrix;
		private final boolean shaderActive;
		private int mode = MODE_NONE;
		private int boundTextureId = Integer.MIN_VALUE;

		private WorldMeshBatchDrawState(FloatBuffer projectionMatrix) {
			this.projectionMatrix = projectionMatrix;
			this.shaderActive = texturedShaderEnabled && texturedShaderProgram != null;
		}

		private void useTextured() throws Exception {
			if (mode == MODE_TEXTURED) {
				return;
			}
			unbindActiveShaderState();
			bindTexturedBatchState(projectionMatrix);
			mode = MODE_TEXTURED;
		}

		private void useFlatColor() throws Exception {
			if (mode == MODE_FLAT_COLOR) {
				return;
			}
			unbindActiveShaderState();
			bindFlatColorBatchState(projectionMatrix);
			mode = MODE_FLAT_COLOR;
		}

		private void bindTexture(int textureId) throws Exception {
			if (boundTextureId == textureId) {
				return;
			}
			gl.glBindTexture(gl.GL_TEXTURE_2D, textureId);
			boundTextureId = textureId;
		}

		private void unbindActiveShaderState() throws Exception {
			if (shaderActive && mode != MODE_NONE) {
				unbindShaderBatchState();
			}
		}

		@Override
		public void close() throws Exception {
			unbindActiveShaderState();
			mode = MODE_NONE;
		}
	}

	private void drawWorldMeshBatchElements(WorldMeshTextureBatch batch) throws Exception {
		boolean wallDepthPriority = batch.modelKind == Renderer3DModelKind.WALL;
		if (wallDepthPriority) {
			enableWallDepthPriority();
		}
		try {
			gl.glDrawElements(
				gl.GL_TRIANGLES,
				batch.indexCount,
				gl.GL_UNSIGNED_INT,
				batch.startIndex * 4L);
		} finally {
			if (wallDepthPriority) {
				disableWallDepthPriority();
			}
		}
	}

	private void enableWallDepthPriority() throws Exception {
		gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(WALL_DEPTH_PRIORITY_FACTOR, WALL_DEPTH_PRIORITY_UNITS);
	}

	private void disableWallDepthPriority() throws Exception {
		gl.glPolygonOffset(0.0f, 0.0f);
		gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
	}

	private void drawWireframeDiagnostic(int indexCount) throws Exception {
		gl.glDisable(gl.GL_TEXTURE_2D);
		gl.glDisableClientState(gl.GL_TEXTURE_COORD_ARRAY);
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glEnable(gl.GL_BLEND);
		gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
		gl.glBindBuffer(gl.GL_ARRAY_BUFFER, vertexBufferId);
		gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
		gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
		gl.glColor4f(0.1f, 0.85f, 1.0f, 0.35f);
		gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
		gl.glVertexPointer(
			POSITION_COMPONENT_COUNT,
			gl.GL_FLOAT,
			STRIDE_BYTES,
			0L);
		gl.glDrawElements(gl.GL_TRIANGLES, indexCount, gl.GL_UNSIGNED_INT, 0L);
	}

	@Override
	public void close() throws Exception {
		if (closed) {
			return;
		}
		closed = true;
		gl.glDeleteBuffers(vertexBufferId);
		gl.glDeleteBuffers(shaderVertexBufferId);
		gl.glDeleteBuffers(indexBufferId);
		gl.glDeleteBuffers(texturedIndexBufferId);
		gl.glDeleteBuffers(occluderIndexBufferId);
	}
}


final class WorldMeshUploadState {
	final WorldMeshUploadSignature signature;
	final int texturedIndexCount;
	final int occluderIndexCount;

	WorldMeshUploadState(
		WorldMeshUploadSignature signature,
		int texturedIndexCount,
		int occluderIndexCount) {
		this.signature = signature;
		this.texturedIndexCount = Math.max(0, texturedIndexCount);
		this.occluderIndexCount = Math.max(0, occluderIndexCount);
	}

	boolean matches(WorldMeshUploadSignature other) {
		return signature != null && signature.equals(other);
	}
}

enum WorldMeshTriangleFilter {
	ALL_STATIC,
	STATIC_OBJECTS;

	boolean accepts(Renderer3DModelKind kind) {
		if (this == ALL_STATIC) {
			return true;
		}
		return kind == Renderer3DModelKind.GAME_OBJECT
			|| kind == Renderer3DModelKind.WALL_OBJECT;
	}
}

final class WorldMeshUploadSignature {
	static final long FNV_OFFSET_BASIS = -3750763034362895579L;
	static final long FNV_PRIME = 1099511628211L;

	final int vertexCount;
	final int indexCount;
	final int triangleCount;
	final int centerX;
	final int centerY;
	final int viewportWidth;
	final int viewportHeight;
	final int brightnessBits;
	final int fogStrengthBits;
	final int lightingModeBits;
	final int geometryModeBits;
	final boolean hideRoofs;
	final WorldMeshTriangleFilter triangleFilter;
	final long hash;

	WorldMeshUploadSignature(
		int vertexCount,
		int indexCount,
		int triangleCount,
		int centerX,
		int centerY,
		int viewportWidth,
		int viewportHeight,
		int brightnessBits,
		int fogStrengthBits,
		int lightingModeBits,
		int geometryModeBits,
		boolean hideRoofs,
		WorldMeshTriangleFilter triangleFilter,
		long hash) {
		this.vertexCount = vertexCount;
		this.indexCount = indexCount;
		this.triangleCount = triangleCount;
		this.centerX = centerX;
		this.centerY = centerY;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.brightnessBits = brightnessBits;
		this.fogStrengthBits = fogStrengthBits;
		this.lightingModeBits = lightingModeBits;
		this.geometryModeBits = geometryModeBits;
		this.hideRoofs = hideRoofs;
		this.triangleFilter = triangleFilter == null ? WorldMeshTriangleFilter.ALL_STATIC : triangleFilter;
		this.hash = hash;
	}

	static WorldMeshUploadSignature from(
		Renderer3DMeshFrame meshFrame,
		int vertexCount,
		int indexCount) {
		return from(meshFrame, vertexCount, indexCount, WorldMeshTriangleFilter.ALL_STATIC);
	}

	static WorldMeshUploadSignature from(
		Renderer3DMeshFrame meshFrame,
		int vertexCount,
		int indexCount,
		WorldMeshTriangleFilter triangleFilter) {
		int triangleCount = meshFrame.getTriangleCount();
		int brightnessBits = Float.floatToIntBits(RendererDayNightCycle.currentBrightnessMultiplier());
		int fogStrengthBits = RendererFogSettings.getMode().ordinal();
		int lightingModeBits = RendererLightingSettings.getMode().ordinal();
		int geometryModeBits = RendererGeometrySettings.getMode().ordinal();
		boolean hideRoofs = Config.C_HIDE_ROOFS;
		long hash = FNV_OFFSET_BASIS;
		hash = mix(hash, vertexCount);
		hash = mix(hash, indexCount);
		hash = mix(hash, triangleCount);
		hash = mix(hash, meshFrame.getCenterX());
		hash = mix(hash, meshFrame.getCenterY());
		hash = mix(hash, meshFrame.getViewportWidth());
		hash = mix(hash, meshFrame.getViewportHeight());
		hash = mix(hash, brightnessBits);
		hash = mix(hash, fogStrengthBits);
		hash = mix(hash, lightingModeBits);
		hash = mix(hash, geometryModeBits);
		hash = mix(hash, hideRoofs ? 1 : 0);
		hash = mix(hash, triangleFilter == null ? 0 : triangleFilter.ordinal());

		float[] vertices = meshFrame.getVertices();
		int vertexFloatCount = Math.min(vertices.length, vertexCount * Renderer3DMeshFrame.FLOATS_PER_VERTEX);
		for (int i = 0; i < vertexFloatCount; i++) {
			hash = mix(hash, Float.floatToIntBits(vertices[i]));
		}

		int[] indices = meshFrame.getIndices();
		for (int i = 0; i < indexCount && i < indices.length; i++) {
			hash = mix(hash, indices[i]);
		}

		int[] triangleTextures = meshFrame.getTriangleTextures();
		int[] triangleLegacyDrawOrders = meshFrame.getTriangleLegacyDrawOrders();
		Renderer3DModelKind[] triangleModelKinds = meshFrame.getTriangleModelKinds();
		for (int i = 0; i < triangleCount; i++) {
			hash = mix(hash, i < triangleTextures.length ? triangleTextures[i] : 0);
			hash = mix(hash, i < triangleLegacyDrawOrders.length ? triangleLegacyDrawOrders[i] : -1);
			Renderer3DModelKind kind =
				i < triangleModelKinds.length ? triangleModelKinds[i] : Renderer3DModelKind.UNCLASSIFIED;
			hash = mix(hash, kind.ordinal());
		}

		return new WorldMeshUploadSignature(
			vertexCount,
			indexCount,
			triangleCount,
			meshFrame.getCenterX(),
			meshFrame.getCenterY(),
			meshFrame.getViewportWidth(),
			meshFrame.getViewportHeight(),
			brightnessBits,
			fogStrengthBits,
			lightingModeBits,
			geometryModeBits,
			hideRoofs,
			triangleFilter,
			hash);
	}

	static long mix(long hash, int value) {
		hash ^= value & 0xffffffffL;
		return hash * FNV_PRIME;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorldMeshUploadSignature)) {
			return false;
		}
		WorldMeshUploadSignature signature = (WorldMeshUploadSignature) other;
		return vertexCount == signature.vertexCount
			&& indexCount == signature.indexCount
			&& triangleCount == signature.triangleCount
			&& centerX == signature.centerX
			&& centerY == signature.centerY
			&& viewportWidth == signature.viewportWidth
			&& viewportHeight == signature.viewportHeight
			&& brightnessBits == signature.brightnessBits
			&& fogStrengthBits == signature.fogStrengthBits
			&& lightingModeBits == signature.lightingModeBits
			&& geometryModeBits == signature.geometryModeBits
			&& hideRoofs == signature.hideRoofs
			&& triangleFilter == signature.triangleFilter
			&& hash == signature.hash;
	}

	@Override
	public int hashCode() {
		int result = vertexCount;
		result = 31 * result + indexCount;
		result = 31 * result + triangleCount;
		result = 31 * result + centerX;
		result = 31 * result + centerY;
		result = 31 * result + viewportWidth;
		result = 31 * result + viewportHeight;
		result = 31 * result + brightnessBits;
		result = 31 * result + fogStrengthBits;
		result = 31 * result + lightingModeBits;
		result = 31 * result + geometryModeBits;
		result = 31 * result + (hideRoofs ? 1 : 0);
		result = 31 * result + triangleFilter.ordinal();
		result = 31 * result + (int) (hash ^ (hash >>> 32));
		return result;
	}
}

final class WorldMeshTextureBatch {
	final int textureId;
	final StaticWorldMaterialPass materialPass;
	final Renderer3DModelKind modelKind;
	int startIndex;
	int indexCount;
	int writeIndex;

	WorldMeshTextureBatch(int textureId) {
		this(textureId, StaticWorldMaterialPass.OPAQUE, Renderer3DModelKind.UNCLASSIFIED);
	}

	WorldMeshTextureBatch(
		int textureId,
		StaticWorldMaterialPass materialPass,
		Renderer3DModelKind modelKind) {
		this.textureId = textureId;
		this.materialPass = materialPass;
		this.modelKind = modelKind == null ? Renderer3DModelKind.UNCLASSIFIED : modelKind;
	}

	boolean isTextureBacked() {
		return textureId >= 0;
	}
}

final class WorldMeshTextureBatchKey {
	final int textureId;
	final StaticWorldMaterialPass materialPass;
	final Renderer3DModelKind modelKind;

	WorldMeshTextureBatchKey(
		int textureId,
		StaticWorldMaterialPass materialPass,
		Renderer3DModelKind modelKind) {
		this.textureId = textureId;
		this.materialPass = materialPass == null ? StaticWorldMaterialPass.OPAQUE : materialPass;
		this.modelKind = modelKind == null ? Renderer3DModelKind.UNCLASSIFIED : modelKind;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof WorldMeshTextureBatchKey)) {
			return false;
		}
		WorldMeshTextureBatchKey key = (WorldMeshTextureBatchKey) other;
		return textureId == key.textureId
			&& materialPass == key.materialPass
			&& modelKind == key.modelKind;
	}

	@Override
	public int hashCode() {
		int result = textureId;
		result = 31 * result + materialPass.ordinal();
		result = 31 * result + modelKind.ordinal();
		return result;
	}
}
