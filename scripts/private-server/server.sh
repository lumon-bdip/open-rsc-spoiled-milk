#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd "$SCRIPT_DIR/../.." && pwd)
ANT_HOME="$ROOT_DIR/tools/vendor/apache-ant-1.10.5"
ANT_BIN="$ANT_HOME/bin/ant"
DB_PATH="$ROOT_DIR/server/inc/sqlite/myworld_dev.db"
SEED_DB_PATH="$ROOT_DIR/server/inc/sqlite/myworld_seed.db"

echo "Spoiled Milk private server"
echo "Keep this window open while people are playing."
echo

if ! command -v java >/dev/null 2>&1; then
  echo "Java was not found. Install Java, then run this file again."
  exit 1
fi

if [ ! -f "$ANT_BIN" ]; then
  echo "Missing bundled Ant launcher: $ANT_BIN"
  exit 1
fi

if [ ! -f "$DB_PATH" ]; then
  if [ ! -f "$SEED_DB_PATH" ]; then
    echo "Missing seed database: $SEED_DB_PATH"
    exit 1
  fi
  echo "Creating a fresh local save database..."
  cp "$SEED_DB_PATH" "$DB_PATH"
else
  echo "Using existing local save database."
fi

printf '%s\n' "localhost" > "$ROOT_DIR/Client_Base/Cache/ip.txt"
printf '%s\n' "43605" > "$ROOT_DIR/Client_Base/Cache/port.txt"

echo "Building and starting the server..."
echo
cd "$ROOT_DIR/server"
ANT_HOME="$ANT_HOME" sh "$ANT_BIN" compile-and-run -DconfFile=myworld
