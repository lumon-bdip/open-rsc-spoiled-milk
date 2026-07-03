# Contributor Workflow

This guide is for anyone helping with Spoiled Milk.

## Before Starting

Read:

- [Branch and pull request workflow](branching-and-prs.md)
- [Plan document workflow](plan-doc-workflow.md)
- [AI-assisted workflow](ai-assisted-workflow.md), if using AI tools

Ask for the relevant plan doc before starting gameplay, balance, content, or
system work.

## Basic Flow

Create a branch from `main`:

```bash
git switch main
git pull
git switch -c fix/short-description
```

Make the change, test it, commit it, and open a pull request into `main`.

## What To Submit Where

- Bug fix: code/docs change on a `fix/...` branch.
- Feature or system work: code/docs change tied to a plan doc.
- Gameplay idea: markdown file under `submissions/ideas/inbox/`.
- Artwork: files under `submissions/artwork/inbox/`.
- Documentation-only update: `docs/...` branch and pull request.

## Pull Request Expectations

Every pull request should explain:

- what changed
- why it changed
- what files or systems were touched
- what was tested
- what still needs review

If you used AI tools, say so in the pull request and review the output before
submitting it.
