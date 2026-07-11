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

- Add completed small fixes here when the record is useful.

## Notes

Do not turn this into a catch-all branch. Keep it as the written queue. Each
actual code change should still happen on a focused branch.
