#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORLD = ROOT / "Client_Base/src/orsc/graphics/three/World.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    world = WORLD.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")

    require("private void resetModels()" in world,
            "World region reset method should still exist")
    require("System.gc()" not in world,
            "Client region loads must not force a stop-the-world GC pause")
    require("sectorTemplateCache" in world and "world-sector-preload" in world,
            "Client region loads should keep a background sector template cache")
    require("public void preloadSections(int worldX, int worldZ, int plane)" in world,
            "World should expose predictive sector preloading")
    require("sectors[sector] = loadSectorTemplate(height, sectionX, sectionY).copy();" in world,
            "Active 2x2 sectors should be cloned from cached templates")
    require("public Sector copy()" in (ROOT / "Client_Base/src/com/openrsc/client/model/Sector.java").read_text(encoding="utf-8"),
            "Sector templates should support cloning before active-world mutation")
    require("this.world.preloadSections(wantX, wantZ, this.requestedPlane);" in mudclient,
            "Normal movement updates should warm sectors before boundary crossings")

    print("PASS: client region loading avoids forced GC stalls and warms sector cache")


if __name__ == "__main__":
    main()
