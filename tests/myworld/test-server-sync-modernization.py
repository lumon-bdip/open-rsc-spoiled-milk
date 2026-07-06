#!/usr/bin/env python3
"""Guard the first server sync modernization fixes."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def extract_between(source: str, start: str, end: str) -> str:
    start_index = source.find(start)
    require(start_index >= 0, f"Missing block start: {start}")
    end_index = source.find(end, start_index)
    require(end_index > start_index, f"Missing block end: {end}")
    return source[start_index:end_index]


def main() -> None:
    known_players = (
        ROOT
        / "server/src/com/openrsc/server/net/rsc/handlers/KnownPlayersHandler.java"
    ).read_text(encoding="utf-8")
    updater = (
        ROOT
        / "server/src/com/openrsc/server/GameStateUpdater.java"
    ).read_text(encoding="utf-8")
    player = (
        ROOT
        / "server/src/com/openrsc/server/model/entity/player/Player.java"
    ).read_text(encoding="utf-8")
    server = (
        ROOT
        / "server/src/com/openrsc/server/Server.java"
    ).read_text(encoding="utf-8")
    profiling = (
        ROOT
        / "server/src/com/openrsc/server/event/rsc/handler/GameEventHandler.java"
    ).read_text(encoding="utf-8")
    region_manager = (
        ROOT
        / "server/src/com/openrsc/server/model/world/region/RegionManager.java"
    ).read_text(encoding="utf-8")
    region = (
        ROOT
        / "server/src/com/openrsc/server/model/world/region/Region.java"
    ).read_text(encoding="utf-8")
    visibility_snapshot = (
        ROOT
        / "server/src/com/openrsc/server/model/world/region/VisibilitySnapshot.java"
    ).read_text(encoding="utf-8")
    server_config = (
        ROOT
        / "server/src/com/openrsc/server/ServerConfiguration.java"
    ).read_text(encoding="utf-8")
    benchmark_script = (
        ROOT
        / "tools/benchmarks/benchmark-foundation.sh"
    ).read_text(encoding="utf-8")

    require(
        "player.knownPlayerPids[i] = payload.playerServerIndex[i];" in known_players,
        "known-player PID cache should use server indexes",
    )
    require(
        "player.knownPlayerPids[i] = payload.playerServerAppearanceId[i];"
        not in known_players,
        "known-player PID cache must not use appearance ids",
    )

    appearance_block = extract_between(
        updater,
        "while ((playerNeedingAppearanceUpdate = playersNeedingAppearanceUpdate.poll()) != null)",
        "if (playerNeedingAppearanceUpdate.getPossessing() != null)",
    )
    require(
        "playerNeedingAppearanceUpdate.getAppearanceID()" in appearance_block,
        "appearance update should send the observed player's appearance id",
    )
    require(
        "updatesMain.add((short) player.getAppearanceID());" not in appearance_block,
        "appearance update must not send the viewer's appearance id",
    )

    normal_update_block = extract_between(
        updater,
        "private void sendNormalUpdatePackets(final Player player)",
        "private void sendWorldTimeIfNeeded",
    )
    require(
        "final VisibilitySnapshot packetVisibility = buildPacketVisibilitySnapshot(player);" in normal_update_block,
        "normal update should build one packet visibility snapshot",
    )
    require(
        "final Collection<Player> visiblePlayers = packetVisibility.getPlayers();" in normal_update_block,
        "normal update should use packet visibility players",
    )
    require(
        "final Collection<Npc> visibleNpcs = packetVisibility.getNpcs();" in normal_update_block,
        "normal update should use packet visibility NPCs",
    )
    require(
        "final Collection<GameObject> visibleSceneryObjects = packetVisibility.getSceneryObjects();" in normal_update_block,
        "normal update should use packet visibility scenery objects",
    )
    require(
        "final Collection<GameObject> visibleWallObjects = packetVisibility.getWallObjects();" in normal_update_block,
        "normal update should use packet visibility wall objects",
    )
    require(
        "recordVisibilityShadowSnapshot(player, packetVisibility);" in normal_update_block,
        "normal update should run shadow visibility comparison after packet visibility is built",
    )
    require(
        "recordUpdatePlayers(() -> updatePlayers(player, visiblePlayers));" in normal_update_block,
        "player update should use the prefetched visible player collection",
    )
    require(
        "recordUpdateNpcs(() -> updateNpcs(player, visibleNpcs));" in normal_update_block,
        "NPC update should use the prefetched visible NPC collection",
    )
    require(
        "recordUpdateGameObjects(() -> updateGameObjects(player, visibleSceneryObjects));" in normal_update_block,
        "scenery update should use the prefetched scenery collection",
    )
    require(
        "recordUpdateWallObjects(() -> updateWallObjects(player, visibleWallObjects));" in normal_update_block,
        "wall update should use the prefetched wall collection",
    )

    packet_visibility_block = extract_between(
        updater,
        "private VisibilitySnapshot buildPacketVisibilitySnapshot(final Player player)",
        "private VisibilitySnapshot buildLegacyVisibilitySnapshot",
    )
    require(
        "useVisibilitySnapshotInput(player)" in packet_visibility_block,
        "packet visibility should be able to use snapshot input",
    )
    require(
        "buildVisibilitySnapshot(player)" in packet_visibility_block,
        "packet visibility should use region-manager snapshot when gated on",
    )
    require(
        "buildLegacyVisibilitySnapshot(player)" in packet_visibility_block,
        "packet visibility should keep legacy fallback",
    )
    require(
        "recordVisibilitySnapshotMetrics(packetVisibility" in packet_visibility_block,
        "packet visibility should record telemetry",
    )
    legacy_visibility_block = extract_between(
        updater,
        "private VisibilitySnapshot buildLegacyVisibilitySnapshot(final Player player)",
        "private boolean useVisibilitySnapshotInput",
    )
    for snippet in (
        "player.getViewArea().getPlayersInView()",
        "player.getViewArea().getNpcsInView()",
        "player.getViewArea().getGameObjectsInView()",
        "player.getViewArea().getItemsInView()",
    ):
        require(snippet in legacy_visibility_block, f"legacy visibility snapshot missing {snippet}")
    visibility_input_gate = extract_between(
        updater,
        "private boolean useVisibilitySnapshotInput",
        "private void recordVisibilitySnapshotMetrics",
    )
    require(
        "player.isUsingCustomClient() && getServer().getConfig().WANT_SYNC_VISIBILITY_SNAPSHOT_INPUT" in visibility_input_gate,
        "snapshot packet input should be custom-client and config gated",
    )
    visibility_metrics_block = extract_between(
        updater,
        "private void recordVisibilitySnapshotMetrics",
        "private void recordVisibilityShadowSnapshot",
    )
    require(
        "addVisibilitySnapshotMetrics(" in visibility_metrics_block,
        "packet visibility should record visibility snapshot telemetry",
    )
    require(
        "packetVisibility.getSceneryCount()" in visibility_metrics_block
        and "packetVisibility.getWallObjectCount()" in visibility_metrics_block,
        "packet visibility metrics should reuse precomputed object counts",
    )
    shadow_block = extract_between(
        updater,
        "private void recordVisibilityShadowSnapshot(",
        "private boolean sameIdentityCollection",
    )
    require(
        "if (!getServer().getConfig().WANT_SYNC_VISIBILITY_SHADOW)" in shadow_block,
        "shadow visibility snapshot must be config gated",
    )
    require(
        "useVisibilitySnapshotInput(player)" in shadow_block,
        "shadow visibility should reverse the comparison when snapshot input is enabled",
    )
    require(
        "buildLegacyVisibilitySnapshot(player)" in shadow_block,
        "shadow visibility should be able to compare snapshot input against legacy visibility",
    )
    require(
        "addVisibilityShadowMetrics(" in shadow_block,
        "shadow visibility snapshot should record parity telemetry",
    )
    require(
        "sameIdentityCollection(packetVisibility.getPlayers(), comparisonSnapshot.getPlayers())" in shadow_block,
        "shadow visibility should compare player identity sets",
    )
    require(
        "sameIdentityCollection(packetVisibility.getNpcs(), comparisonSnapshot.getNpcs())" in shadow_block,
        "shadow visibility should compare NPC identity sets",
    )
    update_npcs_block = extract_between(
        updater,
        "protected void updateNpcs(final Player playerToUpdate, final Collection<Npc> visibleNpcs)",
        "protected void updatePlayers",
    )
    require(
        "playerToUpdate.getViewArea().getNpcsInView()" not in update_npcs_block,
        "updateNpcs should not rescan visible NPCs after prefetch",
    )
    update_players_block = extract_between(
        updater,
        "protected void updatePlayers(final Player playerToUpdate, final Collection<Player> visiblePlayers)",
        "public void updateNpcAppearances",
    )
    require(
        "playerToUpdate.getViewArea().getPlayersInView()" not in update_players_block,
        "updatePlayers should not rescan visible players after prefetch",
    )

    outgoing_block = extract_between(
        player,
        "public long processOutgoingPackets()",
        "public void removeSkull()",
    )
    require(
        "channel.write(outgoing);" in outgoing_block,
        "queued outgoing packets should be written before a shared flush",
    )
    require(
        "channel.flush();" in outgoing_block,
        "queued outgoing packets should flush once after writes",
    )
    require(
        "channel.writeAndFlush(outgoing)" not in outgoing_block,
        "queued outgoing packets should not flush each individual packet",
    )
    require(
        "addOutgoingPacketBytes(outgoing.getID(), outgoing.getLength())" in outgoing_block,
        "queued outgoing packets should record payload byte telemetry",
    )

    for snippet in (
        "private final Map<Integer, Long> outgoingPayloadBytesPerPacketOpcode",
        "private long lastOutgoingPayloadBytes = 0;",
        "private long benchmarkOutgoingPayloadBytesTotal = 0;",
        "public Map<Integer, Long> getOutgoingPayloadBytesPerPacketOpcode()",
        "public long getLastOutgoingPayloadBytes()",
        "public void addOutgoingPacketBytes",
        "benchmarkOutgoingPayloadBytesTotal += getLastOutgoingPayloadBytes();",
        '" avgOutgoingPayloadBytes="',
        "this.lastOutgoingPayloadBytes = 0;",
        "private long lastVisibilitySnapshotDuration = 0;",
        "private long lastVisibilitySnapshotSamples = 0;",
        "private long lastVisiblePlayersTotal = 0;",
        "private long lastVisibilityRegionCacheRequests = 0;",
        "private long lastVisibilityObjectCacheRequests = 0;",
        "private long lastVisibilityObjectCacheEntriesCleared = 0;",
        "private long lastVisibilityObjectSnapshotCacheRequests = 0;",
        "private long lastVisibilityObjectSnapshotCacheHits = 0;",
        "private long lastVisibilityObjectSnapshotCacheMisses = 0;",
        "private long benchmarkVisibilitySnapshotTotal = 0;",
        "private long benchmarkVisibilityRegionCacheRequests = 0;",
        "private long benchmarkVisibilityObjectCacheRequests = 0;",
        "private long benchmarkVisibilityObjectCacheEntriesCleared = 0;",
        "private long benchmarkVisibilityObjectSnapshotCacheRequests = 0;",
        "public long getLastVisibilitySnapshotDuration()",
        "public long getLastVisibilitySnapshotSamples()",
        "public long getLastVisiblePlayersTotal()",
        "public long getLastVisibilityRegionCacheRequests()",
        "public long getLastVisibilityObjectCacheRequests()",
        "public long getLastVisibilityObjectCacheEntriesCleared()",
        "public long getLastVisibilityObjectSnapshotCacheRequests()",
        "public long getLastVisibilityObjectSnapshotCacheHits()",
        "public long getLastVisibilityObjectSnapshotCacheMisses()",
        "public synchronized void addVisibilitySnapshotMetrics",
        "public synchronized void recordVisibilityRegionCacheAccess",
        "public synchronized void recordVisibilityObjectCacheAccess",
        "public synchronized void recordVisibilityObjectSnapshotCacheAccess",
        "public synchronized void recordVisibilityObjectCacheClear",
        "this.lastVisibilityObjectCacheEntriesCleared += entriesCleared;",
        "benchmarkVisibilitySnapshotTotal += getLastVisibilitySnapshotDuration();",
        "benchmarkVisibilityRegionCacheRequests += getLastVisibilityRegionCacheRequests();",
        "benchmarkVisibilityObjectCacheRequests += getLastVisibilityObjectCacheRequests();",
        "benchmarkVisibilityObjectCacheEntriesCleared += getLastVisibilityObjectCacheEntriesCleared();",
        "benchmarkVisibilityObjectSnapshotCacheRequests += getLastVisibilityObjectSnapshotCacheRequests();",
        '" avgVisibilitySnapshotMs="',
        '" avgVisiblePlayers="',
        "visibilityRegionCacheRequests=",
        "visibilityObjectCacheRequests=",
        "visibilityObjectCacheEntriesCleared=",
        "visibilityObjectSnapshotCacheRequests=",
        "visibilityObjectSnapshotCacheHits=",
        "visibilityObjectSnapshotCacheMisses=",
        "this.lastVisibilitySnapshotDuration = 0;",
        "this.lastVisibilityRegionCacheRequests = 0;",
        "this.lastVisibilityObjectCacheRequests = 0;",
        "this.lastVisibilityObjectCacheEntriesCleared = 0;",
        "this.lastVisibilityObjectSnapshotCacheRequests = 0;",
        "private long lastVisibilityShadowDuration = 0;",
        "private long lastVisibilityShadowMismatchSamples = 0;",
        "private long benchmarkVisibilityShadowTotal = 0;",
        "public long getLastVisibilityShadowDuration()",
        "public long getLastVisibilityShadowMismatchSamples()",
        "public synchronized void addVisibilityShadowMetrics",
        "benchmarkVisibilityShadowTotal += getLastVisibilityShadowDuration();",
        '" avgVisibilityShadowMs="',
        '" visibilityShadowMismatches="',
        "this.lastVisibilityShadowDuration = 0;",
    ):
        require(snippet in server, f"server missing outgoing payload byte telemetry: {snippet}")

    for snippet in (
        "=== Outgoing Packets ===",
        "getOutgoingPayloadBytesPerPacketOpcode()",
        '" payload bytes"',
        "=== Visibility Snapshot ===",
        "getLastVisibilitySnapshotSamples()",
        "Average visible: players=",
        "Max visible: players=",
        "Cache: region requests=",
        "entriesCleared=",
        "objectSnapshot requests=",
        "NPC idle throttle skipped:",
        "Shadow snapshot: time=",
        "getLastVisibilityShadowMismatchSamples()",
    ):
        require(snippet in profiling, f"profiling output missing outgoing payload bytes: {snippet}")

    for snippet in (
        "public VisibilitySnapshot buildVisibilitySnapshot(final Mob entity)",
        "private final ConcurrentHashMap<Long, List<Region>> visibleRegionWindowCache;",
        "private final ConcurrentHashMap<Long, List<GameObject>> visibleObjectWindowCache;",
        "private final ConcurrentHashMap<Long, Set<Long>> visibleObjectWindowKeysByRegion;",
        "private final ConcurrentHashMap<Long, VisibleObjectSnapshot> visibleObjectSnapshotCache;",
        "private final ConcurrentHashMap<Long, Set<Long>> visibleObjectSnapshotKeysByRegion;",
        "this.visibleRegionWindowCache = new ConcurrentHashMap<>();",
        "this.visibleObjectWindowCache = new ConcurrentHashMap<>();",
        "this.visibleObjectWindowKeysByRegion = new ConcurrentHashMap<>();",
        "this.visibleObjectSnapshotCache = new ConcurrentHashMap<>();",
        "this.visibleObjectSnapshotKeysByRegion = new ConcurrentHashMap<>();",
        "visibleRegionWindowCache.putIfAbsent",
        "visibleObjectWindowCache.putIfAbsent",
        "visibleObjectSnapshotCache.putIfAbsent",
        "indexCacheKeyByRegion(visibleObjectWindowKeysByRegion, cacheKey, objectRegions);",
        "indexCacheKeyByRegion(visibleObjectSnapshotKeysByRegion, cacheKey, objectRegions);",
        "private void indexCacheKeyByRegion",
        "recordVisibilityRegionCacheAccess",
        "recordVisibilityObjectCacheAccess",
        "recordVisibilityObjectCacheClear",
        "private List<Region> buildVisibleRegionWindow",
        "private VisibleObjectSnapshot getVisibleObjectSnapshot",
        "private VisibleObjectSnapshot buildVisibleObjectSnapshot",
        "recordVisibilityObjectSnapshotCacheAccess(true)",
        "recordVisibilityObjectSnapshotCacheAccess(previous != null)",
        "private List<GameObject> getVisibleObjectWindow",
        "private List<GameObject> buildVisibleObjectWindow",
        "public void invalidateVisibleObjectWindowCache()",
        "public void invalidateVisibleObjectWindowCache(final Region changedRegion)",
        "visibleObjectWindowCache.remove(affectedWindowKey)",
        "visibleObjectSnapshotCache.remove(affectedSnapshotKey)",
        "return Collections.unmodifiableList(visible);",
        "private long packRegionWindowKey",
        "private long packRegionCoordinateKey",
        "private long packObjectSnapshotKey",
        "private static final class VisibleObjectSnapshot",
        "final LinkedHashSet<Player> localPlayers = new LinkedHashSet<>();",
        "final LinkedHashSet<Npc> localNpcs = new LinkedHashSet<>();",
        "final VisibleObjectSnapshot visibleObjects = getVisibleObjectSnapshot(entity.getLocation(), objectRegions);",
        "final LinkedHashSet<GroundItem> localItems = new LinkedHashSet<>();",
        "final List<Region> mobRegions = getVisibleRegionWindow(entity.getLocation());",
        "final List<Region> objectRegions = getVisibleRegionWindow(entity.getLocation(), getWorld().getServer().getConfig().OBJECT_VIEW_DISTANCE);",
        "return new VisibilitySnapshot(",
    ):
        require(snippet in region_manager, f"region manager missing shadow visibility snapshot: {snippet}")

    require(
        "regionManager.invalidateVisibleObjectWindowCache(this);" in region,
        "region game-object mutations should invalidate cached visible object windows for the changed region",
    )
    require(
        "return other.regionX == regionX && other.regionY == regionY;" in region,
        "region equality should compare both region coordinates",
    )
    require(
        "other.regionY == other.getRegionY()" not in region,
        "region equality must not treat all regions with the same X as equal",
    )

    for snippet in (
        "public final class VisibilitySnapshot",
        "private final Collection<Player> players;",
        "private final Collection<Npc> npcs;",
        "private final Collection<GameObject> gameObjects;",
        "private final Collection<GameObject> sceneryObjects;",
        "private final Collection<GameObject> wallObjects;",
        "private final Collection<GroundItem> groundItems;",
        "private static Collection<GameObject> splitGameObjects",
        "public Collection<Player> getPlayers()",
        "public Collection<Npc> getNpcs()",
        "public Collection<GameObject> getGameObjects()",
        "public Collection<GameObject> getSceneryObjects()",
        "public Collection<GameObject> getWallObjects()",
        "public Collection<GroundItem> getGroundItems()",
        "public int getSceneryCount()",
        "public int getWallObjectCount()",
    ):
        require(snippet in visibility_snapshot, f"visibility snapshot DTO missing: {snippet}")

    require(
        "public boolean WANT_SYNC_VISIBILITY_SHADOW;" in server_config,
        "server config should expose shadow visibility flag",
    )
    require(
        "public boolean WANT_SYNC_VISIBILITY_SNAPSHOT_INPUT;" in server_config,
        "server config should expose snapshot packet input flag",
    )
    require(
        'WANT_SYNC_VISIBILITY_SHADOW = readBoolSystemEnvConfig('
        in server_config,
        "shadow visibility flag should support system/env/config override",
    )
    require(
        '"openrsc.syncVisibilityShadow"' in server_config
        and '"OPENRSC_SYNC_VISIBILITY_SHADOW"' in server_config
        and '"want_sync_visibility_shadow"' in server_config,
        "shadow visibility flag should expose benchmark-safe override names",
    )
    require(
        '"openrsc.syncVisibilitySnapshotInput"' in server_config
        and '"OPENRSC_SYNC_VISIBILITY_SNAPSHOT_INPUT"' in server_config
        and '"want_sync_visibility_snapshot_input"' in server_config,
        "snapshot input flag should expose benchmark-safe override names",
    )
    require(
        'BENCHMARK_EXTRA_JVM_ARGS="${MYWORLD_BENCHMARK_EXTRA_JVM_ARGS:-}"' in benchmark_script,
        "foundation benchmark should accept extra JVM args",
    )
    require(
        "$BENCHMARK_EXTRA_JVM_ARGS" in benchmark_script,
        "foundation benchmark should pass extra JVM args to the server",
    )

    print("PASS: server sync modernization guards validated")


if __name__ == "__main__":
    main()
