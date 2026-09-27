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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import reika.dragonapi.interfaces.blockentity.HasFluidResourceHandler;

public class LuaReadTank extends LuaMethod {

	public LuaReadTank() {
		super("readTank", HasFluidResourceHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		ResourceHandler<FluidResource> handler = ((HasFluidResourceHandler)te).getFluidHandler(null);
		if (handler == null)
			throw new LuaMethodException("Block entity has no fluid capability");
		if (args.length == 0 || !(args[0] instanceof Number number))
			throw new LuaMethodException("Expected a tank index");
		int index = number.intValue();
		if (index < 0 || index >= handler.size())
			throw new LuaMethodException("Tank index out of bounds: " + index);
		FluidResource resource = handler.getResource(index);
		if (resource.isEmpty())
			return new Object[]{null, 0, handler.getCapacityAsInt(index, FluidResource.EMPTY), null};
		Object[] o = new Object[4];
		o[0] = BuiltInRegistries.FLUID.getKey(resource.getFluid()).toString();
		o[1] = handler.getAmountAsInt(index);
		o[2] = handler.getCapacityAsInt(index, resource);
		o[3] = resource.toStack(1).getHoverName().getString();
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
