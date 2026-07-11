#!/usr/bin/env python3
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "server/src/com/openrsc/server/diagnostics/MovementStutterDiagnostics.java"
SERVER = ROOT / "server/src/com/openrsc/server/Server.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def run(command: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, text=True, capture_output=True)


FIXTURE = r"""
package com.openrsc.server.diagnostics;

import java.util.List;

public final class MovementStutterDiagnosticsFixture {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        MovementStutterDiagnostics disabled = new MovementStutterDiagnostics(
            false, 10L, 100L, 5L, 5L, 50L);
        disabled.recordMovementPoll(1L, 1000L, 9, 8, 7, 6, 5);
        disabled.recordWorldTick(1L, 2L, 1000L, null);
        require(disabled.drainIfDue(10000L).isEmpty(), "disabled diagnostics emitted output");

        MovementStutterDiagnostics.BoundedHistogram histogram =
            new MovementStutterDiagnostics.BoundedHistogram(
                new long[] { 10L, 20L, Long.MAX_VALUE });
        histogram.record(1L);
        histogram.record(10L);
        histogram.record(11L);
        histogram.record(20L);
        histogram.record(21L);
        require(histogram.count() == 5L, "histogram count");
        require(histogram.percentileUpperBound(0.50D) == 20L, "histogram p50");
        require(histogram.percentileUpperBound(0.95D) == 21L, "histogram overflow p95");
        histogram.reset();
        require(histogram.count() == 0L, "histogram reset");

        MovementStutterDiagnostics diagnostics = new MovementStutterDiagnostics(
            true, 10L, 100L, 5L, 5L, 50L);
        for (int i = 0; i < 25; i++) {
            long start = 1L + i * 20L;
            diagnostics.recordMovementPoll(start, start + 7L, 2, 3, 4, 2, 1);
        }
        diagnostics.recordWorldTick(
            90L,
            77L,
            80L,
            new MovementStutterDiagnostics.TickStages(1L, 2L, 3L, 4L, 5L, 6L, 7L));
        List<String> lines = diagnostics.drainIfDue(600L);
        require(lines.size() == 17, "bounded summary plus outlier storage: " + lines.size());
        String summary = lines.get(0);
        require(summary.contains("polls=25"), "poll accounting");
        require(summary.contains("movedPlayers=50"), "moved player accounting");
        require(summary.contains("movedNpcs=75"), "moved NPC accounting");
        require(summary.contains("queuedPacketsTotal=100"), "queue accounting");
        require(summary.contains("backpressuredPlayerSamples=25"), "backpressure accounting");
        require(summary.contains("outliers=26"), "outlier accounting");
        require(summary.contains("outliersRetained=16"), "outlier bound");
        require(summary.contains("outliersDropped=10"), "outlier drop accounting");
        require(lines.get(1).contains("kind=movement-poll"), "poll outlier detail");
        require(diagnostics.drainIfDue(650L).isEmpty(), "summary interval reset");

        MovementStutterDiagnostics tickOnly = new MovementStutterDiagnostics(
            true, 10L, 100L, 5L, 5L, 50L);
        tickOnly.recordWorldTick(
            1L,
            123L,
            80L,
            new MovementStutterDiagnostics.TickStages(11L, 12L, 13L, 14L, 15L, 16L, 17L));
        List<String> tickLines = tickOnly.drainIfDue(200L);
        require(tickLines.size() == 2, "tick outlier retained");
        require(tickLines.get(1).contains("tick=123"), "tick sequence retained");
        require(tickLines.get(1).contains("updateClientsMs=0"), "tick stage retained");

        System.out.println("PASS: deterministic movement stutter diagnostics");
    }
}
"""


def main() -> None:
    server_text = SERVER.read_text(encoding="utf-8")
    source_text = SOURCE.read_text(encoding="utf-8")
    if "movementDiagnosticsEnabled ? System.nanoTime() : 0L" not in server_text:
        fail("disabled movement-poll path does not gate the monotonic clock read")
    if "if (movementStutterDiagnostics.isEnabled())" not in server_text:
        fail("world-tick diagnostics are not gated")
    if "pollInterval = enabled ? new BoundedHistogram" not in source_text:
        fail("disabled server diagnostics still allocate histogram storage")

    with tempfile.TemporaryDirectory(prefix="movement-stutter-test-") as raw_tmp:
        tmp = Path(raw_tmp)
        fixture_path = tmp / "com/openrsc/server/diagnostics/MovementStutterDiagnosticsFixture.java"
        fixture_path.parent.mkdir(parents=True)
        fixture_path.write_text(FIXTURE, encoding="utf-8")
        classes = tmp / "classes"
        classes.mkdir()
        compile_result = run(
            ["javac", "-d", str(classes), str(SOURCE), str(fixture_path)], ROOT
        )
        if compile_result.returncode != 0:
            fail("fixture compile failed:\n" + compile_result.stdout + compile_result.stderr)
        fixture_result = run(
            ["java", "-cp", str(classes), "com.openrsc.server.diagnostics.MovementStutterDiagnosticsFixture"],
            ROOT,
        )
        if fixture_result.returncode != 0:
            fail("fixture failed:\n" + fixture_result.stdout + fixture_result.stderr)
        print(fixture_result.stdout.strip())


if __name__ == "__main__":
    main()
