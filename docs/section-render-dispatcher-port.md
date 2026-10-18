# Native additive terrain dispatch on Minecraft 26.3

The last working `ReikaRenderDispatcher` (`762c437d`) replaced Minecraft's block
renderer, tried a registered block-specific renderer, fell back to the first
accepting general renderer, emitted custom vertices and then delegated to vanilla.
Later ports reduced all registration/initialization methods to empty bodies.
The original block-renderer object no longer exists in 26.3. Replacing a Minecraft
field or reviving its old access transformer cannot restore that contract.

The current implementation uses the actual NeoForge 26.3.0.76
`AddSectionGeometryEvent`. Native section compilation adds those callbacks after
ordinary block/fluid tessellation and before layer mesh finalization and
translucent sorting. Vanilla geometry therefore remains present exactly once.
Initialization installs one client event listener and is idempotent. DragonAPI's
client setup initializes it before world rendering starts.

## Registration and selection

- `registerBlockRenderer` retains the original first-registration-wins rule.
- A direct renderer whose predicate declines falls back to the general list.
- General registrations retain insertion order and duplicate registrations.
- Selection chooses at most one renderer for each block/layer.
- `unregisterBlockRenderer(block, renderer)` removes only that exact owner's entry.
  `unregisterRenderer(renderer)` removes its first identical general registration.
- Registrations use copy-on-write immutable snapshots. A section uses one registry
  snapshot throughout capture; reentrant or concurrent registrations affect later
  captures rather than invalidating its iteration.

`IBlockRenderer.shouldRender` now takes the native `ChunkSectionLayer` instead of
an entity `RenderType`. Its render method receives a pose already translated from
the current block into section-local coordinates and a recording `VertexConsumer`.
Both renderer methods execute on client main. This is where resource lookup,
neighbor queries and position-dependent geometry belong.

The caller emits complete quads with the actual 26.3 `BLOCK` attributes: position,
ARGB color, atlas UV0 and packed light. The recording consumer preserves native
first-write attribute behavior and rejects incomplete vertices/quads during
capture. Overlay, normal, UV3 and line width are absent from native terrain's
format; their setters have the same non-effect as in a native `BLOCK` buffer.
Failures receive a crash report containing the block, location and layer.

## Thread and resource ownership

Capture uses the event's level, not the globally current client world. It requires
that level to provide a client block/tint view. It visits loaded sections only and
uses the section palette to skip sections with no applicable direct renderer when
there are no general renderers. Air/empty meshes add no callback or layer buffer.

Each section publishes immutable lists of plain vertex records grouped by native
layer. Its meshing callback captures only those lists. It neither accesses a live
world, renderer registry or resource manager nor retains an `IBlockRenderer`.
The native context owns and finalizes its buffers, sort state and GPU upload.
Geometry captured before a world edit stays internally consistent while the
normal dirty/rebuild mechanism replaces it.

## Accepted consumer: GeoStrata ocean spikes

GeoStrata registers `OceanSpikeRenderer` for the spike block. It emits only the
translucent terrain layer, with the original six faces, taper/root splay,
position-dependent gray ramp and original `geostrata:block/deco/0` sprite.
26.3 atlas lookup uses `AtlasIds.BLOCKS`; the old texture location
`minecraft:textures/atlas/blocks.png` is not an atlas identifier. The existing
geometry-free block model keeps the particle/material reference, and the existing
data/model resources provide the texture. Atlas UVs replace standalone-texture
UVs. Packed block/sky light use the brightest neighboring value per channel.

The separate ocean-spike BER is replaced by this implementation rather than
registered alongside it. Its useful geometry/lighting behavior is preserved in
the terrain renderer. The empty block entity/type remain registered for existing
saved worlds; they no longer draw another mesh. Collision and hover outline use
the same common `OceanSpikeGeometry` class, with no client renderer in the
server-side collision dependency path.

The previous table sampled DragonAPI's global RNG during class loading. That
allowed different JVM/load orders to produce different client and server shapes.
An independent fixed seed now builds the same 4x4x4 random table everywhere,
preserving the original radius/rotation ranges. Existing worlds keep their blocks
and empty block entities; generated silhouettes become stable across launches.

The height tint depends on spike blocks above the current segment. A native
client `setBlocksDirty` hook publishes `ClientBlockChangedEvent` after the state
change. GeoStrata invalidates the affected lower column's section range when a
spike changes, including changes across a section boundary. The original height
term is already below the sinusoidal floor after six segments. Both the upward
tint lookup and downward dependency invalidation therefore inspect at most six
blocks; a regression compares the result with the original formula through 4,096
segments. Ordinary adjacent block/lighting invalidation remains native.

## Verification

Final acceptance passed 306 family unit tests (244 DragonAPI, 8 GeoStrata,
34 RotaryCraft, 8 ReactorCraft, 12 ChromatiCraft), all 107 required dedicated
GameTests and the full isolated client suite. The independent native server and
client geometry fingerprints both equal `606f734740349413`.

The first client run exposed the old atlas-identifier use, which is corrected.
A corrected full run passed before the final bounded-tint and stronger fixture
checks. The final owned fixture posts actual registered section events for three
spikes across a section boundary, restores the world, and replays the detached
callbacks into native BLOCK buffers on an owned worker with a region proxy that
rejects world access. It checks every shared corner occurrence and the original
sprite rectangle, exact counts [24, 48, 24], the original lower tint change,
no duplicate BER, the actual native dirty-section flag beyond vanilla's one-block
neighbor reach, and worker termination before its pass marker.

These are native capture/meshing contract checks, not a GPU screenshot comparison.
Evidence: workspace `build/d01-proof.json` and `build/d01-corners-accept.log`.
Strict mixin reports contain 30 transformed / 18 other-side / 2 optional-absent
bindings on dedicated server, and 48 transformed / 2 optional-absent on client.
Normal Play launch argument hashes match the preceding checkpoints; default
isolated audit launch arguments were restored after strict validation.
