#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

printf '============================================================\n'
printf 'Spoiled Milk Workspace Status\n'
printf '============================================================\n\n'

printf 'Current checkout:\n'
printf '  Path:   %s\n' "$ROOT_DIR"
printf '  Branch: %s\n' "$(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || printf 'unknown')"
printf '  Commit: %s\n' "$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
printf '\n'

printf 'Live hosted server:\n'
live_root="${MYWORLD_LIVE_ROOT:-/tmp/spoiled-milk-live-main}"
if [[ -x "$live_root/scripts/live-status.sh" ]]; then
  if (cd "$live_root" && ./scripts/live-status.sh); then
    true
  else
    printf 'WARN: live-status reported a problem or no live server.\n'
  fi
elif [[ -x "$ROOT_DIR/scripts/live-status.sh" ]]; then
  if "$ROOT_DIR/scripts/live-status.sh"; then
    true
  else
    printf 'WARN: live-status reported a problem or no live server.\n'
  fi
else
  printf 'WARN: live-status script is not executable under %s or %s\n' "$live_root" "$ROOT_DIR"
fi
printf '\n'

printf 'Git worktrees:\n'
git -C "$ROOT_DIR" worktree list
printf '\n'

printf 'Named AI workspace targets:\n'
parent_dir="${WORKSPACE_PARENT:-$HOME}"
for workspace in plan-work small-tweaks odds-and-ends; do
  target_dir="$parent_dir/Core-Framework-$workspace"
  if [[ -d "$target_dir/.git" || -f "$target_dir/.git" ]]; then
    branch="$(git -C "$target_dir" rev-parse --abbrev-ref HEAD 2>/dev/null || printf 'unknown')"
    commit="$(git -C "$target_dir" rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
    dirty="$(git -C "$target_dir" status --short 2>/dev/null | wc -l | tr -d ' ')"
    printf '  %-13s %s  branch=%s commit=%s dirty=%s\n' "$workspace" "$target_dir" "$branch" "$commit" "$dirty"
  else
    printf '  %-13s not created  expected=%s\n' "$workspace" "$target_dir"
  fi
done
