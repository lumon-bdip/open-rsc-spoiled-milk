#!/usr/bin/env python3
import subprocess
import tempfile
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ROOF_VISIBILITY = ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DRoofVisibility.java"
MODEL_KIND = ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DModelKind.java"
FRAME = ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DFrame.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
CHUNK_RENDERER = ROOT / "PC_Client/src/orsc/OpenGLWorldChunkRenderer.java"
FRAME_CAPTURE = ROOT / "PC_Client/src/orsc/OpenGLFrameCapture.java"


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"{label} missing expected snippet: {needle!r}")


def run_visibility_matrix() -> None:
    harness = textwrap.dedent(
        """
        import orsc.graphics.three.Renderer3DModelKind;
        import orsc.graphics.three.Renderer3DRoofVisibility;

        public final class RoofVisibilityHarness {
            private static void expect(boolean condition, String label) {
                if (!condition) {
                    throw new AssertionError(label);
                }
            }

            public static void main(String[] args) {
                Renderer3DRoofVisibility outdoor =
                    Renderer3DRoofVisibility.resolve(false, 0, false);
                expect(outdoor == Renderer3DRoofVisibility.VISIBLE, "ground outdoor state");
                expect(outdoor.areRoofsVisible(), "ground outdoor roofs");
                expect(outdoor.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 0, 2),
                    "ground outdoor upper walls");

                Renderer3DRoofVisibility indoor =
                    Renderer3DRoofVisibility.resolve(false, 0, true);
                expect(indoor == Renderer3DRoofVisibility.HIDDEN_INDOORS, "ground indoor state");
                expect(!indoor.areRoofsVisible(), "ground indoor roofs hidden");
                expect(indoor.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 0, 0),
                    "ground indoor active walls");
                expect(!indoor.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 0, 1),
                    "ground indoor upper walls hidden");

                Renderer3DRoofVisibility upstairs =
                    Renderer3DRoofVisibility.resolve(false, 1, false);
                expect(upstairs == Renderer3DRoofVisibility.HIDDEN_ABOVE_ACTIVE_FLOOR,
                    "upper-floor state");
                expect(!upstairs.areRoofsVisible(), "upper-floor roof hidden");
                expect(upstairs.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 1, 1),
                    "upper-floor active walls");
                expect(!upstairs.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 1, 2),
                    "walls above upper floor hidden");

                Renderer3DRoofVisibility setting =
                    Renderer3DRoofVisibility.resolve(true, 1, false);
                expect(setting == Renderer3DRoofVisibility.HIDDEN_BY_SETTING,
                    "global setting precedence");
                expect(!setting.usesAutomaticRoofCameraZoom(), "setting disables roof camera zoom");
                expect(setting.isWorldChunkModelKindVisible(Renderer3DModelKind.WALL, 1, 1),
                    "global setting preserves active-floor walls");
                expect(setting.isWorldChunkModelKindVisible(Renderer3DModelKind.TERRAIN, 0, 2),
                    "terrain remains visible");
            }
        }
        """
    )
    with tempfile.TemporaryDirectory(prefix="roof-visibility-test-") as temp_dir:
        temp = Path(temp_dir)
        harness_path = temp / "RoofVisibilityHarness.java"
        harness_path.write_text(harness, encoding="utf-8")
        subprocess.run(
            [
                "javac",
                "-d",
                str(temp),
                str(MODEL_KIND),
                str(ROOF_VISIBILITY),
                str(harness_path),
            ],
            check=True,
            cwd=ROOT,
        )
        subprocess.run(
            ["java", "-cp", str(temp), "RoofVisibilityHarness"],
            check=True,
            cwd=ROOT,
        )


def main() -> None:
    roof_visibility = ROOF_VISIBILITY.read_text(encoding="utf-8")
    frame = FRAME.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    chunk_renderer = CHUNK_RENDERER.read_text(encoding="utf-8")
    frame_capture = FRAME_CAPTURE.read_text(encoding="utf-8")

    require(roof_visibility, "HIDDEN_INDOORS", "named indoor roof state")
    require(roof_visibility, "HIDDEN_ABOVE_ACTIVE_FLOOR", "named upper-floor roof state")
    require(mudclient, "Renderer3DRoofVisibility roofVisibility = this.currentRenderer3DRoofVisibility();",
            "legacy scene resolves one roof state")
    require(mudclient, "(this.world.collisionFlags[tileX][tileZ] & CollisionFlag.OBJECT) != 0",
            "legacy covered-tile source")
    require(mudclient, "renderer3DFrame.setRoofVisibility(roofVisibility, this.lastHeightOffset);",
            "roof state frame handoff")
    require(frame, "public boolean isWorldChunkModelKindVisible(Renderer3DModelKind modelKind, int chunkPlane)",
            "frame roof visibility query")
    require(chunk_renderer, "frame.isWorldChunkModelKindVisible(modelKind, chunk.getPlane())",
            "resident chunk roof visibility query")
    require(frame_capture, 'writer.println("roofVisibility=" + renderer3DFrame.getRoofVisibility().name());',
            "AI-readable roof state capture")

    run_visibility_matrix()
    print("PASS: roof visibility matrix is shared by legacy and resident rendering")


if __name__ == "__main__":
    main()
