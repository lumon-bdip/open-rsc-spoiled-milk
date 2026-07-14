#!/usr/bin/env python3
import hashlib
import json
import socket
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "tools/world-builder/src"
MAIN_CLASS = "com.openrsc.worldbuilder.WorldBuilderCli"
COMMIT = "b" * 40
AUTHORED = (
    "server/conf/server/data/Custom_Landscape.orsc",
    "Client_Base/Cache/video/Custom_Landscape.orsc",
    "server/conf/server/defs/locs/MyWorldSceneryLocs.json",
    "server/conf/server/defs/locs/MyWorldSceneryRemovals.json",
    "server/conf/server/defs/locs/MyWorldNpcLocs.json",
    "server/conf/server/defs/locs/MyWorldNpcRemovals.json",
)
CONTENT = (
    "server/conf/server/defs/TileDef.xml",
    "server/conf/server/defs/GameObjectDef.xml",
    "server/conf/server/defs/NpcDefs.json",
    "server/conf/server/defs/NpcDefsCustom.json",
    "server/conf/server/defs/NpcDefsMyWorld.json",
    "server/conf/server/defs/NpcDefsPatch18.json",
    "Client_Base/Cache/video/library.orsc",
)
EMPTY_OVERLAYS = {
    AUTHORED[2]: '{"sceneries": []}\n',
    AUTHORED[3]: '{"scenery_removals": []}\n',
    AUTHORED[4]: '{"npclocs": []}\n',
    AUTHORED[5]: '{"npc_removals": []}\n',
}


class WorldBuilderImportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.compile_temp = tempfile.TemporaryDirectory(prefix="world-builder-import-classes-")
        cls.classes = Path(cls.compile_temp.name)
        sources = sorted(str(path) for path in SOURCE_ROOT.rglob("*.java"))
        subprocess.run(
            ["javac", "-source", "8", "-target", "8", "-d", str(cls.classes), *sources],
            cwd=ROOT,
            check=True,
            capture_output=True,
            text=True,
        )

    @classmethod
    def tearDownClass(cls):
        cls.compile_temp.cleanup()

    @staticmethod
    def write_archive(path: Path, seed: int):
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = bytes((seed + index * 7) & 0xFF for index in range(48 * 48 * 10))
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("h0x48y37", raw)

    def make_layout(self, root: Path, seed: int, overlays=True, port=43594):
        config = root / "server/myworld.conf"
        config.parent.mkdir(parents=True, exist_ok=True)
        config.write_text(
            "\n".join(
                (
                    "server_name: Fixture",
                    "server_bind_address: 0.0.0.0",
                    f"server_port: {port}",
                    "max_players: 100",
                    "client_version: 10046",
                    "member_world: true",
                    "based_map_data: 64",
                    "want_myworld: true",
                    "custom_landscape: true",
                    "want_packet_register: true",
                    "",
                )
            ),
            encoding="utf-8",
        )
        self.write_archive(root / AUTHORED[0], seed)
        (root / AUTHORED[1]).parent.mkdir(parents=True, exist_ok=True)
        (root / AUTHORED[1]).write_bytes((root / AUTHORED[0]).read_bytes())
        if overlays:
            for relative, content in EMPTY_OVERLAYS.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8")
        else:
            (root / AUTHORED[2]).parent.mkdir(parents=True, exist_ok=True)
        for index, relative in enumerate(CONTENT):
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(f"compatible-definition-{index}\n".encode())

    def make_runtime(self, root: Path):
        self.make_layout(root, 3, port=43595)
        for relative, data in {
            "server/core.jar": b"core",
            "server/plugins.jar": b"plugins",
            "server/alertwords.txt": b"\n",
            "server/badwords.txt": b"\n",
            "server/goodwords.txt": b"\n",
            "server/globalrules.txt": b"rules\n",
            "server/ipbans.txt": b"\n",
            "server/lib/runtime.jar": b"library",
            "server/database/sqlite/core.sqlite": b"query definitions",
            "server/inc/sqlite/myworld_seed.db": b"clean-seed-database",
            "Client_Base/Open_RSC_Client.jar": b"client",
        }.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)

    @staticmethod
    def snapshot(root: Path):
        result = {}
        for path in sorted(root.rglob("*")):
            if path.is_file() and not path.is_symlink():
                result[path.relative_to(root).as_posix()] = (
                    path.stat().st_size,
                    path.stat().st_mtime_ns,
                    hashlib.sha256(path.read_bytes()).hexdigest(),
                )
        return result

    def run_cli(self, *args):
        return subprocess.run(
            ["java", "-cp", str(self.classes), MAIN_CLASS, *map(str, args)],
            cwd=ROOT,
            text=True,
            capture_output=True,
            timeout=20,
        )

    def prepared_export(self, base: Path, overlays=True):
        target, runtime, workspace = base / "target", base / "runtime", base / "project"
        self.make_layout(target, 11, overlays=overlays)
        self.make_runtime(runtime)
        prepared = self.run_cli(
            "prepare", "--server-root", target, "--runtime-root", runtime,
            "--workspace", workspace, "--port", "43615",
        )
        self.assertEqual(0, prepared.returncode, prepared.stderr)
        working = workspace / "working"
        self.write_archive(working / AUTHORED[0], 27)
        (working / AUTHORED[1]).write_bytes((working / AUTHORED[0]).read_bytes())
        if overlays:
            (working / AUTHORED[2]).write_text(
                '{"sceneries":[{"id":10,"pos":{"X":100,"Y":200},"direction":2}]}\n',
                encoding="utf-8",
            )
        exported = self.run_cli(
            "export", "--workspace", workspace, "--builder-version", "v-test",
            "--source-commit", COMMIT,
        )
        self.assertEqual(0, exported.returncode, exported.stderr)
        export_dir = Path(json.loads(exported.stdout)["exportDirectory"])
        return target, workspace, export_dir

    def preview(self, workspace: Path, export_dir: Path, target: Path):
        return self.run_cli(
            "import", "--workspace", workspace, "--export", export_dir,
            "--target-root", target, "--dry-run",
        )

    def test_dry_run_reports_exact_actions_without_target_writes(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-import-preview-") as temp:
            target, workspace, export_dir = self.prepared_export(Path(temp))
            before = self.snapshot(target)

            result = self.preview(workspace, export_dir, target)

            self.assertEqual(0, result.returncode, result.stderr)
            preview = json.loads(result.stdout)
            self.assertEqual("ready", preview["status"])
            self.assertTrue(preview["dryRun"])
            self.assertEqual([], preview["configurationChanges"])
            self.assertEqual(
                [
                    "server/conf/server/data/Custom_Landscape.orsc",
                    "Client_Base/Cache/video/Custom_Landscape.orsc",
                    "server/conf/server/defs/locs/MyWorldSceneryLocs.json",
                ],
                [action["relativePath"] for action in preview["actions"]],
            )
            self.assertTrue(all(action["operation"] == "replace" for action in preview["actions"]))
            self.assertEqual(before, self.snapshot(target))
            self.assertFalse((workspace / "backups").exists())
            self.assertFalse((workspace / "receipts").exists())

    def test_absent_unchanged_overlays_are_not_planned_as_additions(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-import-absent-") as temp:
            target, workspace, export_dir = self.prepared_export(Path(temp), overlays=False)
            result = self.preview(workspace, export_dir, target)
            self.assertEqual(0, result.returncode, result.stderr)
            preview = json.loads(result.stdout)
            self.assertEqual(2, len(preview["actions"]))
            self.assertTrue(all("Landscape" in action["relativePath"] for action in preview["actions"]))
            self.assertFalse(any((target / relative).exists() for relative in AUTHORED[2:]))

    def test_target_revision_drift_is_refused(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-import-drift-") as temp:
            target, workspace, export_dir = self.prepared_export(Path(temp))
            (target / AUTHORED[3]).write_text(
                '{"scenery_removals":[{"pos":{"X":1,"Y":2}}]}\n', encoding="utf-8"
            )
            changed = self.snapshot(target)
            result = self.preview(workspace, export_dir, target)
            self.assertEqual(3, result.returncode)
            self.assertIn("changed since this Builder project was created", result.stderr)
            self.assertEqual(changed, self.snapshot(target))

    def test_occupied_target_port_is_refused(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-import-running-") as temp:
            target, workspace, export_dir = self.prepared_export(Path(temp))
            before = self.snapshot(target)
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
                listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 0)
                listener.bind(("127.0.0.1", 43594))
                listener.listen(1)
                result = self.preview(workspace, export_dir, target)
            self.assertEqual(3, result.returncode)
            self.assertIn("offline state is uncertain", result.stderr)
            self.assertEqual(before, self.snapshot(target))


if __name__ == "__main__":
    unittest.main()
