package reika.dragonapi.interfaces;

import net.minecraft.world.item.ItemStack;

/** Item-owned conversion of old save components, shared with inventory and entity load boundaries. */
public interface LegacyItemData {
    void migrateLegacyComponents(ItemStack stack);

    static void migrate(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof LegacyItemData legacy) legacy.migrateLegacyComponents(stack);
    }
}
