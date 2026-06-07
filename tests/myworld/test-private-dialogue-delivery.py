#!/usr/bin/env python3
"""Validate recipient-targeted dialogue is not sent to nearby players."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
UPDATER = ROOT / "server/src/com/openrsc/server/GameStateUpdater.java"
CLIENT_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, snippet: str, description: str) -> None:
    if snippet not in text:
        fail(f"missing {description}: {snippet}")


def main() -> None:
    updater = UPDATER.read_text(encoding="utf-8")
    client = CLIENT_HANDLER.read_text(encoding="utf-8")

    require(
        updater,
        "if (chatMessage.getRecipient() == null || chatMessage.getRecipient() == player)",
        "NPC chat recipient filtering",
    )
    require(
        updater,
        "boolean directedToViewer = chatMessage.getRecipient() == player;",
        "directed player dialogue recipient filtering",
    )
    require(
        updater,
        "boolean publicChatVisible = chatMessage.getRecipient() == null",
        "separation between public and directed player chat",
    )
    require(
        updater,
        "if ((directedToViewer || publicChatVisible)",
        "player chat visibility gate",
    )

    if "|| updateFlags.getChatMessage().getRecipient() != null" in updater:
        fail("recipient-targeted dialogue must not be made visible to every nearby player")

    require(
        client,
        "if (mc.getLocalPlayer().serverIndex == chatRecipient)",
        "local-player NPC dialogue history gate",
    )
    require(
        client,
        "MessageType.QUEST, 0, null, \"@yel@\"",
        "NPC dialogue routing into the current Game tab message bucket",
    )
    require(
        client,
        "if (mc.getLocalPlayer() == player)",
        "local-player scripted dialogue history gate",
    )

    print("PASS: recipient-targeted dialogue is private to the participating player")


if __name__ == "__main__":
    main()
