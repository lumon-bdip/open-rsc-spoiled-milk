# Plan Document Workflow

Spoiled Milk uses plan documents to keep work organized across human and
AI-assisted development.

## Where Plans Live

Plans live under `docs/myworld`:

- `proposed-work-plans/`: reviewed ideas that may become official work.
- `in-progress-work-plans/`: active plans.
- `completed-work-plans/`: implemented plans and permanent records.
- `parked-work-plans/`: good ideas that are not active.
- `rejected-work-plans/`: intentionally rejected plans.
- `templates/`: reusable plan templates.

The older `rough-drafts/` folder is still used for early notes that are not yet
ready to become formal proposals.

## Plan Lifecycle

```text
rough draft or submission
        ↓ owner decision
proposed-work-plans/
        ↓ owner starts work
in-progress-work-plans/
        ↓ implementation complete
completed-work-plans/
```

Not every idea moves forward. Some plans move to `parked-work-plans/` or
`rejected-work-plans/`.

## Branches And Plans

The plan is long-lived. The branch is temporary.

Example:

```text
Plan:
docs/myworld/in-progress-work-plans/prayer-devotion-equipment-plan.md

Branch:
feat/prayer-relic-rewards
```

Large plans can have several branches. Each branch should implement a focused
part of the plan.

## Owner Rule

Contributors can submit ideas. The owner decides when an idea becomes a formal
plan and when a formal plan becomes active work.
