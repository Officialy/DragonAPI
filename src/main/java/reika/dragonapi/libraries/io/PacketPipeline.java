package reika.dragonapi.libraries.io;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PacketPipeline {

    private ArrayList<Class<? extends PacketObj>> packets = new ArrayList<>();
    private boolean isPostInitialized = false;
    private final DragonAPIMod mod;
    public final String packetChannel;
    private final PacketHandler handler;
    private final SimpleChannel wrapper;

    public PacketPipeline(DragonAPIMod mod, String modChannel, PacketHandler handler, SimpleChannel wrapper) {
        packetChannel = modChannel;
        this.mod = mod;
        this.handler = handler;
        this.wrapper = wrapper;
    }

    public void registerPacket(Class<? extends PacketObj> cl) {
        int id = packets.size();

        wrapper.messageBuilder(cl, id).encoder(PacketObj::readData).encoder(PacketObj::encode).consumerNetworkThread(PacketObj::handleClient).add(); //todo check if these should be mainThread or networkThread
        wrapper.messageBuilder(cl, id).encoder(PacketObj::readData).encoder(PacketObj::encode).consumerNetworkThread(PacketObj::handleServer).add();

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
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
//        channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.ALL.noArg(), p);
    }

    public Packet<?> getMinecraftPacket(PacketObj p) {
//todo used to be       return wrapper.getPacketFrom(p);
        return wrapper.toVanillaPacket(p, NetworkDirection.PLAY_TO_SERVER); // TODO MAX THIS MIGHT NOT WORK PLS CHECK
    }

    public void sendToPlayer(PacketObj p, ServerPlayer player) {
        if (player == null)
            throw new MisuseException("You cannot send a packet to a null player!");
        if (ReikaPlayerAPI.isFake(player))
            throw new MisuseException("You cannot send a packet to a fake player!");
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        //channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.PLAYER.with(() -> player), p);
    }

    public void sendToAllAround(PacketObj p, BlockEntity te, double range) {
        this.sendToAllAround(p, new WorldLocation(te), range);
    }

    public void sendToAllAround(PacketObj p, Level world, double x, double y, double z, double range) {
        this.sendToAllAround(p, world.dimension(), x, y, z, range);
    }

    public void sendToAllAround(PacketObj p, ResourceKey<Level> world, double x, double y, double z, double range) {
        PacketDistributor.TargetPoint pt = new PacketDistributor.TargetPoint(x, y, z, range, world);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(pt);
//        channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.NEAR.with(() -> pt), p);
    }

    public void sendToAllAround(PacketObj p, Entity e, double range) {
        PacketDistributor.TargetPoint pt = new PacketDistributor.TargetPoint(e.getX(), e.getY(), e.getZ(), range, e.getLevel().dimension());
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(pt);
        //channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.NEAR.with(() -> pt), p);
    }

    public void sendToAllAround(PacketObj p, WorldLocation loc, double range) {
        PacketDistributor.TargetPoint pt = new PacketDistributor.TargetPoint(loc.pos.getX(), loc.pos.getY(), loc.pos.getZ(), range, loc.getDimension());
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(pt);
        //channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.NEAR.with(() -> pt), p);
    }

    public void sendToDimension(PacketObj p, Level world) {
        this.sendToDimension(p, world.dimension());
    }

    public void sendToDimension(PacketObj p, ResourceKey<Level> dimensionId) {
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        //channels.get(Dist.DEDICATED_SERVER).attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimensionId);
        //channels.get(Dist.DEDICATED_SERVER).writeAndFlush(p);
        wrapper.send(PacketDistributor.DIMENSION.with(() -> dimensionId), p);
    }

    public void sendToServer(PacketObj p) {
//        channels.get(Dist.CLIENT).attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
//        channels.get(Dist.CLIENT).writeAndFlush(p);
        wrapper.sendToServer(p);
    }

    public static class InternalHandler {

        public InternalHandler() {

        }

        public boolean handle(PacketObj message, Supplier<NetworkEvent.Context> ctx) {
            final var success = new AtomicBoolean(false);
            switch (ctx.get().getDirection()) {
                case PLAY_TO_CLIENT -> message.handleClient(ctx);
                case PLAY_TO_SERVER -> message.handleServer(ctx);
            }
            ctx.get().setPacketHandled(true);
            return success.get(); //return a packetObj if sending a reply
        }

    }
}
