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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class LuaReadTank extends LuaMethod {

	public LuaReadTank() {
		super("readTank", IFluidHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		IFluidTank ifl = (IFluidTank)te;
		if (ifl.getFluid().isEmpty())
			return new Object[]{null, 0, ifl.getCapacity()};
		Object[] o = new Object[4];
		o[0] = BuiltInRegistries.FLUID.getKey(ifl.getFluid().getFluid()).getPath(); //todo check this
		o[1] = ifl.getFluidAmount();
		o[2] = ifl.getCapacity();
		o[3] = ifl.getFluid().getHoverName().getString();
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

