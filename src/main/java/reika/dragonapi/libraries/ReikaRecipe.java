package reika.dragonapi.libraries;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import reika.dragonapi.interfaces.IReikaRecipe;

public class ReikaRecipe implements IReikaRecipe {

    public ItemStack output;
    public Object[] ingredients;

    public ReikaRecipe(ItemStack output, Object[] ingredients) {
        this.output = output;
        this.ingredients = ingredients;
    }

    @Override
    public boolean matches(Container inv, Level level) {
        return false;
    }

    @Override
    public ItemStack getCraftingResult(Container inv) {
        return output;
    }

    @Override
    public int getRecipeGridSize() {
        return ingredients.length;
    }

    @Override
    public ItemStack getResult() {
        return output;
    }
}
