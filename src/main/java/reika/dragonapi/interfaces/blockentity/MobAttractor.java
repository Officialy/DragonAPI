/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.blockentity;

import net.minecraft.world.entity.LivingEntity;

public interface MobAttractor {

    boolean canAttract(LivingEntity e);

}
