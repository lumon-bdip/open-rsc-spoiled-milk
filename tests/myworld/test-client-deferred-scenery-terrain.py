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
            and "xTile < 95 && zTile < 95" in client,
            "Terrain checks should match the elevation interpolation window")
    require("if (hasLoadedTerrainForWallObject(x, y, dir)) {\n\t\t\tthis.scene.addModel(model);\n\t\t}" in client,
            "Boundary models should only be added to the scene when terrain exists below them")

    add_model = "mc.getScene().addModel(m);"
    terrain_gate = "if (mc.hasLoadedTerrainForGameObject(xTile, zTile, id, dir)) {"
    store_record = "mc.setGameObjectInstanceModel(mc.getGameObjectInstanceCount(), m);"
    require(terrain_gate in packet_handler,
            "Scenery model drawing should be gated on loaded terrain")
    require(packet_handler.index(terrain_gate) < packet_handler.index(add_model),
            "Scenery terrain gate should wrap scene insertion")
    require(packet_handler.index(add_model) < packet_handler.index(store_record),
            "Scenery should still be stored after optional drawing")
    require("m.translate2(xWorld, -mc.getWorld().getElevation(xWorld, zWorld), zWorld);" in packet_handler,
            "Scenery should only query terrain elevation inside the loaded-terrain draw path")
    require("mc.getWorld().addGameObject_UpdateCollisionMap(xTile, zTile, id, false);" in packet_handler,
            "Scenery collision should be applied only when the local terrain tile is loaded")

    print("PASS: client defers scenery drawing until terrain is loaded")


if __name__ == "__main__":
    main()
