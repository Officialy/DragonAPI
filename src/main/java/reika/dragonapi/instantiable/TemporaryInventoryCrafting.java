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
        this(getWidth(in), getHeight(in));
        for (int i = 0; i < in.length; i++) {
            for (int k = 0; k < in[i].length; k++) {
                this.setItem(k, i, in[i][k] != null ? in[i][k] : ItemStack.EMPTY);
            }
        }
    }

    private static int getWidth(ItemStack[][] in) {
        validateInput(in);
        return in[0].length;
    }

    private static int getHeight(ItemStack[][] in) {
        validateInput(in);
        return in.length;
    }

    private static void validateInput(ItemStack[][] in) {
        if (in == null || in.length == 0 || in[0] == null || in[0].length == 0)
            throw new IllegalArgumentException("Crafting input must be a non-empty rectangular matrix");
        int width = in[0].length;
        for (int row = 0; row < in.length; row++) {
            if (in[row] == null || in[row].length != width)
                throw new IllegalArgumentException("Crafting input row " + row + " does not match width " + width);
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
