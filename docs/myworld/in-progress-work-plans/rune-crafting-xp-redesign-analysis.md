# Enchanting Rune-Crafting XP Redesign Analysis

Status: analysis only; no rune XP values or runtime behavior changed.

## Decision Summary

Do not adopt the proposed table and full per-base-rune XP multiplication as
written. The output multiplier already exists, so multiplying XP by that output
again causes the best level-99 method to rise from `45` to `630` displayed XP
per Stone at the normal `3x` My World rate. A 30-Stone action rises from `1,350`
to `18,900` XP before equipment or other XP bonuses.

A uniform scale cannot preserve the early game and control the late game:

- retaining the current best level-1 rate requires at least a `0.90` scale on
  the proposed table (`15 x 0.90 = 13.5`)
- limiting the proposed level-99 optimum to twice the current optimum requires
  at most a `0.142857` scale (`630 x 0.142857 = 90`)

The recommended review candidate is therefore a redesigned progression, not a
uniformly scaled version of the proposal:

1. Keep the current XML XP table as the base XP per Stone.
2. Credit each additional base-output rune at `25%` of the base Stone XP:
   `effective XP multiplier = 1 + 0.25 x (base rune multiplier - 1)`.
3. Continue excluding Law-robe and Chaos-amulet bonus runes from XP.
4. Round once on the total altar action in internal quarter-XP units.

This candidate preserves every unlock's current XP, makes later output
breakpoints meaningful, and places the level-99 optimum at `67.5` XP per Stone
and `2,025` XP per 30 Stones. That is a `1.5x` increase over the current optimal
trip, instead of the proposal's `14x` increase. The `0.25` marginal scalar is a
conservative starting point for route testing, not an authorized balance
change.

## Confirmed Current Implementation

The preliminary findings are confirmed in
`server/plugins/com/openrsc/server/plugins/custom/myworld/skills/runecraft/Runecraft.java`:

- Base output is `1 + floor((current level - unlock level) / 10)`, with one
  rune at the unlock level. The code expresses the same rule by starting at
  `1` and adding integer division when the level exceeds the requirement.
- The current level is the temporary/current skill level, so boosts can unlock
  an altar and can cross an output breakpoint early.
- One altar interaction counts all unnoted Stone in inventory and processes it
  in one loop. Stone is non-stackable, so a full My World equipment-tab
  inventory normally means 30 Stone.
- Current action XP is `def.getExp() * successCount`: XP is awarded once per
  successfully removed Stone, not once per produced rune.
- `runeCount` contains base multiplier output. Law-robe and Chaos-amulet bonuses
  are added afterward and do not affect the current XP award.

The conservative version of the proposal would change the action XP basis to
`proposed internal XP per rune x runeCount`, where `runeCount` is still the
base output before equipment. It must not count the equipment-generated runes.

## XP Units And Runtime Modifiers

`ObjectRunecraft.xml` stores integer internal XP in quarter-XP units. Before
other bonuses:

- `1x` displayed XP is `internal XP / 4`
- normal `3x` My World displayed XP is `internal XP x 3 / 4`

The proposed internal values supplied in the request exactly reproduce the
suggested normal-`3x` displayed values.

| Rune | Unlock | Current internal | Current displayed at 3x | Proposed internal | Proposed displayed per rune at 3x |
| --- | ---: | ---: | ---: | ---: | ---: |
| Air | 1 | 12 | 9 | 20 | 15 |
| Water | 1 | 14 | 10.5 | 20 | 15 |
| Earth | 1 | 16 | 12 | 20 | 15 |
| Fire | 1 | 18 | 13.5 | 20 | 15 |
| Life | 1 | 12 | 9 | 20 | 15 |
| Mind | 8 | 12 | 9 | 21 | 15.75 |
| Body | 15 | 20 | 15 | 27 | 20.25 |
| Chaos | 22 | 28 | 21 | 28 | 21 |
| Cosmic | 30 | 24 | 18 | 29 | 21.75 |
| Nature | 38 | 32 | 24 | 30 | 22.5 |
| Law | 46 | 36 | 27 | 94 | 70.5 |
| Death | 54 | 42 | 31.5 | 168 | 126 |
| Soul | 62 | 50 | 37.5 | 186 | 139.5 |
| Blood | 70 | 60 | 45 | 256 | 192 |

`Player.incExp` applies modifiers in this order:

1. Mind-jewelry XP bonus, prayer skilling bonus, and skiller-brew XP bonus are
   applied to internal XP, with a ceiling after each applicable percentage.
   Enchanting is explicitly eligible for Mind-necklace and prayer bonuses.
2. The configured skill rate is applied later. The normal My World rate is
   `3x`; the player's `1x` mode bypasses that skilling rate.
3. Double-XP applies even in `1x` mode. Wilderness and skull modifiers are
   additive to the configured rate when applicable.

Consequently, all tables below are unboosted normal-`3x` displayed XP. Divide
by three for ordinary `1x`, double for double-XP, and do not assume equipment
XP bonuses are already included.

The proposed values multiply to exact integer internal XP before percentage
bonuses. Non-integral configured rates truncate when assigned back to the
integer XP accumulator. The largest ordinary proposed 30-Stone action at level
99 is Death: `168 x 5 x 30 = 25,200` internal XP before the `3x` rate. This is
far below integer-overflow limits. Normal temporary boosts also remain safe,
although the uncapped output formula continues above level 99 if a boost raises
the current level.

## Every Unlock And Multiplier Breakpoint

Current XP per Stone is the constant third column. Each proposed breakpoint is
shown as `level: base output / proposed displayed XP per Stone` at normal `3x`.
Levels between listed breakpoints retain the preceding row's output and XP.
Multiply any XP-per-Stone value by the number of Stone carried (normally 30)
for the full altar action.

| Rune | Unlock | Current XP/Stone | Proposed breakpoints through 99 |
| --- | ---: | ---: | --- |
| Air | 1 | 9 | 1: 1x/15; 11: 2x/30; 21: 3x/45; 31: 4x/60; 41: 5x/75; 51: 6x/90; 61: 7x/105; 71: 8x/120; 81: 9x/135; 91: 10x/150 |
| Water | 1 | 10.5 | 1: 1x/15; 11: 2x/30; 21: 3x/45; 31: 4x/60; 41: 5x/75; 51: 6x/90; 61: 7x/105; 71: 8x/120; 81: 9x/135; 91: 10x/150 |
| Earth | 1 | 12 | 1: 1x/15; 11: 2x/30; 21: 3x/45; 31: 4x/60; 41: 5x/75; 51: 6x/90; 61: 7x/105; 71: 8x/120; 81: 9x/135; 91: 10x/150 |
| Fire | 1 | 13.5 | 1: 1x/15; 11: 2x/30; 21: 3x/45; 31: 4x/60; 41: 5x/75; 51: 6x/90; 61: 7x/105; 71: 8x/120; 81: 9x/135; 91: 10x/150 |
| Life | 1 | 9 | 1: 1x/15; 11: 2x/30; 21: 3x/45; 31: 4x/60; 41: 5x/75; 51: 6x/90; 61: 7x/105; 71: 8x/120; 81: 9x/135; 91: 10x/150 |
| Mind | 8 | 9 | 8: 1x/15.75; 18: 2x/31.5; 28: 3x/47.25; 38: 4x/63; 48: 5x/78.75; 58: 6x/94.5; 68: 7x/110.25; 78: 8x/126; 88: 9x/141.75; 98: 10x/157.5 |
| Body | 15 | 15 | 15: 1x/20.25; 25: 2x/40.5; 35: 3x/60.75; 45: 4x/81; 55: 5x/101.25; 65: 6x/121.5; 75: 7x/141.75; 85: 8x/162; 95: 9x/182.25 |
| Chaos | 22 | 21 | 22: 1x/21; 32: 2x/42; 42: 3x/63; 52: 4x/84; 62: 5x/105; 72: 6x/126; 82: 7x/147; 92: 8x/168 |
| Cosmic | 30 | 18 | 30: 1x/21.75; 40: 2x/43.5; 50: 3x/65.25; 60: 4x/87; 70: 5x/108.75; 80: 6x/130.5; 90: 7x/152.25 |
| Nature | 38 | 24 | 38: 1x/22.5; 48: 2x/45; 58: 3x/67.5; 68: 4x/90; 78: 5x/112.5; 88: 6x/135; 98: 7x/157.5 |
| Law | 46 | 27 | 46: 1x/70.5; 56: 2x/141; 66: 3x/211.5; 76: 4x/282; 86: 5x/352.5; 96: 6x/423 |
| Death | 54 | 31.5 | 54: 1x/126; 64: 2x/252; 74: 3x/378; 84: 4x/504; 94: 5x/630 |
| Soul | 62 | 37.5 | 62: 1x/139.5; 72: 2x/279; 82: 3x/418.5; 92: 4x/558 |
| Blood | 70 | 45 | 70: 1x/192; 80: 2x/384; 90: 3x/576 |

This breakpoint table is an exhaustive level-1-to-99 comparison: neither the
current nor proposed result changes at an unlisted level.

## Thirty-Stone Trip Comparison

The current trip column is unchanged by later output multipliers. Proposed
unlock and level-99 columns include base output, but exclude equipment bonus
runes and XP-boosting equipment/prayers/brews.

| Rune | Current 30-Stone XP | Proposed at unlock | Proposed at level 99 | Level-99 ratio to current |
| --- | ---: | ---: | ---: | ---: |
| Air | 270 | 450 | 4,500 | 16.67x |
| Water | 315 | 450 | 4,500 | 14.29x |
| Earth | 360 | 450 | 4,500 | 12.50x |
| Fire | 405 | 450 | 4,500 | 11.11x |
| Life | 270 | 450 | 4,500 | 16.67x |
| Mind | 270 | 472.5 | 4,725 | 17.50x |
| Body | 450 | 607.5 | 5,467.5 | 12.15x |
| Chaos | 630 | 630 | 5,040 | 8.00x |
| Cosmic | 540 | 652.5 | 4,567.5 | 8.46x |
| Nature | 720 | 675 | 4,725 | 6.56x |
| Law | 810 | 2,115 | 12,690 | 15.67x |
| Death | 945 | 3,780 | 18,900 | 20.00x |
| Soul | 1,125 | 4,185 | 16,740 | 14.88x |
| Blood | 1,350 | 5,760 | 17,280 | 12.80x |

Nature is the only proposed unlock value below its current XP. Chaos is
unchanged at unlock. Law through Blood are already `2.61x` to `4.27x` their
current XP before any later output breakpoint; the multiplier then compounds
those increases.

## Optimal-Method Changes

Ignoring route time and rune value, current XP-per-Stone optimization has eight
regimes (seven changes after the starting choice):

| Level | Current best method | XP/Stone |
| ---: | --- | ---: |
| 1 | Fire | 13.5 |
| 15 | Body | 15 |
| 22 | Chaos | 21 |
| 38 | Nature | 24 |
| 46 | Law | 27 |
| 54 | Death | 31.5 |
| 62 | Soul | 37.5 |
| 70 | Blood | 45 |

The full proposal produces 22 optimal regimes, or 21 actual changes after the
initial level-1 choice:

| Level | Proposed best method(s) | XP/Stone |
| ---: | --- | ---: |
| 1 | Air / Water / Earth / Fire / Life | 15 |
| 8 | Mind | 15.75 |
| 11 | Air / Water / Earth / Fire / Life | 30 |
| 18 | Mind | 31.5 |
| 21 | Air / Water / Earth / Fire / Life | 45 |
| 28 | Mind | 47.25 |
| 31 | Air / Water / Earth / Fire / Life | 60 |
| 35 | Body | 60.75 |
| 38 | Mind | 63 |
| 41 | Air / Water / Earth / Fire / Life | 75 |
| 45 | Body | 81 |
| 51 | Air / Water / Earth / Fire / Life | 90 |
| 54 | Death | 126 |
| 56 | Law | 141 |
| 64 | Death | 252 |
| 72 | Soul | 279 |
| 74 | Death | 378 |
| 80 | Blood | 384 |
| 82 | Soul | 418.5 |
| 84 | Death | 504 |
| 90 | Blood | 576 |
| 94 | Death | 630 |

The claimed 25 optimal-method changes are therefore not supported by the
XP-per-Stone model. Counting all five tied level-1 elemental/life choices as
separate methods can inflate a count, but they are alternatives within the same
regime, not level-triggered method changes. Real travel time can change or
remove transitions because altars have different routes; it cannot establish a
fixed count of 25 without measured route times.

The proposal also creates unintuitive oscillation: low-tier runes repeatedly
become optimal at their ten-level breakpoints, and Death, Soul, and Blood trade
places several times after all are unlocked. That may be desirable variety,
but it is not a simple unlock ladder.

## Travel, Mining, And Banking Sensitivity

No route-duration telemetry is stored in the rune plugin, so these are explicit
scenarios rather than measured rates:

- `90 seconds`: aggressive pre-mined Stone and short bank/altar route
- `180 seconds`: moderate banking, travel, and contention
- `360 seconds`: self-supplied mining plus a longer altar loop

The table uses the best pure-XP method at each sample level, a full 30-Stone
action, and normal `3x` displayed XP. Each rate cell is `current / proposed`.

| Level | Current best trip | Proposed best trip | 90-sec XP/hour | 180-sec XP/hour | 360-sec XP/hour |
| ---: | --- | --- | ---: | ---: | ---: |
| 1 | Fire: 405 | tied: 450 | 16,200 / 18,000 | 8,100 / 9,000 | 4,050 / 4,500 |
| 22 | Chaos: 630 | basic: 1,350 | 25,200 / 54,000 | 12,600 / 27,000 | 6,300 / 13,500 |
| 46 | Law: 810 | Body: 2,430 | 32,400 / 97,200 | 16,200 / 48,600 | 8,100 / 24,300 |
| 54 | Death: 945 | Death: 3,780 | 37,800 / 151,200 | 18,900 / 75,600 | 9,450 / 37,800 |
| 70 | Blood: 1,350 | Death: 7,560 | 54,000 / 302,400 | 27,000 / 151,200 | 13,500 / 75,600 |
| 99 | Blood: 1,350 | Death: 18,900 | 54,000 / 756,000 | 27,000 / 378,000 | 13,500 / 189,000 |

Inventory sizes below 30 scale every trip and hourly number linearly. Faster
Stone mining from higher Mining level, pickaxe tier, gathering bonuses, a Law
ring, or stored certificates can move a player toward the shorter scenarios.
The single altar action itself processes the whole inventory immediately, so
travel and Stone acquisition dominate sustained rates.

Before any large XP increase, measure representative round trips for at least
the elemental, Law, Death, Soul, and Blood altars and decide a target XP/hour
band. Without that target, the proposal can only be judged by relative growth.

## Equipment-Generated Runes

Equipment output must remain outside the XP count:

- Law robes add `2%` production per total robe tier and can reach `100%` with
  the documented full tier-10 set. Fractional progress is carried per rune.
- Chaos amulets add `20%` to `100%` output when crafting Chaos runes and roll
  the bonus across Mind, Chaos, Death, and Blood runes.
- Both systems calculate from the same base `runeCount`. With maximum applicable
  bonuses, a Chaos action can produce base output plus up to `100%` Law-robe
  output and `100%` Chaos-amulet output.

Awarding XP on those bonus runes would turn equipment yield into another XP
multiplier, make randomized Chaos output affect training XP, and amplify the
already large late-level rates. A future implementation should name the base
count explicitly and test that equipment methods run after the XP-eligible
count is fixed.

## Recommendation And Review Gate

Recommendation: redesign before implementation.

The proposed internal table is valid in terms of units, and the base-output
formula matches current behavior. The balance problem is their combination:
large high-tier per-rune values are multiplied again at every output
breakpoint. Uniform scaling cannot fix both ends of the curve, and the pure-XP
optimizer yields 21 changes rather than the claimed 25.

For the first controlled implementation review, use the existing XML table and
the `0.25` marginal-output scalar described in the decision summary. In
conceptual internal units for an altar action:

```text
extraOutput = baseRuneMultiplier - 1
actionXP = floor(currentXmlXP * stonesProcessed * (4 + extraOutput) / 4)
```

Round once per full action, not once per Stone, to minimize loss from quarter
increments. Keep the existing current-level boost behavior, apply normal shared
XP modifiers afterward, and exclude all equipment-generated output.

Do not implement even this candidate until representative route times and a
target XP/hour band are reviewed. If testing shows the candidate too flat, move
the marginal scalar from `0.25` toward `0.50`; do not jump directly to the
proposal's implicit `1.00` marginal credit plus its much higher table.
