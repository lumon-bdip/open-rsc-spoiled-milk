#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "Client_Base" / "src" / "orsc" / "mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    text = CLIENT.read_text(encoding="utf-8")
    require(
        text,
        "if (hardAreaLoad || heightOffsetChanged) {\n\t\t\t\t\t\tthis.gameObjectInstanceCount = 0;\n\t\t\t\t\t\tthis.wallObjectInstanceCount = 0;\n\t\t\t\t\t}",
        "hard area/plane object cache reset",
    )
    require(
        text,
        "if (hardAreaLoad || heightOffsetChanged) {\n\t\t\t\t\t\tthis.groundItemCount = 0;",
        "hard area/plane ground item cache reset",
    )
    print("PASS: scene entity lifecycle clears retained caches on hard area and plane loads")


if __name__ == "__main__":
    main()
