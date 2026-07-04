#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

myworld_load_local_env

HOSTED_LAUNCH_MODE="live"
GENERATOR_ARGS=()
SERVER_CONF="myworld-host"

while (($#)); do
  case "$1" in
    --dev-unsafe)
      HOSTED_LAUNCH_MODE="dev-unsafe"
      ;;
    --sync-generated)
      GENERATOR_ARGS+=("$1")
      ;;
    *)
      myworld_fail "Unknown option: $1"
      ;;
  esac
  shift
done

if [[ "$HOSTED_LAUNCH_MODE" == "live" ]]; then
  myworld_require_safe_hosted_launch
else
  printf 'WARN: Hosted launch safety bypassed with --dev-unsafe. Do not use this for the public server.\n' >&2
fi

GENERATOR_MODE="$(myworld_resolve_generator_mode "${GENERATOR_ARGS[@]}")"

myworld_require_hosted_conf "$SERVER_CONF"
myworld_require_port_free "$(myworld_conf_value "$SERVER_CONF" server_port)"
myworld_print_server_launch_banner "LIVE SPOILED MILK HOSTED ALPHA" "$SERVER_CONF"
myworld_prepare_generated_artifacts "$GENERATOR_MODE"

HOSTED_DB_PATH="$ROOT_DIR/server/inc/sqlite/spoiled_milk_alpha.db"
if [[ ! -f "$HOSTED_DB_PATH" ]]; then
  printf 'FAIL: hosted alpha database is missing:\n' >&2
  printf '  %s\n' "$HOSTED_DB_PATH" >&2
  printf 'Refusing to create a fresh hosted database during server startup.\n' >&2
  printf 'Restore the live database backup, or run scripts/init-hosted-sqlite.sh only for a deliberate new alpha reset.\n' >&2
  exit 1
fi

myworld_write_launch_marker "LIVE SPOILED MILK HOSTED ALPHA" "$SERVER_CONF"
myworld_ant_server compile-and-run -DconfFile=myworld-host
