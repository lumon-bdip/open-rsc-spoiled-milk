#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OVERRIDES = ROOT / "tools" / "generators" / "item-overrides" / "20-ranged-weapons.json"
SERVER_ITEMS = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsMyWorld.json"
CLIENT_ITEMS = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"

SHURIKEN_IDS = tuple(range(3208, 3228))
EXPECTED_OFFENSE = {item_id: 2 + ((item_id - 3208) % 10) for item_id in SHURIKEN_IDS}


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def load_items(path: Path) -> dict[int, dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return {int(entry["id"]): entry for entry in data["items"]}


def require_equal(actual: object, expected: object, description: str) -> None:
    if actual != expected:
        fail(f"{description}: expected {expected!r}, found {actual!r}")


def main() -> None:
    source_items = load_items(OVERRIDES)
    server_items = load_items(SERVER_ITEMS)
    client_items = CLIENT_ITEMS.read_text(encoding="utf-8")

    for item_id in SHURIKEN_IDS:
        for label, items in (("source", source_items), ("generated server", server_items)):
            item = items.get(item_id)
            if item is None:
                fail(f"{label} shuriken balance missing id {item_id}")
            require_equal(item.get("weaponSpeed"), 6, f"{label} shuriken {item_id} speed")
            require_equal(item.get("rangedOffense"), EXPECTED_OFFENSE[item_id], f"{label} shuriken {item_id} ranged offense")
            require_equal(item.get("wearableID"), 24, f"{label} shuriken {item_id} two-handed wearable id")

    if 'true, true, 24, pictureMask' not in client_items:
        fail("client shuriken definitions should use two-handed wearable id 24")

    print("PASS: Shuriken balance is two-handed, very fast, and low damage")


if __name__ == "__main__":
    main()
