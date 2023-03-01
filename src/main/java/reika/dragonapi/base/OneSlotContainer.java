/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class OneSlotContainer<T extends BlockEntityBase> extends CoreContainer<T> {

    private final Container inv;

    public OneSlotContainer(MenuType<?> type, int id, Inventory inv, T te, int offsetY) {
        super(type, id, inv, te);
        this.inv = (Container) te;

        this.addPlayerInventoryWithOffset(inv, 0, offsetY);
    }

}