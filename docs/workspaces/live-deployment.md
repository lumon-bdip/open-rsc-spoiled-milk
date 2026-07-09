# Live Deployment

The live server uses `/tmp/spoiled-milk-live-main`, but that worktree is
detached at an exact published `spoiled-milk/main` commit. It does not own the
`main` branch and never receives development edits.

Persistent state lives outside Git:

```text
~/.local/share/spoiled-milk/live/spoiled_milk_alpha.db
~/.local/share/spoiled-milk/live/backups/
```

The live worktree's ignored database path is a symlink into that data root.
Removing or recreating a worktree therefore cannot remove player data.

## Status

```bash
./scripts/live-status.sh
```

A running server may safely remain on the previous published commit while the
manager advances `main`. Status should distinguish that expected state from a
wrong worktree, configuration, database, port, or unexplained dirty checkout.

## Controlled Deployment

From clean, published manager `main`:

```bash
./scripts/ai-manager.sh release-check
```

Make an online SQLite backup under the external `backups/` directory while the
current server is still running. Activation then uses a short, explicit
maintenance window:

```bash
./scripts/stop-hosted-server.sh --yes
./scripts/deploy-live-main.sh
cd /tmp/spoiled-milk-live-main
./scripts/run-hosted-server.sh
```

If `live-status.sh` reports a wrong or deleted runtime database descriptor,
do not stop the JVM yet. Copy that open descriptor to the backup directory and
integrity-check it first. Only after confirming recovery may the stop command
be acknowledged with `--yes --database-recovered`; the guard otherwise
refuses a shutdown that could discard an unlinked SQLite inode.

Deployment refuses to change the live worktree while anything is listening on
the public port. It validates the external-database link before and after
switching the detached checkout, so an old JVM can never run over a mixture of
old code and newly deployed definitions.

Run `./scripts/live-status.sh` afterward and confirm both checkout and runtime
report the same current published-main commit, a `verified-live` launch
attestation, and the external database file descriptor.

Ordinary worker activity, manager merges, and pushes never alter the deployed
tree. That separation is what allows the hosted server to remain uninterrupted
while several AI sessions work. Only deliberate activation needs the brief
stop/deploy/start window.
