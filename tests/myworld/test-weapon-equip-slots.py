#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BASE_ITEMS_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefs.json"
CUSTOM_ITEMS_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsCustom.json"
MYWORLD_ITEMS_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsMyWorld.json"

WEAPON_SLOT = 4
SHIELD_WEARABLE_MASK = 8
STAFF_OF_IBAN_ID = 1000
BOW_IDS = {
    188,
    189,
    648,
    649,
    650,
    651,
    652,
    653,
    654,
    655,
    656,
    657,
    1454,
    2117,
    2118,
    2121,
    2122,
    2125,
    2126,
    2129,
    2130,
}
WEAPON_NAME_TOKENS = (
    "sword",
    "dagger",
    "mace",
    "axe",
    "hatchet",
    "spear",
    "halberd",
    "scimitar",
    "club",
    "pickaxe",
    "knife",
    "dart",
    "bow",
    "crossbow",
    "staff",
    "battlestaff",
    "wand",
    "maul",
    "claws",
    "whip",
)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def load_json_entries(path: Path, key: str) -> list[dict]:
    if not path.exists():
        fail(f"Missing file: {path}")
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    entries = data.get(key)
    if not isinstance(entries, list):
        fail(f"{path.name} must contain a top-level '{key}' array")
    return entries


def load_effective_items() -> dict[int, dict]:
    effective_items = {
        entry["id"]: dict(entry) for entry in load_json_entries(BASE_ITEMS_PATH, "item")
    }
    for path, key in (
        (CUSTOM_ITEMS_PATH, "items"),
        (MYWORLD_ITEMS_PATH, "items"),
    ):
        for entry in load_json_entries(path, key):
            item_id = entry["id"]
            merged = dict(effective_items.get(item_id, {}))
            merged.update(entry)
            effective_items[item_id] = merged
    return effective_items


def require_weapon_slot(item: dict, label: str) -> None:
    actual_slot = item.get("wearSlot")
    if actual_slot != WEAPON_SLOT:
        fail(
            f"{label} id {item.get('id')} ({item.get('name', '<unnamed>')}) "
            f"must use wearSlot {WEAPON_SLOT}, found {actual_slot}"
        )


def require_does_not_conflict_with_shields(item: dict, label: str) -> None:
    wearable_id = item.get("wearableID")
    if not isinstance(wearable_id, int):
        fail(f"{label} id {item.get('id')} missing numeric wearableID")
    if wearable_id & SHIELD_WEARABLE_MASK:
        fail(
            f"{label} id {item.get('id')} ({item.get('name', '<unnamed>')}) "
            f"must not conflict with shields, found wearableID {wearable_id}"
        )


def is_weapon_named_wearable(item: dict) -> bool:
    if item.get("isWearable") != 1:
        return False
    name = item.get("name", "").lower()
    return any(token in name for token in WEAPON_NAME_TOKENS)


def main() -> None:
    items = load_effective_items()

    for item_id in sorted(BOW_IDS):
        item = items.get(item_id)
        if item is None:
            fail(f"Missing bow item id {item_id}")
        require_weapon_slot(item, "Bow family")

    iban_staff = items.get(STAFF_OF_IBAN_ID)
    if iban_staff is None:
        fail(f"Missing Staff of Iban id {STAFF_OF_IBAN_ID}")
    require_weapon_slot(iban_staff, "Staff of Iban")
    require_does_not_conflict_with_shields(iban_staff, "Staff of Iban")

    offenders = [
        item
        for item in items.values()
        if is_weapon_named_wearable(item) and item.get("wearSlot") != WEAPON_SLOT
    ]
    if offenders:
        formatted = ", ".join(
            f"{item['id']} {item.get('name', '<unnamed>')} slot={item.get('wearSlot')}"
            for item in sorted(offenders, key=lambda entry: entry["id"])
        )
        fail(f"Wearable weapon-named items outside weapon slot: {formatted}")

    print("OK: wearable weapon items use the weapon equipment slot")


if __name__ == "__main__":
    main()
