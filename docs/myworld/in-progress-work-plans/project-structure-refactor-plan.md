# Project Structure Refactor Plan

This is the AI-facing plan for rebuilding Spoiled Milk's folder and package
structure safely. The project is inherited from OpenRSC, which inherited from
older RuneScape Classic code. The goal is not cosmetic cleanup. The goal is to
make the codebase easier to reason about while preserving the live game.

Use this plan together with
[`code-cleanup-and-modularization-plan.md`](code-cleanup-and-modularization-plan.md).
The cleanup plan lists oversized files and extraction targets. This file defines
the workspace safety rules and the intended ownership layout.

## Current Refactor Scope

The active refactor is the project/workflow safety refactor created after branch
and launch confusion put the live server at risk. Do not confuse this with the
older renderer-size refactor.

Current priority order:

1. Make the public hosted server launch path mechanically safe.
2. Make local/dev/refactor server launches impossible to mistake for live.
3. Separate the manager, neutral AI worker slots, and live deployment by
   physical worktree.
4. Restructure folders/packages only after the server workflow is protected.
5. Return to renderer or gameplay cleanup only when the live-server path is
   clear, documented, and verified.

Renderer cleanup remains a later structure target because the renderer is still
large and important, but it is not the reason this refactor exists.

## Safety Contract

The live hosted server must never depend on the active development worktree.

- Manager worktree: `main` only, used to collect, test, publish, and release.
- AI worker worktrees: neutral numbered folders, each owning one temporary
  focused topic branch at a time.
- Live worktree: clean detached `HEAD` at an exact published-main commit;
  never used for development.
- Dirty worktrees are acceptable for development, but unsafe for public hosted
  server startup.
- If a command starts the hosted server, it must be obvious which worktree,
  branch, commit, database, and item definitions it is using.

If these rules conflict with convenience, the safety rules win.

## Working Folders

Use physical folders so branch state is visible from the path.

| Folder | Branch | Purpose | Live server allowed |
| --- | --- | --- | --- |
| `/tmp/spoiled-milk-live-main` | detached published commit | Clean live-server deployment. | Yes |
| `/home/justin/Core-Framework` | `main` | Manager: review, merge, test, publish, release, and deploy. | No |
| `/home/justin/Core-Framework-ai-1` | temporary topic branch or detached IDLE | Neutral AI worker slot. | No |
| `/home/justin/Core-Framework-ai-2` | temporary topic branch or detached IDLE | Neutral AI worker slot. | No |
| `/home/justin/Core-Framework-ai-3` | temporary topic branch or detached IDLE | Neutral AI worker slot. | No |

The numbered folders describe AI seats, not categories of work. Feature,
bugfix, plan, and structural tasks all receive short-lived descriptive
branches in whichever slot is free.

Recommended setup:

```bash
./scripts/ai-workspace.sh init 3
./scripts/ai-workspace.sh start ai-1 refactor/project-structure
./scripts/ai-manager.sh status
```

Before any hosted release or server restart, confirm the live checkout is
clean, detached, published-main history, and not a feature/refactor branch.

## Refactor Branch Rules

- One ownership area per commit where practical.
- Compile after every extraction.
- Keep moved code behavior-identical in the first pass.
- Do not rename and rewrite in the same pass unless the old shape makes a safe
  move impossible.
- Do not delete compatibility paths until their callers, launchers, configs,
  generated data, and docs have been checked.
- Do not expose hidden legacy options as player-facing settings while moving
  code.
- Avoid touching live server scripts from the structure branch unless the task
  is specifically about launch safety.
- If launch safety scripts are changed, test them from a clean main-derived
  branch before merging.

## Current Top-Level Reality

These roots are active today:

| Current path | Current role |
| --- | --- |
| `Client_Base/` | Shared client code, game UI, legacy graphics, client definitions, settings, world/scene logic. |
| `PC_Client/` | Desktop/LWJGL launcher and OpenGL presenter. |
| `server/` | Server runtime, definitions, plugins, persistence, commands, combat, world state. |
| `tools/generators/` | Source-of-truth generators for item/NPC/data definitions. |
| `scripts/` | Build, launch, package, release, and utility wrappers. |
| `tests/myworld/` | MyWorld validation tests. |
| `docs/myworld/` | Active plans, completed records, stable references, and screenshots. |
| `dev/myworld/` | Compatibility wrappers and active visual asset source. Do not add new planning docs here. |
| `docs/inherited-openrsc/` | Archived inherited OpenRSC/Cabbage reference material. |

## Target Ownership Map

This is the direction to move toward. It does not require a single giant folder
rename. Start by extracting classes into clear ownership boundaries inside the
existing source roots.

```text
client
  app
    launch and platform integration
  config
    saved settings, migrations, profile application
  game
    gameplay client state, packet reactions, local prediction
  ui
    interface screens, guides, menus, chat, login
  assets
    sprite, model, texture, font, and external PNG loading
  world
    sectors, terrain products, walls, roofs, objects, scene export
  renderer
    api
      renderer-facing data contracts
    opengl
      GL lifecycle, buffers, shaders, windows, viewport, frame presentation
    lighting
      day/night tones, terrain relief, sky/fog, glow, future light sources
    shadows
      shadow masks, caster classification, indoor blockers, contact shadows
    sprites
      sprite composition, occlusion, billboards, projectile visuals
    telemetry
      F6 debug, frame timing, CPU/memory/culling counters
    legacy
      compatibility-only software renderer and scaling paths

server
  bootstrap
    startup, config loading, hosted/local mode checks
  world
    regions, paths, NPC spawns, scenery, map edits, day/night clock
  entity
    players, NPCs, projectiles, combat state
  content
    skills, quests, drops, item effects, commands, minigames
  content/myworld
    Spoiled Milk owned content where inherited packages do not need direct edits
  protocol
    packet decoding, packet sending, compatibility messages
  persistence
    database access, player state, per-player charge stores
  definitions
    item/NPC/object/spell/prayer definitions and generated data loading

shared
  protocol constants, definition schemas, generated IDs, and cross-client/server
  contracts where sharing reduces drift

tools
  generators, map tools, sprite exporters, release helpers, diagnostics

docs
  myworld plans and references, release docs, contributor guides, inherited docs

tests
  focused validation for generators, gameplay rules, renderer-safe contracts,
  release packaging, and launch safety
```

## Migration Phases

### Phase 0: Guardrails Before Moving Code

- Confirm hosted server launch safety exists and refuses unsafe worktrees.
- Ensure `run-hosted-server.sh` can only run the public server from the clean
  live-main worktree.
- Ensure local/dev server commands do not silently look like the hosted server.
- Add a status command or checklist that reports running server PID, worktree,
  branch, commit, config file, database, and port.
- Add loud labels to startup output so `Spoiled Milk LIVE/HOSTED` and private
  dev servers are visually distinct.
- Keep dev/refactor server ports and database names obvious. A local server
  should never trick a tester into thinking they are logging into the live
  alpha database.
- Add/update docs before moving files.
- Record the current source roots and compatibility exceptions.
- Do not restructure while the public server is running from a dirty branch.

No package moves or renderer extractions should happen until these launch
guardrails are in place.

### Phase 1: Behavior-Preserving Extractions

Keep Java packages mostly stable. Extract focused classes from oversized files
without changing runtime behavior.

Priority examples:

- `OpenGLFramePresenter` into LWJGL bindings, shader program, viewport,
  window, chunk renderer, shadows, sprite composite, frame capture, and input.
- `mudclient` into renderer settings, profile application, asset loading,
  combat effects, movement interpolation, and scene instance storage.
- `World` into sector cache, terrain/wall/roof builders, minimap, collision,
  and renderer chunk export.
- `RenderTelemetry` into frame, scene, world, sprite, shadow, CPU, and memory
  categories behind a small facade.

### Phase 2: Package Ownership

After extracted classes compile and behave correctly, move them into clearer
packages such as:

- `orsc.renderer.opengl`
- `orsc.renderer.lighting`
- `orsc.renderer.shadows`
- `orsc.renderer.sprites`
- `orsc.renderer.telemetry`
- `orsc.client.ui`
- `orsc.client.assets`
- `orsc.client.world`

This phase should be mechanical. If behavior changes are needed, do them in a
separate follow-up commit.

### Phase 3: Legacy Quarantine

Move compatibility-only code into clearly named legacy areas and comment why it
still exists.

Targets:

- software renderer scaling
- hidden render-surface aliases
- retired graphics toggles
- obsolete launch wrappers
- archived OpenRSC configs and launchers
- compatibility-only item/content families

The important rule is that future AI sessions should not see old scaling,
resolution, or renderer paths and assume they are active remaster systems.

### Phase 4: Top-Level Folder Renames

Only consider top-level folder renames after extraction and package ownership
are stable. Renaming `Client_Base` and `PC_Client` too early risks breaking
build scripts, release packaging, and inherited assumptions before the code is
ready.

Possible final names:

- `client/common` for current `Client_Base`
- `client/desktop` for current `PC_Client`
- `server` stays `server`
- `tools`, `scripts`, `tests`, and `docs` stay as they are

### Phase 5: Prune Or Archive

Once the new structure is proven through builds, client launch, packaged launch,
and hosted-server checks:

- delete unused compatibility wrappers
- move historical references to `docs/inherited-openrsc/`
- remove generated files that are no longer source-of-truth
- simplify release packaging paths
- update contributor docs so new work lands in the new ownership areas

## Visual And Runtime Checkpoints

Before any hosted restart:

- `git status --short --branch` in the live worktree shows clean detached
  `HEAD`
- live worktree commit matches the published `spoiled-milk/main` selected for
  deployment
- startup output says it is using `myworld-host.conf`
- startup output says it connected to `spoiled_milk_alpha.db`
- `live-status.sh` verifies the launch attestation, database symlink, and
  database file descriptor
- public port `43605` belongs to the hosted server process
- no manager or AI worker worktree has a server process bound to the public
  player port

Before any dev/refactor launch:

- startup output says private/dev/refactor explicitly
- the database name is not `spoiled_milk_alpha.db`
- the server is stopped after testing unless the user explicitly asks to keep it
  running
- the final status message says which server, if any, is still running

After each renderer-facing extraction:

- login screen graphic appears correctly
- 4:3 and 16:9 aspect settings work
- borderless on/off works through the packaged launcher
- player/NPC sprites occlude correctly behind walls and scenery
- ground items, projectiles, and scenery animations render
- roofs hide/show without breaking scenery
- day/night tones, fog, skybox, glow, contact shadows, and directional shadows
  still work
- F6 simple/expanded debug still reports useful values
- player movement, NPC movement, combat, teleporting, and region loading still
  behave normally

After each server-facing extraction:

- generator checks pass
- build server succeeds
- login works with the expected client build
- item/NPC definition max IDs match the packaged client release
- no dev-only branch is used to start the public hosted server

## Stop Conditions

Stop and reassess before continuing if any of these happen:

- there is any uncertainty about which server, database, branch, or worktree is
  active
- hosted server would need to run from the refactor branch
- a moved file requires unrelated gameplay changes to compile
- a compatibility system looks unused but has not been checked against scripts,
  configs, saved settings, and release packaging
- a visual-risk renderer extraction fails more than twice
- git status contains unrelated user changes in files that the refactor needs
  to rewrite

The project is in alpha, so large structural swings are allowed. The safety
requirement is that each swing has a clear owner, a clean rollback point, and no
path to accidentally becoming the live hosted server before approval.
