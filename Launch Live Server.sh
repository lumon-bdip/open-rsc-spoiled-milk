#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIVE_ROOT="${MYWORLD_LIVE_ROOT:-/tmp/spoiled-milk-live-main}"

if [[ ! -d "$LIVE_ROOT" && -f "$SCRIPT_DIR/scripts/run-hosted-server.sh" ]]; then
  LIVE_ROOT="$SCRIPT_DIR"
fi

export MYWORLD_LIVE_ROOT="$LIVE_ROOT"

read -r -d '' SERVER_COMMAND <<'EOF' || true
set -uo pipefail

LIVE_ROOT="${MYWORLD_LIVE_ROOT:-/tmp/spoiled-milk-live-main}"
COMMAND_FILE="${SM_LIVE_LAUNCH_COMMAND_FILE:-}"

pause_and_exit() {
  local status="$1"
  if [[ -n "$COMMAND_FILE" && -f "$COMMAND_FILE" ]]; then
    rm -f "$COMMAND_FILE"
  fi
  printf '\nPress Enter to close this terminal...'
  read -r _
  exit "$status"
}

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  pause_and_exit 1
}

cd "$LIVE_ROOT" || fail "live worktree not found: $LIVE_ROOT"

printf 'Spoiled Milk live server launcher\n'
printf 'Worktree: %s\n\n' "$PWD"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 \
  || fail "live server must be launched from a git worktree"

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
if [[ "$branch" != "main" ]]; then
  printf 'Switching live worktree from %s to main...\n' "${branch:-unknown}"
  git switch main || fail "could not switch live worktree to main"
fi

if git remote get-url spoiled-milk >/dev/null 2>&1; then
  remote_name="spoiled-milk"
elif git remote get-url origin >/dev/null 2>&1; then
  remote_name="origin"
else
  fail "no spoiled-milk or origin remote is configured"
fi

printf 'Fetching published main from %s...\n' "$remote_name"
git fetch "$remote_name" main || fail "could not fetch $remote_name/main"

printf 'Fast-forwarding live main...\n'
git merge --ff-only "$remote_name/main" || fail "live main cannot be fast-forwarded cleanly"

printf '\nCurrent live server status:\n'
./scripts/live-status.sh || true

printf 'Starting hosted Spoiled Milk server...\n\n'
./scripts/run-hosted-server.sh
status=$?

printf '\nHosted server process exited with status %s.\n' "$status"
pause_and_exit "$status"
EOF

COMMAND_FILE="$(mktemp "${TMPDIR:-/tmp}/spoiled-milk-live-launch.XXXXXX.sh")"
printf '%s\n' "$SERVER_COMMAND" > "$COMMAND_FILE"
chmod 700 "$COMMAND_FILE"
export SM_LIVE_LAUNCH_COMMAND_FILE="$COMMAND_FILE"

if command -v gnome-terminal >/dev/null 2>&1; then
  exec gnome-terminal --title="Spoiled Milk Live Server" -- bash "$COMMAND_FILE"
elif command -v x-terminal-emulator >/dev/null 2>&1; then
  exec x-terminal-emulator -e bash "$COMMAND_FILE"
elif command -v xfce4-terminal >/dev/null 2>&1; then
  exec xfce4-terminal --title="Spoiled Milk Live Server" --command="bash $COMMAND_FILE"
elif command -v konsole >/dev/null 2>&1; then
  exec konsole --new-tab -p tabtitle="Spoiled Milk Live Server" -e bash "$COMMAND_FILE"
elif command -v xterm >/dev/null 2>&1; then
  exec xterm -T "Spoiled Milk Live Server" -e bash "$COMMAND_FILE"
fi

printf 'No graphical terminal launcher was found. Running in this shell.\n\n'
exec bash "$COMMAND_FILE"
