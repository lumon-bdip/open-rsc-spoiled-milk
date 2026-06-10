package com.openrsc.interfaces.misc;

import com.openrsc.client.entityhandling.defs.ItemDef;

import java.util.EnumSet;
import java.util.Locale;

enum BankItemTag {
	MINING("Mining", Group.SKILLS),
	SMITHING("Smithing", Group.SKILLS),
	CRAFTING("Crafting", Group.SKILLS),
	ENCHANTING("Enchanting", Group.SKILLS),
	PRAYER("Prayer", Group.SKILLS),
	WOODCUTTING("Woodcutting", Group.SKILLS),
	FLETCHING("Fletching", Group.SKILLS),
	FISHING("Fishing", Group.SKILLS),

	FOOD("Food", Group.ITEM_TYPES),
	POTIONS("Potions", Group.ITEM_TYPES),
	POTION_INGREDIENTS("Potion ingredients", Group.ITEM_TYPES),
	COOKING_INGREDIENTS("Cooking ingredients", Group.ITEM_TYPES),
	ARMOUR("Armour", Group.ITEM_TYPES),
	MAGIC("Magic", Group.ITEM_TYPES),
	MELEE("Melee", Group.ITEM_TYPES),
	RANGED("Ranged", Group.ITEM_TYPES),
	JEWELRY("Jewelry", Group.ITEM_TYPES),
	QUEST_ITEMS("Quest items", Group.ITEM_TYPES);

	enum Group {
		SKILLS,
		ITEM_TYPES
	}

	final String label;
	final Group group;

	BankItemTag(String label, Group group) {
		this.label = label;
		this.group = group;
	}

	static EnumSet<BankItemTag> classify(ItemDef def) {
		EnumSet<BankItemTag> tags = EnumSet.noneOf(BankItemTag.class);
		if (def == null || def.getName() == null) {
			return tags;
		}

		String name = normalize(def.getName());
		String description = normalize(def.getDescription());
		String commands = commands(def);

		boolean rune = endsWithAny(name, "-rune", " rune", " runes")
			|| containsAny(name, "rune essence");
		boolean ore = containsAny(name, " ore") || endsWithAny(name, "ore", "coal");
		boolean bar = endsWithAny(name, " bar");
		boolean raw = startsWithAny(name, "raw ", "raw-");
		boolean gem = containsAny(name, "sapphire", "emerald", "ruby", "diamond", "dragonstone",
			"opal", "jade", "topaz");
		boolean uncutGem = gem && containsAny(name, "uncut");
		boolean jewelry = containsAny(name, "amulet", "necklace", "ring", "bracelet", "symbol",
			"holy mould", "tiara");
		boolean logs = containsAny(name, "logs", " log");
		boolean bow = containsAny(name, "bow", "crossbow");
		boolean rangedAmmo = containsAny(name, "arrow", "bolt", "dart", "throwing knife",
			"javelin", "cannonball", "cannon ball");
		boolean bonesOrAshes = containsAny(name, "bone", "ashes", "demon ash");
		boolean staff = containsAny(name, "staff", "stave", "wand");
		boolean potion = containsAny(name, "potion", "brew", "antidote", "serum")
			|| (containsAny(commands, "drink") && containsAny(description, "dose", "potion"));
		boolean food = containsAny(commands, "eat") || isFoodDrink(name, commands);
		boolean armour = def.isWieldable() && isArmour(name);
		boolean melee = def.isWieldable() && isMeleeWeapon(name);

		if (ore || uncutGem || containsAny(name, "geode", "pickaxe", "mining helmet", "clay")
			|| isRawMiningMaterial(name)) {
			tags.add(MINING);
		}
		if (ore || bar || containsAny(name, "hammer") && !containsAny(name, "warhammer")) {
			tags.add(SMITHING);
		}
		if (uncutGem || gem || jewelry || containsAny(name, "gold ore", "gold bar", "mould",
			"ball of wool", "wool", "thread", "needle", "hide", "leather", "clay", "chisel",
			"glass", "spinning", "pottery")) {
			tags.add(CRAFTING);
		}
		if (rune || containsAny(name, "stone") || jewelry || staff
			|| containsAny(name, "unenchant", "unenchanted")) {
			tags.add(ENCHANTING);
		}
		if (bonesOrAshes || containsAny(name, "prayer", "holy", "unholy", "blessed symbol",
			"symbol of saradomin", "symbol of zamorak", "symbol of guthix")) {
			tags.add(PRAYER);
		}
		if (logs || isWoodcuttingAxe(name)) {
			tags.add(WOODCUTTING);
		}
		if (logs || bow || rangedAmmo || containsAny(name, "bow string", "bowstring", "feather",
			"arrow shaft", "headless arrow", "knife")) {
			tags.add(FLETCHING);
		}
		if ((raw && isFish(name)) || containsAny(name, "fishing rod", "fly fishing rod", "harpoon",
			"lobster pot", "fishing bait", "bait", "fishing net")) {
			tags.add(FISHING);
		}

		if (food) {
			tags.add(FOOD);
		}
		if (potion) {
			tags.add(POTIONS);
		}
		if (isPotionIngredient(name, description)) {
			tags.add(POTION_INGREDIENTS);
		}
		if (raw || isCookingIngredient(name)) {
			tags.add(COOKING_INGREDIENTS);
		}
		if (armour) {
			tags.add(ARMOUR);
		}
		if (rune || staff || containsAny(name, "enchanted", "magic ") || bonesOrAshes) {
			tags.add(MAGIC);
		}
		if (melee) {
			tags.add(MELEE);
		}
		if (bow || rangedAmmo || containsAny(name, "chinchompa")) {
			tags.add(RANGED);
		}
		if (jewelry) {
			tags.add(JEWELRY);
		}
		if (def.untradeable && !containsAny(description, "swap this note")) {
			tags.add(QUEST_ITEMS);
		}

		return tags;
	}

	private static boolean isFoodDrink(String name, String commands) {
		return containsAny(commands, "drink") && containsAny(name, "wine", "beer", "ale", "cider",
			"milk", "tea", "stew", "soup");
	}

	private static boolean isRawMiningMaterial(String name) {
		return equalsAny(name, "stone", "silver", "gold", "coal", "limestone", "granite");
	}

	private static boolean isWoodcuttingAxe(String name) {
		return containsAny(name, " axe", "hatchet") && !containsAny(name, "battle axe", "battleaxe",
			"pickaxe", "throwing axe");
	}

	private static boolean isFish(String name) {
		return containsAny(name, "shrimp", "sardine", "herring", "anchov", "trout", "salmon",
			"tuna", "lobster", "swordfish", "shark", "mackerel", "cod", "bass", "pike",
			"karambwan", "eel", "fish");
	}

	private static boolean isPotionIngredient(String name, String description) {
		return containsAny(name, "herb", "vial", "eye of newt", "limpwurt root", "red spiders",
			"unicorn horn", "snape grass", "white berries", "blue dragon scale", "wine of zamorak",
			"toadflax", "avantoe", "kwuarm", "cadantine", "lantadyme", "dwarf weed", "torstol",
			"marrentill", "tarromin", "harralander", "ranarr", "irit", "guam")
			|| containsAny(description, "potion ingredient", "used in potion", "herblaw");
	}

	private static boolean isCookingIngredient(String name) {
		return containsAny(name, "flour", "grain", "wheat", "egg", "milk", "butter", "cheese",
			"tomato", "onion", "potato", "cabbage", "chocolate", "cream", "dough", "pastry",
			"pie dish", "pie plate", "cake tin", "baking pan", "bowl", "cooking apple",
			"banana", "orange", "lemon", "lime", "pineapple", "seaweed");
	}

	private static boolean isArmour(String name) {
		return containsAny(name, "armour", "armor", "helmet", "helm", "plate body", "platebody",
			"plate mail", "chain mail", "chainbody", "plate legs", "platelegs", "skirt", "shield",
			"boots", "gloves", "gauntlet", "greaves", "chaps", "vambrace", "cape", "robe",
			"coif", "hood", "mask", "defender");
	}

	private static boolean isMeleeWeapon(String name) {
		return containsAny(name, "sword", "dagger", "scimitar", "mace", "battle axe", "battleaxe",
			"warhammer", "spear", "halberd", "claws", "whip", "maul", "longsword", "shortsword",
			"2-handed", "two-handed");
	}

	private static String commands(ItemDef def) {
		if (def.getCommand() == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		for (String command : def.getCommand()) {
			if (command != null) {
				result.append(' ').append(command);
			}
		}
		return normalize(result.toString());
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
	}

	private static boolean containsAny(String value, String... fragments) {
		for (String fragment : fragments) {
			if (value.contains(fragment)) {
				return true;
			}
		}
		return false;
	}

	private static boolean startsWithAny(String value, String... prefixes) {
		for (String prefix : prefixes) {
			if (value.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean endsWithAny(String value, String... suffixes) {
		for (String suffix : suffixes) {
			if (value.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	private static boolean equalsAny(String value, String... choices) {
		for (String choice : choices) {
			if (value.equals(choice)) {
				return true;
			}
		}
		return false;
	}
}
