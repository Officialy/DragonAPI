package reika.dragonapi.libraries;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.maps.PlayerMap;
import reika.dragonapi.instantiable.event.GetPlayerLookEvent;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

import java.util.*;

public class ReikaPlayerAPI {

    //private static final HashMap<String, FakePlayer> fakePlayers = new HashMap();
    private static final HashMap<String, UUID> uuidMap = new HashMap<>();
    private static final PlayerMap<SkullBlockEntity> headCache = new PlayerMap<>();
    private static GameProfile clientProfile;

    /**
     * Transfers a player's entire inventory to an inventory. Args: Player, Inventory
     */
    public static void transferInventoryToChest(Player ep, ItemStack[] inv) {
        int num = ReikaInventoryHelper.getTotalUniqueStacks(ep.getInventory().items.toArray(new ItemStack[0]));
        if (num >= inv.length)
            return;
    }

    public static void kickPlayer(ServerPlayer ep, String reason) {
//todo        ep.playerNetServerHandler.kickPlayerFromServer(reason);
    }

    private static boolean isAdmin(ServerLevel world, String name, UUID uuid) {
        FakePlayer fp = getFakePlayerByNameAndUUID(world, name, uuid);
        return isAdmin(fp);
    }

    public static boolean isAdmin(ServerPlayer ep) {
        return ep.getServer().getPlayerList().isOp(ep.getGameProfile());
    }


    public static void kickPlayerClientside(Player ep, String reason) {
        ReikaPacketHelper.sendStringPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.PLAYERKICK.ordinal(), reason, PacketTarget.server);
    }

    public static void syncCustomData(ServerPlayer ep) {
        ReikaPacketHelper.sendNBTPacket(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.PLAYERDATSYNC.ordinal(), ep.serializeNBT(), new PacketTarget.PlayerTarget(ep));
    }

    public static boolean playerCanBreakAt(ServerLevel world, BlockArray b, ServerPlayer ep) {
        for (int i = 0; i < b.getSize(); i++) {
            BlockPos c = b.getNthBlock(i);
            if (!playerCanBreakAt(world, c, ep))
                return false;
        }
        return true;
    }

    public static boolean playerCanBreakAt(ServerLevel world, BlockPos pos, ServerPlayer ep) {
        return playerCanBreakAt(world, pos.getX(), pos.getY(), pos.getZ(), world.getBlockState(pos), ep.getName().getString(), ep.getUUID());
    }

    public static boolean playerCanBreakAt(ServerLevel world, BlockPos pos, BlockState id, ServerPlayer ep) {
        return playerCanBreakAt(world, pos.getX(), pos.getY(), pos.getZ(), id, ep.getName().getString(), ep.getUUID());
    }

    public static List<ServerPlayer> getPlayersWithin(Level world, AABB box) {
        ArrayList<ServerPlayer> li = new ArrayList<>();
        for (Object o : world.players()) {
            if (o instanceof ServerPlayer && ((ServerPlayer)o).getBoundingBox().intersects(box)) {
                ServerPlayer ep = (ServerPlayer)o;
                if (ep.getBoundingBox().intersects(box)) {
                    li.add(ep);
                }
            }
        }
        return li;
    }

    public static boolean playerCanBreakAt(ServerLevel world, int x, int y, int z, BlockState id, String name, UUID uuid) {
        if (name == null) {
            DragonAPI.LOGGER.error("Cannot check permissions of a null player!");
            return false;
        }
        if (DragonAPI.isSinglePlayer())
            return true;
        if (isAdmin(world, name, uuid) && DragonOptions.ADMINPERMBYPASS.getState())
            return true;
        FakePlayer fp = getFakePlayerByNameAndUUID(world, name, uuid);
//        if (MinecraftServer.getServer().isBlockProtected(world, x, y, z, fp))
//            return false;
        BlockEvent.BreakEvent evt = new BlockEvent.BreakEvent(world, new BlockPos(x, y, z), id, fp);
        MinecraftForge.EVENT_BUS.post(evt);
        return !evt.isCanceled();
    }

    public static boolean isReika(Player ep) {
        return ep.getUUID().equals(DragonAPI.Reika_UUID);
    }
    public static Entity getLookedAtEntity(Player ep, double reach, double boxSize) {
        Vec3 vec = new Vec3(ep.getX(), (ep.getY() + 1.62) - ep.yo, ep.getZ()); //yOffset
        Vec3 vec2 = ep.getLookAngle();
        double s = boxSize;
        for (double d = 0; d <= reach; d += boxSize * 2) {
            double x = vec.x + d * vec2.x;
            double y = vec.y + d * vec2.y;
            double z = vec.z + d * vec2.z;
            AABB box = new AABB(x - s, y - s, z - s, x + s, y + s, z + s);
            List<Entity> li = ep.level.getEntities(ep, box);
            if (!li.isEmpty())
                return li.get(0);
        }
        return null;
    }

    public static BlockHitResult getLookedAtBlock(Player ep, double reach, boolean liq) {
        Vec3 vec = ep.getEyePosition();//new Vec3(ep.getX(), (ep.getY() + 1.62) - ep.yo, ep.getZ());
        Vec3 vec2 = ep.getViewVector(1F); //1F
        Vec3 vec3 = vec.add(vec2.x() * reach, vec2.y() * reach, vec2.z() * reach);
        BlockHitResult hit = ep.level.clip(new ClipContext(vec, vec3, ClipContext.Block.COLLIDER, liq ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, ep));

        GetPlayerLookEvent evt = new GetPlayerLookEvent(ep, hit, vec, vec3);
        MinecraftForge.EVENT_BUS.post(evt);
        hit = (BlockHitResult) evt.newLook;

        if (hit != null && hit.getType() == BlockHitResult.Type.BLOCK)
            return hit;
        return null;
    }

    public static boolean isFake(Player ep) {
        if (ep instanceof FakePlayer)
            return true;
        if (ep.getName().getString().contains("CoFH") || ep.getName().getString().contains("Thaumcraft"))
            return true;
        String s = ep.getClass().getName().toLowerCase(Locale.ENGLISH);
        return s.contains("fake") || s.contains("dummy");
    }

    public static FakePlayer getFakePlayerByNameAndUUID(ServerLevel world, String name, UUID uuid) {
		/*
		FakePlayer fp = fakePlayers.get(name);
		if (fp == null) {
			fp = FakePlayerFactory.get(world, new GameProfile(uuid, name));
			fakePlayers.put(name, fp);
		}
		return fp;
		 */
        return FakePlayerFactory.get(world, new GameProfile(uuid, name));
    }

    public static BlockHitResult getLookedAtBlockClient(double reach, boolean liq) {
        Player ep = Minecraft.getInstance().player;
        return getLookedAtBlock(ep, reach, liq);
    }

    public static CompoundTag getDeathPersistentNBT(Player ep) {
        CompoundTag nbt = ep.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ep.getPersistentData().put(Player.PERSISTED_NBT_TAG, nbt);
        return nbt;
    }

}
