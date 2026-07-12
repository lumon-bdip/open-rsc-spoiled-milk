# In-Game World Editor Plan

Status: active; implementation-ready foundation assigned to AI-2

Owner: Spoiled Milk project owner

Initial branch: `feat/in-game-world-editor-foundation`

## Summary

Build an administrator-only in-game world editor that uses the desktop player
and world view as an editing cursor. The editor will provide a dedicated UI for
inspecting, copying, painting, placing, rotating, and removing terrain,
scenery, boundaries, and NPCs without requiring the editor to memorize command
syntax or encoded map values.

The existing developer commands and pending scenery/NPC edit files are useful
groundwork, but they are not the final editor architecture. Editing must be
owned by an explicit server-authorized session, represented as reversible
transactions, previewed consistently by the client and server, and saved into
reviewable project files from a private development server.

This plan is intentionally staged. The first implementation branch establishes
the permission, session, protocol, UI, and read-only inspection foundation. It
must not jump directly to terrain painting before the client and server agree
on raw tile values, coordinates, and session state.

## Goals

- Make routine world editing possible from a visual in-game interface.
- Let the world editor select exactly which terrain fields a stamp or brush
  changes; unchecked fields remain untouched.
- Use human-readable definition names, colors, previews, and orientation
  controls instead of requiring encoded numeric knowledge.
- Support single-tile stamps and true click-drag brush strokes with adjustable
  radius.
- Support scenery and NPC placement/removal through exclusive editor modes.
- Make every edit inspectable, undoable, recoverable, testable, and safe to
  hand off through the normal worker/manager release workflow.
- Keep the server's collision state, the client's rendered landscape, and the
  packaged server/client landscape data synchronized.

## Non-Goals

- Do not enable editing on the public live deployment by default.
- Do not permit an ordinary player, client-side developer toggle, or spoofed
  packet to obtain editing authority.
- Do not rewrite the complete landscape archive after every click or brush
  sample.
- Do not expose raw orientation offsets, collision masks, or archive quirks as
  values the editor must calculate manually.
- Do not support multiple simultaneous terrain editors in the first release.
- Do not include Android UI support in the first implementation. The existing
  Android experience remains unchanged unless a later plan explicitly adds a
  supported editor interface.
- Do not replace the standalone 2D landscape editor. The in-game editor is an
  additional visual workflow and an eventual source of compatible terrain
  patches.
- Do not add advanced fill, selection, prefab, scripting, or collaborative
  tools before the safe stamp-and-brush foundation is validated.

## Existing Foundation

The repository already includes partial editing support:

- `Development.java` can add, repeat, rotate, and remove ordinary scenery;
  add/remove NPCs; show pending counts; save pending scenery/NPC JSON; and
  clear pending queues.
- The desktop client has a developer menu with ground, object, and NPC actions
  that translate menu selections into development commands.
- Existing developer examines can expose object and NPC IDs and coordinates.
- `WorldSceneryEditFiles` and `WorldNpcEditFiles` provide deterministic custom
  location/removal files for ordinary scenery and NPC edits.
- The custom landscape archive stores a fixed raw tile record containing
  elevation, ground texture, overlay, roof texture, horizontal wall, vertical
  wall, and diagonal wall values.

These implementations should be refactored behind reusable editor services.
The UI must not build a second independent set of object/NPC mutation rules.

### Known gaps that shape the design

- Existing pending edit queues are static/global rather than owned by a player
  or editing session.
- Clearing the existing queues does not revert live entity mutations.
- Ordinary scenery is persisted, but boundary/wall-object editing does not yet
  have equivalent persistence.
- The server runtime `TileValue` does not retain all raw archive values. In
  particular, it is not a complete source for ground texture and roof texture.
- Landscape data is normally packaged separately in the client and server. It
  is not streamed as an authoritative complete map by the server.
- Terrain, wall, overlay, and elevation changes affect derived collision,
  projectiles, shared vertices, roofs, minimap data, and renderer caches.

## Terminology and Coordinate Rules

- **Raw tile**: the seven authored values stored in the landscape archive.
- **Derived tile state**: traversal, projectile, geometry, lighting, roof, and
  other runtime data calculated from raw values and definitions.
- **Editor session**: a server-authorized, bounded period during which one
  administrator may inspect or mutate a draft.
- **Draft**: the collection of unsaved terrain/entity transactions owned by an
  editor session.
- **Stamp**: one application of the current tool at one target tile.
- **Stroke**: one mouse-down through mouse-up brush operation, potentially
  affecting many tiles, recorded as one undo transaction.
- **Plane**: the world height/floor associated with the target tile. Every
  inspect and edit message must include it explicitly.
- **Base revision**: the landscape/edit-file revision from which the session
  began, used to prevent stale saves.

All editor displays should show world X, world Y, and plane. Conversion between
world coordinates, 48-by-48 archive sectors, local scene tiles, and client
rendering coordinates must live in tested helpers rather than duplicated UI
arithmetic.

## Permission and Environment Contract

`::worldeditormode` is the only normal entry point. It requests a session from
the server; it does not grant authority locally.

Entering the editor requires all of the following:

- a server-side administrator or dedicated world-builder capability;
- a server configuration flag such as `allow_in_game_world_editor=true`;
- a compatible custom desktop client and protocol version;
- no conflicting active editor session under the initial single-editor rule;
  and
- a server environment permitted to author project edits.

The configuration flag defaults to false. The public detached live deployment
must keep it disabled. A client-side `::dev` flag, local player property, or UI
visibility check is never sufficient authorization.

The server returns an opaque session ID and explicit capabilities. Every edit,
undo, redo, inspect, copy, save, discard, and exit request carries that session
ID and is validated server-side. Logout, disconnect, permission loss, server
shutdown, or explicit exit closes the authority. The draft must remain either
recoverable or explicitly discarded; it must never silently become published
world data.

For the first implementation, allow only one active mutating world-editor
session. Read-only inspection may remain part of that same session. Region
locks and multiple editors can be considered only after revision and conflict
handling are proven.

## Editor State and Exclusive Modes

The editor uses one exclusive mode enum, not independent top-level checkboxes:

1. `NAVIGATE`
2. `INSPECT`
3. `TERRAIN`
4. `SCENERY`
5. `NPC`
6. `BOUNDARY` after boundary persistence is implemented

Selecting one mode deactivates the others. Terrain field selection remains a
set of checkboxes inside `TERRAIN`; it does not compete with the top-level
mode.

`NAVIGATE` is necessary because a paint click cannot also be an ordinary walk
click. The initial behavior is:

- Navigate: ordinary supported movement and camera interaction.
- Inspect: clicking selects without changing the world.
- Terrain: click stamps; mouse drag paints a stroke.
- Scenery/NPC/Boundary: click places the selected definition; context actions
  inspect, copy, rotate where applicable, or remove.

Entering editor mode clears selected spells/items and blocks combat, trading,
ordinary object actions, and other gameplay interactions that could be confused
with editor input. Exiting restores the normal action menus without changing
the player's saved game settings.

## Desktop Editor Interface

Implement a dedicated movable desktop overlay rather than adding these tools
to the normal Options tabs. The first version can combine existing `NComponent`
layout support with existing numeric-input behavior; an inline numeric entry
control can follow when it is reliable.

Suggested compact layout:

```text
+ World Editor ------------------------------------------------+
| Mode: Navigate | Inspect | Terrain | Scenery | NPC           |
| Target: x=000 y=000 plane=0       Draft: 24 changes          |
|                                                              |
| Terrain fields                                               |
| [x] Elevation       [-] 12 [+] [Enter]                       |
| [x] Ground texture  [-] 37 [+] [Enter] Grass [preview]       |
| [ ] Overlay         [-]  0 [+] [Enter] None                  |
| [ ] Roof texture    [-]  0 [+] [Enter]                       |
| [ ] Horizontal wall [-]  0 [+] [Enter] None                  |
| [ ] Vertical wall   [-]  0 [+] [Enter] None                  |
| [x] Diagonal wall   [-]  1 [+] [Enter] Stone wall            |
|     Orientation: \  [Rotate]                                 |
|                                                              |
| Brush: Square   Radius: [-] 1 [+]                             |
| [Undo] [Redo] [Save] [Discard] [Exit]                        |
+--------------------------------------------------------------+
```

The actual component should fit supported desktop resolutions and may use
tabs, collapsible sections, or scrolling. It must not obstruct modal dialogs,
chat input, or the logout flow.

### Common UI behavior

- `-` and `+` change to the previous/next valid value, not merely the previous
  integer when definitions are sparse.
- `Enter` or the value field accepts a typed numeric value and validates it.
- Definition-backed values show ID and canonical name.
- Texture/overlay values show a color swatch or small preview when available.
- Invalid, absent, invisible, or collision-significant definitions receive a
  clear warning rather than silently painting.
- A checked field without a valid value blocks painting and explains why.
- Radius uses a clear footprint definition. Start with radius `0` meaning one
  tile and cap the first release at `5`.
- The current target and footprint are highlighted before application.
- UI state may persist locally for convenience, but authority and pending
  edits never do.

## Terrain Field Contract

The raw editable fields are:

- elevation;
- ground texture;
- ground overlay/tile decoration;
- roof texture;
- horizontal wall;
- vertical wall; and
- diagonal wall plus its normalized orientation.

Traversal masks, projectile flags, generated faces, normals, roof geometry,
and renderer chunks are derived/read-only. They can appear in the inspector for
diagnostics but cannot be painted directly.

Each terrain stamp includes a field mask and values. A raw field is changed
only when its mask bit is selected. An unchecked field remains byte-for-byte
unchanged.

Ranges must be validated against both the archive representation and loaded
definitions. Byte-backed fields are displayed as unsigned values where
appropriate. Do not permit silent wrapping from a typed integer to a byte.

### Diagonal wall orientation and rotation

The editor must never require the user to add or subtract `12000`.

The client and server represent the UI selection as two normalized properties:

- `baseDiagonalWallValue`: `0` for no wall or a valid base wall value; and
- `diagonalOrientation`: `A` or `B`.

The archive encoding remains an implementation detail:

- no diagonal wall: `0`;
- orientation A: base values `1..11999`;
- orientation B: `baseDiagonalWallValue + 12000`, producing
  `12001..23999`;
- raw `12000` and `24000` are boundary values, not valid selectable walls.

The UI provides a `Rotate` toggle/button beside the diagonal wall value and a
small `\` or `/` preview. Activating Rotate switches A to B or B to A without
changing the selected wall definition. The control is disabled when the
diagonal field is unchecked or the selected value is `0`.

Copy and inspection must decode an existing raw value into the base wall and
orientation. Painting must encode the normalized pair exactly once. Undo/redo
stores normalized values plus the exact before/after raw tile records so
repeated rotation cannot accumulate offsets.

The details inspector may show the raw encoded value in a diagnostic row, but
the primary editable field always shows the base wall value, definition name,
and visible orientation. Tests must explicitly cover `1 <-> 12001`, the
highest supported valid definition, `0`, invalid boundary values, both
collision masks, and copy/paint/undo round trips.

## Inspect, Examine, and Copy

While editor mode is active, normal world context menus are replaced or
filtered by the selected editor mode.

Terrain context actions include:

- `Inspect terrain`
- `Copy terrain`
- `Paint here` when Terrain mode is active
- relevant remove actions when a wall is present

`Inspect terrain` opens or updates a details panel with all raw fields, decoded
definition names, decoded diagonal orientation, coordinates, sector identity,
base revision, derived traversal/projectile state, and pending-draft state.

`Copy terrain` requests the authoritative raw tile from the server, populates
all raw terrain controls, decodes diagonal wall orientation, and checks every
terrain field. The server response, not an unverified client-local value, is
the final source for the copied state.

Object and NPC inspection includes:

- definition ID and name;
- world coordinates and plane;
- direction/orientation;
- object footprint or NPC roaming bounds;
- stable authored key and runtime instance ID where applicable;
- base-world versus custom-overlay source; and
- pending upsert/removal state.

Copying scenery/NPC data selects the matching exclusive mode and fills its
controls. It does not immediately place another entity.

## Scenery, Boundary, and NPC Tools

### Scenery

The scenery mode includes definition ID/name, direction `0..7`, footprint
preview, place, copy, rotate, and remove. Existing ordinary scenery mutation
and JSON persistence logic should be refactored into a reusable service and
called by both legacy commands and editor packets during migration.

Placement validation must cover occupied origins, multi-tile footprints,
collision impact, valid directions, plane, range, and session authority.

### Boundaries

Boundary objects are distinct from ordinary scenery and from diagonal wall
values embedded in raw terrain tiles. Do not present them as one interchangeable
system merely because each can render a wall.

Boundary mode should not become writable until it has deterministic upsert and
removal persistence equivalent to ordinary scenery. Read-only identification
may arrive earlier. Its controls include definition, direction/type, origin,
copy, rotate when meaningful, and remove.

### NPCs

NPC mode includes definition ID/name, direction where supported, start tile,
and roaming bounds/radius. Removal and editing must use a stable authored key;
the volatile runtime instance ID is diagnostic context, not the sole persisted
identity.

NPC placement/removal must reuse refactored `WorldNpcEditFiles` behavior and
support undo, redo, discard, and exact recovery of the previous live state.

## Brush and Stamp Input

Do not simulate repeated operating-system clicks or send one text command per
mouse frame.

The client implements a real editor stroke:

1. On world mouse-down, resolve the targeted world tile and plane.
2. Add it only when it differs from the last sampled tile.
3. Rasterize between samples so rapid movement cannot leave gaps.
4. Expand each sample by the selected radius and shape.
5. Deduplicate all tiles within the stroke.
6. Show the pending footprint locally.
7. Send bounded batches under one stroke ID and sequence.
8. End the transaction on mouse-up, cancellation, or loss of editor focus.

Start with a square footprint. A circle, line, rectangle, fill, and selection
tool are later enhancements.

The server enforces maximum radius, tile count, packet count, coordinate
bounds, plane, session ID, sequence ordering, capability, and rate limits. A
malicious client cannot bypass these limits by crafting packets.

The client may optimistically preview a small operation, but it must retain the
before state and roll back rejected tiles. Server acknowledgement is the
authoritative result.

## Session Draft, Undo, Redo, and Recovery

Introduce a `WorldEditSessionManager` or equivalent server service. It owns:

- the active editor identity and session ID;
- environment/capability checks;
- base revisions;
- terrain, scenery, boundary, and NPC draft changes;
- ordered stroke/stamp transactions;
- bounded undo and redo history;
- dirty regions/sectors;
- save/discard state; and
- recovery metadata.

Each stamp is one transaction. Each complete drag stroke is one transaction,
even when transported in several packets. Transactions store enough before and
after state to reconstruct both raw terrain and live entities exactly.

Any new edit after Undo clears the redo branch. Undo/redo limits must be based
on both transaction count and affected-tile/entity memory, not count alone.

Disconnect or server interruption must preserve a recoverable draft journal or
refuse to claim the work is safe. On reconnect, the editor can resume the
matching recoverable draft or explicitly discard it. Recovery files are local
development state until a deliberate save/checkpoint includes authored output.

`Discard` reverts the live preview and clears the draft. This is different from
the current `clearworldedits` behavior, which clears queues without reverting
live entities. Existing commands should eventually route to the same semantics
or clearly retain a separately named legacy action.

## Protocol

Text commands may remain compatibility entry points for simple legacy object
and NPC actions. Terrain inspection and brush editing require dedicated,
bounded protocol messages.

Required logical messages include:

- client to server: request open, close, inspect, copy, apply stamp/stroke
  batch, finish/cancel stroke, undo, redo, save, and discard;
- server to client: session opened/denied/closed, capabilities, tile/entity
  snapshot, draft summary, operation acknowledgement/rejection, authoritative
  patch, undo/redo result, save result, and forced close.

Every mutating message includes session ID, sequence, plane, operation/stroke
ID, base revision, and bounded payload. Terrain writes include a field mask and
normalized values; diagonal walls use base value plus orientation on the wire,
not an unchecked raw `+12000` integer.

Packets must not contain arbitrary filesystem paths. Unknown fields, invalid
enum values, excessive counts, invalid definitions, stale revisions, and
out-of-order stroke fragments are rejected without partial silent mutation.

## Authoritative Terrain Model and Runtime Rebuilds

The server needs a complete raw terrain source that retains all seven archive
fields. Runtime `TileValue` alone is insufficient. Build a terrain archive or
overlay service that can:

- load raw sectors deterministically;
- return an authoritative raw tile by world coordinate and plane;
- apply/revert masked patches;
- track dirty sectors and neighboring dependencies;
- derive server collision/projectile state through one reusable calculation;
  and
- serialize/materialize edits without losing untouched archive entries.

Extract reusable collision derivation from landscape loading rather than
copying its conditions into the editor. Wall changes can affect adjacent
tiles. Elevation and roof changes can cross tile and sector boundaries.

On an acknowledged terrain change, the desktop client updates its in-memory
sector data and invalidates the smallest safe affected region, including as
needed:

- terrain mesh/input;
- wall mesh/input;
- roof geometry and visibility;
- local collision/picking;
- minimap raster;
- lighting/normals; and
- renderer-v2 and legacy-renderer caches.

Both render paths must remain behaviorally supported. Rebuilding the entire
visible world per brush sample is not an acceptable final implementation; a
coarse rebuild may be used temporarily in a diagnostic prototype only when
clearly measured and replaced before brush rollout.

## Persistence and `::saveworldedits`

Use a versioned, deterministic terrain patch representation during editing,
for example `MyWorldTerrainEdits.json`. The exact filename and schema may be
adjusted during the architecture phase, but the following contract is fixed:

- keys include plane and world coordinate;
- records preserve only deliberate authored changes plus required base
  revision/schema information;
- serialization order and formatting are deterministic;
- unknown schema versions are rejected;
- duplicate/conflicting tile records are rejected;
- client and server can apply the same normalized result; and
- the patch can be materialized into complete landscape archives without
  changing unrelated sectors or ZIP entries.

`::worldedits` reports per-session and total counts for terrain tiles,
scenery, boundaries, NPCs, transactions, and dirty sectors.

`::saveworldedits` becomes the deliberate commit point for the active draft.
It must:

1. require the active authorized session;
2. verify the base archives/edit files have not changed;
3. validate all terrain definitions, entity definitions, coordinates,
   collisions, packet completion, and persisted removals/upserts;
4. write temporary outputs and validate them before replacement;
5. atomically write deterministic patch/entity files;
6. materialize both server and client `Custom_Landscape.orsc` outputs when the
   selected persistence design requires it;
7. verify matching expected client/server landscape hashes;
8. preserve a timestamped backup or restorable prior revision;
9. emit a summary of coordinates, sectors, entities, and hashes; and
10. retain or close the session according to an explicit successful-save
    policy.

A failed validation or filesystem operation leaves the previous saved files
intact and the draft recoverable.

The normal project workflow remains mandatory after save:

```text
worker branch -> private editor server -> save -> tests -> checkpoint/handoff
              -> manager review/merge -> release -> detached live deployment
```

Current players will not automatically receive newly packaged terrain merely
because the server archive changed. Until a future supported map-patch delivery
system exists, releasing materialized terrain requires a matching client
release and server deployment.

## Implementation Phases

### Phase 0: Architecture inventory and round-trip fixtures

- [ ] Document every raw tile field, range, definition lookup, diagonal
  orientation encoding, coordinate transform, plane mapping, and derived
  collision dependency.
- [ ] Add byte-exact sector/archive round-trip fixtures for representative
  terrain, walls, roofs, upper floors, and diagonal orientations.
- [ ] Prove how server and client archive copies are compared and
  materialized without unrelated drift.
- [ ] Inventory available custom protocol opcode ranges and UI integration
  points before assigning identifiers.
- [ ] Record the chosen terrain patch schema and recovery-journal location.
- [ ] Record any architectural decision that changes this plan before feature
  implementation proceeds.

### Phase 1: Authorized read-only editor foundation

This is the initial `feat/in-game-world-editor-foundation` branch scope.

- [ ] Add the disabled-by-default server configuration gate.
- [ ] Add `::worldeditormode` open/close handling with server-authoritative
  administrator/world-builder checks.
- [ ] Add a single active session state with opaque ID, sequence validation,
  logout/disconnect cleanup, and clear denial messages.
- [ ] Add the movable desktop interface shell and exclusive Navigate/Inspect/
  Terrain/Scenery/NPC mode controls; mutation controls remain disabled.
- [ ] Add authoritative terrain inspection messages and a complete raw tile
  snapshot model.
- [ ] Display raw fields, definition names, decoded diagonal orientation,
  coordinates, plane, sector, and derived collision diagnostics.
- [ ] Add read-only object/NPC inspection details and origin/source labels.
- [ ] Add `Copy terrain` UI population locally from the authoritative response,
  including normalized diagonal wall value and Rotate state; it must not paint.
- [ ] Ensure normal gameplay behavior is unchanged when editor mode is closed
  or unavailable.
- [ ] Do not enable Android editor UI.
- [ ] Handoff with changed files, protocol decisions, automated tests, manual
  private-server verification, and known risks.

### Phase 2: Session-scoped scenery and NPC mutations

- [ ] Refactor existing scenery/NPC command logic into reusable validated
  services.
- [ ] Replace global pending state with session-owned draft transactions.
- [ ] Implement scenery/NPC UI search/value controls, placement, copy, rotate,
  removal, undo, redo, discard, and pending summaries.
- [ ] Preserve compatible legacy commands by routing them through the shared
  services where practical.
- [ ] Add stable NPC/scenery authored keys and exact live-state reversal.
- [ ] Add boundary read-only inspection and design/persist its edit-file
  contract before enabling mutations.

### Phase 3: Single-tile terrain stamps

- [ ] Implement the versioned terrain patch/draft store.
- [ ] Add masked single-tile stamps with strict validation.
- [ ] Implement diagonal wall base-value/orientation encoding, Rotate, copy,
  inspect, apply, undo, redo, discard, and round-trip tests.
- [ ] Recompute server collision/projectile state for the complete dependency
  neighborhood.
- [ ] Apply acknowledged patches to client in-memory sectors and invalidate
  terrain/wall/roof/minimap/render caches safely.
- [ ] Validate both legacy and renderer-v2 paths on a private server/client.
- [ ] Add target/footprint preview and rejected-edit rollback.

### Phase 4: Brush strokes and radius

- [ ] Implement real mouse drag sampling, line gap filling, square radius,
  deduplication, bounded batching, and operation sequencing.
- [ ] Treat each complete stroke as one undoable transaction.
- [ ] Enforce client and server radius/count/rate limits.
- [ ] Handle sector edges, visible-scene edges, upper floors, focus loss,
  cancellation, rejection, and partial transport without partial silent save.
- [ ] Measure renderer rebuild and server collision costs before raising radius
  limits.

### Phase 5: Atomic save and landscape materialization

- [ ] Integrate terrain, scenery, boundary when available, and NPC drafts into
  `::worldedits`, `::saveworldedits`, and Discard.
- [ ] Add stale-base detection, deterministic ordering, temporary output,
  validation, atomic replacement, backup, and recovery.
- [ ] Materialize matching server/client archives or prove the chosen shared
  overlay packaging path.
- [ ] Hash-check both landscape outputs and confirm unrelated archive entries
  remain unchanged.
- [ ] Add release-check coverage for terrain patch schema, archives, and
  server/client parity.
- [ ] Document the private editor server through manager release workflow.

### Phase 6: Boundary editing and advanced tools

- [ ] Implement deterministic boundary upserts/removals and then enable its
  exclusive UI mode.
- [ ] Add circle/line/rectangle/fill/selection tools only after stamp and brush
  safety is proven.
- [ ] Add definition search, favorites, richer thumbnails, prefab support, and
  optional keyboard shortcuts.
- [ ] Consider region locks and multiple editors only with conflict-aware
  revisions and recoverable drafts.
- [ ] Consider a supported signed map-patch distribution system separately
  from the first editor release.

## Test Plan

### Automated coverage

- [ ] Permission/configuration matrix denies ordinary players, disabled
  environments, incompatible clients, stale sessions, and spoofed packets.
- [ ] Session sequence, reconnect, disconnect, forced close, conflicting open,
  and recovery behavior are deterministic.
- [ ] Raw sector and complete archive unpack/pack are byte-exact when unchanged.
- [ ] Every field mask changes only selected values.
- [ ] Typed values reject overflow, negative invalid values, missing
  definitions, and reserved encodings.
- [ ] Diagonal orientation tests cover no wall, both directions, rotate twice,
  copy, stamp, undo, redo, save, reload, collision flags, and archive output.
- [ ] Wall/overlay/elevation changes recalculate required neighboring derived
  state.
- [ ] Scenery/NPC mutations preserve existing placement/removal behavior and
  revert exactly on Undo/Discard.
- [ ] Stroke rasterization has no gaps, duplicates, out-of-bounds tiles, or
  unbounded batches.
- [ ] Stale-base saves and injected write failures leave previous saved data
  intact and the draft recoverable.
- [ ] Deterministic materialization gives matching server/client hashes and
  does not alter unrelated sectors/entries.
- [ ] Editor-disabled builds preserve normal menus, movement, context actions,
  and protocol behavior.

### Private manual validation

- [ ] Enter/exit repeatedly with authorized and unauthorized accounts.
- [ ] Navigate and inspect terrain on ground and upper floors.
- [ ] Copy representative textures, overlays, elevation, roofs, straight
  walls, both diagonal orientations, and empty fields.
- [ ] Rotate the same diagonal wall between `\` and `/` without typing or
  seeing `+12000` as the normal workflow.
- [ ] Stamp one field while confirming every unchecked field is unchanged.
- [ ] Brush slowly and quickly across tiles, sector boundaries, and radius
  edges; verify no gaps and one-step Undo.
- [ ] Confirm server walking/projectile collision matches visible walls and
  overlays after apply, undo, reload, save, and restart.
- [ ] Place/copy/rotate/remove scenery and place/copy/remove NPCs, then Undo,
  Redo, Discard, Save, and restart.
- [ ] Compare legacy and renderer-v2 visuals, roof handling, minimap, picking,
  and performance after edits.
- [ ] Build a private client and server from the saved worker branch and verify
  the same terrain appears before any manager merge or live rollout.

## Safety and Performance Requirements

- The server is authoritative for permission, session state, definitions,
  limits, base revision, edit acceptance, and save.
- No editor packet accepts an arbitrary filesystem path or unbounded list.
- All persisted writes use validation and atomic replacement.
- No normal workflow writes authored map changes from the detached live
  deployment.
- No routine workflow depends on stash, destructive reset, forced worktree
  removal, or ignored local files as the only copy of edits.
- Expensive geometry/collision rebuilds are bounded and measured.
- Brush inputs coalesce rather than flooding commands or rebuilding per mouse
  frame.
- Logs identify session, editor, operation, affected count/region, result, and
  save hash without logging credentials or excessive per-tile spam.
- A failure defaults to rejecting or reverting the draft operation, not
  leaving a silent client/server disagreement.

## Decisions

- The editor is an administrator/world-builder tool with a separate
  disabled-by-default environment gate.
- The first supported editor UI is desktop-only; Android remains unchanged.
- The first mutating release permits one active editor at a time.
- Navigate and Inspect are explicit modes so painting never also means walking.
- Terrain field checkboxes control a field mask; unchecked fields are never
  changed.
- Terrain, scenery, NPC, and boundary are exclusive top-level modes.
- Click is a stamp and click-drag is a real brush stroke; no synthetic rapid
  click loop is used.
- Diagonal walls expose a base definition plus Rotate/orientation control. The
  `+12000` archive encoding is never required user input.
- Undo/redo and true live-state Discard are prerequisites for broad brush use.
- Terrain edits use a versioned draft/patch representation and are
  materialized atomically; the full ZIP is not rewritten per click.
- Private client/server visual validation is required before manager merge and
  live release.

## Open Questions for Later Phases

These do not block the Phase 0/1 foundation:

- Whether successful Save keeps the editor session open with a new base
  revision or closes it by default.
- Whether the durable authored source remains a terrain overlay in releases or
  is always materialized into both complete archives.
- The exact recovery journal retention and cleanup policy.
- Whether boundary editing belongs in the first mutating entity phase or a
  separate branch after terrain stamps.
- Which texture thumbnail source and cache provide the clearest low-cost UI.
- Whether Paint mode should also support an explicit `paint under player`
  hotkey after cursor painting is stable.

## Completion Criteria

This plan is complete only when:

- an authorized administrator can enter a private in-game editor and ordinary
  users/public deployments cannot;
- terrain, scenery, boundary, and NPC modes are mutually exclusive and usable
  without memorized command syntax;
- all raw terrain fields can be inspected, copied, selectively stamped, and
  brushed with adjustable radius;
- diagonal walls can be selected and rotated visually in both directions
  without manual `12000` arithmetic;
- object/NPC/boundary edits use deterministic persistent identities and files;
- every mutation supports safe Undo, Redo, Discard, recovery, and atomic Save;
- client rendering/picking/minimap and server collision/projectiles agree after
  edits, reloads, restarts, and materialization;
- client and server landscape outputs match and unrelated archive data remains
  unchanged;
- relevant automated tests, private manual client/server tests, and release
  checks pass; and
- the completed worker work is reviewed, merged, published, packaged, and
  deployed through the manager workflow rather than edited directly on live.
