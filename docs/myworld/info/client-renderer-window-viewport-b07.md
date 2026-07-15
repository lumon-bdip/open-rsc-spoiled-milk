# B07 Client Renderer Window and Viewport Extraction

This branch extracts GLFW window lifecycle and viewport presentation from
`OpenGLFramePresenter` without changing the renderer's world, sprite, UI, or
capture pass order.

## Ownership Boundaries

`OpenGLWindowController` now owns:

- GLFW initialization, the native window handle, context creation, event
  polling, buffer swaps, window destruction, and GLFW termination;
- windowed and primary-monitor borderless transitions;
- restored, resized, and shutdown-time windowed-bounds persistence;
- framebuffer and logical-window size queries; and
- bounded diagnostics when bounds persistence, native-window destruction, or
  GLFW termination fails.

It calls a narrow delegate for input release/suppression, settings persistence,
renderer logging, context-created device logging, and the maintained desktop
client-close compatibility hook.

`OpenGLViewportPresenter` now owns:

- aspect-fit, integer-fit, and debug-stretch viewport calculation;
- the separate logical-window and HiDPI framebuffer viewports;
- the primary-window automatic aspect-fit policy;
- fractional-scale text-smoothing selection; and
- mouse coordinate remapping and bar-edge clamping.

`OpenGLFramePresenter` retains frame queueing, OpenGL resource ownership,
input-bridge construction, rendering/capture orchestration, projection setup,
and all world/sprite/UI composite decisions. It contains no direct GLFW call.

## Preserved Behavior and Deliberate Limits

- Frame pass order remains base, world, world sprites, sprite/UI overlay,
  debug overlay, final capture, then swap.
- Window mode changes still release held input and suppress keys until release.
- Primary mode still forces aspect fit; the mirror/debug mode still honors the
  configured aspect, integer, or stretch policy.
- Windowed bounds are rejected as restore candidates when they are effectively
  fullscreen, and borderless mode still selects GLFW's primary monitor.
- No sprite classification, composite ordering, shader, atlas, world geometry,
  roof, animation, day/night, or server behavior changes. The only fallback
  correction is an ownership gate: software rendering remains authoritative
  until OpenGL initialization has actually succeeded, and resumes ownership
  if OpenGL is disabled or cleaned up.
- Input forwarding remains in the presenter for this branch. It can be moved
  only with its own callback and focus-state characterization.

Cleanup remains best effort. Unlike the previous silent catches, failures now
identify the failed cleanup phase and exception type/message in the local
renderer log. Native window destruction and GLFW termination are attempted
even if an earlier OpenGL resource cleanup fails. Repeated native shutdown is
safe when the handle and GLFW state have already been cleared.

## Automated Verification

`tests/myworld/test-opengl-window-viewport-extraction.py` compiles and executes
the extracted production classes against focused fixtures. It covers:

- 4:3 and 16:9 aspect fit, integer fit, debug stretch, and HiDPI layout;
- primary-window aspect enforcement and fractional/integer smoothing;
- mouse mapping through pillarbox bars and coordinate clamping;
- saved window restoration, resize capture, windowed/borderless round trips,
  primary-monitor selection, event polling, and buffer swaps;
- programmatic versus native-window close behavior;
- repeated cleanup; and
- injected window-destroy and GLFW-terminate failures, including diagnostic
  evidence and continued shutdown; and
- configured-but-unavailable, successfully initialized, and cleaned-up OpenGL
  replacement ownership, including legacy raster, projected capture, and
  resident object decisions.

The renderer guardrail suite also verifies input modifiers, graphics options,
frame/capture ordering, UI replay, world geometry, roof visibility, relog
cleanup, materials, shading, and sprite transparency. The client build and the
changed-code compiler/static-analysis gates must pass on the final commit.

## Required Private Visual Verification

The branch must not be handed off until the owner confirms this matrix on the
private development server and branch-built client:

1. Login graphic and mouse/keyboard input are correctly positioned.
2. Switch between 4:3 and 16:9; bars remain centered and the image is not
   stretched, clipped, or offset.
3. Switch windowed to borderless and back; the saved windowed size/position is
   restored. Resize the window and repeat the round trip.
4. If multiple monitors are available, confirm borderless uses the configured
   primary monitor and returning to windowed restores the saved bounds.
5. Confirm input at viewport center and near each bar/edge maps to the expected
   in-game location.
6. Inspect representative roofs, sprites behind walls, ground/inventory items,
   an animation, and a visible day/night transition or lighting state.
7. Close with the native window control, relaunch, reconnect, and confirm no
   cleanup exception or stuck input/window state.
8. Launch once with the OpenGL presenter disabled and confirm the software
   fallback still reaches login and renders the game.

Visual verification status: **owner-confirmed for OpenGL and software
fallback**. The owner exercised 4:3/16:9 presentation,
windowed/borderless transitions, resizing, input, representative in-game
rendering, and the modern Graphics rows. The corrected OpenGL run rendered
active resident chunks and ended through `windowCloseRequested=true` after
`Alt+F4`, with no cleanup failure or unexpected exception.

The first software-fallback run exposed that renderer replacement flags were
enabled from configuration even though the OpenGL presenter was disabled,
leaving software output without terrain and resident-owned world content. The
fix gates every replacement optimization on actual presenter readiness. In the
corrected run, the owner confirmed terrain, objects, sprites, the player,
movement, UI, and interactions. Diagnostics recorded active legacy scene draw
time and zero OpenGL frames. The legacy scaler rows shown in this mode are the
documented software-presenter controls; saved OpenGL-primary settings were not
reset.

Borderless mode has no native title-bar close button; this pre-existing UX gap
is recorded in the renderer plan as a separate in-client close-affordance
follow-up rather than expanding this lifecycle extraction.

The public server and detached live checkout are outside this branch and must
remain untouched.
