# AoE Scythe Weapon Plan

Status: implemented. The live first pass includes the tin-through-rune scythe
line, god-knight scythe variants, smithing access, skill-guide coverage,
server-side PvM adjacent NPC cleave, summon/player exclusion, and follow-up
delay scaling for extra targets.

## Goal

Add a main-line melee scythe family from tin through rune. These scythes are
two-handed melee weapons that attack at normal speed against one target, but
become slower when a swing hits multiple enemies.

The existing holiday `Scythe` item must remain a holiday/cosmetic item. The new
line should use the same broad visual identity, but with a combat-ready palette
swap where the blade uses white and grey metal tones.

## Intended Behavior

- The scythe is a melee weapon.
- The scythe is two-handed.
- Base speed should feel normal when only one enemy is hit.
- Each additional enemy hit by the same swing adds a follow-up delay penalty.
- The attack checks the eight tiles surrounding the player.
- The selected combat target does not determine the damage area; every valid
  enemy standing in any adjacent tile is eligible.
- The current target should still be included when adjacent, even if other
  enemies are also hit.
- The scythe should not hit summons or unrelated players by accident.
- PvP behavior should be conservative. Default plan: only allow player splash
  targets when normal PvP rules say that player could be attacked by the
  scythe user. If that is uncertain, start with NPC-only splash and add PvP
  later.

## Balance Shape

Use two-handed sword power as the first tuning reference, then tune the scythe
just below that because it can hit several enemies in one swing. The first
implementation uses about 90% of the matching two-handed sword offense per
metal tier.

Suggested first-pass shape:

- Scythe tier offense: just below the same-tier two-handed sword.
- Weapon speed stat: `3`, the current normal-speed bucket.
- Follow-up delay penalty: `+1` tick for each enemy hit after the first.
- Maximum splash count: all valid enemies in the eight adjacent tiles.
- Maximum extra delay at full surround: `+7` ticks.

This gives the scythe strong packed-enemy value without making it the best
single-target weapon. If the full-surround penalty feels too harsh in testing,
cap the penalty at `+4` or `+5` ticks.

## Item Line

Create a new item for each main metal tier:

- Tin scythe
- Copper scythe
- Bronze scythe
- Iron scythe
- Steel scythe
- Mithril scythe
- Titan steel scythe
- Adamantite scythe
- Orichalcum scythe
- Rune scythe

The existing holiday scythe is:

- Item ID: `1289`
- Appearance: `SCYTHE`
- Client sprite reference: `items:434`
- Equipment animation: `scythe`

The new scythes should get new item IDs rather than repurposing `1289`.

## Visual Asset Plan

1. Use the existing scythe silhouette as the base reference.
2. Create a combat palette version:
   - blade: white and grey
   - handle: readable darker grip/shaft
   - avoid holiday-only colors that make it look like the original event item
3. Export a full equipment frame set under:
   - `dev/myworld/assets/sprites/equipment/scythe/source/`
   - `dev/myworld/assets/sprites/equipment/scythe/numbered/`
4. Register or reuse the existing equipment animation name only if the frames
   align correctly. Otherwise add a new custom animation entry.
5. Create inventory sprites for each metal tier. The first pass can reuse one
   icon with tier tinting, but the blade should stay white/grey so the scythe
   family remains visually consistent.

## Definition Work

Expected files and systems:

- `server/src/com/openrsc/server/constants/ItemId.java`
- `server/conf/server/defs/ItemDefsMyWorld.json`
- `Client_Base/src/com/openrsc/client/entityhandling/MyWorldItemOverrides.java`
- `tools/generators/item-overrides/10-melee-weapons.json`
- generated item override validation
- client runtime item definitions if a manual custom definition is required

Each scythe should have:

- wield action
- weapon equip slot
- two-handed flag or equivalent shield-blocking behavior
- melee offense
- `weaponSpeed: 3`
- appropriate required Attack level where the tier normally uses one
- economy values matching its tier
- bank/tag classification as melee equipment

## Smithing And Acquisition

Initial plan: make scythes smithable from bars, since this is a main-line metal
weapon family.

Suggested recipe:

- `3 bars` per scythe, matching the two-handed AOE identity.
- Same level as the tier's two-handed sword or one level above the tier's
  weapon baseline.
- Add to modern Smithing production UI and legacy smithing menus.
- Add to Smithing skill guide.

The implementation should use 3 bars from the start.

## Combat Implementation Plan

The combat logic should be implemented server-side in the melee event path.
The relevant places are:

- `server/src/com/openrsc/server/event/rsc/impl/combat/CombatEvent.java`
- `server/src/com/openrsc/server/event/rsc/impl/combat/PvmMeleeEvent.java`
- existing splash examples in melee/projectile combat
- existing summon/player exclusion tests around area effects

Implementation outline:

1. Add a helper that identifies scythe weapons by item ID.
2. When the hitter is a player wielding a scythe, collect valid targets on the
   eight adjacent tiles around the hitter.
3. Use the normal selected target as the anchor for combat state, facing, and
   attack permission.
4. For each additional valid target, run a normal melee hit calculation against
   that specific target.
5. Apply hit splats, NPC damage ownership, XP, poison/equipment effects, and
   death handling consistently with ordinary melee.
6. Count the number of targets actually hit by the swing.
7. Apply follow-up delay penalty based on that hit count.

Important rule: the AOE area is based on the player's current tile, not the
target's tile.

## Delay Model

Current melee delay is driven by `weaponSpeed` through each melee event's
`getAdjustedMeleeDelayTicks` logic. The scythe should keep `weaponSpeed: 3`,
then add a dynamic post-swing penalty.

Recommended first-pass formula:

```text
nextDelay = normalAdjustedDelay + max(0, targetsHit - 1)
```

Examples:

- 1 target hit: normal delay
- 2 targets hit: normal delay + 1
- 4 targets hit: normal delay + 3
- 8 targets hit: normal delay + 7

If this interacts badly with speed buffs/debuffs, apply the scythe penalty
after the normal adjusted delay so buffs still affect the base swing but not
the cost of cleaving extra enemies.

## Target Rules

Valid scythe AOE targets should:

- be alive
- be attackable by the player
- be in one of the eight adjacent tiles around the player
- be on the same plane
- not be the player's summon
- not be another player's summon
- not be unrelated players outside valid PvP rules
- not be hit twice by the same swing

NPC target handling should respect existing combat ownership and retaliation
rules. If a non-primary NPC is hit, it should receive damage ownership for loot
and should enter combat or retaliate according to current melee rules.

## Tests

Add tests for:

- item definitions contain the full tin-to-rune scythe line
- scythes are melee weapons
- scythes are two-handed
- scythes use normal weapon speed
- scythes are present in Smithing recipes and guide
- old holiday scythe remains unchanged
- scythe AOE excludes summons
- scythe AOE excludes invalid player targets
- scythe AOE uses player-adjacent tiles, not target-adjacent tiles
- follow-up delay increases only when multiple targets are hit

Likely test files:

- `tests/myworld/test-combat-data.py`
- `tests/myworld/test-weapon-equip-slots.py`
- `tests/myworld/test-smithing-production-coverage.py`
- a new focused test for scythe AOE behavior

## Open Questions

- Should scythe splash work in PvP at launch, or should the first pass be
  NPC-only except for the selected target?
- Should each extra enemy receive full normal damage, or should secondary
  targets receive reduced damage?
- Should the maximum delay penalty be uncapped up to eight targets, or capped
  lower for feel?
- Should scythes be smithable only, or should shops/drops also provide some
  tiers?
- Should dragon eventually get a scythe, or should this line stop at rune for
  now?

## Recommended First Pass

Implement the smallest useful version first:

1. Add tin through rune scythe definitions and sprites.
2. Make them smithable and visible in the skill guide.
3. Add server-side PvM AOE against adjacent NPCs only.
4. Exclude summons and players from splash targets.
5. Apply `+1` tick follow-up delay per extra NPC hit.
6. Playtest packed NPC groups, then decide whether PvP splash or secondary
   target damage reduction is worth adding.
