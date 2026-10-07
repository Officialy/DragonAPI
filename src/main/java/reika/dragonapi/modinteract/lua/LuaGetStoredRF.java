/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.modinteract.lua;

import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

//1.7.10 was @ModTileDependent on CoFH IEnergyProvider/IEnergyReceiver; now read through the always-present NeoForge energy capability
public class LuaGetStoredRF extends LuaMethod {

	public LuaGetStoredRF() {
		// Any block entity may expose FE through the capability, so there is no required class;
		// invoke() rejects block entities without one.
		super("getStoredRF", null);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{getEnergyHandler(te, args).getAmountAsInt()};
	}

	/**
	 * The energy capability of {@code te} on the compass side named by {@code args[0]}, as the
	 * 1.7.10 {@code ForgeDirection.valueOf} argument: a face name, or "unknown" for an unsided query.
	 */
	static EnergyHandler getEnergyHandler(BlockEntity te, Object[] args) throws LuaMethodException {
		if (args.length == 0 || !(args[0] instanceof String name))
			throw new LuaMethodException("Expected a side (compass direction)");
		@Nullable Direction side;
		if (name.equalsIgnoreCase("unknown")) {
			side = null;
		}
		else {
			side = Direction.byName(name.toLowerCase(Locale.ROOT));
			if (side == null)
				throw new LuaMethodException("Invalid side: " + name);
		}
		EnergyHandler handler = te.getLevel() == null ? null : te.getLevel().getCapability(
				Capabilities.Energy.BLOCK, te.getBlockPos(), te.getBlockState(), te, side);
		if (handler == null)
			throw new LuaMethodException("Block entity has no energy capability on side " + name);
		return handler;
	}

	@Override
	public String getDocumentation() {
		return "Returns the stored RF value.\nArgs: Side (compass)\nReturns: Energy";
	}

	@Override
	public String getArgsAsString() {
		return "String dir";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.INTEGER;
	}

}
