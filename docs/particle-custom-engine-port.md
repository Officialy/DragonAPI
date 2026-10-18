# D03 custom particle engine port — work in progress

D03 remains open. The legacy `ReikaParticleEngine` implementation is not accepted
into the native rendering slice yet. D02's controller remains the active native
engine integration. This document records verified migration constraints and the
completed queue prerequisite; it does not claim the custom renderer is restored.

## Completed prerequisite

`BoundedParticleQueue` accepts an optional counted-entry predicate. The existing
three-argument constructor still counts every entry, preserving D02 behavior.
Exempt entries participate in the global FIFO and all cleanup/iterator operations,
but never enter the counted bucket and never trigger an eviction. Counted entries
retain the full configured capacity even when exempt entries are interspersed.
The predicate is sampled once per offer; changing the original object's exemption
flag cannot change the bookkeeping of an already admitted node. Key/predicate
validation happens before eviction. Retirement callbacks still see a coherent
queue, and clear still detaches the previous generation before callbacks.

Seven focused tests cover the original five queue contracts plus interspersed
exemption/overflow/removal/clear and mutable-predicate/failed-admission atomicity.
Focused verification: `build/d03-queue-tests.log` (successful Gradle run).

## Verified native rendering constraints

Reference: generated Minecraft/NeoForge 26.3.0.76 source and resource jars under
DragonAPI/build/moddev/artifacts. Historical engine reference:
`git show de1bcf1f:Instantiable/Rendering/ParticleEngine.java` in DragonAPI.

- The original mode has six flags: ALPHA, LIGHT, ADDITIVE, DEPTH, ALPHACLIP,
  and FOG. The partial port lost FOG. The legacy alpha-test threshold is 1/255,
  whereas the native particle fragment shader currently clips below 0.1.
- Native SingleQuadParticle.Layer supplies texture identity, pipeline, translucency,
  and all three OIT pipelines. QuadParticleFeatureRenderer resolves its texture
  through TextureManager.getTexture, so a standalone custom texture is supported;
  it does not have to be an atlas.
- Native quad extraction supplies particle positions, quaternion, UV, color, and
  packed light coordinates. Preserve all of them when mapping a custom mode.
- Native particle shaders apply lightmap and fog. The feature renderer binds
  DynamicTransforms, Sampler2 (lightmap), and Sampler0 (layer texture), alongside
  RenderSystem.bindDefaultUniforms. LIGHT must retain its original entity-lighting
  meaning; do not silently reinterpret it as just the lightmap toggle.
- Native Lighting owns a level lighting UBO with two directional lights and a
  LEVEL entry. Check its binding contract before introducing a Lighting shader
  dependency; a declared but unbound uniform is not a complete port.
- OIT has separate depth-bounds, transmittance, and accumulation shaders/states.
  A mode must preserve its blend/alpha/fog meaning through those passes, including
  additive modes. An opaque pipeline cannot simply be relabelled translucent.
- RegisterParticleGroupsEvent adds a group to the render order unconditionally.
  Registering SINGLE_QUADS again duplicates rendering. Hook existing extraction
  rather than registering a second builtin group.
- QuadParticleRenderState.clear retains empty layer keys. Native client assertions
  must select the expected populated/mapped layer rather than the first map key.

## Remaining implementation and acceptance

Replace eager construction with a lazy native backend sharing the global engine's
loaded ParticleResources. Preserve exact-class delegation and native cancellation,
ParticleLimit reservation, ticking, cleanup, and resource lifecycle. Canonical
batch identities need immutable snapshots of mutable render modes, texture and
native particle group; keep exemptions stable across pending-to-active transfer.
Connect the new counted predicate to custom backend admission, not indiscriminately
to the global engine. Do not mark D03 complete on the queue prerequisite alone.

Restore all mode flags through native pipelines/shaders with deterministic cache
identities, scoped extraction, standalone textures, and correct OIT behavior.
Prove normal and exempt per-key limits, mutation after admission, original UV/color/
light preservation, native resource sharing, world changes, registration lifecycle,
and actual custom shader compilation/drawing in the isolated client. Keep normal
Play configuration and the owner's running client untouched.

Full DragonAPI verification also passed: 251 unit cases, zero failures/errors/skips
(build/d03-queue-full-tests.log). D03 stays open; checklist remains 55 complete /
108 open / 163. This prerequisite does not alter the active native queue policy.
