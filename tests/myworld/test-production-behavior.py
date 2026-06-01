#!/usr/bin/env python3
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
PRODUCTION_SESSION = (
    ROOT / "server" / "src" / "com" / "openrsc" / "server" / "content" / "production" / "ProductionSession.java"
)
SMITHING = (
    ROOT
    / "server"
    / "plugins"
    / "com"
    / "openrsc"
    / "server"
    / "plugins"
    / "authentic"
    / "skills"
    / "smithing"
    / "Smithing.java"
)
SMELTING = (
    ROOT
    / "server"
    / "plugins"
    / "com"
    / "openrsc"
    / "server"
    / "plugins"
    / "authentic"
    / "skills"
    / "smithing"
    / "Smelting.java"
)
CRAFTING = (
    ROOT
    / "server"
    / "plugins"
    / "com"
    / "openrsc"
    / "server"
    / "plugins"
    / "authentic"
    / "skills"
    / "crafting"
    / "Crafting.java"
)
INTERFACE_OPTION_HANDLER = (
    ROOT
    / "server"
    / "src"
    / "com"
    / "openrsc"
    / "server"
    / "net"
    / "rsc"
    / "handlers"
    / "InterfaceOptionHandler.java"
)
FLETCHING = (
    ROOT
    / "server"
    / "plugins"
    / "com"
    / "openrsc"
    / "server"
    / "plugins"
    / "authentic"
    / "skills"
    / "fletching"
    / "Fletching.java"
)
DO_SKILL_INTERFACE = (
    ROOT
    / "Client_Base"
    / "src"
    / "com"
    / "openrsc"
    / "interfaces"
    / "misc"
    / "DoSkillInterface.java"
)


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def main() -> None:
    session_text = PRODUCTION_SESSION.read_text(encoding="utf-8")
    smithing_text = SMITHING.read_text(encoding="utf-8")
    smelting_text = SMELTING.read_text(encoding="utf-8")
    crafting_text = CRAFTING.read_text(encoding="utf-8")
    fletching_text = FLETCHING.read_text(encoding="utf-8")
    do_skill_interface_text = DO_SKILL_INTERFACE.read_text(encoding="utf-8")
    interface_handler_text = INTERFACE_OPTION_HANDLER.read_text(encoding="utf-8")

    require(
        session_text,
        "if (recipe.isLevelMet() && recipe.isMaterialsMet()) {",
        "ProductionSession should prefer fully craftable recipes as the default selection",
    )
    require(
        session_text,
        "if (recipe.isLevelMet()) {",
        "ProductionSession should fall back to level-met recipes when materials are missing",
    )
    require(
        session_text,
        "return recipes.isEmpty() ? -1 : recipes.get(0).getItemId();",
        "ProductionSession should fall back to the first recipe only as a final default",
    )
    require(
        smithing_text,
        'player.message("You are not skilled enough for that yet");',
        "Smithing should refuse to open the production window when nothing is craftable",
    )
    require(
        smithing_text,
        "level >= def.getRequiredLevel(), materialCount >= def.getRequiredBars()",
        "Smithing production recipes should reflect live level and bar requirements in disabled states",
    )
    require(
        smithing_text,
        "new int[]{barId}, new int[]{-1}, new int[]{def.getRequiredBars()}",
        "Smithing production recipes should expose bar cost details for icon-based material display",
    )
    require(
        smithing_text,
        "stopbatch();\n\t\t\t\tbreak;",
        "Smithing production should close the batch window when resources run out mid-batch",
    )
    require(
        do_skill_interface_text,
        "boolean showQuantityControls = !isSmithingMaterialPicker();",
        "Smithing material picker should hide unused quantity controls",
    )
    require(
        crafting_text,
        "materialCount >= piece.materialCost && threadUses >= 1",
        "Leather crafting production recipes should reflect both material and thread requirements",
    )
    require(
        crafting_text,
        "player.getConfig().WANT_FATIGUE",
        "Crafting interface production should use player config outside plugin callback context",
    )
    require(
        smelting_text,
        "player.getConfig().WANT_FATIGUE",
        "Smelting interface production should use player config outside plugin callback context",
    )
    require(
        smithing_text,
        "player.getConfig().WANT_FATIGUE",
        "Smithing interface production should use player config outside plugin callback context",
    )
    for label, text in (
        ("crafting", crafting_text),
        ("smelting", smelting_text),
        ("smithing", smithing_text),
    ):
        if "config()" in text:
            fail(f"{label} production-heavy plugin should not depend on plugin-thread config()")
    require(
        interface_handler_text,
        "catch (RuntimeException e)",
        "Production starts should fail gracefully instead of bubbling packet handler exceptions",
    )
    require(
        crafting_text,
        "barCount >= 1 && hasMould && hasGem",
        "Gold jewelry production recipes should reflect bar, mould, and gem requirements",
    )
    require(
        crafting_text,
        "barCount >= 1 && hasMould",
        "Silver jewelry production recipes should reflect bar and mould requirements",
    )
    require(
        fletching_text,
        "int outputAmount = recipe.resultId == ItemId.ARROW_SHAFTS.id() ? getNumberOfShafts(player, log.getCatalogId()) : 1;",
        "Fletching production should preserve dynamic arrow-shaft output amounts",
    )
    require(
        fletching_text,
        "level >= recipe.requiredLevel, materialCount >= 1",
        "Fletching production recipes should reflect current level and log availability",
    )
    for label, text in (
        ("smithing", smithing_text),
        ("crafting", crafting_text),
        ("fletching", fletching_text),
    ):
        require(
            text,
            "if (!session.hasAnyCraftableRecipe()) {",
            f"{label.capitalize()} should gate the production window on at least one level-met recipe",
        )
        require(
            text,
            'player.setAttribute("production_session", session);',
            f"{label.capitalize()} should keep the active production session on the player",
        )
        require(
            text,
            "ActionSender.showProductionInterface(player, session);",
            f"{label.capitalize()} should route through the shared production window",
        )

    print("PASS: Production behavior invariants validated")


if __name__ == "__main__":
    main()
