#!/usr/bin/env python3
"""Guard against account-specific login patches."""

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PLAYER_LOGIN = ROOT / "server/plugins/com/openrsc/server/plugins/shared/PlayerLogin.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    text = PLAYER_LOGIN.read_text(encoding="utf-8")
    forbidden = (
        "setGroupID(",
        "normalizeMyWorldStaffName",
        "anactualduck",
        "devduck",
        "REWARD_BACKFILL",
        "ifCompletedGive",
        "ifCompletedEnsure",
    )
    for snippet in forbidden:
        if snippet in text:
            fail(f"PlayerLogin should not contain account-specific login patch: {snippet}")

    print("PASS: player login policy is not account-specific")


if __name__ == "__main__":
    main()
