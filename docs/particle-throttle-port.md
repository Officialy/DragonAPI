# Native particle admission and lifecycle on Minecraft 26.3

This is D02 (`ThrottleableEffectRenderer`). D03 (`ReikaParticleEngine` custom
texture/render-mode/exemption batching) remains open. The commented D03 drawing
bodies are not claimed as restored by this work.

## Original contract and modern ownership

DragonAPI `de1bcf1f`, `Extras/ThrottleableEffectRenderer.java` owned the global
1.7.10 EffectRenderer replacement. It ran ordered cancellation handlers, dispatched
exact particle classes to delegates, applied a configurable FIFO cap to its old
texture layers, rendered visible particles, propagated tick/world-clear/drawing,
and appended delegate statistics. Insert disabled depth testing. Bounds were
centered on each particle's scale. Later ports left admission/rendering incomplete,
returned a shared zero bounding box and cast the ordinary Minecraft engine to an
uninstalled replacement class.

26.3's engine owns ParticleResources, pending admission, native limit reservations,
particle groups and extraction into immutable drawing data. The port attaches a
controller to each actual native engine through client mixins. It keeps the
Minecraft engine instance and its existing ParticleResources. It never creates
another resource/provider cache or assigns Minecraft's final particleEngine field.

`getRegisteredInstance()` obtains that attached controller. Constructing another
facade for an engine shares its existing registrations/state rather than replacing
or orphaning them. Creation freezes the original configurable limit with the same
minimum of 250. Registrations and native mutations belong to the engine's creating
thread; foreign-thread add, tick, clear and extraction are rejected. Tick/clear
checks run before native mutation, rather than after it.

## Admission and capacity

Handlers run from an ordered snapshot before native admission. Cancellation does
not reserve a native ParticleLimit slot. Exact-class delegation follows handlers;
subclasses do not accidentally inherit another class's delegate. Native admission
and native ParticleLimit rejection remain authoritative in the receiving engine.

26.3 SINGLE_QUADS combines the old particle, terrain and item texture layers.
Their budgets remain separate, classified by native layer texture location.
Equal texture identifiers share a canonical identity key. Non-quad native types
retain their own identity-keyed buckets.

Both pending and active queues use configured FIFO budgets per bucket. Pending
overflow removes the oldest pending entry of that bucket without changing global
FIFO order for surviving entries. Active overflow does the same in its native
group. This bounds spawn bursts before the next native tick as well as rendered
particles after admission. Active and pending entries are separate native phases;
the count API includes both, so it may temporarily exceed one active bucket's
limit between ticks. Pending entries are not drawn until the native tick admits
them, and each active bucket stays within its configured rendering limit.

Retirement marks an owned particle removed and releases its native ParticleLimit
reservation once. Native death/iterator removal keeps native counter handling;
iterator removal does not apply a second retirement callback. Clearing a world
retires active and pending particles before releasing native collections/counters.
The queue detaches its old generation before running retirement callbacks, so a
reentrant producer cannot extend that clear indefinitely. The engine's clear scope
also rejects admission into its owned subtree until cleanup finishes, preventing
remove callbacks from repopulating cleared worlds or recreating limit reservations.
FIFO replaces vanilla's probabilistic reservoir/hard cap for these configured
queues. No swallowed concurrent-modification failure is used as control flow.

## Delegate ownership and lifecycle

An engine has one lifecycle parent when used as a delegate. Self/ancestor cycles,
sharing across multiple parents and delegating the global Minecraft engine into
another tree are rejected. Multiple class registrations may share one delegate,
but it ticks, extracts, receives level changes and appears in statistics once.

Replacing/removing the last registration releases that delegate's ownership and
particles; removing one of several class registrations leaves the others intact.
Level propagation suppresses duplicate child clears caused by native setLevel
internally calling clearParticles. Registrations persist across worlds while
particles, pending reservations and remembered view data are cleared. Tick-time
or extraction-time spawning stays in the native pending queue and cannot mutate
the currently iterated active group.

## Bounds and drawing

Native quad extraction uses an AABB rather than the old zero box or a center-only
frustum test. Quad bounds include their visual diagonal at the tick endpoints,
unioned with their physical box; non-quad callers retain the native physical box.
Each result is an independent immutable AABB. Invalid non-finite visual sizes
report an error rather than silently producing corrupt geometry.

Drawing remains on the native ParticleGroup -> ParticleGroupRenderState ->
SubmitNodeCollector path. Existing particle layers keep their own shaders,
transparency targets, blend modes, light, sprite UVs and native/OIT batching.
The render toggle suppresses extraction while ticking/retirement continue.

Insert is sampled once per extraction tree using the current SDL InputConstants
API. A scoped override copies each layer's pipeline and all three OIT pipelines,
disabling depth test/writes while preserving shaders, defines, buffer layouts,
blending, stencil and texture. It never changes global OpenGL state. Nested scopes
inherit their parent's setting and restore it even on exceptions. Cache keys are
weak source descriptors; values are immutable declarations, not textures/GPU
allocations. World clears release transient layer mappings while pipeline copies
remain reusable for still-live source definitions. Native Minecraft owns compiled
GPU resources and their normal reload lifecycle.

The native pipeline builder uses an unordered set for bind-group layouts. A
toBuilder round trip can change list iteration order. Verification compares the
complete layout set, retaining every uniform/type, instead of treating incidental
list order as a pipeline change.

## Verification

Five queue unit regressions cover per-bucket overflow/global order, identity keys,
iterator removal/fail-fast behavior, invalid-input atomicity and reentrant
retirement, including detached-generation clearing. Final acceptance passed 311
family unit cases (249 DragonAPI, 8 GeoStrata, 34 RotaryCraft, 8 ReactorCraft,
12 ChromatiCraft), all 107 required dedicated tests and the complete isolated
client suite.

The fixture uses separate native engines sharing the already-loaded resources;
it never clears the playing engine. It verifies cancellation/native limit
reservations, exact/shared delegates, cycle and multiple-owner rejection,
statistics, one tick/clear per owner, actual queued/active FIFO overflow under the
configured limit, separate texture-sheet budgets, frustum-edge bounds, a real
four-vertex native particle mesh, normal/depth/OIT pipeline declaration parity,
render suppression, resources/engine identity and level cleanup. Foreign-thread
tick/clear calls fail before native mutation and the owned worker is joined.
A particle whose remove callback tries to spawn a replacement leaves its clearing
engine empty.

The fixture initially used the removed getMainCamera name, then compared an
unordered native bind-group layout set as an ordered list. Both were corrected;
shaders, defines, blend targets, complete layout membership/types and vertex
bindings remain strict checks. The corrected full suite passed, as did the owner
guard client run and the final detached-clear/admission-barrier full run.
Evidence: workspace build/d02-proof.json and build/d02-cleanup-final-accept.log.
The rendering checks cover native extraction/mesh and pipeline declarations, not a
GPU screenshot comparison of the debug key. Default isolated audit arguments
were restored; normal Play argument hashes match D01.
