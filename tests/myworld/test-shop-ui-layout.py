#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def main() -> None:
    text = CLIENT.read_text(encoding="utf-8")
    shop_start = text.find("private void drawDialogShop()")
    if shop_start < 0:
        fail("missing shop dialog renderer")
    shop_end = text.find("\n\tprivate ", shop_start + 1)
    if shop_end < 0:
        fail("could not locate end of shop dialog renderer")
    shop = text[shop_start:shop_end]

    stock_marker = "drawString(\"\" + this.shopItemCount[slot], 1 + sx, 10 + sy"
    owned_marker = "this.getSurface().b(47 + sx, \"\" + this.getInventoryCount(this.shopCategoryID[slot], this.shopItemNoted[slot]),\n\t\t\t\t\t\t\t\t31 + sy"

    if stock_marker not in shop:
        fail("shop stock count should remain at the top-left of each slot")
    if owned_marker not in shop:
        fail("shop owned count should be drawn at the bottom-right of each slot")
    if "this.getInventoryCount(this.shopCategoryID[slot], this.shopItemNoted[slot]),\n\t\t\t\t\t\t\t\t10 + sy" in shop:
        fail("shop owned count is still using the stock-count vertical position")

    print("PASS: shop owned count is separated from stock count")


if __name__ == "__main__":
    main()
