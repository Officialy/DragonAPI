# Data, math and I/O distribution audit (D14)

This audit covers the accepted 26.3 source slice, not pristine excluded 1.7.10
files or future integrations. The search includes `instantiable/data`,
`instantiable/math`, `instantiable/io`, `libraries/mathsci` and `libraries/io`.
It checks Minecraft-client, Blaze3D and LWJGL references, side annotations,
physical-client bridges and accepted consumers across the mod family.

An import is not a runtime invocation. Accepted consumers are identified against
the current compiled primary classes after all eight accepted-class guards pass;
source references are then inspected for actual usage. The retained inventory is
`build/d14-accepted-callers.json`. In particular, a client drawing method does not
establish that a common constructor or calculation invokes it.

| Candidate | Accepted use and boundary | Result |
| --- | --- | --- |
| `CircularDivisionRenderer` | Common category geometry/colors; native collector and vertex methods are drawing entry points. `Proportionality` is its only accepted concrete subtype. | No common calculation invokes native drawing. Server construction and API reflection succeed. |
| `Proportionality` | ReactorCraft heat/steam/boiler accounting, GeoStrata vent selection and ChromatiCraft element counts use weights/NBT. The accepted direct drawing consumer is the client `LexiconEnergyPie.Renderer`, registered for `Dist.CLIENT`. | Native server fractions, category geometry, NBT and reflection pass. Existing drawing API remains intact. |
| `Spline` | Common interpolation supplies world shapes, motion paths, entities and machine logic. Accepted visual callers sample positions into their own client renderers; the collector entry point has no accepted common caller. | All three interpolation modes, closed curves, variable endpoints and public metadata pass on the native server. |
| `EnumSound` | Inherently a physical-client `AbstractSoundInstance` adapter. No accepted common caller constructs it; `ReikaSoundHelper` only had an unused import. | Removed the unused import. Restored original repetition and public sound metadata; native client fixture checks the adapter. |
| `SyncPacket` | Common packet framing/deltas and block-entity synchronization. Its Minecraft import was unused. | Removed that import. Public reflection and existing native packet/sync tests pass without introducing a client implementation. |
| `ReikaKeyHelper` | Raw physical-client key bindings and SDL state. No accepted caller exists in the scanned family. | It is a client utility, not server input state. Common gameplay uses `KeyWatcher`; no server route was found into the raw key methods. |
| `ControlledConfig` | The Minecraft search hit is a comment. Session lookups already live in `ClientEnvironment`, reached only from the `Dist.CLIENT` branch of user hashing. | Native invocation verifies the dedicated-server hash is zero without calling the client bridge. |
| `ReikaSoundHelper` and common sound descriptors | Native server emission uses world broadcasts/packets; explicit local-client helpers reach `ClientSounds` only behind the existing physical-distribution guard. `SingleSound` and `SoundVariant` contain no client engine implementation. | The dedicated server invokes a local-client helper safely through the guard; packet/nearby-client delivery is covered separately by existing sound tests. |
| `ReikaChatHelper` | Common item/tool callers include real server-player targets. The port checked physical distribution, which also reports CLIENT on an integrated-server thread. | Restored logical-side routing; player-targeted clearing always sends the existing packet. Untargeted local operations ignore server threads. Local UI work is owned by `ClientEnvironment` and dispatched to client main. |
| `WorldLocation` | Authoritative level/server context resolves dimensions without client lookup. Its compatibility fallback already isolates client-level lookup behind `ClientEnvironment` and physical distribution. | Native server context resolution and public metadata pass. Context-bearing APIs remain preferred. |
| `RemoteSourcedAsset` | Client resource lookup already uses the physical bridge guard; dedicated lookup uses classpath assets. | Native server opens and closes the real warning PNG without a client resource manager or download. Thread/resource loading ownership remains separate D15 work. |
| `PacketPipeline` / `ReikaPacketHelper` | Packet flow uses explicit targets, reply context and integrated-server thread ownership where required; client-level convenience lookup is an explicitly client-serverbound helper. | Existing native framing/direction tests remain active. D14's targeted chat clear additionally exercises native integrated-server-to-client delivery. |

The existing `ClientEnvironment` and `ClientSounds` implementations provide the
physical boundaries. Local chat writes are now implemented in `ClientEnvironment`
with client-main dispatch. Common data/drawing APIs and their callers remain intact;
this audit does not claim all legacy rendering APIs or visual output are complete.
D06 remains the rendering-context/matrix/lighting task. Standalone authenticated
hosting and unported source remain separate acceptance work.

## Regressions and discovered sound behavior

`DragonAPISideAuditTests` runs in the actual native dedicated GameTest server.
It checks proportional weights, hit geometry and NBT round-trip, all spline
interpolation modes, closure, variable endpoint updates, the sound distribution
guard, dedicated config hashing, common world/resource lookups and declared/inherited API reflection. The
initial probe passed before production changes as well; it did not demonstrate
a client-import crash. This avoids treating a textual reference as a defect.

The indirect bridge review exposed an actual routing defect. The original
`Libraries/IO/ReikaChatHelper.java` at `de1bcf1f` uses logical/effective side.
Physical distribution alone sent integrated-server calls into client chat UI.
The restored contract routes `ServerPlayer` clearing through CLEARCHAT, suppresses
untargeted local operations on server threads, and delegates local strings and
component snapshots to the client-main executor. Strings retain the original
splitting/chunking helper and components retain translation keys/arguments.
The native `ChatBoundaryClientChecks` fixture blocks client main while the real
integrated server performs operations, verifies no direct UI mutation, then lets
the packet queue deliver targeted clearing. It also verifies worker-originated
local writes execute only on client main and restores prior chat state.

The audit did find a real adapter regression: `EnumSound.setRepeating()` returned
the instance without enabling repetition, and `isLooping()` always returned
false. The original implementation in DragonAPI commit `de1bcf1f`,
`Instantiable/IO/EnumSound.java`, sets repeat true and reports it with zero delay.
The modern adapter now uses `AbstractSoundInstance.looping`, keeps fluent and
idempotent `setRepeating`, and retains the nonrepeating constructor default.
Original public final `posX`, `posY`, `posZ`, `volume`, `pitch` and `attenuate`
metadata are restored, with modern getters returning those same values. Reference
constructors and explicit attenuation override retain the original semantics.

The opt-in `SoundClientChecks` fixture verifies that metadata and constructor
behavior, submits the repeating adapter to the actual client sound engine,
observes its looping flag/zero delay through the native sound event, and stops
its owned voice in `finally`. Ordinary Play never creates that fixture voice.

The native acceptance is scoped to the current NeoForge 26.3.0.76 runtime and
accepted build. It does not establish that arbitrary client methods can run on
a server or prove every possible external server distribution/configuration.
