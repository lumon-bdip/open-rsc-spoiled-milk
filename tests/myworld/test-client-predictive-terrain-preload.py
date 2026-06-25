#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
PLAN = ROOT / "docs/myworld/renderer-v2-plan.md"


def require(text: str, needle: str, message: str) -> None:
    if needle not in text:
        raise AssertionError(message)


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require(client, "private int predictivePreloadPlane = Integer.MIN_VALUE;",
            "Predictive preload should track the last warmed plane")
    require(client, "private int predictiveWaypointPreloadPlane = Integer.MIN_VALUE;",
            "Waypoint prediction should keep a separate throttle from click-target prediction")
    require(client, "private int predictiveCameraPreloadPlane = Integer.MIN_VALUE;",
            "Camera prediction should keep a separate throttle from click-target prediction")
    require(client, "private static final int PREDICTIVE_CAMERA_PRELOAD_DISTANCE_TILES = World.SECTION_SIZE;",
            "Camera look-ahead should warm roughly one section in the viewed direction")
    require(client, "public void preloadTerrainForIncomingWorldPosition(int worldX, int worldZ)",
            "Packet/custom movement should be able to warm terrain from absolute world tiles")
    require(client, "private void preloadTerrainForLocalTarget(int localTileX, int localTileZ)",
            "Client walk intent should warm terrain from local target tiles")
    require(client, "int preloadWorldX = worldX + this.worldOffsetX;",
            "Predictive preload should use the same offset-adjusted coordinates as region loading")
    require(client, "int sectionX = World.worldTileToSection(preloadWorldX);",
            "Predictive preload should throttle by target section")
    require(client, "this.world.preloadSections(preloadWorldX, preloadWorldZ, this.requestedPlane);",
            "Predictive preload should warm the sector cache without forcing an active region swap")
    require(client, "preloadTerrainForLocalTarget(tileX, tileZ);",
            "Blink targets should warm terrain")
    require(client, "preloadTerrainForLocalTarget(x1, z1);",
            "Click-walk targets should warm terrain")
    require(client, "preloadTerrainForIncomingWorldPosition(worldX, worldZ);",
            "Custom movement updates should warm incoming terrain before region checks")
    require(client, "private void preloadTerrainForActiveMovementContext()",
            "Main-loop prediction should warm terrain from active movement state")
    require(client, "preloadTerrainForActiveWaypoint();",
            "Main-loop prediction should include the local player's active waypoint")
    require(client, "preloadTerrainForCameraDirection();",
            "Main-loop prediction should include camera-direction look-ahead")
    require(client, "int inactiveWaypoint = (this.localPlayer.waypointIndexCurrent + 1) % this.localPlayer.waypointsX.length;",
            "Waypoint prediction should follow the existing circular waypoint queue convention")
    require(client, "PREDICTIVE_CAMERA_STEP_X[direction] * PREDICTIVE_CAMERA_PRELOAD_DISTANCE_TILES",
            "Camera prediction should use the quantized camera direction to pick a target section")
    require(packet_handler, "mc.preloadTerrainForIncomingWorldPosition(mc.getLocalPlayerX(), mc.getLocalPlayerZ());",
            "Standard movement packets should warm incoming terrain before loadNextRegion")
    require(plan, "Start predictive terrain preload from client walk intent",
            "Renderer plan should record the completed predictive preload slice")
    require(plan, "[x] Extend prediction to active local-player waypoint/camera direction",
            "Renderer plan should mark active waypoint/camera prediction complete")
    require(plan, "persistent CPU terrain chunks",
            "Renderer plan should keep the next world-streaming backend step visible")

    print("PASS: client predictive terrain preload is wired")


if __name__ == "__main__":
    main()
