package reika.dragonapi.auxiliary;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.interfaces.LegacyItemData;

@EventBusSubscriber(modid = DragonAPI.MODID)
public final class LegacyItemMigration {
    private LegacyItemMigration() {}
    @SubscribeEvent
    public static void loaded(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity item) {
            var stack = item.getItem().copy();
            LegacyItemData.migrate(stack);
            item.setItem(stack);
        } else if (event.getEntity() instanceof Player player) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
                LegacyItemData.migrate(player.getInventory().getItem(slot));
        }
    }
}
