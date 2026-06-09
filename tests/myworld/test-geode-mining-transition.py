#!/usr/bin/env python3
"""Validate Mining geodes and retired gem-seed generation."""

from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[2]
ITEM_ID = ROOT / "server/src/com/openrsc/server/constants/ItemId.java"
CLIENT_ITEMS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
SERVER_ITEMS = ROOT / "server/conf/server/defs/ItemDefsCustom.json"
MINING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/mining/Mining.java"
WOODCUTTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/Woodcutting.java"
RESOURCE_SEEDS = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/gathering/ResourceSeeds.java"
GEODES = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/gathering/Geodes.java"
GEODE_SPRITE = ROOT / "dev/myworld/assets/sprites/items/inventory-ground/geode.png"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def forbid(text: str, snippet: str, label: str) -> None:
    if snippet in text:
        fail(f"{label} should not contain retired snippet: {snippet}")


def main() -> None:
    item_id = ITEM_ID.read_text(encoding="utf-8")
    for snippet in (
        "KEY_HALF_SEED(3176)",
        "SMALL_GEODE(3177)",
        "STANDARD_GEODE(3178)",
        "LARGE_GEODE(3179)",
        "HUGE_GEODE(3180)",
        "public static final int maxCustom = 3181;",
    ):
        require(item_id, snippet, "ItemId.java")

    client_items = CLIENT_ITEMS.read_text(encoding="utf-8")
    for snippet in (
        'addResourceSeedDefinition("Key half seed"',
        'setCustomItemDefinition(3177, new ItemDef("Small geode"',
        'setCustomItemDefinition(3178, new ItemDef("Standard geode"',
        'setCustomItemDefinition(3179, new ItemDef("Large geode"',
        'setCustomItemDefinition(3180, new ItemDef("Huge geode"',
        '"external-png:geode@22x16"',
        '"external-png:geode@42x30"',
    ):
        require(client_items, snippet, "client item definitions")

    client = CLIENT.read_text(encoding="utf-8")
    for snippet in (
        "assetSpec.indexOf('@')",
        "loadExternalItemSprite(getExternalPngFile(assetName), targetWidth, targetHeight)",
        'return new String[]{"Select mining focus", "Just the ore", "A few geodes", "Plenty of geodes", "Lots of geodes"};',
    ):
        require(client, snippet, "mudclient geode support")

    server_items = json.loads(SERVER_ITEMS.read_text(encoding="utf-8"))["items"]
    names_by_id = {item["id"]: item["name"] for item in server_items}
    expected_names = {
        3176: "Key half seed",
        3177: "Small geode",
        3178: "Standard geode",
        3179: "Large geode",
        3180: "Huge geode",
    }
    for item_id, name in expected_names.items():
        if names_by_id.get(item_id) != name:
            fail(f"ItemDefsCustom missing {item_id}: {name}")

    mining = MINING.read_text(encoding="utf-8")
    for snippet in (
        "MYWORLD_MINING_GEODE_REWARDS",
        "private boolean maybeAwardMyWorldMiningGeode(Player player, GameObject rock, int pickaxeTier)",
        "rollMyWorldMiningGeode(pickaxeTier)",
        "Formulae.adjustedSideRewardWeightForToolTier(reward.tier, pickaxeTier, reward.weight)",
        "Your cosmic amulet glimmers and another geode appears.",
    ):
        require(mining, snippet, "Mining geode reward")
    forbid(mining, "maybeAwardMyWorldMiningGem", "Mining geode reward")
    forbid(mining, "getRandomGemChance", "Mining geode reward")

    woodcutting = WOODCUTTING.read_text(encoding="utf-8")
    require(woodcutting, "new SeedReward(ItemId.KEY_HALF_SEED.id(), 10, 1)", "Woodcutting key-half seed reward")
    for retired in (
        "new SeedReward(ItemId.SAPPHIRE_SEED.id()",
        "new SeedReward(ItemId.EMERALD_SEED.id()",
        "new SeedReward(ItemId.RUBY_SEED.id()",
        "new SeedReward(ItemId.DIAMOND_SEED.id()",
        "new SeedReward(ItemId.DRAGONSTONE_SEED.id()",
    ):
        forbid(woodcutting, retired, "Woodcutting retired gem seeds")

    resource_seeds = RESOURCE_SEEDS.read_text(encoding="utf-8")
    require(resource_seeds, "ItemId.KEY_HALF_SEED.id()", "ResourceSeeds key-half tree")
    require(resource_seeds, "YieldType.KEY_HALF", "ResourceSeeds key-half yield")
    require(resource_seeds, "You harvest a key half.", "ResourceSeeds key-half message")
    for retired in (
        "ItemId.SAPPHIRE_SEED.id(), SceneryId.RESOURCE_TREE.id()",
        "ItemId.EMERALD_SEED.id(), SceneryId.RESOURCE_TREE.id()",
        "ItemId.RUBY_SEED.id(), SceneryId.RESOURCE_TREE.id()",
        "ItemId.DIAMOND_SEED.id(), SceneryId.RESOURCE_TREE.id()",
        "ItemId.DRAGONSTONE_SEED.id(), SceneryId.RESOURCE_TREE.id()",
    ):
        forbid(resource_seeds, retired, "ResourceSeeds retired gem trees")

    geodes = GEODES.read_text(encoding="utf-8")
    for snippet in (
        "Use a chisel to crack this open.",
        "You crack the geode open...",
        '"There was " + reward.description + " inside!"',
        "ItemId.RUNE_STONE.id()",
        "ItemId.TOOTH_KEY_HALF.id()",
        "ItemId.LOOP_KEY_HALF.id()",
        "KEY_HALVES(new int[] {0, 2, 4, 4})",
    ):
        require(geodes, snippet, "Geodes handler")

    if not GEODE_SPRITE.is_file():
        fail(f"Missing active geode sprite: {GEODE_SPRITE}")

    print("PASS: Mining geode transition and retired gem seed generation look correct")


if __name__ == "__main__":
    main()
