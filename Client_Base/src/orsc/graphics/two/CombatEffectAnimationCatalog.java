package orsc.graphics.two;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Spritesheet-backed on-entity spell effects keyed by stable combat-effect ID. */
public final class CombatEffectAnimationCatalog {

	public static final String ON_ENTITY = "on-entity";
	public static final String PROJECTILE_MOVING = "projectile-moving";

	public static final class Definition {
		private final String key;
		private final String category;
		private final String sheetPath;
		private final int columns;
		private final int rows;
		private final int firstFrame;
		private final int frameCount;
		private final int maxTargetSize;
		private final boolean horizontallyCentered;

		private Definition(String key, String category, String sheetPath, int columns, int rows,
				int firstFrame, int frameCount, int maxTargetSize, boolean horizontallyCentered) {
			this.key = key;
			this.category = category;
			this.sheetPath = sheetPath;
			this.columns = columns;
			this.rows = rows;
			this.firstFrame = firstFrame;
			this.frameCount = frameCount;
			this.maxTargetSize = maxTargetSize;
			this.horizontallyCentered = horizontallyCentered;
		}

		public String getKey() { return key; }
		public String getCategory() { return category; }
		public String getSheetPath() { return sheetPath; }
		public int getColumns() { return columns; }
		public int getRows() { return rows; }
		public int getFirstFrame() { return firstFrame; }
		public int getFrameCount() { return frameCount; }
		public int getMaxTargetSize() { return maxTargetSize; }
		public boolean isHorizontallyCentered() { return horizontallyCentered; }
	}

	private static final Map<Integer, Definition> DEFINITIONS;

	static {
		LinkedHashMap<Integer, Definition> definitions = new LinkedHashMap<Integer, Definition>();
		define(definitions, 6, "fire-2", ON_ENTITY, "fire-2/Fire Claw.png", 9, 1, 0, 9, 64);
		define(definitions, 8, "earth-2", ON_ENTITY, "earth-2/Earth Hammer (48x48).png", 5, 5, 0, 21, 64);
		defineHorizontallyCentered(definitions, 16, "lesser-heal", ON_ENTITY,
			"lesser-heal/Buff n Debuff P1 03.png", 12, 1, 0, 12, 64);
		define(definitions, 17, "greater-heal", ON_ENTITY,
			"greater-heal/Buff n Debuff P07 04.png", 19, 1, 0, 19, 64);
		define(definitions, 18, "holy-vfx-09", ON_ENTITY,
			"Holy VFX 09/Holy Effect 09(16x16).png", 12, 1, 0, 12, 32);
		define(definitions, 30, "thunder-2-hit", PROJECTILE_MOVING,
			"thunder-2/Hit/Thunder hit wo blur.png", 6, 1, 0, 6, 64);
		define(definitions, 31, "ice-2", ON_ENTITY, "ice-2/Ice VFX 3(48x48).png", 22, 1, 0, 20, 64);
		define(definitions, 32, "acid-2", ON_ENTITY, "acid-2/Acid VFX 09(72x80).png", 23, 1, 0, 23, 64);
		define(definitions, 33, "wood-2", ON_ENTITY, "wood-2/Wood VFX 04(32x48).png", 16, 1, 0, 15, 64);
		define(definitions, 65, "teleport", ON_ENTITY,
			"teleport/Buff n Debuff P1 04.png", 18, 1, 0, 18, 64);
		DEFINITIONS = Collections.unmodifiableMap(definitions);
	}

	private CombatEffectAnimationCatalog() {
	}

	private static void define(Map<Integer, Definition> definitions, int effectType, String key,
			String category, String sheetPath, int columns, int rows, int firstFrame,
			int frameCount, int maxTargetSize) {
		define(definitions, effectType, key, category, sheetPath, columns, rows, firstFrame,
			frameCount, maxTargetSize, false);
	}

	private static void defineHorizontallyCentered(Map<Integer, Definition> definitions, int effectType,
			String key, String category, String sheetPath, int columns, int rows, int firstFrame,
			int frameCount, int maxTargetSize) {
		define(definitions, effectType, key, category, sheetPath, columns, rows, firstFrame,
			frameCount, maxTargetSize, true);
	}

	private static void define(Map<Integer, Definition> definitions, int effectType, String key,
			String category, String sheetPath, int columns, int rows, int firstFrame,
			int frameCount, int maxTargetSize, boolean horizontallyCentered) {
		if (definitions.containsKey(effectType)) {
			throw new IllegalStateException("Duplicate combat effect animation: " + effectType);
		}
		if (columns <= 0 || rows <= 0 || firstFrame < 0 || frameCount <= 0
			|| firstFrame + frameCount > columns * rows || maxTargetSize <= 0) {
			throw new IllegalArgumentException("Invalid combat effect animation: " + key);
		}
		definitions.put(effectType, new Definition(key, category, sheetPath, columns, rows,
			firstFrame, frameCount, maxTargetSize, horizontallyCentered));
	}

	public static Definition getDefinition(int effectType) {
		return DEFINITIONS.get(effectType);
	}

	public static Map<Integer, Definition> getDefinitions() {
		return DEFINITIONS;
	}
}
