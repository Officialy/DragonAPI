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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import reika.dragonapi.interfaces.blockentity.HasFluidResourceHandler;

public class LuaIsTankFull extends LuaMethod {

	public LuaIsTankFull() {
		super("isTankFull", HasFluidResourceHandler.class);
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
		long amount = handler.getAmountAsLong(index);
		return new Object[]{amount > 0 && amount >= handler.getCapacityAsLong(index, resource)};
	}

	@Override
	public String getDocumentation() {
		return "Checks if a tank is full.\nArgs: Tank Index\nReturns: true/false";
	}

	@Override
	public String getArgsAsString() {
		return "int tankIndex";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.BOOLEAN;
	}

}
