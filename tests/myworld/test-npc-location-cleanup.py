#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORLD_POPULATOR = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "database" / "WorldPopulator.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    source = WORLD_POPULATOR.read_text(encoding="utf-8")
    required = (
        "applyMyWorldNpcLocationCleanup();",
        "if (!getWorld().getServer().getConfig().WANT_MYWORLD)",
        "private boolean keepBankerNpcLoc(NPCLoc loc, ArrayList<int[]> bankerClusters)",
        "if (cluster[3] >= 2)",
        "return loc.startX >= 190 && loc.startX <= 245",
        "&& loc.startY >= 710 && loc.startY <= 760;",
        "npcId == NpcId.BANKER.id()",
        "npcId == NpcId.FAIRY_BANKER.id()",
        "npcId == NpcId.BANKER_ALKHARID.id()",
        "npcId == NpcId.GNOME_BANKER.id()",
        "npcId == NpcId.JUNGLE_BANKER.id()",
    )
    for snippet in required:
        if snippet not in source:
            fail(f"WorldPopulator missing NPC cleanup snippet: {snippet}")
    if "AUCTIONEER" in source[source.find("private boolean isBankerNpc"):source.find("private boolean isTutorialIslandNpcLoc")]:
        fail("Auctioneers must not be counted as bankers for bank population caps")
    print("PASS: MyWorld NPC location cleanup is scoped and covered")


if __name__ == "__main__":
    main()
