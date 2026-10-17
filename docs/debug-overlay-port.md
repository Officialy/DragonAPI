# Debug overlay on Minecraft 26.3

D09 restores the original display contract from DragonAPI `de1bcf1f`,
`Auxiliary/DebugOverlay.java`, through the native 26.3 GUI extraction API.

## Native layer and drawing context

The physical-client subscriber registers `dragonapi:debug` immediately above
`VanillaGuiLayers.CAMERA_OVERLAYS`, corresponding to the original post-HELMET
stage. The layer receives `GuiGraphicsExtractor` directly. The unused old overlay
registration comment and the disconnected 3D `PoseStack` are removed.

The debug-mode banner retains its original text, white color, no-shadow drawing,
`(1, 1)` coordinates and three-times scale. Its text is now a generated translation.
The scale is applied to the extractor's actual 2D pose and isolated by push/pop,
so the deferred text state contains the correct transform and later HUD elements
retain their original transform.

The overlay requires an active client world/player and a visible HUD. F1 is
checked explicitly because custom NeoForge GUI layers do not inherit the
visibility predicate of the vanilla layer beside them.

## Block-entity inspection

`TABNBT` and the configured debug key retain their existing config identities.
Key codes are native SDL scancodes on 26.3. The keyboard query bounds-checks the
configured integer against SDL's actual keyboard-state buffer, so negative or
out-of-range values cannot crash GUI extraction. Renaming/migrating the historical
LWJGL config label remains part of D62; valid current values are preserved here.

While enabled and held, the original four-block, non-fluid selection ray is used.
The view remains suppressed while a screen is open and emits no stale information
on a miss or a hit without a block entity. Native `saveWithFullMetadata` restores
the type ID and coordinates that original `writeToNBT` included. Current
components, inventory fields, NeoForge persistent data and attachments stay in
the registry-aware save output.

NBT lines retain the original unscaled no-shadow text and 24-row columns, with
each next column offset by half the logical GUI width. Save exceptions remain
readable on screen in red, followed by at most four stack frames. Empty/short
traces are handled safely. Errors are not logged on every rendered frame.

## Related element-buffer correction

The accepted Chroma element-buffer overlay incorrectly assumed that a registered
custom layer inherited vanilla HUD visibility. It now checks `Hud.isHidden()`
before reading the buffer or extracting its wheel, plates, runes and capacity
label. Original holding-item, corner, radius, rune and energy behavior is retained.

## Acceptance

The isolated native client fixture verifies the real layer callback, actual text
transforms/glyph preparation, pose restoration and SDL key-state query/bounds.
It uses temporary client-side cells and a real chest selection ray to check ID,
coordinates, inventory and persistent data, second-column placement, released
input, open-screen suppression, misses, reach, non-BE hits and failing/stackless
serializers. The inspection branch receives a semantic held-key snapshot in the
fixture; SDL-owned keyboard memory is never modified to simulate a key.

It also extracts the actual manipulator element-buffer HUD while visible and
hidden, including the picture-in-picture wheel. Temporary cells, player position,
rotation, hand item, buffer data, screen, HUD visibility and debug flag are
restored before other audit checks continue. A live native HUD frame is observed
with the debug banner enabled. These fixtures run only in Automated Checks.

Final evidence is recorded in the module ledgers and root backlog. Broader visual
acceptance remains tracked under V09 and the existing remote/shader tasks.
