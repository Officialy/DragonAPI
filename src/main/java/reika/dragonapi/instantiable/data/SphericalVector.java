/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data;

import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

public class SphericalVector {

    public double magnitude;
    public double inclination;
    public double rotation;

    public SphericalVector(double m, double theta, double phi) {
        magnitude = m;
        inclination = theta;
        rotation = phi;
    }

    public static SphericalVector fromCartesian(double dx, double dy, double dz) {
        SphericalVector ret = new SphericalVector(0, 0, 0);
        ret.aimFrom(dx, dy, dz, 0, 0, 0);
        return ret;
    }

    public double[] getCartesian() {
        return ReikaPhysicsHelper.polarToCartesian(magnitude, inclination, rotation);
    }

    public double getXProjection() {
        return this.getCartesian()[0];
    }

    public double getYProjection() {
        return this.getCartesian()[1];
    }

    public double getZProjection() {
        return this.getCartesian()[2];
    }

    public void aimFrom(double x1, double y1, double z1, double x2, double y2, double z2) {
        double[] dat = ReikaPhysicsHelper.cartesianToPolar(x2 - x1, y2 - y1, z2 - z1);
        magnitude = dat[0];
        inclination = -(dat[1] - 90);
        rotation = -dat[2] - 90;
    }

}
