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

import reika.dragonapi.DragonAPI;
import reika.dragonapi.instantiable.Interpolation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fluids.IFluidBlock;

public final class ReikaPhysicsHelper extends DragonAPI {

	public static final double TNTenergy = 12420000000D;

	public static final double g = 9.81D;
	public static final double LIGHT_SPEED = 299792458D;
	public static final double ELECTRON_CHARGE = 1.602 / ReikaMathLibrary.doubpow(10, 19);
	public static final double ELECTRON_MASS = 9.11 / ReikaMathLibrary.doubpow(10, 31);
	public static final double NEUTRON_MASS = 1.674 / ReikaMathLibrary.doubpow(10, 27);
	public static final double PROTON_MASS = 1.672 / ReikaMathLibrary.doubpow(10, 27);
	private static final double DEG_TO_RAD = Math.PI / 180D;
	private static final double RAD_TO_DEG = 180D / Math.PI;
	private static final Interpolation tempColorList = new Interpolation(true);

	static {
		tempColorList.addPoint(500, 0x000000);
		tempColorList.addPoint(700, 0x9C0000);
		tempColorList.addPoint(1700, 0xff0000);
		tempColorList.addPoint(2700, 0xFF9C00);
		tempColorList.addPoint(3700, 0xFFFF00);
		tempColorList.addPoint(5700, 0xFFFFFF);
		tempColorList.addPoint(9700, 0x56C4FF);
	}

	public static double[] polarToCartesianFast(double mag, double theta, double phi) {
		double[] coords = new double[3];
		theta = degToRad(theta);
		phi = degToRad(phi);
		double ct = Mth.cos((float) theta);
		coords[0] = mag * ct * Mth.cos((float) phi);
		coords[1] = mag * Mth.sin((float) theta);
		coords[2] = mag * ct * Mth.sin((float) phi);
		return coords;
	}

	/**
	 * Converts 3D polar coordinates into cartesian ones. Use angles in degrees. Args: magnitude, theta, phi
	 */
	public static double[] polarToCartesian(double mag, double theta, double phi) {
		double[] coords = new double[3];
		theta = degToRad(theta);
		phi = degToRad(phi);
		coords[0] = mag * Math.cos(theta) * Math.cos(phi);
		coords[1] = mag * Math.sin(theta);
		coords[2] = mag * Math.cos(theta) * Math.sin(phi);
		return coords;
	}

	public static double[] cartesianToPolarFast(double x, double y, double z) {
		double[] coords = new double[3];
		coords[0] = ReikaMathLibrary.py3d(x, y, z); //length
		coords[1] = Math.acos(y / coords[0]);
		coords[2] = Math.atan2(x, z);
		coords[1] = radToDeg(coords[1]);
		coords[2] = 180 + radToDeg(coords[2]);
		return coords;
	}

	/**
	 * Converts 3D cartesian coordinates into polar ones. Returns angles in degrees, mapped 0-360. Args: x, y, z; Returns: Dist, Theta, Phi
	 */
	public static double[] cartesianToPolar(double x, double y, double z) {
		double[] coords = new double[3];
		//boolean is90to270 = false;
		coords[0] = ReikaMathLibrary.py3d(x, y, z); //length
		coords[1] = Math.acos(y / coords[0]);
		coords[2] = Math.atan2(x, z);
		coords[1] = radToDeg(coords[1]);
		coords[2] = 180 + radToDeg(coords[2]);
		//if (is90to270) {
		//	coords[2] *= -1;
		//}
		return coords;
	}

	/**
	 * Converts a degree angle to a radian one. Args: Angle
	 */
	public static double degToRad(double ang) {
		return ang * DEG_TO_RAD;
	}

	/**
	 * Converts a degree angle to a radian one. Args: Angle
	 */
	public static double radToDeg(double ang) {
		return ang * RAD_TO_DEG;
	}

	/**
	 * Calculates the required velocity (in xyz cartesian coordinates) required to travel in
	 * projectile motion from point A to point B. Args: start x,y,z end x,y,z, double g
	 */
	public static double[] targetPosn(double x, double y, double z, double x2, double y2, double z2, double ag) {
		double[] v = new double[3];
		double[] target = {x2, y2, z2};
		double velocity;
		int theta;
		int phi;
		double dx = target[0] - x - 0.5;
		double dy = target[1] - y - 1;
		double dz = target[2] - z - 0.5;
		double dl = ReikaMathLibrary.py3d(dx, 0, dz); //Horiz distance
		double g = 8.4695 * ReikaMathLibrary.doubpow(dl, 0.2701);
		if (dy > 0)
			g *= (0.8951 * ReikaMathLibrary.doubpow(dy, 0.0601));
		velocity = 10;
		theta = 0;
		phi = (int) Math.toDegrees(Math.atan2(dz, dx));
		while (theta <= 0) {
			velocity++;
			double s = ReikaMathLibrary.intpow(velocity, 4) - g * (g * dl * dl + 2 * dy * velocity * velocity);
			double a = velocity * velocity + Math.sqrt(s);
			theta = (int) Math.toDegrees(Math.atan(a / (g * dl)));
			phi = (int) Math.toDegrees(Math.atan2(dz, dx));
		}
		v = polarToCartesian(velocity, theta, phi);
		return v;
	}

	/**
	 * Returns a modified value for the inverse-square law, based on the distance and initial magnitude.
	 * Args: Distance x,y,z, initial magnitude
	 */
	public static double inverseSquare(double dx, double dy, double dz, double mag) {
		return mag / (dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Returns a float value for MC-scaled explosion power, based off the input energy in joules. Recall TNT has
	 * a float power of 4F, corresponding to a real-energy value of 12.4 Gigajoules. Args: Energy
	 */
	public static float getExplosionFromEnergy(double energy) {
		double ratio = energy / TNTenergy;
		return (float) (4 * ratio);
	}

	public static float getEnergyFromExplosion(float ex) {
		return ex / 4F * (float) TNTenergy;
	}

	public static double getBlockDensity(Block b) {
		if (b == Blocks.AIR)
			return 1;
		if (b == Blocks.GOLD_BLOCK)
			return ReikaEngLibrary.rhogold;
		if (b == Blocks.IRON_BLOCK)
			return ReikaEngLibrary.rhoiron;
		if (b == Blocks.DIAMOND_BLOCK)
			return ReikaEngLibrary.rhodiamond;
		if (b == Blocks.EMERALD_BLOCK)
			return 2740;
		if (b == Blocks.LAPIS_BLOCK)
			return 2800;
		if (b == Blocks.GRAVEL)
			return 1680;
		if (b instanceof IFluidBlock)
			return ((IFluidBlock) b).getFluid().getFluidType().getDensity();
		if (b.defaultBlockState().getMaterial() == Material.STONE)
			return ReikaEngLibrary.rhorock;
		if (b.defaultBlockState().getMaterial() == Material.GLASS)
			return ReikaEngLibrary.rhorock;
		if (b.defaultBlockState().getMaterial() == Material.GRASS)
			return 1250;
		if (b.defaultBlockState().getMaterial() == Material.DIRT)
			return 1220;
		if (b.defaultBlockState().getMaterial() == Material.CLAY)
			return 1650;
		if (b.defaultBlockState().getMaterial() == Material.SAND)
			return 1555;
		if (b.defaultBlockState().getMaterial() == Material.WOOD)
			return ReikaEngLibrary.rhowood;
		if (b.defaultBlockState().getMaterial() == Material.LEAVES)
			return 100;
		if (b.defaultBlockState().getMaterial() == Material.SPONGE)
			return 280;
		if (b.defaultBlockState().getMaterial() == Material.PLANT)
			return 100;
		if (b.defaultBlockState().getMaterial() == Material.WATER_PLANT)
			return 100;
		if (b.defaultBlockState().getMaterial() == Material.CLOTH_DECORATION)
			return 1314;
		if (b.defaultBlockState().getMaterial() == Material.METAL)
			return ReikaEngLibrary.rhoiron;
		if (b.defaultBlockState().getMaterial() == Material.WATER)
			return ReikaEngLibrary.rhowater;
		if (b.defaultBlockState().getMaterial() == Material.LAVA)
			return ReikaEngLibrary.rholava;
		if (b.defaultBlockState().getMaterial() == Material.ICE)
			return 917;
		return 2200;
	}

	public static double getProjectileVelocity(double dist, double ang, double dy, double gravity) {
		ang = Math.toRadians(ang);
		double denom = dist * Math.tan(ang) + dy;
		double root = Math.sqrt(0.5 * gravity * dist * dist / denom);
		return root / Math.cos(ang);
	}

	public static double getProjectileRange(double vel, double ang, double dy, double gravity) {
		ang = Math.toRadians(ang);
		double root = Math.pow(vel * Math.sin(ang), 2) + 2 * g * dy;
		double term = vel * Math.sin(ang) + Math.sqrt(root);
		return vel * Math.cos(ang) / gravity * term;
	}
/*
	public static void reflectEntitySpherical(BlockPos pos, Entity e) {
		double dx = e.getX() - pos.getX();
		double dy = e.getY() - pos.getY();
		double dz = e.getZ() - pos.getZ();
		Vec3 vec = Vec3.createVectorHelper(dx, dy, dz);
		double l = vec.lengthVector();
		vec.xCoord /= l;
		vec.yCoord /= l;
		vec.zCoord /= l;
		double vel = vec.dotProduct(Vec3.createVectorHelper(e.motionX, e.motionY, e.motionZ));
		e.motionX += -2 * vel * vec.xCoord;
		e.motionY += -2 * vel * vec.yCoord;
		e.motionZ += -2 * vel * vec.zCoord;
		e.velocityChanged = true;
	}
*/
	/**
	 * Blackbody
	 */
	public static int getColorForTemperature(int temp) {
		return (int) tempColorList.getValue(temp);
	}
}
