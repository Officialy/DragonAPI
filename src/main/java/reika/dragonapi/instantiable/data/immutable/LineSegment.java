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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import reika.dragonapi.libraries.ReikaDirectionHelper;

public final class LineSegment {

	public final BlockPos origin;
	public final BlockPos target;

	public LineSegment(BlockPos pos, BlockPos pos2) {
		origin = pos.immutable();
		target = pos2.immutable();
	}

	public static final LineSegment getFromXYZDir(BlockPos pos, Direction dir, int len) {
		return new LineSegment(pos, new BlockPos(pos.getX() + len * dir.getStepX(), pos.getY() + len * dir.getStepY(), pos.getZ() + len * dir.getStepZ()));
	}

	public static final LineSegment getFromXYZDir(BlockPos pos, ReikaDirectionHelper.CubeDirections dir, double len) {
		return new LineSegment(pos, new BlockPos(Mth.floor(pos.getX() + len * dir.offsetX), pos.getY(), Mth.floor(pos.getZ() + len * dir.offsetZ)));
	}

	public double getLength() {
		return Math.sqrt(target.distSqr(origin)) ;
	}

	@Override
	public String toString() {
		return origin.toString() + " >> " + target.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LineSegment) {
			LineSegment ls = (LineSegment) o;
			return ls.origin.equals(origin) && ls.target.equals(target);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return origin.hashCode() ^ target.hashCode();
	}

	public DecimalLineSegment asDecimalSegment() {
		return new DecimalLineSegment(new DecimalPosition(origin), new DecimalPosition(target));
	}

}
