#!/usr/bin/env python3
import re
import sys
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENERY_IDS = ROOT / "server/src/com/openrsc/server/constants/SceneryId.java"
SCENERY_DEFS = ROOT / "server/conf/server/defs/GameObjectDef.xml"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
ORE_CRUSHER = ROOT / "server/plugins/com/openrsc/server/plugins/custom/skills/crafting/OreCrusher.java"


EXPECTED_ORES = {
    "TIN_ORE": (20, 90, 9, 1, 0),
    "COPPER_ORE": (30, 85, 13, 2, 0),
    "IRON_ORE": (40, 70, 23, 6, 1),
    "MITHRIL_ORE": (50, 50, 35, 13, 2),
    "ADAMANTITE_ORE": (60, 30, 42, 23, 5),
    "RUNITE_ORE": (80, 15, 35, 40, 10),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def scenery_index(name: str) -> int:
    index = -1
    for line in SCENERY_DEFS.read_text(encoding="utf-8").splitlines():
        if "<GameObjectDef>" in line:
            index += 1
        if f"<name>{name}</name>" in line:
            return index
    fail(f"Missing scenery definition for {name}")


def main() -> None:
    scenery_ids = SCENERY_IDS.read_text(encoding="utf-8")
    scenery_defs = SCENERY_DEFS.read_text(encoding="utf-8")
    scenery_locs = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    client_defs = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    crusher = ORE_CRUSHER.read_text(encoding="utf-8")

    if scenery_index("Ore crusher") != 1324:
        fail("Ore crusher scenery definition should resolve to id 1324")
    if "ORE_CRUSHER(1324)" not in scenery_ids:
        fail("SceneryId should expose ORE_CRUSHER as id 1324")
    expected_client = 'new GameObjectDef("Ore crusher", "A machine for crushing ore to find gems", "WalkTo", "Examine", 1, 2, 2, 0, "madmachine", ++i)); //1324'
    if expected_client not in client_defs:
        fail("Client should define Ore crusher as id 1324 with madmachine model")

    crusher_def = re.search(
        r"<name>Ore crusher</name>.*?<width>2</width>\s*<height>2</height>.*?<objectModel>madmachine</objectModel>",
        scenery_defs,
        re.DOTALL,
    )
    if crusher_def is None:
        fail("Ore crusher should be a 2x2 madmachine scenery object")
    if "<description>A machine for crushing ore to find gems</description>" not in scenery_defs:
        fail("Ore crusher should describe that it crushes ore to find gems")

    locs_by_pos = {}
    for loc in scenery_locs:
        pos = loc.get("pos", {})
        locs_by_pos.setdefault((pos.get("X"), pos.get("Y")), []).append(loc)

    if {"id": 48, "pos": {"X": 343, "Y": 601}, "direction": 6} not in scenery_locs:
        fail("Crafting guild downstairs sink should replace the west table at 343,601 with the flipped orientation")
    for loc in locs_by_pos.get((344, 601), []):
        if loc.get("id") == 3:
            fail("Crafting guild downstairs east table at 344,601 should be removed")
    for loc in locs_by_pos.get((343, 601), []):
        if loc.get("id") == 3:
            fail("Crafting guild downstairs west table at 343,601 should be removed")
    if {"id": 1324, "pos": {"X": 343, "Y": 1547}, "direction": 2} not in scenery_locs:
        fail("Ore crusher should replace the upstairs crafting guild sink at 343,1547 facing west")
    for loc in locs_by_pos.get((343, 1547), []):
        if loc.get("id") == 48:
            fail("Upstairs crafting guild sink should be moved downstairs")

    if "obj.getID() == SceneryId.ORE_CRUSHER.id()" not in crusher:
        fail("Ore crusher plugin should target SceneryId.ORE_CRUSHER")
    for gem in ("UNCUT_SAPPHIRE", "UNCUT_EMERALD", "UNCUT_RUBY", "UNCUT_DIAMOND"):
        if f"ItemId.{gem}.id()" not in crusher:
            fail(f"Ore crusher should be able to produce {gem}")

    for ore, values in EXPECTED_ORES.items():
        expected = f"new OreCrushDef(ItemId.{ore}.id(), {values[0]}, {values[1]}, {values[2]}, {values[3]}, {values[4]})"
        if expected not in crusher:
            fail(f"Ore crusher has wrong chance table for {ore}")

    table_body = re.search(r"ORE_CRUSH_DEFS = \{(.*?)\};", crusher, re.DOTALL)
    if table_body is None:
        fail("Could not find ore crusher definition table")
    for item in ("COAL", "SILVER", "GOLD", "CLAY", "STONE"):
        if f"ItemId.{item}" in table_body.group(1):
            fail(f"Ore crusher should not accept {item}")

    print("PASS: ore crusher scenery and ore conversion rules look correct")


if __name__ == "__main__":
    main()
