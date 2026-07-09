#!/usr/bin/env python3
import os
import shlex
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
COMMON = ROOT / "scripts" / "lib" / "myworld-common.sh"
STATUS = ROOT / "scripts" / "live-status.sh"
RUN_HOSTED = ROOT / "scripts" / "run-hosted-server.sh"
DEPLOY = ROOT / "scripts" / "deploy-live-main.sh"
STOP = ROOT / "scripts" / "stop-hosted-server.sh"
TERMINAL_LAUNCHER = ROOT / "Launch Live Server.sh"
FIXED_LIVE_ROOT = "/tmp/spoiled-milk-live-main"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def run_bash(script: str, *, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    process_env = os.environ.copy()
    if env:
        process_env.update(env)
    return subprocess.run(
        ["bash", "-c", script],
        cwd=ROOT,
        env=process_env,
        capture_output=True,
        text=True,
    )


def output(result: subprocess.CompletedProcess[str]) -> str:
    return result.stdout + result.stderr


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def require_output(result: subprocess.CompletedProcess[str], snippet: str, label: str) -> None:
    require(snippet in output(result), f"{label} missing {snippet!r}:\n{output(result)}")


def test_fixed_live_paths() -> None:
    result = run_bash(
        f'source {shlex.quote(str(COMMON))}; printf "%s\\n%s\\n" "$MYWORLD_LIVE_ROOT" "$MYWORLD_LIVE_DB_ROOT"',
        env={
            "MYWORLD_LIVE_ROOT": "/tmp/not-the-live-server",
            "MYWORLD_LIVE_DB_ROOT": "/tmp/external-live-db-test",
        },
    )
    require(result.returncode == 0, f"unable to load live path defaults:\n{output(result)}")
    lines = result.stdout.splitlines()
    require(lines == [FIXED_LIVE_ROOT, "/tmp/external-live-db-test"], f"unexpected live path resolution: {lines}")


def safe_launch_result(*, branch: str, head: str, published: str, dirty: str = "") -> subprocess.CompletedProcess[str]:
    script = f"""
source {shlex.quote(str(COMMON))}
myworld_realpath() {{ printf '%s\\n' {shlex.quote(FIXED_LIVE_ROOT)}; }}
myworld_git_branch() {{ printf '%s\\n' "$TEST_BRANCH"; }}
myworld_git_commit() {{ printf '%s\\n' "$TEST_HEAD"; }}
myworld_git_main_commit() {{ printf '%s\\n' "$TEST_PUBLISHED"; }}
git() {{
  case "$*" in
    *"rev-parse --is-inside-work-tree"*) return 0 ;;
    *"status --porcelain --untracked-files=all"*) printf '%s' "${{TEST_DIRTY:-}}"; return 0 ;;
    *) command git "$@" ;;
  esac
}}
myworld_require_safe_hosted_launch
"""
    return run_bash(
        script,
        env={
            "TEST_BRANCH": branch,
            "TEST_HEAD": head,
            "TEST_PUBLISHED": published,
            "TEST_DIRTY": dirty,
        },
    )


def test_hosted_launch_preflight() -> None:
    result = safe_launch_result(branch="HEAD", head="published", published="published")
    require(result.returncode == 0, f"clean detached published launch was rejected:\n{output(result)}")
    require_output(result, "clean detached published main", "safe launch confirmation")

    result = safe_launch_result(branch="main", head="published", published="published")
    require(result.returncode != 0, "attached main unexpectedly passed hosted launch safety")
    require_output(result, "must use detached HEAD", "attached-branch rejection")

    result = safe_launch_result(
        branch="HEAD",
        head="published",
        published="published",
        dirty="? forgotten-untracked-file\n",
    )
    require(result.returncode != 0, "untracked file unexpectedly passed hosted launch safety")
    require_output(result, "including untracked files", "untracked-file rejection")

    result = safe_launch_result(branch="HEAD", head="old", published="published")
    require(result.returncode != 0, "previous published commit unexpectedly passed hosted launch safety")
    require_output(result, "expected published main published", "stale-commit rejection")


def test_publication_state_helper() -> None:
    script = f"""
source {shlex.quote(str(COMMON))}
current="$(git -C "$ROOT_DIR" rev-parse spoiled-milk/main)"
previous="$(git -C "$ROOT_DIR" rev-parse spoiled-milk/main^)"
printf '%s\\n' "$(myworld_git_commit_publication_state "$ROOT_DIR" "$current")"
printf '%s\\n' "$(myworld_git_commit_publication_state "$ROOT_DIR" "$previous")"
printf '%s\\n' "$(myworld_git_commit_publication_state "$ROOT_DIR" not-a-commit)"
"""
    result = run_bash(script)
    require(result.returncode == 0, f"publication-state helper failed:\n{output(result)}")
    require(result.stdout.splitlines() == ["current", "previous", "unknown"], f"wrong publication states: {result.stdout}")


def test_live_root_process_detection() -> None:
    script = f"""
source {shlex.quote(str(COMMON))}
myworld_realpath() {{ printf '%s\\n' {shlex.quote(FIXED_LIVE_ROOT)}; }}
ps() {{
  printf '%s\\n' \
    '111 java -classpath {FIXED_LIVE_ROOT}/server/lib/x.jar:{FIXED_LIVE_ROOT}/server/core.jar com.openrsc.server.Server myworld-host.conf' \
    '222 unrelated-process'
}}
myworld_server_pids_for_root {shlex.quote(FIXED_LIVE_ROOT)}
"""
    result = run_bash(script)
    require(result.returncode == 0, f"live-root process detection failed:\n{output(result)}")
    require(result.stdout.splitlines() == ["111"], f"unexpected live-root PIDs: {result.stdout}")


def test_status_accepts_previous_published_runtime() -> None:
    with tempfile.TemporaryDirectory(prefix="hosted-status-") as temp_dir:
        fake_root = Path(temp_dir)
        (fake_root / "server" / "run").mkdir(parents=True)
        external_root = fake_root / "external"
        external_root.mkdir()
        external_db = external_root / "spoiled_milk_alpha.db"
        external_db.write_text("fixture database\n", encoding="utf-8")
        checkout_db = fake_root / "server" / "inc" / "sqlite" / "spoiled_milk_alpha.db"
        checkout_db.parent.mkdir(parents=True)
        checkout_db.symlink_to(external_db)
        (fake_root / "server" / "myworld-host.conf").write_text(
            "database:\n\tdb_name: spoiled_milk_alpha\n"
            "world:\n\tserver_name: Spoiled Milk\n",
            encoding="utf-8",
        )
        (fake_root / "server" / "run" / "server-43605.env").write_text(
            "marker_label=LIVE\n"
            f"marker_root={shlex.quote(str(fake_root))}\n"
            "marker_branch=HEAD\n"
            "marker_commit=previous-commit\n"
            "marker_config=myworld-host.conf\n"
            "marker_db=spoiled_milk_alpha\n"
            f"marker_db_path={shlex.quote(str(external_db.resolve()))}\n"
            "marker_safety=verified-live\n"
            "marker_port=43605\n"
            "marker_started_at=test-time\n",
            encoding="utf-8",
        )

        script = f"""
source {shlex.quote(str(STATUS))}
TEST_FAKE_ROOT={shlex.quote(str(fake_root))}
myworld_server_root_for_pid() {{ printf '%s\\n' "$TEST_FAKE_ROOT"; }}
myworld_server_conf_for_pid() {{ printf '%s\\n' myworld-host.conf; }}
myworld_process_args() {{ printf '%s\\n' fake-server-process; }}
myworld_realpath() {{ printf '%s\\n' {shlex.quote(FIXED_LIVE_ROOT)}; }}
myworld_git_published_main_commit() {{ printf '%s\\n' current-commit; }}
myworld_git_dirty_status() {{ return 0; }}
myworld_process_database_targets() {{ printf '%s\\n' {shlex.quote(str(external_db.resolve()))}; }}
myworld_git_commit_publication_state() {{
  if [[ "$2" == previous-commit ]]; then
    printf '%s\\n' previous
  else
    printf '%s\\n' current
  fi
}}
git() {{
  case "$*" in
    *"rev-parse --abbrev-ref HEAD"*) printf '%s\\n' HEAD ;;
    *"rev-parse HEAD"*) printf '%s\\n' previous-commit ;;
    *) command git "$@" ;;
  esac
}}
print_pid_status 12345 43605 TEST
"""
        result = run_bash(script, env={"MYWORLD_LIVE_DB_ROOT": str(external_root)})
        require(result.returncode == 0, f"status classifier failed:\n{output(result)}")
        require_output(result, "Checkout: detached previous published main", "detached checkout status")
        require_output(result, "DB link:  verified external database", "external database link status")
        require_output(result, f"Runtime DB: {external_db.resolve()}", "runtime database descriptor status")
        require_output(result, "Runtime:  previous published main", "previous runtime status")
        require_output(
            result,
            "Verdict:  OK LIVE HOSTED SERVER - running previous published main; restart pending",
            "previous published runtime verdict",
        )
        require("DANGER" not in output(result), f"safe previous runtime reported danger:\n{output(result)}")

        unsafe_script = script.replace(
            "myworld_git_dirty_status() { return 0; }",
            "myworld_git_dirty_status() { printf '%s\\n' ' M tracked-file'; }",
        )
        result = run_bash(unsafe_script, env={"MYWORLD_LIVE_DB_ROOT": str(external_root)})
        require_output(result, "DANGER: live checkout is not clean", "dirty checkout verdict")

        exact_target_function = (
            "myworld_process_database_targets() { printf '%s\\n' "
            f"{shlex.quote(str(external_db.resolve()))}; }}"
        )
        mixed_target_function = (
            "myworld_process_database_targets() { "
            f"printf '%s\\n' {shlex.quote(str(external_db.resolve()))} "
            f"{shlex.quote(str(fake_root / 'server/inc/sqlite/spoiled_milk_alpha.db') + ' (deleted)')}; }}"
        )
        mixed_script = script.replace(exact_target_function, mixed_target_function)
        result = run_bash(mixed_script, env={"MYWORLD_LIVE_DB_ROOT": str(external_root)})
        require_output(result, "runtime database file descriptor is missing, wrong, or deleted", "mixed DB verdict")

        marker = fake_root / "server" / "run" / "server-43605.env"
        marker.write_text(
            marker.read_text(encoding="utf-8").replace("marker_safety=verified-live", "marker_safety=unverified"),
            encoding="utf-8",
        )
        result = run_bash(script, env={"MYWORLD_LIVE_DB_ROOT": str(external_root)})
        require_output(result, "lacks verified live identity", "unverified marker verdict")


def test_script_contracts_and_syntax() -> None:
    common = COMMON.read_text(encoding="utf-8")
    deploy = DEPLOY.read_text(encoding="utf-8")
    runner = RUN_HOSTED.read_text(encoding="utf-8")
    stopper = STOP.read_text(encoding="utf-8")
    terminal_launcher = TERMINAL_LAUNCHER.read_text(encoding="utf-8")

    require('MYWORLD_LIVE_ROOT="/tmp/spoiled-milk-live-main"' in common, "live root is not fixed")
    require("origin/main" not in common, "hosted safety still falls back to origin/main")
    require("--untracked-files=all" in common, "hosted dirty check omits untracked files")
    require("worktree add --detach" in deploy, "initial live deployment is not detached")
    require("switch --detach" in deploy, "live update does not detach HEAD")
    require("primary manager checkout" in deploy, "deployment is not manager-only")
    require("clean manager main checkout" in deploy, "deployment does not require clean manager main")
    require("myworld_require_live_database_link" in deploy, "deployment does not preserve external live DB link")
    require("myworld_require_live_database_link" in runner, "hosted launch does not verify external live DB link")
    require(runner.count("myworld_require_safe_hosted_launch") >= 2, "hosted launch is not rechecked after generators")
    require("verified-live" in runner, "hosted launch marker lacks a verified-live attestation")
    require("marker_db_path" in common and "marker_safety" in common, "launch marker omits database/safety attestation")
    require("myworld_process_database_targets" in common, "runtime database descriptor cannot be audited")
    require(
        "myworld_process_uses_only_external_live_database" in common,
        "mixed live database descriptors cannot be rejected",
    )
    require("myworld_server_pids_for_root" in common, "deployment cannot detect a live JVM before it binds")
    require(
        'myworld_require_port_free "$MYWORLD_PUBLIC_PORT"' in deploy,
        "deployment does not refuse a running public listener",
    )
    require(
        deploy.index('myworld_require_port_free "$MYWORLD_PUBLIC_PORT"') < deploy.index("switch --detach"),
        "deployment checks the public listener only after changing live files",
    )
    require(
        'myworld_server_pids_for_root "$MYWORLD_LIVE_ROOT"' in deploy,
        "deployment does not refuse a live-root JVM when its listener is unavailable",
    )
    require(
        deploy.index("myworld_require_live_database_link") < deploy.index("switch --detach"),
        "deployment validates the existing database link only after changing live files",
    )
    require("git clean" not in deploy, "deployment must not erase unexpected files")
    require(
        "myworld_process_uses_only_external_live_database" in stopper
        and "--database-recovered" in stopper,
        "hosted stop does not guard an unrecovered wrong/deleted database inode",
    )
    require('LIVE_ROOT="/tmp/spoiled-milk-live-main"' in terminal_launcher, "terminal launcher does not use the fixed live root")
    require("./scripts/run-hosted-server.sh" in terminal_launcher, "terminal launcher does not use the guarded hosted runner")
    for forbidden_mutation in ["git switch", "git fetch", "git merge"]:
        require(
            forbidden_mutation not in terminal_launcher,
            f"terminal launcher must not mutate deployment state with {forbidden_mutation}",
        )
    require(os.access(DEPLOY, os.X_OK), "deploy-live-main.sh is not executable")

    syntax = subprocess.run(
        ["bash", "-n", COMMON, STATUS, RUN_HOSTED, DEPLOY, STOP, TERMINAL_LAUNCHER],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    require(syntax.returncode == 0, f"hosted script syntax failed:\n{output(syntax)}")

    for forbidden_option in ["--dev-unsafe", "--sync-generated"]:
        result = subprocess.run(
            ["bash", RUN_HOSTED, forbidden_option],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        require(result.returncode != 0, f"hosted runner accepted forbidden option {forbidden_option}")
        require_output(result, "forbidden for the public hosted server", f"{forbidden_option} rejection")


def main() -> None:
    test_fixed_live_paths()
    test_hosted_launch_preflight()
    test_publication_state_helper()
    test_live_root_process_detection()
    test_status_accepts_previous_published_runtime()
    test_script_contracts_and_syntax()
    print("PASS: detached hosted-server deployment and runtime safety validated")


if __name__ == "__main__":
    main()
