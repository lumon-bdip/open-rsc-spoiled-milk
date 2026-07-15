package orsc;

final class RemasterShadowMaskBuild {
	final RemasterTerrainShadowMask mask;
	final int stripCasterCount;
	final int softSceneryCasterCount;
	final int contactCasterCount;
	final boolean cacheHit;
	final boolean rebuild;
	final String reason;

	RemasterShadowMaskBuild(
		RemasterTerrainShadowMask mask,
		int stripCasterCount,
		int softSceneryCasterCount,
		int contactCasterCount,
		boolean cacheHit,
		boolean rebuild,
		String reason) {
		this.mask = mask;
		this.stripCasterCount = stripCasterCount;
		this.softSceneryCasterCount = softSceneryCasterCount;
		this.contactCasterCount = contactCasterCount;
		this.cacheHit = cacheHit;
		this.rebuild = rebuild;
		this.reason = reason == null ? "" : reason;
	}
}
