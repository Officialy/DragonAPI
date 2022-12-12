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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;


public interface RedstoneTile {

    int getStrongPower(BlockGetter world, BlockPos pos, Direction side);

    int getWeakPower(BlockGetter world, BlockPos pos, Direction side);

}
