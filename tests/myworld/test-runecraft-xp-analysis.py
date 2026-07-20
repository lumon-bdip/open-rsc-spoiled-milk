#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RUNECRAFT = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/runecraft/Runecraft.java"
ALTARS = ROOT / "server/conf/server/defs/extras/ObjectRunecraft.xml"
ANALYSIS = ROOT / "docs/myworld/in-progress-work-plans/rune-crafting-xp-redesign-analysis.md"

CURRENT_INTERNAL_XP = {
    "air": 12,
    "water": 14,
    "earth": 16,
    "fire": 18,
    "life": 12,
    "mind": 12,
    "body": 20,
    "chaos": 28,
    "cosmic": 24,
    "nature": 32,
    "law": 36,
    "death": 42,
    "soul": 50,
    "blood": 60,
}

PROPOSED = {
    "Air": (1, 15.0),
    "Water": (1, 15.0),
    "Earth": (1, 15.0),
    "Fire": (1, 15.0),
    "Life": (1, 15.0),
    "Mind": (8, 15.75),
    "Body": (15, 20.25),
    "Chaos": (22, 21.0),
    "Cosmic": (30, 21.75),
    "Nature": (38, 22.5),
    "Law": (46, 70.5),
    "Death": (54, 126.0),
    "Soul": (62, 139.5),
    "Blood": (70, 192.0),
}

CURRENT_DISPLAYED_XP = {
    "Air": 9.0,
    "Water": 10.5,
    "Earth": 12.0,
    "Fire": 13.5,
    "Life": 9.0,
    "Mind": 9.0,
    "Body": 15.0,
    "Chaos": 21.0,
    "Cosmic": 18.0,
    "Nature": 24.0,
    "Law": 27.0,
    "Death": 31.5,
    "Soul": 37.5,
    "Blood": 45.0,
}

CURRENT_ROUTE_XP_HOUR = {
    "Nature": 134_600,
    "Blood": 75_300,
    "Soul": 68_500,
    "Death": 51_800,
    "Body": 44_400,
    "Water": 30_400,
    "Fire": 30_100,
    "Mind": 27_300,
    "Chaos": 25_700,
    "Air": 21_200,
    "Law": 20_800,
    "Cosmic": 19_500,
    "Life": 17_000,
    "Earth": 15_900,
}

EXPECTED_ROUTE_REGIMES = (
    (1, "Water"),
    (8, "Mind"),
    (11, "Water"),
    (18, "Mind"),
    (21, "Water"),
    (28, "Mind"),
    (31, "Water"),
    (35, "Body"),
    (38, "Mind"),
    (41, "Water"),
    (45, "Body"),
    (48, "Nature"),
    (51, "Water"),
    (55, "Body"),
    (58, "Nature"),
    (64, "Death"),
    (68, "Nature"),
    (72, "Soul"),
    (74, "Death"),
    (78, "Nature"),
    (80, "Blood"),
    (82, "Soul"),
    (84, "Death"),
    (90, "Blood"),
    (92, "Soul"),
    (94, "Death"),
)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    runecraft = RUNECRAFT.read_text(encoding="utf-8")
    analysis = ANALYSIS.read_text(encoding="utf-8")

    actual_xp = {}
    for entry in ET.parse(ALTARS).getroot().findall("entry"):
        definition = entry.find("ObjectRunecraftDef")
        if definition is None:
            fail("ObjectRunecraft.xml contains an entry without a definition")
        actual_xp[definition.findtext("runeName", default="")] = int(definition.findtext("exp", default="-1"))
    if actual_xp != CURRENT_INTERNAL_XP:
        fail(f"Rune XP data changed during analysis: {actual_xp}")

    required_runtime_findings = (
        "retVal += (level - requiredLevel) / 10;",
        "int repeatTimes = player.getCarriedItems().getInventory().countId",
        "player.incExp(Skill.RUNECRAFT.id(), def.getExp() * successCount, true);",
        "addLawRobeBonusRunes(player, def.getRuneId(), runeCount);",
        "addChaosAmuletBonusRunes(player, def.getRuneId(), runeCount);",
    )
    for finding in required_runtime_findings:
        if finding not in runecraft:
            fail(f"Runecraft behavior no longer matches the analysis: {finding}")

    previous_winners = None
    proposed_regimes = 0
    for level in range(1, 100):
        available = []
        for name, (unlock, per_rune_xp) in PROPOSED.items():
            if level < unlock:
                continue
            multiplier = 1 + ((level - unlock) // 10)
            available.append((name, per_rune_xp * multiplier))
        best_xp = max(xp for _, xp in available)
        winners = tuple(name for name, xp in available if xp == best_xp)
        if winners != previous_winners:
            proposed_regimes += 1
            previous_winners = winners

    if proposed_regimes != 22:
        fail(f"Expected 22 proposed optimal regimes, found {proposed_regimes}")

    route_regimes = []
    previous_winner = None
    for level in range(1, 100):
        route_rates = []
        for name, (unlock, per_rune_xp) in PROPOSED.items():
            if level < unlock:
                continue
            multiplier = 1 + ((level - unlock) // 10)
            stone_throughput = CURRENT_ROUTE_XP_HOUR[name] / CURRENT_DISPLAYED_XP[name]
            route_rates.append((stone_throughput * per_rune_xp * multiplier, name))
        winner = max(route_rates)[1]
        if winner != previous_winner:
            route_regimes.append((level, winner))
            previous_winner = winner

    if tuple(route_regimes) != EXPECTED_ROUTE_REGIMES:
        fail(f"Route-normalized method changes no longer match the supplied model: {route_regimes}")

    for snippet in (
        "Status: analysis only; no rune XP values or runtime behavior changed.",
        "22 optimal regimes, or 21 actual changes",
        "26 route-normalized optimal regimes",
        "25 actual method",
        "`134,600 XP/hour`",
        "`1,035,616 XP/hour`",
        "`18,900` displayed XP per 30 Stone before equipment",
        "Recommendation: accept the proposed table and base-output multiplication",
    ):
        if snippet not in analysis:
            fail(f"Rune XP decision document is missing: {snippet}")

    print("PASS: rune XP analysis matches unchanged runtime data and route-normalized level-1-to-99 model")


if __name__ == "__main__":
    main()
