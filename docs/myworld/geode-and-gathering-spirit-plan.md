# Geode and Gathering Spirit Plan

This is the design record for replacing direct mining gem drops with geodes,
retiring gem resource seeds, and introducing very rare gathering-spirit NPCs as
long-tail gathering rewards.

## Goals

- Move ordinary gem acquisition back toward Mining as the primary skill source.
- Replace direct gem drops from ores with a more exciting side-reward item.
- Rename the Mining side-reward focus from `Gems` to `Geodes`.
- Keep crushers and gem rocks useful without making standard ore mining feel
  unrewarding.
- Remove gem seeds, including the dragonstone gem seed, from the resource seed
  reward family.
- Add a `Key half` tree seed/object concept as the replacement rare tree reward.
- Establish a reusable pattern for rare gathering-spirit NPCs that can later be
  adapted to other gathering skills.

## Geode Items

Four geode items should use the new `geode` sprite source, scaled or framed into
four clear sizes:

- `Small geode`
- `Standard geode`
- `Large geode`
- `Huge geode`

Geodes should be regular inventory items. They are opened with a chisel and
consumed when opened.

First-pass behavior:

1. The player uses a chisel on a geode.
2. The geode is consumed.
3. The server sends: `You crack the geode open...`
4. The reward is rolled.
5. The server sends: `There was X inside!`

If a reward is not a simple item name, the second message should still read
naturally. Example: `There was some Mining experience inside!`

## Mining Reward Focus Rename

Any player-facing Mining rare-reward focus that currently refers to gems should
be renamed to geodes.

The intent is that Mining no longer directly rolls the standard gem line as its
main rare side reward from ore. Instead, ore mining rolls geodes, and geodes
roll their own reward table when opened.

## Geode Sources

Geodes should replace gem drops from normal ore mining.

Open tuning questions:

- Whether all ores can roll every geode size, or whether geode size should be
  gated by ore tier and pickaxe tier.
- Whether the player's selected Mining loot focus affects only geode drop
  chance, geode size, or both.
- Whether gem rocks should continue to give direct gems or should also have
  their own geode chance.

Conservative first pass:

- Low-tier ores mostly roll `Small geode`.
- Mid-tier ores can roll `Small geode` and `Standard geode`.
- High-tier ores can roll `Standard geode` and `Large geode`.
- The highest-tier ore sources can rarely roll `Huge geode`.

## Geode Reward Categories

Geodes can contain:

- Mining XP
- Crafting XP
- coins
- standard gems
- runes
- noted stone
- key halves
- a released `Dwarven Spirit`

The reward intensity should scale by geode size. Larger geodes should not only
increase quantity; they should also unlock better reward categories.

## Reward Tier Shape

### Small Geode

Expected contents:

- small Mining XP reward
- small Crafting XP reward
- modest coins
- low-to-mid standard gems
- low-to-mid rune bundles
- noted stone, roughly `25-50`

Small geodes should not contain key halves.

### Standard Geode

Expected contents:

- stronger Mining XP reward
- stronger Crafting XP reward
- better coin bundles
- broader standard gem line
- broader rune bundles
- noted stone, with a higher average than small geodes
- `1` key half on rare rolls
- extremely rare `Dwarven Spirit`

### Large Geode

Expected contents:

- high Mining XP reward
- high Crafting XP reward
- larger coin bundles
- better standard gem weighting
- larger rune bundles
- larger noted stone bundles
- `1-2` key halves on rare rolls
- very rare `Dwarven Spirit`

### Huge Geode

Expected contents:

- very high Mining XP reward
- very high Crafting XP reward
- high-value coin bundles
- best standard gem weighting
- high-tier rune bundles
- large noted stone bundles
- `1-3` key halves on rare rolls
- rarest but most plausible `Dwarven Spirit` chance

Huge geodes should still avoid becoming guaranteed jackpot items. Their value
should be exciting but uneven, closer to a rare casket than a direct purchase.

## Key Half Tree

Gem seeds should be retired, including the dragonstone gem seed.

The replacement rare tree concept is a `Key half` tree:

- The seed grows into a temporary owned tree.
- The tree gives `3` harvests.
- Each harvest gives a random key half.
- The tree follows the same ownership, lifetime, and harvest-count rules as the
  existing resource seed framework unless a balance reason appears later.

This keeps Woodcutting's rare seed system interesting without making it a major
source of standard gems.

## Dwarven Spirit Concept

`Dwarven Spirit` is a very rare geode result. The concept is that the player
releases a trapped spirit from inside the geode.

Suggested flow:

1. The geode opens.
2. A `Dwarven Spirit` appears or speaks to the player.
3. It says: `Thank you for releasing me, I've been trapped for so long!`
4. It gives the player one random piece of mining equipment.
5. The spirit disappears.

The reward equipment should visually fit dwarves: armor-like mining gear based
on the equipment dwarves wear.

Mechanical identity:

- Equipment is Mining-focused, not combat-focused.
- It gives raw Mining boost value.
- The boost can push the player's effective Mining above their normal maximum.
- The set should feel like a long-term chase reward rather than normal
  progression gear.

Open tuning questions:

- Whether the equipment is tradeable.
- Whether spirit rewards are unique until the player owns each piece.
- Whether duplicate pieces should be allowed, converted into coins, or rerolled.
- Whether the spirit appears as an NPC in the world or is represented as a
  dialogue-only reward event.

Conservative first pass:

- Use a dialogue/event reward first, without a persistent world NPC.
- Pick from a fixed mining-equipment table.
- Avoid duplicate protection until the drop rate and table size are known.

## Future Gathering Spirits

The `Dwarven Spirit` should become the template for other rare gathering-spirit
events.

Possible future equivalents:

- Woodcutting spirit that rewards woodcutting gear.
- Fishing spirit that rewards fishing gear.
- Harvesting spirit that rewards harvesting gear.

Shared rules to consider:

- Released from the skill's rare side-reward item, not from every normal action.
- Gives one rare equipment piece.
- Equipment improves the relevant gathering skill beyond normal tool progress.
- Rare enough to be a long-term chase, not an expected leveling reward.

## Implementation Notes For Later

When this moves from plan to implementation, expected work areas are:

- Add geode item definitions and client sprite definitions.
- Add chisel-on-geode item handling.
- Replace direct Mining gem side rewards with geode rolls.
- Rename Mining focus UI/text from gems to geodes.
- Add geode reward tables by size.
- Add noted stone reward support if not already available for this path.
- Remove gem seeds from Woodcutting side rewards.
- Add the `Key half` tree seed/object definition.
- Add skill-guide updates for Mining, Crafting, and Woodcutting.
- Add tests for geode opening, reward sizing, key-half eligibility, and retired
  gem seed acquisition.

## Current Status

Implemented for geodes and key-half tree, except the `Dwarven Spirit` NPC and
mining-equipment reward are still deferred.

Active implementation notes:

- Normal ore rare rewards now roll geodes instead of direct gems.
- Mining focus text now uses geodes: `Just the ore`, `A few geodes`,
  `Plenty of geodes`, and `Lots of geodes`.
- Geodes open by using a chisel or superchisel on them.
- Geodes can reward Mining XP, Crafting XP, coins, standard gems, runes, noted
  stone, and key halves for standard geodes and above.
- Small geodes do not contain key halves.
- Gem rocks still give direct gems.
- Gem seeds are retired from active Woodcutting acquisition and planting.
- `Key half seed` grows into a three-yield key-half tree.
