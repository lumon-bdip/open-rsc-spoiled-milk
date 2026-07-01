#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEVOTION = ROOT / "server/src/com/openrsc/server/content/Devotion.java"
NPC_BEHAVIOR = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcBehavior.java"


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        raise SystemExit(f"FAIL: {message}")


def main() -> None:
    devotion = DEVOTION.read_text(encoding="utf-8")
    behavior = NPC_BEHAVIOR.read_text(encoding="utf-8")

    require(devotion, "MIN_DEVOTION_LEVEL = -1000", "devotion should support a -1000 floor")
    require(devotion, "MIN_OFFERINGS = MIN_DEVOTION_LEVEL * OFFERINGS_PER_DEVOTION_LEVEL", "negative devotion offering floor")
    require(devotion, "clampDevotionLevel(offerings / OFFERINGS_PER_DEVOTION_LEVEL)", "signed devotion clamp")

    require(behavior, "final boolean devotionAggro = !naturalAggro && isGodFollower();", "devotion should add aggro without replacing natural aggro")
    require(behavior, "Devotion.getDevotionLevel(player, followerGod)", "follower aggro should read devotion to that god")
    require(behavior, "if (devotionLevel >= 0)", "non-negative devotion should not add follower aggro")
    require(behavior, "Math.min(1000, Math.abs(devotionLevel))", "negative devotion should scale to 1000")
    require(behavior, "DataConversions.getRandom().nextInt(1000) < hostilityChance", "-1000 devotion should guarantee aggro")
    require(behavior, "return PrayerCatalog.GodLine.SARADOMIN;", "Saradomin follower mapping")
    require(behavior, "return PrayerCatalog.GodLine.ZAMORAK;", "Zamorak follower mapping")
    require(behavior, "return PrayerCatalog.GodLine.GUTHIX;", "Guthix follower mapping")

    print("PASS: negative devotion follower aggro is wired")


if __name__ == "__main__":
    main()
