/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.callbacks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface PositionCallable<V> {

    V call(BlockGetter world, BlockPos pos);

}
