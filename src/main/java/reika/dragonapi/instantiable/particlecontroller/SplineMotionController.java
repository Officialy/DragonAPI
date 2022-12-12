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

import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.interfaces.PositionController;
import reika.dragonapi.instantiable.math.Spline;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class SplineMotionController implements PositionController {

	public final int entityLife;
	private final Spline spline;
	private int tick = 0;
	private boolean updateSpline = false;
	private List<DecimalPosition> points;

	public SplineMotionController(int l, Spline s) {
		entityLife = l;
		spline = s;
		points = spline.get(32, false);
	}

	public SplineMotionController(double x1, double y1, double z1, double x2, double y2, double z2, int l, double var, double vel) {
		DecimalPosition pos1 = new DecimalPosition(x1, y1, z1);
		DecimalPosition pos2 = new DecimalPosition(x2, y2, z2);

		entityLife = l;

		spline = new Spline(Spline.SplineType.CHORDAL);
		for (double d = 0; d <= 1; d += 0.125) {
			Spline.BasicVariablePoint p = new Spline.BasicVariablePoint(DecimalPosition.interpolate(pos1, pos2, d), var, vel);
			p.tolerance *= 0.03125;
			spline.addPoint(p);
		}
		updateSpline = true;
		points = spline.get(32, false);
	}

	public SplineMotionController setTick(int tick) {
		this.tick = tick;
		return this;
	}

	@Override
	public void update(Entity e) {
		if (updateSpline) {
			spline.update();
			points = spline.get(32, false);
		}
		tick++;
	}

	private int getIndex(Entity e) {
		int t = tick * points.size() / entityLife;
		return Math.max(0, Math.min(t, points.size() - 1));
	}

	@Override
	public double getPositionX(Entity e) {
		return points.get(this.getIndex(e)).xCoord;
	}

	@Override
	public double getPositionY(Entity e) {
		return points.get(this.getIndex(e)).yCoord;
	}

	@Override
	public double getPositionZ(Entity e) {
		return points.get(this.getIndex(e)).zCoord;
	}

}
