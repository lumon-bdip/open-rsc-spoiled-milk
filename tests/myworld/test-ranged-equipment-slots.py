#!/usr/bin/env python3
"""Validate ranged weapon equipment slot and visual contracts."""

import json
import re
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
ENTITY_HANDLER = ROOT / "server/src/com/openrsc/server/external/EntityHandler.java"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
EQUIPMENT = ROOT / "server/src/com/openrsc/server/model/container/Equipment.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
ITEM_DEFS = ROOT / "server/conf/server/defs/ItemDefs.json"
CUSTOM_ITEM_DEFS = ROOT / "server/conf/server/defs/ItemDefsCustom.json"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    entity_handler = ENTITY_HANDLER.read_text(encoding="utf-8")
    client_entity_handler = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    equipment = EQUIPMENT.read_text(encoding="utf-8")
    player = PLAYER.read_text(encoding="utf-8")
    item_defs = ITEM_DEFS.read_text(encoding="utf-8")
    custom_items = {
        int(item["id"]): item
        for item in json.loads(CUSTOM_ITEM_DEFS.read_text(encoding="utf-8"))["items"]
    }

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

    require(
        player,
        "RangeUtils.isBow(mainhand.getCatalogId())",
        "Bow visuals should be detected separately from their mainhand combat slot",
    )
    require(
        player,
        "visibleWornItems[AppearanceId.SLOT_SHIELD] = visibleWornItems[AppearanceId.SLOT_WEAPON];",
        "Bow visuals should be routed through the legacy offhand render layer",
    )
    require(
        player,
        "visibleWornItems[AppearanceId.SLOT_WEAPON] = AppearanceId.NOTHING.id();",
        "Bow visuals should clear the mainhand render layer after visual routing",
    )

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

    require(
        entity_handler,
        "items.get(ItemId.DRAGON_CROSSBOW.id()).setAppearanceId(471);",
        "Dragon crossbow should use the dragon crossbow equipment animation",
    )
    require(
        entity_handler,
        "items.get(ItemId.DRAGON_LONGBOW.id()).setAppearanceId(472);",
        "Dragon longbow should use the dragon longbow equipment animation",
    )
    require(
        client_entity_handler,
        'animations.add(new AnimationDef("crossbow", "equipment", 16711748, 0, false, false, 0)); //470 - dragon crossbow',
        "Client animation 470 should remain the dragon crossbow equipment visual",
    )
    require(
        client_entity_handler,
        'animations.add(new AnimationDef("longbow", "equipment", 16711748, 0, false, false, 0)); //471 - dragon longbow',
        "Client animation 471 should remain the dragon longbow equipment visual",
    )
    require(
        mudclient,
        "int animID = player.layerAnimation[layer] - 1;",
        "Client equipment rendering should still treat server appearance IDs as one-based animation IDs",
    )
    if custom_items[1453].get("appearanceID") != 471:
        fail("Dragon crossbow ItemDefsCustom appearanceID should be 471")
    if custom_items[1454].get("appearanceID") != 472:
        fail("Dragon longbow ItemDefsCustom appearanceID should be 472")

    print("PASS: ranged equipment slot contracts validated")


if __name__ == "__main__":
    main()
