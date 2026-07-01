package com.openrsc.server.util;

import com.openrsc.server.external.NPCLoc;
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

public final class WorldNpcEditFiles {
	private static final String LOCS_RELATIVE_PATH = "defs/locs/MyWorldNpcLocs.json";
	private static final String REMOVALS_RELATIVE_PATH = "defs/locs/MyWorldNpcRemovals.json";
	private static final String LOCS_ROOT = "npclocs";
	private static final String REMOVALS_ROOT = "npc_removals";

	private WorldNpcEditFiles() {
	}

	public enum Action {
		UPSERT,
		REMOVE
	}

	public static final class Edit {
		public final Action action;
		public final NPCLoc loc;

		private Edit(Action action, NPCLoc loc) {
			this.action = action;
			this.loc = copyLoc(loc);
		}

		public static Edit upsert(NPCLoc loc) {
			return new Edit(Action.UPSERT, loc);
		}

		public static Edit remove(NPCLoc loc) {
			return new Edit(Action.REMOVE, loc);
		}

		public String key() {
			return npcKey(loc);
		}

		public String describe() {
			if (action == Action.REMOVE) {
				return "remove npc " + loc.id + " at " + loc.startX + "," + loc.startY;
			}
			return "place npc " + loc.id + " at " + loc.startX + "," + loc.startY
				+ " radius x " + (loc.startX - loc.minX) + "/" + (loc.maxX - loc.startX)
				+ " y " + (loc.startY - loc.minY) + "/" + (loc.maxY - loc.startY);
		}
	}

	public static final class SaveResult {
		public final int editsApplied;
		public final int npcLocsWritten;
		public final int removalsWritten;
		public final Path npcLocsPath;
		public final Path removalsPath;

		private SaveResult(int editsApplied, int npcLocsWritten, int removalsWritten,
						   Path npcLocsPath, Path removalsPath) {
			this.editsApplied = editsApplied;
			this.npcLocsWritten = npcLocsWritten;
			this.removalsWritten = removalsWritten;
			this.npcLocsPath = npcLocsPath;
			this.removalsPath = removalsPath;
		}
	}

	public static Path npcLocsPath(String configDir) {
		return Paths.get(configDir, LOCS_RELATIVE_PATH);
	}

	public static Path npcRemovalsPath(String configDir) {
		return Paths.get(configDir, REMOVALS_RELATIVE_PATH);
	}

	public static String npcKey(NPCLoc loc) {
		return npcKey(loc.id, loc.startX, loc.startY);
	}

	public static String npcKey(int id, int startX, int startY) {
		return id + "," + startX + "," + startY;
	}

	public static Set<String> readNpcRemovalKeys(Path path) throws IOException {
		LinkedHashSet<String> removals = new LinkedHashSet<String>();
		for (NPCLoc loc : readNpcLocList(path, REMOVALS_ROOT)) {
			removals.add(npcKey(loc));
		}
		return removals;
	}

	public static SaveResult save(String configDir, Collection<Edit> edits) throws IOException {
		Path npcLocsPath = npcLocsPath(configDir);
		Path removalsPath = npcRemovalsPath(configDir);

		LinkedHashMap<String, NPCLoc> npcLocs = readNpcLocs(npcLocsPath);
		LinkedHashMap<String, NPCLoc> removals = readNpcRemovals(removalsPath);
		int applied = 0;

		for (Edit edit : edits) {
			String key = edit.key();
			if (edit.action == Action.REMOVE) {
				npcLocs.remove(key);
				removals.put(key, copyLoc(edit.loc));
			} else {
				npcLocs.put(key, copyLoc(edit.loc));
				removals.remove(key);
			}
			applied++;
		}

		writeNpcLocs(npcLocsPath, LOCS_ROOT, npcLocs.values());
		writeNpcLocs(removalsPath, REMOVALS_ROOT, removals.values());

		return new SaveResult(applied, npcLocs.size(), removals.size(), npcLocsPath, removalsPath);
	}

	private static LinkedHashMap<String, NPCLoc> readNpcLocs(Path path) throws IOException {
		LinkedHashMap<String, NPCLoc> locs = new LinkedHashMap<String, NPCLoc>();
		for (NPCLoc loc : readNpcLocList(path, LOCS_ROOT)) {
			locs.put(npcKey(loc), loc);
		}
		return locs;
	}

	private static LinkedHashMap<String, NPCLoc> readNpcRemovals(Path path) throws IOException {
		LinkedHashMap<String, NPCLoc> locs = new LinkedHashMap<String, NPCLoc>();
		for (NPCLoc loc : readNpcLocList(path, REMOVALS_ROOT)) {
			locs.put(npcKey(loc), loc);
		}
		return locs;
	}

	private static List<NPCLoc> readNpcLocList(Path path, String rootName) throws IOException {
		ArrayList<NPCLoc> locs = new ArrayList<NPCLoc>();
		if (!Files.exists(path)) {
			return locs;
		}

		JSONObject root = new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
		JSONArray locDefs = root.optJSONArray(rootName);
		if (locDefs == null) {
			return locs;
		}

		for (int i = 0; i < locDefs.length(); i++) {
			JSONObject locObj = locDefs.getJSONObject(i);
			JSONObject start = locObj.getJSONObject("start");
			JSONObject min = locObj.getJSONObject("min");
			JSONObject max = locObj.getJSONObject("max");
			locs.add(new NPCLoc(
				locObj.getInt("id"),
				start.getInt("X"),
				start.getInt("Y"),
				min.getInt("X"),
				max.getInt("X"),
				min.getInt("Y"),
				max.getInt("Y")
			));
		}
		return locs;
	}

	private static void writeNpcLocs(Path path, String rootName, Collection<NPCLoc> locs) throws IOException {
		ArrayList<String> entries = new ArrayList<String>();
		for (NPCLoc loc : locs) {
			entries.add(formatNpcLoc(loc));
		}
		writeJsonArrayFile(path, rootName, entries);
	}

	private static String formatNpcLoc(NPCLoc loc) {
		return "\t\t{\n"
			+ "\t\t\t\"id\": " + loc.id + ",\n"
			+ "\t\t\t\"start\": {\"X\": " + loc.startX + ", \"Y\": " + loc.startY + "},\n"
			+ "\t\t\t\"min\": {\"X\": " + loc.minX + ", \"Y\": " + loc.minY + "},\n"
			+ "\t\t\t\"max\": {\"X\": " + loc.maxX + ", \"Y\": " + loc.maxY + "}\n"
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

	private static NPCLoc copyLoc(NPCLoc loc) {
		return new NPCLoc(loc.id, loc.startX, loc.startY, loc.minX, loc.maxX, loc.minY, loc.maxY);
	}
}
