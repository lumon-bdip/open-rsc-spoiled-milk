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

Preparation never replaces an existing workspace. It copies verified authored
world data into a new staging directory, installs a clean Builder database,
removes generated client identity/connection files, writes a loopback-only
configuration, verifies the copies, then publishes the workspace atomically.
The target private-server tree is read-only throughout preparation and use.

The launcher keeps logs under `<workspace>/logs`, active PID files and the
last-run receipt under `<workspace>/run`, and refuses a second process for the
same workspace. Closing the client requests an orderly local server shutdown.
Generated credentials are never printed or placed in manifests.

The manifest schemas in `schema/` are release contracts. Add a new schema
version instead of changing the meaning of an existing version.
