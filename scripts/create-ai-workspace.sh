#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_DIR="${ROOT_DIR:-$SCRIPT_ROOT}"

workspace="${1:-}"
base_ref="${2:-}"

usage() {
  cat <<'USAGE'
Usage: ./scripts/create-ai-workspace.sh <major-work|plan-work|small-tweaks|odds-and-ends> [base-ref]

Creates a sibling Git worktree for one AI/contributor work stream.

Examples:
  ./scripts/create-ai-workspace.sh plan-work
  ./scripts/create-ai-workspace.sh small-tweaks main
USAGE
}

if [[ -z "$workspace" || "$workspace" == "-h" || "$workspace" == "--help" ]]; then
  usage
  exit 0
fi

case "$workspace" in
  major-work|plan-work|small-tweaks|odds-and-ends)
    ;;
  *)
    printf 'FAIL: unknown workspace: %s\n\n' "$workspace" >&2
    usage >&2
    exit 1
    ;;
esac

if [[ -z "$base_ref" ]]; then
  if git -C "$ROOT_DIR" rev-parse --verify --quiet spoiled-milk/main >/dev/null; then
    base_ref="spoiled-milk/main"
  else
    base_ref="main"
  fi
fi

parent_dir="${WORKSPACE_PARENT:-$HOME}"
target_dir="$parent_dir/Core-Framework-$workspace"
branch_name="workspace/$workspace"
guide_path="$ROOT_DIR/docs/workspaces/$workspace.md"

[[ -f "$guide_path" ]] || {
  printf 'FAIL: missing workspace guide: %s\n' "$guide_path" >&2
  exit 1
}

if [[ -e "$target_dir" ]]; then
  printf 'FAIL: workspace path already exists: %s\n' "$target_dir" >&2
  printf 'Run ./scripts/workspace-status.sh to inspect existing worktrees.\n' >&2
  exit 1
fi

printf 'Creating workspace: %s\n' "$workspace"
printf 'Target:             %s\n' "$target_dir"
printf 'Branch:             %s\n' "$branch_name"
printf 'Base ref:           %s\n' "$base_ref"

if git -C "$ROOT_DIR" rev-parse --verify --quiet "$branch_name" >/dev/null; then
  git -C "$ROOT_DIR" worktree add "$target_dir" "$branch_name"
else
  git -C "$ROOT_DIR" worktree add -b "$branch_name" "$target_dir" "$base_ref"
fi

cp "$guide_path" "$target_dir/AI_WORKSPACE.md"

printf '\nWorkspace ready.\n'
printf 'Open this folder in the AI session:\n'
printf '  %s\n' "$target_dir"
printf 'The local AI instructions are in:\n'
printf '  %s/AI_WORKSPACE.md\n' "$target_dir"
