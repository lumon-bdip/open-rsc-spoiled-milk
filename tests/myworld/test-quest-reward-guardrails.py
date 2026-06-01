#!/usr/bin/env python3

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
QUEST_REWARD_REGISTRAR = ROOT / "server/plugins/com/openrsc/server/plugins/shared/QuestRewardRegistrar.java"
AUTHENTIC_QUESTS = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests"
PLAYER_LOGIN = ROOT / "server/plugins/com/openrsc/server/plugins/shared/PlayerLogin.java"


RETIRED_REWARD_SKILLS = ("ATTACK", "STRENGTH", "DEFENSE", "FLETCHING")


def fail(message: str) -> None:
    raise AssertionError(message)


def find_forbidden_reward_skills(path: Path, pattern: str) -> list[str]:
    text = path.read_text(encoding="utf-8")
    failures: list[str] = []
    for skill in RETIRED_REWARD_SKILLS:
        if re.search(pattern.format(skill=skill), text):
            failures.append(f"{path.relative_to(ROOT)} still rewards Skill.{skill}")
    return failures


def main() -> None:
    failures: list[str] = []
    failures.extend(find_forbidden_reward_skills(
        QUEST_REWARD_REGISTRAR,
        r"new\s+XPReward\s*\(\s*Skill\.{skill}\b",
    ))

    for path in sorted(AUTHENTIC_QUESTS.rglob("*.java")):
        failures.extend(find_forbidden_reward_skills(
            path,
            r"(?:incExp|incStat)\s*\(\s*Skill\.{skill}\.id\(\)",
        ))

    shortcut_text = (ROOT / "server/plugins/com/openrsc/server/plugins/custom/quests/MyWorldQuestShortcuts.java").read_text(encoding="utf-8")
    for source, target in {
        "ATTACK": "MELEE",
        "STRENGTH": "MELEE",
        "DEFENSE": "MELEE",
        "FLETCHING": "CRAFTING",
    }.items():
        expected = f"Skill.{source}, Skill.{target}"
        if expected not in shortcut_text:
            failures.append(f"MyWorldQuestShortcuts.java is missing explicit {source} -> {target} reward remap")

    login_text = PLAYER_LOGIN.read_text(encoding="utf-8")
    for snippet in (
        'private static final String ANACTUALDUCK_REWARD_BACKFILL = "myworld_anactualduck_quest_reward_backfill_20260531";',
        'if (!"anactualduck".equalsIgnoreCase(player.getUsername())',
        "ifCompletedEnsure(player, Quests.DEMON_SLAYER, ItemId.SILVERLIGHT.id(), 1);",
        "ifCompletedGive(player, Quests.PIRATES_TREASURE, ItemId.GOLD_RING.id(), 1);",
        "ifCompletedGive(player, Quests.PIRATES_TREASURE, ItemId.EMERALD.id(), 1);",
        "ifCompletedEnsure(player, Quests.DRAGON_SLAYER, ItemId.ANTI_DRAGON_BREATH_SHIELD.id(), 1);",
    ):
        if snippet not in login_text:
            failures.append(f"PlayerLogin.java is missing one-time anactualduck reward backfill snippet: {snippet}")

    if failures:
        fail("\n".join(failures))

    print("PASS: quest reward guardrails validated")


if __name__ == "__main__":
    main()
