#!/usr/bin/env python3
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ROWBOAT_LOCS = ROOT / "server/conf/server/defs/locs/SceneryLocsOther.json"
ROWBOAT_PLUGIN = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/misc/RandomObjects.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(condition: bool, description: str) -> None:
    if not condition:
        fail(description)


def extract(text: str, start: str, end: str) -> str:
    start_index = text.find(start)
    require(start_index >= 0, f"missing start marker: {start}")
    end_index = text.find(end, start_index)
    require(end_index >= 0, f"missing end marker: {end}")
    return text[start_index:end_index]


class SceneClientModel:
    """Minimal model of baseline delivery across the rowboat/death sequence."""

    def __init__(self) -> None:
        self.loaded_origin = "lumbridge"
        self.stored_origin = "lumbridge"
        self.applied_key = "lumbridge"
        self.objects = {1242}

    def receive_complete_baseline(self, origin: str, objects: set[int]) -> None:
        self.stored_origin = origin
        if self.loaded_origin == origin:
            self.applied_key = origin
            self.objects = set(objects)

    def defer_death_region_load(self, respawn_origin: str) -> None:
        self.receive_complete_baseline(respawn_origin, {1242})

    def finish_region_load(self, origin: str) -> None:
        self.loaded_origin = origin
        self.objects.clear()
        if self.stored_origin == origin:
            # Region loads must force a replay even when this baseline key was
            # applied on an earlier visit to the same place.
            self.applied_key = origin
            self.objects = {1242}


def verify_repeated_use_and_death_model() -> None:
    client = SceneClientModel()
    for _ in range(2):
        require(1242 in client.objects, "rowboat missing before repeated travel")
        client.loaded_origin = "edgeville"
        client.objects.clear()
        client.defer_death_region_load("lumbridge")
        require(not client.objects, "respawn baseline applied against the stale Edgeville origin")
        client.finish_region_load("lumbridge")
        require(1242 in client.objects, "rowboat baseline was not replayed after respawn region load")


def main() -> None:
    locations = json.loads(ROWBOAT_LOCS.read_text(encoding="utf-8"))["sceneries"]
    rowboats = [
        loc for loc in locations
        if loc.get("id") == 1242
        and loc.get("pos") == {"X": 119, "Y": 642}
    ]
    require(len(rowboats) == 1, "Lumbridge rowboat must remain one persistent scenery spawn")

    plugin = ROWBOAT_PLUGIN.read_text(encoding="utf-8")
    rowboat_case = extract(plugin, "case 1242:", "break;")
    require("player.teleport(206,449);" in rowboat_case, "rowboat travel destination changed")
    for mutation in ("delloc", "changeloc", "unregisterGameObject", "delayedSpawnObject"):
        require(mutation not in rowboat_case, f"rowboat travel must not mutate persistent scenery via {mutation}")

    player = PLAYER.read_text(encoding="utf-8")
    death = extract(player, "public void killedBy(final Mob mob)", "private int getEquippedWeaponID()")
    require(
        "teleport(getConfig().RESPAWN_LOCATION_X, getConfig().RESPAWN_LOCATION_Y, false);" in death,
        "death must continue through the normal respawn teleport lifecycle",
    )

    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    require(
        "public void reapplyCompleteSceneBaselineAfterRegionLoad()" in packet_handler
        and "applyCompleteSceneBaselineToLegacyLists(true);" in packet_handler,
        "completed scene baselines must be force-replayed after a region load",
    )
    require(
        "|| !sceneBaselineDebugState.isBaselineOriginLoaded(mc)" in packet_handler
        and re.search(
            r"private boolean isBaselineOriginLoaded\(mudclient mc\).*?World\.isLocalTile\(",
            packet_handler,
            re.DOTALL,
        ),
        "scene baselines must not apply against a stale region origin during the death screen",
    )

    client = CLIENT.read_text(encoding="utf-8")
    region_load = extract(client, "public final boolean loadNextRegion", "private void loadSounds()")
    require(
        "if (this.deathScreenTimeout != 0)" in region_load
        and "this.packetHandler.reapplyCompleteSceneBaselineAfterRegionLoad();" in region_load,
        "deferred death region loads must replay their stored scenery baseline when loading completes",
    )
    require(
        region_load.index("this.world.playerAlive = true;")
        < region_load.index("this.packetHandler.reapplyCompleteSceneBaselineAfterRegionLoad();")
        < region_load.index("return true;"),
        "baseline replay must happen after the new region is live and before region load returns",
    )

    verify_repeated_use_and_death_model()
    print("PASS: Lumbridge rowboat survives repeated travel and deferred death/respawn region loads")


if __name__ == "__main__":
    main()
