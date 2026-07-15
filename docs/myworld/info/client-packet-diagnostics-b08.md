# B08 Client Packet Diagnostic-State Extraction

This branch extracts movement diagnostics and scene-baseline state from
`PacketHandler` without changing packet decoding, packet order, or client
game-state mutations.

## Ownership Boundaries

`MovementSnapshotDiagnostics` now owns:

- movement-update and movement-snapshot fingerprints;
- update/snapshot wire-parity comparison state;
- visible local-player, player, and NPC cache-parity accounting;
- snapshot sequence, record-count, and staged-cache diagnostic history; and
- movement diagnostic summary lines and bounded mismatch logging.

`SceneBaselineState` now owns:

- received-page indexes, counts, duplicate detection, and page-record storage;
- complete scenery and wall baseline snapshots;
- baseline reset, completion, staleness, and apply accounting;
- scene parity and recent diagnostic history; and
- the legacy-scene pruning state and range-bound pruning operation that depend
  on the stored baseline identity.

`PacketHandler` remains the protocol boundary. It still dispatches opcodes,
reads every field from `packetsIncoming`, constructs scenery and wall records,
applies movement updates, materializes baseline game objects and walls, and
preserves the original diagnostic calls after those mutations. The existing
`MovementSnapshotStage` continues to own staged snapshot replacement.

This first extraction reduces `PacketHandler` from 4,593 to 3,507 lines. It is
not permission to split packet families on this branch; subsequent family
extractions require their own characterization and runtime verification.

## Preserved Behavior and Deliberate Limits

- Custom movement updates are still applied immediately in wire-read order.
- Snapshot parity is still measured against the already-mutated visible
  character caches, then compared with the preceding movement-update
  fingerprint and staged snapshot result.
- Scene-baseline packets still retain their original page and record decode
  order. Baseline application still occurs only after all required scenery and
  wall pages are stored and the baseline origin is loaded.
- Stored scene records are exposed only through defensive snapshot lists, as
  before.
- Scene pruning, parity refresh, stale thresholds, summary wording, and bounded
  recent-log behavior are copied unchanged into the state owner.
- No opcode, bit width, packet length, protocol version, movement rule, world
  editor rule, renderer behavior, server behavior, or scene-sync configuration
  default changes.
- Scene-baseline sync remains disabled by default. This extraction does not
  broaden or retire that compatibility path.

## Automated Verification

`tests/myworld/test-client-packet-diagnostic-extraction.py` guards both
ownership and behavior. Its source checks preserve movement-update,
movement-snapshot, and scene-baseline decode/apply ordering. Its compiled Java
fixtures cover:

- matching and mismatching update/snapshot fingerprints;
- local-player, visible-player, and visible-NPC parity summaries;
- staged snapshot diagnostics and bounded mismatch history;
- multi-page scenery/wall completion and duplicate-page accounting;
- defensive baseline snapshots and stable application keys;
- scene parity, legacy out-of-range pruning, and apply accounting.

The existing packet-shape and client/server modernization guards now inspect
the extracted owners for the state they authoritatively own while continuing
to inspect `PacketHandler` for all protocol reads and mutations.

Verification on the extraction checkpoint:

- `tests/myworld/test-client-packet-diagnostic-extraction.py`: passed.
- Packet-shape, server-sync modernization, movement stability, movement timing,
  and movement-stutter diagnostic tests: passed.
- Deferred terrain, region-load performance, relog resident-world, and scene
  entity-lifecycle tests: passed.
- Client/server landscape parity and world-editor foundation, compact-toolbar,
  region-collision, and import tests: passed.
- Player release packaging test: passed.
- `./scripts/build-client.sh`: passed.
- `./scripts/lint.sh compiler --offline --base spoiled-milk/main`: passed with
  no new gated compiler warnings.

## Required Private Runtime Verification

The branch must not be handed off until the owner confirms the following on a
loopback-only private development server and the branch-built client:

1. Log in, walk and run through an area containing moving NPCs and, where
   practical, another player; movement remains smooth and entity positions do
   not jump or disappear.
2. Teleport between distant areas and cross a normal region boundary; terrain,
   scenery, walls, ground items, NPCs, and the local player load normally.
3. Relog or reconnect after movement and a region change; the resident scene is
   rebuilt without stale or duplicated entities.
4. Exercise the world-editor entry/display path without saving map changes;
   editor overlays and the loaded region remain intact.
5. Inspect the movement diagnostic summary where practical and confirm no new
   parity/staging mismatch is reported during the representative actions.
6. Close the client normally and inspect private client/server logs for packet,
   scene, movement, or cleanup exceptions.

Private runtime status: **pending owner confirmation**.

The public server and detached live checkout are outside this branch and must
remain untouched.
