# Block entity tick failures — D53, Minecraft 26.3

`BlockEntityBase.updateEntity()` lets unexpected lifecycle errors propagate to the
native block entity ticker. Animation or first-tick initialization failure aborts
the remaining synchronization, callbacks, completed-age increment and caller's
machine body. Tick timing, natural/accelerated tick gates, locked-mod behavior and
the existing initial-sync opt-outs retain their contracts.

The original 1.7.10 base caught five broad programming exception classes, logged
them and continued the tick. The port retained those catches for shared lifecycle
hooks while running the machine body outside them. That could publish incomplete
state, execute deferred actions after failed initialization and skip initialization
on later ticks. Broad exception classes do not establish that recovery is safe.

## Native failure policy

The generated `minecraft-patched-26.3.0.51-beta-sources.jar`,
`LevelChunk.BoundTickingBlockEntity.tick()`, supplies the policy for every failure
that escapes a registered ticker:

- Build a `Ticking block entity` crash report and fill its block entity category
  with the registered type, implementation and world position/state.
- With NeoForge's `removeErroringBlockEntities=false` default, throw a
  `ReportedException`. The failing tick cannot silently continue.
- With that existing option enabled by the owner, log the full report, mark the
  entity removed and remove it from the chunk. The native policy retains the block
  state; DragonAPI does not add drops, retries or replacement entities.

The NeoForge 26.3 `NeoForgeSyncedConfig` source defines the option as a world-restart
setting, default false. Production code never changes that setting. This policy
applies to ordinary production runs and development runs alike. Direct tick callers
receive the original exception/error and must supply their own explicit policy;
the base does not presume every caller is a native level ticker.

## Explicitly handled operations

`writeError(Throwable)` remains public for operations that catch and handle an
error locally. It reports rather than throwing, removing the entity or choosing
recovery for the caller. Existing local handling is preserved. In particular, the
original ChromatiCraft effect/acceleration callers retain their existing handlers;
this task does not claim that every still-excluded legacy handler has been ported.

The log entry now passes the complete Throwable to Log4j, retaining its stack,
causes and suppressed exceptions. Its context includes the registered type,
implementation class, position, dimension and **logical** client/server side.
An unattached entity is labelled as such. The optional `CHATERRORS` display remains;
the structured log is emitted before chat. Physical distribution is no longer used
to mislabel integrated-server errors as client errors. No raw `printStackTrace()`
or empty informational log accompanies the report.

## Regression coverage

`RotaryCraft/DragonAPITickFailureAuditTests` uses a controlled subclass of the real
registered reservoir entity, its production block ticker, and a real ServerLevel.
It covers all five formerly swallowed exception classes plus an illegal-state
exception and a linkage error, at both animation and initialization. Each failure
escapes unchanged and stops sync stages, machine logic, callbacks and completed
age advancement.

The native-policy fixture invokes the actual private chunk ticker factory by
reflection; it does not reproduce the policy. It verifies the default crash
report and opt-in removal, no retry of a removed entity, retained block state and
normal world ticking after a fresh real reservoir is placed. It changes only the
isolated test process's in-memory config value/cache, restores it in `finally`
and never saves configuration. The handled-operation fixture confirms normal
first-tick completion and captures the real structured log to check its context
and nested Throwable cause.

Run focused regressions with:

```text
gradlew :TestInstance:runGameTest -PexcludeExternalMods -PgameTestSelector=rotarycraft:dragonapi_tick_failure_*
```

`-PexcludeExternalMods` belongs to the disposable headless audit. Normal Play and
its installed mods/saves are separate.
