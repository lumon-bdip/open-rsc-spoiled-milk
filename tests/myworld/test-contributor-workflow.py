#!/usr/bin/env python3
"""Integration coverage for the portable external-contributor workflow."""

from __future__ import annotations

import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


SOURCE_ROOT = Path(__file__).resolve().parents[2]
TOOL = SOURCE_ROOT / "scripts" / "contributor-workspace.py"
HOOK = SOURCE_ROOT / ".githooks" / "pre-push"
GUIDE = SOURCE_ROOT / "docs" / "workspaces" / "external-contributor.md"
AGENTS = SOURCE_ROOT / "AGENTS.md"
PULL_REQUEST_TEMPLATE = SOURCE_ROOT / ".github" / "pull_request_template.md"


class ContributorFixture:
    def __init__(self) -> None:
        self.temp = tempfile.TemporaryDirectory(prefix="contributor-workflow-test-")
        self.base = Path(self.temp.name)
        self.remote = self.base / "remote.git"
        self.seed = self.base / "seed"
        self.clone = self.base / "contributor"

        self.run(["git", "init", "--bare", "--initial-branch=main", str(self.remote)])
        self.seed.mkdir()
        self.git(self.seed, "init", "--initial-branch=main")
        self.configure_identity(self.seed)
        hook_copy = self.seed / ".githooks" / "pre-push"
        hook_copy.parent.mkdir(parents=True)
        shutil.copy2(HOOK, hook_copy)
        (self.seed / "README.md").write_text("fixture\n", encoding="utf-8")
        self.git(self.seed, "add", ".")
        self.git(self.seed, "commit", "-m", "Initial fixture")
        self.initial_main = self.git(self.seed, "rev-parse", "HEAD").stdout.strip()
        self.git(self.seed, "remote", "add", "origin", str(self.remote))
        self.git(self.seed, "push", "--set-upstream", "origin", "main")

        self.run(["git", "clone", "--branch", "main", str(self.remote), str(self.clone)])
        self.configure_identity(self.clone)

    def close(self) -> None:
        self.temp.cleanup()

    @staticmethod
    def run(command: list[str], *, cwd: Path | None = None, check: bool = True) -> subprocess.CompletedProcess[str]:
        result = subprocess.run(command, cwd=cwd, capture_output=True, text=True)
        if check and result.returncode != 0:
            raise AssertionError(
                f"{' '.join(command)} failed ({result.returncode}):\n"
                f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
            )
        return result

    def git(self, root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return self.run(["git", "-C", str(root), *arguments], check=check)

    def configure_identity(self, root: Path) -> None:
        self.git(root, "config", "user.name", "Goutan")
        self.git(root, "config", "user.email", "goutan@example.test")

    def tool(self, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return self.run(["python3", str(TOOL), *arguments], cwd=self.clone, check=check)

    def setup(self) -> None:
        self.tool("setup", "--username", "Goutan", "--remote", "origin")

    def remote_head(self, branch: str) -> str:
        result = self.git(self.clone, "ls-remote", "--heads", "origin", f"refs/heads/{branch}")
        return result.stdout.split()[0] if result.stdout.strip() else ""


class ContributorWorkflowTest(unittest.TestCase):
    def setUp(self) -> None:
        self.fixture = ContributorFixture()

    def tearDown(self) -> None:
        self.fixture.close()

    def test_setup_checkpoint_and_exact_handoff(self) -> None:
        f = self.fixture
        f.setup()
        self.assertEqual(f.git(f.clone, "config", "--local", "core.hooksPath").stdout.strip(), ".githooks")
        self.assertEqual(
            f.git(f.clone, "config", "--local", "spoiledMilkContributor.branchPrefix").stdout.strip(),
            "goutan",
        )

        f.tool("start", "fix/collision-definitions")
        branch = f.git(f.clone, "branch", "--show-current").stdout.strip()
        self.assertEqual(branch, "goutan/fix/collision-definitions")
        (f.clone / "collision.txt").write_text("checkpoint\n", encoding="utf-8")
        f.tool("checkpoint", "-m", "Checkpoint collision fix")
        checkpoint_head = f.git(f.clone, "rev-parse", "HEAD").stdout.strip()
        self.assertEqual(f.remote_head(branch), checkpoint_head)
        self.assertEqual(f.remote_head("main"), f.initial_main)

        (f.clone / "collision.txt").write_text("ready\n", encoding="utf-8")
        handoff = f.tool(
            "handoff",
            "-m",
            "Finish collision fix",
            "--tests",
            "client build; manual pathing check",
        )
        ready_head = f.git(f.clone, "rev-parse", "HEAD").stdout.strip()
        self.assertEqual(f.remote_head(branch), ready_head)
        self.assertIn("SPOILED MILK CONTRIBUTOR HANDOFF: READY", handoff.stdout)
        self.assertIn(f"Branch: {branch}", handoff.stdout)
        self.assertIn(f"Commit: {ready_head}", handoff.stdout)
        self.assertIn("Open or update a pull request targeting main", handoff.stdout)

        state = json.loads((f.clone / ".git" / "spoiled-milk-contributor" / "state.json").read_text())
        self.assertEqual(state["phase"], "READY")
        self.assertEqual(state["head"], ready_head)
        status = f.tool("status").stdout
        self.assertIn("Remote backup: EXACT", status)
        self.assertIn("Handoff phase: READY", status)
        self.assertIn("Public server/release access: NOT PART", status)

        (f.clone / "collision.txt").write_text("post-handoff edit\n", encoding="utf-8")
        dirty_status = f.tool("status").stdout
        self.assertIn("Working folder: HAS UNSAVED CHANGES", dirty_status)
        self.assertIn("Handoff phase: ACTIVE", dirty_status)
        (f.clone / "collision.txt").write_text("ready\n", encoding="utf-8")

        f.git(f.seed, "fetch", "origin", branch)
        f.git(f.seed, "merge", "--no-ff", f"origin/{branch}", "-m", "Merge contributor fixture")
        f.git(f.seed, "push", "origin", "main")
        f.git(f.seed, "push", "origin", "--delete", branch)
        f.tool("start", "fix/follow-up")
        self.assertEqual(
            f.git(f.clone, "branch", "--show-current").stdout.strip(),
            "goutan/fix/follow-up",
        )

    def test_main_push_is_blocked_and_unique_main_cannot_start_task(self) -> None:
        f = self.fixture
        f.setup()
        (f.clone / "wrong-place.txt").write_text("must be rescued\n", encoding="utf-8")
        f.git(f.clone, "add", "wrong-place.txt")
        f.git(f.clone, "commit", "-m", "Accidental main work")

        push = f.git(f.clone, "push", "origin", "main", check=False)
        self.assertNotEqual(push.returncode, 0)
        self.assertIn("must never push directly to main", push.stderr)
        self.assertEqual(f.remote_head("main"), f.initial_main)

        start = f.tool("start", "fix/another-task", check=False)
        self.assertNotEqual(start.returncode, 0)
        self.assertIn("Run rescue", start.stderr)

    def test_sensitive_checkpoint_is_left_staged_and_never_pushed(self) -> None:
        f = self.fixture
        f.setup()
        f.tool("start", "fix/sensitive-guard")
        branch = f.git(f.clone, "branch", "--show-current").stdout.strip()
        initial_head = f.git(f.clone, "rev-parse", "HEAD").stdout.strip()
        (f.clone / ".env").write_text("SECRET=value\n", encoding="utf-8")

        blocked = f.tool("checkpoint", "-m", "Unsafe checkpoint", check=False)
        self.assertNotEqual(blocked.returncode, 0)
        self.assertIn("Checkpoint blocked", blocked.stderr)
        self.assertEqual(f.git(f.clone, "rev-parse", "HEAD").stdout.strip(), initial_head)
        self.assertEqual(f.remote_head(branch), "")
        self.assertIn(".env", f.git(f.clone, "diff", "--cached", "--name-only").stdout)

    def test_rescue_preserves_dirty_main_on_namespaced_remote_branch(self) -> None:
        f = self.fixture
        f.setup()
        (f.clone / "unfinished.txt").write_text("preserve me\n", encoding="utf-8")
        rescued = f.tool("rescue", "-m", "Rescue unfinished work")
        branch = f.git(f.clone, "branch", "--show-current").stdout.strip()
        self.assertTrue(branch.startswith("goutan/rescue/"))
        self.assertEqual(f.remote_head(branch), f.git(f.clone, "rev-parse", "HEAD").stdout.strip())
        self.assertIn("not marked READY", rescued.stdout)
        status = f.tool("status").stdout
        self.assertIn("Handoff phase: ACTIVE", status)

    def test_beginner_guide_and_pull_request_contract_remain_connected(self) -> None:
        guide = GUIDE.read_text(encoding="utf-8")
        agents = AGENTS.read_text(encoding="utf-8")
        pull_request = PULL_REQUEST_TEMPLATE.read_text(encoding="utf-8")
        script = TOOL.read_text(encoding="utf-8")

        for snippet in (
            "GitHub contributor: `Goutan`",
            "Operating system: Windows",
            "Expected AI sessions: one",
            "py -3 scripts/contributor-workspace.py setup --username Goutan --remote origin",
            "py -3 scripts/contributor-workspace.py checkpoint",
            "py -3 scripts/contributor-workspace.py handoff",
            "py -3 scripts/contributor-workspace.py rescue",
            "collect-contributor ai-N goutan/TYPE/task EXACT_COMMIT",
            "Do not use a stash, `git clean`, `git reset --hard`",
            "does not alter GitHub settings",
        ):
            self.assertIn(snippet, guide)
        self.assertIn("docs/workspaces/external-contributor.md", agents)
        self.assertIn("identify the session role and run its matching", agents)
        self.assertIn("py -3 scripts/contributor-workspace.py status", agents)
        self.assertIn("External AI contributors must not run", agents)
        self.assertIn("Exact 40-character commit", pull_request)
        self.assertNotIn("/home/justin", script)
        self.assertNotIn("/tmp/spoiled-milk-live-main", script)
        self.assertNotIn(b"\r\n", HOOK.read_bytes())
        self.assertIn(".githooks/* text eol=lf", (SOURCE_ROOT / ".gitattributes").read_text())


if __name__ == "__main__":
    unittest.main()
