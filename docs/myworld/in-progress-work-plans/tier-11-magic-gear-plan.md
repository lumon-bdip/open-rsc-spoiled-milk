# Tier 11 Magic Gear Plan

This document starts the planning track for tier-11 magic gear. It records the
current direction before item IDs, recipes, stat numbers, and acquisition paths
are implemented.

## Goals

- Give Magic a clear tier-11 equipment endpoint matching the tier-11 direction
  already used by other combat styles.
- Treat god staves as the tier-11 endpoint of the blessed staff line.
- Add non-god tier-11 combination staves as the endpoint of the ordinary staff
  line.
- Move god staves out of the Mage Arena reward flow and into a new
  devotion-based reward system.
- Replace the Mage Arena god-staff rewards with the three new tier-11
  combination staves.
- Keep rune-preservation behavior consistent with existing staff logic.
- Preserve the cost requirement before preservation rolls so powerful staves do
  not bypass rune ownership entirely.

## Tier 11 Staff Families

### Blessed Staff Line

The existing god staves are the tier-11 equivalent of the blessed staff line.
They should no longer be awarded through Mage Arena. They should move into a
new reward system that uses devotion, so god-staff access is tied to long-term
god alignment rather than a one-time Mage Arena choice.

Planned tier-11 blessed staves:

- Staff of Saradomin
- Staff of Zamorak
- Staff of Guthix

These should receive tier-11 magic-weapon stats. Their exact Prayer bonus and
god-spell behavior should be resolved alongside the prayer-devotion equipment
plan and the god relic reward plan.

### Ordinary Staff Line

The ordinary staff line should culminate in three tier-11 combination staves:

- Staff of Elements
- Staff of Power
- Staff of Enlightenment

These staves should receive tier-11 magic-weapon stats and should behave like
ordinary rune-associated staves, but each covers several rune types.

These three staves should become the Mage Arena staff rewards, replacing the
old god-staff reward choice.

## Combination Staff Coverage

### Staff of Elements

Rune coverage:

- Air rune
- Water rune
- Earth rune
- Fire rune

This staff is the elemental endpoint. It should support any spell cost that
uses one or more elemental runes.

### Staff of Power

Rune coverage:

- Mind rune
- Chaos rune
- Death rune
- Blood rune

Special requirement coverage:

- Satisfies the Staff of Iban requirement.

This staff is the combat-power endpoint. It supports the main direct-damage
rune ladder and also replaces the need to equip Staff of Iban for Iban-specific
casting requirements.

### Staff of Enlightenment

Rune coverage:

- Body rune
- Cosmic rune
- Nature rune
- Law rune
- Soul rune
- Life rune

This staff covers every current rune not covered by Staff of Elements or Staff
of Power. If future runes are added, they should default here unless they are
clearly elemental or direct-combat runes.

## Rune Preservation Rules

Tier-11 combination staves should follow the existing staff preservation model:

- The player must have the full required rune cost in inventory before casting.
- For each rune type covered by the equipped staff and used by the spell, roll
  a `50%` preservation chance.
- On success, preserve the full cost of that rune type for that cast.
- On failure, consume the rune type normally.
- Rune types not covered by the staff are always consumed normally.

Because these staves cover multiple rune types, a single cast can preserve
multiple different rune costs. This is intentional, but it makes the items
exceptionally strong and should be treated as a major balance lever.

Example:

- A spell costs air, fire, chaos, and death runes.
- Staff of Elements can roll preservation for air and fire.
- Staff of Power can roll preservation for chaos and death.
- Staff of Enlightenment does not help that spell.

## Balance Concerns

The combination staves are stronger than one-rune staves because they compress
several preservation effects into one item. The main balancing levers are:

- acquisition difficulty
- required Magic level
- required Enchanting, Crafting, Prayer, or quest completion
- material cost
- whether tradeability is allowed
- combat stats
- Prayer bonus, if any
- whether Iban requirement coverage belongs only on Staff of Power

The default mechanical target is still `50%` preservation per covered rune
type. If the items prove too efficient in play, adjust acquisition or stat
budget first before weakening the core identity.

## Implementation Notes

Likely code areas to audit before implementation:

- server item definitions for tier-11 stats and requirements
- client item definitions and icons
- staff rune-preservation lookup tables
- spell casting rune-cost checks
- Staff of Iban requirement checks
- god-spell casting requirements currently tied to Mage Arena cast-count
  training and god-staff equipment checks
- skill guide entries
- bank tag filters
- loot, shop, crafting, enchanting, or quest acquisition sources
- regression tests for rune preservation and Iban substitution

The preservation implementation should be shared with existing staff logic
rather than adding special one-off checks in spell casting.

## Reward Direction

### Mage Arena

Mage Arena should award the ordinary tier-11 combination staves:

- Staff of Elements
- Staff of Power
- Staff of Enlightenment

This preserves Mage Arena as a major Magic reward source without forcing the
player to choose a god-aligned staff there.

Mage Arena likely needs a broader overhaul rather than a simple reward-item
swap. Its existing flow was built around god choice and god-staff rewards, so
the surrounding mechanics should be reviewed together before implementation.

Current mechanics likely tied to the old god-staff model:

- god choice / sacred-stone flow
- god cape reward flow
- Chamber Guardian god-staff reward and god-staff shop
- Mage Arena god-spell cast-count training
- god-spell outside-arena unlock checks
- matching god-staff requirements for god spells
- Charge spell behavior and its relationship to god spells
- dialogue that frames the arena around god allegiance
- `mage_arena` and `mage_arena_god_choice` cache-state progression

Overhaul target:

- Mage Arena becomes the place to earn high-end non-god Magic staves.
- God-staff acquisition moves entirely to devotion altar rewards.
- God-spell mechanics are either moved to devotion/god-staff content or kept as
  a separate spell-training system that no longer drives staff rewards.
- Existing progression states should be migrated carefully so old characters do
  not lose access unexpectedly.

Open implementation details:

- whether Mage Arena still gives only one staff choice
- whether players can later swap or earn the other two
- whether existing god-staff ownership is migrated, exchanged, or left as-is
- whether the god capes stay in the current Mage Arena reward flow or move
  with god staves to devotion content
- whether old Mage Arena spell-training remains, is replaced, or is converted
  into a new high-end staff challenge

### Devotion Rewards

God staves should move to a new devotion-based reward system:

- Staff of Saradomin
- Staff of Zamorak
- Staff of Guthix

Reward rule:

- God staves are part of the broader god relic pool.
- The full reward rules live in
  [`god-relic-reward-plan.md`](god-relic-reward-plan.md).
- Current relic reward gate: Prayer `80`, aligned devotion `800`, matching god
  altar prayer, and a `400` devotion cost on successful relic award.

God-staff relic mappings:

- Saradomin relic pool can award Staff of Saradomin
- Zamorak relic pool can award Staff of Zamorak
- Guthix relic pool can award Staff of Guthix

Open implementation details for staves specifically:

- whether the god-staff relic must be claimed before the matching god spell can
  be used
- whether the player must already have completed Mage Arena or god-spell
  training for any related spell behavior

### God Spell Decoupling

God-staff spell casting is currently tied to Mage Arena behavior. The live code
tracks god-spell cast counts in Mage Arena and also checks for the matching god
staff when casting those spells.

The Mage Arena redesign should separate these concerns:

- Mage Arena should focus on earning the new high-end combination staves.
- God-staff acquisition should come from devotion at god altars.
- God-spell unlocking, god-spell casting, and the Charge spell should be
  audited so they do not accidentally keep Mage Arena as god-staff content.

Likely implementation question:

- Should god spells become devotion/altar/god-staff unlocks, keep a separate
  spell-training step, or simply require the matching god staff after the
  devotion reward is earned?

## Open Decisions

- Final item IDs.
- Exact item names and examine text.
- Exact tier-11 magic offense and defense stat budgets.
- Whether these staves require level 80 Magic, level 80 Enchanting, both, or a
  different gate.
- Whether Staff of Enlightenment should include all utility runes exactly as
  listed above, or whether any should remain excluded for balance.
- Exact Mage Arena acquisition rules for Staff of Elements, Staff of Power, and
  Staff of Enlightenment.
- God-staff reclaim and replacement rules after the `800` devotion relic reward
  is claimed.
- God-spell casting and Charge-spell behavior after god staves leave Mage
  Arena.
- Whether combination staves are tradeable.
- Whether god staves receive the same rune-preservation behavior as blessed
  wood staves, or a separate god-spell-focused mechanic.
- Whether these preservation effects should apply only to Magic casting or also
  to Summoning/support rune costs later.
