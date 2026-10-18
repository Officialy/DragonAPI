# Boss colors on Minecraft 26.3

`BossColorEvent.fire(boolean lightmap)` posts one mutable event and returns an
immutable `BossColorEvent.Color` containing that dispatch's final RGB values.
Listeners retain the original `red`, `green`, `blue`, `originalRed`,
`originalGreen`, `originalBlue`, and `isLightmap` contract. The original defaults
are `(0.7F, 0.6F, 0.6F)`. Values are not clamped by the event; the renderer's
existing color math still applies. Nested and concurrent calls cannot overwrite
another invocation's result, and later mutation of a retained event cannot alter
an already returned result. Listener exceptions propagate normally.

The split `fireAndReturnRed`, `fireAndReturnRed_Light`, `returnGreen`, and
`returnBlue` methods belonged to the removed 1.7.10 ASM implementation. There
were no accepted source callers. Consumers should call `fire` once and read all
three components of its returned result. No global output cache remains.

The original behavior is recorded in DragonAPI commit `de1bcf1f`, files
`Instantiable/Event/Client/BossColorEvent.java` and
`ASM/Patchers/Hooks/Event/Render/BossColorEvent.java`. Its two rendering hooks
replace the RGB multipliers inside boss darkening, rather than tinting the final
screen. The modern client mixins preserve those stages:

- `MixinBossFogColor` shares one invocation-local result between the three
  multipliers in `FogRenderer.computeFogColor`, before native night vision and
  the NeoForge fog event. It posts only inside the positive boss-darkening branch.
- `MixinBossLightmap` posts once when a dirty `Lightmap` update has positive boss
  darkening. Unchanged colors use the original native pipeline. Changed colors
  use `BossLightmapRenderer` with the original native lightmap math and an extra
  RGB uniform at the boss multiplier stage. The renderer belongs to that
  `Lightmap` instance, is allocated lazily, and closes with its owner. It never
  owns or closes Minecraft's destination texture.
- `MixinBossColorReset` delivers the original `BossColorResetEvent` after each
  `GameRenderer.tick` with a world present. It remains a listener notification;
  there is no shared output to reset.

The custom shader is under DragonAPI's namespace. It is derived from
`assets/minecraft/shaders/core/lightmap.fsh` in the generated Minecraft
26.3 / NeoForge 26.3.0.76 source jar. Run
`python DragonAPI/tools/verify_boss_lightmap_shader.py` from the workspace root
to check the pinned native SHA-256 and verify that only the RGB uniform and boss
multiplier differ. Supply a source jar path as the optional first argument.
Future native shader changes require reviewing the math and both UBO layouts.

`BossColorEventTest` covers listener defaults, immutable snapshots, nested and
concurrent dispatch, unrestricted RGB values, cleanup and reset notifications.
The opt-in `BossColorClientChecks` fixture exercises the transformed native fog
method, native reset ticks, and actual lightmap GPU readback. It compares every
texel of the custom default shader to the native pipeline, including a state
with sky/block lighting, night vision, darkness and gamma; it also checks changed
RGB, partial boss darkening, conditional dispatch and no-update preservation.
Its listeners and GPU resources are confined to Automated Checks. Normal Play
does not run the fixture.

These checks do not establish compatibility with every external shader mod or
replace the separate renderer-stack acceptance backlog.
