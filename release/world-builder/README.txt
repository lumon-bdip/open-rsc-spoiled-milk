SPOILED MILK WORLD BUILDER @VERSION@
====================================

This package edits a compatible Spoiled Milk private server without changing
its active map until you deliberately import your work.

INSTALLATION
------------

Place the entire "Spoiled Milk World Builder" folder directly inside the root
of your private server. The result should look like this:

  Your Private Server/
    Client_Base/
    server/
    Spoiled Milk World Builder/

Do not place it inside the server/ or Client_Base/ folder.

STARTING AND SAVING
-------------------

Linux: run "Start World Builder.sh".
Windows: double-click "Start World Builder.cmd".

The first launch validates the parent private server and creates an isolated
workspace. Later launches reopen that same workspace. The local Builder account
logs in automatically, is invulnerable, and opens the world editor.

Use the editor's Save control before closing. Saves affect only:

  Spoiled Milk World Builder/workspace/

They do not change the parent private server.

EDITOR KEYBOARD SHORTCUTS
-------------------------

Editor shortcuts are active by default and ordinary chat typing is paused.
Press Ctrl+Enter to switch between editor shortcuts and normal chat/command
entry. Numeric editor fields still accept typing when explicitly selected.

  B                 Brush; press again while the Brush tool is active to
                    switch between 1x1 and 3x3
  N                 Navigate; press again to toggle click teleport
  I                 Inspect; press again to copy the current inspection
  D                 Hide or show the editor dock
  H / C / T / R     Toggle Height, Floor Color, Floor Texture, or Roof while
                    in Terrain mode
  Shift+N / E / D   Toggle North, East, or Diagonal Wall in Terrain mode
  Ctrl+H/C/T/R      Open that terrain value and select its numeric field
  Ctrl+N/E/D        Open that wall value and select its numeric field
  Ctrl+Shift+S      Save world edits
  Ctrl+Enter        Toggle editor-shortcut mode and chat/command-entry mode

Arrow keys and camera controls remain available in either input mode.

IMPORTING MAP CHANGES
---------------------

1. Close World Builder.
2. Stop the target private server and client.
3. Run "Import Map Changes.sh" or "Import Map Changes.cmd".
4. Review the exact preview.
5. Type IMPORT only if the listed files are correct.

The script exports the latest saved terrain, scenery, and NPC data, validates
the target revision, backs up every destination, installs both server and
client terrain copies, verifies the result, and writes a receipt. It refuses
unknown, running, changed, or incompatible targets. There is no force option.

UNDOING THE LAST IMPORT
-----------------------

Keep the target private server stopped, then run "Undo Last Map Import.sh" or
"Undo Last Map Import.cmd". Review the preview and type UNDO. Undo refuses if
the installed files were changed after import.

Backups, transaction receipts, exports, logs, and the editable project remain
inside workspace/. Preserve that folder if you move or update World Builder.

REQUIREMENTS AND LIMITS
-----------------------

- Use the package matching your operating system: Linux x64 or Windows x64.
  Both packages include their own Java runtime; users do not install Java,
  Git, Ant, or the project source code.
- macOS is not included in the first public release.
- The first release supports the current Spoiled Milk repository/private-server
  layout using server/myworld.conf, Custom_Landscape.orsc, and MyWorld scenery
  and NPC overlays. Similar-looking OpenRSC forks are not assumed compatible.
- The default local Builder port is 43615. Before the first launch, advanced
  users may set WORLD_BUILDER_PORT to another free port from 1 through 65534.
- Boundary-object authoring and automatic project rebasing are not included.
- Import and undo require the target private server to be offline.

Release source commit: @SOURCE_COMMIT@
