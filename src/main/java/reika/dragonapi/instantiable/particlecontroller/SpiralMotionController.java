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
import reika.dragonapi.interfaces.PositionController;
import net.minecraft.world.entity.Entity;


public class SpiralMotionController implements PositionController, MotionController {

	public final double posX;
	public final double posZ;
	public final double originAngle;
	public final double originRadius;

	public final double angleVelocity;
	public final double verticalVelocity;
	public final double radiusVelocity;

	private double angle;
	private double radius;

	public SpiralMotionController(double x, double z, double a, double v, double r, double rv) {
		this(x, z, a, v, r, rv, 0);
	}

	public SpiralMotionController(double x, double z, double a, double v, double r, double rv, double o) {
		angleVelocity = a;
		verticalVelocity = v;
		originRadius = r;
		radius = originRadius;
		radiusVelocity = rv;
		posX = x;
		posZ = z;

		originAngle = o;
		angle = originAngle;
	}

	@Override
	public void update(Entity e) {
		angle += angleVelocity;
		radius += radiusVelocity;
	}

	@Override
	public double getMotionX(Entity e) {
		return e.getMotionDirection().getStepX();
	}

	@Override
	public double getMotionY(Entity e) {
		return verticalVelocity;
	}

	@Override
	public double getMotionZ(Entity e) {
		return e.getMotionDirection().getStepZ();
	}

	@Override
	public double getPositionX(Entity e) {
		return posX + radius * Math.cos(Math.toRadians(angle));
	}

	@Override
	public double getPositionY(Entity e) {
		return e.getY();
	}

	@Override
	public double getPositionZ(Entity e) {
		return posZ + radius * Math.sin(Math.toRadians(angle));
	}

}
