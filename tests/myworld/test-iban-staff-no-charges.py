#!/usr/bin/env python3
"""Ensure Staff of Iban no longer uses the legacy charge system."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[2]
SPELL_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/SpellHandler.java"
MECHANISM = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/members/undergroundpass/mechanism/UndergroundPassMechanismMap2.java"
KOFTIK = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/quests/members/undergroundpass/npcs/UndergroundPassKoftik.java"
SHORTCUTS = ROOT / "server/plugins/com/openrsc/server/plugins/custom/quests/MyWorldQuestShortcuts.java"
ADMINS = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/commands/Admins.java"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> int:
    spell_handler = SPELL_HANDLER.read_text(encoding="utf-8")
    combined = "\n".join(path.read_text(encoding="utf-8") for path in (
        SPELL_HANDLER,
        MECHANISM,
        KOFTIK,
        SHORTCUTS,
        ADMINS,
    ))

    if "hasEquipped(ItemId.STAFF_OF_IBAN.id())" not in spell_handler:
        fail("Iban Blast should still require Staff of Iban to be equipped")
    if "Quests.UNDERGROUND_PASS" not in spell_handler:
        fail("Iban Blast should still require Underground Pass completion")
    for retired_snippet in (
        "Iban blast_casts",
        "you need to recharge the staff of iban",
        "at iban's temple",
        "FLAMES_OF_ZAMORAK && item.getCatalogId() == ItemId.STAFF_OF_IBAN.id()",
    ):
        if retired_snippet in combined:
            fail(f"Iban staff charge system still present: {retired_snippet}")

    print("PASS: Staff of Iban no longer uses cast charges")
    return 0


if __name__ == "__main__":
    sys.exit(main())
