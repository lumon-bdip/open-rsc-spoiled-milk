# World Layer Capacity Exploration Plan

Status: discussion and architecture study

Branch: `docs/world-layer-capacity-exploration`

Started: 2026-07-17

## Purpose

This is a living planning document for deciding how Spoiled Milk should organize
future underground world space. The immediate concerns are that existing
underground content feels crowded and fragmented, some underground ladders act
as long-distance fast travel rather than geographic vertical movement, and a
deeper category of underground content may be useful.

The study compares expanding within the current underground plane,
reorganizing existing underground content, reserving separated dungeon
regions, supporting true instances, and adding a true fifth plane. It is
intentionally discussion-first: the architecture should emerge from a series
of smaller decisions rather than being selected before the intended world
experience is clear.

## Scope Boundary

In scope:

- documenting the current world-coordinate and terrain-archive architecture;
- auditing capacity, compatibility assumptions, and content dependencies;
- building an underground-area inventory and entrance/exit graph;
- defining possible coordinate-allocation and entrance-correspondence rules;
- comparing architecture options and migration risks;
- recording owner decisions as the discussion progresses;
- planning private-server validation for a later implementation phase.

Not authorized by this plan:

- editing terrain archives;
- relocating existing map content;
- changing runtime client or server code;
- changing ladders, portals, teleports, respawns, quests, or placements;
- modifying player databases or live world data;
- deploying experimental map work to the public server.

Documentation may be revised as decisions are made. Implementation requires a
separately approved focused plan.

## Executive Finding

The first audit indicates that plane 3 is not close to exhausting its raw map
space. Its practical problem is fragmented allocation and distributed content
ownership rather than physical capacity.

Only about one fifth of the archived plane-3 sector grid shows varied terrain
or placement usage. More than 350 sectors appear blank or unallocated at this
level of inspection. These figures are not permission to build in those
sectors: apparently blank areas may still be buffers, inactive variants, or
targets of hard-coded scripts. They do show that a formal inventory and
allocation policy could unlock substantial existing capacity.

A provisional direction worth discussing is to reserve organized shallow- and
deep-underground districts within plane 3 before undertaking a true fifth-plane
compatibility project. This is not yet the chosen architecture.

## Current Coordinate Architecture

The world is represented primarily by a two-dimensional `(x, y)` coordinate.
The plane is encoded in the Y coordinate rather than stored as an independent Z
dimension:

| Plane | Current role | Logical world-Y band |
| --- | --- | ---: |
| 0 | Surface | `0-943` |
| 1 | First floor | `944-1887` |
| 2 | Second floor | `1888-2831` |
| 3 | Underground | `2832-3775` |

The central relationship is:

```text
plane = floor(worldY / 944)
baseY = worldY - (plane * 944)
```

This means that a nominally vertical surface-to-underground connection at
surface `(x, y)` would arrive near `(x, y + 2832)` on plane 3.

The four plane roles are enforced by more than naming convention:

- the server world loader has a four-floor load loop;
- shared floor calculations divide Y by `944`;
- generic stair and ladder movement assumes the existing plane cycle;
- client wall and roof model grids have four plane slots;
- surface scene construction handles upper floors differently from the
  standalone underground scene;
- membership-area checks repeat some surface rectangles across four planes;
- the in-game terrain editor accepts only planes `0-3`;
- World Builder and terrain-save validation currently understand only the
  existing four-plane layout.

The configured server `MAX_HEIGHT` being greater than `3776` does not make the
remaining coordinate range a fifth plane. The loader, client, editor, and plane
semantics still stop at four planes, and the remaining range is not a complete
944-tile band.

## Terrain Archive Organization

The current authoritative terrain is duplicated at:

- `server/conf/server/data/Custom_Landscape.orsc`
- `Client_Base/Cache/video/Custom_Landscape.orsc`

At the time of this audit, the two copies have the same SHA-256 digest:

```text
d50089fcc81d51aa461567f4416a8f1a329ed439bcf64606ca1441c600e7229b
```

There is also a differently hashed historical-looking archive under
`server/conf/server/defs/locs/`. No active loader, editor, import script, or
World Builder reference was found for it. It should be treated as a naming and
inventory hazard, not assumed to be authoritative or disposable.

The `.orsc` file is a ZIP archive whose entries use names such as:

```text
h{plane}x{sectorX}y{sectorY}
```

Each sector is `48 x 48` tiles. Each tile has a ten-byte terrain record covering
elevation, ground texture, overlay, roof, horizontal and vertical walls, and a
diagonal value. The documented coordinate conversion is:

```text
sectorX = floorDiv(worldX + 2304, 48)
sectorY = floorDiv(baseY + 1776, 48)
localX  = floorMod(worldX + 2304, 48)
localY  = floorMod(baseY + 1776, 48)
```

The archive currently has 1,771 entries and no duplicate entry names. The
observed unique sector ranges are:

| Plane | Entries | Sector-X range | Sector-Y range |
| --- | ---: | --- | --- |
| 0 | 445 | `48-69` | `36-57` |
| 1 | 444 | `47-68` | `36-57` |
| 2 | 441 | `48-68` | `37-57` |
| 3 | 441 | `48-68` | `37-57` |

Sector dimensions do not divide evenly into the 944-tile logical plane height,
so archive border sectors must not be mistaken for additional playable plane
space.

## Preliminary Plane-3 Capacity Audit

The plane-3 archive is a complete 21-by-21 sector rectangle containing 441
sectors. A preliminary terrain-content scan found:

- 83 sectors with more than one distinct terrain tile record;
- 358 sectors that appear uniform or nearly blank;
- approximately 84 plane-3 sectors touched by a conservative scan across the
  available location JSON files.

The terrain and placement results closely correspond. Large apparent gaps
include northern rows, several central rows, much of the eastern side, and many
smaller internal gaps between legacy complexes.

This is only a capacity signal. Before reserving coordinates, the inventory
must also check:

- inactive and alternate map-data variants;
- all base and conditional placement files;
- MyWorld addition and removal overlays;
- Java coordinate literals and rectangular area checks;
- quest stages and recovery locations;
- teleport, ladder, portal, and door destinations;
- saved-player locations;
- intentionally blank isolation and renderer-loading buffers;
- sector-boundary and plane-boundary behavior.

## Content Layers Affected by Relocation

Moving an underground area is not equivalent to copying its terrain. A complete
migration may need to update:

1. Terrain, overlays, roofs, walls, elevation, and collision.
2. Scenery and boundary placements.
3. NPC spawn points and roaming bounds.
4. Ground-item placements and respawns.
5. Central object telepoints.
6. Hard-coded ladder, door, portal, spell, boat, and transport destinations.
7. Quest coordinate checks, cutscenes, failure paths, and stage recovery.
8. Minigame entry, exit, logout, and emergency recovery behavior.
9. Death and respawn behavior.
10. Players whose saved coordinates still point to the old area.
11. Client/server terrain parity and World Builder compatibility.

Current world population is assembled from base scenery, boundary, item, and
NPC files plus conditional feature files and MyWorld overlays. World Builder's
current authored bundle covers terrain and the supported MyWorld scenery/NPC
overlays, but it does not own every legacy placement file or coordinate embedded
in Java. It therefore cannot perform a complete legacy-dungeon migration by
itself.

## Entrance and Exit Behavior

The existing transition graph mixes geographic vertical movement and explicit
long-distance travel.

Generic stairs and ladders can use the shared floor calculation. That logic
preserves the normalized X/Y position while moving through the established
surface, upper-floor, and underground relationships. Many exceptions bypass
that behavior and teleport to explicit coordinates.

The central `ObjectTelePoints.xml` currently contains only 20 connections. The
audit found seven cross-plane connections and numerous same-plane transitions,
including many whose endpoints are far apart. This XML is only one part of the
graph; ladder plugins, quests, sewers, doors, portals, pools, boats, rings,
spells, minigames, and other content contain additional explicit edges.

Examples of the mixed semantics include:

- Surface `(499,469)` to underground `(499,3295)`. The underground normalized
  point is `(499,463)`, only six tiles from geographic alignment.
- Surface `(223,110)` to underground `(446,3368)`. The normalized underground
  point is `(446,536)`, hundreds of tiles from the entrance.
- Hard-coded ladder handling includes other distant surface-to-underground and
  underground-to-underground transitions.

An eventual entrance policy could classify edges as:

- **Vertical:** exact or near-exact normalized correspondence.
- **Regional:** located beneath the same surface territory, with an intentional
  offset to prevent overlap.
- **Transit:** deliberate fast travel whose transport role should be explicit.
- **Magical or exceptional:** geographic correspondence intentionally does not
  apply.

This classification is a discussion proposal, not a decided rule.

## Player Persistence, Death, and Recovery

Player X/Y coordinates are stored directly in the player database and restored
as the login location. The inspected load path does not automatically detect
that a dungeon has moved and redirect a saved player to its replacement.

The current MyWorld configuration uses surface respawn `(120,648)`. Ordinary
player death ultimately teleports the player to that configured location after
combat and drop processing. That reduces one form of underground stranding but
does not cover:

- a player logged out inside an area before it moved;
- reconnecting during a quest or minigame;
- quest-specific failure and recovery destinations;
- special emergency teleports;
- stale caches or state that reference the former area;
- safe recovery when a destination archive or placement set is absent.

Every future relocation needs an explicit old-area login redirect and a policy
for when that redirect can safely be removed.

## Client Loading and Legacy Compatibility

The custom protocol can numerically carry a plane value larger than three in
some fields, but that does not make a fifth plane compatible. Both client and
server contain structural four-plane assumptions.

A true fifth plane would at minimum require auditing or changing:

- server terrain loading and post-load compression loops;
- region bounds and maximum world height;
- shared floor and vertical-travel calculations;
- membership and other repeated area checks;
- client wall and roof model arrays;
- surface/upper/underground scene construction and roof rules;
- terrain archive availability for plane 4;
- in-game editor plane validation and naming;
- World Builder import/export validation;
- world-info, movement, teleport, and coordinate packet paths;
- private and release client tests;
- every supported legacy-client parser and renderer.

Unmodified legacy clients should be presumed incompatible with a fifth plane
until proven otherwise. A plane-3 static region is much more likely to remain
compatible because it uses the established coordinate and archive contract.

The recent hard-area-load scenery work also makes distant-transition testing
important. Any later map experiment should validate full object and wall
baselines after teleport, death, login, logout, reconnect, and rapid travel
between dense and quiet areas.

## World Builder Compatibility

World Builder works from isolated copies and currently targets the existing
`.orsc` MyWorld layout. Its terrain addressing and validation understand planes
zero through three. It can support formally reserved static regions on plane 3
without changing the archive format.

A fifth plane requires a prior World Builder/tooling project. Reorganizing
existing legacy content also requires a broader manifest or migration audit
because the current authored bundle is not authoritative for all base and
feature placements or Java logic.

Any later implementation should retain the existing safeguards:

- edit isolated working copies rather than active archives;
- require identical client/server terrain before import;
- require compatibility fingerprints;
- import only while the private target is offline;
- compare unrelated sectors and placement records for unintended changes.

## Architecture Options

### Option 1: Add a True Fifth Plane

Concept:

- Add plane 4 using another 944-tile Y band, nominally beginning at Y 3776.
- Give deep underground a distinct engine-level plane identity.

Benefits:

- clear semantic separation from the existing underground layer;
- approximately another full logical plane of coordinate space;
- no need to interleave new deep content around old plane-3 complexes;
- potential foundation for additional future layers.

Costs and risks:

- broad server, client, renderer, editor, World Builder, archive, limit, and
  regression-test changes;
- generic vertical movement currently encodes a four-plane cycle and would need
  new semantics;
- plane 3 has special underground rendering behavior that would not
  automatically apply to plane 4;
- unmodified legacy clients are unlikely to load or render it correctly;
- packet fields accepting the number do not guarantee end-to-end support;
- it adds engineering capacity without fixing the undocumented allocation and
  transition graph on plane 3.

Current assessment: technically possible as a dedicated compatibility project,
but disproportionate as the first response to present-day crowding.

### Option 2: Allocate Deep Regions Within Plane 3

Concept:

- Reserve large currently unused plane-3 rectangles for future deep-underground
  content.
- Treat "deep" as a documented world category rather than a separate engine
  plane.

Benefits:

- reuses established client, server, archive, editor, and World Builder paths;
- likely preserves legacy-client access;
- uses abundant apparent free capacity;
- can be adopted incrementally without moving existing content first;
- supports both geographically aligned shallow spaces and intentionally remote
  deep districts.

Costs and risks:

- depends on a trustworthy coordinate and reference inventory;
- needs buffers and reservation ownership to prevent renewed fragmentation;
- deep destinations will not all correspond directly to surface coordinates;
- care is needed to prevent accidental walking or loading between unrelated
  nearby regions;
- remains one global coordinate space rather than true instancing.

Current assessment: strongest near-term candidate, subject to the desired
meaning of "deep underground."

### Option 3: Reorganize or Partition Existing Plane-3 Content

Concept:

- Establish coherent plane-3 zones and selectively move existing complexes
  into them.
- Potentially restore geographic correspondence for suitable entrances.

Benefits:

- reduces historical fragmentation;
- makes future capacity easier to understand;
- can group related surface and underground areas;
- may clarify which ladders are vertical and which are transport.

Costs and risks:

- high content-migration risk despite requiring little engine-format change;
- quests, NPCs, items, scenery, collision, teleports, saved players, and
  recovery paths can all retain old coordinates;
- mature quest-heavy areas have a much larger dependency surface than new or
  unreleased content;
- wholesale reorganization could spend substantial effort without improving
  gameplay proportionally.

Current assessment: useful selectively after the inventory exists. Low-risk,
custom, or clearly misplaced regions would be better early candidates than
established quest complexes.

### Option 4: Separated or Instanced Dungeon Regions

This option has two materially different interpretations.

#### Static isolated regions

- Reserve disconnected blocks on plane 3.
- Use explicit entrances and exits.
- Keep enough unused space between unrelated complexes.

This is compatible with the current architecture and is effectively a more
formal version of Option 2.

#### True player- or party-specific instances

- Allow multiple independent copies of the same dungeon to exist at once.
- Keep players, NPCs, items, scenery state, and collision isolated by instance.

The current coordinate model cannot distinguish two copies at the same X/Y.
World regions and entity registries are global by coordinate. Genuine
instancing would require an additional instance identity or a comparable
server-world abstraction, plus visibility, persistence, packet, logout,
reconnect, cleanup, and client-loading rules.

Disjoint coordinate blocks could simulate a limited number of copies, but that
consumes map space and is not a scalable instance architecture.

Current assessment: static isolated regions are immediately viable. True
instancing should be treated as a separate engine project if it is actually a
gameplay goal.

## Provisional Hybrid Direction for Discussion

The initial audit supports exploring this sequence without yet adopting it:

1. Preserve established, quest-heavy legacy complexes initially.
2. Inventory all current plane-3 areas and every known entrance and exit.
3. Divide plane 3 into documented allocation districts.
4. Reserve geographically aligned shallow-underground space beneath important
   surface regions where feasible.
5. Reserve one or more large contiguous blocks as deep underground.
6. Connect deep districts using explicit descents or transit edges rather than
   implying that every destination sits immediately below its entrance.
7. Classify existing fast-travel ladders before deciding whether to preserve,
   replace, or relocate them.
8. Consider selective relocation only after dependencies and saved-player
   recovery are documented.
9. Keep a true fifth plane as a possible future custom-client compatibility
   project rather than assuming it is required now.

## Discussion Modules

The remaining decisions are deliberately divided so they can be handled one at
a time.

### Module A: Meaning of Deep Underground

Decide whether deep underground is:

- one connected explorable underworld;
- several separate deep districts with a shared visual or narrative identity;
- simply an allocation category for unrelated deep dungeons;
- some combination of connected hubs and isolated branches.

This decision affects how much contiguous space must be reserved and whether a
global entrance hierarchy is needed.

### Module B: Client Compatibility Target

Decide whether all future underground content must remain accessible through
legacy clients, or whether a future Spoiled Milk-only layer is acceptable.

This is the most important constraint on whether a fifth plane is worth deeper
engineering investigation.

### Module C: Geographic Correspondence

Decide how strongly underground entrances should correspond to the surface:

- exact normalized X/Y whenever possible;
- a small tolerance around the surface point;
- placement somewhere under the same named surface region;
- thematic association only for deep or exceptional destinations.

The result should also define when an entrance may be labeled transit or
magical rather than vertical.

### Module D: Existing Content and Fast Travel

Decide:

- whether established quest dungeons are candidates for relocation;
- whether only custom, unreleased, or low-dependency regions should move;
- whether distant ladder fast travel is a valued convenience;
- whether transport ladders should use a different object or presentation;
- whether underground-to-underground shortcuts should form a deliberate
  network.

### Module E: Static Separation Versus True Instancing

Decide whether the goal is isolated static dungeon blocks or genuine private
player/party copies. The latter is not merely a coordinate-allocation policy
and would need a separate architecture study.

### Module F: Allocation Policy

After Modules A-E, define:

- sector-sized reservation units;
- district boundaries and ownership;
- minimum buffers between unrelated areas;
- shallow, deep, transport, quest, expansion, and experimental categories;
- reserved future-growth space for every complex;
- rules for crossing sector and plane boundaries;
- a machine-readable allocation registry or manifest, if desired.

### Module G: Migration and Validation

After an architecture is selected, define the phased migration order and a
private-server test matrix covering terrain parity, collision, placements,
entrances, exits, quests, death, login, logout, reconnect, distant teleports,
client scene baselines, and rollback.

## Open Owner Questions

1. Is deep underground intended to be one connected underworld or an
   organizational category for separate dungeons?
2. Must all new underground areas remain usable by legacy clients?
3. Should geographic correspondence be exact, regional, or conditional on
   entrance type?
4. Are existing quest dungeons eligible for relocation?
5. Is true player/party instancing a desired feature, or is static isolation
   sufficient?
6. Should existing long-distance ladder travel be preserved, re-presented as
   transportation, or gradually removed?

## Living Inventory: Pending Discussion and Audit

The completed planning document will include an underground-area inventory
with at least these fields:

| Field | Purpose |
| --- | --- |
| Area ID and name | Stable reference independent of coordinates |
| Current bounds | Exact terrain and placement extent |
| Plane-3 sectors | Capacity and adjacency tracking |
| Surface association | Geographic or narrative parent region |
| Area category | Shallow, deep, transit, quest, minigame, or exceptional |
| Entrances and exits | Directed edges, requirements, and recovery paths |
| Terrain source | Archive sectors and fingerprint |
| Placement sources | Base, feature, and MyWorld files |
| Script owners | Plugins, quests, commands, and coordinate checks |
| Persistence risks | Saved player, quest cache, minigame, or item state |
| Legacy-client status | Known compatibility requirements |
| Migration eligibility | Preserve, review, candidate, or prohibited |
| Growth reservation | Buffer and future expansion needs |

The entrance/exit graph should record directed edges rather than assuming that
every route is reversible. Each edge should record source, destination,
interaction object, entrance class, requirements, quest state, one-way
behavior, failure destination, death/reconnect behavior, and implementation
owner.

## Later Private-Server Validation Outline

No map experiment is authorized yet. When one is eventually approved, a
private environment should validate at least:

1. Client and server terrain archives begin and end identical.
2. Only approved sectors and placement records change.
3. Terrain appearance, roofs, walls, elevation, and collision match the design.
4. Every entrance and exit works in both intended directions.
5. Requirements and quest gates remain correct.
6. NPCs, scenery, boundaries, and ground items load after walking and hard
   teleports.
7. Death, emergency teleport, logout, login, and reconnect recover safely.
8. Players saved at legacy coordinates follow the approved migration path.
9. Relevant quests and minigames pass their entry, progress, failure, and
   completion flows.
10. Legacy clients behave according to the selected compatibility target.
11. World Builder round trips the approved content without touching unrelated
    data.
12. A rollback restores terrain, placements, scripts, and copied test data.

## Related Documentation

- `docs/myworld/info/in-game-world-editor-foundation.md`
- `docs/myworld/in-progress-work-plans/terrain-expansion-plan.md`
- `docs/myworld/in-progress-work-plans/in-game-world-editor-plan.md`
- `docs/myworld/in-progress-work-plans/standalone-world-builder-plan.md`
- `docs/myworld/in-progress-work-plans/legacy-limits-audit.md`
- `docs/workspaces/README.md`

## Decision Log

| Date | Decision | Status |
| --- | --- | --- |
| 2026-07-17 | Begin a discussion-first architecture and capacity study; documentation only. | Confirmed |
| 2026-07-17 | Divide the remaining design into smaller discussion modules before choosing an architecture. | Confirmed |

## Next Discussion

The owner will select the first discussion module. No architecture should be
marked chosen and no implementation plan should be prepared until the relevant
modules have been resolved.
