/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.particlecontroller;

import reika.dragonapi.instantiable.rendering.ColorBlendList;
import reika.dragonapi.interfaces.ColorController;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;

public class BlendListColorController implements ColorController {

	private final ColorBlendList colors;

	public BlendListColorController(ColorBlendList cbl) {
		colors = cbl;
	}

	@Override
	public void updateTarget(ControllerTarget e) {

	}

	@Override
	public int getColorFor(ControllerTarget e) {
		return colors.getColor(e.age());
	}

}
