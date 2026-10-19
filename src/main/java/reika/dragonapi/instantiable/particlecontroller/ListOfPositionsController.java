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
import net.minecraft.world.entity.Entity;
import reika.dragonapi.interfaces.ControllerTarget;

import java.util.List;

public class ListOfPositionsController implements PositionController {

	public final int entityLife;
	private final List<DecimalPosition> points;
	private int tick = 0;

	public ListOfPositionsController(int l, List<DecimalPosition> li) {
		entityLife = l;
		points = li;
	}

	public ListOfPositionsController setTick(int tick) {
		this.tick = tick;
		return this;
	}

	@Override
	public void updateTarget(ControllerTarget e) {
		tick++;
	}

	private int getIndex(ControllerTarget e) {
		int t = tick * points.size() / entityLife;
		return Math.min(t, points.size() - 1);
	}

	@Override
	public double getPositionXFor(ControllerTarget e) {
		return points.get(this.getIndex(e)).xCoord();
	}

	@Override
	public double getPositionYFor(ControllerTarget e) {
		return points.get(this.getIndex(e)).yCoord();
	}

	@Override
	public double getPositionZFor(ControllerTarget e) {
		return points.get(this.getIndex(e)).zCoord();
	}

}
