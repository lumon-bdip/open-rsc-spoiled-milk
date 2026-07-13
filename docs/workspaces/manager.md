# Manager AI

The manager AI lives in `/home/justin/Core-Framework` on `main`. Its job is to
turn several independent worker sessions into one tested, published history.

## Daily Collection Loop

```bash
./scripts/ai-manager.sh status
```

For each slot, the manager should see whether it is IDLE, ACTIVE, READY, dirty,
ahead of published `main`, and remotely checkpointed.

For a READY branch:

```bash
git log --oneline main..fix/example
git diff main...fix/example
./scripts/ai-manager.sh merge fix/example
```

The merge command must verify the READY commit, remote backup, clean manager,
and clean worker. Inspect the full diff before invoking it; the command prints
a final summary and immediately creates a no-fast-forward integration merge.
Run focused tests, then the broader suite appropriate to the risk. Publish only
tested `main`:

```bash
git push spoiled-milk main
./scripts/ai-workspace.sh recycle ai-1
```

## Rescue Loop

If a worker is dirty, detached with files, or abandoned mid-task, preserve it
before interpreting it:

```bash
./scripts/ai-manager.sh rescue ai-2 -m "Rescue abandoned work"
```

Then inspect the pushed rescue branch like any other task. Do not silently mix
rescued files into an unrelated branch.

## Release Loop

```bash
./scripts/ai-manager.sh release-check
./scripts/test.sh
./scripts/build-server.sh
./scripts/ai-manager.sh release --version VERSION ...
```

Release inputs must come from clean, published `main`. Player packages record
the exact source commit. The `release` command forwards the remaining options
to `package-player-release.sh` after repeating the manager gate. Deploying the
hosted server is a separate controlled operation described in
[live-deployment.md](live-deployment.md).

Release preparation and publication never authorize stopping or restarting
the public server. Keep the existing live process uninterrupted until the user
gives fresh, explicit shutdown permission for the current maintenance window.
Only then may an administrator run the in-game `::update` warning and allow
its full countdown to initiate the graceful shutdown required for deployment.

Active worker branches do not block a release when their changes are cleanly
checkpointed and excluded from `main`. Dirty, unrescued, or ambiguous state
does block release management.
