package reika.dragonapi.libraries.io;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.DataPacket;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;

/** A namespaced, channel-bound compatibility transport for the 26.3 payload API. */
public class CustomNetworkBridge {

    private final reika.dragonapi.interfaces.PacketHandler packetHandler;
    private final String modId;
    private final String channel;
    private final Identifier payloadId;
    private final CustomPacketPayload.Type<DataPacketPayload> payloadType;
    private final StreamCodec<FriendlyByteBuf, DataPacketPayload> streamCodec;

    public CustomNetworkBridge(String modId, String channel, reika.dragonapi.interfaces.PacketHandler handler) {
        this.packetHandler = java.util.Objects.requireNonNull(handler);
        this.modId = modId;
        this.channel = channel;
        this.payloadId = Identifier.fromNamespaceAndPath(modId, channel.toLowerCase());
        this.payloadType = new CustomPacketPayload.Type<>(payloadId);
        // Per-bridge codec so decoded payloads carry the right type() reference; a single static
        // codec would have to return payloads with a null type, which works for routing but breaks
        // any caller that inspects payload.type() (e.g. for logging or re-dispatch).
        this.streamCodec = StreamCodec.of(
                (buf, val) -> {
                    buf.writeVarInt(val.data.length);
                    buf.writeBytes(val.data);
                },
                buf -> {
                    int len = buf.readVarInt();
                    if (len < 0 || len > PacketValidation.MAX_PAYLOAD_BYTES || len != buf.readableBytes())
                        throw new IllegalArgumentException("Invalid payload length: " + len);
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new DataPacketPayload(this.payloadType, bytes);
                }
        );
    }

    public void registerAll(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(modId).versioned("2.1.0");

        IPayloadHandler<DataPacketPayload> handler = this::handleIncoming;

        registrar.playBidirectional(payloadType, streamCodec, handler, handler);
    }

    private void handleIncoming(DataPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            try {
                DataPacket packet = DataPacket.decode(buf, packetHandler);
                Player player = context.player();
                if (player == null) {
                    // Shouldn't happen in the PLAY phase, but be defensive: the legacy helpers all
                    // dereference player.level() unconditionally and we don't want to NPE the netty
                    // thread.
                    DragonAPI.LOGGER.warn("Dropping incoming packet on channel {} — context.player() is null", channel);
                    return;
                }
                Level level = player.level();
                if (!level.isClientSide() && packet.getType().isClientboundOnly()) return;
                packetHandler.handleData(packet, level, player);
            } catch (Exception e) {
                DragonAPI.LOGGER.debug("Rejected packet on channel {}: {}", channel, e.toString());
            } finally {
                buf.release();
            }
        });
    }

    public CustomPacketPayload toPayload(String modId, PacketObj p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            if (p.handler != packetHandler) throw new IllegalArgumentException("Packet belongs to another channel");
            p.encode(buf);
            if (buf.readableBytes() > PacketValidation.MAX_PAYLOAD_BYTES)
                throw new IllegalArgumentException("Payload too large");
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new DataPacketPayload(payloadType, bytes);
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
