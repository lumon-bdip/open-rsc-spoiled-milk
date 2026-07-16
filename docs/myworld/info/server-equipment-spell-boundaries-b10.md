# B10 Server Equipment and Spell Decision Boundaries

This branch extracts deterministic equipment and spell decisions from two
large server runtime owners without changing combat balance, packet contents,
event scheduling, or item/spell definitions.

## Equipment Ownership

`EquipmentSlotRules` now owns side-effect-free slot decisions:

- offensive weapon slots and the legacy ranged-offense fallback;
- bow/off-hand compatibility;
- hand/foot versus body/leg overlap rules; and
- major armor-penalty and armor-stat slot classification.

`EquipmentStatCalculator` now owns side-effect-free arithmetic and stable item
classification used by equipment aggregation:

- displayed offense and clamped combat-stat calculations;
- god-equipment prayer resource, natural-prayer, weapon, and armor mappings;
- blessed-wool base and target magic-defense calculations; and
- the legacy ranged-offense fallback calculation.

`Equipment` remains the mutable container and compatibility boundary. It still
owns item insertion/removal, equip and unequip validation, appearance updates,
player/config access, stat aggregation order, devotion application, equipment
packets, prayer availability checks, charge/durability handling, and all item
effect hooks. The extracted calculators never access `Player`, mutate an item
or container, send a packet, or schedule work.

This first boundary reduces `Equipment` from 3,771 to 3,341 lines. Further
separation of item-effect hooks or mutations requires a separately
characterized branch.

## Spell Ownership

`SpellClassification` now owns deterministic decisions keyed by `Spells`
identity (and, for blood-rune detection, the authoritative `SpellDef`):

- elemental, mind-rune, dual-element, god-lineage, heal, boost, teleport, and
  blood-rune classification;
- tier, damage-cap, elemental debuff, and dual-element proc values;
- projectile, impact, heal, and god-spell presentation IDs;
- elemental-ring compatibility and projectile-visibility decisions; and
- the two mob-cast packet opcodes.

`SpellValidationRules` owns the side-effect-free ordering of auto-cast and
cast-definition rejection decisions, divided good/evil magic skill selection,
teleport-validation eligibility, and duel-action blocking.

`SpellHandler` remains the packet, player-state, and combat orchestration
boundary. It still owns definition lookup, user messages, suspicious-player
marking, path resets, rune checks and removal, staff/equipment requirements,
teleport destination and side effects, target lookup, damage calculation,
experience, combat event creation, AoE target selection, scheduling, and spell
finalization. The extracted classes cannot message a player, mutate inventory,
or schedule an event.

The extraction reduces `SpellHandler` from 2,754 to 2,396 lines. Event
scheduling, target acquisition, and the large cast dispatch remain intentionally
in place; they are higher-risk follow-up boundaries, not part of B10.

## Behavior Characterization

The equipment fixture records published-main commit
`3c9298185d545a64aa24282256e44c77b3bfaf27` results for every item ID from
`-1` through `4000`, all 2,431 `ItemId` constants, every equipment-slot
combination, and representative arithmetic boundaries. All 13 result matrices
match after extraction.

The spell fixture records the same published-main behavior for every one of
the 81 `Spells` values plus `null`. It fingerprints 34 classifications/value
families, 12,382 ring/spell combinations, 3,444 projectile-visibility
combinations, and all 95 opcode/null cases. It also directly covers rejection
precedence, unified and divided magic skill selection, teleport eligibility,
and duel eligibility. Every published-main fingerprint matches after
extraction.

The existing client/server elemental parity fixture continues to confirm all
28 elemental and dual-element spell identities have identical names,
classifications, tiers, base values, caps, and client display metadata.

## Deliberate Limits

- No equipment stat, slot, prayer cost, spell cost, damage cap, proc chance,
  AoE rule, teleport destination, or combat formula changed.
- No item, NPC, spell, projectile, animation, or packet identity changed.
- Rune removal, charges/durability, equipment mutation, and combat scheduling
  remain in their existing runtime owners.
- No public-server configuration, process, database, or detached live file is
  part of this branch.

## Automated Verification

The following checks passed on the spell-boundary checkpoint:

- equipment calculation extraction plus weapon, ranged, hand/foot, armor
  penalty, magic weapon, devotion, blessed wool, prayer-equipment, and charged
  item preservation tests;
- spell classification extraction, spell costs, no-random-fail, chaos
  gauntlets, capped rolls, elemental client/server metadata, tier-two and
  player spell animations, projectile fallbacks, enchanted robes, staff data,
  NPC elemental magic, god spells, and Iban behavior;
- combat data, runtime invariants, exception guardrails, and scenario tests;
- full authoritative Ant server/core and plugin build;
- changed-code compiler analysis with no new gated warnings; and
- Checkstyle, PMD, Ruff, and ShellCheck with zero changed-file findings, plus
  no new SpotBugs findings.

## Required Private Runtime Verification

The branch must not be handed off until the owner confirms this matrix on the
loopback-only development server using the branch-built client:

1. Equip and unequip representative main-hand, off-hand, bow, body, leg,
   glove, and boot items. Confirm slot conflicts and displayed equipment stats
   behave normally.
2. Exercise representative elemental tiers, a dual-element spell, a god
   spell, and Iban blast where practical. Confirm projectiles/impacts, hits,
   rune costs, and staff/equipment gates remain normal.
3. Exercise auto-cast and at least one rejected spell (insufficient runes,
   level, membership, or equipment where practical). Confirm feedback is
   unchanged and bounded.
4. Exercise an AoE spell and a spell teleport. Confirm targets, region load,
   player position, terrain, objects, and entities remain normal.
5. Bank or re-equip a charged/durable item where practical, then log out,
   reconnect, and close the client. Inspect private client/server logs for new
   equipment, spell, event, or cleanup exceptions.

Private runtime status: **pending owner confirmation**.

The public server and detached live checkout are outside this branch and must
remain untouched.
