#!/usr/bin/env python3
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def forbid(text: str, snippet: str, label: str) -> None:
    if snippet in text:
        fail(f"{label} should not contain retired snippet: {snippet}")


def load_base_items() -> dict[int, dict]:
    with (ROOT / "server/conf/server/defs/ItemDefs.json").open("r", encoding="utf-8") as handle:
        return {entry["id"]: entry for entry in json.load(handle)["item"]}


def main() -> None:
    fletching = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/fletching/Fletching.java").read_text(encoding="utf-8")
    skill_ui = (ROOT / "Client_Base/src/com/openrsc/interfaces/misc/DoSkillInterface.java").read_text(encoding="utf-8")
    skill_guide = (ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java").read_text(encoding="utf-8")
    shop = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/grandtree/Gulluck.java").read_text(encoding="utf-8")
    present = (ROOT / "server/plugins/com/openrsc/server/plugins/custom/misc/Present.java").read_text(encoding="utf-8")
    entrana = (ROOT / "server/plugins/com/openrsc/server/plugins/shared/EntranaRestrictions.java").read_text(encoding="utf-8")
    client_items = (ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java").read_text(encoding="utf-8")

    for retired in (
        "batchPearlCutting",
        "batchBolts",
        "OYSTER_PEARL_BOLT_TIPS",
        "OYSTER_PEARL_BOLTS",
        "QUEST_OYSTER_PEARLS",
        "OYSTER_PEARLS",
    ):
        forbid(fletching, retired, "Fletching")

    forbid(skill_ui, "Fletch pearl bolts", "DoSkillInterface")
    forbid(skill_guide, "Can be opened at level 34", "Crafting guide")
    forbid(shop, "OYSTER_PEARL_BOLTS", "Gulluck shop")
    forbid(present, "OYSTER_PEARL_BOLT_TIPS", "Present drops")
    forbid(entrana, "OYSTER_PEARL_BOLTS", "Entrana restrictions")
    forbid(entrana, "OYSTER_PEARL_BOLT_TIPS", "Entrana restrictions")
    require(present, "ItemId.OYSTER_PEARLS.id(), 1, 1", "Present oyster pearl reward")

    for path in (
        "server/src/com/openrsc/server/model/container/Equipment.java",
        "server/src/com/openrsc/server/util/rsc/Formulae.java",
        "server/src/com/openrsc/server/event/rsc/impl/combat/OSRSCombatFormula.java",
        "server/src/com/openrsc/server/event/rsc/impl/combat/CombatFormula.java",
        "server/src/com/openrsc/server/event/rsc/impl/projectile/RangeUtils.java",
        "server/src/com/openrsc/server/external/EntityHandler.java",
    ):
        forbid((ROOT / path).read_text(encoding="utf-8"), "OYSTER_PEARL_BOLTS", path)

    items = load_base_items()
    for item_id in (779, 792):
        entry = items[item_id]
        if entry["basePrice"] != 25000:
            fail(f"oyster pearls id {item_id} should be worth 25000")
        if "chisel" in entry["description"].lower():
            fail(f"oyster pearls id {item_id} should not mention chiseling")
    if items[793]["basePrice"] != 10000:
        fail("rare oyster should be worth 10000")
    if "valuable" not in items[793]["description"].lower():
        fail("rare oyster description should explain that it is valuable")

    for snippet in (
        'new ItemDef("oyster pearls", "Valuable pearls from a rare oyster", "", 25000',
        'new ItemDef("oyster", "It\'s a rare and valuable oyster", "open", 10000',
        'new ItemDef("Oyster pearl bolts", "Retired oyster pearl bolts", "", 1',
        'new ItemDef("Oyster pearl bolt tips", "Retired oyster pearl bolt tips", "", 1',
    ):
        require(client_items, snippet, "Client item definitions")
    forbid(client_items, "I could work wonders with these and a chisel", "Client item definitions")

    print("PASS: oyster pearls are valuable and pearl bolts are retired")


if __name__ == "__main__":
    main()
