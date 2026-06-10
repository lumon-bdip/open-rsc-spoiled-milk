#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDITOR_DIR="${LANDSCAPE_EDITOR_DIR:-/home/justin/2D-Landscape-Editor}"
MAPS_DIR="$EDITOR_DIR/maps"

if [[ $# -ne 1 ]]; then
	echo "Usage: $0 FILE.orsc" >&2
	exit 1
fi

source_path="$1"
if [[ "$source_path" != /* ]]; then
	source_path="$MAPS_DIR/$source_path"
fi

if [[ ! -f "$source_path" ]]; then
	echo "Landscape not found: $source_path" >&2
	exit 1
fi

unzip -tqq "$source_path"
cp "$source_path" "$ROOT_DIR/server/conf/server/data/Custom_Landscape.orsc"
cp "$source_path" "$ROOT_DIR/Client_Base/Cache/video/Custom_Landscape.orsc"

server_hash="$(sha256sum "$ROOT_DIR/server/conf/server/data/Custom_Landscape.orsc" | cut -d' ' -f1)"
client_hash="$(sha256sum "$ROOT_DIR/Client_Base/Cache/video/Custom_Landscape.orsc" | cut -d' ' -f1)"
if [[ "$server_hash" != "$client_hash" ]]; then
	echo "Server and client landscape hashes do not match" >&2
	exit 1
fi

echo "Imported landscape from $source_path"
