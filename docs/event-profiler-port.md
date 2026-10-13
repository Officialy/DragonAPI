# EventProfiler listener instrumentation — D48, 2026-10-13

Target: Minecraft 26.3, NeoForge 26.3.0.51-beta, event bus 8.0.5.

The original profiler at DragonAPI c29c4fcc2d3a65566783a4a163d8e682116e868c
(Auxiliary/Trackers/EventProfiler.java) wraps actual handlers and ranks their elapsed
time. The previous accepted port parsed the command and exposed manual timing hooks,
but ordinary NeoForge callbacks never reached those hooks.

The common NeoForge initializer now builds a ProfilingEventBus using its actual
BusBuilder. This is a subclass of the native EventBus, constructed before EVENT_BUS
is published, with the native checker, shutdown state and exception handler intact.
Native registration, dispatch, listener priority, inheritance, cancellation and
exceptions remain authoritative. Mod lifecycle buses are not replaced.

Enabling installs temporary GeneratedEventListener wrappers in the selected event's
native listener lists and their concrete parents. The adapter updates registration
targets too, so native unregister removes the same listener identity. Registering or
unregistering during a session refreshes instrumentation. Disabling restores original
handlers and invalidates native caches; ordinary idle posts take the native path.

The adapter is intentionally isolated and version bound to bus 8.0.5. Source reference:
the cached net.neoforged:bus:8.0.5 sources, particularly EventBus, ListenerList,
ConsumerEventHandler, SubscribeEventListener and GeneratedEventListener. The source
jar SHA-1 is aad64cf0f455585a518c54d7aecf8d656aff912e. Private metadata is accessed
when installing/restoring handlers; enabled posts also read the native shutdown
flag. Handler bodies do not reflectively invoke their delegates. Bus upgrades must
rerun the native acceptance suite. Unsupported instrumentation reports UNAVAILABLE
instead of starting a session that cannot measure callbacks.

Measurements select the exact concrete event class and the thread that enabled
profiling. A server console command therefore profiles its server thread; other
threads and event subclasses still dispatch normally but do not contribute. The
explicit-bus overload supports instrumented integration buses. Abstract event bases
and events rejected by the native bus checker cannot start a session.

Thread-local dispatch frames preserve nested posts, even when a handler reuses the
same event object. Each real body call contributes elapsed nanoseconds, call count
and failure count. Cancelled handlers whose bodies are skipped contribute nothing.
Native exception handling and propagation remain intact. Event fires count actual
posts, including empty-list posts, instead of dividing handler calls by the current
number of handlers. Shut-down or invalid phase posts are not reported as fires.
Times are inclusive handler times: nested calls can contribute to both their own
row and the invoking handler's row. The sum is not whole-post wall-clock duration.

Reports are immutable coherent snapshots, sorted by descending total handler time.
Stopping retains the report; a successful new session resets it. Invalid start
attempts preserve the previous report. Common shortcut declarations use string
class names to avoid loading client-only classes on dedicated servers. Colliding
nested aliases such as Pre require an enclosing-class or fully qualified name.
Legacy explicit firePre/firePost hooks remain available with balanced nested spans;
they do not invent a count of bus posts.

Commands retain console support and existing status return behavior:

```text
/profileevent enable reika.dragonapi.instantiable.event.SetBlockEvent$Post
/profileevent display
/profileevent disable
```

Display includes event fires, thread scope, total inclusive handler time, per-handler
averages, share of measured time, call counts and failure counts. Stop profiling when
the sample is complete to restore the original handler lists.
The class-name argument accepts the $ in nested binary names; Brigadier's word
parser previously rejected those names before they could reach the profiler.

Regressions: EventProfilerTest uses the real bus implementation for cancellation,
annotated and lambda handlers, priority, recursion, dynamic registration, native
exception handling, exact-class/thread selection, shutdown, phases, empty posts,
immutable snapshots and start validation. The native dragonapi_audit_event_profiler
fixture verifies NeoForge.EVENT_BUS's concrete instrumentation and measures a real
LevelChunk block mutation through the command. TestInstance's opt-in client audit
also measures client event callbacks on that global bus and cleans up its listener.
