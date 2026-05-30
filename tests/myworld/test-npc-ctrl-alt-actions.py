#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    text = CLIENT.read_text(encoding="utf-8")
    helper_start = text.find("private int selectCtrlClickNpcAltAction(int item)")
    if helper_start < 0:
        fail("missing Ctrl-click NPC alt-action selector")

    helper_end = text.find("\n\tprivate ", helper_start + 1)
    if helper_end < 0:
        fail("could not locate end of Ctrl-click NPC alt-action selector")
    helper = text[helper_start:helper_end]

    require(text, "item = selectCtrlClickNpcAltAction(item);", "menu dispatch hook")
    require(helper, "this.controlPressed", "Ctrl key guard")
    require(helper, "MenuItemAction.NPC_TALK_TO", "Talk-to primary action guard")
    require(helper, "MenuItemAction.NPC_COMMAND1", "NPC command 1 shortcut target")
    require(helper, "MenuItemAction.NPC_COMMAND2", "NPC command 2 shortcut target")
    require(helper, 'normalizedLabel.equals("bank")', "bank label shortcut")
    require(helper, 'normalizedLabel.equals("shop")', "shop label shortcut")
    require(helper, 'normalizedLabel.equals("trade")', "trade-as-shop label shortcut")

    lowered = helper.lower()
    for forbidden in ["shop_buy", "shop_sell", "buy_", "sell_"]:
        if forbidden in lowered:
            fail(f"Ctrl-click NPC shortcut should not touch buy/sell handling: {forbidden}")

    print("PASS: NPC Ctrl-click shortcuts are scoped to Bank/Shop/Trade alt actions")


if __name__ == "__main__":
    main()
