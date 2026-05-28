#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

SOURCE_DB_PATH="$ROOT_DIR/server/inc/sqlite/myworld_seed.db"
DB_PATH="$ROOT_DIR/server/inc/sqlite/spoiled_milk_alpha.db"

[[ -f "$SOURCE_DB_PATH" ]] || {
  printf 'FAIL: missing seed database: %s\n' "$SOURCE_DB_PATH" >&2
  exit 1
}

if [[ -f "$DB_PATH" ]]; then
  printf 'Hosted alpha database already exists:\n'
  printf '  %s\n' "$DB_PATH"
  printf 'Leaving it untouched.\n'
  exit 0
fi

cp "$SOURCE_DB_PATH" "$DB_PATH"
printf 'Created hosted alpha database:\n'
printf '  %s\n' "$DB_PATH"
