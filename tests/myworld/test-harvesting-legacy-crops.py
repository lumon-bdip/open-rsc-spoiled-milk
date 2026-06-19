#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OBJECT_DEFS = ROOT / "server/conf/server/defs/GameObjectDef.xml"
HARVESTING_DEFS = ROOT / "server/conf/server/defs/extras/ObjectHarvesting.xml"
SKILL_GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
WORLD_POPULATOR = ROOT / "server/src/com/openrsc/server/database/WorldPopulator.java"

LEGACY_HARVESTABLES = (
    ("Wheat", 72, 29, 72),
    ("Banana tree", 183, 249, 88),
    ("Potato", 191, 348, 72),
    ("flax", 313, 675, 40),
    ("Pineapple tree", 430, 861, 176),
)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        raise AssertionError(message)


def main() -> None:
    object_defs = OBJECT_DEFS.read_text(encoding="utf-8")
    harvesting_defs = HARVESTING_DEFS.read_text(encoding="utf-8")
    skill_guide = SKILL_GUIDE.read_text(encoding="utf-8")
    world_populator = WORLD_POPULATOR.read_text(encoding="utf-8")

    for name, object_id, produce_id, experience in LEGACY_HARVESTABLES:
        start = object_defs.index(f"<name>{name}</name>")
        definition = object_defs[start:object_defs.index("</GameObjectDef>", start)]
        require(definition, "<command2>Harvest</command2>", f"{name} must expose the Harvest action")
        require(
            harvesting_defs,
            f"<int>{object_id}</int>\n\t\t<ObjectHarvestingDef>",
            f"Object {object_id} is missing its Harvesting definition",
        )
        entry_start = harvesting_defs.index(f"<int>{object_id}</int>")
        entry = harvesting_defs[entry_start:harvesting_defs.index("</ObjectHarvestingDef>", entry_start)]
        require(entry, f"<prodId>{produce_id}</prodId>", f"Object {object_id} has the wrong produce")
        require(entry, f"<exp>{experience}</exp>", f"Object {object_id} has the wrong Harvesting XP")

    if "<command2>pick</command2>" in object_defs.lower():
        raise AssertionError("A generic scenery Pick action still bypasses Harvesting")
    require(skill_guide, 'new SkillMenuItem(29, "1", "Grain - T1 shears")', "Harvesting guide is missing grain")
    require(skill_guide, 'new SkillMenuItem(675, "15", "Flax - T3 shears")', "Harvesting guide is missing flax")
    require(skill_guide, 'new SkillMenuItem(422, "1", "Event Pumpkin - T1 shears, bonus XP")', "Harvesting guide is missing event pumpkins")
    require(harvesting_defs, "<int>1327</int>", "Regular event pumpkins are missing a Harvesting definition")
    require(harvesting_defs, "<prodId>422</prodId>\n\t\t\t<exp>360</exp>", "Event pumpkins must grant their bonus Harvesting XP")

    for item in ("CABBAGE", "CADAVABERRIES", "GRAPES", "UNIDENTIFIED_GUAM_LEAF", "REDBERRIES",
                 "ONION", "TOMATO", "PUMPKIN", "SNAPE_GRASS", "WHITE_BERRIES", "SEAWEED",
                 "DWELLBERRIES", "JANGERBERRIES"):
        require(world_populator, f"itemId == ItemId.{item}.id()", f"Ground {item} must route through Harvesting")
    for quest_item in ("DOOGLE_LEAVES", "NIGHTSHADE"):
        if f"itemId == ItemId.{quest_item}.id()" in world_populator:
            raise AssertionError(f"Quest item {quest_item} must remain a ground pickup")

    print("legacy crop harvesting checks passed")


if __name__ == "__main__":
    main()
