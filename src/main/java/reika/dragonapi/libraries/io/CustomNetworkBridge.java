package reika.dragonapi.libraries.io;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.DataPacket;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;

public class CustomNetworkBridge {

    private final String modId;
    private final String channel;
    private final Map<ResourceLocation, CustomPacketPayload.Type<CustomPacketPayload>> payloadTypes = new HashMap<>();

    public CustomNetworkBridge(String modId, String channel) {
        this.modId = modId;
        this.channel = channel;
    }

    public void registerAll(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(modId)
                .versioned("1.0.0"); // You might want to make this dynamic

        // Register the main DataPacket payload
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, channel.toLowerCase());
        CustomPacketPayload.Type<DataPacketPayload> type = new CustomPacketPayload.Type<>(id);
        
        registrar.playBidirectional(
                type,
                DataPacketPayload.STREAM_CODEC,
                (payload, context) -> {
                    // Handle packet on main thread
                    context.enqueueWork(() -> {
                        try {
                            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
                            DataPacket packet = DataPacket.decode(buf);
                            
                            if (packet != null) {
                                Player player = context.player();
                                Level level = player.level();
                                packet.handler.handleData(packet, level, player);
                            } else {
                                DragonAPI.LOGGER.error("Failed to decode packet on channel " + channel);
                            }
                        } catch (Exception e) {
                            DragonAPI.LOGGER.error("Error handling packet on channel " + channel, e);
                        }
                    });
                }
        );
    }

    public CustomPacketPayload toPayload(String modId, PacketObj p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        p.encode(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new DataPacketPayload(ResourceLocation.fromNamespaceAndPath(modId, channel.toLowerCase()), bytes);
    }

    public record DataPacketPayload(ResourceLocation id, byte[] data) implements CustomPacketPayload {
        
        public static final StreamCodec<FriendlyByteBuf, DataPacketPayload> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                DataPacketPayload::id,
                StreamCodec.of((buf, val) -> {
                    buf.writeVarInt(val.length);
                    buf.writeBytes(val);
                }, buf -> {
                    int len = buf.readVarInt();
                    byte[] bytes = new byte[len];
                    buf.readBytes(bytes);
                    return bytes;
                }),
                DataPacketPayload::data,
                DataPacketPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return new Type<>(id);
        }
    }
}
