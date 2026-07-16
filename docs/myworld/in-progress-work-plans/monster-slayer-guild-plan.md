# Monster Slayer Guild Plan

Status: **foundation audit complete; implementation-ready design pending the
explicit reward decisions below**
Owner: An-actual-duck
Audit baseline: published `main` `4be5b9fc5` on 2026-07-16
Audit branch: `docs/monster-slayer-foundation-audit`

Related active plans:

- `docs/myworld/in-progress-work-plans/how-to-acquire-dragon-armor.md`
- `docs/myworld/in-progress-work-plans/dragon-gear-crafting-plan.md`

Source policy:

- This is the only official Monster Slayer implementation plan.
- `docs/myworld/rough-drafts/slayer-guild-rough-draft-plan.md` is superseded
  historical context, not an implementation source.
- Do not restore the rough draft's monster-drop/certificate turn-ins.
- Do not restore its dragon plate-leg, dragon-skirt, or other finished dragon
  armor rewards. Finished dragon equipment remains owned by the dragon gear
  crafting plan.

## Product Contract

Monster Slayer is a distributed guild-standing system built from the useful
parts of Combat Odyssey. It is not a visible skill and does not award Slayer
XP. Players advance through seven named ranks, complete deterministic kill-only
task chains at six contacts, and then use those contacts for repeatable tasks
and rank-gated point shops.

The intended tone and progression remain:

- An unstamped player brings a beer to the Falador pub contact and receives a
  deliberately silly `Fledgling` hand stamp.
- Six fixed task chains advance the player through `Initiate`, `Veteran`,
  `Elite`, `Champion`, `Hero`, and `Legend`.
- Completed contacts offer repeatable random kill tasks for global Monster
  Slayer points.
- Biggum Flodrot's personality and the idea of a legendary capstone survive,
  but the 101-task Odyssey is no longer a required 40,906-kill reward wall.
- Unique shop rewards, not finished dragon equipment, provide the long-term
  point grind.

Core rules:

- One active Monster Slayer task across all contacts.
- The beer is a one-time introduction, not a repeatable material turn-in.
- Mandatory chains are fixed and cannot be cancelled or rerolled.
- Repeatables use rank-appropriate family pools and award points without
  advancing rank.
- Points are awarded on task completion, not per kill. This bounds cache writes,
  avoids partial-task farming, and makes the economy auditable.
- Higher contacts refuse assignment/shop access until the required rank and the
  host guild's normal access requirements are satisfied.
- Existing Combat Odyssey state is migrated once without deleting its keys.

Non-goals:

- No new skill, XP curve, skill-guide entry, material proof, certificates, or
  trade-in ledger.
- No dragon plate mail legs, dragon scale mail legs, dragon skirts, or other
  finished dragon equipment in tasks, shops, migration grants, or capstones.
- No broad rewrite of NPC death, loot, XP, quest kill triggers, or party combat
  in the foundation branch.
- No automatic conversion of the old JSON's item rewards into shop stock.

## Evidence-Backed Combat Odyssey Audit

### Activation And Runtime Owners

MyWorld currently sets `want_combat_odyssey: true` in `server/myworld.conf`.
`World` constructs `CombatOdysseyData` and loads its JSON only when that flag is
enabled. New starts are nevertheless intentionally hidden:
`LegendsQuestSirRadimusErkle.doCombatOdyssey` returns `false` for
`NOT_STARTED`, while partial runs, reward claims, and prestige repeats remain
reachable. This is active compatibility code, not dead code.

The maintained implementation is split across:

- `server/conf/server/defs/extras/CombatOdyssey.json`: tier/task/reward data.
- `server/src/com/openrsc/server/content/minigame/combatodyssey/`: the JSON
  loader and positional `Tier`/`Task` models.
- `server/plugins/com/openrsc/server/plugins/custom/minigames/CombatOdyssey.java`:
  intro, Biggum, active task state, final-blow kill tracking, random task order,
  final reward, prestige, and developer controls.
- Eight authentic quest/NPC dialogue owners plus the Legends ladder, Biggum
  visibility, and player-stat display integrations inventoried below.

Do not delete or switch off those paths until Monster Slayer activation has
migrated live state and supplied replacement dialogue behavior.

### JSON Inventory And Semantics

Repository validation of `CombatOdyssey.json` produced:

| Measure | Result |
| --- | ---: |
| Tiers | 14 (`0` through `13`) |
| Tasks | 101 |
| Required kills | 40,906 |
| Unique referenced NPC IDs | 192 |
| Missing NPC definitions | 0 |
| Maximum tasks in one tier | 20 |
| Referenced unattackable NPC IDs | 2 (`375`, `376`) |
| Referenced IDs with no active MyWorld static spawn | 6 |
| Families with no active static target at all | 0 |

The active tier path is:

| Tier | Master/contact and active location | Tasks | Kills | Reward stored on this tier |
| ---: | --- | ---: | ---: | --- |
| 0 | General Wartface `151`, Goblin Village `324,447` | 20 | 8,170 | none |
| 1 | General Wartface `151` | 1 | 500 | 20 stat-restoration certs, 200 giant-carp certs, strength amulet, medium rune helmet |
| 2 | Thormac `300`, `511,1452` | 14 | 5,525 | none |
| 3 | Grew `681`, `663,759` | 19 | 7,600 | rune square shield |
| 4 | Dark Mage `667`, `665,567` | 12 | 5,550 | rune battle axe, 20 cure-poison certs |
| 5 | Dark Mage `667` | 1 | 100 | 200 blood runes |
| 6 | Hazelmere `546`, `532,754` | 13 | 5,800 | 200 lava-eel certs, power amulet, rune helmet |
| 7 | Hazelmere `546` | 1 | 500 | 20 poison-antidote certs, rune paladin shield |
| 8 | Sigbert `573`, `584,3575` | 8 | 3,550 | none |
| 9 | Achetties `253`, Heroes Guild `372,443` | 5 | 860 | 100 prayer-potion certs, charged dragonstone amulet, rune two-handed sword |
| 10 | Sir Radimus `785`, Legends Guild `514,535` | 3 | 1,250 | 100 manta-ray certs, 100 sea-turtle certs, dragonstone ring, 20 each super attack/strength/defense certs |
| 11 | Sir Radimus `785` | 2 | 1,000 | none |
| 12 | Sir Radimus `785` | 1 | 500 | none |
| 13 | Sir Radimus `785` | 1 | 1 | none |

Important implementation facts:

- `CombatOdysseyData.load` assigns each task's ID from its array position.
  `getTier(int)` also indexes the tier list instead of looking up `tierId`.
  Reordering either array silently changes persisted meaning.
- `tierId` is loaded into `Tier` but has no getter and is not used as a lookup
  identity. The new system must not inherit this positional contract.
- Every task contains `taskInfoDialog`, but the loader and `Task` discard it.
  Only `monsterInfoDialog` survives.
- A tier's `rewards` are actually granted after the preceding tier completes:
  dialogue first calls `assignNewTier`, then `giveRewards`. Treating the field
  as a completion reward without understanding this order would shift rewards.
- Mandatory tasks within a tier are random, not deterministic. A bit in the
  current tier mask prevents repeats until all tasks in that tier are done.
- The final reward is not in JSON. `CombatOdyssey.radimusDialog` directly gives
  `DRAGON_PLATE_MAIL_LEGS` (`1429`), removes active keys, and then increments
  prestige. That reward path is excluded from Monster Slayer.
- The old intermediate supplies and equipment are balance evidence only. None
  automatically become Monster Slayer shop stock.

### Legacy Cache And Completion State

Combat Odyssey uses three player-cache keys and an inventory/bank item:

| State | Type | Current meaning and lifecycle |
| --- | --- | --- |
| `combat_odyssey` | String | Overloaded. Missing/`"0"` is not started, `"1"` is Radimus accepted, `"2"` is Biggum met, and `"tier:task:kills"` is an active run. Colon parsing is positional. |
| `co_tier_progress` | Long | Bit mask for completed tasks in only the current tier. It resets on every new tier and is removed after the final reward. |
| `co_prestige` | Integer | Number of final rewards claimed/full Odyssey completions. It is the only durable completed-state marker after active keys are removed. |
| Biggum item `BIGGUM_FLODROT` | item | Companion/tracker may be in inventory or bank. The courtyard NPC `826` at `511,544` is visible only to a prestiged player not carrying/banking the item. |

Consequences for migration:

- `co_prestige > 0` is authoritative proof of at least one claimed completion.
- A valid tier-13 state with its KBD task complete and no prestige is a
  completed-but-unclaimed Odyssey. It must be honored without granting dragon
  armor.
- A tier number proves every lower tier was completed, because tier assignment
  is gated by `isTierCompleted`; the current tier mask plus active kill count
  provides the remaining partial evidence.
- Item possession is not completion evidence and must not be used to infer a
  reward claim.
- The final legacy flow removes active keys before giving the item and only then
  increments prestige. A crash in that window could leave ambiguous state; the
  migration must fail closed rather than infer completion from item ownership.
- `isTierCompleted` uses exact mask equality. Unknown high bits and malformed
  strings should be diagnosed and quarantined, not normalized silently.

The cache already persists typed primitive values through `Cache`,
`GameDatabase.querySavePlayerCache`, and `PlayerService.loadPlayerCache`; no
database schema change is required for Monster Slayer.

### Current Kill Credit

`Npc.killedBy` currently calls the generic `KillNpcTrigger` before
`handleXpDistribution` selects the top-damage owner. `CombatOdyssey.onKillNpc`
therefore receives the player associated with the killing blow (or the owner of
a summon that delivered it), not every contributor and not necessarily the
top-damage player. It increments exactly one player's active count.

Later in the same death path, `Npc` already aggregates melee, ranged, magic,
and summon-owner damage. Its personal-loot path accepts online, living players
with positive damage and applies a contribution scale with a `0.05` floor.
Those maps are private and cleared after XP/drop processing.

Monster Slayer should not change `KillNpcTrigger`, because many quests depend
on its existing final-blow semantics. Its later kill-credit branch should add a
focused contribution snapshot/hook at the NPC-death layer and use this rule:

- Aggregate melee, ranged, magic, and owned-summon damage by player UUID.
- Credit each online, living contributor still within 16 tiles who dealt at
  least `max(1, ceil(npcMaxHits * 0.05))` damage; always credit the top-damage
  contributor when their damage is positive.
- Credit a player at most once per NPC death even if several damage styles or a
  summon contributed.
- Match only the active task's validated family IDs.
- Do not grant points per kill; only advance the bounded active count.

The five-percent recommendation aligns with the existing personal-loot scale
floor while preventing a one-hit tag on larger monsters. It is a Monster
Slayer rule, not authorization to change XP, loot, or existing quest credit.

### Dialogue And Compatibility Integrations

| Current integration | Active Odyssey responsibility |
| --- | --- |
| `GoblinDiplomacy` generals | Start tier 0 after Biggum, advance `0 -> 1`, grant tier-1 rewards. |
| `ScorpionCatcher` / Thormac | Advance `1 -> 2`. |
| `WatchTowerDialogues` / Grew | Advance `2 -> 3`, grant tier-3 rewards. |
| `npcs/ardougne/west/DarkMage` | Advance `3 -> 4 -> 5`, grant both entry reward sets. |
| `GrandTree` / Hazelmere | Advance `5 -> 6 -> 7`, require the translation item, grant both entry reward sets. |
| `SigbertTheAdventurer` | Advance `7 -> 8`. |
| `HerosQuest` / Achetties | Advance `8 -> 9`, grant tier-9 rewards. |
| `LegendsQuestSirRadimusErkle` | Hide new starts, recover Biggum, advance `9 -> 10 -> 11 -> 12 -> 13`, grant tier-10 rewards, and claim the final reward. |
| `Ladders` | Automatically introduces/recovers Biggum on the Legends Guild upper-floor ladder. Radimus says to see Siegfried, but no Siegfried Odyssey dialogue starts the tasks. |
| `Npc.isInvisibleTo` | Hides/shows courtyard Biggum from prestige and carried/banked item state. |
| `RegularPlayer` stat display | Shows `co_prestige` as Odyssey completions. |

These integrations are maintained compatibility boundaries until activation.
The Monster Slayer foundation must not edit them. A later cutover branch must
disable legacy advancement as one coordinated change, keep the old keys
readable, and replace rather than stack dialogue routes.

## Target Rank And Contact Design

Stable rank codes are part of the save contract and must never be reordered:

| Code | Rank | Advancement contact |
| ---: | --- | --- |
| 0 | Unstamped | Falador introduction |
| 1 | Fledgling | Beer completed; Falador mandatory chain available |
| 2 | Initiate | Falador chain complete; Port Sarim available |
| 3 | Veteran | Port Sarim chain complete; Brimhaven available |
| 4 | Elite | Brimhaven chain complete; Champions Guild available |
| 5 | Champion | Champions chain complete; Heroes Guild available |
| 6 | Hero | Heroes chain complete; Legends Guild available |
| 7 | Legend | Legends chain complete; all rank shops available |

Candidate contacts are existing, actively spawned NPCs; no new placement is
needed:

| Contact key | Candidate and exact active location | Existing owner | Integration boundary |
| --- | --- | --- | --- |
| `falador` | Barmaid `142`, Rising Sun ground floor `321,549` | `npcs/falador/Barmaid.java` | Coordinate-gate this spawn because ID `142` also exists upstairs at `320,1491`; preserve bar crawl and holiday dialogue priority. |
| `port_sarim` | Bartender `150`, Rusty Anchor `257,626` | `npcs/portsarim/Bartender.java` | Add guild options after quest/bar-crawl priority; do not replace drink service. |
| `brimhaven` | Bartender `279`, Dead Man's Chest `451,705` | `npcs/brimhaven/BrimHavenBartender.java` | Add guild options after bar-crawl/drink behavior. |
| `champions` | Guildmaster `111`, Champions Guild `149,557` | `npcs/varrock/Guildmaster.java` | Preserve Dragon Slayer and normal guild-access dialogue before Monster Slayer options. |
| `heroes` | Achetties `253`, Heroes Guild `372,443` | `quests/members/HerosQuest.java` | Preserve Heroes Quest/cape behavior; remove the old Odyssey tier transition only in the coordinated activation branch. |
| `legends` | Sir Radimus `785`, Legends Guild `514,535` | `LegendsQuestSirRadimusErkle.java` | Use only the guild NPC, not house Radimus `735`; preserve Legends Quest reward/training behavior and replace the hidden Odyssey route during activation. |

Higher contacts require both the previous Monster Slayer rank and their normal
host-guild access. Early conversation should explain which stamp is required
without bypassing Champions, Heroes, or Legends Guild entry requirements.

## Data Design

Create `server/conf/server/defs/extras/MonsterSlayer.json`; do not extend the
positional Odyssey schema. The systems have different identity, ordering,
repeatable, rank, and migration requirements, and extending the old arrays
would make old cache meaning less safe.

Required top-level shape:

```json
{
  "schemaVersion": 1,
  "families": [],
  "contacts": []
}
```

Family shape:

```json
{
  "key": "goblin",
  "displayName": "Goblins",
  "npcIds": [4, 62, 153, 154, 660]
}
```

Contact/task shape:

```json
{
  "key": "falador",
  "npcId": 142,
  "requiredRank": "FLEDGLING",
  "awardedRank": "INITIATE",
  "mandatoryTasks": [
    {
      "key": "falador.rats",
      "familyKey": "rat",
      "requiredKills": 100,
      "pointReward": 5
    }
  ],
  "repeatableTasks": [
    {
      "key": "falador.rats.repeatable",
      "familyKey": "rat",
      "requiredKills": 75,
      "pointReward": 5,
      "weight": 1
    }
  ]
}
```

Stable identity rules:

- Persist task/contact/family keys, never JSON array positions or display text.
- Keys are lowercase ASCII dot-separated identifiers and are immutable once
  published. Display text may change without migration.
- A family owns NPC membership. Multiple contacts may reference the same family
  key; do not duplicate its ID list.
- Mandatory array order is authored progression order, but an active save stores
  the stable task key. Reordering future display data cannot retarget a player.
- Dialogue remains in the contact plugin/service, with optional dialogue keys
  in data only after a localization contract exists. Do not copy the unused
  `taskInfoDialog` field.

Load-time/CI validation must reject:

- unknown/duplicate keys, rank codes, contact NPC IDs, family NPC IDs, or task
  references;
- nonattackable targets or a family with no active MyWorld static spawn;
- duplicate NPC IDs across different families (reuse one family instead);
- zero/negative kills, points, weights, or counts beyond configured safe caps;
- a mandatory chain whose required/awarded ranks do not form the exact ladder;
- duplicate mandatory/repeatable task keys or empty task pools;
- any material/certificate turn-in field or any finished dragon-equipment item
  reward field.

### Validated Launch Family Inventory And Tuning

Spawn counts below are active location records for the current MyWorld load set:
base `NpcLocs.json`, enabled discontinued/mod-room/runecraft/auction/harvesting/
custom-quest/other files, `MyWorldNpcLocs.json`, tutorial cleanup, and explicit
MyWorld removals. They are validation evidence, not a promise that every spawn
is equally accessible. All listed IDs resolve, are attackable, and have at least
one active static spawn.

| Contact | Stable task/family | Valid NPC IDs | Active spawns | Mandatory kills | Repeatable kills | Points |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| Falador | `falador.rats` / `rat` | `19,29,47,177` | 170 | 100 | 75 | 5 |
| Falador | `falador.goblins` / `goblin` | `4,62,153,154,660` | 198 | 100 | 75 | 5 |
| Falador | `falador.cows` / `cow` | `6` | 41 | 100 | 75 | 5 |
| Falador | `falador.dwarves` / `dwarf` | `94,699` | 28 | 100 | 75 | 5 |
| Falador | `falador.dark_wizards` / `dark_wizard` | `57,60` | 33 | 100 | 75 | 5 |
| Port Sarim | `port_sarim.pirates` / `pirate` | `137,264` | 25 | 125 | 100 | 8 |
| Port Sarim | `port_sarim.muggers` / `mugger` | `21` | 7 | 50 | 40 | 8 |
| Port Sarim | `port_sarim.skeletons` / `skeleton` | `40,45,46,179,195` | 175 | 150 | 100 | 8 |
| Port Sarim | `port_sarim.zombies` / `zombie` | `41,52,68,180,214` | 72 | 150 | 100 | 8 |
| Port Sarim | `port_sarim.hobgoblins` / `hobgoblin` | `67,311` | 51 | 125 | 100 | 8 |
| Brimhaven | `brimhaven.jungle_spiders` / `jungle_spider` | `521` | 70 | 200 | 125 | 12 |
| Brimhaven | `brimhaven.scorpions` / `scorpion` | `70` | 36 | 150 | 100 | 12 |
| Brimhaven | `brimhaven.jogres` / `jogre` | `523` | 21 | 150 | 100 | 12 |
| Brimhaven | `brimhaven.moss_giants` / `moss_giant` | `104,594` | 17 | 200 | 100 | 12 |
| Brimhaven | `brimhaven.lesser_demons` / `lesser_demon` | `22,181` | 37 | 150 | 75 | 12 |
| Champions | `champions.giants` / `giant` | `61` | 24 | 250 | 150 | 18 |
| Champions | `champions.dark_warriors` / `dark_warrior` | `199` | 13 | 200 | 125 | 18 |
| Champions | `champions.black_knights` / `black_knight` | `66,108,189` | 45 | 250 | 150 | 18 |
| Champions | `champions.ogres` / `ogre` | `312,525,706` | 67 | 250 | 150 | 18 |
| Champions | `champions.greater_demons` / `greater_demon` | `184` | 16 | 150 | 75 | 18 |
| Heroes | `heroes.baby_blue_dragons` / `baby_blue_dragon` | `203` | 11 | 150 | 75 | 25 |
| Heroes | `heroes.blue_dragons` / `blue_dragon` | `202` | 6 | 100 | 50 | 25 |
| Heroes | `heroes.shadow_warriors` / `shadow_warrior` | `787` | 8 | 150 | 100 | 25 |
| Heroes | `heroes.paladins` / `paladin` | `323,632,633` | 11 | 150 | 100 | 25 |
| Heroes | `heroes.otherworldly_beings` / `otherworldly_being` | `298` | 7 | 150 | 100 | 25 |
| Heroes | `heroes.hellhounds` / `hellhound` | `294` | 9 | 150 | 100 | 25 |
| Legends | `legends.death_wings` / `death_wing` | `768` | 8 | 150 | 75 | 35 |
| Legends | `legends.fire_giants` / `fire_giant` | `344` | 9 | 250 | 125 | 35 |
| Legends | `legends.greater_demons` / `greater_demon` | `184` | 16 | 250 | 125 | 35 |
| Legends | `legends.red_dragons` / `red_dragon` | `201` | 7 | 125 | 50 | 35 |
| Legends | `legends.black_demons` / `black_demon` | `290` | 13 | 250 | 125 | 35 |
| Legends | `legends.black_dragons` / `black_dragon` | `291` | 4 | 100 | 40 | 35 |
| Legends | `legends.king_black_dragon` / `king_black_dragon` | `477` | 1 | 1 | not random | 50 |

Launch totals and recommendations:

| Band | Mandatory tasks | Mandatory kills | Mandatory points | Random repeatable pool |
| --- | ---: | ---: | ---: | ---: |
| Fledgling -> Initiate | 5 | 500 | 25 | 5 |
| Initiate -> Veteran | 5 | 600 | 40 | 5 |
| Veteran -> Elite | 5 | 850 | 60 | 5 |
| Elite -> Champion | 5 | 1,100 | 90 | 5 |
| Champion -> Hero | 6 | 850 | 150 | 6 |
| Hero -> Legend | 7 | 1,126 | 260 | 6; KBD remains a capstone only |
| **Total** | **33** | **5,026** | **625** | **32** |

This cuts the mandatory wall to about 12 percent of the old 40,906 kills while
keeping a substantial rank path. Expensive unique rewards can take the full
lifetime grind toward the old scale through repeatables without delaying shop
access.

Repeatable policy:

- Equal weight `1` at launch; family-specific counts in the table already
  account for density and difficulty.
- Assignment uses the contact whose mandatory chain is complete. A player may
  use any completed contact, regardless of higher rank.
- Cancelling an accepted repeatable costs half its point reward rounded up:
  Falador `3`, Port Sarim `4`, Brimhaven `6`, Champions `9`, Heroes `13`,
  Legends `18`. No negative balance and no free replacement if payment fails.
- Task blocks, free rerolls, streak bonuses, and boss randomization are deferred
  until their shop rewards are explicitly approved.

Legacy IDs intentionally excluded from the launch inventory include unspawned
variants (`473`, `583`, `694`, `50`, `359`, `710`), unattackable guards (`375`,
`376`), civilians/quest personalities, and misleading family additions such as
Firebird `252`, Wormbrain `192`, Greldo `109`, Melzar `182`, Gunthor `78`,
Salarin `567`, Colonel Radick `518`, named bandit leaders, Ogre citizen `704`,
Blessed Vermen `630`, target-practice zombies, and happy peasants. They remain
valid historical Odyssey targets but are unsafe defaults for the new guild.

## Player State Design

Use existing typed player-cache storage through one `MonsterSlayerState` owner.
Dialogue and kill handlers must not manipulate raw keys.

| Key | Type | Contract |
| --- | --- | --- |
| `monster_slayer_state_version` | Integer | Current state schema; starts at `1`. |
| `monster_slayer_intro_stage` | Integer | `0` not started, `1` beer requested, `2` beer completed. Stage 2 requires rank at least Fledgling. |
| `monster_slayer_rank` | Integer | Stable rank code `0..7`; never a display/string ordinal. |
| `monster_slayer_points` | Long | Global nonnegative balance. Checked additions and deductions; cap at `2,000,000,000`. |
| `monster_slayer_active_task` | String | Stable task key; absent means no task. Contact/family/type derive from definitions. |
| `monster_slayer_active_kills` | Integer | Bounded `0..requiredKills`; absent/zero when no active task. |
| `monster_slayer_mandatory_<contact>` | Integer | Six keys storing the number of fixed tasks completed for that contact, bounded by that chain's data length. |
| `monster_slayer_tasks_completed` | Long | Lifetime mandatory plus repeatable completion statistic; no rank authority. |
| `monster_slayer_migration_version` | Integer | One-time Odyssey migration marker; version `1` for the rules below. |
| `monster_slayer_legacy_status` | Integer | `0` none, `1` partial, `2` completed-unclaimed, `3` completed-claimed. Preserves future commemorative eligibility. |
| `monster_slayer_legacy_prestige` | Integer | Nonnegative snapshot of `co_prestige`; historical statistic only. |

Invariants:

- Rank is monotonic. Point spending cannot lower rank.
- A rank requires every lower contact cursor to equal its chain length.
- The active mandatory task must be exactly the current contact cursor's stable
  key. A repeatable task requires that contact cursor to be complete.
- Missing active-task definitions, out-of-range cursors, negative values, or
  rank/cursor contradictions produce a bounded diagnostic and no mutation.
- Task completion updates progress, cursor/rank, points, lifetime count, and
  active-task clearing through one state-owner method on the game thread.
- Completion is derived from `rank == LEGEND` and all six mandatory cursors;
  do not add a redundant completion boolean.
- Do not create `monster_slayer_prestige` in version 1. Old `co_prestige` counts
  full 40,906-kill Odyssey completions and is not equivalent to repeating the
  new rank path. A future prestige contract needs its own approved loop first.

## Safe Combat Odyssey Migration

Migration is lazy for accounts with legacy evidence and idempotent through
`monster_slayer_migration_version`. The foundation branch implements and tests
the pure conversion but does not invoke it on live players. The later activation
branch invokes it before Monster Slayer dialogue or kill credit can mutate new
state.

Never delete or rewrite `combat_odyssey`, `co_tier_progress`, or `co_prestige`.
They remain recovery evidence. Do not remove Biggum from inventory/bank in the
migration transaction.

### Classification

1. If migration version is already `1`, return without awarding anything.
2. If `co_prestige > 0`, classify `completed-claimed`, grant `Legend`, complete
   all mandatory cursors, and preserve the prestige count.
3. Otherwise, if a valid tier-13 active record has its sole KBD task marked or
   has a bounded kill count of at least one, classify `completed-unclaimed`,
   grant `Legend`, and complete all mandatory cursors.
4. Otherwise, a valid intro stage `1`/`2` or active colon record is `partial`.
5. Missing/zero intro state with no other evidence is `none`; initialize normal
   defaults without legacy credit.
6. Wrong cache types, malformed strings, unknown tier/task IDs, negative kills,
   impossible masks, or rank contradictions are quarantined. Log the key names
   and validation reason, not a whole cache dump; make no migration award and
   do not set the migration version until repaired.

### Partial Rank Mapping

Do not try to map old positional task bits onto different new task keys. Grant
the highest fully supported rank and convert within-band work to points; start
the new current-rank mandatory chain at index zero.

| Legacy evidence | New rank | New chains marked complete |
| --- | --- | --- |
| Intro stage `1` or `2`; active tier `0..1` | Fledgling | none |
| Active tier `2..3` | Initiate | Falador |
| Active tier `4..5` | Veteran | Falador, Port Sarim |
| Active tier `6..7` | Elite | through Brimhaven |
| Active tier `8..9` | Champion | through Champions |
| Active tier `10..13` but final KBD incomplete | Hero | through Heroes |
| Completed-unclaimed or `co_prestige > 0` | Legend | all six |

### Partial Point Formula

Compute legacy credited kills without guessing:

1. Sum every task's required kills in tiers below the active tier.
2. In the active tier, sum required kills for valid completed mask bits.
3. If the current task's bit is not complete, add its active kill count clamped
   to `0..requiredKills`. Do not count a completed current task twice.
4. `partialPoints = min(800, floor(creditedKills / 50))`.

For `completed-unclaimed`, award `1,000` points. For claimed completion, award
`min(10,000, 1,000 + 250 * (co_prestige - 1))`. If a prestiged account also
has a valid active repeat Odyssey, add its partial-point result before applying
the same `10,000` cap. This gives a full legacy completion more credit than the
new 625-point mandatory path while leaving the future shop economy usable.

The conversion records no new active task. It never grants old intermediate
items, a final dragon item, material credit, or a new prestige count.

### Cutover Rules

The later activation branch must be atomic in behavior even if delivered as a
focused commit:

- migrate before the first new dialogue/kill mutation;
- stop legacy kill advancement and tier assignment for migrated players;
- keep legacy keys/stat display readable and keep Biggum personality content;
- remove the hidden Radimus start/reward role only when the new Legends contact
  can serve migrated players;
- prevent both systems from crediting the same death;
- provide a staff inspection command/report before any repair command;
- back up the player database and test migration on a copy before live use.

## Point Shops And Explicit Owner Decisions

The point scale can be implemented independently, but unique items cannot be
specified responsibly from the repository. No existing item is a `Giant's Axe`,
and the old Odyssey rewards do not define the intended new power budget.

The owner must decide these before a shop/item implementation branch:

| Decision | Recommended default | Why it is required |
| --- | --- | --- |
| Launch shop breadth | Give every contact at least one cosmetic/utility reward, but introduce combat uniques in separate balance branches. | Prevents one large item/stat/art branch from blocking rank/task activation. |
| Giant's Axe | If approved, place a new slow two-handed sidegrade in the Champions shop; owner must choose stats, requirements, special behavior, tradeability, art, and price. | No such current item or authoritative stat line exists. |
| Legends chase reward | Choose the archetype first: weapon, non-armor accessory, reusable contract utility, or prestige cosmetic. Recommended: a monster-hunting utility/accessory, not armor. | Price and task grind cannot be finalized without its combat value. |
| Legacy completion recognition | Give claimed and completed-unclaimed Odyssey players the same noncombat commemorative entitlement; decide title versus cosmetic item. | The migration preserves an entitlement class but must not improvise a reward. |
| Consumables | Approve exact effects and whether each is tradable. Do not bypass Herblaw/Cooking progression; recommended first stock is convenience rather than best-in-slot healing/buffs. | Effects determine repeatable demand and point inflation. |
| Convenience unlocks | Decide whether paid rerolls/task blocks are shop purchases or only the direct cancellation fee. Recommended launch: cancellation fee only. | Permanent blocks materially change assignment probabilities and state. |
| Purchase/reclaim model | Decide point-only versus points-plus-coins, stock limits, tradeability, death behavior, and lost-item reclaim for every unique. | These are economy and duplication contracts, not presentation details. |
| Price bands | After item power is approved, choose exact prices within provisional bands: `25-100` minor utility/cosmetic, `250-750` mid-tier sidegrade, `1,000-2,000` high utility/combat unique, `2,500-4,000` Legends chase. | The 625-point mandatory grant and migration awards were tuned against these provisional bands. |

Hard exclusions requiring no further decision: dragon armor and dragon skirts
are not rewards; material/certificate turn-ins are not progression; old item
rewards are not automatically shop stock.

## Bounded First Implementation Branch

Suggested branch: `feat/monster-slayer-data-state-foundation`.

Scope:

- Add `MonsterSlayer.json` with schema version 1, the stable rank/contact/
  family/task keys, exact mandatory/repeatable counts, and point values above.
- Add focused immutable definition types and `MonsterSlayerData` loader/
  validator. Load/validate the data in server startup, but expose no player
  dialogue, task assignment, kill credit, points, shop, or reward behavior.
- Add `MonsterSlayerRank` with explicit numeric codes and parsing that does not
  depend on enum ordinal.
- Add `MonsterSlayerState` as the only raw-cache-key owner, including default,
  validation, bounded arithmetic, active-task invariants, and an in-memory
  snapshot/write API. Do not wire new state mutation into login or gameplay.
- Add a pure `CombatOdysseyMigration` converter implementing the rules above.
  It accepts a legacy snapshot plus validated Odyssey/Monster Slayer data and
  returns either a proposed new snapshot or a typed validation failure. It does
  not write a player cache in this branch.
- Document the old data/integration classes as compatibility sources. Do not
  edit their runtime behavior, the eight dialogue integrations, `Npc.killedBy`,
  rewards, items, configs, databases, or the public server.

Tests:

- JSON/schema fixture: unique stable keys, exact rank ladder, exact 33 mandatory
  tasks/5,026 kills/625 points, 32 repeatables, positive bounds, resolvable and
  attackable NPC IDs, active spawn evidence, and no unsafe excluded IDs.
- Exclusion fixture: no material/certificate field, finished dragon-equipment
  ID, retired skirt, item reward, or positional persisted task identity.
- Compiled data fixture: load the real JSON; resolve keys independent of array
  order; reject duplicates, missing families, bad ranks, nonattackable IDs,
  zero-spawn families, invalid counts, and broken contact chains.
- Compiled state fixture: defaults, every rank code, cursor/rank consistency,
  active mandatory/repeatable validation, point add/spend bounds, completion
  derivation, cache round trip, missing keys, wrong types, and corrupt values.
- Compiled migration fixture: no-state, intro stages, every tier boundary,
  partial current task, completed current-task bit without double count,
  malformed masks/strings/types, completed-unclaimed KBD, claimed completion,
  active prestige repeat, caps, legacy-key preservation, and repeated migration
  idempotence.
- Run the existing dragon-production/removal guard, NPC location cleanup test,
  full server build, plugin build (even though plugins should be unchanged),
  and changed-code static analysis.

Stop conditions:

- Stop if the foundation needs player-visible dialogue, login mutation, kill
  hooks, shop/item definitions, reward balance, database schema changes, or
  edits to Combat Odyssey compatibility behavior.
- Stop on any need to infer completion from inventory, silently repair malformed
  legacy state, reuse positional IDs, or include material/dragon rewards.
- Hand off the tested data/state/migration foundation before starting the
  Falador introduction or contribution-aware kill-credit branch.
