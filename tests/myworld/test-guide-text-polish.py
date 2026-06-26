#!/usr/bin/env python3
"""Release-facing checks for in-game guide spelling and formatting."""

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
SKILL_GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
GUIDE_DIALOGUE_FILES = (
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/BankAssistant.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/CommunityInstructor.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/ControlsGuide.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/FinancialAdvisor.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/Guide.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/npcs/tutorial/WildernessGuide.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/members/digsite/DigsiteGuide.java",
    ROOT / "server/plugins/com/openrsc/server/plugins/retro/npcs/Guide.java",
)


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def forbid(text: str, snippet: str, label: str) -> None:
    if snippet in text:
        fail(f"{label} still contains retired wording: {snippet}")


def main() -> None:
    guide = SKILL_GUIDE.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")
    guide_dialogue = "\n".join(path.read_text(encoding="utf-8") for path in GUIDE_DIALOGUE_FILES)

    for snippet in (
        "Lily's Pumpkin Pie - Heals 12 (6 per slice)",
        "White Pumpkin Pie - Heals 16 (8 per slice)",
        "Greenman's Ale - Heals 1",
        "Chocolaty Milk - Heals 4",
        "Cooked Meat - Heals 3",
        "Oomlie Meat Parcel - Heals 8",
        "Leather armor mimics the defenses of its creature",
    ):
        require(guide, snippet, "Skill guide polish")

    for snippet in (
        "the Bartender",
        "a Guard",
        "on the top floor of the Sorcerer's Tower south of Seer's Village",
        '"12 Quest Points"',
        '"3 Quest Points", "Free passage through the Al-Kharid tollgate"',
        "Ability to defeat level 83 monsters",
        "8 Law runes",
        "1 Oyster pearl",
        "15 Death runes",
        "30 Fire runes",
        "A chocolate cake and stew",
    ):
        require(client, snippet, "Quest guide polish")

    for snippet in (
        "Welcome to the world of RuneScape",
        "Hello welcome to the bank of RuneScape",
        "several of RuneScape's skills are good money making skills",
        "can be accessed by the menus",
        "they will be much more persistent in chasing after you",
        "First, you need a panning tray",
        "He will calculate its value for you",
        "Some are creatures and people that live in RuneScape",
        "You have been a great help, thank you",
    ):
        require(guide_dialogue, snippet, "NPC guide dialogue polish")

    for snippet in (
        "Bertender",
        "Gaurd",
        "top floor fo",
        '"12 quest points"',
        '"3 Quest points"',
        "Ability to defeat a level 83 monsters",
        "it's creature",
        "White pumpkin pie - Heals",
        "Greenmans ale - Heals",
        "cookedmeat - Heals",
        "world of runescape",
        "bank of runescape",
        "ways to make money in runescape",
        "runescapes skills",
        "accesed",
        "Thankyou",
        "thankyou",
        "ingame",
        "through the  door",
        "persistant",
        "oppurtunites",
        "First You",
        "who are you ?",
        "it's value",
        "green You",
    ):
        forbid(guide + client + guide_dialogue, snippet, "Guide text polish")

    lower_heal_rows = re.findall(r'"([a-z][^"]* - Heals [^"]*)"', guide)
    if lower_heal_rows:
        fail("Healing guide rows should start with title-cased names: " + ", ".join(lower_heal_rows))

    print("PASS: in-game guide text polish validated")


if __name__ == "__main__":
    main()
