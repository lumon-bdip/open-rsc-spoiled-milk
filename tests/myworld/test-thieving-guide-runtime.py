#!/usr/bin/env python3
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GUIDE = ROOT / "Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java"
THIEVING = ROOT / "server/plugins/com/openrsc/server/plugins/authentic/skills/thieving/Thieving.java"


def fail(message: str) -> None:
	raise SystemExit(f"FAIL: {message}")


def require(condition: bool, message: str) -> None:
	if not condition:
		fail(message)


def parse_guide_pickpocket_levels() -> dict[str, int]:
	guide = GUIDE.read_text(encoding="utf-8")
	match = re.search(
		r'if \(mc\.getSkillGuideChosen\(\)\.equals\("Thieving"\)\) \{\s*'
		r"if \(curTab == 0\) \{(?P<body>.*?)\n\s*\} else if \(curTab == 1\)",
		guide,
		re.S,
	)
	require(match is not None, "Could not find Thieving pickpocket guide tab")
	body = match.group("body")
	levels = {
		name: int(level)
		for level, name in re.findall(
			r'new SkillMenuNPC\(\d+,\s*"(\d+)",\s*"([^"]+)"\)',
			body,
		)
	}
	for name in (
		"Man",
		"Farmer",
		"Warrior",
		"Workman",
		"Rogue",
		"Guard",
		"Knight",
		"Watchman",
		"Paladin",
		"Gnome",
		"Hero",
	):
		require(name in levels, f"Guide should include pickpocket level for {name}")
	return levels


def parse_runtime_pickpocket_levels() -> dict[str, int]:
	thieving = THIEVING.read_text(encoding="utf-8")
	match = re.search(
		r"enum Pickpocket \{(?P<body>.*?)\n\s*private final ArrayList<LootItem>",
		thieving,
		re.S,
	)
	require(match is not None, "Could not find Pickpocket enum")
	return {
		name: int(required_level)
		for name, required_level in re.findall(
			r"^\s*([A-Z_]+)\((\d+),\s*\d+,",
			match.group("body"),
			re.M,
		)
	}


def main() -> None:
	guide_levels = parse_guide_pickpocket_levels()
	runtime_levels = parse_runtime_pickpocket_levels()
	runtime_to_guide = {
		"MAN": "Man",
		"FARMER": "Farmer",
		"WARRIOR": "Warrior",
		"WORKMAN": "Workman",
		"ROGUE": "Rogue",
		"GUARD": "Guard",
		"KNIGHT": "Knight",
		"YANILLE_WATCHMAN": "Watchman",
		"PALADIN": "Paladin",
		"GNOME_LOCAL": "Gnome",
		"GNOME_CHILD": "Gnome",
		"GNOME_TRAINER": "Gnome",
		"GNOME_WAITER": "Gnome",
		"BLURBERRY_BARMAN": "Gnome",
		"HERO": "Hero",
	}
	for runtime_name, guide_name in runtime_to_guide.items():
		require(runtime_name in runtime_levels, f"Runtime should define {runtime_name}")
		require(
			runtime_levels[runtime_name] == guide_levels[guide_name],
			f"{runtime_name} requires {runtime_levels[runtime_name]}, but guide lists {guide_levels[guide_name]} for {guide_name}",
		)

	print("PASS: Thieving pickpocket runtime levels match the guide")


if __name__ == "__main__":
	main()
