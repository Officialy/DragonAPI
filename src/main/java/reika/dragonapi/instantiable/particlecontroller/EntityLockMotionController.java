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


public class EntityLockMotionController implements MotionController {

	private final Entity target;

	private final double damping;

	private final double acceleration;
	private double velocityXZ = 0;

	private double accelerationY = 0;
	private double maxVelocityY;
	private double velocityY;

	public EntityLockMotionController(Entity e, double axz, double vy, double damping) {
		target = e;
		this.damping = damping;
		acceleration = axz;
		maxVelocityY = vy;
		velocityY = maxVelocityY;
	}

	public void updateTarget(ControllerTarget e) {
		accelerationY = -1 * 0.125 * (e.getY() - target.getY() - 0.5);
		velocityY += accelerationY;
		velocityY = Mth.clamp(velocityY, -maxVelocityY, maxVelocityY);
		maxVelocityY *= damping;
		velocityXZ += acceleration;
	}

	@Override
	public double getMotionXFor(ControllerTarget e) {
		return -(e.getX() - target.getX()) * velocityXZ / e.distanceTo(target);
	}

	@Override
	public double getMotionYFor(ControllerTarget e) {
		return -(e.getY() - target.getY() - target.getBbHeight() / 2F) * velocityXZ / e.distanceTo(target);
	}

	@Override
	public double getMotionZFor(ControllerTarget e) {
		return -(e.getZ() - target.getZ()) * velocityXZ / e.distanceTo(target);
	}

}
