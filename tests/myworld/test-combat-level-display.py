#!/usr/bin/env python3

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
FORMULAE = ROOT / "server/src/com/openrsc/server/util/rsc/Formulae.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    raise AssertionError(message)


def myworld_combat(main: int, hits: int, prayer: int, summoning: int) -> int:
    weighted = 1 + ((main * 16) + (hits * 2) + prayer + summoning) // 20
    return max(3, min(100, weighted))


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    formulae = FORMULAE.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")

    if myworld_combat(63, 42, 28, 35) != 58:
        fail("Example MyWorld combat level should be 58")
    if myworld_combat(99, 99, 99, 99) != 100:
        fail("Maxed considered MyWorld combat stats should cap at 100")
    if myworld_combat(63, 42, 28, 35) != myworld_combat(63, 42, 28, 35):
        fail("Lower unused combat styles should not affect MyWorld combat level")

    for snippet, message in (
        ("return getMyWorldCombatLevel(stats);", "MyWorld players should use the custom combat-level formula"),
        ("int mainCombat = Math.max(stats[Skill.MELEE.id()], Math.max(stats[Skill.RANGED.id()], stats[Skill.MAGIC.id()]));",
         "MyWorld formula should use only the highest main combat style"),
        ("+ (stats[Skill.HITS.id()] * 2)", "Hits should be the largest support stat"),
        ("+ stats[Skill.PRAYER.id()]", "Prayer should be a secondary support stat"),
        ("+ stats[Skill.SUMMONING.id()]) / 20", "Summoning should be a secondary support stat"),
        ("Math.max(3, Math.min(100, weightedLevel))", "MyWorld combat level should be clamped to 3-100"),
    ):
        require(formulae, snippet, message)

    for snippet, message in (
        ("private int getLocalPlayerMenuCombatLevel()", "Client should centralize local combat-level menu reads"),
        ("return this.localPlayer == null ? 0 : this.localPlayer.level;", "NPC menu should use server-sent player combat level"),
        ("private int getNpcMenuCombatLevel(int npcId)", "Client should centralize NPC menu combat-level calculation"),
        ("int npcLevel = getNpcMenuCombatLevel(var13);", "NPC menu should use the NPC combat-level helper"),
        ("int playerLevel = getLocalPlayerMenuCombatLevel();", "NPC menu should use the local-player combat-level helper"),
    ):
        require(client, snippet, message)

    if "this.playerStatBase[3] + this.playerStatBase[2]\n\t\t\t\t\t\t\t\t\t\t+ this.playerStatBase[1] + this.playerStatBase[0] + 27" in client:
        fail("NPC menu still uses old player stat-slot combat math")

    print("PASS: MyWorld combat level formula and NPC danger display validated")


if __name__ == "__main__":
    main()
