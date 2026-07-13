# World Editor Icon Contract

World-editor icons are replaceable client assets. Each icon must:

- be a transparent RGBA PNG on an exact 24x24 pixel canvas;
- use the stable lowercase kebab-case filename listed in the editor plan;
- remain legible at native size against light and dark terrain;
- be drawn at native size with nearest-neighbor behavior; and
- have redistribution rights recorded in `CREDITS.md` before release.

The client loads and decodes each icon once, caches the resulting sprite, and
uses button backgrounds, borders, tint, or badges to communicate interaction
state. Missing, malformed, incorrectly sized, or non-alpha PNGs activate a
labeled code-drawn fallback without preventing editor use.

Do not add final artwork until its source and redistribution terms are known.

Author files live in this directory. The client build packages them at
`myworld-assets/ui/world-editor/` so a player archive never depends on the
source checkout.
