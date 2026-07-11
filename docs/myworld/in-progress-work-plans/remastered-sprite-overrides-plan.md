# Remastered Sprite Override Plan

Status: active foundation plan for AI-3 implementation and iterative visual
testing.

## Goal

Add an optional client-side sprite path that can display curated remastered PNG
art alongside the existing sprite system. The original archives and current
runtime sprites remain canonical. When the player enables remastered sprites,
the client uses a valid remastered replacement when one exists and falls back
to the exact original sprite for every missing or invalid replacement. When the
option is disabled, behavior must remain identical to the original path.

This plan deliberately permits partial coverage. A remaster pack does not need
to replace every item, character, object, interface image, or animation before
it is useful.

## Boundaries

- Do not replace, rewrite, or delete `Custom_Sprites.osar` or the authentic
  sprite archives.
- Do not reuse `Config.S_WANT_CUSTOM_SPRITES` as the player option. That flag
  selects an existing server/client sprite source and is a separate concern.
- Do not fold the combat VFX work in
  [`animation-asset-migration-plan.md`](animation-asset-migration-plan.md) into
  this project. Both systems may use PNGs, but their identities and rendering
  contracts differ.
- Do not require the server or network protocol to know which art style a
  player selected.
- Do not decode PNGs, read files, or emit repeated missing-asset messages from
  the render loop.
- Do not change gameplay bounds, click targets, animation timing, or sprite
  placement merely because replacement art has a different source resolution.
- Begin with packaged, curated assets. Loading arbitrary external mod folders
  can be considered later as a separate security and support decision.

## Terminology

- **Canonical sprite**: the existing sprite produced by the current archive,
  custom-sprite, or generated runtime path.
- **Remastered override**: an optional PNG plus the metadata needed to render
  it in place of one canonical sprite or frame.
- **Resolved sprite**: the canonical sprite or valid remastered override chosen
  for the current player setting.

Documentation and UI should use `remastered` or `override`, not the already
overloaded term `custom sprites`.

## Asset and Catalog Layout

Use the following authored location unless the implementation inventory finds
a compelling packaging constraint:

```text
dev/myworld/assets/sprites/remastered/
  manifest.json
  items/
  interface/
  entities/
  world/
```

The client build already packages PNG files beneath
`dev/myworld/assets/sprites/`. If the catalog is stored as JSON, the build must
also package `manifest.json`, and an automated test must prove both the catalog
and declared PNGs are present in the player artifact. A generated Java catalog
is an acceptable first foundation only if it keeps the same explicit schema
and a later author can add art without editing renderer code.

PNG pixels are not enough to reconstruct the current `Sprite` contract. Each
catalog entry must identify the replacement and preserve the metadata relevant
to its canonical sprite, including:

- stable sprite key and PNG path;
- optional sheet frame, row, and column information;
- source and logical width and height;
- `requiresShift`, X/Y shift, and bound width/height;
- category and any scale or anchor policy;
- palette-mask/recolor policy where equipment or character art needs it.

Catalog paths must be normalized beneath the packaged remaster root. Reject
absolute paths and traversal rather than allowing the manifest to read an
arbitrary local file.

## Stable Sprite Identity

The resolver needs a stable key before a large art library is authored. Phase
0 must inventory the actual load and draw paths and document the selected key
scheme. At minimum, distinguish:

- archive/array sprites by source subspace and canonical identifier;
- item icons by their real item sprite identity, respecting the
  `mudclient.spriteItem + spriteID` authentic offset described in
  [`client-sprite-reference.md`](../info/client-sprite-reference.md);
- sprite-tree/archive animation frames by subspace, entry, type/layer, and
  frame index, or an equally stable named identity;
- generated or runtime-only sprites that cannot safely be overridden yet.

Do not treat an incidental render-array index as a universal identity unless
the inventory proves that index is stable and namespaced. Different sources
may legitimately use the same numeric ID.

## Runtime Architecture

1. Load and retain the canonical sprite through the existing path.
2. Parse and validate the remaster catalog once through a small component such
   as `RemasteredSpriteCatalog`.
3. Resolve a stable key through a single component such as
   `RemasteredSpriteResolver`, passing the canonical sprite as the guaranteed
   fallback.
4. When the setting is on and a valid entry exists, return the cached
   remastered sprite. Otherwise return the original object.
5. Make software and OpenGL rendering consume the same resolved sprite
   decision so the two renderers cannot drift.

The implementation must not destructively overwrite the only copy of the
canonical sprite. A player must be able to return to original art without
restarting or re-reading all original archives. Cache keys must account for
the selected art mode and catalog revision. Toggling the option must safely
invalidate or rebind OpenGL textures and any software-side derived caches.

Load or decode remastered PNGs at startup, during a controlled asset reload,
or lazily once per catalog entry. Never perform filesystem I/O or image decode
once per frame. Use a bounded cache if loading the complete catalog would be
materially expensive.

## Player Setting

Add a persisted client-local option labelled clearly, such as
`Remastered sprites: On/Off`, under the most discoverable graphics or
interface settings group. Default it to **Off** while coverage is experimental.
It is independent of the server and other players.

The target behavior is an immediate live toggle. If the first proof requires a
controlled renderer/asset reload, make that explicit to the player and keep it
safe; do not silently leave half of the caches in the previous mode.

## Fallback and Rendering Rules

- Resolve per sprite and per frame. A partially remastered animation may use
  its original frame wherever the corresponding PNG is absent.
- A missing file, malformed PNG, invalid frame declaration, or incompatible
  metadata logs one useful diagnostic and falls back to the canonical sprite.
  It must not crash, show a checkerboard, or hide the sprite.
- Disabled mode should avoid probing or decoding remastered assets except for
  any minimal catalog work required by the settings UI.
- Treat PNGs as full ARGB images. Preserve transparent pixels; do not use black
  as an implicit transparency key.
- Keep logical dimensions, shifts, anchors, and bounds separate from source
  pixel resolution. Define an explicit scale policy before accepting higher
  resolution art.
- Equipment and character recoloring must have an explicit entry policy:
  either the remastered pixels are mask-compatible and receive the normal mask
  once, or the entry opts out because it is final-color art. Never double-tint
  a replacement.

## Diagnostics

Expose compact, AI-readable counters through the existing F6 diagnostic
capture rather than regular per-frame logs:

- catalog entries and successfully loaded replacements;
- remastered resolutions and canonical fallbacks;
- missing, invalid, or metadata-incompatible entries;
- cache hits, misses, memory estimate, and reload count;
- active art-mode setting and renderer backend.

Each invalid catalog entry should also produce one actionable startup/reload
message containing its stable key and reason.

## Implementation Phases

### Phase 0: Inventory and key contract

- Trace the canonical archive, sprite-array, sprite-tree, item, equipment,
  software-renderer, and OpenGL texture paths.
- Record stable key formats and unsupported categories in this plan.
- Confirm where settings persistence and runtime cache invalidation belong.
- Select the smallest useful proof sprites with the owner before producing a
  large art set.

### Phase 1: Safe foundation

- Add the setting, catalog schema/loader, resolver, validation, and diagnostics
  with zero required overrides.
- Prove that disabled mode and enabled mode with an empty catalog both render
  canonical art.
- Add packaging and fallback tests before expanding coverage.

### Phase 2: Visually testable proof

- Add a deliberately small cross-section: one static interface/world sprite,
  one item icon, and one short animated or equipped frame set when practical.
- Test software and OpenGL paths, toggling on and off in the same client
  session.
- Use a private client/server for owner visual approval. Do not deploy this
  work directly to the public live server.

### Phase 3: Character and animation coverage

- Expand the stable-key adapter to sprite-tree equipment, player, NPC, and
  object frames.
- Verify shifts, bounds, mirroring, palette masks, partial sequences, and cache
  invalidation under real movement and camera changes.
- Add replacements in reviewable batches rather than a bulk conversion.

### Phase 4: Authoring workflow

- Add validation/export tooling and a concise contributor guide for naming,
  metadata, transparent padding, scaling, and visual review.
- Keep a ledger of implemented keys so PNG filenames do not become the only
  source of truth.

### Phase 5: Release promotion

- Measure startup cost, memory, frame pacing, and package size.
- Complete private visual testing across representative scenes and equipment.
- Decide whether the option remains opt-in or can become the default only
  after coverage and fallback behavior are proven.

## Required Automated Coverage

- Disabled mode returns the exact canonical path/object behavior.
- Enabled mode selects an available valid override.
- Missing, malformed, or metadata-incompatible entries fall back and log once.
- A partial multi-frame replacement falls back frame by frame.
- Logical dimensions, shifts, bounds, anchors, and transparency are preserved.
- Mask-compatible and final-color equipment policies cannot double-recolor.
- Toggling invalidates derived textures/caches and preserves software/OpenGL
  parity.
- The player build contains the catalog and every declared packaged asset.
- Repeated rendering performs no repeated disk I/O or PNG decode.
- Existing client builds, renderer guardrails, and release hotkey policy remain
  green.

## Acceptance Criteria

- Original sprite archives and current custom-sprite behavior remain intact.
- The player has one clear, persisted, reversible remastered-sprite toggle.
- Mixed original/remastered coverage is safe at sprite and animation-frame
  granularity.
- Bad or absent remastered assets visibly degrade only to canonical art.
- Both renderers share the same resolution decision and remain stable after a
  live toggle.
- At least the proof set is visually approved on a private client before any
  release handoff.
- Diagnostics allow another AI session to identify loaded, missing, invalid,
  and fallback entries without relying only on visual inspection.

## AI-3 Working Agreement

AI-3 owns this umbrella while it remains on one focused topic branch. It may
iterate through inventory, foundation, proof assets, testing, and refinement
without asking the manager to authorize every small adjustment. It should:

- checkpoint after the key contract, safe foundation, and each visually useful
  proof increment;
- update this plan with discovered identities, decisions, and the asset ledger;
- keep original fallback behavior working at every checkpoint;
- report private test instructions and stop short of live deployment;
- request a manager handoff once the foundation and a coherent visually
  testable proof are ready for review, rather than handing off every experiment.

## Open Decisions

- Exact stable key spelling and namespace boundaries after Phase 0 inventory.
- Whether higher-resolution PNGs scale to canonical logical bounds or may
  declare carefully constrained display dimensions.
- Which three sprites form the first proof set.
- Final settings section and wording after checking available UI space.
- Whether a future external user-mod directory is desirable after the curated
  packaged system is mature.
