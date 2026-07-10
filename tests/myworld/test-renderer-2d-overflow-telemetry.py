#!/usr/bin/env python3
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_JAR = ROOT / "Client_Base/Open_RSC_Client.jar"
GRAPHICS = ROOT / "Client_Base/src/orsc/graphics/two/GraphicsController.java"
FRAME = ROOT / "Client_Base/src/orsc/graphics/Renderer2DFrame.java"
TELEMETRY = ROOT / "Client_Base/src/orsc/RenderTelemetry.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
FRAME_CAPTURE = ROOT / "PC_Client/src/orsc/OpenGLFrameCapture.java"
ANALYZER = ROOT / "scripts/analyze-renderer-v2-capture.py"


JAVA_SOURCE = r"""
package orsc.graphics.two;

import com.openrsc.client.model.Sprite;
import java.lang.reflect.Method;
import orsc.graphics.Renderer2DFrame;

public final class Renderer2DOverflowStress {
	public static void main(String[] args) throws Exception {
		System.setProperty("spoiled_milk.opengl_sprite_overlay", "true");
		System.setProperty("spoiled_milk.opengl_sprite_overlay_mode", "native-ui");
		System.setProperty("spoiled_milk.opengl_native_ui_replace", "true");
		System.setProperty("spoiledmilk.openglWorldUiReplay", "true");

		GraphicsController graphics = new GraphicsController(64, 64, 1);
		graphics.beginRenderer2DFrame();
		Sprite sprite = new Sprite(new int[] {0x00ffffff}, 1, 1);
		Method record = GraphicsController.class.getDeclaredMethod(
			"recordRenderer2DRotatedSprite",
			Sprite.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			int.class,
			boolean.class);
		record.setAccessible(true);

		int accepted = 0;
		int dropped = 0;
		for (int attempt = 0; attempt < 259; attempt++) {
			boolean captured = ((Boolean) record.invoke(
				graphics,
				sprite,
				0,
				0,
				1,
				0,
				1,
				1,
				0,
				1,
				0,
				0,
				64,
				64,
				false)).booleanValue();
			if (captured) {
				accepted++;
			} else {
				dropped++;
			}
		}

		Renderer2DFrame frame = graphics.consumeRenderer2DFrame();
		Renderer2DFrame.CaptureStats stats = frame.getCaptureStats();
		require(accepted == 256, "accepted=" + accepted);
		require(dropped == 3, "dropped=" + dropped);
		require(frame.getRotatedSpriteCommands().length == 256,
			"commands=" + frame.getRotatedSpriteCommands().length);
		require(stats.getRotatedSpriteCommandLimit() == 256,
			"limit=" + stats.getRotatedSpriteCommandLimit());
		require(stats.getRotatedSpriteCommandAttempts() == 259,
			"attempts=" + stats.getRotatedSpriteCommandAttempts());
		require(stats.getRotatedSpriteCommandsCaptured() == 256,
			"captured=" + stats.getRotatedSpriteCommandsCaptured());
		require(stats.getRotatedSpriteCommandsSkippedOverflow() == 3,
			"overflow=" + stats.getRotatedSpriteCommandsSkippedOverflow());

		System.out.println("PASS: rotated-sprite overflow records 259/256/3 attempts/accepted/dropped");
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
"""


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def run(command: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, capture_output=True, text=True)


def ensure_client_jar() -> None:
    source_files = (GRAPHICS, FRAME)
    if CLIENT_JAR.exists() and all(
        source.stat().st_mtime <= CLIENT_JAR.stat().st_mtime for source in source_files
    ):
        return
    result = run([str(ROOT / "scripts/build-client.sh")], ROOT)
    if result.returncode != 0:
        fail("client build failed:\n" + result.stdout + result.stderr)


def validate_source_guards() -> None:
    graphics = GRAPHICS.read_text(encoding="utf-8")
    frame = FRAME.read_text(encoding="utf-8")
    telemetry = TELEMETRY.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    presenter = PRESENTER.read_text(encoding="utf-8")
    frame_capture = FRAME_CAPTURE.read_text(encoding="utf-8")
    analyzer = ANALYZER.read_text(encoding="utf-8")

    for constant, limit in (
        ("SPRITE_COMMAND_LIMIT", 4096),
        ("TEXT_COMMAND_LIMIT", 4096),
        ("PRIMITIVE_COMMAND_LIMIT", 4096),
        ("ROTATED_SPRITE_COMMAND_LIMIT", 256),
        ("CIRCLE_COMMAND_LIMIT", 512),
    ):
        require(frame, f"public static final int {constant} = {limit};", f"{constant} shared cap")

    for counter in (
        "renderer2DCaptureSkippedOverflow++",
        "renderer2DTextSkippedOverflow++",
        "renderer2DPrimitiveSkippedOverflow++",
        "renderer2DRotatedSpriteSkippedOverflow++",
        "renderer2DCircleSkippedOverflow++",
    ):
        require(graphics, counter, f"overflow counter {counter}")

    for stream in ("sprite", "text", "primitive", "rotated-sprite", "circle"):
        require(graphics, f'logRenderer2DCommandOverflow("{stream}"', f"{stream} overflow warning")

    require(
        presenter,
        "Renderer2DFrame.CaptureStats renderer2DCaptureStats = frame.renderer2DFrame.getCaptureStats();",
        "unconditional per-frame stats read",
    )
    require(
        presenter,
        "RenderTelemetry.recordRenderer2DCommandLimits(renderer2DCaptureStats);",
        "telemetry handoff",
    )
    require(telemetry, "public static String renderer2DCommandLimitSummary()", "F6 telemetry summary")
    require(telemetry, "renderer2DRotatedSpriteCommandDroppedStats.record", "rotated drop aggregation")
    require(applet, '"2d cap cur/max/drop@limit " + RenderTelemetry.renderer2DCommandLimitSummary()', "expanded F6 limit line")
    require(frame_capture, 'new File(directory, "renderer-2d-command-limits.tsv")', "Ctrl+F9 limit capture")
    require(analyzer, 'read_optional_tsv(capture_dir / "renderer-2d-command-limits.tsv")', "backward-compatible analyzer input")


def run_rotated_sprite_stress() -> None:
    ensure_client_jar()
    with tempfile.TemporaryDirectory(prefix="renderer-2d-overflow-") as tmp_name:
        tmp = Path(tmp_name)
        source_dir = tmp / "orsc" / "graphics" / "two"
        classes_dir = tmp / "classes"
        source_dir.mkdir(parents=True)
        classes_dir.mkdir()
        source_file = source_dir / "Renderer2DOverflowStress.java"
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
            ROOT,
        )
        if compile_result.returncode != 0:
            fail("overflow stress compile failed:\n" + compile_result.stdout + compile_result.stderr)

        run_result = run(
            [
                "java",
                "-cp",
                f"{classes_dir}:{CLIENT_JAR}",
                "orsc.graphics.two.Renderer2DOverflowStress",
            ],
            ROOT,
        )
        if run_result.returncode != 0:
            fail("overflow stress run failed:\n" + run_result.stdout + run_result.stderr)
        if "PASS: rotated-sprite overflow records 259/256/3" not in run_result.stdout:
            fail("overflow stress did not report success:\n" + run_result.stdout + run_result.stderr)
        warning = "rotated-sprite command capture limit 256 reached"
        if run_result.stdout.count(warning) != 1:
            fail(
                "overflow stress did not report exactly one limit warning:\n"
                + run_result.stdout
                + run_result.stderr
            )


def main() -> None:
    validate_source_guards()
    run_rotated_sprite_stress()
    print("PASS: renderer 2D command overflow telemetry is wired and stress-tested")


if __name__ == "__main__":
    main()
