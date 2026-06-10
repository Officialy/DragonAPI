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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import reika.dragonapi.instantiable.storage.ManagedItemHandler;

/**
 * Identical to a normal handler-backed slot but disallows item insertion and/or extraction
 * based on the two flags passed in.
 *
 * <p>1.21.9: was a {@code SlotItemHandler} subclass. The whole {@code IItemHandler} family
 * was deprecated; we now extend the new {@link ResourceHandlerSlot} and bind to a
 * {@link ManagedItemHandler} via its {@code set} index modifier.
 */
public class SlotNoClick extends ResourceHandlerSlot {

    public final boolean allowInsertion;
    public final boolean allowExtraction;

    public SlotNoClick(ManagedItemHandler ii, int id, int x, int y, boolean add, boolean take) {
        super(ii, ii::set, id, x, y);
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
