package reika.dragonapi.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.event.client.ClientLoginEvent;
import reika.dragonapi.instantiable.event.client.ClientLogoutEvent;

/** Native connection lifecycle drives the original common events, including packaged file checks. */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class ClientSessionEvents {
    private ClientSessionEvents() {}
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        NeoForge.EVENT_BUS.post(new ClientLoginEvent(event.getPlayer(),true));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        NeoForge.EVENT_BUS.post(new ClientLogoutEvent(event.getPlayer()));
    }
}
