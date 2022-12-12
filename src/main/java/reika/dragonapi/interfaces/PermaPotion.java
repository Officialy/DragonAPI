/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;

public interface PermaPotion {

    boolean canBeCleared(LivingEntity e, Potion pot);

}
