/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.gui.Slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that needs no inventory. Use it for things like ghost items (like diamond pipes).
 */
public final class GhostSlot extends Slot {

    public GhostSlot(Container ii, int id, int x, int y) {
        super(ii, id, x, y);
    }

    public GhostSlot(int idx, int x, int y) {
        this(null, idx, x, y);
    }


    public ItemStack getStack() {
        return null;
    }


    public void putStack(ItemStack par1ItemStack) {
    }


    public void onSlotChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public ItemStack decrStackSize(int par1) {
        return null;
    }

}
