#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
THROWING_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ThrowingEvent.java"
RANGE_UTILS = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/RangeUtils.java"
CRAFTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/crafting/Crafting.java"
SMELTING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/smithing/Smelting.java"
ITEM_ID = ROOT / "server/src/com/openrsc/server/constants/ItemId.java"
FORMULAE = ROOT / "server/src/com/openrsc/server/util/rsc/Formulae.java"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def main() -> None:
    throwing = THROWING_EVENT.read_text(encoding="utf-8")
    range_utils = RANGE_UTILS.read_text(encoding="utf-8")
    crafting = CRAFTING.read_text(encoding="utf-8")
    smelting = SMELTING.read_text(encoding="utf-8")
    item_id = ITEM_ID.read_text(encoding="utf-8")
    formulae = FORMULAE.read_text(encoding="utf-8")
    client = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")

    for snippet in (
        "SHURIKEN_THROW_COUNT = 3",
        "selectThrowingTargets(player, throwingID, attackRadius)",
        "throwsToConsume = RangeUtils.SHURIKENS.contains(throwingID) ? throwingTargets.size() : 1",
        "while (throwingTargets.size() > throwsToConsume)",
        "targets.add(target)",
        "!target.isNpc()",
        "isAggroedToPlayer((Npc) target, player) ? 1 : 0",
        "boolean preferAggroed = aggroedCount >= SHURIKEN_THROW_COUNT",
        "addRandomShurikenTargets(targets, preferred)",
        "addRandomShurikenTargets(targets, fallback)",
        "npc.getOpponent() == player || npc.getPreferredThreatTarget() == player",
        "RangeUtils.THROWING_KNIVES.contains(throwingID) || RangeUtils.SHURIKENS.contains(throwingID)",
    ):
        require(throwing, snippet, "ThrowingEvent shuriken combat contract")

    for snippet in (
        "ItemId.TIN_SHURIKEN.id()",
        "ItemId.RUNE_SHURIKEN.id()",
        "ItemId.POISONED_TIN_SHURIKEN.id()",
        "ItemId.POISONED_RUNE_SHURIKEN.id()",
    ):
        require(range_utils, snippet, "RangeUtils.SHURIKENS")
        require(formulae, snippet, "Formulae.throwingIDs")

    for snippet in (
        "POISONED_TIN_SHURIKEN",
        "POISONED_COPPER_SHURIKEN",
        "POISONED_BRONZE_SHURIKEN",
        "POISONED_IRON_SHURIKEN",
        "POISONED_STEEL_SHURIKEN",
        "POISONED_MITHRIL_SHURIKEN",
        "POISONED_TITAN_STEEL_SHURIKEN",
        "POISONED_ADAMANTITE_SHURIKEN",
        "POISONED_ORICHALCUM_SHURIKEN",
        "POISONED_RUNE_SHURIKEN",
    ):
        require(item_id, snippet, "ItemId poison-compatible shuriken names")

    require(crafting, 'addRangedMouldRecipe(recipes, player, barId, getShurikenId(barId), ItemId.SHURIKEN_MOULD.id(), "Shuriken", 9, 4)', "Crafting shuriken recipe")
    require(crafting, 'new RangedMouldRecipe("Shuriken", itemId, ItemId.SHURIKEN_MOULD.id(), 9', "Crafting shuriken yield")
    require(smelting, "FURNACE_CATEGORY_SHURIKEN", "Smelting shuriken category")
    require(smelting, "ItemId.SHURIKEN_MOULD.id()", "Smelting shuriken mould gate")
    require(client, 'new ItemDef("Shuriken mould", "Use with bars to cast shuriken"', "Client shuriken mould definition")
    require(client, '"external-png:shuriken-mould"', "Client shuriken mould sprite")
    require(client, '"external-png:shuriken-basic"', "Client shuriken sprite")
    require(client, '"external-png:shuriken-basic-poison"', "Client poisoned shuriken sprite")


if __name__ == "__main__":
    main()
