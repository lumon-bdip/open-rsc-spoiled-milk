# Branching And Pull Requests

## Main Rule

Do not implement ordinary changes directly on `main`.

`main` should represent the official, owner-approved game state.
The dedicated manager checkout may own `main` solely to review, integrate,
test, publish, and release completed topic branches.

## Branch Names

Use one branch per focused topic:

- `fix/prayer-ui-points`
- `feat/tier-11-magic-staffs`
- `content/soul-altar-room`
- `balance/dragon-gear-gates`
- `art/herblaw-ingredient-icons`
- `idea/new-mining-boss`
- `docs/private-server-guide`
- `chore/github-templates`

## Good Branch Scope

A branch should usually answer one question:

- Did this fix the bug?
- Did this implement this plan step?
- Did this add this submission?
- Did this update this guide?

Avoid mixing unrelated gameplay, artwork, formatting, and cleanup changes.

Maintainer-owned concurrent AI sessions use the neutral `ai-1`, `ai-2`, and
`ai-3` worktrees instead of category-named permanent branches. External
contributors do not enter those worktrees. They use their own clone and a
username-namespaced branch such as `goutan/fix/collision-definitions`; see the
[external contributor workflow](../workspaces/external-contributor.md).

## Small Fixes

Small bugs and minor updates can be tracked in:

```text
docs/myworld/in-progress-work-plans/bug-fixes-and-small-updates.md
```

That file is the ongoing queue. The branch should still be short-lived and
focused:

```text
fix/specific-bug-name
```

Avoid a permanent `bug-fixes` branch because it becomes hard to review and easy
to mix unrelated changes.

## Pull Request Rules

Pull requests should target `main`.

Use the pull request template and include:

- summary
- related plan or submission
- testing
- screenshots, if visual
- known risks or follow-up work

## Merge Rule

A merged pull request means the owner accepted the work into the official
project state.

Use squash merge for most work. It keeps the project history readable while
still preserving the pull request discussion.

Manager-collected AI handoffs are the local exception: `ai-manager.sh merge`
creates an explicit no-fast-forward merge after verifying the exact READY
commit and its remote backup. This preserves which parallel session supplied
the handoff. External pull requests still use squash merge by default.
