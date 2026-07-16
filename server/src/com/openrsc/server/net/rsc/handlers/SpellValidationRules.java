package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.constants.Skill;
import com.openrsc.server.constants.Spells;
import com.openrsc.server.net.rsc.enums.OpcodeIn;

/** Side-effect-free spell eligibility decisions; messages and mutations stay in {@link SpellHandler}. */
final class SpellValidationRules {
	enum AutoCastRejection {
		NONE,
		MISSING_SELECTION,
		NON_COMBAT,
		MEMBERS_ONLY,
		LEVEL_TOO_LOW
	}

	enum CastDefinitionRejection {
		NONE,
		MISSING_SELECTION,
		MISSING_DEFINITION,
		MEMBERS_ONLY,
		LEVEL_TOO_LOW
	}

	private SpellValidationRules() {
	}

	static int magicSkillId(boolean dividedGoodEvil, boolean evilSpell) {
		if (!dividedGoodEvil) {
			return Skill.MAGIC.id();
		}
		return evilSpell ? Skill.EVILMAGIC.id() : Skill.GOODMAGIC.id();
	}

	static AutoCastRejection validateAutoCast(
		Spells spell,
		boolean hasDefinition,
		int spellType,
		boolean membersSpell,
		boolean memberWorld,
		int currentMagicLevel,
		int requiredMagicLevel) {
		if (spell == null) {
			return AutoCastRejection.MISSING_SELECTION;
		}
		if (!hasDefinition || spellType != 2) {
			return AutoCastRejection.NON_COMBAT;
		}
		if (membersSpell && !memberWorld) {
			return AutoCastRejection.MEMBERS_ONLY;
		}
		if (currentMagicLevel < requiredMagicLevel) {
			return AutoCastRejection.LEVEL_TOO_LOW;
		}
		return AutoCastRejection.NONE;
	}

	static CastDefinitionRejection validateDefinition(
		Spells spell,
		boolean hasDefinition,
		boolean membersSpell,
		boolean memberWorld,
		int currentMagicLevel,
		int requiredMagicLevel) {
		if (spell == null) {
			return CastDefinitionRejection.MISSING_SELECTION;
		}
		if (!hasDefinition) {
			return CastDefinitionRejection.MISSING_DEFINITION;
		}
		if (membersSpell && !memberWorld) {
			return CastDefinitionRejection.MEMBERS_ONLY;
		}
		if (currentMagicLevel < requiredMagicLevel) {
			return CastDefinitionRejection.LEVEL_TOO_LOW;
		}
		return CastDefinitionRejection.NONE;
	}

	static boolean requiresTeleportValidation(OpcodeIn opcode, int spellType, Spells spell) {
		return opcode == OpcodeIn.CAST_ON_SELF
			&& spellType == 0
			&& SpellClassification.isTeleportSpell(spell);
	}

	static boolean isDuelActionBlocked(OpcodeIn opcode, boolean duelActive) {
		return duelActive && opcode != OpcodeIn.PLAYER_CAST_PVP;
	}
}
