#!/usr/bin/env python3
import json
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_DIR = ROOT / "Client_Base"
CLIENT_JAR = CLIENT_DIR / "Open_RSC_Client.jar"
CLIENT_ENTITY_HANDLER = (
    ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
)
SERVER_ITEM_PATHS = [
    ROOT / "server/conf/server/defs/ItemDefs.json",
    ROOT / "server/conf/server/defs/ItemDefsCustom.json",
    ROOT / "server/conf/server/defs/ItemDefsMyWorld.json",
]

# Established client display aliases that differ from server data without changing item identity.
ALLOWED_NAME_MISMATCH_IDS = {
    152, 198, 313, 383, 434,
    *range(474, 501),
    *range(569, 572),
    807, 808,
    *range(963, 966),
    *range(1411, 1417),
    *range(1468, 1480),
    1480, 1488, 1543,
    1823, 1824,
    *range(1925, 1930),
    2165, 2166,
}


def load_server_items() -> dict[int, str]:
    items: dict[int, str] = {}
    for path in SERVER_ITEM_PATHS:
        payload = json.loads(path.read_text(encoding="utf-8"))
        entries = payload.get("item") or payload.get("items")
        if not isinstance(entries, list):
            raise ValueError(f"Unknown item definition shape: {path}")
        for entry in entries:
            item_id = int(entry["id"])
            if "name" in entry:
                items[item_id] = entry["name"]

    max_item_id = max(items)
    missing_ids = sorted(set(range(max_item_id + 1)) - set(items))
    if missing_ids:
        raise AssertionError(
            f"Server item catalog is not contiguous through {max_item_id}: {missing_ids[:20]}"
        )
    return items


def main() -> int:
    server_items = load_server_items()
    max_item_id = max(server_items)
    client_source = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    if "items.add(new ItemDef" in client_source:
        print("FAIL: client item definitions must use ID-addressed insertion", file=sys.stderr)
        return 1
    if '"Unknown item"' in client_source:
        print("FAIL: client catalog contains generic Unknown item definitions", file=sys.stderr)
        return 1

    subprocess.run([str(ROOT / "scripts/build-client.sh")], cwd=ROOT, check=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8") as catalog:
        for item_id, name in sorted(server_items.items()):
            expected_name = "" if item_id in ALLOWED_NAME_MISMATCH_IDS else name
            catalog.write(f"{item_id}\t{expected_name}\n")
        catalog.flush()
        result = subprocess.run(
            [
                "java",
                "-cp",
                str(CLIENT_JAR),
                "com.openrsc.client.tools.ItemDefinitionAudit",
                catalog.name,
            ],
            cwd=CLIENT_DIR,
            text=True,
        )
    if result.returncode != 0:
        return result.returncode

    print(
        f"PASS: runtime client catalog resolves every server item ID 0-{max_item_id} "
        "without unexpected Unobtanium placeholders"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
