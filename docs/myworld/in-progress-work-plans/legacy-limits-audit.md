# Legacy Limits Audit

This audit tracks old RSC-era size limits that can turn into remaster bugs as
Spoiled Milk raises draw distance, object density, bank size, UI scope, and
renderer workload. Use it when a crash, disappearing visual, packet desync, or
strange wraparound looks like a byte/short-era ceiling rather than normal game
logic.

## Current Priority

1. Replace or instrument the fixed client network send and incoming packet
   buffers before larger custom payloads turn back-pressure into disconnects.
2. Separate game-object, wall-object, and ground-item picking from numeric key
   ranges before growable object storage can make those ranges overlap.
3. Keep old-client packet formats constrained by `ClientLimitations`; do not
   widen authentic payload fields unless every matching old client parser is
   handled.
4. Continue observing renderer 2D capacity in ordinary diagnostics, but do not
   grow current caps without a reproducible live overflow.

## Recently Fixed

| Area | Former risk | Current direction |
| --- | --- | --- |
| Bank item visuals | Bank entries could overflow a byte-sized client display path and wrap item visuals around after `255` entries. | Custom client/server bank paths now support wider counts. Keep legacy client compatibility in `ClientLimitations`. |
| Scene sprite draw slots | Dense object placement could overflow the scene sprite/pick model backing arrays and freeze the client. | `Scene.drawSprite` now grows its sprite arrays and `RSModel` backing storage dynamically. |
| Custom movement update counts | Player or NPC counts above `255` could wrap and desynchronize the packet. | The custom server writes 16-bit counts and the Spoiled Milk client reads them with `getShort()`. Keep the custom movement packet-shape guard in normal validation. |
| Client world-instance arrays | Initial player, NPC, game-object, wall-object, and ground-item arrays acted like hard visibility ceilings. | The client now expands the parallel arrays through capacity helpers before insertion. Numeric pick-key range ownership remains a separate risk. |
| Client loop crash logging | Some renderer/client-loop failures lost useful cause chains. | `RSRuntimeError` now preserves the wrapped cause and the client loop shuts down the stale connection after failure. |

## Highest-Risk Remaining Limits

### Game object pick-key ranges

File: `Client_Base/src/orsc/mudclient.java`

Current shape:

- Game-object storage starts at `GAME_OBJECT_INSTANCE_INITIAL_CAPACITY =
  WALL_OBJECT_KEY_BASE` but now grows when required.
- `WALL_OBJECT_KEY_BASE = 20000`
- Game-object model keys use the raw instance index while wall-object keys add
  `WALL_OBJECT_KEY_BASE`; ground-item scene picks also use a numeric base.

Risk:

- Storage growth no longer drops the object at `20000`, but an unusually dense
  world-edit or remaster area can make the numeric key ranges overlap.

Recommended fix:

- Replace pick-index ranges with explicit typed pick records.
- Until then, add expanded F6 headroom/warning telemetry for each key range.

### Renderer 2D command capture caps

Files:

- `Client_Base/src/orsc/graphics/Renderer2DFrame.java`
- `Client_Base/src/orsc/graphics/two/GraphicsController.java`

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

Current instrumentation:

- Every stream records capacity attempts, accepted commands, and overflow
  drops in the immutable frame capture stats.
- Expanded F6 reports current accepted, lifetime maximum accepted, latest
  dropped, and the configured cap for sprites, text, primitives, rotated
  sprites, and circles.
- `Ctrl+F9` writes `renderer-2d-command-limits.tsv`; the offline analyzer
  validates the accounting while remaining compatible with older captures.
- The executable regression fixture confirms that `259` valid rotated-sprite
  submissions retain `256` commands and report `3` drops.

Recommended fix:

- Run representative dense-area, minimap, debug-overlay, and UI captures before
  changing storage policy.
- Convert a stream to capacity-managed storage only when current/max/drop data
  proves its cap is reachable, retaining a high emergency ceiling if needed.

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

The measured renderer 2D capture study is complete. Keep the present command
capacities and move the legacy-limit audit to fixed network buffers and numeric
pick-key ownership.

Reason:

- The accepted dense-area route and strict `12`-frame capture burst showed zero
  current-frame renderer 2D drops and comfortable sprite/text/rotated/circle
  headroom.
- Primitive drops were confined to load-transition windows and every reported
  overflow aligned with a world-section load. No visual loss was reported.
- Raising capacities now would add cost without evidence that ordinary play
  reaches the limits. Existing F6/session/capture telemetry remains the
  regression detector.

Suggested next implementation shape:

1. Instrument fixed network buffer occupancy and rejected/oversized packet
   paths without changing authentic protocol widths.
2. Exercise the largest current custom payloads and identify whether capacity,
   back-pressure, or parsing is the actual constraint.
3. Document and then separate game-object, wall-object, and ground-item pick
   key ranges before expanding those stores further.

## Useful Audit Searches

```bash
rg -n "new (byte|short|int|long|boolean|String)\\[[0-9]+\\]|CAPACITY|writeByte\\(\\(byte\\)" Client_Base/src server/src server/plugins -g '*.java'
rg -n "getUnsignedByte\\(\\)|getByte\\(\\)|readUnsignedByte\\(\\)|readByte\\(\\)|writeByte\\(" Client_Base/src/orsc/PacketHandler.java server/src/com/openrsc/server/net/rsc -g '*.java'
rg -n "ClientLimitations|maxBankItems|maxFriends|maxIgnore|maxServerId" server/src/com/openrsc/server/net/rsc -g '*.java'
```
