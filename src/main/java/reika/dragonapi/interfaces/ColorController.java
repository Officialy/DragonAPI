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

public interface ColorController extends ParticleController {

    int getColorFor(ControllerTarget target);
    default int getColor(Entity entity) { return getColorFor(ControllerTarget.of(entity)); }
}
