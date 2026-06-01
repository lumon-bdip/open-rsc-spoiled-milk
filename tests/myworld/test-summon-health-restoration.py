#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
STAT_RESTORATION = ROOT / "server/src/com/openrsc/server/event/rsc/impl/StatRestorationEvent.java"
SUMMONING = ROOT / "server/src/com/openrsc/server/content/Summoning.java"
PROJECTILE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ProjectileEvent.java"
PVM_MELEE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/PvmMeleeEvent.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(path: Path, needle: str, context: str) -> None:
    text = path.read_text(encoding="utf-8")
    if needle not in text:
        fail(f"{context}: missing {needle!r} in {path}")


def main() -> None:
    require(
        SUMMONING,
        "public static boolean isSummon(final Mob mob)",
        "Summoning should expose summon checks for shared runtime systems",
    )
    require(
        SUMMONING,
        'private static final String SUMMON_CURRENT_HITS_KEY = "myworld_summon_current_hits";',
        "Summons should track authoritative hitpoints outside regular NPC restoration",
    )
    require(
        SUMMONING,
        'private static final String SUMMON_MAX_HITS_KEY = "myworld_summon_max_hits";',
        "Summons should track authoritative maximum hitpoints outside regular NPC restoration",
    )
    require(
        SUMMONING,
        "public static int getSummonCurrentHits(final Npc summon)",
        "Summoning should expose authoritative current hitpoints for update packets",
    )
    require(
        SUMMONING,
        "summon.setAttribute(SUMMON_CURRENT_HITS_KEY, summonHits);",
        "Summon profiles should initialize authoritative current hitpoints",
    )
    require(
        SUMMONING,
        "summon.setAttribute(SUMMON_CURRENT_HITS_KEY, nextHits);",
        "Summon damage absorption should update authoritative hitpoints",
    )
    require(
        SUMMONING,
        "syncSummonHitpoints(summon);",
        "Summon runtime should clamp accidental NPC hitpoint restoration",
    )
    require(
        STAT_RESTORATION,
        "import com.openrsc.server.content.Summoning;",
        "Stat restoration should be aware of summons",
    )
    require(
        STAT_RESTORATION,
        "if (Summoning.isSummon(getOwner())) {\n\t\t\tstop();\n\t\t\treturn;\n\t\t}",
        "Summons should not inherit regular NPC hit restoration",
    )
    require(
        PROJECTILE_EVENT,
        "damage = Summoning.applySummonDamageAbsorption(opponentPlayer, caster, damage);",
        "NPC projectile damage should still be absorbed by combat summons",
    )
    require(
        PVM_MELEE_EVENT,
        "damage = Summoning.applySummonDamageAbsorption(targetPlayer, hitter, damage);",
        "NPC melee damage should still be absorbed by combat summons",
    )
    require(
        ROOT / "server/src/com/openrsc/server/GameStateUpdater.java",
        "updates.add((byte) Summoning.getSummonCurrentHits(summonedNpcHealth));",
        "Summon health packets should use authoritative summon hitpoints",
    )
    print("PASS: summon health restoration guardrails are present")


if __name__ == "__main__":
    main()
