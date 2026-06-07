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

    require(summoning, "public static void applySummonLifesteal(final Mob hitter, final Mob target, final int damageDealt)", "summon lifesteal helper")
    require(summoning, 'KIND_GIANT_BAT.equals(summon.getAttribute(SUMMON_KIND_KEY, ""))', "bat-only lifesteal guard")
    require(summoning, "final int healed = Math.min(damageDealt, maxHits - currentHits);", "100 percent damage-to-heal conversion")
    require(summoning, "owner.getUpdateFlags().addHitSplat(new HitSplat(owner, HitSplat.TYPE_HEAL, healed));", "owner heal hitsplat")
    require(summoning, "ActionSender.sendStat(owner, Skill.HITS.id());", "owner hits stat update")
    require(summoning, "target.getUpdateFlags().setProjectile(new Projectile(target, summon, Projectile.SUMMON_BAT_VAMPIRISM));", "reverse vampirism projectile")
    require(summoning, "TRAIT_VAMPIRISM", "bat vampirism trait")
    require(summoning, '"Duskwind Bat", 26, 110, NpcId.GIANT_BAT.id(), KIND_GIANT_BAT, 7, 9, 3, 18, 30, TRAIT_VAMPIRISM', "lowered bat max hit")
    require(summoning, "public static int getSummonDamageHitSplatType(final Mob hitter)", "summon damage hitsplat helper")
    require(summoning, "return isSummon(hitter) ? HitSplat.TYPE_ARMOR_PROC : HitSplat.TYPE_STANDARD;", "yellow summon damage hitsplat")

    require(projectile, "Summoning.applySummonLifesteal(caster, opponent, damageDealt);", "projectile summon lifesteal")
    require(projectile, "Summoning.getSummonDamageHitSplatType(caster)", "projectile yellow summon damage")
    require(pvm_melee, "Summoning.applySummonLifesteal(hitter, target, damageDealt);", "pvm melee summon lifesteal")
    require(pvm_melee, "Summoning.getSummonDamageHitSplatType(hitter)", "pvm melee yellow summon damage")
    require(combat, "Summoning.applySummonLifesteal(hitter, target, damageDealt);", "shared combat summon lifesteal")
    require(combat, "Summoning.getSummonDamageHitSplatType(hitter)", "shared combat yellow summon damage")

    print("PASS: bat summon lifesteal is tied to landed damage")


if __name__ == "__main__":
    main()
