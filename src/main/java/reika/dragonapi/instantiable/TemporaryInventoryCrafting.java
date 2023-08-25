package reika.dragonapi.instantiable;

import net.minecraft.world.inventory.TransientCraftingContainer;
import reika.dragonapi.instantiable.gui.DummyContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;


public class TemporaryInventoryCrafting extends TransientCraftingContainer {

    public final int width;
    public final int height;

    public TemporaryInventoryCrafting(int w, int h) {
        super(new DummyContainer(MenuType.GENERIC_3x3, 4), w, h);
        width = w;
        height = h;
    }

    public TemporaryInventoryCrafting(ItemStack[][] in) {
        this(in.length, in[0].length);
        for (int i = 0; i < in.length; i++) {
            for (int k = 0; k < in[i].length; k++) {
                this.setItem(k, i, in[i][k]);
            }
        }
    }

    public TemporaryInventoryCrafting setItem(int x, int y, ItemStack is) {
        int slot = y * width + x;
        this.setItem(slot, is);
        return this;
    }
/*

	public TemporaryInventoryCrafting setItems(IRecipeContainer ir) {
		ItemStack[] disp = ReikaRecipeHelper.getPermutedRecipeArray(ir);
		for (int i = 0; i < disp.length; i++) {
			this.setItem(i, disp[i]);
		}
		return this;
	}*/

}
