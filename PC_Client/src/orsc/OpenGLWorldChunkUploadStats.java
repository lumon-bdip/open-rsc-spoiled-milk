package orsc;

final class OpenGLWorldChunkUploadStats {
	static final OpenGLWorldChunkUploadStats EMPTY =
		new OpenGLWorldChunkUploadStats(
			0,
			0,
			0,
			0,
			0,
			"empty",
			0L,
			OpenGLWorldChunkRenderer.CHUNK_UPLOAD_BUDGET_NANOS);

	final int requestedChunks;
	final int uploadedChunks;
	final int reusedChunks;
	final int deferredChunks;
	final int evictedChunks;
	final String reason;
	final long budgetUsedNanos;
	final long budgetLimitNanos;

	OpenGLWorldChunkUploadStats(
		int requestedChunks,
		int uploadedChunks,
		int reusedChunks,
		int deferredChunks,
		int evictedChunks,
		String reason,
		long budgetUsedNanos,
		long budgetLimitNanos) {
		this.requestedChunks = requestedChunks;
		this.uploadedChunks = uploadedChunks;
		this.reusedChunks = reusedChunks;
		this.deferredChunks = deferredChunks;
		this.evictedChunks = evictedChunks;
		this.reason = reason == null || reason.trim().isEmpty() ? "unknown" : reason;
		this.budgetUsedNanos = Math.max(0L, budgetUsedNanos);
		this.budgetLimitNanos = Math.max(0L, budgetLimitNanos);
	}
}
