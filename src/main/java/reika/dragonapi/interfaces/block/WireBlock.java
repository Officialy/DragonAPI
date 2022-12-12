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
import net.minecraft.world.level.BlockGetter;

import java.awt.*;

public interface WireBlock {

	int getPowerState(BlockGetter world, BlockPos pos);

	/**
	 * Does it connect to this side at all?
	 */
	boolean isConnectedTo(BlockGetter world, BlockPos pos, int side);

	/**
	 * Is it another wire block or a redstone logic block?
	 */
	boolean isDirectlyConnectedTo(BlockGetter world, BlockPos pos, int side);

	boolean isTerminus(BlockGetter world, BlockPos pos, int side);

	Color getColor();

	boolean drawWireUp(BlockGetter world, BlockPos pos, int side);

}
