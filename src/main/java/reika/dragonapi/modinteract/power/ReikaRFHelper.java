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

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import reika.dragonapi.libraries.java.ReikaReflectionHelper;
import reika.dragonapi.libraries.mathsci.ReikaThermoHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

public class ReikaRFHelper {

    private static final int JoulePerRF_legacy = 5628;

    private static final int crucibleStoneMeltRF_Default = 200000; //configurable, defaults to 200k
    private static int crucibleStoneMelt = -1; //configurable, defaults to 200k

    private static final ReikaReflectionHelper.FieldSelector energyStorageFinder = f -> EnergyHandler.class.isAssignableFrom(f.getType());

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
            Class<?> c = Class.forName("thermalexpansion.core.TEProps");
            Field f = c.getDeclaredField("lavaRF");
            crucibleStoneMelt = f.getInt(null);
            crucibleStoneMelt = Mth.clamp(crucibleStoneMelt, 100000, 400000); //clamp read int to 1/2 and 2x normal
        } catch (Exception e) {

        }
    }

    /**
     * Forcibly drains up to {@code amt} FE from a block entity, the way an energy-sapping effect
     * would: first through its energy capability on every face, then (for storages that refuse
     * extraction) through any {@link EnergyHandler} fields it holds, and finally by lowering any
     * int field whose name contains "energy".
     */
    public static void drainStorage(BlockEntity te, int amt) {
        if (te == null)
            throw new IllegalArgumentException("Block entity cannot be null");
        if (amt <= 0)
            return;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<EnergyHandler> faces = getFaceHandlers(te);

        for (EnergyHandler handler : faces)
            extract(handler, amt);
        if (getStored(faces) == 0)
            return;

        for (Field f : ReikaReflectionHelper.getFields(te.getClass(), energyStorageFinder)) {
            try {
                if (!f.canAccess(te))
                    f.setAccessible(true);
                drainStorage((EnergyHandler) f.get(te), amt, visited);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (getStored(faces) == 0)
            return;

        try {
            drainEnergyFromFields(te, amt);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /** Forcibly drains up to {@code amt} FE from one energy store; see {@link #drainStorage(BlockEntity, int)}. */
    public static void drainStorage(EnergyHandler storage, int amt) {
        if (storage == null)
            throw new IllegalArgumentException("Energy storage cannot be null");
        if (amt <= 0)
            return;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        drainStorage(storage, amt, visited);
    }

    /** The distinct energy handlers a block entity exposes, over all six faces. */
    private static Set<EnergyHandler> getFaceHandlers(BlockEntity te) {
        Set<EnergyHandler> handlers = Collections.newSetFromMap(new IdentityHashMap<>());
        Level world = te.getLevel();
        if (world == null)
            return handlers;
        for (Direction dir : Direction.values()) {
            EnergyHandler handler = world.getCapability(Capabilities.Energy.BLOCK, te.getBlockPos(), te.getBlockState(), te, dir);
            if (handler != null)
                handlers.add(handler);
        }
        return handlers;
    }

    private static long getStored(Set<EnergyHandler> handlers) {
        long has = 0;
        for (EnergyHandler handler : handlers)
            has += handler.getAmountAsLong();
        return has;
    }

    private static void extract(EnergyHandler handler, int amt) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(amt, transaction) > 0)
                transaction.commit();
        }
    }

    private static void drainEnergyFromFields(Object o, int amt) throws ReflectiveOperationException {
        Collection<Field> c = ReikaReflectionHelper.getFields(o.getClass(), energyFieldFinder);
        for (Field f : c) {
            if (!f.canAccess(o))
                f.setAccessible(true);
            f.setInt(o, Math.max(0, f.getInt(o) - amt));
        }
    }

    private static void drainStorage(EnergyHandler ies, int amt, Set<Object> visited) {
        if (ies == null || !visited.add(ies))
            return;
        int has = ies.getAmountAsInt();
        extract(ies, amt);
        if (ies.getAmountAsLong() == 0)
            return;
        if (ies instanceof SimpleEnergyHandler simple) {
            simple.set(Math.max(0, has - amt));
        } else {
            try {
                Method m = ies.getClass().getMethod("setEnergyStored", int.class);
                m.invoke(ies, Math.max(0, has - amt));
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (ies.getAmountAsLong() == 0)
            return;
        try {
            drainEnergyFromFields(ies, amt);
        } catch (ReflectiveOperationException ignored) {
        }

        for (Field f : ReikaReflectionHelper.getFields(ies.getClass(), energyStorageFinder)) {
            try {
                if (!f.canAccess(ies))
                    f.setAccessible(true);
                drainStorage((EnergyHandler) f.get(ies), amt, visited);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

}
