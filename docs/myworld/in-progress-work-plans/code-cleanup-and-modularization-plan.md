# Code Cleanup And Modularization Plan

This is the AI-facing cleanup roadmap for Spoiled Milk code structure. It is
not a feature plan. Its job is to keep future work from getting lost inside
large inherited files, half-retired options, and renderer bridge code that was
useful during migration but should not remain the long-term shape of the
engine.

Use this file when a task touches renderer internals, graphics settings,
presentation, asset loading, telemetry, entity definitions, or other large
systems. The goal is not cleanup for its own sake. The goal is to make large
renderer swings easier, safer, and less likely to accidentally revive old
software-renderer assumptions.

For the repository-level folder/package direction and safe worktree workflow,
use
[`project-structure-refactor-plan.md`](project-structure-refactor-plan.md)
before moving files.

## Cleanup Rule

This project is in alpha and large under-the-hood changes are acceptable.
Cleanup should favor structural moves that unlock future renderer work:

- Break large files along real ownership boundaries.
- Prefer moving code into focused classes over adding more helper methods to
  already oversized files.
- Keep behavior stable during pure extraction passes.
- Remove or quarantine retired settings so future AI sessions do not mistake
  them for active player-facing systems.
- Keep compatibility shims only when a current launcher, saved setting, server
  config, or fallback path still needs them.
- Do not spend many iterations polishing one small subsystem if a larger
  ownership problem is blocking renderer modernization.

## Current Hotspots

Approximate line counts from the July 3, 2026 structure-refactor branch:

| File | Lines | Why It Matters |
| --- | ---: | --- |
| `Client_Base/src/orsc/mudclient.java` | 25,988 | Owns gameplay UI, renderer settings UI, region loading, scene instance arrays, movement smoothing, combat effects, projectiles, asset import, and many renderer bridge calls. |
| `Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java` | 9,676 | Mixes definition storage, accessors, MyWorld overrides, generated item/style data, prayers, spells, sprites, and compatibility fallbacks. |
| `server/src/com/openrsc/server/model/entity/player/Player.java` | 5,908 | Central server entity state; likely too broad for continued gameplay feature growth. |
| `PC_Client/src/orsc/OpenGLFramePresenter.java` | 4,544 | Still owns OpenGL lifecycle, input callback dispatch, window mode, frame pass orchestration, viewport presentation, and remaining composite glue. Major shader, shadow, texture, capture, and world renderer extractions have already landed. |
| `Client_Base/src/orsc/graphics/two/GraphicsController.java` | 4,289 | Legacy 2D drawing plus OpenGL capture/replay instrumentation, sprite scaling, text plotting, and sprite archive loading. |
| `Client_Base/src/orsc/graphics/three/World.java` | 4,131 | Client terrain, wall, roof, minimap, collision, sector loading, CPU product caching, and renderer chunk export are mixed. |
| `Client_Base/src/orsc/graphics/three/Scene.java` | 3,789 | Legacy scene sorting/raster flow and renderer-v2 frame export live together. |
| `Client_Base/src/orsc/PacketHandler.java` | 3,189 | Packet parsing plus config propagation and client state mutation. |
| `Client_Base/src/orsc/RenderTelemetry.java` | 2,559 | Many unrelated counters and reports in one static sink. Useful, but hard to extend safely. |
| `server/src/com/openrsc/server/model/container/Equipment.java` | 3,718 | Equipment behavior and item-effect hooks continue growing. |
| `server/src/com/openrsc/server/net/rsc/handlers/SpellHandler.java` | 2,784 | Spell routing, special effects, and combat interactions are likely separable. |
| `server/src/com/openrsc/server/net/rsc/ActionSender.java` | 2,707 | Outgoing protocol building and gameplay-specific sender helpers are mixed. |
| `server/src/com/openrsc/server/content/Summoning.java` | 1,856 | All Summoning profiles, runtime behavior, utility commands, combat, and scaling are in one utility class. |
| `server/src/com/openrsc/server/content/EnchantingItemEffects.java` | 1,828 | Jewelry, robe, staff, altar, charge, and tier-effect logic are accumulating in one class. |

## Priority Order

1. Finish splitting `OpenGLFramePresenter.java`.
2. Split renderer-facing parts of `mudclient.java`.
3. Quarantine and remove stale scaling/resolution/player-option paths.
4. Split `World.java` product builders and client sector caches.
5. Split `GraphicsController.java` legacy drawing from renderer-v2 capture.
6. Split telemetry into categories after renderer ownership is clearer.
7. Split `EntityHandler.java` MyWorld overrides and generated definition logic.
8. Handle large server gameplay classes after the renderer cleanup has reduced
   active client churn.

## OpenGLFramePresenter Split Plan

This is the highest-value cleanup. The file should become a coordinator, not
the place every OpenGL concept lives.

Keep the first extraction passes in package `orsc` unless there is a clear
reason to expose APIs across packages. Moving to `orsc.opengl` is desirable
later, but doing that first will force public APIs and make behavior-preserving
extraction noisier.

### Proposed Files

- `LwjglBindings.java`
  - Move the reflective LWJGL/GLFW/GL method table and callback proxy classes.
  - This is a near-mechanical extraction from the bottom of
    `OpenGLFramePresenter.java`.
  - Expected risk: low if moved unchanged.
  - Status: done.

- `OpenGLShaderProgram.java`
  - Move shader compilation, uniform binding, attribute binding, and inline
    shader source strings.
  - Longer term, shader source should become external `.glsl` resources so
    shader work does not require editing a Java string wall.
  - Expected risk: medium because shader uniforms/attributes are tightly coupled
    to vertex layouts.
  - Status: done.

- `OpenGLWorldChunkRenderer.java`
  - Move resident chunk upload, draw, batch culling, material passes, buffer
    lifetime, world matrices, and chunk shader input packing.
  - This class is currently the most important renderer runtime path.
  - Expected risk: medium-high. Extract after `LwjglBindings` and
    `OpenGLShaderProgram`.
  - Status: done.

- `RemasterShadowMaskBuilder.java`
  - Move terrain shadow mask rasterization, blur, cache signature, light-angle
    quantization, and mask texture data.
  - Keep pure data/math separated from GL upload so it can later be tested
    without a window.
  - Expected risk: medium. This is visually sensitive but has clear boundaries.
  - Status: done.

- `RemasterShadowClassifier.java`
  - Move roof coverage, indoor flood fill, wall blocker crossing, and caster
    eligibility/classification.
  - This should become the source of truth for "outdoor sunlight eligible"
    terrain and casters.
  - Expected risk: medium. Needs visual validation around roofless interiors.
  - Status: done.

- `OpenGLWorldSpriteComposite.java`
  - Move world sprite matching, anchor lookup, legacy scene sprite restore, and
    sprite/world occlusion ordering.
  - Expected risk: high. This area caused invisible sprites and layering bugs,
    so extract only after the GL foundation classes are stable.
  - Status: partially done through `OpenGLWorldSpriteDrawController` and
    `OpenGLWorldSpriteRenderer`; keep remaining sprite composite glue in the
    presenter until the input/window/viewport split is stable.

- `OpenGLFrameCapture.java`
  - Move frame capture directory writing, layer export, static-world TSV output,
    shader parity output, and capture burst bookkeeping.
  - Keep disabled by default for release builds.
  - Expected risk: low-medium.
  - Status: done.

- `OpenGLInputBridge.java`
  - Move GLFW key/mouse/scroll/char callbacks and AWT event posting.
  - Expected risk: medium. Needs text input, backspace repeat, mouse wheel zoom,
    chat scroll, and focus tests.
  - Status: started. `OpenGLKeyBindings.java` now owns the GLFW-to-AWT key map
    and `KeyBinding` data class.

- `OpenGLWindowController.java`
  - Move borderless/windowed creation, monitor mode, viewport resize, close
    handling, and fullscreen-effectiveness checks.
  - Expected risk: medium. Needs testing through both dev and packaged launchers.

- `OpenGLViewportPresenter.java`
  - Move source framebuffer to destination viewport scaling, aspect-fit bars,
    integer-fit/stretch policy, and framebuffer upload for non-world UI layers.
  - Expected risk: medium. This must stay consistent with current aspect-ratio
    settings.

### Extraction Sequence

Completed in the current renderer split:

1. Extracted `LwjglBindings`.
2. Extracted `OpenGLShaderProgram`.
3. Extracted several plain renderer data holders and texture helpers.
4. Extracted `RemasterShadowClassifier`.
5. Extracted `RemasterShadowMaskBuilder`.
6. Extracted `OpenGLFrameCapture`.
7. Extracted `OpenGLWorldChunkRenderer`.
8. Extracted world mesh, world sprite, glow mask, texture cache, and material
   helper classes.
9. Started input extraction with `OpenGLKeyBindings`.

Next sequence:

1. Finish `OpenGLInputBridge` by moving GLFW callback install, mouse/key state,
   coordinate mapping, and AWT event posting behind a delegate.
2. Extract `OpenGLViewportPresenter` for viewport fitting, integer-scale text
   smoothing, framebuffer presentation, and aspect-fit bars.
3. Extract `OpenGLWindowController` for borderless/windowed creation, monitor
   placement, resize, and close handling.
4. Reassess remaining `OpenGLFramePresenter` responsibilities before touching
   sprite composite glue.
5. Extract remaining sprite composite code only after input/window/viewport
   behavior has been validated.

Each step should compile before moving to the next. For visual-risk steps, run
the client and validate at least:

- login screen graphic
- 4:3 and 16:9 aspect settings
- borderless on/off
- player and NPC sprites behind walls/scenery
- ground item sprites
- scenery animations
- roof hide/show
- day/night tone and shadow motion
- F6 simple/expanded debug overlay

## mudclient Split Plan

`mudclient.java` is larger than the OpenGL presenter and is still the main
client god object. It should not be split randomly. Start with pieces that are
already conceptually independent and have low gameplay risk.

### Proposed Extractions

- `RendererSettingsPanel`
  - Move Graphics/Interface setting row construction and click dispatch for:
    preset, aspect ratio, borderless, lighting, geometry, fog, brightness, font.
  - Keep legacy gameplay settings separate.
  - This prevents old scaling rows from being accidentally mixed into active
    OpenGL graphics options.

- `LegacySoftwareScalingSettings`
  - Move `renderingScalar`, `newRenderingScalar`, `scalingType`,
    `scaleUp`, `scaleDown`, `cycleScalingType`, and `saveScalingSettings`.
  - Mark as software presenter only.
  - Long-term target: remove from OpenGL builds entirely, leaving only saved
    setting migration if needed.

- `RendererProfileApplier`
  - Move Classic/Remaster/Custom preset application.
  - This should be the one place that decides default aspect, lighting,
    geometry, fog, brightness, tone, and borderless state.

- `ClientExternalAssetLoader`
  - Move `dev/myworld/assets` and embedded `myworld-assets` lookup, image reads,
    external item sprites, UI icons, prayer/magic/summon icons, equipment
    sprites, and utility image transforms.
  - Current asset loading is mixed into `mudclient` and is too easy to break
    while changing renderer logic.

- `CombatEffectSpriteSystem`
  - Move combat-effect constants, frame counts, scene sizes, detach/draw logic,
    projectile visual generation, and effect sprite lookup.
  - This will reduce the chance that renderer sprite changes alter gameplay
    combat state by accident.

- `ClientSceneInstanceStore`
  - Move game object and wall object instance arrays, capacity checks,
    materialized flags, and getter/setter wrappers.
  - This is useful before further resident-object renderer work.

- `PredictiveTerrainPreloader`
  - Move section preload tracking for target, waypoint, and camera direction.
  - This keeps movement smoothing and terrain streaming from becoming scattered.

- `MovementInterpolator`
  - Move per-frame movement amount, reset, and custom waypoint append helpers.
  - Keep server-authoritative path state separate from visual interpolation.

- `LoginSceneRenderer`
  - Move login camera/model drawing and login-screen scene setup.
  - This is low priority, but it isolates the login graphic/minimap regression
    class of bugs.

### mudclient Guardrails

- Do not move packet mutation or gameplay state into renderer classes.
- Do not make renderer settings depend on server settings except where the
  server actually owns state, such as day/night time.
- Do not expose hidden resolution or scaling options through the new settings
  panel.
- Keep `mudclient` as the integration point until each extracted class has a
  clear constructor dependency list.

## Stale And Hidden Option Audit

These paths should be explicitly quarantined before deeper cleanup. The main
risk is future work seeing the old names and assuming they are active
player-facing graphics systems.

### Legacy Software Scaling

Current locations:

- `Client_Base/src/orsc/mudclient.java`
  - `scalingType`
  - `renderingScalar`
  - `newRenderingScalar`
  - `saveScalingSettings`
  - `scaleUp`
  - `scaleDown`
  - `cycleScalingType`
  - old `Scaling` and `Scaling type` settings rows
- `PC_Client/src/orsc/ScaledWindow.java`
  - software presenter resize, repaint, mouse mapping, and interpolation scale
    logic
- `PC_Client/src/orsc/OpenRSC.java`
  - loads `scaling_type` and `scaling_scalar`
- `Client_Base/clientSettings.conf`
  - still contains `scaling_type=0` and `scaling_scalar=1.0`

Current intended status:

- Software-presenter compatibility only.
- Not an OpenGL-primary player graphics setting.
- Not a replacement for true render-surface/aspect or camera zoom.

Cleanup target:

- Move to `LegacySoftwareScalingSettings`.
- Add a source-level comment that this is not for OpenGL-primary renderer work.
- Consider deleting saved defaults from packaged configs once confirmed safe.

### OpenGL Presentation Scale Mode

Current locations:

- `Client_Base/src/orsc/OpenGLPresentationSettings.java`
  - `aspect-fit`
  - `integer-fit`
  - `stretch`
- `Client_Base/src/orsc/mudclient.java`
  - `cycleOpenGLScaleMode`
- `PC_Client/src/orsc/OpenGLFramePresenter.java`
  - `currentScaleMode`
  - viewport computation and filtering behavior
- `Client_Base/clientSettings.conf`
  - `opengl_scale_mode=aspect-fit`

Current intended status:

- Presentation policy, not a normal player-facing graphics option.
- Aspect-fit is the baseline. Borderless/windowed and aspect ratio are the
  current user-facing controls.

Cleanup target:

- Either keep as a runtime diagnostic only or fold into the new
  `OpenGLViewportPresenter`.
- Do not re-add it to the in-game Graphics menu without a deliberate design
  decision.

### Hidden Render Surface Values

Current locations:

- `Client_Base/src/orsc/RenderSurfaceSettings.java`
  - hidden values: `512x346`, `640x480`, `1024x576`, `1280x720`, `1920x1080`
  - visible values: `800x600` and `960x540`

Current intended status:

- Player-facing setting is `Aspect Ratio`, not a full resolution list.
- Old 720p/1080p saved/runtime values migrate to `960x540`.
- Old 4:3 values migrate to `800x600`.

Cleanup target:

- Keep hidden modes only as migration aliases or dev/runtime diagnostics.
- Make the migration/alias role obvious in code comments.
- Avoid restoring the old visible resolution list.

### Camera Tilt And Extra Zoom

Current locations:

- `Client_Base/src/orsc/RendererExperimentalSettings.java`
  - defaults `cameraTiltEnabled` and `extraZoomEnabled` to true
  - runtime properties/env vars can still override for diagnostics
- `Client_Base/src/orsc/mudclient.java`
  - camera zoom min/max and camera tilt behavior

Current intended status:

- Baseline-on, no longer player-facing experimental toggles.

Cleanup target:

- Rename or document away from "experimental" once the behavior is considered
  permanent.
- Do not re-add player-facing toggles unless a real accessibility or
  compatibility problem appears.

### Server Zoom View Toggle

Current locations:

- `Client_Base/src/orsc/Config.java`
  - `S_ZOOM_VIEW_TOGGLE`
- `Client_Base/src/orsc/PacketHandler.java`
  - receives and saves the server config
- `PC_Client/src/orsc/ORSCApplet.java`
  - checks server zoom permission for wheel behavior
- `server/src/com/openrsc/server/ServerConfiguration.java`
  - `ZOOM_VIEW_TOGGLE`
- `server/src/com/openrsc/server/net/rsc/ActionSender.java`
  - sends config slot 15

Current intended status:

- Needs a dedicated audit before removal because it is a server/client config
  path, not only a renderer setting.

Cleanup target:

- Decide whether zoom is now always allowed for Spoiled Milk.
- If yes, migrate the server config into compatibility-only behavior and remove
  the dead conditionals from client input.

### Debug And Capture Flags

Current locations:

- `PC_Client/src/orsc/OpenGLFramePresenter.java`
  - frame capture, renderer mode flags, composite debug, shadow debug
- `Client_Base/src/orsc/RendererDebugSettings.java`
  - F6 overlay state and mode

Current intended status:

- F6 debug HUD is acceptable for open-source diagnostics.
- Capture hotkeys and invasive debug toggles should remain disabled in release
  unless explicitly requested.

Cleanup target:

- Move capture/debug code out of the presenter.
- Centralize debug property names in one class.
- Keep release-default debug behavior obvious.

## World And Scene Cleanup

### Client World

`Client_Base/src/orsc/graphics/three/World.java` should be split by product:

- `WorldSectorCache`
  - sector template loading, section window cache, preload queueing.
- `TerrainModelBuilder`
  - terrain vertex/tile/overlay collection and terrain model emission.
- `WallModelBuilder`
  - wall segments, wall collision/elevation/minimap side effects.
- `RoofModelBuilder`
  - roof coverage, roof elevation workspace, hidden roof products.
- `WorldMinimapBuilder`
  - minimap tile and wall draw logic.
- `WorldCollisionBuilder`
  - collision flag mutation and wall/object collision handling.
- `RendererWorldChunkExporter`
  - build `Renderer3DWorldChunkFrame` and chunk mesh data for OpenGL.

This split matters because shader and shadow work needs terrain/wall/roof
semantics without dragging in minimap, collision, and sector IO.

### Client Scene

`Client_Base/src/orsc/graphics/three/Scene.java` should separate:

- legacy polygon sorting and raster path
- pick/hit testing
- sprite submission and anchors
- renderer-v2 frame export
- camera/frustum state

Renderer-v2 should consume structured scene data instead of reaching through
legacy sort/raster details whenever possible.

## GraphicsController Cleanup

`Client_Base/src/orsc/graphics/two/GraphicsController.java` should split:

- `LegacySurfaceRaster`
  - boxes, lines, circles, strings, sprite plotting, low-level pixel writes.
- `Renderer2DCaptureRecorder`
  - command capture for sprites, text, primitives, rotated sprites, circles.
- `SpriteArchiveLoader`
  - sprite tree loading and archive unpack.
- `SpriteDrawTransform`
  - scaled sprite clipping, masks, alpha thresholds, and transform decisions.

This reduces risk around UI replacement work. It also makes it easier to decide
which legacy 2D paths are still needed once OpenGL owns more of the UI.

## Telemetry Cleanup

`RenderTelemetry.java` is valuable but too broad. Split after renderer classes
are extracted:

- `FrameTimingTelemetry`
- `ClientLoopTelemetry`
- `SceneTelemetry`
- `OpenGLWorldTelemetry`
- `OpenGLSpriteTelemetry`
- `OpenGLShadowTelemetry`
- `AllocationTelemetry`
- `RendererDebugSummary`

Keep one public facade named `RenderTelemetry` if that avoids changing many
call sites. Internally, route to category objects.

## Entity Definition Cleanup

`Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java` is a
large non-renderer hotspot. It should eventually split into:

- `ClientDefinitionRegistry`
  - read-only access to items, NPCs, spells, prayers, tiles, doors, textures,
    animations, sprites, and models.
- `MyWorldDefinitionOverrides`
  - MyWorld-specific overrides and generated additions.
- `PrayerBookDefinitions`
  - Saradomin/Guthix/Zamorak prayer definitions and active book state.
- `GeneratedEquipmentDefinitions`
  - staff, cloth, pickaxe, and other generated visual/stat definitions.
- `DefinitionFallbackLogger`
  - one-shot fallback logging for missing or placeholder definitions.

Do this after renderer cleanup unless definition churn starts blocking client
work.

## Server Cleanup Candidates

These are not the first priority for the renderer push, but they are worth
tracking:

- `Player.java`
  - split persistent state, runtime combat state, social/party/clan state,
    skill state, and UI/session state where possible.
- `Equipment.java`
  - separate slot rules, stat aggregation, item-effect hooks, durability/charge
    helpers, and visual appearance mapping.
- `EnchantingItemEffects.java`
  - split jewelry line effects, robe effects, staff effects, charge stores, and
    altar/tier requirements.
- `Summoning.java`
  - split profiles, casting/costs, runtime summon ownership, utility summons,
    combat summons, support upkeep, and UI command handlers.
- `SpellHandler.java`
  - split spell validation, rune costs, combat spell dispatch, utility spells,
    and special/AOE behavior.
- `ActionSender.java`
  - separate packet writer primitives from gameplay-specific response helpers.

## Refactor Safety Rules

- One ownership extraction per commit when possible.
- Compile after every extraction.
- For renderer-presenter extraction, run at least a client smoke test before
  stacking visual-risk work.
- Keep moved code textually identical first, then improve naming in a second
  pass.
- Do not delete compatibility aliases in the same pass that extracts code.
- Do not convert hidden/debug options into player-facing settings unless the
  design docs explicitly say to.
- When removing stale settings, search launch scripts, packaged configs, server
  configs, and docs first.
- Any change touching sprite ordering, shadow masking, roof visibility, or
  region loading needs visual validation.

## Recommended First Cleanup Pass

Start with `OpenGLFramePresenter.java` because it is the exact file people are
noticing and because it is blocking shader/shadow work from being approachable.

Suggested first three commits:

1. Move `LwjglBindings` and callback handlers into `LwjglBindings.java`.
2. Move `OpenGLShaderProgram` and inline shader source into
   `OpenGLShaderProgram.java`.
3. Move `Viewport`, `MonitorMode`, and viewport calculation helpers into an
   `OpenGLViewport` or `OpenGLViewportPresenter` class.

These should reduce the file size without changing visuals. After those, move
the remaster shadow classifier/mask code so future lighting work has a focused
home.

## Definition Of Done

This cleanup track is succeeding when:

- The largest renderer file is below 5,000 lines.
- `mudclient.java` no longer owns renderer settings UI, external asset loading,
  combat effect sprite management, and scene instance stores directly.
- Old scaling/resolution options are visibly compatibility-only or removed.
- Shader source and shader program binding are not embedded in the presenter.
- Shadow classification and shadow mask generation can be reasoned about
  without reading the full presenter.
- Future AI sessions can identify the right file for renderer, shader, UI,
  asset, telemetry, and definition work without searching a 16k-25k line file.
