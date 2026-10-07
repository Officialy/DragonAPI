package reika.dragonapi.auxiliary;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.auxiliary.trackers.VersionTransitionTracker;
import reika.dragonapi.auxiliary.trackers.WorldgenProfiler;
import reika.dragonapi.libraries.ReikaIngredientHelper;

/** Releases shared caches at the boundaries which invalidate their contents. */
@EventBusSubscriber(modid = DragonAPI.MODID)
public final class SessionLifecycle {
    private SessionLifecycle() {}

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && level.dimension() == Level.OVERWORLD) {
            VersionTransitionTracker.instance.onWorldLoad(level);
            if (!reika.dragonapi.libraries.level.ReikaWorldHelper.getCurrentWorldID(level).isValid())
                reika.dragonapi.libraries.level.ReikaWorldHelper.onWorldCreation(level);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            VersionTransitionTracker.instance.notifyPlayerOfVersionChanges(player);
    }

    @SubscribeEvent
    public static void reload(OnDatapackSyncEvent event) {
        ReikaIngredientHelper.clearCache();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        KeyWatcher.instance.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() == WorldgenProfiler.getLevel()) WorldgenProfiler.finishProfiling();
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        KeyWatcher.instance.clear();
        ReikaIngredientHelper.clearCache();
        WorldgenProfiler.finishProfiling();
        VersionTransitionTracker.instance.clear();
        reika.dragonapi.libraries.level.ReikaWorldHelper.clearWorldIDCache();
    }
}
