#!/usr/bin/env python3
import os
import subprocess
import tempfile
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RELIEF = ROOT / "Client_Base/src/orsc/RendererReliefSettings.java"
SHADER = ROOT / "PC_Client/src/orsc/OpenGLShaderProgram.java"
SHADOW_MASK = ROOT / "PC_Client/src/orsc/RemasterShadowMaskBuilder.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
FRAME_CAPTURE = ROOT / "PC_Client/src/orsc/OpenGLFrameCapture.java"


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"{label} missing expected snippet: {needle!r}")


def compile_fixture(temp: Path) -> None:
    fixture = textwrap.dedent(
        """
        package orsc;

        public final class RendererShadingDiagnosticsFixture {
            public static void main(String[] args) {
                System.out.println(RendererReliefSettings.getTerrainMode().id);
                System.out.println(RendererReliefSettings.getTerrainStrength());
                System.out.println(RendererReliefSettings.getObjectMode().id);
                System.out.println(RendererReliefSettings.getObjectStrength());
            }
        }
        """
    )
    fixture_path = temp / "RendererShadingDiagnosticsFixture.java"
    fixture_path.write_text(fixture, encoding="utf-8")
    subprocess.run(
        ["javac", "-d", str(temp), str(RELIEF), str(fixture_path)],
        check=True,
        cwd=ROOT,
    )


def run_fixture(temp: Path, overrides: dict[str, str]) -> list[str]:
    env = os.environ.copy()
    for key in (
        "SPOILED_MILK_OPENGL_RELIEF",
        "SPOILED_MILK_OPENGL_TERRAIN_RELIEF",
        "SPOILED_MILK_OPENGL_OBJECT_RELIEF",
    ):
        env.pop(key, None)
    env.update(overrides)
    result = subprocess.run(
        ["java", "-cp", str(temp), "orsc.RendererShadingDiagnosticsFixture"],
        check=True,
        cwd=ROOT,
        env=env,
        text=True,
        capture_output=True,
    )
    return result.stdout.strip().splitlines()


def main() -> None:
    relief = RELIEF.read_text(encoding="utf-8")
    shader = SHADER.read_text(encoding="utf-8")
    shadow_mask = SHADOW_MASK.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    frame_capture = FRAME_CAPTURE.read_text(encoding="utf-8")

    require(relief, 'TERRAIN_RELIEF_ENV = "SPOILED_MILK_OPENGL_TERRAIN_RELIEF"',
            "terrain diagnostic override")
    require(relief, 'OBJECT_RELIEF_ENV = "SPOILED_MILK_OPENGL_OBJECT_RELIEF"',
            "object diagnostic override")
    require(relief, "readRuntimeSetting(RELIEF_PROPERTY, RELIEF_ENV)",
            "legacy shared override compatibility")
    require(shader, '"uniform float uTerrainReliefStrength;\\n"',
            "terrain relief shader ownership")
    require(shader, '"uniform float uObjectReliefStrength;\\n"',
            "object relief shader ownership")
    require(shader, '"\\t\\t? uTerrainReliefStrength : uObjectReliefStrength;\\n"',
            "model-kind relief selection")
    require(shader, "RendererReliefSettings.getTerrainStrength()",
            "terrain relief uniform upload")
    require(shader, "RendererReliefSettings.getObjectStrength()",
            "object relief uniform upload")
    require(shadow_mask, 'return "terrain shadow dir " + REMASTER_SHADOW_MASK_BASE_ALPHA',
            "shadow channel debug summary")
    require(shadow_mask,
            'DIRECTIONAL_ALPHA_SCALE_ENV =\n\t\t"SPOILED_MILK_REMASTER_DIRECTIONAL_SHADOW_ALPHA_SCALE"',
            "directional terrain-shadow diagnostic override")
    require(shadow_mask,
            "* REMASTER_SHADOW_MASK_DIRECTIONAL_ALPHA_SCALE;",
            "directional terrain-shadow diagnostic scaling")
    require(applet, "String shadingLine = RendererReliefSettings.debugSummary()",
            "F6 shading summary")
    for key in (
        "shading.terrainReliefMode=",
        "shading.terrainReliefStrength=",
        "shading.objectReliefMode=",
        "shading.objectReliefStrength=",
        "shading.diffuseResponse=model-kind-fixed",
        "shading.terrainShadowChannels=directional+contact",
        "shading.objectShadowMask=not-applied",
        "shading.terrainShadowDirectionalBaseAlpha=",
        "shading.terrainShadowDirectionalAlphaScale=",
        "shading.terrainShadowContactAlpha=",
    ):
        require(frame_capture, key, f"AI-readable capture field {key}")

    with tempfile.TemporaryDirectory(prefix="renderer-shading-test-") as temp_dir:
        temp = Path(temp_dir)
        compile_fixture(temp)
        if run_fixture(temp, {}) != ["max", "2.0", "max", "2.0"]:
            raise AssertionError("parity defaults must retain max relief for terrain and objects")
        if run_fixture(temp, {"SPOILED_MILK_OPENGL_RELIEF": "low"}) != [
            "low", "0.5", "low", "0.5"
        ]:
            raise AssertionError("legacy shared relief override must still drive both scopes")
        if run_fixture(
            temp,
            {
                "SPOILED_MILK_OPENGL_RELIEF": "medium",
                "SPOILED_MILK_OPENGL_TERRAIN_RELIEF": "off",
                "SPOILED_MILK_OPENGL_OBJECT_RELIEF": "high",
            },
        ) != ["off", "0.0", "high", "1.5"]:
            raise AssertionError("scoped terrain/object overrides must be independent")

    print("PASS: terrain and object shading diagnostics are independent with parity defaults")


if __name__ == "__main__":
    main()
