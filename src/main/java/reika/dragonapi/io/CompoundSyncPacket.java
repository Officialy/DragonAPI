/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017. All rights reserved.
 * Distribution requires explicit prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.io;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.DragonOptions;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.io.SyncPacket;
import reika.dragonapi.interfaces.DataSync;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

/** Optional native bundles of the existing channel-bound BE_NBT_SYNC payloads. */
public class CompoundSyncPacket implements DataSync {
    public static final CompoundSyncPacket instance = new CompoundSyncPacket();
    private static final int SNAPSHOT_VERSION = 1;
    private static final int MAX_SNAPSHOT_ENTRIES = 4096;
    private static final int BUNDLE_SIZE = 256;
    private final Map<WorldLocation, State> states = new LinkedHashMap<>();
    private final Map<WorldLocation, Change> received = new LinkedHashMap<>();

    private static final class State {
        final WeakReference<BlockEntity> tile;
        final Identifier type;
        CompoundTag sent = new CompoundTag();
        CompoundTag latest;
        boolean force;
        double radius;
        long revision;
        State(BlockEntity tile) { this.tile = new WeakReference<>(tile); type = typeOf(tile); }
    }
    /** Immutable snapshot identity and detached NBT; accessors cannot mutate the queued state. */
    public record Change(WorldLocation location, Identifier type, CompoundTag patch) {
        public Change {
            Objects.requireNonNull(location); Objects.requireNonNull(type);
            patch = Objects.requireNonNull(patch).copy();
        }
        @Override public CompoundTag patch() { return patch.copy(); }
    }
    private static Identifier typeOf(BlockEntity tile) {
        return Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType()));
    }
    /** Source compatibility: direct aggregate callers address all players in that dimension. */
    public void setData(BlockEntity tile, boolean force, CompoundTag tag) { setData(tile, force, tag, Double.POSITIVE_INFINITY); }
    public synchronized void setData(BlockEntity tile, boolean force, CompoundTag tag, double radius) {
        if (!(tile.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Compound sync must be queued on the owning server thread");
        if (Double.isNaN(radius) || radius < 0) throw new IllegalArgumentException("Invalid sync radius");
        if (tag.contains(SyncPacket.REMOVED_KEYS)) throw new IllegalArgumentException("Full sync state contains the reserved removal key");
        WorldLocation location = new WorldLocation(tile);
        State state = states.get(location);
        if (state == null || state.tile.get() != tile) { state = new State(tile); states.put(location, state); }
        state.latest = tag.copy(); state.force |= force; state.radius = radius; state.revision++;
    }
    /** Carry the prior per-tile state into batching, including keys which later disappear before a flush. */
    public synchronized void primeBaseline(BlockEntity tile, CompoundTag prior) {
        if (!(tile.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Compound baseline must be primed on the server thread");
        State state = new State(tile); state.sent = prior.copy(); states.put(new WorldLocation(tile), state);
    }
    /** Diff against the consumed baseline; intermediate queue writes cannot erase outstanding changes. */
    public static CompoundTag difference(CompoundTag before, CompoundTag after, boolean force) {
        CompoundTag patch = new CompoundTag();
        for (String key : after.keySet())
            if (force || !Objects.equals(before.get(key), after.get(key))) patch.put(key, after.get(key).copy());
        ListTag removed = new ListTag();
        before.keySet().stream().filter(key -> !after.contains(key)).sorted().forEach(key -> removed.add(StringTag.valueOf(key)));
        if (!removed.isEmpty()) patch.put(SyncPacket.REMOVED_KEYS, removed);
        return patch;
    }
    public static void merge(CompoundTag target, CompoundTag patch) {
        for (var key : patch.getListOrEmpty(SyncPacket.REMOVED_KEYS)) key.asString().ifPresent(target::remove);
        for (String key : patch.keySet()) if (!key.equals(SyncPacket.REMOVED_KEYS)) target.put(key, patch.get(key).copy());
    }
    private static CompoundTag pending(State state) {
        return state.latest != null ? difference(state.sent, state.latest, state.force) : new CompoundTag();
    }
    public synchronized List<Change> snapshot() {
        ArrayList<Change> result = new ArrayList<>();
        for (var entry : states.entrySet()) {
            CompoundTag patch = pending(entry.getValue());
            if (!patch.isEmpty()) result.add(new Change(entry.getKey(), entry.getValue().type, patch));
        }
        return List.copyOf(result);
    }
    public synchronized boolean isEmpty() { return snapshot().isEmpty(); }
    public synchronized boolean isEmpty(WorldLocation location) {
        State state = states.get(location); return state == null || pending(state).isEmpty();
    }
    @Override public synchronized boolean hasNoData() { return received.isEmpty() && states.isEmpty(); }
    @Override public synchronized String toString() { return snapshot().toString(); }
    public synchronized void readForSync(BlockEntity tile, CompoundTag tag) {
        WorldLocation location = new WorldLocation(tile);
        Change change = received.get(location);
        if (change != null && change.type().equals(typeOf(tile))) merge(tag, change.patch());
        else {
            State state = states.get(location);
            if (state != null && state.tile.get() == tile) merge(tag, pending(state));
        }
    }
    public static CompoundSyncPacket fromChanges(BlockEntity tile, CompoundTag patch) {
        CompoundSyncPacket packet = new CompoundSyncPacket();
        WorldLocation location = new WorldLocation(tile);
        packet.received.put(location, new Change(location, typeOf(tile), patch));
        return packet;
    }
    /** Standalone snapshot codec, not a registered network packet. Full signed BlockPos Y is retained. */
    public synchronized void write(FriendlyByteBuf output) {
        List<Change> changes = received.isEmpty() ? snapshot() : List.copyOf(received.values());
        if (changes.size() > MAX_SNAPSHOT_ENTRIES) throw new IllegalArgumentException("Too many snapshot entries");
        int start = output.writerIndex();
        try {
            output.writeVarInt(SNAPSHOT_VERSION); output.writeVarInt(changes.size());
            for (Change change : changes) {
                output.writeResourceKey(change.location().getDimension()); output.writeInt(change.location().pos.getX()); output.writeInt(change.location().pos.getY()); output.writeInt(change.location().pos.getZ());
                output.writeIdentifier(change.type()); output.writeNbt(change.patch());
            }
            if (output.writerIndex() - start > 1_048_576) throw new IllegalArgumentException("Snapshot too large");
        } catch (RuntimeException failure) { output.writerIndex(start); throw failure; }
    }
    public synchronized void readPacketData(FriendlyByteBuf input) {
        if (input.readableBytes() > 1_048_576) throw new IllegalArgumentException("Snapshot too large");
        if (input.readVarInt() != SNAPSHOT_VERSION) throw new IllegalArgumentException("Unknown snapshot version");
        int count = input.readVarInt();
        if (count < 0 || count > MAX_SNAPSHOT_ENTRIES) throw new IllegalArgumentException("Invalid snapshot count");
        Map<WorldLocation, Change> decoded = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceKey<Level> dimension = input.readResourceKey(Registries.DIMENSION);
            WorldLocation location = new WorldLocation(dimension, input.readInt(), input.readInt(), input.readInt());
            Identifier type = input.readIdentifier(); CompoundTag patch = input.readNbt();
            if (patch == null || decoded.put(location, new Change(location, type, patch)) != null)
                throw new IllegalArgumentException("Missing patch or duplicate location");
        }
        if (input.isReadable()) throw new IllegalArgumentException("Trailing snapshot data");
        received.clear(); received.putAll(decoded);
    }
    /** Apply only to loaded, matching client tiles; do not load chunks to deliver a snapshot. */
    public synchronized void handle(Level level) {
        if (level == null || !level.isClientSide()) return;
        for (Change change : received.values()) {
            if (!change.location().getDimension().equals(level.dimension()) || !level.hasChunkAt(change.location().pos)) continue;
            BlockEntity tile = level.getBlockEntity(change.location().pos);
            if (tile instanceof CompoundSyncPacketHandler receiver && typeOf(tile).equals(change.type())) receiver.handleCompoundSyncPacket(this);
        }
    }
    public interface CompoundSyncPacketHandler { void handleCompoundSyncPacket(CompoundSyncPacket packet); }
    public synchronized void forget(WorldLocation location) { states.remove(location); received.remove(location); }
    public synchronized void forgetDimension(ResourceKey<Level> dimension) {
        states.keySet().removeIf(location -> location.getDimension().equals(dimension));
        received.keySet().removeIf(location -> location.getDimension().equals(dimension));
    }
    public synchronized void clear() { states.clear(); received.clear(); }
    protected boolean supports(ServerPlayer player, CustomPacketPayload payload) { return player.connection.hasChannel(payload); }
    protected void send(ServerPlayer player, List<CustomPacketPayload> payloads) {
        PacketDistributor.sendToPlayer(player, payloads.getFirst(), payloads.subList(1, payloads.size()).toArray(CustomPacketPayload[]::new));
    }
    /** Snapshot before encoding/sending; failed sends retain pending work for an idempotent retry. */
    public synchronized void flush(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Compound sync flush requires the server thread");
        record Delivery(WorldLocation location, State state, CompoundTag full, long revision, double radius, CustomPacketPayload payload) {}
        ArrayList<Delivery> deliveries = new ArrayList<>();
        var iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!entry.getKey().getDimension().equals(level.dimension())) continue;
            State state = entry.getValue(); BlockEntity tile = state.tile.get();
            if (tile == null || tile.isRemoved() || !level.hasChunkAt(entry.getKey().pos) || level.getBlockEntity(entry.getKey().pos) != tile) {
                iterator.remove(); continue;
            }
            CompoundTag patch = pending(state);
            if (!patch.isEmpty()) {
                CustomPacketPayload payload = ReikaPacketHelper.toPayload(DragonAPI.MODID, SyncPacket.fromChanges(tile, patch), DragonAPI.packetChannel);
                deliveries.add(new Delivery(entry.getKey(), state, state.latest.copy(), state.revision, state.radius, payload));
            }
        }
        if (deliveries.isEmpty()) return;
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.level() != level) continue;
            ArrayList<CustomPacketPayload> selected = new ArrayList<>();
            for (Delivery delivery : deliveries) {
                double radius = delivery.radius();
                if (player.distanceToSqr(delivery.location().pos.getX(), delivery.location().pos.getY(), delivery.location().pos.getZ()) <= radius * radius
                        && supports(player, delivery.payload())) selected.add(delivery.payload());
            }
            for (int start = 0; start < selected.size(); start += BUNDLE_SIZE)
                send(player, List.copyOf(selected.subList(start, Math.min(selected.size(), start + BUNDLE_SIZE))));
        }
        for (Delivery delivery : deliveries) {
            delivery.state().sent = delivery.full();
            if (delivery.state().revision == delivery.revision()) {
                delivery.state().latest = null; delivery.state().force = false;
            }
        }
    }
    public static final class CompoundSyncPacketTracker implements TickRegistry.TickHandler {
        public static final CompoundSyncPacketTracker instance = new CompoundSyncPacketTracker();
        private final Map<ResourceKey<Level>, Long> lastTick = new LinkedHashMap<>();
        private final Map<ResourceKey<Level>, Integer> ticks = new LinkedHashMap<>();
        private final Map<ResourceKey<Level>, Long> lastError = new LinkedHashMap<>();
        private CompoundSyncPacketTracker() {}
        @Override public void tick(TickRegistry.TickType type, Object... data) {
            if (!(data[0] instanceof ServerLevel level)) return;
            long time = level.getGameTime();
            if (Objects.equals(lastTick.put(level.dimension(), time), time)) return;
            int count = ticks.getOrDefault(level.dimension(), 0) + 1;
            int interval = DragonOptions.SLOWSYNC.getState() ? 20 : 4;
            ticks.put(level.dimension(), count >= interval ? 0 : count);
            if (count >= interval) {
                try { CompoundSyncPacket.instance.flush(level); }
                catch (RuntimeException failure) {
                    Long previous = lastError.get(level.dimension());
                    if (previous == null || time - previous >= 200) {
                        lastError.put(level.dimension(), time);
                        DragonAPI.LOGGER.error("Compound sync flush failed; pending updates retained for retry", failure);
                    }
                }
            }
        }
        void clear(ResourceKey<Level> dimension) { lastTick.remove(dimension); ticks.remove(dimension); lastError.remove(dimension); }
        void clear() { lastTick.clear(); ticks.clear(); lastError.clear(); }
        @Override public EnumSet<TickRegistry.TickType> getType() { return EnumSet.of(TickRegistry.TickType.WORLD); }
        @Override public boolean canFire(TickRegistry.Phase phase) { return phase == TickRegistry.Phase.END; }
        @Override public String getLabel() { return "Compound Sync Packet"; }
    }
    @EventBusSubscriber(modid = DragonAPI.MODID)
    public static final class Lifecycle {
        @SubscribeEvent public static void tick(LevelTickEvent.Post event) { CompoundSyncPacketTracker.instance.tick(TickRegistry.TickType.WORLD, event.getLevel()); }
        @SubscribeEvent public static void unload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel level) {
                instance.forgetDimension(level.dimension()); CompoundSyncPacketTracker.instance.clear(level.dimension());
            }
        }
        @SubscribeEvent public static void stop(ServerStoppedEvent event) { instance.clear(); CompoundSyncPacketTracker.instance.clear(); }
    }
}
