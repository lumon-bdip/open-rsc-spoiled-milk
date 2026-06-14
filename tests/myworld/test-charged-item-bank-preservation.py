#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BANK = ROOT / "server/src/com/openrsc/server/model/container/Bank.java"
INVENTORY = ROOT / "server/src/com/openrsc/server/model/container/Inventory.java"
DATABASE = ROOT / "server/src/com/openrsc/server/database/GameDatabase.java"


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        raise SystemExit(f"FAIL: {label} missing expected snippet: {snippet}")


def forbid(text: str, snippet: str, label: str) -> None:
    if snippet in text:
        raise SystemExit(f"FAIL: {label} contains forbidden snippet: {snippet}")


def main() -> None:
    bank = BANK.read_text(encoding="utf-8")
    inventory = INVENTORY.read_text(encoding="utf-8")
    database = DATABASE.read_text(encoding="utf-8")

    require(bank, "copyItemForBank(itemToAdd, itemToAdd.getAmount(), itemID)", "Bank new stack copy")
    require(bank, "copyItemForBank(itemToAdd, itemToAdd.getAmount() - remainingSize, itemID)", "Bank overflow copy")
    require(bank, "rememberLastItemDurability(existingStack, itemToAdd);", "Bank stack durability merge")
    require(bank, "item.getItemStatus().setDurability(bankItem.getItemStatus().getDurability());", "Bank withdraw durability copy")
    require(bank, "Item itemToAdd = depositItem.copy();", "Bank deposit source copy")
    require(bank, "itemToAdd.setCatalogId(itemToAddCatalogId);", "Bank deposit id normalization")
    require(bank, "itemToAdd.setAmount(itemToAddAmount);", "Bank deposit amount normalization")
    require(bank, "copyItemForTransfer(item, requestedAmount, item.getNoted())", "Bank stackable withdrawal copy")
    require(bank, "copyItemForTransfer(item, 1, item.getNoted())", "Bank unstackable withdrawal copy")
    forbid(bank, "Item itemToAdd = new Item(itemToAddCatalogId, itemToAddAmount);", "Bank deposit durability reset")

    require(inventory,
            "existingStack.getItemStatus().setDurability(itemToAdd.getItemStatus().getDurability());",
            "Inventory stack durability merge")
    require(database,
            "inventory[i].durability = player.getCarriedItems().getInventory().get(i).getItemStatus().getDurability();",
            "Inventory save durability persistence")
    forbid(database, "inventory[i].durability = 100;", "Inventory save durability reset")

    print("PASS: charged item durability is preserved through bank/inventory stack flows")


if __name__ == "__main__":
    main()
