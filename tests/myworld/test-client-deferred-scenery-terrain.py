#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")

    require("public boolean hasLoadedTerrainForGameObject(int xTile, int zTile, int objectID, int dir)" in client,
            "Client should expose a terrain-availability check for scenery")
    require("private boolean hasLoadedTerrainForWallObject(int x, int y, int dir)" in client,
            "Client should check wall endpoints before drawing boundary models")
    require("private boolean hasLoadedTerrainForWorldPoint(int xWorld, int zWorld)" in client
            and "return World.isLocalFaceTile(xTile, zTile);" in client,
            "Terrain checks should match the elevation interpolation window")
    require("private final boolean[] gameObjectInstanceMaterialized" in client
            and "private final boolean[] wallObjectInstanceMaterialized" in client,
            "Deferred scenery should track explicit scene materialization state")
    require("public void materializeGameObjectInstance(int index)" in client
            and "if (!hasLoadedTerrainForGameObject(xTile, zTile, objectID, dir)) {\n\t\t\treturn;\n\t\t}" in client,
            "Scenery materialization should be gated on loaded terrain")
    require("public void materializeWallObjectInstance(int index)" in client
            and "if (!hasLoadedTerrainForWallObject(x, z, dir)) {\n\t\t\treturn;\n\t\t}" in client,
            "Boundary materialization should be gated before wall geometry is built")
    require("private void materializeLoadedTerrainScenery()" in client
            and "this.materializeLoadedTerrainScenery();" in client,
            "Region loads should retry deferred scenery once terrain is active")

    store_record = "mc.setGameObjectInstanceModel(instanceIndex, m);"
    materialize_record = "mc.materializeGameObjectInstance(instanceIndex);"
    capacity_guard = "if (!mc.hasGameObjectInstanceCapacity()) {\n\t\t\t\t\t\tcontinue;\n\t\t\t\t\t}"
    require("private static final int WALL_OBJECT_KEY_BASE = 20000;" in client
            and "private static final int GAME_OBJECT_INSTANCE_CAPACITY = WALL_OBJECT_KEY_BASE;" in client,
            "Scenery capacity should grow past the old 5000 limit without colliding with wall pick keys")
    require("private static final int WALL_OBJECT_INSTANCE_CAPACITY = 5000;" in client,
            "Boundary capacity should grow past the old 500 limit")
    require("new boolean[GAME_OBJECT_INSTANCE_CAPACITY]" in client
            and "new RSModel[GAME_OBJECT_INSTANCE_CAPACITY]" in client,
            "Scenery instance arrays should use the expanded named capacity")
    require("new boolean[WALL_OBJECT_INSTANCE_CAPACITY]" in client
            and "new RSModel[WALL_OBJECT_INSTANCE_CAPACITY]" in client,
            "Boundary instance arrays should use the expanded named capacity")
    require("public boolean hasGameObjectInstanceCapacity()" in client
            and "public boolean hasWallObjectInstanceCapacity()" in client,
            "Packet handlers should be able to guard instance appends")
    require(capacity_guard in packet_handler,
            "Scenery append packets should be ignored instead of overflowing local arrays")
    require("if (!mc.hasWallObjectInstanceCapacity()) {\n\t\t\t\t\t\tcontinue;\n\t\t\t\t\t}" in packet_handler,
            "Boundary append packets should be ignored instead of overflowing local arrays")
    require("var8.key >= WALL_OBJECT_KEY_BASE" in client
            and "var8.key - WALL_OBJECT_KEY_BASE" in client
            and "this.wallObjectInstanceModel[i].key = i + WALL_OBJECT_KEY_BASE;" in client,
            "Boundary pick-key encoding should move with the expanded scenery capacity")
    require(packet_handler.index(capacity_guard) < packet_handler.index(store_record),
            "Scenery capacity should be checked before storing a new instance")
    require(packet_handler.index(store_record) < packet_handler.index(materialize_record),
            "Scenery should be stored before terrain-ready materialization is retried")
    require("mc.setGameObjectInstanceMaterialized(instanceIndex, false);" in packet_handler,
            "New scenery packets should start as deferred materialization candidates")
    require("mc.setWallObjectInstanceModel(instanceIndex, null);" in packet_handler
            and "mc.materializeWallObjectInstance(instanceIndex);" in packet_handler,
            "Boundary packets should defer model construction until terrain endpoints are loaded")
    require("mc.dematerializeGameObjectInstance(" in packet_handler
            and "mc.dematerializeWallObjectInstance(" in packet_handler,
            "Packet removals should only remove scene/collision for materialized instances")
    require("this.world.addGameObject_UpdateCollisionMap(xTile, zTile, objectID, false);" in client,
            "Scenery collision should be applied only by terrain-ready materialization")
    require("this.world.applyWallToCollisionFlags(id, x, z, dir);" in client,
            "Boundary collision should be applied only by terrain-ready materialization")

    print("PASS: client defers scenery drawing until terrain is loaded")


if __name__ == "__main__":
    main()
