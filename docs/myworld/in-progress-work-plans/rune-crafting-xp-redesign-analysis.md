# Enchanting Rune-Crafting XP Redesign Analysis

Status: implemented on this feature branch; pending review and merge.

## Decision Summary

The accepted table and full base-output multiplication are now implemented for
review. The supplied fastest-route estimates establish that the table is
route-normalized: it is designed to correct Nature's pre-implementation
`134,600 XP/hour` route, which substantially exceeds Blood at `75,300` and Soul
at `68,500`, rather than merely to order XP per Stone.

With those route estimates, the implemented model produces 26 optimal level
ranges and therefore the claimed 25 method changes. Nature remains competitive
when its nine-tile Stone source earns that advantage, but Death, Soul, and Blood
become the appropriate late-game methods. Their final optimal rates are tightly
grouped:

- Blood: `964,151 XP/hour` at levels `90-91`
- Soul: `1,019,695 XP/hour` at levels `92-93`
- Death: `1,035,616 XP/hour` at levels `94-99`

That is a much healthier relative progression than the pre-implementation route
table. The absolute rate increase remains a deliberate balance decision: the
implemented peak is about `7.7x` the pre-implementation global maximum and the
level-99 Death action is `18,900` displayed XP per 30 Stone before equipment or
other XP bonuses. The implementation should be merged only if an approximately
one-million-XP/hour late-game ceiling matches the intended faster Enchanting
grind.

Runtime XP is awarded only for base multiplier output.
Law-robe and Chaos-amulet bonus runes remain excluded. The route estimates
should also receive a short reproducible in-game timing pass, especially for
the Nature, Death, Soul, and Blood routes; that is validation of the accepted
model, not a reason to replace it with the earlier `0.25` marginal-output
alternative.

## Implemented Runtime Behavior

The implementation in
`server/plugins/com/openrsc/server/plugins/custom/myworld/skills/runecraft/Runecraft.java`:

- Base output is `1 + floor((current level - unlock level) / 10)`, with one
  rune at the unlock level. The code expresses the same rule by starting at
  `1` and adding integer division when the level exceeds the requirement.
- The current level is the temporary/current skill level, so boosts can unlock
  an altar and can cross an output breakpoint early.
- One altar interaction counts all unnoted Stone in inventory and processes it
  in one loop. Stone is non-stackable, so a full My World equipment-tab
  inventory normally means 30 Stone.
- A Stone counts as processed only after its exact inventory removal succeeds.
  Missing or stale Stone stops the loop without producing runes or XP for that
  failed iteration.
- `baseRuneCount` accumulates the multiplier output from successfully processed
  Stone. Action XP is `def.getExp() * baseRuneCount`.
- XP is awarded once after the inventory loop, so a full inventory remains one
  altar action and one XP grant.
- Law-robe and Chaos-amulet bonuses are calculated afterward from
  `baseRuneCount`. Their extra output never enters the XP calculation.

## XP Units And Runtime Modifiers

`ObjectRunecraft.xml` stores integer internal XP in quarter-XP units. Before
other bonuses:

- `1x` displayed XP is `internal XP / 4`
- normal `3x` My World displayed XP is `internal XP x 3 / 4`

The implemented internal values supplied in the request exactly reproduce the
suggested normal-`3x` displayed values.

| Rune | Unlock | Pre-implementation internal | Pre-implementation displayed at 3x | Implemented internal | Implemented displayed per rune at 3x |
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

The implemented values multiply to exact integer internal XP before percentage
bonuses. Non-integral configured rates truncate when assigned back to the
integer XP accumulator. The largest ordinary implemented 30-Stone action at
level 99 is Death: `168 x 5 x 30 = 25,200` internal XP before the `3x` rate.
This is far below integer-overflow limits. Normal temporary boosts also remain
safe, although the uncapped output formula continues above level 99 if a boost
raises the current level.

## Every Unlock And Multiplier Breakpoint

Pre-implementation XP per Stone is the constant third column. Each implemented
breakpoint is shown as `level: base output / implemented displayed XP per Stone`
at normal `3x`.
Levels between listed breakpoints retain the preceding row's output and XP.
Multiply any XP-per-Stone value by the number of Stone carried (normally 30)
for the full altar action.

| Rune | Unlock | Pre-implementation XP/Stone | Implemented breakpoints through 99 |
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
pre-implementation nor implemented result changes at an unlisted level.

## Thirty-Stone Trip Comparison

The pre-implementation trip column is unchanged by later output multipliers.
Implemented unlock and level-99 columns include base output, but exclude
equipment bonus runes and XP-boosting equipment/prayers/brews.

| Rune | Pre-implementation 30-Stone XP | Implemented at unlock | Implemented at level 99 | Level-99 ratio to previous |
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

Nature is the only implemented unlock value below its pre-implementation XP. Chaos is
unchanged at unlock. Law through Blood are already `2.61x` to `4.27x` their
current XP before any later output breakpoint; the multiplier then compounds
those increases.

## XP-Per-Stone Cross-Check

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

The full implemented model produces 22 optimal regimes, or 21 actual changes after the
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

This 22-regime result is correct for XP per Stone, but it is not the metric used
by the claimed 25 changes. The proposal intentionally optimizes XP per hour
after applying each altar's fastest Stone-supply route. That extra variable
breaks the level-1 tie in Water's favor and lets Nature's exceptional route
compete at several later breakpoints.

## Route-Normalized Evidence

The user-supplied current-rate estimate gives the following fastest loops. The
rates use normal `3x` displayed XP and already include the loop-specific Stone
throughput.

| Rune | Current maximum XP/hour | Fastest estimated loop |
| --- | ---: | --- |
| Nature | 134,600 | Direct; rock is 9 tiles from the altar |
| Blood | 75,300 | Mage Arena bank to altar; webs take about 2 seconds each |
| Soul | 68,500 | Yanille bank to altar; requires Magic 66 route access |
| Death | 51,800 | Direct; dungeon rock is 63 tiles away |
| Body | 44,400 | Direct; rock is 29 tiles away |
| Water | 30,400 | Direct; rock is 30 tiles away |
| Fire | 30,100 | Direct; rock is 43 tiles away |
| Mind | 27,300 | Direct; rock is 28 tiles away |
| Chaos | 25,700 | Edgeville bank to altar |
| Air | 21,200 | Direct; rock is 40 tiles away and barely beats the bank loop |
| Law | 20,800 | Bank, ferry, altar, then free return ship |
| Cosmic | 19,500 | Zanaris bank to altar |
| Life | 17,000 | Direct underground rock with ladder travel both ways |
| Earth | 15,900 | Varrock East bank to altar |

Nature currently earns `1.79x` Blood's rate and `1.96x` Soul's despite being a
lower-tier rune. That is the concrete grind-rate defect the proposal addresses.

For a consistency check, divide each pre-implementation hourly rate by its
pre-implementation XP per Stone to estimate that route's Stone throughput, then multiply by implemented XP
per rune and the level's base-output multiplier. The rounded current table
reproduces the same winning runes and level boundaries as the supplied
suggested-rate table; small rate differences come from the source calculation
retaining more timing precision.

The supplied suggested results are:

| # | Levels | Optimal rune | Base output | Maximum XP/hour |
| ---: | --- | --- | ---: | ---: |
| 1 | 1-7 | Water | 1x | 43,408 |
| 2 | 8-10 | Mind | 1x | 47,780 |
| 3 | 11-17 | Water | 2x | 86,817 |
| 4 | 18-20 | Mind | 2x | 95,559 |
| 5 | 21-27 | Water | 3x | 130,225 |
| 6 | 28-30 | Mind | 3x | 143,339 |
| 7 | 31-34 | Water | 4x | 173,633 |
| 8 | 35-37 | Body | 3x | 179,956 |
| 9 | 38-40 | Mind | 4x | 191,118 |
| 10 | 41-44 | Water | 5x | 217,042 |
| 11 | 45-47 | Body | 4x | 239,941 |
| 12 | 48-50 | Nature | 2x | 252,336 |
| 13 | 51-54 | Water | 6x | 260,450 |
| 14 | 55-57 | Body | 5x | 299,926 |
| 15 | 58-63 | Nature | 3x | 378,505 |
| 16 | 64-67 | Death | 2x | 414,247 |
| 17 | 68-71 | Nature | 4x | 504,673 |
| 18 | 72-73 | Soul | 2x | 509,848 |
| 19 | 74-77 | Death | 3x | 621,370 |
| 20 | 78-79 | Nature | 5x | 630,841 |
| 21 | 80-81 | Blood | 2x | 642,767 |
| 22 | 82-83 | Soul | 3x | 764,772 |
| 23 | 84-89 | Death | 4x | 828,493 |
| 24 | 90-91 | Blood | 3x | 964,151 |
| 25 | 92-93 | Soul | 4x | 1,019,695 |
| 26 | 94-99 | Death | 5x | 1,035,616 |

This is 26 route-normalized optimal regimes, producing 25 actual method
changes. The claim is supported. The alternation is also purposeful rather than
an XP-per-Stone anomaly: Water, Mind, Body, and Nature capitalize on accessible
Stone sources early and mid-game, while Death, Soul, and Blood converge as the
late-game choices.

The table is still based on estimated fastest-possible execution. Before a
balance implementation is finalized, reproduce a sample of the route timings
with consistent assumptions for movement cadence, Mining yield, rock respawn,
bank interaction time, web delays, and required teleports. Inventory sizes
below 30 and ordinary player execution will reduce all rates, while gathering
bonuses or banked Stone can raise supply throughput. These factors affect
absolute rates but do not invalidate the route-normalized design goal.

## Equipment-Generated Runes

Equipment output must remain outside the XP count:

- Law robes add `2%` production per total robe tier and can reach `100%` with
  the documented full tier-10 set. Fractional progress is carried per rune.
- Chaos amulets add `20%` to `100%` output when crafting Chaos runes and roll
  the bonus across Mind, Chaos, Death, and Blood runes.
- Both systems calculate from the same `baseRuneCount`. With maximum applicable
  bonuses, a Chaos action can produce base output plus up to `100%` Law-robe
  output and `100%` Chaos-amulet output.

Awarding XP on those bonus runes would turn equipment yield into another XP
multiplier, make randomized Chaos output affect training XP, and amplify the
already large late-level rates. The implementation names the base count
explicitly and awards XP before either equipment method runs. Regression tests
lock that ordering and the XP-eligible count.

## Implementation And Review Gate

Implementation status: the accepted table and base-output multiplication are
implemented for review.

The route-normalized evidence resolves the concern raised by the XP-per-Stone
cross-check. The pure-XP optimizer has 21 changes, but it ignores the different
Stone throughput that the redesign intentionally balances. Applying measured
route throughput produces the claimed 25 changes and replaces Nature's large
current lead with a late-game Death/Soul/Blood cluster whose rates are within
about `7.4%` of one another.

The runtime calculates XP from the base rune output before adding
equipment-generated runes. In conceptual internal units for one altar action:

```text
baseRunesCrafted = stonesProcessed * baseRuneMultiplier
actionXP = configuredXmlXP * baseRunesCrafted
```

The configured XML values are integral quarter-XP units, so this needs no
special per-Stone rounding. Keep the existing current-level boost behavior,
apply normal shared XP modifiers afterward, and exclude Law-robe and
Chaos-amulet output from `baseRunesCrafted`.

Before merging the balance implementation, time representative Nature, Death,
Soul, and Blood loops in-game and confirm the target ceiling. The proposal's
relative progression is supported, but its approximately `1.0 million
XP/hour` endgame rate is still an explicit product decision. If that absolute
ceiling is too high, uniformly scale the proposed per-rune table and rerun the
integer-quarter-XP and route-transition checks. Do not substitute the earlier
`0.25` marginal-output model unless the design goal changes; it was based on an
incomplete comparison that omitted route throughput.
