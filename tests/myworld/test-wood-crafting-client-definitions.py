#!/usr/bin/env python3
"""Validate client definitions for knife-on-log crafting outputs."""

import re
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
FLETCHING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/fletching/Fletching.java"


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, message: str) -> None:
    if snippet not in text:
        fail(message)


def require_helper_uses_explicit_ids(client_text: str, helper_name: str, expected_calls: tuple[str, ...]) -> None:
    match = re.search(
        rf"private static void {helper_name}\(.*?\n\t\}}",
        client_text,
        re.DOTALL,
    )
    if match is None:
        fail(f"Missing client helper {helper_name}")
    helper = match.group(0)
    if "items.add(new ItemDef" in helper:
        fail(f"{helper_name} must not append sparse item IDs with items.add")
    for call in expected_calls:
        require(helper, call, f"{helper_name} missing explicit item-slot write: {call}")


def main() -> None:
    client_text = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
    fletching_text = FLETCHING.read_text(encoding="utf-8")

    require_helper_uses_explicit_ids(
        client_text,
        "addCustomWoodBowDefinitions",
        (
            "setCustomItemDefinition(unstrungLongId,",
            "setCustomItemDefinition(unstrungShortId,",
            "setCustomItemDefinition(longbowId,",
            "setCustomItemDefinition(shortbowId,",
        ),
    )
    require_helper_uses_explicit_ids(
        client_text,
        "addWoodCrossbowDefinition",
        ("setCustomItemDefinition(id, item);",),
    )
    require_helper_uses_explicit_ids(
        client_text,
        "addCustomWoodStaffDefinitions",
        (
            "setCustomItemDefinition(baseStaffId,",
            "setCustomItemDefinition(airStaffId,",
            "setCustomItemDefinition(waterStaffId,",
            "setCustomItemDefinition(earthStaffId,",
            "setCustomItemDefinition(fireStaffId,",
        ),
    )

    for item_name in (
        "PINE_STAFF",
        "PALM_STAFF",
        "EBONY_STAFF",
        "BLOOD_STAFF",
        "PINE_FISHING_ROD",
        "BLOOD_FISHING_ROD",
        "UNSTRUNG_PINE_SHORTBOW",
        "UNSTRUNG_BLOOD_LONGBOW",
        "OAK_CROSSBOW",
        "BLOOD_CROSSBOW",
    ):
        require(
            fletching_text,
            f"ItemId.{item_name}.id()",
            f"Knife-on-log crafting should still reference {item_name}",
        )

    client_staff_ids = {
        int(value)
        for value in re.findall(
            r"MYWORLD_STAFF_BASE_IDS\s*=\s*\{(.*?)\};",
            client_text,
            re.DOTALL,
        )[0].replace("\n", " ").split(",")
        if value.strip()
    }
    rune_staff_block = re.findall(
        r"MYWORLD_RUNE_STAFF_IDS\s*=\s*\{(.*?)\};",
        client_text,
        re.DOTALL,
    )[0]
    client_staff_ids.update(int(value) for value in re.findall(r"\d+", rune_staff_block))

    legacy_staff_ids = {100, 101, 102, 103, 197}
    helper_staff_ids = set()
    for call in re.findall(r"addCustomWoodStaffDefinitions\((.*?)\);", client_text, re.DOTALL):
        helper_staff_ids.update(int(value) for value in re.findall(r"\b\d{3,4}\b", call))

    for item_id in sorted(client_staff_ids - legacy_staff_ids - helper_staff_ids):
        require(
            client_text,
            f"setCustomItemDefinition({item_id},",
            f"Craftable, enchanted, or blessed staff {item_id} should have an explicit client definition",
        )

    print("PASS: wood crafting client definitions validated")


if __name__ == "__main__":
    main()
