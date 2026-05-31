#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GUILDMASTER = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/varrock/Guildmaster.java"
DRAGON_SLAYER = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/free/DragonSlayer.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        fail(f"missing {label}: {needle}")


def forbid(text: str, needle: str, label: str) -> None:
    if needle in text:
        fail(f"retired {label} still present: {needle}")


def main() -> None:
    guildmaster = GUILDMASTER.read_text(encoding="utf-8")
    dragon_slayer = DRAGON_SLAYER.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")

    require(client, '"32 Quest Points", "33 Magic"', "Dragon Slayer guide requirements")
    require(guildmaster, "DRAGON_SLAYER_QUEST_POINTS = 32", "server quest point requirement")
    require(guildmaster, "DRAGON_SLAYER_MAGIC_LEVEL = 33", "server magic requirement")
    require(guildmaster, "player.getQuestStage(Quests.DRAGON_SLAYER) == 0", "start option stage gate")
    require(guildmaster, "meetsDragonSlayerRequirements(player)", "start requirement check")
    require(guildmaster, "player.getQuestPoints() >= DRAGON_SLAYER_QUEST_POINTS", "quest point validation")
    require(guildmaster, "player.getSkills().getMaxStat(Skill.MAGIC.id()) >= DRAGON_SLAYER_MAGIC_LEVEL", "magic validation")
    forbid(guildmaster, "player.getConfig().BASED_MAP_DATA >= 23 && player.getClientVersion() >= 73", "client/map start gate")

    stage_zero = dragon_slayer.split("case 0:", 1)[1].split("break;", 1)[0]
    stage_one = dragon_slayer.split("case 1:", 1)[1].split("break;", 1)[0]
    stage_two = dragon_slayer.split("case 2:", 1)[1].split("break;", 1)[0]
    for label, block in (("stage 0", stage_zero), ("stage 1", stage_one), ("stage 2", stage_two)):
        require(block, "MyWorldQuestShortcuts.ALREADY_DONE_OPTION", f"Oziach shortcut option at {label}")
        require(block, "MyWorldQuestShortcuts.completeDragonSlayer(player, n);", f"Oziach shortcut completion at {label}")

    print("PASS: Dragon Slayer start and shortcut flow validated")


if __name__ == "__main__":
    main()
