/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.item;

import net.minecraft.world.item.ItemStack;

public interface ActivatedInventoryItem {

    ItemStack[] getInventory(ItemStack is);

    void decrementSlot(ItemStack is, int slot, int amt);

    boolean isSlotActive(ItemStack is, int slot);

    int getInventorySize(ItemStack is);

    ItemStack getItem(ItemStack is, int slot);

}
