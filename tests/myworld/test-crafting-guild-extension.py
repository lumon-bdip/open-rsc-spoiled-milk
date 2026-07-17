#!/usr/bin/env python3
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
MASTER_CRAFTER = (
    ROOT
    / "server/plugins/com/openrsc/server/plugins/authentic/npcs/rimmington/MasterCrafter.java"
)

STAIRS_ID = 42
STAIRS_POSITION = (338, 614)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        raise SystemExit(f"FAIL: {label} missing expected snippet: {snippet}")


def main() -> None:
    sceneries = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    stairs = next(
        (
            loc
            for loc in sceneries
            if loc.get("id") == STAIRS_ID
            and (loc.get("pos", {}).get("X"), loc.get("pos", {}).get("Y")) == STAIRS_POSITION
        ),
        None,
    )
    if stairs is None:
        raise SystemExit("FAIL: missing Crafting Guild extension stairs at 338,614")
    if stairs.get("direction") != 2:
        raise SystemExit("FAIL: Crafting Guild extension stairs should use direction 2")

    plugin = MASTER_CRAFTER.read_text(encoding="utf-8")
    require(plugin, "implements TalkNpcTrigger, OpLocTrigger", "Master Crafter object trigger")
    require(plugin, "private static final int CRAFTING_GUILD_EXTENSION_STAIRS = 42;", "stair id")
    require(plugin, "Point.location(338, 614)", "stair coordinate")
    require(plugin, "obj.getID() == CRAFTING_GUILD_EXTENSION_STAIRS", "exact stair guard")
    require(plugin, "obj.getLocation().equals(CRAFTING_GUILD_EXTENSION_STAIRS_LOCATION)", "coordinate guard")
    require(plugin, 'command.equals("go down")', "use-action guard")
    require(plugin, '"That area is currently off limits"', "Master Crafter refusal")
    require(plugin, '"The Crafting Guild extension is currently off limits"', "fallback refusal")

    print("PASS: Crafting Guild extension stairs are placed and blocked by the Master Crafter")


if __name__ == "__main__":
    main()
