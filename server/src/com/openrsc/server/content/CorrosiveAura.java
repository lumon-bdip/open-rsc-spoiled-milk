package com.openrsc.server.content;

import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.PrayerCatalog;
import com.openrsc.server.model.entity.player.Prayers;
import com.openrsc.server.model.entity.update.CombatEffect;

public final class CorrosiveAura {
	private static final int MIN_POISON_POWER = 10;
	private static final int MAX_POISON_POWER = 50;
	private static final double MAX_POWER_HEALTH_FRACTION = 0.30D;

	private CorrosiveAura() {
	}

	public static boolean apply(final Player defender, final Mob attacker, final int incomingDamage) {
		if (!canProc(defender, attacker, incomingDamage)) {
			return false;
		}
		final int poisonPower = getPoisonPower(defender);
		attacker.applyPoison(poisonPower, attacker.getCurrentPoisonPower() + poisonPower);
		attacker.getUpdateFlags().setCombatEffect(new CombatEffect(attacker, CombatEffect.CORROSIVE_AURA));
		return true;
	}

	private static boolean canProc(final Player defender, final Mob attacker, final int incomingDamage) {
		return defender != null
			&& attacker != null
			&& incomingDamage > 0
			&& attacker.getSkills().getLevel(Skill.HITS.id()) > 0
			&& defender.getPrayerBook() == PrayerCatalog.GodLine.GUTHIX
			&& defender.getPrayers().isPrayerActivated(Prayers.CORROSIVE_AURA);
	}

	private static int getPoisonPower(final Player defender) {
		final int currentHits = Math.max(0, defender.getSkills().getLevel(Skill.HITS.id()));
		final int maxHits = Math.max(1, defender.getSkills().getMaxStat(Skill.HITS.id()));
		final double healthFraction = currentHits / (double) maxHits;
		if (healthFraction <= MAX_POWER_HEALTH_FRACTION) {
			return MAX_POISON_POWER;
		}
		final double missingScaled = (1.0D - healthFraction) / (1.0D - MAX_POWER_HEALTH_FRACTION);
		final int bonusPower = (int) Math.round((MAX_POISON_POWER - MIN_POISON_POWER) * missingScaled);
		return Math.max(MIN_POISON_POWER, Math.min(MAX_POISON_POWER, MIN_POISON_POWER + bonusPower));
	}
}
