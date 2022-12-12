/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnhancedFluid extends Fluid {

    private String ingame;
    private int color;

    public EnhancedFluid setGameName(String name) {
        ingame = name;
        return this;
    }

    public EnhancedFluid setColor(int rgb) {
        color = rgb;
        return this;
    }

    @Override
    public Item getBucket() {
        return null;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState p_76127_, BlockGetter p_76128_, BlockPos p_76129_, Fluid p_76130_, Direction p_76131_) {
        return false;
    }

    @Override
    protected Vec3 getFlow(BlockGetter p_76110_, BlockPos p_76111_, FluidState p_76112_) {
        return null;
    }

    @Override
    public int getTickDelay(LevelReader p_76120_) {
        return 0;
    }

    @Override
    protected float getExplosionResistance() {
        return 0;
    }

    @Override
    public float getHeight(FluidState p_76124_, BlockGetter p_76125_, BlockPos p_76126_) {
        return 0;
    }

    @Override
    public float getOwnHeight(FluidState p_76123_) {
        return 0;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState p_76136_) {
        return null;
    }

    @Override
    public boolean isSource(FluidState p_76140_) {
        return false;
    }

    @Override
    public int getAmount(FluidState p_76141_) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState p_76137_, BlockGetter p_76138_, BlockPos p_76139_) {
        return null;
    }
}
