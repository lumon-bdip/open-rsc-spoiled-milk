#!/usr/bin/env python3
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVER_MODE = ROOT / "server/src/com/openrsc/server/content/worldedit/WorldBuilderMode.java"
DATABASE_TYPE = ROOT / "server/src/com/openrsc/server/database/DatabaseType.java"
CLIENT_PROFILE = ROOT / "Client_Base/src/orsc/WorldBuilderClientProfile.java"


class WorldBuilderRuntimeTest(unittest.TestCase):
    def compile_and_run(self, sources, harness_name, harness_source, *args):
        with tempfile.TemporaryDirectory(prefix="world-builder-runtime-") as temp:
            temp_path = Path(temp)
            harness = temp_path / (harness_name.replace(".", "/") + ".java")
            harness.parent.mkdir(parents=True, exist_ok=True)
            harness.write_text(textwrap.dedent(harness_source), encoding="utf-8")
            classes = temp_path / "classes"
            classes.mkdir()
            subprocess.run(
                [
                    "javac",
                    "-source",
                    "8",
                    "-target",
                    "8",
                    "-d",
                    str(classes),
                    *map(str, sources),
                    str(harness),
                ],
                cwd=ROOT,
                check=True,
                capture_output=True,
                text=True,
            )
            return subprocess.run(
                ["java", "-cp", str(classes), harness_name, *map(str, args)],
                cwd=ROOT,
                check=True,
                capture_output=True,
                text=True,
            ).stdout

    def test_server_mode_is_opt_in_and_fail_closed(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-server-stub-") as temp:
            stub = Path(temp) / "com/openrsc/server/ServerConfiguration.java"
            stub.parent.mkdir(parents=True)
            stub.write_text(
                textwrap.dedent(
                    """
                    package com.openrsc.server;
                    import com.openrsc.server.database.DatabaseType;
                    public class ServerConfiguration {
                        public boolean WORLD_BUILDER_MODE;
                        public String SERVER_BIND_ADDRESS;
                        public DatabaseType DB_TYPE;
                        public String DB_NAME;
                        public String DB_TABLE_PREFIX;
                        public int MAX_PLAYERS;
                        public boolean WANT_PACKET_REGISTER;
                        public boolean ALLOW_IN_GAME_WORLD_EDITOR;
                        public boolean WANT_CUSTOM_LANDSCAPE;
                        public boolean WANT_MYWORLD;
                    }
                    """
                ),
                encoding="utf-8",
            )
            output = self.compile_and_run(
                [stub, DATABASE_TYPE, SERVER_MODE],
                "WorldBuilderModeHarness",
                """
                import com.openrsc.server.ServerConfiguration;
                import com.openrsc.server.content.worldedit.WorldBuilderMode;
                import com.openrsc.server.database.DatabaseType;

                public final class WorldBuilderModeHarness {
                    private static ServerConfiguration config(boolean enabled, String host) {
                        ServerConfiguration c = new ServerConfiguration();
                        c.WORLD_BUILDER_MODE = enabled;
                        c.SERVER_BIND_ADDRESS = host;
                        c.DB_TYPE = DatabaseType.SQLITE;
                        c.DB_NAME = "world_builder";
                        c.DB_TABLE_PREFIX = "";
                        c.MAX_PLAYERS = 1;
                        c.WANT_PACKET_REGISTER = false;
                        c.ALLOW_IN_GAME_WORLD_EDITOR = true;
                        c.WANT_CUSTOM_LANDSCAPE = true;
                        c.WANT_MYWORLD = true;
                        return c;
                    }

                    private static void require(boolean value, String message) {
                        if (!value) throw new AssertionError(message);
                    }

                    public static void main(String[] args) {
                        ServerConfiguration ordinary = config(false, "0.0.0.0");
                        ordinary.DB_TYPE = DatabaseType.MYSQL;
                        ordinary.DB_NAME = "live";
                        WorldBuilderMode.validate(ordinary);

                        WorldBuilderMode.validate(config(true, "127.0.0.1"));
                        require(WorldBuilderMode.isBuilderAccount("builder"), "identity case");
                        require(!WorldBuilderMode.isBuilderAccount("DevDuck"), "identity scope");
                        require(WorldBuilderMode.isLoopbackAddress("::1"), "IPv6 loopback");

                        ServerConfiguration unsafe = config(true, "0.0.0.0");
                        unsafe.DB_NAME = "myworld_dev";
                        unsafe.MAX_PLAYERS = 100;
                        unsafe.WANT_PACKET_REGISTER = true;
                        boolean refused = false;
                        try {
                            WorldBuilderMode.validate(unsafe);
                        } catch (IllegalArgumentException expected) {
                            refused = expected.getMessage().contains("loopback")
                                && expected.getMessage().contains("world_builder")
                                && expected.getMessage().contains("max_players")
                                && expected.getMessage().contains("want_packet_register");
                        }
                        require(refused, "unsafe Builder configuration must be refused");
                        System.out.println("server-mode-ok");
                    }
                }
                """,
            )
            self.assertEqual("server-mode-ok\n", output)

    def test_client_profile_is_explicit_bounded_and_loopback_only(self):
        with tempfile.TemporaryDirectory(prefix="world-builder-client-fixture-") as fixture:
            fixture_path = Path(fixture)
            credential = fixture_path / "builder.credential"
            credential.write_text("Abcdefghijk23456789Z", encoding="ascii")
            invalid = fixture_path / "invalid.credential"
            invalid.write_text("not-valid", encoding="ascii")

            stub = fixture_path / "orsc/Config.java"
            stub.parent.mkdir(parents=True)
            stub.write_text(
                "package orsc; public final class Config { "
                "public static String SERVER_IP = \"unchanged\"; public static int SERVER_PORT = 12; }\n",
                encoding="utf-8",
            )
            output = self.compile_and_run(
                [stub, CLIENT_PROFILE],
                "orsc.WorldBuilderClientProfileHarness",
                """
                package orsc;

                public final class WorldBuilderClientProfileHarness {
                    private static void require(boolean value, String message) {
                        if (!value) throw new AssertionError(message);
                    }

                    private static void expectRefusal(String host, String port, String credential) {
                        System.setProperty(WorldBuilderClientProfile.ENABLED_PROPERTY, "true");
                        System.setProperty(WorldBuilderClientProfile.HOST_PROPERTY, host);
                        System.setProperty(WorldBuilderClientProfile.PORT_PROPERTY, port);
                        System.setProperty(WorldBuilderClientProfile.CREDENTIAL_FILE_PROPERTY, credential);
                        try {
                            WorldBuilderClientProfile.initializeFromSystemProperties();
                            throw new AssertionError("unsafe client profile was accepted");
                        } catch (IllegalArgumentException expected) {
                        }
                    }

                    public static void main(String[] args) {
                        System.clearProperty(WorldBuilderClientProfile.ENABLED_PROPERTY);
                        WorldBuilderClientProfile.initializeFromSystemProperties().applyConnection();
                        require(!WorldBuilderClientProfile.isEnabled(), "profile default");
                        require("unchanged".equals(Config.SERVER_IP) && Config.SERVER_PORT == 12,
                            "disabled profile must not change normal connection");

                        System.setProperty(WorldBuilderClientProfile.ENABLED_PROPERTY, "true");
                        System.setProperty(WorldBuilderClientProfile.HOST_PROPERTY, "127.0.0.1");
                        System.setProperty(WorldBuilderClientProfile.PORT_PROPERTY, "43615");
                        System.setProperty(WorldBuilderClientProfile.CREDENTIAL_FILE_PROPERTY, args[0]);
                        WorldBuilderClientProfile profile =
                            WorldBuilderClientProfile.initializeFromSystemProperties();
                        profile.applyConnection();
                        require(profile.isEnabled(), "profile enabled");
                        require("Builder".equals(profile.username()), "fixed identity");
                        require("Abcdefghijk23456789Z".equals(profile.credential()), "credential");
                        require("127.0.0.1".equals(Config.SERVER_IP) && Config.SERVER_PORT == 43615,
                            "explicit connection");

                        expectRefusal("0.0.0.0", "43615", args[0]);
                        expectRefusal("127.0.0.1", "0", args[0]);
                        expectRefusal("127.0.0.1", "43615", args[1]);
                        System.out.println("client-profile-ok");
                    }
                }
                """,
                credential,
                invalid,
            )
            self.assertEqual("client-profile-ok\n", output)

    def test_runtime_wiring_preserves_authoritative_paths(self):
        config = (ROOT / "server/src/com/openrsc/server/ServerConfiguration.java").read_text()
        server = (ROOT / "server/src/com/openrsc/server/Server.java").read_text()
        login = (ROOT / "server/src/com/openrsc/server/login/LoginRequest.java").read_text()
        shared_login = (ROOT / "server/plugins/com/openrsc/server/plugins/shared/PlayerLogin.java").read_text()
        command = (
            ROOT / "server/plugins/com/openrsc/server/plugins/authentic/commands/Development.java"
        ).read_text()
        client = (ROOT / "Client_Base/src/orsc/mudclient.java").read_text()

        self.assertIn('tryReadBool("world_builder_mode").orElse(false)', config)
        self.assertLess(server.index("WorldBuilderMode.validate(getConfig())"), server.index("packetFilter ="))
        self.assertIn("WorldBuilderAccountProvisioner.provision(this)", server)
        self.assertIn("WorldBuilderRuntimeControl.start(this)", server)
        self.assertIn("!WorldBuilderMode.isBuilderAccount(username)", login)
        self.assertIn("WorldBuilderPlayerSession.activate(player)", shared_login)
        self.assertIn("WorldEditorAccessService.open(player)", command)
        self.assertIn("player.setCacheInvulnerable(true)", (
            ROOT / "server/src/com/openrsc/server/content/worldedit/WorldBuilderPlayerSession.java"
        ).read_text())
        self.assertIn("isAndroid() || !WorldBuilderClientProfile.isEnabled()", client)
        self.assertIn("this.autoLoginTimeout = 3", client)


if __name__ == "__main__":
    unittest.main()
