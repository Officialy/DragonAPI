/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface SemiUnbreakable {

    boolean isUnbreakable(Level world, BlockPos pos);

}
