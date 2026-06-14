#!/usr/bin/env python3
import json
import struct
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVER_LANDSCAPE = ROOT / "server/conf/server/data/Custom_Landscape.orsc"
CLIENT_LANDSCAPE = ROOT / "Client_Base/Cache/video/Custom_Landscape.orsc"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"
MYWORLD_SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldSceneryLocs.json"
OBJECT_TELEPOINTS = ROOT / "server/conf/server/defs/extras/ObjectTelePoints.xml"
BASEMENT_SECTOR = "h3x58y46"
BASEMENT_SECTOR_ORIGIN_X = 480
BASEMENT_SECTOR_ORIGIN_Y = 3264
GROUND_SECTOR = "h0x58y46"
GROUND_SECTOR_ORIGIN_X = 480
GROUND_SECTOR_ORIGIN_Y = 432
BASEMENT_REQUIRED_WALKABLE_TILES = {
    (499, 3295),
    (499, 3296),
}
GROUND_DOWN_STAIR_TILES = {
    (x, y)
    for x in range(499, 501)
    for y in range(469, 472)
}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_sector(path, sector_name):
    with zipfile.ZipFile(path) as archive:
        require(archive.testzip() is None, f"{path} must be a valid landscape archive")
        return archive.read(sector_name)


def tile(sector, x, y, *, origin_x, origin_y):
    offset = ((x - origin_x) * 48 + (y - origin_y)) * 10
    return struct.unpack_from(">BBBBBBI", sector, offset)


def load_scenery(path):
    return json.loads(path.read_text(encoding="utf-8"))["sceneries"]


def scenery_tuple(loc):
    return (loc["id"], loc["pos"]["X"], loc["pos"]["Y"], loc["direction"])


def scenery_set(path):
    return {scenery_tuple(loc) for loc in load_scenery(path)}


def ensure_basement_terrain():
    server_sector = read_sector(SERVER_LANDSCAPE, BASEMENT_SECTOR)
    client_sector = read_sector(CLIENT_LANDSCAPE, BASEMENT_SECTOR)
    require(client_sector == server_sector, "Client and server Rangers Guild basement terrain must match")

    edited_tiles = []
    for x in range(BASEMENT_SECTOR_ORIGIN_X, BASEMENT_SECTOR_ORIGIN_X + 48):
        for y in range(BASEMENT_SECTOR_ORIGIN_Y, BASEMENT_SECTOR_ORIGIN_Y + 48):
            elevation, texture, overlay, roof, east_wall, north_wall, diagonal_wall = tile(
                server_sector,
                x,
                y,
                origin_x=BASEMENT_SECTOR_ORIGIN_X,
                origin_y=BASEMENT_SECTOR_ORIGIN_Y,
            )
            if overlay != 8 or east_wall or north_wall or diagonal_wall:
                edited_tiles.append((x, y, overlay, texture, elevation))

    require(
        len(edited_tiles) >= 500,
        "Rangers Guild basement terrain should contain the imported first-draft edit, not only the small seed",
    )
    xs = [entry[0] for entry in edited_tiles]
    ys = [entry[1] for entry in edited_tiles]
    require(
        min(xs) <= 484 and max(xs) >= 515 and min(ys) <= 3281 and max(ys) >= 3310,
        "Rangers Guild basement terrain edit no longer covers the expected first-draft footprint",
    )

    for x, y in BASEMENT_REQUIRED_WALKABLE_TILES:
        require(
            tile(
                server_sector,
                x,
                y,
                origin_x=BASEMENT_SECTOR_ORIGIN_X,
                origin_y=BASEMENT_SECTOR_ORIGIN_Y,
            )[2] != 8,
            f"Rangers Guild basement stair/landing tile should be walkable at {x},{y}",
        )

    server_sector = read_sector(SERVER_LANDSCAPE, GROUND_SECTOR)
    client_sector = read_sector(CLIENT_LANDSCAPE, GROUND_SECTOR)
    require(client_sector == server_sector, "Client and server Rangers Guild ground terrain must match")

    for x, y in GROUND_DOWN_STAIR_TILES:
        require(
            tile(
                server_sector,
                x,
                y,
                origin_x=GROUND_SECTOR_ORIGIN_X,
                origin_y=GROUND_SECTOR_ORIGIN_Y,
            )
            == (152, 70, 8, 0, 0, 0, 0),
            f"Ground-floor down-stair opening is wrong at {x},{y}",
        )


def ensure_scenery_layout():
    base = scenery_set(SCENERY_LOCS)
    myworld = scenery_set(MYWORLD_SCENERY_LOCS)

    for expected in {
        (272, 500, 467, 2),
        (272, 500, 464, 2),
        (274, 496, 471, 0),
        (41, 490, 466, 0),
        (42, 490, 1410, 0),
    }:
        require(expected in base, f"Missing base Rangers Guild scenery {expected}")

    for removed in {
        (272, 500, 466, 2),
        (272, 500, 465, 2),
        (272, 500, 468, 2),
        (274, 497, 471, 0),
    }:
        require(removed not in base, f"Old base scenery still present {removed}")

    for expected in {
        (145, 490, 464, 6),
        (47, 491, 471, 2),
        (279, 494, 471, 0),
        (42, 499, 469, 4),
        (41, 499, 3296, 0),
        (31, 496, 1408, 0),
        (31, 498, 1408, 0),
    }:
        require(expected in myworld, f"Missing MyWorld Rangers Guild scenery {expected}")

    for removed in {
        (145, 491, 464, 6),
        (47, 491, 470, 6),
        (279, 493, 471, 0),
        (42, 499, 469, 0),
        (31, 496, 1408, 4),
        (31, 498, 1408, 4),
    }:
        require(removed not in myworld, f"Old MyWorld scenery still present {removed}")


def ensure_stair_telepoints():
    root = ET.parse(OBJECT_TELEPOINTS).getroot()
    telepoints = {}
    for entry in root.findall("entry"):
        point = entry.find("Point")
        telepoint = entry.find("TelePoint")
        telepoints[
            (
                int(point.findtext("x")),
                int(point.findtext("y")),
                telepoint.findtext("command"),
            )
        ] = (int(telepoint.findtext("x")), int(telepoint.findtext("y")))

    require(
        telepoints.get((499, 469, "Go down")) == (499, 3295),
        "Ground-floor Rangers Guild stairs should lead to the basement seed",
    )
    require(
        telepoints.get((499, 3296, "Go up")) == (499, 468),
        "Basement Rangers Guild stairs should return to the ground floor",
    )


def main():
    ensure_basement_terrain()
    ensure_scenery_layout()
    ensure_stair_telepoints()
    print("PASS: Rangers Guild first-pass layout validated")


if __name__ == "__main__":
    main()
