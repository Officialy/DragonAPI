package reika.dragonapi.interfaces;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;

/** Item-owned subtype identity when a component's meaning depends on its item, such as stored charge. */
public interface ItemVariantProvider {
    /** Return the native component values belonging to subtype identity, excluding unrelated item data. */
    DataComponentMap getKeyVariantComponents(ItemStack stack);
}
