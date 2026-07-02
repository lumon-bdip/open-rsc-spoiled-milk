#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

myworld_load_local_env

GENERATOR_MODE="$(myworld_resolve_generator_mode "$@")"

myworld_prepare_generated_artifacts "$GENERATOR_MODE"

HOSTED_DB_PATH="$ROOT_DIR/server/inc/sqlite/spoiled_milk_alpha.db"
if [[ ! -f "$HOSTED_DB_PATH" ]]; then
  printf 'FAIL: hosted alpha database is missing:\n' >&2
  printf '  %s\n' "$HOSTED_DB_PATH" >&2
  printf 'Refusing to create a fresh hosted database during server startup.\n' >&2
  printf 'Restore the live database backup, or run scripts/init-hosted-sqlite.sh only for a deliberate new alpha reset.\n' >&2
  exit 1
fi

myworld_ant_server compile-and-run -DconfFile=myworld-host
