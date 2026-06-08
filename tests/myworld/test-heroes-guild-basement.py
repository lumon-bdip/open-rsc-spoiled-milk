#!/usr/bin/env python3
import json
import struct
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVER_LANDSCAPE = ROOT / "server/conf/server/data/Custom_Landscape.orsc"
CLIENT_LANDSCAPE = ROOT / "Client_Base/Cache/video/Custom_Landscape.orsc"
SCENERY_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocs.json"
NPC_LOCS = ROOT / "server/conf/server/defs/locs/NpcLocs.json"
MYWORLD_NPC_LOCS = ROOT / "server/conf/server/defs/locs/MyWorldNpcLocs.json"
MYWORLD_NPC_DEFS = ROOT / "server/conf/server/defs/NpcDefsMyWorld.json"
LADDERS = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/defaults/Ladders.java"
SECTOR = "h3x55y46"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_sector(path):
    with zipfile.ZipFile(path) as archive:
        require(archive.testzip() is None, f"{path} must be a valid landscape archive")
        return archive.read(SECTOR)


def tile(sector, x, y):
    offset = ((x - 336) * 48 + (y - 3264)) * 10
    return struct.unpack_from(">BBBBBBI", sector, offset)


server_sector = read_sector(SERVER_LANDSCAPE)
client_sector = read_sector(CLIENT_LANDSCAPE)
require(client_sector == server_sector, "Client and server Heroes' Guild terrain must match")

for x in range(365, 377):
    for y in range(3264, 3276):
        elevation, texture, overlay, roof, _, _, diagonal = tile(server_sector, x, y)
        require(
            (elevation, texture, overlay, roof, diagonal) == (128, 70, 3, 0, 0),
            f"Heroes' Guild chamber tile {x},{y} has unexpected terrain",
        )

for y in range(3264, 3276):
    require(tile(server_sector, 365, y)[4] == 1, "West chamber wall is incomplete")
    require(tile(server_sector, 377, y)[4] == 1, "East chamber wall is incomplete")

for x in range(365, 377):
    require(tile(server_sector, x, 3264)[5] == 1, "North chamber wall is incomplete")

for y in range(3264, 3277):
    _, _, overlay, roof, horizontal_wall, vertical_wall, diagonal = tile(
        server_sector, 377, y
    )
    require((overlay, roof, vertical_wall, diagonal) == (8, 0, 0, 0),
            f"East void column contains terrain at 377,{y}")
    require(
        horizontal_wall == 1,
        f"East cage wall is incorrect at 377,{y}",
    )

for y in (3266, 3267, 3272, 3273):
    require(tile(server_sector, 369, y)[4] == 0, f"West gate gap is blocked at y={y}")
    require(tile(server_sector, 373, y)[4] == 0, f"East gate gap is blocked at y={y}")

for y in (3264, 3265, 3268, 3270, 3271, 3274, 3275):
    require(tile(server_sector, 369, y)[4] == 6, f"West aisle railing is missing at y={y}")

for y in (3264, 3265, 3268, 3269, 3270, 3271, 3274, 3275):
    require(tile(server_sector, 373, y)[4] == 6, f"East aisle railing is missing at y={y}")

for y in (3269, 3276):
    require(tile(server_sector, 369, y)[4] == 0, f"West railing remains at 369,{y}")

for x in (*range(365, 370), *range(373, 377)):
    require(
        tile(server_sector, x, 3270)[5] == 6,
        f"Cage divider railing is missing at {x},3270",
    )
for x in range(370, 373):
    require(
        tile(server_sector, x, 3270)[5] == 0,
        f"Central aisle is blocked at {x},3270",
    )

for x in range(364, 377):
    _, texture, overlay, roof, horizontal_wall, vertical_wall, diagonal = tile(
        server_sector, x, 3276
    )
    require(
        texture == 176 and overlay == 0 and roof == 0 and diagonal == 0,
        f"Ore floor is missing at {x},3276",
    )
    require(
        horizontal_wall == (1 if x == 365 else 0),
        f"West approach wall is incorrect at {x},3276",
    )
    require(
        vertical_wall == (6 if x in (*range(365, 369), *range(373, 377)) else 0),
        f"South cage railing is incorrect at {x},3276",
    )

for x in range(365, 369):
    for y in range(3276, 3281):
        _, texture, overlay, roof, horizontal_wall, vertical_wall, diagonal = tile(
            server_sector, x, y
        )
        require(
            texture == 176 and overlay == 0 and roof == 0 and diagonal == 0,
            f"Ore floor is missing at {x},{y}",
        )

for y in range(3275, 3279):
    require(
        tile(server_sector, 365, y)[4] == 1,
        f"West approach wall is missing at 365,{y}",
    )
require(tile(server_sector, 365, 3279)[4] == 0, "West approach wall extends too far")
for y in range(3276, 3281):
    require(tile(server_sector, 368, y)[4] == 0, f"Incorrect wall remains at 368,{y}")

require(tile(server_sector, 369, 3275)[4] == 6, "Greater Demon corner fence is missing")
require(tile(server_sector, 369, 3276)[5] == 0, "Greater Demon south fence protrudes")
require(tile(server_sector, 377, 3276)[4] == 1, "Dragon-side wall has a south gap")

_, _, overlay, roof, horizontal_wall, vertical_wall, diagonal = tile(
    server_sector, 354, 3276
)
require(
    (overlay, roof, horizontal_wall, vertical_wall, diagonal) == (8, 0, 0, 0, 0),
    "Out-of-bounds floor remains at 354,3276",
)

sceneries = json.loads(SCENERY_LOCS.read_text())["sceneries"]
room_sceneries = {
    (entry["id"], entry["pos"]["X"], entry["pos"]["Y"], entry["direction"])
    for entry in sceneries
    if 365 <= entry["pos"]["X"] <= 377 and 3264 <= entry["pos"]["Y"] <= 3276
}
require((41, 370, 3264, 4) in room_sceneries, "Central north-wall stairs are missing")
require(
    {(57, 369, 3266, 0), (57, 373, 3266, 0), (57, 369, 3272, 0), (57, 373, 3272, 0)}
    <= room_sceneries,
    "One or more aisle-facing cage gates are missing",
)
require((41, 368, 3270, 4) not in room_sceneries, "Old staircase placement remains")
require((57, 374, 3276, 6) not in room_sceneries, "Old dragon gate placement remains")

npcs = json.loads(NPC_LOCS.read_text())["npclocs"]
require(
    any(
        entry["id"] == 202
        and entry["start"] == {"X": 375, "Y": 3272}
        and entry["min"] == {"X": 374, "Y": 3271}
        and entry["max"] == {"X": 376, "Y": 3275}
        for entry in npcs
    ),
    "The existing Heroes' Guild blue dragon must remain in the lower-right cage",
)

myworld_npcs = json.loads(MYWORLD_NPC_LOCS.read_text())["npclocs"]


def cage_spawns(npc_id, minimum, maximum):
    return [
        entry
        for entry in myworld_npcs
        if entry["id"] == npc_id
        and entry["min"] == minimum
        and entry["max"] == maximum
    ]


require(
    len(cage_spawns(104, {"X": 366, "Y": 3265}, {"X": 368, "Y": 3269})) == 3,
    "Upper-left cage must contain three moss giants",
)
require(
    len(cage_spawns(294, {"X": 374, "Y": 3265}, {"X": 376, "Y": 3269})) == 1,
    "Upper-right cage must contain one hellhound",
)
require(
    len(cage_spawns(184, {"X": 366, "Y": 3271}, {"X": 368, "Y": 3275})) == 2,
    "Lower-left cage must contain two greater demons",
)

green_dragon = next(
    entry
    for entry in json.loads(MYWORLD_NPC_DEFS.read_text())["npcs"]
    if entry["id"] == 196
)
require(green_dragon.get("name") == "Green Dragon", "Earth Dragon must be renamed")
require(
    green_dragon.get("description") == "A powerful and ancient green dragon",
    "Green Dragon description must match its name",
)

ladders = LADDERS.read_text()
require(
    "obj.getID() == 42 && obj.getX() == 368 && obj.getY() == 438" in ladders
    and "player.teleport(371, 3267, false);" in ladders,
    "Surface Heroes' Guild stairs must land in the expanded chamber's central aisle",
)
require(
    "obj.getX() == 370 && obj.getY() == 3264" in ladders
    and "player.teleport(369, 440, false);" in ladders,
    "Expanded basement stairs must return beside the surface stairs",
)

print("Heroes' Guild basement layout checks passed")
