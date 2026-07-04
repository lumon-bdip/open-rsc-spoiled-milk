# MyWorld Prune Candidates

This repo still contains inherited OpenRSC/Cabbage files while the standalone extraction is in progress. Do not delete these blindly; use this list to review what should be archived, removed, or kept as inherited engine attribution.

## Server Configs

Archived under `legacy/docs/inherited-openrsc/server-configs/`:

- `legacy/docs/inherited-openrsc/server-configs/2001scape.conf`
- `legacy/docs/inherited-openrsc/server-configs/default.conf`
- `legacy/docs/inherited-openrsc/server-configs/openpk.conf`
- `legacy/docs/inherited-openrsc/server-configs/preservation.conf`
- `legacy/docs/inherited-openrsc/server-configs/rsccabbage.conf`
- `legacy/docs/inherited-openrsc/server-configs/rsccoleslaw.conf`
- `legacy/docs/inherited-openrsc/server-configs/uranium.conf`

Keep:

- `server/myworld.conf`
- `server/connections.conf`

## Root Legacy Docs

Archived under `legacy/docs/inherited-openrsc/`:

- `legacy/docs/inherited-openrsc/Linux Getting Started Guide.md`
- `legacy/docs/inherited-openrsc/MacOS Getting Started Guide.md`
- `legacy/docs/inherited-openrsc/Windows Getting Started Guide.md`
- `legacy/docs/inherited-openrsc/Raspberry Pi Getting Started Guide .md`
- `legacy/docs/inherited-openrsc/Commands.md`
- `legacy/docs/inherited-openrsc/CONTRIBUTING.md`
- `legacy/docs/inherited-openrsc/SECURITY.md`

Keep or rewrite:

- `README.md`

## Client Targets

Review before removing:

- `Android_Client/`
- `PC_Launcher/`

The PC client currently still uses `Client_Base/` and `PC_Client/`, so those are not prune candidates.

## Deployment Scripts

Archived under `legacy/docs/inherited-openrsc/`:

- `legacy/docs/inherited-openrsc/legacy-launchers/Deployment_Scripts/`
- `legacy/docs/inherited-openrsc/legacy-launchers/Start-Linux.sh`
- `legacy/docs/inherited-openrsc/legacy-launchers/Start-Windows.cmd`
- `legacy/docs/inherited-openrsc/legacy-server-launchers/ant_launcher.sh`
- `legacy/docs/inherited-openrsc/legacy-server-launchers/run_server.sh`

Keep active:

- `scripts/`
- root `Makefile`
- `docs/myworld/active-workspace.md`

## Database Seeds

Keep:

- `server/inc/sqlite/myworld_seed.db`
- `server/inc/sqlite/myworld_dev.db` as local dev state, not a canonical seed

Archived under `legacy/docs/inherited-openrsc/sqlite-seeds/`:

- `legacy/docs/inherited-openrsc/sqlite-seeds/2001scape.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/cabbage.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/coleslaw.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/openpk.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/openrsc.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/preservation.db`
- `legacy/docs/inherited-openrsc/sqlite-seeds/uranium.db`

## Next Safe Action

Move prune candidates into an archive folder or remove them in a dedicated cleanup commit after confirming no scripts, docs, or tests reference them.
