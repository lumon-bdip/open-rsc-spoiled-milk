#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORLD = ROOT / "Client_Base/src/orsc/graphics/three/World.java"
SCENE = ROOT / "Client_Base/src/orsc/graphics/three/Scene.java"


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        raise SystemExit(f"Missing {label}: {snippet}")


def main() -> None:
    world = WORLD.read_text(encoding="utf-8")
    scene = SCENE.read_text(encoding="utf-8")

    require(
        world,
        "private boolean isPickableInvisibleOverlay(int xTile, int zTile, int plane)",
        "scoped invisible overlay click helper",
    )
    require(
        world,
        "return this.getTileDecorationID(xTile, zTile, plane) == 26;",
        "overlay 26-only clickability guard",
    )
    require(
        world,
        "colorResource != Scene.TRANSPARENT || pickableInvisibleOverlay",
        "transparent overlay 26 terrain face insertion",
    )
    require(
        scene,
        "boolean pickableTransparentFace = var2.facePickIndex != null",
        "transparent face pick-index null guard",
    )
    require(
        scene,
        "var3 < var2.facePickIndex.length",
        "transparent face pick-index bounds guard",
    )
    require(
        scene,
        "var13 != Scene.TRANSPARENT || pickableTransparentFace",
        "transparent terrain pick face inclusion",
    )
    require(
        scene,
        "var25.m_b >= 0 && var25.m_b != Scene.TRANSPARENT",
        "transparent pick face lighting guard",
    )
    require(
        scene,
        "this.m_Xb < this.m_Cb && var25.m_b != Scene.TRANSPARENT",
        "transparent pick face draw skip",
    )

    print("PASS: cosmic altar invisible overlay remains invisible but clickable")


if __name__ == "__main__":
    main()
