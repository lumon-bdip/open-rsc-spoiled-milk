#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
KATRINE = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "varrock" / "Katrine.java"
NPC_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "NpcDefs.json"
NPC_PATCH_18 = ROOT / "server" / "conf" / "server" / "defs" / "NpcDefsPatch18.json"
CLIENT_NPCS = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    katrine = KATRINE.read_text(encoding="utf-8")
    npc_defs = NPC_DEFS.read_text(encoding="utf-8")
    npc_patch = NPC_PATCH_18.read_text(encoding="utf-8")
    client_npcs = CLIENT_NPCS.read_text(encoding="utf-8")

    for item in (
        "TIN_THROWING_DART",
        "COPPER_THROWING_DART",
        "BRONZE_THROWING_DART",
        "IRON_THROWING_DART",
        "STEEL_THROWING_DART",
        "MITHRIL_THROWING_DART",
        "TIN_THROWING_KNIFE",
        "COPPER_THROWING_KNIFE",
        "BRONZE_THROWING_KNIFE",
        "IRON_THROWING_KNIFE",
        "STEEL_THROWING_KNIFE",
        "MITHRIL_THROWING_KNIFE",
        "TIN_SHURIKEN",
        "COPPER_SHURIKEN",
        "BRONZE_SHURIKEN",
        "IRON_SHURIKEN",
        "STEEL_SHURIKEN",
        "MITHRIL_SHURIKEN",
    ):
        require(katrine, f"ItemId.{item}.id()", f"Katrine shop stocks {item}")

    require(katrine, "return false;", "Katrine shop preserves quest Talk-to handling")
    require(katrine, "n.getID() == NpcId.KATRINE.id()", "Katrine shop binds to Katrine")
    require(katrine, "player.getConfig().RIGHT_CLICK_TRADE", "Katrine shop respects right-click trade config")
    require(katrine, 'command.equalsIgnoreCase("Trade") || command.equalsIgnoreCase("Shop")', "Katrine shop accepts Trade and Shop")

    require(npc_defs, '"name": "Katrine",\n        "description": "She doesn\'t look to friendly",\n        "command": "Trade",\n        "command2": ""', "base Katrine Trade command")
    require(npc_patch, '"name": "Katrine",\n\t\t"description": "She doesn\'t look to friendly",\n\t\t"command": "Trade"', "patch Katrine Trade command")
    require(client_npcs, 'new NPCDef("Katrine", "She doesn\'t look to friendly", shopOption, 35, 25, 10, 30', "client Katrine right-click Trade command")

    print("PASS: Katrine sells first-six-tier thrown weapons with Trade shortcuts")


if __name__ == "__main__":
    main()
