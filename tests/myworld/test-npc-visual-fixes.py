#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_NPCS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
SERVER_NPCS = ROOT / "server/conf/server/defs/NpcDefs.json"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> int:
    client = CLIENT_NPCS.read_text(encoding="utf-8")
    server_entries = json.loads(SERVER_NPCS.read_text(encoding="utf-8"))["npcs"]
    server_by_name = {entry["name"].lower(): entry for entry in server_entries}

    for name in ("jinno", "watto"):
        if f'new NPCDef("{name if name == "jinno" else "Watto"}", "A small fairy-market resident"' not in client:
            fail(f"Client NPC definition missing updated {name} description")
        npc = server_by_name.get(name)
        if npc is None:
            fail(f"Server NPC definition missing {name}")
        if npc.get("sprites1") != 0 or npc.get("sprites2") != 1 or npc.get("sprites3") != 2:
            fail(f"{name} should have head, torso, and legs sprites 0/1/2")
        if npc.get("description") != "A small fairy-market resident":
            fail(f"{name} description should no longer reference missing legs")
        if npc.get("camera1") != 94 or npc.get("camera2") != 143:
            fail(f"{name} should use the fairy-dimension small render size")

    if client.count("sprites = new int[]{0, 1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1};") < 2:
        fail("Client Jinno/Watto definitions should use the standard small humanoid leg sprite")

    print("PASS: NPC visual fixes validated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
