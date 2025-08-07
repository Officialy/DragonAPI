package reika.dragonapi.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterClientPayloadHandlersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

public record SyncPayload(String modId, BlockPos pos, int beTypeId, CompoundTag changes) implements CustomPacketPayload {
    public static final Type<SyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dragonapi", "sync"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, (SyncPayload p) -> p.pos().getX(),
            ByteBufCodecs.VAR_INT, (SyncPayload p) -> p.pos().getY(),
            ByteBufCodecs.VAR_INT, (SyncPayload p) -> p.pos().getZ(),
            ByteBufCodecs.VAR_INT, SyncPayload::beTypeId,
            ByteBufCodecs.COMPOUND_TAG, SyncPayload::changes,
            (x, y, z, id, nbt) -> new SyncPayload("dragonapi", new BlockPos(x, y, z), id, nbt)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent.PayloadRegistrar registrar, String modId) {
        registrar.playBidirectional(TYPE, STREAM_CODEC, (payload, ctx) -> {
            ctx.workHandler().enqueue(() -> {
                var lvl = ctx.level().orElse(null);
                if (lvl != null) {
                    BlockEntity te = lvl.getBlockEntity(payload.pos);
                    if (te != null) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.merge(payload.changes);
                        te.load(nbt);
                    }
                }
            });
        });
    }

    public static void registerClient(RegisterClientPayloadHandlersEvent event) {
        event.register(TYPE, (payload, ctx) -> {
            ctx.workHandler().enqueue(() -> {
                var mc = Minecraft.getInstance();
                if (mc.level != null) {
                    BlockEntity te = mc.level.getBlockEntity(payload.pos);
                    if (te != null) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.merge(payload.changes);
                        te.load(nbt);
                    }
                }
            });
        });
    }
}



