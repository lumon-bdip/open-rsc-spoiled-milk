# Renderer V2 Plan

This document tracks the alpha-era renderer rewrite effort for Spoiled Milk.
The working assumption is that this is the right time to make drastic
under-the-hood changes if they remove old engine limits, improve visual
stability, and make the client easier to work with.

## Baseline Decision

Do not treat integer, bilinear, and bicubic scaling as the main problem.
Those settings expose or soften instability that starts earlier in the render
pipeline.

The target is a renderer-v2 path that can run beside the legacy renderer until
it reaches acceptable parity. The new path should prioritize stable frame
timing, explicit transparency, deterministic ordering, and a depth-buffered
world renderer.

Treat renderer-v2 as an HD-remaster style engine effort, not a compatibility
wrapper. When old rendering constraints only exist because of applet-era,
software-era, or fixed-resolution assumptions, prefer promoting that area to a
new renderer-v2 design over permanently adapting the new path around the old
constraint. Legacy behavior can remain as a fallback or parity reference, but
it should not be the default design target when a cleaner engine boundary is
available.

## Goals

- Reduce sprite, object, wall, and texture flicker during normal camera motion.
- Remove see-through texture behavior caused by implicit color-key rules.
- Replace applet-era framebuffer plumbing with direct frame ownership.
- Separate game simulation, scene rendering, scaling, and presentation.
- Keep the existing server protocol, gameplay logic, cache data, maps, and
  content definitions usable.
- Preserve a legacy-renderer fallback until renderer-v2 is proven in alpha.
- Make future rendering work easier to reason about, test, and profile.

## Non-Goals

- Do not rewrite server gameplay as part of this project.
- Do not change game rules, combat behavior, or map data unless a renderer bug
  exposes bad data that must be fixed.
- Do not rely on blur as the primary visual-quality solution.
- Do not keep compatibility with applet-era rendering internals once the new
  path is stable.

## Current Problem Areas

- `PC_Client/src/orsc/ScaledWindow.java`
  - Performs scaling and presentation work in a Swing component.
  - Uses synchronous painting from the draw path.
  - Allocates or rebuilds scaling state during frame presentation.
  - Splits interpolation work across per-frame async image tasks in some modes.
- `PC_Client/src/orsc/ORSCApplet.java`
  - Uses `ImageProducer`, `ImageConsumer`, and intermediate image copies.
  - Bridges old applet rendering into the current desktop client.
- `Client_Base/src/orsc/graphics/two/GraphicsController.java`
  - Owns the main software pixel buffer.
  - Several sprite paths treat black or zero-like values as transparent.
- `Client_Base/src/orsc/graphics/three/Shader.java`
  - Several texture paths skip writes when the sampled texture value is zero.
  - This can make real black texture pixels behave as transparency.
- `Client_Base/src/orsc/graphics/three/Scene.java`
  - Uses old polygon sorting/painter behavior rather than a z-buffer.
  - Near-equal-depth faces and projected sprites can change draw order as the
    camera moves.
- `Client_Base/src/orsc/mudclient.java`
  - Mixes camera logic, world scene setup, entity sprite placement, overlays,
    and final draw orchestration in one large class.

## Target Architecture

Renderer-v2 should move toward these boundaries:

1. **Frame source**
   - Owns a direct framebuffer or render target.
   - Exposes a stable `int[]` or GPU texture target to the renderer.
   - Avoids applet-era `ImageProducer` handoff.

2. **World renderer**
   - Converts visible terrain, walls, models, objects, and projected sprites
     into draw commands.
   - Uses a depth buffer or a design that gives equivalent stable ordering.
   - Treats transparency as explicit metadata, not a magic RGB value.

3. **UI and overlay renderer**
   - Draws UI, chat, inventory, menus, combat overlays, and debug output after
     the world render.
   - Can remain software-rendered initially if that keeps the migration
     smaller.

4. **Scaler**
   - Scales the completed frame only after the frame is stable.
   - Provides selectable modes such as nearest, sharp filtered, bilinear, and
     bicubic.
   - Reuses destination buffers and avoids per-frame task allocation.

5. **Presenter**
   - Presents frames using a clear swap model.
   - Does not block the game render path on synchronous Swing painting.
   - Can be backed by Java2D, `VolatileImage`, `BufferStrategy`, or a GPU
     windowing path.
   - Starts with an opt-in `VolatileImage` GPU-presenter bridge before moving
     to a true OpenGL/LWJGL presenter.

## Preferred Technical Direction

Default to a z-buffered renderer-v2 implementation. If early geometry and
texture extraction proves clean enough, prefer a GPU-backed implementation over
another large software-only renderer. Hardware depth testing and texture
sampling would remove several classes of legacy rendering problems at once.

A modernized software renderer is still acceptable as an intermediate step if
it allows faster parity, but it should use the same renderer-v2 boundaries so a
GPU backend can replace it later.

## Current Alpha Rollout State

The current renderer-v2 rollout baseline is the OpenGL primary-window path with
the OpenGL static world replacement composite enabled. In this mode the client
still keeps the legacy renderer available as the data source and parity
reference, but the in-game world is no longer presented as a scaled copy of the
completed software framebuffer. The OpenGL path draws the captured static world
from renderer-v2 mesh data, then replays visible scene/entity sprites,
world-overlay sprites, and UI commands above it.

The first visually acceptable baseline is now:

- Direct framebuffer source with OpenGL primary presentation.
- Aspect-fit fullscreen/window scaling with selectable render-surface sizes.
- Captured terrain, walls, static objects, scenery, and wall objects promoted
  into typed renderer-v2 geometry.
- OpenGL texture atlas upload for legacy world textures, with padded atlas
  edges and texel-center UV sampling to prevent texture bleeding.
- Legacy draw-order preservation for the static-world color pass while the
  final depth-buffer path is still being proven.
- Legacy-derived terrain lighting restored in OpenGL: texture-backed faces use
  the old four-band texture shade curve, while flat resource-color terrain uses
  the old 256-step shade ramp from captured per-render-vertex light/fog
  values.
- Exact-ID scene/entity sprite restoration and software-visible sprite recovery
  used as the current bridge so NPCs, players, items, projectiles, and world
  overlays stay visually correct while full GPU sprite depth is still pending.
- Login/menu/loading frames remain on the uploaded software framebuffer until a
  captured 3D world mesh is ready, preventing stale or partial world geometry
  from appearing during load transitions.

This state is suitable as the first alpha rollout target because it fixes the
major terrain flatness/neon-color regression, keeps sprites readable, keeps UI
above the world, and leaves the legacy renderer available for comparison. It is
not the final architecture: the current world color pass still leans on fixed
function OpenGL state, CPU-built color arrays, legacy sorted order, and
software-visible sprite recovery.

## Shader Roadmap

The next large visual-quality step should be a shader-backed world renderer,
but it should be built in two deliberate stages.

1. **Parity shader**
   - Reproduce the current accepted look before adding new visual effects.
   - Move texture sampling, color-key alpha, flat resource-color shade ramps,
     four-band legacy texture shading, fog, and per-vertex light interpolation
     into GLSL.
   - Treat legacy binary terrain data as the source of truth through the
     generated terrain mesh, normals, tile splits, overlays, texture ids, and
     captured light/fog values.
   - Use explicit material metadata instead of relying on magic texture ids or
     first-texel behavior.
   - Keep a side-by-side software/OpenGL capture mode so parity can be checked
     by frame diff, not only by visual tuning.

2. **Polish shader**
   - Add optional improvements only after parity is stable.
   - Support configurable saturation/contrast and fog response through named
     renderer settings with conservative defaults.
   - Add higher-quality distance fade, optional dithered fog bands, and
     material-aware brightness correction for terrain, water, walls, roofs,
     scenery, and sprites.
   - Explore normal-derived terrain/slope emphasis for higher render-surface
     sizes, but keep it separate from the parity shader so it can be disabled
     if it changes the classic look too much.
   - Keep low-resolution art crisp by default; do not use blur as a substitute
     for correct geometry, ordering, alpha, or lighting.

Long term, the shader path should replace the fixed-function OpenGL arrays with
explicit VBO attributes for position, UV, material id, model kind, legacy
light/fog, alpha mode, and draw/depth metadata. That will make visual changes
auditable, let us add proper GPU sprite/material passes, and remove more of the
old renderer's applet-era assumptions without losing a known-good baseline.

## Visual Overhaul Backlog

These are worthwhile renderer-v2 expansions that remain open beyond the
current alpha baseline:

- **GPU sprite world pass**
  - Promote players, NPCs, ground items, projectiles, spell effects, and other
    billboard sprites into a real OpenGL world pass.
  - Preserve depth anchors, draw order, alpha, clipping, mirroring, and skewed
    sprite transforms without relying on software-visible pixel recovery.
  - Define transparent and semi-transparent sprite sorting rules before making
    this path source-of-truth.
- **Static world depth ownership**
  - Move terrain, walls, scenery, object models, roofs, bridges, water, and
    wall objects onto a coherent GPU depth model.
  - Replace legacy painter-order exceptions with deterministic depth and tie
    rules where possible, while documenting any intentional legacy matches.
- **Texture and material cleanup**
  - Convert legacy texture/palette transparency into explicit material
    metadata.
  - Preserve true black texture pixels without special black-to-1 behavior.
  - Add material categories for terrain, water, roof, wall, object, foliage,
    ore, and effect surfaces so shader work is data-driven.
- **Camera, zoom, and render-surface expansion**
  - Stress test larger render surfaces and mouse-wheel zoom limits now that the
    OpenGL world can draw a wider scene.
  - Record the first failure point for sprite placement, wall/object ordering,
    minimap clipping, UI panels, and projectiles before making expanded values
    normal options.
  - Audit camera rounding and section-load timing so increased draw distance
    does not reintroduce flicker or hitching.
- **Native UI and options cleanup**
  - Move the remaining UI primitives, text, minimap details, menus, and panels
    to native renderer-v2 drawing only after coverage is measurable.
  - Redesign the options menu so normal players see renderer/display settings
    rather than alpha debug flags.
  - Keep debug toggles available for testing, but separate them from shippable
    quality settings.
- **Scaler and presentation polish**
  - Add a sharp pixel-art-aware scaling mode after the renderer output itself
    is stable.
  - Verify integer, bilinear, bicubic, sharp filtered, and fullscreen modes
    against the same baseline routes.
  - Keep world clarity and UI readability as separate concerns if high
    resolutions make a single scale policy insufficient.
- **Validation tooling**
  - Add scripted camera routes and frame capture/diff tooling for known
    problem areas.
  - Track terrain-light parity, sprite depth, see-through black pixels,
    minimap clipping, section-load hitches, and scaling artifacts as distinct
    renderer-v2 bug classes.
  - Require visual checkpoints before promoting renderer-v2 from opt-in alpha
    to default.

## Current Opt-In Renderer Flags

These flags are intentionally separate so alpha testers can isolate each
renderer-v2 layer:

- `SPOILED_MILK_RENDERER_TELEMETRY=true`
  - Enables frame, scene, presentation, scaler, Java2D GPU presenter, and
    OpenGL presenter timing logs.
- `SPOILED_MILK_DIRECT_FRAMEBUFFER=true`
  - Bypasses the applet-era `ImageProducer` frame path and wraps the software
    framebuffer directly.
- `SPOILED_MILK_GPU_PRESENTER=true`
  - Uses the Java2D `VolatileImage` presenter bridge inside the Swing viewport.
- `SPOILED_MILK_OPENGL_PRESENTER=true`
  - Starts the LWJGL/OpenGL presenter probe. By itself this opens a separate
    mirror window and uploads the software framebuffer as a texture.
- `SPOILED_MILK_OPENGL_INPUT=true`
  - Enables the experimental GLFW input bridge for the OpenGL mirror. It polls
    cursor position and mouse buttons, receives native key, text, focus, and
    mouse wheel callbacks, then forwards AWT events into the existing applet
    input handlers.
- `SPOILED_MILK_OPENGL_PRIMARY_WINDOW=true`
  - Hides the Swing client window and uses the OpenGL window as the only
    visible client window. This implies the OpenGL input bridge and should be
    used for field testing so telemetry reflects a single visible client.
- `SPOILED_MILK_RENDER_SURFACE_MODE=512x346|640x480|800x600|960x540|1024x576|1280x720`
  - Selects the actual source framebuffer size. This changes the scene
    projection and visible world area before final scaling.
- `SPOILED_MILK_OPENGL_SCALE_MODE=aspect-fit|integer-fit|stretch`
  - Legacy/debug fit-policy override for non-primary OpenGL mirror testing.
    OpenGL-primary ignores this setting and always uses automatic aspect-fit
    presentation with black bars when the window and selected resolution do not
    match.
- `SPOILED_MILK_OPENGL_WINDOW_MODE=windowed|borderless-fullscreen`
  - Selects the OpenGL primary-window mode. `borderless-fullscreen` uses the
    primary monitor's current desktop mode without changing monitor resolution.
- `SPOILED_MILK_OPENGL_UI_FONT=legacy|h11p|h12p|h13b|h14b`
  - Selects the OpenGL-primary UI body-font policy. `legacy` keeps the old
    `h12b.jf` body font. The other modes remap legacy body text to the named
    bundled face so alpha testing can compare readability without a relaunch.
- `SPOILED_MILK_OPENGL_SPRITE_OVERLAY=true`
  - Enables the first atlas-backed 2D sprite overlay probe. The software
    framebuffer still renders normally; simple full-sprite draw calls are
    captured and replayed from the OpenGL atlas on top for validation.
- `SPOILED_MILK_OPENGL_SPRITE_OVERLAY_MODE=safe|visible|phased|native-ui|geometry`
  - Selects the sprite overlay replay policy. `safe` skips commands that need
    original draw order, `visible` visibility-filters every replayed sprite
    against the completed software frame, `phased` replays UI-phase commands
    directly while visibility-filtering scene/world commands, `native-ui`
    additionally omits captured full-opacity UI sprites from the software
    framebuffer so OpenGL becomes their source of truth, and `geometry` forces
    raw commands for debugging and can draw world/entity sprites above later
    software pixels.
- `SPOILED_MILK_OPENGL_UI_BASE_FRAME=true`
  - Diagnostic mode for `native-ui`. When enabled, OpenGL may present the
    pre-UI scene/world framebuffer and then draw captured UI commands on top,
    but only on frames where the command capture marks the native UI base as
    eligible. Unsupported post-base UI writes still fall back to the complete
    software framebuffer.
- `SPOILED_MILK_OPENGL_NATIVE_UI_REPLACE=true`
  - Experimental destructive mode for `native-ui`. Without this flag,
    `native-ui` is capture/telemetry-only and the complete software framebuffer
    remains the source of truth for NPC sprites, menus, messages, and minimap.
- `SPOILED_MILK_RENDERER_DEBUG_OVERLAY=true`
  - Enables the compact renderer-v2 overlay inside the presented framebuffer.
- `SPOILED_MILK_RENDERER3D_GEOMETRY_CAPTURE=true`
  - Captures visible legacy world model faces into a renderer-v2 geometry
    frame after frustum/orientation checks. This is a diagnostic bridge for
    the depth-buffered and GPU world renderer work; it does not change the
    displayed frame by itself.
- `SPOILED_MILK_RENDERER3D_VISIBLE_WORLD=true`
  - Draws the captured static world through the software diagnostic depth
    buffer before overlays and UI. This is an early visible renderer-v2 world
    prototype, flat-colored for validation before the OpenGL texture/shader
    backend replaces it.
- `SPOILED_MILK_OPENGL_WORLD_MESH=true`
  - Uploads the captured renderer-v2 world mesh through OpenGL VBO/EBO buffers
    and reports GPU-upload counts without changing the visible frame.
- `SPOILED_MILK_OPENGL_WORLD_MESH_VISIBLE=true`
  - Also draws the uploaded mesh over the frame with a screen-space depth test.
    This is intentionally diagnostic because un-clipped legacy triangles can
    cover unrelated screen regions until the GPU world path owns clipping and
    projection.
- `SPOILED_MILK_OPENGL_WORLD_TEXTURED_VISIBLE=true`
  - Draws texture-backed renderer-v2 terrain triangles from the OpenGL world
    texture atlas using atlas-space mesh UVs. It defaults to an opaque
    transparent-key cutout pass for parity testing, and can still be dialed
    down into a translucent comparison overlay through the alpha flag below.
- `SPOILED_MILK_OPENGL_WORLD_TEXTURED_STATIC_VISIBLE=true`
  - Also includes walls, roofs, scenery, static objects, and wall objects in
    the textured diagnostic. This remains intentionally separate while the
    world-sprite and alpha-ordering paths are still diagnostic.
- `SPOILED_MILK_OPENGL_WORLD_STATIC_TEXTURES=true|false`
  - Controls whether walls, roofs, scenery, static objects, and wall objects
    sample the OpenGL world texture atlas. It defaults on with the static
    textured world path; set it false to force the flat average-material
    fallback while debugging UV/order issues.
- `SPOILED_MILK_OPENGL_WORLD_TEXTURED_ALPHA=0.0..1.0`
  - Controls the visible textured OpenGL world diagnostic opacity. It defaults
    to `1.0` for parity testing; use `0.55` to restore the older translucent
    comparison overlay.
- `SPOILED_MILK_OPENGL_WORLD_REPLACEMENT_COMPOSITE=true|false`
  - Lets the visible textured/static OpenGL world replace the old full-frame
    software base for in-game frames, then replays anchored scene/entity
    sprites, world-overlay visible sprites, and captured UI commands over that
    world. It defaults on when the textured static world path is active,
    because drawing the GPU world over the final software frame cannot produce
    stable ordering. Login/menu/loading frames without captured 3D world mesh
    keep using the uploaded software framebuffer and do not run replacement
    replay.
- `SPOILED_MILK_OPENGL_WORLD_SPRITES_VISIBLE=true`
  - Draws matched scene/entity sprites from captured renderer-v2 depth anchors
    through the OpenGL sprite atlas as a translucent depth-tested diagnostic.
    This requires a visible OpenGL world mesh pass so sprites compare against a
    fresh depth buffer. It enables the 2D command capture stream needed to map
    anchors back to concrete sprite assets without enabling the older
    after-frame sprite overlay replay.

When the OpenGL-primary path is active, the in-game general options panel
exposes two player-facing renderer rows: `Resolution` and `Font`. The selected
resolution changes the actual source framebuffer and visible world area;
presentation into the window uses automatic aspect-fit bars. The selected font
cycles the OpenGL-primary body-font candidates for readability testing: legacy
`h12b.jf`, `h11p.jf`, `h12p.jf`, `h13b.jf`, and `h14b.jf`. `F8` still cycles
resolution. `F6` still toggles the renderer debug overlay and `F7` still cycles
windowed/borderless mode as alpha debug shortcuts, but those controls are no
longer normal options-menu rows. The legacy software-presenter scaling controls
and integer/bilinear/bicubic labels remain available only outside
OpenGL-primary fallback presentation.

Before using the OpenGL presenter locally, run:

```bash
./scripts/download-lwjgl.sh
./scripts/build-client.sh
```

The download script pins LWJGL to `3.3.4` by default and stores local jars under
`PC_Client/lib/lwjgl/`. The Ant build includes nested library jars when they
are present, while normal non-OpenGL builds still compile without LWJGL.

The current alpha renderer-v2 visual baseline uses these runtime flags:

```bash
SPOILED_MILK_DIRECT_FRAMEBUFFER=true
SPOILED_MILK_OPENGL_PRESENTER=true
SPOILED_MILK_OPENGL_INPUT=true
SPOILED_MILK_OPENGL_PRIMARY_WINDOW=true
SPOILED_MILK_RENDERER3D_GEOMETRY_CAPTURE=true
SPOILED_MILK_OPENGL_WORLD_MESH=true
SPOILED_MILK_OPENGL_WORLD_TEXTURED_VISIBLE=true
SPOILED_MILK_OPENGL_WORLD_TEXTURED_STATIC_VISIBLE=true
SPOILED_MILK_OPENGL_WORLD_STATIC_TEXTURES=true
SPOILED_MILK_OPENGL_WORLD_TEXTURED_ALPHA=1.0
SPOILED_MILK_OPENGL_WORLD_SPRITES_VISIBLE=true
```

Telemetry and the debug overlay remain recommended for local validation, but
they are not visual requirements for the baseline.

## Migration Checklist

### Phase 0: Baseline Capture

- [ ] Pick three to five known bad visual-acuity locations.
- [ ] Record exact coordinates, camera angle, zoom, roof mode, and scaling mode.
- [ ] Capture short reference clips or frame sequences for each location.
- [x] Add frame timing around scene render, framebuffer commit, scaling, and
      presentation.
- [ ] Add counters for world section load, texture decode, sprite draw count,
      polygon count, and frame allocation count.
- [ ] Define pass/fail examples for flicker, see-through textures, camera
      hitching, and unacceptable blur.

### Phase 1: Direct Framebuffer and Presentation

- [x] Introduce an opt-in direct framebuffer path for the desktop client.
- [x] Back the legacy software renderer with a direct `BufferedImage` or
      equivalent render target.
- [x] Remove `ImageProducer` from the active renderer-v2 frame path.
- [x] Remove unnecessary copy steps between software pixel data and the final
      displayed frame for the opt-in renderer-v2 path.
- [x] Replace synchronous `paintImmediately` frame delivery with a stable
      present/swap model.
- [x] Reuse presentation source buffers across frames.
- [x] Reuse smooth-scaling destination buffers across frames.
- [ ] Keep the legacy renderer selectable for comparison.

### Phase 1A: GPU Presenter Bridge

- [x] Add an opt-in Java2D `VolatileImage` presenter path.
- [x] Track GPU-presenter timing separately from software smooth scaling.
- [x] Add recursive client library packaging for optional renderer backends.
- [x] Add an LWJGL dependency bootstrap script for local OpenGL validation.
- [x] Add an opt-in OpenGL/LWJGL framebuffer mirror that uploads the software
      framebuffer as a texture.
- [x] Track OpenGL snapshot, upload, render, and dropped-frame telemetry.
- [x] Reuse pooled direct OpenGL frame buffers instead of allocating a heap
      pixel snapshot every frame.
- [x] Add an opt-in GLFW input bridge for basic keyboard, cursor, and mouse
      button forwarding into the legacy applet handlers.
- [x] Add an opt-in OpenGL-primary launch mode that hides the Swing client
      window while keeping the legacy frame source internally available.
- [x] Move fullscreen scaling policy into the OpenGL presenter with
      aspect-fit, integer-fit, and debug stretch modes.
- [x] Lock OpenGL-primary presentation to automatic aspect-fit bars so
      resolution changes alter the source framebuffer instead of exposing
      stretch/scaler choices to players.
- [x] Remap OpenGL mouse coordinates through the actual drawn viewport instead
      of the whole window surface.
- [x] Add in-game options and `F8` cycling for actual render-surface size
      modes.
- [x] Retire OpenGL fit from the player-facing options menu; keep the old fit
      override only for non-primary mirror/debug testing.
- [x] Add in-game options and `F7` cycling for windowed versus
      borderless-fullscreen OpenGL primary mode.
- [x] Add in-game options and `F6` cycling for a renderer-v2 debug overlay.
- [x] Forward OpenGL mouse wheel scroll into the existing zoom and scroll
      handlers.
- [x] Skip hidden Swing viewport buffering when OpenGL-primary mode owns
      presentation.
- [ ] Field-test GPU presenter across integer, bilinear, and bicubic modes.
- [ ] Decide whether the Java2D GPU presenter is worth keeping after a true
      OpenGL presenter exists.
- [x] Promote the OpenGL presenter from mirror/probe mode to the primary client
      window with GLFW input, focus, resize, and shutdown handling.
- [x] Replace remaining polled OpenGL input with callback-backed key, text, and
      focus events.
- [ ] Add release packaging rules for non-Linux LWJGL native classifiers.

### Phase 2: Explicit Transparency

- [x] Inventory every sprite, texture, and shader path that treats `0`, black,
      or `Scene.TRANSPARENT` as transparency.
- [x] Decide a single renderer-v2 transparency model.
- [x] Add asset metadata or conversion rules for opaque black versus masked
      pixels.
- [x] Add shared sprite-to-RGBA texture upload data for the OpenGL atlas path.
- [x] Route legacy world texture palette normalization and shader sample
      visibility through named transparency rules.
- [x] Route primary sprite mask checks through named transparency rules.
- [x] Add lazy OpenGL sprite atlas/cache scaffolding with padded alpha texture
      regions for the GPU path.
- [x] Add an opt-in atlas-backed OpenGL 2D sprite overlay probe for simple,
      unskewed sprite draws.
- [x] Add sprite source-rectangle commands so clipped simple sprite draws can
      replay from atlas UVs instead of cropped temporary textures.
- [x] Replay shifted and clipped scaled sprite draws from fixed-point source
      spans in the OpenGL overlay probe.
- [x] Keep semi-transparent sprite replay out of overlay probe mode to avoid
      double-blending over the already-rendered software framebuffer.
- [x] Add atlas texture variants for full-opacity legacy sprite mask and
      recolor commands, covering non-skewed, non-mirrored masked
      `drawSpriteClipping` calls without shader-side compatibility hacks.
- [x] Add command and OpenGL quad geometry support for full-opacity mirrored
      and top-skewed masked sprites.
- [x] Add sprite overlay replay-safety modes so after-frame overlay skips
      order-sensitive world/entity sprite commands by default.
- [x] Add an opt-in visible-pixel after-frame replay mode for order-sensitive
      sprite commands, using the completed software frame as the occlusion
      authority.
- [x] Add sprite overlay telemetry for captured, static-replayed,
      visible-replayed, skipped-order, skipped-invisible, dynamic-atlas-full,
      and visible-pixel counts, and show the summary in the renderer debug
      overlay.
- [x] Add per-phase replay telemetry for direct and visibility-filtered
      OpenGL sprite commands so phase-aware coverage is measurable in-game.
- [x] Add renderer-v2 capture rejection telemetry so alpha, bounds, source,
      transform, interlace, overflow, and invalid sprite blockers are visible
      before native UI replacement work.
- [x] Add renderer-v2 2D command phases for scene, world overlay, and UI
      overlay sprite captures, with phase bucket telemetry in the OpenGL
      presenter and debug overlay.
- [x] Add a phase-aware OpenGL sprite replay mode that draws UI-phase sprite
      commands directly while keeping scene/world commands visibility-filtered.
- [x] Add an opt-in native UI sprite replacement mode for captured
      full-opacity UI sprite draws, leaving alpha and masked/skewed UI paths on
      software until their native equivalents are exact.
- [x] Capture a pre-UI renderer-v2 base framebuffer at the world-to-UI phase
      boundary and route it to the OpenGL presenter behind a diagnostic flag.
- [x] Capture the legacy `spriteClipping` scaled-sprite path in the
      renderer-v2 command stream for full-opacity, replay-safe draws.
- [x] Replace replay-safe masked UI sprite draws in `native-ui` mode while
      keeping mirrored or skewed masked sprites on software until ordered
      native replay exists.
- [x] Add renderer-v2 text draw and glyph telemetry by phase to size the
      native OpenGL font/glyph pass before replacing software UI text.
- [x] Add an OpenGL glyph atlas and native replacement for replay-safe UI text
      draws in `native-ui` mode, preserving software fallback for clipped or
      antialiased text.
- [x] Add native OpenGL UI primitive commands for solid/alpha rectangles and
      lines, replayed only when pre-UI base-frame eligibility proves the full
      post-base UI stack is native-covered.
- [x] Capture UI `setPixel` calls as 1x1 native primitives so minimap/entity
      dots do not block eligible pre-UI base frames.
- [x] Add native UI base blocker reason telemetry so unsupported post-base UI
      writes identify their source instead of only clearing base eligibility.
- [x] Add native rotated minimap sprite commands, including an opaque texture
      variant for the main minimap where legacy color `0` is real black.
- [x] Carry the current renderer clip rectangle into native rotated minimap
      replay and enforce it with OpenGL scissor so rotated map pixels stay
      inside the minimap content square.
- [x] Capture UI `drawCircle` calls as native scanline circle commands so
      minimap dots and interface circles no longer block eligible pre-UI base
      frames.
- [ ] Replace remaining low-level sprite/glyph mask checks with explicit
      metadata where they are true sprite transparency rather than font masks.
- [ ] Update world texture sampling so true black can render as true black in
      renderer-v2 without relying on legacy black-to-1 substitution.
- [ ] Wire GPU sprite/UI draw calls to the OpenGL atlas and use alpha instead
      of color-key checks in those draw calls.
- [ ] Enable mirrored/top-skewed world sprite replay by default only after
      renderer-v2 can preserve original draw order instead of drawing captured
      sprites over the completed software frame.
- [ ] Expand the sprite overlay probe to alpha-dependent sprite draws and
      minimap-rotated draw paths.
- [ ] Add visual tests for black texture pixels, sprite masks, and UI icons.
- [ ] Verify that fixes do not break intentional transparent sprites.

### Phase 3: Renderer-V2 Boundary

- [ ] Add a renderer selection setting or launch flag.
- [ ] Create a renderer interface that can accept camera, scene, entity, and UI
      state from the current client.
- [ ] Extract world draw input from `mudclient` without changing gameplay
      behavior.
- [x] Mark the current software draw stream with renderer-v2 scene, world
      overlay, and UI overlay phases so native OpenGL passes can be enabled
      incrementally without guessing draw intent.
- [ ] Keep UI overlays on the old software path initially if needed.
- [ ] Add side-by-side debug output for legacy versus renderer-v2 frame timing.
- [x] Log OpenGL vendor, renderer, and version for support diagnostics.
- [x] Clean up OpenGL texture resources on presenter shutdown.
- [ ] Document every legacy render behavior that renderer-v2 intentionally
      drops.

### Phase 4: Depth-Buffered World Prototype

- [x] Normalize the known bad `tinrock1` model data at the load boundary so
      its legacy red back faces are hidden like the other ore rock models.
- [x] Add an opt-in renderer-v2 geometry frame that captures visible world
      model faces from `Scene.endScene()` without changing the legacy output.
- [x] Expose captured world model and face counts through renderer telemetry
      and the F6 debug overlay.
- [x] Classify captured world geometry as terrain, wall, roof, game object,
      wall object, or unclassified so the depth-buffer prototype can enable
      each category deliberately.
- [x] Render captured terrain, wall, game-object, and wall-object faces into
      an offscreen diagnostic depth buffer, with face, triangle, and
      pixel-write counts in telemetry.
- [x] Promote the diagnostic terrain/wall/object depth buffer into an opt-in
      visible renderer-v2 world backend.
- [x] Render static scenery and object models into the diagnostic depth buffer.
- [x] Render static scenery and object models visibly with depth testing in the
      flat-colored software prototype.
- [ ] Render player, NPC, item, projectile, and effect sprites with stable
      depth behavior.
- [ ] Define alpha rules for partially transparent sprites and effects.
- [ ] Replace near-depth painter tie instability with deterministic behavior.
- [ ] Compare frame captures against the legacy renderer for missing geometry.
- [ ] Stress test high draw distance, crowded entity scenes, roofs, fog, and
      underground areas.

### Phase 4A: OpenGL World Backend

- [ ] Keep the depth-buffered world prototype behind an opt-in renderer-v2
      setting until parity is proven.
- [x] Extract visible legacy world model faces into explicit renderer-v2
      geometry commands for diagnostics and backend prototyping.
- [x] Split captured terrain, wall, roof, scenery, and static-object geometry
      into explicit typed renderer-v2 draw commands instead of depending on
      legacy model identity and painter order.
- [x] Map typed renderer-v2 geometry commands into a CPU-side GPU-ready
      vertex/index stream.
- [x] Upload captured world geometry through an opt-in OpenGL VBO/EBO
      diagnostic pass.
- [x] Isolate OpenGL upload, framebuffer, world, and sprite passes with
      explicit state guards so experimental passes cannot leak texture, depth,
      client-array, or scissor state into presentation.
- [x] Center projected renderer-v2 mesh coordinates for OpenGL and gate the
      visible diagnostic pass to positive-depth, viewport-intersecting,
      conservatively bounded triangles.
- [x] Invert uploaded OpenGL world mesh Z so smaller positive legacy camera
      depth wins the GPU depth test like the software depth prototype.
- [x] Draw the visible OpenGL world mesh diagnostic as a translucent
      depth-tested wireframe overlay so unfinished flat mesh colors cannot
      cover software sprites or replace textured scenery.
- [x] Carry per-triangle texture id, fallback color, model kind, source model,
      face id, and average depth through the renderer-v2 mesh for atlas and
      shader work.
- [x] Capture the final legacy sorted draw order for visible static world
      faces and carry it through renderer-v2 triangle metadata.
- [x] Capture the final near-plane clipped camera/projected vertices for
      visible static world faces and use them for renderer-v2 mesh generation.
- [x] Preserve captured legacy draw order for textured static OpenGL
      diagnostics by writing index runs in sorted face order instead of one
      global batch per texture, and keep the textured color pass free of GPU
      depth rejection until the depth path can match legacy terrain/static
      ordering.
- [x] Capture projected legacy sprite depth anchors from the final sorted draw
      loop, including sprite id, pick id, camera depth, screen rect, and
      scene-wide legacy draw order.
- [x] Capture legacy world texture palette/index data as immutable renderer-v2
      texture assets and attach the texture catalog to geometry and mesh
      frames.
- [x] Derive normalized face-basis texture coordinates from captured
      camera-space geometry and carry them in the renderer-v2 mesh vertex
      stream for future atlas sampling.
- [x] Upload referenced renderer-v2 world textures into an OpenGL atlas cache
      and report cache/upload/missing coverage without changing visible world
      rendering.
- [x] Upload world geometry and textures through OpenGL buffers and atlases.
- [x] Add an opt-in textured OpenGL world diagnostic that draws texture-backed
      terrain triangles from atlas-space UVs through texture-grouped index
      batches.
- [x] Extend the textured diagnostic to static geometry now that sprite depth
      anchors are available as renderer-v2 metadata.
- [x] Build an opt-in OpenGL world-sprite diagnostic batch from captured depth
      anchors, preserving legacy scene order until depth-tested alpha rules are
      proven.
- [x] Replay captured UI-phase commands over the visible OpenGL world pass so
      menus, message boxes, and text remain above terrain/static geometry while
      the software UI is still the source of truth.
- [x] Restore software-visible scene/world sprite pixels over the visible
      OpenGL world pass so NPCs, players, item sprites, projectiles, and world
      overlays are not buried while the GPU depth sprite path is still being
      built.
- [x] Render flat-colored static world triangles in the same legacy-ordered
      OpenGL draw stream as texture-backed triangles so the GPU world can stand
      on its own without borrowing the old completed software framebuffer.
- [x] Add an OpenGL world replacement composite mode that skips the old
      full-frame software base on in-game frames, draws the OpenGL static world
      first, and then replays anchored scene/entity sprites, world-overlay
      visible sprites, and captured UI commands above it.
- [x] Pad OpenGL texture-atlas uploads with duplicated border texels and sample
      atlas regions at texel centers so exact-edge UVs cannot bleed into
      unrelated packed textures during camera movement.
- [x] Add a conservative projected-triangle sanity guard to the OpenGL static
      world color pass so malformed or extreme near-plane triangles cannot
      paint large unrelated screen regions.
- [x] Capture legacy per-render-vertex light/fog intensity and use it as
      OpenGL texture modulation so textured terrain is no longer uniformly
      bright while the full shader path is still being built.
- [x] Apply the captured legacy light/fog intensity to OpenGL flat resource
      colors using the old 256-step shade ramp, so ground elevation and
      terrain-resource colors are no longer drawn full-bright and flat.
- [x] Add a flat material-color fallback for texture-backed walls, scenery,
      objects, and wall objects while their static UV/shader path is proven.
- [x] Use average opaque texture color as the flat material fallback for
      texture-backed static geometry so transparent or black first texels do
      not turn trees/scenery black when static texture sampling is disabled.
- [x] Add a dedicated `SPOILED_MILK_OPENGL_WORLD_STATIC_TEXTURES` switch so
      static texture detail can be reintroduced without removing the fallback.
- [x] Route exact-ID replacement-composite scene/entity sprites through captured
      3D sprite anchors, and restore missing-ID scene/world sprites from
      software-visible pixels so composite humanoid sprites are not guessed by
      rectangle alone.
- [x] Gate replacement-composite UI/world-overlay replay to frames with captured
      3D world mesh so login/menu/loading sprites cannot be replayed over an
      already-complete software framebuffer.
- [x] Gate renderer-v2 3D frame handoff until the initial region load completes,
      preventing the post-login loading interval from presenting partial or
      stale room-like world geometry.
- [ ] Promote the OpenGL world-sprite batch from translucent diagnostic overlay
      to source-of-truth rendering after alpha, clipping, and replacement rules
      are stable.
- [ ] Use the GPU depth buffer for terrain, walls, scenery, and object models.
- [ ] Move world texture sampling and fog/light application into a shader path
      with explicit transparency rules.
- [ ] Add a side-by-side capture mode for software world output versus the GPU
      world backend.
- [ ] Promote player, NPC, item, projectile, and effect sprites into the GPU
      world pass only after depth anchors and alpha ordering are stable.

### Phase 4B: Shader-Backed Visual Fidelity

- [ ] Add a GLSL parity shader for static world geometry that reproduces the
      current accepted OpenGL look before adding new effects.
- [ ] Move flat resource-color lighting into the shader using the legacy
      256-step shade ramp and captured per-render-vertex light/fog values.
- [ ] Move texture-backed lighting into the shader using the legacy four-band
      texture shade curve or a prebuilt equivalent atlas/LUT.
- [ ] Move fog application into shader code with the same captured fog values
      used by the current renderer-v2 mesh.
- [ ] Replace fixed-function OpenGL color arrays with explicit shader
      attributes for position, UV, material id, model kind, alpha mode, and
      legacy light/fog.
- [ ] Add material metadata for terrain, water, walls, roofs, foliage, ore,
      objects, sprites, and effects so shader behavior is data-driven.
- [ ] Add a frame-capture comparison mode that can show legacy software world,
      current OpenGL world, and shader world output for the same camera frame.
- [ ] Add conservative shader polish controls for saturation, contrast, fog
      strength, and slope emphasis, defaulting to parity values.
- [ ] Keep shader polish optional until it has been tested against towns,
      mines, forests, water, interiors, roofs, underground areas, and crowded
      entity scenes.

### Phase 5: Camera and World-Load Stability

- [ ] Audit camera smoothing and coordinate rounding.
- [ ] Ensure camera updates produce stable projected positions frame to frame.
- [ ] Separate world section preload and render presentation timing.
- [ ] Detect and log frame stalls during section transitions.
- [ ] Avoid render-thread blocking on asset or sector loads where possible.
- [ ] Verify that increased draw distance does not reintroduce flicker through
      unstable ordering or late-loaded assets.

### Phase 6: Scaler Rebuild

- [ ] Implement nearest/integer scaling using stable source and destination
      rectangles.
- [x] Implement bilinear and bicubic scaling without per-frame worker creation.
- [ ] Add a sharp filtered mode for low-resolution art.
- [ ] Consider Scale2x, hq2x, xBRZ, or a similar pixel-art-aware option.
- [ ] Separate world scaling and UI scaling if readability requires it.
- [ ] Ensure resize behavior does not rebuild heavyweight renderer state every
      frame.
- [ ] Verify text, menus, inventory, and combat overlays at common window sizes.

### Phase 7: Performance and Tooling

- [ ] Add a debug overlay for frame time, render time, present time, polygon
      count, sprite count, and active scaling mode.
- [ ] Add a command-line or config option to dump frame timing to a log.
- [ ] Add automated frame-diff tooling for the baseline acuity locations.
- [ ] Track allocation rate during camera movement and section loading.
- [ ] Establish minimum target hardware for alpha testing.
- [ ] Compare legacy renderer and renderer-v2 on the same route.

### Phase 8: Options and Quality Settings Cleanup

- [ ] Redesign the options menu into user-friendly renderer, display, audio,
      controls, and gameplay groups instead of exposing raw debug rows.
- [x] Collapse the OpenGL-primary player-facing render options to
      `Resolution` and `Font` rows.
- [x] Separate alpha/debug-only renderer switches from settings intended for
      normal players.
- [ ] Add user-friendly display labels around resolution and future quality
      settings after the old options menu is redesigned.
- [ ] Replace integer/bilinear/bicubic terminology with OpenGL-native quality
      controls only if field testing shows a player-facing filter option is
      still needed.
- [ ] Expand supported render-surface sizes in measured steps and record where
      sprites, UI panels, minimap, or world projection start to break.
- [ ] Stress test mouse-wheel zoom beyond the old limits and document the
      first failure point for sprites, walls, object ordering, and UI overlays.
- [ ] Decide which extreme resolution and zoom settings are shippable, which
      remain debug-only, and which should be blocked.

### Phase 9: Alpha Rollout

- [x] Establish the terrain-lit OpenGL replacement composite as the first
      alpha rollout baseline.
- [ ] Ship renderer-v2 behind an opt-in setting first.
- [ ] Ask alpha testers to run specific camera routes and crowded scenes.
- [ ] Collect screenshots or clips for regressions.
- [ ] Track bugs separately as renderer-v2 parity, visual-quality, or
      performance issues.
- [ ] Promote renderer-v2 to default only after the legacy path is no longer
      clearly better in any common play scenario.
- [ ] Keep the legacy renderer for one additional alpha cycle after promotion.

### Phase 10: Legacy Decommission

- [ ] Remove unused applet-era frame plumbing from the desktop client.
- [ ] Remove legacy scaler modes that only exist to compensate for old render
      instability.
- [ ] Delete dead compatibility branches after the fallback window closes.
- [ ] Update client settings, docs, and release notes to describe the new
      renderer behavior.
- [ ] Keep enough migration notes to explain old asset transparency decisions.

## Validation Checklist

- [ ] No see-through black pixels in opaque terrain textures.
- [ ] No red back-face flash when walking past `tinrock1` ore rocks.
- [ ] No rapid object, wall, or floor face flicker during slow camera rotation.
- [ ] No player/NPC/item sprite depth popping in crowded scenes.
- [ ] No major frame hitch when crossing active section boundaries.
- [ ] Integer or nearest scaling is crisp without excessive shimmer.
- [ ] Smooth scaling modes reduce shimmer without unacceptable blur.
- [ ] UI remains readable at supported desktop sizes.
- [ ] Draw distance remains at least as good as the current alpha client.
- [ ] High render-surface sizes and extended mouse-wheel zoom do not break
      sprite placement or world/UI clipping.
- [ ] Renderer-v2 can be disabled for comparison during the alpha period.

## Decision Gates

- If texture transparency fixes remove most see-through artifacts, keep that
  work independent and ship it before the full renderer rewrite.
- If direct framebuffer and presentation cleanup removes most camera hitching,
  ship that as the first renderer-v2 milestone.
- If flicker remains after transparency and presentation fixes, prioritize
  depth buffering over more scaler tuning.
- If the depth-buffer prototype becomes more complex than the legacy renderer
  itself, switch attention to a GPU-backed backend rather than expanding the
  software renderer indefinitely.

## First Implementation Slice

Start with a narrow but meaningful renderer-v2 foundation:

1. [x] Add frame timing and allocation counters around the current renderer.
2. [x] Add a direct framebuffer path that can display the existing software output.
3. [x] Remove the active frame path's dependency on `ImageProducer`.
4. [x] Replace current scaling presentation with persistent buffers and a clean
   present step.
5. Re-test the known bad acuity locations before touching polygon rendering.

Renderer telemetry is opt-in. Enable it with the JVM property
`-Dspoiledmilk.rendererTelemetry=true` or the environment variable
`SPOILED_MILK_RENDERER_TELEMETRY=true`. Optional JVM properties:
`-Dspoiledmilk.rendererTelemetryInterval=300` and
`-Dspoiledmilk.rendererSlowFrameMs=35`.

The direct framebuffer path is also opt-in while it is being compared against
the legacy image-producer path. Enable it with the JVM property
`-Dspoiledmilk.directFramebuffer=true` or the environment variable
`SPOILED_MILK_DIRECT_FRAMEBUFFER=true`.

The GPU-presenter bridge is opt-in. Enable it with the JVM property
`-Dspoiledmilk.gpuPresenter=true` or the environment variable
`SPOILED_MILK_GPU_PRESENTER=true`. This path uses Java2D `VolatileImage`
surfaces and the existing Java2D OpenGL pipeline; it is not the final
LWJGL/OpenGL backend.

This slice gives immediate performance and timing data while creating the
surface that the deeper z-buffered or GPU renderer can plug into.

## Open Questions

- Should renderer-v2 require a modern desktop client only, or must it remain
  compatible with every inherited launch target?
- Should UI scale separately from the world, especially for high-resolution
  monitors?
- Should sprite and texture assets be converted offline into explicit alpha
  formats, or should conversion happen during client cache loading?
- How long should the legacy renderer remain available after renderer-v2
  becomes the default?
