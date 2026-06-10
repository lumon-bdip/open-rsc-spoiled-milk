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

The complete Cosmic Altar specification now lives in the
[project roadmap](roadmap.md#1-minor-terrain-and-cosmic-altar). Keep that
section as the authoritative source for its coordinates, route geometry,
visual treatment, animation targets, prototype order, and completion gate.

### Long Term: Northern Wilderness Expansion

Extend the Wilderness eastward across the northern map until it reaches the
region north of the Tree Gnome Stronghold.

This is not only a visual extension. It requires world-boundary design,
Wilderness-level behavior, travel routes, encounter planning, and strong
protection around existing regions.

Build and release this expansion in waves based on RuneScape's established map
grid. Each wave should contain a manageable set of neighboring grid sections.
The selected sections must match every existing landscape edge while also
following a coherent plan for the eventual complete northern Wilderness.
Published waves must have deliberate, safe boundaries and cannot depend on a
future wave to prevent access into unfinished or restricted areas.

Desired result:

- Fill missing northern terrain with a natural continuation of Wilderness
  geography.
- Create multiple recognizable regions rather than one large empty field.
- Support future PvM clusters, gathering areas, ruins, caves, and landmarks.
- Give each released wave immediate gameplay value through enemies, gathering,
  exploration, or useful routes rather than publishing empty terrain.
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

Original enemy families are desired, but terrain work should not depend on
unavailable sprites. Suitable existing enemies and variants may populate early
waves when they fit the region. New art can be commissioned, adapted from
compatible sources, or created manually and introduced in later waves or
population revisions. Any original sprite must match RuneScape Classic's
low-resolution, chunky early paint-program style; generic modern pixel art and
smoothed AI-generated imagery are not adequate substitutes.

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
