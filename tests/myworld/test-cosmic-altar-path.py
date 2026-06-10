#!/usr/bin/env python3
import re
import struct
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVER_LANDSCAPE = ROOT / "server/conf/server/data/Custom_Landscape.orsc"
CLIENT_LANDSCAPE = ROOT / "Client_Base/Cache/video/Custom_Landscape.orsc"
SERVER_TILE_DEFS = ROOT / "server/conf/server/defs/TileDef.xml"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
SECTORS = ("h3x50y51", "h3x50y52", "h3x51y51", "h3x51y52")
INVISIBLE_OVERLAY = 26
VOID_TILE = (0, 0, 8, 0, 0, 0, 0)
INVISIBLE_PATH_TILE = (96, 100, INVISIBLE_OVERLAY, 0, 0, 0, 0)


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_sectors(path):
    with zipfile.ZipFile(path) as archive:
        require(archive.testzip() is None, f"{path} must be a valid landscape archive")
        return {sector: archive.read(sector) for sector in SECTORS}


def sector_name(x, y):
    height = y // 944
    y_in_height = y - height * 944
    return f"h{height}x{x // 48 + 48}y{y_in_height // 48 + 37}"


def sector_origin(sector):
    height = int(sector[1])
    section_x = int(sector[3:5])
    section_y = int(sector[6:8])
    return (section_x - 48) * 48, height * 944 + (section_y - 37) * 48


def tile(sectors, x, y):
    sector = sector_name(x, y)
    origin_x, origin_y = sector_origin(sector)
    offset = ((x - origin_x) * 48 + (y - origin_y)) * 10
    return struct.unpack_from(">BBBBBBI", sectors[sector], offset)


def old_grass_tiles():
    rows = {
        3541: ((150, 150),),
        3542: ((148, 151),),
        3543: ((148, 151),),
        3544: ((148, 151),),
        3545: ((148, 151),),
        3546: ((148, 151),),
        3547: ((148, 151),),
        3548: ((148, 151),),
        3549: ((148, 151),),
        3550: ((148, 151),),
        3551: ((148, 151),),
        3552: ((147, 152),),
        3553: ((147, 152),),
        3554: ((97, 144), (147, 152)),
        3555: ((97, 144), (147, 152)),
        3556: ((97, 151),),
        3557: ((97, 150),),
        3558: ((97, 101), (138, 149)),
        3559: ((97, 101), (139, 148)),
        3560: ((97, 101),),
        3561: ((97, 101),),
        3562: ((97, 102),),
        3563: ((97, 103),),
        3564: ((97, 111),),
        3565: ((97, 111),),
        3566: ((98, 111),),
        3567: ((99, 111),),
        3568: ((100, 111),),
        3569: ((101, 111),),
    }
    return {
        (x, y)
        for y, spans in rows.items()
        for start_x, end_x in spans
        for x in range(start_x, end_x + 1)
    }


def invisible_path_tiles():
    path = {(x, y) for x in range(106, 149) for y in (3541, 3542)}
    path.update((x, y) for y in range(3541, 3567) for x in (106, 107))
    return path


def object_support_tiles():
    return {
        (106, 3565),
        (104, 3564),
        (109, 3564),
        (104, 3568),
        (109, 3568),
    }


def ensure_transparent_walkable_tile_def():
    tile_defs = SERVER_TILE_DEFS.read_text()
    require(
        re.search(
            r"<colour>12345678</colour>\s*<unknown>5</unknown>\s*<objectType>0</objectType>",
            tile_defs,
        ),
        "Server must define a transparent, non-blocking overlay tile",
    )
    client_defs = CLIENT_ENTITY_HANDLER.read_text()
    require(
        "tiles.add(new TileDef(12345678, 5, 0));" in client_defs,
        "Client must define the same transparent, non-blocking overlay tile",
    )


def ensure_path_shape(sectors):
    preserved_torch_grass = {(150, 3541), (149, 3542), (150, 3542)}
    path = invisible_path_tiles()
    supports = object_support_tiles()

    for point in path | supports:
        require(tile(sectors, *point) == INVISIBLE_PATH_TILE, f"{point} is not transparent walkable path")

    for point in preserved_torch_grass:
        elevation, texture, overlay, roof, horizontal_wall, vertical_wall, diagonal_wall = tile(sectors, *point)
        require(
            (overlay, roof, horizontal_wall, vertical_wall, diagonal_wall) == (0, 0, 0, 0, 0),
            f"{point} should keep ordinary torch-gap grass",
        )
        require(elevation > 0 and texture > 0, f"{point} should keep visible grass terrain")

    removed_grass = old_grass_tiles() - path - supports - preserved_torch_grass
    for point in removed_grass:
        require(tile(sectors, *point) == VOID_TILE, f"{point} should be empty non-walkable void")

    require((149, 3541) not in path, "Path should start immediately west of the torch gap")
    require((148, 3541) in path and (148, 3542) in path, "Path entrance should be two tiles wide")
    require((108, 3566) not in path, "Path should remain exactly two tiles wide at the altar approach")


def main():
    ensure_transparent_walkable_tile_def()
    server_sectors = read_sectors(SERVER_LANDSCAPE)
    client_sectors = read_sectors(CLIENT_LANDSCAPE)
    require(client_sectors == server_sectors, "Client and server Cosmic Altar terrain must match")
    ensure_path_shape(server_sectors)
    print("PASS: cosmic altar invisible path terrain validated")


if __name__ == "__main__":
    main()
