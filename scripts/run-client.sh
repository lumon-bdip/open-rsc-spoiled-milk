#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

CLIENT_TARGET_MODE="live"
CLIENT_TARGET_HOST="localhost"
CLIENT_TARGET_PORT="$MYWORLD_PUBLIC_PORT"

while (($#)); do
  case "$1" in
    --live)
      CLIENT_TARGET_MODE="live"
      CLIENT_TARGET_HOST="localhost"
      CLIENT_TARGET_PORT="$MYWORLD_PUBLIC_PORT"
      ;;
    --dev)
      CLIENT_TARGET_MODE="dev"
      CLIENT_TARGET_HOST="localhost"
      CLIENT_TARGET_PORT="$MYWORLD_DEV_PORT"
      ;;
    --cache)
      CLIENT_TARGET_MODE="cache"
      CLIENT_TARGET_HOST=""
      CLIENT_TARGET_PORT=""
      ;;
    --target)
      [[ $# -ge 2 ]] || myworld_fail "--target requires host:port"
      CLIENT_TARGET_MODE="custom"
      CLIENT_TARGET_HOST="${2%:*}"
      CLIENT_TARGET_PORT="${2##*:}"
      [[ -n "$CLIENT_TARGET_HOST" && "$CLIENT_TARGET_HOST" != "$2" ]] || myworld_fail "--target requires host:port"
      [[ "$CLIENT_TARGET_PORT" =~ ^[0-9]+$ ]] || myworld_fail "--target port must be numeric"
      shift
      ;;
    *)
      myworld_fail "Unknown option: $1"
      ;;
  esac
  shift
done

if [[ ! -f "$ANT_BIN" ]]; then
  printf 'FAIL: Missing bundled Ant launcher: %s\n' "$ANT_BIN" >&2
  exit 1
fi

if [[ "$CLIENT_TARGET_MODE" != "cache" ]]; then
  mkdir -p "$ROOT_DIR/Client_Base/Cache"
  printf '%s\n' "$CLIENT_TARGET_HOST" > "$ROOT_DIR/Client_Base/Cache/ip.txt"
  printf '%s\n' "$CLIENT_TARGET_PORT" > "$ROOT_DIR/Client_Base/Cache/port.txt"
else
  CLIENT_TARGET_HOST="$(tr -d '\r\n' < "$ROOT_DIR/Client_Base/Cache/ip.txt" 2>/dev/null || true)"
  CLIENT_TARGET_PORT="$(tr -d '\r\n' < "$ROOT_DIR/Client_Base/Cache/port.txt" 2>/dev/null || true)"
fi

printf '\n============================================================\n' >&2
case "$CLIENT_TARGET_MODE" in
  live)
    printf 'CLIENT TARGET: LIVE SPOILED MILK HOSTED ALPHA\n' >&2
    ;;
  dev)
    printf 'CLIENT TARGET: PRIVATE SPOILED MILK DEV SERVER\n' >&2
    ;;
  cache)
    printf 'CLIENT TARGET: EXISTING CACHE VALUES\n' >&2
    ;;
  custom)
    printf 'CLIENT TARGET: CUSTOM SERVER\n' >&2
    ;;
esac
printf 'Worktree: %s\n' "$(myworld_realpath "$ROOT_DIR" 2>/dev/null || printf '%s\n' "$ROOT_DIR")" >&2
printf 'Branch:   %s\n' "$(myworld_git_branch)" >&2
printf 'Commit:   %s\n' "$(myworld_git_commit)" >&2
printf 'Endpoint: %s:%s\n' "${CLIENT_TARGET_HOST:-unknown}" "${CLIENT_TARGET_PORT:-unknown}" >&2
printf '============================================================\n\n' >&2

(
  cd "$ROOT_DIR/Client_Base"
  ANT_HOME="$ANT_HOME" sh "$ANT_BIN" compile-and-run
)
