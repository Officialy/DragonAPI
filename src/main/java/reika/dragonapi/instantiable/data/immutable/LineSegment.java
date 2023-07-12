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
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import reika.dragonapi.libraries.ReikaDirectionHelper;

public final class LineSegment {

    public final Vec3 origin;
    public final Vec3 target;

    public LineSegment(Vec3 pos, Vec3 pos2) {
        origin = pos;
        target = pos2;
    }

    public static LineSegment getFromXYZDir(Vec3 pos, Direction dir, int len) {
        return new LineSegment(pos, new Vec3(pos.x() + len * dir.getStepX(), pos.y() + len * dir.getStepY(), pos.z() + len * dir.getStepZ()));
    }

    public static LineSegment getFromXYZDir(Vec3 pos, ReikaDirectionHelper.CubeDirections dir, double len) {
        return new LineSegment(pos, new Vec3(Mth.floor(pos.x() + len * dir.offsetX), pos.y(), Mth.floor(pos.z() + len * dir.offsetZ)));
    }

    public double getLength() {
        return Math.sqrt(distSqr(target, origin.x(), origin.y(), origin.z));
    }

    public double distSqr(Vec3 vec, double p_203203_, double p_203204_, double p_203205_) {
        double d0 = vec.x() - p_203203_;
        double d1 = vec.y() - p_203204_;
        double d2 = vec.z() - p_203205_;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    @Override
    public String toString() {
        return origin.toString() + " >> " + target.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LineSegment ls) {
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
