package reika.dragonapi.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.registry.ReikaParticleHelper;

import java.util.UUID;

/**
 * All DragonAPI packet payloads as strongly-typed records.
 * Each payload replaces a specific packet type from the old system.
 */
public final class DragonPayloads {
    
    private DragonPayloads() {}
    
    // ==================== PLAYER DATA SYNC ====================
    
    public record PlayerDataSyncPayload(UUID playerId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<PlayerDataSyncPayload> TYPE = new Type<>(DragonNetwork.id("player_data_sync"));
        public static final StreamCodec<ByteBuf, PlayerDataSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlayerDataSyncPayload::playerId,
            ByteBufCodecs.COMPOUND_TAG, PlayerDataSyncPayload::data,
            PlayerDataSyncPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(PlayerDataSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                // Handle player data sync on client
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    // Client-side handling
                    DragonAPI.LOGGER.debug("Received player data sync for {}", payload.playerId);
                }
            });
        }
    }
    
    // ==================== BLOCK ENTITY SYNC ====================
    
    public record BlockEntitySyncPayload(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
        public static final Type<BlockEntitySyncPayload> TYPE = new Type<>(DragonNetwork.id("block_entity_sync"));
        public static final StreamCodec<ByteBuf, BlockEntitySyncPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BlockEntitySyncPayload::pos,
            ByteBufCodecs.COMPOUND_TAG, BlockEntitySyncPayload::data,
            BlockEntitySyncPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(BlockEntitySyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    // Apply block entity data on client
                    DragonAPI.LOGGER.debug("Received block entity sync for {}", payload.pos);
                }
            });
        }
    }
    
    // ==================== SOUND PLAY ====================
    
    public record SoundPlayPayload(Identifier soundId, SoundSource source, double x, double y, double z, 
                                 float volume, float pitch, boolean relative) implements CustomPacketPayload {
        public static final Type<SoundPlayPayload> TYPE = new Type<>(DragonNetwork.id("sound_play"));
        public static final StreamCodec<ByteBuf, SoundPlayPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SoundPlayPayload::soundId,
            ByteBufCodecs.idMapper(i -> SoundSource.values()[i], SoundSource::ordinal), SoundPlayPayload::source,
            ByteBufCodecs.DOUBLE, SoundPlayPayload::x,
            ByteBufCodecs.DOUBLE, SoundPlayPayload::y,
            ByteBufCodecs.DOUBLE, SoundPlayPayload::z,
            ByteBufCodecs.FLOAT, SoundPlayPayload::volume,
            ByteBufCodecs.FLOAT, SoundPlayPayload::pitch,
            ByteBufCodecs.BOOL, SoundPlayPayload::relative,
            SoundPlayPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(SoundPlayPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    var soundOptional = BuiltInRegistries.SOUND_EVENT.get(payload.soundId);
                    if (soundOptional != null && soundOptional.isPresent()) {
                        Vec3 pos = new Vec3(payload.x, payload.y, payload.z);
                        Minecraft.getInstance().level.playLocalSound(pos.x, pos.y, pos.z, soundOptional.get().value(), payload.source, payload.volume, payload.pitch, false);
                    }
                }
            });
        }
    }
    
    // ==================== PARTICLE SPAWN ====================
    
    public record ParticleSpawnPayload(Identifier particleType, double x, double y, double z, 
                                     double vx, double vy, double vz, int count) implements CustomPacketPayload {
        public static final Type<ParticleSpawnPayload> TYPE = new Type<>(DragonNetwork.id("particle_spawn"));
        public static final StreamCodec<ByteBuf, ParticleSpawnPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ParticleSpawnPayload::particleType,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::x,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::y,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::z,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::vx,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::vy,
            ByteBufCodecs.DOUBLE, ParticleSpawnPayload::vz,
            ByteBufCodecs.INT, ParticleSpawnPayload::count,
            ParticleSpawnPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(ParticleSpawnPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    var type = BuiltInRegistries.PARTICLE_TYPE.get(payload.particleType);
                    if (type != null && type.isPresent()) {
                        Vec3 pos = new Vec3(payload.x, payload.y, payload.z);
                        Vec3 vel = new Vec3(payload.vx, payload.vy, payload.vz);
                        Minecraft.getInstance().level.addParticle((ParticleOptions)type.get().value(), pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
                    }
                }
            });
        }
    }
    
    // ==================== CONFIG SYNC ====================
    
    public record ConfigSyncPayload(CompoundTag configData) implements CustomPacketPayload {
        public static final Type<ConfigSyncPayload> TYPE = new Type<>(DragonNetwork.id("config_sync"));
        public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, ConfigSyncPayload::configData,
            ConfigSyncPayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(ConfigSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    // Apply config sync on client
                    DragonAPI.LOGGER.debug("Received config sync");
                }
            });
        }
    }
    
    // ==================== CHAT CLEAR ====================
    
    public record ChatClearPayload() implements CustomPacketPayload {
        public static final Type<ChatClearPayload> TYPE = new Type<>(DragonNetwork.id("chat_clear"));
        public static final StreamCodec<ByteBuf, ChatClearPayload> STREAM_CODEC = StreamCodec.unit(new ChatClearPayload());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(ChatClearPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    ReikaChatHelper.clearChat();
                }
            });
        }
    }
    
    // ==================== POPUP MESSAGE ====================
    
    public record PopupMessagePayload(String message, int duration) implements CustomPacketPayload {
        public static final Type<PopupMessagePayload> TYPE = new Type<>(DragonNetwork.id("popup_message"));
        public static final StreamCodec<ByteBuf, PopupMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PopupMessagePayload::message,
            ByteBufCodecs.INT, PopupMessagePayload::duration,
            PopupMessagePayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
        
        public static void handle(PopupMessagePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    // Show popup message on client
                    DragonAPI.LOGGER.debug("Received popup message: {}", payload.message);
                }
            });
        }
    }
} 


