#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ONLINE_LIST_PATH = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "interfaces" / "misc" / "OnlineListInterface.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    source = ONLINE_LIST_PATH.read_text(encoding="utf-8")
    required = (
        "private static final int PANEL_WIDTH = 184;",
        "private static final int PANEL_MARGIN = 6;",
        "anchorToBottomRight();",
        "userComponent.setText(text);",
        "userComp.setLocation(5, currentY + 3);",
        "currentY += textHeight;",
        "panel.reposition(scroll, getX(), getY() + TITLE_HEIGHT, getWidth(), getHeight() - TITLE_HEIGHT);",
        "getClient().getGameWidth() - getWidth() - PANEL_MARGIN",
        "getClient().getGameHeight() - getHeight() - PANEL_MARGIN",
    )
    for snippet in required:
        if snippet not in source:
            fail(f"online list layout missing expected snippet: {snippet}")
    if 'text + ", "' in source:
        fail("online list still formats players as comma-wrapped inline text")
    print("PASS: online list is bottom-right, narrow, and single-column scrollable")


if __name__ == "__main__":
    main()
