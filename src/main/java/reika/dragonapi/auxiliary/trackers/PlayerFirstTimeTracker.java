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
import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;

import java.util.ArrayList;

public class PlayerFirstTimeTracker {

    private static final String BASE_TAG = "DragonAPI_PlayerTracker_";
    private static final ArrayList<PlayerTracker> list = new ArrayList<>();
    private static final ArrayList<String> tags = new ArrayList<>();

    public static void addTracker(PlayerTracker pt) {
        String s = pt.getID();
        if (tags.contains(s))
            throw new MisuseException("Duplicate PlayerTracker ID: " + s);
        DragonAPI.LOGGER.info("Creating player tracker " + s);
        list.add(pt);
        tags.add(s);
    }

    public interface PlayerTracker {

        void onNewPlayer(Player ep);

        /**
         * This MUST be unique!
         */
        String getID();
    }
}
