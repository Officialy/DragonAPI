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
import net.minecraft.resources.Identifier;
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
        

        //prevDistanceWalkedModified = distanceWalkedModified;
        xOld = position().x;
        yOld = position().y;
        zOld = position().z;
        //prevRotationPitch = rotationPitch;
        //prevRotationYaw = rotationYaw;

        //portalCounter = 0;

        if (getY() < -64.0D)
            this.discard();

        

        //this.move(motionX, motionY, motionZ);

        tickCount++;
    }

    
    public void setRemainingFireTicks(int p_241209_1_) {
        super.setRemainingFireTicks(p_241209_1_);
    }

    
    public int getRemainingFireTicks() {
        return super.getRemainingFireTicks();
    }

    
    public void rideTick() {
        super.rideTick();
    }

    
    protected int getFireImmuneTicks() {
        return super.getFireImmuneTicks();
    }

    
    public boolean isInvulnerableTo(DamageSource p_20122_) {
        return true;
    }

    
    public boolean isPushedByFluid() {
        return false;
    }

    
    public boolean isAttackable() {
        return false;
    }

    
    public boolean isPushable() {
        return false;
    }

    
    protected SoundEvent getSwimSplashSound() {
        return SoundEvent.createVariableRangeEvent(Identifier.parse(""));
    }

    
    public final boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos p_180429_1_, BlockState p_180429_2_) {
    }

}
