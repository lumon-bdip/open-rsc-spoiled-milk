#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"
source "$SCRIPT_ROOT/scripts/lib/myworld-common.sh"

print_pid_status() {
  local pid="$1"
  local port="$2"
  local label="$3"
  local root conf branch commit db_name server_name args marker_path marker_state

  root="$(myworld_server_root_for_pid "$pid")"
  conf="$(myworld_server_conf_for_pid "$pid")"
  args="$(myworld_process_args "$pid")"
  marker_state="missing"

  if [[ -n "$root" && -n "$conf" && -f "$root/server/$conf" ]]; then
    branch="$(git -C "$root" rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
    commit="$(git -C "$root" rev-parse HEAD 2>/dev/null || true)"
    ROOT_DIR="$root"
    db_name="$(myworld_conf_value "$conf" db_name)"
    server_name="$(myworld_conf_value "$conf" server_name)"
    marker_path="$root/server/run/server-${port}.env"
  else
    branch="unknown"
    commit="unknown"
    db_name="unknown"
    server_name="unknown"
    marker_path=""
  fi

  printf '%s\n' "$label"
  printf '  PID:      %s\n' "$pid"
  printf '  Port:     %s\n' "$port"
  printf '  Worktree: %s\n' "${root:-unknown}"
  printf '  Branch:   %s\n' "${branch:-unknown}"
  printf '  Commit:   %s\n' "${commit:-unknown}"
  printf '  Config:   %s\n' "${conf:-unknown}"
  printf '  DB:       %s\n' "${db_name:-unknown}"
  printf '  Server:   %s\n' "${server_name:-unknown}"

  if [[ -n "$marker_path" && -f "$marker_path" ]]; then
    marker_state="present"
    marker_label=""
    marker_root=""
    marker_branch=""
    marker_commit=""
    marker_config=""
    marker_db=""
    marker_server=""
    marker_port=""
    marker_started_at=""
    # shellcheck disable=SC1090
    source "$marker_path"
    printf '  Launch:   %s\n' "${marker_label:-unknown}"
    printf '  Started:  %s\n' "${marker_started_at:-unknown}"
    printf '  Marker:   %s %s %s %s\n' "${marker_branch:-unknown}" "${marker_commit:-unknown}" "${marker_config:-unknown}" "${marker_db:-unknown}"
  else
    printf '  Launch:   missing marker; server was likely started before launch markers existed\n'
  fi

  if [[ -z "$root" || -z "$conf" ]]; then
    printf '  Verdict:  UNKNOWN PROCESS\n'
    printf '  Args:     %s\n' "$args"
  elif [[ "$port" == "$MYWORLD_PUBLIC_PORT" ]]; then
    local live_root_real root_real
    live_root_real="$(myworld_realpath "$MYWORLD_LIVE_ROOT" 2>/dev/null || true)"
    root_real="$(myworld_realpath "$root" 2>/dev/null || true)"
    if [[ "$root_real" == "$live_root_real" && "$branch" == "main" && "$conf" == "myworld-host.conf" && "$db_name" == "spoiled_milk_alpha" ]]; then
      if [[ "$marker_state" == "present" && "${marker_config:-}" == "myworld-host.conf" && "${marker_db:-}" == "spoiled_milk_alpha" && "${marker_port:-}" == "$MYWORLD_PUBLIC_PORT" ]]; then
        printf '  Verdict:  OK LIVE HOSTED SERVER\n'
      else
        printf '  Verdict:  OK live identity; restart once convenient to activate launch-marker tracking\n'
      fi
    else
      printf '  Verdict:  DANGER: public port is not the approved live server\n'
    fi
  else
    printf '  Verdict:  private/non-public port\n'
  fi
  printf '\n'
}

print_port_status() {
  local port="$1"
  local label="$2"
  local pids pid found=0

  pids="$(myworld_listener_pids_for_port "$port")"
  if [[ -z "$pids" ]]; then
    printf '%s\n' "$label"
    printf '  Port:    %s\n' "$port"
    printf '  Verdict: no listener\n\n'
    return
  fi

  while IFS= read -r pid; do
    [[ -n "$pid" ]] || continue
    found=1
    print_pid_status "$pid" "$port" "$label"
  done <<<"$pids"

  [[ "$found" == 1 ]] || printf '%s\n  Port: %s\n  Verdict: no listener\n\n' "$label" "$port"
}

printf 'Spoiled Milk server status\n'
printf 'Live root: %s\n\n' "$MYWORLD_LIVE_ROOT"
print_port_status "$MYWORLD_PUBLIC_PORT" "PUBLIC PLAYER PORT"
print_port_status "$MYWORLD_DEV_PORT" "PRIVATE DEV PORT"
