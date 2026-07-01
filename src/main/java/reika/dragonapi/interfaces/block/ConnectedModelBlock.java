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

import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional hook for blocks rendered with DragonAPI's connected-texture models
 * ({@code reika.dragonapi.instantiable.rendering.connected}). Lets a block widen (or narrow) what it
 * visually connects to; without it the models default to same-block equality.
 */
public interface ConnectedModelBlock {

	/**
	 * Whether this block's connected-texture model should treat {@code neighbor} as connected.
	 * Default: the neighbour is the same block.
	 */
	default boolean connectsToCT(BlockState self, BlockState neighbor) {
		return neighbor.is(self.getBlock());
	}
}
