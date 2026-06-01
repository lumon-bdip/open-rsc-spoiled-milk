#!/usr/bin/env python3
import json
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
ITEM_DEFS_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefs.json"
ITEM_DEFS_PATCH18_PATH = ROOT / "server" / "conf" / "server" / "defs" / "ItemDefsPatch18.json"
ENTITY_HANDLER_PATH = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"
CRAFTING_PATH = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "skills" / "crafting" / "Crafting.java"

UNCUT_GEMS = {
    157: "uncut diamond",
    158: "uncut ruby",
    159: "uncut emerald",
    160: "uncut sapphire",
    542: "uncut dragonstone",
    889: "Uncut Red Topaz",
    890: "Uncut Jade",
    891: "Uncut Opal",
}

CUT_GEMS = {
    161: "diamond",
    162: "ruby",
    163: "emerald",
    164: "sapphire",
    892: "Red Topaz",
    893: "Jade",
    894: "Opal",
}


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def load_item_defs(path: Path, root_key: str) -> dict[int, dict]:
    return {item["id"]: item for item in json.loads(path.read_text(encoding="utf-8"))[root_key]}


def assert_server_stackability(items: dict[int, dict], expected: dict[int, str], stackable: int, label: str) -> None:
    for item_id, name in expected.items():
        item = items.get(item_id)
        if item is None:
            fail(f"{label} missing item {item_id} ({name})")
        if item["name"] != name:
            fail(f"{label} item {item_id} should be {name}, found {item['name']}")
        if item["isStackable"] != stackable:
            fail(f"{label} item {item_id} ({name}) stackable should be {stackable}, found {item['isStackable']}")


def require_client(snippet: str, label: str, client_defs: str) -> None:
    if snippet not in client_defs:
        fail(f"EntityHandler.java missing {label}: {snippet}")


def require_crafting(snippet: str, label: str, crafting: str) -> None:
    if snippet not in crafting:
        fail(f"Crafting.java missing {label}: {snippet}")


def main() -> None:
    item_defs = load_item_defs(ITEM_DEFS_PATH, "item")
    assert_server_stackability(item_defs, UNCUT_GEMS, 1, "ItemDefs.json")
    assert_server_stackability(item_defs, CUT_GEMS, 0, "ItemDefs.json")

    patch18_defs = load_item_defs(ITEM_DEFS_PATCH18_PATH, "item")
    assert_server_stackability(
        patch18_defs,
        {item_id: UNCUT_GEMS[item_id] for item_id in (157, 158, 159, 160)},
        1,
        "ItemDefsPatch18.json",
    )
    assert_server_stackability(
        patch18_defs,
        {item_id: CUT_GEMS[item_id] for item_id in (161, 162, 163, 164)},
        0,
        "ItemDefsPatch18.json",
    )

    client_defs = ENTITY_HANDLER_PATH.read_text(encoding="utf-8")
    require_client('new ItemDef("uncut diamond", "this would be worth more cut", "", 200, 73, "items:73", true,', "stackable uncut diamond", client_defs)
    require_client('new ItemDef("uncut ruby", "this would be worth more cut", "", 100, 73, "items:73", true,', "stackable uncut ruby", client_defs)
    require_client('new ItemDef("uncut emerald", "this would be worth more cut", "", 50, 73, "items:73", true,', "stackable uncut emerald", client_defs)
    require_client('new ItemDef("uncut sapphire", "this would be worth more cut", "", 25, 73, "items:73", true,', "stackable uncut sapphire", client_defs)
    require_client('new ItemDef("uncut dragonstone", "this would be worth more cut", "", 1000, 73, "items:73", true,', "stackable uncut dragonstone", client_defs)
    require_client('new ItemDef("Uncut Red Topaz", "A semi precious stone", "", 40, 73, "items:73", true,', "stackable uncut red topaz", client_defs)
    require_client('new ItemDef("Uncut Jade", "A semi precious stone", "", 30, 73, "items:73", true,', "stackable uncut jade", client_defs)
    require_client('new ItemDef("Uncut Opal", "A semi precious stone", "", 20, 73, "items:73", true,', "stackable uncut opal", client_defs)

    require_client('new ItemDef("diamond", "this looks valuable", "", 2000, 74, "items:74", false,', "unstackable diamond", client_defs)
    require_client('new ItemDef("ruby", "this looks valuable", "", 1000, 74, "items:74", false,', "unstackable ruby", client_defs)
    require_client('new ItemDef("emerald", "this looks valuable", "", 500, 74, "items:74", false,', "unstackable emerald", client_defs)
    require_client('new ItemDef("sapphire", "this looks valuable", "", 250, 74, "items:74", false,', "unstackable sapphire", client_defs)
    require_client('new ItemDef("Red Topaz", "A semi precious stone", "", 200, 74, "items:74", false,', "unstackable red topaz", client_defs)
    require_client('new ItemDef("Jade", "A semi precious stone", "", 150, 74, "items:74", false,', "unstackable jade", client_defs)
    require_client('new ItemDef("Opal", "A semi precious stone", "", 100, 74, "items:74", false,', "unstackable opal", client_defs)

    crafting = CRAFTING_PATH.read_text(encoding="utf-8")
    require_crafting(
        "Item gemToCut = new Item(item.getCatalogId(), 1, item.getNoted(), item.getItemId());",
        "single gem removal from stacked uncut gems",
        crafting,
    )
    require_crafting(
        "if (!player.getCarriedItems().getInventory().canHold(cutGem, freedSlots)) {",
        "inventory-full guard before cutting stacked gems",
        crafting,
    )

    print("PASS: uncut gems stack while cut gems stay unstackable")


if __name__ == "__main__":
    main()
