#!/usr/bin/env python3
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
NPC_DROPS = ROOT / "server/src/com/openrsc/server/constants/NpcDrops.java"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, needle: str, message: str) -> None:
    if needle not in text:
        fail(message)


def forbid(text: str, needle: str, message: str) -> None:
    if needle in text:
        fail(message)


def between(text: str, start: str, end: str, message: str) -> str:
    if start not in text:
        fail(f"Missing start marker for {message}: {start}")
    tail = text.split(start, 1)[1]
    if end not in tail:
        fail(f"Missing end marker for {message}: {end}")
    return tail.split(end, 1)[0]


def main() -> None:
    drops = NPC_DROPS.read_text(encoding="utf-8")

    for item in (
        "DRAGON_SWORD",
        "DRAGON_AXE",
        "DRAGON_2_HANDED_SWORD",
        "DRAGON_DAGGER",
        "POISONED_DRAGON_DAGGER",
        "DRAGON_BATTLE_AXE",
        "DRAGON_CROSSBOW",
        "DRAGON_LONGBOW",
        "DRAGON_ARROWS",
        "POISON_DRAGON_ARROWS",
        "DRAGON_BOLTS",
        "POISON_DRAGON_BOLTS",
    ):
        forbid(
            drops,
            f"addItemDrop(ItemId.{item}.id()",
            f"{item} should not be an NPC item drop",
        )

    shared_ultra = between(
        drops,
        "private void createUltraRareDropTable() {",
        "\n\tprivate DropTable createBlackDemonUltraRareDropTable()",
        "shared ultra rare drop table",
    )
    forbid(shared_ultra, "DRAGON_MEDIUM_HELMET", "Shared ultra rare table should not drop dragon medium helm")
    forbid(shared_ultra, "LARGE_DRAGON_HELMET", "Shared ultra rare table should not drop dragon full helm")

    forbid(drops, "dragonDropTable", "Retired OpenPK dragon drop table should stay removed")

    black_demon_ultra = between(
        drops,
        "private DropTable createBlackDemonUltraRareDropTable() {",
        "\n\tprivate void createBoneDrops()",
        "black demon ultra rare drop table",
    )
    forbid(black_demon_ultra, "DRAGON_MEDIUM_HELMET", "Black Demon ultra rare table should not directly drop dragon medium helm")
    forbid(black_demon_ultra, "LARGE_DRAGON_HELMET", "Black Demon ultra rare table should not directly drop dragon full helm")

    black_demon = between(
        drops,
        'currentNpcDrops = new DropTable("Black Demon (290)")',
        "this.npcDrops.put(NpcId.BLACK_DEMON.id(), currentNpcDrops);",
        "black demon drop table",
    )
    require(
        black_demon,
        "currentNpcDrops.addTableDrop(createBlackDemonUltraRareDropTable(), 1);",
        "Black Demon should use its dragon-helm-specific ultra rare table",
    )

    kbd = between(
        drops,
        'currentNpcDrops = new DropTable("King Black Dragon (477)")',
        "this.npcDrops.put(NpcId.KING_BLACK_DRAGON.id(), currentNpcDrops);",
        "king black dragon drop table",
    )
    forbid(kbd, "DRAGON_MEDIUM_HELMET", "KBD should not directly drop dragon medium helm")
    forbid(kbd, "LARGE_DRAGON_HELMET", "KBD should not directly drop dragon full helm")

    kbd_custom = between(
        drops,
        "private void initializeCustomRareDropTables()",
        "\n\tprivate void createCustomQuestDrops()",
        "custom KBD rare drop table",
    )
    forbid(kbd_custom, "DRAGON_2_HANDED_SWORD", "KBD custom rare table should not drop dragon weapons")

    hidden_unique = between(
        drops,
        "private void createHiddenUniqueDrops()",
        "\n\tprivate void addHiddenUniqueDrop(final int npcId, final int itemId, final int amount, final HiddenUniqueRarity rarity)",
        "hidden unique drop registry",
    )
    require(
        hidden_unique,
        "addHiddenUniqueDrop(NpcId.BLACK_DEMON.id(), ItemId.DRAGON_MEDIUM_HELMET.id(), 1, HiddenUniqueRarity.VERY_RARE_UNIQUE);",
        "Black Demon should drop dragon medium helm through the hidden unique layer",
    )
    require(
        hidden_unique,
        "addHiddenUniqueDrop(NpcId.BLACK_DEMON.id(), ItemId.LARGE_DRAGON_HELMET.id(), 1, HiddenUniqueRarity.VERY_RARE_UNIQUE);",
        "Black Demon should drop dragon full helm through the hidden unique layer",
    )
    require(
        hidden_unique,
        "addHiddenUniqueDrop(NpcId.BLACK_DRAGON.id(), ItemId.DRAGON_SQUARE_SHIELD.id(), 1, HiddenUniqueRarity.VERY_RARE_UNIQUE);",
        "Black Dragon should drop dragon square shield through the hidden unique layer",
    )
    require(
        hidden_unique,
        "addHiddenUniqueDrop(NpcId.KING_BLACK_DRAGON.id(), ItemId.DRAGON_SQUARE_SHIELD.id(), 1, HiddenUniqueRarity.VERY_RARE_UNIQUE);",
        "King Black Dragon should drop dragon square shield through the hidden unique layer",
    )

    print("PASS: dragon NPC drop sources validated")


if __name__ == "__main__":
    main()
