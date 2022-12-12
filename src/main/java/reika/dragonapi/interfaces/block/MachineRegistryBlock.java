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

import reika.dragonapi.interfaces.registry.TileEnum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;


public interface MachineRegistryBlock {

    TileEnum getMachine(BlockGetter world, BlockPos pos);

}
