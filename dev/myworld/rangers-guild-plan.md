# Rangers Guild Plan

## Goal

Repurpose the mostly empty Seers Party Hall building at `494,467` into a real gameplay space: the Rangers Guild.

The guild should become the main late-game ranged hub, with:

- Ranged master and Ranged cape access.
- Standard ranged supply shop continuity for Varrock.
- Dragon ranged weapon and ammo vendors.
- High-end ranged ammo and equipment vendors.
- Bank access near the high-end vendors.
- A caged ranged-training basement with ammo-focused enemy drops.

## Existing Space

Current building identity in code: `Seers Party Hall`.

Current bounds:

- Ground floor: `490,464` to `500,471`
- Upper floor: `490,1408` to `500,1415`

Current static contents:

- Ground floor:
  - `stairs` at `490,466`
  - barrels at `500,467` and `500,464`
  - fireplace at `496,471`
  - basement stairs at `499,469`
- Upper floor:
  - `stairs` at `490,1410`

Current non-static/event use:

- `Event.toggleSeersParty` can spawn a temporary party chest at `495,467` or `495,1411`.
- `SeersPartyChest` lets players add tradable items to that chest and drops them randomly in the party hall bounds.
- Telegrab is blocked on upstairs party-hall items.

Repurposing work should either remove this party-hall behavior from the area or move it elsewhere. Leaving the party chest behavior active would conflict with the new guild identity and could create odd item-drop behavior in a guild shop/training space.

## Access Requirement

Entry requirement: `66 Ranged`, matching the Wizard Guild style of access.

Implementation options:

- Add a guild guard or door check at the building entrance.
- Add a guard dialogue near the entrance and block entry if the player has less than 66 Ranged.
- If the building currently has open physical access, add a clear gate/door boundary or an NPC guard interaction so the restriction is enforceable.

Player-facing rejection text should be direct, for example:

> You need level 66 Ranged to enter the Rangers Guild.

## Ground Floor

Purpose: normal guild floor, Ranged master, standard identity, and transition to upper floor/basement.

First-pass scenery layout:

- `490,464`: display shield, oriented west-wall style.
- `491,471`: bookcase, rotated 180 degrees from the first pass.
- `494,471`: wall bench near the fireplace side of the room.
- `494,468`: stool.
- `495,468`: small round table.
- `496,468`: stool.
- `498,469`: basement stairs down, rotated 180 degrees from the first pass; footprint occupies `498-499`.

Existing objects preserved:

- `490,466`: stairs up.
- `496,471`: fireplace.
- `500,467` and `500,464`: ale barrels.

Notes:

- The old party chest spawn tile at `495,467` is intentionally left clear until the party-hall behavior is removed or relocated.
- The stairs and nearby approach tiles are left open.

NPC changes:

- Move `Lowe` (`NpcId.LOWE`, id `58`) from Varrock to this guild.
- Lowe is currently the Ranged cape provider through `LowesArchery`.
- Keep Lowe as the Ranged master and cape seller, but change his dialogue to fit the Rangers Guild instead of the Varrock archery shop.
- Remove Lowe's current trade/shop behavior from the moved guild version unless we intentionally want him to sell regular stock there too.

Varrock replacement:

- Add a new standard shopkeeper NPC in Lowe's current Varrock shop.
- Give this NPC a unique name.
- Give this NPC Lowe's current shop stock.
- This replacement should not sell the Ranged cape.
- This replacement should have standard archery-shop dialogue.

Current Lowe stock to preserve for the Varrock replacement:

- Tin through mithril arrows.
- Copper through mithril bolts.
- Tin through mithril arrow heads.
- Shortbows and longbows through yew plus current custom tiers in that shop.
- Crossbows through yew plus current custom tiers in that shop.

Scenery goals:

- Add archery targets, bow racks, arrow barrels, weapon racks, crates, guild signage, and simple training decor.
- Keep paths clear around stairs, entrance, and NPC interaction tiles.

## Upper Floor

Purpose: high-end vendors and bank access.

First-pass scenery layout:

- `496,1408`: archery target, rotated 180 degrees from the first pass.
- `498,1408`: archery target, rotated 180 degrees from the first pass.
- `493,1415`: future vendor counter.
- `496,1415`: future vendor counter.
- `499,1415`: bank chest.
- `500,1412` and `500,1413`: supply crates.

Existing objects preserved:

- `490,1410`: stairs down.

Notes:

- The old party chest spawn tile at `495,1411` is intentionally left clear until the party-hall behavior is removed or relocated.
- The upper floor is more shop/training themed than the ground floor.

Required objects:

- Bank chest.
- Stairs back down to ground floor.
- High-end ranged shop decor: target dummies, crates, bow/arrow racks, maybe a guild ledger/table.

Vendor 1: Dragon ranged vendor

Known existing dragon ranged items:

- `DRAGON_ARROWS` (`1449`)
- `POISON_DRAGON_ARROWS` (`1450`)
- `DRAGON_BOLTS` (`1451`)
- `POISON_DRAGON_BOLTS` (`1452`)
- `DRAGON_CROSSBOW` (`1453`)
- `DRAGON_LONGBOW` (`1454`)

Notes:

- Dragon darts, throwing knives, and shuriken do not appear to currently exist in `ItemId`.
- If those are desired later, they should be separate item-creation work before adding them to this vendor.

Vendor 2: High-end ranged ammo and equipment vendor

Intended stock bands:

- Rune ranged ammo where it exists.
- Titan Steel ranged ammo and thrown weapons.
- Adamantite ranged ammo and thrown weapons.
- Orichalcum ranged ammo and thrown weapons.
- High-tier bows/crossbows as appropriate, especially if they are not already easy to buy elsewhere.

Candidate stock families:

- Arrows: rune, titan steel, orichalcum.
- Bolts: rune, titan steel, adamantite, orichalcum.
- Darts: rune, titan steel, adamantite, orichalcum.
- Throwing knives: rune, titan steel, adamantite, orichalcum.
- Shuriken: rune, titan steel, adamantite, orichalcum.
- Poisoned versions can be included if the design goal is convenience; otherwise keep poison application in Herblaw to preserve production value.

Open balance decision:

- Decide whether this vendor sells poison ammo directly or only base ammo.
- Current dragon poison arrows/bolts already exist, so dragon poison ammo should be explicitly decided rather than accidentally omitted or over-included.

## Basement Training Floor

Purpose: caged ranged-training space with ammo-oriented drops.

This should be a new lower level, separate from the existing ground and upper floors. It needs a new stair/ladder connection from the ground floor.

First-pass basement seed:

- Temporary underground plane starts around `496,3294` to `503,3301`.
- `499,3296`: return stairs up.
- Ground-floor stairs at `498,469` route to `499,3295`; the 2x3 stair footprint uses overlay `8` to read visually as a down-stair opening.
- Basement stairs at `499,3296` route back to `499,468`.
- The central stair square is a solid grey overlay (`5`) with no ground texture, keeping it visually distinct from the cage floors.
- The current basement layout has two large cage areas and three smaller side areas, with no gates by design.

Basement training population:

- North large cage: 12 level-32 zombies (`id 68`).
- South large cage: 12 level-31 skeletons (`id 45`).
- Left merged side cage: 8 chaos dwarfs (`id 190`).
- Upper-right small cage: 4 ice giants (`id 135`).
- Lower-right small cage: 4 lesser demons (`id 22`).

Drop implementation note:

- Prefer an area-based Rangers Guild basement bonus-drop hook over cloned NPC IDs. The NPCs can remain stock monsters, while the guild basement grants the extra ammo table only for kills inside this coordinate range.
- Cloned NPCs are still viable, but they add new IDs and duplicate definitions for visually identical creatures. That is more maintenance unless the NPCs need different names, stats, examine text, or behavior outside drops.

Technical requirement:

- Cages must prevent NPCs from reaching the player while still allowing ranged attacks through.
- If the chosen fence/wall boundaries block projectiles, add or choose projectile-friendly cage boundaries.
- Validate that arrows, bolts, darts, knives, shuriken, and thrown projectile visuals can pass through the cage boundaries.

Enemy variants:

Create cloned NPC variants rather than changing global enemies. These should reuse standard sprites/combat behavior but have ranged-guild drop tables.

Suggested names:

- Guild zombie
- Guild skeleton
- Guild ice giant
- Guild lesser demon

Training layout:

| Enemy | Level | Cage layout | Count | Ammo drop band |
|---|---:|---|---:|---|
| Zombie | 32 | 2 cages of 6 | 12 | Bronze to steel |
| Skeleton | 54 | 2 cages of 4 | 8 | Iron to mithril |
| Ice giant | 68 | 1 cage of 4 | 4 | Steel to adamantite |
| Lesser demon | 87 | 1 cage of 2 | 2 | Mithril to orichalcum |

Drop design:

- Drops should strongly favor ammo, not general gold-value loot.
- Ammo drops should be frequent enough that ranged training feels self-sustaining.
- Higher-tier enemies should have better ammo bands but fewer spawn slots.
- Avoid making this an all-purpose money farm by limiting unrelated drops.
- Consider including arrows, bolts, darts, throwing knives, and shuriken in each appropriate tier band.

## Seers Party Hall Cleanup

Since the area becomes the Rangers Guild:

- Rename area labels from `Seers Party Hall` to `Rangers Guild`.
- Remove or relocate the `toggleSeersParty` command behavior.
- Remove the Seers party chest item-use behavior from this area, unless the party hall is moved elsewhere.
- Remove the upstairs telegrab restriction if it only existed for the party chest.
- Update any messages that refer to the party hall.

## Implementation Order

1. Repurpose area identity and remove party-hall conflicts.
2. Add access restriction and entrance handling for 66 Ranged.
3. Move Lowe to the guild and split his shop/cape responsibilities.
4. Add the Varrock replacement shopkeeper with Lowe's current shop stock.
5. Add ground-floor guild scenery and NPC placements.
6. Add upper-floor bank chest, dragon vendor, high-end ammo/equipment vendor, and scenery.
7. Create basement map/objects/stairs.
8. Add caged enemy variants and ammo-focused drop tables.
9. Validate projectile clipping through cages.
10. Add tests for NPC/shop placement, area naming, access requirement, and drop-table wiring.

## Tests And Validation

Recommended checks:

- Lowe no longer appears in Varrock after the move.
- New Varrock shopkeeper appears and opens the old archery shop.
- Lowe appears in the Rangers Guild and still sells the Ranged cape to eligible players.
- Players below 66 Ranged cannot enter.
- Players with 66+ Ranged can enter.
- Upper-floor bank chest works.
- Dragon vendor opens the intended dragon ranged shop.
- High-end vendor opens the intended high-end ranged shop.
- Basement stairs route correctly both directions.
- Caged enemies cannot path out to the player.
- Player ranged attacks can hit caged enemies.
- Scythe/shuriken style multi-target behavior does not hit summons or unintended NPCs through unrelated cages.
- Ammo drops use the intended tier bands.

## Open Questions

- What should the new Varrock archery shopkeeper be named?
- Should the dragon vendor sell poisoned dragon ammo, or should poisoned ammo remain player-made?
- Should the high-end vendor sell poisoned ammo directly?
- Should dragon darts, dragon throwing knives, or dragon shuriken be created later?
- Should the old Seers Party Hall event feature be removed completely or moved to another empty building?
