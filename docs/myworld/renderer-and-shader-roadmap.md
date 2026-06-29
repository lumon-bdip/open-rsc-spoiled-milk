# Renderer And Shader Roadmap

This is the current AI-facing roadmap for Spoiled Milk renderer-v2,
shader, lighting, shadow, and visual-performance work. It consolidates the
active state from `renderer-v2-plan.md` and
`remaster-lighting-and-shadow-plan.md`.

Use this file first when deciding what renderer work to do next. The older
documents remain detailed implementation ledgers and historical checklists.
For structural cleanup, file splitting, and stale-option quarantine work, use
`code-cleanup-and-modularization-plan.md`.

## Working Rule

This is an alpha remaster effort. Do not get tunnel vision on tiny optimizations
inside one renderer subsystem. Lock down each major architectural swing well
enough that visuals are acceptable and ownership is clear, then move to the
next structural problem.

Major renderer changes are allowed when they remove legacy limits, simplify
future work, improve frame stability, or make the OpenGL/remaster path a better
engine foundation.

## Current Baseline

The current accepted player-facing renderer baseline is:

- OpenGL primary window and input bridge.
- Renderer-v2 replacement composite for the world.
- Resident chunk OpenGL backend for terrain, walls, roofs, scenery, wall
  objects, and game objects.
- Legacy renderer still available as the data/parity source and fallback, but
  not the desired long-term rendering owner.
- `Classic`, `Remaster`, and `Custom` graphics presets.
- `Aspect Ratio` instead of a long resolution list.
- `Borderless` as a player-facing Graphics toggle, default-on for Classic and
  Remaster.
- Server-owned 60-minute day/night cycle.
- Directional/remaster lighting path with movable sun azimuth/elevation.
- Strengthened dawn/dusk/night tone filters driven by server time.
- Terrain shadow mask/decal proof promoted into the normal directional
  lighting path.
- F6 debug HUD with simple and expanded modes.

The renderer has reached playable alpha quality in many areas. The remaining
work is now less about proving OpenGL can draw Classic and more about removing
the remaining bridges that make performance, shaders, and shadows harder than
they need to be.

## Completed Foundation

- Direct framebuffer/presenter work is complete enough for the OpenGL-primary
  path to own presentation.
- OpenGL primary window, scaling, fullscreen/borderless behavior, mouse mapping,
  input bridge, and mouse-wheel zoom are live.
- Explicit texture/sprite transparency work is mostly complete for the current
  OpenGL atlas paths.
- Native OpenGL UI, glyph, primitive, minimap, and overlay replacement paths
  are partly in place and are good enough for current alpha use.
- Static world geometry is split into terrain, wall, roof, wall object, game
  object, and unclassified model kinds.
- Resident chunk mesh products and GPU buffers are the runtime default for
  static world rendering.
- Resident object chunks now own materialized scenery and wall-object models.
- The old projected static world path has been reduced to a bridge where still
  needed instead of being the main visual owner.
- Texture atlas ownership is shared between projected and resident renderers.
- Renderer telemetry covers OpenGL phases, resident chunks, batch culling,
  sprite overlay behavior, shadow mask cost, and rolling recent timings.
- Resident chunk spatial culling is promoted into the default renderer-v2
  runtime path. It subdivides static-world material batches so frustum/fog
  culling can skip smaller terrain, wall, roof, scenery, and wall-object work
  before draw submission. Expanded F6 and frame captures report whether the
  spatial path is active. Dense-area testing showed the original 6-tile
  spatial cells culled aggressively but produced very high draw-call counts, so
  the default cell size is now a coarser 12 tiles with a runtime override
  available for A/B testing.
- Resident chunk uploads are now frame-budgeted. The default budget is
  `3.0ms` per rendered frame via `spoiledmilk.openglWorldChunkUploadBudgetMs`
  / `SPOILED_MILK_OPENGL_WORLD_CHUNK_UPLOAD_BUDGET_MS`; `0` disables the cap
  for A/B testing. Reused chunks are always accepted immediately, at least one
  dirty chunk upload is allowed per frame, and remaining dirty chunks are
  deferred so section/teleport bursts do not land entirely on one frame. Upload
  budgeting is fallback-aware: if the projected static-world bridge is not
  available for the current frame, resident uploads are forced to complete
  rather than presenting black terrain/scenery while chunks are partially
  resident. If resident chunks are incomplete and fallback exists, the
  projected-world bridge remains the visual fallback until resident ownership
  catches up. Expanded F6 reports `req/up/reuse/defer/evict` plus upload budget
  used/limit.
- Expanded F6 now includes `world load avg/max/recent`, measuring
  `World.loadSections` directly, plus `world load phases
  reset/main/up/bridge/chunk/preload`. Use these to confirm whether zoomed-out
  walking hitches line up with active section/window rebuilds rather than
  steady-state draw cost, then target the dominant blocking phase instead of
  tuning steady renderer draw.
- In OpenGL-primary mode, the F6 `fps` value is observed/rendered FPS from
  completed OpenGL frame cadence, not the legacy client loop counter. This is
  the number testers expect because it reflects what they visually see when
  frames are dropped or GL render time exceeds the frame budget.
- OpenGL-primary presentation defaults to game-loop pacing instead of GLFW
  vsync (`spoiledmilk.openglVsync=false`). The client loop already targets
  60 Hz; letting both the game loop and swap interval throttle presentation can
  create phase drift where cheap frames are still presented at an uneven
  cadence. `spoiledmilk.openglVsync` / `SPOILED_MILK_OPENGL_VSYNC` remain
  available for A/B testing.
- Remaster terrain shadow masks are intentionally kept cheaper than the first
  proof pass. The default mask is 512x512 with a smaller blur and coarser
  light-angle buckets so server day/night movement does not trigger 1024x1024
  CPU mask rebuilds during normal play. Runtime overrides remain available:
  `spoiledmilk.remasterShadowMaskTextureSize`,
  `spoiledmilk.remasterShadowMaskBlurRadius`,
  `spoiledmilk.remasterShadowMaskAzimuthBucket`, and
  `spoiledmilk.remasterShadowMaskElevationBucket`.
- Terrain shadow masks are applied in the resident chunk shader when that path
  is active. The old immediate-mode shadow overlay remains only as a fallback;
  normal remaster rendering should not pay for a second terrain pass.
- Remaster brightness is shader-owned for resident chunks. Day/night/player
  brightness should not dirty static chunk buffers while directional remaster
  lighting is active.
- Runtime release/debug function-key clutter has been removed except for the
  renderer debug overlay controls.
- Resident chunk draw submission coalesces adjacent compatible material ranges
  after culling. F6 `batch c/d/cull` still reports logical material batches,
  while `submit calls/binds` reports the reduced GL draw submissions and
  texture binds.
- The shadow-mask cache key has been narrowed to shadow-owned inputs only.
  Broad resident chunk render signatures must not invalidate the shadow mask.
- Remaster shadow preparation now has a signature-first steady-state path.
  Roof/interior classification is cached by static world-chunk signature, and
  the shadow mask builder returns the existing mask immediately when the world
  signature plus quantized sun bucket are unchanged. This keeps normal frames
  from rebuilding caster lists or indoor/outdoor data just to rediscover a
  cache hit.
- The first renderer file-size refactor pass has split shader, texture atlas,
  resident chunk, projected mesh, frame capture, logging, material-label, and
  remaster shadow helper code out of `OpenGLFramePresenter`. It also split the
  package-local composite scene-command/world-sprite records and pure
  scene-command builder/classifier logic out of the presenter. Camera-space
  world-sprite quad submission now lives in `OpenGLWorldSpriteRenderer`, while
  world-sprite draw orchestration and command-sized sprite texture construction
  live in `OpenGLWorldSpriteDrawController` and `OpenGLSpriteTextureBuilder`.
  The presenter still owns window/input/pass orchestration and some bridge
  callbacks, but it should no longer be treated as the place to add low-level
  renderer systems.

## Current Shader State

Implemented or started:

- Projected world shader path exists.
- Resident chunk parity shader exists.
- Resident raw-material inspection shader exists and remains the clean-slate
  baseline for remaster lighting work.
- Remaster directional-light shader path exists for resident chunks.
- Resident chunk vertex data includes shader-ready material RGB, normals, and
  model kind.
- Terrain normals are smoothed enough for the current remaster terrain lighting
  target.
- Tone filters are shader uniforms and are driven by server-owned world time.

Still incomplete:

- Fixed-function/OpenGL color-array behavior still exists in important bridge
  paths.
- Some visual state, especially player brightness, is still baked into resident
  chunk or mesh signatures. Automatic day/night presentation must not animate
  baked brightness until brightness is fully shader-owned.
- Material behavior is not yet fully data-driven. Terrain, water, roofs, walls,
  foliage, ore, scenery, sprites, and effects still need explicit material
  metadata before polish shaders can be trusted.
- Shader comparison tooling is not yet strong enough to show legacy software,
  current OpenGL, and shader output for the same camera frame.
- Shader polish controls such as saturation, contrast, fog response, slope
  emphasis, or tile blending are not ready for release use.

## Current Lighting And Shadow State

Implemented or started:

- Server owns world time and syncs custom clients.
- `::settime MMSS` and `::advtime MMSS` exist for development testing.
- Remaster lighting converts server cycle time into sun azimuth, sun elevation,
  shadow direction, and shadow length.
- Dawn, dusk, cool night, and deep blue presentation tones are active.
- Dawn/dusk dimming is applied through tone RGB uniforms rather than animating
  the baked brightness multiplier.
- Semantic caster and receiver inventory exists for resident chunks.
- Roof coverage and roofless-interior classification exist as first-pass
  indoor/outdoor data.
- Terrain receives shadows from semantic wall/scenery caster proofs.
- Wall-boundary clipping exists for the world-space shadow mask.
- Terrain shadow masks are quantized by light buckets so normal day/night
  motion does not rebuild the mask every frame.
- Unchanged shadow frames reuse prior roof coverage, caster inventory, and mask
  results from signatures instead of walking the full shadow path every frame.

Still incomplete:

- Shadows are a terrain-receiver proof, not a full shadow engine.
- Diagonal wall shadows are intentionally skipped or simplified to avoid false
  triangular artifacts.
- Scenery shadows use broad heuristics, not per-asset shadow metadata.
- Shadows do not yet receive onto objects, walls, sprites, players, or NPCs.
- Terrain shadows currently appear static, or their movement is too subtle,
  when directional light/time changes. Treat this as the first shadow follow-up
  after the current renderer refactor is wrapped.
- Shadow movement is visibly stepped during `::advtime` because mask rebuilds
  are bucketed. This is acceptable for normal play but not a final high-quality
  shadow solution.
- The CPU-built 1024x1024 shadow mask is a proof. A GPU-side, incremental, or
  lower-cost path is the likely long-term direction.

## Biggest Remaining Swings

1. Shader ownership unification

   Move lighting, fog, tone, brightness, alpha, material sampling, and shadow
   terms into explicit shader-owned inputs. Stop baking visual settings into
   resident chunk geometry unless the setting actually changes geometry. This
   is the best next swing for making future lighting and polish predictable.

2. GPU-side or incremental shadows

   Replace or supplement the CPU-built terrain shadow mask with a cheaper
   approach. Candidate paths include a GPU mask/decal, sparse caster-local
   updates, lower-resolution masks with better filtering, or interpolation
   between cached masks. Keep semantic casters and indoor/outdoor classification
   as the data source.

3. True world streaming ownership

   Turn the current section-window caches and resident chunk products into an
   explicit world-stream manager. It should own decode, model-input products,
   GPU upload, predictive preload, dirty flags, roof inclusion, and ready-state
   telemetry. This is the biggest path toward stable high draw distance.

4. World-space entity and sprite renderer

   Move NPCs, players, ground items, projectiles, and effects away from
   screen-space legacy command replay and into world-space sprite/billboard
   submissions with explicit depth anchors, alpha rules, and batching.

5. Visibility and quality settings that really cull work

   Make fog/draw distance, roof visibility, entity distance, and quality
   presets affect what gets built, submitted, or drawn. A lower-quality setting
   should reduce terrain/scenery/entity work, not only change appearance.

6. Performance validation harness

   Add repeatable benchmark routes, frame dumps, frame diffing, and shader
   comparison captures. F6 is useful, but release-quality renderer work needs
   reproducible routes and automated regression checks.

7. Legacy renderer decommission plan

   Keep the legacy renderer while renderer-v2 is still an alpha remaster path,
   but continue shrinking bridge code. The long-term goal is explicit renderer
   data contracts, not permanent compatibility masks around old software
   renderer behavior.

## Long-Term Shader Goals

- More interesting scenery shadows without hand-authoring every object. The
  current broad scenery shadow proof is useful, but the long-term goal is to
  derive rough caster shape automatically from existing model data. Candidate
  approaches include projected model footprints, coarse top-down silhouette
  masks, convex hulls around visible/caster vertices, object bounding volumes,
  or material/model-kind heuristics that produce line/blob/canopy primitives
  without per-object code.
- Smoother shadow movement across terrain. The current terrain shadow mask uses
  quantized light buckets so normal play is affordable, but fast-forwarded time
  exposes stepped movement. Future options include GPU-side projected masks,
  interpolation between cached light buckets, temporal blending, lower-cost
  incremental updates, or a cheaper lower-resolution mask with better filtering.
- Better shader performance and cleaner shader ownership. Presentation-only
  state such as tone and day/night color should remain uniforms. Geometry or
  chunk rebuilds should happen only when geometry, material identity, or real
  visibility changes. Keep moving brightness, fog, material response, shadow
  contribution, and polish controls toward shader/material inputs rather than
  baked CPU vertex colors.
- Material-aware polish. Once parity is stable, shader work should understand
  terrain, water, walls, roofs, foliage, ore, scenery, sprites, projectiles, and
  effects as different material families instead of applying one generic rule to
  everything.
- Subtle material variation for broad flat areas. Large runs of identical
  terrain or scenery color can still read too flat even after relief lighting.
  A future remaster polish pass should test deterministic low-amplitude color
  variation or noise, keyed by tile/material position, so repeated same-texture
  areas gain visual breakup without changing original assets or flickering.

## Long-Term Renderer Goals

- Continue optimization until renderer-v2 reaches diminishing returns for the
  current alpha goals. The focus should remain on structural wins: culling real
  work, reducing bridge paths, stabilizing chunks, shrinking rebuilds, reducing
  draw calls/binds, and keeping high draw distance practical.
- Use the renderer as a capability unlock, not only as a faster Classic clone.
  Once the core renderer feels stable enough, explore what the old RuneScape
  Classic renderer made impractical:
  - camera tilt and richer camera controls without breaking billboard visuals
  - cleaner particle and spell effects
  - weather, ambient effects, fog variants, and post-process style polish
  - better projectile/effect anchoring in world space
  - richer debug/inspection views for map editing and content work
  - optional remaster presentation features that preserve Classic assets while
    taking advantage of depth, shaders, and modern GPU ownership
- Keep practical-use experiments separate from renderer foundation work. If a
  feature is mainly a showcase or gameplay-facing remaster addition, document
  it as a renderer capability experiment so optimization work does not get
  confused with content or feature design.

## Started Areas With Most Improvement Room

- Resident chunk backend: strong foundation, but still needs clearer dirty
  flags, async or incremental preparation, quality-aware build windows, and
  less dependence on baked visual settings. Spatial draw culling is now on by
  default, but true streaming ownership should eventually avoid preparing work
  that cannot contribute to the visible frame.
- Shader path: enough exists to prove direction, but material ownership and
  fixed-function retirement remain large wins.
- Terrain/scenery shadows: visually promising, but currently the most proof-like
  remaster system. It needs the static/subtle terrain-shadow movement issue
  rechecked, better metadata, cheaper rebuilds, and cleaner diagonal/interior
  handling before treating it as final.
- Entity sprites: visually much better than earlier alpha work, but still an
  important modernization target because sprites are central to Classic.
- Quality settings: presets exist, but settings do not yet consistently reduce
  underlying renderer work.
- Tooling: F6 telemetry is good for live diagnosis, but capture replay and
  benchmark automation would prevent repeated manual rediscovery.
- Code organization: the initial OpenGL presenter split and first code-level
  `LEGACY BRIDGE` / `RENDERER-V2 OWNER` labels are in place. Pure composite
  scene-command building, camera-space world-sprite quad submission,
  world-sprite draw orchestration, and command-sized sprite texture building
  now live outside the presenter. This is a good stopping point for the first
  refactor pass before returning to renderer optimization.

## Recommended Near-Term Order

1. Lock the day/night color and shadow presentation if current visual testing is
   accepted.
2. Stop time-of-day and other presentation-only settings from invalidating or
   rebuilding resident chunks.
3. Continue shader ownership cleanup: brightness, fog, tone, and material terms
   should become uniforms or material data rather than baked geometry state.
4. Smoke-test the refactor branch, migrate it back to the main worktree, and
   clean the worktree so optimization work starts from a stable baseline.
5. Validate the coarser default resident chunk spatial culling in dense areas.
   Compare F6 drawn/culled batch counts, draw calls, texture binds, triangle
   counts, process CPU, and FPS with fog on/off and multiple camera rotations.
   If it holds visually and improves or stabilizes frame time, use it as the
   bridge toward quality-aware world-stream culling.
6. Decide whether the next shadow swing is GPU/incremental masks or whether
   shadows should pause while renderer ownership and streaming mature.
7. Add benchmark/capture routes before another large optimization pass.

## Open Long-Term Questions

- How far should Remaster lighting go before it becomes a distinct gameplay
  feature instead of only a visual renderer feature?
- Should Classic mode keep a simplified clock while Remaster owns visual
  day/night transitions?
- How much automatic model-derived shadow shape can we get before explicit
  per-asset shadow metadata becomes necessary?
- What renderer capability experiments should become real gameplay or showcase
  features after the foundation reaches diminishing returns?
- What is the final supported draw distance at 4:3 and 16:9?
- Which low-quality settings should be exposed for weak hardware?
- How long should the legacy renderer remain packaged once renderer-v2 is the
  default?
