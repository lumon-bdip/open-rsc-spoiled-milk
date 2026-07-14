#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

VERSION=""
WINDOWS_JRE=""
ASSETS_CLEARED=false
SKIP_BUILD=false
SOURCE_COMMIT=""

fail() {
	printf 'FAIL: %s\n' "$*" >&2
	exit 1
}

usage() {
	cat <<'EOF'
Usage:
  ./scripts/package-world-builder-release.sh \
    --version v0.1.0-alpha.1 \
    --windows-jre /path/to/temurin-17-windows-x64-jre \
    --assets-cleared

Options:
  --assets-cleared  Attest that all packaged visual assets, including editor
                    icons, have confirmed redistribution terms.
  --skip-build      Use existing jars. Restricted to packaging tests.
EOF
}

while (($#)); do
	case "$1" in
		--version)
			[[ $# -ge 2 ]] || fail "--version requires a value"
			VERSION="$2"
			shift 2
			;;
		--windows-jre)
			[[ $# -ge 2 ]] || fail "--windows-jre requires a value"
			WINDOWS_JRE="$2"
			shift 2
			;;
		--assets-cleared)
			ASSETS_CLEARED=true
			shift
			;;
		--skip-build)
			SKIP_BUILD=true
			shift
			;;
		-h|--help)
			usage
			exit 0
			;;
		*)
			fail "Unknown option: $1"
			;;
	esac
done

[[ "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-alpha\.[0-9]+)?$ ]] \
	|| fail "Version must use semantic form, for example v0.1.0 or v0.1.0-alpha.1"
[[ "$ASSETS_CLEARED" == true ]] \
	|| fail "Confirm redistribution terms with --assets-cleared before packaging"
if [[ "$SKIP_BUILD" == true && "${SPOILED_MILK_WORLD_BUILDER_RELEASE_TEST_MODE:-}" != 1 ]]; then
	fail "--skip-build is restricted to World Builder packaging tests"
fi

for command_name in git jar zip sha256sum; do
	command -v "$command_name" >/dev/null 2>&1 || fail "Missing dependency: $command_name"
done

require_release_git_state() {
	local expected_commit="${1:-}"
	local git_dir current_branch current_commit worktree_status published_commit
	local operation_entry operation_marker operation_name

	git -C "$ROOT_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1 \
		|| fail "World Builder release packaging must run from the manager Git worktree"
	git_dir="$(git -C "$ROOT_DIR" rev-parse --absolute-git-dir)"
	for operation_entry in \
		"MERGE_HEAD:merge" \
		"CHERRY_PICK_HEAD:cherry-pick" \
		"REVERT_HEAD:revert" \
		"REBASE_HEAD:rebase" \
		"rebase-apply:rebase or am" \
		"rebase-merge:rebase" \
		"sequencer:sequenced operation" \
		"BISECT_LOG:bisect"; do
		operation_marker="${operation_entry%%:*}"
		operation_name="${operation_entry#*:}"
		[[ ! -e "$git_dir/$operation_marker" ]] \
			|| fail "Packaging is blocked by an in-progress Git $operation_name operation"
	done

	current_branch="$(git -C "$ROOT_DIR" symbolic-ref --quiet --short HEAD 2>/dev/null || true)"
	[[ "$current_branch" == "main" ]] \
		|| fail "World Builder packaging must run from manager branch main; found ${current_branch:-detached HEAD}"
	worktree_status="$(git -C "$ROOT_DIR" status --porcelain --untracked-files=all)"
	[[ -z "$worktree_status" ]] \
		|| fail "World Builder packaging requires a clean manager main worktree"

	current_commit="$(git -C "$ROOT_DIR" rev-parse --verify 'HEAD^{commit}')"
	if [[ -n "$expected_commit" && "$current_commit" != "$expected_commit" ]]; then
		fail "Release source changed during packaging (expected $expected_commit, found $current_commit)"
	fi
	if [[ -z "$SOURCE_COMMIT" ]]; then
		SOURCE_COMMIT="$current_commit"
	fi
	published_commit="$(git -C "$ROOT_DIR" rev-parse --verify 'refs/remotes/spoiled-milk/main^{commit}' 2>/dev/null || true)"
	[[ -n "$published_commit" ]] || fail "Missing spoiled-milk/main"
	[[ "$SOURCE_COMMIT" == "$published_commit" ]] \
		|| fail "Packaging requires HEAD to match spoiled-milk/main"
}

require_release_git_state

[[ -d "$WINDOWS_JRE" ]] || fail "Windows JRE directory does not exist: $WINDOWS_JRE"
[[ -f "$WINDOWS_JRE/bin/java.exe" ]] || fail "Windows JRE must contain bin/java.exe"
[[ -f "$WINDOWS_JRE/release" ]] || fail "Windows JRE must contain release metadata"
[[ -f "$WINDOWS_JRE/LICENSE" || -f "$WINDOWS_JRE/NOTICE" || -f "$WINDOWS_JRE/legal/java.base/LICENSE" ]] \
	|| fail "Windows JRE must contain redistribution legal files"

runtime_version="$(sed -n 's/^JAVA_VERSION="\([^"]*\)".*/\1/p' "$WINDOWS_JRE/release" | head -n 1)"
[[ -n "$runtime_version" ]] || fail "Unable to read JAVA_VERSION from the Windows JRE"
if [[ "$runtime_version" == 1.* ]]; then
	runtime_major="${runtime_version#1.}"
	runtime_major="${runtime_major%%.*}"
else
	runtime_major="${runtime_version%%.*}"
fi
[[ "$runtime_major" =~ ^[0-9]+$ ]] && ((runtime_major >= 17)) \
	|| fail "Windows World Builder requires Java 17+; found $runtime_version"

PACKAGE_ASSETS="$ROOT_DIR/release/world-builder"
ICON_CREDITS="$ROOT_DIR/dev/myworld/assets/ui/world-editor/CREDITS.md"
[[ -f "$ICON_CREDITS" ]] || fail "World editor icon credits are missing"
if grep -Eiq 'pending confirmation|pending;|not release-ready' "$ICON_CREDITS"; then
	fail "World editor icon provenance is unresolved; update $ICON_CREDITS before packaging"
fi

if [[ "$SKIP_BUILD" != true ]]; then
	"$ROOT_DIR/scripts/build-server.sh"
	"$ROOT_DIR/scripts/build-client.sh"
	"$ROOT_DIR/scripts/build-world-builder-tools.sh"
fi
require_release_git_state "$SOURCE_COMMIT"

CLIENT_JAR="$ROOT_DIR/Client_Base/Open_RSC_Client.jar"
TOOLS_JAR="$ROOT_DIR/output/world-builder-tools/world-builder-tools.jar"
SERVER_JAR="$ROOT_DIR/server/core.jar"
PLUGINS_JAR="$ROOT_DIR/server/plugins.jar"
SEED_DATABASE="$ROOT_DIR/server/inc/sqlite/myworld_seed.db"

for required_path in \
	"$CLIENT_JAR" \
	"$ROOT_DIR/Client_Base/Cache/audio" \
	"$ROOT_DIR/Client_Base/Cache/video" \
	"$ROOT_DIR/Client_Base/Cache/config.txt" \
	"$SERVER_JAR" \
	"$PLUGINS_JAR" \
	"$ROOT_DIR/server/lib" \
	"$ROOT_DIR/server/conf" \
	"$ROOT_DIR/server/database" \
	"$SEED_DATABASE" \
	"$TOOLS_JAR" \
	"$ROOT_DIR/tools/world-builder/schema" \
	"$ROOT_DIR/LICENSE" \
	"$ROOT_DIR/release/player/ASSET-SOURCES.txt" \
	"$PACKAGE_ASSETS/README.txt" \
	"$PACKAGE_ASSETS/ASSET-SOURCES.txt" \
	"$PACKAGE_ASSETS/world-builder-runtime.conf" \
	"$PACKAGE_ASSETS/Start World Builder.sh" \
	"$PACKAGE_ASSETS/Start World Builder.cmd" \
	"$PACKAGE_ASSETS/Import Map Changes.sh" \
	"$PACKAGE_ASSETS/Import Map Changes.cmd" \
	"$PACKAGE_ASSETS/Undo Last Map Import.sh" \
	"$PACKAGE_ASSETS/Undo Last Map Import.cmd"; do
	[[ -e "$required_path" ]] || fail "Missing release input: $required_path"
done

require_jar_entry() {
	local archive="$1" entry="$2" label="$3"
	jar tf "$archive" | grep -Fx "$entry" >/dev/null \
		|| fail "$label is missing required entry: $entry"
}

require_jar_entry "$CLIENT_JAR" "orsc/WorldBuilderClientProfile.class" "client jar"
require_jar_entry "$SERVER_JAR" "com/openrsc/server/content/worldedit/WorldEditStorageContext.class" "server jar"
require_jar_entry "$SERVER_JAR" "com/openrsc/server/content/worldedit/WorldBuilderRuntimeControl.class" "server jar"
require_jar_entry "$TOOLS_JAR" "com/openrsc/worldbuilder/WorldBuilderCli.class" "tools jar"

jar tf "$CLIENT_JAR" | grep '^myworld-assets/ui/world-editor/' >/dev/null \
	|| fail "Client jar is missing embedded World Builder UI assets"
for native_entry in \
	"linux/x64/org/lwjgl/liblwjgl.so" \
	"linux/x64/org/lwjgl/glfw/libglfw.so" \
	"linux/x64/org/lwjgl/opengl/liblwjgl_opengl.so" \
	"windows/x64/org/lwjgl/lwjgl.dll" \
	"windows/x64/org/lwjgl/glfw/glfw.dll" \
	"windows/x64/org/lwjgl/opengl/lwjgl_opengl.dll"; do
	require_jar_entry "$CLIENT_JAR" "$native_entry" "client jar"
done

server_protocol="$(sed -n 's/^[[:space:]]*client_version:[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$ROOT_DIR/server/myworld.conf" | head -n 1)"
client_protocol="$(sed -n 's/.*CLIENT_VERSION[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$ROOT_DIR/Client_Base/src/orsc/Config.java" | head -n 1)"
runtime_protocol="$(sed -n 's/^[[:space:]]*client_version:[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$PACKAGE_ASSETS/world-builder-runtime.conf" | head -n 1)"
[[ -n "$server_protocol" && "$server_protocol" == "$client_protocol" && "$server_protocol" == "$runtime_protocol" ]] \
	|| fail "Client, server, and Builder runtime protocol versions disagree"

OUTPUT_DIR="$ROOT_DIR/output/releases/world-builder/$VERSION"
STAGING_DIR="$OUTPUT_DIR/staging"
PACKAGE_NAME="Spoiled Milk World Builder"
JAVA_STAGE="$STAGING_DIR/java/$PACKAGE_NAME"
WINDOWS_STAGE="$STAGING_DIR/windows/$PACKAGE_NAME"
JAVA_ARCHIVE="$OUTPUT_DIR/spoiled-milk-world-builder-$VERSION-java.zip"
WINDOWS_ARCHIVE="$OUTPUT_DIR/spoiled-milk-world-builder-$VERSION-windows-x64.zip"

rm -rf "$OUTPUT_DIR"
mkdir -p "$JAVA_STAGE" "$WINDOWS_STAGE" "$OUTPUT_DIR"

stage_builder() {
	local destination="$1"
	local runtime="$destination/builder-runtime"

	mkdir -p "$runtime/Client_Base/Cache" "$runtime/server/inc/sqlite" \
		"$runtime/launcher/schema"
	cp "$CLIENT_JAR" "$runtime/Client_Base/Open_RSC_Client.jar"
	cp -R "$ROOT_DIR/Client_Base/Cache/audio" "$runtime/Client_Base/Cache/audio"
	cp -R "$ROOT_DIR/Client_Base/Cache/video" "$runtime/Client_Base/Cache/video"
	cp "$ROOT_DIR/Client_Base/Cache/config.txt" "$runtime/Client_Base/Cache/config.txt"
	cp "$SERVER_JAR" "$runtime/server/core.jar"
	cp "$PLUGINS_JAR" "$runtime/server/plugins.jar"
	cp -R "$ROOT_DIR/server/lib" "$runtime/server/lib"
	cp -R "$ROOT_DIR/server/conf" "$runtime/server/conf"
	cp -R "$ROOT_DIR/server/database" "$runtime/server/database"
	cp "$SEED_DATABASE" "$runtime/server/inc/sqlite/myworld_seed.db"
	for name in alertwords.txt badwords.txt goodwords.txt globalrules.txt ipbans.txt; do
		cp "$ROOT_DIR/server/$name" "$runtime/server/$name"
	done
	cp "$PACKAGE_ASSETS/world-builder-runtime.conf" "$runtime/server/myworld.conf"
	cp "$TOOLS_JAR" "$runtime/launcher/world-builder-tools.jar"
	cp -R "$ROOT_DIR/tools/world-builder/schema"/. "$runtime/launcher/schema/"

	cp "$PACKAGE_ASSETS/Start World Builder.sh" "$destination/Start World Builder.sh"
	cp "$PACKAGE_ASSETS/Start World Builder.cmd" "$destination/Start World Builder.cmd"
	cp "$PACKAGE_ASSETS/Import Map Changes.sh" "$destination/Import Map Changes.sh"
	cp "$PACKAGE_ASSETS/Import Map Changes.cmd" "$destination/Import Map Changes.cmd"
	cp "$PACKAGE_ASSETS/Undo Last Map Import.sh" "$destination/Undo Last Map Import.sh"
	cp "$PACKAGE_ASSETS/Undo Last Map Import.cmd" "$destination/Undo Last Map Import.cmd"
	chmod +x "$destination/Start World Builder.sh" "$destination/Import Map Changes.sh" \
		"$destination/Undo Last Map Import.sh"
	sed "s/@VERSION@/$VERSION/g; s/@SOURCE_COMMIT@/$SOURCE_COMMIT/g" \
		"$PACKAGE_ASSETS/README.txt" > "$destination/README.txt"
	cp "$ROOT_DIR/LICENSE" "$destination/LICENSE"
	cp "$PACKAGE_ASSETS/ASSET-SOURCES.txt" "$destination/ASSET-SOURCES.txt"
	cp "$ROOT_DIR/release/player/ASSET-SOURCES.txt" "$destination/PLAYER-ASSET-SOURCES.txt"
	cp "$ICON_CREDITS" "$destination/EDITOR-ICON-CREDITS.txt"
	printf '%s\n' "$VERSION" > "$destination/VERSION.txt"
	printf '%s\n' "$SOURCE_COMMIT" > "$destination/SOURCE-COMMIT.txt"
}

stage_builder "$JAVA_STAGE"
stage_builder "$WINDOWS_STAGE"
mkdir -p "$WINDOWS_STAGE/runtime"
cp -R "$WINDOWS_JRE"/. "$WINDOWS_STAGE/runtime/"

for stage in "$JAVA_STAGE" "$WINDOWS_STAGE"; do
	if find "$stage" -type f | grep -E '/(workspace|exports|backups|receipts|logs)/|world_builder\.db$|world-builder\.credential$|credentials\.txt$|uid\.dat$|clientSettings\.conf$|/ip\.txt$|/port\.txt$' >/dev/null; then
		fail "Staged World Builder package contains generated project, credential, identity, or endpoint state"
	fi
done

require_release_git_state "$SOURCE_COMMIT"

(
	cd "$STAGING_DIR/java"
	zip -qr "$JAVA_ARCHIVE" "$PACKAGE_NAME"
)
(
	cd "$STAGING_DIR/windows"
	zip -qr "$WINDOWS_ARCHIVE" "$PACKAGE_NAME"
)
(
	cd "$OUTPUT_DIR"
	sha256sum "$(basename "$JAVA_ARCHIVE")" "$(basename "$WINDOWS_ARCHIVE")" > SHA256SUMS.txt
)

rm -rf "$STAGING_DIR"

printf 'Created World Builder release artifacts:\n'
printf '  %s\n' "$JAVA_ARCHIVE"
printf '  %s\n' "$WINDOWS_ARCHIVE"
printf '  %s\n' "$OUTPUT_DIR/SHA256SUMS.txt"
