#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDITOR_DIR="${LANDSCAPE_EDITOR_DIR:-/home/justin/2D-Landscape-Editor}"
MAPS_DIR="$EDITOR_DIR/maps"
SOURCE="$ROOT_DIR/server/conf/server/data/Custom_Landscape.orsc"

if [[ $# -ne 1 || ! "$1" =~ ^[0-9]+-[0-9]+$ ]]; then
	echo "Usage: $0 X-Y" >&2
	exit 1
fi

mkdir -p "$MAPS_DIR"
destination="$MAPS_DIR/Custom_Landscape_$1.orsc"
cp "$SOURCE" "$destination"

echo "Exported landscape to $destination"
