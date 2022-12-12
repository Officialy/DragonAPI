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
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

public class LuaReadTank extends LuaMethod {

	public LuaReadTank() {
		super("readTank", IFluidHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		IFluidTank ifl = (IFluidTank)te;
		if (ifl.getFluid() == null)
			return new Object[]{null, 0, ifl.getCapacity()};
		Object[] o = new Object[4];
		o[0] = ForgeRegistries.FLUIDS.getKey(ifl.getFluid().getFluid()).getPath(); //todo check this
		o[1] = ifl.getFluidAmount();
		o[2] = ifl.getCapacity();
		o[3] = ifl.getFluid().getDisplayName();
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns the contents of an fluid tank.\nArgs: Tank Index\nReturns: [Fluid, Amount, Capacity, Internal Name]";
	}

	@Override
	public String getArgsAsString() {
		return "int tankIndex";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
