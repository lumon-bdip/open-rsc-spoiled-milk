# Spoiled Milk AI Collaboration Rules

This repository is designed for one manager AI and up to three worker AI
sessions. The directory identifies the role; the branch identifies the task.

Before changing anything, run:

```bash
git status --short --branch
./scripts/ai-workspace.sh status
```

## Roles

- `/home/justin/Core-Framework` is the manager checkout. It owns `main`,
  integrates finished work, runs final tests, publishes `main`, and builds
  releases. Do not use it for ordinary feature implementation.
- `/home/justin/Core-Framework-ai-1` through `-ai-3` are neutral worker slots.
  A worker may edit only after the manager starts a focused topic branch in
  that slot.
- `/tmp/spoiled-milk-live-main` is the detached live deployment. Never edit,
  commit, build experimental work, or switch branches there.

## Worker Rules

1. One task and one topic branch per slot. Never work on `main` or detached
   `HEAD`.
2. Use a descriptive branch such as `fix/prayer-display`,
   `feat/mining-expansion`, or `docs/renderer-plan`; never name a branch after
   the slot.
3. Run `./scripts/ai-workspace.sh checkpoint -m "message"` at useful
   milestones and before the session may end. A checkpoint commits tracked and
   untracked project files and pushes the same branch to `spoiled-milk`.
   Review any sensitive/large-file quarantine instead of bypassing it casually.
4. Run `./scripts/ai-workspace.sh handoff -m "message"` only when the exact
   pushed commit is ready for manager review.
5. Report changed files, tests, known risks, and whether the handoff is ready.

## Manager Rules

1. Keep the manager checkout on clean `main` except for deliberate integration
   or repository-management work.
2. Begin collection with `./scripts/ai-manager.sh status`.
3. If a session disappeared with uncommitted work, use
   `./scripts/ai-manager.sh rescue <slot> -m "message"` before doing anything
   else to that slot.
4. Inspect the branch diff, then merge only an exact READY handoff with
   `./scripts/ai-manager.sh merge <branch>`. Run the relevant tests before
   publishing.
5. Push tested `main`, then recycle a merged slot with
   `./scripts/ai-workspace.sh recycle <slot>`. Recycling must refuse any branch
   not contained in published `main`.
6. Run `./scripts/ai-manager.sh release-check` before live deployment and use
   `./scripts/ai-manager.sh release ...` for player packaging.
7. Back up and stop the public server before `deploy-live-main.sh`; deployment
   refuses to change tracked live files while the public port is occupied.

## Preservation Rules

- Never use `git stash`, `git clean`, `git reset --hard`, forced checkout,
  forced branch deletion, or forced worktree removal as routine workflow.
- Never delete a dirty slot. Rescue and push it first.
- Do not assume ignored files are disposable. Databases, credentials, logs,
  and release artifacts live outside Git and must be audited separately.
- Do not run two AI sessions in the same worktree.
- Do not launch the public server from the manager or a worker slot.
- If Git state is unexpected, stop editing and let the manager inventory it.

The full workflow is documented in
[`docs/workspaces/README.md`](docs/workspaces/README.md).
