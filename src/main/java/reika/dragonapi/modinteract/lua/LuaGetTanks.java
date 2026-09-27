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

public class LuaGetTanks extends LuaMethod {

	public LuaGetTanks() {
		super("getTanks", HasFluidResourceHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		ResourceHandler<FluidResource> handler = ((HasFluidResourceHandler)te).getFluidHandler(null);
		if (handler == null)
			throw new LuaMethodException("Block entity has no fluid capability");
		int tanks = handler.size();
		Object[] o = new Object[tanks*4];
		for (int i = 0; i < tanks; i++) {
			FluidResource fluid = handler.getResource(i);
			int offset = i*4;
			if (!fluid.isEmpty()) {
				o[offset] = fluid.getFluid().toString();
				o[offset+1] = handler.getAmountAsInt(i);
				o[offset+2] = handler.getCapacityAsInt(i, fluid);
				o[offset+3] = fluid.toStack(1).getHoverName().getString();
			}
			else {
				o[offset] = null;
				o[offset+1] = 0;
				o[offset+2] = handler.getCapacityAsInt(i, FluidResource.EMPTY);
				o[offset+3] = null;
			}
		}
		return o;
	}

	@Override
	public String getDocumentation() {
		return "Returns all the fluid tanks.\nArgs: None\nReturns: List of [Fluid, Amount, Capacity, Internal ID]";
	}

	@Override
	public String getArgsAsString() {
		return "";
	}

	@Override
	public ReturnType getReturnType() {
		return ReturnType.ARRAY;
	}

}
