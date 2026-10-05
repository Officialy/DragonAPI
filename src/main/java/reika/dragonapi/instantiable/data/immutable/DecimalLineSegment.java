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

import net.minecraft.util.Mth;
import reika.dragonapi.libraries.ReikaDirectionHelper;


public record DecimalLineSegment(DecimalPosition origin, DecimalPosition target) {

    private static final double EPSILON = 1e-12;
    private static final double INTERSECTION_TOLERANCE = 1e-6;

    public DecimalLineSegment(double x, double y, double z, double x2, double y2, double z2) {
        this(new DecimalPosition(x, y, z), new DecimalPosition(x2, y2, z2));
    }

    public DecimalLineSegment {
        origin = new DecimalPosition(origin);
        target = new DecimalPosition(target);
    }

    public static DecimalLineSegment getFromXYZDir(double x1, double y1, double z1, Direction dir, double len) {
        return new DecimalLineSegment(x1, y1, z1, x1 + len * dir.getStepX(), y1 + len * dir.getStepY(), z1 + len * dir.getStepZ());
    }

    public static DecimalLineSegment getFromXYZDir(double x1, double y1, double z1, ReikaDirectionHelper.CubeDirections dir, double len) {
        return new DecimalLineSegment(x1, y1, z1, Mth.floor(x1 + len * dir.offsetX), y1, Mth.floor(z1 + len * dir.offsetZ));
    }

    public double getLength() {
        return target.getDistanceTo(origin);
    }

    @Override
    public String toString() {
        return origin.toString() + " >> " + target.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DecimalLineSegment(DecimalPosition origin1, DecimalPosition target1)) {
            return origin1.equals(origin) && target1.equals(target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return origin.hashCode() ^ target.hashCode();
    }

    public DecimalPosition findIntersection(LineSegment l) {
        return this.findIntersection(l.asDecimalSegment());
    }

    /**
     * Full 3D segment-segment intersection
     * Finds the closest points between the two segments and returns their midpoint
     * if they are within {@link #INTERSECTION_TOLERANCE}, else null.
     * Collinear overlapping segments return one point inside the overlap.
     */
    public DecimalPosition findIntersection(DecimalLineSegment l) {
        double d1x = target.xCoord() - origin.xCoord();
        double d1y = target.yCoord() - origin.yCoord();
        double d1z = target.zCoord() - origin.zCoord();
        double d2x = l.target.xCoord() - l.origin.xCoord();
        double d2y = l.target.yCoord() - l.origin.yCoord();
        double d2z = l.target.zCoord() - l.origin.zCoord();
        double rx = origin.xCoord() - l.origin.xCoord();
        double ry = origin.yCoord() - l.origin.yCoord();
        double rz = origin.zCoord() - l.origin.zCoord();

        double a = d1x * d1x + d1y * d1y + d1z * d1z; //squared length of this
        double e = d2x * d2x + d2y * d2y + d2z * d2z; //squared length of l
        double f = d2x * rx + d2y * ry + d2z * rz;

        double s; //parameter along this
        double t; //parameter along l
        if (a <= EPSILON && e <= EPSILON) { //both are points
            s = 0;
            t = 0;
        }
        else if (a <= EPSILON) { //this is a point
            s = 0;
            t = Mth.clamp(f / e, 0, 1);
        }
        else {
            double c = d1x * rx + d1y * ry + d1z * rz;
            if (e <= EPSILON) { //l is a point
                t = 0;
                s = Mth.clamp(-c / a, 0, 1);
            }
            else {
                double b = d1x * d2x + d1y * d2y + d1z * d2z;
                double denom = a * e - b * b;
                s = denom > EPSILON ? Mth.clamp((b * f - c * e) / denom, 0, 1) : 0; //parallel -> pick an arbitrary s, t fixes it
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Mth.clamp(-c / a, 0, 1);
                }
                else if (t > 1) {
                    t = 1;
                    s = Mth.clamp((b - c) / a, 0, 1);
                }
            }
        }

        double p1x = origin.xCoord() + d1x * s;
        double p1y = origin.yCoord() + d1y * s;
        double p1z = origin.zCoord() + d1z * s;
        double p2x = l.origin.xCoord() + d2x * t;
        double p2y = l.origin.yCoord() + d2y * t;
        double p2z = l.origin.zCoord() + d2z * t;
        double dx = p1x - p2x;
        double dy = p1y - p2y;
        double dz = p1z - p2z;
        if (dx * dx + dy * dy + dz * dz > INTERSECTION_TOLERANCE * INTERSECTION_TOLERANCE) //skew or disjoint
            return null;
        return new DecimalPosition((p1x + p2x) / 2, (p1y + p2y) / 2, (p1z + p2z) / 2);
    }

    public DecimalPosition findIntersection2D(DecimalLineSegment l) {
        double x1 = origin.xCoord();
        double x2 = target.xCoord();
        double x3 = l.origin.xCoord();
        double x4 = l.target.xCoord();
        double y1 = origin.zCoord();
        double y2 = target.zCoord();
        double y3 = l.origin.zCoord();
        double y4 = l.target.zCoord();
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (denom == 0) //parallel
            return null;
        double px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom;
        double pz = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom;
        if ((px < origin.xCoord() && px < target.xCoord()) || (px > origin.xCoord() && px > target.xCoord())) //intersection is outside lines
            return null;
        if ((pz < origin.zCoord() && pz < target.zCoord()) || (pz > origin.zCoord() && pz > target.zCoord()))
            return null;
        if ((px < l.origin.xCoord() && px < l.target.xCoord()) || (px > l.origin.xCoord() && px > l.target.xCoord()))
            return null;
        if ((pz < l.origin.zCoord() && pz < l.target.zCoord()) || (pz > l.origin.zCoord() && pz > l.target.zCoord()))
            return null;
        return new DecimalPosition(px, 0, pz);
    }

}
