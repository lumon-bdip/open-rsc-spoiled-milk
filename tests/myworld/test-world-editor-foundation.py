#!/usr/bin/env python3
import re
import struct
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SERVER_ARCHIVE = ROOT / "server/conf/server/data/Custom_Landscape.orsc"
CLIENT_ARCHIVE = ROOT / "Client_Base/Cache/video/Custom_Landscape.orsc"

class WorldEditorFoundationTest(unittest.TestCase):
    def test_client_server_archives_are_byte_identical(self):
        with zipfile.ZipFile(SERVER_ARCHIVE) as server, zipfile.ZipFile(CLIENT_ARCHIVE) as client:
            self.assertEqual(server.namelist(), client.namelist())
            for name in server.namelist():
                self.assertEqual(server.read(name), client.read(name), name)

    def test_all_sector_tiles_round_trip_byte_exact(self):
        with zipfile.ZipFile(SERVER_ARCHIVE) as archive:
            for name in archive.namelist():
                raw = archive.read(name)
                self.assertEqual(48 * 48 * 10, len(raw), name)
                rebuilt = bytearray()
                for offset in range(0, len(raw), 10):
                    fields = struct.unpack(">6Bi", raw[offset:offset + 10])
                    rebuilt.extend(struct.pack(">6Bi", *fields))
                self.assertEqual(raw, bytes(rebuilt), name)

    def test_coordinate_materialization_matches_archive_entry(self):
        samples = [(128, 640, 0), (512, 1700, 1), (216, 2840, 3)]
        with zipfile.ZipFile(SERVER_ARCHIVE) as archive:
            names = set(archive.namelist())
            for x, y, plane in samples:
                self.assertEqual(plane, y // 944)
                base_y = y - plane * 944
                sx, sy = (x + 2304) // 48, (base_y + 1776) // 48
                name = f"h{plane}x{sx}y{sy}"
                if name in names:
                    raw = archive.read(name)
                    index = (x % 48) * 48 + (base_y % 48)
                    self.assertEqual(10, len(raw[index * 10:index * 10 + 10]))

    def test_gate_and_protocol_are_read_only_by_construction(self):
        config = (ROOT / "server/src/com/openrsc/server/ServerConfiguration.java").read_text()
        self.assertIn('tryReadBool("allow_in_game_world_editor").orElse(false)', config)
        for path in (ROOT / "server/myworld.conf", ROOT / "server/myworld-host.conf"):
            self.assertRegex(path.read_text(), r"allow_in_game_world_editor:\s*false")
        incoming = (ROOT / "server/src/com/openrsc/server/net/rsc/handlers/WorldEditorHandler.java").read_text()
        self.assertNotRegex(incoming.lower(), r"\b(paint|place|delete|rotate|save|publish)\b")
        self.assertIn("Unsupported read-only editor operation", incoming)

    def test_desktop_shell_and_session_guards_are_present(self):
        ui = (ROOT / "Client_Base/src/com/openrsc/interfaces/misc/WorldEditorInterface.java").read_text()
        self.assertIn("Config.isAndroid()", ui)
        self.assertIn('"Navigate","Inspect","Terrain","Scenery","NPC"', ui)
        self.assertIn("editing disabled", ui.lower())
        sessions = (ROOT / "server/src/com/openrsc/server/content/worldedit/WorldEditorSessionManager.java").read_text()
        self.assertIn("!player.isAdmin()", sessions)
        self.assertIn("sequence != active.nextSequence", sessions)
        world = (ROOT / "server/src/com/openrsc/server/model/world/World.java").read_text()
        self.assertIn("getWorldEditorSessions().closeFor(player)", world)

    def test_navigate_and_inspect_have_distinct_behavior(self):
        ui = (ROOT / "Client_Base/src/com/openrsc/interfaces/misc/WorldEditorInterface.java").read_text()
        self.assertIn("mode==Mode.INSPECT", ui)
        self.assertIn("mode==Mode.NAVIGATE&&clickTeleportPreferred", ui)
        self.assertIn("Last clicked tile", ui)
        self.assertIn("Brush: inactive at", ui)
        self.assertIn("Teleport to coordinates", ui)
        self.assertIn("Reserved for a later editor phase", ui)
        client = (ROOT / "Client_Base/src/orsc/mudclient.java").read_text()
        self.assertIn("setWorldEditorNavigateClickTeleport", client)
        self.assertIn("worldEditorTeleport", client)
        self.assertIn("isClickTeleportActiveForCurrentContext", client)
        terrain_menu = re.search(
            r"if \(worldEditorInterface != null && worldEditorInterface\.isInspecting\(\)\) \{(?P<body>.*?)\n\s*\}",
            client,
            re.S,
        )
        self.assertIsNotNone(terrain_menu)
        self.assertIn("WORLD_EDITOR_COPY_TERRAIN", client)
        self.assertIn("WORLD_EDITOR_COPY_OBJECT", client)
        self.assertIn("WORLD_EDITOR_COPY_NPC", client)
        for label in (
            "Coordinates:", "Floor Color:", "Floor Texture:", "Walls: North",
            "First floor", "Second floor", "Underground", "Copy inspected",
        ):
            self.assertIn(label, ui)

if __name__ == "__main__":
    unittest.main()
