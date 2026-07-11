package orsc.remastered;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class RemasteredSpriteCatalog {
	private static final Pattern RESOURCE_PATH = Pattern.compile(
		"[a-z0-9][a-z0-9._-]*(?:/[a-z0-9][a-z0-9._-]*)*/frames/[0-9]{2,3}\\.png"
	);
	private static final Map<String, Entry> ENTRIES = build();

	private RemasteredSpriteCatalog() {
	}

	public static Entry get(String key) {
		return key == null ? null : ENTRIES.get(key);
	}

	public static int size() {
		return ENTRIES.size();
	}

	public static String revision() {
		return RemasteredSpriteCatalogData.REVISION;
	}

	private static Map<String, Entry> build() {
		LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>();
		for (RemasteredSpriteCatalogData.Record record : RemasteredSpriteCatalogData.ENTRIES) {
			validate(record);
			Entry entry = new Entry(record);
			if (entries.put(record.key, entry) != null) {
				throw new IllegalStateException("Duplicate remastered sprite key: " + record.key);
			}
		}
		return Collections.unmodifiableMap(entries);
	}

	private static void validate(RemasteredSpriteCatalogData.Record record) {
		if (record == null || !RemasteredSpriteKey.isValidCatalogKey(record.key)) {
			throw new IllegalStateException("Invalid remastered sprite catalog key");
		}
		if (record.png == null || record.png.contains("..") || record.png.startsWith("/")
			|| record.png.contains("\\") || !RESOURCE_PATH.matcher(record.png).matches()) {
			throw new IllegalStateException("Invalid remastered sprite resource: " + record.png);
		}
		if (record.pixelWidth <= 0 || record.pixelHeight <= 0
			|| record.pixelWidth > 4096 || record.pixelHeight > 4096
			|| record.boundWidth <= 0 || record.boundHeight <= 0) {
			throw new IllegalStateException("Invalid remastered sprite dimensions: " + record.key);
		}
		if (!"exact-canonical".equals(record.sizePolicy)
			|| !("inherit".equals(record.recolorPolicy) || "none".equals(record.recolorPolicy))
			|| record.alphaThreshold < 1 || record.alphaThreshold > 255) {
			throw new IllegalStateException("Unsupported remastered sprite policy: " + record.key);
		}
	}

	public static final class Entry {
		private final String key;
		private final String png;
		private final int pixelWidth;
		private final int pixelHeight;
		private final boolean requiresShift;
		private final int shiftX;
		private final int shiftY;
		private final int boundWidth;
		private final int boundHeight;
		private final String recolorPolicy;
		private final int alphaThreshold;
		private final String setId;
		private final String contributorId;

		private Entry(RemasteredSpriteCatalogData.Record record) {
			this.key = record.key;
			this.png = record.png;
			this.pixelWidth = record.pixelWidth;
			this.pixelHeight = record.pixelHeight;
			this.requiresShift = record.requiresShift;
			this.shiftX = record.shiftX;
			this.shiftY = record.shiftY;
			this.boundWidth = record.boundWidth;
			this.boundHeight = record.boundHeight;
			this.recolorPolicy = record.recolorPolicy;
			this.alphaThreshold = record.alphaThreshold;
			this.setId = record.setId;
			this.contributorId = record.contributorId;
		}

		public String getKey() { return key; }
		public String getPng() { return png; }
		public int getPixelWidth() { return pixelWidth; }
		public int getPixelHeight() { return pixelHeight; }
		public boolean requiresShift() { return requiresShift; }
		public int getShiftX() { return shiftX; }
		public int getShiftY() { return shiftY; }
		public int getBoundWidth() { return boundWidth; }
		public int getBoundHeight() { return boundHeight; }
		public String getRecolorPolicy() { return recolorPolicy; }
		public int getAlphaThreshold() { return alphaThreshold; }
		public String getSetId() { return setId; }
		public String getContributorId() { return contributorId; }
	}
}
