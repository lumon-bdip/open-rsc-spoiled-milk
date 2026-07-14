package com.openrsc.worldbuilder;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Validates an export against its exact offline target and prepares an import plan. */
public final class WorldBuilderImporter {
	private static final Destination[] DESTINATIONS = {
		new Destination("terrain", "serverTerrain",
			"server/conf/server/data/Custom_Landscape.orsc"),
		new Destination("terrain", "clientTerrain",
			"Client_Base/Cache/video/Custom_Landscape.orsc"),
		new Destination("sceneryLocs", "sceneryLocs",
			"server/conf/server/defs/locs/MyWorldSceneryLocs.json"),
		new Destination("sceneryRemovals", "sceneryRemovals",
			"server/conf/server/defs/locs/MyWorldSceneryRemovals.json"),
		new Destination("npcLocs", "npcLocs",
			"server/conf/server/defs/locs/MyWorldNpcLocs.json"),
		new Destination("npcRemovals", "npcRemovals",
			"server/conf/server/defs/locs/MyWorldNpcRemovals.json")
	};

	public ImportPlan preview(Path requestedWorkspace, Path requestedExport, Path requestedTarget)
		throws IOException, WorldBuilderDiscoveryException {
		Path workspace = canonicalDirectory(requestedWorkspace, "Builder workspace");
		Path target = canonicalDirectory(requestedTarget, "target private-server root");
		Path workspaceLockPath = workspace.getParent()
			.resolve("." + workspace.getFileName() + ".world-builder.lock");
		try (FileChannel workspaceChannel = FileChannel.open(workspaceLockPath,
			StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			FileLock workspaceLock = tryLock(workspaceChannel,
				"Close World Builder before previewing or importing map changes.");
			try {
				WorldBuilderSourceSnapshot.verify(workspace);
				WorldBuilderProjectSource project = WorldBuilderProjectSource.read(
					workspace.resolve("source/project-source.json"));
				Path configPath = containedFile(target, target.resolve(project.selectedConfig),
					"selected target configuration");
				try (FileChannel configChannel = FileChannel.open(configPath,
					StandardOpenOption.READ, StandardOpenOption.WRITE)) {
					FileLock targetLock = tryLock(configChannel,
						"Another import or rollback is already using the target configuration.");
					try (WorldBuilderTargetOfflineLease offline =
						WorldBuilderTargetOfflineLease.acquire(target, project.selectedConfig)) {
						return planLocked(workspace, requestedExport, target, project);
					} finally {
						targetLock.release();
					}
				}
			} finally {
				workspaceLock.release();
			}
		}
	}

	private ImportPlan planLocked(Path workspace, Path requestedExport, Path target,
		WorldBuilderProjectSource project) throws IOException, WorldBuilderDiscoveryException {
		WorldBuilderExportBundle bundle = WorldBuilderExportBundle.open(requestedExport);
		Path exports = workspace.resolve("exports").normalize();
		if (!Files.isDirectory(exports, LinkOption.NOFOLLOW_LINKS)
			|| Files.isSymbolicLink(exports) || !bundle.root.getParent().equals(exports.toRealPath())) {
			throw new WorldBuilderDiscoveryException(
				"Import requires one immutable export directory from this Builder workspace.");
		}
		if (!project.layoutAdapter.equals(bundle.manifest.layoutAdapter)
			|| !project.sourceFingerprint.equals(bundle.manifest.sourceFingerprint)
			|| !project.contentFingerprint.equals(bundle.manifest.contentFingerprint)) {
			throw new WorldBuilderDiscoveryException(
				"Export provenance does not match this Builder project source.");
		}

		WorldBuilderDiscoveryResult discovered = new WorldBuilderDiscovery().discover(
			target, project.selectedConfig, project.contentFingerprint);
		if (!project.layoutAdapter.equals(discovered.layoutAdapter)
			|| !project.sourceFingerprint.equals(discovered.sourceFingerprintSha256)) {
			throw new WorldBuilderDiscoveryException(
				"Target world files or configuration changed since this Builder project was created.");
		}

		List<Action> actions = new ArrayList<Action>();
		for (Destination destination : DESTINATIONS) {
			WorldBuilderExportBundle.ExportedFile exported = bundle.required(destination.exportLogical);
			WorldBuilderProjectSource.FileState source = project.required(destination.sourceLogical);
			WorldBuilderDiscoveryResult.SourceFile current = sourceFile(discovered, destination.sourceLogical);
			if (source.present != current.present || source.size != current.size
				|| !source.sha256.equals(current.sha256)
				|| source.present != exported.record.sourcePresent
				|| !source.sha256.equals(exported.record.sourceSha256)) {
				throw new WorldBuilderDiscoveryException(
					"Target/source state is inconsistent for " + destination.relativePath + ".");
			}
			if (!exported.record.changed) {
				continue;
			}
			actions.add(new Action(source.present ? "replace" : "add", destination.relativePath,
				source.present, source.sha256, exported.record.sha256, exported.path));
		}
		if (actions.isEmpty()) {
			throw new WorldBuilderDiscoveryException("Export contains no installable changes.");
		}
		return new ImportPlan(workspace, target, bundle.root, project.selectedConfig,
			project.sourceFingerprint, bundle.manifestSha256, bundle.manifest.builderVersion,
			bundle.manifest.sourceCommit, actions);
	}

	private static WorldBuilderDiscoveryResult.SourceFile sourceFile(
		WorldBuilderDiscoveryResult discovered, String logicalName)
		throws WorldBuilderDiscoveryException {
		for (WorldBuilderDiscoveryResult.SourceFile file : discovered.files) {
			if (logicalName.equals(file.logicalName)) {
				return file;
			}
		}
		throw new WorldBuilderDiscoveryException(
			"Discovered target is missing source state for " + logicalName + ".");
	}

	private static FileLock tryLock(FileChannel channel, String message)
		throws IOException, WorldBuilderDiscoveryException {
		FileLock lock;
		try {
			lock = channel.tryLock();
		} catch (OverlappingFileLockException busy) {
			lock = null;
		}
		if (lock == null) {
			throw new WorldBuilderDiscoveryException(message);
		}
		return lock;
	}

	private static Path canonicalDirectory(Path requested, String label)
		throws IOException, WorldBuilderDiscoveryException {
		if (requested == null) {
			throw new WorldBuilderDiscoveryException(label + " is required.");
		}
		Path normalized = requested.toAbsolutePath().normalize();
		if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
			|| Files.isSymbolicLink(normalized)) {
			throw new WorldBuilderDiscoveryException(label + " is missing or unsafe: " + normalized);
		}
		return normalized.toRealPath();
	}

	private static Path containedFile(Path root, Path requested, String label)
		throws IOException, WorldBuilderDiscoveryException {
		Path normalized = requested.toAbsolutePath().normalize();
		if (!normalized.startsWith(root) || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
			|| Files.isSymbolicLink(normalized) || !normalized.toRealPath().startsWith(root)) {
			throw new WorldBuilderDiscoveryException(label + " is missing or unsafe.");
		}
		return normalized.toRealPath();
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"")
			.replace("\n", "\\n").replace("\r", "\\r");
	}

	private static final class Destination {
		final String exportLogical;
		final String sourceLogical;
		final String relativePath;

		Destination(String exportLogical, String sourceLogical, String relativePath) {
			this.exportLogical = exportLogical;
			this.sourceLogical = sourceLogical;
			this.relativePath = relativePath;
		}
	}

	static final class Action {
		final String operation;
		final String relativePath;
		final boolean existedBefore;
		final String beforeSha256;
		final String installedSha256;
		final Path exportedPath;

		Action(String operation, String relativePath, boolean existedBefore,
			String beforeSha256, String installedSha256, Path exportedPath) {
			this.operation = operation;
			this.relativePath = relativePath;
			this.existedBefore = existedBefore;
			this.beforeSha256 = beforeSha256;
			this.installedSha256 = installedSha256;
			this.exportedPath = exportedPath;
		}
	}

	public static final class ImportPlan {
		public final Path workspace;
		public final Path targetRoot;
		public final Path exportDirectory;
		public final String selectedConfig;
		public final String sourceFingerprint;
		public final String exportManifestSha256;
		public final String builderVersion;
		public final String sourceCommit;
		final List<Action> actions;

		ImportPlan(Path workspace, Path targetRoot, Path exportDirectory,
			String selectedConfig, String sourceFingerprint, String exportManifestSha256,
			String builderVersion, String sourceCommit, List<Action> actions) {
			this.workspace = workspace;
			this.targetRoot = targetRoot;
			this.exportDirectory = exportDirectory;
			this.selectedConfig = selectedConfig;
			this.sourceFingerprint = sourceFingerprint;
			this.exportManifestSha256 = exportManifestSha256;
			this.builderVersion = builderVersion;
			this.sourceCommit = sourceCommit;
			this.actions = java.util.Collections.unmodifiableList(
				new ArrayList<Action>(actions));
		}

		public String toJson() {
			StringBuilder json = new StringBuilder(2048);
			json.append("{\n  \"status\": \"ready\",\n  \"dryRun\": true,\n")
				.append("  \"targetRoot\": \"").append(escape(targetRoot.toString())).append("\",\n")
				.append("  \"selectedConfig\": \"").append(escape(selectedConfig)).append("\",\n")
				.append("  \"exportDirectory\": \"").append(escape(exportDirectory.toString())).append("\",\n")
				.append("  \"builderVersion\": \"").append(escape(builderVersion)).append("\",\n")
				.append("  \"sourceCommit\": \"").append(sourceCommit).append("\",\n")
				.append("  \"sourceFingerprintSha256\": \"").append(sourceFingerprint).append("\",\n")
				.append("  \"exportManifestSha256\": \"").append(exportManifestSha256).append("\",\n")
				.append("  \"backupDestination\": \"")
				.append(escape(workspace.resolve("backups/<transaction-id>").toString())).append("\",\n")
				.append("  \"configurationChanges\": [],\n  \"actions\": [\n");
			for (int index = 0; index < actions.size(); index++) {
				Action action = actions.get(index);
				json.append("    {\"operation\": \"").append(action.operation)
					.append("\", \"relativePath\": \"").append(action.relativePath)
					.append("\", \"beforeSha256\": \"").append(action.beforeSha256)
					.append("\", \"installedSha256\": \"").append(action.installedSha256)
					.append("\"}").append(index + 1 < actions.size() ? "," : "").append('\n');
			}
			json.append("  ]\n}\n");
			return json.toString();
		}
	}
}
