# In-game world editor: Phase 0/1 foundation

Status: read-only foundation. Painting, placement, deletion, rotation, undo/redo,
save, publish, and patch replay are intentionally unavailable.

## Raw terrain contract

Both custom clients and the server consume `Custom_Landscape.orsc`, a ZIP whose
sector entries are named `h{plane}x{sectorX}y{sectorY}`. A sector is 48 by 48
tiles in X-major order (`x * 48 + y`). Each tile is exactly ten big-endian
bytes:

| Offset | Width | Field | Raw range |
|---:|---:|---|---:|
| 0 | 1 | elevation | unsigned 0..255 |
| 1 | 1 | ground texture | unsigned 0..255 |
| 2 | 1 | ground overlay | unsigned 0..255 |
| 3 | 1 | roof texture | unsigned 0..255 |
| 4 | 1 | horizontal wall | unsigned 0..255 |
| 5 | 1 | vertical wall | unsigned 0..255 |
| 6 | 4 | diagonal wall encoding | signed 32-bit integer, preserved byte-exactly |

Diagonal values `1..11999` mean definition `raw - 1`, NW-SE. Values
`12001..23999` mean definition `raw - 12001`, NE-SW. Zero means none; 12000
and values outside those ranges are reserved/invalid for editor authoring.

World Y encodes the plane in 944-tile bands. The request must satisfy
`floorDiv(worldY, 944) == plane`. Archive coordinates are:

```text
baseY   = worldY - plane * 944
sectorX = floorDiv(worldX + 2304, 48)
sectorY = floorDiv(baseY + 1776, 48)
localX  = floorMod(worldX, 48)
localY  = floorMod(baseY, 48)
```

The authoritative response reports both raw archive fields and the current
runtime tile's derived traversal mask/projectile flag. Collision is derived
from overlay definitions, horizontal/vertical walls, diagonal walls, runtime
scenery, and neighboring tiles; it must never be inferred from one raw field.

## Protocol and authorization

- Client to server opcode 152 and server to client opcode 151 are reserved for
  one versioned editor envelope. Version 1 has only close, terrain inspect/copy,
  scenery inspect, and NPC inspect operations. It contains no mutation type.
- `allow_in_game_world_editor` defaults false and is explicitly false in both
  MyWorld configurations.
- `::worldeditormode` is server authoritative. It requires the gate, an
  administrator account, and the custom desktop-capable protocol.
- The server grants one active owner globally using an opaque random 64-bit
  session ID. Every request must carry the exact next sequence. Ownership is
  released on explicit close or player unregister/disconnect.
- Android never creates or opens the shell. Normal play receives no editor
  packets and adds no menu entries while the shell is closed.

## Read-only UI contract

The movable desktop overlay exposes mutually exclusive Navigate, Inspect,
Terrain, Scenery, and NPC tabs. Navigate continuously reports the player,
last-clicked tile, and stored inactive brush positions. Its coordinate teleport
and click-teleport preference use existing staff-authorized movement and are
active only in Navigate. Inspect restores ordinary walking and is the only mode
that adds terrain, boundary, scenery, and NPC inspection entries. It displays
the last authoritative result. Terrain snapshots show raw coordinates/fields
plus derived collision. The three editing tabs are explicit placeholders and
contain no duplicated inspection or mutation controls. Object and NPC results
are resolved against the live authoritative world and labelled
`runtime-authoritative`. A later persistence phase must distinguish base
archive, custom overlay, and draft origins before it can write.

## Future patch/recovery decision

The future durable format will be an append-only, versioned patch journal, not
a rewritten ZIP as the editing working copy. Each transaction will carry an
opaque transaction ID, editor identity, base archive digest, ordered before/after
raw tile records or entity loc records, and a commit marker. Startup recovery
may replay committed transactions only. Export will materialize a temporary
archive, byte-verify it, then atomically replace the destination. This decision
is documented now but no journal code exists in Phase 1.
