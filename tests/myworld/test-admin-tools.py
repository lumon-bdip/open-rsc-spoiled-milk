#!/usr/bin/env python3
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PLAYER_LOGIN = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "shared" / "PlayerLogin.java"
MODERATOR_COMMANDS = ROOT / "server" / "plugins" / "com" / "openrsc" / "server" / "plugins" / "authentic" / "commands" / "Moderator.java"
MYWORLD_CONFIG = ROOT / "server" / "myworld.conf"
HOSTED_CONFIG = ROOT / "server" / "myworld-host.conf"


def fail(message: str) -> None:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(text: str, snippet: str, description: str) -> None:
	if snippet not in text:
		fail(f"Missing {description}: {snippet!r}")


def main() -> None:
	player_login = PLAYER_LOGIN.read_text(encoding="utf-8")
	require(player_login, '"devduck".equals(staffName)', "devduck staff group guard")
	require(player_login, "player.setGroupID(Group.OWNER)", "devduck owner group assignment")
	require(player_login, '"anactualduck".equals(staffName)', "anactualduck staff group guard")
	require(player_login, "player.setGroupID(Group.MOD)", "anactualduck moderator group assignment")
	require(player_login, "public boolean blockPlayerLogin(Player player) {\n\t\treturn true;", "staff login trigger activation")

	moderator = MODERATOR_COMMANDS.read_text(encoding="utf-8")
	require(moderator, 'command.equalsIgnoreCase("s")', "::s system broadcast alias")
	require(moderator, "sendSystemBroadcast(player, command, args)", "::s system broadcast handler")
	require(moderator, '"@yel@System: @whi@"', "system broadcast display prefix")
	require(moderator, "ActionSender.sendMessage(playerToUpdate, null, MessageType.QUEST", "system broadcast to all players")

	for config_path in (MYWORLD_CONFIG, HOSTED_CONFIG):
		config = config_path.read_text(encoding="utf-8")
		require(config, "want_global_rules_agreement: false", f"{config_path.name} global chat rules opt-out")
		require(config, "global_message_reading_total_level_req: 0", f"{config_path.name} global read level requirement")
		require(config, "global_message_total_level_req: 0", f"{config_path.name} global send level requirement")

	print("PASS: MyWorld admin tools and global chat access validated")


if __name__ == "__main__":
	main()
