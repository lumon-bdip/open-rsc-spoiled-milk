#!/usr/bin/env python3
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TILE = ROOT / "server/src/com/openrsc/server/model/world/region/TileValue.java"
FLAGS = ROOT / "server/src/com/openrsc/server/util/rsc/CollisionFlag.java"

HARNESS = r"""
import com.openrsc.server.model.world.region.TileValue;
import com.openrsc.server.util.rsc.CollisionFlag;

public final class WorldEditorTileCollisionHarness {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        TileValue tile = new TileValue();
        tile.initializeTerrainCollision();
        require(tile.traversalMask == 0, "initial terrain collision was not cleared");

        tile.addTerrainCollision(CollisionFlag.WALL_NORTH);
        tile.addDynamicCollision(CollisionFlag.WALL_NORTH);
        tile.removeTerrainCollision(CollisionFlag.WALL_NORTH);
        require((tile.traversalMask & CollisionFlag.WALL_NORTH) != 0,
            "removing terrain erased dynamic wall collision");
        tile.removeDynamicCollision(CollisionFlag.WALL_NORTH);
        require((tile.traversalMask & CollisionFlag.WALL_NORTH) == 0,
            "wall collision remained after both sources were removed");

        tile.setTerrainBlocked(true);
        tile.addBlockingScenery();
        tile.setTerrainBlocked(false);
        require((tile.traversalMask & CollisionFlag.FULL_BLOCK_C) != 0,
            "removing terrain overlay erased blocking scenery");
        tile.removeBlockingScenery();
        require((tile.traversalMask & CollisionFlag.FULL_BLOCK_C) == 0,
            "full block remained after both sources were removed");

        tile.addTerrainWallProjectileBlock();
        tile.addDynamicProjectileBlock();
        tile.removeTerrainWallProjectileBlock();
        require(!tile.originalProjectileAllowed && tile.projectileAllowed,
            "dynamic projectile collision did not survive terrain removal");
        tile.removeDynamicProjectileBlock();
        require(!tile.projectileAllowed, "projectile collision remained without a source");
    }
}
"""

with tempfile.TemporaryDirectory(prefix="world-editor-collision-") as temp:
    temp_path = Path(temp)
    harness = temp_path / "WorldEditorTileCollisionHarness.java"
    harness.write_text(HARNESS, encoding="utf-8")
    subprocess.run(["javac", "-d", temp, str(FLAGS), str(TILE), str(harness)], check=True)
    subprocess.run(["java", "-cp", temp, "WorldEditorTileCollisionHarness"], check=True)

print("PASS: terrain and dynamic collision ownership remains independent")
