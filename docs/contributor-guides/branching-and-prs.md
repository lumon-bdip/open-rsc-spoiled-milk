# Branching And Pull Requests

## Main Rule

Do not work directly on `main`.

`main` should represent the official, owner-approved game state.

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

When multiple AI sessions or contributors need to work at once, use separate
Git worktrees instead of switching branches inside one folder. The standard
workspace setup is documented in [`../workspaces/README.md`](../workspaces/README.md).

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
