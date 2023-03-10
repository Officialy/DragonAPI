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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * This is an interface for ENUMS!
 */
public interface SoundEnum {

    String getName();

    ResourceLocation getPath();

    SoundSource getCategory();

    int ordinal();

    boolean canOverlap();

    void playSound(Level world, BlockPos pos, float volume, float pitch);

    void playSound(Entity e, float volume, float pitch);

    void playSound(Level world, BlockPos pos, float volume, float pitch, boolean attenuate);

    void playSoundNoAttenuation(Level world, BlockPos pos, float volume, float pitch, int range);

    boolean attenuate();


    /**
     * Use this for clientside volume controls.
     */
    float getModulatedVolume();

    /**
     * Should this audio file be preloaded for real-time playback?
     */
    boolean preload();

}
