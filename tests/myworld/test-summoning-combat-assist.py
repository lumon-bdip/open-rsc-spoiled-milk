#!/usr/bin/env python3
"""Validate summon combat assist target selection."""

import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
SUMMONING = ROOT / "server/src/com/openrsc/server/content/Summoning.java"
NPC = ROOT / "server/src/com/openrsc/server/model/entity/npc/Npc.java"
PROJECTILE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ProjectileEvent.java"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    summoning = SUMMONING.read_text(encoding="utf-8")
    npc = NPC.read_text(encoding="utf-8")
    projectile_event = PROJECTILE_EVENT.read_text(encoding="utf-8")

    require(
        summoning,
        "final Mob target = resolveSummonAssistTarget(owner, summon);",
        "Summon assist should use the resolver instead of only owner.getOpponent()",
    )
    require(
        summoning,
        "private static Mob findOwnerAttacker",
        "Summon assist should be able to find mobs attacking the owner",
    )
    require(
        summoning,
        "npc.getOpponent() == owner",
        "Summon assist should detect melee NPC attackers",
    )
    require(
        summoning,
        "&& ownerHasTakenDamageFrom(owner, npc)",
        "Defensive summon assist should require the owner to have taken or blocked damage from the NPC",
    )
    require(
        summoning,
        "private static Mob getOwnerActiveAttackTarget",
        "Summon assist should still support owner-initiated ranged, thrown, magic, and melee attacks",
    )
    for snippet in (
        "owner.getRangeEvent()",
        "owner.getThrowingEvent()",
        "owner.getMagicCombatEvent()",
        "owner.getPvmMeleeEvent()",
    ):
        require(
            summoning,
            snippet,
            f"Summon assist active-target resolver missing {snippet}",
        )

    for snippet in (
        "&& ownerHasDamagedTarget(owner, activeTarget)",
        "private static boolean ownerHasDamagedTarget",
        "return ((Npc) target).hasDamageFrom(owner);",
        "private static boolean ownerHasTakenDamageFrom",
        "return owner.getTrackedDamage(attacker) > 0 || owner.getTrackedBlockedDamage(attacker) > 0;",
    ):
        require(
            summoning,
            snippet,
            f"Summon assist damage-participation resolver missing {snippet}",
        )

    if "npc.getBehavior().getChaseTarget() == owner" in summoning:
        fail("Summon assist should not attack NPCs that are only chasing or aggro to the owner")

    assist_body_start = summoning.index("private static void assistOwnerTarget")
    assist_body_end = summoning.index("private static Mob resolveSummonAssistTarget")
    assist_body = summoning[assist_body_start:assist_body_end]
    if "ownerIsActivelyAttacking(owner, target)" in assist_body:
        fail("Defensive summon assist should not require the owner to be actively attacking the attacker")

    for snippet in (
        "public boolean hasDamageFrom(final Player player)",
        "getCombatDamageInfoBy(id).getLeft() > 0",
        "getRangeDamageInfoBy(id).getLeft() > 0",
        "getMageDamageInfoBy(id).getLeft() > 0",
    ):
        require(npc, snippet, f"NPC damage participation helper missing {snippet}")

    require(
        projectile_event,
        "((Player) opponent).updateDamageAndBlockedDamageTracking(caster, damageDealt, 0);",
        "NPC projectile damage should count as owner taking damage for summon assist",
    )

    print("PASS: summon combat assist contracts validated")


if __name__ == "__main__":
    main()
