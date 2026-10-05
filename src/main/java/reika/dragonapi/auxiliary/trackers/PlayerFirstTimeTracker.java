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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.ArrayList;

@EventBusSubscriber(modid = DragonAPI.MODID)
public class PlayerFirstTimeTracker {

    private static final String BASE_TAG = "DragonAPI_PlayerTracker_";
    private static final ArrayList<PlayerTracker> list = new ArrayList<>();
    private static final ArrayList<String> tags = new ArrayList<>();

    public static synchronized void addTracker(PlayerTracker pt) {
        String s = pt.getID();
        if (tags.contains(s))
            throw new MisuseException("Duplicate PlayerTracker ID: " + s);
        DragonAPI.LOGGER.info("Creating player tracker " + s);
        list.add(pt);
        tags.add(s);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        checkPlayer(event.getEntity());
    }

    /** Each callback runs once per player and tracker, with flags surviving death and reconnects. */
    public static void checkPlayer(Player player) {
        if (!(player instanceof ServerPlayer) || player instanceof net.neoforged.neoforge.common.util.FakePlayer)
            return;
        var data = ReikaPlayerAPI.getDeathPersistentNBT(player);
        for (PlayerTracker tracker : snapshot()) {
            String tag = BASE_TAG + tracker.getID();
            if (!data.getBooleanOr(tag, false)) {
                tracker.onNewPlayer(player);
                data.putBoolean(tag, true);
            }
        }
    }

    private static synchronized java.util.List<PlayerTracker> snapshot() {
        return java.util.List.copyOf(list);
    }

    public interface PlayerTracker {

        void onNewPlayer(Player ep);

        /**
         * This MUST be unique!
         */
        String getID();
    }
}
