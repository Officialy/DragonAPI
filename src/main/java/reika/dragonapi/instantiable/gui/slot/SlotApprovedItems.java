/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.gui.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.ArrayList;

public class SlotApprovedItems extends Slot {

    private final ArrayList<ItemStack> items = new ArrayList<>();

    public SlotApprovedItems(Container ii, int par2, int par3, int par4) {
        super(ii, par2, par3, par4);
    }

    public SlotApprovedItems addItem(Item i) {
        return this.addItem(new ItemStack(i));
    }

    public SlotApprovedItems addItem(Block b) {
        return this.addItem(new ItemStack(b));
    }

    public SlotApprovedItems addItem(ItemStack is) {
        //if (!ReikaItemHelper.collectionContainsItemStack(items, is))
        items.add(is);
        return this;
    }

    public SlotApprovedItems addItems(ItemStack... is) {
        for (ItemStack itemStack : is) this.addItem(itemStack);
        return this;
    }

    public SlotApprovedItems addItems(ArrayList<ItemStack> li) {
        for (ItemStack stack : li) this.addItem(stack);
        return this;
    }


    public boolean isItemValid(ItemStack is) {
        return ReikaItemHelper.collectionContainsItemStack(items, is);
    }

}
