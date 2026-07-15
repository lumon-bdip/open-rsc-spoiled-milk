# B05 Server Failure-Handling Scope

This branch addresses the confirmed social/offline-message failures from the
2026-07-12 code-health audit. It does not change database schemas, privacy
rules, friend/ignore semantics, private-message contents, or server authority.

## Database Result Contracts

The maintained MySQL implementation, also inherited by the SQLite
implementation, has distinct ordinary-miss results:

- `GameDatabase.getProperUsernameCapitalization` returns `null` when a
  canonical friend/ignore name does not exist.
- `GameDatabase.playerIdFromUsername` returns `-1` when an offline
  private-message recipient does not exist.
- Both operations throw `GameDatabaseException` for database/infrastructure
  failures.

`DatabaseLookupResult` preserves those distinctions and deliberately retains
no raw exception message. The new logs contain only a constant operation name,
database implementation type, exception type, and database-layer class/method
origin. They do not contain the requesting player, target name/hash, private
message, credentials, IP addresses, or JDBC reason text.

## Behavior Boundaries

| Operation | Not found | Database failure | Existing success path |
| --- | --- | --- | --- |
| Add friend | Existing unknown-player response and list refresh | Retry-later response, current list refresh, redacted error | Canonical/former names and friend updates unchanged |
| Add ignore | Existing unknown-player response and list refresh | Retry-later response, current list refresh, redacted error | Staff protection and ignore update unchanged |
| Offline private message | Explicit unknown-player response | Retry-later response and redacted error | Existing legacy offline/privacy and modern unavailable responses unchanged |
| Online private message | Not applicable | Not changed in this branch | Friendship, ignore, moderator, and privacy-mode checks unchanged |
| Plugin class-loader cleanup | No loaded class loader is an intentional no-op | Warning is logged and cleanup continues | Loaded classes are cleared and the loader is released idempotently |

The offline recipient path now relies on the successful player-ID lookup
instead of immediately querying `playerExists(id)` a second time. A returned ID
already came from the players table; removing the redundant query avoids a
race and does not change successful feedback or delivery authority.

## Verification

- `tests/myworld/test-server-social-failure-handling.py` executes found,
  `null`, `-1`, and injected `GameDatabaseException` outcomes. The injected
  exception contains private-message, credential, and username markers and
  asserts that none reach the diagnostic metadata. It also guards the existing
  online privacy and delivery conditions.
- `tests/myworld/test-server-best-effort-cleanup.py` executes cleanup with no
  loader, repeated cleanup, and an injected class-loader close failure.
- `tests/myworld/test-social-name-lookup.py` continues to guard canonical name
  lookup.
- `scripts/build-server.sh` verifies the authoritative Ant core/plugin build
  and artifact inventory.
- `scripts/lint.sh all --offline --base spoiled-milk/main` verifies changed
  Java, Python, and shell lines with the repository's compiler and analyzer
  gates.

## Intentionally Deferred Findings

These paths were inspected or found while tracing this work and remain outside
the focused social/offline-message branch:

- `GameDatabase.banPlayer` converts every `GameDatabaseException` into “There
  is not an account by that username.” Correcting that conflation affects
  moderator command/audit semantics and needs its own tests.
- `JContent.dump` and `JContentFile.dump` catch and discard every archive-write
  exception. No active direct caller was found in the server source scan, so
  ownership and any dynamic/debug use should be proved before changing or
  pruning them.
- `PluginJarLoader.loadJar` and `loadClasses` retain pre-existing PMD
  `CloseResource` findings around `JarFile` ownership. Discovery through
  `JarURLConnection` and reload behavior need a separate loader-lifecycle
  change; this branch changes only the audit-confirmed `clear` path.
- `GameDatabaseException` currently embeds raw JDBC reason text in its message,
  and established call sites elsewhere may log that exception directly. This
  branch does not alter the repository-wide exception contract; its new social
  logs intentionally do not consume that raw message.
- Broad catches in moderator-disconnect helpers (`Player`, `SleepHandler`, and
  captcha paths) were not changed. Their comments indicate compatibility with
  moderators logging out mid-operation, and narrowing them requires focused
  lifecycle coverage rather than a mechanical catch rewrite.
