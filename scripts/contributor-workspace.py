#!/usr/bin/env python3
"""Safe, portable checkpoint workflow for external Spoiled Milk contributors."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


CONFIG_SECTION = "spoiledMilkContributor"
ALLOWED_TYPES = {
    "art",
    "balance",
    "chore",
    "content",
    "docs",
    "feat",
    "feature",
    "fix",
    "idea",
    "refactor",
    "test",
}
MAX_CHECKPOINT_BYTES = 25 * 1024 * 1024
ZERO_COMMIT = "0" * 40
SENSITIVE_CONTENT = re.compile(
    rb"-----BEGIN ([A-Z0-9]+ )?PRIVATE KEY-----"
    rb"|AKIA[0-9A-Z]{16}"
    rb"|gh[pousr]_[A-Za-z0-9]{20,}"
    rb"|xox[baprs]-[A-Za-z0-9-]{20,}"
)


class WorkflowError(RuntimeError):
    """An expected safety refusal with a beginner-readable explanation."""


def run(
    command: list[str],
    *,
    cwd: Path | None = None,
    check: bool = True,
    text: bool = True,
) -> subprocess.CompletedProcess:
    result = subprocess.run(command, cwd=cwd, capture_output=True, text=text)
    if check and result.returncode != 0:
        stderr = result.stderr.strip() if text else result.stderr.decode(errors="replace").strip()
        stdout = result.stdout.strip() if text else result.stdout.decode(errors="replace").strip()
        detail = stderr or stdout or f"exit status {result.returncode}"
        raise WorkflowError(f"Command failed: {' '.join(command)}\n{detail}")
    return result


def git(root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return run(["git", "-C", str(root), *arguments], check=check)


def git_bytes(root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[bytes]:
    return run(["git", "-C", str(root), *arguments], check=check, text=False)


def repository_root() -> Path:
    result = run(["git", "rev-parse", "--show-toplevel"])
    return Path(result.stdout.strip()).resolve()


def config_get(root: Path, key: str, *, required: bool = True) -> str:
    result = git(root, "config", "--local", "--get", f"{CONFIG_SECTION}.{key}", check=False)
    value = result.stdout.strip()
    if required and not value:
        raise WorkflowError(
            "Contributor setup is incomplete. From the repository root, run:\n"
            "  py -3 scripts/contributor-workspace.py setup --username YOUR_GITHUB_NAME --remote origin"
        )
    return value


def config_set(root: Path, key: str, value: str) -> None:
    git(root, "config", "--local", f"{CONFIG_SECTION}.{key}", value)


def current_branch(root: Path) -> str:
    result = git(root, "symbolic-ref", "--quiet", "--short", "HEAD", check=False)
    return result.stdout.strip() or "DETACHED"


def current_head(root: Path) -> str:
    return git(root, "rev-parse", "HEAD").stdout.strip()


def dirty_status(root: Path) -> str:
    return git(root, "status", "--porcelain=v1", "--untracked-files=all").stdout


def require_clean(root: Path) -> None:
    status = dirty_status(root)
    if status:
        raise WorkflowError(
            "The working folder has unsaved changes. Preserve them before switching tasks:\n"
            "  py -3 scripts/contributor-workspace.py checkpoint -m \"Describe the saved work\"\n"
            "If the branch or state is confusing, use the rescue command instead.\n\n"
            f"Git status:\n{status.rstrip()}"
        )


def require_no_git_operation(root: Path) -> None:
    for marker in ("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "rebase-merge", "rebase-apply"):
        marker_path = git(root, "rev-parse", "--git-path", marker).stdout.strip()
        path = Path(marker_path)
        if not path.is_absolute():
            path = root / path
        if path.exists():
            raise WorkflowError(
                f"An unfinished Git operation ({marker}) exists. Do not reset or clean it. "
                "Ask the repository owner to help preserve and resolve this state."
            )


def remote_exists(root: Path, remote: str) -> bool:
    return remote in git(root, "remote").stdout.splitlines()


def fetch_main(root: Path, remote: str, main_branch: str) -> str:
    git(root, "fetch", remote, main_branch)
    ref = f"refs/remotes/{remote}/{main_branch}"
    result = git(root, "rev-parse", "--verify", f"{ref}^{{commit}}", check=False)
    if result.returncode != 0:
        raise WorkflowError(f"Unable to find {remote}/{main_branch} after fetching it.")
    return result.stdout.strip()


def remote_branch_head(root: Path, remote: str, branch: str) -> str:
    result = git(root, "ls-remote", "--heads", remote, f"refs/heads/{branch}")
    lines = [line for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        return ""
    return lines[0].split()[0]


def branch_prefix(username: str) -> str:
    prefix = re.sub(r"[^a-z0-9-]+", "-", username.lower()).strip("-")
    if not prefix:
        raise WorkflowError("The GitHub username cannot be converted into a safe branch prefix.")
    return prefix


def validate_topic_branch(root: Path, branch: str, prefix: str, *, allow_rescue: bool = False) -> None:
    result = git(root, "check-ref-format", "--branch", branch, check=False)
    if result.returncode != 0:
        raise WorkflowError(f"Invalid Git branch name: {branch}")
    parts = branch.split("/", 2)
    if len(parts) != 3 or parts[0] != prefix or not parts[2]:
        raise WorkflowError(
            f"Use a contributor topic branch such as {prefix}/fix/short-description. "
            "Do not work on main or a generic patch branch."
        )
    allowed = set(ALLOWED_TYPES)
    if allow_rescue:
        allowed.add("rescue")
    if parts[1] not in allowed:
        allowed_text = ", ".join(sorted(allowed))
        raise WorkflowError(f"Branch type '{parts[1]}' is not allowed. Choose one of: {allowed_text}")


def topic_branch_name(root: Path, requested: str, prefix: str) -> str:
    branch = requested if requested.startswith(f"{prefix}/") else f"{prefix}/{requested}"
    validate_topic_branch(root, branch, prefix)
    return branch


def state_path(root: Path) -> Path:
    raw_path = git(root, "rev-parse", "--git-path", "spoiled-milk-contributor/state.json").stdout.strip()
    path = Path(raw_path)
    return path if path.is_absolute() else root / path


def write_state(root: Path, **values: str) -> None:
    path = state_path(root)
    path.parent.mkdir(parents=True, exist_ok=True)
    values["updatedUtc"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    path.write_text(json.dumps(values, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def read_state(root: Path) -> dict[str, str]:
    path = state_path(root)
    if not path.is_file():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {}


def sensitive_path(path: str) -> bool:
    normalized = path.replace("\\", "/").lower()
    base = normalized.rsplit("/", 1)[-1]
    if base in {".env.example", ".env.sample", ".env.template"}:
        return False
    if base == ".env" or base.startswith(".env."):
        return True
    if base in {
        "credentials.json",
        "credentials.txt",
        "id_ed25519",
        "id_rsa",
        "secrets.json",
        "secrets.txt",
    }:
        return True
    return any(base.endswith(suffix) for suffix in (".jks", ".p12", ".pem", ".pfx"))


def review_staged_files(root: Path) -> None:
    names = git_bytes(root, "diff", "--cached", "--name-only", "--diff-filter=ACMR", "-z").stdout
    problems = []
    print("Staged checkpoint files:")
    for raw_name in names.split(b"\0"):
        if not raw_name:
            continue
        name = raw_name.decode("utf-8", errors="surrogateescape")
        size_result = git(root, "cat-file", "-s", f":{name}", check=False)
        size = int(size_result.stdout.strip()) if size_result.returncode == 0 else 0
        print(f"  {size:>10} bytes  {name}")
        if sensitive_path(name):
            problems.append(f"likely sensitive path: {name}")
        if size > MAX_CHECKPOINT_BYTES:
            problems.append(f"file exceeds the {MAX_CHECKPOINT_BYTES // (1024 * 1024)} MB limit: {name}")
        if 0 < size <= 1024 * 1024:
            content = git_bytes(root, "show", f":{name}", check=False).stdout
            if SENSITIVE_CONTENT.search(content):
                problems.append(f"likely credential content: {name}")

    if problems:
        details = "\n".join(f"  - {problem}" for problem in problems)
        raise WorkflowError(
            "Checkpoint blocked. Nothing was committed or pushed; the files remain staged for review.\n"
            f"{details}\nAsk the repository owner before proceeding."
        )


def push_exact_branch(root: Path, remote: str, branch: str) -> str:
    git(root, "push", "--set-upstream", remote, f"HEAD:refs/heads/{branch}")
    head = current_head(root)
    remote_head = remote_branch_head(root, remote, branch)
    if remote_head != head:
        raise WorkflowError(
            f"The push returned, but {remote}/{branch} is not the exact local commit. "
            "Stop and ask the repository owner to inspect the branch."
        )
    return head


def checkpoint(root: Path, message: str, *, phase: str = "ACTIVE", tests: str = "") -> tuple[str, str]:
    require_no_git_operation(root)
    prefix = config_get(root, "branchPrefix")
    remote = config_get(root, "remote")
    main_branch = config_get(root, "mainBranch")
    username = config_get(root, "username")
    branch = current_branch(root)
    validate_topic_branch(root, branch, prefix, allow_rescue=True)

    git(root, "add", "-A")
    staged = git(root, "diff", "--cached", "--quiet", check=False)
    if staged.returncode not in (0, 1):
        raise WorkflowError("Unable to inspect staged files.")
    if staged.returncode == 1:
        git(root, "diff", "--cached", "--check")
        review_staged_files(root)
        git(root, "commit", "-m", message)
    else:
        print("No file changes to commit; verifying the existing branch tip.")

    require_clean(root)
    head = push_exact_branch(root, remote, branch)
    write_state(
        root,
        phase=phase,
        username=username,
        branch=branch,
        head=head,
        remote=remote,
        mainBranch=main_branch,
        tests=tests,
    )
    print(f"{phase}: {branch} at {head}")
    return branch, head


def github_compare_url(root: Path, remote: str, main_branch: str, branch: str) -> str:
    remote_url = git(root, "remote", "get-url", remote).stdout.strip()
    match = re.match(r"https?://github\.com/([^/]+)/(.+)$", remote_url)
    if not match:
        match = re.match(r"git@github\.com:([^/]+)/(.+)$", remote_url)
    if not match:
        return ""
    owner, repository = match.groups()
    repository = repository.removesuffix(".git")
    return f"https://github.com/{owner}/{repository}/compare/{main_branch}...{branch}?expand=1"


def command_setup(root: Path, arguments: argparse.Namespace) -> None:
    if not re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})", arguments.username):
        raise WorkflowError("Enter the exact GitHub username, using only letters, numbers, and hyphens.")
    if not remote_exists(root, arguments.remote):
        remotes = ", ".join(git(root, "remote").stdout.splitlines()) or "none"
        raise WorkflowError(f"Remote '{arguments.remote}' does not exist. Available remotes: {remotes}")
    if git(root, "check-ref-format", "--branch", arguments.main_branch, check=False).returncode != 0:
        raise WorkflowError(f"Invalid main branch name: {arguments.main_branch}")

    effective_hooks = git(root, "config", "--get", "core.hooksPath", check=False).stdout.strip()
    if effective_hooks and effective_hooks.replace("\\", "/").rstrip("/") != ".githooks":
        raise WorkflowError(
            f"This clone already uses a custom Git hooks path ({effective_hooks}). "
            "Ask the repository owner to combine the safety hook instead of overwriting it."
        )
    hook = root / ".githooks" / "pre-push"
    if not hook.is_file():
        raise WorkflowError("The contributor main-protection hook is missing from this checkout.")

    for key in ("user.name", "user.email"):
        if not git(root, "config", "--get", key, check=False).stdout.strip():
            raise WorkflowError(
                f"Git identity '{key}' is not configured. Set it with GitHub's recommended value, "
                "then rerun setup."
            )

    fetch_main(root, arguments.remote, arguments.main_branch)
    config_set(root, "username", arguments.username)
    config_set(root, "branchPrefix", branch_prefix(arguments.username))
    config_set(root, "remote", arguments.remote)
    config_set(root, "mainBranch", arguments.main_branch)
    git(root, "config", "--local", "core.hooksPath", ".githooks")

    print("Contributor workflow configured locally for this clone.")
    print(f"  Contributor: {arguments.username}")
    print(f"  Topic prefix: {branch_prefix(arguments.username)}/")
    print(f"  Published base: {arguments.remote}/{arguments.main_branch}")
    print("  Main push protection: enabled")
    print("No GitHub permissions, credentials, or live-server access were added.")


def command_start(root: Path, arguments: argparse.Namespace) -> None:
    require_no_git_operation(root)
    require_clean(root)
    prefix = config_get(root, "branchPrefix")
    remote = config_get(root, "remote")
    main_branch = config_get(root, "mainBranch")
    branch = topic_branch_name(root, arguments.topic, prefix)
    fetch_main(root, remote, main_branch)
    remote_main_ref = f"refs/remotes/{remote}/{main_branch}"

    current = current_branch(root)
    if current != main_branch:
        if current == "DETACHED":
            raise WorkflowError("HEAD is detached. Use rescue before starting another task.")
        remote_head = remote_branch_head(root, remote, current)
        current_commit = current_head(root)
        merged_into_main = (
            git(root, "merge-base", "--is-ancestor", current_commit, remote_main_ref, check=False).returncode == 0
        )
        if remote_head != current_commit and not merged_into_main:
            raise WorkflowError(
                f"The current branch '{current}' is not backed up at its exact tip. "
                "Checkpoint or rescue it before starting another task."
            )

    local_main = git(root, "rev-parse", "--verify", f"refs/heads/{main_branch}^{{commit}}", check=False)
    if local_main.returncode != 0:
        git(root, "branch", main_branch, remote_main_ref)
    else:
        ahead = int(git(root, "rev-list", "--count", f"{remote_main_ref}..{main_branch}").stdout.strip())
        if ahead:
            raise WorkflowError(
                f"Local {main_branch} contains {ahead} commit(s) not in {remote}/{main_branch}. "
                "Run rescue before updating main."
            )

    git(root, "switch", main_branch)
    git(root, "merge", "--ff-only", remote_main_ref)

    if git(root, "show-ref", "--verify", "--quiet", f"refs/heads/{branch}", check=False).returncode == 0:
        raise WorkflowError(f"Local branch already exists: {branch}")
    if remote_branch_head(root, remote, branch):
        raise WorkflowError(f"Remote branch already exists: {remote}/{branch}")

    git(root, "switch", "-c", branch, remote_main_ref)
    head = current_head(root)
    write_state(
        root,
        phase="ACTIVE",
        username=config_get(root, "username"),
        branch=branch,
        head=head,
        remote=remote,
        mainBranch=main_branch,
        tests="",
    )
    print(f"Started {branch} from {remote}/{main_branch} at {head}.")
    print("Open this same repository folder in the single contributor AI session.")


def command_checkpoint(root: Path, arguments: argparse.Namespace) -> None:
    checkpoint(root, arguments.message)


def command_handoff(root: Path, arguments: argparse.Namespace) -> None:
    branch, head = checkpoint(root, arguments.message, phase="ACTIVE", tests=arguments.tests)
    remote = config_get(root, "remote")
    main_branch = config_get(root, "mainBranch")
    main_head = fetch_main(root, remote, main_branch)
    merge_base = git(root, "merge-base", head, main_head).stdout.strip()
    commit_count = int(git(root, "rev-list", "--count", f"{main_head}..{head}").stdout.strip())
    if commit_count <= 0:
        raise WorkflowError("The handed-off branch has no commits outside current published main.")
    if remote_branch_head(root, remote, branch) != head:
        raise WorkflowError("The remote branch changed while preparing the handoff. Stop and ask for review.")

    write_state(
        root,
        phase="READY",
        username=config_get(root, "username"),
        branch=branch,
        head=head,
        remote=remote,
        mainBranch=main_branch,
        publishedMain=main_head,
        mergeBase=merge_base,
        tests=arguments.tests,
    )
    compare_url = github_compare_url(root, remote, main_branch, branch)
    print("\nSPOILED MILK CONTRIBUTOR HANDOFF: READY")
    print(f"Contributor: {config_get(root, 'username')}")
    print(f"Branch: {branch}")
    print(f"Commit: {head}")
    print(f"Published main checked: {main_head}")
    print(f"Merge base: {merge_base}")
    print(f"Tests: {arguments.tests}")
    if compare_url:
        print(f"Open or update the pull request: {compare_url}")
    else:
        print("Open or update a pull request targeting main, then paste this handoff into it.")
    print("Send the Branch and Commit values to Justin for exact manager-AI collection.")


def command_rescue(root: Path, arguments: argparse.Namespace) -> None:
    require_no_git_operation(root)
    prefix = config_get(root, "branchPrefix")
    branch = current_branch(root)
    try:
        validate_topic_branch(root, branch, prefix, allow_rescue=True)
    except WorkflowError:
        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        branch = f"{prefix}/rescue/{timestamp}"
        git(root, "switch", "-c", branch)
        print(f"Created preservation branch {branch} before touching the work.")
    checkpoint(root, arguments.message)
    print("Rescue is backed up but not marked READY. Ask Justin to inspect it before cleanup.")


def command_status(root: Path, _arguments: argparse.Namespace) -> None:
    username = config_get(root, "username", required=False)
    remote = config_get(root, "remote", required=False)
    main_branch = config_get(root, "mainBranch", required=False)
    branch = current_branch(root)
    head = current_head(root)
    clean = not dirty_status(root)
    state = read_state(root)
    phase = "UNCONFIGURED"
    remote_state = "UNKNOWN"
    if username and remote and main_branch:
        phase = "ACTIVE"
        if state.get("branch") == branch and state.get("head") == head:
            phase = state.get("phase", "ACTIVE")
        try:
            remote_state = "EXACT" if remote_branch_head(root, remote, branch) == head else "NOT BACKED UP"
        except WorkflowError:
            remote_state = "UNAVAILABLE"

    print("Spoiled Milk contributor status")
    print(f"  Repository: {root}")
    print(f"  Contributor: {username or 'not configured'}")
    print(f"  Published base: {f'{remote}/{main_branch}' if remote and main_branch else 'not configured'}")
    print(f"  Branch: {branch}")
    print(f"  Commit: {head}")
    print(f"  Working folder: {'clean' if clean else 'HAS UNSAVED CHANGES'}")
    print(f"  Remote backup: {remote_state}")
    print(f"  Handoff phase: {phase}")
    print("  Public server/release access: NOT PART OF CONTRIBUTOR WORKFLOW")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(
        description="Safe one-clone workflow for external Spoiled Milk contributors."
    )
    commands = result.add_subparsers(dest="command", required=True)

    setup = commands.add_parser("setup", help="Configure this clone without changing branches or GitHub access.")
    setup.add_argument("--username", required=True, help="Exact GitHub username.")
    setup.add_argument("--remote", default="origin", help="Remote used for main and topic branches.")
    setup.add_argument("--main-branch", default="main", help="Published branch; normally main.")

    start = commands.add_parser("start", help="Update clean local main and start a namespaced topic branch.")
    start.add_argument("topic", help="Descriptive topic such as fix/collision-definitions.")

    checkpoint_parser = commands.add_parser("checkpoint", help="Commit all reviewed work and push the topic branch.")
    checkpoint_parser.add_argument("-m", "--message", required=True, help="Short checkpoint commit message.")

    handoff = commands.add_parser("handoff", help="Push an exact READY commit and print pull-request instructions.")
    handoff.add_argument("-m", "--message", required=True, help="Commit message if files have changed.")
    handoff.add_argument("--tests", required=True, help="Tests or manual checks completed for this exact commit.")

    rescue = commands.add_parser("rescue", help="Preserve dirty or confusing work on a pushed rescue branch.")
    rescue.add_argument("-m", "--message", required=True, help="Description of the rescued work.")

    commands.add_parser("status", help="Show branch, backup, cleanliness, and handoff state.")
    return result


def main() -> int:
    try:
        root = repository_root()
        arguments = parser().parse_args()
        commands = {
            "setup": command_setup,
            "start": command_start,
            "checkpoint": command_checkpoint,
            "handoff": command_handoff,
            "rescue": command_rescue,
            "status": command_status,
        }
        commands[arguments.command](root, arguments)
        return 0
    except (WorkflowError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
