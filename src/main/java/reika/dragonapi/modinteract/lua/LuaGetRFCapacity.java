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


import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

public class LuaGetRFCapacity extends LuaMethod {

	public LuaGetRFCapacity() {
		super("getMaxStoredRF", IEnergyStorage.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{((IEnergyStorage)te).getMaxEnergyStored()};
	}

	@Override
	public String getDocumentation() {
		return "Returns the RF capacity.\nArgs: Side (compass)\nReturns: Capacity";
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
