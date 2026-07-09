#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

myworld_load_local_env

SERVER_CONF="myworld-host"

while (($#)); do
  case "$1" in
    --dev-unsafe|--sync-generated)
      myworld_fail "$1 is forbidden for the public hosted server. Use scripts/run-server.sh for private development and commit generated changes before deployment."
      ;;
    *)
      myworld_fail "Unknown option: $1"
      ;;
  esac
  shift
done

myworld_require_safe_hosted_launch

myworld_require_hosted_conf "$SERVER_CONF"
myworld_require_port_free "$(myworld_conf_value "$SERVER_CONF" server_port)"
myworld_print_server_launch_banner "LIVE SPOILED MILK HOSTED ALPHA" "$SERVER_CONF"
myworld_prepare_generated_artifacts check
# Recheck after every pre-launch command so a concurrent mutation cannot turn a
# verified deployment into an unsafe public launch.
myworld_require_safe_hosted_launch

HOSTED_DB_PATH="$(myworld_live_database_path "$ROOT_DIR")"
myworld_require_live_database_link "$ROOT_DIR"

if [[ ! -f "$HOSTED_DB_PATH" ]]; then
  printf 'FAIL: hosted alpha database is missing:\n' >&2
  printf '  %s\n' "$HOSTED_DB_PATH" >&2
  printf 'Refusing to create a fresh hosted database during server startup.\n' >&2
  printf 'Restore the live database backup, or run scripts/init-hosted-sqlite.sh only for a deliberate new alpha reset.\n' >&2
  exit 1
fi

myworld_write_launch_marker "LIVE SPOILED MILK HOSTED ALPHA" "$SERVER_CONF" verified-live
myworld_ant_server compile-and-run -DconfFile=myworld-host
