package reika.dragonapi.modinteract;

import net.neoforged.bus.api.IEventBus;

import reika.dragonapi.ModList;

/**
 * ComputerCraft-free entry point for the CC: Tweaked integration. Every method here is safe to call without CC
 * installed; anything that touches CC classes is delegated to {@link CCCompat}, which is only ever loaded once
 * {@link ModList#COMPUTERCRAFT} is confirmed present.
 *
 * <p>This replaces 1.7.10's {@code @Injectable} {@code IPeripheral} on {@code TileEntityBase} plus
 * {@code PeripheralHandlerCC} (the {@code IPeripheralProvider} DragonAPIInit registered when ComputerCraft was loaded):
 * every DragonAPI-based block entity is a peripheral whose methods are the registered
 * {@link reika.dragonapi.modinteract.lua.LuaMethod}s valid for it.
 */
public final class CCHooks {

	private CCHooks() {

	}

	/** Called once from DragonAPI's constructor. */
	public static void init(IEventBus modEventBus) {
		if (ModList.COMPUTERCRAFT.isLoaded()) {
			CCCompat.init(modEventBus);
		}
	}

}
