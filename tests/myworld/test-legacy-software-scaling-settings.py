#!/usr/bin/env python3
"""Exercise active legacy software-scaling state and persistence compatibility."""

from __future__ import annotations

import subprocess
import tempfile
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_JAR = ROOT / "Client_Base/Open_RSC_Client.jar"
OWNER = ROOT / "Client_Base/src/orsc/LegacySoftwareScalingSettings.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
OPEN_RSC = ROOT / "PC_Client/src/orsc/OpenRSC.java"
APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
SCALED_WINDOW = ROOT / "PC_Client/src/orsc/ScaledWindow.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"


FIXTURE = r"""
package orsc;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

public final class LegacySoftwareScalingSettingsFixture {
	private static final class MemoryStore
		implements LegacySoftwareScalingSettings.SettingsStore {
		final Properties properties = new Properties();
		int loads;
		int saves;

		public Properties load() {
			loads++;
			return properties;
		}

		public void save(Properties saved) {
			saves++;
		}
	}

	private static final class FileStore
		implements LegacySoftwareScalingSettings.SettingsStore {
		private final Path path;

		FileStore(Path path) {
			this.path = path;
		}

		public Properties load() {
			return read(path);
		}

		public void save(Properties properties) {
			write(path, properties);
		}
	}

	private LegacySoftwareScalingSettingsFixture() {
	}

	public static void main(String[] args) {
		String scenario = args[0];
		if ("sequences".equals(scenario)) {
			sequencesAndOrdinals();
		} else if ("bounds".equals(scenario)) {
			boundsAndHeadlessMath();
		} else if ("load".equals(scenario)) {
			loadValidationAndPrecedence();
		} else if ("legacy".equals(scenario)) {
			legacyMigration();
		} else if ("screen-clamp".equals(scenario)) {
			screenClamp();
		} else if ("missing".equals(scenario)) {
			missingSettings();
		} else if ("write-restart".equals(scenario)) {
			writeRestart(Paths.get(args[1]));
		} else if ("read-restart".equals(scenario)) {
			readRestart(Paths.get(args[1]));
		} else {
			throw new AssertionError("unknown scenario " + scenario);
		}
	}

	private static void sequencesAndOrdinals() {
		assertEquals(0, ScaledWindow.ScalingAlgorithm.INTEGER_SCALING.ordinal(),
			"integer ordinal");
		assertEquals(1, ScaledWindow.ScalingAlgorithm.BILINEAR_INTERPOLATION.ordinal(),
			"bilinear ordinal");
		assertEquals(2, ScaledWindow.ScalingAlgorithm.BICUBIC_INTERPOLATION.ordinal(),
			"bicubic ordinal");

		LegacySoftwareScalingSettings.configureAllowedScalars(7);
		assertEquals(Arrays.asList(1.0f, 2.0f),
			LegacySoftwareScalingSettings.getIntegerScalars(), "integer sequence");
		assertEquals(Arrays.asList(1.0f, 1.5f, 2.0f),
			LegacySoftwareScalingSettings.getInterpolationScalars(),
			"interpolation sequence");
		assertUnmodifiable(LegacySoftwareScalingSettings.getIntegerScalars());

		MemoryStore store = new MemoryStore();
		store.properties.setProperty("unrelated", "retained");
		assertEquals(ScaledWindow.ScalingAlgorithm.BILINEAR_INTERPOLATION,
			LegacySoftwareScalingSettings.cycleScalingAlgorithm(store),
			"integer to bilinear");
		assertEquals(ScaledWindow.ScalingAlgorithm.BICUBIC_INTERPOLATION,
			LegacySoftwareScalingSettings.cycleScalingAlgorithm(store),
			"bilinear to bicubic");
		assertEquals(ScaledWindow.ScalingAlgorithm.INTEGER_SCALING,
			LegacySoftwareScalingSettings.cycleScalingAlgorithm(store),
			"bicubic to integer");
		assertEquals(3, store.loads, "cycle load count");
		assertEquals(3, store.saves, "cycle save count");
		assertProperty(store.properties, "unrelated", "retained");

		Properties fractional = settings("2", "1.5", null);
		LegacySoftwareScalingSettings.loadFromClientSettings(fractional);
		LegacySoftwareScalingSettings.applyPendingScalar(
			LegacySoftwareScalingSettings.getPendingRenderingScalar());
		LegacySoftwareScalingSettings.cycleScalingAlgorithm(store);
		assertEquals(ScaledWindow.ScalingAlgorithm.INTEGER_SCALING,
			LegacySoftwareScalingSettings.getScalingAlgorithm(),
			"fractional transition algorithm");
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"fractional transition truncates");
	}

	private static void boundsAndHeadlessMath() {
		LegacySoftwareScalingSettings.configureAllowedScalars(2);
		MemoryStore store = new MemoryStore();
		assertFloat(1.0f, LegacySoftwareScalingSettings.scaleDown(store),
			"integer lower endpoint");
		assertTrue(LegacySoftwareScalingSettings.isLoginRedrawPending(),
			"scale action marks login redraw");
		LegacySoftwareScalingSettings.clearLoginRedrawPending();
		assertTrue(!LegacySoftwareScalingSettings.isLoginRedrawPending(),
			"login redraw clears");

		assertFloat(2.0f, LegacySoftwareScalingSettings.scaleUp(store),
			"integer scale up");
		assertTrue(LegacySoftwareScalingSettings.hasPendingScalarChange(),
			"integer pending transition");
		LegacySoftwareScalingSettings.applyPendingScalar(
			LegacySoftwareScalingSettings.getPendingRenderingScalar());
		assertFloat(2.0f, LegacySoftwareScalingSettings.scaleUp(store),
			"integer upper endpoint");
		LegacySoftwareScalingSettings.scaleDown(store);
		LegacySoftwareScalingSettings.applyPendingScalar(
			LegacySoftwareScalingSettings.getPendingRenderingScalar());

		LegacySoftwareScalingSettings.cycleScalingAlgorithm(store);
		assertFloat(1.5f, LegacySoftwareScalingSettings.scaleUp(store),
			"interpolation first step");
		LegacySoftwareScalingSettings.applyPendingScalar(
			LegacySoftwareScalingSettings.getPendingRenderingScalar());
		assertEquals(768, LegacySoftwareScalingSettings.scaleDimension(512),
			"scaled width");
		assertEquals(519, LegacySoftwareScalingSettings.scaleDimension(346),
			"scaled height");
		assertEquals(50, LegacySoftwareScalingSettings.unscaleCoordinate(75),
			"mouse coordinate mapping");
		assertEquals(BufferedImage.TYPE_3BYTE_BGR, ScaledWindow.getBufferedImageType(),
			"interpolation image type");

		LegacySoftwareScalingSettings.cycleScalingAlgorithm(store);
		assertEquals(BufferedImage.TYPE_3BYTE_BGR, ScaledWindow.getBufferedImageType(),
			"bicubic image type");
		LegacySoftwareScalingSettings.cycleScalingAlgorithm(store);
		assertEquals(BufferedImage.TYPE_INT_RGB, ScaledWindow.getBufferedImageType(),
			"integer image type");
		assertTrue(store.loads == store.saves, "every mutation has one transaction");
	}

	private static void loadValidationAndPrecedence() {
		LegacySoftwareScalingSettings.configureAllowedScalars(2);
		LegacySoftwareScalingSettings.loadFromClientSettings(settings("1", "1.5", "2.0"));
		assertEquals(ScaledWindow.ScalingAlgorithm.BILINEAR_INTERPOLATION,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "loaded type");
		assertFloat(1.5f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"ui_scale precedence");

		Properties malformed = settings("garbage", "garbage", "2.0");
		LegacySoftwareScalingSettings.loadFromClientSettings(malformed);
		assertEquals(ScaledWindow.ScalingAlgorithm.INTEGER_SCALING,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "malformed type fallback");
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"malformed ui_scale does not use legacy value");

		LegacySoftwareScalingSettings.loadFromClientSettings(settings("99", "99", null));
		assertEquals(ScaledWindow.ScalingAlgorithm.INTEGER_SCALING,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "out-of-range type");
		assertFloat(2.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"high scalar clamp");
		LegacySoftwareScalingSettings.loadFromClientSettings(settings("1", "-5", null));
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"low scalar clamp");
		LegacySoftwareScalingSettings.loadFromClientSettings(settings("1", "NaN", null));
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"non-finite scalar fallback");
	}

	private static void legacyMigration() {
		LegacySoftwareScalingSettings.configureAllowedScalars(2);
		Properties legacy = settings("2", null, "1.5");
		LegacySoftwareScalingSettings.loadFromClientSettings(legacy);
		assertEquals(ScaledWindow.ScalingAlgorithm.BICUBIC_INTERPOLATION,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "legacy type");
		assertFloat(1.5f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"legacy scalar fallback");
		LegacySoftwareScalingSettings.saveToClientSettings(legacy);
		assertProperty(legacy, "ui_scale", "1.5");
		assertProperty(legacy, "scaling_scalar", "1.5");
	}

	private static void screenClamp() {
		LegacySoftwareScalingSettings.loadFromClientSettings(settings("2", "2.0", null));
		LegacySoftwareScalingSettings.configureAllowedScalars(1);
		assertEquals(Arrays.asList(1.0f),
			LegacySoftwareScalingSettings.getIntegerScalars(), "small-screen integer list");
		assertEquals(Arrays.asList(1.0f),
			LegacySoftwareScalingSettings.getInterpolationScalars(),
			"small-screen interpolation list");
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"small-screen pending clamp");
	}

	private static void missingSettings() {
		LegacySoftwareScalingSettings.loadFromClientSettings(new Properties());
		assertEquals(ScaledWindow.ScalingAlgorithm.INTEGER_SCALING,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "missing type default");
		assertFloat(1.0f, LegacySoftwareScalingSettings.getRenderingScalar(),
			"missing current default");
		assertFloat(1.0f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"missing pending default");
	}

	private static void writeRestart(Path path) {
		Properties configured = settings("2", "1.5", null);
		LegacySoftwareScalingSettings.loadFromClientSettings(configured);
		Properties initial = new Properties();
		initial.setProperty("restart.unrelated", "retained");
		write(path, initial);
		LegacySoftwareScalingSettings.persist(new FileStore(path));
	}

	private static void readRestart(Path path) {
		Properties saved = read(path);
		LegacySoftwareScalingSettings.loadFromClientSettings(saved);
		assertProperty(saved, "restart.unrelated", "retained");
		assertProperty(saved, "scaling_type", "2");
		assertProperty(saved, "ui_scale", "1.5");
		assertProperty(saved, "scaling_scalar", "1.5");
		assertEquals(ScaledWindow.ScalingAlgorithm.BICUBIC_INTERPOLATION,
			LegacySoftwareScalingSettings.getScalingAlgorithm(), "restart type");
		assertFloat(1.5f, LegacySoftwareScalingSettings.getPendingRenderingScalar(),
			"restart scalar");
	}

	private static Properties settings(String type, String uiScale, String legacyScale) {
		Properties properties = new Properties();
		if (type != null) properties.setProperty("scaling_type", type);
		if (uiScale != null) properties.setProperty("ui_scale", uiScale);
		if (legacyScale != null) properties.setProperty("scaling_scalar", legacyScale);
		return properties;
	}

	private static void assertUnmodifiable(java.util.List<Float> scalars) {
		try {
			scalars.add(3.0f);
			throw new AssertionError("scalar list is mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected immutable owner view.
		}
	}

	private static Properties read(Path path) {
		Properties properties = new Properties();
		if (!path.toFile().isFile()) return properties;
		try (FileInputStream input = new FileInputStream(path.toFile())) {
			properties.load(input);
			return properties;
		} catch (IOException failure) {
			throw new AssertionError("unable to read settings", failure);
		}
	}

	private static void write(Path path, Properties properties) {
		try (FileOutputStream output = new FileOutputStream(path.toFile())) {
			properties.store(output, "fixture");
		} catch (IOException failure) {
			throw new AssertionError("unable to write settings", failure);
		}
	}

	private static void assertProperty(Properties properties, String key, String expected) {
		assertEquals(expected, properties.getProperty(key), "property " + key);
	}

	private static void assertFloat(float expected, float actual, String label) {
		if (Float.compare(expected, actual) != 0) {
			throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
		}
	}

	private static void assertTrue(boolean condition, String label) {
		if (!condition) throw new AssertionError(label);
	}

	private static void assertEquals(Object expected, Object actual, String label) {
		if (expected == null ? actual != null : !expected.equals(actual)) {
			throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
		}
	}
}
"""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def verify_source_ownership() -> None:
    owner = OWNER.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    open_rsc = OPEN_RSC.read_text(encoding="utf-8")
    applet = APPLET.read_text(encoding="utf-8")
    scaled_window = SCALED_WINDOW.read_text(encoding="utf-8")
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")

    require("active software-presenter scaling compatibility state" in owner,
            "active compatibility label is missing")
    for key in ("scaling_type", "ui_scale", "scaling_scalar"):
        require(f'"{key}"' in owner, f"owner is missing persisted key {key}")
    for retired in (
        "public static ScalingAlgorithm scalingType",
        "public static float renderingScalar",
        "public static float newRenderingScalar",
        "public static boolean scalarChangedSinceLogin",
        "public static List<Float> integerScalars",
        "public static List<Float> interpolationScalars",
        "saveScalingSettings(",
        "changeRenderingScalar(",
        "cycleScalingType(",
    ):
        require(retired not in mudclient, f"mudclient still owns {retired}")

    require("LegacySoftwareScalingSettings.loadFromClientSettings(props);" in open_rsc,
            "desktop startup does not load scaling owner")
    require("oldRenderingScalar" not in applet,
            "ORSCApplet still owns pending/current transition state")
    require("LegacySoftwareScalingSettings.applyPendingScalar(scalar);" in applet,
            "ORSCApplet does not consume pending scalar")
    require("LegacySoftwareScalingSettings.configureAllowedScalars(maxRenderingScalar);"
            in scaled_window, "ScaledWindow still owns scalar sequences")
    require("LegacySoftwareScalingSettings.unscaleCoordinate(e.getX())" in scaled_window,
            "mouse mapping does not query scaling owner")
    require("LegacySoftwareScalingSettings.clearLoginRedrawPending();" in packet_handler,
            "login redraw suppression does not query scaling owner")


def run_fixture(classes: Path, scenario: str, settings_path: Path | None = None) -> None:
    command = [
        "java",
        "-Djava.awt.headless=true",
        "-cp",
        f"{classes}:{CLIENT_JAR}",
        "orsc.LegacySoftwareScalingSettingsFixture",
        scenario,
    ]
    if settings_path is not None:
        command.append(str(settings_path))
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> None:
    require(CLIENT_JAR.is_file(), f"build the client first: {CLIENT_JAR}")
    verify_source_ownership()
    with tempfile.TemporaryDirectory(prefix="legacy-software-scaling-") as directory:
        temp = Path(directory)
        source = temp / "orsc/LegacySoftwareScalingSettingsFixture.java"
        source.parent.mkdir(parents=True)
        source.write_text(textwrap.dedent(FIXTURE), encoding="utf-8")
        subprocess.run(
            [
                "javac",
                "-source",
                "1.8",
                "-target",
                "1.8",
                "-cp",
                str(CLIENT_JAR),
                "-d",
                str(temp),
                str(source),
            ],
            cwd=ROOT,
            check=True,
        )
        for scenario in ("sequences", "bounds", "load", "legacy", "screen-clamp", "missing"):
            run_fixture(temp, scenario)
        restart = temp / "restart.conf"
        run_fixture(temp, "write-restart", restart)
        run_fixture(temp, "read-restart", restart)

    print("PASS: active legacy software scaling state, compatibility, and math are stable")


if __name__ == "__main__":
    main()
