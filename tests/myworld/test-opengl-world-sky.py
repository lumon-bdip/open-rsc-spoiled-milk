#!/usr/bin/env python3
import pathlib
import shutil
import subprocess
import tempfile
import textwrap


ROOT = pathlib.Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "PC_Client/src/orsc/OpenGLSkySettings.java"
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

        public final class OpenGLSkySettingsFixture {
            public static void main(String[] args) {
                require(OpenGLSkySettings.Mode.from(null) == OpenGLSkySettings.Mode.SCREEN,
                    "screen sky remains default during comparison");
                require(OpenGLSkySettings.Mode.from("world-dome")
                    == OpenGLSkySettings.Mode.WORLD_DOME, "world dome setting");
                require(OpenGLSkySettings.Mode.from("sphere")
                    == OpenGLSkySettings.Mode.WORLD_DOME, "sphere alias");
                require(OpenGLSkySettings.Mode.from("unknown") == OpenGLSkySettings.Mode.SCREEN,
                    "unknown sky setting must retain baseline");
                OpenGLSkySettings.setMode(OpenGLSkySettings.Mode.WORLD_DOME);
                require(OpenGLSkySettings.getMode() == OpenGLSkySettings.Mode.WORLD_DOME,
                    "runtime comparison override");
                OpenGLSkySettings.setMode(null);
                require(OpenGLSkySettings.getMode() == OpenGLSkySettings.Mode.SCREEN,
                    "null override must restore baseline");
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
        fixture_path = temp / "OpenGLSkySettingsFixture.java"
        fixture_path.write_text(fixture, encoding="utf-8")
        subprocess.run([javac, "-d", str(temp), str(SETTINGS), str(fixture_path)], check=True)
        subprocess.run([java, "-cp", str(temp), "orsc.OpenGLSkySettingsFixture"], check=True)


def main() -> None:
    settings_fixture()
    settings = SETTINGS.read_text(encoding="utf-8")
    presenter = PRESENTER.read_text(encoding="utf-8")
    chunks = CHUNKS.read_text(encoding="utf-8")
    require('"spoiledmilk.openglSky"' in settings, "system-property sky switch missing")
    require('"SPOILED_MILK_OPENGL_SKY"' in settings, "environment sky switch missing")
    require('WORLD_DOME("world-dome")' in settings, "world-dome mode missing")
    require(
        "worldChunkRenderer.drawWorldAnchoredSky(frame.renderer3DFrame, presentation);" in presenter,
        "presenter must route the comparison through world geometry",
    )
    sky_view_start = chunks.index("private float[] skyViewMatrix(Renderer3DFrame frame)")
    sky_view_end = chunks.index("private void configureFog", sky_view_start)
    sky_view = chunks[sky_view_start:sky_view_end]
    require("getCameraRotationX()" in sky_view, "sky view must follow camera pitch")
    require("getCameraRotationY()" in sky_view, "sky view must follow camera yaw")
    require("getCameraOffset" not in sky_view, "sky view must ignore camera translation")
    require("SKY_DOME_ELEVATION_DEGREES" in chunks, "world sky needs explicit altitude rings")
    require("presentation.fogRed" in chunks, "horizon must stitch to presentation fog")
    require("frame.getFogDistance() * 0.92f" in chunks, "dome must remain inside the far clip")
    load_matrix_start = chunks.index("private void loadMatrix(int matrixMode, float[] matrix)")
    load_matrix_end = chunks.index("private FloatBuffer putWorldToClipMatrix", load_matrix_start)
    require(
        "if (worldToClipMatrixBuffer == null)" in chunks[load_matrix_start:load_matrix_end],
        "first-frame sky drawing must initialize the shared matrix buffer",
    )
    print("OpenGL world-anchored sky checks passed")


if __name__ == "__main__":
    main()
