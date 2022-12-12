package reika.dragonapi.instantiable.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.item.ItemEvent;
import reika.dragonapi.interfaces.registry.ItemEnum;

public class ItemUpdateEvent extends ItemEvent {

    public ItemUpdateEvent(ItemEntity ei) {
        super(ei);
    }
}
