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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Implement this if the block can be treated as a liquid source block for some implementations
 * (eg an open-top vat being a valid source for a suction pump).
 */
public interface FluidBlockSurrogate {

	Fluid getFluid(Level world, BlockPos pos);

	/**
	 * If the block supports non-bucket values (eg "remove 250mB of fluid")
	 */
	boolean supportsQuantization(Level world, BlockPos pos);

	/**
	 * Works like the same in {@link IFluidHandler}.
	 */
	int drain(Level world, BlockPos pos, Fluid f, int amt, boolean doDrain);

}
