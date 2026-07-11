# Intermittent Movement Stutter Investigation

Status: investigation complete; no runtime fix implemented

Date: 2026-07-11

Branch: `docs/movement-stutter-investigation`

Live revision measured: `6f693f929`

Source revision reviewed: `64207c9f1`

## Executive Conclusion

The strongest evidence points to **multiple contributing timing issues**, led
by a client/server movement-cadence boundary that has almost no jitter margin,
with real connection-specific network jitter and retransmission making that
boundary visible as intermittent tile-center pauses. A renderer hitch can
produce a similar jump because movement advances from frame elapsed time, but
the sampled client was not CPU-saturated and prior no-capture renderer sessions
had stable frame pacing. Rare server GameThread stalls are a demonstrated
second shared-stutter mechanism, but no such stall was logged during the
current server run.

The current `430ms` walking cadence is encoded to the client as the rounded
integer `6` pixels per classic `20ms` frame. That is nominally one 128-pixel
tile in about `426.7ms`. At 60 FPS, packet-to-frame phase and the synthetic
first interpolation sample leave the client with only a few milliseconds of
margin and can let it reach a tile before the next waypoint. The server polls
movement from a `10ms` scheduler on the same single GameThread as the `640ms`
world tick, so an update normally arrives on a quantized cadence and can be
delayed by world-tick work. Any network arrival jitter then becomes visible
rather than being absorbed.

No production process, configuration, file, or deployment was changed. The
live server and client were not restarted. All live measurements were
read-only and short-lived.

This finding agrees with the pre-existing diagnosis in the
[movement and pathing release plan](../in-progress-work-plans/movement-pathing-release-plan.md),
but adds live host, JVM, socket, log, and code-path evidence and defines the
instrumentation needed to prove individual stutters end to end.

## Architecture Trace

### Server movement and tick scheduling

- `Server` has one scheduled GameThread, invoked every `10ms` with
  `scheduleAtFixedRate`.
- The authoritative world tick runs every `640ms`. Incoming packets, events,
  player/NPC processing, per-player update construction, and outgoing packet
  submission are serialized on that GameThread.
- MyWorld enables `want_custom_walking_speed` and `walking_tick: 430`.
  Between world ticks the same GameThread scans every player and live NPC.
  Each `Mob` advances only when its wall-clock `lastMovementTime + 430ms` is
  due. The due time is reset to the time at which the poll actually ran, so
  scheduler/world-tick delay becomes cadence drift rather than being recovered
  against a monotonic deadline.
- When anything moves, the server constructs movement records per viewer and
  immediately calls `processOutgoingPackets()`. Movement writes are submitted
  and flushed on that iteration; they do not wait for the next `640ms` tick.
- Both the established custom movement packet and the staged movement snapshot
  packet are currently sent. The client deduplicates identical coordinates in
  `appendCustomMovementWaypoint`, so the paired packets do not add duplicate
  waypoints. They do add a small amount of serialization and wire traffic.

### Client movement and rendering

- The live OpenGL client uses the modern loop at a `60 FPS` target with up to
  five catch-up updates after a delayed frame. VSync is disabled in the live
  launch flags.
- Server configuration converts `430ms` to
  `round(4 * 640 / 430) = 6` pixels per classic frame. The client advances by
  `(elapsedMillis / 20) * 6`, carrying a subpixel remainder and clamping a
  single elapsed sample at `250ms`.
- A newly idle character resets its interpolation clock. On the first frame
  after a waypoint arrives, the client assumes one `1/60s` sample regardless
  of the packet's phase within the render interval. With only a roughly
  `3.3ms` nominal difference between tile traversal and server walking cadence,
  render phase alone can consume the margin; scheduler and network jitter make
  a visible idle frame more likely.
- Movement snapshots are applied as soon as the network handler parses them.
  There is no arrival-jitter buffer, target presentation timestamp, or bounded
  interpolation delay.

### Network submission

- TCP_NODELAY is enabled on both client and server.
- Server packet construction and `channel.write(...)` calls occur on the
  GameThread, but Netty performs socket I/O asynchronously. Packets are flushed
  once per non-empty player queue.
- If a channel is not writable, the server skips that player's outgoing
  processing and retains the queue. A persistently slow reader therefore does
  not normally block the GameThread on socket I/O, although a later backlog
  drain can add serialization/submission work on the shared thread.

## 1. Evidence Gathered And Measurement Method

### Live host and server JVM

Measurements were taken from the production host with `ps`, `vmstat`,
`pidstat`, `/proc`, and `jstat`. Sampling did not enable profilers, trigger a
heap dump, force GC, or attach an intrusive capture.

- The host has 24 logical CPUs. During the sample it remained about `95-96%`
  idle with zero observed I/O wait and no active swap-in/swap-out. Load average
  was approximately `1.9 / 2.2 / 1.8`. This rules out host-wide CPU starvation
  during the window.
- The server used about `972 MiB` RSS. A 15-second process sample averaged
  approximately `36%` of one CPU, ranging from `15%` to `72%` in one-second
  buckets.
- Per-thread sampling placed nearly all non-GC server CPU on the single
  `Spoiled Milk : GameThread`. Its one-second utilization was bursty
  (`16-64%` in the sampled interval), but it was not continuously saturated.
- Fifteen one-second G1 samples recorded 24 young collections totaling about
  `42ms`, or roughly `1.75ms` per collection. No full collection occurred and
  old-generation occupancy was stable in the short window.

Interpretation: current host pressure and server GC are poor explanations for
the observed intermittent movement. Single-thread bursts and rare outliers
remain possible because average CPU does not expose a delayed movement poll.

### Server timing logs

The current log and all available compressed live logs were searched for the
server's exact late-tick and skipped-tick warnings.

- The current server process started on 2026-07-10 at approximately 22:41.
  There were no `Tick ... is late` or `behind. Skipping` warnings from that
  start through the investigation window on 2026-07-11.
- Historical logs contain 14 ticks longer than `640ms`: 11 on July 5 and 3 on
  July 6. Recorded durations ranged from `653ms` to `825ms`.
- The warning's existing sub-counters reported incoming/outgoing packet work at
  `0ms` and events at only `1-33ms` for those cases. Most of the late duration
  was therefore outside the four values printed by the warning, and cannot be
  assigned to world update, NPC/player processing, cleanup, JVM safepoint, or
  another stage from retained logs.
- Coarse tick numbers embedded in normal plugin logs continued at the expected
  long-run rate during the live sample, but one-second log timestamps are not
  precise enough to detect a one-frame movement hitch.

Interpretation: long GameThread ticks are real and would delay movement for
everyone, but the retained evidence does not correlate them with the current
report. The lack of current warnings does not rule out sub-640ms delays, which
are large enough to stutter movement but below the only persistent threshold.

### Live socket state

Two short `ss -tinp` snapshots were taken for the public game port. Public IPs
are intentionally omitted from this repository document.

- Five established sessions were present: three through one remote WAN
  endpoint, one through another remote WAN endpoint, and one local/gateway
  session.
- The three same-endpoint WAN sessions showed observed RTTs roughly
  `44-75ms`, RTT variation roughly `9-25ms`, retransmission timeouts around
  `245-266ms`, and accumulated retransmitted-byte ratios of approximately
  `1.4-2.3%`.
- The other WAN session showed roughly `59-68ms` RTT, about `9-12ms` RTT
  variation, and only one accumulated retransmission in the snapshot.
- Receive queues were zero. Send queues were normally zero and briefly only
  `58-292` bytes, so there was no evidence of persistent server-side socket
  backpressure.
- Retransmission counters are lifetime connection counters, not a direct
  stutter correlation. Small counter increases were observed between samples,
  but no player-side stutter marker existed to compare against them.

Interpretation: network quality differs materially by connection. With a
movement stream that has almost no presentation slack, tens of milliseconds of
jitter can create client-specific pauses. A lost TCP segment can also
head-of-line block later movement until retransmission, on the order of the
observed RTO. TCP batching/Nagle delay is unlikely because TCP_NODELAY is set
and movement queues are flushed promptly.

### Running client JVM and prior renderer evidence

The already-running local OpenGL live client was sampled with `pidstat` and
`jstat`; diagnostics and frame capture were not activated.

- A 15-second sample averaged about `49.6%` of one CPU, with one-second values
  from `34%` to `99%`. The host still had abundant idle CPU.
- Work was split mainly between the OpenGL/presenter thread and the client-loop
  thread rather than one continuously saturated thread.
- Fourteen young collections added about `46ms` total collection time, roughly
  `3.3ms` each, with no full collection.
- G1 old-region occupancy read near `100%` in most short samples and briefly
  fell near `85%`; concurrent G1 workers consumed a small share of CPU. This is
  a retention/longer-session watch item, but it did not produce a full GC or a
  measured pause matching the movement report. Prior no-capture retention work
  found a stable old-generation plateau across relog, so the percentage alone
  is not evidence of a new leak or stutter cause.
- The live client was not launched with renderer session diagnostics, so this
  sample has no frame-time series and cannot be correlated to a reported
  stutter.
- Prior accepted no-capture renderer sessions in the existing diagnostics plan
  measured client-loop p95 near `17.2ms`, OpenGL render p95 near `8.7ms`, GC at
  about `0.61%` of session time, and no slow-frame events. Those sessions are a
  useful baseline, not proof about today's exact client/session.

Interpretation: neither current CPU utilization nor GC demonstrates a recent
renderer CPU regression. Transient frame hitches remain plausible because a
late frame advances movement by a larger elapsed-time step and the modern loop
then runs catch-up updates, but direct frame/stutter correlation is missing.

### Existing correctness diagnostics and tests

- The movement snapshot contains server tick and sequence fields and the F6
  diagnostics compare its wire content, staged cache, and visible entity cache.
  It records recent samples only when a parity mismatch occurs.
- The client applies authoritative waypoints immediately and rejects invalid
  region/direction records. Identical paired movement/snapshot positions are
  deduplicated.
- Existing guardrails cover frame-time conversion, subpixel remainder, idle
  reset, server/client movement-speed mirroring, region handoff, and movement
  snapshot/cache parity structure.

Interpretation: no evidence currently points to corrupt coordinates or stale
waypoints as the normal stutter mechanism. Existing diagnostics validate state
agreement, but not arrival cadence, time spent idle at a waypoint, correction
frequency, or renderer/network correlation.

## 2. Likely Causes Ranked By Confidence

1. **High confidence: movement-cadence/interpolation boundary amplified by
   connection jitter.** The 430ms authoritative step, rounded 6-pixel client
   speed, 10ms server polling, shared GameThread, reset interpolation phase, and
   absence of a jitter buffer provide a direct mechanism for intermittent
   tile-center pauses. Live WAN RTT variance and retransmissions supply a
   measured amplifier. This is the leading explanation for stutters affecting
   some clients more than others.
2. **Medium confidence: occasional shared GameThread delays.** Fourteen
   historical 653-825ms ticks prove that the server can create a simultaneous
   pause. Normal game-tick work also blocks the custom movement poll even when
   it stays below 640ms. No current late-tick event was captured, and existing
   logs cannot expose sub-threshold movement-poll lateness.
3. **Medium-low confidence: transient client frame-pacing hitches as a second
   presentation path.** Movement is driven by elapsed frame time and can jump
   after a slow frame. Current CPU/GC samples and prior renderer sessions do not
   show sustained pressure, but the affected session lacked active frame
   telemetry and a user marker.
4. **Low confidence: movement state synchronization/correction defect.** The
   recent region and snapshot fixes, coordinate deduplication, and parity
   guardrails make a normal state mismatch less likely. Timing diagnostics are
   insufficient, so this is not fully ruled out.
5. **Low confidence in the current window: server GC, total host CPU pressure,
   disk pressure, or generic packet batching.** Measurements directly argue
   against each of these as the primary cause during the sample.

The overall classification is **multiple contributing issues**, not one
confirmed renderer or server regression.

## 3. Causes Ruled Out Or Considered Unlikely

- **Host CPU exhaustion:** unlikely in the sampled period; 24-CPU host remained
  overwhelmingly idle.
- **Sustained server CPU saturation:** unlikely; the GameThread was bursty but
  far below one full core on average. Rare latency outliers still matter.
- **Full-GC pauses:** no full GC occurred in either sampled JVM. Short young-GC
  totals were too small to explain recurring large pauses during the sample.
- **Disk or swap thrashing:** no I/O wait or active swap traffic was observed.
- **Nagle/application flush delay:** unlikely. TCP_NODELAY is enabled at both
  ends, and non-empty movement queues are flushed in the same custom movement
  iteration.
- **A paired snapshot creating two visual steps:** unlikely. The client ignores
  an identical coordinate when appending the second packet's waypoint.
- **A distant player's RTT directly blocking all clients:** unlikely under the
  normal path because Netty socket I/O is asynchronous and current socket
  queues were small.

None of these conclusions means the component can never cause a hitch; they
describe the evidence in the measured window.

## 4. Can Slow Or Distant Players Affect Everyone?

**Geographic or network distance alone should not affect everyone.** A remote
player's high RTT is handled by Netty I/O threads and does not synchronously
wait on the GameThread. In-game distance also tends to reduce shared visibility
records because movement packets include only locally visible entities.

**The presence and activity of any player can still affect everyone
indirectly.** All player `processTick` and `sendUpdates` calls are serialized on
one GameThread. The custom movement path scans every player and every live NPC
from a 10ms scheduler, and constructs/sends per-viewer records when movement
occurs. Expensive plugin actions, dense local visibility, login/logout work, or
many active entities can extend the shared thread's work regardless of where
the initiating player is in the world.

A truly slow reader can also become an indirect contributor: while its Netty
channel is non-writable, its application queue is retained, and later draining
that queue performs submission work on the GameThread. There was no persistent
send backlog in the live socket snapshots, so this is a theoretical path rather
than the current leading explanation.

## Missing Instrumentation

The repository has good state-parity counters but lacks one common timeline.
The following cannot currently be answered after a player reports a stutter:

- exact custom movement poll start interval, duration, and lateness;
- actual per-mob authoritative movement interval distribution;
- per-stage server duration for a sub-640ms hitch, including update clients,
  NPC/player processing, cleanup, packet construction, and queue drain;
- movement packet arrival interval and sequence gap at each client;
- time an entity spends idle at a waypoint awaiting its next update;
- client correction/snap count and waypoint queue depth over time;
- frame-time/GC/network-arrival values sharing the same timestamp and hitch ID;
- a low-overhead player hotkey that marks “I saw the stutter now.”

## 5. Targeted Instrumentation And Experiment Plan

### Bounded production-safe instrumentation

1. Add allocation-light rolling histograms for world-tick duration, scheduler
   lateness, custom movement-poll duration/interval, moved entity counts,
   per-stage duration, per-player update maximum, outgoing queue depth, and
   channel-writability skips. Emit one summary every 60 seconds and one compact
   record only when a conservative threshold is exceeded.
2. Extend renderer diagnostic sessions with monotonic movement packet arrival
   intervals, server tick/sequence, waypoint depth, interpolation idle time,
   correction/snap counters, and the closest frame-loop timing sample. Do not
   enable full frame capture for this temporal issue; prior capture work proves
   it is intentionally intrusive.
3. Add a manual diagnostic marker available to ordinary test clients. A marker
   should record local monotonic time, recent movement arrivals, recent frame
   windows, local coordinates, and renderer mode without account/chat/private
   network data.
4. Give client and server summaries comparable monotonic deltas plus wall-clock
   timestamps. Synchronize only approximately; exact clock synchronization is
   unnecessary if packet server tick/sequence is retained.

### Controlled private experiments

1. Run a fixed long straight walk locally with the same `430ms` configuration.
   Measure packet interval, tile completion, and idle-at-waypoint time. Repeat
   enough times to observe render-phase variation.
2. Repeat under `tc netem` on a private client path with controlled latency,
   jitter, and small packet loss. Compare zero-jitter, realistic WAN jitter,
   and one forced loss. This isolates the leading cadence-plus-network theory.
3. Repeat the same route with OpenGL/legacy rendering and VSync on/off while
   renderer diagnostics are enabled but capture is disabled. This separates
   packet arrival from frame pacing.
4. Run a private server load matrix with players distributed across the world
   versus co-located in a dense region. Include a deliberately slow-reader test
   client and compare GameThread poll/tick histograms and queue depth.
5. Use short private JFR/GC logging only if server histogram markers correlate
   with a shared hitch. Do not profile production merely to search broadly.

### Correlation decision rules

- Multiple clients mark the same server tick and server poll/tick lateness is
  high: shared server cause.
- One client marks a hitch with a movement-arrival gap/retransmission-like
  interval while server cadence is normal: client network/jitter cause.
- Arrival cadence is normal but the client records a slow render window and a
  large interpolation step: renderer/frame-pacing cause.
- Arrival and frames are normal but waypoint idle/correction spikes: movement
  interpolation or synchronization defect.

## 6. Phased Fix Plan

No phase below is approved by this investigation alone; instrumentation should
select the behavioral change.

### Phase 1: preserve evidence with no gameplay change

- Implement the bounded server/client timelines and marker above.
- Add deterministic tests for histogram bounds, disabled overhead, privacy,
  and movement arrival/idle accounting.
- Capture at least one marked private reproduction and, if practical, one
  low-overhead marked live occurrence before changing cadence.

### Phase 2: remove the known interpolation margin defect

- Send the authoritative walking interval to custom clients instead of only a
  rounded pixels-per-classic-frame integer.
- Interpolate a tile against that duration with a small, bounded presentation
  margin or adaptive jitter allowance. Avoid an unconditional full-waypoint
  delay; it would make input visibly laggy.
- Test 50/60 FPS, VSync modes, variable frame time, diagonal movement, combat
  approach, teleports, region changes, and legacy-client compatibility.

### Phase 3: make server movement cadence explicit and cheaper

- Replace per-mob wall-clock reset drift with monotonic next deadlines and a
  measured due-work scheduler.
- Stop scanning every live NPC at 100Hz when no movement can be due. Preserve
  immediate interaction/path behavior through explicit wakeups or a bounded
  due queue.
- Keep movement authority single-threaded initially. Validate cadence and
  behavior before considering parallel update construction.

### Phase 4: address evidenced shared server hotspots

- Use the new stage histogram to optimize only the stage correlated with
  marked shared stutters.
- If queue drain is implicated, bound per-player drain work and expose backlog
  age/size. If NPC/player/plugin work is implicated, isolate that exact path.
- Add private load and long-soak regressions before production rollout.

### Phase 5: renderer changes only if correlated

- If marked events show normal network/server cadence but slow frames, compare
  the same route against the established renderer diagnostic baseline and
  optimize the measured render/update phase.
- Do not change GC policy, VSync defaults, frame catch-up, or renderer features
  solely from process CPU percentages.

## Investigation Verification

- Confirmed workspace and live checkout state before investigation.
- Reviewed server scheduler, movement, visibility/update, packet queue, Netty,
  client socket, movement snapshot, interpolation, and modern render-loop code.
- Compared relevant movement/sync and renderer history.
- Sampled live host/server/client CPU, memory, threads, JVM GC, sockets, config,
  and logs using read-only commands.
- Confirmed no live server restart, modification, deployment, diagnostic burst,
  forced GC, heap dump, or packet capture was performed.
