#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd "$SCRIPT_DIR/.." && pwd)
ANT_HOME="$ROOT_DIR/Portable_Windows/apache-ant-1.10.5"
ANT_BIN="$ANT_HOME/bin/ant"

echo "Spoiled Milk local client"
echo "This points the repo client at localhost:43605."
echo

if ! command -v java >/dev/null 2>&1; then
  echo "Java was not found. Install Java, then run this file again."
  exit 1
fi

if [ ! -f "$ANT_BIN" ]; then
  echo "Missing bundled Ant launcher: $ANT_BIN"
  exit 1
fi

printf '%s\n' "localhost" > "$ROOT_DIR/Client_Base/Cache/ip.txt"
printf '%s\n' "43605" > "$ROOT_DIR/Client_Base/Cache/port.txt"

cd "$ROOT_DIR/Client_Base"
ANT_HOME="$ANT_HOME" sh "$ANT_BIN" compile-and-run

