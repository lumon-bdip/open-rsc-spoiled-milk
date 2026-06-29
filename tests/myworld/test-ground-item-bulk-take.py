#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
DEFAULT_PLUGIN = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/defaults/Default.java"
GROUND_ITEM_TAKE = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/GroundItemTake.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
TARGET_POSITION_STRUCT = ROOT / "server/src/com/openrsc/server/net/rsc/struct/incoming/TargetPositionStruct.java"
PARSERS = ROOT / "server/src/com/openrsc/server/net/rsc/parsers/impl"
CUSTOM_PARSER = PARSERS / "PayloadCustomParser.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    default_plugin = DEFAULT_PLUGIN.read_text(encoding="utf-8")
    take_handler = GROUND_ITEM_TAKE.read_text(encoding="utf-8")
    player = PLAYER.read_text(encoding="utf-8")
    struct = TARGET_POSITION_STRUCT.read_text(encoding="utf-8")
    custom_parser = CUSTOM_PARSER.read_text(encoding="utf-8")
    parser_text = "\n".join(path.read_text(encoding="utf-8") for path in PARSERS.glob("Payload*Parser.java"))

    require(client, "this.controlPressed ? this.getCtrlGroundItemTakeCount(indexOrX, idOrZ, dir) : 1",
            "ctrl-click ground item take count selection")
    require(client, "private void sendGroundItemTakePacket(int localX, int localZ, int itemId, int takeCount)",
            "ground item take packet helper")
    require(client, "if (takeCount > 1) {\n\t\t\tthis.packetHandler.getClientStream().bufferBits.putShort(takeCount);",
            "optional bulk take packet count")
    require(client, "private int countMatchingGroundItemsOnTile(int localX, int localZ, int itemId)",
            "same-tile same-item counting")
    require(client, "S_PLAYER_INVENTORY_SLOTS - this.inventoryItemCount", "unstackable pickup slot cap")

    require(struct, "public int takeCount = 1;", "default single ground item take count")
    require(player, "MAX_BULK_GROUND_ITEM_TAKE_COUNT = 5000", "server bulk take count cap")
    require(take_handler, "payload.takeCount", "server reads requested bulk take count")
    require(take_handler, "item.setAttribute(Player.BULK_GROUND_ITEM_TAKE_COUNT_ATTRIBUTE, takeCount)",
            "server attaches bulk count to the default pickup item")
    require(take_handler, "item.removeAttribute(Player.BULK_GROUND_ITEM_TAKE_COUNT_ATTRIBUTE)",
            "special take plugins clear unused bulk marker")
    require(default_plugin, "player.groundItemTakeMatching(i);", "default take plugin consumes matching stack")
    require(player, "public int groundItemTakeMatching(final GroundItem firstItem)", "matching ground item take loop")
    require(player, "getViewArea().getVisibleGroundItem(itemId, location, this)", "bulk loop fetches next visible match")
    require(player, "if (!canTakeVisibleGroundItem(item, false))", "bulk loop revalidates each item without plugin self-busy blocking")
    require(player, "if (!groundItemTake(item))", "bulk loop stops when inventory cannot hold more")

    if parser_text.count("tp.takeCount = packet.readShort();") != 1:
        fail("only the custom/current parser should read optional bulk take count")
    require(custom_parser, "tp.takeCount = packet.readShort();", "custom parser optional bulk take count")
    require(custom_parser, "return packet.getLength() == 6 || packet.getLength() == 8;",
            "custom parser accepts old and bulk ground take payload lengths")

    print("PASS: ctrl-click ground item bulk take remains wired through client, parser, and server")


if __name__ == "__main__":
    main()
