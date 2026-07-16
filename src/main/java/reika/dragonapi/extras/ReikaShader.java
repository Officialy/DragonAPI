/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.extras;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import reika.dragonapi.DragonAPI;
import reika.dragonapi.extras.shader.ReikaShaderSystem;
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reika's motion trail: when Reika's own player moves quickly, the positions they pass through drag
 * and tint the screen behind them. The rendering belongs to {@link ReikaShaderSystem}; this class is
 * only the trail — where the points are, how strong each one is, and when they die.
 *
 * <p>Ported from the 1.7.10 class of the same name; the behaviour is unchanged. A point is dropped
 * at the player's position each tick, points live 30 ticks and fade as they age, a point's strength
 * comes from how fast the player was moving when it was dropped (so walking shows nothing and only
 * real speed lights it up), and the overall effect eases in and out rather than popping.</p>
 */
@EventBusSubscriber(modid = DragonAPI.MODID, value = Dist.CLIENT)
public final class ReikaShader {

    /** Ticks a trail point lives for. */
    private static final int LIFESPAN = 30;

    /** Below this speed the effect eases out rather than in. */
    private static final double MOVING_SPEED = 0.08;

    /** Cancels out a tick of gravity, so simply falling does not read as movement. */
    private static final double GRAVITY_PER_TICK = 0.0784000015258789;

    private static final List<ShaderPoint> POINTS = new ArrayList<>();

    /** The eased overall strength; ramps up while moving and decays when not. */
    private static float intensity;

    private ReikaShader() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player ep = event.getEntity();
        if (!ep.level().isClientSide() || !ReikaPlayerAPI.isReika(ep))
            return;
        updatePosition(ep);
        postFoci();
    }

    private static void updatePosition(Player ep) {
        long time = ep.level().getGameTime();
        Vec3 position = ep.position();

        boolean isNew = true;
        Iterator<ShaderPoint> it = POINTS.iterator();
        while (it.hasNext()) {
            ShaderPoint p = it.next();
            if (p.position.equals(position)) {
                p.refresh(time);
                isNew = false;
            }
            if (p.tick(time))
                it.remove();
        }

        if (isNew)
            POINTS.add(0, new ShaderPoint(ep, POINTS.isEmpty() ? null : POINTS.get(0)));
    }

    /**
     * Hand the live points to the shader system. Points are posted every tick rather than every
     * frame; the system holds a point for longer than a tick, so the trail stays continuous.
     */
    private static void postFoci() {
        // Ease the overall strength based on whether the newest point was laid down at speed, so the
        // effect builds as Reika accelerates and trails off on stopping.
        boolean moving = !POINTS.isEmpty() && POINTS.get(0).speed > MOVING_SPEED;
        intensity = moving ? Math.min(1, intensity * 1.02F + 0.005F) : Math.max(0, intensity * 0.99F - 0.02F);
        if (intensity <= 0)
            return;

        for (ShaderPoint p : POINTS) {
            float f = p.getIntensity();
            if (f > 0)
                ReikaShaderSystem.addFocus(p.position, f, intensity);
        }
    }

    private static final class ShaderPoint {

        private final Vec3 position;
        private final double speed;

        private long creation;
        private int age;

        private ShaderPoint(Player ep, ShaderPoint last) {
            position = ep.position();
            creation = ep.level().getGameTime();
            Vec3 motion = ep.getDeltaMovement();
            double v = new Vec3(motion.x, motion.y + GRAVITY_PER_TICK, motion.z).length();
            // Teleports and other jumps never show up in the motion vector, so the gap since the
            // previous point counts toward the speed too.
            if (last != null)
                v += last.position.distanceTo(position);
            speed = v;
        }

        void refresh(long time) {
            age = 0;
            creation = time;
        }

        /** @return true once the point has aged out and should be dropped */
        boolean tick(long time) {
            age++;
            return Math.max(age, time - creation) >= LIFESPAN;
        }

        private float getAgeFactor() {
            return 1F - age / (float) LIFESPAN;
        }

        float getIntensity() {
            return (float) Math.min(1, Math.pow(this.getAgeFactor() * speed * 5, 1.5));
        }
    }
}
