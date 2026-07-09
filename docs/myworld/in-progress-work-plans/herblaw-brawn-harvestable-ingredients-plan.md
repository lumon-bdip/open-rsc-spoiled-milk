# Herblaw Brawn Harvestable Ingredients Plan

Status: implemented-pending-manual-testing
Owner: An-actual-duck
Branch: no active branch; assign the next checklist item to a fresh topic branch in a neutral AI slot
Related plans:
- `docs/myworld/in-progress-work-plans/herblaw-side-ingredient-expansion-plan.md`

## Summary

Add a six-step Harvesting ingredient ladder for `Potion of Brawn` so each
Brawn potion version uses a distinct natural secondary instead of repeating
`Limpwurt root` for every tier.

This is an implementation plan, not an idea submission. The assets already
exist in `/home/justin/Core-Framework/output` and should be imported into the
repo during implementation:

- `fern-leaf.png`
- `mushroom.png`
- `fungus.png`
- `red-flower.png`
- `blue-flower.png`

`Limpwurt root` remains part of the six-ingredient Brawn family, but will no
longer be used by every Brawn potion version.

## Goal

Make early Brawn potion progression easier and clearer by using common
harvestable scenery for lower-tier ingredients while preserving rarer or more
special ingredients for later potion tiers.

## Scope

- Add inventory items for `Fern leaf`, `Mushroom`, `Fungus`, `Red flower`, and
  `Blue flower`.
- Use the provided PNGs as their inventory/ground sprites.
- Make the selected scenery objects harvestable through the existing Harvesting
  system and equipped-shears progression.
- Update the Brawn potion recipe ladder to use the new natural ingredients.
- Update Herblaw and Harvesting skill guides.
- Add focused tests for item definitions, scenery IDs, recipe mapping, guide
  text, and harvesting behavior.

## Non-Goals

- Do not change Deftness or Insight recipes in this pass.
- Do not add new map placements unless testing shows the live distribution is
  too sparse after conversion.
- Do not make quest-specific or generic `plant` scenery harvestable.
- Do not change Brawn potion effects, dose counts, boost values, or durations.
- Do not remove existing limpwurt non-scenery sources.

## Rarity Audit

Audit method:

- Counted final MyWorld-loaded scenery using `based_map_data: 64`,
  `location_data: 2`, and the MyWorld config's enabled scenery overlays.
- Included base `SceneryLocs.json`, discontinued fixes, mod room, runecraft,
  harvesting, custom quest, expansion, woodcutting guild, other scenery, and
  `MyWorldSceneryLocs.json`.
- Applied `MyWorldSceneryRemovals.json` before adding MyWorld scenery locs,
  matching `WorldPopulator`.
- Omitted event-only `SceneryLocsMiceToMeetYou.json`.

| Candidate | Object IDs | Live count | Notes |
| --- | ---: | ---: | --- |
| Fern leaf | `34`, `396`, `397`, `398`, `399`, `401`, `402` | `2457` | Very common. Includes normal and jungle objects named `Fern`. |
| Fungus | `205` | `552` | Common but mostly dungeon/cave-flavored. |
| Mushroom | `38` | `476` | Common natural starter candidate. |
| Red flower | `37` | `338` | Displays as `Flower` and uses model `flower`. Object `188` was excluded after implementation audit because Pirate's Treasure checks that exact flower for spade use. |
| Blue flower | `285` | `123` | Displays as `flower` and uses model `blueflower`; rarest new scenery candidate. |
| Limpwurt root | `1281` | `4` | Very rare as scenery, but has existing drops, pouches, seed, certificate, and quest/source history. |

Excluded candidates:

- `plant` / `smallfern` object `286` is generic and visually close to ferns,
  but it is not named `Fern`.
- `Baby Yommi Tree` object `1112` uses `smallfern` but is quest-flavored and
  must remain untouched.
- `Flower` object `188` is the Pirate's Treasure Falador flower and must keep
  its quest interaction.
- Other plant, fly trap, moss, or quest-jungle objects are left for a later
  audit.

## Ingredient Tier Decision

The raw scenery-count order would put limpwurt at the very top because only
four limpwurt scenery nodes are loaded. However, limpwurt also has established
non-scenery acquisition paths and the owner already indicated it likely belongs
around the fourth Brawn tier rather than the first. This plan therefore treats
limpwurt as a mid-high ingredient and reserves blue flower as the rarest new
scenery-only ingredient.

| Brawn version | Herb | Herblaw level | Brawn secondary | Harvesting gate | Source object IDs |
| --- | --- | ---: | --- | ---: | --- |
| v1 | Guam leaf | `3` | Fern leaf | `1` / T1 shears | `34`, `396`, `397`, `398`, `399`, `401`, `402` |
| v2 | Tarromin | `12` | Mushroom | `8` / T2 shears | `38` |
| v3 | Ranarr weed | `30` | Fungus | `30` / T5 shears | `205` |
| v4 | Avantoe | `50` | Limpwurt root | `38` / T6 shears | `1281` plus existing item sources |
| v5 | Cadantine | `66` | Red flower | `54` / T8 shears | `37` |
| v6 | Torstol | `78` | Blue flower | `70` / T10 shears | `285` |

This gives early Brawn access a common level-1 scenery resource while keeping
the high-end Brawn recipes tied to rarer or higher-gated resources.

## Recipe Matrix

| Potion | Current secondary | Planned secondary |
| --- | --- | --- |
| `Potion of Brawn v1` | `Limpwurt root` | `Fern leaf` |
| `Potion of Brawn v2` | `Limpwurt root` | `Mushroom` |
| `Potion of Brawn v3` | `Limpwurt root` | `Fungus` |
| `Potion of Brawn v4` | `Limpwurt root` | `Limpwurt root` |
| `Potion of Brawn v5` | `Limpwurt root` | `Red flower` |
| `Potion of Brawn v6` | `Limpwurt root` | `Blue flower` |

## Implementation Notes

Use the same broad implementation style as the current Harvesting and Herblaw
systems.

- Add new item constants after the current custom item range.
- Add server item definitions in `ItemDefsCustom.json`.
- Add client item definitions in `EntityHandler.java`.
- Import the five PNGs into the appropriate repo sprite asset location, then
  wire item definitions to those sprite paths.
- Add `SceneryId` constants only where missing; existing constants already
  cover the candidate object IDs.
- Change target scenery commands from passive `WalkTo`/`Examine` to primary
  `Harvest` for the new fixed-output nodes so small type-0 objects also get
  the client expanded click bounds.
- Extend `ObjectHarvesting.xml` for the new fixed-output plant secondaries.
- Preserve shears gating through the existing Harvesting tool-tier checks.
- Use the existing damaged-ground depleted visual for the new fixed-output
  plant secondaries.
- Update `ItemHerbSecond.xml` so each Brawn potion version uses the planned
  secondary.
- Update `SkillGuideInterface.java` so the Herblaw guide names each Brawn
  secondary and the Harvesting guide lists the new harvestables with their
  shears tiers.
- Ensure bank/Auction House Herblaw filtering catches the new ingredients.

## Affected Systems

Server:

- `server/src/com/openrsc/server/constants/ItemId.java`
- `server/src/com/openrsc/server/constants/SceneryId.java`
- `server/conf/server/defs/ItemDefsCustom.json`
- `server/conf/server/defs/GameObjectDef.xml`
- `server/conf/server/defs/extras/ItemHerbSecond.xml`
- `server/plugins/com/openrsc/server/plugins/custom/skills/harvesting/Harvesting.java`
- `server/plugins/com/openrsc/server/plugins/authentic/skills/herblaw/Herblaw.java`
- bank/Auction House filter helpers if needed

Client:

- `Client_Base/src/com/openrsc/client/entityhandling/EntityHandler.java`
- `Client_Base/src/com/openrsc/interfaces/misc/SkillGuideInterface.java`
- sprite asset directories used by custom item definitions

Tests:

- `tests/myworld/test-herblaw-remap.py`
- `tests/myworld/test-herblaw-side-ingredients.py`
- `tests/myworld/test-herblaw-brawn-harvestables.py`
- `tests/myworld/test-harvesting-legacy-crops.py`
- `tests/myworld/test-skill-guides.py`
- bank filter tests if matching needs explicit coverage

## Implementation Checklist

- [x] Import the five provided PNGs into the repo.
- [x] Add item IDs/constants for `Fern leaf`, `Mushroom`, `Fungus`,
  `Red flower`, and `Blue flower`.
- [x] Add server item definitions with Herblaw ingredient descriptions.
- [x] Add client item definitions pointing at the imported sprites.
- [x] Update the selected scenery definitions to expose a harvestable command.
- [x] Add harvesting output rules for all selected object IDs.
- [x] Apply the planned Harvesting level/shears gates.
- [x] Update the six Brawn `ItemHerbSecond.xml` recipes.
- [x] Update Herblaw guide Brawn text to show six distinct secondaries.
- [x] Update Harvesting guide with the new ingredient unlocks.
- [x] Update bank/Auction House filters if the new item names are not
  automatically captured by existing Herblaw rules.
- [x] Add/extend regression tests.

## Testing Checklist

- [x] Run Herblaw recipe tests.
- [x] Run Herblaw side-ingredient tests.
- [x] Run Harvesting tests.
- [x] Run skill guide tests.
- [x] Compile server Java.
- [x] Compile/build client if sprite wiring changes require it.
- [ ] Manually verify at least one node from every new object family can be
  harvested, depletes, respawns, and gives the intended item.
- [ ] Manually verify all six Brawn potions craft with the intended secondary.

## Decisions

- `Fern leaf` is the starter ingredient because fern scenery is extremely
  common and fits the low-tier Harvesting role.
- `Blue flower` is the top new scenery ingredient because it is much rarer
  than the other new decorative candidates.
- `Limpwurt root` stays at Brawn v4 despite low scenery count because it has
  existing non-scenery supply paths and was already called out as a likely
  midline Brawn ingredient.
- Generic `plant` and quest-flavored `Baby Yommi Tree` objects are excluded.
- Pirate's Treasure object `188` is excluded even though it shares the red
  flower model, because its spade interaction is quest content.

## Open Questions

- Should the new Brawn ingredients be noteable, or should they behave like most
  small Herblaw secondaries?
- Should the new harvestables receive seed/resource-plant support in this same
  pass, or remain scenery-only at first?
- Should the higher-tier Brawn secondaries increase Herblaw XP, or keep XP tied
  only to the potion version as it is now?
- Is `DAMAGED_GROUND` acceptable as the shared depleted visual for all five new
  scenery families?

## Completion Criteria

- The five new PNG assets are committed in the repo and used by inventory item
  definitions.
- All six Brawn potion tiers use the planned secondary ingredients.
- The selected scenery objects are harvestable with the intended shears gates.
- Skill guides match live recipe and Harvesting requirements.
- Automated coverage passes for Herblaw recipes, Harvesting definitions, item
  definitions, and guide text.
- Manual client testing confirms the new items and harvest nodes are visible
  and usable.
