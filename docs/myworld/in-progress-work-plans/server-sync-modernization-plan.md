# Server Sync Modernization Plan

Status: in progress
Owner: Justin / AI-assisted
Branch: `feature/server-v2-sync`
Related docs:
- `docs/myworld/in-progress-work-plans/movement-pathing-release-plan.md`
- `docs/myworld/in-progress-work-plans/legacy-limits-audit.md`
- `docs/myworld/in-progress-work-plans/code-cleanup-and-modularization-plan.md`

## Live Testing Constraint

Do not assume this work can be proven on the public live server. Live testing
server sync rewrites is difficult because players may be online, player data
must remain safe, and destabilizing the hosted server is not acceptable for
experimental architecture work.

Primary validation should happen on an offline/local server with copied or
throwaway data. Where live-like pressure is needed, prefer controlled stress
tests: synthetic players, scripted clients, local bot clients, or compatible
botting tools configured specifically to create server load. Any such tooling
must be used only against development servers, never against the public server
without an explicit planned test window.

## Read This First

This is the server-side equivalent of the renderer "big swings" mindset, but
with more caution because the server owns player data and game authority.

Plain-English goal: make the server and custom client stay synchronized with
less repeated work, fewer legacy limits, and better performance for players.
The debug counters exist to prove correctness; they are not the goal. When in
doubt, prefer changes that move us toward a stronger sync foundation with a
clear rollback path for the live game.

The goal is not to rewrite combat, drops, saves, or content. The goal is to
modernize how world state is discovered, packaged, and sent to clients so the
server can support denser areas, smoother movement, larger client view ranges,
and future remaster-era features without being trapped by legacy RSC packet
limits.

Keep legacy clients working unless a change is explicitly marked
custom-client-only.

## Recommended First Steps

Current branch progress:

- [x] Added this branch-local modernization plan and live-testing constraint.
- [x] Fixed known-player PID cache bookkeeping so server indexes are not
  confused with appearance ids.
- [x] Fixed appearance update cache packets to send the observed player's
  appearance id instead of the viewer's.
- [x] Fixed `Region.equals` so it compares both region X and region Y. The
  inherited typo made every region with the same X compare equal, which is
  dangerous for future region-set and interest-manager work.
- [x] Batched queued outgoing writes so each player flushes once per outgoing
  packet cycle.
- [x] Added outgoing payload byte telemetry to profiling and benchmark output.
- [x] Prefetched visible players, NPCs, objects, and items once per normal
  player update and recorded visibility count/time telemetry.
- [x] Built a default-off shadow-mode visibility snapshot with parity counters
  for old-vs-new sync comparison.
- [x] Ran a short local benchmark with `openrsc.syncVisibilityShadow=true`,
  16 synthetic players, and 320 shadow samples; observed 0 mismatches.
- [x] Ran a longer local benchmark with `openrsc.syncVisibilityShadow=true`,
  64 synthetic players, and 7,680 shadow samples; observed 0 mismatches.
- [x] Added `want_sync_visibility_snapshot_input` / 
  `openrsc.syncVisibilitySnapshotInput` as a default-off, custom-client-only
  gate to let packets use the new visibility snapshot during local testing.
- [x] Ran local parity with both shadow mode and snapshot packet input enabled,
  64 synthetic players, and 7,680 shadow samples; observed 0 mismatches.
- [x] Added the first lower-allocation interest-manager cache: region windows
  are cached by view bounds and reused by legacy visibility and snapshot
  visibility without caching entity contents.
- [x] Moved scenery/wall counts into `VisibilitySnapshot` so packet telemetry
  does not rescan visible objects after the snapshot is built.
- [x] Re-ran local parity with both shadow mode and snapshot packet input
  enabled after the cache/count changes; 64 synthetic players and 7,680 shadow
  samples still produced 0 mismatches.
- [x] Split `VisibilitySnapshot` object input into packet-ready scenery and
  wall-object buckets. Game-object and wall-object packet stages now consume
  the smaller bucket they need instead of walking one shared object set and
  filtering the other type back out.
- [x] Re-ran local parity with both shadow mode and snapshot packet input
  enabled after the object-bucket split; 64 synthetic players and 7,680 shadow
  samples still produced 0 mismatches.
- [x] Added a cached visible-object window for the new snapshot path. The cache
  stores immutable candidate object lists by region-window bounds and is
  cleared whenever a `GameObject` is added to or removed from a region, so
  doors, rocks, crops, dev-placed scenery, and other object mutations cannot
  leave stale snapshot input behind.
- [x] Refined visible-object cache invalidation from global clears to
  region-targeted clears. Each object-window cache entry is indexed by the
  regions it covers; when a `GameObject` mutates, only windows that include
  that changed region are removed. This keeps object snapshot caching useful
  when doors, rocks, crops, and future world-edit deltas mutate one region.
- [x] Added a second filtered object-snapshot cache keyed by the same 8x8
  object-visibility grid used by `withinGridRange`. Players in the same object
  visibility cell can now reuse the filtered/split game-object, scenery, and
  wall-object buckets instead of filtering thousands of raw object candidates
  per update. Object mutations invalidate affected raw windows and filtered
  snapshots through the same region-targeted index.
- [x] Re-ran a short direct parity benchmark after filtered object-snapshot
  caching: 64 synthetic players, 3,840 visibility shadow samples, 0 mismatches,
  `visibilityObjectSnapshotCacheRequests=3840`,
  `visibilityObjectSnapshotCacheHits=3840`,
  `visibilityObjectSnapshotCacheMisses=0`. Overall tick time was noisy because
  NPC processing varied between runs, but update-client time improved compared
  with the pre-filtered-cache short run (`avgUpdateClientsMsPrecise` roughly
  29ms before vs 22-24ms after).
- [x] Re-ran a short direct parity benchmark after targeted object-cache
  invalidation: 64 synthetic players, 3,840 visibility shadow samples,
  0 mismatches, `visibilityObjectCacheHits=3840`,
  `visibilityObjectCacheMisses=0`, `visibilityObjectCacheClears=0`,
  `visibilityObjectCacheEntriesCleared=0`.
- [x] Re-ran local parity with both shadow mode and snapshot packet input
  enabled after cached object windows; 64 synthetic players and 7,680 shadow
  samples still produced 0 mismatches.
- [x] Add hit/miss telemetry for region-window and object-window caches before
  assuming the cached object windows are a measurable win. The first benchmark
  after adding object-window caching remained parity-safe but was effectively
  performance-neutral/noisy, so the next data question is whether the synthetic
  player layout is hitting the cache enough to matter.
- [x] Added visibility cache hit/miss telemetry to profiling and benchmark
  summaries. In the 64 synthetic player benchmark with shadow mode and snapshot
  packet input enabled, measured ticks showed `visibilityRegionCacheHits=758641`
  vs `visibilityRegionCacheMisses=6`, and
  `visibilityObjectCacheHits=7680` vs `visibilityObjectCacheMisses=0`.
  Visibility parity still held with 7,680 shadow samples and 0 mismatches.
- [x] Split benchmark mode from NPC profiling mode. Use
  `-Dopenrsc.benchmarkNpcProfiling=false` for production-shaped benchmark runs
  that still emit total tick/process timing, and use
  `-Dopenrsc.benchmarkDeepNpcProfiling=false` when per-NPC behavior/movement
  timing is useful but deep roam substage attribution is too noisy.
- [x] Final retained production-shaped parity benchmark:
  `npcProfiling=false`, `deepNpcProfiling=false`, 64 synthetic players, 7,680
  visibility shadow samples, 0 mismatches. Cache telemetry remained strong:
  `visibilityRegionCacheHits=757921` vs `visibilityRegionCacheMisses=8`, and
  `visibilityObjectCacheHits=7680` vs `visibilityObjectCacheMisses=0`.
  `avgProcessNpcsMsPrecise=56.921`, so NPC processing remains the largest
  benchmark cost outside client update/sync work.
- [x] Added default-off idle NPC tick throttling as the first active-NPC
  scheduler experiment:
  `want_npc_idle_tick_throttle=false` / `openrsc.npcIdleTickThrottle=false`
  by default, with `npc_idle_tick_throttle_interval=4`. Active NPCs always
  tick: unregistering, respawning, busy, combat/hostile/following, moving,
  interacting, or currently wanted by a player. Only truly idle NPCs are spread
  over the configured interval.
- [x] Benchmarked idle NPC tick throttling with 64 synthetic players and
  production-shaped benchmark mode. Control with throttle off:
  `avgTickMsPrecise=83.257`, `avgProcessNpcsMsPrecise=52.092`,
  `npcIdleThrottleSkipped=0`, 7,680 visibility shadow samples, 0 mismatches.
  Opt-in throttle interval 4:
  `avgTickMsPrecise=70.548`, `avgProcessNpcsMsPrecise=39.311`,
  `npcIdleThrottleSkipped=336482` total / `avgNpcIdleThrottleSkipped=2804`,
  7,680 visibility shadow samples, 0 mismatches.
- [x] Tested an observed-region safety guard for idle NPC throttling, where
  NPCs in player-visible region windows would always tick. This was not kept:
  the direct local benchmark with 64 synthetic players preserved visibility
  parity, but regressed to `avgTickMsPrecise=86.931` and
  `avgProcessNpcsMsPrecise=53.501`, worse than the throttle-off control.
- [x] Before enabling idle NPC tick throttling by default, run a local visual
  test around dense NPC areas and interactions: NPC dialogue, combat aggro,
  NPC respawns, NPCs already walking, and players entering a previously
  empty area.
- [x] Ran a local visual smoke test with `openrsc.syncVisibilityShadow=true`
  and `openrsc.syncVisibilitySnapshotInput=true` on the private dev server.
  Login, movement, interaction, equipment changes, and NPC attack testing
  looked correct from the client side; the server logged clean logout/save and
  no sync mismatch warnings.
- [x] Ran a local visual smoke test with `openrsc.npcIdleTickThrottle=true`
  alongside snapshot input/shadow mode. Admin login, long-distance movement,
  banker interaction, and several skeleton attack interactions looked correct;
  no sync mismatch warnings or server exceptions were logged. Remaining
  throttle-specific checks before default-on consideration: NPC respawn timing,
  aggro from idle hostile NPCs after entering a quiet area, and NPCs that were
  already moving before the player came into view.
- [x] Ran a focused local dev test for idle-throttle respawn and aggro paths
  using `::killnearnpcs` and `::forceaggro 20`. The command path killed
  nearby combat NPCs through normal NPC death handling and forced nearby
  attackers without server-side exceptions or sync mismatch warnings. The only
  server exception observed was the expected local socket reset after closing
  the dev client.
- [x] Promoted idle NPC tick throttling to the MyWorld branch default after the
  benchmark win and local visual checks. Both `myworld.conf` and
  `myworld-host.conf` now set `want_npc_idle_tick_throttle: true`, with
  `npc_idle_tick_throttle_interval: 4`. Rollback is config-only: set the flag
  back to `false` if live testing reveals an NPC edge case.
- [x] Tested two NPC roam scan reductions after cache telemetry. Reordering
  random-walk guards and adding a player-observed-region gate reduced region
  cache requests, but did not improve total/process-NPC benchmark time. Those
  behavior changes were not kept. Keep this as a note: do not continue tweaking
  roam locality without better evidence or a larger design change.
- [ ] The cache is being hit, so do not keep polishing this in isolation. The
  current benchmark hotspot is NPC roam/random-walk behavior, not object
  visibility. The next practical backend optimization should reduce repeated
  NPC roam spatial checks while preserving server authority and live behavior.
- [x] Tested and rejected a default-off NPC roam scan cadence experiment. The
  idea targeted a repeated-scan pattern where an idle aggressive NPC can
  perform nearby-player aggro scans every tick after the old combat timer
  threshold becomes true. A paired benchmark with snapshot input, shadow parity,
  idle throttle, and 64 synthetic players showed no win: control
  `avgTickMsPrecise=66.090` / `avgProcessNpcsMsPrecise=40.939`; cadence
  enabled `avgTickMsPrecise=68.326` /
  `avgProcessNpcsMsPrecise=42.070`. It reduced region-cache requests but made
  total timing worse, so the code was not kept.
- [x] Continue converting the snapshot builder from a parity-safe duplicate
  scan into a lower-allocation interest-manager cache that can be reused across
  packet stages. After cache telemetry, the next practical candidate is
  per-tick player visibility snapshot reuse, still default-off and compared
  against legacy in shadow mode.
- [x] Added default-off per-player, per-tick visibility snapshot reuse:
  `want_sync_visibility_tick_cache=false` /
  `openrsc.syncVisibilityTickCache=false`. Normal tick-driven player updates
  can reuse a cached legacy or snapshot-mode `VisibilitySnapshot` for the same
  player, tick, mode, and location. Immediate/manual refreshes, including login
  refreshes, stay uncached to avoid stale state during forced packet sends.
  Benchmark and expanded profiling output now include
  `visibilityTickSnapshotCacheRequests`, hits, and misses so this groundwork can
  be evaluated before it becomes a dependency for larger protocol work.
- [x] Benchmarked tick snapshot cache off/on with snapshot input, shadow parity,
  idle NPC throttle, and 64 synthetic players. Control:
  `avgTickMsPrecise=64.537`, `avgUpdateClientsMsPrecise=21.471`,
  `avgProcessNpcsMsPrecise=39.866`, 7,680 shadow samples, 0 mismatches, and 0
  tick-cache requests. Tick-cache enabled:
  `avgTickMsPrecise=64.317`, `avgUpdateClientsMsPrecise=21.845`,
  `avgProcessNpcsMsPrecise=39.354`, 7,680 shadow samples, 0 mismatches,
  `visibilityTickSnapshotCacheRequests=15360`,
  `visibilityTickSnapshotCacheHits=0`, and
  `visibilityTickSnapshotCacheMisses=15360`. This is expected for the current
  packet path because legacy and snapshot-mode visibility are each requested
  once per player per tick. Keep the flag default-off until custom scene/protocol
  consumers can reuse the same snapshot mode within the tick.
- [x] Started the default-off custom-client scene baseline packet path:
  `want_sync_scene_baseline=false` / `openrsc.syncSceneBaseline=false`.
  Stage 0 intentionally sends a tiny baseline proof packet only to custom
  clients: protocol version, server tick, origin, and visible player/NPC/scenery
  / wall / ground-item counts. The client parses and discards opcode 143 for
  now. This does not replace legacy local-list sync yet; it only establishes the
  packet lane and consumes the same visibility snapshot mode so
  `want_sync_visibility_tick_cache` can produce real reuse when both flags are
  enabled in local benchmarks.
- [x] Added benchmark-only `openrsc.benchmarkSyntheticClientVersion`, default
  235, so custom-client-only sync work can be stress tested with synthetic
  players without changing normal server behavior.
- [x] Ran a short opt-in custom-client benchmark with scene baseline, snapshot
  input, shadow parity, and tick snapshot cache enabled:
  `syntheticClientVersion=10046`, `avgTickMsPrecise=71.918`,
  `avgUpdateClientsMsPrecise=29.442`,
  `visibilityTickSnapshotCacheRequests=11520`,
  `visibilityTickSnapshotCacheHits=3840`,
  `visibilityTickSnapshotCacheMisses=7680`, 3,840 shadow samples, and 0
  visibility mismatches. The hit count confirms the new packet lane is reusing
  the same per-tick snapshot mode; the timing is not a production target because
  this run deliberately kept expensive shadow parity enabled.
- [x] Expanded scene baseline protocol to v2 with compact identity summaries.
  The packet still sends counts, and now adds one order-independent checksum for
  each visible category: players, NPCs, scenery, walls, and ground items. This
  keeps the proof packet small while giving us a way to detect whether the
  client-visible scene identity changed without dumping thousands of object
  records every tick. The client continues to parse and discard the packet.
- [x] Re-ran the opt-in custom-client benchmark after v2 checksums:
  `syntheticClientVersion=10046`, `avgTickMsPrecise=79.304`,
  `avgUpdateClientsMsPrecise=31.362`,
  `visibilityTickSnapshotCacheRequests=11520`,
  `visibilityTickSnapshotCacheHits=3840`,
  `visibilityTickSnapshotCacheMisses=7680`, 3,840 shadow samples, and 0
  visibility mismatches. This confirms parity and cache reuse, but also confirms
  that per-player, per-tick checksum computation is not production-shaped for
  dense object scenes. Treat v2 as protocol proof; the production path should
  only refresh static object summaries on region load or object changes.
- [x] Changed the v2 baseline proof path to consume the already-built packet
  visibility snapshot instead of requesting a second snapshot, moved baseline
  sending after scenery/wall/ground-item update stages, and returned those
  stages' existing `changed` booleans to drive summary refreshes. Static
  scenery, wall, and ground-item checksums are cached per player and only
  recomputed when the corresponding legacy local-list update changed or the
  visible count diverges. Identical baseline payloads are skipped.
- [x] Re-ran the opt-in custom-client benchmark after the v2 cache/gate pass:
  `syntheticClientVersion=10046`, `avgTickMsPrecise=72.863`,
  `avgUpdateClientsMsPrecise=29.716`,
  `visibilityTickSnapshotCacheRequests=7680`,
  `visibilityTickSnapshotCacheHits=0`,
  `visibilityTickSnapshotCacheMisses=7680`, 3,840 shadow samples, and 0
  visibility mismatches. The missing tick-cache hits are expected now: the
  baseline path no longer asks for a duplicate same-mode snapshot because it
  reads the packet snapshot directly.
- [x] Introduced scene baseline protocol v3 with paged full-baseline records
  for static scenery and wall objects. The packet is still default-off and
  custom-client only. It sends the compact v2 counts/checksums first, then at
  most one 64-record page per player update when the static category baseline
  changed or a new page still needs to be drained. The client parses and
  discards these records for now.
- [x] Added scene-baseline benchmark telemetry for attempted packets, object
  pages, object records, and estimated payload bytes. This is measured at the
  baseline builder rather than the socket flush path so synthetic-player
  benchmarks report useful data even without real network writes.
- [x] Re-ran the opt-in custom-client benchmark after the v3 page/telemetry
  pass: `syntheticClientVersion=10046`, `avgTickMsPrecise=73.703`,
  `avgUpdateClientsMsPrecise=29.858`,
  `visibilityTickSnapshotCacheRequests=7680`,
  `visibilityTickSnapshotCacheHits=0`,
  `visibilityTickSnapshotCacheMisses=7680`, 3,840 shadow samples, and 0
  visibility mismatches. Scene-baseline telemetry reported
  `sceneBaselinePackets=3840`, `sceneBaselinePages=2650`,
  `sceneBaselineRecords=166728`, and
  `sceneBaselinePayloadBytes=1510464`. This keeps the page path in the same
  rough performance band as the v2 cache/gate proof while proving the packet
  lane can now carry static object records in bounded chunks. It also shows the
  next split clearly: static object pages are bounded, but the packet is still
  sent every sampled player update because the same proof packet includes
  dynamic player/NPC summary data.
- [x] Split static scene baseline state from dynamic player/NPC summary state.
  Protocol v4 keeps the same frame shape for now but reserves player/NPC
  summary fields at zero. The static baseline path no longer consumes the
  dynamic visibility snapshot and no longer computes player/NPC checksums, so
  moving players and NPCs cannot force summary-only static-scene packets.
- [x] Re-ran the opt-in custom-client benchmark after the static/dynamic split:
  `syntheticClientVersion=10046`, `avgTickMsPrecise=78.202`,
  `avgUpdateClientsMsPrecise=30.500`, 3,840 shadow samples, and 0 visibility
  mismatches. Scene-baseline telemetry reported `sceneBaselinePackets=2650`,
  `sceneBaselinePages=2650`, `sceneBaselineRecords=166728`, and
  `sceneBaselinePayloadBytes=1455724`. Packet count now matches page count,
  confirming dynamic player/NPC movement is no longer causing summary-only
  static-scene sends. Tick/update timings were slightly noisier on this run,
  but the packet-shaping goal was achieved.
- [x] Added client-side scene-baseline debug storage. Opcode 143 still does not
  modify renderer/client world state, but the client now records the latest
  static baseline identity, received page totals by category, duplicate page
  counts, and records consumed. This gives us a safe diagnostic layer for
  proving completeness before using the packet as authoritative scene data.
- [x] Exposed scene-baseline completeness through the expanded F6 overlay.
  Local client/server capture showed protocol v4 live with static counts,
  packet/page/record progress, duplicate-page counts, and scenery page progress
  while the page stream was draining. The client debug now infers expected
  scenery/wall page totals from the static counts so wall progress is visible
  even before scenery paging finishes.
- [x] Added a bounded scene-baseline page burst. While a static baseline is
  incomplete, the server may send up to 4 baseline pages per update to custom
  clients, then returns to silence once the baseline is complete and unchanged.
  A dense local capture completed at `scenery 54/54`, `walls 2/2`, `dup 0`,
  `pk/pg/rec 56/56/3505`.
- [x] Added lower-risk client-side static-baseline storage. The custom client
  now stores received scenery and wall object records by page, rebuilds a
  complete stored baseline once every expected page has arrived, and exposes
  `loading` / `complete` plus stored scenery/wall record counts in expanded
  F6. This still does not drive rendering; it is a staging layer before
  authoritative scene input.
- [x] Validated stored baseline completion in a local client capture. F6 showed
  `sceneBase v4 complete`, `scenery 54/54`, `walls 2/2`, `dup 0`, and
  `stored 3432/73`.
- [x] Added a client-side scene-baseline staleness watchdog. Expanded F6 now
  reports `loading`, `complete`, or `stale`; tracks incomplete static-scene
  resets; and tracks completed stored baselines. This remains diagnostic-only
  and does not drive rendering yet.
- [x] Added stored-baseline vs legacy-local-list parity diagnostics. Expanded
  F6 now compares the stored complete static baseline against the legacy client
  scenery and wall instance arrays after converting legacy local coordinates
  with `midRegionBaseX/Z`. The diagnostic is throttled and reports compact
  scenery/wall parity plus legacy list counts. This still does not drive
  rendering.
- [x] Added scene-baseline protocol v5 object-range metadata. Expanded F6 now
  reports the server object sync range and splits legacy extras into
  inside/outside-range counts, e.g. `objects +N in/out A/B -M`. Extras outside
  the range are expected legacy local-list leftovers after movement or region
  changes; extras inside the range mean the new baseline path is missing data
  that still needs to be fixed before it can become authoritative.
- [x] Local capture with protocol v5 showed `objects +144 in/out 0/144 -0`
  and `walls ok`: the stored baseline was complete, no in-range baseline data
  was missing, and the drift was entirely stale legacy object-list state.
- [x] Added client-side stale legacy scene pruning after a complete static
  baseline. The custom client dematerializes and compacts scenery/wall entries
  outside the server-declared object sync range, then re-runs parity against
  the cleaned local lists. Expanded F6 reports cumulative `pruned objects/walls`
  counts.
- [x] Local follow-up capture after pruning showed `scene sync match ok` with
  `pruned objects/walls 165/6`, confirming the custom static baseline can clean
  stale legacy local-list entries without losing visible scenery or walls.
- [x] Added a custom-client baseline apply step after complete static baseline
  storage. The client now rebuilds its local scenery/wall arrays from the
  completed baseline records, while legacy packets still remain active during
  loading and as the compatibility path. Expanded F6 reports cumulative
  `applied` baseline rebuilds.
- [x] Local capture after the apply step showed `scene sync match ok` with
  `applied 2`; the stored baseline counts and legacy local-list counts matched
  after the baseline rebuild, confirming static scenery/wall arrays can be
  rebuilt from the custom baseline without visible parity loss in that dense
  movement test.
- [x] Local object-mutation capture after tree/rock/door-style interaction
  testing still showed `scene sync match ok`; stored and legacy counts matched
  after the custom baseline apply path, so static object changes are surviving
  the baseline rebuild path in the tested area.
- [x] Added conservative legacy static-scene packet suppression for custom
  clients after their complete static baseline has been sent. The server still
  updates the player's local scenery/wall sets so the baseline remains current,
  but duplicate legacy scenery/wall packets are skipped after baseline takeover.
  Legacy clients and custom clients still loading their first baseline keep the
  original packet path.
- [x] Local validation after suppression still showed `scene sync match ok`
  with matching stored and legacy counts and `applied 3`, confirming the custom
  baseline path can maintain static scenery/wall state after duplicate legacy
  static-scene packets are suppressed.
- [x] Added benchmark telemetry for suppressed legacy static-scene packets:
  `suppressedLegacySceneryPackets`, `suppressedLegacySceneryRecords`,
  `suppressedLegacyWallPackets`, and `suppressedLegacyWallRecords`. These sit
  beside scene-baseline packet/record/byte telemetry so future benchmark runs
  can show both the cost of the new baseline and the old scenery/wall packet
  work avoided.
- [x] Ran a short 64-synthetic-client custom-protocol benchmark after legacy
  static-scene suppression telemetry was added:
  `syntheticClientVersion=10046`, `sceneBaselinePackets=1498`,
  `sceneBaselinePages=1498`, `sceneBaselineRecords=93000`, and
  `sceneBaselinePayloadBytes=815904`. The run reported
  `suppressedLegacySceneryPackets=0` and `suppressedLegacyWallPackets=0`
  because this synthetic workload did not mutate scenery or walls after
  baseline takeover; it validates bounded baseline transfer cost, while the
  local F6 capture remains the proof that suppressing duplicate legacy
  scenery/wall packets still leaves `scene sync match ok`.
- [x] Added guarded static-scene scan skipping for custom clients after
  baseline takeover. The server skips the scenery/wall local-list scan only
  when the player is on the custom client, `want_sync_scene_baseline` is
  enabled, the complete static baseline has already been sent, the cached
  object snapshot key/version is unchanged, and the player's exact X/Y
  position is unchanged. Player movement, teleports, and any object mutation
  that invalidates the cached object snapshot still force the normal scan.
- [x] Re-ran the same short 64-synthetic-client custom-protocol benchmark after
  guarded static-scene scan skipping. Scene-baseline transfer stayed identical
  (`sceneBaselinePackets=1498`, `sceneBaselineRecords=93000`), while
  `avgUpdateClientsMsPrecise` improved from `11.383` to `6.859`,
  `avgUpdateGameObjectsMsPrecise` improved from `4.649` to `0.685`, and
  `avgUpdateWallObjectsMsPrecise` improved from `0.161` to `0.025`.
  This proves the branch can avoid repeated static scenery/wall scan work for
  stationary custom clients without changing the baseline packet shape.
- [x] Added the first default-off movement snapshot packet lane:
  `want_sync_movement_snapshot=false` / `openrsc.syncMovementSnapshot=false`.
  Custom clients can now receive opcode `146` with protocol version, server
  tick, sequence, local player position/sprite, and absolute moved-player/NPC
  records. The client parses and discards this packet for now, then exposes the
  last snapshot in expanded F6. Existing movement rendering still uses the
  legacy/custom movement path; this is a parity and instrumentation step before
  any authoritative replacement.
- [x] Ran the first synthetic custom-client packet-lane benchmark with scene
  baseline, snapshot input, tick-cache, and movement snapshot enabled:
  `syntheticClientVersion=10046`, `avgTickMsPrecise=52.649`,
  `avgUpdateClientsMsPrecise=6.713`, `sceneBaselinePackets=1498`,
  `sceneBaselineRecords=93000`, `movementSnapshotPackets=24506`,
  `movementSnapshotRecords=165348`, and
  `movementSnapshotPayloadBytes=1427002`. This proves opcode `146` is being
  generated under synthetic load. This run did not enable visibility-shadow
  parity; it was a packet-lane/telemetry proof, not a correctness proof for
  movement replacement.
- [x] Added client-side movement snapshot diagnostics that split packet-lane
  parity from visible-cache parity. Expanded F6 now reports `move snap` with
  wire agreement against the paired custom movement packet, plus `move cache`
  with local/player/NPC mismatch buckets and one compact sample when the visible
  cache disagrees.
- [x] Local capture showed `wire ok` while `move cache` flickered on one NPC.
  The sample was `n#289 e 111,409:6 a 108,415:6`, which showed the server was
  sending movement for an NPC inside the broad server view distance but outside
  the client's currently loaded 3x48 local terrain window. The server now
  filters custom movement records for both opcode `141` and opcode `146` to the
  viewer's client-local tile window. A busy-area retest held `wire ok` and
  `move cache ok`.
- [x] Added a snapshot-driven client-side staging path for movement records
  while keeping legacy/custom movement authoritative. Opcode `146` now feeds a
  separate staged local/player/NPC target set, expanded F6 folds the staged
  target status into `move cache`, and the rendered movement state is still
  driven only by the existing custom movement path. This gives us a safe
  future replacement lane without changing live movement behavior yet.
- [x] Completed a default-off hardening smoke test after the staged movement
  lane. Server/client builds passed, sync guard tests passed, checked-in config
  kept all new sync flags off, and a private default-off client/server run
  matched the public-style behavior in F6. This makes the dormant sync
  foundation suitable for merge/release with the experimental flags disabled.
- [ ] Next movement milestone: run longer local movement captures with staged
  targets enabled across walking, camera rotation, teleport/region changes,
  combat, and dense NPC areas. If `wire ok`, `move cache ok`, and `stage ok`
  hold under those cases, the next code step is an opt-in staged-consumer path
  that can animate from the snapshot state while falling back to the current
  custom movement path.
- [ ] Next custom-scene milestone: validate parity in local captures across
  login, movement, teleport/region changes, and object mutations, then decide
  whether the renderer should consume the stored baseline as a diagnostic
  source first or as an opt-in replacement path.

1. Add sync telemetry before changing behavior.
   - Track bytes sent by opcode.
   - Track packets sent by opcode.
   - Track visible players, NPCs, ground items, scenery, and wall objects per
     player update.
   - Track time spent building each update stage per player.
   - Track allocations only if practical; timing and packet volume come first.

2. Fix clear sync-bookkeeping bugs before larger rewrites.
   - Review `KnownPlayersHandler`; it appears to store appearance ids into the
     known-player PID array.
   - Review player appearance update ids; some code appears to send the
     viewer's appearance id where the observed player's appearance id may be
     intended.
   - These are small changes, but bad cache bookkeeping can make later
     protocol work look broken.

3. Batch outgoing packet flushes.
   - The current path writes and flushes each queued packet individually.
   - Move toward writing all queued packets for a player, then flushing once.
   - This is a low-risk first optimization because it should not alter game
     state or packet content.

4. Build a shadow-mode visibility snapshot.
   - Create the new visibility snapshot beside the existing `ViewArea` path.
   - Compare old vs new visible entities in logs/debug without using the new
     snapshot to drive packets yet.
   - Only switch packet building to the new snapshot after parity is proven.

5. Gate every major sync change behind custom-client and config flags.
   - Legacy clients stay on legacy packets.
   - Custom clients can opt into modern packets.
   - A release can quickly roll back to the old path without touching player
     data.

## Largest Swings First

### 1. Custom Client Scene Delta Protocol

Build a new protocol path for Spoiled Milk clients that sends explicit scene
deltas instead of relying on the inherited local-list packet model.

Target packet categories:

- Entity enters view.
- Entity leaves view.
- Entity moves.
- Entity sprite or animation changes.
- Entity appearance changes.
- Entity health, hitsplat, projectile, and combat-effect events.
- Ground item add/remove/update.
- Scenery and wall-object add/remove/update.
- Region or chunk baseline version.

Why this matters:

- Removes many legacy bit-width limits from the active custom client path.
- Avoids awkward remove/re-add behavior used to work around old sprite and
  movement packet limits.
- Gives the client cleaner data for interpolation and future visual effects.
- Lets old clients keep using the current protocol.

Risk:

- High. This touches server packet generation and client packet parsing.
- Must be introduced beside the old protocol, not as a replacement in one pass.

### 2. Server Interest Manager

Replace repeated per-player `ViewArea` scans with an explicit server-side
interest-management layer.

Current shape:

- Each player update asks for nearby players, NPCs, objects, and items.
- These calls allocate collections and scan nearby regions repeatedly.
- Multiple packet stages then walk local lists and visible lists again.

Target shape:

- Once per server tick, build or update visibility snapshots.
- Reuse those snapshots for player updates, NPC updates, object updates, item
  updates, and telemetry.
- Store enter/stay/leave sets so packet generation starts from known deltas.

Why this matters:

- Reduces repeated spatial work.
- Reduces allocation pressure and GC spikes.
- Gives protocol v2 a clean source of truth.
- Makes future entity culling and distance policies easier.

Risk:

- Medium-high. Visibility bugs are player-facing and can look like missing
  NPCs, lingering objects, or bad interaction menus.
- Shadow-mode comparison should come before switching live packets.

### 3. Static Region And Chunk Cache

Treat terrain, walls, scenery, roofs, and mostly-static world objects as
versioned region or chunk data.

Target shape:

- Each region/chunk has a stable version or hash.
- Client receives a baseline once.
- Server sends only dynamic deltas after that.
- Object placement/removal commands update the affected chunk version.

Why this matters:

- The remaster client loads larger visible areas.
- Static scenery and wall data should not be repeatedly rediscovered and
  repacked as if it were dynamic state.
- This prepares the server for a cleaner map-editing workflow.

Risk:

- Medium. Object mutations, doors, mining rocks, crops, and temporary scenery
  must still update correctly.
- Must distinguish static baseline data from dynamic replacements.

### 4. Movement Snapshot Stream

Turn the current custom movement packet into a cleaner movement snapshot/delta
stream.

Current shape:

- The custom packet sends absolute x/y/sprite for moved local players and NPCs.
- It supplements the legacy coordinate update path.
- The client appends waypoints to existing server-indexed entities.
- A default-off diagnostic snapshot packet now exists for custom clients. It is
  intentionally passive: the client records/debugs it but does not render from
  it yet.

Target shape:

- Server sends movement deltas with a tick or sequence number.
- Client interpolates between authoritative positions.
- Teleports, region changes, and hard resets are explicit packet events.
- Movement no longer depends on legacy remove/re-add behavior for combat
  sprites or wide local entity counts.

Why this matters:

- Reduces movement jitter and perceived desync.
- Makes NPC movement batching easier to reason about.
- Keeps gameplay authoritative on the server while allowing smoother client
  display.

Risk:

- Medium-high. Movement bugs are very visible and can affect interactions.
- Should be custom-client-only until proven.

### 5. Outbound Packet Batching And Buffer Reuse

Modernize packet output after protocol behavior is stable.

Target shape:

- Queue packets per player.
- Write all queued packets.
- Flush once per player update cycle.
- Reuse packet builders or buffers where safe.
- Keep packet contents identical during the first pass.

Why this matters:

- Reduces Netty flush overhead.
- Reduces short-lived buffer allocation.
- Lowers jitter under packet-heavy scenes.

Risk:

- Low-medium. Behavior should remain identical if packet order is preserved.

### 6. Snapshot-Based Off-Thread Encoding

Use more CPU cores only after world state can be safely snapshotted.

Target shape:

- Server tick mutates world state on the main game thread.
- At the end of mutation, create immutable or effectively immutable sync
  snapshots.
- Worker threads encode packets from snapshots.
- Main/network thread enqueues encoded output.

Why this matters:

- Uses extra CPU cores without making combat, movement, drops, or plugin logic
  concurrent.
- Avoids the highest-risk version of "multi-core server" work.

Risk:

- High if attempted too early.
- Reasonable after interest snapshots and protocol boundaries are clear.

### 7. Entity Storage And Allocation Cleanup

Reduce hot-path allocation and old collection patterns after the larger sync
shape is clear.

Targets:

- Avoid per-query `LinkedHashSet` allocation in visibility paths.
- Avoid stream snapshots in hot entity iteration where mutation behavior is
  known.
- Prefer stable arrays, primitive ids, or reusable buffers in packet builders.
- Keep object identity and server indexes stable for client references.

Why this matters:

- Helps with GC spikes.
- Makes dense areas and high player counts more predictable.

Risk:

- Medium. Collection semantics are easy to accidentally change.
- Do this after telemetry identifies the hottest paths.

## Expected Improvements

Exact numbers need telemetry, but these are the realistic targets:

- Lower server CPU per connected player in dense areas.
- Fewer short lag spikes from repeated visibility scans and allocation churn.
- Lower packet volume once protocol v2 and static chunk baselines are active.
- Smoother movement display for players and NPCs.
- Fewer local entity cache edge cases from old bit limits and remove/re-add
  workarounds.
- Better scalability when several players stand in the same busy area.
- Cleaner groundwork for future remaster features, including larger view
  ranges, denser object placement, richer projectiles, particles, and visual
  state events.

Small player counts may only see modest server gains at first. The largest
benefits should show up when there are many visible NPCs, objects, ground
items, players, or frequent movement/combat events.

This work may not directly raise renderer FPS unless the client is currently
spending significant time processing packet churn or rebuilding scene state.
It should still make the game feel steadier because server update cadence and
client-side entity state become cleaner.

## Migration And Data Safety

This restructure can be done without wiping or converting player data if the
work stays at the sync/protocol layer.

Safe migration rules:

- Do not change player save schemas as part of this plan unless a separate
  migration plan exists.
- Do not change item ids, NPC ids, scenery ids, quest keys, bank format, or
  character records for sync-only work.
- Keep legacy protocol generation available while custom protocol v2 is added.
- Use config flags for new sync paths so the live server can fall back quickly.
- Roll out in shadow mode where possible: build new snapshots, compare them to
  old visibility results, but keep sending old packets until parity is proven.
- Add release testing with existing accounts before making a new sync path the
  default.

Player data should remain safe because synchronization changes affect what the
server sends to clients, not the authoritative database state. The main risks
are visual/gameplay presentation bugs: missing entities, stale objects,
incorrect movement, duplicated ground items, or bad interaction menus.

## Testing Checklist

- [ ] Existing account can log in and move.
- [ ] Existing bank, inventory, equipment, quest state, devotion, and location
      remain intact.
- [ ] Legacy-compatible clients can still connect when legacy support is
      enabled.
- [ ] Custom client can enter, leave, and re-enter dense areas without missing
      NPCs, scenery, walls, or ground items.
- [ ] NPC combat, death, respawn, and loot drops remain correct.
- [ ] Player-vs-NPC movement and retargeting remain correct.
- [ ] Teleports and region changes do not produce black terrain or missing
      objects.
- [ ] Object mutations such as doors, mined rocks, crops, and placed world
      edits update correctly.
- [ ] Packet telemetry confirms byte/packet volume changes.
- [ ] Server timing telemetry confirms update-stage changes.

Useful local dev commands for idle-NPC-throttle testing:

- `::nearbynpcs (radius)` lists nearby NPC instance ids, definition ids, HP,
  combat/noncombat status, state, distance, and coordinates. Aliases:
  `::npcnear`, `::npcsnear`.
- `::forceaggro (radius)` forces nearby valid combat NPCs to attack the
  player. Aliases: `::aggroall`, `::aggronear`.
- `::killnearnpcs (radius)` kills nearby valid combat NPCs through the normal
  NPC death path so respawn timing and re-entry visibility can be tested.
  Aliases: `::killnearcombat`, `::killcombatnear`.
- Radius defaults to `8` and is clamped to `1..20`.

## Non-Goals

- Do not rewrite combat rules as part of this plan.
- Do not rewrite persistence or database schemas as part of this plan.
- Do not break older clients just to simplify the first implementation.
- Do not move world mutation to multiple threads before snapshots are isolated.
- Do not optimize one small packet endlessly if the larger snapshot/protocol
  structure is still missing.

## Completion Criteria

This plan can move to completed when:

- The server has useful sync telemetry.
- Visibility snapshots are the source of truth for custom-client sync.
- Custom clients can use a modern scene delta path for major entity/object
  updates.
- Static world data uses baseline plus delta behavior where practical.
- Existing player data migrates without manual conversion.
- Legacy-client support remains available or has an explicit separate removal
  decision.
- Dense-area testing shows equal or better correctness and measurably improved
  server sync cost.

## Unrelated Findings To Follow Up On

- Equipping items during local server-v2 testing logged `Unable to handle unknown plugin: WearObjTrigger`.
  This did not appear tied to the sync work, but it should be checked as normal
  equipment/plugin cleanup.
