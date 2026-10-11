# BlockArrayComputer — D16, Minecraft 26.3

## Original boundary and defined operations

The original DragonAPI source (`de1bcf1f`, `Auxiliary/BlockArrayComputer.java`)
contains empty LOAD, FILL and ITERATE branches. Its only source-tree reference is
a private computer instance in `BlockArray`, allocated before constructor coordinates are
inserted, with no accessor or execution path. The accepted port had the same
empty branches. There is no implemented operation behavior to recover from that
source; this change defines explicit inputs using the existing coordinate-NBT,
iteration and native block replacement contracts.

The constructor, `Runnable.run()`, `setOperation(Operation)` and three operation
names remain. `BlockArray.getComputer()` now captures current membership on each
call, rather than eagerly retaining an unused instance. Missing operation inputs
fail explicitly. Configuring an operation selects it; `setOperation` can select
an already configured operation. A successful result is available through
`getResult()`; unsuccessful, unexecuted or reconfigured tasks have no result.

| Operation | Input | Work and result | Execution thread |
|---|---|---|---|
| LOAD | `setLoad(label, CompoundTag)` | Decode a deep copy of coordinate NBT into detached, bounded, deduplicated coordinates | Owner or worker |
| ITERATE | `setIteration(Consumer<BlockPos>)` | Visit immutable captured coordinates in insertion order; return that snapshot | Owner or worker, with a world-free callback |
| FILL | `setFill(ServerLevel, BlockState, FillAuthorization)` | Replace authorized captured targets through native `setBlock(..., Block.UPDATE_ALL)`; return successful changed-block count | Owning server thread |

Results contain an immutable ordered list of immutable `BlockPos` values. LOAD
does not overwrite its source array, and FILL/ITERATE do not consume membership.
Caller changes to the array, mutable positions, bounds or supplied NBT after
capture/configuration cannot alter an outstanding task's snapshot. A decoded
result can be used to construct a new `BlockArray` under that array's owner's
control; the utility does not publish worker mutations into a live mutable array.

## Thread ownership and scheduling

`BlockArray` remains mutable and is not made globally thread-safe. Capture it while
exclusively owning it. Capturing a server-attached array checks the owning server
thread. Unattached arrays and client-owned arrays require the caller to retain
exclusive ownership during capture; copying a list while another caller mutates
it is not supported. The task retains no source-array reference.

LOAD owns a private deep copy of NBT and decodes into a private world-free array.
ITERATE passes only immutable positions to the callback. The callback's captured
objects and side effects remain the caller's responsibility: a worker callback
must not access a live world, live block entities or other thread-confined state.

FILL capture and execution each check `ServerLevel.getServer().isSameThread()`
before accessing chunks/states or invoking authorization. The utility never
starts threads, chooses an executor, acquires chunk tickets or silently schedules
a world change. A caller may run pure computation through its own executor and
explicitly submit a prepared FILL task with `server.execute(task)`. Do not block
the server thread waiting for work that is queued to that same server.

Configuration and execution are coordinated: overlapping runs, reentrant runs and
reconfiguration while running fail. Results publish only after successful
completion. Callbacks and native exceptions propagate to the caller/executor;
the task does not swallow them or report a stale successful result. A completed
task may be configured/run again; FILL expected states must be freshly captured
when previous execution or outside work changed its targets.

## Authorization and native placement

FILL requires an explicit authorization callback; there is no default allow-all
policy. The callback receives the server world, exact position, captured previous
state and requested replacement state. It runs on the server thread at execution
time and must enforce the caller's actual actor/claim policy without mutating the
world. This generic utility cannot infer a player or substitute for an integration's
protection checks.

Capture checks every target's world bounds, world border and currently loaded full
chunk using the 26.3 `ServerChunkCache.getChunkNow` API. It records full `BlockState`
values, preserving properties rather than metadata. Execution rechecks target
availability and expected state, then obtains permission for every target before
writing any target. It rechecks all expected states after permission callbacks.
A denial, authorization exception or stale preflight changes no target itself.

Each native write uses `Block.UPDATE_ALL`, retaining neighbor updates and client
notification. Targets already equal to the requested state are authorized but
count as zero changes. Membership outside the captured snapshot is untouched.
No chunk is force-loaded. Block entity/NBT lifecycle and drops caused by neighbor
updates follow native placement; this operation does not restore a declared
`FilledBlockArray` blueprint or copy block entity data.

Native world updates are sequential and **not transactional**. Neighbor updates
can change a later target after earlier writes succeed. FILL rechecks each target
before writing and stops instead of overwriting that changed state. Stale/write
failure messages include the number of completed changes; failed runs publish no
successful result. There is no unsafe rollback of blocks, inventories, drops or
neighbor effects. Expected-state comparisons cover block states, not a snapshot
of inventories/NBT; authorization can inspect current block entity data when its
policy needs that information.

## Coordinate persistence compatibility

The original coordinate writer used `x`, `y`, `z`. The port accidentally wrote
`pos.getX()`, `pos.getY()`, `pos.getZ()` while the reader still expected original
keys, collapsing nonzero saved coordinates to the origin. `BlockArray` now writes
the original keys and reads both formats, preferring an original key when both
exist. Missing numeric fields retain the existing zero fallback.

The legacy `<label>_lim` tag remains written. Loaded limits are derived from actual
accepted, deduplicated membership rather than trusting that persisted cache,
which can describe coordinates rejected by current bounds. This also repairs
stale limits without discarding valid coordinates. Empty loads retain the normal
empty-array limits. The schema remains coordinate-only; no fabricated state/NBT
persistence format is added.

## Validation

Six unit regressions cover detached worker iteration and positions, private NBT
copying, inherited bounds/deduplication, original and affected-port save formats,
actual loaded limits, missing/failed operations, concurrent configuration/run
rejection, and fresh captures after array construction.

Five native ServerLevel fixtures cover thread guards before world access or
permission, pure worker iteration from a captured server array, deny-all-before-write
behavior, authorization exceptions, stale jobs, full stair-state properties and
changed counts, no-op fills, explicit worker-to-server submission, build-height and
unloaded-chunk rejection, and real torch-support neighbor changes with partial
failure reporting.

Focused command:

```text
gradlew :DragonAPI:test --tests reika.dragonapi.auxiliary.BlockArrayComputerTest :TestInstance:runGameTest -PexcludeExternalMods -PgameTestSelector=rotarycraft:dragonapi_computer_*
```

The headless audits use disposable GameTest worlds. Normal Play saves, installed
mods, launch arguments and the separate pending porting batches remain separate.
