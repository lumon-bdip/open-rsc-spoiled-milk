#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANT_HOME="${ANT_HOME:-$SCRIPT_ROOT/tools/vendor/apache-ant-1.10.5}"
ANT_BIN="${ANT_BIN:-$ANT_HOME/bin/ant}"

if [[ ! -f "$ANT_BIN" ]]; then
  printf 'FAIL: Missing bundled Ant launcher: %s\n' "$ANT_BIN" >&2
  exit 1
fi

(
  cd "$SCRIPT_ROOT/tools/world-builder"
  ANT_HOME="$ANT_HOME" sh "$ANT_BIN" jar
)

printf 'Built %s\n' "$SCRIPT_ROOT/output/world-builder-tools/world-builder-tools.jar"
