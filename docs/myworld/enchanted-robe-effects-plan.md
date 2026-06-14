# Enchanted Robe Effects Plan

This is a running design document for fresh altar-bound enchanted robe effect
ideas. Effects here are proposals until they are implemented and tested.

Robes already own rune conservation, and that should stay. Earlier robe effects
were largely moved onto amulets, including Nature poison mitigation. This doc is
for new robe ideas that can coexist with rune conservation rather than restoring
the old retired robe-effect set.

## Design Goals

- Each rune family can have a distinct robe effect identity.
- Rune conservation remains the baseline robe identity and should not be
  removed by these new effects.
- Effects should scale by robe tier, from tier `1` through tier `10`.
- Effects should also scale by equipped robe pieces, so partial sets matter but
  full sets are meaningfully stronger.
- Per-tier gains should stay conservative because multiple robe pieces can
  stack.
- Any effect that changes combat pacing, healing, damage prevention, or damage
  output needs explicit caps or small per-tier values before implementation.

## Open Assumptions

- Robe effects are expected to stack per equipped enchanted robe piece.
- A full robe set is currently treated as five contributing pieces for balance
  math unless the implementation later defines fewer eligible slots.
- Tier scaling should use the robe's enchanted tier, not the altar's unlock
  tier.

## Existing Baseline

- Enchanted robes conserve matching runes.
- Most old robe effects were moved to amulets.
- Retired effects should not be copied back onto robes unless explicitly
  redesigned as a new effect.

## Nature Robes

Implemented identity: potion amplification.

- Effect: potions are stronger and last longer.
- Scaling: `+2%` potion strength and `+2%` potion duration per robe tier per equipped Nature
  robe piece.
- Full tier-10 set target: `+100%` potion strength and duration, assuming five equipped
  contributing pieces.
- Balance reason: Nature is a higher-tier support rune than the basic elemental
  runes, and potion amplification gives it a distinct non-combat support identity.

Example scaling:

| Equipped Nature robe pieces | Tier 1 bonus | Tier 5 bonus | Tier 10 bonus |
| --- | ---: | ---: | ---: |
| 1 piece | `+2%` | `+10%` | `+20%` |
| 3 pieces | `+6%` | `+30%` | `+60%` |
| 5 pieces | `+10%` | `+50%` | `+100%` |

Notes:

- Nature poison mitigation has moved to amulets and should stay there.
- Potion strength uses the existing potion value hooks and rounds upward because
  most potion effects are stored as integer percentages or multipliers.

## Elemental Robes

Implemented identity: elemental resistance.

Applies to:

- Air robes
- Water robes
- Earth robes
- Fire robes

Implemented effect:

- Reduce incoming elemental magic damage matching the robe's element.
- Scaling: `2%` resistance per robe tier per equipped matching elemental robe
  piece.
- Full tier-10 set target: `100%` resistance to that element, assuming five
  equipped contributing pieces.
- This is framed as resistance or negation, not absorption. Prevented damage is
  not converted into another resource.

Implementation notes:

- NPC magic attacks now carry an elemental identity through `NpcMagicElement`.
- Fire associations cover fire giants, red/black/KBD-style dragons, demons, and
  fire casters.
- Water and earth associations cover obvious elemental enemies and dragons.
- Air is used for generic caster splits where appropriate.
- Holy, divine, and intentionally untyped magic remain `NONE` and are not resisted by elemental robes.
- Pure magic casters should avoid having only one element if that would make a
  full tier-10 elemental set completely shut them down.

Candidate NPC elemental assignments:

| Element | NPC candidates | Notes |
| --- | --- | --- |
| Fire | Delrith; Lesser Demon; Greater Demon; Black Demon; Fire Giant; The Fire warrior of lesarkus; red/black/king dragon magic | Demons, fire-themed enemies, and red/black dragons fit fire magic. |
| Water | Ice Giant; Ice Warrior; Ice Queen | Blue/ice-themed enemies should use water-aligned magic. |
| Earth | Moss Giant; Tree Spirit | Green/nature/plant-themed enemies fit earth magic. |
| Water dragon magic | Blue Dragon; Baby Blue Dragon | Dragon breath remains separate; this is only the mixed-in magic projectile. |
| Air/Fire split | Darkwizard | Randomly uses fire or air magic. |
| Air/Earth split | Witch | Randomly uses earth or air magic. |
| Air/Water split | Wizard | Randomly uses water or air magic. |
| Fire/Water split | Necromancer | Randomly uses fire or water magic. |
| Fire/Earth split | Skeleton Mage | Randomly uses fire or earth magic. |
| Water/Earth split | Ghost | Randomly uses water or earth magic. |
| All elements | Battle Mage | Battle mages should be pure magic users and randomly use all four elemental types. |
| Untyped | Lucien | Uses magic without elemental alignment. |
| Adjacent follow-up | Dragon breath | Dragon breath remains a special dragonbreath attack. Dragons also mix in their associated elemental magic attack through the NPC magic projectile profile. |

Visual notes:

- The basic `FIREBALL` projectile is used for basic spellcaster fire spells
  only: Darkwizard, Necromancer, and Skeleton Mage.
- The basic `enemy-air-basic` projectile is used for basic spellcaster air
  spells only: Darkwizard, Witch, and Wizard.
- The basic `enemy-water-basic` projectile is used for basic spellcaster water
  spells only: Wizard, Necromancer, and Ghost. It renders as a rooted projectile
  animation that extends from the caster rather than moving across the scene.
- The basic `enemy-earth-basic` visual is an `On Player` combat effect used for
  basic spellcaster earth impacts only: Witch, Skeleton Mage, and Ghost.
- Demons, dragons, ice enemies, and battle mages should not use those basic
  fireball, air, water, or earth visuals.
- Black Demon and Balrog use their own `On Player` magic impact effects.
- Balrog magic splashes nearby players within `2` tiles for `50%` of the
  primary target's actual damage dealt.
- Battle mages are magic-only and randomly use all four elements. Their impacts
  use battle-mage-specific `On Player` effect ids that borrow tier 3 spell
  visuals: air uses Tornado, earth uses Earth Burst, water uses Water Eruption,
  and fire uses Explosion.

Excluded from elemental assignment:

- God or holy aligned: Priest, Monk, Monk of Zamorak, High priest of Entrana,
  Druid, Chaos Druid, Chaos Druid Warrior, Paladin, Black Knight, White Knight,
  Grey Knight, Black Knight Titan.
- Noncombat quest/shop wizards can stay unassigned until they become attackable
  combatants.

## Law Robes

Implemented identity: runecrafting production increase.

Implemented effect:

- Runecrafting yields `2%` more runes per robe tier per equipped Law robe
  piece.
- Full tier-10 set target: `100%` more runes, assuming five equipped
  contributing pieces.
- Bonus progress is tracked per player and per rune type as fixed-point
  carryover, so fractional bonuses are fair without letting progress earned on
  one rune type become another rune type.

Implementation notes:

- The carryover unit is `10000` points per rune.
- Each crafted rune earns `bonusPercent * 100` points.
- Whole bonus runes are paid out immediately; remaining points stay in player
  cache under a rune-specific key.

Rejected direction:

- Defensive equalization was left behind because it overlaps too strongly with
  dragon scale mail's identity.

## Death Robes

Fresh proposed identity: overkill spillover.

Proposed effect:

- When the player kills a target, a portion of the overkill damage splashes to
  surrounding enemies within `2` tiles of the killed target.
- Scaling: `2%` of overkill damage per robe tier per equipped Death robe piece.
- Full tier-10 set target: `100%` of overkill damage splashes, assuming five
  equipped contributing pieces.
- If overflow damage exists and the scaled spillover would be below `1`, deal a
  minimum of `1` spillover damage.

Example:

- A hit deals `18` to a monster with `5` HP remaining.
- Overkill is `13`.
- A full tier-5 set gives `50%` spillover, so nearby enemies take `6` damage
  after rounding rules are chosen.
- A full tier-10 set gives `100%` spillover, so nearby enemies take `13`.

Open implementation choices:

- Choose floor, round, or ceil for scaled spillover above the minimum case.
- Decide whether spillover can tag or aggro enemies. Current intent should be
  explicit before implementation.

## Life Robes

Fresh proposed identity: summon endurance.

Proposed effect:

- Support summons last longer.
- Combat summons have more health.
- Scaling: `2%` per robe tier per equipped Life robe piece.
- Full tier-10 set target: `100%`, assuming five equipped contributing pieces.

Support summon behavior:

- Extends time before upkeep is consumed.
- Example: if the bonus is `10%`, a support summon with a `60` second upkeep
  interval consumes the next rune after `66` seconds.

Combat summon behavior:

- Increases summon health by the same percentage.
- The bonus should apply to maximum summon HP and current HP at summon creation.

## Cosmic Robes

Fresh proposed identity: certainty/critical fate.

Proposed effect:

- All player attacks have a chance to crit.
- Scaling: `1%` crit chance per robe tier per equipped Cosmic robe piece.
- Full tier-10 set target: `50%` crit chance, assuming five equipped
  contributing pieces.
- A crit forces the attack to hit for its maximum damage.
- Applies to all attack styles, not just magic.

Implementation notes:

- A crit should override the damage roll, not add extra damage.
- Needs a clear interaction with existing special damage rolls, true damage,
  splash damage, and multi-target weapons.

## Mind Robes

Fresh proposed identity: mental spell amplification.

Proposed effect:

- Mind spell damage caps are raised.
- Scaling: `+1%` damage-cap increase per robe tier per equipped Mind robe
  piece.
- Full tier-10 set target: `+50%` damage cap, assuming five equipped
  contributing pieces.
- This affects the cap for Mind spells, not the base damage roll directly.

Implementation notes:

- This needs to work alongside Chaos gauntlets.
- The robe bonus should be an additional cap modifier in the same damage-cap
  path, not a replacement for Chaos gauntlet behavior.
- Implementation should define ordering explicitly if Chaos gauntlets and Mind
  robes both alter the same cap.

## Body Robes

Fresh proposed identity: pain-fueled power.

Proposed effect:

- Taking damage grants temporary weapon power for all weapon styles.
- Maximum power boost: `+1` weapon power per robe tier per equipped Body robe
  piece.
- Full tier-10 set target: up to `+50` weapon power, assuming five equipped
  contributing pieces.
- Damage taken stacks until the current temporary power reaches the cap.
- Temporary power decays over time.

Decay target:

- Decay should be slow enough that the player can usually attack while near the
  gained value.
- Suggested decay pace: roughly the time it takes to perform two attacks.
- The decay should still be steady enough that the bonus falls off if the player
  stops taking damage.

Open implementation choices:

- Decide whether each point of damage grants `+1` temporary power up to cap, or
  whether damage converts through a smaller ratio.
- Decide whether self-inflicted or environmental damage can charge the effect.

## Chaos Robes

Fresh proposed identity: surrounded aggression.

Proposed effect:

- Increase all damage based on the number of adjacent enemies.
- Scaling: `+2%` damage per robe tier per equipped Chaos robe piece per
  adjacent enemy.
- Full tier-10 set target: `+100%` damage per adjacent enemy, assuming five
  equipped contributing pieces.
- Applies to all damage, not just magic.

Implementation notes:

- Adjacent means directly surrounding the player. The exact shape should match
  the game's existing adjacency rules.
- Needs a cap or careful testing because it can multiply very quickly in dense
  PvM clusters.
- Should count hostile enemies, not summons or neutral NPCs.

## Blood Robes

Fresh proposed identity: blood-spell splash.

Proposed effect:

- Blood spells splash a portion of their dealt damage to enemies within `2`
  tiles of the primary target.
- Scaling: `2%` splash damage per robe tier per equipped Blood robe piece.
- Full tier-10 set target: `100%` splash damage, assuming five equipped
  contributing pieces.
- If the blood spell deals damage and the scaled splash would be below `1`,
  splash damage is a minimum of `1`.

Implementation notes:

- This applies to blood spells only.
- Splash should be based on damage actually dealt, not potential max damage.
- Needs an explicit aggro/tagging rule before implementation.

## Soul Robes

Implemented identity: enhanced regeneration.

- Effect: increased health regeneration.
- Scaling: `+2%` health-regeneration rate per robe tier per equipped Soul robe
  piece.
- Full tier-10 set target: `+100%` health regeneration, assuming five equipped
  contributing pieces.
- Balance reason: Soul is a higher-tier rune, so it inherits the old Nature
  regeneration role at a stronger per-tier rate.

## Unassigned Rune Families

All rune families now have at least a draft identity.
