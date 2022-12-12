/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.math;

import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;
import net.minecraft.util.Mth;

import java.util.Random;


public class LobulatedCurve {

	private static final Random delegateRand = new Random();
	public final double amplitudeVariation;
	public final int degree;
	public final double angleStep;
	public final double minRadius;
	private final double[] radii;

	private double clampVar;
	private double clampMin;
	private double clampMax;

	public LobulatedCurve(double r, double a, int d) {
		this(r, a, d, 0.25);
	}

	public LobulatedCurve(double r, double a, int d, double da) {
		minRadius = r;
		amplitudeVariation = a;
		degree = d;
		angleStep = da;

		if (degree * amplitudeVariation >= minRadius)
			throw new IllegalArgumentException("Radius variation larger than base radius!");

		radii = new double[(int) (360 / da)];
	}

	public static LobulatedCurve fromMinMaxRadii(double min, double max, int d) {
		return fromMinMaxRadii(min, max, d, false);
	}

	public static LobulatedCurve fromMinMaxRadii(double min, double max, int d, boolean clamp) {
		double diff = (max - min) / 2D;
		LobulatedCurve ret = new LobulatedCurve(min + diff, diff / d, d);
		if (clamp) {
			ret = ret.setClamped(min, max);
		}
		return ret;
	}

	public LobulatedCurve setClamped(double min, double max) {
		clampVar = (max - min) / 2;
		clampMin = min;
		clampMax = max;
		return this;
	}

	public LobulatedCurve generate() {
		return this.generate(delegateRand);
	}

	public LobulatedCurve generate(Random rand) {
		double[] amps = new double[degree];
		for (int i = 0; i < degree; i++) {
			amps[i] = rand.nextDouble() * amplitudeVariation;
			if (clampVar > 0) {
				amps[i] = clampVar * (1 - i / (double) degree);
			}
		}
		double phase = rand.nextDouble() * 360;
		for (int i = 0; i < radii.length; i++) {
			double theta = i * angleStep;
			double r = minRadius;
			for (int k = 0; k < degree; k++) {
				r += amps[k] * Math.sin(Math.toRadians(phase + k * theta));
			}
			if (clampVar > 0) {
				r = Mth.clamp(r, clampMin, clampMax);
			}
			radii[i] = r;
		}
		return this;
	}

	public double getRadius(double ang) {
		double didx = ((ang % 360) + 360) % 360 / angleStep;
		int idx = (int) didx;
		return ReikaMathLibrary.linterpolate(didx, idx, idx + 1, radii[idx], radii[(idx + 1) % radii.length]);
	}

	public boolean isPointInsideCurve(double x, double z) {
		double[] arr = ReikaPhysicsHelper.cartesianToPolar(x, 0, z);
		double ang = arr[2];
		ang %= 360;
		if (ang < 0)
			ang += 360;
		ang = ReikaMathLibrary.roundToNearestFraction(ang, angleStep);
		return arr[0] <= this.getRadius(ang);
	}

}
