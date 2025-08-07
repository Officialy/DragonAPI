package reika.dragonapi.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

public record RawBytesPayload(String modId, ReikaPacketHelper.PacketObj source) implements CustomPacketPayload {
    public static final Type<RawBytesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dragonapi", "raw"));
    public static final StreamCodec<ByteBuf, RawBytesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                // Encode minimal: handler id, type, len, bytes
                buf.writeShort(ReikaPacketHelper.getHandlerIdFor(msg.source));
                buf.writeByte(msg.source.getType().ordinal());
                if (msg.source instanceof ReikaPacketHelper.DataPacket dp) {
                    buf.writeInt(dp.getSize());
                    buf.writeBytes(dp.getBytes());
                } else {
                    buf.writeInt(0);
                }
            },
            buf -> {
                FriendlyByteBuf fbb = new FriendlyByteBuf(buf);
                ReikaPacketHelper.DataPacket dp = ReikaPacketHelper.DataPacket.decode(fbb);
                if (dp == null) dp = new ReikaPacketHelper.DataPacket(new byte[0]);
                return new RawBytesPayload("dragonapi", dp);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}


