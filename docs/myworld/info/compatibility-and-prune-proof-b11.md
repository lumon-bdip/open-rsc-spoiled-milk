# B11 Compatibility Labels And Prune Proof

This cleanup corrects misleading labels and removes only artifacts whose active
references were disproved. It does not change game rules, protocol behavior,
database support, plugin discovery, or the repository's archive policy.

## Active Client And Compatibility Boundaries

The desktop client is one product built from two active source roots:

- `Client_Base/src` owns shared game state, packet handling, interfaces, the
  software renderer, and client definitions.
- `PC_Client/src` owns the desktop/LWJGL platform, window lifecycle, and OpenGL
  presentation.

`Client_Base/build.xml` compiles both roots into `Open_RSC_Client.jar` and
packages jars from `PC_Client/lib`. Neither directory is a prune candidate.
OpenGL remains optional, so `ScaledWindow` and the persisted `scaling_type`,
`ui_scale`, and `scaling_scalar` bridge remain active. Hidden
`RenderSurfaceSettings.Mode` values remain accepted migration aliases for old
saved settings even though the current selector exposes only 4:3 and 16:9.

The server compatibility flags `old_pray_xp`, `old_quest_mechanics`, and
`old_skill_defs` remain active in plugins or definition loading. Their public
Java fields and configuration keys are retained. The inverted
`BREAK_NPC_LOCATION_CACHE` label now has the semantic internal replacement
`DISABLE_NPC_LOCATION_CACHE`; the original public field and the
`break_npc_location_cache` key remain as compatibility aliases. The default is
still `false`, so local-NPC caching remains enabled unless explicitly disabled.

Protocol parser/generator variants, both database backends, and plugin classes
also remain active boundaries. Protocol selection references each supported
version directly. `PluginJarLoader` enumerates `plugins.jar`, and
`PluginHandler` registers classes and trigger methods by reflection. An absent
ordinary Java call site is not dead-code evidence for those classes.

The desktop `ClientPort.playSound(byte[], ...)` method is retained. B02 already
documents it at the interface and desktop implementations as a non-throwing
platform compatibility hook, while current desktop playback uses
`soundPlayer`. Its removal requires a separate mobile/shared-source policy.

## Corrected Labels And Intentional No-Ops

- `KnownPlayersHandler` already copies server indexes and appearance IDs into
  the player's known-player cache; the stale “needs implementation” comment at
  its active opcode binding was removed.
- `World.getWorldMapX/Z` are ordinary accessors, not generated stubs.
- `Bank.setTab` remains a compatibility no-op because server bank storage has
  no tab state; the active client owns tab presentation.
- `Duel` and `Trade` retain their `ContainerListener` methods as compatibility
  no-ops. Their packet handlers explicitly validate offer capacity and send
  offer updates. Removing the implemented interface would widen this cleanup
  into a public/plugin compatibility change.
- The bank-PIN “I don't know it” control remains intentionally passive because
  the current client/server protocol defines no recovery request. Adding a
  recovery workflow is a separate user-visible feature.

## Removed With Complete Reference Proof

### Archive dump methods

`JContent` and `JContentFile` are active readers used by `WorldLoader`, but
their `dump(String)` writer methods had no callers. Searches covered maintained
Java, Python, shell, tests, documentation, tracked path strings, reflection
sites, and the Ant-built server artifacts. The active reflection sites invoke
payload processors, plugins, persistence aliases, or tests by other explicit
names; none can select these dump methods. Only the two methods and their
unused write/logging imports were removed, leaving archive open, unpack, read,
and close behavior intact.

The four exact SpotBugs baseline entries owned by those swallowed-exception
methods (`DE_MIGHT_IGNORE` and `REC_CATCH_EXCEPTION` for each class) were
removed. No unrelated baseline entry changed.

### Active-root IDE metadata

Tracked IntelliJ files under `Client_Base/.idea`, the two client `.iml` files,
and `server/Game Server.iml` were not referenced by build, run, release,
workspace, or contributor documentation. They were mutually inconsistent
(Java 8 versus Java 11), incomplete relative to the authoritative Ant
classpaths, and contained unresolved project-library names. The authoritative
scripts derive every source root and dependency without them, so the files were
removed and `*.iml` is now ignored alongside `.idea/`.

For a clean IntelliJ setup, open the repository root as a new project, select a
Java 8 project SDK/bytecode target, and add `Client_Base/build.xml` and
`server/build.xml` as Ant build files. If the IDE does not infer roots from Ant,
mark `Client_Base/src`, `PC_Client/src`, `server/src`, and `server/plugins` as
source roots. Use `scripts/build-client.sh` and `scripts/build-server.sh` as the
build source of truth; generated IDE files stay local.

## Retained Because Proof Is Incomplete

`server/database/mysql/depreciated` contains five historical manual SQL helpers:

- `add_custom_items.sql`;
- `add_custom_npcs.sql`;
- `add_custom_objects.sql`;
- `remove_custom_npcs.sql`;
- `remove_custom_objects.sql`.

No maintained script or documentation calls these files. Some related custom
content appears in `mysql/upgrades/convert_custom_4.3.0.sql`, but repository
history does not establish whether every operator applied that upgrade, still
uses the helpers for rollback/reference, or expects their unprefixed table
names. MySQL credentials and an isolated operator environment are not
available in this workspace. The misspelled directory is therefore retained
unchanged pending a MySQL operator/migration inventory and an explicit archive
or deletion decision.

The whole `legacy/` tree also remains unchanged. `legacy/README.md` identifies
it as a non-runtime reference archive retained by policy. Protocol variants,
dynamically discovered plugins, both database implementations, generated
content, and string-addressed assets were not considered removable merely
because an IDE or direct-call scan reported them unused.

## Verification Contract

`tests/myworld/test-compatibility-labels-and-prune-proof.py` guards the active
roots, compatibility keys/aliases and consumers, label corrections, dump-method
removal, exact baseline reduction, IDE ignore/removal, retained MySQL helpers,
archive policy, protocol variants, and plugin discovery boundary. The B11
handoff also records authoritative client/server builds, world-load and World
Builder/editor suites, plugin and protocol tests, documentation-link checking,
and changed-file static analysis.
