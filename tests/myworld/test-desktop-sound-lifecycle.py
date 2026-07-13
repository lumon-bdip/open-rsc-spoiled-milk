#!/usr/bin/env python3

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_PORT = ROOT / "Client_Base/src/orsc/multiclient/ClientPort.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
OPEN_RSC = ROOT / "PC_Client/src/orsc/OpenRSC.java"
ORSC_APPLET = ROOT / "PC_Client/src/orsc/ORSCApplet.java"
SOUND_PLAYER = ROOT / "PC_Client/src/orsc/soundPlayer.java"
LIFECYCLE = ROOT / "PC_Client/src/orsc/DesktopSoundLifecycle.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def section(source: str, start: str, end: str) -> str:
    start_index = source.index(start)
    return source[start_index:source.index(end, start_index)]


def verify_transition_paths() -> None:
    client_port = CLIENT_PORT.read_text(encoding="utf-8")
    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    open_rsc = OPEN_RSC.read_text(encoding="utf-8")
    applet = ORSC_APPLET.read_text(encoding="utf-8")
    sound_player = SOUND_PLAYER.read_text(encoding="utf-8")

    require("Cleanup must be idempotent" in client_port, "ClientPort cleanup contract is undocumented")
    require("must not\n\t * throw" in client_port, "Unsupported compatibility playback must be non-throwing")
    require("UnsupportedOperationException" not in open_rsc, "OpenRSC sound hooks still throw")
    require("UnsupportedOperationException" not in applet, "ORSCApplet sound hooks still throw")
    require(open_rsc.count("soundPlayer.stopAllSounds();") == 1, "OpenRSC must delegate desktop cleanup")
    require(applet.count("soundPlayer.stopAllSounds();") == 1, "ORSCApplet must delegate desktop cleanup")

    normal_logout = section(
        mudclient,
        "public final void closeConnection(boolean sendPacket)",
        "private void stopSoundPlayback()",
    )
    require("this.stopSoundPlayback();" in normal_logout, "Normal/forced logout bypasses sound cleanup")
    require(
        normal_logout.index("this.stopSoundPlayback();") < normal_logout.index('this.setUsername("");'),
        "Sound cleanup must happen before returning to login",
    )
    reconnect = section(mudclient, "private void lostConnection(int var1)", "private void putStringPair")
    require("this.stopSoundPlayback();" in reconnect, "Reconnect bypasses sound cleanup")
    guarded_cleanup = section(mudclient, "private void stopSoundPlayback()", "private void closeClientStreamOnly()")
    require("clientPort == null" in guarded_cleanup, "Cleanup is not safe without an initialized client port")
    require("catch (RuntimeException e)" in guarded_cleanup, "Compatibility cleanup failures are not contained")
    require("ClientRuntimeLogger.logThrowable" in guarded_cleanup, "Compatibility cleanup failures are swallowed")

    require("mc.closeConnection(true);" in packet_handler, "Normal/server logout route is missing")
    require("mc.closeConnection(false);" in packet_handler, "Forced disconnect route is missing")
    require("public void windowClosing(WindowEvent event)" in open_rsc, "Desktop frame shutdown hook is missing")
    window_close = section(open_rsc, "public void windowClosing(WindowEvent event)", "\n\t\t\t\t}")
    require("applet.stopSoundPlayer();" in window_close, "Desktop window close bypasses sound cleanup")
    applet_stop = section(applet, "public final void stop()", "public final void update(Graphics")
    require("stopSoundPlayer();" in applet_stop, "Applet/compatibility shutdown bypasses sound cleanup")

    require("LIFECYCLE.register(clip);" in sound_player, "Started clips are not lifecycle tracked")
    require("LIFECYCLE.releaseAndClose(activeClip);" in sound_player, "Naturally stopped clips are not released")
    require("try (AudioInputStream audioInput" in sound_player, "Decoded audio input stream is not closed")
    require("public static void stopAllSounds()" in sound_player, "Desktop cleanup entry point is missing")


def verify_runtime_cleanup() -> None:
    javac = shutil.which("javac")
    java = shutil.which("java")
    require(javac is not None and java is not None, "Java compiler/runtime are required")
    fixture = r'''
package orsc;

import javax.sound.sampled.Clip;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class DesktopSoundLifecycleFixture {
    private static final class ClipCalls implements InvocationHandler {
        int stops;
        int closes;
        final boolean throwOnCleanup;

        ClipCalls(boolean throwOnCleanup) {
            this.throwOnCleanup = throwOnCleanup;
        }

        Clip clip() {
            return (Clip) Proxy.newProxyInstance(
                Clip.class.getClassLoader(), new Class<?>[] {Clip.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("stop")) {
                stops++;
                if (throwOnCleanup) throw new IllegalStateException("synthetic stop failure");
            } else if (method.getName().equals("close")) {
                closes++;
                if (throwOnCleanup) throw new IllegalStateException("synthetic close failure");
            }
            Class<?> type = method.getReturnType();
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0F;
            if (type == double.class) return 0.0D;
            if (type == char.class) return (char) 0;
            return null;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        DesktopSoundLifecycle lifecycle = new DesktopSoundLifecycle();
        lifecycle.stopAll();
        lifecycle.stopAll();

        ClipCalls first = new ClipCalls(false);
        ClipCalls second = new ClipCalls(false);
        lifecycle.register(first.clip());
        lifecycle.register(second.clip());
        lifecycle.stopAll();
        check(first.stops == 1 && first.closes == 1, "first clip not stopped and closed once");
        check(second.stops == 1 && second.closes == 1, "second clip not stopped and closed once");
        lifecycle.stopAll();
        check(first.stops == 1 && first.closes == 1, "repeated cleanup touched first clip");
        check(second.stops == 1 && second.closes == 1, "repeated cleanup touched second clip");

        ClipCalls released = new ClipCalls(false);
        Clip releasedClip = released.clip();
        lifecycle.register(releasedClip);
        lifecycle.release(releasedClip);
        lifecycle.stopAll();
        check(released.stops == 0 && released.closes == 0, "released clip remained tracked");

        ClipCalls callbackFailure = new ClipCalls(true);
		Clip callbackClip = callbackFailure.clip();
		lifecycle.register(callbackClip);
		lifecycle.releaseAndClose(callbackClip);
		check(callbackFailure.closes == 1, "callback cleanup did not attempt close");
		lifecycle.stopAll();
		check(callbackFailure.stops == 0 && callbackFailure.closes == 1,
			"callback-released clip remained tracked");

        ClipCalls broken = new ClipCalls(true);
        ClipCalls survivor = new ClipCalls(false);
        lifecycle.register(broken.clip());
        lifecycle.register(survivor.clip());
        lifecycle.stopAll();
        check(broken.stops == 1 && broken.closes == 1, "failed clip did not attempt full cleanup");
        check(survivor.stops == 1 && survivor.closes == 1, "one failed clip blocked later cleanup");
    }
}
'''
    with tempfile.TemporaryDirectory(prefix="desktop-sound-lifecycle-") as directory:
        temp = Path(directory)
        fixture_path = temp / "DesktopSoundLifecycleFixture.java"
        fixture_path.write_text(fixture, encoding="utf-8")
        compiled = subprocess.run(
            [javac, "-d", str(temp), str(LIFECYCLE), str(fixture_path)],
            capture_output=True,
            text=True,
        )
        require(compiled.returncode == 0, f"Lifecycle fixture failed to compile:\n{compiled.stderr}")
        executed = subprocess.run(
            [java, "-cp", str(temp), "orsc.DesktopSoundLifecycleFixture"],
            capture_output=True,
            text=True,
        )
        require(executed.returncode == 0, f"Lifecycle fixture failed:\n{executed.stderr}")


def main() -> int:
    verify_transition_paths()
    verify_runtime_cleanup()
    print("PASS: desktop sound cleanup is tracked, idempotent, non-throwing, and wired to lifecycle transitions")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, FileNotFoundError, ValueError) as error:
        print(f"FAIL: {error}")
        raise SystemExit(1)
