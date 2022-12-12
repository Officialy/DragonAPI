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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Identical to Slot but disallows item insertion.
 */
public class SlotNoClick extends Slot {

    public final boolean allowInsertion;
    public final boolean allowExtraction;

    public SlotNoClick(Container ii, int id, int x, int y, boolean add, boolean take) {
        super(ii, id, x, y);
        allowInsertion = add;
        allowExtraction = take;
    }

    @Override
    public boolean mayPlace(ItemStack is) {
        return allowInsertion && super.mayPlace(is);
    }

    @Override
    public boolean mayPickup(Player ep) {
        return allowExtraction && super.mayPickup(ep);
    }
}
