package com.openrsc.interfaces.misc;

/**
 * Builds the displayed-row to protocol skill-ID mapping shared by the XP
 * selectors. Skill ID 11 is the retired Firemaking slot and must remain in
 * the protocol arrays without appearing as a selectable skill.
 */
public final class ExperienceSkillSelector {
	public static final int RETIRED_FIREMAKING_SKILL_ID = 11;

	private ExperienceSkillSelector() {
	}

	public static int[] buildSelectableSkillIds(int skillCount) {
		int safeSkillCount = Math.max(0, skillCount);
		int selectableCount = safeSkillCount > RETIRED_FIREMAKING_SKILL_ID
			? safeSkillCount - 1
			: safeSkillCount;
		int[] skillIds = new int[selectableCount];
		int row = 0;
		for (int skillId = 0; skillId < safeSkillCount; skillId++) {
			if (skillId != RETIRED_FIREMAKING_SKILL_ID) {
				skillIds[row++] = skillId;
			}
		}
		return skillIds;
	}

	public static int resolveSkillId(int[] selectableSkillIds, int displayedRow) {
		if (displayedRow < 0 || displayedRow >= selectableSkillIds.length) {
			return -1;
		}
		return selectableSkillIds[displayedRow];
	}
}
