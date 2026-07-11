# Fixes And Changes Backlog Plan

Status: active; Tasks 1 through 4 implemented and guarded.
Owner: An-actual-duck, with independently checkpointed tasks coordinated from this plan.
Implementation branch: `feat/player-experience-backlog`.

## Summary

This plan preserves and coordinates the current "Fixes and Changes" backlog:

- [x] Default camera mode should be Manual.
- [x] Correct indoor/upper-floor roof hiding when roofs are enabled.
- [ ] Explore adjustable terrain ambient occlusion/shading.
- [ ] Explore adjustable object shading.
- [x] Add optional text-list layouts for Magic, Prayer, and Summoning.
- [ ] Add a Lumbridge bank or bank chest.

The list crosses client defaults, account persistence, renderer ownership,
interface layout, and server world content. It must not be implemented as one
code commit. This document is the coordinating ledger; the manager-approved
umbrella branch keeps each task in its own tested and pushed checkpoint before
the next task begins.

## Current-System Findings

### Camera default

- `mudclient.optionCameraModeAuto` currently initializes to `true` before the
  server sends the account setting.
- Camera mode is account-persisted as game-setting index `0` and the client
  already sends explicit Auto/Manual changes over packet `111`.
- New accounts currently inherit `cameraauto DEFAULT 1` in the MySQL and SQLite
  schema templates and in `server/inc/sqlite/myworld_seed.db`.
- `MySqlQueries.createPlayer` omits `cameraauto`, so both MySQL and the SQLite
  subclass depend on that database default.
- Existing players' saved choices should not be migrated or overwritten. The
  requested change is a new-account and pre-sync default, not removal of Auto.

### Roof visibility

- Global `Config.C_HIDE_ROOFS` currently controls whether roof geometry is
  included in cached world products at all. Toggling it performs a region
  reload through `reloadCurrentRegionForRoofVisibility()`.
- With roofs enabled, the legacy frame path removes current roof grids every
  frame, then re-adds them only when `lastHeightOffset == 0` and the player's
  collision tile does not contain bit `0x80`. This makes upper-floor behavior
  depend on a ground-plane-only condition.
- On the ground plane, the same loop also removes/re-adds plane 1 and 2 wall
  and roof grids as a group. It has no explicit, named contract for outdoor,
  indoor, upstairs, or roof-above-player states.
- The resident OpenGL paths primarily filter roofs by the global
  `C_HIDE_ROOFS` setting. A fix must keep legacy scene ownership, resident
  chunks, world-product cache keys, roof shadow coverage, and toggle/relog
  lifecycle in agreement.

### Terrain and object shading

- Remaster shading currently combines directional diffuse,
  `remasterClassicShadeFactor`, and `remasterLocalReliefFactor` in the resident
  shader. Terrain and non-terrain geometry use different fixed curves, but
  share one runtime-only `RendererReliefSettings` strength.
- The accepted terrain shadow mask also contains directional cast shadows and
  scenery contact shadows. The contact channel is described as AO-like
  grounding, while interior terrain receivers are suppressed.
- Therefore "terrain ambient occlusion/shading" could refer to at least two
  different controls: local terrain relief/diffuse contrast or terrain shadow
  mask/contact strength. Those channels must be measured separately before a
  player-facing label is chosen.
- Material-family metadata is now available, but model kind remains the safer
  first ownership boundary for terrain versus scenery. Exploration must not
  silently change Classic or normal Remaster defaults.

### Magic, Prayer, and Summoning layouts

- All three tabs are rendered and clicked inside the large
  `mudclient.drawUiTabMagic()` path.
- The current presentation is a four-column, two-visible-row icon grid with
  separate scroll-row state and click arithmetic for Magic, Prayer, and
  Summoning.
- Magic filters hidden spells and owns selection, autocast, self-cast, and
  inventory-target behavior. Prayer owns active/allocation checks and the
  current prayer-book ordering. Summoning sends an interface option by summon
  index. A text layout must call these same actions rather than create parallel
  gameplay rules.
- The option is purely presentational and should be stored in client settings,
  not consume another server game-setting/protocol index. Icon layout remains
  the default until the text mode is manually selected.
- Authentic/custom panel placement, Android's last-spell box, mouse wheel,
  scrollbar bounds, hover descriptions, disabled colors, and selected states
  all need explicit coverage.

### Lumbridge banking

- The reusable `Bank Chest` scenery is `SceneryId.BANK_CHEST` (`942`).
- The authentic Shantay Pass plugin currently claims every scenery object with
  id `942`, applies a members-world gate before coordinate-specific behavior,
  and only special-cases Shantay Pass and McGrubor's Wood. Placing another
  chest without changing ownership would make Lumbridge banking depend on an
  unrelated desert plugin.
- Standard banker NPCs refuse bank access unless their location is recognized
  by `Point.isInBank(...)`; adding a banker alone therefore also requires bank
  area data and PvP/safety review.
- A focused MyWorld bank-chest handler is the lower-content-cost direction,
  provided trigger ownership is narrowed and Ultimate Ironman plus bank-pin
  rules remain intact.
- Final placement needs an in-game map check. Proximity to the new-player spawn,
  castle routes, respawn/death flow, doors, quests, and existing scenery can
  materially change early-game convenience.

## Dependency And Risk Map

| Work item | Depends on | Primary risks |
| --- | --- | --- |
| Manual camera default | Account creation query and schema defaults | Overwriting existing choices; client/server disagreement before settings sync; retro-schema drift |
| Roof visibility | Plane/collision semantics, world-product cache, resident roof filtering | Entire floors disappearing; roofs leaking indoors; stale chunks after toggle/relog; shadow coverage disagreement |
| Shading exploration | Accepted roof behavior, renderer diagnostics, existing relief/shadow-mask channels | Mislabeling shadows as AO; double-darkening terrain; changing defaults; shader/cache churn |
| Terrain shading control | Exploration contract and visual route | Crushed dark terrain, noisy slopes, indoor mismatch, Classic contamination |
| Object shading control | Exploration contract, model-kind/material ownership | Flat or black two-sided scenery, foliage/ore over-tuning, sprite confusion |
| Text-list layouts | Shared tab action/index mapping and client-setting persistence | Wrong spell/prayer/summon activation; scroll/click mismatch; mobile/custom UI regressions |
| Lumbridge bank chest | Approved placement and chest trigger ownership | Bypassed UIM/pin rules; unwanted members restriction; spawn/economy distortion; scenery overlap |

## Focused Implementation Tasks And Order

### 1. Default new players to Manual camera

Recommended branch: `fix/default-manual-camera`

This is first because it is small, isolated, easy to guard, and establishes a
clean settings/default change without entangling renderer behavior. The full
implementation-ready specification is below.

### 2. Correct enabled-roof indoor and upper-floor visibility

Checkpoint scope: roof visibility only.

Define a named roof-visibility state from player plane and roof/collision
coverage, then apply the same decision to legacy scene grids and resident
OpenGL draw ownership. Do this before shading so indoor and upper-floor visual
tests have trustworthy geometry.

The first checkpoint should be diagnostic/test-only if the intended visibility
matrix cannot be proven from the current conditions. Required test states:

- ground-floor outdoor;
- ground-floor under a roof;
- first/second upper floors, both covered and uncovered;
- stairs/plane transitions;
- global Hide Roofs on/off;
- logout/relogin and section transition;
- Classic and Remaster renderer profiles.

#### Implementation record — 2026-07-11

- Added the named per-frame states `VISIBLE`, `HIDDEN_BY_SETTING`,
  `HIDDEN_INDOORS`, and `HIDDEN_ABOVE_ACTIVE_FLOOR`. Resolution preserves the
  original client contract: enabled roofs appear only on an outdoor ground
  tile; a covered ground tile hides roof layers and structures above the
  active floor; an upper floor keeps its active walls while hiding the roof
  above it.
- The legacy scene loop and resident OpenGL chunks now consume the same state.
  This closes the split where projected geometry hid roofs indoors/upstairs
  but resident chunks filtered only from the global saved option.
- Global Hide Roofs still selects no-roof cached world products and reloads
  the region. Resident filtering additionally suppresses upper-plane wall
  batches from a ground-floor view, matching the legacy scene-grid behavior,
  without suppressing active-floor walls upstairs.
- `Ctrl+F9` capture metadata now records `activePlane`, `roofVisibility`, and
  `roofsVisible`, making a visual report directly correlatable with the
  resolved technical state.
- The focused executable matrix covers outdoor ground, covered ground, upper
  floors, global-option precedence, active-floor walls, upper-plane walls,
  roofs, and terrain. The full renderer guard suite and Java 8 client build
  pass.
- This task deliberately preserves the legacy whole-grid visibility unit. A
  connected-roof-volume refinement would require a separate spatial ownership
  design and is not necessary to correct renderer parity.

### 3. Add optional text-list spellbook layouts

Checkpoint scope: spellbook presentation only.

Add one persisted client-side `Icons / Text` presentation setting and a shared
row model for the three tabs. Reuse the current action methods and canonical
indices. Keep icon mode as default. This is one cohesive UI feature, but it
must not be bundled with roof, shading, or content changes.

#### Implementation record — 2026-07-11

- Added one local `spellbook_layout` preference with `Icons` as the default and
  `Text` as the optional mode. It is stored in `clientSettings.conf`; no server
  game-setting or protocol index was added.
- Added the preference to the General settings Interface section. One choice
  controls Magic, Prayer, and Summoning consistently, as approved in this
  plan.
- Text mode uses the existing 196x90 scrolling panel in authentic and custom UI
  placement. Each tab retains its own text scroll position, mouse-wheel and
  scrollbar behavior, hover description, and tooltip/cost area.
- Magic text rows preserve hidden-spell filtering, canonical spell indices,
  level/rune colors, selected state, and autocast state. Both layouts call the
  same validation and spell-selection/autocast action.
- Prayer text rows preserve prayer-book order, active and unavailable colors,
  current allocation-point rules, and the same activate/deactivate packets as
  icon mode. Summoning rows preserve canonical summon indices and call the
  existing interface action. The Android last-spell box remains available in
  either layout.
- The focused executable settings fixture verifies default, cycle, save/load,
  compatibility alias, and fallback behavior. Source guards verify all three
  row builders, shared actions, canonical Magic mapping, per-tab scrolling,
  persistence, and the settings control. Prayer UI guards and the Java 8 client
  build pass.

### 4. Separate terrain and object shading diagnostics

Checkpoint scope: diagnostic-only shading ownership.

First add runtime/diagnostic-only terrain and object strength ownership with
parity defaults. Attribute whether the requested terrain control belongs to
local relief, diffuse response, shadow-mask directional strength, or contact
strength. Record the chosen terms in captures/F6 before exposing a normal
setting.

#### Implementation record — 2026-07-11

- Split the old shared runtime relief override into independent terrain and
  non-terrain/object scopes. Both retain the accepted `Max`/`2.0` default, and
  the legacy `SPOILED_MILK_OPENGL_RELIEF` override remains a compatibility
  fallback that drives both scopes when no scoped override is supplied.
- Added diagnostic overrides
  `SPOILED_MILK_OPENGL_TERRAIN_RELIEF` and
  `SPOILED_MILK_OPENGL_OBJECT_RELIEF`. The resident shader selects between the
  two from model kind; the object scope currently means all non-terrain static
  geometry, including walls, roofs, scenery, and wall objects.
- Identified the existing channels precisely: local relief reshapes captured
  legacy base light; directional diffuse uses fixed model-kind curves; the
  terrain-only shadow mask combines directional wall/scenery projection with
  a contact channel; non-terrain objects do not currently receive that shadow
  mask.
- Added `SPOILED_MILK_REMASTER_DIRECTIONAL_SHADOW_ALPHA_SCALE` as a
  diagnostic-only terrain shadow comparison knob with a parity default of
  `1.0`. The existing contact-alpha runtime override remains the contact
  comparison knob. Both participate in mask cache signatures.
- F6 now reports terrain/object relief modes and the directional/contact shadow
  tuning. `Ctrl+F9` metadata records stable individual shading fields,
  including the fixed diffuse response and the absence of object shadow-mask
  receiving, so owner observations can be attributed to one channel.
- The executable diagnostic fixture verifies parity defaults, legacy shared
  override compatibility, and independent scoped overrides. The full renderer
  guard suite and Java 8 client build pass without changing default visuals.

### 5. Promote an accepted terrain shading/AO control

Recommended branch: `feat/terrain-shading-control`

Only after Task 4 visual comparison, persist the narrow accepted terrain
control and place it in the appropriate Graphics/profile ownership. Do not
combine it with object tuning. Classic stays unchanged and existing Remaster
appearance remains the migration/default value.

### 6. Promote an accepted object shading control

Recommended branch: `feat/object-shading-control`

Use model kind first and material families only where evidence supports a
different response. Test ordinary scenery, walls, wall objects, foliage, ore,
emissive objects, two-sided materials, and animated scenery independently from
terrain.

### 7. Add a Lumbridge bank chest

Recommended branch: `content/lumbridge-bank-chest`

Prefer a bank chest over a banker unless map review shows a real bank room is
appropriate. Approve the exact tile before implementation. Narrow the existing
Shantay chest trigger to its owned coordinates or introduce explicit shared
chest routing, then add the Lumbridge placement and a MyWorld-owned handler.
Preserve bank-pin validation, Ultimate Ironman denial, and normal bank close /
movement behavior.

## Task 1 Implementation-Ready Specification

### Goal

New players and the client before account-setting synchronization start in
Manual camera mode. Auto remains selectable and existing saved Auto/Manual
values continue to load unchanged.

### Intended changes

- Change `mudclient.optionCameraModeAuto`'s initialization from `true` to
  `false`.
- Make the shared account-creation SQL explicitly insert `cameraauto = 0` so
  both MySQL and the SQLite subclass get the new behavior even on an existing
  deployed schema whose historical column default is still `1`.
- Change the MySQL core/retro and SQLite core/retro schema-template defaults to
  `0` for new databases.
- Update the empty canonical `myworld_seed.db` schema default reproducibly;
  preserve every other table, index, trigger, and row.
- Replace or supersede the historical `correct_cameraauto_default.sql` intent
  with a forward migration/default statement for fresh or externally managed
  MySQL deployments. Do not update existing player values.
- Add `tests/myworld/test-default-manual-camera.py` and include it in the
  appropriate small client/server guard suite.

### Guard assertions

- Client fallback is Manual.
- Account-creation SQL names `cameraauto` explicitly and supplies `0`.
- Every text schema template declares `cameraauto DEFAULT 0`.
- The canonical seed's `PRAGMA table_info(players)` reports default `0`.
- The setting packet and save/load paths still use game-setting index `0` and
  still accept both values.
- No SQL statement performs a bulk `UPDATE players SET cameraauto = 0`.

### Validation

- Run the focused camera-default guard.
- Run `./scripts/build-client.sh`.
- Run `./scripts/build-server.sh`.
- Create a disposable new account and confirm Settings shows `Camera mode -
  Manual` on first login.
- Toggle to Auto, logout/login, and confirm Auto persists.
- Toggle back to Manual, logout/login, and confirm Manual persists.
- Confirm an existing account saved as Auto is not changed by deployment.

### Completion criteria

- New accounts reliably start Manual on SQLite and MySQL paths.
- Existing account preferences are preserved.
- Auto camera remains functional and persistent.
- The task is committed and handed back independently of every other item in
  this plan.

### Implementation record — 2026-07-11

- Client pre-sync camera state now initializes to Manual.
- Shared account creation explicitly writes `cameraauto = 0`, covering both
  MySQL and the SQLite subclass without depending on a deployed column default.
- MySQL/SQLite core and retro schema templates plus the empty canonical MyWorld
  seed now declare default `0`. The seed table rebuild preserved all other
  schema objects and passed SQLite integrity validation.
- Existing player rows are not migrated. The unchanged account load/save and
  packet-index-`0` paths continue to preserve explicit Auto and Manual choices.
- `test-default-manual-camera.py` verifies client fallback, creation SQL,
  schema/seed defaults, a materialized fresh seed account, and the absence of
  preference-overwriting SQL. Client and server Java 8 builds pass alongside
  the focused camera and player-data guards.

## Documentation Workflow

- Keep the six owner requests verbatim at the top until each is completed.
- Each implementation task updates its section and relevant source plan, then
  checkpoints independently on the manager-approved umbrella branch.
- Keep completed task detail here until the overall backlog receives its final
  roundup and handoff.

## Open Decisions

- Roof behavior is resolved for this task: retain the legacy whole-grid unit
  while making its state explicit and consistent across renderers. Connected
  roof-volume hiding remains a possible later enhancement, not a parity fix.
- Shading terminology: does "terrain ambient occlusion" mean local terrain
  relief, scenery contact shadow strength, or both as separate controls?
- Text layout is resolved as one shared Icons/Text preference for all three
  tabs. Per-tab modes would add preference and interaction complexity without
  changing any gameplay action, so they are outside this task.
- Lumbridge bank: approve a precise chest tile after an in-game map check and
  decide whether the area should merely provide bank access or become a formal
  protected bank zone.

## Backlog Completion Criteria

- Every checked request has an independently reviewable implementation commit.
- Defaults and saved settings migrate without overwriting player intent.
- Roof and shading work passes Classic/Remaster, indoor/outdoor, plane,
  relog, and section-transition tests.
- Text mode preserves all actions and icon mode remains available.
- Lumbridge banking preserves pin/UIM rules and has an approved, non-overlapping
  location.
- Documentation records final decisions, tests, risks, and deferred follow-up.
