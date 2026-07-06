#!/usr/bin/env python3
"""Build an offline bot-suspicion report from Spoiled Milk server logs."""

from __future__ import annotations

import argparse
import gzip
import json
import math
import re
import sqlite3
import statistics
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Iterable
from urllib.parse import quote


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DB = ROOT / "server" / "inc" / "sqlite" / "spoiled_milk_alpha.db"
DEFAULT_LOG = ROOT / "server" / "logs" / "spoiled_milk_98.log"
DEFAULT_ROTATED_LOG_GLOB = "spoiled_milk_98.*.log.gz"

PLUGIN_RE = re.compile(
    r"^(?P<date>\d{4}-\d\d-\d\d \d\d:\d\d:\d\d) "
    r".*?PluginHandler: - Tick (?P<tick>\d+) : "
    r"(?P<action>[A-Za-z0-9_$]+\.[A-Za-z0-9_$]+) : \[(?P<payload>.*)\]$"
)
PLAYER_RE = re.compile(r"\[Player:(?P<id>-?\d+):(?P<name>.+?) @ \((?P<x>-?\d+), (?P<y>-?\d+)\)\]")
NPC_RE = re.compile(r"\[NPC:(?P<id>-?\d+):(?P<name>.+?) @ \((?P<x>-?\d+), (?P<y>-?\d+)\)\]")
LOC_RE = re.compile(r"(?P<index>\d+):id = (?P<id>-?\d+); dir = (?P<dir>-?\d+); location = \((?P<x>-?\d+), (?P<y>-?\d+)\);")
ITEM_RE = re.compile(
    r"Item\((?P<id>-?\d+), (?P<amount>-?\d+)(?:, (?P<noted>true|false))?\)"
    r"(?: location = \((?P<x>-?\d+), (?P<y>-?\d+)\))?"
)
GENERIC_LOG_PATTERNS = [
    (
        re.compile(r"^(?P<player>.+?) picked up (?P<item>.+?) x(?P<amount>\d+) at \((?P<x>-?\d+), (?P<y>-?\d+)\)$"),
        "picked_up",
    ),
    (
        re.compile(r"^(?P<player>.+?) bought (?P<item>.+?) x(?P<amount>\d+) for (?P<coins>\d+)gp at \((?P<x>-?\d+), (?P<y>-?\d+)\)$"),
        "bought",
    ),
    (
        re.compile(r"^(?P<player>.+?) sold (?P<item>.+?) x(?P<amount>\d+) for (?P<coins>\d+)gp at \((?P<x>-?\d+), (?P<y>-?\d+)\)$"),
        "sold",
    ),
    (
        re.compile(r"^(?P<player>.+?) dropped (?P<item>.+?) x(?P<amount>\d+) at \((?P<x>-?\d+), (?P<y>-?\d+)\)$"),
        "dropped",
    ),
    (
        re.compile(r"^(?P<player>.+?) telegrabbed (?P<item>.+?) x(?P<amount>\d+) at \((?P<x>-?\d+), (?P<y>-?\d+)\)$"),
        "telegrabbed",
    ),
    (
        re.compile(r"^(?P<player>.+?) guessed !_.*_! for filename:: (?P<filename>.+)$"),
        "sleep_guess",
    ),
    (
        re.compile(r"^(?P<player>.+?) has failed sleeping captcha (?P<count>\d+) times!$"),
        "sleep_failed",
    ),
]

IGNORED_PLUGIN_HOOKS = {
    "onCatGrowth",
    "onCommand",
    "onKillNpc",
    "onPlayerLogin",
    "onPlayerLogout",
    "onStartup",
    "onTimedEvent",
    "onWineFerment",
}


@dataclass(frozen=True)
class Event:
    source: str
    timestamp: int
    tick: int | None
    player: str
    action: str
    hook: str
    player_pos: tuple[int, int] | None
    target_type: str
    target_id: str
    target_name: str
    target_pos: tuple[int, int] | None
    command: str
    detail: str

    @property
    def signature(self) -> str:
        target_pos = format_pos(self.target_pos)
        return "|".join(
            [
                self.action,
                self.target_type,
                self.target_id,
                self.target_name,
                target_pos,
                self.command,
            ]
        )

    @property
    def short_signature(self) -> str:
        target = self.target_name or self.target_id or self.target_type
        target_pos = format_pos(self.target_pos)
        pieces = [self.action]
        if self.command:
            pieces.append(self.command)
        if target:
            pieces.append(target)
        if target_pos:
            pieces.append("@" + target_pos)
        return " ".join(pieces)

    @property
    def sequence_signature(self) -> str:
        return "|".join([self.action, self.command, self.target_type, self.target_id, format_pos(self.player_pos), format_pos(self.target_pos)])


def format_pos(pos: tuple[int, int] | None) -> str:
    if pos is None:
        return ""
    return f"{pos[0]},{pos[1]}"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Scan Spoiled Milk server logs and SQLite generic logs for repeated, "
            "low-variance action patterns worth admin review."
        )
    )
    parser.add_argument(
        "logs",
        nargs="*",
        type=Path,
        help="Log file(s) or directories to scan. Defaults to server/logs/spoiled_milk_98.log and rotated spoiled_milk_98 logs.",
    )
    parser.add_argument("--db", type=Path, default=DEFAULT_DB, help="SQLite DB to read generic_logs from. Default: server/inc/sqlite/spoiled_milk_alpha.db")
    parser.add_argument("--no-db", action="store_true", help="Do not read SQLite generic_logs.")
    parser.add_argument("--hours", type=float, help="Only include activity from the last N hours.")
    parser.add_argument("--since", help="Only include activity after an epoch second or ISO/local datetime, for example '2026-07-05 12:00:00'.")
    parser.add_argument("--player", action="append", default=[], help="Limit report to a player. Can be repeated.")
    parser.add_argument("--top", type=int, default=20, help="Number of ranked players to print.")
    parser.add_argument("--min-score", type=float, default=15.0, help="Hide players below this suspicion score.")
    parser.add_argument("--min-repeated-actions", type=int, default=20, help="Minimum same-signature count before repetition affects score.")
    parser.add_argument("--include-background", action="store_true", help="Include plugin background hooks such as timed events and cat growth.")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON instead of text.")
    return parser.parse_args()


def parse_since(args: argparse.Namespace) -> int | None:
    since_values: list[int] = []
    if args.hours is not None:
        since_values.append(int((datetime.now() - timedelta(hours=args.hours)).timestamp()))
    if args.since:
        since_values.append(parse_datetime_or_epoch(args.since))
    if not since_values:
        return None
    return max(since_values)


def parse_datetime_or_epoch(value: str) -> int:
    stripped = value.strip()
    if stripped.isdigit():
        return int(stripped)
    try:
        return int(datetime.fromisoformat(stripped.replace("Z", "+00:00")).timestamp())
    except ValueError as exc:
        raise SystemExit(f"Could not parse --since value {value!r}. Use epoch seconds or ISO datetime.") from exc


def default_log_paths() -> list[Path]:
    paths: list[Path] = []
    if DEFAULT_LOG.exists():
        paths.append(DEFAULT_LOG)
    log_dir = ROOT / "server" / "logs"
    if log_dir.exists():
        paths.extend(sorted(log_dir.glob(DEFAULT_ROTATED_LOG_GLOB)))
    if paths:
        return paths
    return expand_log_paths([log_dir])


def expand_log_paths(paths: Iterable[Path]) -> list[Path]:
    expanded: list[Path] = []
    for path in paths:
        if path.is_dir():
            expanded.extend(sorted(p for p in path.iterdir() if p.name.endswith(".log") or p.name.endswith(".log.gz")))
        elif path.exists():
            expanded.append(path)
    return dedupe_paths(expanded)


def dedupe_paths(paths: Iterable[Path]) -> list[Path]:
    seen: set[Path] = set()
    result: list[Path] = []
    for path in paths:
        resolved = path.resolve()
        if resolved not in seen:
            result.append(path)
            seen.add(resolved)
    return result


def open_text(path: Path):
    if path.suffix == ".gz":
        return gzip.open(path, "rt", encoding="utf-8", errors="replace")
    return path.open("r", encoding="utf-8", errors="replace")


def parse_plugin_logs(paths: Iterable[Path], since: int | None, include_background: bool) -> tuple[list[Event], list[str]]:
    events: list[Event] = []
    warnings: list[str] = []
    for path in paths:
        try:
            with open_text(path) as handle:
                for line in handle:
                    event = parse_plugin_line(line.rstrip("\n"), path, include_background)
                    if event is None:
                        continue
                    if since is not None and event.timestamp < since:
                        continue
                    events.append(event)
        except OSError as exc:
            warnings.append(f"Could not read {path}: {exc}")
    return events, warnings


def parse_plugin_line(line: str, path: Path, include_background: bool) -> Event | None:
    match = PLUGIN_RE.match(line)
    if not match:
        return None

    action = match.group("action")
    hook = action.rsplit(".", 1)[-1]
    if not include_background and hook in IGNORED_PLUGIN_HOOKS:
        return None

    payload = match.group("payload")
    player_match = PLAYER_RE.search(payload)
    if player_match is None:
        return None

    player = player_match.group("name")
    player_pos = (int(player_match.group("x")), int(player_match.group("y")))
    remainder = payload[player_match.end() :]

    target_type = "unknown"
    target_id = ""
    target_name = ""
    target_pos: tuple[int, int] | None = None
    command = ""

    npc_match = NPC_RE.search(remainder)
    loc_match = LOC_RE.search(remainder)
    item_match = ITEM_RE.search(remainder)
    players = list(PLAYER_RE.finditer(payload))
    target_player_match = players[1] if len(players) > 1 else None

    if npc_match:
        target_type = "npc"
        target_id = npc_match.group("id")
        target_name = npc_match.group("name")
        target_pos = (int(npc_match.group("x")), int(npc_match.group("y")))
        command = trailing_command(remainder[npc_match.end() :])
    elif loc_match:
        target_type = "object"
        target_id = loc_match.group("id")
        target_name = "object"
        target_pos = (int(loc_match.group("x")), int(loc_match.group("y")))
        command = trailing_command(remainder[loc_match.end() :])
    elif item_match:
        target_type = "item"
        target_id = item_match.group("id")
        target_name = "item"
        if item_match.group("x") and item_match.group("y"):
            target_pos = (int(item_match.group("x")), int(item_match.group("y")))
        command = trailing_command(remainder[item_match.end() :])
    elif target_player_match:
        target_type = "player"
        target_id = target_player_match.group("id")
        target_name = target_player_match.group("name")
        target_pos = (int(target_player_match.group("x")), int(target_player_match.group("y")))
        command = trailing_command(payload[target_player_match.end() :])
    else:
        command = trailing_command(remainder)

    timestamp = int(datetime.strptime(match.group("date"), "%Y-%m-%d %H:%M:%S").timestamp())
    return Event(
        source=str(path),
        timestamp=timestamp,
        tick=int(match.group("tick")),
        player=player,
        action=action,
        hook=hook,
        player_pos=player_pos,
        target_type=target_type,
        target_id=target_id,
        target_name=target_name,
        target_pos=target_pos,
        command=command,
        detail=line,
    )


def trailing_command(text: str) -> str:
    cleaned = text.strip()
    if cleaned.startswith(","):
        cleaned = cleaned[1:].strip()
    cleaned = cleaned.strip("[] ")
    if not cleaned:
        return ""
    if "," in cleaned:
        cleaned = cleaned.rsplit(",", 1)[-1].strip()
    return cleaned.strip("[] ")


def parse_sqlite_generic_logs(db_path: Path, since: int | None) -> tuple[list[Event], list[str]]:
    if not db_path.exists():
        return [], [f"SQLite DB not found: {db_path}"]
    uri = "file:" + quote(str(db_path.resolve())) + "?mode=ro"
    events: list[Event] = []
    warnings: list[str] = []
    try:
        connection = sqlite3.connect(uri, uri=True)
    except sqlite3.Error as exc:
        return [], [f"Could not open SQLite DB {db_path}: {exc}"]

    try:
        connection.execute("PRAGMA query_only=ON")
        query = "SELECT time, message FROM generic_logs"
        params: tuple[int, ...] = ()
        if since is not None:
            query += " WHERE time >= ?"
            params = (since,)
        query += " ORDER BY time"
        for timestamp, message in connection.execute(query, params):
            event = parse_generic_log_row(int(timestamp), str(message), db_path)
            if event is not None:
                events.append(event)
    except sqlite3.Error as exc:
        warnings.append(f"Could not read generic_logs from {db_path}: {exc}")
    finally:
        connection.close()
    return events, warnings


def parse_generic_log_row(timestamp: int, message: str, db_path: Path) -> Event | None:
    for pattern, verb in GENERIC_LOG_PATTERNS:
        match = pattern.match(message)
        if not match:
            continue
        groups = match.groupdict()
        pos = None
        if groups.get("x") and groups.get("y"):
            pos = (int(groups["x"]), int(groups["y"]))
        item = groups.get("item") or groups.get("filename") or verb
        target_id = groups.get("filename") or item
        amount = groups.get("amount") or groups.get("count") or ""
        command = verb
        if amount:
            command += f" x{amount}"
        return Event(
            source=str(db_path),
            timestamp=timestamp,
            tick=None,
            player=groups["player"],
            action=f"Generic.{verb}",
            hook=verb,
            player_pos=pos,
            target_type="generic",
            target_id=target_id,
            target_name=item,
            target_pos=pos,
            command=command,
            detail=message,
        )
    return None


def analyze(events: list[Event], min_repeated_actions: int) -> list[dict[str, object]]:
    by_player: dict[str, list[Event]] = defaultdict(list)
    for event in events:
        by_player[event.player].append(event)

    reports = [analyze_player(player, player_events, min_repeated_actions) for player, player_events in by_player.items()]
    reports.sort(key=lambda report: (-float(report["score"]), -int(report["total_events"]), str(report["player"]).lower()))
    return reports


def analyze_player(player: str, events: list[Event], min_repeated_actions: int) -> dict[str, object]:
    ordered = sorted(events, key=lambda event: (event.timestamp, event.tick if event.tick is not None else -1))
    total = len(ordered)
    signatures = Counter(event.signature for event in ordered)
    top_signature, top_count = signatures.most_common(1)[0]
    top_event = next(event for event in ordered if event.signature == top_signature)
    longest_run_signature, longest_run_count = longest_run(ordered)
    longest_run_event = next(event for event in ordered if event.signature == longest_run_signature)
    timing = best_timing_signal(ordered, min_repeated_actions)
    loop = best_loop_signal(ordered)
    first_ts = ordered[0].timestamp
    last_ts = ordered[-1].timestamp
    observed_seconds = max(0, last_ts - first_ts)
    active_seconds = active_duration(ordered)
    unique_signatures = len(signatures)
    unique_ratio = unique_signatures / total if total else 1.0

    score = 0.0
    reasons: list[str] = []

    if top_count >= min_repeated_actions:
        top_pct = top_count / total
        points = min(35.0, 8.0 + ((top_count - min_repeated_actions) * 0.12) + (top_pct * 20.0))
        score += points
        reasons.append(f"{top_count} matching actions: {top_event.short_signature}")

    if longest_run_count >= max(8, min_repeated_actions // 2):
        points = min(25.0, 6.0 + ((longest_run_count - 8) * 0.8))
        score += points
        reasons.append(f"{longest_run_count} consecutive same-signature actions: {longest_run_event.short_signature}")

    if timing:
        timing_points = timing_score(timing)
        if timing_points > 0:
            score += timing_points
            reasons.append(
                "regular timing on "
                + timing["event"].short_signature
                + f": {timing['count']} actions, median gap {timing['median_gap']:.2f}s, cv {timing['cv']:.3f}"
            )

    if total >= 50 and unique_ratio <= 0.20:
        points = 15.0 if unique_ratio <= 0.10 else 8.0
        score += points
        reasons.append(f"low action variety: {unique_signatures} unique signatures across {total} actions")

    if loop and loop["count"] >= 8:
        points = min(15.0, 4.0 + ((int(loop["count"]) - 8) * 1.5))
        score += points
        reasons.append(f"repeated {loop['length']}-step action loop {loop['count']} times")

    if active_seconds >= 3600 and total >= 120:
        events_per_hour = total / (active_seconds / 3600)
        if events_per_hour >= 60:
            points = min(12.0, events_per_hour / 20.0)
            score += points
            reasons.append(f"heavy tracked activity: {total} actions over about {format_duration(active_seconds)} active time")

    score = min(100.0, round(score, 1))
    return {
        "player": player,
        "score": score,
        "level": score_level(score),
        "total_events": total,
        "active_time": format_duration(active_seconds),
        "observed_range": format_duration(observed_seconds),
        "first_seen": format_time(first_ts),
        "last_seen": format_time(last_ts),
        "unique_signatures": unique_signatures,
        "top_action": {
            "count": top_count,
            "percent": round((top_count / total) * 100, 1),
            "signature": top_event.short_signature,
        },
        "longest_run": {
            "count": longest_run_count,
            "signature": longest_run_event.short_signature,
        },
        "timing": serialize_timing(timing),
        "loop": loop,
        "reasons": reasons,
    }


def longest_run(events: list[Event]) -> tuple[str, int]:
    best_signature = events[0].signature
    best_count = 1
    current_signature = events[0].signature
    current_count = 1
    for event in events[1:]:
        if event.signature == current_signature:
            current_count += 1
        else:
            if current_count > best_count:
                best_signature = current_signature
                best_count = current_count
            current_signature = event.signature
            current_count = 1
    if current_count > best_count:
        best_signature = current_signature
        best_count = current_count
    return best_signature, best_count


def best_timing_signal(events: list[Event], min_repeated_actions: int) -> dict[str, object] | None:
    by_signature: dict[str, list[Event]] = defaultdict(list)
    for event in events:
        by_signature[event.signature].append(event)

    best: dict[str, object] | None = None
    for signature_events in by_signature.values():
        if len(signature_events) < min_repeated_actions:
            continue
        ordered = sorted(signature_events, key=lambda event: (event.timestamp, event.tick if event.tick is not None else -1))
        gaps = time_gaps(ordered)
        if len(gaps) < min_repeated_actions - 1:
            continue
        mean_gap = statistics.fmean(gaps)
        if mean_gap <= 0:
            continue
        stdev = statistics.pstdev(gaps) if len(gaps) > 1 else 0.0
        cv = stdev / mean_gap
        rounded_gaps = Counter(round(gap, 1) for gap in gaps)
        same_gap_pct = rounded_gaps.most_common(1)[0][1] / len(gaps)
        candidate = {
            "count": len(ordered),
            "event": ordered[0],
            "mean_gap": mean_gap,
            "median_gap": statistics.median(gaps),
            "stdev": stdev,
            "cv": cv,
            "same_gap_pct": same_gap_pct,
        }
        if best is None or timing_score(candidate) > timing_score(best):
            best = candidate
    return best


def time_gaps(events: list[Event]) -> list[float]:
    gaps: list[float] = []
    for previous, current in zip(events, events[1:]):
        if previous.tick is not None and current.tick is not None and current.tick > previous.tick:
            tick_gap = current.tick - previous.tick
            if tick_gap <= 188:
                gaps.append(tick_gap * 0.64)
                continue
        seconds_gap = current.timestamp - previous.timestamp
        if 0 < seconds_gap <= 120:
            gaps.append(float(seconds_gap))
    return gaps


def timing_score(timing: dict[str, object]) -> float:
    count = int(timing["count"])
    cv = float(timing["cv"])
    same_gap_pct = float(timing["same_gap_pct"])
    points = 0.0
    if count >= 30 and cv <= 0.12:
        points += min(24.0, (0.12 - cv) / 0.12 * 24.0)
    if count >= 30 and cv <= 0.50 and same_gap_pct >= 0.70:
        points += min(8.0, (same_gap_pct - 0.70) / 0.30 * 8.0)
    if count >= 100 and cv <= 0.18:
        points += min(8.0, count / 50.0)
    return points


def active_duration(events: list[Event], max_gap_seconds: int = 600) -> int:
    total = 0
    for previous, current in zip(events, events[1:]):
        gap = current.timestamp - previous.timestamp
        if 0 < gap <= max_gap_seconds:
            total += gap
    return total


def serialize_timing(timing: dict[str, object] | None) -> dict[str, object] | None:
    if timing is None:
        return None
    return {
        "count": timing["count"],
        "signature": timing["event"].short_signature,
        "mean_gap_seconds": round(float(timing["mean_gap"]), 2),
        "median_gap_seconds": round(float(timing["median_gap"]), 2),
        "stdev_seconds": round(float(timing["stdev"]), 2),
        "coefficient_of_variation": round(float(timing["cv"]), 4),
        "same_gap_percent": round(float(timing["same_gap_pct"]) * 100, 1),
    }


def best_loop_signal(events: list[Event]) -> dict[str, object] | None:
    best: dict[str, object] | None = None
    sequence = [event.sequence_signature for event in events]
    for length in (4, 5, 6, 8):
        if len(sequence) < length * 2:
            continue
        windows = Counter(tuple(sequence[i : i + length]) for i in range(0, len(sequence) - length + 1))
        loop, count = windows.most_common(1)[0]
        if count < 2:
            continue
        candidate = {
            "length": length,
            "count": count,
            "sample": list(loop),
        }
        if best is None or int(candidate["count"]) > int(best["count"]):
            best = candidate
    return best


def score_level(score: float) -> str:
    if score >= 70:
        return "HIGH"
    if score >= 45:
        return "MEDIUM"
    if score >= 25:
        return "LOW"
    return "WATCH"


def format_duration(seconds: int) -> str:
    if seconds < 60:
        return f"{seconds}s"
    minutes, sec = divmod(seconds, 60)
    if minutes < 60:
        return f"{minutes}m {sec}s"
    hours, minutes = divmod(minutes, 60)
    return f"{hours}h {minutes}m"


def format_time(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


def print_text_report(
    reports: list[dict[str, object]],
    sources: list[Path],
    db_path: Path | None,
    event_count: int,
    warnings: list[str],
    min_score: float,
    top: int,
) -> None:
    print("Spoiled Milk Bot Suspicion Report")
    print("Use this as an admin review lead, not as proof by itself.")
    print()
    print(f"Events analyzed: {event_count}")
    print(f"Log sources: {len(sources)}")
    for path in sources[:8]:
        print(f"  - {path}")
    if len(sources) > 8:
        print(f"  - ... {len(sources) - 8} more")
    if db_path is not None:
        print(f"SQLite generic_logs: {db_path}")
    if warnings:
        print()
        print("Warnings:")
        for warning in warnings:
            print(f"  - {warning}")

    visible = [report for report in reports if float(report["score"]) >= min_score][:top]
    print()
    if not visible:
        print(f"No players met the minimum score of {min_score}.")
        return

    print("Ranked players:")
    for index, report in enumerate(visible, start=1):
        print()
        print(f"{index}. {report['player']} - {report['score']} ({report['level']})")
        print(
            f"   Actions: {report['total_events']} | Active time: about {report['active_time']} | "
            f"Observed range: {report['first_seen']} to {report['last_seen']}"
        )
        top_action = report["top_action"]
        assert isinstance(top_action, dict)
        print(f"   Top repeated action: {top_action['count']}x ({top_action['percent']}%) {top_action['signature']}")
        longest_run_report = report["longest_run"]
        assert isinstance(longest_run_report, dict)
        print(f"   Longest run: {longest_run_report['count']}x {longest_run_report['signature']}")
        reasons = report["reasons"]
        assert isinstance(reasons, list)
        for reason in reasons[:5]:
            print(f"   - {reason}")


def main() -> int:
    args = parse_args()
    since = parse_since(args)
    log_paths = expand_log_paths(args.logs) if args.logs else default_log_paths()

    events, warnings = parse_plugin_logs(log_paths, since, args.include_background)
    db_path = None
    if not args.no_db:
        db_path = args.db
        db_events, db_warnings = parse_sqlite_generic_logs(args.db, since)
        events.extend(db_events)
        warnings.extend(db_warnings)

    if args.player:
        wanted = {player.lower() for player in args.player}
        events = [event for event in events if event.player.lower() in wanted]

    reports = analyze(events, args.min_repeated_actions) if events else []
    if args.json:
        print(
            json.dumps(
                {
                    "eventsAnalyzed": len(events),
                    "logs": [str(path) for path in log_paths],
                    "db": str(db_path) if db_path is not None else None,
                    "warnings": warnings,
                    "reports": reports,
                },
                indent=2,
            )
        )
    else:
        print_text_report(reports, log_paths, db_path, len(events), warnings, args.min_score, args.top)

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BrokenPipeError:
        try:
            sys.stdout.close()
        finally:
            raise SystemExit(1)
