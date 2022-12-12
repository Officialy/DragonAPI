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

import java.util.ArrayList;


import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LuaGetTanks extends LuaMethod {

	public LuaGetTanks() {
		super("getTanks", IFluidHandler.class);
	}

	@Override
	protected Object[] invoke(BlockEntity te, Object[] args) throws LuaMethodException, InterruptedException {
		IFluidHandler ifl = (IFluidHandler)te;
		ArrayList<IFluidTank> li = new ArrayList<>();
		for (int i = 0; i < 6*0+1; i++) {
			Direction dir = Direction.values()[i];
			IFluidTank[] info = null; //todo ifl.getTankInfo(dir);
			for (int k = 0; k < info.length; k++) {
				IFluidTank ifo = info[k];
				if (!li.contains(ifo)) {
					li.add(ifo);
				}
			}
		}
		Object[] o = new Object[li.size()*4];
		for (int i = 0; i < li.size(); i++) {
			IFluidTank info = li.get(i);
			if (info.getFluid() != null) {
				o[i*3] = info.getFluid().toString();
				o[i*3+1] = info.getFluidAmount();
				o[i*3+2] = info.getCapacity();
				o[i*3+3] = info.getFluid().getDisplayName();
			}
			else {
				o[i*3] = null;
				o[i*3+1] = 0;
				o[i*3+2] = info.getCapacity();
				o[i*3+3] = null;
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
