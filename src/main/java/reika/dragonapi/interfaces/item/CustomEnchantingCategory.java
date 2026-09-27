package reika.dragonapi.interfaces.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * V33a {@code CustomEnchantingCategory}: an item that declares which enchanting category it counts as. 1.7.10 returned
 * an {@code EnumEnchantmentType}; in 26.2 an enchantment's category is the item tag in its supported items, so this is
 * that tag (for example {@code #minecraft:enchantable/mining}).
 */
public interface CustomEnchantingCategory {

	TagKey<Item> getEnchantingCategory();

}
