#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RUNECRAFT = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/runecraft/Runecraft.java"
ALTARS = ROOT / "server/conf/server/defs/extras/ObjectRunecraft.xml"
ANALYSIS = ROOT / "docs/myworld/in-progress-work-plans/rune-crafting-xp-redesign-analysis.md"

# name: (unlock level, configured internal quarter-XP per base rune, level-99 multiplier)
RUNE_CONFIG = {
    "air": (1, 20, 10),
    "water": (1, 20, 10),
    "earth": (1, 20, 10),
    "fire": (1, 20, 10),
    "life": (1, 20, 10),
    "mind": (8, 21, 10),
    "body": (15, 27, 9),
    "chaos": (22, 28, 8),
    "cosmic": (30, 29, 7),
    "nature": (38, 30, 7),
    "law": (46, 94, 6),
    "death": (54, 168, 5),
    "soul": (62, 186, 4),
    "blood": (70, 256, 3),
}

EXPECTED_LEVEL_99_FULL_INVENTORY_XP = {
    "air": 6_000,
    "water": 6_000,
    "earth": 6_000,
    "fire": 6_000,
    "life": 6_000,
    "mind": 6_300,
    "body": 7_290,
    "chaos": 6_720,
    "cosmic": 6_090,
    "nature": 6_300,
    "law": 16_920,
    "death": 25_200,
    "soul": 22_320,
    "blood": 23_040,
}

PRE_IMPLEMENTATION_DISPLAYED_XP = {
    "air": 9.0,
    "water": 10.5,
    "earth": 12.0,
    "fire": 13.5,
    "life": 9.0,
    "mind": 9.0,
    "body": 15.0,
    "chaos": 21.0,
    "cosmic": 18.0,
    "nature": 24.0,
    "law": 27.0,
    "death": 31.5,
    "soul": 37.5,
    "blood": 45.0,
}

PRE_IMPLEMENTATION_ROUTE_XP_HOUR = {
    "nature": 134_600,
    "blood": 75_300,
    "soul": 68_500,
    "death": 51_800,
    "body": 44_400,
    "water": 30_400,
    "fire": 30_100,
    "mind": 27_300,
    "chaos": 25_700,
    "air": 21_200,
    "law": 20_800,
    "cosmic": 19_500,
    "life": 17_000,
    "earth": 15_900,
}

EXPECTED_ROUTE_REGIMES = (
    (1, "water"),
    (8, "mind"),
    (11, "water"),
    (18, "mind"),
    (21, "water"),
    (28, "mind"),
    (31, "water"),
    (35, "body"),
    (38, "mind"),
    (41, "water"),
    (45, "body"),
    (48, "nature"),
    (51, "water"),
    (55, "body"),
    (58, "nature"),
    (64, "death"),
    (68, "nature"),
    (72, "soul"),
    (74, "death"),
    (78, "nature"),
    (80, "blood"),
    (82, "soul"),
    (84, "death"),
    (90, "blood"),
    (92, "soul"),
    (94, "death"),
)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def rune_multiplier(level: int, unlock: int) -> int:
    return 1 + ((level - unlock) // 10)


def action_internal_xp(internal_xp: int, multiplier: int, processed_stones: int) -> int:
    base_runes_crafted = multiplier * processed_stones
    return internal_xp * base_runes_crafted


def load_altar_config() -> dict[str, tuple[int, int]]:
    actual = {}
    for entry in ET.parse(ALTARS).getroot().findall("entry"):
        definition = entry.find("ObjectRunecraftDef")
        if definition is None:
            fail("ObjectRunecraft.xml contains an entry without a definition")
        name = definition.findtext("runeName", default="")
        actual[name] = (
            int(definition.findtext("requiredLvl", default="-1")),
            int(definition.findtext("exp", default="-1")),
        )
    return actual


def ensure_runtime_uses_successful_base_output(runecraft: str) -> None:
    craft_start = runecraft.index("private void craftRunesAtAltar")
    craft_end = runecraft.index("private void addLawRobeBonusRunes", craft_start)
    craft_method = runecraft[craft_start:craft_end]

    required_in_order = (
        "int repeatTimes = player.getCarriedItems().getInventory().countId",
        "for (int loop = 0; loop < repeatTimes; ++loop)",
        "if (player.getCarriedItems().remove(stone) == -1)",
        "baseRuneCount += craftedRunes;",
        "++processedStoneCount;",
        "if (processedStoneCount > 0)",
        "player.incExp(Skill.RUNECRAFT.id(), def.getExp() * baseRuneCount, true);",
        "addLawRobeBonusRunes(player, def.getRuneId(), baseRuneCount);",
        "addChaosAmuletBonusRunes(player, def.getRuneId(), baseRuneCount);",
    )
    position = -1
    for snippet in required_in_order:
        next_position = craft_method.find(snippet)
        if next_position <= position:
            fail(f"Runecraft successful-output ordering is missing or changed: {snippet}")
        position = next_position

    if craft_method.count("player.incExp(Skill.RUNECRAFT.id()") != 1:
        fail("A full inventory must award Enchanting XP in one action")

    for forbidden in (
        "def.getExp() * repeatTimes",
        "def.getExp() * processedStoneCount",
        "def.getExp() * successCount",
    ):
        if forbidden in craft_method:
            fail(f"Enchanting XP must use successful base-rune output, found: {forbidden}")


def ensure_multiplier_and_xp_breakpoints() -> None:
    for name, (unlock, internal_xp, level_99_multiplier) in RUNE_CONFIG.items():
        for level, expected_multiplier in (
            (unlock, 1),
            (unlock + 9, 1),
            (unlock + 10, 2),
            (80, rune_multiplier(80, unlock)) if unlock <= 80 else (99, level_99_multiplier),
            (90, rune_multiplier(90, unlock)) if unlock <= 90 else (99, level_99_multiplier),
            (99, level_99_multiplier),
        ):
            actual_multiplier = rune_multiplier(level, unlock)
            if actual_multiplier != expected_multiplier:
                fail(
                    f"{name} multiplier at level {level}: "
                    f"expected {expected_multiplier}, found {actual_multiplier}"
                )

            for processed_stones in (0, 1, 17, 30):
                expected_xp = internal_xp * expected_multiplier * processed_stones
                actual_xp = action_internal_xp(internal_xp, actual_multiplier, processed_stones)
                if actual_xp != expected_xp:
                    fail(
                        f"{name} XP at level {level} for {processed_stones} successful Stone: "
                        f"expected {expected_xp}, found {actual_xp}"
                    )

        full_inventory_xp = action_internal_xp(internal_xp, level_99_multiplier, 30)
        if full_inventory_xp != EXPECTED_LEVEL_99_FULL_INVENTORY_XP[name]:
            fail(
                f"{name} level-99 full-inventory XP: expected "
                f"{EXPECTED_LEVEL_99_FULL_INVENTORY_XP[name]}, found {full_inventory_xp}"
            )


def ensure_route_progression() -> None:
    route_regimes = []
    previous_winner = None
    for level in range(1, 100):
        route_rates = []
        for name, (unlock, internal_xp, _) in RUNE_CONFIG.items():
            if level < unlock:
                continue
            stone_throughput = (
                PRE_IMPLEMENTATION_ROUTE_XP_HOUR[name] / PRE_IMPLEMENTATION_DISPLAYED_XP[name]
            )
            displayed_xp_per_rune = internal_xp * 3 / 4
            route_rates.append(
                (stone_throughput * displayed_xp_per_rune * rune_multiplier(level, unlock), name)
            )
        winner = max(route_rates)[1]
        if winner != previous_winner:
            route_regimes.append((level, winner))
            previous_winner = winner

    if tuple(route_regimes) != EXPECTED_ROUTE_REGIMES:
        fail(f"Route-normalized method changes no longer match the accepted model: {route_regimes}")


def main() -> None:
    runecraft = RUNECRAFT.read_text(encoding="utf-8")
    analysis = ANALYSIS.read_text(encoding="utf-8")

    expected_altar_config = {
        name: (unlock, internal_xp) for name, (unlock, internal_xp, _) in RUNE_CONFIG.items()
    }
    actual_altar_config = load_altar_config()
    if actual_altar_config != expected_altar_config:
        fail(f"Configured rune XP values do not match the implemented model: {actual_altar_config}")

    for finding in (
        "retVal += (level - requiredLevel) / 10;",
        "final int earnedPoints = baseRuneCount * bonusPercent",
    ):
        if finding not in runecraft:
            fail(f"Runecraft implementation is missing: {finding}")

    ensure_runtime_uses_successful_base_output(runecraft)
    ensure_multiplier_and_xp_breakpoints()
    ensure_route_progression()

    for snippet in (
        "Status: implemented on this feature branch; pending review and merge.",
        "26 route-normalized optimal regimes",
        "25 actual method",
        "`1,035,616 XP/hour`",
        "`18,900` displayed XP per 30 Stone before equipment",
        "Law-robe and Chaos-amulet bonus runes remain excluded",
    ):
        if snippet not in analysis:
            fail(f"Rune XP implementation document is missing: {snippet}")

    print("PASS: implemented Enchanting XP values, breakpoints, processing, and route model validated")


if __name__ == "__main__":
    main()
