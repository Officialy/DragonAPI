/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.ModList;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.extras.BlockFlags;
import reika.dragonapi.instantiable.BlockUpdateCallback;
import reika.dragonapi.instantiable.HybridTank;
import reika.dragonapi.instantiable.RedstoneTracker;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.data.maps.TimerMap;
import reika.dragonapi.instantiable.io.SyncPacket;
import reika.dragonapi.interfaces.DataSync;
import reika.dragonapi.interfaces.registry.MenuFactory;
import reika.dragonapi.io.CompoundSyncPacket;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaReflectionHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.lang.reflect.Field;
import java.util.*;

public abstract class BlockEntityBase extends BlockEntity implements IForgeBlockEntity, CompoundSyncPacket.CompoundSyncPacketHandler {

    protected static final Random rand = new Random();
    protected final Direction[] dirs = Direction.values();
    private final StepTimer updateTimer;
    //    private final StepTimer packetTimer;
    private final StepTimer fullSyncTimer;
    private final TimerMap<TimerMap.TimerCallback> callbacks = new TimerMap<>();
    private final BlockEntity[] adjTEMap = new BlockEntity[6];
    private boolean forceSync = true;
    private long lastTickCall = -1;
    private boolean isNaturalTick = true;
    protected boolean shutDown;
    protected String placer;
    protected UUID placerUUID;
    protected boolean fakePlaced;
    private int ticksExisted;

    private FakePlayer fakePlayer;
    private long tileAge = 0;
    private boolean lastRedstone;
    private boolean redstoneInput;

    private final RedstoneTracker comparatorTracker = new RedstoneTracker();
    private final SyncPacket syncTag;

    /**
     * For mapmakers
     */
    private boolean unharvestable = false;
    private boolean unmineable = false;

    public BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        syncTag = new SyncPacket(this);
        updateTimer = new StepTimer(this.getBlockUpdateDelay());
        updateTimer.randomizeTick(rand);
//        packetTimer = new StepTimer(this.getPacketDelay());
        fullSyncTimer = new StepTimer(1200);
        fullSyncTimer.randomizeTick(rand);
    }

    public static boolean isStandard8mReach(Player ep, BlockEntity te) {
        double dist = ReikaMathLibrary.py3d(te.getBlockPos().getX() + 0.5 - ep.getX(), te.getBlockPos().getY() + 0.5 - ep.getY(), te.getBlockPos().getZ() + 0.5 - ep.getZ());
        return (dist <= 8);
    }

    public abstract Block getBlockEntityBlockID();

    public abstract void updateEntity(Level world, BlockPos pos);

    protected abstract void animateWithTick(Level world, BlockPos pos);

    public abstract int getRedstoneOverride();

    public final boolean hasRedstoneSignal() {
        return redstoneInput;
    }

    public final void onBlockUpdate() {
        lastRedstone = redstoneInput;
        redstoneInput = level.hasNeighborSignal(worldPosition);
        if (redstoneInput && !lastRedstone)
            this.onPositiveRedstoneEdge();
        if (redstoneInput != lastRedstone) {
            ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.REDSTONECHANGE.ordinal(), this, 32, redstoneInput ? 1 : 0, lastRedstone ? 1 : 0);
            this.syncAllData(false);
        }
        this.onAdjacentBlockUpdate();
    }

    protected void onAdjacentBlockUpdate() {

    }

    @OnlyIn(Dist.CLIENT)
    public final void onRedstoneChangedClientside(boolean redstone, boolean last) {
        if (redstone && !last)
            this.onPositiveRedstoneEdge();
    }

    protected void onPositiveRedstoneEdge() {

    }

    public final boolean isChunkLoadedOnSide(Direction dir) {
        return level.hasChunksAt(worldPosition.getX() + dir.getStepX(), worldPosition.getY() + dir.getStepY(), worldPosition.getZ() + dir.getStepZ(), worldPosition.getX() + dir.getStepX(), worldPosition.getY() + dir.getStepY(), worldPosition.getZ() + dir.getStepZ());
    }

    public boolean allowTickAcceleration() {
        return true;
    }

    public final int getTicksExisted() {
        return ticksExisted;
    }

    /**
     * Persistent across world saves, unlike getTicksExisted()
     */
    public final long getBlockEntityAge() {
        return tileAge;
    }


//    public int getPacketDelay() {
//        return DragonOptions.COMMON.SLOWSYNC.getState() ? 20 : 5;
//    }


    public void animateItem() {
        if (level == null) {
            this.animateWithTick(null, getBlockPos());
        }
    }

    public final boolean isPlacer(Player ep) {
        if (placer == null || placerUUID == null || placer.isEmpty())
            return false;
        return ep.getCommandSenderWorld().equals(placer) && ep.getUUID().equals(placerUUID);
    }

    public final Block getTEBlock() {
        Block id = this.getBlockEntityBlockID();
        if (id == Blocks.AIR)
            DragonAPI.LOGGER.error("BlockEntity " + this + " tried to register ID 0!");
        if (id == null) {
            DragonAPI.LOGGER.error(id + " is an invalid block ID for " + this + "!");
            return null;
        }
        return id;
    }

    public boolean isPlayerAccessible(Player var1) {
        double dist = ReikaMathLibrary.py3d(getBlockPos().getX() + 0.5 - var1.getX(), getBlockPos().getY() + 0.5 - var1.getY(), getBlockPos().getZ() + 0.5 - var1.getZ());
        return (dist <= 8) && level.getBlockEntity(getBlockPos()) == this;
    }

    /**
     * Can be called from the client to request a sync from the server
     */
    public final void syncAllData(boolean fullNBT) {
        if (level.isClientSide) {
            ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.TILESYNC.ordinal(), this, 512, fullNBT ? 1 : 0);
        } else {
            level.markAndNotifyBlock(worldPosition, level.getChunkAt(worldPosition), level.getBlockState(worldPosition), level.getBlockState(worldPosition), BlockFlags.BLOCK_UPDATE, 512); //todo check
            CompoundTag var1 = new CompoundTag();
            if (fullNBT)
                this.saveAdditional(var1);
            this.writeSyncTag(var1);
            if (fullNBT)
                var1.putBoolean("fullData", true);
            ClientboundBlockEntityDataPacket p = ClientboundBlockEntityDataPacket.create(this, (blockEntity) -> var1);
            int r = this.getUpdatePacketRadius();
            if (r < 0 || r == Integer.MAX_VALUE) {
                this.sendPacketToAllAround(p, r);
            } else {
                this.sendPacketToAllAround(p, r);
            }

            this.onDataSync(fullNBT);
        }
        if (level.hasChunksAt(worldPosition, worldPosition))
            this.setChanged();
    }

    private void sendPacketToAllAround(ClientboundBlockEntityDataPacket p, int r) {
        if (!level.isClientSide()) {
            AABB box = ReikaAABBHelper.getBlockAABB(worldPosition).expandTowards(r, r, r);
            List<ServerPlayer> li = ReikaPlayerAPI.getPlayersWithin(level, box);
            for (ServerPlayer serverPlayer : li) {
                serverPlayer.connection.send(p);
            }
        }
    }

    private void sendPacketToAll(ClientboundBlockEntityDataPacket p) {
        if (!level.isClientSide()) {
            List<ServerPlayer> li = ReikaPlayerAPI.getPlayersWithin(level, INFINITE_EXTENT_AABB);
            for (ServerPlayer serverPlayer : li) {
                serverPlayer.connection.send(p);
            }
        }
    }

    public int getUpdatePacketRadius() {
        return 32;
    }

    private void syncTankData() {
        Collection<Field> c = ReikaReflectionHelper.getFields(this.getClass(), new ReikaReflectionHelper.TypeSelector(HybridTank.class));
    }

    protected void onDataSync(boolean fullNBT) {

    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag nbt = new CompoundTag();
        this.writeSyncTag(nbt);
        this.saveAdditional(nbt);
        nbt.putBoolean("fullData", true);
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity) -> nbt);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag nbt) {
        this.load(nbt);
    }

    private boolean shouldFullSync() {
        return forceSync;
    }

    public void forceFullSync() {
        forceSync = true;
    }

    public final void updateEntity() {
//        DragonAPI.LOGGER.info("Running update entity code for "+this+"!");
        long time = level.getGameTime();
        isNaturalTick = time != lastTickCall;
        if (!isNaturalTick && !this.allowTickAcceleration())
            return;

        lastTickCall = time;

        if (this.shouldRunUpdateCode()) {
            try {
                if (isNaturalTick)
                    this.updateBlockEntity();
//                this.updateEntity(level, worldPosition);
            } catch (IndexOutOfBoundsException | NullPointerException | ClassCastException | ArithmeticException |
                     IllegalArgumentException e) {
                this.writeError(e);
            }
        }

        if (isNaturalTick) {
            if (this.getTicksExisted() < 20 && this.getTicksExisted() % 4 == 0)
                this.syncAllData(true);

            fullSyncTimer.update();
            if (fullSyncTimer.checkCap()) {
                this.forceFullSync();
            }

            if (this.shouldSendSyncPackets()) {
                if (this.shouldSendSyncPacket() || this.shouldFullSync()) {
                    this.sendSyncPacket();
                }
            }

            callbacks.tick();
            ticksExisted++;
            tileAge++;
        }
    }


    private void updateBlockEntity() {
        this.animateWithTick(level, worldPosition);
        if (this.getTicksExisted() == 0) {
            for (int i = 0; i < 6; i++)
                this.updateCache(dirs[i]);
            if (ModList.OPENCOMPUTERS.isLoaded()) {
//         todo       this.initOCNodes();
            }
            this.onFirstTick(level, worldPosition);
            redstoneInput = level.hasNeighborSignal(worldPosition);
        }
        if (!level.isClientSide() && this.getBlockEntityAge() % 8 == 0)
            comparatorTracker.update(this);
    }

    protected boolean shouldRunUpdateCode() {
        return true;
    }

    protected void onFirstTick(Level world, BlockPos pos) {

    }

    @Override
    public final void handleCompoundSyncPacket(CompoundSyncPacket p) {
        if (!p.hasNoData()) {
            CompoundTag NBT = new CompoundTag();
            this.writeSyncTag(NBT); //so unsent fields do not zero out, we sync the current values in
            p.readForSync(this, NBT);
            this.readSyncTag(NBT);
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet instanceof DataSync) {
            DataSync p = (DataSync) packet;
            if (!p.hasNoData()) {
                CompoundTag NBT = new CompoundTag();
                this.writeSyncTag(NBT); //so unsent fields do not zero out, we sync the current values in
                p.readForSync(this, NBT);
                this.readSyncTag(NBT);
            }
        } else {
            this.readSyncTag(packet.getTag());
            if (packet.getTag().getBoolean("fullData")) {
                this.load(packet.getTag());
            }

        }
    }

    protected void onSetPlacer(Player ep) {

    }

    public final String getPlacerName() {
        return placer;
    }

    public final UUID getPlacerID() {
        return placerUUID;
    }

    public final Player getPlacer() {
        if (placer == null || placer.isEmpty())
            return null;
        Player ep = getLevel().getPlayerByUUID(placerUUID);
        return ep != null ? ep : this.getFakePlacer();
    }

    public final void setPlacer(Player ep) {
        placer = ep.getName().getString();
        fakePlaced = ReikaPlayerAPI.isFake(ep);
        if (ep.getGameProfile().getId() != null)
            placerUUID = ep.getGameProfile().getId();
        this.onSetPlacer(ep);
    }

    public final ServerPlayer getServerPlacer() {
        if (getLevel().isClientSide())
            throw new MisuseException("Cannot get the serverside player on the client!");
        Player ep = this.getPlacer();
        if (ep instanceof ServerPlayer)
            return (ServerPlayer) ep;
        else if (!(ReikaPlayerAPI.isFake(ep)))
            throw new MisuseException("Cannot get the serverside player on the client!");
        else
            return null;
    }

    public final Player getFakePlacer() {
        if (placer == null || placer.isEmpty())
            return null;
        if (level.isClientSide())
            return null;
        if (fakePlayer == null)
            fakePlayer = ReikaPlayerAPI.getFakePlayerByNameAndUUID((ServerLevel) level, placer, placerUUID);
        return fakePlayer;
    }

    public final void triggerBlockUpdate() {
        getLevel().blockUpdated(getBlockPos(), this.getBlockState().getBlock()); //todo make sure block updating works
    }

    public final void scheduleBlockUpdate(int ticks) {
        this.scheduleCallback(new BlockUpdateCallback(this), ticks);
    }

    public final void scheduleCallback(TimerMap.TimerCallback c, int delay) {
        callbacks.put(c, delay);
    }


    private boolean shouldSendSyncPacket() {
        return level != null && level.getGameTime() % this.getPacketDelay() == 0;
    }

    public int getPacketDelay() {
        return DragonOptions.SLOWSYNC.getState() ? 20 : 5;
    }

    private void sendSyncPacket() {
        CompoundTag nbt = new CompoundTag();
        this.writeSyncTag(nbt);
        //if (DragonOptions.COMPOUNDSYNC.getState()) {
        //	CompoundSyncPacket.instance.setData(this, this.shouldFullSync(), nbt);
        //}
        //else {
        syncTag.setData(this, this.shouldFullSync(), nbt);
        if (!syncTag.isEmpty()) {
            int r = this.shouldFullSync() ? 128 : this.getUpdatePacketRadius();
            ResourceKey<Level> dim = level.dimension();
            //PacketDispatcher.sendPacketToAllAround(xCoord, yCoord, zCoord, r, dim, syncTag);
            this.sendPacketToAllAround(syncTag, r);
            //DragonAPICore.debug("Packet "+syncTag+" sent from "+this);
        }
        //}
        level.setBlocksDirty(worldPosition, this.getBlockState(), this.getBlockState());
        this.onSync();
        forceSync = false;
    }

    protected void onSync() {

    }

    protected final boolean shouldSendSyncPackets() {
        return !level.isClientSide();
    }

    protected void writeSyncTag(CompoundTag tag) {
        tag.putBoolean("lastredstone", lastRedstone);
        tag.putBoolean("thisredstone", redstoneInput);
    }

    protected void readSyncTag(CompoundTag tag) {
        lastRedstone = tag.getBoolean("lastredstone");
        redstoneInput = tag.getBoolean("thisredstone");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readSyncTag(tag);

        placer = tag.getString("place");
        if (tag.contains("placeUUID"))
            placerUUID = UUID.fromString(tag.getString("placeUUID"));

        unharvestable = tag.getBoolean("no_drops");
        unmineable = tag.getBoolean("no_mine");

        tileAge = tag.getLong("age_ticks");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeSyncTag(tag);

        if (placer != null && !placer.isEmpty())
            tag.putString("place", placer);
        if (placerUUID != null)
            tag.putString("placeUUID", placerUUID.toString());

        tag.putBoolean("no_drops", unharvestable);
        tag.putBoolean("no_mine", unmineable);

        tag.putLong("age_ticks", tileAge);
    }


    public final boolean isUnMineable() {
        return unmineable;
    }

    public final boolean isUnHarvestable() {
        return unharvestable;
    }

    public final void setUnmineable(boolean nomine) {
        unmineable = nomine;
    }

    public final boolean isInWorld() {
        return level != null;
    }

    public void writeError(Throwable e) {
        if (DragonOptions.CHATERRORS.getState()) {
            ReikaChatHelper.write(this + " [" + FMLLoader.getDist() + "] is throwing " + e.getClass() + " on update: " + e.getMessage());
            ReikaChatHelper.write(Arrays.toString(e.getStackTrace()));
            ReikaChatHelper.write("");
        }

        DragonAPI.LOGGER.error(this + " [" + FMLLoader.getDist() + "] is throwing " + e.getClass() + " on update: " + e.getMessage());
        e.printStackTrace();
        DragonAPI.LOGGER.info("");
    }

    public Random getRandom() {
        return rand;
    }

    protected abstract String getTEName();

    /**
     * Do not reference world, pos, etc here, as this is called in the constructor
     */
    public final int getBlockUpdateDelay() {
        return 20;
    }

    protected final void delete() {
        level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 1);
    }

    /*
        @Override

        public AABB getRenderBoundingBox() {
            return ReikaAABBHelper.getBlockAABB(getBlockPos());
        }
    */
    public final BlockEntity getBlockEntity(BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    public final BlockEntity getAdjacentBlockEntity(Direction dir) {
        if (this.cachesTEs()) {
//            DragonAPI.LOGGER.info(this.getCachedTE(dir));
            return this.getCachedTE(dir);
        } else {
            int dx = worldPosition.getX() + dir.getStepX();
            int dy = worldPosition.getY() + dir.getStepY();
            int dz = worldPosition.getZ() + dir.getStepZ();
            if (!ReikaWorldHelper.tileExistsAt(getLevel(), new BlockPos(dx, dy, dz)))
                return null;
            return level.getBlockEntity(new BlockPos(dx, dy, dz));
        }
    }

    private boolean cachesTEs() {
        return this.getBlockType().getBlock() instanceof BlockTEBase;
    }

    public final BlockState getBlockType() {
        //DragonAPI.LOGGER.info(this.blockType);
        if (getBlockState() != null)
            return getBlockState();
        if (this.isInWorld()) {
            setBlockState(level.getBlockState(worldPosition));
        } else {
            setBlockState(this.getBlockEntityBlockID().defaultBlockState());
        }
        return getBlockState();
    }

    public final WorldLocation getAdjacentLocation(Direction dir) {
        return new WorldLocation(level, worldPosition.getX() + dir.getStepX(), worldPosition.getY() + dir.getStepY(), worldPosition.getZ() + dir.getStepZ());
    }

    public final boolean isDirectlyAdjacent(int x, int y, int z) {
        return Math.abs(x - worldPosition.getX()) + Math.abs(y - worldPosition.getY()) + Math.abs(z - worldPosition.getZ()) == 1;
    }

    private BlockEntity getCachedTE(Direction dir) {
        return dir != null ? adjTEMap[dir.ordinal()] : null;
    }

    public final void updateCache(Direction dir) {
        BlockEntity te = level.getBlockEntity(new BlockPos(worldPosition.getX() + dir.getStepX(), worldPosition.getY() + dir.getStepY(), worldPosition.getZ() + dir.getStepZ()));
		/*if (te instanceof SpaceRift) {
			te = ((SpaceRift)te).getBlockEntityFrom(dir);
		}*/
        adjTEMap[dir.ordinal()] = te;
        this.onPlacedNextToThis(te, dir);
    }

    protected void onPlacedNextToThis(BlockEntity te, Direction dir) {

    }

    public final int getObjectID() {
        return System.identityHashCode(this);
    }

    public final String getName() {
        return this.getTEName();
    }

    public void writeMenu(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

}
