package com.openrsc.worldbuilder;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Initial read-only command-line boundary for World Builder project discovery. */
public final class WorldBuilderCli {
	private WorldBuilderCli() {
	}

	public static void main(String[] args) {
		int result = run(args);
		if (result != 0) {
			System.exit(result);
		}
	}

	static int run(String[] args) {
		if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
			usage();
			return args.length == 0 ? 2 : 0;
		}
		if (!"discover".equals(args[0])) {
			System.err.println("ERROR: Unsupported World Builder command: " + args[0]);
			usage();
			return 2;
		}

		Path root = null;
		String config = WorldBuilderDiscovery.DEFAULT_CONFIG;
		String expectedContent = null;
		for (int index = 1; index < args.length; index++) {
			String argument = args[index];
			if ("--server-root".equals(argument) && index + 1 < args.length) {
				root = Paths.get(args[++index]);
			} else if ("--config".equals(argument) && index + 1 < args.length) {
				config = args[++index];
			} else if ("--expected-content-sha256".equals(argument) && index + 1 < args.length) {
				expectedContent = args[++index];
			} else {
				System.err.println("ERROR: Unknown or incomplete argument: " + argument);
				usage();
				return 2;
			}
		}
		if (root == null) {
			System.err.println("ERROR: --server-root is required.");
			usage();
			return 2;
		}

		try {
			WorldBuilderDiscoveryResult discovered =
				new WorldBuilderDiscovery().discover(root, config, expectedContent);
			System.out.print(discovered.toJson());
			return 0;
		} catch (WorldBuilderDiscoveryException refusal) {
			System.err.println("ERROR: " + refusal.getMessage());
			return 3;
		}
	}

	private static void usage() {
		System.err.println("Usage: WorldBuilderCli discover --server-root <path>"
			+ " [--config server/myworld.conf]"
			+ " [--expected-content-sha256 <sha256>]");
	}
}
