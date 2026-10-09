package reika.dragonapi.data;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import reika.dragonapi.DragonAPI;

@EventBusSubscriber(modid = DragonAPI.MODID)
public final class DragonAPIDataProviders {
    private DragonAPIDataProviders() {}

    @SubscribeEvent
    public static void onGatherServer(GatherDataEvent.Server event) {
        event.createProvider(DragonAPIBlockTagsProvider::new);
    }
}
