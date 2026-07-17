#!/usr/bin/env python3
import pathlib
import shutil
import subprocess
import tempfile
import textwrap


ROOT = pathlib.Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "PC_Client/src/orsc/OpenGLBelowTerrainSettings.java"
PRESENTER = ROOT / "PC_Client/src/orsc/OpenGLFramePresenter.java"
CHUNKS = ROOT / "PC_Client/src/orsc/OpenGLWorldChunkRenderer.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def settings_fixture() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")
    fixture = textwrap.dedent(
        """
        package orsc;

        public final class BelowTerrainSettingsFixture {
            public static void main(String[] args) {
                require(OpenGLBelowTerrainSettings.Mode.from(null)
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR,
                    "missing setting must use accepted depth floor");
                require(OpenGLBelowTerrainSettings.Mode.from("off") == OpenGLBelowTerrainSettings.Mode.OFF,
                    "off setting");
                require(OpenGLBelowTerrainSettings.Mode.from("depth-floor")
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR, "depth-floor setting");
                require(OpenGLBelowTerrainSettings.Mode.from("floor")
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR, "floor alias");
                require(OpenGLBelowTerrainSettings.Mode.from("unknown")
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR,
                    "unknown setting must retain accepted default");
                OpenGLBelowTerrainSettings.setMode(OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR);
                require(OpenGLBelowTerrainSettings.getMode()
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR, "runtime comparison override");
                OpenGLBelowTerrainSettings.setMode(null);
                require(OpenGLBelowTerrainSettings.getMode()
                    == OpenGLBelowTerrainSettings.Mode.DEPTH_FLOOR,
                    "null override must restore accepted default");
            }

            private static void require(boolean condition, String message) {
                if (!condition) {
                    throw new AssertionError(message);
                }
            }
        }
        """
    )
    with tempfile.TemporaryDirectory() as temp_dir:
        temp = pathlib.Path(temp_dir)
        fixture_path = temp / "BelowTerrainSettingsFixture.java"
        fixture_path.write_text(fixture, encoding="utf-8")
        subprocess.run([javac, "-d", str(temp), str(SETTINGS), str(fixture_path)], check=True)
        subprocess.run([java, "-cp", str(temp), "orsc.BelowTerrainSettingsFixture"], check=True)


def main() -> None:
    settings_fixture()
    settings = SETTINGS.read_text(encoding="utf-8")
    presenter = PRESENTER.read_text(encoding="utf-8")
    chunks = CHUNKS.read_text(encoding="utf-8")

    require('"spoiledmilk.openglBelowTerrain"' in settings, "system-property switch missing")
    require('"SPOILED_MILK_OPENGL_BELOW_TERRAIN"' in settings, "environment switch missing")
    require('DEPTH_FLOOR("depth-floor")' in settings, "depth-floor mode missing")
    require(
        "worldChunkRenderer.drawBelowTerrainDepthFloor(frame.renderer3DFrame);" in presenter,
        "below-terrain pass must follow resident chunk drawing",
    )
    require(
        presenter.index("worldChunkRenderer.drawDiagnostic(")
        < presenter.index("worldChunkRenderer.drawBelowTerrainDepthFloor(frame.renderer3DFrame);"),
        "occlusion floor must draw after terrain establishes depth",
    )
    require("gl.glDepthMask(false);" in chunks, "diagnostic floor must not claim later sprite depth")
    require("chunk.getPlane() == activePlane" in chunks, "floor must follow the active terrain plane")
    require(
        "chunk.getChunkRole() == Renderer3DWorldChunkFrame.CHUNK_ROLE_WORLD" in chunks,
        "object-only chunks must not own floor geometry",
    )
    require("maxY + BELOW_TERRAIN_DEPTH" in chunks, "floor must sit below the lowest terrain point")
    print("OpenGL below-terrain experiment checks passed")


if __name__ == "__main__":
    main()
