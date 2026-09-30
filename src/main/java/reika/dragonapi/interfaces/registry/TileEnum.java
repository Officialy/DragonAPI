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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface TileEnum {

    Class<? extends BlockEntity> getTEClass();

    String getName();

    BlockState getBlockState();

    /** 1.7.10 {@code getCraftedProduct}: the item this tile is crafted as; by default its block's item. */
    default ItemStack getCraftedProduct() {
        return new ItemStack(this.getBlockState().getBlock());
    }

}
