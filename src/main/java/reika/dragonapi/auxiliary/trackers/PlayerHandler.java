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

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import reika.dragonapi.DragonAPI;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = DragonAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PlayerHandler {

    public static final PlayerHandler instance = new PlayerHandler();

    private final ArrayList<PlayerTracker> trackers = new ArrayList<>();

    private PlayerHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void registerTracker(PlayerTracker p) {
        trackers.add(p);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (PlayerTracker p : trackers) {
            p.onPlayerLogin(evt.getEntity());
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent evt) {
        for (PlayerTracker p : trackers) {
            p.onPlayerLogout(evt.getEntity());
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent evt) {
        for (PlayerTracker p : trackers) {
            p.onPlayerRespawn(evt.getEntity());
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent evt) {
        for (PlayerTracker p : trackers) {
            p.onPlayerChangedDimension(evt.getEntity(), evt.getFrom(), evt.getTo());
        }
    }

    public interface PlayerTracker {

        void onPlayerLogin(Player ep);

        void onPlayerLogout(Player player);

        void onPlayerChangedDimension(Player player, ResourceKey<Level> dimFrom, ResourceKey<Level> dimTo);

        void onPlayerRespawn(Player player);

    }

}
