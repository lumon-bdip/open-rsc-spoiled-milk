#!/usr/bin/env python3

import shutil
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REGISTRY = ROOT / "server/src/com/openrsc/server/model/world/AuthoredGroundItemRegistry.java"
WORLD = ROOT / "server/src/com/openrsc/server/model/world/World.java"
GROUND_ITEM = ROOT / "server/src/com/openrsc/server/model/entity/GroundItem.java"
POPULATOR = ROOT / "server/src/com/openrsc/server/database/WorldPopulator.java"
ADMINS = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/commands/Admins.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def require_text(path: Path, needle: str, description: str) -> None:
    require(needle in path.read_text(encoding="utf-8"), f"missing {description}: {needle}")


HARNESS = r"""
import com.openrsc.server.model.world.AuthoredGroundItemRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class AuthoredGroundItemRegistryHarness {
    private static final class Item {
        final String source;
        final int amount;

        Item(String source, int amount) {
            this.source = source;
            this.amount = amount;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        AuthoredGroundItemRegistry<Item> registry = new AuthoredGroundItemRegistry<>();
        AtomicInteger authoredCreations = new AtomicInteger();

        Item original = registry.register(343, 606,
            () -> new Item("authored", authoredCreations.incrementAndGet()));
        for (int i = 0; i < 50; i++) {
            Item seenAgain = registry.register(343, 606,
                () -> new Item("duplicate", authoredCreations.incrementAndGet()));
            require(seenAgain == original, "untouched authored spawn changed identity");
        }
        require(registry.size() == 1, "region activity accumulated authored copies");
        require(authoredCreations.get() == 1, "untouched authored spawn was recreated");

        long pickupGeneration = registry.remove(343, 606, original);
        require(pickupGeneration >= 0, "pickup did not release authored spawn");
        require(registry.remove(343, 606, original) == AuthoredGroundItemRegistry.NO_GENERATION,
            "repeated removal claimed the authored spawn twice");

        Item replacement = registry.registerForGeneration(343, 606, pickupGeneration,
            () -> new Item("respawn", authoredCreations.incrementAndGet()));
        Item repeatedTimer = registry.registerForGeneration(343, 606, pickupGeneration,
            () -> new Item("duplicate timer", authoredCreations.incrementAndGet()));
        require(replacement == repeatedTimer, "repeated timer created another authored item");
        require(registry.size() == 1, "pickup produced more than one replacement");
        require(authoredCreations.get() == 2, "pickup replacement count was not exactly one");

        long staleGeneration = registry.remove(343, 606, replacement);
        registry.reset();
        Item startupSpawn = registry.register(343, 606,
            () -> new Item("startup", authoredCreations.incrementAndGet()));
        Item repeatedStartup = registry.register(343, 606,
            () -> new Item("repeated startup", authoredCreations.incrementAndGet()));
        require(startupSpawn == repeatedStartup, "repeated startup registered the spawn twice");
        require(registry.registerForGeneration(343, 606, staleGeneration,
            () -> new Item("stale timer", authoredCreations.incrementAndGet())) == null,
            "pre-restart timer entered the new world generation");
        require(registry.size() == 1, "startup plus stale timer accumulated copies");

        Item adjacent = registry.register(344, 606,
            () -> new Item("adjacent authored", authoredCreations.incrementAndGet()));
        require(adjacent != startupSpawn && registry.size() == 2,
            "separate authored tiles did not remain independent");

        List<Item> dynamicDrops = new ArrayList<>();
        Item playerDrop = new Item("player", 7);
        Item npcDrop = new Item("npc", 3);
        dynamicDrops.add(playerDrop);
        dynamicDrops.add(npcDrop);
        require(dynamicDrops.size() == 2, "same-tile dynamic drops were collapsed");
        require(playerDrop.amount == 7 && npcDrop.amount == 3, "dynamic stack amounts changed");
        dynamicDrops.remove(playerDrop);
        require(dynamicDrops.size() == 1 && dynamicDrops.get(0) == npcDrop,
            "player drop despawn affected NPC drop");
        dynamicDrops.remove(npcDrop);
        require(dynamicDrops.isEmpty(), "dynamic drops did not despawn independently");
    }
}
"""


def run_registry_fixture() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")
    with tempfile.TemporaryDirectory(prefix="authored-ground-item-") as raw_temp:
        temp = Path(raw_temp)
        harness = temp / "AuthoredGroundItemRegistryHarness.java"
        harness.write_text(HARNESS, encoding="utf-8")
        compiled = subprocess.run(
            [javac, "-d", str(temp), str(REGISTRY), str(harness)],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        require(compiled.returncode == 0, f"registry fixture failed to compile:\n{compiled.stderr}")
        executed = subprocess.run(
            [java, "-cp", str(temp), "AuthoredGroundItemRegistryHarness"],
            cwd=ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        require(executed.returncode == 0, f"registry fixture failed:\n{executed.stderr}")


def main() -> None:
    run_registry_fixture()

    require_text(POPULATOR, "getWorld().registerAuthoredGroundItem(i);",
                 "idempotent authored world population")
    require_text(ADMINS, "player.getWorld().registerAuthoredGroundItem(item);",
                 "idempotent administrator-authored spawn registration")
    require_text(GROUND_ITEM, "getWorld().removeAuthoredGroundItem(this)",
                 "identity-checked authored pickup removal")
    require_text(GROUND_ITEM, "getWorld().registerAuthoredGroundItem(loc, authoredGeneration);",
                 "generation-checked authored respawn")
    require_text(WORLD, "authoredGroundItems.reset();", "restart generation invalidation")
    require_text(WORLD, "if (i.getLoc() == null)", "separate dynamic-drop lifecycle")
    require_text(WORLD, "unregisterItem(i);", "per-instance dynamic-drop despawn")

    print("PASS: authored ground-item lifecycle remains singular while dynamic drops stay independent")


if __name__ == "__main__":
    main()
