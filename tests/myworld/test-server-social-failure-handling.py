#!/usr/bin/env python3
"""Exercise social database-result classification and guard private-message redaction."""

from pathlib import Path
import shutil
import subprocess
import tempfile
import textwrap


ROOT = Path(__file__).resolve().parents[2]
LOOKUP_RESULT = ROOT / "server/src/com/openrsc/server/database/DatabaseLookupResult.java"
DATABASE_EXCEPTION = ROOT / "server/src/com/openrsc/server/database/GameDatabaseException.java"
FRIEND_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/FriendHandler.java"
GAME_STATE_UPDATER = ROOT / "server/src/com/openrsc/server/GameStateUpdater.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def method_body(source: str, signature: str) -> str:
    start = source.find(signature)
    require(start >= 0, f"missing method: {signature}")
    opening_brace = source.find("{", start)
    require(opening_brace >= 0, f"missing method body: {signature}")
    depth = 0
    for index in range(opening_brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[opening_brace + 1 : index]
    fail(f"unterminated method body: {signature}")
    return ""


def logger_call(method: str) -> str:
    start = method.find("LOGGER.error(")
    require(start >= 0, "missing database failure log")
    end = method.find(");", start)
    require(end >= 0, "unterminated database failure log")
    return method[start : end + 2]


def run_lookup_fixture() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")

    harness = textwrap.dedent(
        """
        package com.openrsc.server.database;

        public final class DatabaseLookupResultHarness {
            private static final String SENSITIVE_MARKER =
                "private-message=meet-at-bank credential=hunter2 username=Alice";

            private static void check(boolean condition, String message) {
                if (!condition) {
                    throw new AssertionError(message);
                }
            }

            public static void main(String[] args) {
                DatabaseLookupResult<String> found = DatabaseLookupResult.resolve(
                    () -> "Canonical Name", value -> value != null);
                check(found.isFound(), "record result was not classified as found");
                check("Canonical Name".equals(found.getValue()), "found value was not retained");

                DatabaseLookupResult<String> missingRecord = DatabaseLookupResult.resolve(
                    () -> null, value -> value != null);
                check(missingRecord.isNotFound(), "null record was not classified as not found");

                DatabaseLookupResult<Integer> missingPlayerId = DatabaseLookupResult.resolve(
                    () -> -1, value -> value != null && value >= 0);
                check(missingPlayerId.isNotFound(), "-1 player ID was not classified as not found");

                DatabaseLookupResult<Integer> foundPlayerId = DatabaseLookupResult.resolve(
                    () -> 42, value -> value != null && value >= 0);
                check(foundPlayerId.isFound(), "positive player ID was not classified as found");

                DatabaseLookupResult<String> failed = DatabaseLookupResult.resolve(
                    () -> {
                        throw new GameDatabaseException(
                            DatabaseLookupResultHarness.class, SENSITIVE_MARKER);
                    },
                    value -> value != null);
                check(failed.isFailure(), "database exception was not classified as failure");
                String diagnostics = failed.getFailureType() + " " + failed.getFailureOrigin();
                check(!diagnostics.contains(SENSITIVE_MARKER),
                    "raw database exception message leaked into diagnostics");
                check(!diagnostics.contains("Alice") && !diagnostics.contains("hunter2"),
                    "personal or credential fixture data leaked into diagnostics");

                boolean unexpectedPropagated = false;
                try {
                    DatabaseLookupResult.resolve(
                        () -> { throw new IllegalStateException("programming fault"); },
                        value -> value != null);
                } catch (IllegalStateException expected) {
                    unexpectedPropagated = true;
                }
                check(unexpectedPropagated,
                    "non-database failures must propagate to the existing server boundary");
            }
        }
        """
    )

    with tempfile.TemporaryDirectory(prefix="server-social-lookup-") as directory:
        temp = Path(directory)
        harness_path = temp / "DatabaseLookupResultHarness.java"
        harness_path.write_text(harness, encoding="utf-8")
        compile_result = subprocess.run(
            [
                javac,
                "-source",
                "8",
                "-target",
                "8",
                "-d",
                str(temp),
                str(DATABASE_EXCEPTION),
                str(LOOKUP_RESULT),
                str(harness_path),
            ],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        require(compile_result.returncode == 0, f"lookup fixture compile failed:\n{compile_result.stderr}")
        run_result = subprocess.run(
            [java, "-cp", str(temp), "com.openrsc.server.database.DatabaseLookupResultHarness"],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        require(run_result.returncode == 0, f"lookup fixture failed:\n{run_result.stderr}")


def main() -> None:
    run_lookup_fixture()

    friend_handler = FRIEND_HANDLER.read_text(encoding="utf-8")
    game_state_updater = GAME_STATE_UPDATER.read_text(encoding="utf-8")
    offline_lookup = method_body(
        game_state_updater,
        "private void processOfflinePrivateMessageLookup",
    )
    offline_log = logger_call(offline_lookup)
    friend_log = logger_call(method_body(friend_handler, "private void logLookupFailure"))

    require("catch (Exception" not in friend_handler, "FriendHandler still swallows broad exceptions")
    require(
        "Unable to add friend - unknown player." in friend_handler
        and "Unable to add name - unknown player." in friend_handler,
        "unknown friend/ignore results lost their bounded feedback",
    )
    require(
        "Unable to add friend right now. Please try again later." in friend_handler
        and "Unable to add name right now. Please try again later." in friend_handler,
        "friend/ignore database failures lack bounded retry feedback",
    )
    require(
        "addFriend(friendHash, 0, friendProperUsername.playerName" in friend_handler
        and "addIgnore(friendHash, DataConversions.usernameToHash(enemyProperUsername.formerName))"
        in friend_handler,
        "successful friend/ignore operations changed unexpectedly",
    )
    require(
        "Staff may not be added to ignore list" in friend_handler,
        "staff ignore protection changed unexpectedly",
    )

    require(
        "recipientLookup.isFound()" in offline_lookup
        and "recipientLookup.isNotFound()" in offline_lookup
        and "Unable to send message right now. Please try again later." in offline_lookup,
        "offline private-message lookup does not distinguish found, missing, and failed results",
    )
    require(
        "playerExists(" not in offline_lookup,
        "offline recipient lookup still performs a redundant existence query",
    )
    require(
        "is offline or has privacy mode enabled" in offline_lookup
        and "Unable to send message - player unavailable." in offline_lookup,
        "existing offline-recipient feedback changed unexpectedly",
    )
    require(
        "Unable to send message - unknown player." in offline_lookup,
        "unknown offline recipient lacks bounded feedback",
    )

    require(
        "getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES" in game_state_updater
        and "affectedPlayer.getSocial().isFriendsWith(player.getUsernameHash())" in game_state_updater
        and "!affectedPlayer.getSocial().isIgnoring(player.getUsernameHash()) || player.isMod()"
        in game_state_updater,
        "online private-message privacy policy changed unexpectedly",
    )
    require(
        "ActionSender.sendPrivateMessageSent(player" in game_state_updater
        and "ActionSender.sendPrivateMessageReceived(affectedPlayer" in game_state_updater,
        "online private-message delivery calls changed unexpectedly",
    )

    forbidden_log_fragments = (
        "getMessage()",
        "getUsername()",
        "getUsernameHash()",
        "hashToUsername",
        "friendName",
        "privateMessage",
    )
    for fragment in forbidden_log_fragments:
        require(fragment not in friend_log, f"friend failure log includes sensitive source: {fragment}")
        require(fragment not in offline_log, f"offline-message failure log includes sensitive source: {fragment}")

    print("PASS: social/offline-message failures are classified, bounded, and privacy-safe")


if __name__ == "__main__":
    main()
