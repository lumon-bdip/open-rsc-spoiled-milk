# AI-Assisted Workflow

AI tools can help with Spoiled Milk, but contributors are responsible for what
they submit.

## Required Context For AI Tools

Before asking an AI tool to change the project, give it the relevant docs:

- `README.md`
- `CONTRIBUTING.md`
- `docs/myworld/README.md`
- the relevant plan doc
- the relevant contributor guide

For code changes, also ask the AI to inspect existing patterns before editing.

## Rules For AI-Assisted Work

- Use a branch. Do not let AI work directly on `main`.
- Keep the task focused.
- Tell the AI not to revert unrelated changes.
- Ask the AI to summarize what files it changed.
- Ask the AI to run relevant tests where possible.
- Review the diff yourself before opening a pull request.
- Mention AI assistance in the pull request.

## Good AI Prompt Shape

```text
We are working on Spoiled Milk. Read README.md, CONTRIBUTING.md,
docs/myworld/README.md, and this plan doc first: <path>.

Implement only the checklist item I name. Follow existing code patterns.
Do not revert unrelated work. After editing, summarize changed files and tests.
```

## What AI Should Not Decide Alone

AI should not decide:

- whether an idea becomes official
- whether balance is final
- whether art is accepted
- whether a system should be redesigned beyond the plan
- whether a pull request should be merged

Those are owner decisions.
