# B09 Client Definition Registry Extraction

This branch separates client definition storage, indexed access, prayer-book
authorship, and fallback diagnostics without changing the definition load order
or any client-visible definition value.

## Ownership Boundaries

`ClientDefinitionRegistry` now owns:

- the NPC, item, texture, animation, projectile, GUI-part, crown, spell,
  prayer, tile, door, elevation, object, and model catalogs;
- index-based counts and lookups for those catalogs;
- inventory-sprite count accounting and model-name interning; and
- the existing NPC, item, animation, and object fallback rules.

`PrayerBookDefinitions` owns all 48 Saradomin, Zamorak, and Guthix prayer
entries plus active-book selection. `ClientDefinitionFallbackDiagnostics` owns
deduplication and emission of malformed item-ID diagnostics.

`EntityHandler` remains the public compatibility facade and the authored base
definition source. Its public lookup methods delegate to the registry, while
its loader methods populate package-private mutable catalog views. The existing
public NPC, projectile, GUI-part, and crown lists remain aliases of the same
registry-owned lists because active client consumers still use them directly.
They were not replaced or made read-only on this behavior-preserving branch.

The already separate generated `MyWorldItemOverrides` input remains unchanged
and is still applied immediately after base item definitions. MyWorld-authored
base entries, generated definition families, spells, NPC overrides, and every
other loader retain their previous order in `EntityHandler`.

This extraction reduces `EntityHandler` from 9,778 to 9,629 lines. It does not
move thousands of authored base definitions merely to reduce the file length;
those blocks can be divided by definition family only after an equally strict
source-generation and index-parity boundary is designed.

## Index and Behavior Preservation

The characterization fixture hashes every non-static field of every loaded
definition and compares it with fingerprints recorded from published main
commit `7305b45c02f1a6f62d54ebce2ba2972f53412537`. It covers:

- 845 NPCs, 3,281 items, 55 textures, and 1,060 animations;
- 41 projectiles, 54 GUI parts, 5 crowns, and 46 spells;
- all three 16-entry prayer books;
- 26 tiles, 214 doors, 6 elevations, 1,329 objects, and 459 models; and
- the two note/certificate presentation templates.

The published-main and branch fingerprints match exactly for every catalog.
The fixture additionally proves that every item ID and every projectile,
GUI-part, and crown sprite ID still equals its list index. Invalid item lookups
retain fallback item 1544, including the existing negative noted-item encoding.
Malformed NPC, animation, and object lookups retain fallbacks 825, 0, and 4.

Some inherited NPC `id` fields and animation `number` fields do not equal their
list positions. B09 deliberately preserves those values and fingerprints; it
does not reinterpret them as registry defects or silently renumber content.

Item fallback diagnostics remain bounded to one line per distinct malformed
lookup. They contain only requested/resolved numeric IDs, noted state, reason,
and catalog size. They do not include item text, player data, credentials, or
other sensitive values.

## Automated Verification

`tests/myworld/test-client-definition-registry-extraction.py` builds the real
client and executes the parity fixture. It also guards the ownership boundary,
all 48 prayer definitions, generated-override load order, invalid-ID fallback
identity, and exact deduplicated fallback-log shape.

The following verification passed on the extraction checkpoint:

- generated item/NPC definition checks, item-ID integrity, runtime item
  definitions, item asset coverage, sprite-reference coverage, NPC visuals,
  and animation migrations;
- bank filters, shortcuts, wide slots, tooltips, and charged-item preservation;
- shop definitions/layouts and equipment slots, visuals, and hover data;
- prayer definitions/UI/faction behavior, spellbook layouts, elemental and
  dual-element spell metadata, spell costs, and tier-two spell coverage;
- remastered sprite workspace/loader, summoning animation, white-cape, and
  wood-crafting client-definition guards;
- player release packaging and `./scripts/build-client.sh`; and
- `./scripts/lint.sh all --offline --base spoiled-milk/main`, with no new
  gated compiler or SpotBugs findings and no PMD, ShellCheck, Ruff, or
  changed-file Checkstyle findings.

## Required Private Runtime Verification

The branch must not be handed off until the owner confirms this matrix on the
loopback-only development server and branch-built client:

1. Log in and load a populated area; the player, NPCs, terrain, scenery,
   ground items, animations, and projectiles appear normally.
2. Open inventory and bank interfaces, inspect ordinary and noted items, and
   confirm names, sprites, quantities, and tooltips remain correct.
3. Open a shop and confirm item names, sprites, stock, and prices. Equip and
   unequip representative armor and weapons and inspect their worn visuals.
4. Open the prayer and magic tabs and inspect representative entries, icons,
   levels, descriptions, and book layout. Switch prayer books if an available
   private-world altar permits it.
5. Log out, reconnect, and close the client normally. Inspect the private
   client log for unexpected exceptions or `CLIENT_ITEM_DEF_FALLBACK` lines.

Private runtime status: **pending owner confirmation**.

Malformed-ID fallback behavior is covered by an isolated automated fixture and
must not be induced through normal gameplay or server data.

The public server and detached live checkout are outside this branch and must
remain untouched.
