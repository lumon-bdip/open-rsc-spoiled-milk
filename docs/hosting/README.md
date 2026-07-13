# Hosting Docs

This folder holds current hosting documentation for Spoiled Milk.

- `private-server-setup-guide.md`: player-facing instructions for running a
  private local server from the repo.

Public hosted-alpha operation is handled by the guarded scripts in `scripts/`,
especially `scripts/run-hosted-server.sh`, `scripts/stop-hosted-server.sh`, and
`scripts/live-status.sh`. Use `scripts/deploy-live-main.sh` from the manager
checkout only after an explicitly authorized in-game `::update` warning has
completed and gracefully stopped the public server; release publication alone
is never shutdown permission. Deployment refuses to change tracked files
beneath a running JVM. Persistent live data belongs under
`~/.local/share/spoiled-milk/live`, outside Git worktrees.
