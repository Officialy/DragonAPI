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
	public void update(Entity e) {
		tick++;
	}

	private int getIndex(Entity e) {
		int t = tick * points.size() / entityLife;
		return Math.min(t, points.size() - 1);
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
