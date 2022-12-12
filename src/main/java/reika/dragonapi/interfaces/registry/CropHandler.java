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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public interface CropHandler {

    boolean isCrop(Block id);

    boolean isRipeCrop(Level world, BlockPos pos);

    void makeRipe(Level world, BlockPos pos);

    int getGrowthState(Level world, BlockPos pos);

    boolean isSeedItem(ItemStack is);

    //public abstract float getSecondSeedDropRate();

    List<ItemStack> getDropsOverride(Level world, BlockPos pos, Block id, int fortune);

    List<ItemStack> getAdditionalDrops(Level world, BlockPos pos, Block id, int fortune);

    void editTileDataForHarvest(Level world, BlockPos pos);

    boolean initializedProperly();

    boolean neverDropsSecondSeed();

    boolean isBlockEntity();

}
