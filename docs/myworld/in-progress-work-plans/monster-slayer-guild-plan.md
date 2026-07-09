# Monster Slayer Guild Plan

Status: in-progress design
Owner: An-actual-duck
Branch: no active branch; assign the next checklist item to a fresh topic branch in a neutral AI slot
Related docs:
- `docs/myworld/rough-drafts/slayer-guild-rough-draft-plan.md`
- `docs/myworld/in-progress-work-plans/how-to-acquire-dragon-armor.md`
- `docs/myworld/in-progress-work-plans/dragon-gear-crafting-plan.md`

## Summary

Create the `Monster Slayer` guild as a rank-and-points combat progression
system built from the existing Combat Odyssey monster task foundation.

This should not become a normal visible skill. It should behave more like
guild standing: players earn ranks by completing monster kill tasks for guild
sects around the world, then spend Monster Slayer points in rank-gated shops.
The current Combat Odyssey dragon plate legs route should be absorbed into this
system as monster-hunting progression content so the work is still preserved,
but finished dragon equipment rewards now belong to the dragon gear crafting
plan.
The guild should no longer ask the player to clear more than 40,000 mandatory
kills in one isolated quest chain before its reward structure opens up.
Until the Monster Slayer replacement exists, the current Legends Guild task
provider for that old dragon-leg path can be hidden so players are not sent
into a route whose reward ownership is being moved.

## Goal

The current dragon plate legs route has useful content behind it, but its
current scale is too front-loaded. The redesign should:

- Preserve the monster task system and personality of Combat Odyssey.
- Reduce the mandatory kill wall needed to unlock all guild shops.
- Keep the total long-term grind meaningful through optional repeatable tasks
  and expensive point-shop rewards.
- Spread rewards across the player journey instead of only at the end.
- Preserve the old dragon plate legs quest work while moving dragon gear itself
  into the repaired lava forge and Smithing route.
- Replace dragon-armor rewards with completely unique Monster Slayer rewards.

## Core Decisions

- Official name: `Monster Slayer` guild.
- This is not a `Slayer` skill and should not add a new skill guide entry or XP
  curve.
- Progression uses guild rank, task completion, and Monster Slayer points.
- Tasks are kill tasks only. The earlier rough-draft idea of turning in monster
  drops or certs is no longer the intended first implementation.
- Each guild sect has deterministic mandatory tasks the first time the player
  progresses through that rank band.
- After a sect's mandatory tasks are complete, that sect can offer repeatable
  random tasks for points.
- Higher-rank task givers refuse to work with the player until the player's
  current rank is high enough.
- The player's rank is represented in flavor as a stamp on the back of their
  hand, like a child's access stamp at a theme park.
- The first task is intentionally silly: get the Falador pub task giver a beer.
  Returning with a beer earns the player the `Fledgling` stamp.

## Non-Goals

- Do not implement a new combat skill.
- Do not require monster drop turn-ins for first-pass rank progression.
- Do not reintroduce dragon skirts.
- Do not remove Combat Odyssey code blindly. Reuse or migrate useful task,
  progress, dialogue, and reward pieces.
- Do not use Monster Slayer as the acquisition route for any finished dragon
  equipment while the dragon gear crafting plan owns that tier.

## Rank Structure

Ranks are earned in order. Each location represents the task giver responsible
for advancing the player into the next rank.

| Current state | Next rank | Location | Role |
| --- | --- | --- | --- |
| Unstamped | Fledgling | Falador pub | Intro beer task and guild pitch |
| Fledgling | Initiate | Falador pub | First real local monster tasks |
| Initiate | Veteran | Port Sarim pub | Coastal, dungeon, and travel-adjacent tasks |
| Veteran | Elite | Brimhaven pub | Karamja, jungle, volcano, and harder wilderness-style tasks |
| Elite | Champion | Champions Guild | Formal recognition by a combat guild |
| Champion | Hero | Heroes Guild | High-end heroic monster assignments |
| Hero | Legend | Legends Guild | Top-end legendary assignments and prestige shop unlock |

The location order matters. A player can talk to higher-rank contacts early,
but those contacts should respond with flavor that they do not recognize the
stamp yet.

## Intro Flow

The first contact should be in the Falador pub.

Opening idea:

1. The player asks about the Monster Slayer guild.
2. The task giver says they are not stamping anyone who cannot handle basic
   errands.
3. The player receives the first task: get a beer.
4. The player buys or already has a beer and returns it.
5. The task giver drinks it, stamps the back of the player's hand, and grants
   `Fledgling` status.
6. The real kill-task system becomes available.

This keeps the guild tone grounded and a little silly before the heavier grind
begins.

## Mandatory Task Philosophy

The mandatory path should be meaningfully long, but much shorter than the
current Combat Odyssey total. The target is:

- Mandatory unlock path: substantial, but not a 40,000 kill wall.
- Full reward grind: can approach the old total after repeatable tasks and
  point costs are included.

Recommended initial tuning target:

| Rank band | Approximate mandatory kill budget | Notes |
| --- | ---: | --- |
| Fledgling to Initiate | 250 to 500 | Teaches task flow with safe monsters |
| Initiate to Veteran | 700 to 1,200 | Early travel and dungeon comfort |
| Veteran to Elite | 1,500 to 2,500 | Midgame monsters and stronger zones |
| Elite to Champion | 2,000 to 3,500 | Guild-worthy combat proving ground |
| Champion to Hero | 3,000 to 5,000 | High-end but not legendary yet |
| Hero to Legend | 4,000 to 7,000 | Capstone sequence using legacy Odyssey pieces |

This puts the full shop-unlock path roughly in the `11,000` to `19,700` kill
range before tuning. The top rewards can still require enough points that a
player chasing every major reward may do a total amount of hunting closer to
the old Combat Odyssey scale, but the player sees ranks, shops, and rewards
along the way.

## Mandatory Task Structure

Each contact has a fixed set of mandatory tasks used for rank advancement.
These should be authored in data, not hard-coded into one dialogue file.

Suggested task-pack shape:

| Sect | Mandatory task families | Design note |
| --- | --- | --- |
| Falador pub | goblins, rats, cows, dwarves, dark wizards | Basic local threats and starter combat familiarity |
| Port Sarim pub | pirates, muggers, skeletons, zombies, hobgoblins | Travel-adjacent enemies and early dungeon comfort |
| Brimhaven pub | jungle spiders, scorpions, moss giants, lesser demons, Karamja volcano enemies | Midgame danger and Karamja identity |
| Champions Guild | hill giants, dark warriors, black knights, ogres, greater demons | Formal combat reputation |
| Heroes Guild | dragons, demons, shadow warriors, paladins, elemental or magical threats | Hero-tier monsters and mixed combat pressure |
| Legends Guild | black demons, black dragons, high-tier dragons, KBD-adjacent tasks, legacy Odyssey capstone tasks | Legendary tier and prestige shop unlock |

Exact monster families and counts need an implementation audit against current
NPC IDs, spawn density, and travel friction.

## Repeatable Random Tasks

Once a contact's mandatory chain is complete:

- The contact unlocks a repeatable random task pool.
- Tasks are drawn from that contact's rank-appropriate monster families.
- Completing repeatable tasks grants Monster Slayer points.
- Repeatables do not grant additional rank once that contact's rank milestone
  is complete.
- Higher contacts should generally award more points per task because their
  targets are harder, slower, or more dangerous.

Repeatable random tasks are how the player continues earning points for shop
items after all mandatory ranks are unlocked.

Recommended rules:

- One active Monster Slayer task at a time.
- The player can ask which task is active and current progress.
- The player can cancel a random repeatable task, but not a mandatory rank task
  without a deliberate owner-approved design reason.
- Random task rerolls should either be limited, cost points, or unlock at a
  later rank.
- Kill credit should use the same contribution rules as other PvM systems so
  party play remains viable without making tag-only credit too generous.

## Point Shops

Monster Slayer points should be global, but shops are rank-gated by sect.
Unlocking a higher sect does not invalidate earlier shops.

Each shop should primarily contain items the player cannot typically get
elsewhere. These shops should not become generic food, potion, or equipment
vendors. If a shop sells consumables, they should be guild-specific variants,
unusual mixes, or convenience items that feel like Monster Slayer rewards rather
than normal store stock.

Expected shop progression:

| Rank/shop | Reward direction |
| --- | --- |
| Fledgling/Falador | Low-tier guild consumables, novelty trophies, minor combat conveniences |
| Initiate/Port Sarim | Early unique weapons or tools, travel-adjacent slayer utility |
| Veteran/Brimhaven | Midgame guild food/potions, jungle and poison utility, first notable unique gear |
| Elite/Champions Guild | Stronger unique weapons, guild cosmetics, task conveniences |
| Champion/Heroes Guild | High-end guild consumables, special contracts, stronger utility rewards |
| Hero/Legends Guild | Top-end unique gear, special contracts, prestige rewards |

Dragon gear relationship:

- The dragon gear crafting plan now owns dragon plate legs and dragon scale
  mail legs.
- The dragon gear crafting plan should cover all finished dragon equipment
  acquisition routes.
- Monster Slayer should not sell finished dragon equipment as its top-end
  reward in the first implementation of this guild plan.
- Monster Slayer may still reward dragon-adjacent prestige items, cosmetics,
  contracts, or utility, but finished dragon equipment should come from the
  repaired lava forge and normal anvil Smithing route.
- Dragon skirts stay retired and should not return as shop items.

The Legends shop should require `Legend` rank before the player can buy its
top-end Monster Slayer rewards. The price should be high enough that unlocking
the shop and affording its best items are separate achievements.

Reward examples and direction:

- Guild-specific potions that are useful but not direct replacements for
  Herblaw progression.
- Guild-specific cooked foods or monster-hunter rations that sit between normal
  Cooking tiers or add small utility.
- Lower-rank novelty gear with real use but clear limitations.
- A `Giant's Axe` style reward: a slow, heavy, hard-hitting two-handed weapon
  suited to a mid-rank or Champions Guild shop.
- Higher-rank unique combat tools that feel earned through monster hunting
  rather than crafting or normal drops.
- Cosmetic trophies, titles, or guild-marked gear for players who want prestige
  rewards after buying the practical items.

Shop tiering rule:

- Earlier shops should give interesting sidegrades, utility, and flavor.
- Middle shops can offer stronger niche gear and unusual consumables.
- High shops should offer serious but expensive progression rewards.
- Legends shop should hold future top-end Monster Slayer prestige gear, not
  finished dragon equipment.

## Temporary Legacy Provider Handling

The current Legends Guild task provider for the old Combat Odyssey dragon-leg
route can be hidden while this plan is not implemented.

Intent:

- Do not delete the existing Combat Odyssey task code or progress data.
- Do not send new players into the old dragon-leg reward path while dragon
  equipment is being moved into the repaired lava forge and anvil route.
- Preserve existing player progress so it can be migrated, honored, or
  converted when Monster Slayer is implemented.
- Bring the Legends Guild task provider back only after it offers Monster
  Slayer rank, tasks, contracts, points, or unique non-dragon-equipment rewards.

## Combat Odyssey Migration

Combat Odyssey should be treated as source material, not discarded.

Keep or adapt:

- Monster family task definitions.
- Task progress storage.
- Kill-credit handling.
- NPC dialogue structure.
- Biggum Flodrot's personality and oddball flavor.
- The idea of a final legendary combat proving sequence.

Change direction:

- The 101-task, 40,906-kill chain should no longer be the mandatory direct path
  to dragon plate legs.
- Combat Odyssey should become part of the Monster Slayer Guild's high-rank
  ecosystem, likely as:
  - a Legends Guild capstone chain,
  - a repeatable prestige challenge,
  - Biggum's personal optional contract list,
  - or a high-rank random/special task source.
- Sir Radimus Erkle, Siegfried Erkle, and Biggum Flodrot can remain relevant,
  but their reward role should point into the Monster Slayer rank/shop system.

Migration considerations:

- Players who already completed Combat Odyssey should not lose access to any
  reward path they already earned.
- Completed Combat Odyssey should likely grant at least meaningful rank credit
  or a large Monster Slayer point credit when the system launches.
- Partial Combat Odyssey progress should either convert into rank progress or
  remain as legacy/prestige progress, depending on implementation complexity.

## Dragon Armor Route Relationship

This plan no longer owns finished dragon equipment acquisition routes. The
active dragon gear direction lives in `dragon-gear-crafting-plan.md`.

Current relationship:

- Combat Odyssey content should migrate into Monster Slayer as rank, tasks,
  points, contracts, or prestige content.
- Dragon plate legs and dragon scale mail legs should move to the repaired lava
  forge and normal anvil Smithing route.
- All finished dragon equipment should be owned by the dragon gear crafting
  plan unless that plan explicitly keeps a special route.
- Monster Slayer shops need new top-end unique rewards that are not finished
  dragon equipment.
- Dragon plated skirt remains retired.

## Player State

Likely state to track:

- Monster Slayer rank.
- Monster Slayer points.
- Active task contact/sect.
- Active task family.
- Active task required kill count.
- Active task current kill count.
- Mandatory task index per sect.
- Completed mandatory sect milestones.
- Optional repeatable task stats if useful for future prestige.

The hand stamp is flavor, but it should map cleanly to the rank field. Dialogue
can describe the stamp changing as the player ranks up.

## Data Model Direction

Prefer data-driven definitions so the system is easier to tune:

- `rank`: required rank to receive this task.
- `contact`: Falador, Port Sarim, Brimhaven, Champions, Heroes, Legends.
- `taskType`: mandatory or repeatable.
- `monsterFamily`: display name and list of NPC IDs.
- `requiredKills`: integer.
- `pointReward`: integer.
- `nextMandatoryTask`: chain ordering.
- `unlockRankOnCompletion`: optional rank award.
- `weight`: random repeatable selection weight.

Existing Combat Odyssey data should be reviewed before creating a new format.
If it already covers most of this, extend it rather than making a parallel
system.

## Dialogue Requirements

Every task giver needs:

- Intro dialogue.
- Rank-too-low dialogue.
- Current task reminder.
- Mandatory task assignment dialogue.
- Mandatory task completion dialogue.
- Rank-up dialogue with stamp flavor.
- Repeatable task assignment dialogue.
- Repeatable task completion dialogue.
- Point shop access dialogue.
- No-task/come-back-later fallback dialogue.

Tone:

- Pub contacts can be informal, practical, and a little dismissive.
- Major guild contacts should sound more formal as ranks increase.
- Biggum can remain strange and personal.
- The hand-stamp joke should recur, but not become long or intrusive.

## Implementation Checklist

- [ ] Audit Combat Odyssey task definitions, player cache keys, and kill-credit
      code.
- [ ] Decide whether to extend Combat Odyssey data or create Monster Slayer
      data alongside it.
- [ ] Add Monster Slayer rank and point state.
- [ ] Add contact eligibility checks by rank.
- [ ] Add the Falador beer intro task and `Fledgling` stamp.
- [ ] Add mandatory task chains for each sect.
- [ ] Add repeatable random task pools for completed sects.
- [ ] Add Monster Slayer point rewards.
- [ ] Add rank-gated point shops with unique, appropriately tiered rewards.
- [ ] Replace the old direct dragon-leg reward role with Monster Slayer rank,
      points, contracts, or prestige rewards.
- [ ] Hide the current Legends Guild task provider for the old dragon-leg path
      until the Monster Slayer replacement exists.
- [ ] Design new Legends shop rewards that are not finished dragon equipment.
- [ ] Add guide/help text for Monster Slayer rank, tasks, and shops.
- [ ] Add migration handling for completed or partial Combat Odyssey progress.

## Testing Checklist

- [ ] New player can complete the beer task and receive `Fledgling`.
- [ ] Each rank contact refuses players below the required rank.
- [ ] Mandatory tasks progress only from eligible kills.
- [ ] Mandatory task completion awards points and the correct next rank.
- [ ] Repeatable random tasks unlock only after that contact's mandatory chain.
- [ ] Repeatable tasks grant points but do not skip rank gates.
- [ ] Shop stock is correctly gated by rank.
- [ ] Legends shop top-end rewards are gated behind `Legend`.
- [ ] Finished dragon equipment is not sold by Monster Slayer shops unless the
      dragon crafting plan changes again.
- [ ] The old Legends Guild dragon-leg task provider is hidden while its
      replacement reward structure is incomplete.
- [ ] Retired dragon skirt items do not appear in dialogue, shops, rewards, or
      guides.
- [ ] Existing Combat Odyssey completion state migrates or remains honored.
- [ ] Party/multi-contributor kills grant credit according to intended PvM
      contribution rules.

## Open Questions

- What should the exact mandatory task counts be for each rank band?
- Should Monster Slayer points be awarded per task only, per kill, or both?
- Should repeatable random tasks have a cancel/reroll cost?
- What unique consumables and gear should each rank shop contain?
- Where should a `Giant's Axe` style heavy two-handed reward sit in the shop
  ladder?
- Which existing NPCs should serve as the pub task givers?
- Should Champions, Heroes, and Legends task givers be existing guild leaders
  or new Monster Slayer representatives inside those guilds?
- Should Biggum become a Legends-only special task giver, a prestige contact,
  or remain tied to a preserved Combat Odyssey side path?
- How should partial Combat Odyssey progress convert?
- Should completed Combat Odyssey grant `Legend` rank directly or grant a large
  point/rank credit?
- Should point shops sell only unique rewards and supplies, or also task
  convenience unlocks like rerolls and task blocks?
- What unique non-dragon-equipment reward should become the Legends shop chase
  reward?

## Completion Criteria

This plan is ready to move to completed when:

- Monster Slayer rank and points are implemented.
- All six location-based progression steps exist.
- Mandatory task chains unlock every rank through `Legend`.
- Repeatable random tasks work after mandatory chains.
- Rank-gated point shops work at each sect.
- The old Combat Odyssey dragon-leg reward role has been replaced with
  Monster Slayer rank, points, contracts, or unique prestige rewards.
- Monster Slayer rewards no longer conflict with the dragon gear crafting plan.
- Combat Odyssey's existing progress and personality are preserved or migrated
  intentionally.
- Tests cover rank gates, task progress, shop gates, and dragon reward routes.
