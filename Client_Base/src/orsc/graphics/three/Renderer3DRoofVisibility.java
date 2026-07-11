package orsc.graphics.three;

/**
 * Per-frame roof visibility resolved from the saved roof option and the
 * player's current floor/coverage state.
 */
public enum Renderer3DRoofVisibility {
	VISIBLE(true, true),
	HIDDEN_BY_SETTING(false, false),
	HIDDEN_INDOORS(false, false),
	HIDDEN_ABOVE_ACTIVE_FLOOR(false, false);

	private final boolean roofsVisible;
	private final boolean structuresAboveActiveFloorVisible;

	Renderer3DRoofVisibility(
		boolean roofsVisible,
		boolean structuresAboveActiveFloorVisible) {
		this.roofsVisible = roofsVisible;
		this.structuresAboveActiveFloorVisible = structuresAboveActiveFloorVisible;
	}

	public static Renderer3DRoofVisibility resolve(
		boolean hideRoofsSetting,
		int activePlane,
		boolean playerTileCovered) {
		if (hideRoofsSetting) {
			return HIDDEN_BY_SETTING;
		}
		if (activePlane > 0) {
			return HIDDEN_ABOVE_ACTIVE_FLOOR;
		}
		if (playerTileCovered) {
			return HIDDEN_INDOORS;
		}
		return VISIBLE;
	}

	public boolean areRoofsVisible() {
		return roofsVisible;
	}

	public boolean usesAutomaticRoofCameraZoom() {
		return this != HIDDEN_BY_SETTING;
	}

	public boolean isWorldChunkModelKindVisible(
		Renderer3DModelKind modelKind,
		int activePlane,
		int chunkPlane) {
		if (modelKind == Renderer3DModelKind.ROOF) {
			return roofsVisible;
		}
		if (modelKind == Renderer3DModelKind.WALL && chunkPlane > activePlane) {
			return structuresAboveActiveFloorVisible;
		}
		return true;
	}
}
