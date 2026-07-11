package orsc.graphics.three;

public enum Renderer3DMaterialFamily {
	UNCLASSIFIED(0, "unclassified"),
	TERRAIN(1, "terrain"),
	WATER(2, "water"),
	WALL(3, "wall"),
	ROOF(4, "roof"),
	SCENERY(5, "scenery"),
	FOLIAGE(6, "foliage"),
	ORE(7, "ore"),
	EMISSIVE(8, "emissive"),
	EFFECT(9, "effect");

	private final int shaderId;
	private final String telemetryName;

	Renderer3DMaterialFamily(int shaderId, String telemetryName) {
		this.shaderId = shaderId;
		this.telemetryName = telemetryName;
	}

	public int getShaderId() {
		return shaderId;
	}

	public String getTelemetryName() {
		return telemetryName;
	}

	public static Renderer3DMaterialFamily fromShaderId(int shaderId) {
		for (Renderer3DMaterialFamily family : values()) {
			if (family.shaderId == shaderId) {
				return family;
			}
		}
		return UNCLASSIFIED;
	}
}
