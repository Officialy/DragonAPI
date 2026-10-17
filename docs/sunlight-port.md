# Sunlight intensity on Minecraft 26.3

`ReikaWorldHelper.getSunIntensity` retains the original normalized sunlight curve,
including zero at night and the original independent rain and rain-weighted
thunder attenuation. The port had converted an already-radian angle through a
second full-cycle factor and added an ambient floor. That produced extra light
oscillations and prevented callers from receiving the original zero intensity.

The original contract is in DragonAPI commit `de1bcf1f`,
`Libraries/World/ReikaWorldHelper.java`. Its return value is `base`, without
`base * 0.8 + 0.2`. Those latter factors belong to consumers that explicitly
need an ambient floor, such as RotaryCraft's solar plant.

## Native angle and sampling

The authoritative 26.3 APIs were read from the generated
`minecraft-patched-26.3.0.76-sources.jar`:

- `EnvironmentAttributes.SUN_ANGLE` is an angle **in degrees**, supports positional
  modifiers, and is synchronized to clients.
- `Timelines.OVERWORLD_DAY` supplies the native eased day cycle. The helper uses
  that value rather than reconstructing a day from the overworld clock.
- `EnvironmentAttributeReader.getValue(attribute, BlockPos)` samples the block
  center and respects dimension, timeline and positional layers. Calling
  `getDimensionValue` for this positional attribute is invalid in development.
- `EnvironmentAttributeProbe.getValue(attribute, partialTicks)` supplies the
  camera's spatially and temporally interpolated value.
- `Level.getThunderLevel(partialTicks)` already includes rain weighting. The
  helper does not multiply thunder by rain a second time.

The block-position overload samples the current tick's angle and uses the
supplied partial tick for native weather interpolation. The public degree-angle
overload supports consumers that already have an interpolated angle. The original
position-free overload remains available and samples `BlockPos.ZERO`; callers
with a meaningful position should use the positional overload.

The angle is converted to radians once before evaluating the original curve.
Weather strengths use the original `1 - strength * 5 / 16` factors. No new
brightness floor or weather balancing is introduced.

## Consumers

- Exposed ambient temperature samples the actual temperature position. Other
  temperature contributions, winter adjustments and underground/altitude behavior
  retain their existing contracts.
- A solar plant retains an immutable copy of its build position for sampling.
  Its public no-argument constructor remains available with origin sampling.
  Its authored ambient floor, integer light rounding, moon-phase fallback and
  Nether/End exclusion remain in the consumer. Normalized zero sunlight makes
  the moon-phase fallback reachable again.
- Beam mirrors sample their own block position. Their authored clock-based
  night exclusion, range limits and obstacle checks remain in place.
- Luminous Cliffs fog samples `SUN_ANGLE` through the event camera's probe at the
  event partial tick, then evaluates sunlight without weather attenuation, as
  in the original callback. The existing fade, violet colors, fluid exclusions
  and original unclamped normalization of `[0.2, 0.8]` are preserved.
- Unknown Artefact shader brightness continues to use the separate native
  `SKY_LIGHT_FACTOR` camera attribute; its owner-selected effect is unchanged.

## Validation coverage

The two unit regressions compare the degree-angle curve against independent
double-precision cosine values over four whole cycles, check the normalized
range and exercise the authored rain/thunder strengths, including midnight zero.

Three registered native GameTests cover 96 samples of the actual overworld day
timeline, weather modulation, positional attribute overrides, the real exposed
climate caller, solar daylight/storm/moon-phase output and the actual beam-mirror
range path. They restore clocks, environment attributes and both interpolation
endpoints of weather in `finally` blocks.

The opt-in `SunlightClientChecks` fixture invokes the native Cliffs fog callback
with the real camera probe at three partial ticks and three render factors. It
checks all color channels against the original curve, preserves alpha, and
restores the render factor. It runs only in the isolated automated client.

Run evidence and final results are recorded in `PORT-AUDIT-REVIEW.md`,
`ReactorCraft/PORTING.md` and the root `PROJECT-BACKLOG.md`. Remote-client and
external shader/LOD acceptance remain separate backlog tasks.
