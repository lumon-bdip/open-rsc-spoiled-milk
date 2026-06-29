package orsc;

import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DWorldChunkFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RemasterShadowMaskBuilder {
	static final int REMASTER_SHADOW_MASK_GRID_SIZE = 512;
	static final float REMASTER_SHADOW_MASK_BASE_ALPHA = 0.42f;
	static final float REMASTER_SHADOW_MASK_MAX_ALPHA = 0.58f;
	static final float REMASTER_SHADOW_MASK_MIN_LENGTH = 96.0f;
	static final float REMASTER_SHADOW_MASK_MAX_LENGTH = 1792.0f;
	static final float REMASTER_SHADOW_MASK_MIN_WIDTH = 24.0f;
	static final float REMASTER_SHADOW_MASK_MIN_DRAW_ALPHA = 0.018f;
	static final int REMASTER_SHADOW_MASK_TEXTURE_SIZE = 1024;
	static final float REMASTER_SHADOW_MASK_TEXTURE_PADDING = 384.0f;
	static final int REMASTER_SHADOW_MASK_BLUR_RADIUS = 7;
	static final float REMASTER_SHADOW_MASK_AZIMUTH_BUCKET_DEGREES = 6.0f;
	static final float REMASTER_SHADOW_MASK_ELEVATION_BUCKET_DEGREES = 3.0f;
	static final float REMASTER_SHADOW_MASK_CENTER_RETAIN = 0.82f;
	static final float REMASTER_SHADOW_MASK_BLUR_BOOST = 1.18f;
	static final float REMASTER_SHADOW_MASK_CLIP_START_OFFSET = 24.0f;
	static final boolean REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP = RemasterShadowClassifier.DIRECT_WALL_SEGMENT_CLIP;

	private RemasterTerrainShadowMask cache;
	private long cacheSignature;
	private boolean lastCacheHit;
	private boolean lastRebuild;

	RemasterShadowMaskBuild build(
		Renderer3DWorldChunkFrame chunkFrame,
		RemasterShadowRoofCoverage roofCoverage) {
		List<RemasterTerrainShadowCaster> casters = buildRemasterTerrainShadowCasters(chunkFrame, roofCoverage);
		if (casters.isEmpty()) {
			return null;
		}
		int stripCasterCount = countRemasterShadowMaskCasters(casters, RemasterTerrainShadowCaster.STYLE_STRIP);
		int softSceneryCasterCount =
			countRemasterShadowMaskCasters(casters, RemasterTerrainShadowCaster.STYLE_SOFT_SCENERY);
		RemasterTerrainShadowMask mask = buildMaskTexture(roofCoverage, chunkFrame, casters);
		if (mask == null) {
			return null;
		}
		return new RemasterShadowMaskBuild(mask, stripCasterCount, softSceneryCasterCount, lastCacheHit, lastRebuild);
	}

	void clear() {
		cache = null;
		cacheSignature = 0L;
		lastCacheHit = false;
		lastRebuild = false;
	}

	private RemasterTerrainShadowMask buildMaskTexture(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame chunkFrame,
		List<RemasterTerrainShadowCaster> casters) {
		RemasterShadowMaskBounds bounds = RemasterShadowMaskBounds.from(roofCoverage, chunkFrame);
		if (bounds == null) {
			return null;
		}
		bounds = bounds.withPadding(REMASTER_SHADOW_MASK_TEXTURE_PADDING);
		long signature = remasterTerrainShadowMaskSignature(casters, bounds);
		if (cache != null && cacheSignature == signature) {
			lastCacheHit = true;
			lastRebuild = false;
			return cache;
		}
		lastCacheHit = false;
		lastRebuild = true;
		int width = REMASTER_SHADOW_MASK_TEXTURE_SIZE;
		int height = REMASTER_SHADOW_MASK_TEXTURE_SIZE;
		float[] sourceAlpha = new float[width * height];
		float[] horizontalAlpha = new float[width * height];
		float[] blurredAlpha = new float[width * height];
		Map<Long, List<RemasterTerrainShadowCaster>> casterGrid =
			buildRemasterTerrainShadowCasterGrid(casters);
		for (int y = 0; y < height; y++) {
			float z = bounds.zAt(y, height);
			int row = y * width;
			for (int x = 0; x < width; x++) {
				sourceAlpha[row + x] =
					remasterTerrainShadowMaskAlpha(roofCoverage, casterGrid, bounds.xAt(x, width), z);
			}
		}
		blurHorizontal(sourceAlpha, horizontalAlpha, width, height, REMASTER_SHADOW_MASK_BLUR_RADIUS);
		blurVertical(horizontalAlpha, blurredAlpha, width, height, REMASTER_SHADOW_MASK_BLUR_RADIUS);
		ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
		int visiblePixels = 0;
		for (int index = 0; index < sourceAlpha.length; index++) {
			float alpha = Math.max(
				sourceAlpha[index] * REMASTER_SHADOW_MASK_CENTER_RETAIN,
				blurredAlpha[index] * REMASTER_SHADOW_MASK_BLUR_BOOST);
			alpha = clamp(alpha, 0.0f, REMASTER_SHADOW_MASK_MAX_ALPHA);
			if (alpha > REMASTER_SHADOW_MASK_MIN_DRAW_ALPHA) {
				visiblePixels++;
			}
			pixels.put((byte) 0);
			pixels.put((byte) 0);
			pixels.put((byte) 0);
			pixels.put((byte) Math.round(alpha * 255.0f));
		}
		if (visiblePixels <= 0) {
			return null;
		}
		pixels.flip();
		cache = new RemasterTerrainShadowMask(
			signature,
			width,
			height,
			visiblePixels,
			bounds.minX,
			bounds.minZ,
			bounds.invSpanX(),
			bounds.invSpanZ(),
			pixels);
		cacheSignature = signature;
		return cache;
	}

	private long remasterTerrainShadowMaskSignature(
		List<RemasterTerrainShadowCaster> casters,
		RemasterShadowMaskBounds bounds) {
		long hash = 0xcbf29ce484222325L;
		hash = mix(hash, REMASTER_SHADOW_MASK_TEXTURE_SIZE);
		hash = mix(hash, REMASTER_SHADOW_MASK_BLUR_RADIUS);
		hash = mix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_TEXTURE_PADDING));
		hash = mix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_CENTER_RETAIN));
		hash = mix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_BLUR_BOOST));
		hash = mix(hash, Float.floatToIntBits(REMASTER_SHADOW_MASK_CLIP_START_OFFSET));
		hash = mix(hash, REMASTER_SHADOW_MASK_DIRECT_WALL_SEGMENT_CLIP ? 1 : 0);
		hash = mix(hash, Float.floatToIntBits(bounds.minX));
		hash = mix(hash, Float.floatToIntBits(bounds.maxX));
		hash = mix(hash, Float.floatToIntBits(bounds.minZ));
		hash = mix(hash, Float.floatToIntBits(bounds.maxZ));
		hash = mix(hash, casters == null ? 0 : casters.size());
		if (casters != null) {
			for (RemasterTerrainShadowCaster caster : casters) {
				hash = caster.mixSignature(hash);
			}
		}
		return hash;
	}

	private float remasterTerrainShadowMaskAlpha(
		RemasterShadowRoofCoverage roofCoverage,
		Map<Long, List<RemasterTerrainShadowCaster>> casterGrid,
		float x,
		float z) {
		List<RemasterTerrainShadowCaster> casters =
			casterGrid.get(remasterShadowMaskCellKey(remasterShadowMaskCell(x), remasterShadowMaskCell(z)));
		if (casters == null || casters.isEmpty()) {
			return 0.0f;
		}
		float alpha = 0.0f;
		for (RemasterTerrainShadowCaster caster : casters) {
			float casterAlpha = caster.alphaAt(x, z);
			if (casterAlpha <= 0.0f || caster.isBlockedBy(roofCoverage, x, z)) {
				continue;
			}
			alpha = Math.max(alpha, casterAlpha);
		}
		return clamp(alpha, 0.0f, REMASTER_SHADOW_MASK_MAX_ALPHA);
	}

	private static void blurHorizontal(float[] source, float[] target, int width, int height, int radius) {
		if (radius <= 0) {
			System.arraycopy(source, 0, target, 0, source.length);
			return;
		}
		for (int y = 0; y < height; y++) {
			int row = y * width;
			for (int x = 0; x < width; x++) {
				float total = 0.0f;
				int count = 0;
				for (int offset = -radius; offset <= radius; offset++) {
					int sampleX = x + offset;
					if (sampleX < 0 || sampleX >= width) {
						continue;
					}
					total += source[row + sampleX];
					count++;
				}
				target[row + x] = count <= 0 ? 0.0f : total / count;
			}
		}
	}

	private static void blurVertical(float[] source, float[] target, int width, int height, int radius) {
		if (radius <= 0) {
			System.arraycopy(source, 0, target, 0, source.length);
			return;
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float total = 0.0f;
				int count = 0;
				for (int offset = -radius; offset <= radius; offset++) {
					int sampleY = y + offset;
					if (sampleY < 0 || sampleY >= height) {
						continue;
					}
					total += source[sampleY * width + x];
					count++;
				}
				target[y * width + x] = count <= 0 ? 0.0f : total / count;
			}
		}
	}

	private List<RemasterTerrainShadowCaster> buildRemasterTerrainShadowCasters(
		Renderer3DWorldChunkFrame chunkFrame,
		RemasterShadowRoofCoverage roofCoverage) {
		List<RemasterTerrainShadowCaster> casters = new ArrayList<RemasterTerrainShadowCaster>();
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
			int casterCount = chunk.getShadowCasterCount();
			for (int index = 0; index < casterCount; index++) {
				Renderer3DWorldChunkFrame.ShadowCaster caster = chunk.getShadowCaster(index);
				if (!RemasterShadowClassifier.isEligibleCaster(caster)) {
					continue;
				}
				if (RemasterShadowClassifier.roofClassificationForCaster(roofCoverage, chunk, caster) != 0) {
					continue;
				}
				RemasterTerrainShadowCaster projected = RemasterTerrainShadowCaster.from(caster, chunk.getPlane());
				if (projected != null) {
					casters.add(projected);
				}
			}
		}
		return casters;
	}

	private int countRemasterShadowMaskCasters(List<RemasterTerrainShadowCaster> casters, int style) {
		int count = 0;
		for (RemasterTerrainShadowCaster caster : casters) {
			if (caster.style == style) {
				count++;
			}
		}
		return count;
	}

	private Map<Long, List<RemasterTerrainShadowCaster>> buildRemasterTerrainShadowCasterGrid(
		List<RemasterTerrainShadowCaster> casters) {
		Map<Long, List<RemasterTerrainShadowCaster>> grid =
			new HashMap<Long, List<RemasterTerrainShadowCaster>>();
		for (RemasterTerrainShadowCaster caster : casters) {
			int minCellX = remasterShadowMaskCell(caster.minX);
			int maxCellX = remasterShadowMaskCell(caster.maxX);
			int minCellZ = remasterShadowMaskCell(caster.minZ);
			int maxCellZ = remasterShadowMaskCell(caster.maxZ);
			for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
				for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
					long key = remasterShadowMaskCellKey(cellX, cellZ);
					List<RemasterTerrainShadowCaster> cellCasters = grid.get(key);
					if (cellCasters == null) {
						cellCasters = new ArrayList<RemasterTerrainShadowCaster>();
						grid.put(key, cellCasters);
					}
					cellCasters.add(caster);
				}
			}
		}
		return grid;
	}

	private static int remasterShadowMaskCell(float value) {
		return (int) Math.floor(value / REMASTER_SHADOW_MASK_GRID_SIZE);
	}

	private static long remasterShadowMaskCellKey(int cellX, int cellZ) {
		return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
	}

	static float remasterShadowMaskLightAzimuthDegrees() {
		return quantize(
			RendererRemasterLightSettings.getAzimuthDegrees(),
			REMASTER_SHADOW_MASK_AZIMUTH_BUCKET_DEGREES);
	}

	static float remasterShadowMaskLightElevationDegrees() {
		return clamp(
			quantize(
				RendererRemasterLightSettings.getElevationDegrees(),
				REMASTER_SHADOW_MASK_ELEVATION_BUCKET_DEGREES),
			5.0f,
			85.0f);
	}

	static float quantize(float value, float bucketSize) {
		if (bucketSize <= 0.0f) {
			return value;
		}
		return Math.round(value / bucketSize) * bucketSize;
	}

	static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	static long mix(long hash, int value) {
		hash ^= value;
		return hash * 0x100000001b3L;
	}
}

final class RemasterShadowMaskBuild {
	final RemasterTerrainShadowMask mask;
	final int stripCasterCount;
	final int softSceneryCasterCount;
	final boolean cacheHit;
	final boolean rebuild;

	RemasterShadowMaskBuild(
		RemasterTerrainShadowMask mask,
		int stripCasterCount,
		int softSceneryCasterCount,
		boolean cacheHit,
		boolean rebuild) {
		this.mask = mask;
		this.stripCasterCount = stripCasterCount;
		this.softSceneryCasterCount = softSceneryCasterCount;
		this.cacheHit = cacheHit;
		this.rebuild = rebuild;
	}
}

final class RemasterTerrainShadowMask {
	final long signature;
	final int width;
	final int height;
	final int visiblePixels;
	final float minX;
	final float minZ;
	final float invSpanX;
	final float invSpanZ;
	private final ByteBuffer pixels;

	RemasterTerrainShadowMask(
		long signature,
		int width,
		int height,
		int visiblePixels,
		float minX,
		float minZ,
		float invSpanX,
		float invSpanZ,
		ByteBuffer pixels) {
		this.signature = signature;
		this.width = width;
		this.height = height;
		this.visiblePixels = visiblePixels;
		this.minX = minX;
		this.minZ = minZ;
		this.invSpanX = invSpanX;
		this.invSpanZ = invSpanZ;
		this.pixels = pixels;
	}

	float u(float x) {
		return RemasterShadowMaskBuilder.clamp((x - minX) * invSpanX, 0.0f, 1.0f);
	}

	float v(float z) {
		return RemasterShadowMaskBuilder.clamp((z - minZ) * invSpanZ, 0.0f, 1.0f);
	}

	ByteBuffer pixels() {
		ByteBuffer duplicate = pixels.duplicate();
		duplicate.position(0);
		return duplicate;
	}
}

final class RemasterShadowMaskBounds {
	final float minX;
	final float maxX;
	final float minZ;
	final float maxZ;

	RemasterShadowMaskBounds(float minX, float maxX, float minZ, float maxZ) {
		this.minX = minX;
		this.maxX = maxX;
		this.minZ = minZ;
		this.maxZ = maxZ;
	}

	static RemasterShadowMaskBounds from(
		RemasterShadowRoofCoverage roofCoverage,
		Renderer3DWorldChunkFrame chunkFrame) {
		float minX = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float minZ = Float.POSITIVE_INFINITY;
		float maxZ = Float.NEGATIVE_INFINITY;
		for (Renderer3DWorldChunkFrame.ChunkMesh chunk : chunkFrame.getChunks()) {
			int triangleCount = Math.min(chunk.getTriangleCount(), chunk.getIndexCount() / 3);
			for (int triangle = 0; triangle < triangleCount; triangle++) {
				if (chunk.getTriangleModelKind(triangle) != Renderer3DModelKind.TERRAIN) {
					continue;
				}
				if (RemasterShadowClassifier.roofClassificationForTriangle(roofCoverage, chunk, triangle) != 0) {
					continue;
				}
				int sourceIndex = triangle * 3;
				for (int corner = 0; corner < 3; corner++) {
					int vertex = chunk.getIndex(sourceIndex + corner);
					int coord = vertex * 3;
					float x = chunk.getVertexCoord(coord);
					float z = chunk.getVertexCoord(coord + 2);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minZ = Math.min(minZ, z);
					maxZ = Math.max(maxZ, z);
				}
			}
		}
		if (!Float.isFinite(minX)
			|| !Float.isFinite(maxX)
			|| !Float.isFinite(minZ)
			|| !Float.isFinite(maxZ)
			|| maxX <= minX
			|| maxZ <= minZ) {
			return null;
		}
		return new RemasterShadowMaskBounds(minX, maxX, minZ, maxZ);
	}

	RemasterShadowMaskBounds withPadding(float padding) {
		return new RemasterShadowMaskBounds(
			minX - padding,
			maxX + padding,
			minZ - padding,
			maxZ + padding);
	}

	float xAt(int pixelX, int width) {
		return minX + ((pixelX + 0.5f) / Math.max(1.0f, (float) width)) * spanX();
	}

	float zAt(int pixelY, int height) {
		return minZ + ((pixelY + 0.5f) / Math.max(1.0f, (float) height)) * spanZ();
	}

	float invSpanX() {
		return 1.0f / Math.max(1.0f, spanX());
	}

	float invSpanZ() {
		return 1.0f / Math.max(1.0f, spanZ());
	}

	private float spanX() {
		return maxX - minX;
	}

	private float spanZ() {
		return maxZ - minZ;
	}
}

final class RemasterTerrainShadowCaster {
	static final int STYLE_STRIP = 0;
	static final int STYLE_SOFT_SCENERY = 1;

	final int style;
	final int plane;
	final float baseX0;
	final float baseZ0;
	final float baseX1;
	final float baseZ1;
	final float centerX;
	final float centerZ;
	final float length;
	final float halfWidth;
	private final float edgeX;
	private final float edgeZ;
	private final float edgeLength;
	final float directionX;
	final float directionZ;
	final float opacity;
	final float minX;
	final float maxX;
	final float minZ;
	final float maxZ;

	private RemasterTerrainShadowCaster(
		int style,
		int plane,
		float baseX0,
		float baseZ0,
		float baseX1,
		float baseZ1,
		float length,
		float halfWidth,
		float directionX,
		float directionZ,
		float opacity) {
		float edgeDx = baseX1 - baseX0;
		float edgeDz = baseZ1 - baseZ0;
		float edgeLength = (float) Math.sqrt(edgeDx * edgeDx + edgeDz * edgeDz);
		this.style = style;
		this.plane = plane;
		this.baseX0 = baseX0;
		this.baseZ0 = baseZ0;
		this.baseX1 = baseX1;
		this.baseZ1 = baseZ1;
		this.centerX = (baseX0 + baseX1) * 0.5f;
		this.centerZ = (baseZ0 + baseZ1) * 0.5f;
		this.length = length;
		this.halfWidth = halfWidth;
		this.edgeLength = edgeLength;
		this.edgeX = edgeDx / Math.max(0.0001f, edgeLength);
		this.edgeZ = edgeDz / Math.max(0.0001f, edgeLength);
		this.directionX = directionX;
		this.directionZ = directionZ;
		this.opacity = RemasterShadowMaskBuilder.clamp(opacity, 0.0f, 1.0f);
		float projectedX0 = baseX0 + directionX * length;
		float projectedZ0 = baseZ0 + directionZ * length;
		float projectedX1 = baseX1 + directionX * length;
		float projectedZ1 = baseZ1 + directionZ * length;
		this.minX = Math.min(Math.min(baseX0, baseX1), Math.min(projectedX0, projectedX1)) - halfWidth;
		this.maxX = Math.max(Math.max(baseX0, baseX1), Math.max(projectedX0, projectedX1)) + halfWidth;
		this.minZ = Math.min(Math.min(baseZ0, baseZ1), Math.min(projectedZ0, projectedZ1)) - halfWidth;
		this.maxZ = Math.max(Math.max(baseZ0, baseZ1), Math.max(projectedZ0, projectedZ1)) + halfWidth;
	}

	static RemasterTerrainShadowCaster from(
		Renderer3DWorldChunkFrame.ShadowCaster source,
		int plane) {
		if (source == null || source.getHeight() <= 0) {
			return null;
		}
		double azimuth = Math.toRadians(RemasterShadowMaskBuilder.remasterShadowMaskLightAzimuthDegrees());
		double elevation = Math.toRadians(RemasterShadowMaskBuilder.remasterShadowMaskLightElevationDegrees());
		float lightX = (float) (Math.cos(elevation) * Math.cos(azimuth));
		float lightY = Math.max(0.12f, Math.abs((float) Math.sin(elevation)));
		float lightZ = (float) (Math.cos(elevation) * Math.sin(azimuth));
		float horizontalLength = (float) Math.sqrt(lightX * lightX + lightZ * lightZ);
		if (horizontalLength <= 0.0001f) {
			return null;
		}
		float shadowDirectionX = -lightX / horizontalLength;
		float shadowDirectionZ = -lightZ / horizontalLength;
		float height = Math.max(0.0f, source.getHeight());
		float length = RemasterShadowMaskBuilder.clamp(
			RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_MIN_LENGTH + height * (horizontalLength / lightY) * 2.0f,
			RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_MIN_LENGTH,
			RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_MAX_LENGTH);
		float width = Math.max(
			source.getWidth(),
			Math.max(
				Math.abs(source.getBaseX1() - source.getBaseX0()),
				Math.abs(source.getBaseZ1() - source.getBaseZ0())));
		Renderer3DModelKind kind = source.getModelKind();
		int style = kind == Renderer3DModelKind.GAME_OBJECT ? STYLE_SOFT_SCENERY : STYLE_STRIP;
		float halfWidth = remasterShadowHalfWidth(kind, width);
		float baseX0 = source.getBaseX0();
		float baseZ0 = source.getBaseZ0();
		float baseX1 = source.getBaseX1();
		float baseZ1 = source.getBaseZ1();
		if (style == STYLE_SOFT_SCENERY) {
			float centerX = (source.getBaseX0() + source.getBaseX1()) * 0.5f;
			float centerZ = (source.getBaseZ0() + source.getBaseZ1()) * 0.5f;
			float normalX = -shadowDirectionZ;
			float normalZ = shadowDirectionX;
			halfWidth = Math.max(36.0f, Math.min(132.0f, width * 0.30f));
			baseX0 = centerX - normalX * halfWidth;
			baseZ0 = centerZ - normalZ * halfWidth;
			baseX1 = centerX + normalX * halfWidth;
			baseZ1 = centerZ + normalZ * halfWidth;
		}
		return new RemasterTerrainShadowCaster(
			style,
			plane,
			baseX0,
			baseZ0,
			baseX1,
			baseZ1,
			length,
			halfWidth,
			shadowDirectionX,
			shadowDirectionZ,
			source.getOpacity() / 255.0f);
	}

	private static float remasterShadowHalfWidth(Renderer3DModelKind kind, float width) {
		if (kind == Renderer3DModelKind.GAME_OBJECT) {
			return Math.max(36.0f, Math.min(132.0f, width * 0.30f));
		}
		if (kind == Renderer3DModelKind.WALL_OBJECT) {
			return Math.max(32.0f, width * 0.28f);
		}
		return Math.max(RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_MIN_WIDTH, Math.min(96.0f, width * 0.18f));
	}

	float alphaAt(float x, float z) {
		if (style == STYLE_SOFT_SCENERY) {
			return alphaAtSoftScenery(x, z);
		}
		float determinant = edgeX * directionZ - edgeZ * directionX;
		if (edgeLength > 0.0001f && Math.abs(determinant) > 0.08f) {
			float px = x - baseX0;
			float pz = z - baseZ0;
			float edgeAlong = (px * directionZ - pz * directionX) / determinant;
			float shadowAlong = (edgeX * pz - edgeZ * px) / determinant;
			if (shadowAlong < 0.0f || shadowAlong > length) {
				return 0.0f;
			}
			if (edgeAlong < -halfWidth || edgeAlong > edgeLength + halfWidth) {
				return 0.0f;
			}
			float sideFade = Math.min(
				(edgeAlong + halfWidth) / Math.max(16.0f, halfWidth),
				(edgeLength + halfWidth - edgeAlong) / Math.max(16.0f, halfWidth));
			float endFade = (length - shadowAlong) / Math.max(64.0f, length * 0.22f);
			return RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_BASE_ALPHA
				* opacity
				* RemasterShadowMaskBuilder.clamp(Math.min(sideFade, endFade), 0.0f, 1.0f);
		}
		return alphaAtCenterFallback(x, z);
	}

	private float alphaAtSoftScenery(float x, float z) {
		float dx = x - centerX;
		float dz = z - centerZ;
		float along = dx * directionX + dz * directionZ;
		if (along < -halfWidth * 0.45f || along > length) {
			return 0.0f;
		}
		float across = Math.abs(dx * -directionZ + dz * directionX);
		float farFade = smoothStep(0.0f, Math.max(96.0f, length * 0.28f), length - along);
		float startFade = smoothStep(-halfWidth * 0.35f, Math.max(16.0f, halfWidth * 0.45f), along);
		float trunkWidth = Math.max(9.0f, Math.min(26.0f, halfWidth * 0.24f));
		float trunk = 1.0f - smoothStep(trunkWidth, trunkWidth * 3.2f, across);
		float trunkFade = smoothStep(0.0f, Math.max(36.0f, halfWidth * 0.75f), along)
			* smoothStep(0.0f, Math.max(112.0f, length * 0.36f), length - along);
		float canopyCenter = Math.min(length * 0.36f, Math.max(72.0f, halfWidth * 1.45f));
		float canopyRadiusAlong = Math.max(112.0f, halfWidth * 2.35f);
		float canopyRadiusAcross = Math.max(48.0f, halfWidth * 1.25f);
		float canopyAlong = (along - canopyCenter) / canopyRadiusAlong;
		float canopyAcross = across / canopyRadiusAcross;
		float canopyDistance = canopyAlong * canopyAlong + canopyAcross * canopyAcross;
		float canopy = 1.0f - smoothStep(0.16f, 1.0f, canopyDistance);
		float shapedAlpha = Math.max(trunk * trunkFade * 0.85f, canopy * startFade * farFade * 0.55f);
		return RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_BASE_ALPHA
			* opacity
			* RemasterShadowMaskBuilder.clamp(shapedAlpha, 0.0f, 1.0f);
	}

	private float alphaAtCenterFallback(float x, float z) {
		float dx = x - centerX;
		float dz = z - centerZ;
		float along = dx * directionX + dz * directionZ;
		if (along < 0.0f || along > length) {
			return 0.0f;
		}
		float across = Math.abs(dx * -directionZ + dz * directionX);
		if (across > halfWidth) {
			return 0.0f;
		}
		float sideFade = (halfWidth - across) / Math.max(16.0f, halfWidth * 0.25f);
		float endFade = (length - along) / Math.max(64.0f, length * 0.22f);
		return RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_BASE_ALPHA
			* opacity
			* RemasterShadowMaskBuilder.clamp(Math.min(sideFade, endFade), 0.0f, 1.0f);
	}

	boolean isBlockedBy(RemasterShadowRoofCoverage roofCoverage, float x, float z) {
		if (roofCoverage == null) {
			return false;
		}
		float along = shadowAlongAt(x, z);
		if (along <= RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_CLIP_START_OFFSET) {
			return false;
		}
		float sourceX = x - directionX * along;
		float sourceZ = z - directionZ * along;
		float startX = sourceX + directionX * RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_CLIP_START_OFFSET;
		float startZ = sourceZ + directionZ * RemasterShadowMaskBuilder.REMASTER_SHADOW_MASK_CLIP_START_OFFSET;
		return roofCoverage.crossesShadowBlocker(plane, startX, startZ, x, z);
	}

	private float shadowAlongAt(float x, float z) {
		if (style == STYLE_SOFT_SCENERY) {
			float dx = x - centerX;
			float dz = z - centerZ;
			return dx * directionX + dz * directionZ;
		}
		float determinant = edgeX * directionZ - edgeZ * directionX;
		if (edgeLength > 0.0001f && Math.abs(determinant) > 0.08f) {
			float px = x - baseX0;
			float pz = z - baseZ0;
			return (edgeX * pz - edgeZ * px) / determinant;
		}
		float dx = x - centerX;
		float dz = z - centerZ;
		return dx * directionX + dz * directionZ;
	}

	long mixSignature(long hash) {
		hash = RemasterShadowMaskBuilder.mix(hash, style);
		hash = RemasterShadowMaskBuilder.mix(hash, plane);
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(baseX0));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(baseZ0));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(baseX1));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(baseZ1));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(centerX));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(centerZ));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(length));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(halfWidth));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(directionX));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(directionZ));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(opacity));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(minX));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(maxX));
		hash = RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(minZ));
		return RemasterShadowMaskBuilder.mix(hash, Float.floatToIntBits(maxZ));
	}

	private static float smoothStep(float edge0, float edge1, float value) {
		if (edge1 <= edge0) {
			return value >= edge1 ? 1.0f : 0.0f;
		}
		float t = RemasterShadowMaskBuilder.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
		return t * t * (3.0f - 2.0f * t);
	}
}
