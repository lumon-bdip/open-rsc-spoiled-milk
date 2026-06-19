package orsc.graphics.three;

import java.util.EnumSet;
import java.util.List;

public final class Renderer3DDepthFrame {
	private final int width;
	private final int height;
	private final int[] depth;
	private final int[] color;
	private int acceptedFaceCount;
	private int triangleCount;
	private int pixelWriteCount;

	private Renderer3DDepthFrame(int width, int height) {
		this.width = width;
		this.height = height;
		this.depth = new int[width * height];
		this.color = new int[width * height];
		for (int i = 0; i < this.depth.length; i++) {
			this.depth[i] = Integer.MAX_VALUE;
		}
	}

	static Renderer3DDepthFrame render(Renderer3DFrame frame, EnumSet<Renderer3DModelKind> includedKinds) {
		Renderer3DDepthFrame depthFrame = new Renderer3DDepthFrame(frame.getViewportWidth(), frame.getViewportHeight());
		List<Renderer3DFrame.FaceCommand> faces = frame.getWorldFaces();
		for (Renderer3DFrame.FaceCommand face : faces) {
			if (!includedKinds.contains(face.getModelKind())) {
				continue;
			}
			if (face.getVertexCount() < 3) {
				continue;
			}
			depthFrame.acceptedFaceCount++;
			for (int vertex = 1; vertex < face.getVertexCount() - 1; vertex++) {
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
		if (destination == null || destination.length < color.length) {
			return;
		}
		for (int i = 0; i < color.length; i++) {
			destination[i] = color[i];
		}
	}

	private void rasterizeTriangle(Renderer3DFrame frame, Renderer3DFrame.FaceCommand face, int a, int b, int c) {
		int ax = frame.getCenterX() + face.getScreenX()[a];
		int ay = frame.getCenterY() + face.getScreenY()[a];
		int bx = frame.getCenterX() + face.getScreenX()[b];
		int by = frame.getCenterY() + face.getScreenY()[b];
		int cx = frame.getCenterX() + face.getScreenX()[c];
		int cy = frame.getCenterY() + face.getScreenY()[c];
		int az = face.getCameraZ()[a];
		int bz = face.getCameraZ()[b];
		int cz = face.getCameraZ()[c];

		int minX = Math.max(0, Math.min(ax, Math.min(bx, cx)));
		int maxX = Math.min(width - 1, Math.max(ax, Math.max(bx, cx)));
		int minY = Math.max(0, Math.min(ay, Math.min(by, cy)));
		int maxY = Math.min(height - 1, Math.max(ay, Math.max(by, cy)));
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
			for (int x = minX; x <= maxX; x++) {
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
				int pixel = y * width + x;
				if (z < depth[pixel]) {
					depth[pixel] = z;
					color[pixel] = shadeColor(face.getColor(), z, face.getModelKind());
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

	private static int edge(int ax, int ay, int bx, int by, int px, int py) {
		return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
	}
}
