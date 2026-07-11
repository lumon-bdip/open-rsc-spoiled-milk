package orsc.graphics.three;

import com.openrsc.client.entityhandling.defs.GameObjectDef;

import java.util.Locale;

public final class Renderer3DMaterialClassifier {
	private Renderer3DMaterialClassifier() {
	}

	public static Renderer3DMaterialFamily fallbackFor(Renderer3DModelKind kind) {
		if (kind == Renderer3DModelKind.TERRAIN) {
			return Renderer3DMaterialFamily.TERRAIN;
		}
		if (kind == Renderer3DModelKind.WALL) {
			return Renderer3DMaterialFamily.WALL;
		}
		if (kind == Renderer3DModelKind.ROOF) {
			return Renderer3DMaterialFamily.ROOF;
		}
		if (kind == Renderer3DModelKind.GAME_OBJECT || kind == Renderer3DModelKind.WALL_OBJECT) {
			return Renderer3DMaterialFamily.SCENERY;
		}
		return Renderer3DMaterialFamily.UNCLASSIFIED;
	}

	public static Renderer3DMaterialFamily classifyTerrain(boolean waterLike, boolean emissive) {
		if (emissive) {
			return Renderer3DMaterialFamily.EMISSIVE;
		}
		return waterLike ? Renderer3DMaterialFamily.WATER : Renderer3DMaterialFamily.TERRAIN;
	}

	public static Renderer3DMaterialFamily classifyObject(
		Renderer3DModelKind kind,
		GameObjectDef definition,
		boolean emissive) {
		if (emissive) {
			return Renderer3DMaterialFamily.EMISSIVE;
		}
		if (kind != Renderer3DModelKind.GAME_OBJECT || definition == null) {
			return fallbackFor(kind);
		}

		String name = lower(definition.getName());
		String model = lower(definition.getObjectModel());
		String command1 = lower(definition.getCommand1());
		String command2 = lower(definition.getCommand2());
		if (isMineableOre(name, model, command1, command2)) {
			return Renderer3DMaterialFamily.ORE;
		}
		if (isFoliage(name, model)) {
			return Renderer3DMaterialFamily.FOLIAGE;
		}
		if (model.equals("portal") || model.startsWith("myworld_cosmic_sparkles")) {
			return Renderer3DMaterialFamily.EFFECT;
		}
		return Renderer3DMaterialFamily.SCENERY;
	}

	private static boolean isMineableOre(
		String name,
		String model,
		String command1,
		String command2) {
		boolean miningCommand = command1.contains("mine")
			|| command1.contains("prospect")
			|| command2.contains("mine")
			|| command2.contains("prospect");
		return miningCommand && (name.contains("ore") || name.contains("rock") || model.contains("rock"));
	}

	private static boolean isFoliage(String name, String model) {
		return containsAny(
			name,
			"tree", "plant", "bush", "fern", "cactus", "palm", "crop", "herb", "vine", "grass", "weed")
			|| containsAny(
				model,
				"tree", "plant", "bush", "fern", "cactus", "palm", "cabbage", "herb", "vine", "grass", "weed");
	}

	private static boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
