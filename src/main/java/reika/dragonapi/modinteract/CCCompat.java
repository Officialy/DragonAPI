package reika.dragonapi.modinteract;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.ModList;
import reika.dragonapi.base.BlockEntityBase;
import reika.dragonapi.interfaces.blockentity.PeripheralRelay;
import reika.dragonapi.modinteract.lua.LuaMethod;
import reika.dragonapi.modinteract.lua.LuaMethod.LuaMethodException;

/**
 * The CC-typed half of {@link CCHooks}. Only loaded when CC: Tweaked is present.
 */
final class CCCompat {

	/** Relay-to-relay hops (rift chains) one peripheral lookup may make before it gives up. */
	private static final int MAX_RELAY_DEPTH = 8;
	private static final ThreadLocal<int[]> RELAY_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	private CCCompat() {

	}

	static void init(IEventBus modEventBus) {
		modEventBus.addListener(CCCompat::registerCapabilities);
	}

	/**
	 * 1.7.10's PeripheralHandlerCC answered for every block; a capability has to be attached per type, so it goes on
	 * every block entity type of Reika's mods and answers only for those whose block entity is DragonAPI-based.
	 */
	@SuppressWarnings("unchecked")
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		Set<String> namespaces = new HashSet<>();
		for (ModList mod : ModList.getReikasMods())
			namespaces.add(mod.modid);
		int n = 0;
		for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
			if (id != null && namespaces.contains(id.getNamespace())) {
				event.registerBlockEntity(PeripheralCapability.get(), (BlockEntityType<BlockEntity>)type, CCCompat::getPeripheral);
				n++;
			}
		}
		DragonAPI.LOGGER.info("Exposed {} block entity types to ComputerCraft as peripherals.", n);
	}

	private static @Nullable IPeripheral getPeripheral(BlockEntity be, @Nullable Direction side) {
		if (!(be instanceof BlockEntityBase te) || be.getLevel() == null || be.getLevel().isClientSide() || be.isRemoved())
			return null;
		if (be instanceof PeripheralRelay relay)
			return getRelayedPeripheral(relay);
		if (te.getComputerPeripheral() instanceof ReikaPeripheral p)
			return p;
		ReikaPeripheral p = new ReikaPeripheral(te);
		te.setComputerPeripheral(p);
		return p;
	}

	/** 1.7.10 TileEntityRift: getType/getMethodNames/callMethod/attach/detach all went to the far tile's peripheral. */
	private static @Nullable IPeripheral getRelayedPeripheral(PeripheralRelay relay) {
		PeripheralRelay.Target target = relay.getRelayedPeripheral();
		if (target == null)
			return null;
		int[] depth = RELAY_DEPTH.get();
		if (depth[0] >= MAX_RELAY_DEPTH)
			return null;
		depth[0]++;
		try {
			return target.level().getCapability(PeripheralCapability.get(), target.pos(), target.side());
		}
		finally {
			depth[0]--;
		}
	}

	/** 1.7.10 TileEntityBase's IPeripheral implementation, as a wrapper since BlockEntity already owns getType(). */
	private static final class ReikaPeripheral implements IDynamicPeripheral {

		private final BlockEntityBase tile;
		private final String type;
		/** Resolved once: CC caches the name list on attach and calls back by index into it. */
		private final LuaMethod[] methods;
		private final String[] names;

		private ReikaPeripheral(BlockEntityBase te) {
			tile = te;
			type = te.getPeripheralType();
			methods = te.getLuaMethods();
			names = new String[methods.length];
			for (int i = 0; i < methods.length; i++)
				names[i] = methods[i].displayName;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public String[] getMethodNames() {
			return names.clone();
		}

		/**
		 * CC calls peripherals from the computer thread; Reika's methods read and change the world (SCRAM, turrets,
		 * gear ratios), so each one runs on the server thread, as the 1.7.10 world access implicitly assumed.
		 */
		@Override
		public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException {
			if (method < 0 || method >= methods.length)
				throw new LuaException("No such method: "+method);
			LuaMethod m = methods[method];
			Object[] args = arguments.getAll();
			for (int i = 0; i < args.length; i++) {
				//1.7.10 ComputerCraft handed every Lua number over as a Double, which the methods cast to
				if (args[i] instanceof Number num && !(args[i] instanceof Double))
					args[i] = num.doubleValue();
			}
			return context.executeMainThreadTask(() -> invokeCC(m, tile, args));
		}

		/** 1.7.10 LuaMethod.invokeCC. */
		private static Object[] invokeCC(LuaMethod m, BlockEntityBase te, Object[] args) throws LuaException {
			if (te.isRemoved())
				throw new LuaException("Peripheral is no longer present");
			try {
				Object[] ret = LuaMethod.call(m, te, args);
				return ret != null ? ret : new Object[0];
			}
			catch (LuaMethodException e) {
				throw new LuaException(e.getMessage());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new LuaException(m.displayName+" was interrupted");
			}
			catch (ClassCastException | IndexOutOfBoundsException e) {
				//malformed arguments; 1.7.10 let these escape as a "Java Exception Thrown" error
				throw new LuaException(m.displayName+"("+m.getArgsAsString()+"): bad arguments ("+e+")");
			}
			catch (RuntimeException e) {
				DragonAPI.LOGGER.warn("LuaMethod {} threw on {}", m, te, e);
				throw new LuaException(m.displayName+" failed: "+e);
			}
		}

		@Override
		public Object getTarget() {
			return tile;
		}

		@Override
		public boolean equals(@Nullable IPeripheral other) {
			return other instanceof ReikaPeripheral p && p.tile == tile;
		}

	}

}
