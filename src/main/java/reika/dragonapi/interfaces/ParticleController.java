package reika.dragonapi.interfaces;

import net.minecraft.world.entity.Entity;

/** Shared update contract prevents a combined motion/position controller being updated twice. */
public interface ParticleController {
    void updateTarget(ControllerTarget target);
    default void update(Entity entity) { updateTarget(ControllerTarget.of(entity)); }
}
