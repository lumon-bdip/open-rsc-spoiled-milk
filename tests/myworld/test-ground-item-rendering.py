#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def forbid(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"retired {description} still present: {needle}")


def main() -> None:
    text = CLIENT.read_text(encoding="utf-8")
    require(text, "groundItemRenderStackIndex", "per-item ground stack index")
    require(text, "assignGroundItemStackOffsets();", "stack offset assignment")
    require(text, "getGroundItemStackOffsetX", "ground item x offset helper")
    require(text, "getGroundItemStackOffsetZ", "ground item z offset helper")
    require(text, "this.groundItemRenderOrder.add(index);", "all visible ground items stay renderable")
    forbid(text, "groundItemRenderTopByTile", "single top ground item per tile collapse")
    print("PASS: ground item rendering keeps stable stacked items visible")


if __name__ == "__main__":
    main()
