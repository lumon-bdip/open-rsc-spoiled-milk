#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"


def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"{label} missing expected snippet: {needle!r}")


def main() -> None:
    client = CLIENT.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")

    require(
        client,
        "boolean needNextRegion = loadNextRegion(worldZ, worldX, false);",
        "custom movement region check",
    )
    require(
        client,
        "if (needNextRegion && consumeRegionLoadNeedsHardPlayerReset())",
        "custom movement hard region reset handling",
    )
    require(
        client,
        "if (!World.isLocalTile(this.playerLocalX, this.playerLocalZ))",
        "custom local player tile bounds guard",
    )
    require(
        client,
        "private boolean isValidCustomMovementDirection(int direction)",
        "custom movement direction guard",
    )
    require(
        client,
        "if (character == null || !World.isLocalTile(localTileX, localTileZ) || !isValidCustomMovementDirection(direction))",
        "custom movement waypoint guard",
    )
    require(
        applet,
        "if (imageProducer != null) {\n\t\t\timageProducer.setDimensions(newWidth, newHeight);\n\t\t}",
        "resize image producer null guard",
    )

    print("PASS: custom movement update and resize stability guards are present")


if __name__ == "__main__":
    main()
