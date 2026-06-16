#!/usr/bin/env python3
import json
import re
import struct
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require_text(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def png_size(path: Path) -> tuple[int, int]:
    if not path.exists():
        fail(f"Missing PNG asset: {path}")
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        fail(f"Asset is not a PNG: {path}")
    return struct.unpack(">II", data[16:24])


def load_json(path: Path, key: str) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    entries = data.get(key)
    if not isinstance(entries, list):
        fail(f"{path.name} must contain a top-level {key!r} list")
    return entries


def require_entry(entries: list[dict], entry_id: int, label: str) -> dict:
    for entry in entries:
        if entry.get("id") == entry_id:
            return entry
    fail(f"{label} missing id {entry_id}")


def require_drop(block: str, item_name: str, weight: int, label: str) -> None:
    require_text(block, f"ItemId.{item_name}.id(), 1, {weight}", label)


def require_sword_assets(asset_name: str, display_name: str, icon_size: tuple[int, int], requires_equipment: bool = True) -> None:
    frame_dir = ROOT / f"dev/myworld/assets/sprites/equipment/{asset_name}/numbered"
    frames = sorted(frame_dir.glob("*.png"))
    if requires_equipment:
        if len(frames) != 18:
            fail(f"{display_name} equipment must have 18 numbered frames, found {len(frames)}")
        expected_sizes = [
            (6, 36), (12, 33), (18, 34), (19, 33), (12, 34), (6, 36),
            (20, 34), (13, 34), (6, 36), (10, 35), (5, 34), (10, 30),
            (9, 17), (10, 26), (6, 36), (18, 34), (18, 34), (34, 18),
        ]
        for index, expected in enumerate(expected_sizes):
            actual = png_size(frame_dir / f"{index:02d}.png")
            if actual != expected:
                fail(f"{display_name} frame {index:02d}.png expected {expected}, found {actual}")
    icon_path = ROOT / f"dev/myworld/assets/sprites/items/inventory-ground/weapons/{asset_name}-icon.png"
    if png_size(icon_path) != icon_size:
        fail(f"{display_name} icon must preserve the provided {icon_size[0]}x{icon_size[1]} icon canvas")


def require_sword_item(
    custom_items: list[dict],
    myworld_items: list[dict],
    entry_id: int,
    display_name: str,
    appearance_id: int,
) -> None:
    custom_entry = require_entry(custom_items, entry_id, "ItemDefsCustom")
    expected_custom = {
        "name": display_name,
        "appearanceID": appearance_id,
        "wearableID": 16,
        "wearSlot": 4,
        "basePrice": 40000,
    }
    for field, expected in expected_custom.items():
        if custom_entry.get(field) != expected:
            fail(f"ItemDefsCustom {display_name} expected {field}={expected}, found {custom_entry.get(field)}")

    myworld_entry = require_entry(myworld_items, entry_id, "ItemDefsMyWorld")
    for field, expected in {"meleeOffense": 72, "weaponSpeed": 4, "basePrice": 40000}.items():
        if myworld_entry.get(field) != expected:
            fail(f"ItemDefsMyWorld {display_name} expected {field}={expected}, found {myworld_entry.get(field)}")


def main() -> None:
    require_sword_assets("fire-sword", "Fire sword", (48, 28))
    require_sword_assets("ice-sword", "Ice sword", (45, 29))
    require_sword_assets("earth-sword", "Earth sword", (46, 26), requires_equipment=False)

    item_id = ROOT / "server/src/com/openrsc/server/constants/ItemId.java"
    item_id_text = item_id.read_text(encoding="utf-8")
    require_text(item_id_text, "FIRE_SWORD(3235)", "ItemId enum")
    require_text(item_id_text, "ICE_SWORD(3236)", "ItemId enum")
    require_text(item_id_text, "EARTH_SWORD(3237)", "ItemId enum")
    require_text(item_id_text, "maxCustom = 3238", "ItemId maxCustom")

    custom_items = load_json(ROOT / "server/conf/server/defs/ItemDefsCustom.json", "items")
    myworld_items = load_json(ROOT / "server/conf/server/defs/ItemDefsMyWorld.json", "items")
    require_sword_item(custom_items, myworld_items, 3235, "Fire sword", 1035)
    require_sword_item(custom_items, myworld_items, 3236, "Ice sword", 1036)
    require_sword_item(custom_items, myworld_items, 3237, "Earth sword", 49)

    client_handler = (ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java").read_text(encoding="utf-8")
    require_text(client_handler, 'setCustomItemDefinition(3235, new ItemDef("Fire sword"', "Client item definition")
    require_text(client_handler, 'setCustomItemDefinition(3236, new ItemDef("Ice sword"', "Client item definition")
    require_text(client_handler, 'setCustomItemDefinition(3237, new ItemDef("Earth sword"', "Client item definition")
    require_text(client_handler, '"external-png:fire-sword-icon"', "Client Fire sword icon")
    require_text(client_handler, '"external-png:ice-sword-icon"', "Client Ice sword icon")
    require_text(client_handler, '"external-png:earth-sword-icon"', "Client Earth sword icon")
    require_text(client_handler, 'new AnimationDef("firesword", "equipment", 0, 0, true, false, 0)); // 1035 - Fire sword', "Client Fire sword animation")
    require_text(client_handler, 'new AnimationDef("icesword", "equipment", 0, 0, true, false, 0)); // 1036 - Ice sword', "Client Ice sword animation")

    mudclient = (ROOT / "Client_Base/src/orsc/mudclient.java").read_text(encoding="utf-8")
    require_text(mudclient, 'loadExternalCombatMainHandEquipmentSprite("firesword"', "External Fire sword equipment loader")
    require_text(mudclient, 'loadExternalCombatMainHandEquipmentSprite("icesword"', "External Ice sword equipment loader")
    require_text(mudclient, "PLAYER_EQUIPPABLE_HASCOMBAT", "Elemental sword combat equipment entry")

    drops = (ROOT / "server/src/com/openrsc/server/constants/NpcDrops.java").read_text(encoding="utf-8")
    fire_giant = re.search(r'DropTable\("Fire Giant \(344\)"\).*?npcDrops\.put\(NpcId\.FIRE_GIANT\.id\(\)', drops, re.DOTALL)
    red_dragon = re.search(r'DropTable\("Red Dragon \(201\)"\).*?npcDrops\.put\(NpcId\.RED_DRAGON\.id\(\)', drops, re.DOTALL)
    ice_giant = re.search(r'DropTable\("Ice Giant \(135\)"\).*?npcDrops\.put\(NpcId\.ICE_GIANT\.id\(\)', drops, re.DOTALL)
    blue_dragon = re.search(r'DropTable\("Blue Dragon \(202\)"\).*?npcDrops\.put\(NpcId\.BLUE_DRAGON\.id\(\)', drops, re.DOTALL)
    moss_giant = re.search(r'DropTable\("Moss Giant \(104, 594\)"\).*?npcDrops\.put\(NpcId\.MOSS_GIANT2\.id\(\)', drops, re.DOTALL)
    green_dragon = re.search(r'DropTable\("Green Dragon \(196\)"\).*?npcDrops\.put\(NpcId\.DRAGON\.id\(\)', drops, re.DOTALL)
    if fire_giant is None:
        fail("Fire Giant drop block not found")
    if red_dragon is None:
        fail("Red Dragon drop block not found")
    if ice_giant is None:
        fail("Ice Giant drop block not found")
    if blue_dragon is None:
        fail("Blue Dragon drop block not found")
    if moss_giant is None:
        fail("Moss Giant drop block not found")
    if green_dragon is None:
        fail("Green Dragon drop block not found")
    require_drop(fire_giant.group(0), "FIRE_SWORD", 1, "Fire Giant Fire sword drop")
    require_drop(red_dragon.group(0), "FIRE_SWORD", 2, "Red Dragon Fire sword drop")
    require_drop(ice_giant.group(0), "ICE_SWORD", 1, "Ice Giant Ice sword drop")
    require_drop(blue_dragon.group(0), "ICE_SWORD", 2, "Blue Dragon Ice sword drop")
    require_drop(moss_giant.group(0), "EARTH_SWORD", 1, "Moss Giant Earth sword drop")
    require_drop(green_dragon.group(0), "EARTH_SWORD", 2, "Green Dragon Earth sword drop")

    combat_formula = (ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/CombatFormula.java").read_text(encoding="utf-8")
    require_text(combat_formula, "applyFireSwordElementalBonus", "Fire sword combat hook")
    require_text(combat_formula, "applyIceSwordElementalBonus", "Ice sword combat hook")
    require_text(combat_formula, "applyEarthSwordElementalBonus", "Earth sword combat hook")
    require_text(combat_formula, "Math.ceil(damage * 1.5D)", "Fire sword elemental multiplier")
    require_text(combat_formula, "NpcId.BLUE_DRAGON", "Fire sword blue target eligibility")
    require_text(combat_formula, "NpcId.ICE_GIANT", "Fire sword ice target eligibility")
    require_text(combat_formula, "NpcId.EARTH_WARRIOR", "Ice sword earth target eligibility")
    require_text(combat_formula, 'npcName.contains("earth")', "Ice sword earth name eligibility")
    require_text(combat_formula, "NpcId.RED_DRAGON", "Earth sword red target eligibility")
    require_text(combat_formula, "NpcId.FIRE_GIANT", "Earth sword fire target eligibility")
    require_text(combat_formula, 'npcName.contains("fire")', "Earth sword fire name eligibility")


if __name__ == "__main__":
    main()
