package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.database.DatabaseLookupResult;
import com.openrsc.server.database.GameDatabase;
import com.openrsc.server.database.struct.PlayerFriend;
import com.openrsc.server.event.DelayedEvent;
import com.openrsc.server.model.GlobalMessage;
import com.openrsc.server.model.PrivateMessage;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.player.PlayerSettings;
import com.openrsc.server.model.snapshot.Chatlog;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.net.rsc.PayloadProcessor;
import com.openrsc.server.net.rsc.enums.OpcodeIn;
import com.openrsc.server.net.rsc.struct.incoming.FriendStruct;
import com.openrsc.server.util.MessageFilter;
import com.openrsc.server.util.rsc.DataConversions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FriendHandler implements PayloadProcessor<FriendStruct, OpcodeIn> {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int MAX_FRIENDS = 100;
	private static final int MEMBERS_MAX_FRIENDS = 200;
	private static final int CUSTOM_MAX_FRIENDS = 1000;
	private static final int CUSTOM_MAX_IGNORES = 1000;

	private int actualFriendListLimit(Player player) {
		if (player.isUsingCustomClient()) {
			return CUSTOM_MAX_FRIENDS;
		}
		int clientLimit = player.getClientLimitations().maxFriends;
		int freeOrMembersLimit = player.getConfig().MEMBER_WORLD ? MEMBERS_MAX_FRIENDS : MAX_FRIENDS;
		return Math.min(clientLimit, freeOrMembersLimit);
	}

	private int actualIgnoreListLimit(Player player) {
		if (player.isUsingCustomClient()) {
			return CUSTOM_MAX_IGNORES;
		}
		return player.getClientLimitations().maxIgnore;
	}

	public void process(FriendStruct payload, Player player) throws Exception {
		String friendName = payload.player;
		long friendHash = DataConversions.usernameToHash(friendName);

		int maxFriends = actualFriendListLimit(player);
		int maxIgnore = actualIgnoreListLimit(player);

		boolean friendIsGlobal = (friendName.equalsIgnoreCase("Global$") ||
			(friendName.equalsIgnoreCase("Global") && !player.getConfig().CHAR_NAME_CAN_EQUAL_GLOBAL));


		Player affectedPlayer = player.getWorld().getPlayer(friendHash);

		switch (payload.getOpcode()) {
			case SOCIAL_ADD_FRIEND: {
				if (friendName.equalsIgnoreCase("")) return;

				if (player.getSocial().friendCount() >= maxFriends) {
					player.message("Friend list is full");
					ActionSender.sendFriendList(player);
					return;
				}

				if (friendIsGlobal && (player.getConfig().WANT_GLOBAL_CHAT || player.getConfig().WANT_GLOBAL_FRIEND)) {
					if (player.getTotalLevel() < player.getConfig().GLOBAL_MESSAGE_READING_TOTAL_LEVEL_REQ && !player.isPlayerMod()) {
						player.message("You need at least " + player.getConfig().GLOBAL_MESSAGE_READING_TOTAL_LEVEL_REQ + " total level to read Global chat. Your total level is " + player.getTotalLevel() + ".");
					} else {
						player.getSocial().addGlobalFriend(player, false);
					}
					return;
				}

				if (friendHash <= 0L) {
					return;
				}
				final DatabaseLookupResult<PlayerFriend> friendLookup = lookupSocialName(player, friendName);
				if (friendLookup.isNotFound()) {
					player.message("Unable to add friend - unknown player.");
					ActionSender.sendFriendList(player);
					return;
				}
				if (friendLookup.isFailure()) {
					logLookupFailure(player, "friend-list add", friendLookup);
					player.message("Unable to add friend right now. Please try again later.");
					ActionSender.sendFriendList(player);
					return;
				}
				final PlayerFriend friendProperUsername = friendLookup.getValue();

				player.getSocial().addFriend(friendHash, 0, friendProperUsername.playerName, friendProperUsername.formerName);
				ActionSender.sendFriendUpdate(player, friendHash, friendProperUsername.playerName, friendProperUsername.formerName);
				if (affectedPlayer != null && affectedPlayer.loggedIn()) {
					boolean blockAll = affectedPlayer.getSettings().getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES, affectedPlayer.isUsingCustomClient())
						== PlayerSettings.BlockingMode.All.id();
					boolean blockNone = affectedPlayer.getSettings().getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES, affectedPlayer.isUsingCustomClient())
						== PlayerSettings.BlockingMode.None.id();
					if (!blockAll && affectedPlayer.getSocial().isFriendsWith(player.getUsernameHash())) {
						ActionSender.sendFriendUpdate(affectedPlayer, player.getUsernameHash(), player.getUsername(), player.getFormerName());
						ActionSender.sendFriendUpdate(player, friendHash, friendProperUsername.playerName, friendProperUsername.formerName);
					} else if (blockNone && !affectedPlayer.getSocial().isFriendsWith(player.getUsernameHash())) {
						ActionSender.sendFriendUpdate(player, friendHash, friendProperUsername.playerName, friendProperUsername.formerName);
					}
				}
				break;
			}
			case SOCIAL_REMOVE_FRIEND: {
				if (friendIsGlobal) {
					player.getSocial().removeGlobalFriend(player);
					return;
				}

				player.getSocial().removeFriend(friendHash);
				if (affectedPlayer != null && affectedPlayer.loggedIn()) {
					boolean blockAll = player.getSettings().getPrivacySetting(PlayerSettings.PRIVACY_BLOCK_PRIVATE_MESSAGES, player.isUsingCustomClient())
						== PlayerSettings.BlockingMode.All.id();
					if (!blockAll && affectedPlayer.getSocial().isFriendsWith(player.getUsernameHash())) {
						String exFriendProperUsername = affectedPlayer.getSocial().getFriendListNames().get(player.getUsernameHash());
						String exFriendFormerUsername = affectedPlayer.getSocial().getFriendListFormerNames().get(player.getUsernameHash());
						ActionSender.sendFriendUpdate(affectedPlayer, player.getUsernameHash(), exFriendProperUsername, exFriendFormerUsername);
					}
				}
				break;
			}
			case SOCIAL_ADD_IGNORE: {
				if (friendName.equalsIgnoreCase("")) return;
				if (player.getSocial().ignoreCount() >= maxIgnore) {
					player.message("Ignore list full");
					ActionSender.sendIgnoreList(player);
					return;
				}

				if (friendHash <= 0L) {
					return;
				}
				final DatabaseLookupResult<PlayerFriend> ignoreLookup = lookupSocialName(player, friendName);
				if (ignoreLookup.isNotFound()) {
					player.message("Unable to add name - unknown player.");
					ActionSender.sendIgnoreList(player);
					return;
				}
				if (ignoreLookup.isFailure()) {
					logLookupFailure(player, "ignore-list add", ignoreLookup);
					player.message("Unable to add name right now. Please try again later.");
					ActionSender.sendIgnoreList(player);
					return;
				}
				final PlayerFriend enemyProperUsername = ignoreLookup.getValue();

				final int staffGroup = enemyProperUsername.groupId;
				if (staffGroup >= 0 && staffGroup <= 3) {
					player.message("Staff may not be added to ignore list");
					ActionSender.sendIgnoreList(player);
					return;
				}

				player.getSocial().addIgnore(friendHash, DataConversions.usernameToHash(enemyProperUsername.formerName));
				ActionSender.sendIgnoreList(player);
				break;
			}
			case SOCIAL_REMOVE_IGNORE: {
				player.getSocial().removeIgnore(friendHash);
				break;
			}
			case SOCIAL_SEND_PRIVATE_MESSAGE: {
				if (player.getLocation().onTutorialIsland() && !player.isMod()) {
					player.message("@cya@Once you finish the tutorial, this lets you send messages to your friends");
					return;
				}
				Player friendPlayer = player.getWorld().getPlayer(friendHash);
				if (player.isMuted() && (friendPlayer == null || !friendPlayer.hasElevatedPriveledges())) {
					if (!player.isShadowMuted()) {
						player.message("You have been " + (player.getMuteExpires() == -1 ? "permanently" : "temporarily") + " muted.");
						if (player.getMuteExpires() != -1) {
							player.message("This mute will expire in " + player.getMinutesMuteLeft() + " minutes.");
							player.message("To prevent further mutes please read the rules");
						}
					}
					return;
				}

				String message = payload.message;

				boolean globalMessage = (friendName.toLowerCase().startsWith("global$") || friendName.equalsIgnoreCase("global")) && player.getConfig().WANT_GLOBAL_FRIEND;
				message = MessageFilter.filter(player, message, globalMessage ? "global chat" : friendName + "'s private messages");

				if (!player.speakTongues) {
					message = DataConversions.upperCaseAllFirst(
						DataConversions.stripBadCharacters(message));
				} else {
					message = DataConversions.speakTongues(message);
				}

				if (globalMessage) {
					if (player.isElligibleToGlobalChat()) {
						player.getWorld().addGlobalMessage(new GlobalMessage(player, message));
						player.getWorld().addEntryToSnapshots(new Chatlog(player.getUsername(), "(Global) " + message));
					}
				} else {
					if (!player.isBabyModeFiltered()) {
						player.addPrivateMessage(new PrivateMessage(player, message, friendHash));
						player.getWorld().addEntryToSnapshots(new Chatlog(player.getUsername(), "(Private) " + message));
					} else {
						player.message("Sorry, but someone we banned for breaking our rules is actively throwing a tantrum right now.");
						player.message("New accounts are not allowed to speak until they've reached " + player.getConfig().BABY_MODE_LEVEL_THRESHOLD + " total level during this time.");
					}
				}
				break;
			}
			case SOCIAL_ADD_DELAYED_IGNORE: {
				if (player.getSocial().ignoreCount() >= maxIgnore) {
					player.message("Ignore list full");
					return;
				}
				player.getSocial().addIgnore(friendHash, 0);
				ActionSender.sendIgnoreList(player);
				player.getWorld().getServer().getGameEventHandler().add(new DelayedEvent(player.getWorld(), null, 150000, "Delayed ignore") {
					public void run() {
						player.getSocial().removeIgnore(friendHash);
						ActionSender.sendIgnoreList(player);
					}
				});
				break;
			}
		}
	}

	private DatabaseLookupResult<PlayerFriend> lookupSocialName(final Player player, final String name) {
		final GameDatabase database = player.getWorld().getServer().getDatabase();
		return DatabaseLookupResult.resolve(
			() -> database.getProperUsernameCapitalization(name),
			playerFriend -> playerFriend != null
		);
	}

	private void logLookupFailure(final Player player, final String operation,
			final DatabaseLookupResult<?> result) {
		final GameDatabase database = player.getWorld().getServer().getDatabase();
		LOGGER.error(
			"Database lookup failed during {} (databaseType={}, exceptionType={}, origin={})",
			operation,
			database.getClass().getSimpleName(),
			result.getFailureType(),
			result.getFailureOrigin()
		);
	}
}
