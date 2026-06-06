#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
if [[ ! -f "$ROOT_DIR/Client_Base/build.xml" || ! -d "$ROOT_DIR/scripts" ]]; then
  printf 'WARN: Ignoring invalid ROOT_DIR=%s; using %s\n' "$ROOT_DIR" "$SCRIPT_ROOT" >&2
  ROOT_DIR="$SCRIPT_ROOT"
fi
ANT_HOME="${ANT_HOME:-$ROOT_DIR/Portable_Windows/apache-ant-1.10.5}"
ANT_BIN="${ANT_BIN:-$ANT_HOME/bin/ant}"

if [[ ! -f "$ANT_BIN" ]]; then
  printf 'FAIL: Missing bundled Ant launcher: %s\n' "$ANT_BIN" >&2
  exit 1
fi

(
  cd "$ROOT_DIR/Client_Base"
  ANT_HOME="$ANT_HOME" sh "$ANT_BIN" compile-and-run
)
