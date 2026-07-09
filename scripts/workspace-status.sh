#!/usr/bin/env bash
set -euo pipefail

SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$SCRIPT_ROOT/scripts/ai-workspace.sh" status

printf '\nLive hosted server process:\n'
if [[ -x "$SCRIPT_ROOT/scripts/live-status.sh" ]]; then
  "$SCRIPT_ROOT/scripts/live-status.sh" || \
    printf 'WARN: live-status reported a problem or no live server.\n'
else
  printf 'WARN: manager live-status script is unavailable.\n'
fi
