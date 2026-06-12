# New Ideas And Issues

This document is a lightweight intake list for ideas, regressions, and design
notes that are not ready to become scoped work in `work-items.md`.

Move an entry into `work-items.md` once it has a concrete fix plan, owner, or
release target.

## Open

### Aubury rune shop shortcut regression

Status: next after Herblaw.

Aubury, the rune seller in Varrock, still does not support the expected
right-click trade and `Ctrl`-click trade shortcut behavior. This needs to be
worked immediately after the Herblaw/potion planning pass, and the fix should
be verified in game rather than assumed from adding the NPC to a quick-trade
list.

Current expectation:

- Right-click should expose/use the rune shop trade action correctly.
- `Ctrl`-click shortcut should open trade/shop correctly.
- The final fix should account for why previous quick-trade-style fixes did not
  resolve Aubury.

### Brown apron / Crafting Guild access

Status: investigate.

The Crafting Guild entrance still requires the player to wear a brown apron.
The gate dialogue says:

- "Where's your brown apron?"
- "You can't come in here unless you're wearing a brown apron"

Current concern: the brown apron may have been removed, renamed, or made
unavailable during cleanup. The visible item data currently includes `Apron`
with item id `182`, description `A mostly clean apron`, and the brown apron
equipment animation still exists client-side. Confirm whether item `182` is
intended to be the brown apron, whether shops/drops still make it obtainable,
and whether the player-facing name should be restored to `Brown apron`.

Candidate fix:

- Keep Crafting Guild access working with item `182`.
- Restore player-facing naming/source availability if cleanup made the required
  apron unclear or unobtainable.
- Add a guard test for Crafting Guild access requirements and apron
  availability so this does not regress again.

### Hits XP combat focus

Status: design.

Bring back combat focus as a Hits XP preference that applies to every combat
style. Regardless of whether the player is using Melee, Ranged, or Magic, they
can choose how much combat XP is diverted into Hits:

- No Hits XP
- Some Hits XP
- More Hits XP
- All Hits XP

The likely implementation should preserve total combat XP and split it between
the active combat style and Hits, rather than adding bonus Hits XP on top.
