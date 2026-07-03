# Idea Submissions

Ideas should be submitted as markdown files, not direct code changes.

## Where Ideas Go

New ideas start here:

```text
submissions/ideas/inbox/
```

Use the template:

```text
submissions/ideas/templates/idea-proposal-template.md
```

## Contributor Flow

Create a branch:

```bash
git switch main
git pull
git switch -c idea/new-idea-name
```

Add a markdown file:

```text
submissions/ideas/inbox/new-idea-name.md
```

Open a pull request into `main`.

## Owner Decisions

The owner may move the idea to:

- `submissions/ideas/accepted/`
- `submissions/ideas/parked/`
- `submissions/ideas/rejected/`

Accepted means the idea is worth keeping. It does not mean the idea is approved
for development.

If the owner wants to build it, the idea can later become a formal plan under:

```text
docs/myworld/proposed-work-plans/
```
