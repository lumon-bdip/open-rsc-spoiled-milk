#!/usr/bin/env python3
"""Validate Spoiled Milk Mage Arena equipment and stat policy."""

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAGE_ARENA = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/minigames/mage_arena/MageArena.java"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def main() -> int:
	text = MAGE_ARENA.read_text(encoding="utf-8")

	for snippet in (
		"private boolean isAllowedMageArenaGear(Player player, Item item)",
		"private boolean isAllowedMageArenaStaff(final int itemId)",
		"private boolean isAllowedMageArenaRobePiece(final int itemId)",
		"EnchantingItemEffects.isBaseStaff(itemId)",
		"EnchantingItemEffects.isEnchantedStaff(itemId)",
		"EnchantingItemEffects.isBaseWoolRobePiece(itemId)",
		"EnchantingItemEffects.isEnchantedWoolRobePiece(itemId)",
		"Equipment.EquipmentSlot.SLOT_NECK.getIndex()",
		"Equipment.EquipmentSlot.SLOT_RING.getIndex()",
		"Equipment.EquipmentSlot.SLOT_CAPE.getIndex()",
	):
		require(snippet in text, f"Mage Arena allowlist missing: {snippet}")

	for staff in (
		"STAFF_OF_SARADOMIN",
		"STAFF_OF_ZAMORAK",
		"STAFF_OF_GUTHIX",
		"STAFF_OF_ELEMENTS",
		"STAFF_OF_POWER",
		"STAFF_OF_ENLIGHTENMENT",
	):
		require(f"ItemId.{staff}.id()" in text, f"Mage Arena should allow tier-11 staff: {staff}")

	for snippet in (
		"Skill.MELEE.id()",
		"Skill.RANGED.id()",
		"Skill.STRENGTH.id()",
		"setCurrentLevelIfPresent",
	):
		require(snippet in text, f"Mage Arena stat clamp missing: {snippet}")

	for retired_snippet in (
		"Skill.ATTACK.id()",
		"ItemId.MAGIC_STAFF.id()",
		"getMagicBonus() > 0",
		"getPrayerBonus() > 0",
		"getMeleeBonus()",
		"getArmourBonus() <= 2",
		"ItemId.ICE_GLOVES.id()",
	):
		require(retired_snippet not in text, f"Mage Arena still uses retired policy snippet: {retired_snippet}")

	print("PASS: Mage Arena equipment policy validated")
	return 0


if __name__ == "__main__":
	sys.exit(main())
