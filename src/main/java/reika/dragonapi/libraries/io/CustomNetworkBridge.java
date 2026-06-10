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

/**
 * Bridges Reika's legacy single-channel-per-mod packet model onto NeoForge 1.21.5's typed
 * payload system.
 * <p>
 * Each mod registers exactly one {@link CustomPacketPayload.Type} (identified by
 * {@code <modid>:<channel>}). The actual packet-type discrimination happens inside the byte
 * stream: the encoded payload carries a handler-id short + a {@link reika.dragonapi.auxiliary.PacketTypes}
 * byte + a varint length + the data bytes. On receive, {@link DataPacket#decode(FriendlyByteBuf)}
 * reads those fields back out and dispatches to the right {@link reika.dragonapi.interfaces.PacketHandler}.
 * <p>
 * Same handler instance is supplied for both server- and client-bound directions; the handler
 * already discriminates by {@code player.level().isClientSide()}.
 */
public class CustomNetworkBridge {

    private final String modId;
    private final String channel;
    private final Identifier payloadId;
    private final CustomPacketPayload.Type<DataPacketPayload> payloadType;
    private final StreamCodec<FriendlyByteBuf, DataPacketPayload> streamCodec;

    public CustomNetworkBridge(String modId, String channel) {
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
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return new DataPacketPayload(this.payloadType, bytes);
                }
        );
    }

    public void registerAll(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(modId).versioned("1.0.0");

        IPayloadHandler<DataPacketPayload> handler = this::handleIncoming;

        // 1.21.5 / NeoForge 26.x: the 4-arg overload registers the same handler for both server- and
        // client-bound directions. The 3-arg variant only registers serverbound and crashes startup
        // with "Some clientbound payloads are missing client-side handlers" when the channel is also
        // used to push to clients (which Reika's helpers do extensively).
        registrar.playBidirectional(payloadType, streamCodec, handler, handler);
    }

    private void handleIncoming(DataPacketPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
                DataPacket packet = DataPacket.decode(buf);
                if (packet == null) {
                    // DataPacket.decode already logged a one-shot WARN with the bad id; suppress
                    // the per-packet error to avoid filling the log with one line per dropped packet.
                    return;
                }
                Player player = context.player();
                if (player == null) {
                    // Shouldn't happen in the PLAY phase, but be defensive: the legacy helpers all
                    // dereference player.level() unconditionally and we don't want to NPE the netty
                    // thread.
                    DragonAPI.LOGGER.warn("Dropping incoming packet on channel {} — context.player() is null", channel);
                    return;
                }
                Level level = player.level();
                packet.handler.handleData(packet, level, player);
            } catch (Exception e) {
                DragonAPI.LOGGER.error("Error handling packet on channel {}", channel, e);
            }
        });
    }

    public CustomPacketPayload toPayload(String modId, PacketObj p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        p.encode(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new DataPacketPayload(payloadType, bytes);
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
