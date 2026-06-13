#!/usr/bin/env python3
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
HERBLAW = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "skills" / "herblaw" / "Herblaw.java"

BURNT_MEAT_AND_FISH = (
    "BURNTMEAT",
    "BURNT_FISH",
    "BURNT_TROUT",
    "BURNT_PIKE",
    "BURNT_TUNA",
    "BURNT_SWORDFISH",
    "BURNT_LOBSTER",
    "BURNT_SHARK",
    "BURNT_MANTA_RAY",
    "BURNT_SEA_TURTLE",
    "BURNT_OOMLIE_MEAT_PARCEL",
)

NON_MEAT_BURNT_FOODS = (
    "BURNTBREAD",
    "BURNTPIE",
    "BURNT_PIZZA",
    "BURNT_CAKE",
    "BURNT_STEW",
    "BURNT_CURRY",
    "BURNT_GNOMEBATTA",
    "BURNT_GNOMECRUNCHIE",
    "BURNT_GNOMEBOWL",
    "BURNT_PITTA_BREAD",
    "BURNT_SEAWEED_SOUP",
)


def fail(message: str) -> NoReturn:
    print(f"FAIL: {message}")
    sys.exit(1)


def main() -> None:
    text = HERBLAW.read_text(encoding="utf-8")
    for item in BURNT_MEAT_AND_FISH:
        if f"case {item}:" not in text:
            fail(f"Burnt meat/fish ash grinding missing ItemId.{item}")

    if "newID = ItemId.ASHES.id();" not in text:
        fail("Burnt meat/fish pestle grinding should produce Ashes")

    if '" into ashes"' not in text:
        fail("Burnt meat/fish pestle grinding should use an ashes-specific message")

    for item in NON_MEAT_BURNT_FOODS:
        if f"case {item}:" in text:
            fail(f"Non-meat burnt food should not be grindable into ashes: ItemId.{item}")

    print("PASS: burnt meat and fish grind into ashes")


if __name__ == "__main__":
    main()
