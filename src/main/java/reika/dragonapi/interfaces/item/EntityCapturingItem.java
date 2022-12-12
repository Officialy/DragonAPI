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


public interface EntityCapturingItem {

    boolean hasEntity(ItemStack is);

    String currentEntityName(ItemStack is);

}
