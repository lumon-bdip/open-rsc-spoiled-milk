#!/usr/bin/env python3
import math
import re
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
EFFECTS = ROOT / "server/src/com/openrsc/server/content/EnchantingItemEffects.java"
ITEM_DEFS_CUSTOM = ROOT / "server/conf/server/defs/ItemDefsCustom.json"
ITEM_ID = ROOT / "server/src/com/openrsc/server/constants/ItemId.java"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
EQUIPMENT = ROOT / "server/src/com/openrsc/server/model/container/Equipment.java"
NPC = ROOT / "server/src/com/openrsc/server/model/entity/npc/Npc.java"
DROP_TABLE = ROOT / "server/src/com/openrsc/server/content/DropTable.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
POISON_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/PoisonEvent.java"
EATING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/itemactions/Eating.java"
SUMMONING = ROOT / "server/src/com/openrsc/server/content/Summoning.java"
COMBAT_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/CombatEvent.java"
PVM_MELEE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/PvmMeleeEvent.java"
PROJECTILE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ProjectileEvent.java"
FISHING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/fishing/Fishing.java"
MINING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/mining/Mining.java"
WOODCUTTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/woodcutting/Woodcutting.java"
HARVESTING = ROOT / "server/plugins/com/openrsc/server/plugins/custom/skills/harvesting/Harvesting.java"
LAW_JEWELRY = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/enchanting/LawJewelry.java"
NATURE_ALCHEMY_AMULET = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/enchanting/NatureAlchemyAmulet.java"

AIR = 1190
MIND = 1192
WATER = 1194
EARTH = 1196
FIRE = 1198
BODY = 1200
COSMIC = 1202
CHAOS = 1204
NATURE = 1206
LAW = 1208
DEATH = 1210
BLOOD = 1212
SOUL = 1296
LIFE = 1321

RANGED = "RANGED"
MELEE = "MELEE"
MAGIC = "MAGIC"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def require_regex(text: str, pattern: str, message: str) -> None:
    if re.search(pattern, text, flags=re.S) is None:
        fail(message)


def near(actual: float, expected: float, message: str) -> None:
    if abs(actual - expected) > 0.000001:
        fail(f"{message}: expected {expected}, found {actual}")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return "\n".join(line.split("//", 1)[0] for line in text.splitlines())


def parse_item_ids() -> dict[str, int]:
    item_text = ITEM_ID.read_text(encoding="utf-8")
    return {
        match.group(1): int(match.group(2))
        for match in re.finditer(r"\b([A-Z0-9_]+)\((-?\d+)\)", item_text)
    }


ITEM_IDS = parse_item_ids()


def resolve_java_int(token: str) -> int:
    token = token.strip()
    if re.fullmatch(r"-?\d+", token):
        return int(token)
    match = re.fullmatch(r"ItemId\.([A-Z0-9_]+)\.id\(\)", token)
    if match and match.group(1) in ITEM_IDS:
        return ITEM_IDS[match.group(1)]
    fail(f"Could not resolve Java int token: {token!r}")


def parse_int_array(name: str) -> list[int]:
    text = strip_comments(EFFECTS.read_text(encoding="utf-8"))
    pattern = rf"private static final int\[\]\s+{re.escape(name)}\s*=\s*\{{(.*?)\}};"
    match = re.search(pattern, text, flags=re.S)
    if not match:
        fail(f"Missing int[] {name}")
    return [
        resolve_java_int(token)
        for token in match.group(1).replace("\n", " ").split(",")
        if token.strip()
    ]


def parse_double_array(name: str) -> list[float]:
    text = strip_comments(EFFECTS.read_text(encoding="utf-8"))
    pattern = rf"private static final double\[\]\s+{re.escape(name)}\s*=\s*\{{(.*?)\}};"
    match = re.search(pattern, text, flags=re.S)
    if not match:
        fail(f"Missing double[] {name}")
    return [
        float(token.strip().removesuffix("D"))
        for token in match.group(1).replace("\n", " ").split(",")
        if token.strip()
    ]


ELEMENTAL_RINGS = {
    AIR: [1673, 1674, 1675, 1676, 1677],
    WATER: [1678, 1679, 1680, 1681, 1682],
    EARTH: [1683, 1684, 1685, 1686, 1687],
    FIRE: [1688, 1689, 1690, 1691, 1692],
}
ELEMENTAL_AMULETS = {
    AIR: [1593, 1594, 1595, 1596, 1597],
    WATER: [1598, 1599, 1600, 1601, 1602],
    EARTH: [1603, 1604, 1605, 1606, 1607],
    FIRE: [1608, 1609, 1610, 1611, 1612],
}
STANDARD_NECKLACES = {
    AIR: [1613, 1614, 1615, 1616, 1617],
    MIND: [1618, 1619, 1620, 1621, 1622],
    WATER: [1623, 1624, 1625, 1626, 1627],
    EARTH: [1628, 1629, 1630, 1631, 1632],
    FIRE: [1633, 1634, 1635, 1636, 1637],
    BODY: [1638, 1639, 1640, 1641, 1642],
    COSMIC: [1643, 1644, 1645, 1646, 1647],
    CHAOS: [1648, 1649, 1650, 1651, 1652],
    NATURE: [1653, 1654, 1655, 1656, 1657],
    LAW: [1658, 1659, 1660, 1661, 1662],
    DEATH: [1663, 1664, 1665, 1666, 1667],
    BLOOD: [1668, 1669, 1670, 1671, 1672],
}


def special_rings() -> dict[int, list[int]]:
    return {
        MIND: parse_int_array("MIND_RINGS"),
        BODY: parse_int_array("BODY_RINGS"),
        CHAOS: parse_int_array("CHAOS_RINGS"),
        NATURE: parse_int_array("NATURE_RINGS"),
        COSMIC: parse_int_array("COSMIC_RINGS"),
        LAW: parse_int_array("LAW_RINGS"),
        DEATH: parse_int_array("DEATH_RINGS"),
        BLOOD: parse_int_array("BLOOD_RINGS"),
        SOUL: parse_int_array("SOUL_RINGS"),
        LIFE: parse_int_array("LIFE_RINGS"),
    }


def special_amulets() -> dict[int, list[int]]:
    return {
        LAW: parse_int_array("LAW_AMULETS"),
        CHAOS: parse_int_array("CHAOS_AMULETS"),
        DEATH: parse_int_array("DEATH_AMULETS"),
        BLOOD: parse_int_array("BLOOD_AMULETS"),
        MIND: parse_int_array("MIND_AMULETS"),
        BODY: parse_int_array("BODY_AMULETS"),
        NATURE: parse_int_array("NATURE_AMULETS"),
        COSMIC: parse_int_array("COSMIC_AMULETS"),
        SOUL: parse_int_array("SOUL_AMULETS"),
        LIFE: parse_int_array("LIFE_AMULETS"),
    }


SPECIAL_RINGS = special_rings()
SPECIAL_AMULETS = special_amulets()
SOUL_NECKLACES = parse_int_array("SOUL_NECKLACES")
LIFE_NECKLACES = parse_int_array("LIFE_NECKLACES")
MIND_BODY_JEWELRY_XP_BONUSES = parse_int_array("MIND_BODY_JEWELRY_XP_BONUSES")
NATURE_RING_FOOD_BONUSES = parse_double_array("NATURE_RING_FOOD_BONUSES")
CHAOS_RECOIL_CHANCES = parse_double_array("CHAOS_RECOIL_CHANCES")
CHAOS_CHAIN_LIGHTNING_CHANCES = parse_double_array("CHAOS_CHAIN_LIGHTNING_CHANCES")
CHAOS_AMULET_RANDOM_RUNE_INTERVALS = parse_int_array("CHAOS_AMULET_RANDOM_RUNE_INTERVALS")
LAW_BANKING_CHARGES = parse_int_array("LAW_BANKING_CHARGES")
NATURE_ALCHEMY_AMULET_CHARGES = parse_int_array("NATURE_ALCHEMY_AMULET_CHARGES")
GATHERING_AMULET_YIELD_BONUSES = parse_int_array("GATHERING_AMULET_YIELD_BONUSES")


def tier(item_id: int, line: list[int]) -> int:
    return line.index(item_id) + 1 if item_id in line else -1


def tier_for(item_id: int, lines: dict[int, list[int]], altar: int) -> int:
    return tier(item_id, lines.get(altar, []))


def nature_food_bonus(item_id: int) -> float:
    ring_tier = tier_for(item_id, SPECIAL_RINGS, NATURE)
    if ring_tier != -1:
        return NATURE_RING_FOOD_BONUSES[ring_tier - 1]
    return 0.0


def mind_body_jewelry_xp_bonus(item_id: int, lines: dict[int, list[int]], altar: int) -> float:
    jewelry_tier = tier_for(item_id, lines, altar)
    if jewelry_tier != -1:
        return MIND_BODY_JEWELRY_XP_BONUSES[jewelry_tier - 1] / 100.0
    return 0.0


def elemental_power(item_id: int, style: str) -> int:
    return elemental_power_from_lines(item_id, ELEMENTAL_RINGS, style)


def elemental_power_from_lines(item_id: int, lines: dict[int, list[int]], style: str) -> int:
    water_tier = tier_for(item_id, lines, WATER)
    if water_tier != -1:
        return water_tier * 2
    if style == RANGED:
        air_tier = tier_for(item_id, lines, AIR)
        return 0 if air_tier == -1 else air_tier * 3
    if style == MELEE:
        earth_tier = tier_for(item_id, lines, EARTH)
        return 0 if earth_tier == -1 else earth_tier * 3
    if style == MAGIC:
        fire_tier = tier_for(item_id, lines, FIRE)
        return 0 if fire_tier == -1 else fire_tier * 3
    return 0


def elemental_defense(item_id: int, style: str) -> int:
    water_tier = tier_for(item_id, STANDARD_NECKLACES, WATER)
    if water_tier != -1:
        return water_tier * 2
    if style == RANGED:
        air_tier = tier_for(item_id, STANDARD_NECKLACES, AIR)
        return 0 if air_tier == -1 else air_tier * 3
    if style == MELEE:
        earth_tier = tier_for(item_id, STANDARD_NECKLACES, EARTH)
        return 0 if earth_tier == -1 else earth_tier * 3
    if style == MAGIC:
        fire_tier = tier_for(item_id, STANDARD_NECKLACES, FIRE)
        return 0 if fire_tier == -1 else fire_tier * 3
    return 0


def death_low_health_bonus(item_id: int, lines: dict[int, list[int]], altar: int, current_hits: int, max_hits: int) -> int:
    item_tier = tier_for(item_id, lines, altar)
    if item_tier <= 0 or current_hits >= max_hits:
        return 0
    missing_percent = max(0.0, (max_hits - current_hits) / float(max_hits))
    if item_tier == 1:
        step, bonus_per_step = 0.25, 1
    elif item_tier == 2:
        step, bonus_per_step = 0.20, 1
    elif item_tier == 3:
        step, bonus_per_step = 0.15, 1
    elif item_tier == 4:
        step, bonus_per_step = 0.10, 1
    else:
        step, bonus_per_step = 0.10, 2
    return math.floor((missing_percent / step) + 0.0000001) * bonus_per_step


def law_item_max_charges(item_id: int) -> int:
    if tier_for(item_id, SPECIAL_AMULETS, LAW) != -1:
        return 3
    item_tier = tier_for(item_id, SPECIAL_RINGS, LAW)
    if item_tier == -1:
        item_tier = tier_for(item_id, STANDARD_NECKLACES, LAW)
    return 0 if item_tier == -1 else LAW_BANKING_CHARGES[item_tier - 1]


def ensure_formula_source_matches_design() -> None:
    require(SPECIAL_RINGS[CHAOS] == [ITEM_IDS["RING_OF_RECOIL"], 1693, 1694, 1695, 1696],
            "Chaos ring line should keep the vanilla recoil ring as tier 1")
    require(SPECIAL_RINGS[NATURE] == [ITEM_IDS["RING_OF_FORGING"], 1697, 1698, 1699, 1700],
            "Nature ring line should keep the vanilla forging ring as tier 1")
    require(SPECIAL_RINGS[COSMIC][-1] == ITEM_IDS["DRAGONSTONE_RING_OF_FORTUNE"],
            "Cosmic ring line should end on the dragonstone fortune ring")
    require(SPECIAL_RINGS[SOUL][3] == ITEM_IDS["RING_OF_LIFE"],
            "Soul ring line should preserve the vanilla ring of life at diamond tier")

    require(elemental_power(1673, RANGED) == 3, "Sapphire air ring should give +3 ranged power")
    require(elemental_power(1677, RANGED) == 15, "Dragonstone air ring should give +15 ranged power")
    require(elemental_power(1687, MELEE) == 15, "Dragonstone earth ring should give +15 melee power")
    require(elemental_power(1692, MAGIC) == 15, "Dragonstone fire ring should give +15 magic power")
    require(elemental_power(1627, RANGED) == 0, "Dragonstone water necklace should not retain power bonus")

    require(elemental_defense(1617, RANGED) == 15, "Dragonstone air necklace should give +15 ranged defense")
    require(elemental_defense(1632, MELEE) == 15, "Dragonstone earth necklace should give +15 melee defense")
    require(elemental_defense(1637, MAGIC) == 15, "Dragonstone fire necklace should give +15 magic defense")
    require(elemental_defense(1627, RANGED) == 10, "Dragonstone water necklace should give +10 ranged defense")
    require(elemental_defense(1627, MELEE) == 10, "Dragonstone water necklace should give +10 melee defense")
    require(elemental_defense(1627, MAGIC) == 10, "Dragonstone water necklace should give +10 magic defense")

    require(CHAOS_RECOIL_CHANCES == [0.10, 0.20, 0.30, 0.50, 0.90],
            "Chaos recoil should use the requested non-linear chance curve")
    require(CHAOS_CHAIN_LIGHTNING_CHANCES == [0.10, 0.20, 0.30, 0.50, 0.90],
            "Chaos chain lightning should use the requested non-linear chance curve")
    near(CHAOS_RECOIL_CHANCES[tier_for(1314, SPECIAL_RINGS, CHAOS) - 1], 0.10, "Sapphire chaos ring recoil chance")
    near(CHAOS_RECOIL_CHANCES[tier_for(1696, SPECIAL_RINGS, CHAOS) - 1], 0.90, "Dragonstone chaos ring recoil chance")
    near(CHAOS_CHAIN_LIGHTNING_CHANCES[tier_for(1648, STANDARD_NECKLACES, CHAOS) - 1], 0.10, "Sapphire chaos necklace chain chance")
    near(CHAOS_CHAIN_LIGHTNING_CHANCES[tier_for(1652, STANDARD_NECKLACES, CHAOS) - 1], 0.90, "Dragonstone chaos necklace chain chance")
    require(CHAOS_AMULET_RANDOM_RUNE_INTERVALS == [60, 55, 50, 40, 20],
            "Chaos amulets should use the requested random-rune production intervals")

    near(tier_for(1701, SPECIAL_RINGS, COSMIC) * 0.05, 0.05, "Sapphire cosmic ring wealth chance")
    near(tier_for(3111, SPECIAL_RINGS, COSMIC) * 0.05, 0.25, "Dragonstone cosmic ring wealth chance")
    near(tier_for(1647, STANDARD_NECKLACES, COSMIC) * 0.10, 0.50, "Dragonstone cosmic necklace extra standard-drop chance")
    near(tier_for(1753, SPECIAL_AMULETS, COSMIC) * 0.10, 0.50, "Dragonstone cosmic amulet rare-gathering double chance")

    for item_id in SPECIAL_AMULETS[LAW]:
        require(law_item_max_charges(item_id) == 3, f"Law amulet {item_id} should have 3 teleport charges")
    require(LAW_BANKING_CHARGES == [100, 200, 300, 500, 1000],
            "Law banking jewelry should use the requested charge ladder")
    require(law_item_max_charges(1714) == 100, "Sapphire law ring should have 100 banking charges")
    require(law_item_max_charges(1718) == 1000, "Dragonstone law ring should have 1000 banking charges")
    require(law_item_max_charges(1658) == 100, "Sapphire law necklace should have 100 banking charges")
    require(law_item_max_charges(1662) == 1000, "Dragonstone law necklace should have 1000 banking charges")

    require(tier_for(3095, SPECIAL_RINGS, BLOOD) * 2 == 10, "Dragonstone blood ring should give +10 hits")
    require(tier_for(1672, STANDARD_NECKLACES, BLOOD) * 2 == 10, "Dragonstone blood necklace should give +10 hits")
    near(tier_for(1733, SPECIAL_AMULETS, BLOOD) * 0.05, 0.25, "Dragonstone blood amulet lifesteal chance")

    require(death_low_health_bonus(3086, SPECIAL_RINGS, DEATH, 75, 100) == 1,
            "Sapphire death ring should grant +1 power at 25% missing hits")
    require(death_low_health_bonus(3090, SPECIAL_RINGS, DEATH, 50, 100) == 10,
            "Dragonstone death ring should grant +10 power at 50% missing hits")
    require(death_low_health_bonus(1667, STANDARD_NECKLACES, DEATH, 50, 100) == 10,
            "Dragonstone death necklace should grant +10 defense at 50% missing hits")
    require(tier_for(1724, SPECIAL_AMULETS, DEATH) == 1, "Sapphire death amulet should be tier 1")
    require(tier_for(1728, SPECIAL_AMULETS, DEATH) == 5, "Dragonstone death amulet should be tier 5")

    require(MIND_BODY_JEWELRY_XP_BONUSES == [5, 10, 15, 25, 50],
            "Mind and Body jewelry should use the requested non-linear XP curve")
    near(mind_body_jewelry_xp_bonus(1621, STANDARD_NECKLACES, MIND), 0.25, "Diamond mind necklace XP chance")
    near(mind_body_jewelry_xp_bonus(1622, STANDARD_NECKLACES, MIND), 0.50, "Dragonstone mind necklace XP chance")
    near(mind_body_jewelry_xp_bonus(3079, SPECIAL_RINGS, MIND), 0.25, "Diamond mind ring XP chance")
    near(mind_body_jewelry_xp_bonus(3080, SPECIAL_RINGS, MIND), 0.50, "Dragonstone mind ring XP chance")
    near(mind_body_jewelry_xp_bonus(1738, SPECIAL_AMULETS, MIND), 0.50, "Dragonstone mind amulet combat XP chance")
    near(mind_body_jewelry_xp_bonus(1641, STANDARD_NECKLACES, BODY), 0.25, "Diamond body necklace XP chance")
    near(mind_body_jewelry_xp_bonus(1642, STANDARD_NECKLACES, BODY), 0.50, "Dragonstone body necklace XP chance")
    near(mind_body_jewelry_xp_bonus(3084, SPECIAL_RINGS, BODY), 0.25, "Diamond body ring XP chance")
    near(mind_body_jewelry_xp_bonus(3085, SPECIAL_RINGS, BODY), 0.50, "Dragonstone body ring XP chance")
    near(mind_body_jewelry_xp_bonus(1743, SPECIAL_AMULETS, BODY), 0.50, "Dragonstone body amulet discipline XP chance")

    near(nature_food_bonus(1657), 0.0, "Dragonstone nature necklace should no longer give food bonus")
    near(nature_food_bonus(1700), 1.00, "Dragonstone nature ring food bonus")
    near(nature_food_bonus(1748), 0.0, "Dragonstone nature amulet should not retain stale food bonus")
    require(tier_for(1657, STANDARD_NECKLACES, NATURE) == 5, "Dragonstone nature necklace should add 5 poison decay power")
    require(NATURE_ALCHEMY_AMULET_CHARGES == [100, 200, 300, 500, 1000],
            "Nature alchemy amulets should use the requested charge ladder")
    require(GATHERING_AMULET_YIELD_BONUSES == [10, 20, 30, 50, 100],
            "Gathering amulets should use the requested non-linear yield curve")

    require(tier_for(1708, SPECIAL_RINGS, SOUL) == 5, "Dragonstone soul ring should keep 5 extra death items")
    require(tier(1763, SOUL_NECKLACES) == 5, "Dragonstone soul necklace should keep 5 extra death items")
    near(tier_for(1758, SPECIAL_AMULETS, SOUL) * 0.10, 0.50, "Dragonstone soul amulet survival chance")

    require(tier(3105, LIFE_NECKLACES) * 10 == 50, "Dragonstone life necklace should add 50% summon health")
    require(tier_for(3100, SPECIAL_RINGS, LIFE) * 20 == 100, "Dragonstone life ring should double support-summon duration")
    require(tier_for(3110, SPECIAL_AMULETS, LIFE) == 5, "Dragonstone life amulet should add +5 summon max damage")


def ensure_runtime_paths_are_wired() -> None:
    effects = EFFECTS.read_text(encoding="utf-8")
    item_defs_custom = ITEM_DEFS_CUSTOM.read_text(encoding="utf-8")
    client_entity_handler = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    equipment = EQUIPMENT.read_text(encoding="utf-8")
    npc = NPC.read_text(encoding="utf-8")
    drop_table = DROP_TABLE.read_text(encoding="utf-8")
    player = PLAYER.read_text(encoding="utf-8")
    poison_event = POISON_EVENT.read_text(encoding="utf-8")
    eating = EATING.read_text(encoding="utf-8")
    summoning = SUMMONING.read_text(encoding="utf-8")
    combat_event = COMBAT_EVENT.read_text(encoding="utf-8")
    pvm_melee = PVM_MELEE_EVENT.read_text(encoding="utf-8")
    projectile_event = PROJECTILE_EVENT.read_text(encoding="utf-8")
    law_jewelry = LAW_JEWELRY.read_text(encoding="utf-8")
    nature_alchemy_amulet = NATURE_ALCHEMY_AMULET.read_text(encoding="utf-8")
    fishing = FISHING.read_text(encoding="utf-8")
    mining = MINING.read_text(encoding="utf-8")
    woodcutting = WOODCUTTING.read_text(encoding="utf-8")
    harvesting = HARVESTING.read_text(encoding="utf-8")

    require("public static double getCosmicAmuletExtraResourceChance" in effects
            and "return 0.0D;" in effects,
            "Cosmic amulet should not inflate base resource gather chances")
    require("getCosmicItemMaxCharges" not in effects
            and "COSMIC_BANKING_CHARGES_CACHE_PREFIX" not in effects,
            "Cosmic jewelry should remain passive bonus jewelry, not charged jewelry")
    require("getChaosRecoilChance()" in equipment
            and "return ringItem == null ? 0.0D : EnchantingItemEffects.getChaosRingRecoilChance(ringItem.getCatalogId());" in equipment,
            "Chaos recoil should come only from the ring")
    require_regex(equipment, r"getChaosRecoilDamageDivisor\(\)\s*\{\s*return 10;\s*\}",
                  "Chaos recoil should use the ring-only damage divisor")
    require("getChaosAmuletRandomRuneInterval()" in equipment
            and "EnchantingItemEffects.getChaosAmuletRandomRuneInterval(neckItem.getCatalogId())" in equipment,
            "Chaos amulet random-rune interval should be exposed through equipped neck items")
    require_regex(equipment, r"getEquippedElementalPowerBonus.*?getElementalPowerBonus\(ringItem",
                  "Elemental power should come from the equipped ring")
    require_regex(equipment, r"getEquippedElementalDefenseBonus.*?getElementalDefenseBonus\(neckItem",
                  "Elemental defense should come from the equipped necklace slot")

    require("getWealthChance(owner)" in drop_table, "Cosmic rings should feed drop-table wealth chance")
    require("allowExtraRoll && !result.receivedRareTableReward && wealthChance > 0.0D" in drop_table,
            "Cosmic rings should only reroll when the primary roll produced no rare-table reward")
    require("result.merge(rollRareTableChance(owner, contributionScale));" in drop_table,
            "Cosmic rings should only retry the monster rare-table chance")
    require("drop.type == dropType.TABLE && drop.table.isRare()" in drop_table,
            "Ring of wealth retry should ignore standard monster-table drops")
    require("result.hitRareTable = true;" in drop_table,
            "DropTable should track when a roll reaches a rare table")
    require("If a monster rare table misses, has a 5% chance to reroll the drop." in item_defs_custom,
            "Sapphire cosmic ring examine should describe rare-table miss rerolls")
    require("If a monster rare table misses, has a 25% chance to reroll the drop." in item_defs_custom,
            "Dragonstone cosmic ring examine should describe rare-table miss rerolls")
    require("If a monster rare table misses, has a %d%% chance to reroll the drop." in client_entity_handler,
            "Client cosmic ring examine generator should describe rare-table miss rerolls")
    require("If a monster rare table misses, has a 25% chance to reroll the drop." in client_entity_handler,
            "Client dragonstone cosmic ring examine should describe rare-table miss rerolls")
    require("getCosmicNecklaceStandardDropChance(owner)" in drop_table,
            "Cosmic necklaces should feed extra standard drop rolls")
    require("suppressRareTables" in drop_table and "Your cosmic necklace gleams." in drop_table,
            "Cosmic necklace extra rolls should suppress rare-table access")

    for path, text in (
        (FISHING, FISHING.read_text(encoding="utf-8")),
        (MINING, MINING.read_text(encoding="utf-8")),
        (WOODCUTTING, WOODCUTTING.read_text(encoding="utf-8")),
        (HARVESTING, HARVESTING.read_text(encoding="utf-8")),
    ):
        require("maybeDoubleRareGatheringReward" in text
                and "getCosmicAmuletRareGatheringDoubleChance" in text,
                f"Cosmic amulet rare-gathering double path missing from {path.name}")

    require("POWER_DRAIN_PER_TICK = 3" in poison_event, "Poison base drain should remain 3 power per tick")
    require("poisonDrain += player.getCarriedItems().getEquipment().getNatureCleansingPoisonDecayBonus();" in poison_event,
            "Nature cleansing necklace should add to poison tick drain")
    require("getNatureFoodHealingBonus" in eating and "Math.ceil(totalHeal * (1.0D + foodBonus))" in eating,
            "Nature jewelry should increase food healing")
    require("getNatureFoodHealingBonus()" in equipment
            and "getNatureFoodHealingBonus(ringItem.getCatalogId())" in equipment,
            "Nature ring should provide food healing")
    require("getNatureFoodHealingBonus(neckItem.getCatalogId())" not in equipment
            and "Math.max(neckBonus, ringBonus)" not in equipment,
            "Nature necklace should not continue providing food healing")
    require("public static int getNatureAlchemyAmuletMaxCharges" in effects
            and "getNatureAlchemyAmuletCharges(final Player player, final Item item)" in effects
            and "setNatureAlchemyAmuletCharges(final Player player, final Item item, final int charges)" in effects
            and "NATURE_ALCHEMY_CHARGES_CACHE_PREFIX" in effects,
            "Nature alchemy amulet charges should be account-backed")
    require("tryAlchemyMonsterLootWithNatureAmulet" in equipment
            and "EnchantingItemEffects.getNatureAlchemyAmuletCharges(player, neckItem)" in equipment
            and "EnchantingItemEffects.setNatureAlchemyAmuletCharges(player, neckItem, charges)" in equipment
            and "getHighAlchemyValue(itemDef, item.getAmount())" in equipment
            and "alchemyValue < 1000" in equipment
            and "new Item(ItemId.COINS.id(), alchemyValue)" in equipment
            and "player.getCarriedItems().getInventory().canHold(coins)" in equipment
            and "remove(neckItem, 1, true);" not in equipment,
            "Nature alchemy amulet should auto-alch valuable monster drops without breaking")
    require("tryAlchemyMonsterLootWithNatureAmulet(new Item(dropID, amount))" in npc
            and "tryAlchemyMonsterLootWithNatureAmulet(new Item(dropID, amount, item.getNoted()))" in npc
            and "tryAlchemyMonsterLootWithNatureAmulet(item)" in npc,
            "NPC monster drop paths should offer drops to the nature alchemy amulet")
    require("implements OpInvTrigger, UseLocTrigger" in nature_alchemy_amulet
            and '"check".equalsIgnoreCase(command)' in nature_alchemy_amulet
            and "EnchantingItemEffects.NATURE_ALTAR" in nature_alchemy_amulet
            and "ItemId.NATURE_RUNE.id()" in nature_alchemy_amulet
            and "getRechargeRuneCost" in nature_alchemy_amulet
            and "return Math.max(10, ((maxCharges + 99) / 100) * 10);" in nature_alchemy_amulet
            and "setNatureAlchemyAmuletCharges(player, item, maxCharges)" in nature_alchemy_amulet,
            "Nature alchemy amulet should support Check and full-capacity nature-rune recharging")
    for snippet in (
        'GATHERING_AMULET_YIELD_CACHE_PREFIX = "gathering_amulet_yield_bonus_"',
        "GATHERING_AMULET_YIELD_CACHE_PREFIX + skillId + \"_\" + itemId",
        "itemCount * bonusPercent * GATHERING_AMULET_YIELD_POINTS_PER_PERCENT",
        "totalPoints / GATHERING_AMULET_YIELD_POINTS_PER_ITEM",
        "Skill.WOODCUTTING.id()",
        "Skill.FISHING.id()",
        "Skill.HARVESTING.id()",
        "Skill.MINING.id()",
    ):
        require(snippet in effects, f"Gathering amulet shared carryover missing: {snippet}")
    for text, snippets, label in (
        (woodcutting, (
            "int rewardQuantity = quantity + addGatheringAmuletBonusLogs(player, def.getLogId(), quantity);",
            "Skill.WOODCUTTING.id(), logId, logCount",
            "bankSkillingDropWithLawRing(new Item(def.getLogId(), rewardQuantity))",
        ), "woodcutting"),
        (mining, (
            "int rewardQuantity = quantity + addGatheringAmuletBonusOre(player, ore.getCatalogId(), quantity);",
            "Skill.MINING.id(), oreId, oreCount",
            "bankSkillingDropWithLawRing(new Item(ore.getCatalogId(), rewardQuantity))",
        ), "mining"),
        (fishing, (
            "final int rewardQuantity = 1 + addGatheringAmuletBonusFish(player, fish.getCatalogId(), 1);",
            "Skill.FISHING.id(), fishId, fishCount",
            "awardFishingCatch(player, object, fish, true);",
        ), "fishing"),
        (harvesting, (
            "int rewardQuantity = quantity + addGatheringAmuletBonusProduce(player, produce.getCatalogId(), quantity);",
            "Skill.HARVESTING.id(), produceId, produceCount",
            "bankSkillingDropWithLawRing(new Item(produce.getCatalogId(), rewardQuantity))",
        ), "harvesting"),
    ):
        for snippet in snippets:
            require(snippet in text, f"Gathering amulet carryover missing from {label}: {snippet}")

    require("getMindJewelryXpBonus(skill)" in player and "getBodyJewelryXpBonus(skill)" in player,
            "Mind and body jewelry XP bonuses should apply through the unified skill-aware path")
    require("isMindNecklaceXpSkill(skillId)" in equipment and "getMindNecklaceXpBonus(neckId)" in equipment,
            "Mind necklaces should apply only to their mapped skills")
    require("isMindRingXpSkill(skillId)" in equipment and "getMindRingXpBonus(ringItem.getCatalogId())" in equipment,
            "Mind rings should apply only to their mapped skills")
    require("isBodyNecklaceXpSkill(skillId)" in equipment and "getBodyNecklaceXpBonus(neckId)" in equipment,
            "Body necklaces should apply only to their mapped skills")
    require("isBodyRingXpSkill(skillId)" in equipment and "getBodyRingXpBonus(ringItem.getCatalogId())" in equipment,
            "Body rings should apply only to their mapped skills")
    require("isMindCombatAmuletXpSkill(skillId)" in equipment and "getMindCombatAmuletXpBonus(neckId)" in equipment,
            "Mind amulets should apply only to their mapped skills")
    require("isBodyCombatAmuletXpSkill(skillId)" in equipment and "getBodyDisciplineAmuletXpBonus(neckId)" in equipment,
            "Body amulets should apply only to their mapped skills")

    require("applyBloodAmuletLifesteal" in player and "HitSplat.TYPE_HEAL" in player,
            "Blood amulet lifesteal should heal with a heal hitsplat")
    require("syncHitsEquipmentBonuses" in player and "getBloodAmuletHitsBonus" in player,
            "Blood ring and necklace should synchronize max Hits bonuses")
    require("applyDeathAmuletBurst" in player and "HitSplat.TYPE_ARMOR_PROC" in player,
            "Death amulet bursts should use non-direct-combat yellow hitsplats")
    require("getSoulAmuletExtraKeptItems" in equipment and "extraKeptItems" in (ROOT / "server/src/com/openrsc/server/model/container/Inventory.java").read_text(encoding="utf-8"),
            "Soul ring and necklace should feed death item retention")
    require("checkRingOfLife" in player and "getSoulAmuletSurvivalChance" in player,
            "Soul amulet should own the life-saving break chance")

    require("getLifeNecklaceSummonHealthPercent" in summoning,
            "Life necklace should increase combat summon health")
    require("getLifeRingSupportDurationPercent" in summoning,
            "Life ring should extend support summon duration")
    require("getLifeAmuletSummonMaxDamageBonus" in summoning,
            "Life amulet should increase combat summon max damage")

    for text, label in (
        (combat_event, "melee"),
        (pvm_melee, "PvM melee"),
        (projectile_event, "projectile"),
    ):
        require("getChaosNecklaceChainLightningChance" in text
                and "CHAOS_CHAIN_LIGHTNING_MAX_HOPS = 3" in text
                and "selectChaosChainLightningTarget" in text
                and "DataConversions.getRandom().nextDouble() >= chainChance" in text
                and "new Projectile(anchor, chainTarget, Projectile.MAGIC)" in text
                and "Math.ceil(chainDamage / 2.0D)" in text
                and "!Summoning.isSummon(npc)" in text
                and "HitSplat.TYPE_ARMOR_PROC" in text,
                f"Chaos necklace {label} chain lightning should roll each hop, show projectiles, avoid summons, and use yellow hitsplats")
    require("getChaosRecoilChance" in combat_event
            and "getChaosRecoilChance" in pvm_melee
            and "getChaosRecoilChance" in projectile_event
            and "HitSplat.TYPE_ARMOR_PROC" in combat_event
            and "HitSplat.TYPE_ARMOR_PROC" in pvm_melee
            and "HitSplat.TYPE_ARMOR_PROC" in projectile_event,
            "Chaos recoil should use yellow hitsplats across melee and projectile paths")

    require("getLawItemMaxCharges" in law_jewelry
            and "new Item(target.getCatalogId(), target.getAmount(), target.getNoted())" in law_jewelry,
            "Law amulet manual banking should preserve selected stack amount and note state")
    require("public static boolean isLawBankingNecklace" in effects
            and "public static boolean isLawBankingItem" in effects
            and "LAW_BANKING_CHARGES_CACHE_PREFIX" in effects,
            "Law banking necklace/ring charge helpers should be account-backed")
    require("getLawBankingItemCharges(final Player player, final Item item)" in effects
            and "setLawBankingItemCharges(final Player player, final Item item, final int charges)" in effects
            and "player.getCache().set(getLawBankingChargesCacheKey(item.getCatalogId()), clampedCharges);" in effects,
            "Law banking necklace/ring charges should be stored on the player cache")
    require("EnchantingItemEffects.isLawBankingItem(item.getCatalogId())" in law_jewelry
            and "EnchantingItemEffects.getLawBankingItemCharges(player, item)" in law_jewelry
            and "EnchantingItemEffects.setLawBankingItemCharges(player, item, charges)" in law_jewelry,
            "Manual law ring use and recharge should share account-backed banking charges")
    require("chargeLawRunesForBankingRecharge" in law_jewelry
            and "getLawBankingRechargeRuneCost" in law_jewelry
            and "return Math.max(10, (missingCharges + 9) / 10);" in law_jewelry
            and "You need \" + requiredRunes + \" law runes to recharge this jewelry." in law_jewelry,
            "Law banking necklaces/rings should cost 10 law runes per 100 missing charges with a 10-rune minimum")
    require("tryBankMonsterLootWithLawNecklace" in equipment
            and "final int chargeCost = Math.max(1, item.getAmount());" in equipment
            and "EnchantingItemEffects.getLawBankingItemCharges(player, neckItem)" in equipment
            and "EnchantingItemEffects.setLawBankingItemCharges(player, neckItem, charges)" in equipment
            and "Your law necklace sends the loot to your bank and runs out of charges." in equipment
            and "remove(neckItem, 1, true);" not in equipment,
            "Law necklace monster-loot banking should spend account-backed charges and run out at zero")
    require("bankSkillingDropWithLawRing" in equipment
            and "if (item.getDef(player.getWorld()).isStackable())" in equipment
            and "EnchantingItemEffects.getLawBankingItemCharges(player, ringItem)" in equipment
            and "EnchantingItemEffects.setLawBankingItemCharges(player, ringItem, charges)" in equipment
            and "Your law ring sends your resources to your bank and runs out of charges." in equipment
            and "remove(ringItem, 1, true);" not in equipment
            and "for (int count = 0; count < item.getAmount() && charges > 0; count++)" in equipment,
            "Law ring skilling banking should skip stackables and charge per unstackable item from account-backed charges")


def main() -> None:
    ensure_formula_source_matches_design()
    ensure_runtime_paths_are_wired()
    print("PASS: jewelry runtime effect formulas and wiring validated")


if __name__ == "__main__":
    main()
