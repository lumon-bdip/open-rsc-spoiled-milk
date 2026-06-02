#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HELEMOS_SHOP = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "taverly" / "HelemosShop.java"
EQUIPMENT = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "model" / "container" / "Equipment.java"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(text: str, snippet: str, description: str) -> None:
	if snippet not in text:
		fail(f"Missing {description}: {snippet!r}")


def forbid(text: str, snippet: str, description: str) -> None:
	if snippet in text:
		fail(f"Unexpected {description}: {snippet!r}")


def main() -> None:
	shop = HELEMOS_SHOP.read_text(encoding="utf-8")
	require(shop, "new Item(ItemId.DRAGON_AXE.id(), 1)", "dragon hatchet stock")
	require(shop, "new Item(ItemId.DRAGON_BATTLE_AXE.id(), 1)", "dragon battleaxe stock")

	equipment = EQUIPMENT.read_text(encoding="utf-8")
	require(equipment, "item.getCatalogId() == ItemId.DRAGON_BATTLE_AXE.id() && player.getQuestStage(Quests.HEROS_QUEST)", "dragon battleaxe Hero's Quest equip gate")
	forbid(equipment, "item.getCatalogId() == ItemId.DRAGON_AXE.id() && player.getQuestStage(Quests.HEROS_QUEST)", "dragon hatchet Hero's Quest equip gate")

	print("PASS: Hero's Guild dragon shop stock validated")


if __name__ == "__main__":
	main()
