#!/usr/bin/env python3

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
RUNECRAFT_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocsRunecraft.json"
MYWORLD_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"

SERVER_OBELISK_IDS = {
    "air": 303,
    "water": 300,
    "earth": 304,
    "fire": 301,
    "mind": 1298,
    "body": 1299,
    "cosmic": 1300,
    "chaos": 1301,
    "nature": 1302,
    "law": 1303,
    "death": 1304,
    "blood": 1305,
    "soul": 1306,
    "life": 1322,
}


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def expected_corners(anchor: tuple[int, int]) -> set[tuple[int, int]]:
    x, y = anchor
    return {(x - 2, y + 3), (x + 3, y + 3), (x + 3, y - 2), (x - 2, y - 2)}


def load_scenery(path: Path) -> list[dict]:
    return json.loads(path.read_text(encoding="utf-8"))["sceneries"]


def parse_client_arrays() -> tuple[list[str], list[tuple[int, int]], list[list[tuple[int, int]]]]:
    text = CLIENT.read_text(encoding="utf-8")
    elements_match = re.search(r'ALTAR_ELEMENTS = new String\[\] \{(?P<body>.*?)\n\t\};', text, re.S)
    tiles_match = re.search(r'ALTAR_TILES = new int\[\]\[\] \{(?P<body>.*?)\n\t\};', text, re.S)
    obelisks_match = re.search(r'ALTAR_OBELISK_TILES = new int\[\]\[\]\[\] \{(?P<body>.*?)\n\t\};', text, re.S)
    if not elements_match or not tiles_match or not obelisks_match:
        fail("Could not parse client altar visual arrays")

    elements = re.findall(r'"([^"]+)"', elements_match.group("body"))
    anchors = [
        (int(x), int(y))
        for x, y in re.findall(r'\{(\d+),\s*(\d+)\}', tiles_match.group("body"))
    ]
    obelisks = []
    for line in obelisks_match.group("body").splitlines():
        coords = [
            (int(x), int(y))
            for x, y in re.findall(r'\{(\d+),\s*(\d+)\}', line)
        ]
        if coords:
            obelisks.append(coords)

    if not (len(elements) == len(anchors) == len(obelisks)):
        fail(f"Client altar array lengths differ: {len(elements)}, {len(anchors)}, {len(obelisks)}")
    return elements, anchors, obelisks


def main() -> None:
    elements, anchors, client_obelisks = parse_client_arrays()
    for element, anchor, obelisks in zip(elements, anchors, client_obelisks):
        actual = set(obelisks)
        expected = expected_corners(anchor)
        if actual != expected:
            fail(f"Client {element} obelisks were {sorted(actual)}, expected {sorted(expected)}")

    altar_by_id = {
        1191: "air",
        1195: "water",
        1197: "earth",
        1199: "fire",
        1193: "mind",
        1201: "body",
        1203: "cosmic",
        1205: "chaos",
        1207: "nature",
        1209: "law",
        1211: "death",
        1213: "blood",
        1296: "soul",
        1321: "life",
    }
    overworld_anchors = {}
    for loc in load_scenery(RUNECRAFT_LOCS) + load_scenery(MYWORLD_LOCS):
        element = altar_by_id.get(int(loc["id"]))
        if element is None:
            continue
        x, y = int(loc["pos"]["X"]), int(loc["pos"]["Y"])
        if y >= 90:
            overworld_anchors[element] = (x, y)

    locs = load_scenery(MYWORLD_LOCS)
    for element, object_id in SERVER_OBELISK_IDS.items():
        anchor = overworld_anchors.get(element)
        if anchor is None:
            fail(f"Missing overworld altar anchor for {element}")
        actual = {
            (int(loc["pos"]["X"]), int(loc["pos"]["Y"]))
            for loc in locs
            if int(loc["id"]) == object_id
        }
        expected = expected_corners(anchor)
        if actual != expected:
            fail(f"Server {element} obelisks were {sorted(actual)}, expected {sorted(expected)}")

    print("PASS: altar obelisk placements are symmetric")


if __name__ == "__main__":
    main()
