# AI Workspaces

Use separate Git worktrees when multiple AI sessions or contributors are
working at the same time. Do not run multiple unrelated tasks from the same
checkout.

Create a workspace with:

```bash
./scripts/create-ai-workspace.sh major-work
./scripts/create-ai-workspace.sh plan-work
./scripts/create-ai-workspace.sh small-tweaks
./scripts/create-ai-workspace.sh odds-and-ends
```

Check all worktrees and the live server with:

```bash
./scripts/workspace-status.sh
```

## Safety Rules

- The public hosted server runs from `/tmp/spoiled-milk-live-main`.
- Do not launch the public hosted server from feature, bugfix, cleanup, or AI
  workspaces.
- Use `./scripts/run-server.sh` for a private dev server.
- Use `./scripts/run-client.sh --dev` for a client pointed at the private dev
  server.
- Keep one task per branch. If a task changes direction, make a new branch
  instead of stacking unrelated work.
- Before merging or releasing, verify generated files and build scripts from a
  clean current checkout.

## Workspace Types

- `major-work`: large features, reworks, refactors, and other implementation
  branches expected to take multiple commits or testing passes.
- `plan-work`: documentation, planning, audits, and design notes.
- `small-tweaks`: focused bug fixes or tiny gameplay/content adjustments.
- `odds-and-ends`: assets, cleanup, data review, and other low-risk follow-up
  work that does not fit an active feature branch.
