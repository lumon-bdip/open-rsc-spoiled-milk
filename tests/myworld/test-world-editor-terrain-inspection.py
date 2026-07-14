#!/usr/bin/env python3
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / "server/core.jar"
LIB = ROOT / "server/lib/*"
CLIENT = (ROOT / "Client_Base/src/orsc/mudclient.java").read_text()
UI = (ROOT / "Client_Base/src/com/openrsc/interfaces/misc/WorldEditorInterface.java").read_text()
PACKET = (ROOT / "Client_Base/src/orsc/PacketHandler.java").read_text()
HANDLER = (ROOT / "server/src/com/openrsc/server/net/rsc/handlers/WorldEditorHandler.java").read_text()
GENERATOR = (ROOT / "server/src/com/openrsc/server/net/rsc/generators/impl/PayloadCustomGenerator.java").read_text()


def method_body(source, signature):
    start = source.index(signature)
    brace = source.index("{", start)
    depth = 0
    for position in range(brace, len(source)):
        if source[position] == "{":
            depth += 1
        elif source[position] == "}":
            depth -= 1
            if depth == 0:
                return source[brace + 1:position]
    raise AssertionError(f"unterminated method: {signature}")


menu = method_body(CLIENT, "private void addWorldEditorTileActions(int localX,int localZ)")
if not re.search(
    r"if\(worldEditorInterface\.isInspecting\(\)\|\|worldEditorInterface\.isTerrainPainting\(\)\)\s*"
    r"this\.menuCommon\.addCharacterItem_WithID\([^;]+WORLD_EDITOR_COPY_TERRAIN",
    menu,
):
    raise AssertionError("Brush mode does not expose terrain copy")
if not re.search(
    r"if\(worldEditorInterface\.isInspecting\(\)\)\s*"
    r"this\.menuCommon\.addCharacterItem_WithID\([^;]+WORLD_EDITOR_INSPECT_TERRAIN",
    menu,
):
    raise AssertionError("Inspect terrain is no longer restricted to Inspect mode")
for entity_action in ("WORLD_EDITOR_COPY_OBJECT", "WORLD_EDITOR_COPY_NPC"):
    if entity_action in menu:
        raise AssertionError(f"terrain tile menu unexpectedly contains {entity_action}")

for entity_action in ("WORLD_EDITOR_COPY_OBJECT", "WORLD_EDITOR_COPY_NPC"):
    position = CLIENT.index(entity_action)
    guard = CLIENT.rfind("worldEditorInterface.isInspecting()", 0, position)
    if guard < 0 or position - guard > 1200:
        raise AssertionError(f"{entity_action} is no longer kept behind Inspect mode")

if "o.groundOverlay=inspectionGroundOverlay(runtime,s.groundOverlay)" not in HANDLER:
    raise AssertionError("single-tile inspection does not report the runtime overlay")
if "tile.groundOverlay=inspectionGroundOverlay(runtime,s.groundOverlay)" not in HANDLER:
    raise AssertionError("stroke inspection does not report the runtime overlay")
if not re.search(
    r"writeByte\(\(byte\) editor\.groundTexture\);\s*"
    r"builder\.writeByte\(\(byte\) editor\.groundOverlay\)",
    GENERATOR,
):
    raise AssertionError("server inspection packet no longer serializes color before overlay")
if not re.search(
    r"texture=packetsIncoming\.getByte\(\)&0xff,overlay=packetsIncoming\.getByte\(\)&0xff",
    PACKET,
):
    raise AssertionError("client inspection packet no longer parses color before overlay")
if '"Floor Color: "+texture,"Floor Texture: "+overlay' not in UI:
    raise AssertionError("Inspect no longer labels raw overlay as Floor Texture")

if not CORE.exists():
    raise SystemExit("Missing server/core.jar; run ./scripts/build-server.sh first")

HARNESS = r"""
package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.world.region.TileValue;

public final class WorldEditorTerrainInspectionHarness {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        TileValue runtime = new TileValue();
        runtime.overlay = (byte) 197;
        require(WorldEditorHandler.inspectionGroundOverlay(runtime, 8) == 197,
            "inspection reported the archive fallback instead of the current overlay");
        runtime.overlay = (byte) 250;
        require(WorldEditorHandler.inspectionGroundOverlay(runtime, 8) == 250,
            "inspection did not preserve an unsigned overlay value");
        require(WorldEditorHandler.inspectionGroundOverlay(null, 24) == 24,
            "inspection lost its archive fallback outside runtime terrain");
    }
}
"""

with tempfile.TemporaryDirectory(prefix="world-editor-terrain-inspection-") as temp:
    source_root = Path(temp) / "com/openrsc/server/net/rsc/handlers"
    source_root.mkdir(parents=True)
    source = source_root / "WorldEditorTerrainInspectionHarness.java"
    source.write_text(HARNESS, encoding="utf-8")
    classpath = f"{CORE}:{LIB}"
    subprocess.run(["javac", "-cp", classpath, "-d", temp, str(source)], check=True)
    subprocess.run([
        "java", "-cp", f"{temp}:{classpath}",
        "com.openrsc.server.net.rsc.handlers.WorldEditorTerrainInspectionHarness",
    ], check=True)

print("PASS: terrain inspection overlay and Brush-mode copy boundaries validated")
