# Packet pipeline — D43, Minecraft 26.3

Current negotiation: D47 advances the bridge to **2.2.0** for dimension-scoped
entity/biome bodies; this operation's body remains unchanged from the 2.1 checkpoint.
See [dimension-packet-port.md](dimension-packet-port.md).


Original source: DragonAPI `c29c4fcc2d3a65566783a4a163d8e682116e868c`,
`Instantiable/IO/PacketPipeline.java`. Native references: generated Minecraft
26.3 sources (`ClientboundCustomPayloadPacket`, `ServerboundCustomPayloadPacket`,
`PacketFlow`) and NeoForge 26.3.0.51-beta sources (`IPayloadContext`,
`PayloadRegistrar`, `NetworkRegistry`, `ServerPayloadContext`).

## Source and current caller boundary

The original active getMinecraftPacket delegates to SimpleNetworkWrapper.
Its commented replyToPacket describes the intended sender-connection reply,
but does not implement an active reply in the pinned 1.7.10 source. The accepted
26.3 API exposed an empty replyToPacket, a null native packet constructor and a
registerPacket method that ignored its supplied codecs. This batch completes
those public library contracts. No existing non-test family call to replyToPacket
or registerPacket was found. ReikaPacketHelper.getPacket is the retained native
wrapper facade; it keeps null for an unbound channel and gains explicit flow.
This is API completion, not a claim that a currently used gameplay request had
been losing replies in the original source.

## Native transport

Each PacketPipeline owns its actual CustomNetworkBridge. Global helper maps
retain that same bridge rather than creating a second registration/codec owner.
Existing channel IDs, protocol version 2.1.0, legacy packet-type IDs, byte lengths,
DataPacket bodies and default bridge framing are unchanged. All send targets
use the same bridge and retain native channel-negotiation guards for players.
Dimension sends safely handle an unavailable server.

getMinecraftPacket returns a real clientbound or serverbound custom-payload packet.
The overload with PacketFlow explicitly sets outbound direction; use it for worker
threads or code that already knows its logical side. The legacy overload takes
the opposite of an active receive context, otherwise uses the server thread/
dedicated distribution or the physical client. Integrated-server worker callers
should supply flow explicitly. Constructing a packet does not send it or bypass
NeoForge's native negotiated-channel checks.

Incoming dispatch binds the native connection context for the duration of the
application handler only. The implicit reply is limited to that namespaced
pipeline, follows the opposite flow and calls IPayloadContext.reply. Nested
handlers restore the outer scope; exception cleanup removes the scope. Separate
mods with the same short channel name cannot use one another's ambient reply.
The explicit context overload supports code which already has a native payload
context, including a deferred native handler. Calls without an applicable ambient
context fail clearly rather than dropping a reply or guessing its recipient.
Client-only packet categories cannot be constructed or replied serverbound;
the receive-side direction check is preserved.

## Custom registration

registerPacket now retains and uses both supplied encoder and decoder. Each
registered class gets its own native payload identity, bounded byte framing,
codec and bidirectional PLAY handlers. The compatibility overload derives the
identity from the pipeline and qualified class name, normalized to an identifier;
an explicit Identifier overload is available for stable schemas across class
renames or names not representable in identifier paths. IDs must belong to the
pipeline's mod namespace, be unique and differ from the default channel ID.
Register codecs during mod initialization, before RegisterPayloadHandlersEvent;
registrations seal during that actual event and late registrations fail clearly.
Default legacy payload registration remains unchanged. There were no previous
active custom-codec registrations to migrate in this repository.

Typed decoding enforces the registered class and consumes the entire body.
Incoming bytes cannot choose a handler; decoded packets bind to the registered
channel handler. Unknown IDs, foreign outgoing handlers, extra body bytes and
oversized frames are rejected. Existing DataPacket subclasses/default encoders
retain their old bridge encoding unless explicitly registered with a custom codec.

## Validation and remaining work

Eight unit regressions cover native wrappers and unchanged default bytes,
registered codec execution, invalid identities/registrations, foreign handlers,
trailing data, nested replies, server/client flows, explicit contexts, failures,
namespace isolation and client-only direction checks.

Four native fixtures cover production decode/dispatch/reply with a real
ServerLevel/ServerPlayer, handler failure/direction rejection, registered custom
message dispatch, and actual NeoForge registration plus both vanilla native
packet stream-codec round trips. Reply tests use a controlled IPayloadContext
adapter to capture the destination's payload; they do not claim actual remote
socket delivery. The registration fixture runs only when rotarycraft is in the
explicit GameTest namespace property, so it does not add test channels to normal
Play. Its late registration assertion checks the real event boundary.

The final family run includes all 78 required DragonAPI native fixtures and
164 units (116 DA / 34 RC / 8 ReC / 6 Chroma). See the root's
`TestInstance/docs/packet-pipeline-2026-10-12.md` and validation logs.
D44's dormant DragonPayloads/CompoundSyncPacket migration, D45 tank sync sender
semantics, D46 stable legacy sub-IDs, and actual remote-client acceptance remain
separate tasks. No second legacy transport was activated by this batch.
