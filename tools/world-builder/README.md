# World Builder Tools

This module owns standalone World Builder project discovery, manifests,
workspace management, export, import, rollback, and launch supervision as
those phases are implemented.

The current Phase 0 command is deliberately read-only:

```bash
./scripts/build-world-builder-tools.sh
java -jar output/world-builder-tools/world-builder-tools.jar discover \
  --server-root /path/to/private-server
```

Discovery supports the versioned `spoiled-milk-repository-v1` layout and
writes its deterministic source manifest to standard output. It does not
create a workspace or change the target.

The manifest schemas in `schema/` are release contracts. Add a new schema
version instead of changing the meaning of an existing version.
