/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.interfaces;

import net.minecraft.world.entity.Entity;

public interface MotionController extends ParticleController {

    double getMotionXFor(ControllerTarget target);
    default double getMotionX(Entity entity) { return getMotionXFor(ControllerTarget.of(entity)); }
    double getMotionYFor(ControllerTarget target);
    default double getMotionY(Entity entity) { return getMotionYFor(ControllerTarget.of(entity)); }
    double getMotionZFor(ControllerTarget target);
    default double getMotionZ(Entity entity) { return getMotionZFor(ControllerTarget.of(entity)); }
}
