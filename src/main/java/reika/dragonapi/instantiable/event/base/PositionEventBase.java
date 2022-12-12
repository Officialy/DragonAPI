/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2018
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.event.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;

public abstract class PositionEventBase extends Event {

    public final BlockGetter access;
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;

    /**
     * Whether the world is a half-assed fake clone missing most of its data, and probably would crash if most of its functions are called.
     */


    public PositionEventBase(BlockGetter world, BlockPos pos) {
        access = world;
        xCoord = pos.getX();
        yCoord = pos.getY();
        zCoord = pos.getZ();

    }

    public final Block getBlock() {
        return this.getBlock(0, 0, 0);
    }

    public final Block getBlock(int dx, int dy, int dz) {
        return access.getBlockState(new BlockPos(xCoord + dx, yCoord + dy, zCoord + dz)).getBlock();
    }

    public final Block getBlock(BlockPos pos) {
        return this.getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public final BlockEntity getBlockEntity() {
        return access.getBlockEntity(new BlockPos(xCoord, yCoord, zCoord));
    }

    public final int getLightLevel() {
        return access.getLightEmission(new BlockPos(xCoord, yCoord, zCoord));
    }

}
