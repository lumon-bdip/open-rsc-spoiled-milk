# Client Sprite Reference

Use this note when exporting or auditing item and equipment visuals.

## Item Icons

The live MyWorld client normally uses `Config.S_WANT_CUSTOM_SPRITES = true`.
In that mode, item definitions resolve `spriteLocation` directly through
`Client_Base/Cache/video/Custom_Sprites.osar`.

Example:

- `items:273` means subspace `items`, entry `273` in `Custom_Sprites.osar`.
- Dragon sword uses `items:273`.

When custom sprites are disabled, the authentic fallback path does not use the
raw picture id as the archive entry. It loads inventory/object sprites into the
client sprite array at `mudclient.spriteItem`, which is currently `2150`.

Example:

- Dragon sword `spriteID = 273`.
- Authentic fallback archive entry is `2150 + 273 = 2423`.
- Reading archive entry `273` directly is incorrect for an item icon.

This is an offset issue, not an off-by-one issue.

## Equipped Sprites

With custom sprites enabled, equipment visuals resolve by animation category
and name.

Example:

- Dragon sword equipped visual uses animation `equipment:sword`.
- Its dragon color is the animation/item mask `0xff0114`.
- Export from the raw `equipment:sword` frames, then apply the mask once.

Do not use an already palette-swapped item as the source for another
palette-swap export. That preserves the old non-grayscale pixels and produces
coppery or otherwise stale colors.
