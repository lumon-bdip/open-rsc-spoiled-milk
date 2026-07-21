#!/usr/bin/env python3
import shutil
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / "server/src/com/openrsc/server/model/Cache.java"

HARNESS = r"""
import com.openrsc.server.model.Cache;

public final class CacheNumericCoercionHarness {
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireIllegalArgument(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Cache cache = new Cache();

        cache.store("new_integer", 20);
        require(cache.getCacheMap().get("new_integer") instanceof Integer,
            "store(String, int) must preserve Integer type");
        require(cache.getInt("new_integer") == 20,
            "Integer cache value must remain readable as int");
        require(cache.getLong("new_integer") == 20L,
            "Integer cache value must be safely readable as long");

        cache.store("legacy_long_bonus", 20L);
        require(cache.getInt("legacy_long_bonus") == 20,
            "Persisted Long brew bonuses must remain readable as int");

        cache.store("command_string", "20");
        require(cache.getInt("command_string") == 20,
            "Numeric command strings must remain readable as int");
        require(cache.getLong("command_string") == 20L,
            "Numeric command strings must be readable as long");

        cache.store("outside_integer_range", (long) Integer.MAX_VALUE + 1L);
        requireIllegalArgument(
            () -> cache.getInt("outside_integer_range"),
            "Out-of-range Long values must not be truncated to int");

        cache.store("wrong_type", Boolean.TRUE);
        requireIllegalArgument(
            () -> cache.getInt("wrong_type"),
            "Non-numeric values must still be rejected by getInt");
        requireIllegalArgument(
            () -> cache.getLong("wrong_type"),
            "Non-numeric values must still be rejected by getLong");

        System.out.println("PASS: cache numeric coercion preserves XP brew values");
    }
}
"""


def main() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    if javac is None or java is None:
        raise SystemExit("FAIL: Java compiler/runtime are required")

    with tempfile.TemporaryDirectory(prefix="cache-numeric-coercion-") as directory:
        temp = Path(directory)
        harness = temp / "CacheNumericCoercionHarness.java"
        harness.write_text(HARNESS, encoding="utf-8")

        compile_result = subprocess.run(
            [javac, "-source", "8", "-target", "8", "-d", str(temp), str(CACHE), str(harness)],
            capture_output=True,
            text=True,
        )
        if compile_result.returncode != 0:
            raise SystemExit(
                "FAIL: cache numeric coercion fixture did not compile\n"
                + compile_result.stdout
                + compile_result.stderr
            )

        run_result = subprocess.run(
            [java, "-cp", str(temp), "CacheNumericCoercionHarness"],
            capture_output=True,
            text=True,
        )
        if run_result.returncode != 0:
            raise SystemExit(
                "FAIL: cache numeric coercion fixture failed\n"
                + run_result.stdout
                + run_result.stderr
            )

        print(run_result.stdout.strip())


if __name__ == "__main__":
    main()
