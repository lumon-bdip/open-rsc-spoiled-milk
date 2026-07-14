#!/usr/bin/env python3
import hashlib
import os
import shutil
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PACKAGER = ROOT / "scripts/package-world-builder-release.sh"
VERSION = "v0.1.0-alpha.1"
PACKAGE_ROOT = "Spoiled Milk World Builder"
NATIVE_ENTRIES = (
    "linux/x64/org/lwjgl/liblwjgl.so",
    "linux/x64/org/lwjgl/glfw/libglfw.so",
    "linux/x64/org/lwjgl/opengl/liblwjgl_opengl.so",
    "windows/x64/org/lwjgl/lwjgl.dll",
    "windows/x64/org/lwjgl/glfw/glfw.dll",
    "windows/x64/org/lwjgl/opengl/lwjgl_opengl.dll",
)


def write(path: Path, contents: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(contents, encoding="utf-8")


def make_jar(path: Path, entries: tuple[str, ...]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as archive:
        for entry in entries:
            archive.writestr(entry, b"fixture")


def git(root: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=root, text=True, capture_output=True, check=True
    )
    return result.stdout.strip()


def make_fixture(root: Path, resolved_icons: bool = True) -> Path:
    make_jar(
        root / "Client_Base/Open_RSC_Client.jar",
        (
            "orsc/WorldBuilderClientProfile.class",
            "myworld-assets/ui/world-editor/action-save.png",
            *NATIVE_ENTRIES,
        ),
    )
    make_jar(
        root / "server/core.jar",
        (
            "com/openrsc/server/content/worldedit/WorldEditStorageContext.class",
            "com/openrsc/server/content/worldedit/WorldBuilderRuntimeControl.class",
        ),
    )
    make_jar(root / "server/plugins.jar", ("fixture/Plugin.class",))
    make_jar(
        root / "output/world-builder-tools/world-builder-tools.jar",
        ("com/openrsc/worldbuilder/WorldBuilderCli.class",),
    )
    write(root / "Client_Base/Cache/audio/audio.dat", "audio")
    write(root / "Client_Base/Cache/video/library.orsc", "library")
    write(root / "Client_Base/Cache/video/Custom_Landscape.orsc", "terrain")
    write(root / "Client_Base/Cache/config.txt", "Menus:1\n")
    write(root / "Client_Base/Cache/credentials.txt", "must-not-ship")
    write(root / "Client_Base/Cache/uid.dat", "must-not-ship")
    write(root / "Client_Base/src/orsc/Config.java", "CLIENT_VERSION = 10046;\n")
    write(root / "server/lib/runtime.jar", "runtime")
    write(root / "server/conf/server/data/Custom_Landscape.orsc", "terrain")
    write(root / "server/conf/server/defs/TileDef.xml", "<tiles/>\n")
    write(root / "server/database/sqlite/core.sqlite", "queries")
    write(root / "server/inc/sqlite/myworld_seed.db", "clean-seed")
    write(root / "server/myworld.conf", "\tclient_version: 10046\n")
    for name in ("alertwords.txt", "badwords.txt", "goodwords.txt", "ipbans.txt"):
        write(root / "server" / name, "\n")
    write(root / "server/globalrules.txt", "rules\n")
    write(root / "tools/world-builder/schema/project.json", "{}\n")
    shutil.copytree(ROOT / "release/world-builder", root / "release/world-builder")
    write(root / "release/player/ASSET-SOURCES.txt", "player assets resolved\n")
    write(root / "LICENSE", "AGPL fixture\n")
    credits = (
        "All editor icons | Project owner | Confirmed original work | redistribution permitted\n"
        if resolved_icons
        else "All editor icons | Pending confirmation | not release-ready\n"
    )
    write(root / "dev/myworld/assets/ui/world-editor/CREDITS.md", credits)

    runtime = root / "temurin-jre"
    write(runtime / "bin/java.exe", "runtime")
    write(runtime / "release", 'JAVA_VERSION="17.0.13"\n')
    write(runtime / "NOTICE", "runtime notice\n")
    write(runtime / "legal/java.base/LICENSE", "runtime license\n")
    write(root / ".gitignore", "output/\n")

    git(root, "init", "--initial-branch=main")
    git(root, "config", "user.name", "World Builder Release Test")
    git(root, "config", "user.email", "world-builder-test@example.invalid")
    git(root, "add", "--all")
    git(root, "commit", "-m", "Create World Builder release fixture")
    git(root, "remote", "add", "spoiled-milk", "https://example.invalid/spoiled-milk.git")
    git(root, "update-ref", "refs/remotes/spoiled-milk/main", "HEAD")
    return runtime


def run_packager(root: Path, runtime: Path) -> subprocess.CompletedProcess[str]:
    env = dict(os.environ)
    env["ROOT_DIR"] = str(root)
    env["SPOILED_MILK_WORLD_BUILDER_RELEASE_TEST_MODE"] = "1"
    return subprocess.run(
        [
            "bash",
            str(PACKAGER),
            "--version",
            VERSION,
            "--windows-jre",
            str(runtime),
            "--assets-cleared",
            "--skip-build",
        ],
        cwd=ROOT,
        env=env,
        text=True,
        capture_output=True,
    )


class WorldBuilderReleaseTest(unittest.TestCase):
    def test_packager_rejects_unresolved_icon_provenance(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-release-credits-") as temp:
            fixture = Path(temp)
            runtime = make_fixture(fixture, resolved_icons=False)
            result = run_packager(fixture, runtime)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("icon provenance is unresolved", result.stderr)

    def test_packager_is_manager_main_only(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-release-branch-") as temp:
            fixture = Path(temp)
            runtime = make_fixture(fixture)
            git(fixture, "switch", "-c", "feature-test")
            result = run_packager(fixture, runtime)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("manager branch main", result.stderr)

    def test_archives_are_clean_complete_and_launchable_without_repo_paths(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-release-package-") as temp:
            fixture = Path(temp) / "fixture"
            fixture.mkdir()
            runtime = make_fixture(fixture)
            result = run_packager(fixture, runtime)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            source_commit = git(fixture, "rev-parse", "HEAD")
            output = fixture / "output/releases/world-builder" / VERSION
            java_archive = output / f"spoiled-milk-world-builder-{VERSION}-java.zip"
            windows_archive = output / f"spoiled-milk-world-builder-{VERSION}-windows-x64.zip"
            checksums = output / "SHA256SUMS.txt"
            for artifact in (java_archive, windows_archive, checksums):
                self.assertTrue(artifact.is_file(), artifact)

            expected_checksums = {
                path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                for path in (java_archive, windows_archive)
            }
            checksum_text = checksums.read_text(encoding="utf-8")
            for name, digest in expected_checksums.items():
                self.assertIn(f"{digest}  {name}", checksum_text)

            for archive_path, windows in ((java_archive, False), (windows_archive, True)):
                with zipfile.ZipFile(archive_path) as archive:
                    names = set(archive.namelist())
                    prefix = f"{PACKAGE_ROOT}/"
                    required = {
                        prefix + "Start World Builder.sh",
                        prefix + "Start World Builder.cmd",
                        prefix + "Import Map Changes.sh",
                        prefix + "Import Map Changes.cmd",
                        prefix + "Undo Last Map Import.sh",
                        prefix + "Undo Last Map Import.cmd",
                        prefix + "README.txt",
                        prefix + "VERSION.txt",
                        prefix + "SOURCE-COMMIT.txt",
                        prefix + "LICENSE",
                        prefix + "ASSET-SOURCES.txt",
                        prefix + "PLAYER-ASSET-SOURCES.txt",
                        prefix + "EDITOR-ICON-CREDITS.txt",
                        prefix + "builder-runtime/Client_Base/Open_RSC_Client.jar",
                        prefix + "builder-runtime/server/core.jar",
                        prefix + "builder-runtime/server/plugins.jar",
                        prefix + "builder-runtime/server/inc/sqlite/myworld_seed.db",
                        prefix + "builder-runtime/launcher/world-builder-tools.jar",
                    }
                    self.assertFalse(required - names, required - names)
                    if windows:
                        self.assertIn(prefix + "runtime/bin/java.exe", names)
                        self.assertIn(prefix + "runtime/release", names)
                    else:
                        self.assertFalse(any(name.startswith(prefix + "runtime/") for name in names))
                    forbidden = (
                        "/workspace/", "/exports/", "/backups/", "/receipts/", "/logs/",
                        "world_builder.db", "world-builder.credential", "credentials.txt",
                        "uid.dat", "clientSettings.conf", "/ip.txt", "/port.txt",
                    )
                    self.assertFalse(
                        [name for name in names if any(fragment in name for fragment in forbidden)]
                    )
                    self.assertEqual(
                        f"{VERSION}\n", archive.read(prefix + "VERSION.txt").decode()
                    )
                    self.assertEqual(
                        f"{source_commit}\n",
                        archive.read(prefix + "SOURCE-COMMIT.txt").decode(),
                    )
                    readme = archive.read(prefix + "README.txt").decode()
                    self.assertIn(VERSION, readme)
                    self.assertIn(source_commit, readme)
                    self.assertNotIn("@VERSION@", readme)
                    start_cmd = archive.read(prefix + "Start World Builder.cmd").decode()
                    self.assertIn("launch --server-root", start_cmd)
                    self.assertIn("run --workspace", start_cmd)
                    import_cmd = archive.read(prefix + "Import Map Changes.cmd").decode()
                    self.assertIn("export-import", import_cmd)
                    undo_cmd = archive.read(prefix + "Undo Last Map Import.cmd").decode()
                    self.assertIn("undo-latest-import", undo_cmd)

            extracted = Path(temp) / "private-server"
            extracted.mkdir()
            with zipfile.ZipFile(java_archive) as archive:
                archive.extractall(extracted)
            package = extracted / PACKAGE_ROOT
            calls = Path(temp) / "java-calls.txt"
            fake_java = Path(temp) / "fake-java.sh"
            write(
                fake_java,
                "#!/usr/bin/env bash\n"
                "if [[ \"${1:-}\" == -version ]]; then exit 0; fi\n"
                "printf '%s\\n' \"$@\" > \"$FAKE_JAVA_CALLS\"\n",
            )
            fake_java.chmod(0o755)
            env = dict(os.environ)
            env["WORLD_BUILDER_JAVA"] = str(fake_java)
            env["WORLD_BUILDER_PORT"] = "44600"
            env["FAKE_JAVA_CALLS"] = str(calls)

            started = subprocess.run(
                ["bash", str(package / "Start World Builder.sh")],
                cwd=Path(temp), env=env, text=True, capture_output=True,
            )
            self.assertEqual(0, started.returncode, started.stdout + started.stderr)
            start_call = calls.read_text(encoding="utf-8")
            self.assertIn("launch\n", start_call)
            self.assertIn(str(extracted), start_call)
            self.assertIn("44600\n", start_call)

            write(package / "workspace/project-source.json", "{}\n")
            restarted = subprocess.run(
                ["bash", str(package / "Start World Builder.sh")],
                cwd=Path(temp), env=env, text=True, capture_output=True,
            )
            self.assertEqual(0, restarted.returncode, restarted.stdout + restarted.stderr)
            self.assertIn("run\n", calls.read_text(encoding="utf-8"))

            imported = subprocess.run(
                ["bash", str(package / "Import Map Changes.sh")],
                cwd=Path(temp), env=env, text=True, capture_output=True,
            )
            self.assertEqual(0, imported.returncode, imported.stdout + imported.stderr)
            import_call = calls.read_text(encoding="utf-8")
            self.assertIn("export-import\n", import_call)
            self.assertIn(VERSION, import_call)
            self.assertIn(source_commit, import_call)

            undone = subprocess.run(
                ["bash", str(package / "Undo Last Map Import.sh")],
                cwd=Path(temp), env=env, text=True, capture_output=True,
            )
            self.assertEqual(0, undone.returncode, undone.stdout + undone.stderr)
            self.assertIn("undo-latest-import\n", calls.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
