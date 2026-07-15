#!/usr/bin/env python3
"""Guard package-private client type ownership after the B06 extraction."""

from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import textwrap


ROOT = Path(__file__).resolve().parents[2]
CLIENT_ROOTS = (ROOT / "Client_Base/src", ROOT / "PC_Client/src")
EXPECTED_FILES = {
    "ButtonHandler": "Client_Base/src/com/openrsc/interfaces/misc/ButtonHandler.java",
    "Frame": "PC_Client/src/orsc/Frame.java",
    "FrameBuffer": "PC_Client/src/orsc/FrameBuffer.java",
    "FrameBufferPool": "PC_Client/src/orsc/FrameBufferPool.java",
    "KeyBinding": "PC_Client/src/orsc/KeyBinding.java",
    "WorldSpriteMatch": "PC_Client/src/orsc/WorldSpriteMatch.java",
    "WorldSpriteCommand": "PC_Client/src/orsc/WorldSpriteCommand.java",
    "CompositeWorldSpriteTexture": "PC_Client/src/orsc/CompositeWorldSpriteTexture.java",
    "StaticWorldCommand": "PC_Client/src/orsc/StaticWorldCommand.java",
    "StaticWorldCommandBuilder": "PC_Client/src/orsc/StaticWorldCommandBuilder.java",
    "StaticWorldMaterialTriangle": "PC_Client/src/orsc/StaticWorldMaterialTriangle.java",
    "WorldSpriteAnchorMatch": "PC_Client/src/orsc/WorldSpriteAnchorMatch.java",
    "LegacyEntitySpriteDebugStats": "PC_Client/src/orsc/LegacyEntitySpriteDebugStats.java",
    "LegacyEntitySpriteDepthEvaluation": "PC_Client/src/orsc/LegacyEntitySpriteDepthEvaluation.java",
    "EntitySpriteOccluderFaceStats": "PC_Client/src/orsc/EntitySpriteOccluderFaceStats.java",
    "OpenGLWorldChunkUploadStats": "PC_Client/src/orsc/OpenGLWorldChunkUploadStats.java",
    "ResidentChunkReadiness": "PC_Client/src/orsc/ResidentChunkReadiness.java",
    "OpenGLWorldChunkDrawStats": "PC_Client/src/orsc/OpenGLWorldChunkDrawStats.java",
    "ShaderVertexParityStats": "PC_Client/src/orsc/ShaderVertexParityStats.java",
    "RemasterGlowMaskBuild": "PC_Client/src/orsc/RemasterGlowMaskBuild.java",
    "RemasterGlowMask": "PC_Client/src/orsc/RemasterGlowMask.java",
    "RemasterShadowInventory": "PC_Client/src/orsc/RemasterShadowInventory.java",
    "RemasterShadowRoofCoverage": "PC_Client/src/orsc/RemasterShadowRoofCoverage.java",
    "RemasterShadowIndoorFlood": "PC_Client/src/orsc/RemasterShadowIndoorFlood.java",
    "RemasterShadowMaskBuild": "PC_Client/src/orsc/RemasterShadowMaskBuild.java",
    "RemasterTerrainShadowMask": "PC_Client/src/orsc/RemasterTerrainShadowMask.java",
}
TOP_LEVEL_TYPE = re.compile(
    r"(?m)^(?:(?:abstract|final|public|protected|private|static)\s+)*"
    r"(?:class|enum|interface)\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"
)


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def java_sources() -> list[Path]:
    return sorted(path for root in CLIENT_ROOTS for path in root.rglob("*.java"))


def verify_type_ownership() -> None:
    sources = java_sources()
    source_text = {path: path.read_text(encoding="utf-8") for path in sources}
    declarations: dict[str, list[Path]] = {}
    for path, text in source_text.items():
        for name in TOP_LEVEL_TYPE.findall(text):
            declarations.setdefault(name, []).append(path)

    for name, relative_path in EXPECTED_FILES.items():
        expected = ROOT / relative_path
        require(expected.is_file(), f"missing extracted type file: {relative_path}")
        require(expected.stem == name, f"type {name} is not in a correctly named file")
        package_match = re.search(r"(?m)^package\s+([^;]+);", source_text[expected])
        require(package_match is not None, f"missing package for {relative_path}")
        expected_package = package_match.group(1)
        same_package = []
        for path in declarations.get(name, []):
            candidate_package = re.search(r"(?m)^package\s+([^;]+);", source_text[path])
            if candidate_package is not None and candidate_package.group(1) == expected_package:
                same_package.append(path)
        require(
            same_package == [expected],
            f"type {name} must have one top-level declaration in {relative_path}",
        )

    externally_used_auxiliary_types: list[str] = []
    for name, paths in declarations.items():
        for declaration_path in paths:
            if declaration_path.stem == name:
                continue
            reference = re.compile(r"\b" + re.escape(name) + r"\b")
            if any(
                reference.search(text)
                for path, text in source_text.items()
                if path != declaration_path
            ):
                externally_used_auxiliary_types.append(
                    f"{name} in {declaration_path.relative_to(ROOT)}"
                )
    require(
        not externally_used_auxiliary_types,
        "externally used auxiliary types remain: "
        + ", ".join(externally_used_auxiliary_types),
    )


def run_behavior_fixtures() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")

    button_fixture = textwrap.dedent(
        """
        package com.openrsc.interfaces.misc;

        public final class ButtonHandlerFixture {
            public static void main(String[] args) {
                final int[] clicks = new int[1];
                ButtonHandler handler = new ButtonHandler() {
                    @Override
                    void handle() {
                        clicks[0]++;
                    }
                };
                handler.handle();
                if (clicks[0] != 1) {
                    throw new AssertionError("button callback changed");
                }
            }
        }
        """
    )
    renderer_fixture = textwrap.dedent(
        """
        package orsc;

        import java.awt.event.KeyEvent;

        public final class AuxiliaryRendererFixture {
            public static void main(String[] args) {
                KeyBinding letter = new KeyBinding(65, KeyEvent.VK_A, 'a', 'A');
                if (letter.keyChar(false) != 'a' || letter.keyChar(true) != 'A'
                        || !letter.isLetter() || letter.postsPhysicalEvents()) {
                    throw new AssertionError("key binding behavior changed");
                }
                KeyBinding backspace = new KeyBinding(
                    8, KeyEvent.VK_BACK_SPACE, '\b', '\b');
                if (!backspace.postsPhysicalEvents() || !backspace.postsRepeatPressEvents()) {
                    throw new AssertionError("backspace behavior changed");
                }

                FrameBufferPool pool = new FrameBufferPool();
                FrameBuffer first = pool.acquire(32);
                first.buffer.putInt(1234);
                pool.release(first);
                FrameBuffer reused = pool.acquire(16);
                if (reused != first || reused.buffer.position() != 0 || reused.capacity() < 32) {
                    throw new AssertionError("frame-buffer reuse behavior changed");
                }
                pool.release(reused);
            }
        }
        """
    )

    with tempfile.TemporaryDirectory(prefix="client-auxiliary-types-") as directory:
        temp = Path(directory)
        button_path = temp / "ButtonHandlerFixture.java"
        renderer_path = temp / "AuxiliaryRendererFixture.java"
        button_path.write_text(button_fixture, encoding="utf-8")
        renderer_path.write_text(renderer_fixture, encoding="utf-8")
        compile_result = subprocess.run(
            [
                javac,
                "-source",
                "8",
                "-target",
                "8",
                "-d",
                str(temp),
                str(ROOT / EXPECTED_FILES["ButtonHandler"]),
                str(button_path),
                str(ROOT / EXPECTED_FILES["FrameBuffer"]),
                str(ROOT / EXPECTED_FILES["FrameBufferPool"]),
                str(ROOT / EXPECTED_FILES["KeyBinding"]),
                str(renderer_path),
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        require(compile_result.returncode == 0, f"fixture compile failed:\n{compile_result.stderr}")
        for fixture_class in (
            "com.openrsc.interfaces.misc.ButtonHandlerFixture",
            "orsc.AuxiliaryRendererFixture",
        ):
            run_result = subprocess.run(
                [java, "-cp", str(temp), fixture_class],
                cwd=ROOT,
                capture_output=True,
                text=True,
            )
            require(run_result.returncode == 0, f"{fixture_class} failed:\n{run_result.stderr}")


def main() -> None:
    verify_type_ownership()
    run_behavior_fixtures()
    print("PASS: client auxiliary types have stable ownership and behavior")


if __name__ == "__main__":
    main()
