/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.auxiliary.trackers;

import java.util.EnumSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.mixin.ChunkMapAccessor;

/** Server-owned suspension of player chunk tickets and chunk-view updates. */
public final class PlayerChunkTracker implements TickRegistry.TickHandler {
    public static final PlayerChunkTracker instance = new PlayerChunkTracker();
    private static final int UNREGISTER_THRESHOLD = 40;
    private final ConcurrentHashMap<Owner, TrackedPlayer> trackedPlayers = new ConcurrentHashMap<>();
    // Terminal identities are weak: late registrations cannot resurrect them or retain an old server.
    private final Set<ServerPlayer> retiredPlayers = weakSet();
    private final Set<ServerLevel> retiredLevels = weakSet();
    private final Set<MinecraftServer> retiredServers = weakSet();
    private final ThreadLocal<Boolean> lifecycleCleanup = ThreadLocal.withInitial(() -> false);
    private PlayerChunkTracker() {}
    private static <T> Set<T> weakSet() { return Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>())); }

    private record Owner(MinecraftServer server, UUID uuid) {}
    private record TrackedPlayer(Owner owner, ServerPlayer player, ServerLevel level, TrackerEntry state) {}
    private static Owner owner(ServerPlayer player) { return new Owner(player.level().getServer(), player.getUUID()); }

    /** A replacement entity or dimension cannot inherit suspension. */
    public static boolean shouldStopChunkloadingFor(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        TrackedPlayer entry = instance.trackedPlayers.get(owner(serverPlayer));
        return entry != null && entry.player == player && entry.level == player.level();
    }

    /** Off-thread requests are applied on the owning server; callbacks never run off-thread. */
    public static void startTrackingPlayer(Player player, TrackingCondition condition) {
        Objects.requireNonNull(condition, "condition");
        if (!(player instanceof ServerPlayer serverPlayer)) throw new IllegalArgumentException("Chunk tracking requires a server player");
        ServerLevel requestedLevel = serverPlayer.level();
        onServer(requestedLevel.getServer(), () -> {
            if (serverPlayer.level() == requestedLevel) instance.start(serverPlayer, condition);
        });
    }
    private void start(ServerPlayer player, TrackingCondition condition) {
        if (lifecycleCleanup.get() || retiredPlayers.contains(player) || retiredLevels.contains(player.level())
                || retiredServers.contains(player.level().getServer()) || !isCurrent(player, player.level())) return;
        Owner key = owner(player);
        TrackedPlayer next = new TrackedPlayer(key, player, player.level(), new TrackerEntry(UNREGISTER_THRESHOLD, condition));
        TrackedPlayer previous = trackedPlayers.put(key, next);
        // Renewing the same owner's condition retains the original replacement semantics.
        if (previous != null && (previous.player != player || previous.level != player.level())) notifyUntrack(previous, false);
        player.level().getChunkSource().chunkMap.move(player);
    }
    public static void stopTrackingPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        onServer(serverPlayer.level().getServer(), () -> {
            TrackedPlayer entry = instance.trackedPlayers.get(owner(serverPlayer));
            if (entry != null && entry.player == player) instance.release(entry, true);
        });
    }
    private static void onServer(MinecraftServer server, Runnable action) {
        if (server.isSameThread()) action.run(); else server.execute(action);
    }
    private static boolean isCurrent(ServerPlayer player, ServerLevel level) {
        return !player.isRemoved() && player.level() == level
                && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player;
    }

    @Override public void tick(TickRegistry.TickType type, Object... tickData) {
        if (type != TickRegistry.TickType.SERVER) return;
        MinecraftServer server = null;
        for (Object datum : tickData) if (datum instanceof MinecraftServer value) server = value;
        if (server == null || !server.isSameThread()) throw new IllegalArgumentException("Tick with the owning server on its thread");
        for (TrackedPlayer entry : List.copyOf(trackedPlayers.values())) {
            if (entry.owner.server != server || trackedPlayers.get(entry.owner) != entry) continue;
            if (!isCurrent(entry.player, entry.level)) { cleanup(value -> value == entry); continue; }
            boolean keep;
            try { keep = !entry.player.isDeadOrDying() && entry.state.condition.shouldBeTracked(entry.player); }
            catch (RuntimeException error) {
                DragonAPI.LOGGER.error("Chunk tracking condition failed for {}; releasing suspension", entry.owner.uuid, error);
                release(entry, true);
                continue;
            }
            // A callback may replace entries without changing the iteration snapshot.
            if (trackedPlayers.get(entry.owner) != entry) continue;
            entry.state.timeout = keep ? UNREGISTER_THRESHOLD : entry.state.timeout - 1;
            if (entry.state.timeout <= 0) release(entry, true);
        }
    }
    private void release(TrackedPlayer entry, boolean refresh) {
        if (trackedPlayers.remove(entry.owner, entry)) notifyUntrack(entry, refresh);
    }
    private void notifyUntrack(TrackedPlayer entry, boolean refresh) {
        try { entry.state.condition.onUntrack(entry.player); }
        catch (RuntimeException error) { DragonAPI.LOGGER.error("Chunk untrack callback failed for {}", entry.owner.uuid, error); }
        finally {
            if (refresh && isCurrent(entry.player, entry.level)) {
                // Native ignored-player transitions restore tickets, including for an unmoved player.
                var map = entry.level.getChunkSource().chunkMap;
                map.move(entry.player);
                ((ChunkMapAccessor)map).dragonapi$updateChunkTracking(entry.player);
            }
        }
    }

    private void cleanup(Predicate<TrackedPlayer> matches) {
        boolean previous = lifecycleCleanup.get();
        lifecycleCleanup.set(true);
        try {
            for (TrackedPlayer entry : List.copyOf(trackedPlayers.values())) if (matches.test(entry)) release(entry, false);
        } finally {
            if (previous) lifecycleCleanup.set(true); else lifecycleCleanup.remove();
        }
    }
    public void onLogout(ServerPlayer player) {
        onServer(player.level().getServer(), () -> {
            retiredPlayers.add(player);
            cleanup(entry -> entry.player == player);
        });
    }
    /** Respawn/dimension events expose the new owner; only retire the old binding. */
    public void onPlayerReplacement(ServerPlayer player) {
        Owner key = owner(player);
        onServer(key.server, () -> cleanup(entry -> entry.owner.equals(key)
                && (entry.player != player || entry.level != player.level())));
    }
    public void clearLevel(ServerLevel level) {
        onServer(level.getServer(), () -> { retiredLevels.add(level); cleanup(entry -> entry.level == level); });
    }
    public void clearServer(MinecraftServer server) {
        onServer(server, () -> { retiredServers.add(server); cleanup(entry -> entry.owner.server == server); });
    }
    public int getTrackedPlayerCount(MinecraftServer server) {
        return (int)trackedPlayers.keySet().stream().filter(key -> key.server == server).count();
    }

    /** Modern counterpart of the original PlayerManager bridge; ticket updates stay native. */
    public static void tryUpdatePlayerPertinentChunks(net.minecraft.server.level.ChunkMap map, ServerPlayer player) { map.move(player); }
    @Override public EnumSet<TickRegistry.TickType> getType() { return EnumSet.of(TickRegistry.TickType.SERVER); }
    @Override public boolean canFire(TickRegistry.Phase phase) { return phase == TickRegistry.Phase.END; }
    @Override public String getLabel() { return "Player Chunk Tracker"; }
    public interface TrackingCondition {
        boolean shouldBeTracked(Player player);
        void onUntrack(Player player);
    }
    public static class TrackerEntry {
        private final TrackingCondition condition;
        private int timeout;
        public TrackerEntry(int timeout, TrackingCondition condition) {
            if (timeout <= 0) throw new IllegalArgumentException("Timeout must be positive");
            this.timeout = timeout; this.condition = Objects.requireNonNull(condition, "condition");
        }
    }
}
