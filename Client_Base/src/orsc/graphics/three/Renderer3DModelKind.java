package orsc.graphics.three;

public enum Renderer3DModelKind {
	UNCLASSIFIED("other"),
	TERRAIN("terrain"),
	WALL("wall"),
	ROOF("roof"),
	GAME_OBJECT("object"),
	WALL_OBJECT("wallObject");

	private final String telemetryName;

	Renderer3DModelKind(String telemetryName) {
		this.telemetryName = telemetryName;
	}

	public String getTelemetryName() {
		return telemetryName;
	}
}
