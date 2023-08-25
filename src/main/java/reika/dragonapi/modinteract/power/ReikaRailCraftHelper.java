/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.power;

import java.lang.reflect.Field;


import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.auxiliary.trackers.ReflectiveFailureTracker;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.libraries.mathsci.ReikaThermoHelper;

public class ReikaRailCraftHelper extends DragonAPI {

	private static Class sharedClass;
	private static Class solidClass;
	private static Class fluidClass;
	private static Field boiler;
	//private static Field lit;
	private static Field boilerBurnTime;
	private static Field boilerHeat;
	private static Field boilerBurning;

	private static final Fluid STEAM = null;//todo FluidRegistry.getFluid("steam");

	public static boolean doesRailCraftExist() {
		return ModList.RAILCRAFT.isLoaded();
	}

	/** Get energy of steam in joules. */
	public static double getSteamEnergy(int Tinit, int mB) {
		return getSteamBoilingEnergy(mB)+getSteamBucketEnergyToHeat(Tinit, mB);
	}

	/** Get the energy liberated by the conversion of one block of steam to one bucket of water. */
	public static double getSteamBoilingEnergy(int mB) {
		return ReikaThermoHelper.WATER_BOIL_ENTHALPY*mB*1000; //2260 kJ/kg * 1000 kg * 1000 J/kJ
	}

	/** Get the energy required to heat one water bucket to 100 degrees */
	public static double getSteamBucketEnergyToHeat(int Tinit, int mB) {
		double dT = 100-Tinit;
		if (dT < 0)
			dT = 0;
		return ReikaThermoHelper.WATER_HEAT*mB*1000*dT; //4.18 kJ/kgK * 1000 kg * 1000 J/kJ * dT K
	}

	public static int getAmountConvertibleSteam(int Tinit, long energy) {
		double per = getSteamEnergy(Tinit, 1);
		return (int)(energy/per);
	}

	public static boolean isFirebox(BlockEntity te) {
		return te != null && sharedClass.isAssignableFrom(te.getClass());
	}

	public static boolean isSolidFirebox(BlockEntity te) {
		return te != null && solidClass == te.getClass();
	}

	public static boolean isFluidFirebox(BlockEntity te) {
		return te != null && fluidClass == te.getClass();
	}

	static {
		if (ModList.RAILCRAFT.isLoaded()) {
			try {
				sharedClass = Class.forName("mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox");
				boiler = sharedClass.getDeclaredField("boiler");
				boiler.setAccessible(true);
				//lit = tileClass.getDeclaredField("wasLit");

				solidClass = Class.forName("mods.railcraft.common.blocks.machine.beta.TileBoilerFireboxSolid");
				fluidClass = Class.forName("mods.railcraft.common.blocks.machine.beta.TileBoilerFireboxFluid");

				Class<?> c2 = Class.forName("mods.railcraft.common.util.steam.SteamBoiler");
				boilerBurnTime = c2.getDeclaredField("burnTime");
				boilerBurnTime.setAccessible(true);

				boilerHeat = c2.getDeclaredField("heat");
				boilerHeat.setAccessible(true);

				boilerBurning = c2.getDeclaredField("isBurning");
				boilerBurning.setAccessible(true);
			}
			catch (Exception e) {
				DragonAPI.LOGGER.error("Error loading Firebox Handling!");
				e.printStackTrace();
				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.RAILCRAFT, e);
			}
		}
	}

	public static class FireboxWrapper {

		private final long tileID;

		public double temperature;
		private boolean isBurning;
		private double burnTime;

		public FireboxWrapper(BlockEntity te) {
			if (!isFirebox(te))
				throw new MisuseException("Tile is not a firebox!");
			tileID = System.identityHashCode(te);
			this.load(te);
		}

		/** Rerun this to reload the data from the tile. */
		public void load(BlockEntity te) {
			if (System.identityHashCode(te) != tileID)
				throw new MisuseException("You cannot reuse a FireboxWrapper instance for different TileEntities!");
			try {
				Object obj = boiler.get(te);
				temperature = boilerHeat.getDouble(obj);
				burnTime = boilerBurnTime.getDouble(obj);
				isBurning = boilerBurning.getBoolean(obj);
			}
			catch (Exception e) {
				DragonAPI.LOGGER.error("Error running Firebox Handling!");
				e.printStackTrace();
				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.RAILCRAFT, e);
			}
		}

		public void setBurning(int ticks) {
			isBurning = true;
			burnTime = Math.max(ticks, burnTime);
		}

		/** Call this to write the data to the BlockEntity. */
		public void write(BlockEntity te) {
			if (System.identityHashCode(te) != tileID)
				throw new MisuseException("You cannot reuse a FireboxWrapper instance for different TileEntities!");
			try {
				Object obj = boiler.get(te);
				boilerHeat.setDouble(obj, temperature);
				boilerBurning.setBoolean(obj, isBurning);
				boilerBurnTime.setDouble(obj, burnTime);
			}
			catch (Exception e) {
				DragonAPI.LOGGER.error("Error running Firebox Handling!");
				e.printStackTrace();
				ReflectiveFailureTracker.instance.logModReflectiveFailure(ModList.RAILCRAFT, e);
			}
		}
	}

}
