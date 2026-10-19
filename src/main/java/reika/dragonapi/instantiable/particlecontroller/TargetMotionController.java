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


import reika.dragonapi.interfaces.MotionController;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;
import net.minecraft.world.level.block.entity.BlockEntity;


public class TargetMotionController implements MotionController {

	public final double targetX;
	public final double targetY;
	public final double targetZ;

	private final double force;

	public TargetMotionController(BlockEntity te, double f) {
		this(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 0.5, te.getBlockPos().getZ() + 0.5, f);
	}

	public TargetMotionController(double x, double y, double z, double f) {
		targetX = x;
		targetY = y;
		targetZ = z;
		force = f;
	}

	public void updateTarget(ControllerTarget e) {

	}

	@Override
	public double getMotionXFor(ControllerTarget e) {
		return e.getDeltaMovement().x + force * (targetX - e.getX());
	}

	@Override
	public double getMotionYFor(ControllerTarget e) {
		return e.getDeltaMovement().y + force * (targetY - e.getY());
	}

	@Override
	public double getMotionZFor(ControllerTarget e) {
		return e.getDeltaMovement().z + force * (targetZ - e.getZ());
	}

}
