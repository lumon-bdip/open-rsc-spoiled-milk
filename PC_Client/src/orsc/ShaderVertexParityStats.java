package orsc;

import java.nio.FloatBuffer;

final class ShaderVertexParityStats {
	static final ShaderVertexParityStats EMPTY = new ShaderVertexParityStats(0, 0, 0, 0, 0.0f);
	static final int[] PARITY_COLOR_OFFSETS = new int[] {3, 4, 5, 11};
	static final int[] SHADER_COLOR_OFFSETS = new int[] {5, 6, 7, 8};

	final int vertexCount;
	final int positionMismatches;
	final int textureCoordMismatches;
	final int colorMismatches;
	final float maxAbsoluteDelta;

	ShaderVertexParityStats(
		int vertexCount,
		int positionMismatches,
		int textureCoordMismatches,
		int colorMismatches,
		float maxAbsoluteDelta) {
		this.vertexCount = vertexCount;
		this.positionMismatches = positionMismatches;
		this.textureCoordMismatches = textureCoordMismatches;
		this.colorMismatches = colorMismatches;
		this.maxAbsoluteDelta = maxAbsoluteDelta;
	}

	static ShaderVertexParityStats compare(
		FloatBuffer parityVertices,
		FloatBuffer shaderVertices,
		int vertexCount) {
		int positionMismatches = 0;
		int textureCoordMismatches = 0;
		int colorMismatches = 0;
		float maxAbsoluteDelta = 0.0f;
		for (int vertex = 0; vertex < vertexCount; vertex++) {
			int parity = vertex * OpenGLWorldMeshRenderer.UPLOAD_FLOATS_PER_VERTEX;
			int shader = vertex * OpenGLWorldMeshRenderer.SHADER_UPLOAD_FLOATS_PER_VERTEX;
			for (int component = 0; component < 3; component++) {
				float delta = Math.abs(parityVertices.get(parity + component) - shaderVertices.get(shader + component));
				maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
				if (delta != 0.0f) {
					positionMismatches++;
				}
			}
			for (int component = 0; component < 2; component++) {
				float delta = Math.abs(parityVertices.get(parity + 6 + component) - shaderVertices.get(shader + 3 + component));
				maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
				if (delta != 0.0f) {
					textureCoordMismatches++;
				}
			}
			for (int component = 0; component < PARITY_COLOR_OFFSETS.length; component++) {
				float delta = Math.abs(
					parityVertices.get(parity + PARITY_COLOR_OFFSETS[component])
						- shaderVertices.get(shader + SHADER_COLOR_OFFSETS[component]));
				maxAbsoluteDelta = Math.max(maxAbsoluteDelta, delta);
				if (delta != 0.0f) {
					colorMismatches++;
				}
			}
		}
		return new ShaderVertexParityStats(
			vertexCount,
			positionMismatches,
			textureCoordMismatches,
			colorMismatches,
			maxAbsoluteDelta);
	}
}
