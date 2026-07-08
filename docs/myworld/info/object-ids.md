# Key Commands

These are dev-only commands for building scenery in-game and turning the result into official MyWorld map content.

- `::addobject <id>`: places a scenery object on your current tile and queues it for saving.
- `::addobject <id> <x> <y>`: places a scenery object at explicit coordinates and queues it for saving.
- `::r`: repeats your last successful scenery placement on your current tile and queues it for saving.
- `::repeatobject`: long-form alias for `::r`.
- `::removeobject`: removes the scenery object on your current tile and queues a persistent removal.
- `::removeobject <x> <y>`: removes scenery at explicit coordinates and queues a persistent removal.
- `::rotateobject`: rotates the scenery object on your current tile to its next direction and queues it for saving.
- `::rotateobject <x> <y>`: rotates scenery at explicit coordinates to its next direction and queues it for saving.
- `::rotateobject <x> <y> <direction>`: sets scenery at explicit coordinates to a specific direction, `0` through `7`, and queues it for saving.
- `::worldedits`: lists pending scenery edits that have not been written to disk yet.
- `::saveworldedits`: writes pending edits to `MyWorldSceneryLocs.json` and `MyWorldSceneryRemovals.json`.
- `::clearworldedits`: discards the pending save queue without reverting the live world.
- `::cnpc <id> <radius>`: spawns a respawning NPC on your current tile and queues that spawn for saving.
- `::cnpc <id> <radius> <x> <y>`: spawns a respawning NPC at explicit coordinates and queues that spawn for saving.
- `::rpc <npc_instance_id>`: removes a live NPC and queues its spawn point for persistent removal.
- `::coords`: prints your current coordinates.
- `::tile`: prints tile traversal/debug data for your current tile.
- `::cyclescenery`: cycles scenery IDs on your tile for visual browsing. Use carefully; it mutates the live tile while cycling.

Notes:
- This workflow currently persists scenery objects, which covers ore rocks and most placeable game objects.
- The same save workflow now persists NPC spawn points in `MyWorldNpcLocs.json`; NPC removals are written to `MyWorldNpcRemovals.json`.
- For quick area building, use `::addobject <id>` once, move to the next tile, then use `::r`.
- For enemies, use a radius that matches how far the NPC should wander from its spawn tile. A radius of `0` pins it to the tile.
- To replace existing scenery, use `::removeobject` first, then `::addobject <id>`. The last pending edit for a tile is what gets saved.
- Boundary/wall-object placement commands still exist, but they are not saved by `::saveworldedits` yet.
- `::clearworldedits` only clears the queued file changes. Restarting the server is the clean way to undo unsaved live edits.

# Object IDs

This is a builder-facing reference for object IDs used when placing content in-game.
It is intentionally organized by practical use so future categories can be added without digging through generated constants.

Primary sources:
- Mineable object behavior: `server/conf/server/defs/extras/ObjectMining.xml`
- Scenery constants: `server/src/com/openrsc/server/constants/SceneryId.java`
- Scenery models/names: `server/conf/server/defs/GameObjectDef.xml`
- Item names/overrides: `server/conf/server/defs/ItemDefsMyWorld.json`

## Mineable Ore And Rock Objects

Use these IDs with dev object placement commands such as `::addobject <id>` when building mining areas.
Most ore types have two IDs that point at the same visual model; either variant can be used for visual variety unless noted.

| Use | Scenery IDs | Constant names | Model(s) | Gives item | Level | XP | Respawn |
| --- | ---: | --- | --- | --- | ---: | ---: | ---: |
| Stone | 98, 99 | `ROCK_GENERIC`, `ROCK_GENERIC2` | `rocks1`, `rocks2` | 1299 `Stone` | 1 | 20 | 4 |
| Copper ore | 100, 101 | `ROCK_COPPER`, `ROCK_COPPER2` | `copperrock1` | 150 `Copper ore` | 8 | 70 | 4 |
| Iron ore | 102, 103 | `ROCK_IRON`, `ROCK_IRON2` | `ironrock1` | 151 `Iron ore` | 15 | 140 | 7 |
| Tin ore | 104, 105 | `ROCK_TIN`, `ROCK_TIN2` | `tinrock1` | 202 `Tin ore` | 1 | 70 | 4 |
| Mithril ore | 106, 107 | `ROCK_MITHRIL`, `ROCK_MITHRIL2` | `mithrilrock1` | 153 `Mithril ore` | 38 | 320 | 110 |
| Adamantite ore | 108, 109 | `ROCK_ADAMITE`, `ROCK_ADAMITE2` | `adamiterock1` | 154 `Adamantite ore` | 54 | 380 | 220 |
| Coal | 110, 111 | `ROCK_COAL`, `ROCK_COAL2` | `coalrock1` | 155 `Coal` | 22 | 200 | 25 |
| Gold nugget | 112, 113 | `ROCK_GOLD`, `ROCK_GOLD2` | `goldrock1` | 152 `Gold nugget` | 40 | 260 | 70 |
| Clay | 114, 115 | `ROCK_CLAY`, `ROCK_CLAY2` | `clayrock1` | 149 `clay` | 1 | 20 | 2 |
| Blurite ore | 176 | `ROCKS_BLURITE` | `bluriterock1` | 266 `blurite ore` | 10 | 70 | 510 |
| Silver nugget | 195, 196 | `ROCK_SILVER`, `ROCK_SILVER2` | `silverrock1` | 383 `Silver nugget` | 20 | 160 | 70 |
| Runite ore | 210, 211 | `ROCK_RUNITE`, `ROCK_RUNITE2` | `runiterock1`, `runiteruck1` | 409 `Runite ore` | 70 | 500 | 900 |
| Family Crest gold | 315 | `ROCKS_GOLD` | `goldrock1` | 690 `gold` | 40 | 260 | 70 |
| Gem rocks | 588 | `GEM_ROCKS` | `gemrock` | weighted uncut gem roll | 40 | 260 | 70 |
| Tutorial tin | 496 | `ROCKS_TIN_TUTORIAL` | `rocks1` | 202 `Tin ore` | 1 | 70 | 0 |
| Tourist Trap jail rocks | 1030 | `ROCKS` | `tinrock1` | 986 `Rocks` | 1 | 0 | 5 |
| Dragon sulfur | 1328 | `ROCK_DRAGON_SULFUR` | `copperrock1` | 3255 `Dragon sulfur` | 90 | 650 | 420 |

## Notes

- Silver and gold mining rewards are intentionally named `Silver nugget` and `Gold nugget` in MyWorld.
- Gem rocks are handled as a special case in `Mining.java`, not by `ObjectMining.xml`.
- `Family Crest gold` and `Tourist Trap jail rocks` are quest-specific entries. Avoid using them as normal resource nodes unless that behavior is desired.
- `depletion` exists in `ObjectMining.xml`, but the table above keeps only level, XP, and respawn because those are usually the values needed while placing objects.
