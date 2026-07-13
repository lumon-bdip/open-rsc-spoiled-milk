#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

CONFIRM_STOP=false
DATABASE_RECOVERED=false
SHUTDOWN_AUTHORIZED=false
UPDATE_WINDOW_COMPLETE=false
while (($#)); do
  case "$1" in
    --yes)
      CONFIRM_STOP=true
      ;;
    --shutdown-authorized)
      SHUTDOWN_AUTHORIZED=true
      ;;
    --update-window-complete)
      UPDATE_WINDOW_COMPLETE=true
      ;;
    --database-recovered)
      DATABASE_RECOVERED=true
      ;;
    *)
      myworld_fail "Unknown option: $1. A fallback stop requires --yes --shutdown-authorized --update-window-complete; --database-recovered is only for a separately recovered unsafe SQLite descriptor."
      ;;
  esac
  shift
done

pids="$(myworld_listener_pids_for_port "$MYWORLD_PUBLIC_PORT")"
if [[ -z "$pids" ]]; then
  printf 'No hosted server is listening on public port %s.\n' "$MYWORLD_PUBLIC_PORT"
  exit 0
fi

live_root_real="$(myworld_realpath "$MYWORLD_LIVE_ROOT" 2>/dev/null || true)"
[[ -n "$live_root_real" ]] || myworld_fail "Configured live worktree does not exist: $MYWORLD_LIVE_ROOT"

if [[ "$CONFIRM_STOP" == true ]]; then
  [[ "$SHUTDOWN_AUTHORIZED" == true ]] \
    || myworld_fail "Refusing to stop the public server without --shutdown-authorized. This flag may be used only after the user explicitly authorizes this live shutdown."
  [[ "$UPDATE_WINDOW_COMPLETE" == true ]] \
    || myworld_fail "Refusing to stop the public server without --update-window-complete. Run the in-game ::update command after authorization and let its full warning countdown complete first."
fi

while IFS= read -r pid; do
  [[ -n "$pid" ]] || continue
  root="$(myworld_server_root_for_pid "$pid")"
  conf="$(myworld_server_conf_for_pid "$pid")"
  root_real="$(myworld_realpath "$root" 2>/dev/null || true)"

  if [[ "$root_real" != "$live_root_real" || "$conf" != "myworld-host.conf" ]]; then
    myworld_fail "Refusing to stop PID $pid on public port $MYWORLD_PUBLIC_PORT because it is not the approved hosted server. Run ./scripts/live-status.sh and inspect manually."
  fi

  if ! myworld_process_uses_only_external_live_database "$pid"; then
    if [[ "$DATABASE_RECOVERED" != true ]]; then
      printf 'Unsafe runtime database descriptor(s) for PID %s:\n' "$pid" >&2
      myworld_process_database_targets "$pid" | sed 's/^/  /' >&2
      myworld_fail "Refusing to stop a server that may be writing a wrong or deleted SQLite inode. Recover and integrity-check every live database descriptor first, then rerun with --yes --shutdown-authorized --update-window-complete --database-recovered."
    fi
    printf 'WARN: stopping after explicit confirmation that unsafe database descriptors were recovered.\n' >&2
  fi

  if [[ "$CONFIRM_STOP" == true ]]; then
    printf 'Stopping hosted Spoiled Milk server PID %s from %s\n' "$pid" "$root_real"
    kill "$pid"
  else
    printf 'Would stop hosted Spoiled Milk server PID %s from %s\n' "$pid" "$root_real"
    printf 'Fallback only: after explicit shutdown permission and the full ::update countdown, run:\n'
    printf '  %s --yes --shutdown-authorized --update-window-complete\n' "$0"
  fi
done <<<"$pids"
