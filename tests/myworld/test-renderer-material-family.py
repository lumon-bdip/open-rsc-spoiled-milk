#!/usr/bin/env python3
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_JAR = ROOT / "Client_Base/Open_RSC_Client.jar"
FAMILY_SOURCE = ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DMaterialFamily.java"

JAVA_SOURCE = r"""
import com.openrsc.client.entityhandling.defs.GameObjectDef;
import orsc.graphics.three.Renderer3DMaterialClassifier;
import orsc.graphics.three.Renderer3DMaterialFamily;
import orsc.graphics.three.Renderer3DModelKind;
import orsc.graphics.three.Renderer3DWorldChunkFrame;

public final class RendererMaterialFamilyFixture {
	private static void expect(
		Renderer3DMaterialFamily actual,
		Renderer3DMaterialFamily expected,
		String label) {
		if (actual != expected) {
			throw new AssertionError(label + ": expected " + expected + ", got " + actual);
		}
	}

	private static GameObjectDef object(
		String name,
		String command1,
		String command2,
		String model) {
		return new GameObjectDef(name, "fixture", command1, command2, 1, 1, 1, 0, model, 1);
	}

	public static void main(String[] args) {
		int expectedId = 0;
		for (Renderer3DMaterialFamily family : Renderer3DMaterialFamily.values()) {
			if (family.getShaderId() != expectedId) {
				throw new AssertionError("unstable shader id for " + family);
			}
			expect(Renderer3DMaterialFamily.fromShaderId(expectedId), family, "shader id round trip");
			expectedId++;
		}
		expect(
			Renderer3DMaterialFamily.fromShaderId(999),
			Renderer3DMaterialFamily.UNCLASSIFIED,
			"unknown shader id");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(Renderer3DModelKind.TERRAIN),
			Renderer3DMaterialFamily.TERRAIN,
			"terrain fallback");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(Renderer3DModelKind.WALL),
			Renderer3DMaterialFamily.WALL,
			"wall fallback");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(Renderer3DModelKind.ROOF),
			Renderer3DMaterialFamily.ROOF,
			"roof fallback");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(Renderer3DModelKind.GAME_OBJECT),
			Renderer3DMaterialFamily.SCENERY,
			"game-object fallback");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(Renderer3DModelKind.WALL_OBJECT),
			Renderer3DMaterialFamily.SCENERY,
			"wall-object fallback");
		expect(
			Renderer3DMaterialClassifier.fallbackFor(null),
			Renderer3DMaterialFamily.UNCLASSIFIED,
			"unknown fallback");
		expect(
			Renderer3DMaterialClassifier.classifyTerrain(true, false),
			Renderer3DMaterialFamily.WATER,
			"water terrain");
		expect(
			Renderer3DMaterialClassifier.classifyTerrain(true, true),
			Renderer3DMaterialFamily.EMISSIVE,
			"lava precedence");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Pine Tree", "Chop", "Examine", "tree2"),
				false),
			Renderer3DMaterialFamily.FOLIAGE,
			"foliage object");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Tin rock", "Mine", "Prospect", "tinrock1"),
				false),
			Renderer3DMaterialFamily.ORE,
			"ore object");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Torch", "WalkTo", "Examine", "torcha1"),
				true),
			Renderer3DMaterialFamily.EMISSIVE,
			"emissive precedence");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Fire", "WalkTo", "Examine", "firea1"),
				true),
			Renderer3DMaterialFamily.EMISSIVE,
			"fire emitter");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Furnace", "Smelt", "Examine", "furnace"),
				true),
			Renderer3DMaterialFamily.EMISSIVE,
			"furnace emitter");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Portal", "Enter", "Examine", "portal"),
				false),
			Renderer3DMaterialFamily.EFFECT,
			"explicit effect object");
		expect(
			Renderer3DMaterialClassifier.classifyObject(
				Renderer3DModelKind.GAME_OBJECT,
				object("Table", "WalkTo", "Examine", "table"),
				false),
			Renderer3DMaterialFamily.SCENERY,
			"ordinary scenery");

		Renderer3DWorldChunkFrame.ChunkMesh chunk = new Renderer3DWorldChunkFrame.ChunkMesh(
			0, 0, 0, 0, 0,
			new int[] {0, 0, 0, 128, 0, 0, 0, 0, 128},
			new float[] {0, 1, 0},
			new float[] {0, 0, 1},
			new int[] {0, 0, 0},
			new int[] {0, 1, 2},
			new int[] {-1},
			new int[] {0},
			new Renderer3DModelKind[] {Renderer3DModelKind.WALL},
			0, 1, 0, 1L);
		expect(
			chunk.getTriangleMaterialFamily(0),
			Renderer3DMaterialFamily.WALL,
			"chunk fallback normalization");
		if (chunk.getMaterialFamilyTriangleCount(Renderer3DMaterialFamily.WALL) != 1) {
			throw new AssertionError("chunk family count missing");
		}
	}
}
"""


def main() -> None:
    sources = {
        "classifier": ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DMaterialClassifier.java",
        "chunk": ROOT / "Client_Base/src/orsc/graphics/three/Renderer3DWorldChunkFrame.java",
        "world": ROOT / "Client_Base/src/orsc/graphics/three/World.java",
        "model": ROOT / "Client_Base/src/orsc/graphics/three/RSModel.java",
        "chunk_renderer": ROOT / "PC_Client/src/orsc/OpenGLWorldChunkRenderer.java",
        "shader": ROOT / "PC_Client/src/orsc/OpenGLShaderProgram.java",
        "capture": ROOT / "PC_Client/src/orsc/OpenGLFrameCapture.java",
        "telemetry": ROOT / "Client_Base/src/orsc/RenderTelemetry.java",
    }
    text = {name: path.read_text(encoding="utf-8") for name, path in sources.items()}
    required = {
        ("classifier", "classifyTerrain(boolean waterLike, boolean emissive)"):
            "terrain classifier precedence",
        ("classifier", "isMineableOre"):
            "ore classifier",
        ("chunk", "private Renderer3DMaterialFamily[] triangleMaterialFamilies;"):
            "chunk family storage",
        ("chunk", "getTriangleMaterialFamily"):
            "chunk family accessor",
        ("world", "family.getShaderId()"):
            "world signature family ownership",
        ("model", "family.getShaderId()"):
            "object signature family ownership",
        ("chunk_renderer", "MATERIAL_FAMILY_OFFSET_BYTES"):
            "resident VBO family attribute",
        ("shader", 'gl.glBindAttribLocation(program, MATERIAL_FAMILY_ATTRIBUTE_LOCATION, "aMaterialFamily")'):
            "stable shader family input",
        ("capture", 'new File(directory, "resident-material-families.tsv")'):
            "capture family evidence",
        ("telemetry", "openGLWorldMaterialFoliageStats"):
            "structured family telemetry",
    }
    for (source_name, needle), label in required.items():
        if needle not in text[source_name]:
            raise AssertionError(f"missing {label}: {needle!r}")

    if not CLIENT_JAR.exists() or FAMILY_SOURCE.stat().st_mtime > CLIENT_JAR.stat().st_mtime:
        build_result = subprocess.run(
            [str(ROOT / "scripts/build-client.sh")],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if build_result.returncode != 0:
            raise AssertionError(build_result.stdout + build_result.stderr)
    with tempfile.TemporaryDirectory(prefix="renderer-material-family-") as tmp_name:
        tmp = Path(tmp_name)
        source = tmp / "RendererMaterialFamilyFixture.java"
        source.write_text(JAVA_SOURCE, encoding="utf-8")
        compile_result = subprocess.run(
            [
                "javac",
                "-source",
                "1.8",
                "-target",
                "1.8",
                "-cp",
                str(CLIENT_JAR),
                "-d",
                str(tmp),
                str(source),
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if compile_result.returncode != 0:
            raise AssertionError(compile_result.stdout + compile_result.stderr)
        run_result = subprocess.run(
            ["java", "-cp", f"{tmp}:{CLIENT_JAR}", "RendererMaterialFamilyFixture"],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if run_result.returncode != 0:
            raise AssertionError(run_result.stdout + run_result.stderr)
    print("PASS: renderer material-family contract and classifier are stable")


if __name__ == "__main__":
    main()
