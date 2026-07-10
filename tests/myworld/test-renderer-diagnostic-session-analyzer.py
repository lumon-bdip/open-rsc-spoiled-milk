#!/usr/bin/env python3
import json
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ANALYZER = ROOT / "scripts/analyze-renderer-session.py"


def record(record_type: str, **values) -> dict:
    result = {
        "schema": "renderer-diagnostics",
        "schemaVersion": 1,
        "recordType": record_type,
        "sessionId": "session-fixture",
        "timestamp": "2026-07-10T12:00:00Z",
        "sessionElapsedNanos": 1,
    }
    result.update(values)
    return result


def write_jsonl(path: Path, records: list[dict], partial: str = "") -> None:
    text = "".join(json.dumps(item, separators=(",", ":")) + "\n" for item in records)
    path.write_text(text + partial, encoding="utf-8")


def make_session(session_dir: Path) -> None:
    captures = session_dir / "captures"
    capture_dir = captures / "capture-fixture"
    capture_dir.mkdir(parents=True, exist_ok=True)
    for name in ("metadata.txt", "summary.txt"):
        (capture_dir / name).write_text("fixture\n", encoding="utf-8")

    manifest = {
        "schema": "renderer-diagnostics",
        "schemaVersion": 1,
        "sessionId": "session-fixture",
        "state": "closed",
        "branch": "feat/renderer-v2-refinement",
        "commit": "abcdef123456",
        "targetMode": "live",
    }
    (session_dir / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")
    write_jsonl(
        session_dir / "telemetry.jsonl",
        [
            record(
                "telemetry",
                trigger="periodic",
                rendererFrameSequence=300,
                **{
                    "frame.lastNanos": 16_000_000,
                    "runtime.heap.usedBytes": 100_000_000,
                    "runtime.gc.collectionCountDelta": 0,
                    "runtime.gc.collectionTimeMillisDelta": 0,
                    "openGL.droppedFrames.lifetime": 0,
                    "openGL.droppedFrames.window": 0,
                    "counter.openGLWorldChunkUpload.window.total": 0,
                    "counter.renderer2DRotatedSpriteCommandAccepted.recent.latest": 12,
                    "counter.renderer2DRotatedSpriteCommandAccepted.lifetime.max": 18,
                    "counter.renderer2DRotatedSpriteCommandDropped.lifetime.total": 0,
                    "config.renderer2D.rotatedSpriteCommandLimit": 256,
                },
            ),
            record(
                "telemetry",
                trigger="slow-frame",
                rendererFrameSequence=600,
                **{
                    "frame.lastNanos": 48_000_000,
                    "runtime.heap.usedBytes": 140_000_000,
                    "runtime.gc.collectionCountDelta": 1,
                    "runtime.gc.collectionTimeMillisDelta": 7,
                    "openGL.droppedFrames.lifetime": 2,
                    "openGL.droppedFrames.window": 2,
                    "counter.openGLWorldChunkUpload.window.total": 4,
                    "counter.renderer2DRotatedSpriteCommandAccepted.recent.latest": 256,
                    "counter.renderer2DRotatedSpriteCommandAccepted.lifetime.max": 256,
                    "counter.renderer2DRotatedSpriteCommandDropped.lifetime.total": 3,
                    "counter.renderer2DRotatedSpriteCommandDropped.window.total": 3,
                    "config.renderer2D.rotatedSpriteCommandLimit": 256,
                },
            ),
        ],
    )
    write_jsonl(
        session_dir / "events.jsonl",
        [
            record("event", eventType="session.start"),
            record("event", eventType="renderer.slow-frame"),
            record("event", eventType="renderer.2d-command-overflow", stream="rotated-sprite"),
            record("event", eventType="renderer.world-section-load"),
            record("event", eventType="session.stop"),
        ],
    )
    write_jsonl(
        captures / "capture-index.jsonl",
        [
            record(
                "capture",
                status="completed",
                burstId=1,
                burstFrameIndex=0,
                captureSequence=1,
                rendererFrameSequence=600,
                path="captures/capture-fixture/",
                captureSpanNanos=20_000_000,
                inputCaptureNanos=2_000_000,
                layerCaptureNanos=8_000_000,
                finishCaptureNanos=2_000_000,
                captureWorkNanos=12_000_000,
                failed=False,
                artifacts=["metadata.txt", "summary.txt"],
            )
        ],
    )


def run_analyzer(session_dir: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["python3", str(ANALYZER), str(session_dir), *args],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="renderer-session-analyzer-") as tmp_name:
        session_dir = Path(tmp_name)
        make_session(session_dir)
        result = run_analyzer(session_dir, "--strict", "--verbose")
        if result.returncode != 0:
            raise AssertionError(result.stderr)
        for snippet in (
            "# Renderer Diagnostic Session Summary",
            "Frame sample p50/p95/p99: 16.000ms / 48.000ms / 48.000ms",
            "GC delta across report windows: 1 collections / 7ms",
            "Frame 600: 48.000ms; correlated with GC activity, chunk uploads, OpenGL frame drops, RotatedSprite command overflow.",
            "rotated-sprite: latest accepted 256, lifetime max 256, total dropped 3, limit 256",
            "Burst 1 frame 0: `completed` at `captures/capture-fixture/`",
            "Indexed capture artifacts are present.",
        ):
            if snippet not in result.stdout:
                raise AssertionError(f"missing {snippet!r} in:\n{result.stdout}")
        summary = (session_dir / "ai-summary.md").read_text(encoding="utf-8")
        if summary != result.stdout:
            raise AssertionError("written AI summary differs from stdout")

        make_session(session_dir)
        with (session_dir / "events.jsonl").open("a", encoding="utf-8") as writer:
            writer.write('{"schema":"renderer-diagnostics"')
        result = run_analyzer(session_dir, "--no-write")
        if result.returncode != 0:
            raise AssertionError("partial final JSONL record should be tolerated:\n" + result.stderr)
        if "Ignored an incomplete final JSONL record in: events.jsonl" not in result.stdout:
            raise AssertionError(result.stdout)

        make_session(session_dir)
        manifest = json.loads((session_dir / "manifest.json").read_text(encoding="utf-8"))
        manifest["schemaVersion"] = 99
        (session_dir / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")
        result = run_analyzer(session_dir)
        if result.returncode == 0 or "unsupported schema version 99" not in result.stderr:
            raise AssertionError(result.stderr)

        make_session(session_dir)
        lines = (session_dir / "telemetry.jsonl").read_text(encoding="utf-8").splitlines()
        (session_dir / "telemetry.jsonl").write_text(
            lines[0] + "\n{" + "\n" + lines[1] + "\n",
            encoding="utf-8",
        )
        result = run_analyzer(session_dir)
        if result.returncode == 0 or "invalid JSONL record 2" not in result.stderr:
            raise AssertionError(result.stderr)

        make_session(session_dir)
        (session_dir / "captures/capture-fixture/summary.txt").unlink()
        result = run_analyzer(session_dir, "--strict")
        if result.returncode == 0 or "missing indexed artifact summary.txt" not in result.stderr:
            raise AssertionError(result.stderr)

        make_session(session_dir)
        capture_index_path = session_dir / "captures/capture-index.jsonl"
        capture_record = json.loads(capture_index_path.read_text(encoding="utf-8"))
        capture_record["status"] = "failed"
        capture_record["failed"] = True
        capture_record["failure"] = "fixture failure"
        write_jsonl(capture_index_path, [capture_record])
        result = run_analyzer(session_dir, "--strict")
        if result.returncode == 0 or "reported failure" not in result.stderr:
            raise AssertionError(result.stderr)

        standalone_capture = session_dir / "standalone-capture"
        standalone_capture.mkdir()
        (standalone_capture / "metadata.txt").write_text("sequence=1\n", encoding="utf-8")
        result = run_analyzer(standalone_capture)
        if result.returncode == 0 or "capture missing required files" not in result.stderr:
            raise AssertionError(
                "standalone capture was not delegated to the frame analyzer:\n" + result.stderr
            )

    print("PASS: renderer diagnostic session analyzer validates and correlates AI-readable logs")


if __name__ == "__main__":
    main()
