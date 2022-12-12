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

import net.minecraft.util.Mth;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import reika.dragonapi.libraries.java.ReikaReflectionHelper;
import reika.dragonapi.libraries.mathsci.ReikaThermoHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

public class ReikaRFHelper {

    private static final int JoulePerRF_legacy = 5628;

    private static final int crucibleStoneMeltRF_Default = 200000; //configurable, defaults to 200k
    private static int crucibleStoneMelt = -1; //configurable, defaults to 200k

    private static final ReikaReflectionHelper.FieldSelector energyStorageFinder = f -> IEnergyStorage.class.isAssignableFrom(f.getType());

    private static final ReikaReflectionHelper.FieldSelector energyFieldFinder = f -> f.getType() == int.class && f.getName().toLowerCase(Locale.ENGLISH).contains("energy");

    //Default value yields 1RF/t=520W, with config ranges from 1RF/t=260W to 1RF/t=1040W
    public static int getWattsPerRF() {
        return (int) (20 * ReikaThermoHelper.ROCK_MELT_ENERGY / getRFPerStoneBlock()); //*20 for /t vs /s
    }

    public static long getRFPerStoneBlock() {
        return crucibleStoneMelt > 0 ? crucibleStoneMelt : crucibleStoneMeltRF_Default;
    }

    static {
        try {
            Class c = Class.forName("thermalexpansion.core.TEProps");
            Field f = c.getDeclaredField("lavaRF");
            crucibleStoneMelt = f.getInt(null);
            crucibleStoneMelt = Mth.clamp(crucibleStoneMelt, 100000, 400000); //clamp read int to 1/2 and 2x normal
        } catch (Exception e) {

        }
    }

    public static void drainStorage(EnergyStorage te, int amt) {
        int has = 0;
        has += te.getEnergyStored();
        te.extractEnergy(amt, false);

        int has2 = 0;
            has2 += te.getEnergyStored();

        if (has2 == 0) {
            return;
        }

        Collection<Field> storage = ReikaReflectionHelper.getFields(te.getClass(), energyStorageFinder);
        for (Field f : storage) {
            try {
                drainStorage((IEnergyStorage) f.get(te), amt);
            } catch (Exception e) {

            }
        }

        has2 = 0;
        for (int i = 0; i < 6; i++)
            has2 += te.getEnergyStored();

        if (has2 == 0) {
            return;
        }

        try {
            drainEnergyFromFields(te, amt);
        } catch (Exception e) {

        }
    }

    private static void drainEnergyFromFields(Object o, int amt) throws Exception {
        Collection<Field> c = ReikaReflectionHelper.getFields(o.getClass(), energyFieldFinder);
        for (Field f : c) {
            f.setInt(o, Math.max(0, f.getInt(o) - amt));
        }
    }

    private static void drainStorage(IEnergyStorage ies, int amt) throws Exception {
        int has = ies.getEnergyStored();
        ies.extractEnergy(amt, false);
        if (ies.getEnergyStored() == 0)
            return;
        try {
            Method m = ies.getClass().getMethod("setEnergyStored", int.class);
            m.invoke(ies, Math.max(0, has - amt));
        } catch (Exception e) {

        }
        if (ies.getEnergyStored() == 0)
            return;
        drainEnergyFromFields(ies, amt);
    }

}
