#!/usr/bin/env bash

MYWORLD_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MYWORLD_INFERRED_ROOT="$(cd "$MYWORLD_DIR/../.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$MYWORLD_INFERRED_ROOT}"
if [[ ! -f "$ROOT_DIR/server/build.xml" || ! -d "$ROOT_DIR/scripts" ]]; then
  printf 'WARN: Ignoring invalid ROOT_DIR=%s; using %s\n' "$ROOT_DIR" "$MYWORLD_INFERRED_ROOT" >&2
  ROOT_DIR="$MYWORLD_INFERRED_ROOT"
fi
ANT_HOME="${ANT_HOME:-$ROOT_DIR/Portable_Windows/apache-ant-1.10.5}"
ANT_BIN="${ANT_BIN:-$ANT_HOME/bin/ant}"
MYWORLD_GENERATOR_RUNNER="$ROOT_DIR/tools/generators/run-generators.py"
MYWORLD_LIVE_ROOT="${MYWORLD_LIVE_ROOT:-/tmp/spoiled-milk-live-main}"
MYWORLD_PUBLIC_PORT="${MYWORLD_PUBLIC_PORT:-43605}"
MYWORLD_DEV_PORT="${MYWORLD_DEV_PORT:-43615}"

myworld_fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

myworld_require_command() {
  local command_name="$1"
  command -v "$command_name" >/dev/null 2>&1 || myworld_fail "Missing required dependency: $command_name"
}

myworld_java_major_version() {
  local version_line raw_version major_version

  myworld_require_command java
  version_line="$(java -version 2>&1 | head -n 1)"
  raw_version="$(sed -n 's/.*version "\([^"]*\)".*/\1/p' <<<"$version_line")"
  [[ -n "$raw_version" ]] || myworld_fail "Unable to determine Java version from: $version_line"

  if [[ "$raw_version" == 1.* ]]; then
    major_version="${raw_version#1.}"
    major_version="${major_version%%.*}"
  else
    major_version="${raw_version%%.*}"
  fi

  [[ "$major_version" =~ ^[0-9]+$ ]] || myworld_fail "Unable to parse Java major version from: $raw_version"
  printf '%s\n' "$major_version"
}

myworld_require_ant() {
  [[ -f "$ANT_BIN" ]] || myworld_fail "Missing bundled Ant launcher: $ANT_BIN"
}

myworld_require_seed_db() {
  local source_db_path="$ROOT_DIR/server/inc/sqlite/myworld_seed.db"
  [[ -f "$source_db_path" ]] || myworld_fail "Missing MyWorld seed database: $source_db_path"
}

myworld_require_java_17_for_zgc() {
  local java_major_version
  java_major_version="$(myworld_java_major_version)"
  (( java_major_version >= 17 )) || myworld_fail "run-server-zgc.sh requires Java 17+; found Java $java_major_version"
}

myworld_load_local_env() {
  local env_file="${MYWORLD_LOCAL_ENV_FILE:-$ROOT_DIR/server/local.env}"

  if [[ -f "$env_file" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a
  fi
}

myworld_resolve_generator_mode() {
  local mode="check"

  while (($#)); do
    case "$1" in
      --sync-generated)
        mode="sync"
        ;;
      *)
        myworld_fail "Unknown option: $1"
        ;;
    esac
    shift
  done

  printf '%s\n' "$mode"
}

myworld_git_commit() {
  git -C "$ROOT_DIR" rev-parse HEAD 2>/dev/null || true
}

myworld_git_branch() {
  git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || true
}

myworld_git_main_commit() {
  local remote_ref

  for remote_ref in spoiled-milk/main origin/main; do
    if git -C "$ROOT_DIR" rev-parse --verify --quiet "$remote_ref^{commit}" >/dev/null; then
      git -C "$ROOT_DIR" rev-parse "$remote_ref^{commit}"
      return 0
    fi
  done

  return 1
}

myworld_realpath() {
  local path="$1"
  (cd "$path" 2>/dev/null && pwd -P)
}

myworld_conf_path() {
  local conf_name="$1"
  [[ "$conf_name" == *.conf ]] || conf_name="${conf_name}.conf"
  printf '%s/server/%s\n' "$ROOT_DIR" "$conf_name"
}

myworld_conf_value() {
  local conf_name="$1"
  local key="$2"
  local conf_path

  conf_path="$(myworld_conf_path "$conf_name")"
  [[ -f "$conf_path" ]] || myworld_fail "Missing server config: $conf_path"

  awk -v key="$key" '
    /^[[:space:]]*#/ { next }
    {
      line = $0
      sub(/[[:space:]]+#.*/, "", line)
      if (line ~ "^[[:space:]]*" key "[[:space:]]*:") {
        sub(/^[^:]*:[[:space:]]*/, "", line)
        sub(/[[:space:]]*$/, "", line)
        print line
        exit
      }
    }
  ' "$conf_path"
}

myworld_require_conf_value() {
  local conf_name="$1"
  local key="$2"
  local expected="$3"
  local actual

  actual="$(myworld_conf_value "$conf_name" "$key")"
  [[ "$actual" == "$expected" ]] \
    || myworld_fail "$(myworld_conf_path "$conf_name") expected $key=$expected, found $actual"
}

myworld_listener_pids_for_port() {
  local port="$1"
  local line pid

  command -v ss >/dev/null 2>&1 || return 0

  while IFS= read -r line; do
    [[ "$line" == LISTEN* ]] || continue
    [[ "$line" == *":$port "* || "$line" == *":$port" ]] || continue
    [[ "$line" == *"pid="* ]] || continue
    pid="${line#*pid=}"
    pid="${pid%%,*}"
    [[ "$pid" =~ ^[0-9]+$ ]] && printf '%s\n' "$pid"
  done < <(ss -ltnp 2>/dev/null)
}

myworld_listener_summary_for_port() {
  local port="$1"
  local line

  command -v ss >/dev/null 2>&1 || return 0

  while IFS= read -r line; do
    [[ "$line" == LISTEN* ]] || continue
    [[ "$line" == *":$port "* || "$line" == *":$port" ]] || continue
    printf '%s\n' "$line"
  done < <(ss -ltnp 2>/dev/null)
}

myworld_require_port_free() {
  local port="$1"
  local summary

  summary="$(myworld_listener_summary_for_port "$port")"
  [[ -z "$summary" ]] || myworld_fail "Port $port is already in use:
$summary"
}

myworld_process_args() {
  local pid="$1"
  ps -p "$pid" -o args= 2>/dev/null || true
}

myworld_server_root_for_pid() {
  local pid="$1"
  local args root

  args="$(myworld_process_args "$pid")"
  if [[ "$args" =~ ([^[:space:]:]+/server/core\.jar) ]]; then
    root="${BASH_REMATCH[1]%/server/core.jar}"
    myworld_realpath "$root" 2>/dev/null || printf '%s\n' "$root"
  fi
}

myworld_server_conf_for_pid() {
  local pid="$1"
  local args word last_conf=""

  args="$(myworld_process_args "$pid")"
  for word in $args; do
    if [[ "$word" == *.conf ]]; then
      last_conf="$word"
    fi
  done

  printf '%s\n' "$last_conf"
}

myworld_print_server_launch_banner() {
  local label="$1"
  local conf_name="$2"
  local branch commit db_name server_name bind_address server_port ws_port root_real

  root_real="$(myworld_realpath "$ROOT_DIR" 2>/dev/null || printf '%s\n' "$ROOT_DIR")"
  branch="$(myworld_git_branch)"
  commit="$(myworld_git_commit)"
  db_name="$(myworld_conf_value "$conf_name" db_name)"
  server_name="$(myworld_conf_value "$conf_name" server_name)"
  bind_address="$(myworld_conf_value "$conf_name" server_bind_address)"
  server_port="$(myworld_conf_value "$conf_name" server_port)"
  ws_port="$(myworld_conf_value "$conf_name" ws_server_port)"

  printf '\n============================================================\n' >&2
  printf '%s\n' "$label" >&2
  printf 'Worktree: %s\n' "$root_real" >&2
  printf 'Branch:   %s\n' "${branch:-unknown}" >&2
  printf 'Commit:   %s\n' "${commit:-unknown}" >&2
  printf 'Config:   %s\n' "$(myworld_conf_path "$conf_name")" >&2
  printf 'DB:       %s\n' "$db_name" >&2
  printf 'Server:   %s\n' "$server_name" >&2
  printf 'Bind:     %s:%s\n' "$bind_address" "$server_port" >&2
  printf 'WS Port:  %s\n' "$ws_port" >&2
  printf '============================================================\n\n' >&2
}

myworld_launch_marker_path() {
  local conf_name="$1"
  local server_port

  server_port="$(myworld_conf_value "$conf_name" server_port)"
  printf '%s/server/run/server-%s.env\n' "$ROOT_DIR" "$server_port"
}

myworld_write_launch_marker() {
  local label="$1"
  local conf_name="$2"
  local marker_path marker_dir root_real branch commit db_name server_name bind_address server_port ws_port

  marker_path="$(myworld_launch_marker_path "$conf_name")"
  marker_dir="$(dirname "$marker_path")"
  mkdir -p "$marker_dir"

  root_real="$(myworld_realpath "$ROOT_DIR" 2>/dev/null || printf '%s\n' "$ROOT_DIR")"
  branch="$(myworld_git_branch)"
  commit="$(myworld_git_commit)"
  db_name="$(myworld_conf_value "$conf_name" db_name)"
  server_name="$(myworld_conf_value "$conf_name" server_name)"
  bind_address="$(myworld_conf_value "$conf_name" server_bind_address)"
  server_port="$(myworld_conf_value "$conf_name" server_port)"
  ws_port="$(myworld_conf_value "$conf_name" ws_server_port)"

  {
    printf 'marker_label=%q\n' "$label"
    printf 'marker_root=%q\n' "$root_real"
    printf 'marker_branch=%q\n' "$branch"
    printf 'marker_commit=%q\n' "$commit"
    printf 'marker_config=%q\n' "$(basename "$(myworld_conf_path "$conf_name")")"
    printf 'marker_db=%q\n' "$db_name"
    printf 'marker_server=%q\n' "$server_name"
    printf 'marker_bind=%q\n' "$bind_address"
    printf 'marker_port=%q\n' "$server_port"
    printf 'marker_ws_port=%q\n' "$ws_port"
    printf 'marker_launcher_pid=%q\n' "$$"
    printf 'marker_started_at=%q\n' "$(date -Is 2>/dev/null || date)"
  } > "$marker_path"
}

myworld_require_hosted_conf() {
  local conf_name="$1"

  myworld_require_conf_value "$conf_name" db_name spoiled_milk_alpha
  myworld_require_conf_value "$conf_name" server_name "Spoiled Milk"
  myworld_require_conf_value "$conf_name" server_bind_address "0.0.0.0"
  myworld_require_conf_value "$conf_name" server_port "$MYWORLD_PUBLIC_PORT"
}

myworld_require_private_dev_conf() {
  local conf_name="$1"
  local db_name bind_address server_port

  db_name="$(myworld_conf_value "$conf_name" db_name)"
  bind_address="$(myworld_conf_value "$conf_name" server_bind_address)"
  server_port="$(myworld_conf_value "$conf_name" server_port)"

  [[ "$db_name" != "spoiled_milk_alpha" ]] \
    || myworld_fail "Private dev server config must not use the hosted alpha database."
  [[ "$bind_address" == "127.0.0.1" || "$bind_address" == "localhost" || "$bind_address" == "::1" ]] \
    || myworld_fail "Private dev server must bind only to localhost; found $bind_address."
  [[ "$server_port" != "$MYWORLD_PUBLIC_PORT" ]] \
    || myworld_fail "Private dev server must not bind public player port $MYWORLD_PUBLIC_PORT."
}

myworld_require_safe_hosted_launch() {
  local branch dirty_status head_commit main_commit root_real live_root_real

  git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1 \
    || myworld_fail "Hosted server must be started from a git worktree. Use --dev-unsafe only for private local testing."

  root_real="$(myworld_realpath "$ROOT_DIR" 2>/dev/null || true)"
  live_root_real="$(myworld_realpath "$MYWORLD_LIVE_ROOT" 2>/dev/null || true)"
  [[ -n "$live_root_real" ]] || myworld_fail "Configured live worktree does not exist: $MYWORLD_LIVE_ROOT"
  [[ "$root_real" == "$live_root_real" ]] \
    || myworld_fail "Refusing hosted server launch from '$root_real'. Public hosted server must run from '$live_root_real'."

  branch="$(myworld_git_branch)"
  [[ "$branch" == "main" ]] || myworld_fail "Refusing hosted server launch from branch '$branch'. Public hosted server must run from clean main. Use a dev server or --dev-unsafe only for private testing."

  dirty_status="$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no)"
  [[ -z "$dirty_status" ]] || myworld_fail "Refusing hosted server launch from a dirty worktree. Commit, stash, or use a clean live-main worktree first."

  head_commit="$(myworld_git_commit)"
  main_commit="$(myworld_git_main_commit || true)"
  [[ -n "$main_commit" ]] || myworld_fail "Unable to resolve spoiled-milk/main or origin/main. Fetch first, then start the hosted server."
  [[ "$head_commit" == "$main_commit" ]] || myworld_fail "Refusing hosted server launch from commit $head_commit; expected published main $main_commit."

  printf 'Hosted launch safety: clean main at %s\n' "$head_commit" >&2
}

myworld_prepare_generated_artifacts() {
  local mode="${1:-check}"
  local -a command=(python3 "$MYWORLD_GENERATOR_RUNNER")

  myworld_require_command python3

  case "$mode" in
    check)
      command+=(--check)
      ;;
    sync)
      ;;
    *)
      myworld_fail "Unknown generator mode: $mode"
      ;;
  esac

  (
    cd "$ROOT_DIR"
    "${command[@]}"
  )
}

myworld_ant_build() {
  local target="$1"
  shift || true

  myworld_require_ant
  ANT_HOME="$ANT_HOME" sh "$ANT_BIN" -f "$ROOT_DIR/server/build.xml" "$target" "$@"
}

myworld_ant_server() {
  local target="$1"
  shift || true

  myworld_require_ant
  (
    cd "$ROOT_DIR/server"
    ANT_HOME="$ANT_HOME" sh "$ANT_BIN" "$target" "$@"
  )
}
