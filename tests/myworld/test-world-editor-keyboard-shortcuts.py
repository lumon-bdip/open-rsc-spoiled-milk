#!/usr/bin/env python3
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SHORTCUTS = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/WorldEditorKeyboardShortcuts.java"
UI = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/WorldEditorInterface.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
OPENGL_INPUT = ROOT / "PC_Client/src/orsc/OpenGLInputBridge.java"
OPENGL_KEYS = ROOT / "PC_Client/src/orsc/OpenGLKeyBindings.java"
OPENGL_KEY = ROOT / "PC_Client/src/orsc/KeyBinding.java"


class WorldEditorKeyboardShortcutsTest(unittest.TestCase):
    def test_shortcut_resolution_is_complete_and_unambiguous(self):
        harness = """
            package com.openrsc.interfaces.misc;

            public final class WorldEditorKeyboardShortcutHarness {
                private static void expect(
                    WorldEditorKeyboardShortcuts.Action expected,
                    char typed,
                    int key,
                    int physical,
                    boolean control,
                    boolean shift,
                    boolean terrain,
                    boolean enabled) {
                    WorldEditorKeyboardShortcuts.Action actual =
                        WorldEditorKeyboardShortcuts.resolve(
                            typed, key, physical, control, shift, terrain, enabled);
                    if (actual != expected) {
                        throw new AssertionError(expected + " != " + actual);
                    }
                }

                public static void main(String[] args) {
                    WorldEditorKeyboardShortcuts.Action A;
                    A = WorldEditorKeyboardShortcuts.Action.BRUSH;
                    expect(A, 'b', 'b', 'B', false, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.NAVIGATE, 'n', 'n', 'N', false, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.INSPECT, 'i', 'i', 'I', false, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.DOCK, 'd', 'd', 'D', false, false, false, true);

                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_ELEVATION, 'h', 'h', 'H', false, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_FLOOR_COLOR, 'c', 'c', 'C', false, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_FLOOR_TEXTURE, 't', 't', 'T', false, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_ROOF, 'r', 'r', 'R', false, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_NORTH_WALL, 'N', 'N', 'N', false, true, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_EAST_WALL, 'E', 'E', 'E', false, true, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_DIAGONAL_WALL, 'D', 'D', 'D', false, true, true, true);

                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_ELEVATION, '\\b', '\\b', 'H', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_FLOOR_COLOR, 'c', 'c', 'C', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_FLOOR_TEXTURE, 't', 't', 'T', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_ROOF, 'r', 'r', 'R', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_NORTH_WALL, 'n', 'n', 'N', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_EAST_WALL, 'e', 'e', 'E', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.EDIT_DIAGONAL_WALL, 'd', 'd', 'D', true, false, true, true);
                    expect(WorldEditorKeyboardShortcuts.Action.SAVE, 'S', 'S', 'S', true, true, false, true);

                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_CHAT, '\\r', '\\r', 10, true, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.TOGGLE_CHAT, '\\r', '\\r', 10, true, false, false, false);
                    expect(WorldEditorKeyboardShortcuts.Action.NONE, '\\r', '\\r', 'M', true, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.NONE, 'b', 'b', 'B', false, false, false, false);
                    expect(WorldEditorKeyboardShortcuts.Action.NONE, 'h', 'h', 'H', false, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.NONE, 'e', 'e', 'E', true, false, false, true);
                    expect(WorldEditorKeyboardShortcuts.Action.NONE, 'E', 'E', 'E', true, true, true, true);
                    System.out.println("world-editor-shortcuts-ok");
                }
            }
        """
        with tempfile.TemporaryDirectory(prefix="world-editor-shortcuts-") as output:
            harness_path = Path(output) / "WorldEditorKeyboardShortcutHarness.java"
            harness_path.write_text(textwrap.dedent(harness), encoding="utf-8")
            subprocess.run(
                ["javac", "-d", output, str(SHORTCUTS), str(harness_path)],
                cwd=ROOT,
                check=True,
                text=True,
                capture_output=True,
            )
            result = subprocess.run(
                ["java", "-cp", output, "com.openrsc.interfaces.misc.WorldEditorKeyboardShortcutHarness"],
                cwd=ROOT,
                check=True,
                text=True,
                capture_output=True,
            )
        self.assertEqual("world-editor-shortcuts-ok", result.stdout.strip())

    def test_editor_capture_and_both_desktop_input_paths_share_the_mapping(self):
        ui = UI.read_text(encoding="utf-8")
        client = CLIENT.read_text(encoding="utf-8")
        applet = APPLET.read_text(encoding="utf-8")
        opengl = OPENGL_INPUT.read_text(encoding="utf-8")
        keys = OPENGL_KEYS.read_text(encoding="utf-8")
        keys += "\n" + OPENGL_KEY.read_text(encoding="utf-8")

        self.assertIn("keyboardShortcutsEnabled=true", ui)
        self.assertIn("isKeyboardCaptureActive()", ui)
        self.assertIn("isKeyboardShortcutMode()", ui)
        self.assertIn("Chat input enabled; Ctrl+Enter restores editor shortcuts.", ui)
        self.assertIn("Editor shortcuts enabled; Ctrl+Enter opens chat input.", ui)
        self.assertIn("openTerrainValueEditor(6)", ui)
        self.assertIn("openTerrainValueEditor(12)", ui)
        self.assertIn("if(toolbar.isCollapsed())toolbar.toggleCollapsed()", ui)
        self.assertIn("if(mode==Mode.TERRAIN&&terrainActiveField==0)toggleBrushSize()", ui)
        self.assertIn("if(mode==Mode.INSPECT)copyInspected()", ui)
        self.assertIn("clickTeleportPreferred=!clickTeleportPreferred", ui)

        self.assertIn("handleDesktopKeyPress", client)
        self.assertIn("getDesktopKeyCode()", client)
        self.assertIn("shouldSuppressWorldEditorChatInput", client)
        self.assertIn("worldEditorInterface.isKeyboardCaptureActive()", client)
        self.assertIn("controlPressed && (key == 10 || key == 13)", client)
        self.assertIn("updateTerrainDrag(controlPressed,currentMouseButtonDown==1", client)

        self.assertIn("mudclient.handleDesktopKeyPress((byte) 126, (int) keyChar, keyCode);", applet)
        self.assertGreaterEqual(applet.count("shouldSuppressWorldEditorChatInput(keyCode)"), 5)
        self.assertIn("boolean isWorldEditorShortcutMode()", applet)
        self.assertLess(
            applet.index("handleDesktopKeyPress((byte) 126"),
            applet.index("if (keyCode == 39) mudclient.keyRight = true;"),
        )

        self.assertIn("boolean editorShortcut = pressed", opengl)
        self.assertIn("&& binding.isLetter()", opengl)
        self.assertIn("&& OpenRSC.applet.isWorldEditorShortcutMode()", opengl)
        self.assertIn("Character.isLetter(codepoint)", opengl)
        self.assertIn("boolean isLetter()", keys)
        self.assertIn("if (repeated && !binding.postsRepeatPressEvents())", opengl)


if __name__ == "__main__":
    unittest.main()
