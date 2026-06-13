#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORLD = ROOT / "Client_Base/src/orsc/graphics/three/World.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    world = WORLD.read_text(encoding="utf-8")

    require("private void resetModels()" in world,
            "World region reset method should still exist")
    require("System.gc()" not in world,
            "Client region loads must not force a stop-the-world GC pause")

    print("PASS: client region loading avoids forced GC stalls")


if __name__ == "__main__":
    main()
