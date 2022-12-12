package reika.dragonapi.instantiable.io;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.io.PacketPipeline;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PacketTarget {
    
    public static final PacketTarget server = new ServerTarget();
    public static final PacketTarget allPlayers = new AllPlayersTarget();

    public abstract void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk);

    public static final class PlayerTarget extends PacketTarget {

        private final ServerPlayer player;

        public PlayerTarget(ServerPlayer ep) {
            player = ep;
        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            p.sendToPlayer(pk, player);
        }

    }

    public static final class OtherPlayersTarget extends CompoundPlayerTarget {

        public OtherPlayersTarget(Player ep, double r) {
            super(ep.level.getEntitiesOfClass(ServerPlayer.class, ReikaAABBHelper.getEntityCenteredAABB(ep, r))); //todo check if this works
        }

    }

    public static class CompoundPlayerTarget extends PacketTarget {

        private final Collection<ServerPlayer> player;

        public CompoundPlayerTarget(ServerPlayer... ep) {
            player = ReikaJavaLibrary.makeListFromArray(ep);
        }

        public CompoundPlayerTarget(Collection<ServerPlayer> ep) {
            player = new ArrayList<>(ep);
        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            for (ServerPlayer ep : player)
                p.sendToPlayer(pk, ep);
        }

    }

    public static final class RadiusTarget extends PacketTarget {

        private final ResourceKey<Level> dim;
        private final double x;
        private final double y;
        private final double z;
        private final double radius;

        public RadiusTarget(WorldLocation loc, double r) {
            this(loc.getDimension(), loc.pos.getX(), loc.pos.getY(), loc.pos.getZ(), r);
        }

        public RadiusTarget(Entity e, double r) {
            this(e.level, e.getX(), e.getY(), e.getZ(), r);
        }

        public RadiusTarget(BlockEntity te, double r) {
            this(te.getLevel(), te.getBlockPos().getX()+0.5, te.getBlockPos().getY()+0.5, te.getBlockPos().getZ()+0.5, r);
        }

        public RadiusTarget(Level world, double x, double y, double z, double r) {
            this(world.dimension(), x, y, z, r);
        }

        private RadiusTarget(ResourceKey<Level> world, double x, double y, double z, double r) {
            dim = world;
            this.x = x;
            this.y = y;
            this.z = z;
            radius = r;
        }

        public RadiusTarget(Level world, BlockPos c, int r) {
            this(new WorldLocation(world, c), r);
        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            p.sendToAllAround(pk, dim, x, y, z, radius);
        }
    }

    public static final class DimensionTarget extends PacketTarget {

        private final ResourceKey<Level> dimension;

        public DimensionTarget(ResourceKey<Level> dim) {
            dimension = dim;
        }

        public DimensionTarget(Level world) {
            this(world.dimension());
        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            p.sendToDimension(pk, dimension);
        }
    }

    private static final class AllPlayersTarget extends PacketTarget {

        private AllPlayersTarget() {

        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            p.sendToAllOnServer(pk);
        }
    }

    //@SideOnly(Dist.CLIENT)
    private static final class ServerTarget extends PacketTarget {

        private ServerTarget() {

        }

        @Override
        public void dispatch(PacketPipeline p, ReikaPacketHelper.PacketObj pk) {
            p.sendToServer(pk);
        }
    }
}
