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

public interface PositionController extends ParticleController {

    double getPositionXFor(ControllerTarget target);
    default double getPositionX(Entity entity) { return getPositionXFor(ControllerTarget.of(entity)); }
    double getPositionYFor(ControllerTarget target);
    default double getPositionY(Entity entity) { return getPositionYFor(ControllerTarget.of(entity)); }
    double getPositionZFor(ControllerTarget target);
    default double getPositionZ(Entity entity) { return getPositionZFor(ControllerTarget.of(entity)); }
}
