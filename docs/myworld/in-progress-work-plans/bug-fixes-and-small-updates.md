# Bug Fixes And Small Updates

Status: ongoing
Owner: An-actual-duck
Branch: use short-lived `fix/...`, `balance/...`, `content/...`, or `docs/...`
branches

## Purpose

This is the ongoing maintenance queue for small bugs, small balance changes,
minor content tweaks, documentation updates, and quick quality-of-life work.

This document is permanent. The branches used to complete items should be
temporary.

## Workflow

1. Add the bug or small update to the appropriate section.
2. Create a focused branch for that item.
3. Fix the item.
4. Test the fix.
5. Open a pull request into `main`.
6. After merge, move the item to the completed section or remove it if it was
   only useful as a temporary reminder.

## Branch Examples

```text
fix/bank-filter-jewelry-materials
fix/prayer-tab-points
balance/dragon-smithing-levels
content/fishing-guild-shop-cleanup
docs/player-command-list
```

## Open Bugs

- [ ] Add new bugs here.

## Small Updates

- [ ] Execute the cross-system "Fixes and Changes" queue through independent
      focused branches. The preserved backlog, dependency audit, execution
      order, and implementation-ready first camera task live in
      [fixes-and-changes-plan.md](fixes-and-changes-plan.md).

## Recently Completed

- [x] `2026-07-13` Fix the Experience Config `Select` crash when Enchanting,
      Harvest, and Summoning produce 21 protocol skill slots. All three copied
      selectors now size their lists from the runtime selectable skill IDs,
      omit retired Firemaking ID `11`, and translate displayed rows back to
      their original skill IDs before tracking XP. This omission is UI-only:
      ID `11` remains in the protocol/server arrays at hidden level `99`, so
      standard and custom Firemaking actions retain access to every log tier.

- [x] `2026-07-12` Preserve scenery and walls across deferred death/respawn
      area loads. The production legacy scene packets can arrive while the
      death screen still blocks the hard region load; the later load formerly
      cleared those already-delivered instances, while the server correctly
      considered its local scene current and did not resend them. Legacy
      records received for the pending area-load generation are now retained,
      shifted to the newly loaded origin, and materialized after terrain is
      ready. Old-area records remain discarded, and the default-off complete
      scene baseline remains optional rather than becoming a production
      dependency. The regression covers both scenery and walls with the
      baseline disabled, alongside the existing baseline replay case.
      Live validation used the worktree client against the hosted server and
      reproduced a real death from a distant area to the `120,648` Lumbridge
      respawn. Destination scenery, walls, collision, and interaction remained
      present when the death screen ended; no logout/relogin recovery was
      required.

## Notes

Do not turn this into a catch-all branch. Keep it as the written queue. Each
actual code change should still happen on a focused branch.
