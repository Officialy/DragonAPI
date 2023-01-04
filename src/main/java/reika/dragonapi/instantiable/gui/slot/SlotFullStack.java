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

public class SlotFullStack extends Slot {

    public SlotFullStack(Container ii, int id, int x, int y) {
        super(ii, id, x, y);
    }

}
