#!/usr/bin/env python3
"""Validate ranged weapon equipment slot contracts."""

import re
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
ENTITY_HANDLER = ROOT / "server/src/com/openrsc/server/external/EntityHandler.java"
EQUIPMENT = ROOT / "server/src/com/openrsc/server/model/container/Equipment.java"
ITEM_DEFS = ROOT / "server/conf/server/defs/ItemDefs.json"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    entity_handler = ENTITY_HANDLER.read_text(encoding="utf-8")
    equipment = EQUIPMENT.read_text(encoding="utf-8")
    item_defs = ITEM_DEFS.read_text(encoding="utf-8")

    require(
        entity_handler,
        "int[] mainhandBows = new int[]",
        "EntityHandler should normalize bows to mainhand at load time",
    )
    require(
        entity_handler,
        "items.get(itemId).setWieldPosition(Equipment.EquipmentSlot.SLOT_MAINHAND.getIndex());",
        "Bow slot normalization should use the named mainhand equipment slot",
    )
    for item_name in (
        "SHORTBOW",
        "LONGBOW",
        "PINE_SHORTBOW",
        "PINE_LONGBOW",
        "BLOOD_SHORTBOW",
        "BLOOD_LONGBOW",
        "DRAGON_LONGBOW",
    ):
        require(
            entity_handler,
            f"ItemId.{item_name}.id()",
            f"Bow slot normalization missing {item_name}",
        )

    require(
        equipment,
        "private boolean bowConflictsWithOffhand(Item requestedItem, Item equippedItem)",
        "Equipment should enforce bow/offhand mutual exclusion",
    )
    require(
        equipment,
        "RangeUtils.isBow(requestedItem.getCatalogId())",
        "Equipping a bow should conflict with an existing offhand item",
    )
    require(
        equipment,
        "RangeUtils.isBow(equippedItem.getCatalogId())",
        "Equipping an offhand item should conflict with an existing bow",
    )
    if "RangeUtils.isCrossbow" in equipment:
        fail("Crossbows should not be included in bow/offhand conflict logic")

    crossbow_match = re.search(
        r'"id": 60,\s*"name": "Crossbow".*?"wearSlot": 4',
        item_defs,
        re.DOTALL,
    )
    if crossbow_match is None:
        fail("Standard crossbow should remain a mainhand item")

    throwing_match = re.search(
        r'"name": "Bronze throwing knife".*?"wearSlot": 4',
        item_defs,
        re.DOTALL,
    )
    if throwing_match is None:
        fail("Throwing weapons should remain mainhand items")

    print("PASS: ranged equipment slot contracts validated")


if __name__ == "__main__":
    main()
