#!/usr/bin/env python3
import json
import subprocess
import sys
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
]


def load_max_server_item_id() -> int:
    item_ids: set[int] = set()
    for path in SERVER_ITEM_PATHS:
        payload = json.loads(path.read_text(encoding="utf-8"))
        entries = payload.get("item") or payload.get("items")
        if not isinstance(entries, list):
            raise ValueError(f"Unknown item definition shape: {path}")
        item_ids.update(int(entry["id"]) for entry in entries)

    max_item_id = max(item_ids)
    missing_ids = sorted(set(range(max_item_id + 1)) - item_ids)
    if missing_ids:
        raise AssertionError(
            f"Server item catalog is not contiguous through {max_item_id}: {missing_ids[:20]}"
        )
    return max_item_id


def main() -> int:
    max_item_id = load_max_server_item_id()
    client_source = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    if "items.add(new ItemDef" in client_source:
        print("FAIL: client item definitions must use ID-addressed insertion", file=sys.stderr)
        return 1

    subprocess.run([str(ROOT / "scripts/build-client.sh")], cwd=ROOT, check=True)
    result = subprocess.run(
        [
            "java",
            "-cp",
            str(CLIENT_JAR),
            "com.openrsc.client.tools.ItemDefinitionAudit",
            str(max_item_id),
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
