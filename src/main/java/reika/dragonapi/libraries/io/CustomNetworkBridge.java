package reika.dragonapi.libraries.io;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.PacketFlow;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.DataPacket;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;

/** A namespaced, channel-bound compatibility transport for the 26.3 payload API. */
public class CustomNetworkBridge {

    /** Protocol 2.2.0 migrates dimension-scoped bodies; future incompatible schemas must negotiate a new version. */
    public static final String PROTOCOL_VERSION = "2.2.0";

    private final reika.dragonapi.interfaces.PacketHandler packetHandler;
    private final String modId;
    private final String channel;
    private final Identifier payloadId;
    private final CustomPacketPayload.Type<DataPacketPayload> payloadType;
    private final StreamCodec<FriendlyByteBuf, DataPacketPayload> streamCodec;
    private final Map<Class<? extends PacketObj>, PacketRegistration<?>> registrations = new LinkedHashMap<>();
    private final Map<Identifier, PacketRegistration<?>> registrationsById = new LinkedHashMap<>();
    private boolean sealed;

    private record PacketRegistration<MSG extends PacketObj>(Class<MSG> packetClass,
            CustomPacketPayload.Type<DataPacketPayload> type, StreamCodec<FriendlyByteBuf, DataPacketPayload> codec,
            BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        void encode(PacketObj packet, FriendlyByteBuf buffer) { encoder.accept(packetClass.cast(packet), buffer); }
        MSG decode(FriendlyByteBuf buffer) { return packetClass.cast(Objects.requireNonNull(decoder.apply(buffer), "Decoded packet")); }
    }

    public CustomNetworkBridge(String modId, String channel, reika.dragonapi.interfaces.PacketHandler handler) {
        this.packetHandler = java.util.Objects.requireNonNull(handler);
        this.modId = modId;
        this.channel = channel;
        this.payloadId = Identifier.fromNamespaceAndPath(modId, channel.toLowerCase(Locale.ROOT));
        this.payloadType = new CustomPacketPayload.Type<>(payloadId);
        // Per-bridge codec so decoded payloads carry the right type() reference; a single static
        // codec would have to return payloads with a null type, which works for routing but breaks
        // any caller that inspects payload.type() (e.g. for logging or re-dispatch).
        this.streamCodec = codecFor(this.payloadType);
    }

    private static StreamCodec<FriendlyByteBuf, DataPacketPayload> codecFor(CustomPacketPayload.Type<DataPacketPayload> type) {
        return StreamCodec.of(
                (buf, val) -> {
                    if (val.data.length > PacketValidation.MAX_PAYLOAD_BYTES || !val.type().id().equals(type.id()))
                        throw new IllegalArgumentException("Invalid payload size or channel identity");
                    buf.writeVarInt(val.data.length);
                    buf.writeBytes(val.data);
                },
                buf -> {
                    int len = buf.readVarInt();
                    if (len < 0 || len > PacketValidation.MAX_PAYLOAD_BYTES || len != buf.readableBytes())
                        throw new IllegalArgumentException("Invalid payload length: " + len);
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new DataPacketPayload(type, bytes);
                }
        );
    }

    Identifier channelId() { return payloadId; }

    public synchronized <MSG extends PacketObj> void registerPacket(Class<MSG> cl,
            BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        Objects.requireNonNull(cl, "Packet class");
        Identifier id = Identifier.fromNamespaceAndPath(modId,
                payloadId.getPath() + "/" + cl.getName().toLowerCase(Locale.ROOT).replace('.', '/').replace('$', '/'));
        registerPacket(id, cl, encoder, decoder);
    }

    public synchronized <MSG extends PacketObj> void registerPacket(Identifier id, Class<MSG> cl,
            BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        Objects.requireNonNull(id, "Payload identity"); Objects.requireNonNull(cl, "Packet class");
        Objects.requireNonNull(encoder, "Packet encoder"); Objects.requireNonNull(decoder, "Packet decoder");
        if (sealed) throw new IllegalStateException("Packet codecs must be registered before RegisterPayloadHandlersEvent: " + channel);
        if (!id.getNamespace().equals(modId) || id.equals(payloadId) || registrationsById.containsKey(id)
                || registrations.containsKey(cl)) throw new IllegalArgumentException("Duplicate or foreign packet registration: " + id);
        var type = new CustomPacketPayload.Type<DataPacketPayload>(id);
        PacketRegistration<MSG> registration = new PacketRegistration<>(cl, type, codecFor(type), encoder, decoder);
        registrations.put(cl, registration); registrationsById.put(id, registration);
    }

    public synchronized void registerAll(RegisterPayloadHandlersEvent event) {
        if (sealed) throw new IllegalStateException("Payloads already registered for " + channel);
        sealed = true;
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        IPayloadHandler<DataPacketPayload> handler = this::handleIncoming;
        registrar.playBidirectional(payloadType, streamCodec, handler, handler);
        for (PacketRegistration<?> registration : registrations.values())
            registrar.playBidirectional(registration.type(), registration.codec(), handler, handler);
    }

    StreamCodec<FriendlyByteBuf, DataPacketPayload> codec(Identifier id) {
        if (id.equals(payloadId)) return streamCodec;
        PacketRegistration<?> registration = registrationsById.get(id);
        if (registration == null) throw new IllegalArgumentException("Unknown payload: " + id);
        return registration.codec();
    }

    PacketObj decode(DataPacketPayload payload) {
        if (payload.data().length > PacketValidation.MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("Payload too large");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
        try {
            PacketObj packet;
            if (payload.type().id().equals(payloadId)) packet = DataPacket.decode(buf, packetHandler);
            else {
                PacketRegistration<?> registration = registrationsById.get(payload.type().id());
                if (registration == null) throw new IllegalArgumentException("Unknown payload: " + payload.type().id());
                packet = registration.decode(buf);
                packet.bindHandler(packetHandler);
            }
            if (buf.isReadable()) throw new IllegalArgumentException("Trailing packet data");
            return packet;
        } finally { buf.release(); }
    }

    private void handleIncoming(DataPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                PacketObj packet = decode(payload);
                Player player = context.player();
                if (player == null) {
                    // Shouldn't happen in the PLAY phase, but be defensive: the legacy helpers all
                    // dereference player.level() unconditionally and we don't want to NPE the netty
                    // thread.
                    DragonAPI.LOGGER.warn("Dropping incoming packet on channel {} — context.player() is null", channel);
                    return;
                }
                Level level = player.level();
                if (context.flow() == PacketFlow.SERVERBOUND && packet.getType() != null && packet.getType().isClientboundOnly()) return;
                PacketPipeline.handleWithReplyContext(payloadId, context, () -> packetHandler.handleData(packet, level, player));
            } catch (Exception e) {
                DragonAPI.LOGGER.debug("Rejected packet on channel {}: {}", channel, e.toString());
            }
        });
    }

    public CustomPacketPayload toPayload(String modId, PacketObj p) {
        if (!this.modId.equals(modId)) throw new IllegalArgumentException("Packet namespace does not own this channel");
        return toPayload(p);
    }

    CustomPacketPayload toPayload(PacketObj p) {
        Objects.requireNonNull(p, "Packet");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            if (p.handler != packetHandler) throw new IllegalArgumentException("Packet belongs to another channel");
            PacketRegistration<?> registration = registrations.get(p.getClass());
            if (registration == null) p.encode(buf); else registration.encode(p, buf);
            if (buf.readableBytes() > PacketValidation.MAX_PAYLOAD_BYTES)
                throw new IllegalArgumentException("Payload too large");
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new DataPacketPayload(registration != null ? registration.type() : payloadType, bytes);
        } finally {
            buf.release();
        }
    }

    /**
     * Carries the encoded packet bytes. {@code type()} returns the per-mod registered type so
     * NeoForge routes the payload to the right channel's handler. The wire format only contains
     * the raw byte array — the previous implementation also serialised the {@link Identifier},
     * which was redundant (NeoForge already routes by type) and wasted bandwidth.
     */
    public record DataPacketPayload(CustomPacketPayload.Type<DataPacketPayload> type, byte[] data) implements CustomPacketPayload {
        @Override
        public Type<DataPacketPayload> type() {
            return type;
        }
    }
}
