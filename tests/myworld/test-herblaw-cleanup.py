#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
DRINKABLES = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "itemactions" / "Drinkables.java"
HERBLAW = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "skills" / "herblaw" / "Herblaw.java"
APOTHECARY = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "varrock" / "Apothecary.java"
RUNECRAFT_POTION = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "custom" / "myworld" / "itemactions" / "RunecraftPotion.java"
ITEM_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsCustom.json"

ACTIVE_INSIGHT_IDS = {1411, 1412, 1413, 1414, 1415, 1416}


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def require_absent(text: str, snippet: str, message: str) -> None:
    if snippet in text:
        fail(message)


def main() -> None:
    drinkables_text = DRINKABLES.read_text(encoding="utf-8")
    herblaw_text = HERBLAW.read_text(encoding="utf-8")
    apothecary_text = APOTHECARY.read_text(encoding="utf-8")
    runecraft_potion_text = RUNECRAFT_POTION.read_text(encoding="utf-8")
    defs = json.loads(ITEM_DEFS.read_text(encoding="utf-8"))

    for snippet in (
        "case FULL_RUNECRAFT_POTION: return skillPotion(\"insight\", 11, 11",
        "case FULL_SUPER_RUNECRAFT_POTION: return skillPotion(\"insight\", 14, 14",
    ):
        require(drinkables_text, snippet, "Drinkables should reuse enchanting ids as Insight potion tiers")

    for snippet in (
        "ItemId.FULL_RUNECRAFT_POTION.id()",
        "ItemId.FULL_SUPER_RUNECRAFT_POTION.id()",
    ):
        require_absent(
            apothecary_text,
            snippet,
            "Apothecary should not sell or advertise old enchanting potion behavior",
        )
        require_absent(
            runecraft_potion_text,
            snippet,
            "RunecraftPotion should remain an inert compatibility shell",
        )
        require_absent(
            herblaw_text,
            snippet,
            "Herblaw should craft Insight tiers through recipes rather than old custom potion branches",
        )

    item_defs = {item["id"]: item for item in defs["items"] if item["id"] in ACTIVE_INSIGHT_IDS}
    if set(item_defs) != ACTIVE_INSIGHT_IDS:
        fail("Expected reused Insight potion item definitions to remain present")

    for item_id, item in item_defs.items():
        if item.get("command") != "Drink":
            fail(f"Insight potion item {item_id} should be drinkable")
        if not item.get("name", "").startswith("Potion of Insight v"):
            fail(f"Insight potion item {item_id} should use the new naming convention")

    print("PASS: herblaw cleanup validated")


if __name__ == "__main__":
    main()
