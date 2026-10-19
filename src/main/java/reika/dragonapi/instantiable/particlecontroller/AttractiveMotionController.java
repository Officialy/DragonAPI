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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;
import net.minecraft.world.level.block.entity.BlockEntity;


public class AttractiveMotionController implements MotionController {

	public final double targetX;
	public final double targetY;
	public final double targetZ;

	private final double damping;

	private final double acceleration;
	private double velocityXZ = 0;

	private double accelerationY = 0;
	private double maxVelocityY;
	private double velocityY;

	public AttractiveMotionController(BlockEntity te, double axz, double vy, double damping) {
		this(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 0.5, te.getBlockPos().getZ() + 0.5, axz, vy, damping);
	}

	public AttractiveMotionController(double x, double y, double z, double axz, double vy, double damping) {
		targetX = x;
		targetY = y;
		targetZ = z;
		this.damping = damping;
		acceleration = axz;
		maxVelocityY = vy;
		velocityY = maxVelocityY;
	}

	public void updateTarget(ControllerTarget e) {
		accelerationY = -1 * 0.125 * (e.getY() - targetY - 0.5);
		velocityY += accelerationY;
		velocityY = Mth.clamp(velocityY, -maxVelocityY, maxVelocityY);
		maxVelocityY *= damping;
		velocityXZ += acceleration;
	}

	@Override
	public double getMotionXFor(ControllerTarget e) {
		return -(e.getX() - targetX) * velocityXZ / e.distanceTo(targetX, targetY, targetZ);
	}

	@Override
	public double getMotionYFor(ControllerTarget e) {
		return velocityY;
	}

	@Override
	public double getMotionZFor(ControllerTarget e) {
		return -(e.getZ() - targetZ) * velocityXZ / e.distanceTo(targetX, targetY, targetZ);
	}

}
