package com.openrsc.server.model.entity.npc;

import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.model.entity.update.Projectile;

public enum NpcAttackStyleProfile {
	MELEE,
	PURE_RANGED,
	PURE_MAGIC,
	MELEE_RANGED,
	MELEE_MAGIC,
	MELEE_RARE_MAGIC;

	private static final int DEFAULT_PROJECTILE_RANGE = 5;

	public boolean isProjectilePrimary() {
		return this == PURE_RANGED || this == PURE_MAGIC;
	}

	public boolean usesRangedProjectiles() {
		return this == PURE_RANGED || this == MELEE_RANGED;
	}

	public boolean usesMagicProjectiles() {
		return this == PURE_MAGIC || this == MELEE_MAGIC || this == MELEE_RARE_MAGIC;
	}

	public int getProjectileRange() {
		return DEFAULT_PROJECTILE_RANGE;
	}

	public boolean prefersProjectileAtDistance(final int distance) {
		if (distance > getProjectileRange()) {
			return false;
		}
		if (isProjectilePrimary()) {
			return true;
		}
		return distance > 1 || rollsPreferredProjectileAttack();
	}

	private boolean rollsPreferredProjectileAttack() {
		switch (this) {
			case MELEE_RANGED:
			case MELEE_MAGIC:
				return DataConversions.getRandom().nextInt(100) < 65;
			case MELEE_RARE_MAGIC:
				return DataConversions.getRandom().nextInt(100) < 10;
			default:
				return false;
		}
	}

	public double getMagicSpellPower(final Npc npc) {
		return Math.max(1.0D, getMagicOffense(npc) / 12.0D);
	}

	public int getRangedOffense(final Npc npc) {
		if (!usesRangedProjectiles()) {
			return 0;
		}
		return Math.max(1, Math.max(npc.getDef().getAtt(), npc.getDef().getStr()));
	}

	public int getRangedProjectileVisual(final Npc npc) {
		if (npc == null || npc.getDef() == null || npc.getDef().getName() == null) {
			return Projectile.ARROW;
		}
		final String name = npc.getDef().getName().toLowerCase();
		switch (name) {
			case "thief":
			case "rogue":
			case "head thief":
			case "pirate":
			case "bandit":
				return Projectile.THROWING_KNIFE;
			case "tribesman":
				return Projectile.THROWING_DART;
			case "yanille watchman":
				return Projectile.BOLT;
			default:
				return Projectile.ARROW;
		}
	}

	public int getMagicProjectileVisual(final Npc npc) {
		if (npc == null || npc.getDef() == null || npc.getDef().getName() == null) {
			return Projectile.MAGIC;
		}
		final String name = npc.getDef().getName().toLowerCase();
		if (name.contains("wizard")) {
			return Projectile.WIZARDS_MAGIC;
		}
		if (isHolyMagicNpcName(name)) {
			return Projectile.HOLY_MAGIC;
		}
		return Projectile.MAGIC;
	}

	private static boolean isHolyMagicNpcName(final String name) {
		return name.contains("zamorak")
			|| name.contains("saradomin")
			|| name.contains("druid")
			|| name.contains("priest")
			|| isGodKnightNpcName(name)
			|| "monk".equals(name)
			|| name.startsWith("monk ");
	}

	private static boolean isGodKnightNpcName(final String name) {
		return name.contains("black knight")
			|| name.contains("white knight")
			|| name.contains("grey knight");
	}

	private static boolean prefersHolyMagicOnly(final String name) {
		return "monk".equals(name) || name.contains("priest");
	}

	public int getMagicOffense(final Npc npc) {
		if (!usesMagicProjectiles()) {
			return 0;
		}
		return Math.max(1, Math.max(npc.getDef().getAtt(), npc.getDef().getStr()));
	}

	public static NpcAttackStyleProfile forNpc(final Npc npc) {
		if (npc == null || npc.getDef() == null || npc.getDef().getName() == null) {
			return MELEE;
		}

		final String name = npc.getDef().getName().toLowerCase();
		if (name.contains("wizard")) {
			return PURE_MAGIC;
		}
		if (prefersHolyMagicOnly(name)) {
			return PURE_MAGIC;
		}
		if (isGodKnightNpcName(name)) {
			return MELEE_RARE_MAGIC;
		}
		switch (name) {
			case "darkwizard":
			case "chaos druid":
			case "druid":
			case "witch":
			case "necromancer":
			case "skeleton mage":
				return PURE_MAGIC;
			case "gnome guard":
			case "guard":
			case "jailguard":
			case "carnillean guard":
			case "goblin guard":
			case "ogre guard":
			case "bedabin nomad guard":
			case "draft mercenary guard":
			case "rowdy guard":
			case "shantay pass guard":
			case "thief":
			case "rogue":
			case "head thief":
				return PURE_RANGED;
			case "battle mage":
			case "monk of zamorak":
			case "chaos druid warrior":
			case "paladin":
			case "lesser demon":
			case "greater demon":
			case "black demon":
			case "moss giant":
			case "ice giant":
			case "fire giant":
			case "delrith":
			case "lucien":
			case "ghost":
			case "tree spirit":
			case "ice warrior":
			case "ice queen":
			case "the fire warrior of lesarkus":
			case "chronozon":
			case "nazastarool ghost":
			case "otherworldly being":
			case "salarin the twisted":
				return MELEE_MAGIC;
			case "mercenary":
			case "mercenary captain":
			case "khazard troop":
			case "gnome troop":
			case "pirate":
			case "bandit":
			case "tribesman":
			case "yanille watchman":
				return MELEE_RANGED;
			default:
				return MELEE;
		}
	}
}
