#!/usr/bin/env python3

import re
import sqlite3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
QUERIES = ROOT / "server/src/com/openrsc/server/database/impl/mysql/MySqlQueries.java"
PLAYER_SERVICE = ROOT / "server/src/com/openrsc/server/service/PlayerService.java"
SETTING_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/GameSettingHandler.java"
SEED = ROOT / "server/inc/sqlite/myworld_seed.db"
SCHEMAS = (
    ROOT / "server/database/mysql/core.sql",
    ROOT / "server/database/mysql/retro.sql",
    ROOT / "server/database/sqlite/core.sqlite",
    ROOT / "server/database/sqlite/retro.sqlite",
)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    require(
        "private boolean optionCameraModeAuto = false;" in client,
        "client camera fallback should default to Manual",
    )
    require(
        "this.optionCameraModeAuto = !this.optionCameraModeAuto;" in client,
        "camera setting should remain toggleable",
    )
    require(
        "this.packetHandler.getClientStream().bufferBits.putByte(0);" in client,
        "camera setting should keep protocol index 0",
    )

    queries = QUERIES.read_text(encoding="utf-8")
    require(
        "`creation_ip`, `cameraauto`) VALUES (?, ?, ?, ?, ?, 0)" in queries,
        "account creation should explicitly write Manual camera mode",
    )
    require(
        re.search(r"UPDATE .*players.* SET .*cameraauto", queries, re.IGNORECASE) is None,
        "account creation change must not rewrite saved camera preferences",
    )

    player_service = PLAYER_SERVICE.read_text(encoding="utf-8")
    require(
        "setGameSetting(PlayerSettings.GAME_SETTING_AUTO_CAMERA, playerData.cameraAuto)"
        in player_service,
        "saved account camera preference should still load",
    )
    setting_handler = SETTING_HANDLER.read_text(encoding="utf-8")
    require(
        "setGameSetting(PlayerSettings.GAME_SETTING_AUTO_CAMERA, payload.cameraModeAuto == 1)"
        in setting_handler,
        "Auto and Manual camera choices should remain saveable",
    )

    default_pattern = re.compile(r"`cameraauto`\s+tinyint\(1\).*DEFAULT\s+0", re.IGNORECASE)
    for schema in SCHEMAS:
        text = schema.read_text(encoding="utf-8")
        require(default_pattern.search(text) is not None, f"{schema.name} should default cameraauto to 0")
        require(
            "UPDATE players SET cameraauto" not in text
            and "UPDATE `players` SET `cameraauto`" not in text,
            f"{schema.name} must not overwrite existing player preferences",
        )

    with sqlite3.connect(f"file:{SEED}?mode=ro", uri=True) as connection:
        player_schema = connection.execute(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='players'"
        ).fetchone()[0]
        camera_column = next(
            (row for row in connection.execute("PRAGMA table_info(players)") if row[1] == "cameraauto"),
            None,
        )
        require(camera_column is not None, "canonical seed is missing players.cameraauto")
        require(str(camera_column[4]).strip("'\"") == "0", "canonical seed should default cameraauto to 0")

    with sqlite3.connect(":memory:") as fixture:
        fixture.execute(player_schema)
        fixture.execute(
            "INSERT INTO players (username, email, pass, creation_date, creation_ip) VALUES (?, ?, ?, ?, ?)",
            ("manualtest", "", "fixture", 0, "127.0.0.1"),
        )
        camera_auto = fixture.execute(
            "SELECT cameraauto FROM players WHERE username = ?", ("manualtest",)
        ).fetchone()[0]
        require(camera_auto == 0, "a fresh canonical-seed account should materialize Manual camera mode")

    print("PASS: new accounts default to Manual camera without migrating saved preferences")


if __name__ == "__main__":
    main()
