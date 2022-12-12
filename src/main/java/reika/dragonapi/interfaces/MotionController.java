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

import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;

public interface MotionController {

    void update(Entity e);

    double getMotionX(Entity e);

    double getMotionY(Entity e);

    double getMotionZ(Entity e);

}
