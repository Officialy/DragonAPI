/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.mathsci;

public class ReikaThermoHelper {

	public static final double WATER_HEAT = 4.18;
	public static final double GRAPHITE_HEAT = 0.71;

	public static final double STEEL_HEAT = 0.481;
	public static final double COPPER_HEAT = 0.386;

	public static final double SODIUM_HEAT = 1.23;
	public static final double CO2_HEAT = 1.168;
	public static final double LIFBE_HEAT = 1.102;
	public static final double OXYGEN_HEAT = 0.92;
	public static final double NITROGEN_HEAT = 1.04;

	public static final double WATER_BOIL_ENTHALPY = 2260; // kJ/kg
	public static final double OXYGEN_BOIL_ENTHALPY = 3.41 / 32; // kJ/kg
	public static final double NITROGEN_BOIL_ENTHALPY = 2.793; // kJ/kg

	/**
	 * Heat energy in one block
	 */
	public static final double WATER_BLOCK_HEAT = 4.18e6;

	public static final long ROCK_MELT_ENERGY = (long) 5.2e6;

	/**
	 * E = mc delta T
	 */
	public static double getTemperatureIncrease(double C, double M, double E) {
		return E / (M * C);
	}
}
