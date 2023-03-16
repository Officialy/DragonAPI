package reika.dragonapi.interfaces;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IReikaRecipe {
    boolean matches(Container inv, Level level);

    ItemStack getCraftingResult(Container inv);

    int getRecipeGridSize();

    ItemStack getResult();
}