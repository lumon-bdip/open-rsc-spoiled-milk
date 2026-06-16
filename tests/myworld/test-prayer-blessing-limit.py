#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PRAYER_PLUGIN_DIR = ROOT / "server/plugins/com/openrsc/server/plugins/custom/myworld/skills/prayer"
LIMITER = PRAYER_PLUGIN_DIR / "PrayerBlessingLimit.java"
BLESSING_PLUGINS = [
    PRAYER_PLUGIN_DIR / "BlessedStaffs.java",
    PRAYER_PLUGIN_DIR / "BlessedWoolArmor.java",
    PRAYER_PLUGIN_DIR / "BlessedSymbols.java",
    PRAYER_PLUGIN_DIR / "GodKnightEquipment.java",
]
DESTROY_PLUGIN = PRAYER_PLUGIN_DIR / "DestroyOpposingBlessedObject.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def main() -> None:
    limiter = LIMITER.read_text(encoding="utf-8")
    require("BLESSINGS_PER_HOUR = 10" in limiter, "prayer blessing cap should be 10 per hour")
    require("60L * 60L * 1000L" in limiter, "prayer blessing window should be one hour")
    require('"You hear a low rumbling voice..."' in limiter, "limit should send the rumbling voice intro")
    require('"You must learn Patience"' in limiter, "Saradomin limit message missing")
    require('"That is quite enough for now"' in limiter, "Guthix limit message missing")
    require('"Leave. Me. ALONE!"' in limiter, "Zamorak limit message missing")
    require("myworld_prayer_blessing_window_start" in limiter, "limit should persist the window start in player cache")
    require("myworld_prayer_blessing_window_count" in limiter, "limit should persist the blessing count in player cache")
    require("count < BLESSINGS_PER_HOUR" in limiter, "limit should allow only counts below the cap")
    require("Math.min(BLESSINGS_PER_HOUR, count + 1)" in limiter, "recording should not exceed the cap")

    for plugin_path in BLESSING_PLUGINS:
        plugin = plugin_path.read_text(encoding="utf-8")
        require(
            "PrayerBlessingLimit.canBless(player, godLine)" in plugin,
            f"{plugin_path.name} should check the hourly blessing limit",
        )
        require(
            "PrayerBlessingLimit.recordBlessing(player)" in plugin,
            f"{plugin_path.name} should record successful blessings",
        )
        can_bless_index = plugin.index("PrayerBlessingLimit.canBless(player, godLine)")
        remove_index = plugin.index("player.getCarriedItems().remove(item)")
        record_index = plugin.index("PrayerBlessingLimit.recordBlessing(player)")
        require(can_bless_index < remove_index, f"{plugin_path.name} should check the limit before consuming the item")
        require(remove_index < record_index, f"{plugin_path.name} should record only after item removal succeeds")

    destroy = DESTROY_PLUGIN.read_text(encoding="utf-8")
    require("PrayerBlessingLimit" not in destroy, "destroying opposing blessed objects should not spend blessing slots")

    print("PASS: prayer blessing hourly limit validated")


if __name__ == "__main__":
    main()
