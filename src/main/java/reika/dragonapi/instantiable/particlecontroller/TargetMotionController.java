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

	public void update(Entity e) {

	}

	@Override
	public double getMotionX(Entity e) {
		return e.getMotionDirection().getStepX() + force * (targetX - e.getX());
	}

	@Override
	public double getMotionY(Entity e) {
		return e.getMotionDirection().getStepY() + force * (targetY - e.getY());
	}

	@Override
	public double getMotionZ(Entity e) {
		return e.getMotionDirection().getStepZ() + force * (targetZ - e.getZ());
	}

}
