#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

myworld_load_local_env

GENERATOR_MODE="$(myworld_resolve_generator_mode "$@")"
SERVER_CONF="myworld"

myworld_require_private_dev_conf "$SERVER_CONF"
myworld_require_port_free "$(myworld_conf_value "$SERVER_CONF" server_port)"
myworld_print_server_launch_banner "PRIVATE SPOILED MILK DEV SERVER - NOT PUBLIC HOSTED ALPHA" "$SERVER_CONF"
myworld_prepare_generated_artifacts "$GENERATOR_MODE"
myworld_require_java_17_for_zgc
myworld_write_launch_marker "PRIVATE SPOILED MILK DEV SERVER - NOT PUBLIC HOSTED ALPHA" "$SERVER_CONF"
myworld_ant_server runserverzgc -DconfFile="$SERVER_CONF"
