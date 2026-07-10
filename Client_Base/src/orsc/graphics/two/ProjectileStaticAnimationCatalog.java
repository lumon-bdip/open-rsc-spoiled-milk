package orsc.graphics.two;

import com.openrsc.client.entityhandling.EntityHandler.PROJECTILE_TYPES;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Caster-to-target animations that stay aligned instead of translating through the world. */
public final class ProjectileStaticAnimationCatalog {

	public static final String CATEGORY = "projectile-static";

	public static final class Definition {
		private final String key;
		private final String sheetPath;
		private final int columns;
		private final int rows;
		private final int firstFrame;
		private final int frameCount;
		private final int thickness;

		private Definition(String key, String sheetPath, int columns, int rows,
				int firstFrame, int frameCount, int thickness) {
			this.key = key;
			this.sheetPath = sheetPath;
			this.columns = columns;
			this.rows = rows;
			this.firstFrame = firstFrame;
			this.frameCount = frameCount;
			this.thickness = thickness;
		}

		public String getKey() { return key; }
		public String getSheetPath() { return sheetPath; }
		public int getColumns() { return columns; }
		public int getRows() { return rows; }
		public int getFirstFrame() { return firstFrame; }
		public int getFrameCount() { return frameCount; }
		public int getThickness() { return thickness; }
	}

	private static final Map<Integer, Definition> DEFINITIONS;

	static {
		LinkedHashMap<Integer, Definition> definitions = new LinkedHashMap<Integer, Definition>();
		define(definitions, PROJECTILE_TYPES.WIND_STATIC_2, "wind-2", "wind-2/Wind Breath.png",
			12, 1, 0, 12, 32);
		define(definitions, PROJECTILE_TYPES.WATER_STATIC_2, "water-2", "water-2/Water Beam.png",
			5, 5, 0, 25, 48);
		DEFINITIONS = Collections.unmodifiableMap(definitions);
	}

	private ProjectileStaticAnimationCatalog() {
	}

	private static void define(Map<Integer, Definition> definitions, PROJECTILE_TYPES projectile,
			String key, String sheetPath, int columns, int rows, int firstFrame,
			int frameCount, int thickness) {
		definitions.put(projectile.id(), new Definition(key, sheetPath, columns, rows,
			firstFrame, frameCount, thickness));
	}

	public static Definition getDefinition(int projectileId) {
		return DEFINITIONS.get(projectileId);
	}

	public static Map<Integer, Definition> getDefinitions() {
		return DEFINITIONS;
	}
}
