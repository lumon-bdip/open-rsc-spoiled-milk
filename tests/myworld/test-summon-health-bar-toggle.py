#!/usr/bin/env python3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLIENT_CONFIG = ROOT / "Client_Base/src/orsc/Config.java"
CLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"
PACKET_HANDLER = ROOT / "Client_Base/src/orsc/PacketHandler.java"
ACTION_SENDER = ROOT / "server/src/com/openrsc/server/net/rsc/ActionSender.java"
GAME_SETTING_HANDLER = ROOT / "server/src/com/openrsc/server/net/rsc/handlers/GameSettingHandler.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    config = CLIENT_CONFIG.read_text(encoding="utf-8")
    client = CLIENT.read_text(encoding="utf-8")
    packet_handler = PACKET_HANDLER.read_text(encoding="utf-8")
    action_sender = ACTION_SENDER.read_text(encoding="utf-8")
    game_setting_handler = GAME_SETTING_HANDLER.read_text(encoding="utf-8")
    player = PLAYER.read_text(encoding="utf-8")

    require("public static boolean C_SUMMON_HEALTH_BARS = true;" in config,
            "Summon health bars should default on")
    require('"@whi@Summon Health Bars - " + (C_SUMMON_HEALTH_BARS ? "@gre@On" : "@red@Off")' in client,
            "Options menu should expose the summon health bar toggle")
    require("bufferBits.putByte(50);" in client and "bufferBits.putByte(C_SUMMON_HEALTH_BARS ? 1 : 0);" in client,
            "Client should save the summon health bar toggle at custom setting index 50")
    require("public void setSummonHealthBars(boolean b)" in client,
            "Client should receive the summon health bar setting from the server")
    require("boolean showSummonHealthBar = C_SUMMON_HEALTH_BARS || !npc.suppressAttackOption;" in client,
            "Renderer should hide only summon health bars when the toggle is off")
    require("showSummonHealthBar && (npc.combatTimeout > 0 || npc.suppressAttackOption) && npc.healthMax > 0" in client,
            "Renderer should apply the summon health bar toggle to the health bar draw path")
    require("hasVisibleHitSplats(npc)" in client,
            "Summon health bar toggle should not remove normal hit splat drawing")

    require("mc.setSummonHealthBars(packetsIncoming.packetEnd < length ? packetsIncoming.getUnsignedByte() == 1 : true); // 51" in packet_handler,
            "Client should read the saved summon health bar setting with a default-on fallback")
    require("customOptions.add(player.getShowSummonHealthBars() ? 1 : 0);" in action_sender,
            "Server should send the saved summon health bar setting")
    require('player.getCache().store("setting_summon_health_bars", value == 1);' in game_setting_handler,
            "Server should persist the summon health bar setting")
    require("public boolean getShowSummonHealthBars()" in player
            and 'getCache().hasKey("setting_summon_health_bars")' in player
            and "return true;" in player,
            "Player should expose a default-on persisted summon health bar setting")

    print("PASS: summon health bar options toggle is wired")


if __name__ == "__main__":
	main()
