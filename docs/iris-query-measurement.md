# Iris query measurement (D13, in progress)

D13 remains open until the disabled/enabled/disabled measurement and native
shadow/stage checks all complete. No boolean cache has been introduced.

Both DragonAPI's `IrisCompat` and ChromatiCraft's `IrisCompatibility` resolve
their reflective API handles once and read current shader enablement and shadow
state on every query. The installed Iris jar is
`iris-neoforge-1.11.7+mc26.3.jar`, SHA-256
`58c6bbd537a63df2ab145cc7de6362d6562445c44be50925f638fbf3705c828b`.
Ordinary JDK `javap` inspection of `IrisApiV0Impl` shows that enablement reads the
current pipeline and shadow state reads `ShadowRenderingState`. These observations
come from the installed artifact, not REA output; REA tools/CLI were unavailable.
The bytecode listing is retained in `build/d13-iris-api-implementation.txt`.

The opt-in `IrisQueryProfiler` measures successful calls from both bridges. With
`dragonapi.profileIris` absent or false, it performs no clock reads or collection
updates. During an explicitly owned render-thread session it records query kind,
bridge, observed stage, true/false counts and elapsed nanoseconds. Fixture probes
and production consumers are counted separately. Per-frame consumer totals include
median, p95, p99 and maximum over frames that actually contain consumer queries.
Measurements include timer overhead and profiling changes the surrounding workload;
they are query-time upper bounds, not FPS improvements or isolated microbenchmarks.

The dedicated `irisQueryAudit` fixture uses the live third-person player donation
renderer to generate real consumers. It compares both bridges against Iris's own
API at all nine concrete NeoForge render stages, frame end and Iris's public shadow
callback. It verifies non-shadow list identity and ordered shadow exclusion using
the real shadow state. It requires shadow and normal state in at least twenty
identical frames and real consumer measurements from both bridges. It warms up
thirty frames and measures 120 frames in each disabled/enabled/disabled phase.
Shader enablement changes use the installed public `IrisApiConfig` API. No busy
query loop, fake Iris API or cached boolean supplies the result.

Prepare a shader pack and `iris.properties` only in the disposable directory
`build/dragonapi-client-audit`. Set its initial `enableShaders=false`, select the
pack by filename, then run with a fresh report ID:

```powershell
.\gradlew.bat :TestInstance:runClientAudit -PirisQueryAudit -PmixinAudit -PmixinAuditId=my-iris-measurement -PmixinAuditOptional=iris -PexcludeExternalMods --console=plain
```

The `iris` runtime selection loads only the installed Iris/Sodium jars beyond
the ordinary family and baseline dependencies. It does not edit Play's argument
files or configuration. In this dedicated mode, the ordinary graphical fixtures
are not run; their no-Iris acceptance is separate.

Intermediate `iris-query-audit-progress.json` files identify the last completed
phase and are **incomplete evidence**. Only `iris-query-audit.json` and
`IRIS_QUERY_AUDIT_CLIENT_PASS` indicate the full audit completed. Afterward restore
ordinary audit arguments with `:TestInstance:prepareClientAuditRun` without the
Iris properties. Shader-config changes, callbacks and scene registrations are
confined to the disposable process/directory.

The initial attempt exposed NeoForge's rejection of abstract event listeners;
the fixture now subscribes to each concrete render-stage type. The subsequent
Complementary Reimagined r5.9.3 attempt reached shader activation and failed in
Iris `CenterDepthSampler.sampleCenterDepth` with native frontend validation error
`Missing uniform InSampler (should be COMBINED_IMAGE_SAMPLER)`. The bytecode listing
`build/d13-center-depth-bytecode.txt` shows that this path creates a render pass,
sets `CompositeRenderer.COMPOSITE_PIPELINE` and uses Iris's raw GL program/samplers,
then calls the native draw without binding an `InSampler` through that render
pass. The mismatch between placeholder pipeline validation and Iris's custom
program path is the current compatibility lead, not a proven standalone upstream
reproducer. Validation has not been disabled or bypassed to make the audit pass.
See `build/d13-iris-stages.log` for the actual failure.

Complementary pack SHA-256:
`fed6c879e732cf7fc7bc6570ebb1def43235aa7e96f62859fbdbdbfa0b484b38`.
An independent attempt uses the installed RenderPearl v2.8.0-beta.4 pack, SHA-256
`c494b5eb4706be4e4b8936ddadd88e5071ba7f205c59a9ac36a2c818a4fd7ddc`.
Enabled-phase measurements and a caching decision remain pending. Broader N08
shader compatibility/visual acceptance is also still open.

## Partial result from the independent pack attempt

RenderPearl reproduced the same missing-`InSampler` failure in the installed Iris
center-depth path. Before that failure, `IRIS_QUERY_SHADOW_OBSERVED` confirms both
bridges matched the real active shadow callback and filtering preserved order.
This is one observed callback, not the required twenty complete shadow/normal
frames or enabled-phase performance acceptance. The saved disabled-phase report
is retained separately as `build/d13-disabled-progress.json`.

Across 120 queried frames, real consumers made 720 calls: 480 ChromatiCraft
shadow checks, 120 ChromatiCraft enablement checks and 120 DragonAPI shadow
checks. All returned false, matching disabled Iris. Consumer query totals were
2,700,600 ns; median 20,000 ns/frame, p95 34,000 ns/frame, p99 45,700 ns/frame and
maximum 158,800 ns/frame. These are diagnostic upper bounds for this small live
third-person scene with instrumentation enabled; baseline tests were also running
in another process. They do not justify a broader performance or caching claim.

D13 remains unchecked. Ordinary no-Iris regressions and this partial measurement
must not be presented as enabled-shader acceptance. The compatibility lead is
recorded for follow-up alongside N08; independent backlog work can continue.
