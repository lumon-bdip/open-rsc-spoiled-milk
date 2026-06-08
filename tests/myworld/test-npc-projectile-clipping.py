#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PATH_VALIDATION = ROOT / "server/src/com/openrsc/server/model/PathValidation.java"
NPC_BEHAVIOR = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcBehavior.java"
NPC_RANGE = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/RangeEventNpc.java"
PLAYER_PROJECTILES = (
    ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/RangeEvent.java",
    ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ThrowingEvent.java",
    ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/MagicCombatEvent.java",
)


def require(condition, message):
    if not condition:
        raise AssertionError(message)


path_validation = PATH_VALIDATION.read_text()
require(
    "return checkPath(world, src, dest, false);" in path_validation,
    "Default projectile path validation must retain player-transparent barriers",
)
require(
    "checkAdjacentDistance(world, curPoint, nextPoint, ignoreProjectileAllowed)"
    in path_validation,
    "Strict projectile path validation must reach tile collision checks",
)

require(
    "PathValidation.checkPath(npc.getWorld(), npc.getLocation(), target.getLocation(), true)"
    in NPC_BEHAVIOR.read_text(),
    "Modern hostile NPC projectiles must treat fences as blocking",
)
require(
    "PathValidation.checkPath(getWorld(), owner.getLocation(), victim.getLocation(), true)"
    in NPC_RANGE.read_text(),
    "Legacy hostile NPC ranged attacks must treat fences as blocking",
)

for path in PLAYER_PROJECTILES:
    source = path.read_text()
    require(
        "PathValidation.checkPath(" in source
        and "PathValidation.checkPath(player.getWorld(), player.getLocation(), target.getLocation(), true)"
        not in source,
        f"{path.name} must retain player projectile transparency",
    )

print("NPC projectile clipping checks passed")
