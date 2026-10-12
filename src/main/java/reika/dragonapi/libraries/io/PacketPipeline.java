package reika.dragonapi.libraries.io;

import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import reika.dragonapi.base.DragonAPIMod;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.interfaces.PacketHandler;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper.PacketObj;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class PacketPipeline {

    public final String packetChannel;
    private final PacketHandler handler;
    private final CustomNetworkBridge bridge;
    private static final ThreadLocal<ReplyContext> REPLY_CONTEXT = new ThreadLocal<>();
    private record ReplyContext(Identifier channel, IPayloadContext context) {}

    public PacketPipeline(DragonAPIMod mod, String modChannel, PacketHandler handler) {
        this(mod.getModId(), modChannel, handler);
    }

    PacketPipeline(String modId, String modChannel, PacketHandler handler) {
        packetChannel = Objects.requireNonNull(modChannel);
        this.handler = Objects.requireNonNull(handler);
        bridge = new CustomNetworkBridge(modId, modChannel, handler);
    }

    public <MSG extends PacketObj> void registerPacket(Class<MSG> cl, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        bridge.registerPacket(cl, encoder, decoder);
    }

    public PacketHandler getHandler() {
        return handler;
    }

    /** Explicit identities remain stable if a custom packet class is renamed. Register during mod initialization. */
    public <MSG extends PacketObj> void registerPacket(Identifier id, Class<MSG> cl,
            BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder) {
        bridge.registerPacket(id, cl, encoder, decoder);
    }

    CustomNetworkBridge bridge() { return bridge; }

    /** Bind only while handling this incoming channel; nested handlers restore their caller's context. */
    static void handleWithReplyContext(Identifier channel, IPayloadContext context, Runnable action) {
        ReplyContext previous = REPLY_CONTEXT.get();
        REPLY_CONTEXT.set(new ReplyContext(channel, Objects.requireNonNull(context)));
        try { action.run(); }
        finally {
            if (previous == null) REPLY_CONTEXT.remove(); else REPLY_CONTEXT.set(previous);
        }
    }

    public void replyToPacket(PacketObj p) {
        ReplyContext current = REPLY_CONTEXT.get();
        if (current == null || !bridge.channelId().equals(current.channel()))
            throw new IllegalStateException("Packet replies require an active handler on channel " + packetChannel);
        replyToPacket(p, current.context());
    }

    /** Reply through an explicit native context, including a deferred native handler. */
    public void replyToPacket(PacketObj p, IPayloadContext context) {
        Objects.requireNonNull(context, "Reply context");
        context.reply(payload(p, context.flow().getOpposite()));
    }

    private CustomPacketPayload payload(PacketObj packet, PacketFlow flow) {
        Objects.requireNonNull(packet, "Packet"); Objects.requireNonNull(flow, "Packet flow");
        if (flow == PacketFlow.SERVERBOUND && packet.getType() != null && packet.getType().isClientboundOnly())
            throw new IllegalArgumentException("Client-only packet cannot be sent to the server: " + packet.getType());
        return bridge.toPayload(packet);
    }

    public void sendToAllOnServer(PacketObj p) {
        CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
                this.sendIfSupported(player, payload);
        }
    }

    public Packet<?> getMinecraftPacket(PacketObj p) {
        ReplyContext current = REPLY_CONTEXT.get();
        if (current != null) return getMinecraftPacket(p, current.context().flow().getOpposite());
        var server = ServerLifecycleHooks.getCurrentServer();
        PacketFlow flow = !FMLEnvironment.getDist().isClient() || (server != null && server.isSameThread())
                ? PacketFlow.CLIENTBOUND : PacketFlow.SERVERBOUND;
        return getMinecraftPacket(p, flow);
    }

    /** Explicit outbound flow is suitable for off-thread packet construction and integrated-server callers. */
    public Packet<?> getMinecraftPacket(PacketObj p, PacketFlow flow) {
        CustomPacketPayload payload = payload(p, flow);
        return flow == PacketFlow.CLIENTBOUND ? new ClientboundCustomPayloadPacket(payload)
                : new ServerboundCustomPayloadPacket(payload);
    }

    public void sendToPlayer(PacketObj p, ServerPlayer player) {
        if (player == null)
            throw new MisuseException("You cannot send a packet to a null player!");
        if (ReikaPlayerAPI.isFake(player))
            throw new MisuseException("You cannot send a packet to a fake player!");
        CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
        this.sendIfSupported(player, payload);
    }

    public void sendToAllAround(PacketObj p, BlockEntity te, double range) {
        this.sendToAllAround(p, new WorldLocation(te), range);
    }

    public void sendToAllAround(PacketObj p, Level world, double x, double y, double z, double range) {
        if (world instanceof ServerLevel serverLevel) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload,
                    player -> player.distanceToSqr(x, y, z) <= range * range);
        }
    }

    public void sendToAllAround(PacketObj p, ResourceKey<Level> world, double x, double y, double z, double range) {
        var server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel serverLevel = server != null ? server.getLevel(world) : null;
        if (serverLevel != null) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload,
                    player -> player.distanceToSqr(x, y, z) <= range * range);
        }
    }

    public void sendToAllAround(PacketObj p, Entity e, double range) {
        if (e.level() instanceof ServerLevel serverLevel) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload,
                    player -> player.distanceToSqr(e.getX(), e.getY(), e.getZ()) <= range * range);
        }
    }

    public void sendToAllAround(PacketObj p, WorldLocation loc, double range) {
        if (loc.getWorld() instanceof ServerLevel serverLevel) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload, player -> player.distanceToSqr(
                    loc.pos.getX(), loc.pos.getY(), loc.pos.getZ()) <= range * range);
        }
    }

    public void sendToDimension(PacketObj p, Level world) {
        if (world instanceof ServerLevel serverLevel) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload, player -> true);
        }
    }

    public void sendToDimension(PacketObj p, ResourceKey<Level> dimensionId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel serverLevel = server != null ? server.getLevel(dimensionId) : null;
        if (serverLevel != null) {
            CustomPacketPayload payload = payload(p, PacketFlow.CLIENTBOUND);
            this.sendToPlayers(serverLevel, payload, player -> true);
        }
    }

    private void sendToPlayers(ServerLevel level, CustomPacketPayload payload,
            Predicate<ServerPlayer> filter) {
        for (ServerPlayer player : level.players()) {
            if (filter.test(player)) this.sendIfSupported(player, payload);
        }
    }

    private void sendIfSupported(ServerPlayer player, CustomPacketPayload payload) {
        // NeoForge rejects custom payloads which the remote connection did not negotiate. This
        // matters for vanilla-compatible/optional channels and for headless GameTest players.
        if (player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    public void sendToServer(PacketObj p) {
        CustomPacketPayload payload = payload(p, PacketFlow.SERVERBOUND);
        ClientPacketDistributor.sendToServer(payload);
    }

}
