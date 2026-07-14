# World Builder Tools

This module owns standalone World Builder project discovery, manifests,
workspace management, export, import, rollback, and launch supervision as
those phases are implemented.

Read-only target discovery remains available independently:

```bash
./scripts/build-world-builder-tools.sh
java -jar output/world-builder-tools/world-builder-tools.jar discover \
  --server-root /path/to/private-server
```

Discovery supports the versioned `spoiled-milk-repository-v1` layout and
writes its deterministic source manifest to standard output. It does not
create a workspace or change the target.

The Phase 1 runtime can prepare an isolated workspace and launch the local
Builder server/client pair:

```bash
./scripts/build-server.sh
./scripts/build-client.sh
./scripts/build-world-builder-tools.sh

java -jar output/world-builder-tools/world-builder-tools.jar launch \
  --server-root /path/to/private-server \
  --runtime-root /path/to/world-builder-release \
  --workspace /path/to/world-builder-project \
  --port 43615
```

`prepare` accepts the same arguments but stops after staging. `run` starts an
existing prepared workspace with `--workspace` and `--port`.

Preparation never replaces an existing workspace. It records the target's
verified authored files under immutable-by-contract `<workspace>/source` and
creates the complete runnable copy under `<workspace>/working`. The working
tree receives a clean Builder database, no generated client identity or
connection files, and a loopback-only configuration before the project is
published atomically. Launch re-verifies every source-snapshot hash and refuses
added, changed, missing, or symlinked source files. The target private-server
tree is read-only throughout preparation and use.

The Builder server receives the canonical workspace root explicitly and
refuses to start from any directory other than `<workspace>/working/server`.
Terrain, scenery, NPC overlays, the client terrain mirror, and terrain backups
all resolve through that validated context. The editor shows the project folder
name, source revision, and current saved/unsaved state.

The launcher keeps logs under `<workspace>/logs`, active PID files and the
last-run receipt under `<workspace>/run`, and refuses a second process for the
same workspace. Closing the client requests an orderly local server shutdown.
Generated credentials are never printed or placed in manifests.

The manifest schemas in `schema/` are release contracts. Add a new schema
version instead of changing the meaning of an existing version.
