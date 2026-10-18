package reika.dragonapi.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.auxiliary.trackers.TickRegistry;
import reika.dragonapi.auxiliary.trackers.TickRegistry.Phase;
import reika.dragonapi.instantiable.event.client.GameFinishedLoadingEvent;

/** Physical-client event signatures stay out of the common registry's loading/verification path. */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class ClientTickEvents {
    private static boolean loadingPosted;
    private ClientTickEvents() {}

    @SubscribeEvent public static void renderPre(RenderFrameEvent.Pre event) {
        TickRegistry.instance.fireRender(event.getPartialTick().getGameTimeDeltaPartialTick(false), Phase.START);
    }

    @SubscribeEvent public static void renderPost(RenderFrameEvent.Post event) {
        TickRegistry.instance.fireRender(event.getPartialTick().getGameTimeDeltaPartialTick(false), Phase.END);
    }

    @SubscribeEvent public static void clientPre(ClientTickEvent.Pre event) {
        TickRegistry.instance.fireClient(Phase.START);
        finishedLoading();
    }

    @SubscribeEvent public static void clientPost(ClientTickEvent.Post event) {
        TickRegistry.instance.fireClient(Phase.END);
        finishedLoading();
    }

    private static void finishedLoading() {
        if (!loadingPosted) {
            loadingPosted = true;
            NeoForge.EVENT_BUS.post(new GameFinishedLoadingEvent());
        }
    }
}
