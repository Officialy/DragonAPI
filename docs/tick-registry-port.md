# Tick dispatch on Minecraft 26.3

D10 retains the original DragonAPI `de1bcf1f`
`Auxiliary/Trackers/TickRegistry.java` handler contract while bridging native
26.3 events through physical-side entry points.

## Phases and payloads

Every registered handler's `canFire` is evaluated for the actual START or END
phase. The argument positions remain:

| Tick type | Payload |
| --- | --- |
| RENDER | `Float partialTick`, `Phase` |
| CLIENT | `Phase` |
| PLAYER | `Player`, `Phase` |
| WORLD | `Level`, `Phase` |
| SERVER | `Phase`, `MinecraftServer` |

The SERVER context in the second slot is the accepted port's existing addition;
the original phase remains in slot zero. NeoForge level events run on both
logical sides, so world handlers retain their own side policy. A handler can
select all tick types with `EnumSet.allOf(TickType.class)`.

`TickRegistry` has only common event signatures. The physical-client subscriber
`ClientTickEvents` maps render and client Pre/Post events to START/END. This keeps
`ClientTickEvent`, `RenderFrameEvent` and `DeltaTracker` out of the common
registry's class-loading and verification path.

## Partial ticks and the built-in clock

The cached NeoForge 26.3.0.76 `RenderFrameEvent.getPartialTick()` returns a
`DeltaTracker`, not a float. The bridge reads
`getGameTimeDeltaPartialTick(false)` and passes the resulting boxed Float to the
original handler API. This uses the current game-time interpolation, including
paused residual time and the frozen-game value, rather than elapsed game ticks,
real-time delta or an interpolation that ignores freezing.

Both native frame events dispatch. START-only handlers run once before rendering,
END-only handlers run once afterward, and both-phase handlers receive both.
The built-in `ReikaRenderHelper.RenderTick` is registered once during queued
client setup. Its START-only policy advances the render counter once per frame
and updates the cached partial tick. The counter uses long storage to match its
public getter and avoid the previous int-wrap boundary. It remains process-owned
and advances in menus/paused rendering as well as worlds, as the original frame
contract intended.

The accepted Chroma probe decorator consumes this counter for its authored
six-frame color steps; excluded legacy render consumers retain the same float
and frame API for their future full subsystem ports.

## Registration and startup lifecycle

Per-type copy-on-write lists retain registration order and original multiplicity.
Parallel startup registration cannot lose entries, and a dispatch sees a stable
snapshot even when a callback registers or removes handlers. New registrations
take effect on subsequent dispatches. Payload arrays are independent for each
handler, preserving the original per-call varargs behavior.

`unregisterTickHandler` removes a handler's registrations by identity, including
intentional repeated registrations, without removing equal-but-distinct handlers.
Handler failures continue to propagate to the event owner.

The original `GameFinishedLoadingEvent` was also missing from the port. It is now
restored as the original marker event, posted once after the first native client
tick phase following startup. The flag belongs to the physical client process;
world changes, logout and resource reload do not reset it. Marking it before
posting also prevents nested callbacks from posting it again.

## Acceptance

Unit regressions exercise phase filtering, exact render/client payload positions,
independent payload arrays, reentrant registration, identity cleanup, repeated
registrations and failure propagation. Dedicated-server/native tests verify the
common registry still serves existing gameplay consumers without client-event
loading dependencies.

The isolated client uses actual native Pre/Post events and `DeltaTracker.Timer`
running, paused, frozen and resumed cases. It checks a counter beyond int range,
once-per-frame advancement, current cached partial time, START/END/both handlers,
native client phases, the one-time startup event and the actual Chroma probe
decorator changing color over live frames. Fixture observers are unregistered;
synthetic timer samples restore the prior frame/partial state.

Final evidence is recorded in the audit/porting ledgers and root backlog.
