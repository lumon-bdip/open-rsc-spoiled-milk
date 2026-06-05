#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"
OBJECT_DEFS = ROOT / "server" / "conf" / "server" / "defs" / "GameObjectDef.xml"
PORT_SARIM = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "portsarim" / "PortSarimSailor.java"
KARAMJA = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "npcs" / "karamja" / "BoatFromKaramja.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def object_def(object_id: int) -> ET.Element:
    root = ET.parse(OBJECT_DEFS).getroot()
    defs = root.findall("GameObjectDef")
    if object_id >= len(defs):
        fail(f"missing object def {object_id}")
    return defs[object_id]


def element_text(defn: ET.Element, name: str) -> str:
    elem = defn.find(name)
    if elem is None or elem.text is None:
        fail(f"missing {name} on object definition")
    return elem.text


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    require(client, "item = selectCtrlClickObjectTravelAction(item);", "Ctrl-click object travel dispatch hook")
    require(client, "private int selectCtrlClickObjectTravelAction(int item)", "Ctrl-click object travel selector")
    require(client, "MenuItemAction.OBJECT_COMMAND1", "object command 1 guard")
    require(client, "MenuItemAction.OBJECT_COMMAND2", "object command 2 target")
    require(client, 'equalsIgnoreCase("Travel")', "Travel label match")

    for object_id in (155, 156, 157, 161, 162, 163):
        defn = object_def(object_id)
        if element_text(defn, "command1").lower() != "board":
            fail(f"object {object_id} primary action should remain board")
        if element_text(defn, "command2") != "Travel":
            fail(f"object {object_id} secondary action should be Travel")

    port_sarim = PORT_SARIM.read_text(encoding="utf-8")
    require(port_sarim, '"You do not meet the requirements to travel"', "Port Sarim shortcut failure message")
    require(port_sarim, "player.click == 1", "Port Sarim right-click shortcut")
    require(port_sarim, 'arg1.equalsIgnoreCase("travel")', "Port Sarim travel command shortcut")
    require(port_sarim, "shortcutTravelToKaramja(player)", "Port Sarim shortcut handler")
    require(port_sarim, "new Item(ItemId.COINS.id(), 30)", "Port Sarim travel cost")
    require(port_sarim, "player.teleport(324, 713, false)", "Port Sarim destination")

    karamja = KARAMJA.read_text(encoding="utf-8")
    require(karamja, '"You do not meet the requirements to travel"', "Karamja shortcut failure message")
    require(karamja, "player.click == 1", "Karamja right-click shortcut")
    require(karamja, 'command.equalsIgnoreCase("travel")', "Karamja travel command shortcut")
    require(karamja, "shortcutTravelToPortSarim(player)", "Karamja shortcut handler")
    require(karamja, "ItemId.KARAMJA_RUM.id()", "Karamja rum travel blocker")
    require(karamja, "new Item(ItemId.COINS.id(), 30)", "Karamja travel cost")
    require(karamja, "teleport(player, 269, 648)", "Karamja destination")

    print("PASS: Port Sarim and Karamja boat shortcuts skip dialogue but preserve travel requirements")


if __name__ == "__main__":
    main()
