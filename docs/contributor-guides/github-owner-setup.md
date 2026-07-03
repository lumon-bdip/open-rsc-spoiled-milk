# GitHub Owner Setup

These settings help the owner keep control while allowing collaboration.

## Protect Main

In GitHub:

```text
Settings -> Branches -> Branch protection rules -> Add rule
```

Protect:

```text
main
```

Recommended settings:

- Require a pull request before merging.
- Require owner approval before merging.
- Dismiss stale approvals when new commits are pushed.
- Require conversation resolution before merging.
- Block force pushes.
- Block branch deletion.

Optional later:

- Require status checks once the test suite is stable enough for automation.
- Require signed commits if desired.

## Merge Settings

In:

```text
Settings -> General -> Pull Requests
```

Recommended:

- Allow squash merging.
- Disable merge commits if you want a cleaner history.
- Keep rebase merging disabled unless you are comfortable with it.
- Automatically delete head branches after merge.

## Collaborator Access

Start conservatively:

- New contributors: pull requests from forks or limited write access.
- Trusted contributors: write access after they understand the workflow.
- `main`: protected for everyone except controlled owner merges.

## Code Owners

Use `.github/CODEOWNERS` when you are ready to require review ownership.

A starter example is included at:

```text
.github/CODEOWNERS.example
```

Replace the placeholder owner with your GitHub username before enabling it.
