#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_ROOT="$(cd "$ROOT_DIR/.." && pwd)"
WORKSPACE="$ROOT_DIR/workspace"
TOOLS_JAR="$ROOT_DIR/builder-runtime/launcher/world-builder-tools.jar"

fail() {
	printf 'Map import could not start: %s\n' "$*" >&2
	exit 1
}

if [[ -n "${WORLD_BUILDER_JAVA:-}" ]]; then
	JAVA_EXE="$WORLD_BUILDER_JAVA"
elif [[ -x "$ROOT_DIR/runtime/bin/java" ]]; then
	JAVA_EXE="$ROOT_DIR/runtime/bin/java"
else
	JAVA_EXE="$(command -v java || true)"
fi

[[ -n "$JAVA_EXE" ]] || fail "Java 17 or newer was not found."
[[ -f "$TOOLS_JAR" ]] || fail "The packaged launcher is missing."
[[ -f "$WORKSPACE/project-source.json" ]] || fail "Run Start World Builder before importing."
[[ -f "$ROOT_DIR/VERSION.txt" && -f "$ROOT_DIR/SOURCE-COMMIT.txt" ]] \
	|| fail "Release provenance files are missing."

VERSION="$(tr -d '\r\n' < "$ROOT_DIR/VERSION.txt")"
SOURCE_COMMIT="$(tr -d '\r\n' < "$ROOT_DIR/SOURCE-COMMIT.txt")"

exec "$JAVA_EXE" -jar "$TOOLS_JAR" export-import \
	--workspace "$WORKSPACE" \
	--target-root "$TARGET_ROOT" \
	--builder-version "$VERSION" \
	--source-commit "$SOURCE_COMMIT"
