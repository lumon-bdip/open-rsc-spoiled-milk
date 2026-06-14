package com.openrsc.server.content;

import com.openrsc.server.constants.Skill;
import com.openrsc.server.model.entity.player.Player;

public final class RangersGuildPoints {
	public static final String POINTS_CACHE_KEY = "rangers_guild_points";
	public static final String REMAINDER_CACHE_KEY = "rangers_guild_point_remainder";
	public static final int XP_PER_POINT = 10;

	private static final int BASEMENT_MIN_X = 484;
	private static final int BASEMENT_MAX_X = 515;
	private static final int BASEMENT_MIN_Y = 3281;
	private static final int BASEMENT_MAX_Y = 3310;

	private RangersGuildPoints() {
	}

	public static boolean isInBasement(Player player) {
		return player != null
			&& player.getConfig().WANT_MYWORLD
			&& player.getX() >= BASEMENT_MIN_X
			&& player.getX() <= BASEMENT_MAX_X
			&& player.getY() >= BASEMENT_MIN_Y
			&& player.getY() <= BASEMENT_MAX_Y;
	}

	public static void awardFromExperience(Player player, int skill, int experience) {
		if (player == null || skill != Skill.RANGED.id() || experience <= 0 || !isInBasement(player)) {
			return;
		}

		int remainder = getRemainder(player);
		int total = remainder + experience;
		int earnedPoints = total / XP_PER_POINT;
		int newRemainder = total % XP_PER_POINT;

		if (earnedPoints > 0) {
			addPoints(player, earnedPoints);
		}
		player.getCache().set(REMAINDER_CACHE_KEY, newRemainder);
	}

	public static int getPoints(Player player) {
		return getCacheInt(player, POINTS_CACHE_KEY);
	}

	public static void addPoints(Player player, int points) {
		if (player == null || points <= 0) {
			return;
		}
		setPoints(player, getPoints(player) + points);
	}

	public static boolean spendPoints(Player player, int points) {
		if (player == null || points <= 0) {
			return false;
		}

		int currentPoints = getPoints(player);
		if (currentPoints < points) {
			return false;
		}

		setPoints(player, currentPoints - points);
		return true;
	}

	private static int getRemainder(Player player) {
		return getCacheInt(player, REMAINDER_CACHE_KEY);
	}

	private static void setPoints(Player player, int points) {
		player.getCache().set(POINTS_CACHE_KEY, Math.max(0, points));
	}

	private static int getCacheInt(Player player, String key) {
		if (player == null || !player.getCache().hasKey(key)) {
			return 0;
		}
		return Math.max(0, player.getCache().getInt(key));
	}
}
