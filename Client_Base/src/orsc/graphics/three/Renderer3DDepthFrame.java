package orsc.graphics.three;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class Renderer3DDepthFrame {
	private static final int SPRITE_DEPTH_PADDING_PIXELS = 4;
	private final int width;
	private final int height;
	private final int bufferWidth;
	private final int bufferHeight;
	private final int bufferOriginX;
	private final int bufferOriginY;
	private final int[] depth;
	private final int[] color;
	private final Renderer3DModelKind[] kind;
	private final int[] legacyDrawOrder;
	private final int[] faceId;
	private final int[] modelIndex;
	private final boolean[] spriteClipMask;
	private final int[] spriteClipRowMinX;
	private final int[] spriteClipRowMaxX;
	private final int clipMinX;
	private final int clipMinY;
	private final int clipMaxX;
	private final int clipMaxY;
	private int acceptedFaceCount;
	private int triangleCount;
	private int pixelWriteCount;

	private Renderer3DDepthFrame(
		int width,
		int height,
		SpriteClipMask spriteClipMask) {
		this.width = width;
		this.height = height;
		this.spriteClipMask = spriteClipMask.mask;
		this.spriteClipRowMinX = spriteClipMask.rowMinX;
		this.spriteClipRowMaxX = spriteClipMask.rowMaxX;
		this.clipMinX = spriteClipMask.minX;
		this.clipMinY = spriteClipMask.minY;
		this.clipMaxX = spriteClipMask.maxX;
		this.clipMaxY = spriteClipMask.maxY;
		this.bufferWidth = spriteClipMask.width;
		this.bufferHeight = spriteClipMask.height;
		this.bufferOriginX = spriteClipMask.originX;
		this.bufferOriginY = spriteClipMask.originY;
		int bufferSize = Math.max(0, this.bufferWidth * this.bufferHeight);
		this.depth = new int[bufferSize];
		this.color = new int[bufferSize];
		this.kind = new Renderer3DModelKind[bufferSize];
		this.legacyDrawOrder = new int[bufferSize];
		this.faceId = new int[bufferSize];
		this.modelIndex = new int[bufferSize];
		Arrays.fill(this.depth, Integer.MAX_VALUE);
		Arrays.fill(this.kind, Renderer3DModelKind.UNCLASSIFIED);
		Arrays.fill(this.legacyDrawOrder, -1);
		Arrays.fill(this.faceId, -1);
		Arrays.fill(this.modelIndex, -1);
	}

	static Renderer3DDepthFrame render(Renderer3DFrame frame, EnumSet<Renderer3DModelKind> includedKinds) {
		SpriteClipMask spriteClipMask = SpriteClipMask.from(frame);
		Renderer3DDepthFrame depthFrame = new Renderer3DDepthFrame(
			frame.getViewportWidth(),
			frame.getViewportHeight(),
			spriteClipMask);
		List<Renderer3DFrame.FaceCommand> faces = frame.getWorldFaces();
		for (Renderer3DFrame.FaceCommand face : faces) {
			if (!includedKinds.contains(face.getModelKind())) {
				continue;
			}
			if (face.getRenderVertexCount() < 3) {
				continue;
			}
			depthFrame.acceptedFaceCount++;
			for (int vertex = 1; vertex < face.getRenderVertexCount() - 1; vertex++) {
				depthFrame.rasterizeTriangle(frame, face, 0, vertex, vertex + 1);
			}
		}
		return depthFrame;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getAcceptedFaceCount() {
		return acceptedFaceCount;
	}

	public int getTriangleCount() {
		return triangleCount;
	}

	public int getPixelWriteCount() {
		return pixelWriteCount;
	}

	public void copyColorTo(int[] destination) {
		if (destination == null || destination.length < width * height) {
			return;
		}
		Arrays.fill(destination, 0);
		for (int y = 0; y < bufferHeight; y++) {
			System.arraycopy(
				color,
				y * bufferWidth,
				destination,
				(bufferOriginY + y) * width + bufferOriginX,
				bufferWidth);
		}
	}

	public void copyKindColorTo(int[] destination) {
		if (destination == null || destination.length < width * height) {
			return;
		}
		Arrays.fill(destination, colorForKind(Renderer3DModelKind.UNCLASSIFIED));
		for (int y = 0; y < bufferHeight; y++) {
			int sourceOffset = y * bufferWidth;
			int destinationOffset = (bufferOriginY + y) * width + bufferOriginX;
			for (int x = 0; x < bufferWidth; x++) {
				destination[destinationOffset + x] = colorForKind(kind[sourceOffset + x]);
			}
		}
	}

	public void copyEntityOccluderMaskTo(int[] destination) {
		if (destination == null || destination.length < width * height) {
			return;
		}
		Arrays.fill(destination, 0);
		for (int y = 0; y < bufferHeight; y++) {
			int sourceOffset = y * bufferWidth;
			int destinationOffset = (bufferOriginY + y) * width + bufferOriginX;
			for (int x = 0; x < bufferWidth; x++) {
				Renderer3DModelKind modelKind = kind[sourceOffset + x];
				if (modelKind == Renderer3DModelKind.TERRAIN) {
					destination[destinationOffset + x] = 0xAA267D4D;
				} else if (modelKind == Renderer3DModelKind.WALL) {
					destination[destinationOffset + x] = 0xFFFF3333;
				} else if (modelKind == Renderer3DModelKind.WALL_OBJECT) {
					destination[destinationOffset + x] = 0xFFFFAA33;
				} else if (modelKind == Renderer3DModelKind.GAME_OBJECT) {
					destination[destinationOffset + x] = 0xFF33AAFF;
				} else if (modelKind == Renderer3DModelKind.ROOF) {
					destination[destinationOffset + x] = 0xAA9966FF;
				}
			}
		}
	}

	public boolean isCloserThan(int x, int y, int cameraDepth, int tolerance) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0 || cameraDepth <= 0) {
			return false;
		}
		int depthValue = depth[pixel];
		return depthValue != Integer.MAX_VALUE && depthValue + Math.max(0, tolerance) < cameraDepth;
	}

	public boolean isEntityOccluderCloserThan(int x, int y, int cameraDepth, int tolerance) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0 || cameraDepth <= 0) {
			return false;
		}
		int depthValue = depth[pixel];
		if (depthValue == Integer.MAX_VALUE || depthValue + Math.max(0, tolerance) >= cameraDepth) {
			return false;
		}
		Renderer3DModelKind modelKind = kind[pixel];
		return isEntitySpriteOccluder(modelKind);
	}

	public boolean isEntityOccluderAfterSprite(
		int x,
		int y,
		int spriteLegacyDrawOrder,
		int cameraDepth,
		int tolerance) {
		return getEntityOccluderKindAfterSprite(x, y, spriteLegacyDrawOrder, cameraDepth, tolerance) != null;
	}

	public Renderer3DModelKind getEntityOccluderKindAfterSprite(
		int x,
		int y,
		int spriteLegacyDrawOrder,
		int cameraDepth,
		int tolerance) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0) {
			return null;
		}
		int depthValue = depth[pixel];
		if (depthValue == Integer.MAX_VALUE) {
			return null;
		}
		Renderer3DModelKind modelKind = kind[pixel];
		if (!isEntitySpriteOccluder(modelKind)) {
			return null;
		}
		int occluderLegacyDrawOrder = legacyDrawOrder[pixel];
		if (modelKind == Renderer3DModelKind.GAME_OBJECT || modelKind == Renderer3DModelKind.WALL_OBJECT) {
			return cameraDepth > 0 && depthValue + Math.max(0, tolerance) < cameraDepth ? modelKind : null;
		}
		if (spriteLegacyDrawOrder >= 0 && occluderLegacyDrawOrder >= 0) {
			return occluderLegacyDrawOrder > spriteLegacyDrawOrder ? modelKind : null;
		}
		return cameraDepth > 0 && depthValue + Math.max(0, tolerance) < cameraDepth ? modelKind : null;
	}

	public int getDepthAt(int x, int y) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0) {
			return Integer.MAX_VALUE;
		}
		return depth[pixel];
	}

	public int getLegacyDrawOrderAt(int x, int y) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0) {
			return -1;
		}
		return legacyDrawOrder[pixel];
	}

	public int getFaceIdAt(int x, int y) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0) {
			return -1;
		}
		return faceId[pixel];
	}

	public int getModelIndexAt(int x, int y) {
		int pixel = localPixelIndex(x, y);
		if (pixel < 0) {
			return -1;
		}
		return modelIndex[pixel];
	}

	private static boolean isEntitySpriteOccluder(Renderer3DModelKind modelKind) {
		return modelKind == Renderer3DModelKind.TERRAIN
			|| modelKind == Renderer3DModelKind.WALL
			|| modelKind == Renderer3DModelKind.ROOF
			|| modelKind == Renderer3DModelKind.GAME_OBJECT
			|| modelKind == Renderer3DModelKind.WALL_OBJECT;
	}

	private void rasterizeTriangle(Renderer3DFrame frame, Renderer3DFrame.FaceCommand face, int a, int b, int c) {
		int ax = frame.getCenterX() + face.getRenderScreenX()[a];
		int ay = frame.getCenterY() + face.getRenderScreenY()[a];
		int bx = frame.getCenterX() + face.getRenderScreenX()[b];
		int by = frame.getCenterY() + face.getRenderScreenY()[b];
		int cx = frame.getCenterX() + face.getRenderScreenX()[c];
		int cy = frame.getCenterY() + face.getRenderScreenY()[c];
		int az = face.getRenderCameraZ()[a];
		int bz = face.getRenderCameraZ()[b];
		int cz = face.getRenderCameraZ()[c];

		int minX = Math.max(clipMinX, Math.min(ax, Math.min(bx, cx)));
		int maxX = Math.min(clipMaxX, Math.max(ax, Math.max(bx, cx)));
		int minY = Math.max(clipMinY, Math.min(ay, Math.min(by, cy)));
		int maxY = Math.min(clipMaxY, Math.max(ay, Math.max(by, cy)));
		if (minX > maxX || minY > maxY) {
			return;
		}

		int area = edge(ax, ay, bx, by, cx, cy);
		if (area == 0) {
			return;
		}

		triangleCount++;
		boolean positiveArea = area > 0;
		for (int y = minY; y <= maxY; y++) {
			int rowMinX = minX;
			int rowMaxX = maxX;
			if (spriteClipMask != null) {
				int localRow = y - bufferOriginY;
				if (localRow < 0 || localRow >= bufferHeight) {
					continue;
				}
				int maskRowMinX = spriteClipRowMinX[localRow];
				if (maskRowMinX < 0) {
					continue;
				}
				rowMinX = Math.max(rowMinX, bufferOriginX + maskRowMinX);
				rowMaxX = Math.min(rowMaxX, bufferOriginX + spriteClipRowMaxX[localRow]);
				if (rowMinX > rowMaxX) {
					continue;
				}
			}
			int rowOffset = (y - bufferOriginY) * bufferWidth;
			for (int x = rowMinX; x <= rowMaxX; x++) {
				int pixel = rowOffset + (x - bufferOriginX);
				if (spriteClipMask != null && !spriteClipMask[pixel]) {
					continue;
				}
				int w0 = edge(bx, by, cx, cy, x, y);
				int w1 = edge(cx, cy, ax, ay, x, y);
				int w2 = edge(ax, ay, bx, by, x, y);
				if (positiveArea ? (w0 < 0 || w1 < 0 || w2 < 0) : (w0 > 0 || w1 > 0 || w2 > 0)) {
					continue;
				}

				int z = (int) (((long) w0 * az + (long) w1 * bz + (long) w2 * cz) / area);
				if (z <= 0) {
					continue;
				}
				if (z < depth[pixel]) {
					depth[pixel] = z;
					color[pixel] = shadeColor(face.getColor(), z, face.getModelKind());
					kind[pixel] = face.getModelKind();
					legacyDrawOrder[pixel] = face.getLegacyDrawOrder();
					faceId[pixel] = face.getFaceId();
					modelIndex[pixel] = face.getModelIndex();
					pixelWriteCount++;
				}
			}
		}
	}

	private static int shadeColor(int color, int depth, Renderer3DModelKind kind) {
		int factor = Math.max(96, 256 - Math.max(0, depth - 256) / 12);
		if (kind == Renderer3DModelKind.WALL || kind == Renderer3DModelKind.WALL_OBJECT) {
			factor = Math.max(80, factor - 24);
		}
		int red = ((color >> 16) & 0xFF) * factor >> 8;
		int green = ((color >> 8) & 0xFF) * factor >> 8;
		int blue = (color & 0xFF) * factor >> 8;
		return red << 16 | green << 8 | blue;
	}

	private static int colorForKind(Renderer3DModelKind kind) {
		if (kind == Renderer3DModelKind.TERRAIN) {
			return 0xFF267D4D;
		}
		if (kind == Renderer3DModelKind.WALL) {
			return 0xFFE04C3A;
		}
		if (kind == Renderer3DModelKind.ROOF) {
			return 0xFFE8C33A;
		}
		if (kind == Renderer3DModelKind.GAME_OBJECT) {
			return 0xFF50B8E8;
		}
		if (kind == Renderer3DModelKind.WALL_OBJECT) {
			return 0xFFE8843A;
		}
		return 0xFF404040;
	}

	private static int edge(int ax, int ay, int bx, int by, int px, int py) {
		return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
	}

	private int localPixelIndex(int x, int y) {
		if (x < clipMinX || y < clipMinY || x > clipMaxX || y > clipMaxY) {
			return -1;
		}
		int localX = x - bufferOriginX;
		int localY = y - bufferOriginY;
		if (localX < 0 || localY < 0 || localX >= bufferWidth || localY >= bufferHeight) {
			return -1;
		}
		int pixel = localY * bufferWidth + localX;
		if (spriteClipMask != null && !spriteClipMask[pixel]) {
			return -1;
		}
		return pixel;
	}

	private static final class SpriteClipMask {
		private final boolean[] mask;
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;
		private final int originX;
		private final int originY;
		private final int width;
		private final int height;
		private final int[] rowMinX;
		private final int[] rowMaxX;

		private SpriteClipMask(
			boolean[] mask,
			int minX,
			int minY,
			int maxX,
			int maxY,
			int originX,
			int originY,
			int width,
			int height,
			int[] rowMinX,
			int[] rowMaxX) {
			this.mask = mask;
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.originX = originX;
			this.originY = originY;
			this.width = width;
			this.height = height;
			this.rowMinX = rowMinX;
			this.rowMaxX = rowMaxX;
		}

		private static SpriteClipMask from(Renderer3DFrame frame) {
			int width = frame.getViewportWidth();
			int height = frame.getViewportHeight();
			List<Renderer3DFrame.SpriteAnchor> anchors = frame.getSpriteAnchors();
			if (width <= 0 || height <= 0) {
				return empty();
			}
			if (anchors.isEmpty()) {
				return full(width, height);
			}
			int minX = width;
			int minY = height;
			int maxX = -1;
			int maxY = -1;
			for (Renderer3DFrame.SpriteAnchor anchor : anchors) {
				int left = Math.max(0, anchor.getDrawX() - SPRITE_DEPTH_PADDING_PIXELS);
				int top = Math.max(0, anchor.getDrawY() - SPRITE_DEPTH_PADDING_PIXELS);
				int right = Math.min(width - 1, anchor.getDrawX() + Math.max(0, anchor.getDrawWidth()) + SPRITE_DEPTH_PADDING_PIXELS);
				int bottom = Math.min(height - 1, anchor.getDrawY() + Math.max(0, anchor.getDrawHeight()) + SPRITE_DEPTH_PADDING_PIXELS);
				if (left > right || top > bottom) {
					continue;
				}
				minX = Math.min(minX, left);
				minY = Math.min(minY, top);
				maxX = Math.max(maxX, right);
				maxY = Math.max(maxY, bottom);
			}
			if (maxX < minX || maxY < minY) {
				return empty();
			}
			int maskWidth = maxX - minX + 1;
			int maskHeight = maxY - minY + 1;
			boolean[] mask = new boolean[maskWidth * maskHeight];
			int[] rowMinX = new int[maskHeight];
			int[] rowMaxX = new int[maskHeight];
			Arrays.fill(rowMinX, -1);
			Arrays.fill(rowMaxX, -1);
			for (Renderer3DFrame.SpriteAnchor anchor : anchors) {
				int left = Math.max(0, anchor.getDrawX() - SPRITE_DEPTH_PADDING_PIXELS);
				int top = Math.max(0, anchor.getDrawY() - SPRITE_DEPTH_PADDING_PIXELS);
				int right = Math.min(width - 1, anchor.getDrawX() + Math.max(0, anchor.getDrawWidth()) + SPRITE_DEPTH_PADDING_PIXELS);
				int bottom = Math.min(height - 1, anchor.getDrawY() + Math.max(0, anchor.getDrawHeight()) + SPRITE_DEPTH_PADDING_PIXELS);
				if (left > right || top > bottom) {
					continue;
				}
				for (int y = top; y <= bottom; y++) {
					int row = y - minY;
					int localLeft = left - minX;
					int localRight = right - minX;
					if (rowMinX[row] < 0) {
						rowMinX[row] = localLeft;
						rowMaxX[row] = localRight;
					} else {
						rowMinX[row] = Math.min(rowMinX[row], localLeft);
						rowMaxX[row] = Math.max(rowMaxX[row], localRight);
					}
					Arrays.fill(
						mask,
						row * maskWidth + localLeft,
						row * maskWidth + localRight + 1,
						true);
				}
			}
			return new SpriteClipMask(
				mask,
				minX,
				minY,
				maxX,
				maxY,
				minX,
				minY,
				maskWidth,
				maskHeight,
				rowMinX,
				rowMaxX);
		}

		private static SpriteClipMask full(int width, int height) {
			return new SpriteClipMask(null, 0, 0, width - 1, height - 1, 0, 0, width, height, null, null);
		}

		private static SpriteClipMask empty() {
			return new SpriteClipMask(null, 0, 0, -1, -1, 0, 0, 0, 0, null, null);
		}
	}
}
