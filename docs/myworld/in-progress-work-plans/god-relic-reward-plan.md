# God Relic Reward Plan

This document forks the prayer-side god equipment plan into a dedicated reward
track for god relics. It supersedes the simpler "pray at 1000 devotion for a
god staff" direction where the two conflict.

## Goals

- Make god equipment feel like direct divine rewards rather than ordinary
  crafted or Mage Arena rewards.
- Use Prayer level and devotion as the main access gates.
- Keep relic rewards aligned with the player's current god.
- Prevent duplicate relic rewards for the same god line.
- Cover all combat styles with relic options.
- Move god staves and god capes out of Mage Arena-centered reward logic.

## God Relic Set

God relics are high-end god-aligned equipment awarded by the player's god.

Existing relics already in game:

- Saradomin Cape
- Zamorak Cape
- Guthix Cape
- Staff of Saradomin
- Staff of Zamorak
- Staff of Guthix

New relics needed for combat-style coverage:

- Mace of Saradomin
- Mace of Zamorak
- Mace of Guthix
- Saradomin Paladin Shield
- Zamorak Paladin Shield
- Guthix Paladin Shield
- Saradomin Crossbow
- Zamorak Crossbow
- Guthix Crossbow

Each god should therefore have five relics:

- cape
- staff
- mace
- paladin shield
- crossbow

Design intent:

- cape and staff cover Magic and god identity
- mace covers melee
- paladin shield covers defensive Prayer/combat identity
- crossbow covers ranged

Existing-content note:

- There is already an old `Mace of Zamorak` item in the game. Implementation
  must audit whether it should become the Zamorak relic mace, be migrated, or
  be separated from the new relic system to avoid duplicate/confusing Zamorak
  maces.

## Reward Requirements

To request a god relic, the player must meet all of these:

- Prayer level `80`
- `800` devotion with the aligned god
- active worship/alignment with that god
- praying at the matching god altar
- at least one unowned relic remaining in that god's relic pool

Reward cost:

- claiming a relic removes `400` devotion from that god
- devotion loss should happen only after a relic is successfully awarded

The devotion cost means a player at exactly `800` devotion drops to `400` after
claiming. A player at `1000` devotion drops to `600`.

## Altar Flow

When a qualified aligned player prays at the matching god altar, the player
should receive dialogue with that god.

Dialogue purpose:

- acknowledge the player's devotion
- offer the option to request a holy relic
- warn that receiving a relic costs devotion
- award the relic if the player accepts

The player does not choose the exact relic. They request a holy relic, and the
god awards one from the remaining aligned relic pool.

## Relic Selection Rules

Relic selection must follow these rules:

- only the player's aligned god can award relics
- only relics belonging to that god can be awarded
- the player never receives the same relic twice from that god's relic pool
- the exact relic is unknown before the reward is granted
- if all aligned relics have already been claimed, the altar should not charge
  devotion and should explain that no unclaimed relic remains

Suggested implementation model:

- track claimed relics with per-player cache keys
- derive the eligible reward list from the player's active god
- filter out already claimed relics
- select randomly from remaining eligible relics
- award the item
- record the relic as claimed
- subtract `400` devotion

If a relic item is later lost, the "never twice" rule means replacement/reclaim
must be a separate explicit system rather than another random relic claim.

## God Alignment Pools

Saradomin relic pool:

- Saradomin Cape
- Staff of Saradomin
- Mace of Saradomin
- Saradomin Paladin Shield
- Saradomin Crossbow

Zamorak relic pool:

- Zamorak Cape
- Staff of Zamorak
- Mace of Zamorak
- Zamorak Paladin Shield
- Zamorak Crossbow

Guthix relic pool:

- Guthix Cape
- Staff of Guthix
- Mace of Guthix
- Guthix Paladin Shield
- Guthix Crossbow

## God Excommunication

God excommunication is a permanent late-game dedication path for players who
want to fully commit to one god. It starts when a player destroys another god's
relic at the altar of their preferred god.

This must be treated as irreversible account progression, not a normal item
exchange.

### Trigger

The player must:

- be actively aligned with the chosen god
- be praying at the chosen god's altar
- have a relic from a different god available to destroy
- explicitly confirm the permanent excommunication warning

The destroyed relic's god becomes the excommunicated god. The third god becomes
the neutral god.

Example:

- player is aligned with Saradomin
- player destroys a Zamorak relic at a Saradomin altar
- Saradomin becomes the permanent chosen god
- Zamorak becomes permanently hostile at `-1000`
- Guthix becomes permanently neutral at `0`

### Required Warning

Before any item, devotion, alignment, or cache state changes, the game must show
a strong warning that explains:

- this permanently locks the player to the chosen god
- the player will no longer be able to switch god alignments
- the destroyed relic's god will become permanently `-1000` devotion
- the untouched god will become permanently `0` devotion
- all other-god relics in inventory, worn equipment, and bank will be removed
- the action cannot be undone through ordinary gameplay

Implementation should use a deliberate confirmation step rather than a single
dialogue click.

### Excommunication God-Communication Event

Excommunication should reuse the same broad "god communication" idea introduced
by relic attainment, but with much heavier presentation. The relic flow is a
reward audience with the player's god. Excommunication is a hostile divine
judgement from the god being rejected.

This is a good candidate for the first RuneScape Classic remaster sequence that
intentionally uses camera and lighting as part of gameplay presentation.

Target sequence:

- freeze or heavily restrict player movement while the event plays
- swing the camera around until it faces the player
- tilt the camera down toward an eye-level composition
- zoom in on the player
- dim the scene to low brightness
- optionally suppress or soften ordinary UI distractions while dialogue is
  active
- present special god text treatment for the excommunicated god
- play that god's monologue warning the player about the horrible consequence
  they should expect for their decision
- restore the player's previous camera, zoom, brightness, and UI state after
  the sequence completes

The god monologues are owner-authored content. AI should not invent final
speech text unless explicitly asked. Implementation can add placeholders or
wire the event system, but the final Saradomin, Zamorak, and Guthix speeches
should come from the project owner.

Presentation goals:

- make the choice feel permanent and weighty
- make the rejected god feel personally aware of the player's betrayal
- use the new renderer's camera, zoom, tilt, lighting, and text capabilities
  without changing the underlying RuneScape Classic world style
- keep the sequence deterministic and recoverable if the client disconnects
  mid-event

Technical notes:

- The server should own the permanent excommunication state change.
- The client can own the cinematic presentation once the server starts the
  confirmed event.
- The state change should not depend on the client successfully finishing the
  visuals; if a disconnect happens after confirmation, the account state still
  needs to resolve safely.
- If possible, apply the permanent account changes at a clear commit point
  after the warning confirmation but before or during the cinematic, then make
  the cinematic a presentation of the already-confirmed decision.
- Any camera override must preserve and restore the player's prior camera
  preferences.

### Permanent Consequences

After confirmation:

- lock the player's alignment permanently to the chosen god
- set the chosen god's devotion to `1000`
- increase the chosen god's devotion cap from `1000` to `2000`
- permanently lock the excommunicated god's devotion at `-1000`
- permanently lock the neutral god's devotion at `0`
- remove all relics from non-chosen gods from inventory, worn equipment, and
  bank
- prevent future relic rewards from non-chosen gods
- prevent future alignment switching

The devotion locks should be enforced anywhere devotion can be gained, lost, or
set directly.

### Rewards And Unlocks

The player receives:

- all remaining unclaimed relics from the chosen god
- a sizable Prayer XP award
- access to new prayers tied to full dedication
- the ability for chosen-god blessed armor and weapons to progress toward
  tier-11 equivalency as devotion climbs above `1000`

Awarding the remaining chosen-god relics must still avoid duplicates. If the
player already has some chosen relics, only missing relics are awarded.

Inventory overflow needs an explicit implementation decision. Options:

- require enough free inventory space before confirmation
- place overflow relics at the player's feet
- send overflow relics to the bank if space exists there

The safest initial implementation is to require enough free inventory space
before allowing confirmation, because this feature has permanent consequences.

### Devotion Above 1000

After excommunication, devotion above `1000` should require more offerings than
ordinary devotion progression.

Design intent:

- `0-1000` remains the normal devotion climb
- `1000-2000` is a slower dedication climb
- tier-11 blessed equipment equivalency is only reached through this higher
  devotion range

The exact offering curve is still open. Implementation should make the scaling
data-driven or centralized so the cost curve can be tuned without touching every
offering script.

### Implementation Notes

State likely needed per player:

- permanent chosen god
- excommunicated god
- neutral god
- devotion cap override for chosen god
- alignment-switch lock
- excommunication completed flag

Systems that must respect the new state:

- alignment switching
- devotion gain/loss
- devotion caps and clamps
- god aggression logic
- god altar reward dialogue
- relic claim tracking
- bank, inventory, and equipment cleanup
- Prayer tab and devotion UI
- blessed equipment scaling
- prayer unlock checks

The excommunication flow should be transactional where possible: validate all
requirements first, then apply all state changes together after confirmation.

## Mage Arena Separation

This plan overlaps with the tier-11 magic gear plan.

Relevant direction from that plan:

- god staves should no longer be Mage Arena rewards
- god-staff acquisition moves to devotion altar relic rewards
- god capes should no longer be Mage Arena rewards
- Mage Arena should instead focus on high-end non-god Magic staves:
  - Staff of Elements
  - Staff of Power
  - Staff of Enlightenment
- Mage Arena now sends players to the chamber after the Kolodion fight to claim
  one combination staff from the Elemental, Power, or Enlightenment stone, then
  lets the Chamber Guardian sell the remaining combination staves.
- Mage Arena stones no longer award god capes or set god-choice state.

God capes also become part of the relic reward pool here. Their future handling
should be implemented through Prayer/devotion altar rewards rather than
restoring the old Mage Arena god-stone reward flow.

## God Spell And Charge Follow-Up

God-staff spell casting was tied to Mage Arena mechanics. The old Mage Arena
cast-count training and outside-arena unlock checks have been removed. Before
the relic system is implemented, audit:

- matching god-staff requirements for god spells
- Charge spell behavior
- whether god spell access should require the matching relic staff, devotion,
  altar dialogue, or another god-aligned unlock
- reclaim rules for god staves and capes once relic rewards exist

Target direction:

- god relic acquisition comes from Prayer/devotion altar content
- Mage Arena no longer exists primarily to award god equipment
- god spell access should either become devotion/relic driven or require the
  matching god staff directly. It should not return to Mage Arena cast-count
  training.

## Open Decisions

- Exact item IDs for the nine new relics.
- Whether relic items are tradeable.
- Whether relics can be reclaimed if lost, and at what cost.
- Whether claiming a relic should require inventory space or drop overflow at
  the player's feet.
- Whether relic claim order should be fully random or use weighted randomness.
- Whether existing owned god capes and god staves should automatically count as
  already claimed relics.
- How to migrate existing Mage Arena completion and god-choice cache state.
- Whether god relics should scale with current devotion after being claimed, or
  have fixed high-end stats.
- Exact stat budgets for relic mace, paladin shield, crossbow, cape, and staff.
- How the legacy Mace of Zamorak should be handled.
- Exact Prayer XP award for god excommunication.
- Exact offering curve for devotion `1000-2000`.
- Which new prayers unlock after god excommunication.
- Whether excommunication requires only one off-god relic or a larger relic
  prerequisite.
- How the devotion UI should display permanently locked gods and the increased
  devotion cap.
- Final owner-written Saradomin, Zamorak, and Guthix excommunication
  monologues.
- Exact camera timing, text styling, and lighting values for the
  excommunication god-communication cinematic.
