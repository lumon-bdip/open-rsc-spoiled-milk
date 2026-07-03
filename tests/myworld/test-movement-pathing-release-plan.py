#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
NPC_BEHAVIOR = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcBehavior.java"
NPC = ROOT / "server/src/com/openrsc/server/model/entity/npc/Npc.java"
DOC = ROOT / "docs/myworld/in-progress-work-plans/movement-pathing-release-plan.md"
DOC_INDEX = ROOT / "docs/myworld/README.md"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
    if snippet not in text:
        fail(f"{label} missing expected snippet: {snippet}")


def main() -> None:
    behavior_text = NPC_BEHAVIOR.read_text(encoding="utf-8")
    npc_text = NPC.read_text(encoding="utf-8")
    doc_text = DOC.read_text(encoding="utf-8")
    index_text = DOC_INDEX.read_text(encoding="utf-8")

    require(behavior_text, "nextRoamMovementAt", "NPC roam jitter")
    require(behavior_text, "scheduleNextRoamMovement(now)", "NPC roam jitter")
    require(behavior_text, "ROAM_JITTER_TICKS", "NPC roam jitter")
    require(npc_text, "startIndex = DataConversions.random", "Adjacent NPC displacement")
    require(npc_text, "possiblePoints.get((startIndex + i) % possiblePoints.size())", "Adjacent NPC displacement")

    for snippet in (
        "Server movement is still tile-authoritative",
        "client reaches the next tile before another authoritative",
        "server `Path.addStep(...)` is still a greedy direct",
        "frame-time interpolation",
        "C_NPC_MOVE_PER_FRAME",
        "Repath MyWorld melee attack walk-to actions",
        "bounded A* helper for MyWorld melee approach paths",
        "bounded BFS/A*",
    ):
        require(doc_text, snippet, "Movement/pathing plan")

    require(index_text, "movement-pathing-release-plan.md", "MyWorld docs index")

    print("PASS: Movement/pathing release guardrails look correct")


if __name__ == "__main__":
    main()
