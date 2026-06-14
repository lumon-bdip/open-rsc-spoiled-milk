#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SUMMONING = ROOT / "server/src/com/openrsc/server/content/Summoning.java"
WORLD = ROOT / "server/src/com/openrsc/server/model/world/World.java"
RANGE_UTILS = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/RangeUtils.java"
THROWING_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ThrowingEvent.java"
NPC_ID = ROOT / "server/src/com/openrsc/server/constants/NpcId.java"
NPC_DEFS = ROOT / "server/conf/server/defs/NpcDefsCustom.json"
CLIENT_DEFS = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> int:
    summoning = SUMMONING.read_text(encoding="utf-8")
    world = WORLD.read_text(encoding="utf-8")
    range_utils = RANGE_UTILS.read_text(encoding="utf-8")
    throwing_event = THROWING_EVENT.read_text(encoding="utf-8")
    npc_id = NPC_ID.read_text(encoding="utf-8")
    client_defs = CLIENT_DEFS.read_text(encoding="utf-8")
    guide = GUIDE.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")
    npc_defs = json.loads(NPC_DEFS.read_text(encoding="utf-8"))["npcs"]

    if "LOOT_GOBLIN(838)" not in npc_id:
        fail("NpcId should reserve id 838 for Loot Goblin")
    if 'private static final String KIND_LOOT_GOBLIN = "loot_goblin";' not in summoning:
        fail("Loot Goblin summon kind is missing")
    if "GIANT_SPIDER_PROFILE, IMP_PROFILE, LOOT_GOBLIN_PROFILE, BEAR_PROFILE" not in summoning:
        fail("Loot Goblin should be inserted between Mischief Imp and Ironhide Bear")

    required_summoning_fragments = (
        "public static boolean tryLootGoblinCollectGroundItem(final GroundItem groundItem)",
        "public static boolean tryLootGoblinCollectStackableItem(final Player owner, final int itemId, final int amount)",
        "definition == null || (!definition.isStackable() && !noted)",
        "if (!inventory.canHold(item))",
        'owner.message("The goblin tried to grab an item on the ground...");',
        'owner.message("... But there was no place to put it!");',
        "return inventory.add(item, true);",
    )
    for fragment in required_summoning_fragments:
        if fragment not in summoning:
            fail(f"Loot Goblin collection behavior is missing: {fragment}")

    if "Summoning.tryLootGoblinCollectGroundItem(i)" not in world or "i.remove();" not in world:
        fail("World.registerItem should offer owner-bound stackable drops to the Loot Goblin")
    if "Summoning.tryLootGoblinCollectStackableItem(player, arrowId, 1)" not in range_utils:
        fail("Arrow recovery should offer lost ammo to the Loot Goblin before floor stacking")
    if "Summoning.tryLootGoblinCollectStackableItem(player, throwingID, 1)" not in throwing_event:
        fail("Throwing recovery should offer lost ammo to the Loot Goblin before floor stacking")
    if re.search(r"tryLootGoblinCollectStackableItem\(player, throwingID, 1\)\) \{\s*return;", throwing_event, re.S):
        fail("Throwing recovery must not return before projectile visual and poison handling")

    loot_def = next((npc for npc in npc_defs if npc["id"] == 838), None)
    if loot_def is None:
        fail("Server NPC defs should include Loot Goblin id 838")
    if loot_def["name"] != "Loot Goblin" or loot_def["sprites1"] != 139:
        fail("Loot Goblin should reuse the goblin sprite")
    if loot_def["camera1"] != 110 or loot_def["camera2"] != 103:
        fail("Loot Goblin should use half-size goblin camera dimensions")
    if "setCustomNpcDefinition(838, new NPCDef(" not in client_defs:
        fail("Client NPC defs should include Loot Goblin id 838")

    for text in (
        "Loot Goblin - Support; bones, life, body, mind",
        "Loot Goblin - Does not engage in combat",
        "Scavenger - collects dropped stackable items",
    ):
        if text not in guide:
            fail(f"Summoning guide is missing Loot Goblin entry: {text}")
    if '"Loot Goblin", "Ironhide Bear"' not in client:
        fail("Client summon menu should include Loot Goblin before Ironhide Bear")
    if "{20, 37, 36, 35}" not in client:
        fail("Client summon menu should show bones, life, body, mind costs")

    print("PASS: loot goblin summon validated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
