# Skybox, Horizon, And Below-Terrain Visual R&D

Status: below-terrain depth floor accepted; sky comparison remains active.

## Reproduction Baseline

Use an OpenGL desktop client on the overworld, stand beside a ladder or stair
that descends through an intentionally open terrain tile, and tilt/orbit the
camera until the opening is viewed at a shallow angle. A convenient first
comparison point is the ladder at world coordinates `215, 468`; teleport to an
adjacent tile such as `215, 467`.

The visible sky in the opening is not caused by terrain backface culling:

- `OpenGLFramePresenter.clearFrameBackground` draws the procedural sky into
  the viewport before world geometry.
- `World.addTerrainTileGpuFaces` emits only the authored top faces. Transparent
  or absent terrain faces do not have underside or edge geometry.
- `OpenGLWorldChunkRenderer` enables face culling for selected resident object
  batches, not for terrain.
- Resident terrain correctly writes depth where it exists. Where it does not
  exist (or a transparent fragment is discarded), the earlier sky pass remains
  visible.
- The existing underground-frame check replaces the entire sky with the black
  clear color on underground planes/dungeon-coordinate bands. It cannot
  distinguish a downward opening while the player remains on an overworld
  frame.

The primary failure is therefore missing lower-hemisphere/underside coverage,
with draw order making the missing coverage visible. It is not presently
evidence of broken depth testing.

## Reversible Experiments

### A. Depth floor (first comparison)

Draw a nearly black world-space plane below the lowest point of the active
terrain chunk. It uses the same camera projection as resident terrain and tests
against the existing terrain depth buffer, so it appears only where no nearer
surface covered the pixel.

Purpose: cheaply prove whether a general below-world occluder gives downward
openings the intended sense of depth. It also exposes possible unwanted
coverage of authored transparent areas before a more detailed mesh is built.

Runtime switch:

```bash
SPOILED_MILK_OPENGL_BELOW_TERRAIN=depth-floor
```

The owner visually accepted this result around ladders and surrounding terrain.
It is now the OpenGL default, with `SPOILED_MILK_OPENGL_BELOW_TERRAIN=off`
retained as a diagnostic escape hatch. Classic/software rendering is unchanged.

### B. Hole edges and terrain skirts (likely refinement)

Classify visible terrain faces versus intentionally transparent/absent faces,
find topology edges bordering those holes, and extrude those edges downward to
a dark bottom. This gives terrain thickness and prevents the flat-floor read.
The chunk product may need explicit per-tile hole metadata so invisible but
pickable editor faces are not mistaken for solid visual coverage.

This is more context-aware than a global floor, but requires careful treatment
of water, outer chunk boundaries, streamed neighbor seams, and deliberately
invisible tiles.

### C. Object masks (diagnostic only)

Black quads attached to individual ladders/stairs would confirm specific art
needs quickly, but duplicate world knowledge in object definitions and fail for
new holes. Keep this as a fallback for an asset that intentionally needs a
unique portal shape, not as the general terrain solution.

### D. Stencil or screen-space lower occlusion

A terrain-hole stencil followed by a subterranean fill can precisely limit the
effect, but increases pass/state complexity. A screen-space lower-hemisphere
color is inexpensive but has the same camera-relative feel as the current sky.
Both remain alternatives if mesh-derived skirts prove unreliable.

## Sky And Horizon Direction

The current sky is a full-screen gradient and repeated cloud ellipses. Camera
pitch shifts their screen-space Y positions and camera yaw scrolls their X
positions. It preserves day/night colors cheaply, but does not establish a
physical environment around the player.

Promising longer-term comparison:

1. Draw a camera-centered dome or sphere with camera rotation but no camera
   translation. This makes the sky infinitely distant while preserving a
   stable world-facing direction as the camera orbits.
2. Use an upper sky gradient and a dark lower hemisphere. The lower hemisphere
   becomes a natural last-resort backdrop beneath missing terrain.
3. Add a fog-colored horizon ring/skirt around the resident draw distance so
   terrain fades into haze before meeting the dome.
4. Keep procedural day/night presentation values as the color source. Clouds
   can begin as dome-space bands and later become textured layers.

A cube is easier to author but risks visible corners with gradients. A sphere
or low-poly dome is the best first geometric comparison. The current
screen-space sky should remain available until visual comparison confirms that
the new horizon, camera tilt, fog seam, and night behavior are all better.

The first dome comparison is enabled with:

```bash
SPOILED_MILK_OPENGL_SKY=world-dome
```

It deliberately begins without clouds. This isolates whether camera rotation,
horizon stability, gradient scale, and fog stitching establish the intended
physical space before cloud geometry adds another moving reference.

## Visual Comparison Checklist

- Compare the ladder at `215, 468` from four yaw directions and minimum/maximum
  allowed camera pitch.
- Check another stair opening, coastline/water, deliberately transparent map
  tiles, steep elevation changes, and the edge of loaded terrain.
- Walk and rotate the camera: the dark coverage must remain fixed in world
  space, with no screen-space sliding.
- Visit plane 1 and a dungeon-coordinate region to ensure the existing black
  underground backdrop remains unchanged.
- Compare day, dawn/dusk, and night; then test fog on/off.
- Confirm the legacy/software renderer is pixel-identical to its baseline.

Do not promote an experiment to the default or hand it off until the owner has
visually selected and validated it.
