package com.openrsc.server.content;

import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.update.CombatEffect;
import com.openrsc.server.util.rsc.DataConversions;

public final class TrueDefense {
	private static final double PROC_CHANCE_PER_PIECE = 0.05D;
	private static final int VISUAL_DURATION_TICKS = 40;
	private static final String VISUAL_UNTIL_TICK_KEY = "exalted_rune_true_defense_visual_until_tick";

	private TrueDefense() {
	}

	public static int apply(final Player defender, final int incomingDamage) {
		if (defender == null || incomingDamage <= 0
			|| defender.getSkills().getLevel(Skill.HITS.id()) <= 0) {
			return incomingDamage;
		}
		final int pieces = defender.getExaltedRuneTrueDefensePieces();
		if (pieces <= 0) {
			return incomingDamage;
		}
		final double chance = Math.min(1.0D, pieces * PROC_CHANCE_PER_PIECE);
		if (DataConversions.getRandom().nextDouble() >= chance) {
			return incomingDamage;
		}
		playVisualIfReady(defender);
		return 0;
	}

	private static void playVisualIfReady(final Player defender) {
		final long currentTick = defender.getWorld().getServer().getCurrentTick();
		final long visualUntilTick = defender.getAttribute(VISUAL_UNTIL_TICK_KEY, 0L);
		if (currentTick < visualUntilTick) {
			return;
		}
		defender.getUpdateFlags().setCombatEffect(new CombatEffect(defender, CombatEffect.TRUE_DEFENSE));
		defender.setAttribute(VISUAL_UNTIL_TICK_KEY, currentTick + VISUAL_DURATION_TICKS);
	}
}
