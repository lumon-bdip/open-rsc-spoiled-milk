#!/usr/bin/env python3
"""Validate the craftable white cape uses cape visuals in inventory and equipment."""

import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
ITEM_DEFS_CUSTOM = ROOT / "server/conf/server/defs/ItemDefsCustom.json"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    client = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    server = ITEM_DEFS_CUSTOM.read_text(encoding="utf-8")

    require(
        client,
        'setCustomItemDefinition(2053, new ItemDef("White Cape", "A plain cape ready for dye", "", 8, 59, "items:59", false, true, 2048, 0xFFFFFF',
        "White cape should use the normal cape inventory sprite and white color mask",
    )
    require(
        server,
        '"id": 2053,\n      "name": "White Cape"',
        "White cape server definition should exist",
    )
    require(
        server,
        '"appearanceID": 216,\n      "wearableID": 2048,\n      "wearSlot": 11',
        "White cape should equip as the white cape appearance in the cape slot",
    )

    print("PASS: white cape definition validated")


if __name__ == "__main__":
    main()
