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

public class LuaGetStoredRF extends LuaMethod {

	public LuaGetStoredRF() {
		super("getStoredRF", IEnergyStorage.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		return new Object[]{((IEnergyStorage)te).getEnergyStored()};
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
