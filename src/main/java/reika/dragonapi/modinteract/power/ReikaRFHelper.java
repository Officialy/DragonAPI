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
import reika.dragonapi.libraries.mathsci.ReikaThermoHelper;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ReikaRFHelper {

    private static final int JoulePerRF_legacy = 5628;

    private static final int crucibleStoneMeltRF_Default = 200000; //configurable, defaults to 200k
    private static int crucibleStoneMelt = -1; //configurable, defaults to 200k

    private static final java.util.concurrent.ConcurrentMap<Class<?>, java.util.function.ToIntBiFunction<Object, Integer>> drainAdapters =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Explicit integrations can bypass extraction restrictions through their own storage API. */
    public static <T> void registerEnergyDrainAdapter(Class<T> type,
            java.util.function.ToIntBiFunction<T, Integer> adapter) {
        java.util.Objects.requireNonNull(type);
        java.util.Objects.requireNonNull(adapter);
        if (!EnergyHandler.class.isAssignableFrom(type) && !BlockEntity.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Drain adapters require an energy handler or block entity type");
        if (drainAdapters.putIfAbsent(type, (storage, amount) -> adapter.applyAsInt(type.cast(storage), amount)) != null)
            throw new IllegalStateException("A drain adapter is already registered for " + type.getName());
    }

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
        } catch (ClassNotFoundException absentIntegration) {
            // The default is the original conversion for an absent Thermal integration.
        } catch (ReflectiveOperationException | RuntimeException incompatibleIntegration) {
            reika.dragonapi.DragonAPI.LOGGER.debug("Could not read the Thermal stone-melting conversion", incompatibleIntegration);
        }
    }

    /**
     * Saps at most {@code amt} FE in total. Native SimpleEnergyHandler stores retain forced
     * draining; other stores use their extraction contract or an explicitly registered adapter.
     * Adapters match the exact registered class and own their dirty-state notification.
     */
    public static void drainStorage(BlockEntity te, int amt) {
        if (te == null)
            throw new IllegalArgumentException("Block entity cannot be null");
        if (amt <= 0 || te.getLevel() != null && te.getLevel().isClientSide())
            return;
        if (drainAdapters.containsKey(te.getClass()) || te instanceof EnergyHandler) {
            drain(te, amt);
            return;
        }
        Level world = te.getLevel();
        if (world == null)
            return;
        var handlers = new java.util.ArrayList<EnergyHandler>(7);
        handlers.add(world.getCapability(Capabilities.Energy.BLOCK,
                te.getBlockPos(), te.getBlockState(), te, null));
        for (Direction direction : Direction.values())
            handlers.add(world.getCapability(Capabilities.Energy.BLOCK,
                    te.getBlockPos(), te.getBlockState(), te, direction));
        drainHandlers(handlers, amt);
    }

    /** Unsided and face views may alias one store; every view shares one drain budget. */
    static int drainHandlers(Iterable<? extends EnergyHandler> handlers, int amount) {
        Set<EnergyHandler> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int remaining = amount;
        for (EnergyHandler handler : handlers) {
            if (remaining <= 0)
                break;
            if (handler != null && visited.add(handler))
                remaining -= drain(handler, remaining);
        }
        return amount - remaining;
    }

    public static void drainStorage(EnergyHandler storage, int amt) {
        if (storage == null)
            throw new IllegalArgumentException("Energy storage cannot be null");
        if (amt > 0)
            drain(storage, amt);
    }

    private static int drain(Object storage, int amount) {
        var adapter = drainAdapters.get(storage.getClass());
        int extracted;
        if (adapter != null) {
            extracted = adapter.applyAsInt(storage, amount);
        } else if (storage instanceof SimpleEnergyHandler simple) {
            int previous = simple.getAmountAsInt();
            extracted = Math.min(previous, amount);
            simple.set(previous - extracted);
        } else if (storage instanceof EnergyHandler handler) {
            try (Transaction transaction = Transaction.openRoot()) {
                extracted = handler.extract(amount, transaction);
                validateDrained(extracted, amount);
                if (extracted > 0)
                    transaction.commit();
            }
        } else {
            throw new IllegalArgumentException("No explicit drain adapter for " + storage.getClass().getName());
        }
        validateDrained(extracted, amount);
        return extracted;
    }

    private static void validateDrained(int extracted, int amount) {
        if (extracted < 0 || extracted > amount)
            throw new IllegalStateException("Energy drain exceeded its requested budget: " + extracted + "/" + amount);
    }
}
