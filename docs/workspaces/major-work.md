# Major Work Workspace

Use this workspace for one large implementation stream at a time.

Good fits:

- Feature branches such as Mage Arena work.
- Renderer or engine refactors.
- Large gameplay systems.
- Content passes that need several commits and visual checks.

Avoid by default:

- Tiny bug fixes that should stay isolated in `small-tweaks`.
- Public hosted-server launches.
- Release packaging unless this branch has already been merged into the official release path.
- Stacking unrelated feature work on the same branch.

Before starting, confirm the branch base is current enough for the task. Before
handing work back, summarize the branch name, changed areas, tests performed,
and whether the work is ready to merge.
