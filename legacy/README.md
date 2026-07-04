# Legacy Archive

This folder is the single root archive for material that is not part of the
current Spoiled Milk build, release, or live-server workflow.

Files here are kept for reference, not deleted. Current scripts should not
depend on paths under `legacy/`. If something in this folder becomes active
again, move it back into an explicit current location and update the relevant
docs and scripts in the same change.

## Contents

- `clients/`: inherited or unmaintained game client variants.
- `windows-portable/`: old portable Windows helper tools and launchers.
- `database/`: inherited MariaDB/Docker setup files that are not used by the
  current SQLite hosted-alpha workflow.
- `ci/`: old CI configuration that is not used by the current GitHub release
  workflow.
- `ide/`: tracked IDE metadata from the inherited project.
- `backups/`: old placeholder backup folders from inherited tooling.
- `root-artifacts/`: duplicate or stray files that used to sit in the repo
  root.

## Active Replacements

- PC client source: `Client_Base/` and `PC_Client/`.
- Server source and content: `server/`.
- Current launch/build/release scripts: `scripts/`.
- Current MyWorld docs: `docs/myworld/`.
- Current packaged player launcher templates: `release/player/`.
- Current third-party build tool bundle: `tools/vendor/apache-ant-1.10.5/`.
