#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

VERSION=""
HOST=""
PORT=""
WINDOWS_JRE=""
ASSETS_CLEARED=false
SKIP_BUILD=false

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage:
  ./scripts/package-player-release.sh \
    --version v0.1.1 \
    --host game.example.org \
    --port 43605 \
    --windows-jre /path/to/temurin-17-windows-x64-jre \
    --assets-cleared

Options:
  --assets-cleared  Acknowledge that redistribution terms for packaged visual
                    assets were checked before creating publishable archives.
  --skip-build      Use an already-built client jar. Intended for packaging
                    regression tests only.
EOF
}

while (($#)); do
  case "$1" in
    --version)
      [[ $# -ge 2 ]] || fail "--version requires a value"
      VERSION="$2"
      shift 2
      ;;
    --host)
      [[ $# -ge 2 ]] || fail "--host requires a value"
      HOST="$2"
      shift 2
      ;;
    --port)
      [[ $# -ge 2 ]] || fail "--port requires a value"
      PORT="$2"
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
  || fail "Version must use semantic form, for example v0.1.1 or v0.1.0-alpha.84"
[[ "$HOST" =~ ^[A-Za-z0-9.-]+$ ]] || fail "Host must be a DNS name or IPv4 address"
case "${HOST,,}" in
  localhost|127.*|0.0.0.0)
    fail "Player release host must be a public host/IP, not $HOST"
    ;;
esac
[[ "$PORT" =~ ^[0-9]+$ ]] && ((PORT >= 1 && PORT <= 65535)) \
  || fail "Port must be between 1 and 65535"
[[ "$ASSETS_CLEARED" == true ]] \
  || fail "Confirm redistribution terms for packaged third-party visual assets before packaging with --assets-cleared"

for command_name in jar zip sha256sum; do
  command -v "$command_name" >/dev/null 2>&1 || fail "Missing dependency: $command_name"
done

[[ -d "$WINDOWS_JRE" ]] || fail "Windows JRE directory does not exist: $WINDOWS_JRE"
[[ -f "$WINDOWS_JRE/bin/java.exe" ]] || fail "Windows JRE must contain bin/java.exe"
[[ -f "$WINDOWS_JRE/release" ]] || fail "Windows JRE must contain its release metadata file"
[[ -f "$WINDOWS_JRE/LICENSE" || -f "$WINDOWS_JRE/NOTICE" || -f "$WINDOWS_JRE/legal/java.base/LICENSE" ]] \
  || fail "Windows JRE must contain redistribution legal files"

runtime_version="$(sed -n 's/^JAVA_VERSION="\([^"]*\)".*/\1/p' "$WINDOWS_JRE/release" | head -n 1)"
[[ -n "$runtime_version" ]] || fail "Unable to read JAVA_VERSION from Windows JRE release metadata"
if [[ "$runtime_version" == 1.* ]]; then
  runtime_major="${runtime_version#1.}"
  runtime_major="${runtime_major%%.*}"
else
  runtime_major="${runtime_version%%.*}"
fi
[[ "$runtime_major" =~ ^[0-9]+$ ]] && ((runtime_major >= 17)) \
  || fail "Windows player bundle requires a Java 17+ runtime; found $runtime_version"

if [[ "$SKIP_BUILD" != true ]]; then
  "$ROOT_DIR/scripts/build-client.sh"
fi

CLIENT_JAR="$ROOT_DIR/Client_Base/Open_RSC_Client.jar"
CLIENT_CACHE="$ROOT_DIR/Client_Base/Cache"
PACKAGE_ASSETS="$ROOT_DIR/release/player"

for required_path in \
  "$CLIENT_JAR" \
  "$CLIENT_CACHE/audio" \
  "$CLIENT_CACHE/video" \
  "$CLIENT_CACHE/config.txt" \
  "$ROOT_DIR/LICENSE" \
  "$PACKAGE_ASSETS/PLAYER-README.txt" \
  "$PACKAGE_ASSETS/ASSET-SOURCES.txt" \
  "$PACKAGE_ASSETS/play-spoiled-milk.sh" \
  "$PACKAGE_ASSETS/update-spoiled-milk.sh" \
  "$PACKAGE_ASSETS/update-spoiled-milk.ps1" \
  "$PACKAGE_ASSETS/Play Spoiled Milk.cmd" \
  "$PACKAGE_ASSETS/Update Spoiled Milk.cmd" \
  "$PACKAGE_ASSETS/Play Spoiled Milk Windows.cmd"; do
  [[ -e "$required_path" ]] || fail "Missing release input: $required_path"
done

jar tf "$CLIENT_JAR" | grep '^myworld-assets/' >/dev/null \
  || fail "Client jar is missing embedded MyWorld visual assets; rebuild the client before packaging"

require_client_jar_entry() {
  local entry="$1"

  jar tf "$CLIENT_JAR" | grep -Fx "$entry" >/dev/null \
    || fail "Client jar is missing OpenGL runtime entry: $entry. Run scripts/download-lwjgl.sh with the release native classifiers and rebuild."
}

for opengl_runtime_entry in \
  "linux/x64/org/lwjgl/liblwjgl.so" \
  "linux/x64/org/lwjgl/glfw/libglfw.so" \
  "linux/x64/org/lwjgl/opengl/liblwjgl_opengl.so" \
  "macos/x64/org/lwjgl/liblwjgl.dylib" \
  "macos/x64/org/lwjgl/glfw/libglfw.dylib" \
  "macos/x64/org/lwjgl/opengl/liblwjgl_opengl.dylib" \
  "macos/arm64/org/lwjgl/liblwjgl.dylib" \
  "macos/arm64/org/lwjgl/glfw/libglfw.dylib" \
  "macos/arm64/org/lwjgl/opengl/liblwjgl_opengl.dylib" \
  "windows/x64/org/lwjgl/lwjgl.dll" \
  "windows/x64/org/lwjgl/glfw/glfw.dll" \
  "windows/x64/org/lwjgl/opengl/lwjgl_opengl.dll"; do
  require_client_jar_entry "$opengl_runtime_entry"
done

require_launcher_flag() {
  local launcher="$1"
  local flag="$2"

  grep -F -- "$flag" "$launcher" >/dev/null \
    || fail "Player launcher is missing renderer-v2 flag $flag: $launcher"
}

for launcher in \
  "$PACKAGE_ASSETS/play-spoiled-milk.sh" \
  "$PACKAGE_ASSETS/Play Spoiled Milk.cmd" \
  "$PACKAGE_ASSETS/Play Spoiled Milk Windows.cmd"; do
  require_launcher_flag "$launcher" "-Xms512m"
  require_launcher_flag "$launcher" "-Xmx2g"
  require_launcher_flag "$launcher" "-Dsun.java2d.opengl=true"
  for renderer_flag in \
    "-Dspoiledmilk.directFramebuffer=true" \
    "-Dspoiledmilk.openglPresenter=true" \
    "-Dspoiledmilk.openglInput=true" \
    "-Dspoiledmilk.openglPrimaryWindow=true" \
    "-Dspoiledmilk.openglVsync=false" \
    "-Dspoiledmilk.renderer3DGeometryCapture=true" \
    "-Dspoiledmilk.openglWorldMesh=true" \
    "-Dspoiledmilk.openglWorldMeshTexturedVisible=true" \
    "-Dspoiledmilk.openglWorldMeshTexturedStaticVisible=true" \
    "-Dspoiledmilk.openglWorldStaticTextures=true" \
    "-Dspoiledmilk.openglWorldTexturedAlpha=1.0" \
    "-Dspoiledmilk.openglWorldChunksTexturedVisible=true" \
    "-Dspoiledmilk.openglWorldChunksReplacementComposite=true" \
    "-Dspoiledmilk.openglWorldChunksTrustedReplacement=true" \
    "-Dspoiledmilk.openglWorldChunksResidentObjects=true" \
    "-Dspoiledmilk.openglWorldChunksSpatialCull=true" \
    "-Dspoiledmilk.openglWorldChunkUploadBudgetMs=3.0" \
    "-Dspoiledmilk.openglWorldChunksRemasterLightingShader=true" \
    "-Dspoiledmilk.openglWorldSpritesVisible=true" \
    "-Dspoiledmilk.skipLegacyWorldRaster=true" \
    "-Dspoiledmilk.modernClientLoop=true"; do
    require_launcher_flag "$launcher" "$renderer_flag"
  done
done

OUTPUT_DIR="$ROOT_DIR/output/releases/$VERSION"
STAGING_DIR="$OUTPUT_DIR/staging"
JAVA_NAME="spoiled-milk-$VERSION-java"
WINDOWS_NAME="spoiled-milk-$VERSION-windows-x64"
JAVA_DIR="$STAGING_DIR/$JAVA_NAME"
WINDOWS_DIR="$STAGING_DIR/$WINDOWS_NAME"

rm -rf "$OUTPUT_DIR"
mkdir -p "$JAVA_DIR/Cache" "$WINDOWS_DIR/Cache" "$OUTPUT_DIR"

stage_client_files() {
  local destination="$1"

  cp "$CLIENT_JAR" "$destination/Spoiled_Milk_Client.jar"
  cp -R "$CLIENT_CACHE/audio" "$destination/Cache/audio"
  cp -R "$CLIENT_CACHE/video" "$destination/Cache/video"
  cp "$CLIENT_CACHE/config.txt" "$destination/Cache/config.txt"
  printf '%s\n' "$HOST" > "$destination/Cache/ip.txt"
  printf '%s\n' "$PORT" > "$destination/Cache/port.txt"
  cp "$ROOT_DIR/LICENSE" "$destination/LICENSE"
  cp "$PACKAGE_ASSETS/ASSET-SOURCES.txt" "$destination/ASSET-SOURCES.txt"
  sed "s/@VERSION@/$VERSION/g" "$PACKAGE_ASSETS/PLAYER-README.txt" > "$destination/README.txt"
  printf '%s\n' "$VERSION" > "$destination/VERSION.txt"
}

stage_client_files "$JAVA_DIR"
stage_client_files "$WINDOWS_DIR"

cp "$PACKAGE_ASSETS/play-spoiled-milk.sh" "$JAVA_DIR/play-spoiled-milk.sh"
cp "$PACKAGE_ASSETS/Play Spoiled Milk.cmd" "$JAVA_DIR/Play Spoiled Milk.cmd"
sed "s/@VERSION@/$VERSION/g" "$PACKAGE_ASSETS/update-spoiled-milk.sh" > "$JAVA_DIR/update-spoiled-milk.sh"
sed "s/@VERSION@/$VERSION/g; s/@PACKAGE_KIND@/java/g" "$PACKAGE_ASSETS/update-spoiled-milk.ps1" > "$JAVA_DIR/update-spoiled-milk.ps1"
cp "$PACKAGE_ASSETS/Update Spoiled Milk.cmd" "$JAVA_DIR/Update Spoiled Milk.cmd"
chmod +x "$JAVA_DIR/play-spoiled-milk.sh"
chmod +x "$JAVA_DIR/update-spoiled-milk.sh"

cp "$PACKAGE_ASSETS/Play Spoiled Milk Windows.cmd" "$WINDOWS_DIR/Play Spoiled Milk.cmd"
sed "s/@VERSION@/$VERSION/g; s/@PACKAGE_KIND@/windows-x64/g" "$PACKAGE_ASSETS/update-spoiled-milk.ps1" > "$WINDOWS_DIR/update-spoiled-milk.ps1"
cp "$PACKAGE_ASSETS/Update Spoiled Milk.cmd" "$WINDOWS_DIR/Update Spoiled Milk.cmd"
mkdir -p "$WINDOWS_DIR/runtime"
cp -R "$WINDOWS_JRE"/. "$WINDOWS_DIR/runtime/"

(
  cd "$STAGING_DIR"
  zip -qr "$OUTPUT_DIR/$JAVA_NAME.zip" "$JAVA_NAME"
  zip -qr "$OUTPUT_DIR/$WINDOWS_NAME.zip" "$WINDOWS_NAME"
)
(
  cd "$OUTPUT_DIR"
  sha256sum "$JAVA_NAME.zip" "$WINDOWS_NAME.zip" > SHA256SUMS.txt
)

rm -rf "$STAGING_DIR"

printf 'Created player release artifacts:\n'
printf '  %s\n' "$OUTPUT_DIR/$JAVA_NAME.zip"
printf '  %s\n' "$OUTPUT_DIR/$WINDOWS_NAME.zip"
printf '  %s\n' "$OUTPUT_DIR/SHA256SUMS.txt"
