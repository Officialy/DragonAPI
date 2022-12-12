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

import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import net.minecraft.world.phys.Vec3;

public class Ray {

    public  DecimalPosition origin;
    public  Vec3 directionStep;

    public Ray(DecimalPosition c, double dx, double dy, double dz) {
        this(c, new Vec3(dx, dy, dz));
    }

    public Ray(DecimalPosition c, Vec3 vec) {
        origin = c;
        directionStep = vec;
    }

    public static Ray fromPolar(DecimalPosition c, double theta, double phi) {
        double[] xyz = ReikaPhysicsHelper.polarToCartesian(1, theta, phi);
        return new Ray(c, xyz[0], xyz[1], xyz[2]);
    }

    public DecimalPosition getScaledPosition(double d) {
        return origin.offset(directionStep.x * d, directionStep.y * d, directionStep.z * d);
    }

    @Override
    public String toString() {
        return origin + " > " + directionStep.toString();
    }

}
