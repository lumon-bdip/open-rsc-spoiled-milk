package orsc;

final class WorldTextureUploadStats {
	final int referencedTextures;
	final int cachedTextures;
	final int uploadedTextures;
	final int missingTextures;
	final int atlases;

	WorldTextureUploadStats(
		int referencedTextures,
		int cachedTextures,
		int uploadedTextures,
		int missingTextures,
		int atlases) {
		this.referencedTextures = referencedTextures;
		this.cachedTextures = cachedTextures;
		this.uploadedTextures = uploadedTextures;
		this.missingTextures = missingTextures;
		this.atlases = atlases;
	}
}
