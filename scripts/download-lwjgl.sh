#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

LWJGL_VERSION="${LWJGL_VERSION:-3.3.4}"
LWJGL_REPO_URL="${LWJGL_REPO_URL:-https://repo1.maven.org/maven2}"
LWJGL_LIB_DIR="${LWJGL_LIB_DIR:-$ROOT_DIR/PC_Client/lib/lwjgl}"
LWJGL_MODULES="${LWJGL_MODULES:-lwjgl lwjgl-glfw lwjgl-opengl}"

detect_native_classifier() {
  local os_name arch
  os_name="$(uname -s | tr '[:upper:]' '[:lower:]')"
  arch="$(uname -m | tr '[:upper:]' '[:lower:]')"

  case "$os_name" in
    linux*)
      printf 'natives-linux'
      ;;
    darwin*)
      case "$arch" in
        arm64|aarch64)
          printf 'natives-macos-arm64'
          ;;
        *)
          printf 'natives-macos'
          ;;
      esac
      ;;
    mingw*|msys*|cygwin*)
      printf 'natives-windows'
      ;;
    *)
      printf 'natives-linux'
      ;;
  esac
}

LWJGL_NATIVE_CLASSIFIERS="${LWJGL_NATIVE_CLASSIFIERS:-$(detect_native_classifier)}"

download_file() {
  local url="$1"
  local destination="$2"

  if [[ -f "$destination" ]]; then
    printf 'Already present: %s\n' "$destination"
    return
  fi

  printf 'Downloading %s\n' "$url"
  curl --fail --location --retry 3 --retry-delay 2 --output "$destination.tmp" "$url"
  mv "$destination.tmp" "$destination"
}

main() {
  command -v curl >/dev/null 2>&1 || {
    printf 'FAIL: curl is required to download LWJGL dependencies.\n' >&2
    exit 1
  }

  mkdir -p "$LWJGL_LIB_DIR"

  local module classifier group_path module_path jar_name url
  group_path="org/lwjgl"

  for module in $LWJGL_MODULES; do
    module_path="$group_path/$module/$LWJGL_VERSION"
    jar_name="$module-$LWJGL_VERSION.jar"
    url="$LWJGL_REPO_URL/$module_path/$jar_name"
    download_file "$url" "$LWJGL_LIB_DIR/$jar_name"

    for classifier in $LWJGL_NATIVE_CLASSIFIERS; do
      jar_name="$module-$LWJGL_VERSION-$classifier.jar"
      url="$LWJGL_REPO_URL/$module_path/$jar_name"
      download_file "$url" "$LWJGL_LIB_DIR/$jar_name"
    done
  done

  printf 'LWJGL %s dependencies are in %s\n' "$LWJGL_VERSION" "$LWJGL_LIB_DIR"
  printf 'Native classifiers: %s\n' "$LWJGL_NATIVE_CLASSIFIERS"
}

main "$@"
