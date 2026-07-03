# Project Owner Workflow

This guide is for the project owner. It explains how to keep authority while
still making room for contributors.

## Mental Model

- `main` is the official game.
- Branches are temporary workspaces.
- Pull requests are review checkpoints.
- Plan documents are the project memory.
- Submissions are intake, not approval.

## Daily Solo Workflow

Start from `main`:

```bash
git switch main
git pull
```

Create a focused branch:

```bash
git switch -c fix/prayer-ui-points
```

Do the work, test it, commit it, then push the branch:

```bash
git status
git diff
git add .
git commit -m "Fix prayer point display"
git push -u origin fix/prayer-ui-points
```

Open a pull request into `main`. Even when working alone, this gives you a clean
place to review the change before it becomes official.

## Plan Statuses

Spoiled Milk keeps plans under `docs/myworld`:

- `proposed-work-plans/`: owner-approved ideas that may become work.
- `in-progress-work-plans/`: active plans that guide current implementation.
- `completed-work-plans/`: implemented plans and permanent records.
- `parked-work-plans/`: valid ideas that are not being worked on now.
- `rejected-work-plans/`: ideas intentionally not being pursued.
- `rough-drafts/`: early notes that are not yet formal proposals.

Contributor submissions are separate. They live under `submissions/` until the
owner decides what to keep.

## When To Use A Plan

Use a plan for:

- new systems
- balance passes
- new content areas
- asset pipelines
- renderer work
- database or protocol changes
- anything that may need multiple sessions of work

Small bug fixes can use a clear pull request description instead of a full plan.

## Bug Fixes And Small Updates

Use the ongoing maintenance queue for small fixes:

```text
docs/myworld/in-progress-work-plans/bug-fixes-and-small-updates.md
```

Do not use one permanent bug-fix branch. Keep the document permanent and create
short-lived branches for individual fixes:

```text
fix/prayer-tab-points
balance/dragon-smithing-levels
content/fishing-guild-shop-cleanup
```

This keeps `main` protected, keeps history readable, and still gives you a
single place to track quick work.

## Owner Review Checklist

Before merging:

- Does the change match the plan or pull request description?
- Is the scope focused?
- Were relevant tests run?
- Are player-facing changes documented?
- Are there unrelated file changes?
- Does this belong in the next release?

## Adding Contributors Later

Start contributors with pull requests only. Give them the contributor guide,
the AI-assisted guide if they use AI tools, and the relevant plan doc.

Do not give direct `main` access. Protect `main` first, then add collaborators.
