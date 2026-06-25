#!/usr/bin/env python3
"""Guard new-character appearance flow across server and client login."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ACTION_SENDER = ROOT / "server/src/com/openrsc/server/net/rsc/ActionSender.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def forbid(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"retired {description} still present: {needle}")


def main() -> None:
    action_sender = ACTION_SENDER.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")

    require(
        action_sender,
        "final boolean pendingTutorialAppearance = player.isChangingAppearance()\n"
        "\t\t\t\t\t|| player.getCache().hasKey(\"tutorial_appearance\");",
        "pending tutorial appearance detection",
    )
    require(
        action_sender,
        "if (firstLogin || pendingTutorialAppearance) {\n"
        "\t\t\t\t\tplayer.getCache().store(\"tutorial_appearance\", false);\n\n"
        "\t\t\t\t\tsendAppearanceScreen(player);\n"
        "\t\t\t\t}",
        "appearance screen send for first or interrupted character creation",
    )
    forbid(
        action_sender,
        "if (!player.isChangingAppearance() && player.getCache().hasKey(\"tutorial_appearance\"))",
        "appearance resend guard that skipped already-pending character creation",
    )
    require(
        mudclient,
        "if (show && this.panelAppearance == null) {\n"
        "\t\t\tthis.createAppearancePanel(-24595, Config.S_CHARACTER_CREATION_MODE);\n"
        "\t\t}",
        "lazy appearance panel creation when opcode 59 arrives",
    )
    require(
        mudclient,
        "this.appearanceInputBlockTicks = 2;\n"
        "\t\t\tthis.currentMouseButtonDown = 0;\n"
        "\t\t\tthis.lastMouseButtonDown = 0;\n"
        "\t\t\tthis.enterPressed = false;",
        "stale login input clearing when character creator opens",
    )
    require(
        mudclient,
        "if (this.appearanceInputBlockTicks > 0) {\n"
        "\t\t\t\t\tthis.appearanceInputBlockTicks--;\n"
        "\t\t\t\t\tthis.currentMouseButtonDown = 0;\n"
        "\t\t\t\t\tthis.lastMouseButtonDown = 0;\n"
        "\t\t\t\t} else {\n"
        "\t\t\t\t\tthis.handleAppearancePanelControls(86);\n"
        "\t\t\t\t}",
        "appearance panel input warmup guard",
    )
    require(
        mudclient,
        "&& !this.loadingArea\n"
        "\t\t\t&& !this.isFullScreenModalUiActive();",
        "renderer-v2 world disabled during full-screen modal UI",
    )
    require(
        mudclient,
        "public boolean isFullScreenModalUiActive() {\n"
        "\t\treturn this.deathScreenTimeout != 0\n"
        "\t\t\t|| this.showAppearanceChange",
        "full-screen modal UI helper includes character creator",
    )

    print("PASS: character creation login opens appearance UI")


if __name__ == "__main__":
    main()
