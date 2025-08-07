package reika.dragonapi.libraries.io;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketPipeline {

    private final ArrayList<Class<? extends PacketObj>> packets = new ArrayList<>();
    private final boolean isPostInitialized = false;
    private final DragonAPIMod mod;
    public final String packetChannel;
    private final PacketHandler handler;
    private final String modId;

    public PacketPipeline(DragonAPIMod mod, String modChannel, PacketHandler handler) {
        packetChannel = modChannel;
        this.mod = mod;
        this.handler = handler;
        this.modId = mod.getModId();
    }

    public <MSG extends PacketObj> void registerPacket(Class<MSG> cl, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        // No-op in payload system; kept for API compat
        packets.add(cl);
    }

    public PacketHandler getHandler() {
        return handler;
    }

    private Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

	public void replyToPacket(PacketObj p) {
//		channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
//		channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
	}

    public void sendToAllOnServer(PacketObj p) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public Packet<?> getMinecraftPacket(PacketObj p) {
        // Not supported with payload system; return null
        return null;
    }

    public void sendToPlayer(PacketObj p, ServerPlayer player) {
        if (player == null)
            throw new MisuseException("You cannot send a packet to a null player!");
        if (ReikaPlayerAPI.isFake(player))
            throw new MisuseException("You cannot send a packet to a fake player!");
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToPlayer(player, payload);
    }

    public void sendToAllAround(PacketObj p, BlockEntity te, double range) {
        this.sendToAllAround(p, new WorldLocation(te), range);
    }

    public void sendToAllAround(PacketObj p, Level world, double x, double y, double z, double range) {
        this.sendToAllAround(p, world.dimension(), x, y, z, range);
    }

    public void sendToAllAround(PacketObj p, ResourceKey<Level> world, double x, double y, double z, double range) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public void sendToAllAround(PacketObj p, Entity e, double range) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public void sendToAllAround(PacketObj p, WorldLocation loc, double range) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public void sendToDimension(PacketObj p, Level world) {
        this.sendToDimension(p, world.dimension());
    }

    public void sendToDimension(PacketObj p, ResourceKey<Level> dimensionId) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public void sendToServer(PacketObj p) {
        CustomPacketPayload payload = ReikaPacketHelper.toPayload(modId, p);
        ReikaPacketHelper.INSTANCE.sendToServer(payload);
    }

}

