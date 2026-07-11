# Remastered Sprite Override Plan

Status: active foundation plan for AI-3 implementation and iterative visual
testing. Phase 0 architecture review began on 2026-07-11 against commit
`a2db9c82f`.

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
  ui/
  entities/
  static/
```

The client build already packages PNG files beneath
`dev/myworld/assets/sprites/`, but its current Ant include does not package
JSON. The authoritative catalog will be `manifest.json`; the build must package
it beside the art. The desktop client has no JSON library other than unrelated
Discord runtime code, so Phase 1 should not add an ad hoc render-time parser or
a large dependency. Use the repository's established generated-data pattern:

1. a small build/developer tool validates `manifest.json` and generates a
   deterministic Java catalog data class;
2. the generated class is committed and checked for drift by automation;
3. runtime loads the generated catalog without parsing arbitrary JSON; and
4. the original manifest is still packaged for auditing, release validation,
   and future tooling.

If implementation evidence strongly favors a small dependency-free runtime
format instead, record that decision here before changing the source of truth.
Do not maintain two independently authored catalogs.

PNG pixels are not enough to reconstruct the current `Sprite` contract. Each
catalog entry must identify the replacement and validate the metadata relevant
to its canonical sprite, including:

- stable sprite key and PNG path;
- optional sheet frame, row, and column information;
- expected canonical pixel width/height, `requiresShift`, X/Y shift, and bound
  width/height;
- category and any scale or anchor policy;
- palette-mask/recolor policy where equipment or character art needs it.

Canonical placement metadata should be inherited and copied automatically,
not re-authored as a second source of truth. The expected values in the
manifest are compatibility assertions: if the runtime canonical frame no
longer matches them, reject only that override and use the runtime canonical
frame. This also protects players whose active `.osar` sprite pack changes a
frame contract.

The version-1 entry schema should contain at least:

```json
{
  "key": "sprite/items/273/0",
  "png": "items/dragon-sword.png",
  "sourceRect": null,
  "expectedCanonical": {
    "pixelWidth": 48,
    "pixelHeight": 32,
    "requiresShift": false,
    "shiftX": 0,
    "shiftY": 0,
    "boundWidth": 48,
    "boundHeight": 32
  },
  "sizePolicy": "exact-canonical",
  "recolorPolicy": "inherit",
  "alphaThreshold": 128
}
```

`sourceRect` is optional and supports one frame cut from a declared sheet. The
generator must reject duplicate keys, invalid or overlapping declarations when
overlap is forbidden, out-of-bounds rectangles, unknown policy values, and
paths outside the remaster root. One PNG may intentionally serve more than one
key.

Catalog paths must be normalized beneath the packaged remaster root. Reject
absolute paths and traversal rather than allowing the manifest to read an
arbitrary local file.

## Stable Sprite Identity

The resolver needs a stable key before a large art library is authored. Phase
0 inventory found that the normal Spoiled Milk client uses
`Custom_Sprites.osar` with `Config.S_WANT_CUSTOM_SPRITES = true`. The archive
currently contains `11` subspaces, `1,002` entries, `812` one-frame sprites,
and `190` animations:

| Subspace | Entries | Current contract |
| --- | ---: | --- |
| `clipping` | 1 | static sprite |
| `crowns` | 12 | static sprites |
| `equipment` | 154 | 2 entries with 15 frames, 151 with 18, 1 with 27 |
| `GUI` | 44 | static sprites |
| `GUIutil` | 10 | static sprites |
| `items` | 651 | static sprites |
| `npc` | 28 | 15-, 18-, or 27-frame animations |
| `player` | 8 | 18-frame animations |
| `projectiles` | 7 | static sprites |
| `skill_icons` | 20 | static sprites |
| `textures` | 67 | renderer materials, excluded from this sprite plan |

All current subspace and entry names fit `[A-Za-z0-9._-]+`, but the key parser
must still reject separators and traversal instead of assuming future names
remain simple.

### Selected version-1 key grammar

Use logical definition identity, not the physical array slot from whichever
archive happens to be active:

```text
sprite/<lowercase-subspace>/<lowercase-entry>/<zero-based-frame>
```

Examples:

- `sprite/items/273/0`
- `sprite/gui/7/0`
- `sprite/equipment/sword/15`
- `sprite/npc/dragon/4`

For `ItemDef` and `SpriteDef`, derive the subspace and entry from
`spriteLocation`. For `AnimationDef`, use `category`, `name`, and the requested
frame offset. This remains stable across custom and authentic sources and
correctly shares one override where multiple item definitions intentionally
alias the same visual. Type and equipment layer are compatibility metadata,
not part of the key.

The authentic item offset (`mudclient.spriteItem + spriteID`) is used only to
obtain the canonical fallback. It must never appear in a public remaster key.
Incidental animation `number` and render-array indices are also not keys.

### Source precedence

Canonical resolution already has multiple layers. Preserve this order and put
the remaster decision above it:

1. valid packaged remastered override, when enabled;
2. existing project PNG injection such as external equipment or an
   `external-png:` item;
3. active `.osar` sprite-pack replacement;
4. `Custom_Sprites.osar` or the existing authentic-array path; and
5. existing unknown-sprite behavior only where the current canonical path
   already uses it.

In practice external equipment is installed after active sprite packs today,
so Phase 1 must bind canonical frames only after `loadSprites()` has completed.
The exact fallback is the final canonical object after all existing layers,
not necessarily the base OSAR frame.

### Supported adapters and exclusions

Version 1 should adapt the three normal selection paths:

- `ItemDef` -> logical `spriteLocation` -> frame `0`;
- `SpriteDef` (GUI, crowns, projectiles) -> logical `spriteLocation` -> frame
  `0`; and
- `AnimationDef` -> category/name/requested frame.

The following need explicit later adapters and must not be silently treated as
covered by the foundation:

- generated altar glyph/orb sprites;
- spell, prayer, and summon icon arrays loaded from project PNGs;
- combat-effect, projectile-effect, mirrored, impact, and scaled derived
  sprites owned by the separate animation migration system;
- captcha and network-created images;
- the runtime minimap image and other generated buffers; and
- `textures`, terrain materials, models, walls, and scenery. RSC scenery is
  normally geometry/material data, not a sprite override.

`Panel` stores the appearance-screen arrow `Sprite` objects at panel creation.
Those twelve cached controls are the known retained-reference exception to
otherwise per-draw selection. A live toggle must rebuild/rebind that panel or
resolve at its draw boundary. Add a regression test so this exception does not
leave mixed art after toggling.

The original minimum identity requirements remain useful guardrails:

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

Keep the feature out of `mudclient` as much as the inherited architecture
allows. Target these ownership boundaries:

- `RemasteredSpriteSettings`: persisted setting, runtime override for
  diagnostics, and profile-bundle behavior;
- `RemasteredSpriteKey`: normalized, validated key value;
- generated `RemasteredSpriteCatalogData`: declarations only;
- `RemasteredSpriteCatalog`: runtime validation and immutable entry lookup;
- `RemasteredSpriteResolver`: canonical binding, lazy decode/cache, fallback,
  counters, and one-shot failure reporting; and
- a small reusable packaged-asset reader extracted from or shared with the
  existing `mudclient` helpers instead of duplicating JAR/filesystem probing.

The runtime sequence is:

1. Load the player setting with the other desktop client settings.
2. Load and retain canonical sprites through the unchanged existing paths.
3. After sprite packs and existing project PNG injections finish, bind logical
   keys to the final canonical `Sprite` objects and their contracts.
4. Load the generated catalog declarations into an immutable lookup.
5. Resolve through the `ItemDef`, `SpriteDef`, and `AnimationDef` adapters,
   passing the canonical object as the guaranteed fallback.
6. When enabled and a compatible entry exists, return one immutable cached
   replacement object. Otherwise return the exact canonical object.
7. Make software and OpenGL rendering consume that same returned `Sprite` so
   the two renderers cannot drift.

Do not mutate canonical pixel arrays or install replacement pixels into
`spriteTree`. Canonical and remastered objects must have distinct identities.
The OpenGL texture cache already keys by `Sprite` object identity plus color
transform, so a normal on/off toggle can safely retain separate atlas entries
without returning stale pixels. Toggle itself should not tear down the whole
OpenGL presenter. A catalog reload or replacement-object revision does need an
explicit frame-boundary cache retirement strategy so abandoned atlas entries
do not accumulate forever.

The implementation must not destructively overwrite the only copy of the
canonical sprite. A player must be able to return to original art without
restarting or re-reading all original archives. Cache keys must account for
catalog revision where object identity alone is insufficient. Toggling the
option must rebind the retained `Panel` references described above and any
later software-side derived caches.

Load or decode remastered PNGs at startup, during a controlled asset reload,
or lazily once per catalog entry. Never perform filesystem I/O or image decode
once per frame. Use a bounded cache if loading the complete catalog would be
materially expensive.

Use an immutable catalog/resolver state published in one swap. If a future
developer reload command is added, build and validate the next state away from
the active render state, then swap it at a safe client-frame boundary. A failed
reload keeps the previous valid state; it must not leave a half-populated
catalog active.

## Player Setting

Add a persisted client-local option labelled clearly, such as
`Remastered sprites: On/Off`, under the most discoverable graphics or
interface settings group. Default it to **Off** while coverage is experimental.
It is independent of the server and other players.

The setting is independent in storage but participates in renderer presets:

- choosing `Classic` sets remastered sprites **Off**;
- choosing `Remaster` sets remastered sprites **On** once the proof is approved
  for private testing;
- changing the row manually marks the displayed renderer preset `Custom`; and
- choosing `Custom` does not itself alter the current sprite setting.

Until the proof is approved, both a new installation and an existing saved
Remaster profile should remain Off unless the player explicitly enables the
row. Record the exact promotion checkpoint here rather than changing the
default incidentally.

The target behavior is an immediate live toggle. If the first proof requires a
controlled renderer/asset reload, make that explicit to the player and keep it
safe; do not silently leave half of the caches in the previous mode.

## Pixel, Metadata, and Rendering Contracts

### Transparency

The current `Sprite` pipeline is not true ARGB. It stores RGB pixels with zero
as the transparent key; software draw loops test zero, and OpenGL uploads every
nonzero sprite pixel with alpha `255`. The unused `transparentPixels` field is
not an alpha channel.

Therefore the safe foundation contract is binary transparency:

- decode the PNG as ARGB in a controlled loader;
- treat alpha below the catalog threshold as transparent zero;
- treat alpha at or above the threshold as opaque RGB;
- normalize opaque black away from zero, using the existing
  `RendererTransparency.OPAQUE_BLACK_REPLACEMENT` rule; and
- default the manifest threshold to `128` unless visual testing justifies a
  per-entry value.

Do not claim that fractional PNG alpha is preserved. Assets in the first proof
should use intentional hard transparency or antialiasing authored for the
binary threshold. True fractional-alpha sprites require a separate shared
`Sprite`/software/OpenGL contract extension and parity tests before the
manifest may declare such a mode.

### Dimensions and placement

The legacy scaled draw routines conflate source pixel dimensions with logical
frame/bound dimensions in several places. Simply attaching a 2x PNG to copied
shift/bound metadata can double destination coverage or change unscaled UI
layout. Version 1 therefore supports `sizePolicy: exact-canonical` only:

- the decoded frame dimensions must exactly equal the canonical frame's pixel
  dimensions;
- `requiresShift`, shift X/Y, bound width/height, ID, and package metadata are
  copied from the canonical frame; and
- manifest expected-canonical fields must match before the override is valid.

This restriction is a foundation safety rail, not the final remaster ambition.
Before accepting higher-resolution art, add an explicit logical-source-size
contract to `Sprite`/draw commands or prove a category-specific resampling
policy. Test unscaled UI draws, scaled item draws, shifted entity layers,
mirroring, clipping, hit targets, and OpenGL command-sized texture building.
Never silently downsample master artwork in place; generated runtime renditions
must remain reproducible from the authored PNG.

### Recoloring

Version 1 supports `recolorPolicy: inherit`: the replacement receives the same
item/equipment/player mask transform exactly once. Mask-authored replacements
must preserve the grayscale and blue-mask regions expected by existing draw
code. For a static path that provably receives no recolor arguments,
`final-color` may be accepted later as an explicit no-op policy.

Do not enable final-color equipment or character art in the foundation. That
requires the resolved result to carry policy beside `Sprite` through both the
software mask loops and `RendererSpriteTransform`; returning only a different
pixel object is not sufficient. Reject unsupported policy/key combinations at
catalog generation and runtime validation.

### Fallback rules

- Resolve per sprite and per frame. A partially remastered animation may use
  its original frame wherever the corresponding PNG is absent.
- A missing file, malformed PNG, invalid frame declaration, or incompatible
  metadata logs one useful diagnostic and falls back to the canonical sprite.
  It must not crash, show a checkerboard, or hide the sprite.
- Disabled mode should avoid probing or decoding remastered assets except for
  any minimal catalog work required by the settings UI.
- Decode PNGs as ARGB input, then apply the versioned transparency contract
  above. Never interpret visible black as transparency.
- Keep logical dimensions, shifts, anchors, and bounds separate in the
  catalog model even while version 1 requires matching source dimensions.
- Never return an unknown/checkerboard sprite because only the remaster entry
  failed. Return the exact canonical object supplied to the resolver.

## Asset Safety, Provenance, and Budgets

Even curated packaged data needs deterministic validation:

- release builds resolve remaster art only from the embedded catalog-declared
  resources; a development build may prefer the matching repository path for
  iteration, but must not scan arbitrary user folders;
- normalize separators and require paths relative to
  `myworld-assets/sprites/remastered/`;
- reject absolute paths, `..`, empty segments, control characters, and
  case-colliding keys/files that behave differently on Windows and Linux;
- read and validate PNG headers before full `ImageIO` decode;
- cap width, height, total pixels, sheet frames, catalog entries, and estimated
  decoded bytes with named constants and actionable errors;
- reject duplicate keys and files missing from the packaged JAR;
- make generated catalog order stable; and
- make invalid entries fail individually while structural catalog corruption
  fails the new catalog/reload as a whole.

Every shipped PNG needs redistribution permission and a provenance/credit
entry consistent with `dev/myworld/assets/README.md` and
`release/player/ASSET-SOURCES.txt`. The manifest should carry a required
credit/provenance identifier, including an explicit project-author identifier
for original work, so validation can reject uncredited release art.
Do not commit speculative or license-unclear proof images merely to exercise
the loader; tests can generate temporary fixture PNGs.

Establish baseline budgets before broad coverage:

- catalog parse/generation time;
- startup or first-use decode time;
- decoded CPU bytes and OpenGL atlas allocation growth;
- packaged compressed/uncompressed size; and
- toggle/reload frame-time spike.

The proof does not need aggressive limits, but diagnostics and tests must make
growth visible before hundreds of frames are added.

## Diagnostics

Expose compact, AI-readable counters through the existing
`RendererDiagnosticSession` records and F6 diagnostic capture rather than
regular per-frame logs:

- catalog entries and successfully loaded replacements;
- remastered resolutions and canonical fallbacks;
- missing, invalid, or metadata-incompatible entries;
- cache hits, misses, memory estimate, and reload count;
- active art-mode setting and renderer backend.

Each invalid catalog entry should also produce one actionable startup/reload
message containing its stable key and reason.

Distinguish `not-declared` from `declared-but-invalid`: normal partial coverage
must not flood logs or inflate error counts. Track fallback reasons in bounded
counters, and retain only a bounded sample of keys for diagnostics. Add the
catalog schema version, revision, and generated-data signature so captures can
be compared to the exact asset set.

## Implementation Phases

### Phase 0: Inventory and key contract

- [x] Trace the canonical archive, sprite-array, sprite-tree, item, equipment,
  software-renderer, and OpenGL texture paths.
- [x] Record the initial archive counts, stable key grammar, precedence, and
  unsupported categories in this plan.
- [x] Confirm where settings persistence, preset behavior, retained panel
  references, and OpenGL texture identity belong.
- [ ] Add a repeatable inventory/audit tool that emits the archive counts,
  frame contracts, duplicate logical identities, and manifest coverage without
  changing assets.
- [ ] Verify every current `ItemDef`, `SpriteDef`, and `AnimationDef` logical
  location maps to the selected grammar, including aliases and authentic-mode
  fallback fixtures.
- [ ] Decide the initial named safety limits and generated catalog package.
- [ ] Select the smallest useful proof sprites with the owner before producing a
  large art set.

### Phase 1: Safe foundation

- Add the setting, catalog schema/loader, resolver, validation, and diagnostics
  with zero required overrides.
- Prove that disabled mode and enabled mode with an empty catalog both render
  canonical art.
- Keep version 1 to exact-canonical dimensions, inherited recoloring, and
  binary-alpha conversion.
- Bind canonical objects after OSAR packs and existing external equipment have
  completed loading.
- Rebind the retained appearance-panel arrows on a live toggle.
- Add packaging and fallback tests before expanding coverage.

### Phase 2: Visually testable proof

- Add a deliberately small cross-section: one same-sized UI sprite, one item
  icon exercised in inventory and on the ground, and one complete short
  animation or equipped entry when practical. Use test fixtures, not an
  intentionally incomplete shipped animation, to prove per-frame fallback.
- Test software and OpenGL paths, toggling on and off in the same client
  session.
- Use a private client/server for owner visual approval. Do not deploy this
  work directly to the public live server.

### Phase 2B: Higher-resolution and alpha decision gate

- Decide whether the next visual gain needs higher source resolution,
  fractional alpha, or only improved same-resolution art.
- If higher resolution is needed, introduce and test an explicit logical
  source-size/pixel-density contract before accepting such manifest entries.
- If fractional alpha is needed, extend the shared sprite representation and
  both renderer paths together; do not make it an OpenGL-only visual feature
  while claiming software parity.
- Keep version-1 assets valid and fallback-compatible after either extension.

### Phase 3: Character and animation coverage

- Expand the stable-key adapter to sprite-tree equipment, player, and NPC
  frames.
- Verify shifts, bounds, mirroring, palette masks, partial sequences, and cache
  invalidation under real movement and camera changes.
- Add replacements in reviewable batches rather than a bulk conversion.
- Add final-color recolor policy only after policy travels explicitly through
  software and OpenGL draw transforms.

### Phase 4: Authoring workflow

- Add validation/export tooling and a concise contributor guide for naming,
  metadata, transparent padding, scaling, and visual review.
- Keep a ledger of implemented keys so PNG filenames do not become the only
  source of truth.
- Generate contact sheets or canonical/remastered/fallback comparison captures
  for review without overwriting source art.
- Validate provenance IDs, alpha usage, dimensions, transparent padding,
  canonical metadata assertions, and package-size deltas before checkpointing
  an art batch.

### Phase 5: Release promotion

- Measure startup cost, memory, frame pacing, and package size.
- Complete private visual testing across representative scenes and equipment.
- Decide whether the option remains opt-in or can become the default only
  after coverage and fallback behavior are proven.

## Required Automated Coverage

- Disabled mode returns the exact canonical path/object behavior.
- Enabled mode selects an available valid override.
- A missing or malformed saved setting defaults Off; an explicit supported
  runtime override wins predictably and is reported in diagnostics.
- Empty-catalog enabled mode returns exact canonical objects and performs no
  PNG decode.
- Missing, malformed, or metadata-incompatible entries fall back and log once.
- A partial multi-frame replacement falls back frame by frame.
- Logical dimensions, shifts, bounds, anchors, and transparency are preserved.
- Version-1 alpha thresholding preserves transparent zero and visible black.
- Unsupported size and recolor policies are rejected instead of partially
  applied.
- Mask-compatible equipment receives its normal recolor once.
- Toggling rebinds retained panel references, selects distinct immutable
  objects, and preserves software/OpenGL parity without full presenter
  teardown.
- Canonical precedence fixtures cover base OSAR, active sprite pack, existing
  external PNG injection, and remaster override/fallback.
- Logical keys remain the same across custom and authentic canonical sources.
- Duplicate keys, path traversal, case collisions, excessive PNG dimensions,
  invalid sheets, and structural catalog corruption are rejected safely.
- The player build contains the catalog and every declared packaged asset.
- Generated catalog data is deterministic and matches the manifest.
- Every release asset has a valid provenance/credit identifier.
- Repeated rendering performs no repeated disk I/O or PNG decode.
- Reload failure retains the previous valid catalog; successful reload retires
  abandoned GPU/cache state at a safe boundary.
- Diagnostics distinguish absent coverage from invalid declarations and remain
  bounded under repeated fallback.
- The settings row remains reachable and clickable at supported aspect ratios
  despite the existing tall, scrollable Graphics section.
- Existing client builds, renderer guardrails, and release hotkey policy remain
  green.

## Private Visual Test Matrix

Prepare these instructions before requesting handoff. Use the private server
and development client only.

1. Start with the option Off; capture the proof UI sprite, inventory item,
   ground item, and animation from representative camera zooms.
2. Toggle On without relogging; confirm each declared proof key changes and
   undeclared neighboring sprites remain byte-for-byte/capture-equivalent to
   canonical art.
3. Toggle Off again; confirm exact canonical art returns, including the
   appearance-screen arrows retained by `Panel`.
4. Repeat in OpenGL-primary and the available software-renderer diagnostic
   path. Compare placement, clipping, mirrors, masks, and visible black.
5. Temporarily remove or corrupt one proof asset in a development fixture;
   confirm only that key/frame falls back and one actionable diagnostic is
   emitted.
6. Exercise inventory, bank, trade/shop/production UI, ground-item projection,
   camera rotation/tilt/zoom, combat pose, walk pose, and relog where relevant
   to the selected keys.
7. Record F6/session diagnostics, startup time, first-use decode spike, atlas
   growth, and package-size delta.
8. Restore the validated packaged assets, rerun focused automation, and give
   the owner exact toggle and route instructions. Do not deploy to live.

## Acceptance Criteria

- Original sprite archives and current custom-sprite behavior remain intact.
- The player has one clear, persisted, reversible remastered-sprite toggle.
- Mixed original/remastered coverage is safe at sprite and animation-frame
  granularity.
- Bad or absent remastered assets visibly degrade only to canonical art.
- Both renderers share the same resolution decision and remain stable after a
  live toggle.
- Version-1 behavior is honest about binary alpha and exact canonical source
  dimensions; unsupported richer formats fail back instead of degrading
  differently per renderer.
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

- Exact package/class names for the generated catalog and resolver components.
- Named safety limits for catalog entries, PNG dimensions/pixels, sheet frames,
  decoded bytes, and diagnostic samples.
- Whether higher-resolution PNGs use an extended logical pixel-density
  contract or category-specific generated renditions after Phase 2B.
- Which three sprites form the first proof set.
- Final settings wording and row placement after checking the already tall
  OpenGL Graphics list.
- The proof-approval checkpoint at which the Remaster preset begins enabling
  the option automatically.
- Whether a future external user-mod directory is desirable after the curated
  packaged system is mature.

## Decision Ledger

- 2026-07-11: Keep remastered sprites separate from
  `Config.S_WANT_CUSTOM_SPRITES` and the combat-VFX animation migration.
- 2026-07-11: Use logical `sprite/<subspace>/<entry>/<frame>` keys; physical
  authentic array offsets remain fallback implementation details.
- 2026-07-11: Treat the final post-pack/post-injection object as canonical and
  preserve it unchanged.
- 2026-07-11: Keep `manifest.json` authoritative and generate deterministic
  runtime catalog data unless implementation evidence justifies one
  dependency-free runtime source instead.
- 2026-07-11: Foundation entries require exact canonical pixel dimensions,
  inherited recoloring, and thresholded binary transparency. Higher-resolution,
  final-color, and fractional-alpha contracts require later decision gates.
- 2026-07-11: Classic will force the feature Off. Remaster will force it On
  only after the private proof is approved; manual changes mark the renderer
  preset Custom.
