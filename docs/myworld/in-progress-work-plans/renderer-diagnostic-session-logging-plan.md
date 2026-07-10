# Renderer Diagnostic Session Logging Plan

Status: proposed; awaiting manager approval before implementation.

This plan is a focused milestone ledger within the ongoing renderer-v2
refinement workstream. It does not create a separate renderer project. The
goal is to turn live visual-test sessions into stable diagnostic bundles that
another AI can inspect without reconstructing timing, settings, warnings, and
`Ctrl+F9` frame captures from unrelated console output and directories.

The parent status ledger remains
[renderer-v2-plan.md](renderer-v2-plan.md). Findings from live testing and
completed milestones should be reflected in both documents where relevant.

## Current Baseline

- Expanded `F6` shows a broad renderer telemetry snapshot, including recent
  timings, scene phases, resident chunk state, geometry, sprites, 2D command
  pressure, shadow work, and allocation summaries.
- Opt-in renderer telemetry prints lifetime and short-window summaries to
  standard output every `300` frames by default and on throttled slow-frame
  reports. Those reports are human-oriented strings and are not retained by
  the renderer itself.
- Opt-in `Ctrl+F9` capture writes a `12`-frame burst. Each rendered frame gets a
  separate timestamped directory containing layered PNGs plus analyzer-ready
  TSV/text inputs for world faces, scene commands, static ownership, sprites,
  depth, shader parity, and renderer 2D command limits.
- The offline capture analyzer validates and summarizes one frame directory at
  a time. It does not correlate the burst with the performance window before
  and after the capture.
- `spoiled-milk-client.log` currently records uncaught failures and selected
  scene-sync/movement-cache diagnostics. It does not contain the renderer
  telemetry stream or a capture index.
- There is no session manifest joining client revision, renderer settings,
  telemetry windows, slow frames, warnings, capture requests, capture results,
  and visual-test observations.

## Target Diagnostic Bundle

An explicitly enabled diagnostic launch should create one ignored,
timestamped session directory under `output/renderer-diagnostics/`:

```text
session-YYYYMMDD-HHMMSS/
  manifest.json
  telemetry.jsonl
  events.jsonl
  console.log
  captures/
    capture-index.jsonl
    capture-.../
  ai-summary.md
```

The existing per-frame PNG, TSV, and text capture contract should remain
analyzer-compatible. The session layer indexes those artifacts instead of
embedding large tables or images in the telemetry log.

## Stable Data Contract

- Use a versioned schema identifier in the manifest and every JSONL record.
- Use one complete JSON object per line so partially written sessions retain
  every complete earlier record.
- Keep field names stable, explicit, and grouped by owner, for example:
  `frame`, `clientLoop`, `scene`, `presentation`, `openGL`, `world`, `chunks`,
  `sprites`, `renderer2D`, `shadows`, `runtime`, and `allocations`.
- Store raw numeric values with units in field names (`Nanos`, `Bytes`,
  `Count`) instead of formatted values such as `1.25ms`.
- Distinguish sample scope explicitly: current/latest, recent rolling window,
  report window, lifetime, maximum, and configured limit must not be collapsed
  into ambiguous values.
- Give every record wall-clock time, monotonic session time, renderer frame
  sequence, and session identifier. Capture records additionally receive a
  burst identifier, burst-frame index, and relative artifact path.
- Treat additive fields as backward compatible. Schema-version changes are
  required for renamed fields or changed meaning.
- Do not record credentials, account identifiers, chat contents, private
  messages, authentication material, or raw packet payloads.

## Milestone 1: Session Ownership And Launch Contract

- [ ] Add an opt-in diagnostic-session owner with one buffered writer per
      structured stream and deterministic close/flush behavior.
- [ ] Add a development launcher option that enables renderer telemetry,
      `Ctrl+F9`, the structured session directory, and a visible `console.log`
      tee without changing normal client launches.
- [ ] Write `manifest.json` with schema version, session identifier, start/end
      state, client commit/branch supplied by the launcher, target mode,
      Java/OS details, render dimensions, and active renderer settings.
- [ ] Keep output under ignored `output/renderer-diagnostics/` and make the
      chosen absolute session path obvious at launch.
- [ ] Flush at report and capture boundaries. Avoid per-frame filesystem I/O.
- [ ] Bound retention by configurable session count and/or byte budget. Never
      delete a session that is currently open.

Acceptance:

- A diagnostic launch creates exactly one identifiable session bundle.
- A normal launch creates no new diagnostic files and pays no ongoing logging
  cost beyond the existing disabled checks.
- Abrupt termination may omit final metadata but leaves prior JSONL records
  parseable.

## Milestone 2: Comprehensive Structured Telemetry

- [ ] Refactor renderer reporting so the console formatter and structured
      writer consume the same raw snapshot rather than maintaining competing
      metric lists.
- [ ] Include every metric currently exposed through expanded `F6` or periodic
      renderer telemetry, including frame/client-loop timings, scene phases,
      OpenGL phases, dropped frames, world/chunk geometry and churn, texture
      work, sprite ownership/replay, 2D command limits and drops, shadows,
      renderer reasons/states, and allocation estimates.
- [ ] Add relevant runtime context: heap used/committed/max, process uptime,
      available processors, and per-collector GC count/time deltas.
- [ ] Emit on the existing report cadence, on slow-frame reports, immediately
      before a `Ctrl+F9` burst, and immediately after the burst completes.
- [ ] Preserve the current console report and expanded `F6` behavior while the
      structured output is validated.
- [ ] Add contract tests proving representative F6/console fields exist in the
      JSONL schema with raw values and documented units.

Acceptance:

- An AI can determine whether a visual incident coincided with CPU/render
  timing, frame drops, GC, allocation, chunk churn, command overflow, texture
  pressure, or capture overhead without scraping console prose.
- Structured logging does not add a per-frame helper allocation or materially
  alter non-capture frame timing.

## Milestone 3: Events And `Ctrl+F9` Correlation

- [ ] Write structured events for session start/stop, slow frames, 2D overflow,
      renderer warnings, region/resident-chunk transitions, capture request,
      capture frame start/completion/failure, and uncaught client failures.
- [ ] Index all frames in a capture burst with their telemetry sequence,
      directory, layer list, failure state, and analyzer input inventory.
- [ ] Keep existing per-frame capture paths valid for the current analyzer;
      use relative paths from the session bundle for portability.
- [ ] Record capture readback/write timing separately so diagnostic overhead is
      visible and remains excluded from normal renderer phase totals.
- [ ] Make repeated warning/event records machine-deduplicable with stable
      event types and reason fields rather than message parsing.

Acceptance:

- A reported visual problem can be tied to exact captured frames and the
  telemetry immediately surrounding them.
- A failed or partial burst is explicit in the index and does not masquerade
  as a complete capture.

## Milestone 4: AI-Oriented Session Analyzer

- [ ] Add an offline analyzer that validates the manifest and JSONL streams,
      invokes or reuses the existing per-frame capture analysis, and tolerates
      older standalone frame captures.
- [ ] Generate `ai-summary.md` containing session identity, duration, renderer
      modes, worst timing windows, recent/lifetime drift, frame drops, GC/heap
      changes, chunk churn, command pressure, warnings, capture failures, and
      links to noteworthy capture frames.
- [ ] Rank likely correlations without presenting correlation as proven cause.
      Examples include a frame-time spike beside GC activity, section loading,
      chunk uploads, atlas pressure, or command overflow.
- [ ] Support a concise default report and a verbose mode that exposes raw
      record paths and validation details.
- [ ] Add fixtures for clean sessions, partial last lines, missing optional
      fields, schema incompatibility, failed capture frames, overflow, GC
      pressure, and older capture-only directories.

Acceptance:

- The manager can provide one session directory plus visual observations and
  receive a reproducible, evidence-linked refinement recommendation.
- The summary remains compact enough to inspect directly while preserving
  paths to raw evidence.

## Ongoing Visual Refinement Loop

For each manager-reported visual test:

1. Record the observation, location/action context, expected appearance, and
   approximate capture time or burst identifier.
2. Validate the session and relevant capture frames before proposing renderer
   changes.
3. Separate direct evidence, likely correlation, and speculation.
4. Implement the narrowest refinement that addresses the supported cause.
5. Add or update a guardrail, repeat the relevant capture route, and compare
   diagnostics before and after.
6. Update this plan and `renderer-v2-plan.md` with the finding, result, known
   risk, and completed milestone.
7. Create a pushed checkpoint after each meaningful completed piece and keep
   `feat/renderer-v2-refinement` ACTIVE. Do not create a final handoff unless
   the manager explicitly requests a final roundup.

## Validation

- [ ] Client compiles with Java 8.
- [ ] Existing renderer guardrail and capture-analyzer suites remain green.
- [ ] New schema/rotation/privacy/analyzer fixtures pass.
- [ ] Diagnostic-disabled launch produces no session bundle.
- [ ] Diagnostic-enabled launch produces a parseable bundle and visible
      console output.
- [ ] A real `Ctrl+F9` burst is indexed and analyzable from the session root.
- [ ] Logging overhead is measured with capture inactive and does not
      materially regress recent frame timing or allocation behavior.
