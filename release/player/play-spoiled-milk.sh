#!/usr/bin/env sh
set -eu

GAME_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if [ -f "$GAME_DIR/update-spoiled-milk.sh" ]; then
  if ! sh "$GAME_DIR/update-spoiled-milk.sh"; then
    printf 'Update check failed; launching installed Spoiled Milk client.\n' >&2
  fi
fi

exec java -jar "$GAME_DIR/Spoiled_Milk_Client.jar"
