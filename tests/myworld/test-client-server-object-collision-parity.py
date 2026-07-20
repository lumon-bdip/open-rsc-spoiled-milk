#!/usr/bin/env python3
"""Guard collision-critical client object definitions against server drift."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
CLIENT_DEFS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
SERVER_DEFS = ROOT / "server/conf/server/defs/GameObjectDef.xml"
COLLISION_CRITICAL_OBJECTS = {
    2: ("Well", "The bucket is missing"),
    21: ("Support", "A wooden pole"),
}


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def server_collision_values(object_id: int, expected_name: str) -> tuple[int, int, int]:
    definitions = ET.parse(SERVER_DEFS).getroot().findall("GameObjectDef")
    if object_id >= len(definitions):
        fail(f"Server object definition {object_id} is missing")
    definition = definitions[object_id]
    actual_name = definition.findtext("name", default="")
    if actual_name != expected_name:
        fail(f"Server object {object_id} expected {expected_name!r}, found {actual_name!r}")
    return tuple(
        int(definition.findtext(field, default="-1"))
        for field in ("type", "width", "height")
    )


def client_collision_values(name: str, description: str) -> tuple[int, int, int]:
    source = CLIENT_DEFS.read_text(encoding="utf-8")
    pattern = re.compile(
        r'new GameObjectDef\("' + re.escape(name)
        + r'",\s*"' + re.escape(description)
        + r'",\s*"[^"]*",\s*"[^"]*",\s*(-?\d+),\s*(\d+),\s*(\d+),'
    )
    matches = pattern.findall(source)
    if len(matches) != 1:
        fail(f"Expected one client definition for {name!r}/{description!r}, found {len(matches)}")
    return tuple(int(value) for value in matches[0])


def main() -> None:
    for object_id, (name, description) in COLLISION_CRITICAL_OBJECTS.items():
        server_values = server_collision_values(object_id, name)
        client_values = client_collision_values(name, description)
        if client_values != server_values:
            fail(
                f"Object {object_id} {name} collision mismatch: "
                f"client type/width/height={client_values}, server={server_values}"
            )

    print("PASS: collision-critical client object definitions match the server")


if __name__ == "__main__":
    main()
