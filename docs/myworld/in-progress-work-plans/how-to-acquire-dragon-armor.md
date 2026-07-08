# How To Acquire Dragon Armor

Status: audit
Owner: An-actual-duck
Branch: workspace/major-work

## Purpose

Record the current and legacy acquisition routes for dragon armor so future
work can preserve meaningful quest and progression content without
reintroducing retired skirt items.

Skirts should stay retired. The important legacy thread is the work that went
into Combat Odyssey, not the old skirt reward itself.

## Key Findings

- The remembered Legends Guild quest line appears to be `Combat Odyssey`.
  There is no current `Fezmog` reference in the repo. The quest uses Sir
  Radimus Erkle, Siegfried Erkle, and Biggum Flodrot.
- Combat Odyssey originally awarded a choice between `Dragon Plate Mail Legs`
  and `Dragon Plated Skirt`. That was added in commit `3a6787542 The Odyssey`.
- Commit `837de7256 Balance dragon gear and summoning fixes` removed the skirt
  choice and changed the final reward to `Dragon Plate Mail Legs`.
- Current high-end dragon plate and scale armor is mostly obtained through
  Wayne in Falador after the Dwarf Youth Rescue miniquest, except dragon plate
  legs, which are restored to Combat Odyssey as their route.
- Current dragon shield acquisition still uses the classic shield-half path:
  right half plus left half plus hammer on an anvil.
- Item `1430` is legacy-sensitive. Base custom item defs still call it
  `Dragon Plated Skirt`, but MyWorld and the client override it to
  `Dragon Scale Mail Legs`. Treat `1430` as scale mail legs in current
  Spoiled Milk design.
- `Dragon Plate Mail Top` and `Dragon Scale Mail Top` exist, but no normal
  player-facing acquisition route was found for either. They appear to be
  compatibility, female-appearance, or hidden support items.

## Current Acquisition Matrix

| Requested armor | Current item and ID | Current route found | Legacy route found | Notes |
| --- | --- | --- | --- | --- |
| Platemail body | `Dragon Plate Mail Body` `1427` | Wayne can make it after Dwarf Youth Rescue for `4 Dragon bars` and `500,000 coins`. | No Combat Odyssey route found. | Female characters use the female plate-top appearance automatically from the body item. |
| Platemail legs | `Dragon Plate Mail Legs` `1429` | Combat Odyssey awards it on completion. | Original Combat Odyssey final choice could award this or the retired skirt. | Restored as a Combat Odyssey-only route; Wayne no longer crafts it. |
| Skirt | Retired. Legacy `Dragon Plated Skirt` used item `1430`. | No current skirt route should exist. | Original Combat Odyssey final choice could award `Dragon Plated Skirt`. | Do not reintroduce. Current `1430` is `Dragon Scale Mail Legs`. |
| Top | `Dragon Plate Mail Top` `1428` | No normal acquisition route found. | No current legacy reward route found beyond compatibility/history. | MyWorld/client now label this as `Dragon plate mail body`; it should probably remain a compatibility/female visual support item unless a future plan says otherwise. |
| Scalemail body | `Dragon Scale Mail Body` `1368` | Wayne can make it after Dwarf Youth Rescue for `500 Dragon Metal Chains`, `150 Chipped Dragon Scales`, and `500,000 coins`. | Dwarf Smithy Note points players to this route. | Also reduces dragon breath damage when worn. |
| Scalemail legs | `Dragon Scale Mail Legs` `1430` | Wayne can make it for `500 Dragon Metal Chains`, `100 Chipped Dragon Scales`, and `500,000 coins`. | Same ID was formerly the retired `Dragon Plated Skirt`. | Current name and identity should stay scale mail legs. |
| Scalemail top | `Dragon Scale Mail Top` `1537` | No normal acquisition route found. | No current legacy reward route found. | Exists in equipment logic and dragon-breath mitigation. Needs a design decision before exposing it. |
| Full helm | `Large Dragon Helmet` / MyWorld `Dragon Helmet` `1425` | Hidden unique drop from Black Demons. | No quest route found. | MyWorld renames this to `Dragon Helmet`, so this appears to be the current "full helm" slot. |
| Medium helm | `Dragon medium Helmet` `795` | Hidden unique drop from Black Demons. Also appears in holiday/event reward tables such as Present and Halloween Cracker. | Authentic rare/event lineage exists. | Tests explicitly keep it out of KBD direct drops. |
| Helm | No separate third dragon helm found. | `Dragon Helmet` appears to be the MyWorld name for item `1425`. | N/A | Treat `helm` as an alias question until a distinct item is added. |
| Shield | No plain `Dragon Shield` item found. | Closest routes are `Anti dragon breath Shield` from the Duke/Dragon Slayer flow, and `Dragon Square Shield` via shield halves. | Anti-dragon shield is authentic Dragon Slayer utility. | Keep anti-dragon shield separate from dragon armor tiering. |
| Kite shield | Constant `DRAGON_KITE_SHIELD` `1426`, displayed as `Dragon Paladin Shield`. | Smith from right and left dragon shield halves on an anvil with a hammer at 60 Smithing, choosing the paladin/kite output. | No separate drop route found. | Code calls it kite shield; player-facing name is paladin shield. |
| Square shield | `Dragon Square Shield` `1278` | Smith from right and left dragon shield halves on an anvil with a hammer at 60 Smithing. | Classic shield-half route. | Right half is sold by Siegfried Erkle. Left half is on the mega rare drop table. Direct hidden KBD/Black Dragon square shield drops only exist if OpenPK points are enabled, which MyWorld disables. |
| Paladin shield | `Dragon Paladin Shield` `1426` | Same shield-half smithing route as the kite shield entry. | No separate route found. | This is the current player-facing name for `DRAGON_KITE_SHIELD`. |
| Gloves / gauntlets | Dragonhide glove family: baby `1886`, blue `1921`, green/earth `1926`, red `1931`, black `1941`, elder green `1951`. | Crafted from matching tanned dragon leather with thread. Also available as fishing special rewards. | No metal dragon gauntlet route found. | No item named dragon gauntlets was found. |
| Boots / greaves | Dragonhide boot family: baby `1887`, blue `1922`, green/earth `1927`, red `1932`, black `1942`, elder green `1952`. | Crafted from matching tanned dragon leather with thread. Also available as fishing special rewards. | No metal dragon greaves route found. | Metal greaves exist for other metal tiers, but no dragon greaves were found. |

## Material Routes

### Wayne's Dragon Armor

Source:

- `server/plugins/com/openrsc/server/plugins/authentic/npcs/falador/WaynesChains.java`
- `server/plugins/com/openrsc/server/plugins/custom/minigames/DwarfRescue.java`
- `server/plugins/com/openrsc/server/plugins/authentic/itemactions/InvAction.java`

Unlock:

- Player must have `miniquest_dwarf_youth_rescue == 2`.

Wayne options:

| Output | Cost |
| --- | --- |
| `Dragon Scale Mail Body` | `500 Dragon Metal Chains`, `150 Chipped Dragon Scales`, `500,000 coins` |
| `Dragon Scale Mail Legs` | `500 Dragon Metal Chains`, `100 Chipped Dragon Scales`, `500,000 coins` |
| `Dragon Plate Mail Body` | `4 Dragon bars`, `500,000 coins` |

Dragon material setup:

- `Raw dragon metal` comes from the KBD custom rare table.
- At the lava forge, raw dragon metal requires Dwarf Youth Rescue completion
  and 80 Smithing.
- One raw dragon metal can become either `1 Dragon bar` or
  `50 Dragon Metal Chains`.
- `King Black Dragon scale` comes from the KBD custom rare table.
- Using a chisel on a KBD scale requires 90 Crafting and produces
  `5 Chipped Dragon Scales`.

### Dragon Shield Halves

Source:

- `server/plugins/com/openrsc/server/plugins/authentic/skills/smithing/Smithing.java`
- `server/plugins/com/openrsc/server/plugins/authentic/quests/members/legendsquest/npcs/shop/SiegfriedErkel.java`
- `server/src/com/openrsc/server/constants/NpcDrops.java`

Route:

- Right half: sold by Siegfried Erkle in the Legends Guild shop. Also appears
  in the Present ultra-rare table.
- Left half: mega rare drop table. Also appears in the Present ultra-rare
  table.
- Smithing: use the halves on an anvil with a hammer at 60 Smithing.
- Output choice: `Dragon Square Shield` or `Dragon Paladin Shield`.

Current caveat:

- `Dragon Square Shield` has a Legends Quest equip gate unless quest item
  restrictions are disabled.
- Hidden direct square shield drops from Black Dragons and KBD are behind
  `want_openpk_points`; MyWorld and hosted MyWorld set this false.

### Dragonhide Gloves And Boots

Source:

- `server/src/com/openrsc/server/constants/NpcDrops.java`
- `server/plugins/com/openrsc/server/plugins/custom/skills/crafting/TanningRack.java`
- `server/plugins/com/openrsc/server/plugins/authentic/skills/crafting/Crafting.java`
- `server/plugins/com/openrsc/server/plugins/authentic/skills/fishing/Fishing.java`

Route:

- Matching dragons drop hides as guaranteed drops.
- Tanning rack converts hides into matching leather.
- Crafting uses the hide armor production path with thread.
- Gloves and boots cost `2` matching leather each.
- Crafting levels are tier-based:
  - baby dragon gloves/boots: 23 Crafting
  - blue dragon gloves/boots: 47 Crafting
  - green/earth dragon gloves/boots: 47 Crafting
  - red dragon gloves/boots: 55 Crafting
  - black dragon gloves/boots: 63 Crafting
  - elder green dragon gloves/boots: 81 Crafting
- Fishing special rewards can also award matching dragonhide gloves and boots.

## Combat Odyssey Legacy

Source:

- `server/plugins/com/openrsc/server/plugins/custom/minigames/CombatOdyssey.java`
- `server/conf/server/defs/extras/CombatOdyssey.json`
- `server/plugins/com/openrsc/server/plugins/authentic/quests/members/legendsquest/npcs/LegendsQuestSirRadimusErkle.java`
- `server/plugins/com/openrsc/server/plugins/authentic/defaults/Ladders.java`

Current start flow:

1. Player talks to Sir Radimus Erkle in the Legends Guild.
2. Radimus offers "a combat odyssey" and sends the player upstairs to
   Siegfried.
3. The upstairs Legends Guild ladder flow gives the player `Biggum Flodrot`.
4. Biggum tells the player to start with the goblin generals in north
   Fallington.
5. Combat Odyssey uses Biggum as the tracking item and progresses through
   tiered kill assignments.

Current final reward:

- On tier 13 completion, Radimus awards `Dragon Plate Mail Legs`.
- This is now the restored dragon plate legs acquisition route.
- Biggum receives completion dialogue and prestige handling.
- The quest can be repeated after completion.

Legacy final reward:

- Commit `3a6787542 The Odyssey` awarded a final choice:
  - `Dragon Plate Mail Legs`
  - `Dragon Plated Skirt`
- Commit `837de7256 Balance dragon gear and summoning fixes` removed the
  choice and made the reward always `Dragon Plate Mail Legs`.

Quest scale:

- This is not a small leftover thread. It is a large combat progression system
  with many tiered kill tasks, multiple NPC masters, custom Biggum dialogue,
  repeat/prestige behavior, and a final KBD kill.
- The work should be preserved, but the final reward should be revisited now
  that skirts are retired and Wayne also produces dragon plate legs.

## Follow-Up Design Questions

- Should Combat Odyssey's kill requirements be reduced or streamlined so the
  plate legs route is less grindy while still feeling legendary?
- Should Combat Odyssey eventually award a dragon armor voucher/claim choice
  that excludes skirts, or should plate legs remain its fixed reward?
- Should `Dragon Plate Mail Top` and `Dragon Scale Mail Top` stay hidden
  support items, or should they be removed from any player-facing language?
- Should `Dragon Plated Skirt` be fully renamed in base custom item defs to
  prevent future confusion, since MyWorld already treats item `1430` as
  `Dragon Scale Mail Legs`?
- Should dragon helms remain hidden unique Black Demon drops, or should one
  helm move into a quest/crafting reward path?
- Should the shield-half path be documented in a guide, since it now produces
  both square and paladin shield outputs?
- Should dragonhide gloves and boots be considered part of "dragon armor" in
  player-facing guides, or kept separate as ranged/leather armor?

## Recommended Next Step

Use this audit to build a second implementation plan focused on Combat Odyssey
reward modernization. The safest direction is to keep the quest line, keep
skirts retired, and replace the old skirt choice with a reward structure that
fits current dragon armor progression.
