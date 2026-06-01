#!/usr/bin/env python3

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"missing {label}: {needle}")


def main() -> None:
    prince_ali = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/free/PrinceAliRescue.java").read_text()
    require(prince_ali, '"Do you mind if I just kill your Warriors?",\n\t\t\t\t\tMyWorldQuestShortcuts.ALREADY_DONE_OPTION', "Prince Ali Hassan start shortcut")
    require(prince_ali, '"I\'ll keep working on the rescue",\n\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Prince Ali Hassan in-progress shortcut")
    require(prince_ali, '"And the final thing you need?",\n\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Prince Ali in-progress Osman shortcut")
    require(prince_ali, "wutwut == 2", "Prince Ali key handoff shortcut")
    require(prince_ali, '"Can you tell me what I still need to get?",\n\t\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Prince Ali item-stage shortcut")
    require(prince_ali, '"I\'ll pick up my payment from the chancellor",\n\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Prince Ali payment-stage shortcut")

    shield = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/varrock/Reldo.java").read_text()
    require(shield, '"Tell me about the Shield of Arrav",\n\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Shield of Arrav direct in-progress shortcut")
    require(shield, "completeShieldOfArravShortcut(player, npc);", "Shield of Arrav direct shortcut helper")

    ghost = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/free/TheRestlessGhost.java").read_text()
    require(ghost, '"Sorry, I can\'t find it at the moment",\n\t\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Restless Ghost skull-search shortcut")
    require(ghost, '"I have found it",\n\t\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Restless Ghost skull-found shortcut")
    require(ghost, '"Wow, this amulet works",\n\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Restless Ghost amulet branch shortcut")

    fishing = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/members/FishingContest.java").read_text()
    require(fishing, '"I think I might still be able to find a bigger fish",\n\t\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Fishing Contest paid branch shortcut")
    require(fishing, '"I think I\'ll keep them to myself",\n\t\t\t\t\t\t\tMyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION', "Fishing Contest trophy branch shortcut")

    bartender = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/portsarim/Bartender.java").read_text()
    require(bartender, "import com.openrsc.server.plugins.custom.quests.MyWorldQuestShortcuts;", "Goblin Diplomacy bartender shortcut import")
    require(bartender, "options.add(MyWorldQuestShortcuts.ALREADY_DONE_OPTION);", "Goblin Diplomacy bartender start shortcut")
    require(bartender, "options.add(MyWorldQuestShortcuts.IN_PROGRESS_ALREADY_DONE_OPTION);", "Goblin Diplomacy bartender in-progress shortcut")
    require(bartender, "String selectedOption = options.get(option);", "Goblin Diplomacy bartender text-based option dispatch")
    require(bartender, "MyWorldQuestShortcuts.completeGoblinDiplomacy(player, n);", "Goblin Diplomacy bartender shortcut completion")
    require(bartender, '"I\'m doing Alfred Grimhand\'s barcrawl".equals(selectedOption)', "Port Sarim bartender barcrawl option dispatch")

    barcrawl = (ROOT / "server/plugins/com/openrsc/server/plugins/authentic/minigames/barcrawl/AlfredGrimhandBarCrawl.java").read_text()
    require(barcrawl, "import com.openrsc.server.plugins.custom.quests.MyWorldQuestShortcuts;", "Barcrawl shortcut import")
    require(barcrawl, "MyWorldQuestShortcuts.ALREADY_DONE_OPTION", "Barcrawl shortcut option")
    require(barcrawl, "private void completeBarcrawlShortcut", "Barcrawl shortcut completion helper")
    require(barcrawl, 'player.getCache().store("barcrawl_completed", true);', "Barcrawl completion cache")
    require(barcrawl, "player.sendMiniGameComplete(this.getMiniGameId(), Optional.empty());", "Barcrawl minigame completion event")


if __name__ == "__main__":
    main()
