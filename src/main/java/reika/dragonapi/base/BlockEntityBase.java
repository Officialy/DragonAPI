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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
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
import reika.dragonapi.io.CompoundSyncPacket;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.NBTCompat;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.java.ReikaReflectionHelper;
import reika.dragonapi.libraries.level.ReikaWorldHelper;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;

import java.lang.reflect.Field;
import java.util.*;

public abstract class BlockEntityBase extends BlockEntity implements CompoundSyncPacket.CompoundSyncPacketHandler {

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
        return ep.getName().getString().equals(placer) && ep.getUUID().equals(placerUUID);
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
        // 26.1 debug logging — track sync frequency to diagnose user-reported lag.
        // String key encodes side + fullNBT so we can grep by category.
        String _dbgTag = "syncAllData." + (level == null ? "noLevel" : (level.isClientSide() ? "client" : "server")) + "." + (fullNBT ? "full" : "delta");
        long _saT0 = System.nanoTime();
        try { Class.forName("reika.rotarycraft.auxiliary.PipeDebugLog").getMethod("event", String.class).invoke(null, _dbgTag); } catch (Throwable ignored) {}
        if (level.isClientSide()) {
            ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel, APIPacketHandler.PacketIDs.TILESYNC.ordinal(), this, 512, fullNBT ? 1 : 0);
        } else {
            // 26.1 PERF FIX: previously called {@code level.markAndNotifyBlock(pos, chunk,
            // state, state, BLOCK_UPDATE, 512)} here with {@code oldState == newState}. That
            // had two costly side effects on every BE sync:
            //   (a) {@code sendBlockUpdated} was queued, shipping a ClientboundBlockUpdatePacket
            //       to every client in range; the client then marks the chunk section dirty and
            //       re-meshes — even though the actual blockstate didn't change.
            //   (b) {@code updateNeighborShapes} was fired on the 6 neighbours with depth=511,
            //       calling each neighbour's {@code updateShape}. For pipes this means
            //       6 × {@code canConnect} → 6 BE lookups per sync, plus a cascade if any
            //       neighbour's state happened to need updating.
            // The user reported per-placement and per-flow-tick lag that scaled with the size
            // of the pipe network — chunk re-meshes were piling up faster than the client
            // could process them. The {@link ClientboundBlockEntityDataPacket} below is the
            // actual sync mechanism (it ships the BE NBT to clients via the bridge → onDataPacket
            // → readSyncTag). Vanilla handles blockstate sync for genuine state changes through
            // its own {@code updateShape} path when a neighbour is placed/broken. So this
            // markAndNotifyBlock call was pure overhead. Drop it. Net effect: BE-internal NBT
            // changes still reach clients via the per-BE data packet, but no spurious chunk
            // re-meshes and no cascading updateShape calls per sync.
            CompoundTag var1 = new CompoundTag();
            if (fullNBT)
                this.saveAdditional(var1);
            this.writeSyncTag(var1);
            if (fullNBT)
                var1.putBoolean("fullData", true);
            ClientboundBlockEntityDataPacket p = ClientboundBlockEntityDataPacket.create(this, (blockEntity, provider) -> var1);
            int r = this.getUpdatePacketRadius();
            this.sendPacketToAllAround(p, r);

            this.onDataSync(fullNBT);
        }
        if (level.hasChunksAt(worldPosition, worldPosition))
            this.setChanged();
        long _saDt = System.nanoTime() - _saT0;
        if (_saDt > 5_000_000L) {
            try { Class.forName("reika.rotarycraft.auxiliary.PipeDebugLog").getMethod("event", String.class).invoke(null, _dbgTag + ".slow_ms_" + (_saDt / 1_000_000L)); } catch (Throwable ignored) {}
        }
    }

    private void sendPacketToAllAround(ClientboundBlockEntityDataPacket p, int r) {
        if (!level.isClientSide()) {
            AABB box = ReikaAABBHelper.getBlockAABB(worldPosition).inflate(r, r, r);
            List<ServerPlayer> li = ReikaPlayerAPI.getPlayersWithin(level, box);
            for (ServerPlayer serverPlayer : li) {
                serverPlayer.connection.send(p);
            }
        }
    }

    private void sendPacketToAll(ClientboundBlockEntityDataPacket p) {
        if (!level.isClientSide()) {
            List<ServerPlayer> li = ReikaPlayerAPI.getPlayersWithin(level, new AABB(-1000000, -1000000, -1000000, 1000000, 1000000, 1000000));
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

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag nbt = new CompoundTag();
        this.writeSyncTag(nbt);
        this.saveAdditional(nbt);
        nbt.putBoolean("fullData", true);
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, provider) -> nbt);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        try {
            if (input instanceof TagValueInput) {
                Field f = TagValueInput.class.getDeclaredField("input");
                f.setAccessible(true);
                CompoundTag tag = (CompoundTag) f.get(input);
                if (tag != null) {
                    this.load(tag);
                }
            } else {
                DragonAPI.LOGGER.error("BlockEntityBase handleUpdateTag called with non-TagValueInput: " + input.getClass());
            }
        } catch (Exception e) {
            DragonAPI.LOGGER.error("Failed to extract CompoundTag from TagValueInput in handleUpdateTag", e);
        }
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
            } catch (IndexOutOfBoundsException | NullPointerException | ClassCastException | ArithmeticException |
                     IllegalArgumentException e) {
                this.writeError(e);
            }
        }

        if (isNaturalTick) {
            // 26.1 fix: previously fired {@code syncAllData(true)} 5× in the first 20 ticks for
            // every BE. With many BEs ticking in range simultaneously this triggered a packet
            // storm — the user reported world-load freezes and per-placement 40s stalls when
            // pipes use the inherited lifecycle. The full-NBT broadcast was always semi-redundant
            // anyway: vanilla {@link BlockEntity#getUpdatePacket} already sends the BE state to
            // clients when the chunk is delivered or the BE is freshly created. For BEs whose
            // state changes during the first 20 ticks (e.g. pipes' connections populating from
            // onFirstTick → recomputeConnections), the {@code BE_NBT_SYNC} periodic flow plus
            // any per-BE sync hook (e.g. piping's state-tracker, reservoir's tank.onContentsChanged)
            // delivers those changes. So we now gate the initial burst behind an overridable
            // {@link #shouldDoInitialFullSync} hook — defaults to true to preserve existing
            // behaviour for engines/machines that haven't been audited yet, false for the
            // high-fanout types (pipes, reservoirs, etc.) that hit this lag.
            if (this.shouldDoInitialFullSync() && this.getTicksExisted() < 20 && this.getTicksExisted() % 4 == 0)
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

    /**
     * Whether this BE should fire the legacy 5× full-NBT sync burst in its first 20 ticks
     * ({@link #syncAllData}(true) at ticks 0, 4, 8, 12, 16). Defaults to {@code true} for
     * compatibility with the broad set of BEs that haven't been audited yet.
     *
     * <p>Override to {@code false} on any BE that:
     * <ul>
     *   <li>has many instances in a contiguous chunk (pipes, cables, fluid ducts) — the burst
     *       multiplied by every instance becomes a per-placement packet storm;</li>
     *   <li>already syncs runtime state via a more targeted mechanism (the periodic
     *       {@code BE_NBT_SYNC} flow, tank-contents-changed hooks, etc.);</li>
     *   <li>has no client-visible state that needs to be delivered eagerly within 1s of
     *       placement (vanilla's {@link BlockEntity#getUpdatePacket} already covers the
     *       new-client-joining-range case).</li>
     * </ul>
     */
    protected boolean shouldDoInitialFullSync() {
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
    public void onDataPacket(Connection net, ValueInput input) {
        try {
            if (input instanceof TagValueInput) {
                Field f = TagValueInput.class.getDeclaredField("input");
                f.setAccessible(true);
                CompoundTag tag = (CompoundTag) f.get(input);
                if (tag != null) {
                    this.readSyncTag(tag);
                    if (tag.getBooleanOr("fullData", false)) {
                        this.loadAdditional(input);
                    }
                }
            }
        } catch (Exception e) {}
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
        if (ep.getUUID() != null)
            placerUUID = ep.getUUID();
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
        getLevel().updateNeighborsAt(getBlockPos(), this.getBlockState().getBlock()); //todo make sure block updating works
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
        try { Class.forName("reika.rotarycraft.auxiliary.PipeDebugLog").getMethod("event", String.class).invoke(null, "sendSyncPacket.call." + (this.shouldFullSync() ? "forced" : "delta")); } catch (Throwable ignored) {}
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

    private void sendPacketToAllAround(SyncPacket p, int radius) {
        if (!level.isClientSide()) {
            CustomPacketPayload payload = ReikaPacketHelper.toPayload(DragonAPI.MODID, (ReikaPacketHelper.PacketObj) p, DragonAPI.packetChannel);
            PacketDistributor.sendToPlayersNear((ServerLevel) level, null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), radius, payload);
        }
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
        lastRedstone = NBTCompat.getBoolean(tag, "lastredstone", false);
        redstoneInput = NBTCompat.getBoolean(tag, "thisredstone", false);
    }

    /**
     * Public entry point for the network bridge to deliver a periodic-sync NBT payload (sent by
     * {@link #sendSyncPacket()} and decoded in {@code APIPacketHandler.BE_NBT_SYNC}). Calls into
     * {@link #readSyncTag} with the same merge-in pattern as {@link #handleCompoundSyncPacket}:
     * we write the current local state into a fresh tag first so fields that weren't included in
     * the incremental sync packet don't zero out, then overlay the received values on top, then
     * read the merged result. Without the merge, an incremental SyncPacket carrying only the
     * changed keys would clobber unrelated fields back to their default-empty values.
     */
    public final void applySyncTag(CompoundTag incoming) {
        if (incoming == null) return;
        try { Class.forName("reika.rotarycraft.auxiliary.PipeDebugLog").getMethod("event", String.class).invoke(null, "applySyncTag.call"); } catch (Throwable ignored) {}
        CompoundTag merged = new CompoundTag();
        this.writeSyncTag(merged);
        for (String key : incoming.keySet()) {
            Tag val = incoming.get(key);
            if (val == null) {
                merged.remove(key);
            } else {
                merged.put(key, val);
            }
        }
        this.readSyncTag(merged);
        // 26.1 PERF: removed the {@code level.setBlocksDirty} call that used to live here. It
        // was forcing the client to re-mesh the BE's chunk section on every BE NBT packet —
        // which the user reported as severe lag when a pipe network was active (the periodic
        // sync stream piled up re-mesh work faster than the client could process). BERs read
        // the BE's fields every frame regardless of chunk re-mesh state, so dynamic BE-driven
        // visuals (fluid levels, machine progress bars in BER form) update without forcing a
        // re-mesh. Blockstate-property changes (e.g., pipe arm CONN_X flags via multipart
        // JSON) trigger their own re-mesh via vanilla's ClientboundBlockUpdatePacket path.
    }

    public void load(CompoundTag tag) {
        this.readSyncTag(tag);

        placer = NBTCompat.getString(tag, "place", "");
        if (tag.contains("placeUUID"))
            placerUUID = UUID.fromString(NBTCompat.getString(tag, "placeUUID", "00000000-0000-0000-0000-000000000000"));

        unharvestable = NBTCompat.getBoolean(tag, "no_drops", false);
        unmineable = NBTCompat.getBoolean(tag, "no_mine", false);

        tileAge = NBTCompat.getLong(tag, "age_ticks", 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            if (input instanceof TagValueInput) {
                Field f = TagValueInput.class.getDeclaredField("input");
                f.setAccessible(true);
                CompoundTag tag = (CompoundTag) f.get(input);
                if (tag != null) {
                    this.load(tag);
                }
            } else {
                DragonAPI.LOGGER.error("BlockEntityBase loadAdditional called with non-TagValueInput: " + input.getClass());
            }
        } catch (Exception e) {
            DragonAPI.LOGGER.error("Failed to extract CompoundTag from TagValueInput", e);
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        this.writeSyncTag(tag);

        if (placer != null && !placer.isEmpty())
            tag.putString("place", placer);
        if (placerUUID != null)
            tag.putString("placeUUID", placerUUID.toString());

        tag.putBoolean("no_drops", unharvestable);
        tag.putBoolean("no_mine", unmineable);

        tag.putLong("age_ticks", tileAge);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        try {
            if (output instanceof TagValueOutput) {
                Field f = TagValueOutput.class.getDeclaredField("output");
                f.setAccessible(true);
                CompoundTag tag = (CompoundTag) f.get(output);
                if (tag != null) {
                    this.saveAdditional(tag);
                }
            } else {
                DragonAPI.LOGGER.error("BlockEntityBase saveAdditional called with non-TagValueOutput: " + output.getClass());
            }
        } catch (Exception e) {
            DragonAPI.LOGGER.error("Failed to extract CompoundTag from TagValueOutput", e);
        }
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
            ReikaChatHelper.write(this + " [" + FMLEnvironment.getDist() + "] is throwing " + e.getClass() + " on update: " + e.getMessage());
            ReikaChatHelper.write(Arrays.toString(e.getStackTrace()));
            ReikaChatHelper.write("");
        }

        DragonAPI.LOGGER.error(this + " [" + FMLEnvironment.getDist() + "] is throwing " + e.getClass() + " on update: " + e.getMessage());
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
            BlockEntity cached = this.getCachedTE(dir);
            // 1.21.5 cache-validation: {@link #updateCache} is only called on the BE's first
            // tick, so the cached reference can outlive the neighbour. If the neighbour is
            // broken its BE Java object stays in memory (held by this very cache), with its
            // last {@code omega}/{@code torque}/etc still set — every downstream consumer would
            // then read from a zombie tile forever (and shafts in a chain happily kept their
            // power after the source was removed, eventually overpressuring into explosions).
            // Validate the cached entry against the live world here, and refresh on miss.
            if (cached != null && cached.isRemoved()) {
                cached = null;
            }
            if (cached == null) {
                BlockPos npos = worldPosition.relative(dir);
                BlockEntity fresh = level != null ? level.getBlockEntity(npos) : null;
                adjTEMap[dir.ordinal()] = fresh;
                return fresh;
            }
            // Also bail if the cached entry's position is no longer adjacent (chunk reload
            // edge case): the same BlockEntity object cannot be at two positions, so a mismatch
            // means our cache is stale even though {@code isRemoved()} hasn't fired yet.
            BlockPos expected = worldPosition.relative(dir);
            if (!cached.getBlockPos().equals(expected)) {
                BlockEntity fresh = level != null ? level.getBlockEntity(expected) : null;
                adjTEMap[dir.ordinal()] = fresh;
                return fresh;
            }
            return cached;
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
        if (this.getTEName() != null)
            return this.getTEName();
        else
            return "Unnamed BlockEntity";
    }

}




