/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.registry;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import reika.dragonapi.libraries.java.ReikaRandomHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Random;

public enum ReikaParticleHelper {

    SMOKE(ParticleTypes.SMOKE),
    CRITICAL(ParticleTypes.CRIT),
    ENCHANTMENT(ParticleTypes.ENCHANTED_HIT),
    FLAME(ParticleTypes.FLAME),
    REDSTONE(DustParticleOptions.REDSTONE),
    BONEMEAL(ParticleTypes.HAPPY_VILLAGER),
    BUBBLE(ParticleTypes.BUBBLE),
    //VOID(ParticleTypes.VOID), //TOWN AURA
    LARGESMOKE(ParticleTypes.LARGE_SMOKE),
    SNOWBALL(ParticleTypes.ITEM_SNOWBALL), //todo check if this is right
    PORTAL(ParticleTypes.PORTAL), //and Ender
    RAIN(ParticleTypes.SPLASH),
    DRIPWATER(ParticleTypes.DRIPPING_WATER),
    DRIPLAVA(ParticleTypes.DRIPPING_LAVA),
    EXPLODE(ParticleTypes.EXPLOSION),
    HEART(ParticleTypes.HEART),
    CLOUD(ParticleTypes.CLOUD),
    NOTE(ParticleTypes.NOTE),
    SGA(ParticleTypes.ENCHANT),
    LAVA(ParticleTypes.LAVA),
    //SPRINT(ParticleTypes.FOOTSTEP), this is gone as of 1.13, however it still exists in the jar
    SLIME(ParticleTypes.ITEM_SLIME),
    FIREWORK(ParticleTypes.FIREWORK),
    //SUSPEND(ParticleTypes.SUSPENDED), this is also gone as of 1.13, however it still exists in the jar
    //MOBSPELL(ParticleTypes.MOB_SPELL),
    AMBIENTMOBSPELL(ParticleTypes.AMBIENT_ENTITY_EFFECT),
    SPELL(ParticleTypes.ENTITY_EFFECT),
    INSTANTSPELL(ParticleTypes.INSTANT_EFFECT),
    WITCH(ParticleTypes.WITCH),
    POOF(ParticleTypes.POOF),
    ANGRY(ParticleTypes.ANGRY_VILLAGER);

    public static final ReikaParticleHelper[] particleList = values();
    private static final Random rand = new Random();
    private static final HashMap<ParticleOptions, ReikaParticleHelper> names = new HashMap<>();

    static {
        for (ReikaParticleHelper p : particleList) {
            names.put(p.particle, p);
        }
    }

    public final ParticleOptions particle;

    ReikaParticleHelper(ParticleOptions sg) {
        particle = sg;
    }

    public static ReikaParticleHelper getByString(String name) {
        return names.get(name);
    }

    public static void spawnColoredParticles(Level world, BlockPos pos, double r, double g, double b, int number) {
        REDSTONE.spawnAroundBlock(world, pos, r, g, b, number);
    }

    public static void spawnColoredParticlesWithOutset(Level world, BlockPos pos, double r, double g, double b, int number, double outset) {
        REDSTONE.spawnAroundBlockWithOutset(world, pos, r, g, b, number, outset);
    }

    public static void spawnColoredParticleAt(Level world, double x, double y, double z, int color) {
        REDSTONE.spawnAt(world, x, y, z, ReikaColorAPI.getRed(color) / 255D, ReikaColorAPI.getGreen(color) / 255D, ReikaColorAPI.getBlue(color) / 255D);
    }

    public static void spawnColoredParticleAt(Level world, double x, double y, double z, double r, double g, double b) {
        REDSTONE.spawnAt(world, x, y, z, r, g, b);
    }

    public void spawnAt(Entity e) {
        this.spawnAt(e.level, e.getX(), e.getY(), e.getZ());
    }

    public void spawnAround(Entity e, int n, double r) {
        for (int i = 0; i < n; i++) {
            double dx = ReikaRandomHelper.getRandomPlusMinus(e.position().x, r);
            double dy = ReikaRandomHelper.getRandomPlusMinus(e.position().y, r);
            double dz = ReikaRandomHelper.getRandomPlusMinus(e.position().z, r);
            this.spawnAt(e.level, dx, dy, dz);
        }
    }

    public void spawnAt(Level world, double x, double y, double z, double vx, double vy, double vz) {
        if (particle != DustParticleOptions.REDSTONE) {
            world.addParticle(particle, x, y, z, vx, vy, vz);
        } else {
            var particle = new DustParticleOptions(new Vector3f((float) vx, (float) vy, (float) vz), 1F);
            world.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    public void spawnAt(Level world, double x, double y, double z) {
        this.spawnAt(world, x, y, z, 0, 0, 0);
    }

    public void spawnAroundBlock(Level world, BlockPos pos, int number) {
        this.spawnAroundBlock(world, pos, 0, 0, 0, number);
    }

    public void spawnAroundBlockWithOutset(Level world, BlockPos pos, int number, double outset) {
        this.spawnAroundBlockWithOutset(world, pos, 0, 0, 0, number, outset);
    }

    public void spawnAroundBlock(Level world, BlockPos pos, double vx, double vy, double vz, int number) {
        for (int i = 0; i < number; i++) {
            if (particle != DustParticleOptions.REDSTONE) {
                world.addParticle(particle, pos.getX() + rand.nextDouble(), pos.getY() + rand.nextDouble(), pos.getZ() + rand.nextDouble(), vx, vy, vz);
            } else {
                var particle = new DustParticleOptions(new Vector3f((float) vx, (float) vy, (float) vz), 1F);
                world.addParticle(particle, pos.getX() + rand.nextDouble(), pos.getY() + rand.nextDouble(), pos.getZ() + rand.nextDouble(), vx, vy, vz);
            }
        }

    }

    public void spawnAroundBlockWithOutset(Level world, BlockPos pos, double vx, double vy, double vz, int number, double outset) {
        for (int i = 0; i < number; i++) {
            double rx = ReikaRandomHelper.getRandomPlusMinus(pos.getX() + 0.5, 0.5 + outset);
            double ry = ReikaRandomHelper.getRandomPlusMinus(pos.getY() + 0.5, 0.5 + outset);
            double rz = ReikaRandomHelper.getRandomPlusMinus(pos.getZ() + 0.5, 0.5 + outset);
            if (particle != DustParticleOptions.REDSTONE) {
                world.addParticle(particle, rx, ry, rz, vx, vy, vz);
            } else {
                var particle = new DustParticleOptions(new Vector3f((float) vx, (float) vy, (float) vz), 1F);
                world.addParticle(particle, rx, ry, rz, vx, vy, vz);
            }
        }
    }

}
