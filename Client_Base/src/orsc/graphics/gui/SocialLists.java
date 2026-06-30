package orsc.graphics.gui;

import java.util.Arrays;

public class SocialLists {
	private static final int MIN_GROWTH = 32;
	public static int friendListCount = 0;
	public static String[] friendList = new String[200];
	public static int[] friendListOnlineStatus = new int[200];
	public static String[] friendListWorld = new String[200];
	public static String[] friendListOld = new String[200];

	public static int ignoreListCount = 0;
	public static String[] ignoreList = new String[100];
	public static String[] ignoreListArg0 = new String[100];
	public static String[] ignoreListArg1 = new String[100];
	public static String[] ignoreListOld = new String[100];

	public static int clanListCount = 0;
	public static int partyListCount = 0;

	public static void ensureFriendCapacity(int requiredCapacity) {
		if (requiredCapacity <= friendList.length) {
			return;
		}

		int newCapacity = growCapacity(friendList.length, requiredCapacity);
		friendList = Arrays.copyOf(friendList, newCapacity);
		friendListOnlineStatus = Arrays.copyOf(friendListOnlineStatus, newCapacity);
		friendListWorld = Arrays.copyOf(friendListWorld, newCapacity);
		friendListOld = Arrays.copyOf(friendListOld, newCapacity);
	}

	public static void ensureIgnoreCapacity(int requiredCapacity) {
		if (requiredCapacity <= ignoreList.length) {
			return;
		}

		int newCapacity = growCapacity(ignoreList.length, requiredCapacity);
		ignoreList = Arrays.copyOf(ignoreList, newCapacity);
		ignoreListArg0 = Arrays.copyOf(ignoreListArg0, newCapacity);
		ignoreListArg1 = Arrays.copyOf(ignoreListArg1, newCapacity);
		ignoreListOld = Arrays.copyOf(ignoreListOld, newCapacity);
	}

	private static int growCapacity(int currentCapacity, int requiredCapacity) {
		int newCapacity = Math.max(currentCapacity + Math.max(MIN_GROWTH, currentCapacity / 2), 1);
		while (newCapacity < requiredCapacity) {
			newCapacity += Math.max(MIN_GROWTH, newCapacity / 2);
		}
		return newCapacity;
	}
}
