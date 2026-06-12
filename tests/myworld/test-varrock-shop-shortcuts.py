#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NPC_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "NpcDefs.json"
NPC_PATCH_18 = ROOT / "server" / "conf" / "server" / "defs" / "NpcDefsPatch18.json"
CLIENT_NPCS = ROOT / "Client_Base" / "src" / "com" / "openrsc" / "client" / "entityhandling" / "EntityHandler.java"
APOTHECARY = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "varrock" / "Apothecary.java"
AUBURY = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "varrock" / "AuburysRunes.java"
OPENPK_AUBURY = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "custom" / "npcs" / "AuburysRunesOpenPk.java"
MUDCLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"
SERVER_ENTITY_HANDLER = ROOT / "server" / "src" / "com" / "openrsc" / "server" / "external" / "EntityHandler.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def reject(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"unexpected {description}: {needle}")


def main() -> None:
    npc_defs = NPC_DEFS.read_text(encoding="utf-8")
    npc_patch = NPC_PATCH_18.read_text(encoding="utf-8")
    client_npcs = CLIENT_NPCS.read_text(encoding="utf-8")
    apothecary = APOTHECARY.read_text(encoding="utf-8")
    aubury = AUBURY.read_text(encoding="utf-8")
    openpk_aubury = OPENPK_AUBURY.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    server_entity_handler = SERVER_ENTITY_HANDLER.read_text(encoding="utf-8")

    require(npc_defs, '"id": 33,\n        "name": "Apothecary"', "base Apothecary definition")
    require(npc_defs, '"name": "Apothecary",\n        "description": "I wonder if he has any good potions",\n        "command": "Trade",\n        "command2": "Shop"', "base Apothecary shop commands")
    require(npc_defs, '"name": "Aubury",\n        "description": "I think he might be a shop keeper",\n        "command": "Trade",\n        "command2": "Shop"', "base Aubury shop commands")

    require(npc_patch, '"name": "Apothecary",\n\t\t"description": "I wonder if he has any good potions",\n\t\t"command": "Trade"', "patch Apothecary trade command")
    require(npc_patch, '"name": "Aubury",\n\t\t"description": "I think he might be a shop keeper",\n\t\t"command": "Trade"', "patch Aubury trade command")

    require(client_npcs, 'String shopOption2 = Config.S_RIGHT_CLICK_TRADE ? "Shop" : null;', "client alternate shop shortcut")
    require(client_npcs, 'new NPCDef("Apothecary", "I wonder if he has any good potions", shopOption, shopOption2', "client Apothecary shortcut commands")
    require(client_npcs, 'new NPCDef("Aubury", "I think he might be a shop keeper", shopOption, shopOption2', "client Aubury shortcut commands")

    require(apothecary, 'command.equalsIgnoreCase("Trade") || command.equalsIgnoreCase("Shop")', "Apothecary accepts Trade and Shop")
    require(apothecary, "player.getConfig().RIGHT_CLICK_TRADE", "Apothecary respects right-click trade config")
    require(apothecary, "player.setAccessingShop(shop);", "Apothecary direct shop opens shop")
    require(apothecary, "ActionSender.showShop(player, shop);", "Apothecary direct shop sends shop")

    for text, name in ((aubury, "Aubury"), (openpk_aubury, "OpenPK Aubury")):
        require(text, 'command.equalsIgnoreCase("Trade") || command.equalsIgnoreCase("Shop")', f"{name} accepts Trade and Shop")
        require(text, "ActionSender.showShop(player, shop);", f"{name} direct shop sends shop")
        reject(text, "player.getWorld().getNpc(n.getID()", f"{name} direct shop re-resolves clicked NPC")

    reject(apothecary, "player.getWorld().getNpc(n.getID()", "Apothecary direct shop re-resolves clicked NPC")

    require(mudclient, 'if (normalizedLabel.equals("shop") || normalizedLabel.equals("trade"))', "Ctrl-click shop/trade shortcut selector")
    require(mudclient, 'if (Config.S_RIGHT_CLICK_TRADE) AuburyDef.updateCommand2("Trade");', "Aubury teleport mode keeps Trade shortcut")
    require(server_entity_handler, 'getServer().getConfig().WANT_RUNECRAFT && !getServer().getConfig().WANT_MYWORLD', "legacy Aubury teleport override excluded from MyWorld")
    require(server_entity_handler, 'npcs.get(NpcId.AUBURY.id()).setCommand2("Shop");', "MyWorld Aubury keeps alternate Shop command at runtime")

    print("PASS: Varrock Apothecary and Aubury shop shortcuts are wired")


if __name__ == "__main__":
    main()
