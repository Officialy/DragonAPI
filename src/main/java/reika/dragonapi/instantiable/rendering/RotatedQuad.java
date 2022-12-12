package reika.dragonapi.instantiable.rendering;

import net.minecraft.world.phys.Vec3;
import reika.dragonapi.libraries.mathsci.ReikaVectorHelper;

import java.util.Arrays;

public class RotatedQuad {

    private final double[][] points = new double[4][2];

    public RotatedQuad(double r1, double r2, double r3, double r4, double rot) {
        double[][] p = new double[][]{
                {-r1, -r1},
                {+r2, -r2},
                {+r3, +r3},
                {-r4, +r4},
        };

        for (int i = 0; i < 4; i++) {
            Vec3 vec3 = new Vec3(p[i][0], 0, p[i][1]);
            vec3 = ReikaVectorHelper.rotateVector(vec3, 0, rot, 0);
            p[i][0] = vec3.x();
            p[i][1] = vec3.z();
        }
        System.arraycopy(p, 0, points, 0, 4);

    }

    public double getPosX(int corner) {
        return points[corner][0];
    }

    public double getPosZ(int corner) {
        return points[corner][1];
    }

    @Override
    public String toString() {
        return Arrays.toString(points);
    }
}
