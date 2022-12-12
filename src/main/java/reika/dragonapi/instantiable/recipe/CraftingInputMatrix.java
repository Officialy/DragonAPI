package reika.dragonapi.instantiable.recipe;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import reika.dragonapi.interfaces.blockentity.CraftingTile;

public class CraftingInputMatrix extends CraftingContainer {

    private final CraftingTile tile;

    private boolean hasItems;

    public CraftingInputMatrix(CraftingTile te) {
        super(null, 3, 3);
        tile = te;
    }

    public void update() {
        this.update(0);
    }

    public void update(int gridSlotOffset) {
//todo        eventHandler = tile.constructContainer();
        hasItems = false;
        for (int i = 0; i < 9; i++) {
            ItemStack in = tile.getItem(i+gridSlotOffset);
            ItemStack has = this.getItem(i);
            if (!ItemStack.isSame(in, has)) {
                this.setItem(i, in);
            }
            hasItems |= in != null;
        }
    }

    public boolean isEmpty() {
        return !hasItems;
    }

}