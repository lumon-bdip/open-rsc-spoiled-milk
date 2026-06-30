# Legacy Limits Audit

This audit tracks old RSC-era size limits that can turn into remaster bugs as
Spoiled Milk raises draw distance, object density, bank size, UI scope, and
renderer workload. Use it when a crash, disappearing visual, packet desync, or
strange wraparound looks like a byte/short-era ceiling rather than normal game
logic.

## Current Priority

1. Widen custom movement update counts, or chunk them safely, before dense
   areas can exceed `255` moving players or NPCs in a single update.
2. Convert remaining client-side entity/object/ground-item fixed arrays to
   growable storage, or add explicit telemetry and graceful dropping where the
   cap is intentional.
3. Keep old-client packet formats constrained by `ClientLimitations`; do not
   widen authentic payload fields unless every matching old client parser is
   handled.
4. Add limit telemetry for renderer, scene, UI command capture, and packet
   buffers so testers report "limit reached" instead of discovering freezes.

## Recently Fixed

| Area | Former risk | Current direction |
| --- | --- | --- |
| Bank item visuals | Bank entries could overflow a byte-sized client display path and wrap item visuals around after `255` entries. | Custom client/server bank paths now support wider counts. Keep legacy client compatibility in `ClientLimitations`. |
| Scene sprite draw slots | Dense object placement could overflow the scene sprite/pick model backing arrays and freeze the client. | `Scene.drawSprite` now grows its sprite arrays and `RSModel` backing storage dynamically. |
| Client loop crash logging | Some renderer/client-loop failures lost useful cause chains. | `RSRuntimeError` now preserves the wrapped cause and the client loop shuts down the stale connection after failure. |

## Highest-Risk Remaining Limits

### Custom movement packet count

Files:

- `server/src/com/openrsc/server/net/rsc/generators/impl/PayloadCustomGenerator.java`
- `Client_Base/src/orsc/PacketHandler.java`

Current shape:

- Server writes custom movement update player and NPC counts with
  `writeByte((byte) movement.players.size())` and
  `writeByte((byte) movement.npcs.size())`.
- Client reads both counts with `getUnsignedByte()`.

Risk:

- More than `255` visible/moving players or NPCs in one update will wrap the
  count and desynchronize parsing of the rest of the packet.
- This is especially relevant now that render distance and dense NPC/object
  placement are alpha test targets.

Recommended fix:

- Add a remaster/custom payload revision that uses unsigned short counts, or
  chunk movement updates into multiple packets.
- Keep old/authentic payloads byte-sized for old clients.
- Add a server-side guard that logs and refuses to emit a wrapped count.

### Client visible entity arrays

File: `Client_Base/src/orsc/mudclient.java`

Current fixed arrays include:

- `knownPlayers`, `players`, `npcs`, `npcsCache`: `500`
- `playerServer`: `4000`
- `npcsServer`: `5000`
- server-side known-player arrays in
  `server/src/com/openrsc/server/model/entity/player/Player.java`: `500`

Risk:

- Dense areas or larger visibility windows can exceed client-side visible
  entity slots before the server-side world itself is overloaded.
- Packet counts may fail first, but after those are widened these arrays become
  the next ceiling.

Recommended fix:

- Convert visible player/NPC client arrays to growable lists or capacity-managed
  arrays.
- Keep stable server-index lookup separate from visible render order.
- Add F6 expanded telemetry for visible players, visible NPCs, and capacity
  headroom.

### Game object instance capacity

File: `Client_Base/src/orsc/mudclient.java`

Current shape:

- `GAME_OBJECT_INSTANCE_CAPACITY = WALL_OBJECT_KEY_BASE`
- `WALL_OBJECT_KEY_BASE = 20000`
- Several parallel arrays store object instance ids, positions, directions,
  models, materialization flags, and state.

Risk:

- This is high enough for normal play, but world-edit sessions and denser
  remaster areas can reach it.
- The cap is tied to pick-index/key encoding, not just storage, so increasing it
  blindly can collide with wall-object and ground-item pick ranges.

Recommended fix:

- Replace pick-index ranges with explicit typed pick records.
- Then move object instances to growable storage.
- Until then, add a visible/logged warning when `canMaterializeGameObject`
  returns false because the cap was reached.

### Ground item arrays

File: `Client_Base/src/orsc/mudclient.java`

Current fixed arrays include:

- `groundItemID`, `groundItemX`, `groundItemZ`, `groundItemHeight`,
  `groundItemNoted`, and `groundItemRenderStackIndex`: `5000`
- `groundItems` is already an `ArrayList`, but it mirrors fixed-array state.

Risk:

- Mass drops, loot-share events, debug spawning, or large visibility can exceed
  `5000`.
- Some rendering/menu paths still assume array indexes are valid.

Recommended fix:

- Promote ground item storage to a list of value objects.
- Keep a compact per-frame render order list.
- Add a hard telemetry line for skipped/dropped ground items before removing the
  fixed arrays.

### Renderer 2D command capture caps

File: `Client_Base/src/orsc/graphics/two/GraphicsController.java`

Current caps:

- `MAX_RENDERER_2D_SPRITE_COMMANDS = 4096`
- `MAX_RENDERER_2D_TEXT_COMMANDS = 4096`
- `MAX_RENDERER_2D_PRIMITIVE_COMMANDS = 4096`
- `MAX_RENDERER_2D_ROTATED_SPRITE_COMMANDS = 256`
- `MAX_RENDERER_2D_CIRCLE_COMMANDS = 512`

Risk:

- These caps usually degrade rather than crash: UI or overlay commands may be
  skipped once capture overflows.
- Dense nameplates, debug overlays, minimap/UI work, or future particle-like 2D
  effects could trigger them.

Recommended fix:

- Convert command capture to growable lists with per-frame reserve sizing, or
  keep caps but make overflow visible in expanded F6.
- Treat rotated sprites as the first likely problem because the cap is only
  `256`.

### Network send buffer

File: `Client_Base/src/orsc/net/Network_Socket.java`

Current shape:

- Client socket send buffer is a circular `byte[5000]`.
- It throws `IOException("buffer overflow")` when the write pointer catches the
  read pointer.

Risk:

- Larger custom packets, rapid command bursts, debug world editing, or packet
  format widening can trip the buffer.

Recommended fix:

- Replace the fixed circular byte buffer with a growable queue or larger bounded
  buffer with telemetry.
- If kept bounded, report the packet type/size that caused back-pressure.

### Incoming packet buffer

File: `Client_Base/src/orsc/PacketHandler.java`

Current shape:

- `packetsIncoming = new RSBuffer_Bits(30000)`.

Risk:

- Bigger remaster packets can exceed this even when the server payload is valid.

Recommended fix:

- Audit `RSBuffer_Bits` growth behavior.
- Prefer packet-size checked growth over silently assuming `30000` remains
  enough.

## Medium-Risk Limits

### Shops

Files:

- `server/src/com/openrsc/server/net/rsc/generators/impl/PayloadCustomGenerator.java`
- `Client_Base/src/orsc/PacketHandler.java`
- `Client_Base/src/orsc/mudclient.java`

Current shape:

- Shop open count is written as a byte and read as an unsigned byte.
- Client storage has `256`-slot shop arrays, while the active shop UI appears to
  reset/render a smaller visible range.

Risk:

- Oversized future shops could wrap or silently omit entries.

Recommended fix:

- Keep player-facing shop pages small.
- If large stores are desired, add paged shop payloads instead of one giant
  byte-count packet.

### Social, clan, and party arrays

Files:

- `Client_Base/src/orsc/graphics/gui/SocialLists.java`
- `Client_Base/src/com/openrsc/interfaces/misc/clan/Clan.java`
- `Client_Base/src/com/openrsc/interfaces/misc/party/Party.java`
- `Client_Base/src/orsc/PacketHandler.java`

Current fixed arrays:

- Friends: `200`
- Ignore: `100`
- Clan names/ranks/online state: `200`
- Party names/ranks/online state: `25`
- Party status arrays: `99`

Risk:

- Social list limits are partly intentional, but the client has mixed signed
  byte and unsigned byte count handling.
- If the server allows a count above the client array, the client can write past
  local storage.

Recommended fix:

- Leave intentional social caps as gameplay limits if desired.
- Add count clamping plus protocol validation.
- Use unsigned reads for counts unless negative values are deliberately encoded.

### Character overlays and temporary combat effects

File: `Client_Base/src/orsc/mudclient.java`

Current caps:

- Chat/dialog bubbles, item bubbles, and health bars: `150`
- Teleport bubbles: `50`
- Queued projectile effects: `64`
- Queued combat effects: `64`
- Detached combat/screen effects: `32`

Risk:

- Dense combat or future visual effects can drop overlays or overwrite older
  effects.
- These are often acceptable if deliberate, but they should be visible in debug
  telemetry.

Recommended fix:

- Keep bounded queues for effects where visual dropping is acceptable.
- Add per-frame dropped-count telemetry.
- Convert health/dialog overlays to growable storage if large multiplayer scenes
  become a goal.

### Model and world build capacities

Files:

- `Client_Base/src/orsc/mudclient.java`
- `Client_Base/src/orsc/graphics/three/World.java`
- `Client_Base/src/orsc/graphics/three/ModelFileManager.java`

Current caps:

- `SCENE_MODEL_CAPACITY = 25000`
- `SCENE_POLYGON_CAPACITY = 120000`
- `MODEL_BUFFER_CAPACITY = LOCAL_TILE_COUNT * LOCAL_TILE_COUNT * 4`
- `MODEL_SPLIT_VERTEX_LIMIT = 512`
- `ModelFileManager` model file table: `5000`

Risk:

- Larger scene depth or generated/remastered geometry could hit these after the
  sprite slot fix.
- `MODEL_SPLIT_VERTEX_LIMIT` may be a performance/tooling choice, not a bug.

Recommended fix:

- Add telemetry for scene models, polygons, model buffer use, and split counts.
- Grow or chunk scene/model storage only where telemetry proves pressure.

## Protocol-Bound Legacy Limits

These limits should not be widened blindly because older clients are still
allowed to connect.

Use `server/src/com/openrsc/server/net/rsc/ClientLimitations.java` and the
custom-client login limit reporting in
`server/src/com/openrsc/server/net/rsc/LoginPacketHandler.java` as the ownership
boundary.

Known legacy capability fields include:

- maximum item, NPC, scenery, animation, boundary, roof, texture, tile, quest,
  spell, prayer, skin/hair/clothing color, projectile sprite, and bank ids
- maximum friends and ignore entries
- maximum server id

Rule:

- Authentic payload generators and parsers stay compatible with their client
  versions.
- Remaster/custom payloads can use wider fields, but the client and server must
  be changed together and preferably advertised as a capability/version bump.

## Probably Intentional Or Format-Bound

These showed up in searches but should not be treated as immediate bugs:

- BZip/archive tables with `256`, `257`, `258`, `4096`, or `18002` entries.
- ISAAC and CRC tables with `256` entries.
- Trig tables with fixed sine/cosine lookup sizes.
- Palette/color lookup tables where the source data is palette-indexed.
- Message history of `100` lines unless player-facing history is expanded.
- Short local walk-step deltas in old movement packets, unless pathing is
  redesigned.
- Inventory, trade, and duel slot counts where the cap is deliberate gameplay
  design.

## Next Implementation Recommendation

Start with the custom movement packet count.

Reason:

- It is the clearest remaining byte-sized limit that conflicts with remaster
  draw distance and dense-area testing.
- If it fails, it can desync packet parsing rather than just drop a visual.
- It has a clean client/server pair in the custom payload path, so the old-client
  compatibility blast radius is manageable.

Suggested implementation shape:

1. Add a new custom movement payload version or feature flag.
2. Write player and NPC movement counts as unsigned shorts for remaster clients.
3. Read the same counts as unsigned shorts in the Spoiled Milk client.
4. Log and cap old-byte movement updates for older clients instead of allowing
   wraparound.
5. Add expanded F6/server telemetry for movement players/NPCs sent, skipped, and
   max observed.

## Useful Audit Searches

```bash
rg -n "new (byte|short|int|long|boolean|String)\\[[0-9]+\\]|CAPACITY|writeByte\\(\\(byte\\)" Client_Base/src server/src server/plugins -g '*.java'
rg -n "getUnsignedByte\\(\\)|getByte\\(\\)|readUnsignedByte\\(\\)|readByte\\(\\)|writeByte\\(" Client_Base/src/orsc/PacketHandler.java server/src/com/openrsc/server/net/rsc -g '*.java'
rg -n "ClientLimitations|maxBankItems|maxFriends|maxIgnore|maxServerId" server/src/com/openrsc/server/net/rsc -g '*.java'
```
