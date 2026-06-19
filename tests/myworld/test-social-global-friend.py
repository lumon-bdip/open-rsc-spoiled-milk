#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MYWORLD_CONFIG = ROOT / "server/myworld.conf"
HOSTED_CONFIG = ROOT / "server/myworld-host.conf"
SOCIAL = ROOT / "server/src/com/openrsc/server/model/entity/player/Social.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
ACTION_SENDER = ROOT / "server/src/com/openrsc/server/net/rsc/ActionSender.java"
CUSTOM_GENERATOR = ROOT / "server/src/com/openrsc/server/net/rsc/generators/impl/PayloadCustomGenerator.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL: {message}")


def require(text: str, needle: str, description: str) -> None:
    if needle not in text:
        fail(f"missing {description}: {needle}")


def main() -> None:
    for config_path in (MYWORLD_CONFIG, HOSTED_CONFIG):
        config = config_path.read_text(encoding="utf-8")
        require(config, "want_global_chat: false", f"{config_path.name} direct global chat disabled")
        require(config, "want_global_friend: true", f"{config_path.name} Global$ friend enabled")
        require(config, "global_message_reading_total_level_req: 0", f"{config_path.name} read requirement")
        require(config, "global_message_total_level_req: 0", f"{config_path.name} send requirement")

    social = SOCIAL.read_text(encoding="utf-8")
    require(social, "friendList.put(Long.MIN_VALUE, 0);", "synthetic Global$ friend hash")
    require(social, 'friendListNames.put(Long.MIN_VALUE, "Global$");', "synthetic Global$ display name")
    require(social, 'friendListFormerNames.put(Long.MIN_VALUE, "");', "synthetic Global$ former-name sentinel")
    require(social, "ActionSender.sendFriendUpdate(player, Long.MIN_VALUE, \"Global$\", \"\");", "Global$ add/update packet")

    player = PLAYER.read_text(encoding="utf-8")
    require(player, "public Boolean getBlockGlobalFriend()", "Global$ visibility setting")
    require(player, "return getCache().getBoolean(\"setting_block_global_friend\");", "stored Global$ opt-out")
    require(player, "return false;", "Global$ defaults visible when eligible")

    action_sender = ACTION_SENDER.read_text(encoding="utf-8")
    require(action_sender, "if (player.getClientVersion() <= 204)", "legacy friend-list packet split")
    require(action_sender, "for (Entry<Long, Integer> entry : player.getSocial().getFriendListEntry())", "custom friend replay iterates actual entries")
    require(action_sender, "if (usernameHash == Long.MIN_VALUE && player.getConfig().WANT_GLOBAL_FRIEND)", "custom Global$ replay branch")
    require(action_sender, "if (!player.getBlockGlobalFriend()) {\n\t\t\t\t\t\tsendFriendUpdate(player, usernameHash, \"Global$\", \"\");", "custom Global$ defaults into friends list")
    require(action_sender, "username = DataConversions.hashToUsername(usernameHash);", "custom replay fallback display name")
    require(action_sender, "sendFriendUpdate(player, usernameHash, username, formerName == null ? \"\" : formerName);", "custom replay null-safe former name")

    custom_generator = CUSTOM_GENERATOR.read_text(encoding="utf-8")
    require(custom_generator, "put(OpcodeOut.SEND_FRIEND_UPDATE, 149);", "custom friend update opcode")
    require(custom_generator, "builder.writeString(fr.name);", "custom friend update sends display name")
    require(custom_generator, "builder.writeString(fr.formerName);", "custom friend update sends former name")
    require(custom_generator, "builder.writeByte((byte) fr.onlineStatus);", "custom friend update sends online status")

    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    require(packet_handler, "else if (opcode == 149) sendConnectionMessage();", "client friend update dispatch")
    require(packet_handler, "SocialLists.friendList[SocialLists.friendListCount] = currentName;", "client appends friend update")
    require(packet_handler, "mc.sortOnlineFriendsList();", "client sorts replayed friends")

    mudclient = MUDCLIENT.read_text(encoding="utf-8")
    require(mudclient, "for (index = 0; index < SocialLists.friendListCount; ++index)", "friends tab renders social list")
    require(mudclient, "C_BLOCK_GLOBAL_FRIEND = !C_BLOCK_GLOBAL_FRIEND;", "player can still opt out of Global$")

    print("PASS: social sync preserves database friends and default Global$ friend")


if __name__ == "__main__":
    main()
