package reika.dragonapi.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public record SyncPayload(String modId, BlockPos pos, int beTypeId, CompoundTag changes) implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncPayload.class);
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

    public static void register(PayloadRegistrar registrar, String modId) {
        registrar.playBidirectional(TYPE, STREAM_CODEC, (payload, ctx) -> {
            ctx.enqueueWork(() -> {
                var lvl = ctx.player().level();
                if (lvl != null) {
                    BlockEntity te = lvl.getBlockEntity(payload.pos);
                    if (te != null) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.merge(payload.changes);
                        HolderLookup.Provider registryAccess = lvl.registryAccess();
                        try (var scopedCollector = new ProblemReporter.ScopedCollector(te.problemPath(), LOGGER)) {
                            te.loadWithComponents(TagValueInput.create(scopedCollector, registryAccess, nbt));
                        }
                    }
                }
            });
        });
    }

    public static void registerClient(RegisterClientPayloadHandlersEvent event) {
        event.register(TYPE, (payload, ctx) -> {
            ctx.enqueueWork(() -> {
                var lvl = ctx.player().level();
                if (lvl != null) {
                    BlockEntity te = lvl.getBlockEntity(payload.pos);
                    if (te != null) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.merge(payload.changes);
                        HolderLookup.Provider registryAccess = lvl.registryAccess();
                        try (var scopedCollector = new ProblemReporter.ScopedCollector(te.problemPath(), LOGGER)) {
                            te.loadWithComponents(TagValueInput.create(scopedCollector, registryAccess, nbt));
                        }
                    }
                }
            });
        });
    }
}



