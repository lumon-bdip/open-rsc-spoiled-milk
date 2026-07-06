#!/usr/bin/env python3
import json
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "report-bot-suspicion.py"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def plugin_line(second: int, tick: int, player: str = "Clicker") -> str:
    return (
        f"2026-07-05 12:00:{second:02d} [Spoiled Milk : PluginThread-1] INFO  PluginHandler: - "
        f"Tick {tick} : Mining.onOpLoc : [[Player:1:{player} @ (10, 10)], "
        "0:id = 321; dir = 0; location = (11, 10);, mine]\n"
    )


def run_report(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )


with tempfile.TemporaryDirectory(prefix="bot-report-test-") as tmp_dir_str:
    tmp_dir = Path(tmp_dir_str)
    log_path = tmp_dir / "server.log"
    db_path = tmp_dir / "activity.db"

    lines = [plugin_line(i, 1000 + (i * 5)) for i in range(35)]
    lines.append(
        "2026-07-05 12:02:00 [Spoiled Milk : PluginThread-1] INFO  PluginHandler: - "
        "Tick 1200 : Default.onTimedEvent : [[Player:1:Clicker @ (10, 10)]]\n"
    )
    lines.append(
        "2026-07-05 12:03:00 [Spoiled Milk : PluginThread-1] INFO  PluginHandler: - "
        "Tick 1300 : Bankers.onOpNpc : [[Player:2:Human @ (12, 12)], [NPC:856:Banker @ (13, 13)], bank]\n"
    )
    log_path.write_text("".join(lines), encoding="utf-8")

    connection = sqlite3.connect(db_path)
    try:
        connection.execute("CREATE TABLE generic_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, message text NOT NULL, time int NOT NULL)")
        connection.executemany(
            "INSERT INTO generic_logs(message, time) VALUES (?, ?)",
            [
                ("Clicker picked up Bones x1 at (11, 10)", 1783252850 + i)
                for i in range(3)
            ],
        )
        connection.commit()
    finally:
        connection.close()

    result = run_report(str(log_path), "--db", str(db_path), "--json", "--min-score", "0")
    if result.returncode != 0:
        fail(result.stderr or result.stdout)

    payload = json.loads(result.stdout)
    if payload["eventsAnalyzed"] != 39:
        fail(f"expected 39 events, got {payload['eventsAnalyzed']}")

    reports = {report["player"]: report for report in payload["reports"]}
    clicker = reports.get("Clicker")
    if not clicker:
        fail("Clicker missing from report")
    if clicker["score"] < 45:
        fail(f"expected Clicker to score as suspicious, got {clicker['score']}")
    if clicker["top_action"]["count"] != 35:
        fail(f"expected 35 repeated mining actions, got {clicker['top_action']['count']}")
    if not any("matching actions" in reason for reason in clicker["reasons"]):
        fail(f"Clicker reasons missing repetition explanation: {clicker['reasons']}")

    human = reports.get("Human")
    if not human or human["score"] != 0:
        fail(f"expected Human to stay at score 0, got {human}")
