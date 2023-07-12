package reika.dragonapi.libraries.mathsci;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.math.DoubleMatrix;
import reika.dragonapi.libraries.java.ReikaArrayHelper;
import reika.dragonapi.instantiable.math.LineClipper;

import java.awt.*;
import java.util.HashSet;

public class ReikaVectorHelper {

    /**
     * Returns a standard Vec3 between two specified points, rather than from the origin.
     * Args: start x,y,z, end x,y,z
     */
    public static Vec3 getVec2Pt(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Vec3(x2 - x1, y2 - y1, z2 - z1);
    }

    public static Vec3 subtract(Vec3 p1, Vec3 p2) {
        return new Vec3(p2.x() - p1.x(), p2.y() - p1.y(), p2.z() - p1.z());
    }

    /**
     * Breaks a vector into a size-3 array of its components. Args: Vector
     */
    public static double[] components(Vec3 vec) {
        double[] xyz = new double[3];
        xyz[0] = vec.x();
        xyz[1] = vec.y();
        xyz[2] = vec.z();
        return xyz;
    }

    public static DecimalPosition getPlayerLookCoords(Player ep, double distance) {
        Vec3 look = ep.getLookAngle();
        double dx = ep.getX();
        double dy = ep.getY() + ep.getEyeHeight();
        double dz = ep.getZ();
        double lx = look.x();
        double lz = look.z();
        double ly = look.y();
        lx *= distance;
        ly *= distance;
        lz *= distance;

        return new DecimalPosition(dx + lx, dy + ly, dz + lz); //todo check if this actually works, hope so xoxo
    }

    /**
     * Extends two vectors to infinity and finds their intersection point. If they are
     * parallel (and thus never cross), it returns +infinity. If they are not parallel
     * but still never cross - one-axis parallel and displaced - it returns
     * -infinity. Args: Vec1, Vec2
     */
    public static double[] findIntersection(Vec3 v1, Vec3 v2) {
        double[] xyz = new double[3];
        if (areParallel(v1, v2))
            return ReikaArrayHelper.fillArray(xyz, Double.POSITIVE_INFINITY);
        if (areNonParallelNonIntersecting(v1, v2))
            return ReikaArrayHelper.fillArray(xyz, Double.NEGATIVE_INFINITY);
        //TODO This code is still being written
        return xyz;
    }

    /**
     * Returns the slope of a vector as da/dl, where a is the specified axis.
     * Returns +infinity if invalid axis. Args: Vector, 0/1/2 for x/y/z
     */
    public static double getSlope(Vec3 vec, int axis) {
        return switch (axis) {
            case 0 -> (vec.x() / vec.length());
            case 1 -> (vec.y() / vec.length());
            case 2 -> (vec.z() / vec.length());
            default -> Double.POSITIVE_INFINITY;
        };
    }

    /**
     * Returns true if two vectors are parallel. Args: Vec1, Vec2
     */
    public static boolean areParallel(Vec3 vec1, Vec3 vec2) {
        for (int i = 0; i < 3; i++)
            if (getSlope(vec1, i) != getSlope(vec2, i))
                return false;
        return true;
    }

    /**
     * Returns true if the two vectors are not parallel but will never intersect due to
     * Being parallel in one axis and displaced.
     */
    public static boolean areNonParallelNonIntersecting(Vec3 vec1, Vec3 vec2) {
        if (areParallel(vec1, vec2))
            return false;
        if (getSlope(vec1, 0) == getSlope(vec2, 0)) {

        }
        //TODO This code is still being written
        return false;
    }

    public static double getDistFromPointToLine(double x1, double y1, double z1, double x2, double y2, double z2, double x, double y, double z) {
        Vec3 v01 = getVec2Pt(x, y, z, x1, y1, z1);
        Vec3 v02 = getVec2Pt(x, y, z, x2, y2, z2);
        Vec3 v21 = getVec2Pt(x2, y2, z2, x1, y1, z1);
        return Math.abs(v01.cross(v02).length() / v21.length());
    }


    public static boolean isPointWithinDistOfLineSegment(double x1, double y1, double z1, double x2, double y2, double z2, double x, double y, double z, double dist) {
        double d01 = ReikaMathLibrary.py3d(x - x1, y - y1, z - z1);
        double d02 = ReikaMathLibrary.py3d(x - x2, y - y2, z - z2);
        double d12 = ReikaMathLibrary.py3d(x1 - x2, y1 - y2, z1 - z2);
        double l1 = ReikaMathLibrary.py3d(d01, dist, 0);
        double l2 = ReikaMathLibrary.py3d(d02, dist, 0);
        return d01 + d02 <= l1 + l2;
    }

    public static Vec3 scaleVector(Vec3 vec, double len) {
        Vec3 ret = vec.normalize();
//        ret.xCoord *= len;
//        ret.yCoord *= len;
//        ret.zCoord *= len;
        return ret;
    }

/*   todo public static Vec3 multiplyVectorByMatrix(Vec3 vector, Matrix4f matrix) {
        float newX = (float) (matrix.m00 * vector.x() + matrix.m01 * vector.y() + matrix.m02 * vector.z() + matrix.m03);
        float newY = (float) (matrix.m10 * vector.x() + matrix.m11 * vector.y() + matrix.m12 * vector.z() + matrix.m13);
        float newZ = (float) (matrix.m20 * vector.x() + matrix.m21 * vector.y() + matrix.m22 * vector.z() + matrix.m23);
        return new Vec3(newX, newY, newZ);
    }

    public static void euler321Sequence(Matrix4f mat, double rx, double ry, double rz) {
        float z = (float) Math.toRadians(rz);
        float y = (float) Math.toRadians(ry);
        float x = (float) Math.toRadians(rx);
        mat.rotZ(z);
        mat.rotY(y);
        mat.rotX(x);
    }

    public static void euler213Sequence(Matrix4f mat, double rx, double ry, double rz) {
        float z = (float) Math.toRadians(rz);
        float y = (float) Math.toRadians(ry);
        float x = (float) Math.toRadians(rx);
        mat.rotZ(z);
        mat.rotY(y);
        mat.rotX(x);
    }*/

    public static Vec3 multiplyVectorByMatrix(Vec3 vector, DoubleMatrix matrix) {
        double newX = matrix.m00 * vector.x() + matrix.m01 * vector.y() + matrix.m02 * vector.z() + matrix.m03;
        double newY = matrix.m10 * vector.x() + matrix.m11 * vector.y() + matrix.m12 * vector.z() + matrix.m13;
        double newZ = matrix.m20 * vector.x() + matrix.m21 * vector.y() + matrix.m22 * vector.z() + matrix.m23;
        return new Vec3(newX, newY, newZ);
    }

    public static void euler321Sequence(DoubleMatrix mat, double rx, double ry, double rz) {
        double z = Math.toRadians(rz);
        double y = Math.toRadians(ry);
        double x = Math.toRadians(rx);
        mat.rotate(z, new Vec3(0, 0, 1)).rotate(y, new Vec3(0, 1, 0)).rotate(x, new Vec3(1, 0, 0));
    }

    public static void euler213Sequence(DoubleMatrix mat, double rx, double ry, double rz) {
        double z = Math.toRadians(rz);
        double y = Math.toRadians(ry);
        double x = Math.toRadians(rx);
        mat.rotate(y, new Vec3(0, 1, 0)).rotate(x, new Vec3(1, 0, 0)).rotate(z, new Vec3(0, 0, 1));
    }

    public static Vec3 getXYProjection(Vec3 vec) {
        return new Vec3(vec.x(), vec.y(), 0);
    }

    public static Vec3 getYZProjection(Vec3 vec) {
        return new Vec3(0, vec.y(), vec.z());
    }

    public static Vec3 getXZProjection(Vec3 vec) {
        return new Vec3(vec.x(), 0, vec.z());
    }

    public static Vec3 getInverseVector(Vec3 vec) {
        return new Vec3(-vec.x(), -vec.y(), -vec.z());
    }

    public static Vec3 rotateVector(Vec3 vec, double rx, double ry, double rz) {
        DoubleMatrix mat = new DoubleMatrix();
        euler321Sequence(mat, rx, ry, rz);
        return multiplyVectorByMatrix(vec, mat);
    }

    public static HashSet<BlockPos> getCoordsAlongVector(int x1, int y1, int z1, int x2, int y2, int z2) {
        HashSet<BlockPos> set = new HashSet<>();
        int dd = (int) ReikaMathLibrary.py3d(x2 - x1, y2 - y1, z2 - z1); //todo check if this works
        for (int d = 0; d <= dd; d += 0.25) {
            int f = d / dd;
            int dx = x1 + f * (x2 - x1);
            int dy = y1 + f * (y2 - y1);
            int dz = z1 + f * (z2 - z1);
            BlockPos c = new BlockPos(dx, dy, dz);
            set.add(c);
        }
        return set;
    }

    /**
     * Returns null if no part of the line falls within the clipping box. Uses the Cohen Sutherland Method.
     */
    public static ImmutablePair<Point, Point> clipLine(int x0, int x1, int y0, int y1, int minX, int minY, int maxX, int maxY) {
        return new LineClipper(minX, minY, maxX, maxY).clip(x0, y0, x1, y1);
    }

    /**
     * Gets the angle between two vectors.
     */
    public static double getAngleBetween(Vec3 v1, Vec3 v2) {
        return Math.toDegrees(Math.acos(v1.dot(v2) / (v1.length() * v2.length())));
    }

    public static double getAngleDifference(double a1, double a2) {
        double difference = a2 - a1;
        while (difference < -180)
            difference += 360;
        while (difference > 180)
            difference -= 360;
        return difference;
    }

    /**
     * Will return a result relative to the origin of the vector; if 'vec' is a speed vector, that is relative to 0,0,0.
     */
    public static Vec3 getPointAroundVector(Vec3 vec, double r, double ang) { //P(t) = (acost)U + (asint)V + (bt)W
        Vec3 w = vec.normalize();
        Vec3 u = w.cross(new Vec3(1, 0, 0));
        Vec3 v = w.cross(u);
        double a1 = r * Math.cos(Math.toRadians(ang)); //a*Math.cos(t);
        double a2 = r * Math.sin(Math.toRadians(ang)); //a*Math.sin(t);
        double a3 = 0; //bt;
        Vec3 v1 = new Vec3(u.x() * a1, u.y() * a1, u.z() * a1);
        Vec3 v2 = new Vec3(v.x() * a2, v.y() * a2, v.z() * a2);
        Vec3 v3 = new Vec3(w.x() * a3, w.y() * a3, w.z() * a3);
        return new Vec3(v1.x() + v2.x() + v3.x(), v1.y() + v2.y() + v3.y(), v1.z() + v2.z() + v3.z());
    }

}
