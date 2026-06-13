#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCENE = ROOT / "Client_Base" / "src" / "orsc" / "graphics" / "three" / "Scene.java"
POLYGON = ROOT / "Client_Base" / "src" / "orsc" / "graphics" / "three" / "Polygon.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def forbid(text: str, needle: str, description: str) -> None:
    if needle in text:
        fail(f"retired {description} still present: {needle}")


def main() -> None:
    scene = SCENE.read_text(encoding="utf-8")
    polygon = POLYGON.read_text(encoding="utf-8")

    require(polygon, "int modelIndex;", "polygon model-order field")
    require(scene, "private int comparePolygonSortOrder(Polygon left, Polygon right)", "central polygon comparator")
    require(scene, "if (left.modelIndex != right.modelIndex) {\n\t\t\treturn left.modelIndex < right.modelIndex ? -1 : 1;\n\t\t}", "model-order depth tie-breaker")
    require(scene, "if (left.faceID != right.faceID) {\n\t\t\treturn left.faceID < right.faceID ? -1 : 1;\n\t\t}", "face-order depth tie-breaker")
    require(scene, "var27.modelIndex = var9;", "normal model polygon order capture")
    require(scene, "var16.modelIndex = this.modelCount;", "sprite model polygon order capture")
    forbid(scene, "left.model == this.m_T && right.model == this.m_T", "sprite-only depth tie-breaker")

    print("PASS: client render order uses stable equal-depth polygon tie-breakers")


if __name__ == "__main__":
    main()
