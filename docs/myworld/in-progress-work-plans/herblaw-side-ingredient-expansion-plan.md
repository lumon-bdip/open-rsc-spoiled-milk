# Herblaw Side Ingredient Expansion Plan

This plan extends the current MyWorld Herblaw potion families by giving higher
tiers better side ingredients instead of reusing the same secondary for every
version of a potion path.

It builds on the older
[`herblaw-potion-rework-plan.md`](../rough-drafts/herblaw-potion-rework-plan.md),
but focuses only on side-ingredient progression for the existing tiered potion
families.

## Goals

- Keep potion upgrade paths readable: herb determines potion tier, while side
  ingredient determines potion family.
- Stop every potion version in a family from using the exact same secondary.
- Keep each family tied to a matching acquisition theme:
  - Fishing and Cooking supply Deftness ingredients.
  - Combat supplies Insight ingredients.
  - Harvesting supplies Brawn ingredients.
- Reuse recently added items where they fit, especially `Zombie eye`.
- Turn unused decorative scenery, such as mushrooms, fungus, and ferns, into
  useful resource nodes where appropriate.
- Preserve quest-specific potion ingredients and quest items unless explicitly
  audited and moved.

## Current Live Shape

The live recipe table currently has six versions for each main potion family:

| Potion family | Current secondary | Live versions |
| --- | --- | --- |
| Potion of Brawn | `Limpwurt root` | v1-v6 |
| Potion of Deftness | `10 Fish oil` | v1-v6 |
| Potion of Insight | `Eye of newt` | v1-v6 |

The current herb ladder is:

| Version | Herb | Level |
| --- | --- | --- |
| v1 | Guam leaf | 3 |
| v2 | Tarromin | 12 |
| v3 | Ranarr weed | 30 |
| v4 | Avantoe | 50 |
| v5 | Cadantine | 66 |
| v6 | Torstol | 78 |

This plan targets six side-ingredient quality steps so every live potion version
gets its own matching secondary ingredient.

Preferred starting assumption:

- keep all six potion versions
- use six side-ingredient qualities
- each potion version gets a unique side-ingredient tier

## Proposed Ingredient Ladders

### Potion of Deftness

Theme: Fishing and Cooking.

Fish oil should mirror the quality of the fish used to create it. The existing
`Fish oil` item should be renamed to `Low quality fish oil`, then five higher
quality oils should be added.

Proposed oil names:

| Quality tier | Item name |
| --- | --- |
| 1 | Low quality fish oil |
| 2 | Fair quality fish oil |
| 3 | Good quality fish oil |
| 4 | Fine quality fish oil |
| 5 | High quality fish oil |
| 6 | Superior quality fish oil |

Proposed raw fish tier mapping:

| Fish tier | Raw fish sources | Oil roll skew |
| --- | --- | --- |
| 1 | shrimp, sardine | mostly low, small chance fair |
| 2 | herring | mostly fair, small chance low |
| 3 | anchovies, mackerel | mostly fair, small chance low or good |
| 4 | trout, cod | fair or good |
| 5 | pike, salmon, giant carp | mostly good, small chance fair or fine |
| 6 | tuna | good or fine |
| 7 | lobster | mostly fine, small chance good or high |
| 8 | swordfish, bass | fine or high |
| 9 | lava eel | mostly high, small chance fine or superior |
| 10 | shark, sea turtle, manta ray | mostly superior, small chance high |

Current cooking behavior gives the same `Fish oil` item from every raw fish.
Implementation should replace that with a helper that rolls an oil quality from
the raw fish tier.

Herblaw should still require `10` oil per Deftness potion unless the recipe
economy is deliberately retuned.

### Potion of Insight

Theme: Combat drops.

The ingredient line starts with `Eye of newt`, moves through spider and zombie
eyes, then climbs into stronger creature-family eyes.

Proposed eye ladder:

| Quality tier | Item name | Source direction |
| --- | --- | --- |
| 1 | Eye of newt | existing shops, seeds, and low-tier sources |
| 2 | Spider eye | spider-family monsters, excluding the level-2 spider |
| 3 | Zombie eye | zombies and target-practice zombies |
| 4 | Bat eye | bat-family monsters |
| 5 | Baby dragon's eye | baby blue dragons only |
| 6 | Demon eye | demon-family monsters |

Implementation notes:

- `Zombie eye` already exists and should be reused without changing its drop
  chances.
- `Spider eye`, `Bat eye`, `Demon eye`, and `Baby dragon's eye` need new item
  definitions.
- `Baby dragon's eye` should only drop from baby blue dragons and should match
  the blue dragon scale drop chance.
- Drop rates should keep higher-tier eyes valuable without making Insight
  potions feel impractical.
- Where an enemy family has multiple leveled versions, lower-level versions
  should have lower eye drop weights than higher-level versions.
- Quest usage of `Eye of newt` should remain intact.
- If any monster family has quest-specific variants, avoid adding new potion
  drops to those variants until audited.

### Potion of Brawn

Theme: Harvesting and natural scenery.

The Brawn line should move from one repeated root to a small ladder of
harvested natural ingredients. `Limpwurt root` remains the base item, while
unused plant/fungal scenery becomes higher-tier ingredients.

Known scenery candidates already present in object definitions:

- `Mushroom`
- `Fungus`
- `Fern`
- jungle fern variants
- small fern/plant variants

Proposed brawn ingredient ladder:

| Quality tier | Item name | Source direction |
| --- | --- | --- |
| 1 | Limpwurt root | existing harvesting and legacy sources |
| 2 | Mushroom | harvest from mushroom scenery |
| 3 | Fungus | harvest from fungus scenery |
| 4 | Fern | harvest from fern scenery |
| 5 | Open plant/fungal candidate | audit unused scenery and choose the best fit |
| 6 | Sixth plant/fungal candidate | audit unused scenery and choose the best fit |

The fifth and sixth Brawn ingredients should come from the same
Harvesting/resource theme. Candidate search should include unused plant,
fungus, fern, moss, root, and jungle scenery objects. Quest-only plants should
be left alone unless the quest code is audited first.

## Draft Recipe Matrix

This matrix assumes the six current potion versions remain live and each version
uses a unique side-ingredient tier.

| Potion version | Herb | Deftness secondary | Insight secondary | Brawn secondary |
| --- | --- | --- | --- | --- |
| v1 | Guam leaf | Low quality fish oil | Eye of newt | Limpwurt root |
| v2 | Tarromin | Fair quality fish oil | Spider eye | Mushroom |
| v3 | Ranarr weed | Good quality fish oil | Zombie eye | Fungus |
| v4 | Avantoe | Fine quality fish oil | Bat eye | Fern |
| v5 | Cadantine | High quality fish oil | Baby dragon's eye | fifth plant/fungal ingredient |
| v6 | Torstol | Superior quality fish oil | Demon eye | sixth plant/fungal ingredient |

## Item And Sprite Needs

New or changed items:

- Rename `Fish oil` to `Low quality fish oil`.
- Add five new fish oil quality items.
- Add `Spider eye`.
- Add `Bat eye`.
- Add `Demon eye`.
- Add `Baby dragon's eye`.
- Add `Mushroom` item if no suitable inventory item exists.
- Add `Fungus` item if no suitable inventory item exists.
- Add `Fern` or `Fern frond` item if no suitable inventory item exists.
- Add fifth and sixth Brawn ingredients after the scenery audit.

Sprite direction:

- Fish oils can reuse the fish oil sprite with palette or brightness variants.
- Eye items can reuse the `Eye of newt`/`Zombie eye` visual language with
  palette changes.
- Brawn ingredients can reuse or derive from the scenery object art where that
  produces readable inventory sprites.

## Implementation Touchpoints

Server:

- `server/conf/server/defs/extras/ItemHerbSecond.xml`
- `server/plugins/com/openrsc/server/plugins/authentic/skills/herblaw/Herblaw.java`
- `server/plugins/com/openrsc/server/plugins/authentic/skills/cooking/ObjectCooking.java`
- `server/conf/server/defs/ItemDefsCustom.json`
- `server/src/com/openrsc/server/constants/ItemId.java`
- relevant NPC drop tables for zombies, spiders, bats, demons, and baby dragons
- Harvesting/object interaction plugins for mushroom, fungus, fern, and the
  fifth and sixth Brawn candidates

Client:

- item definitions and overrides
- Herblaw skill guide text
- bank/Auction House category matching if new ingredients are not picked up by
  existing Herblaw filters

Tests:

- recipe table coverage for all potion family versions
- fish-to-oil quality mapping
- Deftness recipes still requiring the intended amount of oil
- guide text matching live recipes
- new item names and descriptions
- combat drop coverage for new eyes
- Harvesting/object coverage for new Brawn ingredients

## Implementation Order

1. Audit unused scenery and choose the fifth and sixth Brawn ingredients.
2. Add item IDs, names, descriptions, and sprites for the new side ingredients.
3. Rename current `Fish oil` to `Low quality fish oil`.
4. Replace cooking's single fish-oil output with quality-specific oil output.
5. Add combat drops for zombie, spider, bat, demon, and baby dragon eyes.
6. Add harvest interactions for mushroom, fungus, fern, and the fifth and sixth
   Brawn ingredients.
7. Update Herblaw recipes to use the new side-ingredient matrix.
8. Update skill guides, bank filters, and Auction House filters.
9. Add targeted tests for the recipe, guide, drop, harvesting, and oil changes.

## Open Decisions

- Exact names for oil quality tiers two through six.
- Exact fifth and sixth Brawn ingredients and source objects.
- Whether mushroom, fungus, and fern harvesting should require Harvesting
  levels, cooldowns, depletion visuals, or simple pickup behavior.
- Drop rates for Spider eye, Bat eye, Demon eye, and Baby dragon's eye.
- Whether higher side ingredients should increase Herblaw XP beyond the current
  herb-tier XP.
- Whether existing stored `Fish oil` should migrate automatically by item ID to
  `Low quality fish oil`.
