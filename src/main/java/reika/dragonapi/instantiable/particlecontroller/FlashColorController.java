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

import reika.dragonapi.instantiable.formula.MathExpression;
import reika.dragonapi.interfaces.ColorController;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;
import net.minecraft.world.entity.Entity;

public class FlashColorController implements ColorController {

	public final int baseColor;
	public final int flashColor;
	private final MathExpression mixFactor;

	public FlashColorController(MathExpression e, int c, int c2) {
		mixFactor = e;
		baseColor = c;
		flashColor = c2;
	}

	@Override
	public void update(Entity e) {

	}

	@Override
	public int getColor(Entity e) {
		float f = Math.abs((float) mixFactor.evaluate(System.currentTimeMillis() / 200D));
		//ReikaJavaLibrary.pConsole(e.ticksExisted+">"+f);
		return ReikaColorAPI.mixColors(baseColor, flashColor, f);
	}

}
