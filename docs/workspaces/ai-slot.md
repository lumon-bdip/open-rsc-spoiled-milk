# Worker AI Slot

A worker slot is a reusable folder named `ai-1`, `ai-2`, or `ai-3`. It starts
detached and becomes active only when the manager assigns a topic branch.

## Start Of Session

Confirm the slot and branch before editing:

```bash
git status --short --branch
./scripts/ai-workspace.sh status
```

Detached `HEAD` alone does not prove the slot is IDLE; a forgotten detached
commit may contain unique work. Require `ai-workspace.sh status` to report
phase `IDLE`, then wait for the manager to run:

```bash
./scripts/ai-workspace.sh start ai-1 TYPE/short-task-name
```

Use `fix/`, `feat/`, `content/`, `balance/`, `art/`, `idea/`, `docs/`,
`refactor/`, `chore/`, or `test/` according to the task. Never reuse
`workspace/ai-1` as a permanent branch.

## Save Progress

Checkpoint after a coherent milestone and before yielding control:

```bash
./scripts/ai-workspace.sh checkpoint -m "Describe saved progress"
```

This is the normal safety net. It commits and pushes the task branch, so a
closed terminal, forgotten session, or damaged worktree does not lose the
work.

Checkpoint prints every staged path and size. Likely credential files and
near-host-limit blobs are quarantined locally instead of being pushed. Review
the staged files first; use `--allow-sensitive` or `--allow-large` only for a
deliberate, legitimate repository asset.

When implementation and relevant tests are complete:

```bash
./scripts/ai-workspace.sh handoff -m "Describe ready result"
```

Tell the manager what changed, what was tested, what was not tested, and any
remaining risks. Do not continue editing after handoff without creating a new
checkpoint and handoff.

## Never Do This In A Worker

- switch to or update `main`
- merge another worker
- create a release
- launch the public hosted server
- stash work
- force-reset, force-clean, force-delete, or remove the worktree

If the branch or slot state looks wrong, stop and ask the manager to rescue it.
