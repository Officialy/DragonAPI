# Inert entity constructor identity — D54

The old world-only InertEntity constructor supplied EntityTypes.ARROW. This can never
infer a subclass's registered identity and would misidentify a future caller for
serialization, spawn synchronization and rendering. It had no accepted consumers.
The unused fallback and its obsolete imports are removed; every concrete subclass
must pass its real EntityType to the existing typed constructor.

The accepted direct subclasses are ReactorCraft EntityRadiation and EntityFusion,
and ChromatiCraft EntityPylonOverloadShock, EntityAurora and EntityDeathFog. The
accepted ParticleEntity base already forwards a supplied type. ReactorCraft neutron
and plasma, ChromatiCraft luma burst, laser pulse and piston spline (through the
abstract EntitySplineProjectile) also preserve their registered identities. The
accepted family recompiles with the fallback absent, checking all indirect callers.

Excluded pristine legacy consumers include ChromatiCraft EntityParticleCluster,
EntityOverloadingPylonShock, EntityNukerBall, EntityTNTPinball and EntityChromaManaBurst.
They remain intact outside the current accepted slice. Their eventual full ports
must supply their own registered types, as the already ported counterparts do;
their absence is not addressed with an invented universal inert entity type.

A real family-server regression creates all ten accepted concrete inert types via
their registered factories, checks runtime identity, completes native loading with
default saved fields, then calls the native Entity save path and verifies the serialized
ID. Factories defer fields such as radiation intensity to loading; serializing an
uninitialized loader instance is not a valid persistence fixture. This covers registered constructors and
native save identity, rather than claiming every entity's full behavior, renderer,
network payload or persisted gameplay round trip. Future entity ports need their
own behavioral acceptance alongside this shared identity contract.
