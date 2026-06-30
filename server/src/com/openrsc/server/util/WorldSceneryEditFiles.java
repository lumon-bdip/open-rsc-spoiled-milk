package com.openrsc.server.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WorldSceneryEditFiles {
	private static final String LOCS_RELATIVE_PATH = "defs/locs/MyWorldSceneryLocs.json";
	private static final String REMOVALS_RELATIVE_PATH = "defs/locs/MyWorldSceneryRemovals.json";
	private static final String LOCS_ROOT = "sceneries";
	private static final String REMOVALS_ROOT = "scenery_removals";

	private WorldSceneryEditFiles() {
	}

	public enum Action {
		UPSERT,
		REMOVE
	}

	public static final class Edit {
		public final Action action;
		public final int id;
		public final int x;
		public final int y;
		public final int direction;
		public final int type;

		private Edit(Action action, int id, int x, int y, int direction, int type) {
			this.action = action;
			this.id = id;
			this.x = x;
			this.y = y;
			this.direction = direction;
			this.type = type;
		}

		public static Edit upsert(int id, int x, int y, int direction, int type) {
			return new Edit(Action.UPSERT, id, x, y, direction, type);
		}

		public static Edit remove(int id, int x, int y, int direction, int type) {
			return new Edit(Action.REMOVE, id, x, y, direction, type);
		}

		public String key() {
			return sceneryKey(x, y);
		}

		public String describe() {
			if (action == Action.REMOVE) {
				return "remove " + id + " at " + x + "," + y;
			}
			return "place " + id + " at " + x + "," + y + " dir " + direction;
		}
	}

	public static final class SaveResult {
		public final int editsApplied;
		public final int sceneryLocsWritten;
		public final int removalsWritten;
		public final Path sceneryLocsPath;
		public final Path removalsPath;

		private SaveResult(int editsApplied, int sceneryLocsWritten, int removalsWritten,
						   Path sceneryLocsPath, Path removalsPath) {
			this.editsApplied = editsApplied;
			this.sceneryLocsWritten = sceneryLocsWritten;
			this.removalsWritten = removalsWritten;
			this.sceneryLocsPath = sceneryLocsPath;
			this.removalsPath = removalsPath;
		}
	}

	private static final class SceneryLoc {
		private final int id;
		private final int x;
		private final int y;
		private final int direction;

		private SceneryLoc(int id, int x, int y, int direction) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.direction = direction;
		}

		private String key() {
			return sceneryKey(x, y);
		}
	}

	public static Path sceneryLocsPath(String configDir) {
		return Paths.get(configDir, LOCS_RELATIVE_PATH);
	}

	public static Path sceneryRemovalsPath(String configDir) {
		return Paths.get(configDir, REMOVALS_RELATIVE_PATH);
	}

	public static String sceneryKey(int x, int y) {
		return x + "," + y;
	}

	public static Set<String> readSceneryRemovalKeys(Path path) throws IOException {
		LinkedHashSet<String> removals = new LinkedHashSet<String>();
		if (!Files.exists(path)) {
			return removals;
		}

		JSONObject root = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
		JSONArray removalDefs = root.optJSONArray(REMOVALS_ROOT);
		if (removalDefs == null) {
			return removals;
		}

		for (int i = 0; i < removalDefs.length(); i++) {
			JSONObject removal = removalDefs.getJSONObject(i);
			JSONObject pos = removal.getJSONObject("pos");
			removals.add(sceneryKey(pos.getInt("X"), pos.getInt("Y")));
		}
		return removals;
	}

	public static SaveResult save(String configDir, Collection<Edit> edits) throws IOException {
		Path sceneryLocsPath = sceneryLocsPath(configDir);
		Path removalsPath = sceneryRemovalsPath(configDir);

		LinkedHashMap<String, SceneryLoc> sceneryLocs = readSceneryLocs(sceneryLocsPath);
		LinkedHashSet<String> removals = new LinkedHashSet<String>(readSceneryRemovalKeys(removalsPath));
		int applied = 0;

		for (Edit edit : edits) {
			if (edit.type != 0) {
				continue;
			}

			String key = edit.key();
			if (edit.action == Action.REMOVE) {
				sceneryLocs.remove(key);
				removals.add(key);
			} else {
				sceneryLocs.put(key, new SceneryLoc(edit.id, edit.x, edit.y, edit.direction));
				removals.remove(key);
			}
			applied++;
		}

		writeSceneryLocs(sceneryLocsPath, sceneryLocs.values());
		writeSceneryRemovals(removalsPath, removals);

		return new SaveResult(applied, sceneryLocs.size(), removals.size(), sceneryLocsPath, removalsPath);
	}

	private static LinkedHashMap<String, SceneryLoc> readSceneryLocs(Path path) throws IOException {
		LinkedHashMap<String, SceneryLoc> locs = new LinkedHashMap<String, SceneryLoc>();
		if (!Files.exists(path)) {
			return locs;
		}

		JSONObject root = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
		JSONArray locDefs = root.optJSONArray(LOCS_ROOT);
		if (locDefs == null) {
			return locs;
		}

		for (int i = 0; i < locDefs.length(); i++) {
			JSONObject locObj = locDefs.getJSONObject(i);
			JSONObject pos = locObj.getJSONObject("pos");
			SceneryLoc loc = new SceneryLoc(
				locObj.getInt("id"),
				pos.getInt("X"),
				pos.getInt("Y"),
				locObj.getInt("direction")
			);
			locs.put(loc.key(), loc);
		}
		return locs;
	}

	private static void writeSceneryLocs(Path path, Collection<SceneryLoc> locs) throws IOException {
		ArrayList<String> entries = new ArrayList<String>();
		for (SceneryLoc loc : locs) {
			entries.add(formatSceneryLoc(loc));
		}
		writeJsonArrayFile(path, LOCS_ROOT, entries);
	}

	private static void writeSceneryRemovals(Path path, Set<String> removals) throws IOException {
		List<String> entries = new ArrayList<String>();
		for (String key : removals) {
			String[] parts = key.split(",", 2);
			entries.add(formatRemoval(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		}
		writeJsonArrayFile(path, REMOVALS_ROOT, entries);
	}

	private static String formatSceneryLoc(SceneryLoc loc) {
		return "\t\t{\n"
			+ "\t\t\t\"id\": " + loc.id + ",\n"
			+ "\t\t\t\"pos\": {\n"
			+ "\t\t\t\t\"X\": " + loc.x + ",\n"
			+ "\t\t\t\t\"Y\": " + loc.y + "\n"
			+ "\t\t\t},\n"
			+ "\t\t\t\"direction\": " + loc.direction + "\n"
			+ "\t\t}";
	}

	private static String formatRemoval(int x, int y) {
		return "\t\t{\n"
			+ "\t\t\t\"pos\": {\n"
			+ "\t\t\t\t\"X\": " + x + ",\n"
			+ "\t\t\t\t\"Y\": " + y + "\n"
			+ "\t\t\t}\n"
			+ "\t\t}";
	}

	private static void writeJsonArrayFile(Path path, String rootName, List<String> entries) throws IOException {
		Files.createDirectories(path.getParent());
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		builder.append("\t\"").append(rootName).append("\": [");
		if (!entries.isEmpty()) {
			builder.append("\n");
			for (int i = 0; i < entries.size(); i++) {
				if (i > 0) {
					builder.append(",\n");
				}
				builder.append(entries.get(i));
			}
			builder.append("\n\t");
		}
		builder.append("]\n");
		builder.append("}\n");
		Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
	}
}
