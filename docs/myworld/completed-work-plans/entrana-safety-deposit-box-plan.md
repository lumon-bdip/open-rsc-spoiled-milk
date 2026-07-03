# Entrana Safety Deposit Box Plan

Status: implemented. The live pass includes shared `EntranaRestrictions`, monk
restriction reuse, Safety Deposit Box object `1326`, placement at `266,660`,
persistent per-player cache snapshots, and guarded deposit/withdraw behavior.

## Goal

Add a per-player Safety Deposit Box near the Port Sarim Entrana ship at `266,660`.
The box should make Entrana travel less tedious by temporarily holding only items
that would fail the Entrana monk restriction check.

This must not become extra bank storage. The box should hold one snapshot at a
time, refuse additional deposits while occupied, and restore all stored items in
one all-or-nothing withdrawal.

## Player Experience

- Place a chest-like object at `266,660`.
- Primary action: `Deposit`.
- Right-click action: `Withdraw`.
- `Deposit` scans the player's inventory and equipment for Entrana-blocked
  items.
- If no blocked items are found, message: `You have nothing that needs to be deposited.`
- If the box already contains items, message: `The box already contains items.`
- `Withdraw` restores the snapshot if possible.
- If the box is empty, message: `There is nothing in the box.`
- If withdrawal cannot fit all items, message: `You do not have enough room to withdraw everything.`

Optional client polish:

- If object definitions can safely vary per state, hide `Deposit` while occupied
  and show `Withdraw`.
- If object state cannot vary per player, always expose both actions and enforce
  the behavior server-side. This is acceptable and simpler.

## Entrana Restriction Reuse

Current restriction logic lives in:

`server/plugins/com/openrsc/server/plugins/authentic/npcs/portsarim/MonkOfEntrana.java`

Relevant methods are currently private:

- `itemIsBlocked(Player, Item)`
- `playerNotAllowedOnEntrana(Player)`
- `hasCombatStats(ItemDefinition)`
- `isGatheringTool(ItemDefinition)`

The logic also depends on local arrays:

- `blockedItems`
- `blockedItemsOnlyOnCabbage`
- `blockedItemsCustom`

Plan:

1. Extract the restriction logic to a shared helper, for example:
   `server/plugins/com/openrsc/server/plugins/shared/EntranaRestrictions.java`
2. Provide:
   - `public static boolean itemIsBlocked(Player player, Item item)`
   - `public static boolean playerNotAllowedOnEntrana(Player player)`
3. Update `MonkOfEntrana` to call the shared helper.
4. Use the same helper from the Safety Deposit Box plugin.

This prevents the box and monk restrictions from drifting.

## Snapshot Model

Use a persistent player cache entry, not session-only attributes.

Store only enough data to reconstruct the snapshot:

- Item id
- Amount
- Noted state if applicable
- Source kind: inventory or equipment
- Original inventory slot for inventory items
- Original equipment slot for equipment items

Candidate cache key:

`entrana_safety_deposit_box`

Suggested serialized format:

`version|entries`

Each entry can be compact, for example:

`I,itemId,amount,noted,slot`

`E,itemId,amount,noted,equipmentSlot`

Use a version prefix so the format can be changed later without ambiguous
parsing.

## Deposit Rules

Deposit should be all-or-nothing.

1. If the cache key exists and is non-empty, reject deposit.
2. Scan inventory for items where `EntranaRestrictions.itemIsBlocked(player, item)` is true.
3. Scan equipment for blocked items when equipment tab support is enabled.
4. If no blocked items are found, reject with the no-items message.
5. Build the complete snapshot first.
6. Remove every snapshotted item from its original source.
7. Write the cache snapshot only after successful removal.

Important detail:

The existing monk logic checks equipment only when `WANT_EQUIPMENT_TAB` is true.
The shared helper should preserve that behavior unless we intentionally decide
that the box should always inspect equipment. The safer first pass is to match
the monk exactly.

## Withdrawal Rules

Withdrawal should be all-or-nothing and should not clear the snapshot until every
item has been restored.

1. If the cache key is absent or empty, reject.
2. Parse and validate the snapshot.
3. Calculate inventory space required:
   - Inventory-sourced entries require inventory space unless exact slot restore
     is available and the original slot is empty.
   - Equipment-sourced entries require no inventory space if their original
     equipment slot is empty and the item can be equipped there.
   - Equipment-sourced entries require inventory space if their original slot is
     occupied or cannot be restored directly.
4. If required inventory space exceeds free inventory space, reject and leave the
   snapshot untouched.
5. Restore equipment entries to original equipment slots where possible.
6. Put displaced or non-restorable equipment entries into inventory.
7. Restore inventory entries to original slots if the inventory API supports it;
   otherwise add them to inventory normally.
8. Clear the cache snapshot after all restores succeed.

## Inventory And Equipment API Checks

Before implementation, inspect these classes for exact-slot support:

- `server/src/com/openrsc/server/model/container/Inventory.java`
- `server/src/com/openrsc/server/model/container/Equipment.java`
- `server/src/com/openrsc/server/model/container/CarriedItems.java`

If exact inventory slot insertion is awkward or unsafe, prefer normal inventory
add over brittle slot manipulation. The important guarantee is no item loss and
no bank-like repeated storage.

Equipment restore should use existing equip/unwield helpers where possible so
appearance, stat recalculation, and packets stay correct.

## Object Placement

Add a chest-like object at `266,660`.

Implementation choices:

- Reuse an existing chest scenery id if the object definition already supports
  `Deposit`/`Withdraw`, or
- Add a custom scenery definition named `Safety Deposit Box`.

The server-side plugin should key behavior by object id and coordinates to avoid
changing every similar chest in the world.

## Abuse Prevention

- One snapshot per player.
- Deposit refuses if already occupied.
- Deposit only accepts items blocked by Entrana restrictions.
- Withdraw is all-or-nothing.
- No partial withdrawals.
- No manual item selection.
- No bank interface.
- Snapshot persists across logout to avoid item loss.

Open question:

- Should the snapshot expire or auto-withdraw if the player leaves Port Sarim?
  Recommendation: no expiration in the first pass. Expiration creates item-loss
  risk unless implemented very carefully.

## Tests

Add a focused MyWorld test, for example:

`tests/myworld/test-entrana-safety-deposit-box.py`

Coverage:

- Shared restriction helper exists and `MonkOfEntrana` uses it.
- Safety Deposit Box plugin uses the shared helper.
- Object placement exists at `266,660`.
- Object supports `Deposit` and `Withdraw` behavior.
- Deposit rejects an already occupied box.
- Deposit stores inventory and equipment entries with source slots.
- Deposit only stores Entrana-blocked items.
- Withdraw checks inventory space before restoring.
- Withdraw clears the cache only after restore.
- Empty withdraw reports there is nothing in the box.

Build verification:

- `python3 tests/myworld/test-travel-shortcuts.py`
- `python3 tests/myworld/test-entrana-safety-deposit-box.py`
- `./scripts/build-server.sh`

## Implementation Order

1. Extract `EntranaRestrictions` helper and update `MonkOfEntrana`.
2. Add regression coverage proving monk travel still uses the same restriction.
3. Add object definition and placement at `266,660`.
4. Add Safety Deposit Box plugin skeleton with empty `Deposit`/`Withdraw` messages.
5. Implement persistent snapshot serialization.
6. Implement deposit scan and all-or-nothing removal.
7. Implement withdrawal capacity checks.
8. Implement restoration and cache clearing.
9. Run targeted tests and server build.

## Risks

- Inventory exact-slot restoration may not be supported cleanly.
- Equipment restore must trigger the same side effects as normal equipping.
- Serialized cache parsing must be defensive; invalid snapshots should fail
  closed and not delete player items.
- Duplicate stackable items may need careful amount handling during removal and
  restoration.
- Existing dirty worktree changes should be kept separate from this feature.
