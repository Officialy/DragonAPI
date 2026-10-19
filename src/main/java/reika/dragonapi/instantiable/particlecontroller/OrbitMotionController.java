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

import reika.dragonapi.instantiable.Orbit;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.interfaces.PositionController;
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;

public class OrbitMotionController implements PositionController {

	public final Orbit orbit;
	public double thetaSpeed = 1;
	private double originX;
	private double originY;
	private double originZ;
	private double theta;
	private DecimalPosition position;

	private Entity trackedEntity;

	public OrbitMotionController(Orbit o, double x, double y, double z) {
		orbit = o;
		originX = x;
		originY = y;
		originZ = z;
		position = new DecimalPosition(x, y, z);
		position = orbit.getPosition(originX, originY, originZ, theta);
	}

	public OrbitMotionController trackEntity(Entity e) {
		trackedEntity = e;
		originX = e.getX();
		originY = e.getY();
		originZ = e.getZ();
		position = orbit.getPosition(originX, originY, originZ, theta);
		return this;
	}

	@Override
	public void updateTarget(ControllerTarget e) {
		if (trackedEntity != null) {
			originX = trackedEntity.getX();
			originY = trackedEntity.getY();
			originZ = trackedEntity.getZ();
		}
		//ReikaJavaLibrary.pConsole(this.hashCode()+": "+new DecimalPosition(originX, originY, originZ).equals(new DecimalPosition(trackedEntity))+": "+new DecimalPosition(originX, originY, originZ)+","+new DecimalPosition(trackedEntity));
		position = orbit.getPosition(originX, originY, originZ, theta);
		theta += thetaSpeed;
	}

	@Override
	public double getPositionXFor(ControllerTarget e) {
		return position.xCoord();
	}

	@Override
	public double getPositionYFor(ControllerTarget e) {
		return position.yCoord();
	}

	@Override
	public double getPositionZFor(ControllerTarget e) {
		return position.zCoord();
	}

}
