#!/usr/bin/env python3
"""Ensure incidental player damage effects never select summons."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[2]
SPELL_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/SpellHandler.java"
PROJECTILE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ProjectileEvent.java"
COMBAT_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/CombatEvent.java"
PVM_MELEE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/PvmMeleeEvent.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> int:
    spell_handler = SPELL_HANDLER.read_text(encoding="utf-8")
    projectile_event = PROJECTILE_EVENT.read_text(encoding="utf-8")
    combat_event = COMBAT_EVENT.read_text(encoding="utf-8")
    pvm_melee_event = PVM_MELEE_EVENT.read_text(encoding="utf-8")

    if "isValidIbanBlastAreaTarget(primaryTarget, npc)" not in spell_handler:
        fail("Iban Blast area selection should use its summon-aware target guard")
    if spell_handler.count("Summoning.isSummon(possibleTarget)") < 2:
        fail("Iban Blast and god-spell area effects must both exclude summons")
    if "caster.getLocation().inWilderness()" in spell_handler:
        fail("God-spell area effects must not include incidental Wilderness player targets")
    if projectile_event.count("!Summoning.isSummon(npc)") < 2:
        fail("Projectile splash and Splinter effects must exclude summons")
    for source_name, source in (
        ("projectile", projectile_event),
        ("combat", combat_event),
        ("PvM melee", pvm_melee_event),
    ):
        if "if (!primaryTarget.isNpc()) {\n\t\t\treturn null;" not in source:
            fail(f"{source_name} splash effects must not apply a second hit to player targets")
    if "!Summoning.isSummon(npc)" not in combat_event:
        fail("Combat splash effects must exclude summons")
    if "!Summoning.isSummon(npc)" not in pvm_melee_event:
        fail("PvM melee splash effects must exclude summons")

    print("PASS: player area and splash effects exclude summons and players")
    return 0


if __name__ == "__main__":
    sys.exit(main())
