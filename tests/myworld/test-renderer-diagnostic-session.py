#!/usr/bin/env python3
import json
import os
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_JAR = ROOT / "Client_Base/Open_RSC_Client.jar"
SESSION_SOURCE = ROOT / "Client_Base/src/orsc/RendererDiagnosticSession.java"
OPENRSC_SOURCE = ROOT / "PC_Client/src/orsc/OpenRSC.java"
RUNTIME_LOGGER_SOURCE = ROOT / "Client_Base/src/orsc/ClientRuntimeLogger.java"
LAUNCHER = ROOT / "scripts/run-client.sh"
BOUNDED_TEE = ROOT / "scripts/bounded-log-tee.py"


JAVA_SOURCE = r"""
package orsc;

public final class RendererDiagnosticSessionFixture {
	public static void main(String[] args) {
		RendererDiagnosticSession.start();
		RendererDiagnosticSession.Record telemetry =
			RendererDiagnosticSession.newTelemetryRecord("fixture", 42L);
		if (telemetry != null) {
			telemetry.number("frame.totalNanos", 1234567L);
			telemetry.number("frame.windowAverageNanos", 765432.5);
			RendererDiagnosticSession.writeTelemetry(telemetry);
		}
		RendererDiagnosticSession.recordEvent("fixture.event", "safe detail");
		RendererDiagnosticSession.recordThrowable(
			"fixture failure",
			new IllegalStateException("expected test exception"));
		RendererDiagnosticSession.close();
	}
}
"""


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def run(command: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(command, cwd=ROOT, capture_output=True, **kwargs)


def ensure_client_jar() -> None:
    if CLIENT_JAR.exists() and SESSION_SOURCE.stat().st_mtime <= CLIENT_JAR.stat().st_mtime:
        return
    result = run([str(ROOT / "scripts/build-client.sh")], text=True)
    if result.returncode != 0:
        fail("client build failed:\n" + result.stdout + result.stderr)


def read_jsonl(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line]


def validate_runtime_session(tmp: Path) -> None:
    ensure_client_jar()
    source_dir = tmp / "source" / "orsc"
    classes_dir = tmp / "classes"
    source_dir.mkdir(parents=True)
    classes_dir.mkdir()
    source_file = source_dir / "RendererDiagnosticSessionFixture.java"
    source_file.write_text(JAVA_SOURCE, encoding="utf-8")

    compile_result = run(
        [
            "javac",
            "-source",
            "1.8",
            "-target",
            "1.8",
            "-cp",
            str(CLIENT_JAR),
            "-d",
            str(classes_dir),
            str(source_file),
        ],
        text=True,
    )
    if compile_result.returncode != 0:
        fail("fixture compile failed:\n" + compile_result.stdout + compile_result.stderr)

    disabled_dir = tmp / "disabled"
    disabled_result = run(
        [
            "java",
            f"-Dspoiledmilk.rendererDiagnosticSessionDir={disabled_dir}",
            "-cp",
            f"{classes_dir}:{CLIENT_JAR}",
            "orsc.RendererDiagnosticSessionFixture",
        ],
        text=True,
    )
    if disabled_result.returncode != 0:
        fail("disabled fixture failed:\n" + disabled_result.stdout + disabled_result.stderr)
    if disabled_dir.exists():
        fail("disabled diagnostics created a session directory")

    session_dir = tmp / "session-fixture"
    environment = os.environ.copy()
    environment.update(
        {
            "SPOILED_MILK_CLIENT_BRANCH": "test-branch",
            "SPOILED_MILK_CLIENT_COMMIT": "0123456789abcdef",
            "SPOILED_MILK_CLIENT_TARGET_MODE": "test",
        }
    )
    enabled_result = run(
        [
            "java",
            "-Dspoiledmilk.rendererDiagnostics=true",
            f"-Dspoiledmilk.rendererDiagnosticSessionDir={session_dir}",
            "-Dspoiledmilk.rendererExampleSetting=enabled",
            "-Dspoiledmilk.testToken=must-not-appear",
            "-cp",
            f"{classes_dir}:{CLIENT_JAR}",
            "orsc.RendererDiagnosticSessionFixture",
        ],
        text=True,
        env=environment,
    )
    if enabled_result.returncode != 0:
        fail("enabled fixture failed:\n" + enabled_result.stdout + enabled_result.stderr)

    manifest = json.loads((session_dir / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("schema") != "renderer-diagnostics" or manifest.get("schemaVersion") != 1:
        fail(f"unexpected manifest schema: {manifest}")
    if manifest.get("state") != "closed":
        fail(f"session did not close cleanly: {manifest.get('state')}")
    if manifest.get("branch") != "test-branch" or manifest.get("commit") != "0123456789abcdef":
        fail("launcher revision metadata was not retained")
    if manifest.get("settings.property.spoiledmilk.rendererExampleSetting") != "enabled":
        fail("safe renderer property missing from manifest")
    if "must-not-appear" in json.dumps(manifest):
        fail("sensitive token-like property leaked into manifest")

    telemetry = read_jsonl(session_dir / "telemetry.jsonl")
    if len(telemetry) != 1:
        fail(f"expected one telemetry record, got {len(telemetry)}")
    if telemetry[0].get("rendererFrameSequence") != 42:
        fail("telemetry frame sequence missing")
    if telemetry[0].get("frame.totalNanos") != 1234567:
        fail("raw numeric telemetry missing")

    events = read_jsonl(session_dir / "events.jsonl")
    event_types = [event.get("eventType") for event in events]
    expected_types = ["session.start", "fixture.event", "client.exception", "session.stop"]
    if event_types != expected_types:
        fail(f"unexpected event sequence: {event_types}")


def validate_bounded_tee(tmp: Path) -> None:
    log_file = tmp / "bounded.log"
    input_text = "first retained line\n" + "x" * 160 + "\nlast visible line\n"
    result = run(
        ["python3", str(BOUNDED_TEE), str(log_file), "120"],
        input=input_text.encode("utf-8"),
    )
    if result.returncode != 0:
        fail("bounded tee failed:\n" + result.stderr.decode("utf-8", errors="replace"))
    if result.stdout.decode("utf-8") != input_text:
        fail("bounded tee did not preserve visible stdout")
    retained = log_file.read_bytes()
    if len(retained) > 120:
        fail(f"bounded tee exceeded byte budget: {len(retained)}")
    if b"byte budget reached" not in retained:
        fail("bounded tee did not mark truncation")
    if b"last visible line" in retained:
        fail("bounded tee retained content after truncation")


def validate_source_contract() -> None:
    session = SESSION_SOURCE.read_text(encoding="utf-8")
    openrsc = OPENRSC_SOURCE.read_text(encoding="utf-8")
    runtime_logger = RUNTIME_LOGGER_SOURCE.read_text(encoding="utf-8")
    launcher = LAUNCHER.read_text(encoding="utf-8")

    required = {
        "disabled-by-default session gate": "spoiledmilk.rendererDiagnostics",
        "versioned JSONL telemetry": 'openWriter(new File(sessionDirectory, "telemetry.jsonl"))',
        "versioned JSONL events": 'openWriter(new File(sessionDirectory, "events.jsonl"))',
        "manifest publication": 'writeManifest("open")',
        "privacy filter": 'normalized.contains("credential")',
        "structured byte budget": "MAX_LOG_BYTES",
    }
    for description, needle in required.items():
        if needle not in session:
            fail(f"missing {description}: {needle}")
    if "RendererDiagnosticSession.start();" not in openrsc:
        fail("desktop client does not start diagnostic sessions")
    if "RendererDiagnosticSession.recordThrowable(context, throwable);" not in runtime_logger:
        fail("uncaught failures are not correlated with the diagnostic session")
    for needle in (
        "--renderer-diagnostics",
        "SPOILED_MILK_RENDERER_TELEMETRY=true",
        "SPOILED_MILK_OPENGL_FRAME_CAPTURE=true",
        "bounded-log-tee.py",
    ):
        if needle not in launcher:
            fail(f"diagnostic launcher missing {needle}")


def main() -> None:
    validate_source_contract()
    with tempfile.TemporaryDirectory(prefix="renderer-diagnostic-session-") as tmp_name:
        tmp = Path(tmp_name)
        validate_runtime_session(tmp)
        validate_bounded_tee(tmp)
    print("PASS: renderer diagnostic session foundation is bounded and machine-readable")


if __name__ == "__main__":
    main()
