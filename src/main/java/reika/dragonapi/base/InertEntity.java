/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.base;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class InertEntity extends Entity {

    public InertEntity(Level par1World) {
        super(EntityType.ARROW, par1World); //TODO Add a new entity type for inert entity

        //noClip = true;
    }

    /**
     * Gets called every tick from main Entity class
     */
    @Override
    public void tick() {
        level.getProfiler().push("entityBaseTick");

        //prevDistanceWalkedModified = distanceWalkedModified;
        xOld = position().x;
        yOld = position().y;
        zOld = position().z;
        //prevRotationPitch = rotationPitch;
        //prevRotationYaw = rotationYaw;

        //portalCounter = 0;

        if (getY() < -64.0D)
            this.kill();

        level.getProfiler().pop(); //endSection();

        //this.move(motionX, motionY, motionZ);

        tickCount++;
    }

    @Override
    public void setRemainingFireTicks(int p_241209_1_) {
        super.setRemainingFireTicks(p_241209_1_);
    }

    @Override
    public int getRemainingFireTicks() {
        return super.getRemainingFireTicks();
    }

    @Override
    public void rideTick() {
        super.rideTick();
    }

    @Override
    protected int getFireImmuneTicks() {
        return super.getFireImmuneTicks();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource p_20122_) {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(""));
    }

    @Override
    public final boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos p_180429_1_, BlockState p_180429_2_) {
    }

}
