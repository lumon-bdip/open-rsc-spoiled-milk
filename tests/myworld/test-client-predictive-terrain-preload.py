#!/usr/bin/env python3
"""Exercise predictive terrain preload ownership, targets, and call boundaries."""

from __future__ import annotations

import subprocess
import tempfile
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_JAR = ROOT / "Client_Base/Open_RSC_Client.jar"
OWNER = ROOT / "Client_Base/src/orsc/PredictiveTerrainPreloader.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
PLAN = ROOT / "docs/myworld/in-progress-work-plans/renderer-v2-plan.md"


FIXTURE = r"""
package orsc;

public final class PredictiveTerrainPreloaderFixture {
	private static final int TILE_SIZE = 128;
	private static final int[] CAMERA_STEP_X = {0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] CAMERA_STEP_Z = {1, 1, 0, -1, -1, -1, 0, 1};

	private PredictiveTerrainPreloaderFixture() {
	}

	public static void main(String[] args) {
		readinessAndCoordinateConversion();
		targetThrottlePlaneAndReset();
		independentSourceThrottles();
		waypointSelectionAndImmutability();
		cameraDirectionsAndTargets();
	}

	private static void readinessAndCoordinateConversion() {
		assertTrue(!PredictiveTerrainPreloader.canPreload(false, true, false),
			"null world guard");
		assertTrue(!PredictiveTerrainPreloader.canPreload(true, false, false),
			"initial region guard");
		assertTrue(!PredictiveTerrainPreloader.canPreload(true, true, true),
			"area loading guard");
		assertTrue(PredictiveTerrainPreloader.canPreload(true, true, false),
			"ready state");

		assertEquals(0, PredictiveTerrainPreloader.localPixelToTile(64, TILE_SIZE),
			"tile origin");
		assertEquals(1, PredictiveTerrainPreloader.localPixelToTile(192, TILE_SIZE),
			"positive tile conversion");
		assertEquals(-1, PredictiveTerrainPreloader.localPixelToTile(63, TILE_SIZE),
			"negative floor conversion");
		assertEquals(-2, PredictiveTerrainPreloader.localPixelToTile(-65, TILE_SIZE),
			"negative multi-tile conversion");

		PredictiveTerrainPreloader preloader = new PredictiveTerrainPreloader();
		preloader.preloadIncomingWorldPosition(null, 100, 200, 5, -7, 2, true, false);
		PredictiveTerrainPreloader.PreloadRequest request =
			preloader.prepareTargetRequest(100, 200, 5, -7, 2);
		assertRequest(request, 105, 193, 2, "null world must not mark throttle");
	}

	private static void targetThrottlePlaneAndReset() {
		PredictiveTerrainPreloader preloader = new PredictiveTerrainPreloader();
		int preparedCalls = 0;
		PredictiveTerrainPreloader.PreloadRequest request =
			preloader.prepareTargetRequest(100, 200, 5, -7, 2);
		preparedCalls += request == null ? 0 : 1;
		assertRequest(request, 105, 193, 2, "offset-adjusted target");
		assertNull(preloader.prepareTargetRequest(100, 200, 5, -7, 2),
			"exact repeat suppressed");
		assertNull(preloader.prepareTargetRequest(110, 210, 5, -7, 2),
			"same-section repeat suppressed");

		request = preloader.prepareTargetRequest(150, 250, 5, -7, 2);
		preparedCalls += request == null ? 0 : 1;
		assertRequest(request, 155, 243, 2, "new section target");
		request = preloader.prepareTargetRequest(150, 250, 5, -7, 3);
		preparedCalls += request == null ? 0 : 1;
		assertRequest(request, 155, 243, 3, "plane change target");
		assertEquals(3, preparedCalls, "target preload request count");

		preloader.reset();
		assertRequest(preloader.prepareTargetRequest(150, 250, 5, -7, 3),
			155, 243, 3, "reset clears target throttle");
		preloader.reset();
		preloader.reset();
		assertRequest(preloader.prepareTargetRequest(150, 250, 5, -7, 3),
			155, 243, 3, "repeated reset remains safe");
	}

	private static void independentSourceThrottles() {
		PredictiveTerrainPreloader preloader = new PredictiveTerrainPreloader();
		ORSCharacter player = new ORSCharacter();
		player.waypointIndexCurrent = 0;
		player.waypointIndexNext = 0;
		player.waypointsX[0] = 64;
		player.waypointsZ[0] = 64;
		player.currentX = -48 * TILE_SIZE + 64;
		player.currentZ = 64;

		PredictiveTerrainPreloader.PreloadRequest target =
			preloader.prepareTargetRequest(100, 100, 0, 0, 0);
		PredictiveTerrainPreloader.PreloadRequest waypoint =
			preloader.prepareWaypointRequest(player, 100, 100, 0, 0, 0, TILE_SIZE);
		PredictiveTerrainPreloader.PreloadRequest camera =
			preloader.prepareCameraRequest(player, 100, 100, 0, 0, 0, 64, TILE_SIZE);
		assertRequest(target, 100, 100, 0, "target source");
		assertRequest(waypoint, 100, 100, 0, "waypoint source same section");
		assertRequest(camera, 100, 100, 0, "camera source same section");
		assertNull(preloader.prepareTargetRequest(100, 100, 0, 0, 0),
			"target source repeat");
		assertNull(preloader.prepareWaypointRequest(player, 100, 100, 0, 0, 0, TILE_SIZE),
			"waypoint source repeat");
		assertNull(preloader.prepareCameraRequest(player, 100, 100, 0, 0, 0, 64, TILE_SIZE),
			"camera source repeat");
	}

	private static void waypointSelectionAndImmutability() {
		PredictiveTerrainPreloader preloader = new PredictiveTerrainPreloader();
		assertNull(preloader.prepareWaypointRequest(null, 100, 200, 5, 7, 1, TILE_SIZE),
			"null waypoint player");

		ORSCharacter player = new ORSCharacter();
		player.waypointIndexCurrent = 0;
		player.waypointIndexNext = 1;
		player.waypointsX[1] = 63;
		player.waypointsZ[1] = -65;
		assertNull(preloader.prepareWaypointRequest(player, 100, 200, 5, 7, 1, TILE_SIZE),
			"inactive waypoint slot");

		player.waypointIndexNext = 0;
		player.waypointsX[0] = 63;
		player.waypointsZ[0] = -65;
		int currentIndexBefore = player.waypointIndexCurrent;
		int nextIndexBefore = player.waypointIndexNext;
		int waypointXBefore = player.waypointsX[0];
		int waypointZBefore = player.waypointsZ[0];
		assertRequest(preloader.prepareWaypointRequest(player, 100, 200, 5, 7, 1, TILE_SIZE),
			104, 205, 1, "active waypoint target");
		assertEquals(currentIndexBefore, player.waypointIndexCurrent, "waypoint current immutable");
		assertEquals(nextIndexBefore, player.waypointIndexNext, "waypoint next immutable");
		assertEquals(waypointXBefore, player.waypointsX[0], "waypoint x immutable");
		assertEquals(waypointZBefore, player.waypointsZ[0], "waypoint z immutable");

		player.waypointIndexNext = -1;
		assertNull(preloader.prepareWaypointRequest(player, 100, 200, 5, 7, 1, TILE_SIZE),
			"negative waypoint index");
		player.waypointIndexNext = player.waypointsX.length;
		assertNull(preloader.prepareWaypointRequest(player, 100, 200, 5, 7, 1, TILE_SIZE),
			"oversized waypoint index");
	}

	private static void cameraDirectionsAndTargets() {
		PredictiveTerrainPreloader preloader = new PredictiveTerrainPreloader();
		ORSCharacter player = new ORSCharacter();
		player.currentX = 64;
		player.currentZ = 64;
		for (int direction = 0; direction < 8; direction++) {
			preloader.reset();
			int expectedX = 1010 + CAMERA_STEP_X[direction] * 48;
			int expectedZ = 1980 + CAMERA_STEP_Z[direction] * 48;
			PredictiveTerrainPreloader.PreloadRequest request = preloader.prepareCameraRequest(
				player, 1000, 2000, 10, -20, 3, direction * 32, TILE_SIZE);
			assertRequest(request, expectedX, expectedZ, 3, "camera direction " + direction);
			assertNull(preloader.prepareCameraRequest(
				player, 1000, 2000, 10, -20, 3, direction * 32, TILE_SIZE),
				"camera repeat " + direction);
			assertRequest(preloader.prepareCameraRequest(
				player, 1000, 2000, 10, -20, 4, direction * 32, TILE_SIZE),
				expectedX, expectedZ, 4, "camera plane change " + direction);
		}
		assertNull(preloader.prepareCameraRequest(null, 1000, 2000, 10, -20, 3, 0, TILE_SIZE),
			"null camera player");
	}

	private static void assertRequest(
		PredictiveTerrainPreloader.PreloadRequest request,
		int worldX,
		int worldZ,
		int plane,
		String label) {
		if (request == null) {
			throw new AssertionError(label + ": expected preload request");
		}
		assertEquals(worldX, request.getWorldX(), label + " world x");
		assertEquals(worldZ, request.getWorldZ(), label + " world z");
		assertEquals(plane, request.getPlane(), label + " plane");
	}

	private static void assertNull(Object value, String label) {
		if (value != null) {
			throw new AssertionError(label + ": expected null");
		}
	}

	private static void assertTrue(boolean condition, String label) {
		if (!condition) {
			throw new AssertionError(label);
		}
	}

	private static void assertEquals(int expected, int actual, String label) {
		if (expected != actual) {
			throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
		}
	}
}
"""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def ensure_client_jar() -> None:
    source_mtime = max(OWNER.stat().st_mtime, MUDCLIENT.stat().st_mtime)
    if CLIENT_JAR.is_file() and CLIENT_JAR.stat().st_mtime >= source_mtime:
        return
    subprocess.run([str(ROOT / "scripts/build-client.sh")], check=True, cwd=ROOT)


def assert_source_boundaries() -> None:
    owner = OWNER.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    plan = PLAN.read_text(encoding="utf-8")

    require("final class PredictiveTerrainPreloader" in owner,
            "predictive terrain preload owner missing")
    require("private final PredictiveTerrainPreloader predictiveTerrainPreloader" in mudclient,
            "mudclient must retain one preloader owner")
    for moved_state in (
        "PREDICTIVE_PRELOAD_SOURCE_TARGET",
        "PREDICTIVE_CAMERA_STEP_X",
        "predictivePreloadPlane",
        "predictiveWaypointPreloadPlane",
        "predictiveCameraPreloadPlane",
    ):
        require(moved_state not in mudclient, f"mudclient still owns {moved_state}")

    for contract in (
        "SOURCE_TARGET",
        "SOURCE_WAYPOINT",
        "SOURCE_CAMERA",
        "CAMERA_PRELOAD_DISTANCE_TILES = World.SECTION_SIZE",
        "World.worldTileToSection(preloadWorldX)",
        "world.preloadSections(request.worldX, request.worldZ, request.plane)",
        "int inactiveWaypoint = (localPlayer.waypointIndexCurrent + 1) % localPlayer.waypointsX.length;",
        "int direction = ((cameraRotation + 16) / 32) & 7;",
        "Math.floorDiv(localPixel - 64, tileSize)",
    ):
        require(contract in owner, f"preloader owner missing contract: {contract}")

    for forbidden in (
        "PacketHandler",
        "MovementTimingDiagnostics",
        "movementLastFrameMillis",
        "movementPixelRemainder",
        "appendCustomMovementWaypoint",
        "loadNextRegion",
        "Renderer",
        "Scene",
        "WorldStreamManager",
        "queueCpuSectionWindowPreload",
    ):
        require(forbidden not in owner, f"preloader absorbed forbidden authority: {forbidden}")

    require("public void preloadTerrainForIncomingWorldPosition(int worldX, int worldZ)" in mudclient,
            "public incoming-position compatibility facade missing")
    require("this.predictiveTerrainPreloader.preloadIncomingWorldPosition(" in mudclient,
            "incoming-position facade must delegate")
    require("this.predictiveTerrainPreloader.preloadLocalTarget(" in mudclient,
            "local-target facade must delegate")
    require("this.predictiveTerrainPreloader.preloadActiveMovementContext(" in mudclient,
            "active-context facade must delegate")
    require("this.predictiveTerrainPreloader.reset();" in mudclient,
            "reset facade must delegate")

    standard_preload = (
        "mc.preloadTerrainForIncomingWorldPosition(mc.getLocalPlayerX(), mc.getLocalPlayerZ());"
    )
    standard_region = (
        "boolean needNextRegion = mc.loadNextRegion(mc.getLocalPlayerZ(), mc.getLocalPlayerX(), false);"
    )
    require(standard_preload in packet_handler and standard_region in packet_handler,
            "standard movement preload/region calls missing")
    require(packet_handler.index(standard_preload) < packet_handler.index(standard_region),
            "standard movement preload must remain before region selection")

    custom_preload = "preloadTerrainForIncomingWorldPosition(worldX, worldZ);"
    custom_region = "boolean needNextRegion = loadNextRegion(worldZ, worldX, false);"
    require(custom_preload in mudclient and custom_region in mudclient,
            "custom movement preload/region calls missing")
    require(mudclient.index(custom_preload) < mudclient.index(custom_region),
            "custom movement preload must remain before region selection")

    for call_site in (
        "preloadTerrainForLocalTarget(tileX, tileZ);",
        "preloadTerrainForLocalTarget(x1, z1);",
        "this.preloadTerrainForActiveMovementContext();",
        "this.resetPredictiveTerrainPreload();",
    ):
        require(call_site in mudclient, f"preload lifecycle call missing: {call_site}")

    for retained_movement in (
        "private int movementAmountForFrame(",
        "private void resetMovementInterpolation(",
        "private boolean appendCustomMovementWaypoint(",
    ):
        require(retained_movement in mudclient,
                f"unrelated movement responsibility left mudclient: {retained_movement}")

    require("Start predictive terrain preload from client walk intent" in plan,
            "renderer plan should retain the completed predictive preload slice")
    require("[x] Extend prediction to active local-player waypoint/camera direction" in plan,
            "renderer plan should retain active waypoint/camera completion")
    require("persistent CPU terrain chunks" in plan,
            "renderer plan should keep the next streaming backend step visible")


def run_fixture() -> None:
    ensure_client_jar()
    with tempfile.TemporaryDirectory(prefix="predictive-terrain-preloader-") as temp_name:
        temp = Path(temp_name)
        source = temp / "orsc/PredictiveTerrainPreloaderFixture.java"
        source.parent.mkdir(parents=True)
        source.write_text(textwrap.dedent(FIXTURE), encoding="utf-8")
        subprocess.run(
            ["javac", "-source", "1.8", "-target", "1.8", "-cp", str(CLIENT_JAR),
             "-d", str(temp), str(source)],
            check=True,
            cwd=ROOT,
        )
        subprocess.run(
            ["java", "-cp", f"{temp}:{CLIENT_JAR}",
             "orsc.PredictiveTerrainPreloaderFixture"],
            check=True,
            cwd=ROOT,
        )


def main() -> None:
    assert_source_boundaries()
    run_fixture()
    print("PASS: predictive terrain preload ownership, targets, throttles, and boundaries are stable")


if __name__ == "__main__":
    main()
