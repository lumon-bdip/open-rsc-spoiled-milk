package orsc;

final class RemasterGlowMaskBuild {
	final RemasterGlowMask mask;
	final boolean cacheHit;
	final boolean rebuild;
	final String reason;

	RemasterGlowMaskBuild(RemasterGlowMask mask, boolean cacheHit, boolean rebuild, String reason) {
		this.mask = mask;
		this.cacheHit = cacheHit;
		this.rebuild = rebuild;
		this.reason = reason;
	}
}
