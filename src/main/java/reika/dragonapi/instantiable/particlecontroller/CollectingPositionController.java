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


import reika.dragonapi.interfaces.PositionController;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;


public class CollectingPositionController implements PositionController {

	public final int duration;

	public final double targetX;
	public final double targetY;
	public final double targetZ;

	public final double startX;
	public final double startY;
	public final double startZ;

	private int tick = 0;

	public CollectingPositionController(double x, double y, double z, double tx, double ty, double tz, int t) {
		startX = x;
		startY = y;
		startZ = z;

		targetX = tx;
		targetY = ty;
		targetZ = tz;

		duration = t;
	}

	@Override
	public void updateTarget(ControllerTarget e) {
		tick++;
	}

	@Override
	public double getPositionXFor(ControllerTarget e) {
		return startX + (targetX - startX) * ((double) tick / duration);
	}

	@Override
	public double getPositionYFor(ControllerTarget e) {
		return startY + (targetY - startY) * ((double) tick / duration);
	}

	@Override
	public double getPositionZFor(ControllerTarget e) {
		return startZ + (targetZ - startZ) * ((double) tick / duration);
	}

}
