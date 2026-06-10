#!/usr/bin/env python3
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_DEFS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
SERVER_DEFS = ROOT / "server/conf/server/defs/GameObjectDef.xml"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
SPARKLE_ID = 1325


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def invisible_path_tiles():
    rows = {
        **{y: ((149, 150),) for y in range(3542, 3554)},
        3554: ((102, 107), (149, 150)),
        3555: ((102, 107), (149, 150)),
        3556: ((102, 150),),
        3557: ((102, 150),),
        3558: ((102, 107),),
        3559: ((102, 107),),
    }
    return {
        (x, y)
        for y, spans in rows.items()
        for start_x, end_x in spans
        for x in range(start_x, end_x + 1)
    }


def expected_anchors():
    return (
        {(149, y) for y in range(3542, 3556, 2)}
        | {(x, 3556) for x in range(102, 150, 2)}
        | {(x, y) for x in range(102, 108, 2) for y in (3554, 3558)}
    )


def ensure_definitions():
    client_defs = CLIENT_DEFS.read_text(encoding="utf-8")
    require(
        re.search(
            r'GameObjectDef\("Cosmic sparkles".*?, 0, 2, 2, 0, '
            r'"myworld_cosmic_sparkles1", \+\+i\)\); //1325',
            client_defs,
        ),
        "Client sparkle object must remain a non-colliding 2x2 object",
    )

    definitions = ET.parse(SERVER_DEFS).getroot()
    sparkle = definitions[SPARKLE_ID]
    require(sparkle.findtext("name") == "Cosmic sparkles", "Server object 1325 must be Cosmic sparkles")
    require(sparkle.findtext("type") == "0", "Cosmic sparkles must not add collision")
    require(
        (sparkle.findtext("width"), sparkle.findtext("height")) == ("2", "2"),
        "Cosmic sparkles must occupy a 2x2 placement footprint",
    )
    require(
        sparkle.findtext("objectModel") == "myworld_cosmic_sparkles1",
        "Cosmic sparkles must use the first generated animation frame",
    )


def ensure_animation():
    client = CLIENT.read_text(encoding="utf-8")
    require("createMyWorldCosmicSparklesModel" in client, "Client must generate the sparkle model")
    require(
        '"myworld_cosmic_sparkles" + (this.objectAnimationNumberCosmicSparkles + 1)' in client,
        "Client must animate all four sparkle frames",
    )
    require("this.gameObjectInstanceID[centerX] == 1325" in client, "Animation must target object 1325")


def ensure_placements():
    locs = json.loads(SCENERY_LOCS.read_text(encoding="utf-8"))["sceneries"]
    anchors = {
        (loc["pos"]["X"], loc["pos"]["Y"])
        for loc in locs
        if loc["id"] == SPARKLE_ID
    }
    require(anchors == expected_anchors(), f"Unexpected cosmic sparkle placements: {sorted(anchors ^ expected_anchors())}")

    path = invisible_path_tiles()
    occupied = set()
    for x, y in anchors:
        footprint = {(x + dx, y + dy) for dx in range(2) for dy in range(2)}
        require(footprint <= path, f"Sparkle placement {(x, y)} extends beyond the invisible path")
        require(not occupied & footprint, f"Sparkle placement {(x, y)} overlaps another sparkle footprint")
        occupied |= footprint
    require(
        path - occupied == {(150, 3556), (150, 3557)},
        "Only the two-tile outside edge of the odd-width bend may remain uncovered",
    )


def main():
    ensure_definitions()
    ensure_animation()
    ensure_placements()
    print("PASS: cosmic sparkle path object and placements validated")


if __name__ == "__main__":
    main()
