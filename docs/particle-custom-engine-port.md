# D03 custom particle engine port

D03 is in progress. The native backend and original default effects compile and
have passed native ownership, callback, GPU and regular-frame acceptance in both
classic and improved transparency. Controlled screen/water pixel composition still
needs verification.
N09 has since repaired the wider gameplay test fixtures/contracts: the final
rotarycraft:* composite profiles pass 998 family-only and 1,002 external-mod tests.
See TestInstance/docs/gametest-fixtures-and-mock-players.md for current evidence.

## Native ownership and initialization

ReikaParticleEngine is a facade with a lazy native ParticleEngine backend. Loading
its mode API or constructing the default facade does not construct an engine.
ClientRendering registers the default blur and floating-seed classes after the
real global engine exists. The backend shares that engine's loaded ParticleResources
and participates in its exact-class delegation, ticking, extraction and level
lifecycle. It never constructs an unprepared resource provider or replaces the
playing engine. Registration is idempotent.

Admission preserves native cancellation and ParticleLimit checks. A rejected
particle does not acquire a custom ownership ticket. Accepted particles freeze the
native group, concrete texture identifier, mode bit mask and exemption status.
Subsequent changes to a caller's builder cannot mutate an admitted batch key.
Duplicate admission of a currently owned particle identity is rejected.

One ownership budget covers pending and active particles for each custom key.
Exempt particles retain FIFO order without consuming capacity or evicting ordinary
particles. The existing BoundedParticleQueue constructor still counts every entry,
so ordinary D02 engines retain their previous admission policy. While native groups
are ticking, overflow marks a victim dead and defers physical removal until the
iterator is safe. Native death cleanup or the post-tick sweep releases its native
reservation exactly once; the sweep does not invoke remove callbacks twice.

World changes clear native ownership, tickets and layer instances. Immutable
pipeline declarations are reused. Statistics include pending and active particles;
mode keys unused after ticking are pruned.

## Native rendering

The original six flags are restored: ALPHA, LIGHT, ADDITIVE, DEPTH, ALPHACLIP, FOG.
All particles retain their supplied native positions, orientation, UV, color and
packed light coordinates. Custom texture identifiers are resolved by the native
TextureManager through SingleQuadParticle.Layer, including standalone textures.
Scoped per-particle extraction restores the previous mapping in a finally block;
the existing Insert-key depth override applies after the custom layer mapping.

- ALPHA controls the pipeline blend state. ALPHA=false disables blending.
- ADDITIVE retains the original ADDITIVEDARK screen law: source ONE, destination
  ONE_MINUS_SRC_COLOR. Its alpha equation uses the equivalent alpha-channel
  factor ONE_MINUS_SRC_ALPHA. Ordinary alpha retains SRC_ALPHA/ONE_MINUS_SRC_ALPHA.
- DEPTH selects native reverse-Z testing or ALWAYS_PASS. Custom particles never
  write scene depth.
- ALPHACLIP restores GL_GREATER with a threshold of 1/255, rather than the native
  particle shader's 0.1 threshold.
- FOG independently controls native world fog.
- LIGHT retains directional/entity lighting and the supplied lightmap coordinates.
  Unlit particles bypass both. Legacy batches emitted no per-vertex normal; the
  custom shader uses the default +Z normal. Native bindDefaultUniforms supplies
  the Lighting buffer, alongside the native particle feature's texture/lightmap
  bindings.

Ordinary alpha modes supply native OIT depth-bounds, transmittance and accumulation
pipelines. Screen modes deliberately use native forward drawing: scalar OIT
transmittance cannot express their per-channel destination factors exactly.
This is an explicit routing choice, not an alpha-based approximation of screen
blending. Relative composition around translucent water and an actual improved-
transparency scene remain part of rendering review; pipeline compilation alone
must not be described as proof of those visual results.

## Default effects and controller input

EntityBlurFX and EntityFloatingSeedsFX are restored from the original bodies.
Blur retains acceleration, drag/no-slowdown, life freezing, size/color/alpha curves,
rapid expansion, bounds, collision response, destinations, locks and controllers.
Native collision shapes replace obsolete Entity collision APIs; position changes
also update the native bounding box. The floating seed retains its angular targets,
random freedom, tolerances and polar velocity calculation.

ControllerTarget provides position, true velocity and age to common controller
code without requiring client particles to inherit Entity. Entity caller wrappers
and state-only null calls remain available. The shared ParticleController update
contract lets one object implement motion and position without duplicate updates.
Accepted Chroma path adapters have been migrated. Corrected partial-port mistakes:
target/spiral motion used cardinal direction instead of velocity, and attraction
used squared distance instead of distance.

## Verification and remaining work

- Seven queue regressions cover FIFO/key isolation, iteration, reentrant retirement,
  detached-generation clearing, exemptions and failed-admission atomicity.
- Controller regressions cover real velocity, shared spiral behavior, original
  attraction normalization and old state-only null calls. Mode snapshots are tested.
- Latest family XML results: 318 cases, zero failures/errors/skips: DragonAPI 256,
  GeoStrata 8, RotaryCraft 34, ReactorCraft 8, ChromatiCraft 12.
- The first successful isolated fixture proves native resource identity, immutable
  modes, UV/light preservation in a real four-vertex mesh, duplicate rejection,
  combined pending/active capacity, exemptions, original blur/seed ticking, shared
  controller updates and level cleanup. It compiled 112 GPU pipelines across all
  64 modes. Evidence: build/d03-native-client2.log.
- Final isolated client acceptance passed in build/d03-native-client4.log. The
  stronger fixture verifies retirement while a native group is ticking, immediate
  combined pending/active capacity, a single remove callback, and actual regular
  world frames extracting lit ordinary-alpha and screen custom-texture probes.
  The exact-class test delegate is unregistered and cleared afterward.
- Dedicated mixin audit: 30 transformed, 24 client bindings correctly classified
  OTHER_SIDE, two optional dependencies absent; no required binding failure.
- At the D03 checkpoint the family-only composite completed 997
  tests with 164 required failures; the external-mod composite completed 1,001
  tests with 209 required failures. Failures include fluid intake, machine input
  sides, progression, menus/sync and external Curios mock-player payload setup.
  N09 subsequently repaired this work; the initial diagnostic logs are: build/d03-family-dedicated.log and
  build/d03-dedicated-smoke.log. Current passing profiles are recorded in
  build/n09-proof.json and the N09 fixture/contract notes.
- The standalone RotaryCraft run completed 979 tests with ten required failures;
  several fixtures now require other included mods, so it is not interchangeable
  with the composite all-family profile. The older 107-test checkpoint is a
  historical narrower result, not evidence for today's complete registered set.
- The initial unfiltered composite selected 1,400 tests including long worldgen
  suites and was stopped by verified audit JVM identity. The default composite
  launcher now explicitly passes --tests rotarycraft:* to implement its configured
  namespace selection. Explicit gameTestSelector overrides remain available.
- Native improved-transparency scene composition and screen/water ordering remain
  unverified. No pixel-level parity claim is made. Evidence summary:
  build/d03-proof.json. Backlog: 55 complete / 109 open / 164 after adding N09.

The first attempted client stopped at NeoForge's loading-warning screen because
of carried-over OnlyIn annotations. Those obsolete annotations were removed;
physical-client entry points and client-only mixin configuration own sided loading.
Normal Play uses its separate arguments, saves and immutable launch snapshot;
these checks use the isolated build/dragonapi-client-audit directory.

Source references: generated Minecraft/NeoForge 26.3.0.76 source and resource jars
under DragonAPI/build/moddev/artifacts; original DragonAPI commit de1bcf1f,
Instantiable/Rendering/ParticleEngine.java and Instantiable/Effects/EntityBlurFX.java.

The final callback regression exposed an additional native-stage eviction after
an overflow victim was already marked dead. Controller retirement and active clear
now call remove only for live particles, while still releasing the reservation
when the actual native queue node is removed. This fixed the double callback;
client4 passed the unchanged once-only assertion.

N09 follow-up (2026-10-19): final unit verification passed all 318 cases; the
rotarycraft:* profiles passed 998 family-only and 1,002 external-mod cases. A mixed
client/GameTest invocation verified client fixture isolation and real ClientLevel
radar/sonic receivers. Normal Play arguments match the D03 baseline. D03 itself
remains open for improved-transparency scene and screen/water composition review.


### Late screen phase (2026-10-19)

The native solid/after-terrain split placed screen layers in the solid phase,
before translucent terrain. Screen blending requires a separate forward phase:
its per-channel destination factor cannot be represented by scalar OIT
transmittance. Custom screen quads now retain native QuadParticleRenderState
storage separately and submit through a dedicated SimpleFeatureRenderPhase.
The native QuadParticleFeatureRenderer still owns mesh preparation and drawing.
The phase participates in native collection draining/reset and prepared grouping.
Presence checks inspect prepared groups, since submission queues have drained.

Classic rendering executes this phase immediately after native after-terrain
particles. Improved transparency executes it after the OIT composite in a pass
that loads the existing main color and opaque scene depth. Neither path clears
color/depth or writes depth. The original screen blend equation is preserved.
The regular scalar-alpha layers retain native OIT pipelines and stages.

The isolated client fixture requires both alpha/lit and screen probe extraction
and a populated, successfully executed screen phase in each transparency mode.
It restores the initial option and unregisters its private delegate afterward.
Screen-only native extraction and reset also have explicit regression assertions.
The first dual-mode run passed in build/d03-late-client.log. Final regression
acceptance and water composition evidence are recorded in subsequent ledger notes;
D03 remains open until deterministic screen/water composition is verified.

Final late-phase acceptance: build/d03-late-client-final.log passed in 3m44s,
including screen-only extraction/storage/reset and both real rendering modes.
DragonAPI units: 256, zero failures/errors/skips. Play VM/program argument hashes
match the prior checkpoint. D03 stays open for controlled water pixel composition.
