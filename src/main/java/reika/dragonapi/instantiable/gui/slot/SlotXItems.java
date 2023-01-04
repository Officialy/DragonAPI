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

public class SlotXItems extends Slot {

    public final int slotCapacity;

    public SlotXItems(Container ii, int par2, int par3, int par4, int size) {
        super(ii, par2, par3, par4);
        slotCapacity = size;
    }

    @Override
    public int getMaxStackSize() {
        return slotCapacity;
    }
}
