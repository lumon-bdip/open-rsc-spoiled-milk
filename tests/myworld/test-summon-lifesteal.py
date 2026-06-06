#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SUMMONING = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "content" / "Summoning.java"
PROJECTILE = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "event" / "rsc" / "impl" / "projectile" / "ProjectileEvent.java"
PVM_MELEE = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "event" / "rsc" / "impl" / "combat" / "PvmMeleeEvent.java"
COMBAT = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "event" / "rsc" / "impl" / "combat" / "CombatEvent.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        fail(f"missing {label}: {needle}")


def main() -> None:
    summoning = SUMMONING.read_text(encoding="utf-8")
    projectile = PROJECTILE.read_text(encoding="utf-8")
    pvm_melee = PVM_MELEE.read_text(encoding="utf-8")
    combat = COMBAT.read_text(encoding="utf-8")

    require(summoning, "public static void applySummonLifesteal(final Mob hitter, final int damageDealt)", "summon lifesteal helper")
    require(summoning, 'KIND_GIANT_BAT.equals(summon.getAttribute(SUMMON_KIND_KEY, ""))', "bat-only lifesteal guard")
    require(summoning, "final int healed = Math.min(damageDealt, maxHits - currentHits);", "100 percent damage-to-heal conversion")
    require(summoning, "owner.getUpdateFlags().addHitSplat(new HitSplat(owner, HitSplat.TYPE_HEAL, healed));", "owner heal hitsplat")
    require(summoning, "ActionSender.sendStat(owner, Skill.HITS.id());", "owner hits stat update")

    require(projectile, "Summoning.applySummonLifesteal(caster, damageDealt);", "projectile summon lifesteal")
    require(pvm_melee, "Summoning.applySummonLifesteal(hitter, damageDealt);", "pvm melee summon lifesteal")
    require(combat, "Summoning.applySummonLifesteal(hitter, damageDealt);", "shared combat summon lifesteal")

    print("PASS: bat summon lifesteal is tied to landed damage")


if __name__ == "__main__":
    main()
