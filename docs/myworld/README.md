# MyWorld Docs

This folder is the navigation surface for Spoiled Milk planning, implemented
work records, rough drafts, and stable references. The public project overview
and player-facing feature summary live in the repository root
[`README.md`](../../README.md).

## Folder Rules

- [`proposed-work-plans/`](proposed-work-plans/): owner-reviewed plans that
  may become official work, but are not active yet.
- [`in-progress-work-plans/`](in-progress-work-plans/): active plans, current
  roadmaps, and work that still drives implementation decisions.
- [`completed-work-plans/`](completed-work-plans/): implemented plans and
  closed migration records that are still useful as source-of-truth history.
- [`parked-work-plans/`](parked-work-plans/): valid plans that are not being
  pursued right now.
- [`rejected-work-plans/`](rejected-work-plans/): plans intentionally not
  pursued, kept when the reasoning is useful.
- [`rough-drafts/`](rough-drafts/): deferred ideas, early designs, and notes
  that should not be treated as committed implementation direction yet.
- [`info/`](info/): stable references, command lists, audits, IDs, and
  implementation facts that are not themselves active work plans.
- [`screenshots/`](screenshots/): documentation and README image assets.
- [`templates/`](templates/): reusable plan templates.

Keep this README as the top-level index. New markdown files should normally go
into one of the category folders, not directly into `docs/myworld/`.

## Start Here

- [Contributor guides](../contributor-guides/README.md): owner and contributor
  workflow, branch rules, pull requests, submissions, and AI-assisted work.
- [AI workspaces](../workspaces/README.md): separate worktree setup for
  multiple AI/contributor sessions.
- [Hosting docs](../hosting/README.md): current private-server hosting notes.
- [Renderer and shader roadmap](in-progress-work-plans/renderer-and-shader-roadmap.md):
  current AI-facing source of truth for renderer-v2, remaster lighting, shadows,
  shader work, and major optimization direction.
- [Roadmap](in-progress-work-plans/roadmap.md): high-level alpha milestone and
  version progression.
- [Work items](in-progress-work-plans/work-items.md): active work list and
  implemented-state summary.
- [Change history](info/change-history.md): consolidated record of major
  completed project changes.
- [Testing quick reference](info/testing-quick-reference.md): compact field
  testing commands, IDs, and targeted fixture notes.

## In Progress Work Plans

- [bug-fixes-and-small-updates.md](in-progress-work-plans/bug-fixes-and-small-updates.md):
  ongoing maintenance queue for focused small fixes and quick updates.
- [chat-and-dialogue-channel-plan.md](in-progress-work-plans/chat-and-dialogue-channel-plan.md):
  chat tab/channel cleanup and dialogue privacy/concurrency path.
- [code-cleanup-and-modularization-plan.md](in-progress-work-plans/code-cleanup-and-modularization-plan.md):
  codebase cleanup roadmap for oversized files, stale options, and renderer
  ownership boundaries.
- [god-relic-reward-plan.md](in-progress-work-plans/god-relic-reward-plan.md):
  prayer/devotion god relic rewards, relic pools, and Mage Arena separation.
- [herblaw-side-ingredient-expansion-plan.md](in-progress-work-plans/herblaw-side-ingredient-expansion-plan.md):
  tiered Herblaw secondary ingredients for fish oils, creature eyes, and
  harvested Brawn ingredients.
- [legacy-limits-audit.md](in-progress-work-plans/legacy-limits-audit.md):
  byte/short-era cap audit and modernization recommendations.
- [movement-pathing-release-plan.md](in-progress-work-plans/movement-pathing-release-plan.md):
  movement smoothness, NPC roaming, and pathfinding follow-up plan.
- [ogg-audio-support-plan.md](in-progress-work-plans/ogg-audio-support-plan.md):
  staged OGG Vorbis support plan with WAV fallback safety.
- [prayer-devotion-equipment-plan.md](in-progress-work-plans/prayer-devotion-equipment-plan.md):
  prayer-devotion equipment direction, blessing rules, and god-tool retirement.
- [remaster-lighting-and-shadow-plan.md](in-progress-work-plans/remaster-lighting-and-shadow-plan.md):
  detailed implementation ledger for clean-slate Remaster lighting and shadows.
- [renderer-and-shader-roadmap.md](in-progress-work-plans/renderer-and-shader-roadmap.md):
  combined current renderer/shader status and next big-swing roadmap.
- [renderer-v2-plan.md](in-progress-work-plans/renderer-v2-plan.md):
  detailed renderer-v2 implementation ledger and historical checklist.
- [resource-seed-plan.md](in-progress-work-plans/resource-seed-plan.md):
  rare resource seed framework and remaining seed-system direction.
- [roadmap.md](in-progress-work-plans/roadmap.md):
  alpha milestone roadmap and release progression.
- [summoning-plan.md](in-progress-work-plans/summoning-plan.md):
  current Summoning implementation source of truth and tuning notes.
- [terrain-expansion-plan.md](in-progress-work-plans/terrain-expansion-plan.md):
  staged terrain expansion process and map-editor workflow.
- [tier-11-magic-gear-plan.md](in-progress-work-plans/tier-11-magic-gear-plan.md):
  tier-11 god staff and combination staff direction.
- [work-items.md](in-progress-work-plans/work-items.md):
  current implementation queue and completed-state rollup.

## Completed Work Plans

- [altar-enchantment-and-conversion-plan.md](completed-work-plans/altar-enchantment-and-conversion-plan.md):
  implemented rune-altar enchanting, item-tier gates, and god knight conversion.
- [aoe-scythe-weapon-plan.md](completed-work-plans/aoe-scythe-weapon-plan.md):
  implemented melee scythe line and NPC-only adjacent cleave behavior.
- [entrana-safety-deposit-box-plan.md](completed-work-plans/entrana-safety-deposit-box-plan.md):
  implemented Entrana restriction helper and Safety Deposit Box flow.
- [fishing-rework-plan.md](completed-work-plans/fishing-rework-plan.md):
  implemented rod-tier and location-pool Fishing rework record.
- [geode-and-gathering-spirit-plan.md](completed-work-plans/geode-and-gathering-spirit-plan.md):
  implemented geode and key-half tree work with deferred spirit notes.
- [migration-regression-audit.md](completed-work-plans/migration-regression-audit.md):
  completed migration recovery record and guardrail notes.
- [pvm-population-and-cluster-plan.md](completed-work-plans/pvm-population-and-cluster-plan.md):
  implemented pre-release PvM population passes and remaining expansion notes.
- [archive/](completed-work-plans/archive/):
  older historical plans, completed tasklists, migration notes, and audits.

## Rough Drafts

- [bank-tag-filter-plan.md](rough-drafts/bank-tag-filter-plan.md):
  deferred bank tag/filter quality-of-life idea.
- [enchanted-robe-effects-plan.md](rough-drafts/enchanted-robe-effects-plan.md):
  exploratory altar-bound robe effect notes and tier scaling ideas.
- [herblaw-potion-rework-plan.md](rough-drafts/herblaw-potion-rework-plan.md):
  square-one Herblaw and potion redesign draft.

## Info

- [change-history.md](info/change-history.md): consolidated major-change record.
- [client-sprite-reference.md](info/client-sprite-reference.md): custom sprite
  reference, authentic item icon offsets, and equipment palette export notes.
- [combat-equipment-spec.md](info/combat-equipment-spec.md): combat stat,
  armor budget, poison, and debuff stacking rules.
- [compatibility-only-content.md](info/compatibility-only-content.md): dormant
  item families, quest exceptions, and retired systems kept for compatibility.
- [dev-admin-commands.md](info/dev-admin-commands.md): development account roles
  and practical command reference.
- [dual-element-spells.md](info/dual-element-spells.md): current dual-element
  spell implementation and effect identities.
- [enemy-ids.md](info/enemy-ids.md): builder-facing attackable NPC ID reference.
- [fishing-spot-map.md](info/fishing-spot-map.md): live fishing spot coordinate
  source list.
- [god-knight-equipment-audit.md](info/god-knight-equipment-audit.md): god
  knight equipment source and tuning record.
- [jewelry-and-retired-robe-effects.md](info/jewelry-and-retired-robe-effects.md):
  current jewelry effects and robe effects retired from cloth armor.
- [new-ideas-and-issues.md](info/new-ideas-and-issues.md): intake list for
  ideas and regressions before they become scoped work.
- [object-ids.md](info/object-ids.md): builder-facing scenery/object ID reference.
- [pvm-npc-cluster-audit.md](info/pvm-npc-cluster-audit.md): hostile-NPC
  proximity audit and Wilderness population view.
- [testing-quick-reference.md](info/testing-quick-reference.md): compact test
  and field-validation reference.

## Current Repository Shape

Active MyWorld workflow:

- `Makefile`: root MyWorld task aliases.
- `scripts/`: current build, check, run, reset, test, and benchmark wrappers.
- `tools/generators/`: authored generated-data sources and generator code.
- `tools/benchmarks/`: current benchmark helpers.
- `tools/vendor/apache-ant-1.10.5/`: repo-bundled Ant launcher used by the
  current script wrappers.
- `tests/myworld/`: current MyWorld validation suite.
- `docs/hosting/`: private-server hosting documentation.
- `docs/workspaces/`: AI/contributor worktree setup and workspace scopes.
- `docs/releases/`: player packaging, hosted-alpha, and publication checklists.
- `server/myworld.conf`: active local server config.
- `server/connections.conf`: required shared connection config.
- `server/inc/sqlite/myworld_seed.db`: canonical local SQLite seed.
- `server/inc/sqlite/myworld_dev.db`: local dev database state.
- `server/plugins/com/openrsc/server/plugins/custom/myworld/`: preferred
  namespace for MyWorld-owned runtime content.

Compatibility areas:

- `dev/myworld/`: compatibility wrappers for older local commands. Its
  `assets/` subtree remains the active visual asset source loaded by the
  client. New tools, tests, and planning docs should not be added there.
- Broad inherited server/client packages can still be changed when needed, but
  new MyWorld-specific handlers should prefer `custom/myworld`.

Archived reference:

- `legacy/`: single root archive for unmaintained inherited clients, old
  Windows portable tooling, old CI/database helpers, IDE metadata, and stray
  root artifacts.
- `docs/myworld/completed-work-plans/archive/`: old MyWorld planning docs and
  detailed audits.
- `legacy/docs/inherited-openrsc/`: inherited OpenRSC/Cabbage docs, launchers,
  configs, SQLite seeds, and Make recipes.

## Validation

Use these before and after structural cleanup:

```bash
make check
python3 ./tests/myworld/test-standalone-layout.py
make build-server
```

Run the full suite when local ports are clear:

```bash
make test
```
