# Terrain Expansion Plan

This document tracks short-term and long-term terrain expansion for MyWorld.
The intended workflow is collaborative: an AI-assisted broad pass establishes
coherent terrain and gameplay structure, then the area is explored in game and
revised through field notes, targeted instructions, and manual editor work.

Terrain work is higher risk than adding scenery, ground items, or NPC
locations. The current repository contains compiled landscape and map archives,
but does not yet expose a confirmed authored terrain source or supported editor
workflow. Tool discovery and reversible map handling must therefore happen
before production terrain is changed.

## Core Goals

- Expand useful world space without breaking existing towns, quests, travel,
  collision, or map transitions.
- Match nearby terrain, wall, floor, elevation, and scenery language instead of
  making additions look disconnected from the original world.
- Let AI handle broad layout work while keeping human exploration and revision
  central to the process.
- Make every terrain experiment reversible and isolated from the normal server.
- Validate boundaries deliberately so expanded terrain does not create
  unintended entrances, exits, safespots, or map escapes.

## Work Order

### Short Term: Heroes' Guild Basement

Use the Heroes' Guild basement as the pilot terrain project.

Desired result:

- Expand the basement into a larger high-level PvM and mining area.
- Add enough room for high-end enemies to be farmed without crowding the
  existing basement route.
- Add high-end ore in deliberate mining pockets.
- Preserve the existing ladder, doors, quest-sensitive spaces, and reliable
  return path.
- Keep combat areas readable, with enough movement space for melee, ranged,
  Magic, summons, and multi-enemy encounters.

Initial design pass:

1. Identify the exact map plane, chunk boundaries, ladder coordinates, and all
   scripts that reference the current basement.
2. Export or decode the affected chunks into an editable and reviewable format.
3. Record screenshots and collision data for the unchanged basement.
4. Sketch an expansion using the current stone floor, walls, rooms, and cave or
   dungeon details as the visual reference.
5. Separate the layout into an entrance buffer, one or more combat rooms,
   mining pockets, and circulation paths.
6. Add temporary enemy and ore placements only after terrain traversal is
   stable.
7. Field-test the space before deciding final spawn density and ore tiers.

Approved first chamber layout:

- Expand north into the flat unused terrain behind the current room rather
  than south into the sloped mining passage.
- Use four equal `3 x 5` cage interiors arranged as two cages on each side of
  a central aisle.
- Retain the existing blue dragon and its current tiles as the lower-right
  cage.
- Put the stairs against the new north wall at the head of the central aisle.
- Face all four gates toward the central aisle.
- Use the chamber's stone outer walls as cage boundaries; only add railings
  where a cage does not already meet an outer wall.
- Keep the central aisle open into the ore passage, widening the effective
  entrance without moving any ore during this terrain-only phase.
- Add the other three NPCs in a later phase, followed by the ore expansion.

The pilot is successful when terrain can be edited, rebuilt, loaded, reverted,
and tested repeatedly without affecting unrelated map chunks.

### Short Term: Cosmic Altar Approach

Rework the overworld path to the Cosmic Altar near `106,3565`. The intended
space should feel isolated, empty, and ethereal. The current irregular grass
path reads as visibly painted terrain rather than a deliberate route through
empty space.

Preferred direction:

- Remove the random grass-road pattern.
- Preserve a fully traversable route while making its ordinary ground surface
  visually disappear into the surrounding emptiness.
- Add a restrained shimmer, ripple, or flowing-light effect along the route so
  players can perceive the safe path without it becoming a conventional road.
- Keep the effect low to the ground and avoid scenery that blocks movement,
  targeting, or the view of the altar.
- Make the route readable enough that players can follow it without trial and
  error, even if its edges remain indistinct.

Current and proposed route:

- The existing entrance begins at `150,3541`, between two torch scenery
  objects. The entrance position and torch opening should remain unchanged.
- Beyond the torches, remove the visible grass floor along the route.
- Remove the grass beneath the Cosmic Altar and its surrounding obelisks as
  well, so the destination appears to occupy the same ethereal space.
- Replace the current elongated, winding route with a simple L-shaped path.
- The existing route passes the altar and turns back, bringing the player to
  the altar from the side near `104,3566`.
- The replacement should approach the opposite side and end near `108,3566`.
- Choose the L-shaped route's single corner after inspecting the affected
  chunks. It should create two direct segments, shorten travel substantially,
  and avoid moving the entrance, altar, obelisks, or torches.

The traversable path may be visually invisible apart from its particle or
moving-pixel markers. Keeping it to a simple L shape is therefore a navigation
requirement, not only a visual preference: players should be able to recover
the route by following one straight segment and making one clear turn.

The existing `fountain` and `fishing` visuals are candidates for visual
reference, but they are ordinary scenery models rather than reusable particle
effects. Their object-definition width and height determine footprint and
placement; increasing those values does not stretch the rendered model.
Producing a long shimmer therefore requires one of:

- a new low-profile model derived from the useful water surface
- a client-generated strip or tile model
- repeated one-tile shimmer scenery pieces designed to join cleanly

Animation is a separate concern. Existing animated scenery such as fires and
torches swaps between explicitly named model frames. The fountain and fishing
models are not currently registered as an animated frame set. A moving path
would need either a small model-frame sequence or a dedicated client-side
animation rule.

The preferred animated prototype should be deliberately minimal: a flat tile
with a few bright pixels or short streaks that advance in one direction across
two or three frames. This could suggest energy flowing toward the altar without
requiring water simulation, transparency-heavy particles, or detailed models.
Use one shared frame set for every path tile and vary each tile's starting frame
or orientation so the complete path does not pulse in lockstep.

Low-resource animation targets:

- two or three frames total
- a slow update rate rather than an update every rendered frame
- one small, reusable texture or flat model per frame
- no collision, interaction option, shadow, or complex geometry
- no per-tile server updates; animation should be entirely client-side
- sparse moving pixels with most of the tile left visually empty

Prototype in this order:

1. Replace the grass path with visually empty but traversable terrain and test
   navigation, collision, camera readability, and route boundaries.
2. Place static, non-interactive one-tile shimmer models along a short test
   segment. Compare fountain-water, fishing-ripple, and a purpose-built
   translucent or light-textured surface.
3. If the static version reads well, test the minimal moving-pixel animation
   before adapting any larger water effect. Avoid synchronized flashing across
   the entire route.
4. Extend the selected treatment across the complete path only after movement,
   scene loading, and performance are stable.

Important constraints:

- An invisible path still needs clear server and client collision agreement.
- The complete walkable route must connect `150,3541` to the altar approach at
  `108,3566`, with only one deliberate turn.
- Tiles outside the L-shaped route must remain blocked even after the visible
  grass is removed.
- Test walking both toward and away from the altar without relying on camera
  rotation to locate the route.
- Decorative objects should use a non-blocking type and should not create
  right-click clutter.
- The route must remain visible enough under different camera angles and
  brightness settings.
- Repeated tiles must not expose obvious seams, z-fighting, or a rigid grid.
- The original path chunks should remain restorable throughout experimentation.

### Long Term: Northern Wilderness Expansion

Extend the Wilderness eastward across the northern map until it reaches the
region north of the Tree Gnome Stronghold.

This is not only a visual extension. It requires world-boundary design,
Wilderness-level behavior, travel routes, encounter planning, and strong
protection around existing regions.

Desired result:

- Fill missing northern terrain with a natural continuation of Wilderness
  geography.
- Create multiple recognizable regions rather than one large empty field.
- Support future PvM clusters, gathering areas, ruins, caves, and landmarks.
- Preserve intentional access routes into the Tree Gnome Stronghold and other
  established regions.
- Prevent players from bypassing gates, quests, or travel requirements by
  walking around old map boundaries.

Candidate terrain language:

- Continue nearby Wilderness ground, dead vegetation, ruins, lava, rocky
  ridges, graveyards, forests, and elevation patterns where appropriate.
- Use mountains, cliffs, dense blocked forest, water, walls, and other
  believable barriers to close unsafe boundaries.
- Include controlled corridors between regions so travel and danger can be
  tuned without relying on invisible walls.
- Leave room for later landmarks and underground entrances rather than filling
  every tile during the first pass.

The Tree Gnome Stronghold boundary needs its own audit. Every perimeter tile,
gate, obstacle, teleport destination, and nearby elevation transition must be
checked so the expansion cannot provide an alternate entrance.

### Existing Northern Enclaves

The expansion must accommodate existing authored areas already occupying the
northern map space.

Sinclair Mansion should remain in place:

- Build the Wilderness around the mansion grounds rather than relocating them.
- Preserve the mansion, perimeter, roads, quest scenery, NPCs, ground items,
  and Murder Mystery coordinates.
- Use terrain and barriers to make the mansion a deliberate non-Wilderness
  enclave or boundary feature instead of allowing Wilderness behavior to bleed
  into the quest area.
- Audit every approach so the new terrain does not bypass mansion gates,
  fences, or quest interactions.

A second smaller special-purpose area may be relocatable, but it must be
identified before terrain work begins. Moving it is a coordinated content
migration, not only a terrain copy:

- Find its exact bounds, plane, purpose, and access mechanism.
- Search scripts, objects, NPCs, teleports, ladders, portals, respawns, logout
  recovery, and quest state for references to its coordinates.
- Copy or rebuild the complete area at an approved unused location.
- Redirect every entry, exit, return, failure, and recovery destination.
- Confirm players already inside the old location cannot become trapped after
  an update.
- Remove or block the old area only after the relocated version passes its
  complete interaction flow.

The first northern map audit should label every existing enclave as one of:

- preserve in place and build around
- relocate with a full coordinate-reference migration
- incorporate into the Wilderness
- unused or inaccessible terrain that can be replaced

## Collaborative Iteration

Each area should use the same review loop:

1. The design brief defines purpose, approximate bounds, required landmarks,
   entrances, exits, barriers, and prohibited access.
2. The existing surrounding chunks are inspected for matching terrain rules.
3. An AI-assisted broad pass creates the first coherent layout.
4. Automated checks catch malformed chunks, broken transitions, and obvious
   collision or boundary failures.
5. The area is explored on the isolated test server.
6. Field notes identify awkward terrain, visual repetition, poor routes,
   safespots, clipping, and areas that need more or less detail.
7. Targeted AI edits or manual editor changes revise the map.
8. NPCs, resources, drops, and rewards are added only after the terrain has
   stabilized.

Screenshots, coordinates, and short descriptions should accompany revision
requests. Exact references such as "open this path at 320,450" or "continue
the western cliff style for six tiles" are more reliable than broad visual
descriptions alone.

## Isolated Test Environment

Terrain experiments must not overwrite the normal development or release
environment.

Create a dedicated terrain-test setup with:

- a separate Git branch or worktree
- copied landscape and map archives
- a separate server configuration
- a separate SQLite development database
- a localhost-only bind address
- server and client ports different from the normal MyWorld ports
- a separate client cache or packaged test client pointed at those ports
- no public port forwarding
- clear scripts for starting and stopping both sides

The normal server and release archives must continue using the known-good map
until an area is approved.

## Tooling Prerequisites

Before editing terrain:

- Identify a compatible OpenRSC/RSC landscape editor or map decoder.
- Confirm which archive is authoritative for server collision and which is
  authoritative for client rendering.
- Determine how `Custom_Landscape.orsc`, map archives, and location definitions
  interact.
- Prove a no-op export/import round trip produces an equivalent map.
- Document chunk coordinates, planes, archive naming, and rebuild commands.
- Keep original chunk exports and checksums for restoration.
- Prefer a reviewable intermediate representation if the available tools
  support one.

If an existing editor cannot safely round-trip the current custom landscape,
the next task is to build or adapt extraction and import tooling before any
large expansion begins.

## Validation Requirements

Every terrain change should verify:

- the client and server load the same terrain
- floors, walls, roofs, elevations, and transitions render correctly
- collision and projectile clipping match visible barriers
- ladders, stairs, doors, teleports, and respawns remain functional
- no unintended route enters restricted areas
- no reachable tile exits the authored map into invalid space
- NPCs and resources spawn on reachable, appropriate tiles
- ranged and Magic combat do not gain unintended safespots
- existing quest and travel coordinates still resolve
- unrelated neighboring chunks are byte-identical unless intentionally changed

For the Wilderness expansion, also verify Wilderness level, PvP behavior,
teleport restrictions, death rules, and any other coordinate-derived
Wilderness logic across the full new area.

## Milestones

1. Terrain tool and archive format confirmed.
2. Isolated terrain-test server and client launch successfully.
3. One harmless test chunk completes a reversible edit cycle.
4. Cosmic Altar path completes terrain-only and static-shimmer prototypes.
5. Heroes' Guild basement broad pass is playable.
6. Heroes' Guild basement receives field revisions and final content.
7. Northern Wilderness boundary and access audit is complete.
8. Sinclair Mansion and every other northern enclave have a documented
   preserve, relocate, incorporate, or replace decision.
9. Any relocatable special-purpose area completes its scripted access and
   recovery migration.
10. Northern Wilderness terrain is divided into manageable implementation
   sectors.
11. Each sector is built, explored, revised, and approved independently.
12. The complete expansion passes boundary, traversal, combat, and Wilderness
   rule validation before entering the normal game.
