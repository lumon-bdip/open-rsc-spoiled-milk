#!/usr/bin/env python3
import sqlite3
import sys
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQLITE_DIR = ROOT / "server/inc/sqlite"
PLAYER_DBS = [
    SQLITE_DIR / "spoiled_milk_alpha.db",
    SQLITE_DIR / "myworld_dev.db",
]
UNOBTAINIUM_IDS = (1544, 1545)
ITEM_DEF_PATHS = [
    ROOT / "server/conf/server/defs/ItemDefs.json",
    ROOT / "server/conf/server/defs/ItemDefsCustom.json",
    ROOT / "server/conf/server/defs/ItemDefsMyWorld.json",
]


def load_item_ids() -> set[int]:
    item_ids: set[int] = set()
    for path in ITEM_DEF_PATHS:
        payload = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(payload, dict) and isinstance(payload.get("item"), list):
            entries = payload["item"]
        elif isinstance(payload, dict) and isinstance(payload.get("items"), list):
            entries = payload["items"]
        else:
            raise ValueError(f"Unknown item definition shape: {path}")
        for entry in entries:
            item_ids.add(int(entry["id"]))
    return item_ids


def fetch_rows(connection: sqlite3.Connection, table: str) -> list[sqlite3.Row]:
    slot_expr = "c.slot" if table != "equipped" else "-1"
    cursor = connection.execute(
        f"""
        SELECT
            '{table}' AS container,
            p.username,
            {slot_expr} AS slot,
            c.itemID AS status_id,
            s.catalogID AS catalog_id,
            s.amount,
            s.noted
        FROM {table} c
        JOIN itemstatuses s ON c.itemID = s.itemID
        JOIN players p ON c.playerID = p.id
        WHERE s.catalogID IN ({','.join('?' for _ in UNOBTAINIUM_IDS)})
        ORDER BY p.username, c.slot, s.catalogID
        """,
        UNOBTAINIUM_IDS,
    )
    return list(cursor)


def fetch_missing_definition_rows(
    connection: sqlite3.Connection,
    table: str,
    valid_item_ids: set[int],
) -> list[sqlite3.Row]:
    slot_expr = "c.slot" if table != "equipped" else "-1"
    cursor = connection.execute(
        f"""
        SELECT
            '{table}' AS container,
            p.username,
            {slot_expr} AS slot,
            c.itemID AS status_id,
            s.catalogID AS catalog_id,
            s.amount,
            s.noted
        FROM {table} c
        JOIN itemstatuses s ON c.itemID = s.itemID
        JOIN players p ON c.playerID = p.id
        ORDER BY p.username, c.slot, s.catalogID
        """
    )
    return [row for row in cursor if int(row["catalog_id"]) not in valid_item_ids]


def main() -> int:
    failures: list[str] = []
    valid_item_ids = load_item_ids()

    for db_path in PLAYER_DBS:
        if not db_path.exists():
            continue

        connection = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
        connection.row_factory = sqlite3.Row
        try:
            for table in ("bank", "invitems"):
                for row in fetch_rows(connection, table):
                    failures.append(
                        f"{db_path.relative_to(ROOT)}: {row['username']} has saved {row['catalog_id']} "
                        f"in {row['container']} slot {row['slot']} "
                        f"(status {row['status_id']}, amount {row['amount']}, noted {row['noted']})"
                    )

                for row in fetch_missing_definition_rows(connection, table, valid_item_ids):
                    failures.append(
                        f"{db_path.relative_to(ROOT)}: {row['username']} has undefined catalog ID {row['catalog_id']} "
                        f"in {row['container']} slot {row['slot']} "
                        f"(status {row['status_id']}, amount {row['amount']}, noted {row['noted']})"
                    )

            try:
                for row in fetch_rows(connection, "equipped"):
                    failures.append(
                        f"{db_path.relative_to(ROOT)}: {row['username']} has saved {row['catalog_id']} "
                        f"in equipped slot {row['slot']} "
                        f"(status {row['status_id']}, amount {row['amount']}, noted {row['noted']})"
                    )
                for row in fetch_missing_definition_rows(connection, "equipped", valid_item_ids):
                    failures.append(
                        f"{db_path.relative_to(ROOT)}: {row['username']} has undefined catalog ID {row['catalog_id']} "
                        f"in equipped slot {row['slot']} "
                        f"(status {row['status_id']}, amount {row['amount']}, noted {row['noted']})"
                    )
            except sqlite3.OperationalError:
                pass
        finally:
            connection.close()

    if failures:
        print("FAIL: invalid player-owned item data found")
        for failure in failures:
            print(f"  {failure}")
        return 1

    print("OK: no placeholder or undefined item IDs found in player-owned item containers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
