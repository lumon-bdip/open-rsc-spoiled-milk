package orsc.remastered;

import com.openrsc.client.entityhandling.defs.ItemDef;
import com.openrsc.client.entityhandling.defs.SpriteDef;
import com.openrsc.client.entityhandling.defs.extras.AnimationDef;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RemasteredSpriteKey {
	private static final Pattern SEGMENT = Pattern.compile("[a-z0-9._-]+");

	private RemasteredSpriteKey() {
	}

	public static String forItem(ItemDef item) {
		return item == null ? null : fromLocation(item.getSpriteLocation(), 0);
	}

	public static String forSprite(SpriteDef sprite) {
		return sprite == null ? null : fromLocation(sprite.getSpriteLocation(), 0);
	}

	public static String forAnimation(AnimationDef animation, int frame) {
		if (animation == null || frame < 0) {
			return null;
		}
		return compose(animation.category, animation.getName(), frame);
	}

	static String fromLocation(String location, int frame) {
		if (location == null || frame < 0) {
			return null;
		}
		int separator = location.indexOf(':');
		if (separator <= 0 || separator != location.lastIndexOf(':') || separator == location.length() - 1) {
			return null;
		}
		return compose(location.substring(0, separator), location.substring(separator + 1), frame);
	}

	static String compose(String subspace, String entry, int frame) {
		if (subspace == null || entry == null || frame < 0) {
			return null;
		}
		String normalizedSubspace = subspace.trim().toLowerCase(Locale.ENGLISH);
		String normalizedEntry = entry.trim().toLowerCase(Locale.ENGLISH);
		if (!SEGMENT.matcher(normalizedSubspace).matches() || !SEGMENT.matcher(normalizedEntry).matches()) {
			return null;
		}
		return "sprite/" + normalizedSubspace + "/" + normalizedEntry + "/" + frame;
	}

	static boolean isValidCatalogKey(String key) {
		if (key == null || !key.equals(key.toLowerCase(Locale.ENGLISH))) {
			return false;
		}
		String[] parts = key.split("/", -1);
		if (parts.length != 4 || !"sprite".equals(parts[0])) {
			return false;
		}
		if (!SEGMENT.matcher(parts[1]).matches() || !SEGMENT.matcher(parts[2]).matches()) {
			return false;
		}
		try {
			return Integer.parseInt(parts[3]) >= 0;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}
}
