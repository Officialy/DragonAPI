# FileInputThread loading and ownership (D15)

The available historical implementations are scaffolds: `run()` is empty in
the original 1.7.10 source at `de1bcf1f:Auxiliary/FileInputThread.java` and in
`08440dc4:src/main/java/reika/dragonapi/auxiliary/FileInputThread.java`. No callers
exist in the pre-D15 source slice. There is no historical decoder/result/error
implementation to recover. This change retains the existing fluent method names,
Runnable entry point, four FileType names/order and Object-valued `getFile`, then
defines the missing behavior explicitly.

| FileType | Successful result |
| --- | --- |
| IMAGE | A decoded heap `BufferedImage`, preserving pixel color/alpha. |
| TEXT | A strict UTF-8 `String`, preserving source line endings. |
| XML | A namespace-aware W3C DOM `Document`. |
| SOUND | A detached `byte[]` containing the original encoded sound payload. |

SOUND loads encoded data verbatim, including original OGG assets. The caller
chooses the audio decoder/playback system afterward. These results own no GPU
texture or native audio handle. Images and DOMs are mutable caller-owned objects;
publication is safe, but subsequent concurrent mutation requires caller ownership
or synchronization.

## Sources and execution

`setFiletype` and `setFilepath(Class<?>, String)` retain their names and fluent
return values. Classpath paths follow `Class.getResourceAsStream`: package-relative
without a leading slash and absolute with one. A missing resource fails clearly.
New `setFile(Path)` selects a filesystem source. `setData(byte[])` captures a copy
of caller data. `setSource(StreamSource)` is the advanced worker-safe opener;
it opens a fresh stream at execution time and must not read live world or client
resource-manager state. Default classpath/filesystem sources need no game context.

Call `run()` directly, place the Runnable on an owned worker, or use
`submit(Executor)`. The utility creates no implicit threads/executors. Configuration
freezes atomically before scheduling and cannot change afterward. Each instance
executes once. A second public run/submission is rejected, and duplicate execution
of an executor's queued command cannot read the source twice.
The loader releases its own source reference when execution configuration is
captured, so retaining a completed loader does not retain another encoded input copy.

The states are NEW, QUEUED, RUNNING, SUCCEEDED and FAILED. `getFile()` returns null
while pending, the decoded object on success, or throws `CompletionException`
with the retained failure after failure. `getFailure()` exposes that cause;
`completion()` supplies a read-only CompletionStage view. Observers cannot replace
the loader's own outcome by completing/cancelling a future copy. There is no
implicit cancellation/interruption of the caller's worker. Interrupts detected
at load boundaries fail with InterruptedIOException and retain interrupt status.
Use a new instance for another load or retry.

The opener executes on the selected loading thread. The loader owns its returned
stream, reads all bytes and closes it before decoding or publishing a result.
Read failures preserve suppressed close failures. Normal loading failures are
recorded and complete the future exceptionally; fatal Errors are recorded and
re-thrown. Rejected scheduling fails without invoking the opener. Missing type
or source also produces an observable failed outcome. No partial decoded result
is published.

Images use an owned ImageReader and in-memory ImageInputStream, disposing/closing
both. XML follows the project's existing external-resource restrictions: external
DTD/schema access and DOCTYPE processing are disabled, avoiding implicit reads
outside the selected input. Unsupported/malformed images, malformed XML and bad
UTF-8 are failures. Empty text and binary inputs remain valid whole-file values.

## Client resource packs

`ClientFileInputs.loadResource(Identifier, FileType, Executor)` is the explicit
physical-client adapter. It queues resource-manager lookup, stream acquisition
and the byte snapshot on client main. The stream closes before the decoder job
is submitted. The common loader sees only detached bytes; decoding uses the
caller-selected executor. Completion and failure delivery return to client main.
The supplied decoding executor controls whether decoding runs in the background.
Standard CompletionStage continuation rules still apply: a continuation attached
after completion can run on the attaching thread. Use an explicit owner executor
with an Async continuation when callback affinity must hold regardless of timing.

This intentionally snapshots resource bytes on their owner thread, including
the I/O needed to capture them. Classpath/filesystem sources instead perform
their entire read on the loading executor. The client path prioritizes a stable
resource-pack snapshot and avoids carrying live resource-manager or world state
into the worker. The helper has no common/server reference and does not add
client classes to the common loader's API.

Example for a classpath resource:

```java
var input = new FileInputThread()
        .setFiletype(FileInputThread.FileType.TEXT)
        .setFilepath(MyMod.class, "/assets/my_mod/data/readme.txt");
input.submit(ownedIoExecutor).whenCompleteAsync((value, failure) -> {
    // Handle the result/error on the owning game thread.
}, ownedGameExecutor);
```

Common success completes on the loading thread; scheduling rejection is published
by the submitting thread. The client helper returns its completion to client main.
Callers own any decoded image/DOM mutations and their worker executor's lifetime.

## Verification

Nine unit regressions cover exact Unicode/line endings, immutable queued data,
queued/running/terminal states, one-shot/concurrent scheduling, relative/absolute
classpath paths, filesystem/binary values, PNG alpha/pixels, XML namespaces,
malformed input, XML external references, source closure/order, suppressed close
failure, executor rejection, observer isolation and retained interrupt status.

`DragonAPIFileInputTests` uses an owned worker in the real dedicated GameTest
server to decode the original warning PNG, generated language file, original
RotaryCraft OGG and XML, then shuts the worker down. `FileInputClientChecks` loads
real pack text/XML plus the original PNG/OGG. It verifies that worker-originated
entry waits for owner-thread capture, mutating an uncached pack file afterward
cannot change already captured bytes, and success/error delivery returns to
client main. It also checks missing/rejected jobs, exact decoder counts and owned
worker/file/registration cleanup. Normal Play runs none of these fixtures.

The first client mutation attempt used the direct pack's cached mode and therefore
retained the cached bytes. Its failure is recorded as a fixture configuration
issue. The intended mutable-file fixture uses the pack's uncached/streaming mode;
the production resource cache behavior is preserved.

Final validation: nine loader unit tests, 290 family tests total, all 106 required
dedicated GameTests, and the complete isolated client suite passed. See the
workspace build/d15-proof.json and its referenced logs. One combined run failed
the existing railgun catalogue assertion after the loader passed; the diagnostic
full-client rerun passed all sixteen recipes. Its cause remains unresolved.

Follow-up: the D04 full client run reproduced an empty railgun recipe catalogue.
RotaryRecipeSync omitted the native CRAFTING type requested by its handbook. It
now explicitly requests that type; a dedicated native-payload regression and the
full client suite passed with all sixteen recipes, with the original assertion.
