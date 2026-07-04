#!/usr/bin/env python3

import re
import sys
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parents[2]
NPC_PROFILE = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcAttackStyleProfile.java"
NPC_ELEMENT = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcMagicElement.java"
NPC_BEHAVIOR = ROOT / "server/src/com/openrsc/server/model/entity/npc/NpcBehavior.java"
PROJECTILE_EVENT = ROOT / "server/src/com/openrsc/server/event/rsc/impl/projectile/ProjectileEvent.java"
PLAYER = ROOT / "server/src/com/openrsc/server/model/entity/player/Player.java"
EQUIPMENT = ROOT / "server/src/com/openrsc/server/model/container/Equipment.java"
SERVER_PROJECTILE = ROOT / "server/src/com/openrsc/server/model/entity/update/Projectile.java"
SERVER_COMBAT_EFFECT = ROOT / "server/src/com/openrsc/server/model/entity/update/CombatEffect.java"
CLIENT_ENTITY_HANDLER = ROOT / "Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java"
CLIENT_MUDCLIENT = ROOT / "Client_Base/src/orsc/mudclient.java"


def fail(message: str) -> NoReturn:
	print(f"FAIL: {message}")
	sys.exit(1)


def require(text: str, snippet: str, label: str) -> None:
	if snippet not in text:
		fail(f"{label} missing expected snippet: {snippet}")


def require_regex(text: str, pattern: str, label: str) -> None:
	if not re.search(pattern, text, re.DOTALL):
		fail(f"{label} missing expected pattern: {pattern}")


def main() -> None:
	profile = NPC_PROFILE.read_text(encoding="utf-8")
	element = NPC_ELEMENT.read_text(encoding="utf-8")
	behavior = NPC_BEHAVIOR.read_text(encoding="utf-8")
	projectile = PROJECTILE_EVENT.read_text(encoding="utf-8")
	player = PLAYER.read_text(encoding="utf-8")
	equipment = EQUIPMENT.read_text(encoding="utf-8")
	server_projectile = SERVER_PROJECTILE.read_text(encoding="utf-8")
	server_combat_effect = SERVER_COMBAT_EFFECT.read_text(encoding="utf-8")
	client_entity_handler = CLIENT_ENTITY_HANDLER.read_text(encoding="utf-8")
	client_mudclient = CLIENT_MUDCLIENT.read_text(encoding="utf-8")

	for name in ("NONE", "AIR", "WATER", "EARTH", "FIRE", "THUNDER", "WOOD"):
		require(element, name, f"NPC magic element {name}")

	require(profile, "if (isDragon(npc)) {\n\t\t\treturn MELEE_MAGIC;\n\t\t}", "Dragons should mix elemental magic into melee profile")
	require(profile, "case \"battle mage\":\n\t\t\t\treturn PURE_MAGIC;", "Battle mages should be magic-only")
	require(profile, "getBattleMageImpactEffect(element)", "Battle mages should use dedicated tier 3 impact aliases")
	require(profile, "return CombatEffect.BATTLE_MAGE_AIR;", "Battle mage air should use tier 3 air visual alias")
	require(profile, "return CombatEffect.BATTLE_MAGE_EARTH;", "Battle mage earth should use tier 3 earth visual alias")
	require(profile, "return CombatEffect.BATTLE_MAGE_WATER;", "Battle mage water should use tier 3 water visual alias")
	require(profile, "return CombatEffect.BATTLE_MAGE_FIRE;", "Battle mage fire should use tier 3 fire visual alias")
	require(profile, "\"lucien\".equals(name)", "Lucien should remain untyped")
	require(profile, "\"otherworldly being\".equals(name)", "Otherworldly beings should remain untyped")
	require(profile, "name.contains(\"druid\")", "Druids should be treated as holy magic users")
	require(profile, "return Projectile.HOLY_MAGIC;", "Holy magic NPCs should use the holy magic projectile")
	require(profile, "return NpcMagicElement.NONE;", "Untyped NPC magic should be supported")
	require(profile, "usesBasicCasterFireProjectile(name)", "Basic fire projectile should be limited to basic fire casters")
	require(profile, "return Projectile.ENEMY_FIRE_BASIC;", "Basic fire casters should use the enemy-fire-basic projectile")
	require(profile, "if (usesBasicCasterFireProjectile(name)) {\n\t\t\t\t\treturn CombatEffect.NONE;", "Basic fire casters should not layer fire claw impact over the projectile")
	require(profile, "if (usesBasicCasterAirProjectile(name)) {\n\t\t\t\t\treturn CombatEffect.NONE;", "Basic air casters should not layer wind slash impact over the projectile")
	require(profile, "if (usesBasicCasterWaterProjectile(name)) {\n\t\t\t\t\treturn CombatEffect.NONE;", "Basic water casters should not layer water burst impact over the projectile")
	require(profile, "\"darkwizard\".equals(name)", "Darkwizard fire spells should use basic caster fireball")
	require(profile, "\"necromancer\".equals(name)", "Necromancer fire spells should use basic caster fireball")
	require(profile, "\"skeleton mage\".equals(name)", "Skeleton mage fire spells should use basic caster fireball")
	require(profile, "usesBasicCasterAirProjectile(name)", "Basic air projectile should be limited to basic casters")
	require(profile, "return Projectile.ENEMY_AIR_BASIC;", "Basic caster air spells should use enemy air projectile")
	require(profile, "\"witch\".equals(name)", "Witch air spells should use basic enemy air projectile")
	require(profile, "usesBasicCasterWaterProjectile(name)", "Basic water projectile should be limited to basic casters")
	require(profile, "return Projectile.ENEMY_WATER_BASIC;", "Basic caster water spells should use enemy water projectile")
	require(profile, "npc.getID() == NpcId.BLUE_DRAGON.id()", "Adult blue dragons should use their own magic projectile")
	require(profile, "return Projectile.BLUE_DRAGON_MAGIC;", "Adult blue dragons should use the blue dragon beam projectile")
	require(profile, "getDragonMagicImpactEffect(element)", "Dragon magic should use dragon-specific impact effects")
	require(profile, "return CombatEffect.GREEN_DRAGON_MAGIC;", "Earth dragons should use green dragon magic impact")
	require(profile, "return CombatEffect.FIRE_DRAGON_MAGIC;", "Fire dragons should use fire dragon magic impact")
	require(profile, "\"necromancer\".equals(name)", "Necromancer water spells should use basic enemy water projectile")
	require(profile, "\"ghost\".equals(name)", "Ghost water spells should use basic enemy water projectile")
	require(profile, "usesBasicCasterEarthImpact(name)", "Basic earth impact should be limited to basic casters")
	require(profile, "return Projectile.BLANK;", "Basic caster earth spells should use no travelling projectile")
	require(profile, "return CombatEffect.ENEMY_EARTH_BASIC;", "Basic caster earth spells should use enemy earth impact")
	require(profile, "\"skeleton mage\".equals(name)", "Skeleton mage earth spells should use basic enemy earth impact")
	require(profile, "enemySpecificEffect != CombatEffect.NONE", "Enemy-specific magic effects should not be replaced by elemental effects")
	require(profile, "usesFireKinMagic(name)", "Fire kin should be assigned their own on-player magic effect")
	require(profile, "usesIceKinMagic(name)", "Ice kin should be assigned their own on-player magic effect")
	require(profile, "usesEarthKinMagic(name)", "Earth kin should be assigned their own on-player magic effect")
	require(profile, "return CombatEffect.FIRE_KIN_MAGIC;", "Fire kin should use fire kin magic impact")
	require(profile, "return CombatEffect.ICE_KIN_MAGIC;", "Ice kin should use ice kin magic impact")
	require(profile, "return CombatEffect.EARTH_KIN_MAGIC;", "Earth kin should use earth kin magic impact")

	expected_splits = {
		"darkwizard": ("FIRE", "AIR"),
		"witch": ("EARTH", "AIR"),
		"wizard": ("WATER", "AIR"),
		"necromancer": ("FIRE", "WATER"),
		"skeleton mage": ("FIRE", "EARTH"),
		"ghost": ("WATER", "EARTH"),
		"battle mage": ("AIR", "WATER", "EARTH", "FIRE"),
	}
	for npc_name, elements in expected_splits.items():
		pattern = rf"case \"{re.escape(npc_name)}\":.*?return randomElement\({', '.join('NpcMagicElement\\.' + e for e in elements)}\);"
		require_regex(profile, pattern, f"{npc_name} elemental split")

	for snippet in (
		"case \"lesser demon\":",
		"case \"greater demon\":",
		"case \"chronozon\":",
		"case \"black demon\":",
		"case \"fire giant\":",
		"case \"fire warrior\":",
		"case \"delrith\":",
		"case \"the fire warrior of lesarkus\":",
		"return NpcMagicElement.FIRE;",
		"case \"ice giant\":",
		"case \"ice warrior\":",
		"case \"ice queen\":",
		"return NpcMagicElement.WATER;",
		"case \"moss giant\":",
		"case \"tree spirit\":",
		"return NpcMagicElement.EARTH;",
	):
		require(profile, snippet, f"Elemental assignment snippet {snippet}")

	require(profile, "case 196: // GREEN_DRAGON", "Green dragons should be earth magic")
	require(profile, "return NpcMagicElement.EARTH;", "Green dragons should use earth magic")
	require(profile, "case 202: // BLUE_DRAGON", "Blue dragons should be water magic")
	require(profile, "case 203: // BABY_BLUE_DRAGON", "Baby blue dragons should be water magic")
	require(profile, "return NpcMagicElement.FIRE;", "Other dragons should default to fire magic")

	require(behavior, "NpcMagicElement magicElement = profile.getMagicElement(npc);", "NPC behavior should choose one element per cast")
	require(behavior, "profile.getMagicProjectileVisual(npc, magicElement)", "NPC projectile visual should match selected element")
	require(behavior, "profile.getMagicImpactEffect(npc, magicElement)", "NPC impact effect should match selected element")
	require(behavior, "int fireDefenseDebuffPercent = profile.getMagicFireDefenseDebuffPercent(npc, magicElement);", "NPC projectile should carry selected fire debuff")
	require(behavior, "0, 0, 0, fireDefenseDebuffPercent, profile.getMagicProjectileVisual(npc, magicElement), impactEffectType, true, magicElement,", "NPC projectile fire debuff should match selected element")
	require(behavior, "true, magicElement,\n\t\t\t\tstartleProcChancePercent, acidPoisonPower, 0, splinterProcChancePercent));", "NPC projectile should carry selected element")

	require(projectile, "protected NpcMagicElement magicElement = NpcMagicElement.NONE;", "Projectile should default to untyped magic")
	require(projectile, "applyRobeDamageMitigation(damage, magicElement)", "Projectile damage should pass element to robe resistance")
	require(projectile, "applyBalrogMagicSplash((Npc) caster, (Player) opponent, damageDealt);", "Balrog magic should splash after primary damage")
	require(projectile, "Math.ceil(primaryDamageDealt * 0.5D)", "Balrog splash should be 50 percent of primary damage")
	require(projectile, "\"balrog\".equalsIgnoreCase(balrog.getDef().getName())", "Balrog splash should only apply to Balrog")
	require(projectile, "splashTarget.withinRange(primaryTarget.getLocation(), 2)", "Balrog splash should use a two-tile radius")
	require(server_projectile, "public static final int ENEMY_AIR_BASIC = 25;", "Server should define enemy air basic projectile id")
	require(server_projectile, "public static final int ENEMY_WATER_BASIC = 26;", "Server should define enemy water basic projectile id")
	require(server_projectile, "public static final int BLUE_DRAGON_MAGIC = 27;", "Server should define blue dragon magic projectile id")
	require(server_combat_effect, "public static final int ENEMY_EARTH_BASIC = 44;", "Server should define enemy earth basic combat effect id")
	require(server_combat_effect, "public static final int BLACK_DEMON_MAGIC = 45;", "Server should define black demon magic combat effect id")
	require(server_combat_effect, "public static final int BALROG_MAGIC = 46;", "Server should define balrog magic combat effect id")
	require(server_combat_effect, "public static final int BATTLE_MAGE_AIR = 47;", "Server should define battle mage air combat effect id")
	require(server_combat_effect, "public static final int BATTLE_MAGE_EARTH = 48;", "Server should define battle mage earth combat effect id")
	require(server_combat_effect, "public static final int BATTLE_MAGE_WATER = 49;", "Server should define battle mage water combat effect id")
	require(server_combat_effect, "public static final int BATTLE_MAGE_FIRE = 50;", "Server should define battle mage fire combat effect id")
	require(server_combat_effect, "public static final int GREEN_DRAGON_MAGIC = 51;", "Server should define green dragon magic combat effect id")
	require(server_combat_effect, "public static final int FIRE_DRAGON_MAGIC = 52;", "Server should define fire dragon magic combat effect id")
	require(server_combat_effect, "public static final int OTHERWORLDLY_BEING_MAGIC = 53;", "Server should define otherworldly being magic combat effect id")
	require(server_combat_effect, "public static final int PALADIN_MAGIC = 54;", "Server should define paladin magic combat effect id")
	require(server_combat_effect, "public static final int FIRE_KIN_MAGIC = 55;", "Server should define fire kin magic combat effect id")
	require(server_combat_effect, "public static final int ICE_KIN_MAGIC = 56;", "Server should define ice kin magic combat effect id")
	require(server_combat_effect, "public static final int EARTH_KIN_MAGIC = 57;", "Server should define earth kin magic combat effect id")
	require(server_combat_effect, "case \"chronozon\":", "Chronozon should use greater demon magic")
	require(server_combat_effect, "case \"black demon\":", "Black demon should use its own magic effect")
	require(server_combat_effect, "case \"balrog\":", "Balrog should use its own magic effect")
	require(server_combat_effect, "case \"otherworldly being\":", "Otherworldly beings should use their own magic effect")
	require(server_combat_effect, "case \"paladin\":", "Paladins should use their own magic effect")
	require(client_entity_handler, "ENEMY_AIR_BASIC(25)", "Client projectile enum should define enemy air basic")
	require(client_entity_handler, "ENEMY_WATER_BASIC(26)", "Client projectile enum should define enemy water basic")
	require(client_entity_handler, "BLUE_DRAGON_MAGIC(27)", "Client projectile enum should define blue dragon magic")
	require(client_entity_handler, '"enemy air basic projectile"', "Client projectile definitions should include enemy air basic")
	require(client_entity_handler, '"enemy water basic projectile"', "Client projectile definitions should include enemy water basic")
	require(client_entity_handler, '"blue dragon magic projectile"', "Client projectile definitions should include blue dragon magic")
	require(client_mudclient, '"enemy-air-basic"', "Client should load enemy-air-basic projectile asset folder")
	require(client_mudclient, '"enemy-water-basic"', "Client should load enemy-water-basic projectile asset folder")
	require(client_mudclient, '"blue-dragon-magic"', "Client should load blue-dragon-magic projectile asset folder")
	require(client_mudclient, "PROJECTILE_TYPES.ENEMY_AIR_BASIC.id()", "Client should size enemy air basic as a spell projectile")
	require(client_mudclient, "PROJECTILE_TYPES.ENEMY_WATER_BASIC.id()", "Client should size enemy water basic as a spell projectile")
	require(client_mudclient, "PROJECTILE_TYPES.BLUE_DRAGON_MAGIC.id()", "Client should size blue dragon magic as a spell projectile")
	require(client_mudclient, "private boolean isRootedProjectile(SpriteDef projectile)", "Client should support rooted projectile visuals")
	require(client_mudclient, "projectile.id == PROJECTILE_TYPES.ENEMY_WATER_BASIC.id()", "Enemy water basic should render as rooted projectile")
	require(client_mudclient, "private boolean isCasterRootedProjectile(SpriteDef projectile)", "Client should support caster-rooted projectile visuals")
	require(client_mudclient, "projectile.id == PROJECTILE_TYPES.BLUE_DRAGON_MAGIC.id()", "Blue dragon magic should render as a caster-rooted beam")
	require(client_mudclient, "private int getPlayerProjectileCenterY(ORSCharacter player)", "Client should anchor player projectiles at body center")
	require(client_mudclient, "private int getNpcProjectileCenterY(ORSCharacter npc)", "Client should anchor NPC projectiles at body center")
	require(client_mudclient, "int projectileBottomY = getProjectileSpriteBottomY(var12, projectileSize);", "Client should draw projectile sprites centered on the projectile path")
	require(client_mudclient, "? getNpcProjectileCenterY(var16)", "Client should use NPC-height shooter anchors for NPC projectiles")
	require(client_mudclient, "COMBAT_EFFECT_ENEMY_EARTH_BASIC = 44", "Client should define enemy earth basic combat effect id")
	require(client_mudclient, '"enemy-earth-basic"', "Client should load enemy-earth-basic On Player asset")
	require(client_mudclient, "COMBAT_EFFECT_BLACK_DEMON_MAGIC = 45", "Client should define black demon magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_BALROG_MAGIC = 46", "Client should define balrog magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_BATTLE_MAGE_AIR = 47", "Client should define battle mage air combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_BATTLE_MAGE_EARTH = 48", "Client should define battle mage earth combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_BATTLE_MAGE_WATER = 49", "Client should define battle mage water combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_BATTLE_MAGE_FIRE = 50", "Client should define battle mage fire combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_GREEN_DRAGON_MAGIC = 51", "Client should define green dragon magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_FIRE_DRAGON_MAGIC = 52", "Client should define fire dragon magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_OTHERWORLDLY_BEING_MAGIC = 53", "Client should define otherworldly being magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_PALADIN_MAGIC = 54", "Client should define paladin magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_FIRE_KIN_MAGIC = 55", "Client should define fire kin magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_ICE_KIN_MAGIC = 56", "Client should define ice kin magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_EARTH_KIN_MAGIC = 57", "Client should define earth kin magic combat effect id")
	require(client_mudclient, "COMBAT_EFFECT_COUNT = 61", "Client should include elemental sword procs in the effect table")
	require(client_mudclient, '"battle-mage-air", "battle-mage-earth", "battle-mage-water", "battle-mage-fire"', "Client should name battle mage visual aliases")
	require(client_mudclient, '"green-dragon-magic", "fire-dragon-magic"', "Client should name dragon magic On Player effects")
	require(client_mudclient, '"otherworldly-being-magic", "paladin-magic"', "Client should name untyped On Player magic effects")
	require(client_mudclient, '"fire-kin-magic", "ice-kin-magic", "earth-kin-magic"', "Client should name elemental kin On Player magic effects")
	require(client_mudclient, 'return "tornado";', "Battle mage air should borrow tornado frames")
	require(client_mudclient, 'return "earth-burst";', "Battle mage earth should borrow earth burst frames")
	require(client_mudclient, 'return "water-eruption";', "Battle mage water should borrow water eruption frames")
	require(client_mudclient, 'return "explosion";', "Battle mage fire should borrow explosion frames")
	require(client_mudclient, '"black-demon-magic"', "Client should load black-demon-magic On Player asset")
	require(client_mudclient, '"balrog-magic"', "Client should load balrog-magic On Player asset")
	require(client_mudclient, '"green-dragon-magic"', "Client should load green-dragon-magic On Player asset")
	require(client_mudclient, '"fire-dragon-magic"', "Client should load fire-dragon-magic On Player asset")
	require(client_mudclient, '"otherworldly-being-magic"', "Client should load otherworldly-being-magic On Player asset")
	require(client_mudclient, '"paladin-magic"', "Client should load paladin-magic On Player asset")
	require(client_mudclient, '"fire-kin-magic"', "Client should load fire-kin-magic On Player asset")
	require(client_mudclient, '"ice-kin-magic"', "Client should load ice-kin-magic On Player asset")
	require(client_mudclient, '"earth-kin-magic"', "Client should load earth-kin-magic On Player asset")

	for robe in ("Air", "Water", "Earth", "Fire"):
		require(equipment, f"get{robe}RobeTierTotal()", f"{robe} robe tier total accessor")
		require(player, f"get{robe}RobeTierTotal()", f"{robe} robe resistance lookup")
	require(player, "tierTotal * 0.02D", "Elemental robe resistance should scale at 2 percent per tier")

	print("PASS: NPC elemental magic assignments validated")


if __name__ == "__main__":
	main()
