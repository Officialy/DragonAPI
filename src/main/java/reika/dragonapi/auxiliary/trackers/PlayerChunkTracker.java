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

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//MultiThread-safe PlayerChunkLoadTracker
//Can be used to disable chunk loading around a specific player.
public class PlayerChunkTracker implements TickRegistry.TickHandler {

    // Singleton instance
    public static final PlayerChunkTracker instance = new PlayerChunkTracker();

    // Ticks until player gets unregistered.
    private static final int UNREGISTER_THRESHOLD = 40; // 2 sec
    private final HashMap<Player, TrackerEntry> trackedPlayers = new HashMap<>(),
            queuedWaitingEntries = new HashMap<>();

    // Flag if the Tracker is currently in the tick of checking all entries'
    // conditions.
    // Used to check where to add new tracker entries.
    private boolean isInTick = false;

    private PlayerChunkTracker() {
    }

    // Check inserted via ASM into Level.setActivePlayerChunksAndCheckLight
    // If this returns true, the chunks around the player are not added to the
    // list of chunks that have to be loaded.
    public static boolean shouldStopChunkloadingFor(Player player) {
        return PlayerChunkTracker.instance.trackedPlayers.containsKey(player);
    }

    // Adds the player with his condition to the
    public static void startTrackingPlayer(Player player, TrackingCondition condition) {
        PlayerChunkTracker pct = PlayerChunkTracker.instance;
        TrackerEntry newEntry = new TrackerEntry(UNREGISTER_THRESHOLD, condition);
        if (pct.isInTick) {
            synchronized (pct.queuedWaitingEntries) {
                pct.queuedWaitingEntries.put(player, newEntry);
            }
        } else {
            synchronized (pct.trackedPlayers) {
                pct.trackedPlayers.put(player, newEntry);
            }
        }
    }

    @Override
    public void tick(TickRegistry.TickType type, Object... tickData) {
        isInTick = true;
        synchronized (trackedPlayers) {
            Iterator<Map.Entry<Player, TrackerEntry>> iterator = trackedPlayers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Player, TrackerEntry> entry = iterator.next();
                TrackerEntry tracker = entry.getValue();
                if (entry.getKey().isDeadOrDying() || !tracker.condition.shouldBeTracked(entry.getKey())) {
                    tracker.timeout--;
                } else {
                    tracker.timeout = UNREGISTER_THRESHOLD;
                }
                if (tracker.timeout <= 0) {
                    iterator.remove();
                    entry.getValue().condition.onUntrack(entry.getKey());

                    // Triggering chunk reload.
                    // "move" the previously managed distance FAR away so
                    // Minecraft thinks it needs to create new chunks around the
                    // player.
                    if (entry.getKey() != null) {
                        Player emp = entry.getKey();
                        emp.xo = Double.MAX_VALUE;
                        emp.zo = Double.MAX_VALUE;
                    }
                }
            }
        }
        isInTick = false;
        synchronized (queuedWaitingEntries) {
            trackedPlayers.putAll(queuedWaitingEntries);
            queuedWaitingEntries.clear();
        }
    }

    // Should only execute for the server.
    @Override
    public EnumSet<TickRegistry.TickType> getType() {
        return EnumSet.of(TickRegistry.TickType.SERVER);
    }

    @Override
    public boolean canFire(TickEvent.Phase p) {
        return p == TickEvent.Phase.END;
    }

    @Override
    public String getLabel() {
        return "Player Chunk Tracker";
    }

    public interface TrackingCondition {

        // Return true, if the player should remain tracked, false otherwise.
        boolean shouldBeTracked(Player player);

        // Called once a player gets removed from the tracker.
        void onUntrack(Player player);

    }

    public static class TrackerEntry {

        private final TrackingCondition condition;
        private int timeout;

        public TrackerEntry(int timeout, TrackingCondition condition) {
            this.timeout = timeout;
            this.condition = condition;
        }

    }

}
