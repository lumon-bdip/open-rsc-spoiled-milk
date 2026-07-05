#!/usr/bin/env python3
"""Validate god special prayers require maces and god spells require staves."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def extract_between(source: str, start: str, end: str) -> str:
    start_index = source.find(start)
    require(start_index >= 0, f"Missing block start: {start}")
    end_index = source.find(end, start_index)
    require(end_index > start_index, f"Missing block end: {end}")
    return source[start_index:end_index]


def main() -> None:
    catalog = (ROOT / "server/src/com/openrsc/server/model/entity/player/PrayerCatalog.java").read_text(encoding="utf-8")
    prayers = (ROOT / "server/src/com/openrsc/server/model/entity/player/Prayers.java").read_text(encoding="utf-8")
    prayer_handler = (ROOT / "server/src/com/openrsc/server/net/rsc/handlers/PrayerHandler.java").read_text(encoding="utf-8")
    equipment = (ROOT / "server/src/com/openrsc/server/model/container/Equipment.java").read_text(encoding="utf-8")
    divine_grace = (ROOT / "server/src/com/openrsc/server/content/DivineGrace.java").read_text(encoding="utf-8")
    divine_retribution = (ROOT / "server/src/com/openrsc/server/content/DivineRetribution.java").read_text(encoding="utf-8")
    corrosive_aura = (ROOT / "server/src/com/openrsc/server/content/CorrosiveAura.java").read_text(encoding="utf-8")
    spell_handler = (ROOT / "server/src/com/openrsc/server/net/rsc/handlers/SpellHandler.java").read_text(encoding="utf-8")
    client_defs = (ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java").read_text(encoding="utf-8")

    require('"Saving Grace"' in catalog, "Saradomin special prayer should be renamed to Saving Grace")
    require('addPrayerDefinition(60, "Saving Grace"' in client_defs, "Client prayer definition should use Saving Grace")

    for snippet in (
        "getActivationBlockMessage",
        "getActivationPointCost",
        "getRequiredSpecialPrayerMace",
        "getRequiredGodCape",
        "SPECIAL_PRAYER_NO_CAPE_COST_NUMERATOR = 3",
        "SPECIAL_PRAYER_NO_CAPE_COST_DENOMINATOR = 2",
        "PrayerCatalog.PrayerKind.SPECIAL",
        "ItemId.SARADOMIN_MACE.id()",
        "ItemId.ZAMORAK_MACE.id()",
        "ItemId.GUTHIX_MACE.id()",
        "ItemId.SARADOMIN_CAPE.id()",
        "ItemId.ZAMORAK_CAPE.id()",
        "ItemId.GUTHIX_CAPE.id()",
        "You lack the necessary holy artifact",
    ):
        require(snippet in prayers, f"Prayers missing special mace gate snippet: {snippet}")
    for forbidden in (
        "getSpecialPrayerMaceName",
        "You need to wield the ",
        'return "Saradomin mace"',
        'return "Zamorak mace"',
        'return "Guthix mace"',
    ):
        require(forbidden not in prayers, f"Prayer missing-artifact message should not reveal exact mace: {forbidden}")

    require("prayers.getActivationBlockMessage(prayerID)" in prayer_handler,
            "PrayerHandler should use specific activation block messages")
    require("player.getPrayers().deactivateUnavailableEquipmentPrayers();" in equipment,
            "Equipment changes should turn off special prayers if the matching mace is removed")
    require("player.getPrayers().deactivateOverflowingPrayers();" in equipment,
            "Equipment changes should re-check prayer allocation after cape cost changes")

    require("ItemId.SARADOMIN_MACE.id()" in divine_grace,
            "Saving Grace runtime proc should require Saradomin mace")
    require("ItemId.ZAMORAK_MACE.id()" in divine_retribution,
            "Divine Retribution runtime proc should require Zamorak mace")
    require("ItemId.GUTHIX_MACE.id()" in corrosive_aura,
            "Corrosive Aura runtime proc should require Guthix mace")

    god_spell_block = extract_between(
        spell_handler,
        "case CLAWS_OF_GUTHIX:",
        "if(getPlayer().getConfig().WANT_OPENPK_POINTS)"
    )
    for staff in (
        "ItemId.STAFF_OF_GUTHIX.id()",
        "ItemId.STAFF_OF_SARADOMIN.id()",
        "ItemId.STAFF_OF_ZAMORAK.id()",
    ):
        require(staff in god_spell_block, f"God spells should require staff: {staff}")
    require(god_spell_block.count("You lack the necessary holy artifact") == 3,
            "Each god spell staff gate should use the generic holy artifact message")
    combat_formula = (ROOT / "server/src/com/openrsc/server/event/rsc/impl/combat/CombatFormula.java").read_text(encoding="utf-8")
    god_spell_max = extract_between(
        combat_formula,
        "public static int getGodSpellMax",
        "public static int calculateGodSpellDamage(final Player source, final Mob victim)"
    )
    require("source.isCharged()" not in god_spell_max,
            "God spell cap boost should no longer require Charge")
    require("return hasCapeEquipped ? 32 : 24;" in god_spell_max
            and "return hasCapeEquipped ? 25 : 18;" in god_spell_max,
            "God spell cap boost should be driven directly by god cape presence")
    for forbidden in (
        "must wield the staff",
        "staff of guthix",
        "staff of saradomin",
        "staff of zamorak",
        "hasFull",
        "god armor",
        "BLACK_PLATE",
        "WHITE_PLATE",
        "GREY_PLATE",
        "BLACK_KNIGHT",
        "WHITE_KNIGHT",
        "GREY_KNIGHT",
    ):
        require(forbidden not in god_spell_block, f"God spell block should not allow an armor-set fallback: {forbidden}")

    print("PASS: god special prayers and god spell staff gates validated")


if __name__ == "__main__":
    main()
