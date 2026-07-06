#!/usr/bin/env python3
"""Guard social-list username lookup normalization."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GAME_DATABASE = ROOT / "server/src/com/openrsc/server/database/GameDatabase.java"
FRIEND_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/FriendHandler.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, snippet: str, description: str) -> None:
    if snippet not in text:
        fail(f"missing {description}: {snippet}")


def main() -> None:
    game_database = GAME_DATABASE.read_text(encoding="utf-8")
    friend_handler = FRIEND_HANDLER.read_text(encoding="utf-8")

    require(
        game_database,
        "final String canonicalUsername = canonicalizeUsernameForLookup(username);",
        "proper username lookup canonicalization",
    )
    require(
        game_database,
        "DataConversions.hashToUsername(usernameHash)",
        "hash-based username display canonicalization",
    )
    require(
        game_database,
        "return queryGetProperUsernameCapitalization(canonicalUsername);",
        "database lookup using canonical social name",
    )
    require(
        friend_handler,
        "long friendHash = DataConversions.usernameToHash(friendName);",
        "social hash lookup that treats underscores and spaces identically",
    )

    print("PASS: social name lookups canonicalize separators before database lookup")


if __name__ == "__main__":
    main()
