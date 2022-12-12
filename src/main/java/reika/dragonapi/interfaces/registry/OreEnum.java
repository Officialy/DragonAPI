/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Random;


public interface OreEnum {

	int getHarvestLevel();

	String getHarvestTool();

	float getXPDropped(Level world, BlockPos pos);

	boolean dropsSelf(Level world, BlockPos pos);

	boolean enforceHarvestLevel();

	Block getBlock();

	BlockEntity getBlockEntity(Level world, BlockPos pos);

	boolean canGenAt(Level world, BlockPos pos);

	int getRandomGeneratedYCoord(Level world, int posX, int posZ, Random random);

}
