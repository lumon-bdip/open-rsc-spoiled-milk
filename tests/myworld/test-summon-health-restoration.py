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
    print("PASS: summon health restoration guardrails are present")


if __name__ == "__main__":
    main()
