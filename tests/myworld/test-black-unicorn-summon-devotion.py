#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SUMMONING = ROOT / "server/src/com/openrsc/server/content/Summoning.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        raise SystemExit(f"FAIL: {label} missing expected snippet: {snippet}")


def main() -> None:
    summoning = SUMMONING.read_text(encoding="utf-8")
    player = PLAYER.read_text(encoding="utf-8")

    require(
        summoning,
        '"Mourning Unicorn", 45, 230, NpcId.BLACK_UNICORN.id(), KIND_BLACK_UNICORN, 0',
        "Black unicorn support profile should not grant a Prayer bonus",
    )
    require(
        player,
        "getCarriedItems().getEquipment().getPrayer() + Summoning.getPrayerBonus(this)",
        "Player prayer calculations should include summon Prayer bonus",
    )
    require(
        summoning,
        "final int devotionBonusXp = recordAutoBuryDevotionBonus(owner, amount);",
        "Black unicorn auto-offerings should calculate devotion bonus XP",
    )
    require(
        summoning,
        "bonusXp += Devotion.recordOfferingAndGetPrayerXpBonus(owner);",
        "Black unicorn auto-offerings should advance devotion once per offering",
    )
    require(
        summoning,
        "final int xp = (getPrayerDropExperience(itemId) * amount * 2) + devotionBonusXp;",
        "Black unicorn auto-offerings should add devotion bonus XP without doubling it",
    )

    print("PASS: black unicorn summon records devotion offerings without granting Prayer")


if __name__ == "__main__":
    main()
