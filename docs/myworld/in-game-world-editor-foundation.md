# In-game world editor: foundation and entity tools

Status: the inspection foundation is complete. Scenery and NPC editing delegate
to the established administrator commands. Terrain painting, undo/redo, publish,
and terrain patch replay remain intentionally unavailable.

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

## UI contract

The movable desktop overlay exposes mutually exclusive Navigate, Inspect,
Terrain, Scenery, and NPC tabs. Navigate continuously reports the player,
last-clicked tile, and stored inactive brush positions. Its coordinate teleport
and click-teleport preference use existing staff-authorized movement and are
active only in Navigate. Inspect restores ordinary walking and is the only mode
that adds terrain, boundary, scenery, and NPC inspection entries. It displays
the last authoritative result. Every target also has a right-click copy action,
and Inspect can copy its last successful result. Copies remain local and seed
the matching editor selection. Terrain snapshots use player-facing
coordinates, named planes, floor color/texture semantics, normalized north/east/
diagonal wall definitions, and derived collision while retaining raw archive
fields in the copied snapshot. Object and NPC results are resolved against the
live authoritative world and labelled `runtime-authoritative`.

Terrain remains a read-only placeholder. Scenery exposes an object-definition
selector with resolved name and Place, Rotate, and Remove tools. These tools are
limited to type-0 scenery; boundary walls remain inspection-only. NPC exposes a
definition selector with resolved name, a roam radius from 0 through 64, and
Place and Remove tools. Active tools add low-priority-number context actions so
one click performs one operation while the editor tab is selected. They invoke
the existing `aobject`, `rotateobject`, `robject`, `cnpc`, and `rpc`
administrator commands, retaining their authorization, validation, runtime
registration, and pending-edit behavior. `Save queued edits` invokes the
existing `saveworldedits` command. Undo and redo are deliberately deferred until
the complete editing workflow exists; no partial command-queue undo semantics
are introduced here.

## Future patch/recovery decision

The future durable format will be an append-only, versioned patch journal, not
a rewritten ZIP as the editing working copy. Each transaction will carry an
opaque transaction ID, editor identity, base archive digest, ordered before/after
raw tile records or entity loc records, and a commit marker. Startup recovery
may replay committed transactions only. Export will materialize a temporary
archive, byte-verify it, then atomically replace the destination. This decision
is documented now but no journal code exists in Phase 1.
