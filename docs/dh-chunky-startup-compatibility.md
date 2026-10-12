# Distant Horizons / Chunky startup compatibility

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta, Distant Horizons 3.3.4,
Chunky NeoForge 1.5.4. The installed jars in TestInstance/run/mods are the
implementation reference; their SHA256 hashes are recorded in
build/dh-chunky-installed-artifacts.json.

## Failure and preserved behavior

The owner's 2026-10-11 crash occurs while IntegratedServer.initServer creates
the initial spawn. A FULL chunk fires NeoForge's chunk-load event, DH applies
the chunk update and calls AbstractChunkyAccessor.tryRunFirstTimeSetup.
ChunkyNeoForge initializes its Chunky instance at ServerStartingEvent, after
this spawn preparation. ChunkyProvider.get therefore throws its known
"Chunky is not loaded." IllegalStateException. The installed DH accessor also
sets listenerBound=true before calling bindOnGenerationProgressEvent, so merely
suppressing the exception would permanently lose its progress integration.

DragonAPI's optional MixinDistantHorizonsChunkyAccessor checks the actual
provider before DH writes that flag. The known absent-provider condition
defers that attempt with listenerBound=false; later chunk updates retry DH's
original binding method. Other provider failures and incompatible APIs remain
visible errors. Neither installed mod jar nor the user's mod/config selection
is changed. DH's original progress callback and world-generation coordination
remain intact.

The accessor also compares provider identity. DH's accessor can outlive an
integrated world, while each new Chunky instance owns a different event bus.
A replacement provider clears the old bound flag and binds once to the new
bus. The recorded provider is weakly referenced so this compatibility hook
does not retain an old Chunky instance and its server/world. Shutdown returns
the accessor to its retryable state.

Reflection avoids linking Chunky classes into DragonAPI when optional mods
are absent. The @Pseudo target likewise permits DH to be absent. Readiness
checks cache the public provider getter, not its per-world result.

## Isolated validation

The opt-in TestInstance client audit uses dhChunkyCompatibilityTest to load
only the installed DH and Chunky jars alongside the existing family audit.
Its separate directory is build/dh-chunky-client-audit and its save is
dh-chunky-audit. Normal Play and the default Automated Checks setup retain
their original paths and arguments. The fixture is prepared from the audit
save's level.dat with initialized=false and without copied chunks/players,
plus 26.3's separate data/minecraft/world_gen_settings.dat. This forces the
native initial-spawn path rather than relying on an already initialized save.

The client checks the real registered DH accessor after integrated-server
startup. A server-thread lifecycle fixture uses actual installed classes to
verify repeated absent-provider calls, exactly one subscription when ready,
real Chunky GenerationProgressEvent delivery to DH's original callback,
rebinding to a replacement provider, repeated-call deduplication and shutdown.
The original provider is restored in a finally block, and the replacement
provider is disabled. These mutations are opt-in audit fixtures only.

Command:

    gradlew :TestInstance:runClientAudit -PdhChunkyCompatibilityTest --offline --console=plain

The dedicated GameTestServer is not a suitable host for this DH version:
DH's server initialization casts it to DedicatedServer. That unrelated
incompatibility is recorded in build/dh-chunky-native-validation.log, and
is not patched or counted as a passing test. The first client fixture lacked
the separate world-generation file and failed before server startup; this
fixture was corrected. A Gradle success without the explicit audit PASS
markers is not validation of the compatibility fix.

The corrected fresh-world client records DH_CHUNKY_INTEGRATED_CLIENT_PASS and
DH_CHUNKY_LIFECYCLE_PASS in build/dh-chunky-client-final.log. Both installed
mods are active; the actual initial spawn and subsequent progress binding
complete. The existing graphical audit also records DRAGONAPI_AUDIT_CLIENT_PASS;
the complete client run exits successfully in 3m 39s. The 87 DragonAPI unit
tests pass. This covers the reported startup failure and listener lifecycle,
rather than DH LOD completeness or the owner's full Iris/Sodium/shader
configuration.

A separate run without either optional mod passes all 46 selected native
DragonAPI tests and all three TestInstance launcher tests in 38s:

    gradlew :TestInstance:runGameTest :TestInstance:test :TestInstance:prepareClientAuditRun -PexcludeExternalMods "-PgameTestSelector=rotarycraft:dragonapi_*" --offline --console=plain

Evidence: build/dh-chunky-optional-absence-validation.log. This also restores
the default client audit arguments (audit-world, no compatibility-test flag).
Normal Play VM/program argument SHA256 hashes match the pre-test baseline in
build/dh-chunky-normal-play-before.json. The packaged DragonAPI jar includes
the new mixin, and installed DH/Chunky jar hashes remain unchanged.
