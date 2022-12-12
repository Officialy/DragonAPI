/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.immutable;

import net.minecraft.core.Direction;

import java.awt.*;

public final class PointDirection {

	private final Point point;
	public Direction direction;

	public PointDirection(Point pt, Direction dir) {
		direction = dir;
		point = pt;
	}

	public PointDirection(int x, int z, Direction dir) {
		this(new Point(x, z), dir);
	}

	@Override
	public int hashCode() {
		return point.hashCode() ^ direction.ordinal();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PointDirection) {
			PointDirection pd = (PointDirection) o;
			return pd.point.equals(point) && pd.direction == direction;
		}
		return false;
	}

	public Point getPoint() {
		return new Point(point.x, point.y);
	}

}
